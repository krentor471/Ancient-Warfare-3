package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.entity.CatapultMobileEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

abstract class LegacySiegeGeoModel<T extends CatapultMobileEntity> extends GeoModel<T> {
   protected void applyWheelRotation(T animatable, float partialTick) {
      float wheelAngle = Mth.lerp(partialTick, animatable.wheelRotationPrev, animatable.wheelRotation);
      this.setRotationX("FRWheelPivot", wheelAngle);
      this.setRotationX("FLWheelPivot", wheelAngle);
      this.setRotationX("RRWheelPivot", wheelAngle);
      this.setRotationX("RLWheelPivot", wheelAngle);
      this.setRotationX("padleAxle", wheelAngle);
   }

   protected void applyTurretRotation(T animatable) {
      this.setRotationY("turretMidBeam", -animatable.getTurretYaw());
   }

   protected void applyCatapultAnimation(T animatable, float partialTick) {
      float elapsed = (float)(27 - animatable.getFireAnimationTicks()) + partialTick;
      float armAngle = 0.0F;
      float crankAngle = 0.0F;
      if (animatable.getFireAnimationTicks() > 0) {
         if (elapsed <= 5.0F) {
            float progress = Mth.clamp(elapsed / 5.0F, 0.0F, 1.0F);
            armAngle = Mth.lerp(progress, 0.0F, 70.0F);
         } else {
            float progress = Mth.clamp((elapsed - 5.0F) / 22.0F, 0.0F, 1.0F);
            armAngle = Mth.lerp(progress, 70.0F, 0.0F);
            crankAngle = progress * 360.0F;
         }
      }

      this.setRotationX("armMain", armAngle);
      this.setRotationX("pulleyAxle", crankAngle);
   }

   protected void applyTrebuchetAnimation(T animatable, float partialTick) {
      float releaseTicks = 8.0F;
      float reloadTicks = 19.0F;
      float elapsed = (float)(27 - animatable.getFireAnimationTicks()) + partialTick;
      float armAngle = -27.0F;
      float stringAngle = -64.0F;
      if (animatable.getFireAnimationTicks() > 0) {
         if (elapsed <= 8.0F) {
            float progress = Mth.clamp(elapsed / 8.0F, 0.0F, 1.0F);
            armAngle = Mth.lerp(progress, -27.0F, 90.0F);
            stringAngle = Mth.lerp(progress, -64.0F, 90.0F);
         } else {
            float progress = Mth.clamp((elapsed - 8.0F) / 19.0F, 0.0F, 1.0F);
            armAngle = Mth.lerp(progress, 90.0F, -27.0F);
            stringAngle = Mth.lerp(progress, 90.0F, -64.0F);
         }
      }

      this.setRotationX("trebuchetAxle", armAngle);
      this.setRotationX("trebuchetWeightAxle", -armAngle);
      this.setRotationX("trebuchetSlingString", stringAngle);
   }

   private void setRotationX(String boneName, float degrees) {
      CoreGeoBone bone = this.getAnimationProcessor().getBone(boneName);
      if (bone != null) {
         bone.setRotX(degrees * (float) (Math.PI / 180.0));
      }
   }

   private void setRotationY(String boneName, float degrees) {
      CoreGeoBone bone = this.getAnimationProcessor().getBone(boneName);
      if (bone != null) {
         bone.setRotY(degrees * (float) (Math.PI / 180.0));
      }
   }
}
