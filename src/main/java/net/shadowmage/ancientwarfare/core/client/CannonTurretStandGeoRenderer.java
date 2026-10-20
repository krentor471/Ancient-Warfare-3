package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.entity.CannonTurretStandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CannonTurretStandGeoRenderer extends GeoEntityRenderer<CannonTurretStandEntity> {
   public CannonTurretStandGeoRenderer(Context context) {
      super(context, new CannonTurretStandGeoModel());
      this.f_114477_ = 0.8F;
   }

   public void preRender(
      PoseStack poseStack,
      CannonTurretStandEntity animatable,
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
      poseStack.m_85837_(0.0, -1.45, 0.0);
      super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
   }

   protected void applyRotations(CannonTurretStandEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
      poseStack.m_252781_(Axis.f_252436_.m_252977_(180.0F - Mth.m_14179_(partialTick, animatable.f_19859_, animatable.m_146908_())));
   }

   public Color getRenderColor(CannonTurretStandEntity animatable, float partialTick, int packedLight) {
      return animatable.getHurtFlashTicks() > 0 ? Color.ofRGBA(255, 80, 80, 255) : Color.WHITE;
   }
}
