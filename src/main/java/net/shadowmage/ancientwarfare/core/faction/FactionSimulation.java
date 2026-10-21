package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;
import net.shadowmage.ancientwarfare.core.tile.AdvancedSpawnerBlockEntity;

/**
 * Faction life far from the player, one step per game day: settlements grow their people, people
 * pay taxes into the faction's treasury, the treasury recruits soldiers from the people, and full
 * settlements grow a tier. Near a player, a settlement's garrison stands as real NPCs: losses come
 * off the garrison, and missing soldiers come back one at a time, out of sight, while the
 * simulated garrison still has them.
 */
public final class FactionSimulation {
   public static final long DAY_TICKS = 24000L;
   private static final int GARRISON_CHECK_TICKS = 100;
   private static final int RESPAWN_TICKS = 600;
   private static final double ACTIVE_RANGE = 128.0;
   private static final double HIDDEN_RANGE = 24.0;
   private static final int GARRISON_MARGIN = 24;
   private static final int LOSS_MARGIN = 32;
   private static final int RECRUIT_COST = 5;
   private static final int MAX_RECRUITS_PER_DAY = 2;
   private static final int TIER_COST = 60;
   private static final int MAX_TIER = 3;

   private FactionSimulation() {
   }

   public static int startingPopulation(int soldiers, int civilians) {
      return Math.max(4, soldiers + civilians * 3);
   }

   /** People a settlement can hold: grows with its starting size and tier. */
   public static int capacity(FactionWorldData.Settlement settlement) {
      int base = "town".equals(settlement.kind()) ? 60 : 12;
      return base + settlement.tier() * base / 2 + settlement.garrison();
   }

   public static void tick(ServerLevel level) {
      FactionWorldData data = FactionWorldData.get(level);
      long day = level.getGameTime() / DAY_TICKS;
      if (data.lastSimulatedDay() < 0L) {
         data.setLastSimulatedDay(day);
      } else if (day > data.lastSimulatedDay()) {
         data.setLastSimulatedDay(day);
         simulateDay(level, data);
      }

      FactionExpansion.tick(level, data);
      FactionArmies.tick(level, data);

      if (level.getGameTime() % GARRISON_CHECK_TICKS == 0L && !level.players().isEmpty()) {
         maintainGarrisons(level, data);
      }
   }

   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      Map<String, Integer> recruited = new HashMap<>();

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         int capacity = capacity(settlement);
         if (settlement.population < capacity) {
            settlement.population = Math.min(capacity, settlement.population + Math.max(1, settlement.population / 20));
         }

         String faction = settlement.faction();
         int gold = data.treasury(faction) + settlement.population / 4;

         // A fort is a garrison with a few hands to keep it; anywhere else a third of the people may serve.
         int maxGarrison = FactionAI.isFort(settlement) ? FactionExpansion.FORT_GARRISON + settlement.tier * 4 : settlement.population / 3;

         for (int i = 0; i < MAX_RECRUITS_PER_DAY && settlement.garrison < maxGarrison && settlement.population > 1 && gold >= RECRUIT_COST; i++) {
            settlement.garrison++;
            settlement.population--;
            gold -= RECRUIT_COST;
            recruited.merge(faction, 1, Integer::sum);
         }

         int tierCost = TIER_COST * (settlement.tier + 1);
         if (settlement.population >= capacity && settlement.tier < MAX_TIER && gold >= tierCost) {
            settlement.tier++;
            gold -= tierCost;
         }

         data.setTreasury(faction, gold);
      }

      FactionExpansion.simulateDay(level, data, data.lastSimulatedDay());
      FactionAI.simulateDay(level, data, data.lastSimulatedDay());
      FactionArmies.simulateDay(level, data, data.lastSimulatedDay());
      data.setDirty();
      AncientWarfareCore.LOG.debug("Simulated faction day {}: recruits {}", data.lastSimulatedDay(), recruited);
   }

   /** A faction NPC died: soldiers come off their settlement's garrison, civilians off its people. */
   public static void onNpcDeath(ServerLevel level, FactionNpcEntity npc) {
      if (FactionArmies.armyId(npc) != 0) {
         return;
      }

      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement settlement = settlementAt(data, npc.getFactionName(), npc.getX(), npc.getZ(), LOSS_MARGIN);
      if (settlement != null) {
         if (isCivilian(npc)) {
            settlement.population = Math.max(0, settlement.population - 1);
         } else {
            settlement.garrison = Math.max(0, settlement.garrison - 1);
         }

         data.setDirty();
      }
   }

   /**
    * A faction NPC entered the world: if it belongs to the faction its settlement was taken from,
    * it fell in that battle (soldiers) or now serves the new owner (civilians). Returns false when
    * the NPC should not be added.
    */
   public static boolean onNpcJoin(ServerLevel level, FactionNpcEntity npc) {
      FactionWorldData data = FactionWorldData.get(level);
      int army = FactionArmies.armyId(npc);
      if (army != 0) {
         // Soldiers of an army that has since gone home or fallen.
         return data.army(army) != null;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.capturedFrom.equals(npc.getFactionName()) && settlement.contains(npc.getX(), npc.getZ(), GARRISON_MARGIN)) {
            if (isCivilian(npc)) {
               npc.setFactionName(settlement.faction());
               return true;
            }

            return false;
         }
      }

      return true;
   }

   public static FactionWorldData.Settlement settlementAt(FactionWorldData data, String faction, double x, double z, int margin) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction) && settlement.contains(x, z, margin)) {
            return settlement;
         }
      }

      return null;
   }

   private static void maintainGarrisons(ServerLevel level, FactionWorldData data) {
      List<ServerPlayer> players = level.players();

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         BlockPos center = settlement.center();
         boolean near = false;

         for (ServerPlayer player : players) {
            double distance = player.distanceToSqr(center.getX(), player.getY(), center.getZ());
            if (distance < PlayerDiplomacy.DISCOVERY_RANGE * PlayerDiplomacy.DISCOVERY_RANGE) {
               PlayerDiplomacy.discover(player, settlement.faction());
            }

            if (distance < ACTIVE_RANGE * ACTIVE_RANGE) {
               near = true;
            }
         }

         if (near && level.isLoaded(settlement.min()) && level.isLoaded(settlement.max())) {
            if (!settlement.manned) {
               if (spawnersRemain(level, settlement)) {
                  continue;
               }

               settlement.manned = true;
               data.setDirty();
            }

            if (level.getGameTime() - settlement.lastRespawn >= RESPAWN_TICKS && liveSoldiers(level, settlement) < settlement.garrison) {
               settlement.lastRespawn = level.getGameTime();
               respawnSoldier(level, settlement);
            }
         }
      }
   }

   private static boolean spawnersRemain(ServerLevel level, FactionWorldData.Settlement settlement) {
      for (int cx = settlement.min().getX() >> 4; cx <= settlement.max().getX() >> 4; cx++) {
         for (int cz = settlement.min().getZ() >> 4; cz <= settlement.max().getZ() >> 4; cz++) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) {
               return true;
            }

            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
               if (blockEntity instanceof AdvancedSpawnerBlockEntity && settlement.contains(blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getZ(), 0)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   static int liveSoldiers(ServerLevel level, FactionWorldData.Settlement settlement) {
      AABB area = new AABB(settlement.min(), settlement.max().offset(1, 1, 1)).inflate(GARRISON_MARGIN, 32.0, GARRISON_MARGIN);
      return level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getFactionName().equals(settlement.faction()) && !isCivilian(npc))
         .size();
   }

   private static void respawnSoldier(ServerLevel level, FactionWorldData.Settlement settlement) {
      RandomSource random = level.getRandom();

      for (int attempt = 0; attempt < 8; attempt++) {
         int x = settlement.min().getX() + random.nextInt(settlement.max().getX() - settlement.min().getX() + 1);
         int z = settlement.min().getZ() + random.nextInt(settlement.max().getZ() - settlement.min().getZ() + 1);
         if (level.getNearestPlayer(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, HIDDEN_RANGE, false) == null
            && spawnSoldier(level, settlement.faction(), x, z) != null) {
            return;
         }
      }
   }

   /** Spawns one of the faction's soldiers (a third of them archers) standing on the ground at x, z; null when there is no room. */
   static FactionNpcEntity spawnSoldier(ServerLevel level, String faction, int x, int z) {
      RandomSource random = level.getRandom();
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos pos = new BlockPos(x, y, z);
      if (!level.getBlockState(pos.below()).getFluidState().isEmpty()
         || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
         || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
         return null;
      }

      EntityType<? extends FactionNpcEntity> type = random.nextInt(3) == 0 ? AWCoreEntityTypes.FACTION_ARCHER_NPC.get() : AWCoreEntityTypes.FACTION_SOLDIER_NPC.get();
      FactionNpcEntity npc = type.create(level);
      if (npc == null) {
         return null;
      }

      npc.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
      npc.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
      npc.setFactionName(faction);
      level.addFreshEntity(npc);
      return npc;
   }

   private static boolean isCivilian(FactionNpcEntity npc) {
      return FactionNpcRole.byId(npc.getNpcSubtype()).map(FactionNpcRole::isCivilian).orElse(false);
   }
}
