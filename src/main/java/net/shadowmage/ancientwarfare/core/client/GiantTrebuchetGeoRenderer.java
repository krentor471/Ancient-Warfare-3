package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.entity.GiantTrebuchetEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GiantTrebuchetGeoRenderer extends GeoEntityRenderer<GiantTrebuchetEntity> {
   public GiantTrebuchetGeoRenderer(Context context) {
      super(context, new GiantTrebuchetGeoModel());
      this.shadowRadius = 2.0F;
   }

   public void preRender(
      PoseStack poseStack,
      GiantTrebuchetEntity animatable,
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
      poseStack.scale(2.5F, 2.5F, 2.5F);
      poseStack.translate(0.0, -1.45, 0.0);
      super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
   }

   protected void applyRotations(GiantTrebuchetEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot())));
   }

   public Color getRenderColor(GiantTrebuchetEntity animatable, float partialTick, int packedLight) {
      return animatable.getHurtFlashTicks() > 0 ? Color.ofRGBA(255, 80, 80, 255) : Color.WHITE;
   }
}
