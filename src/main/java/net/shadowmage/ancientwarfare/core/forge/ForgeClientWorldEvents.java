package net.shadowmage.ancientwarfare.core.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.client.BardMusicPlaybackManager;
import net.shadowmage.ancientwarfare.core.common.worksite.BoundedArea;
import net.shadowmage.ancientwarfare.core.entity.BallistaMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.BallistaTurretStandEntity;
import net.shadowmage.ancientwarfare.core.entity.BatteringRamEntity;
import net.shadowmage.ancientwarfare.core.entity.CannonMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.CannonTurretStandEntity;
import net.shadowmage.ancientwarfare.core.entity.CatapultMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.CatapultTurretStandEntity;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;
import net.shadowmage.ancientwarfare.core.entity.HwachaEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.TrebuchetFixedEntity;
import net.shadowmage.ancientwarfare.core.entity.TrebuchetTurretEntity;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplatePlacer;
import net.shadowmage.ancientwarfare.core.tile.AnimalFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.SurvivalStructureBuilderBlockEntity;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class ForgeClientWorldEvents {
   private static final int CHUNK_RENDER_RADIUS = 4;
   private static final int MAX_PREVIEW_CACHE_ENTRIES = 64;
   private static final float PREVIEW_ALPHA = 0.32F;
   private static final int LARGE_PREVIEW_THRESHOLD = 2048;
   private static final int LARGE_PREVIEW_BLOCKS_PER_BATCH = 1536;
   private static final Map<ForgeClientWorldEvents.PreviewKey, AwsTemplatePlacer.PreviewData> PREVIEW_CACHE = new HashMap<>();
   private static final Map<ForgeClientWorldEvents.PreviewKey, ForgeClientWorldEvents.GhostPreviewCache> GHOST_PREVIEW_CACHE = new HashMap<>();
   private static boolean ballistaFireWasDown;

   private ForgeClientWorldEvents() {
   }

   @SubscribeEvent
   public static void clientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         BardMusicPlaybackManager.clientTick();
         handleVehicleInput();
      }
   }

   private static void handleVehicleInput() {
      Minecraft minecraft = Minecraft.m_91087_();
      if (minecraft.f_91074_ != null && minecraft.f_91080_ == null) {
         Entity vehicle = minecraft.f_91074_.m_20202_();
         if (vehicle instanceof ChestPushcartEntity cart) {
            long window = minecraft.m_91268_().m_85439_();
            float forward = 0.0F;
            float turn = 0.0F;
            if (InputConstants.m_84830_(window, 87)) {
               forward++;
            }

            if (InputConstants.m_84830_(window, 83)) {
               forward--;
            }

            if (InputConstants.m_84830_(window, 65)) {
               turn++;
            }

            if (InputConstants.m_84830_(window, 68)) {
               turn--;
            }

            if (vehicle instanceof BallistaTurretStandEntity) {
               if (forward != 0.0F || turn != 0.0F) {
                  AWCoreNetwork.sendAdjustBallistaTurret(forward, -turn * 2.0F);
               }
            } else if (vehicle instanceof CatapultTurretStandEntity catapultStand) {
               if (forward != 0.0F || turn != 0.0F) {
                  AWCoreNetwork.sendVehicleInput(forward, -turn);
                  if (forward != 0.0F && minecraft.f_91074_ != null) {
                     int nextPercent = Math.max(0, Math.min(30, Math.round(catapultStand.getVelocityBonus() * 100.0F + forward)));
                     minecraft.f_91074_.m_5661_(Component.m_237110_("guistrings.vehicle.catapult_velocity_bonus", new Object[]{nextPercent}), true);
                  }
               }
            } else if (vehicle instanceof CannonMobileEntity cartCannon) {
               cartCannon.setVehicleInput(forward, turn);
               AWCoreNetwork.sendVehicleInput(forward, turn);
            } else if (vehicle instanceof CannonTurretStandEntity) {
               if (forward != 0.0F || turn != 0.0F) {
                  AWCoreNetwork.sendVehicleInput(forward, -turn);
               }
            } else if (vehicle instanceof TrebuchetTurretEntity turretTrebuchet) {
               if (forward != 0.0F || turn != 0.0F) {
                  AWCoreNetwork.sendVehicleInput(forward, turn);
                  if (forward != 0.0F && minecraft.f_91074_ != null) {
                     int nextPercent = Math.max(0, Math.min(30, Math.round(turretTrebuchet.getVelocityBonus() * 100.0F + forward)));
                     minecraft.f_91074_.m_5661_(Component.m_237110_("guistrings.vehicle.catapult_velocity_bonus", new Object[]{nextPercent}), true);
                  }
               }
            } else if (vehicle instanceof TrebuchetFixedEntity fixedTrebuchet) {
               if (forward != 0.0F) {
                  AWCoreNetwork.sendVehicleInput(forward, 0.0F);
                  if (minecraft.f_91074_ != null) {
                     int nextPercent = Math.max(0, Math.min(30, Math.round(fixedTrebuchet.getVelocityBonus() * 100.0F + forward)));
                     minecraft.f_91074_.m_5661_(Component.m_237110_("guistrings.vehicle.catapult_velocity_bonus", new Object[]{nextPercent}), true);
                  }
               }
            } else {
               cart.setVehicleInput(forward, turn);
               AWCoreNetwork.sendVehicleInput(forward, turn);
            }

            if (vehicle instanceof CatapultMobileEntity && !(vehicle instanceof CatapultTurretStandEntity) && !(vehicle instanceof TrebuchetFixedEntity)) {
               float yawDelta = 0.0F;
               float velocityDelta = 0.0F;
               CatapultMobileEntity catapult = (CatapultMobileEntity)vehicle;
               if (!catapult.hasFixedTurret() && ForgeClientInputEvents.BALLISTA_LEFT.m_90857_()) {
                  yawDelta--;
               }

               if (!catapult.hasFixedTurret() && ForgeClientInputEvents.BALLISTA_RIGHT.m_90857_()) {
                  yawDelta++;
               }

               if (ForgeClientInputEvents.BALLISTA_UP.m_90857_()) {
                  velocityDelta++;
               }

               if (ForgeClientInputEvents.BALLISTA_DOWN.m_90857_()) {
                  velocityDelta--;
               }

               if (yawDelta != 0.0F || velocityDelta != 0.0F) {
                  AWCoreNetwork.sendAdjustBallistaTurret(velocityDelta, yawDelta);
                  if (velocityDelta != 0.0F && minecraft.f_91074_ != null) {
                     int nextPercent = Math.max(0, Math.min(30, Math.round(catapult.getVelocityBonus() * 100.0F + velocityDelta)));
                     minecraft.f_91074_.m_5661_(Component.m_237110_("guistrings.vehicle.catapult_velocity_bonus", new Object[]{nextPercent}), true);
                  }
               }

               boolean fireDown = InputConstants.m_84830_(window, 32);
               if (fireDown && !ballistaFireWasDown) {
                  AWCoreNetwork.sendFireBallista();
               }

               ballistaFireWasDown = fireDown;
            } else if (vehicle instanceof CatapultTurretStandEntity) {
               boolean fireDown = InputConstants.m_84830_(window, 32);
               if (fireDown && !ballistaFireWasDown) {
                  AWCoreNetwork.sendFireBallista();
               }

               ballistaFireWasDown = fireDown;
            } else if (vehicle instanceof CannonTurretStandEntity) {
               boolean fireDown = InputConstants.m_84830_(window, 32);
               if (fireDown && !ballistaFireWasDown) {
                  AWCoreNetwork.sendFireBallista();
               }

               ballistaFireWasDown = fireDown;
            } else if (vehicle instanceof TrebuchetFixedEntity) {
               boolean fireDown = InputConstants.m_84830_(window, 32);
               if (fireDown && !ballistaFireWasDown) {
                  AWCoreNetwork.sendFireBallista();
               }

               ballistaFireWasDown = fireDown;
            } else if (vehicle instanceof HwachaEntity) {
               boolean fireDown = InputConstants.m_84830_(window, 32);
               if (fireDown) {
                  AWCoreNetwork.sendFireBallista();
               }

               ballistaFireWasDown = fireDown;
            } else if (vehicle instanceof BatteringRamEntity) {
               boolean fireDown = InputConstants.m_84830_(window, 32);
               if (fireDown && !ballistaFireWasDown) {
                  AWCoreNetwork.sendFireBallista();
               }

               ballistaFireWasDown = fireDown;
            }

            if (vehicle instanceof CannonMobileEntity) {
               float pitchDelta = 0.0F;
               if (ForgeClientInputEvents.BALLISTA_UP.m_90857_()) {
                  pitchDelta++;
               }

               if (ForgeClientInputEvents.BALLISTA_DOWN.m_90857_()) {
                  pitchDelta--;
               }

               if (pitchDelta != 0.0F) {
                  AWCoreNetwork.sendAdjustBallistaTurret(pitchDelta, 0.0F);
               }
            } else if (vehicle instanceof BallistaMobileEntity) {
               float pitchDeltax = 0.0F;
               float yawDeltax = 0.0F;
               if (!(vehicle instanceof BallistaTurretStandEntity) && ForgeClientInputEvents.BALLISTA_UP.m_90857_()) {
                  pitchDeltax++;
               }

               if (!(vehicle instanceof BallistaTurretStandEntity) && ForgeClientInputEvents.BALLISTA_DOWN.m_90857_()) {
                  pitchDeltax--;
               }

               if (!(vehicle instanceof BallistaTurretStandEntity) && ForgeClientInputEvents.BALLISTA_LEFT.m_90857_()) {
                  yawDeltax--;
               }

               if (!(vehicle instanceof BallistaTurretStandEntity) && ForgeClientInputEvents.BALLISTA_RIGHT.m_90857_()) {
                  yawDeltax++;
               }

               if (pitchDeltax != 0.0F || yawDeltax != 0.0F) {
                  AWCoreNetwork.sendAdjustBallistaTurret(pitchDeltax, yawDeltax);
               }

               boolean fireDown = InputConstants.m_84830_(window, 32);
               if (fireDown && !ballistaFireWasDown) {
                  AWCoreNetwork.sendFireBallista();
               }

               ballistaFireWasDown = fireDown;
            } else if (!(vehicle instanceof CatapultMobileEntity) && !(vehicle instanceof CannonTurretStandEntity)) {
               ballistaFireWasDown = false;
            }
         }
      }
   }

   @SubscribeEvent
   public static void renderWorkBounds(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         Minecraft minecraft = Minecraft.m_91087_();
         if (minecraft.f_91073_ != null && minecraft.f_91074_ != null) {
            PoseStack poseStack = event.getPoseStack();
            Vec3 camera = event.getCamera().m_90583_();
            BufferSource bufferSource = minecraft.m_91269_().m_110104_();
            VertexConsumer consumer = bufferSource.m_6299_(RenderType.m_110504_());
            List<SurvivalStructureBuilderBlockEntity> structureBuilders = new ArrayList<>();
            poseStack.m_85836_();
            poseStack.m_85837_(-camera.f_82479_, -camera.f_82480_, -camera.f_82481_);
            int centerChunkX = minecraft.f_91074_.m_20183_().m_123341_() >> 4;
            int centerChunkZ = minecraft.f_91074_.m_20183_().m_123343_() >> 4;

            for (int chunkX = centerChunkX - 4; chunkX <= centerChunkX + 4; chunkX++) {
               for (int chunkZ = centerChunkZ - 4; chunkZ <= centerChunkZ + 4; chunkZ++) {
                  if (minecraft.f_91073_.m_7232_(chunkX, chunkZ)) {
                     LevelChunk chunk = minecraft.f_91073_.m_6325_(chunkX, chunkZ);

                     for (BlockEntity blockEntity : chunk.m_62954_().values()) {
                        if (blockEntity instanceof BoundedArea area) {
                           renderBounds(poseStack, consumer, area);
                        }

                        if (blockEntity instanceof SurvivalStructureBuilderBlockEntity builder) {
                           structureBuilders.add(builder);
                           renderStructureBuilderBounds(poseStack, consumer, builder);
                        }
                     }
                  }
               }
            }

            renderSelectedNpcBoxes(poseStack, consumer, minecraft);
            bufferSource.m_109912_(RenderType.m_110504_());
            Set<ForgeClientWorldEvents.PreviewKey> visibleGhosts = new HashSet<>();

            for (SurvivalStructureBuilderBlockEntity builder : structureBuilders) {
               renderStructureBuilderGhost(poseStack, bufferSource, minecraft, builder, visibleGhosts);
            }

            bufferSource.m_109912_(RenderType.m_110466_());
            GHOST_PREVIEW_CACHE.entrySet().removeIf(entry -> {
               if (visibleGhosts.contains(entry.getKey())) {
                  return false;
               } else {
                  entry.getValue().close();
                  return true;
               }
            });
            poseStack.m_85849_();
         }
      }
   }

   private static void renderStructureBuilderBounds(PoseStack poseStack, VertexConsumer lineConsumer, SurvivalStructureBuilderBlockEntity builder) {
      AwsTemplatePlacer.PreviewData preview = structurePreview(builder);
      if (preview != null) {
         AABB bounds = new AABB(
            (double)preview.min().m_123341_(),
            (double)preview.min().m_123342_(),
            (double)preview.min().m_123343_(),
            (double)preview.max().m_123341_() + 1.0,
            (double)preview.max().m_123342_() + 1.0,
            (double)preview.max().m_123343_() + 1.0
         );
         LevelRenderer.m_109646_(poseStack, lineConsumer, bounds, 1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static void renderStructureBuilderGhost(
      PoseStack poseStack,
      BufferSource bufferSource,
      Minecraft minecraft,
      SurvivalStructureBuilderBlockEntity builder,
      Set<ForgeClientWorldEvents.PreviewKey> visibleGhosts
   ) {
      AwsTemplatePlacer.PreviewData preview = structurePreview(builder);
      if (preview != null) {
         List<AwsTemplatePlacer.PreviewBlock> blocks = preview.blocks();
         if (blocks.size() > 2048) {
            String configured = builder.templateName();
            ForgeClientWorldEvents.PreviewKey key = new ForgeClientWorldEvents.PreviewKey(configured.substring(4), builder.buildOrigin(), builder.buildFacing());
            visibleGhosts.add(key);
            ForgeClientWorldEvents.GhostPreviewCache cache = GHOST_PREVIEW_CACHE.computeIfAbsent(
               key, ignored -> new ForgeClientWorldEvents.GhostPreviewCache(preview)
            );
            cache.buildNextBatch(minecraft);
            cache.render(poseStack);
         } else {
            MultiBufferSource ghostBuffers = ignored -> new ForgeClientWorldEvents.AlphaVertexConsumer(bufferSource.m_6299_(RenderType.m_110466_()), 0.32F);

            for (AwsTemplatePlacer.PreviewBlock previewBlock : blocks) {
               BlockPos pos = previewBlock.pos();
               BlockState state = previewBlock.state();
               if (builder.buildPhase() < 1 || minecraft.f_91073_.m_8055_(pos) != state) {
                  poseStack.m_85836_();
                  poseStack.m_252880_((float)pos.m_123341_(), (float)pos.m_123342_(), (float)pos.m_123343_());
                  minecraft.m_91289_().m_110912_(state, poseStack, ghostBuffers, 15728880, OverlayTexture.f_118083_);
                  poseStack.m_85849_();
               }
            }
         }
      }
   }

   private static AwsTemplatePlacer.PreviewData structurePreview(SurvivalStructureBuilderBlockEntity builder) {
      String configured = builder.templateName();
      if (builder.buildPhase() <= 2 && configured != null && configured.startsWith("aws:")) {
         String path = configured.substring(4);
         if (path.isBlank()) {
            return null;
         } else {
            ForgeClientWorldEvents.PreviewKey key = new ForgeClientWorldEvents.PreviewKey(path, builder.buildOrigin(), builder.buildFacing());

            try {
               AwsTemplatePlacer.PreviewData preview = PREVIEW_CACHE.get(key);
               if (preview == null) {
                  if (PREVIEW_CACHE.size() >= 64) {
                     PREVIEW_CACHE.clear();
                     GHOST_PREVIEW_CACHE.values().forEach(ForgeClientWorldEvents.GhostPreviewCache::close);
                     GHOST_PREVIEW_CACHE.clear();
                  }

                  preview = AwsTemplatePlacer.preview(path, key.origin(), key.facing());
                  PREVIEW_CACHE.put(key, preview);
               }

               return preview;
            } catch (RuntimeException var6) {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static void renderSelectedNpcBoxes(PoseStack poseStack, VertexConsumer consumer, Minecraft minecraft) {
      if (minecraft.f_91074_.m_21205_().m_41720_() instanceof CommandBatonItem || minecraft.f_91074_.m_21206_().m_41720_() instanceof CommandBatonItem) {
         ItemStack baton = minecraft.f_91074_.m_21205_().m_41720_() instanceof CommandBatonItem ? minecraft.f_91074_.m_21205_() : minecraft.f_91074_.m_21206_();
         AABB search = minecraft.f_91074_.m_20191_().m_82400_(128.0);

         for (PlayerOwnedNpcEntity npc : minecraft.f_91073_
            .m_6443_(
               PlayerOwnedNpcEntity.class,
               search,
               entity -> entity.m_6084_()
                     && entity.getOwner().map(owner -> owner.equals(minecraft.f_91074_.m_20148_())).orElse(true)
                     && CommandBatonItem.selectsNpc(baton, entity.m_20148_())
            )) {
            AABB box = npc.m_20191_().m_82400_(0.06);
            LevelRenderer.m_109646_(poseStack, consumer, box, 1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   private static void renderBounds(PoseStack poseStack, VertexConsumer consumer, BoundedArea area) {
      BlockPos min = area.getBoundsMin();
      BlockPos max = area.getBoundsMax();
      double maxY = (double)max.m_123342_() + (area instanceof AnimalFarmBlockEntity ? 3.0 : 1.0);
      AABB bounds = new AABB(
         (double)min.m_123341_(), (double)min.m_123342_(), (double)min.m_123343_(), (double)max.m_123341_() + 1.0, maxY, (double)max.m_123343_() + 1.0
      );
      LevelRenderer.m_109646_(poseStack, consumer, bounds, 1.0F, 1.0F, 1.0F, 0.9F);
   }

   private static final class AlphaVertexConsumer implements VertexConsumer {
      private final VertexConsumer delegate;
      private final int alpha;

      private AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
         this.delegate = delegate;
         this.alpha = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
      }

      public VertexConsumer m_5483_(double x, double y, double z) {
         this.delegate.m_5483_(x, y, z);
         return this;
      }

      public VertexConsumer m_6122_(int red, int green, int blue, int ignoredAlpha) {
         this.delegate.m_6122_(red, green, blue, this.alpha);
         return this;
      }

      public VertexConsumer m_7421_(float u, float v) {
         this.delegate.m_7421_(u, v);
         return this;
      }

      public VertexConsumer m_7122_(int u, int v) {
         this.delegate.m_7122_(u, v);
         return this;
      }

      public VertexConsumer m_7120_(int u, int v) {
         this.delegate.m_7120_(u, v);
         return this;
      }

      public VertexConsumer m_5601_(float x, float y, float z) {
         this.delegate.m_5601_(x, y, z);
         return this;
      }

      public void m_5752_() {
         this.delegate.m_5752_();
      }

      public void m_7404_(int red, int green, int blue, int ignoredAlpha) {
         this.delegate.m_7404_(red, green, blue, this.alpha);
      }

      public void m_141991_() {
         this.delegate.m_141991_();
      }
   }

   private static final class GhostPreviewCache implements AutoCloseable {
      private final AwsTemplatePlacer.PreviewData preview;
      private final List<VertexBuffer> batches = new ArrayList<>();
      private int nextBlock;

      private GhostPreviewCache(AwsTemplatePlacer.PreviewData preview) {
         this.preview = preview;
      }

      private void buildNextBatch(Minecraft minecraft) {
         if (this.nextBlock < this.preview.blocks().size()) {
            RenderType renderType = RenderType.m_110466_();
            BufferBuilder builder = new BufferBuilder(renderType.m_110507_());
            builder.m_166779_(renderType.m_173186_(), renderType.m_110508_());
            MultiBufferSource target = ignored -> new ForgeClientWorldEvents.AlphaVertexConsumer(builder, 0.32F);
            PoseStack blockPose = new PoseStack();
            BlockPos min = this.preview.min();

            for (int end = Math.min(this.nextBlock + 1536, this.preview.blocks().size()); this.nextBlock < end; this.nextBlock++) {
               AwsTemplatePlacer.PreviewBlock previewBlock = this.preview.blocks().get(this.nextBlock);
               BlockPos pos = previewBlock.pos();
               blockPose.m_85836_();
               blockPose.m_252880_(
                  (float)(pos.m_123341_() - min.m_123341_()), (float)(pos.m_123342_() - min.m_123342_()), (float)(pos.m_123343_() - min.m_123343_())
               );
               minecraft.m_91289_().m_110912_(previewBlock.state(), blockPose, target, 15728880, OverlayTexture.f_118083_);
               blockPose.m_85849_();
            }

            RenderedBuffer rendered = builder.m_231168_();
            if (rendered != null) {
               VertexBuffer vertexBuffer = new VertexBuffer(Usage.STATIC);
               vertexBuffer.m_85921_();
               vertexBuffer.m_231221_(rendered);
               VertexBuffer.m_85931_();
               this.batches.add(vertexBuffer);
            }
         }
      }

      private void render(PoseStack poseStack) {
         if (!this.batches.isEmpty()) {
            RenderType renderType = RenderType.m_110466_();
            poseStack.m_85836_();
            BlockPos min = this.preview.min();
            poseStack.m_252880_((float)min.m_123341_(), (float)min.m_123342_(), (float)min.m_123343_());
            renderType.m_110185_();

            for (VertexBuffer batch : this.batches) {
               batch.m_85921_();
               batch.m_253207_(poseStack.m_85850_().m_252922_(), RenderSystem.getProjectionMatrix(), GameRenderer.m_172649_());
            }

            VertexBuffer.m_85931_();
            renderType.m_110188_();
            poseStack.m_85849_();
         }
      }

      @Override
      public void close() {
         this.batches.forEach(VertexBuffer::close);
         this.batches.clear();
      }
   }

   private static record PreviewKey(String path, BlockPos origin, Direction facing) {
   }
}
