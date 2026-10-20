package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.StructureContainerBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.StructureContainerBlockEntity;

public final class LegacyCoffinRenderer implements BlockEntityRenderer<StructureContainerBlockEntity> {
   private static final ResourceLocation WOOD_TEXTURE = new ResourceLocation("ancientwarfare", "textures/block/structure/coffin_oak.png");
   private static final ResourceLocation STONE_TEXTURE = new ResourceLocation("ancientwarfare", "textures/block/structure/stone_coffin_stone.png");
   private final LegacyCoffinModel woodenModel;
   private final LegacyCoffinModel stoneModel;

   public LegacyCoffinRenderer(Context context) {
      this.woodenModel = new LegacyCoffinModel(context.bakeLayer(LegacyCoffinModel.WOOD_LAYER), true);
      this.stoneModel = new LegacyCoffinModel(context.bakeLayer(LegacyCoffinModel.STONE_LAYER), false);
   }

   public void render(
      StructureContainerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay
   ) {
      BlockState state = blockEntity.getBlockState();
      boolean wooden = state.is(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN));
      boolean stone = state.is(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN));
      if (wooden || stone) {
         Direction facing = state.hasProperty(StructureContainerBlock.FACING) ? (Direction)state.getValue(StructureContainerBlock.FACING) : Direction.NORTH;
         float open = blockEntity.getLidProgress(partialTick);
         VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(wooden ? WOOD_TEXTURE : STONE_TEXTURE));
         poseStack.pushPose();
         if (wooden) {
            poseStack.translate(0.5, 2.16, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));
            poseStack.translate(0.0, 0.0, -0.22);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.scale(1.44F, 1.44F, 1.44F);
            this.woodenModel.render(poseStack, consumer, packedLight, packedOverlay, open);
         } else {
            poseStack.translate(0.0, 1.78, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));
            switch (facing) {
               case SOUTH:
                  poseStack.translate(-1.0, 0.0, -2.0);
                  break;
               case EAST:
                  poseStack.translate(0.0, 0.0, -2.0);
                  break;
               case WEST:
                  poseStack.translate(-1.0, 0.0, -1.0);
                  break;
               default:
                  poseStack.translate(0.0, 0.0, -1.0);
            }

            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.scale(1.184F, 1.184F, 1.184F);
            this.stoneModel.render(poseStack, consumer, packedLight, packedOverlay, open);
         }

         poseStack.popPose();
      }
   }

   private static float yawFor(Direction facing) {
      return switch (facing) {
         case SOUTH -> -180.0F;
         case EAST -> -90.0F;
         case WEST -> -270.0F;
         default -> 0.0F;
      };
   }

   public int getViewDistance() {
      return Mth.ceil(64.0F);
   }
}
