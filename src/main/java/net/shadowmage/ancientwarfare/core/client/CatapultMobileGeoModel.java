package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.CatapultMobileEntity;
import software.bernie.geckolib.core.animation.AnimationState;

public class CatapultMobileGeoModel extends LegacySiegeGeoModel<CatapultMobileEntity> {
   private static final ResourceLocation FIXED_MODEL = AWCoreIds.id("geo/entity/vehicle/catapult_mobile_fixed.geo.json");
   private static final ResourceLocation TURRET_MODEL = AWCoreIds.id("geo/entity/vehicle/catapult_mobile_turret.geo.json");
   private static final ResourceLocation FIXED_ANIMATION = AWCoreIds.id("animations/entity/vehicle/catapult_mobile_fixed.animation.json");
   private static final ResourceLocation TURRET_ANIMATION = AWCoreIds.id("animations/entity/vehicle/catapult_mobile_turret.animation.json");
   private static final ResourceLocation[] FIXED_TEXTURES = textures("catapult_mobile_fixed");
   private static final ResourceLocation[] TURRET_TEXTURES = textures("catapult_mobile_turret");

   public ResourceLocation getModelResource(CatapultMobileEntity animatable) {
      return animatable.hasFixedTurret() ? FIXED_MODEL : TURRET_MODEL;
   }

   public ResourceLocation getTextureResource(CatapultMobileEntity animatable) {
      return (animatable.hasFixedTurret() ? FIXED_TEXTURES : TURRET_TEXTURES)[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(CatapultMobileEntity animatable) {
      return animatable.hasFixedTurret() ? FIXED_ANIMATION : TURRET_ANIMATION;
   }

   public void handleAnimations(CatapultMobileEntity animatable, long instanceId, AnimationState<CatapultMobileEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
      this.applyWheelRotation(animatable, state.getPartialTick());
      this.applyTurretRotation(animatable);
      this.applyCatapultAnimation(animatable, state.getPartialTick());
   }

   private static ResourceLocation[] textures(String name) {
      String base = "textures/entity/vehicle/" + name + "/" + name;
      return new ResourceLocation[]{
         AWCoreIds.id(base + ".png"),
         AWCoreIds.id(base + "_iron_band_1.png"),
         AWCoreIds.id(base + "_iron_band_2.png"),
         AWCoreIds.id(base + "_iron.png"),
         AWCoreIds.id(base + "_steel.png")
      };
   }
}
