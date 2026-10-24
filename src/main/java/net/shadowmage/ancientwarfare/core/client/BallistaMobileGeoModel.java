package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.BallistaMobileEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class BallistaMobileGeoModel extends GeoModel<BallistaMobileEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/ballista_mobile.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/ballista_mobile.animation.json");
   private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
      AWCoreIds.id("textures/entity/vehicle/ballista_mobile/ballista_mobile.png"),
      AWCoreIds.id("textures/entity/vehicle/ballista_mobile/ballista_mobile_iron_band_1.png"),
      AWCoreIds.id("textures/entity/vehicle/ballista_mobile/ballista_mobile_iron_band_2.png"),
      AWCoreIds.id("textures/entity/vehicle/ballista_mobile/ballista_mobile_iron.png"),
      AWCoreIds.id("textures/entity/vehicle/ballista_mobile/ballista_mobile_steel.png")
   };

   public ResourceLocation getModelResource(BallistaMobileEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(BallistaMobileEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(BallistaMobileEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(BallistaMobileEntity animatable, long instanceId, AnimationState<BallistaMobileEntity> animationState) {
      super.handleAnimations(animatable, instanceId, animationState);
      float partialTick = animationState.getPartialTick();
      float wheelAngle = Mth.lerp(partialTick, animatable.wheelRotationPrev, animatable.wheelRotation);
      this.setRotationX("FRWheelPivot", wheelAngle);
      this.setRotationX("FLWheelPivot", wheelAngle);
      this.setRotationX("RRWheelPivot", wheelAngle);
      this.setRotationX("RLWheelPivot", wheelAngle);
      CoreGeoBone turret = this.getAnimationProcessor().getBone("armMain");
      if (turret != null) {
         turret.setRotX(degreesToRadians(animatable.getTurretPitch()));
         turret.setRotY(degreesToRadians(-animatable.getTurretYaw()));
      }

      int animationTicks = animatable.getFireAnimationTicks();
      float bowAngle = 67.5F;
      if (animationTicks > 25) {
         float releaseProgress = Mth.clamp(((float)(30 - animationTicks) + partialTick) / 5.0F, 0.0F, 1.0F);
         bowAngle = Mth.lerp(releaseProgress, 67.5F, 30.0F);
      } else if (animationTicks > 0) {
         float reloadProgress = Mth.clamp(((float)(25 - animationTicks) + partialTick) / 25.0F, 0.0F, 1.0F);
         bowAngle = Mth.lerp(reloadProgress, 30.0F, 67.5F);
      }

      float stringAngle = getLegacyStringAngle(bowAngle);
      this.setRotationY("armLeftMain", bowAngle);
      this.setRotationY("armRightMain", -bowAngle);
      this.setRotationY("stringLeft", stringAngle);
      this.setRotationY("stringRight", -stringAngle);
      this.setRotationX("trigger1", animationTicks > 0 ? 0.0F : -70.0F);
      float crankAngle = animatable.getCrankAngle();
      if (animationTicks > 0 && animationTicks <= 25) {
         crankAngle -= 4.0F * partialTick;
      }

      this.setRotationX("crankAxle", crankAngle);
   }

   private void setRotationX(String boneName, float degrees) {
      CoreGeoBone bone = this.getAnimationProcessor().getBone(boneName);
      if (bone != null) {
         bone.setRotX(degreesToRadians(degrees));
      }
   }

   private void setRotationY(String boneName, float degrees) {
      CoreGeoBone bone = this.getAnimationProcessor().getBone(boneName);
      if (bone != null) {
         bone.setRotY(degreesToRadians(degrees));
      }
   }

   private static float degreesToRadians(float degrees) {
      return degrees * (float) (Math.PI / 180.0);
   }

   private static float getLegacyStringAngle(float bowAngle) {
      float percentTravel = (bowAngle - 30.0F) / 37.5F;
      return -30.0F - percentTravel * 1.305F * bowAngle;
   }
}
