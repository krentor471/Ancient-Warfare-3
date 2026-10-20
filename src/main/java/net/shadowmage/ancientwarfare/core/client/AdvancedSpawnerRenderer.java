package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.tile.AdvancedSpawnerBlockEntity;

public class AdvancedSpawnerRenderer implements BlockEntityRenderer<AdvancedSpawnerBlockEntity> {
   public AdvancedSpawnerRenderer(Context context) {
   }

   public void render(
      AdvancedSpawnerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      Player player = Minecraft.m_91087_().f_91074_;
      if (player != null && player.m_7500_()) {
         BlockState state = blockEntity.m_58900_();
         BlockRenderDispatcher blockRenderer = Minecraft.m_91087_().m_91289_();
         BakedModel model = blockRenderer.m_110910_(state);
         VertexConsumer consumer = bufferSource.m_6299_(RenderType.m_110463_());
         blockRenderer.m_110937_().m_111067_(poseStack.m_85850_(), consumer, state, model, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
      }
   }
}
