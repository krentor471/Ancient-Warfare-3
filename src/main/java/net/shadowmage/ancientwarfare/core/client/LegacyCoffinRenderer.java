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
      this.woodenModel = new LegacyCoffinModel(context.m_173582_(LegacyCoffinModel.WOOD_LAYER), true);
      this.stoneModel = new LegacyCoffinModel(context.m_173582_(LegacyCoffinModel.STONE_LAYER), false);
   }

   public void render(
      StructureContainerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay
   ) {
      BlockState state = blockEntity.m_58900_();
      boolean wooden = state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN));
      boolean stone = state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN));
      if (wooden || stone) {
         Direction facing = state.m_61138_(StructureContainerBlock.FACING) ? (Direction)state.m_61143_(StructureContainerBlock.FACING) : Direction.NORTH;
         float open = blockEntity.getLidProgress(partialTick);
         VertexConsumer consumer = buffers.m_6299_(RenderType.m_110458_(wooden ? WOOD_TEXTURE : STONE_TEXTURE));
         poseStack.m_85836_();
         if (wooden) {
            poseStack.m_85837_(0.5, 2.16, 0.5);
            poseStack.m_252781_(Axis.f_252436_.m_252977_(yawFor(facing)));
            poseStack.m_85837_(0.0, 0.0, -0.22);
            poseStack.m_252781_(Axis.f_252529_.m_252977_(180.0F));
            poseStack.m_85841_(1.44F, 1.44F, 1.44F);
            this.woodenModel.render(poseStack, consumer, packedLight, packedOverlay, open);
         } else {
            poseStack.m_85837_(0.0, 1.78, 0.0);
            poseStack.m_252781_(Axis.f_252436_.m_252977_(yawFor(facing)));
            switch (facing) {
               case SOUTH:
                  poseStack.m_85837_(-1.0, 0.0, -2.0);
                  break;
               case EAST:
                  poseStack.m_85837_(0.0, 0.0, -2.0);
                  break;
               case WEST:
                  poseStack.m_85837_(-1.0, 0.0, -1.0);
                  break;
               default:
                  poseStack.m_85837_(0.0, 0.0, -1.0);
            }

            poseStack.m_252781_(Axis.f_252529_.m_252977_(180.0F));
            poseStack.m_85841_(1.184F, 1.184F, 1.184F);
            this.stoneModel.render(poseStack, consumer, packedLight, packedOverlay, open);
         }

         poseStack.m_85849_();
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

   public int m_142163_() {
      return Mth.m_14167_(64.0F);
   }
}
