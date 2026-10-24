package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.tile.FlywheelStorageBlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class FlywheelStorageRenderer extends GeoBlockRenderer<FlywheelStorageBlockEntity> {
   public FlywheelStorageRenderer(Context context) {
      super(new FlywheelStorageGeoModel(false));
      this.addRenderLayer(new FlywheelStorageRenderer.WindowLayer(this));
   }

   public void preRender(
      PoseStack poseStack,
      FlywheelStorageBlockEntity animatable,
      BakedGeoModel model,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      if (!animatable.getStorageRenderState().visible()) {
         poseStack.scale(0.0F, 0.0F, 0.0F);
      }

      super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
   }

   private static class WindowLayer extends GeoRenderLayer<FlywheelStorageBlockEntity> {
      private final FlywheelStorageGeoModel windowModel = new FlywheelStorageGeoModel(true);

      private WindowLayer(FlywheelStorageRenderer renderer) {
         super(renderer);
      }

      public void render(
         PoseStack poseStack,
         FlywheelStorageBlockEntity animatable,
         BakedGeoModel bakedModel,
         RenderType renderType,
         MultiBufferSource bufferSource,
         VertexConsumer buffer,
         float partialTick,
         int packedLight,
         int packedOverlay
      ) {
         if (animatable.getStorageRenderState().visible()) {
            ResourceLocation texture = this.windowModel.getTextureResource(animatable);
            RenderType translucent = RenderType.entityTranslucent(texture);
            BakedGeoModel windows = this.windowModel.getBakedModel(this.windowModel.getModelResource(animatable));
            this.getRenderer()
               .reRender(
                  windows,
                  poseStack,
                  bufferSource,
                  animatable,
                  translucent,
                  bufferSource.getBuffer(translucent),
                  partialTick,
                  packedLight,
                  OverlayTexture.NO_OVERLAY,
                  1.0F,
                  1.0F,
                  1.0F,
                  1.0F
               );
         }
      }
   }
}
