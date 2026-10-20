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
      Player player = Minecraft.getInstance().player;
      if (player != null && player.isCreative()) {
         BlockState state = blockEntity.getBlockState();
         BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
         BakedModel model = blockRenderer.getBlockModel(state);
         VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
         blockRenderer.getModelRenderer().renderModel(poseStack.last(), consumer, state, model, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
      }
   }
}
