package net.shadowmage.ancientwarfare.core.forge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.level.ChunkEvent.Load;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.block.AdvancedLootChestBlock;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.compat.ValkyrienShipyardCompat;
import net.shadowmage.ancientwarfare.core.forge.config.AWStructureConfig;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenTemplateRegistry;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerationQueue;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenTemplateRegistry;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenerationData;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.LargeIslandWorldGenerationData;
import net.shadowmage.ancientwarfare.core.structure.LargeIslandWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.NaturalGenerationContext;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeAwsWorldGenerationEvents {
   private static final Map<UUID, ChunkPos> LAST_PLAYER_CHUNKS = new HashMap<>();
   private static int cooldown;

   private ForgeAwsWorldGenerationEvents() {
   }

   public static void refreshConfigCooldown() {
      cooldown = 0;
   }

   @SubscribeEvent
   public static void onChunkLoad(Load event) {
      if (event.isNewChunk() && event.getLevel() instanceof ServerLevel level && level.m_46472_() == Level.f_46428_) {
         ChunkPos chunk = event.getChunk().m_7697_();
         if (AwsWorldGenerator.isCandidateChunk(level.m_7328_(), chunk) && !NaturalGenerationContext.isInternalChunk(chunk)) {
            if (level.m_7654_().m_18695_()) {
               queueNaturalCandidate(level, chunk);
            } else {
               level.m_7654_().execute(() -> queueNaturalCandidate(level, chunk));
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END
         && event.player instanceof ServerPlayer player
         && player.m_284548_().m_46472_() == Level.f_46428_
         && player.f_19797_ % 20 == 0) {
         ChunkPos current = player.m_146902_();
         ChunkPos previous = LAST_PLAYER_CHUNKS.put(player.m_20148_(), current);
         if (AWCorePlatform.gameplayConfig().awtEnabled()
            && ValkyrienShipyardCompat.isReady()
            && !ValkyrienShipyardCompat.isShipyardChunk(player.m_284548_(), current.f_45578_, current.f_45579_)
            && AwtWorldGenerator.isCandidateChunk(player.m_284548_().m_7328_(), current)) {
            AwtWorldGenerator.queueAt(player.m_284548_(), current, false, "");
         }

         if (!current.equals(previous)) {
            queueNaturalCandidate(player.m_284548_(), current);
         }
      }
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         ServerLevel overworld = event.getServer().m_129783_();
         LargeIslandWorldGenerator.tick(overworld);
         if (!LargeIslandWorldGenerator.active(overworld)) {
            AwtWorldGenerator.tick(overworld);
            if (AwtWorldGenerationData.get(overworld).active() == null) {
               if (cooldown > 0) {
                  cooldown--;
               } else {
                  if (processPending(overworld)) {
                     cooldown = AWStructureConfig.awsCandidateIntervalTicks() - 1;
                  }
               }
            }
         }
      }
   }

   static boolean processPending(ServerLevel level) {
      if (level.m_46472_() == Level.f_46428_
         && AWCorePlatform.gameplayConfig().awsEnabled()
         && !LargeIslandWorldGenerator.active(level)
         && AwtWorldGenerationData.get(level).active() == null
         && ValkyrienShipyardCompat.isReady()) {
         AwsWorldGenerationQueue queue = AwsWorldGenerationQueue.get(level);
         ChunkPos chunk = queue.peek();
         if (chunk == null) {
            return false;
         } else {
            boolean shipyard = ValkyrienShipyardCompat.isShipyardChunk(level, chunk.f_45578_, chunk.f_45579_);
            if (!ValkyrienShipyardCompat.isReady()) {
               return false;
            } else {
               if (!shipyard) {
                  try {
                     AwsWorldGenerator.generateAt(level, chunk, false);
                  } catch (RuntimeException var5) {
                     AncientWarfareCore.LOG
                        .error("AWS candidate {}, {} failed; recording the attempt to prevent a retry loop", new Object[]{chunk.f_45578_, chunk.f_45579_, var5});
                  }
               }

               queue.markAttempted(chunk);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerLoggedOutEvent event) {
      LAST_PLAYER_CHUNKS.remove(event.getEntity().m_20148_());
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      LargeIslandWorldGenerator.shutdown();
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      LargeIslandWorldGenerator.shutdown();
      LAST_PLAYER_CHUNKS.clear();
      cooldown = 0;
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      if (!event.getLevel().f_46443_) {
         if (event.getLevel().m_7702_(event.getPos()) instanceof ChestBlockEntity) {
            AdvancedLootChestBlock.restoreFormerLootrChest(event.getLevel(), event.getPos());
            AdvancedLootChestBlock.generateConnectedLoot(event.getLevel(), event.getPos(), event.getEntity());
         }
      }
   }

   static void queueNaturalCandidate(ServerLevel level, ChunkPos chunk) {
      if (level.m_46472_() == Level.f_46428_
         && AWCorePlatform.gameplayConfig().awsEnabled()
         && !NaturalGenerationContext.isInternalChunk(chunk)
         && AwsWorldGenerator.isCandidateChunk(level.m_7328_(), chunk)
         && !insideActiveConstruction(level, chunk)
         && ValkyrienShipyardCompat.isReady()) {
         if (!ValkyrienShipyardCompat.isShipyardChunk(level, chunk.f_45578_, chunk.f_45579_)) {
            AwsWorldGenerationQueue queue = AwsWorldGenerationQueue.get(level);
            if (queue.enqueue(chunk)) {
               AncientWarfareCore.LOG
                  .debug("Queued AWS world-generation candidate chunk {}, {}; pending={}", new Object[]{chunk.f_45578_, chunk.f_45579_, queue.pendingCount()});
            }
         }
      }
   }

   private static boolean insideActiveConstruction(ServerLevel level, ChunkPos chunk) {
      LargeIslandWorldGenerationData.Task island = LargeIslandWorldGenerationData.get(level).active();
      if (island != null) {
         AwsWorldGenTemplateRegistry.Template template = AwsWorldGenTemplateRegistry.templateForActivePath(island.path);
         if (template != null) {
            Direction facing = Direction.m_122407_(island.facing);
            if (intersectsChunk(
               chunk, island.minX - 16, island.minZ - 16, island.minX + template.rotatedSizeX(facing) + 15, island.minZ + template.rotatedSizeZ(facing) + 15
            )) {
               return true;
            }
         }
      }

      AwtWorldGenerationData.TownTask town = AwtWorldGenerationData.get(level).active();
      if (town == null) {
         return false;
      } else {
         AwtWorldGenTemplateRegistry.Template template = AwtWorldGenTemplateRegistry.templateForActivePath(town.path);
         int halfSize = template == null ? 192 : template.chunks() * 8;
         return intersectsChunk(chunk, town.x - halfSize - 16, town.z - halfSize - 16, town.x + halfSize + 15, town.z + halfSize + 15);
      }
   }

   private static boolean intersectsChunk(ChunkPos chunk, int minX, int minZ, int maxX, int maxZ) {
      return chunk.f_45578_ >= Math.floorDiv(minX, 16)
         && chunk.f_45578_ <= Math.floorDiv(maxX, 16)
         && chunk.f_45579_ >= Math.floorDiv(minZ, 16)
         && chunk.f_45579_ <= Math.floorDiv(maxZ, 16);
   }
}
