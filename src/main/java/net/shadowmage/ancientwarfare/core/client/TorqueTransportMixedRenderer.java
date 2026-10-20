package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.shadowmage.ancientwarfare.core.block.TorqueTransportBlock;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTransportKind;
import net.shadowmage.ancientwarfare.core.tile.TorqueTransportBlockEntity;

public final class TorqueTransportMixedRenderer implements BlockEntityRenderer<TorqueTransportBlockEntity> {
   private final TorqueTransportRenderer legacy;
   private final TorqueTransportGeoRenderer geo;

   public TorqueTransportMixedRenderer(Context context) {
      this.legacy = new TorqueTransportRenderer(context);
      this.geo = new TorqueTransportGeoRenderer();
   }

   public void render(
      TorqueTransportBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      if (blockEntity.m_58900_().m_60734_() instanceof TorqueTransportBlock block && block.getKind() == TorqueTransportKind.SHAFT) {
         this.geo.m_6922_(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
         return;
      }

      if (blockEntity.m_58900_().m_60734_() instanceof TorqueTransportBlock block
         && (block.getKind() == TorqueTransportKind.JUNCTION || block.getKind() == TorqueTransportKind.DISTRIBUTOR)) {
         this.geo.m_6922_(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
         return;
      }

      this.legacy.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
   }

   public int m_142163_() {
      return Math.max(this.legacy.m_142163_(), this.geo.m_142163_());
   }
}
