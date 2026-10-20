package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.CatapultTurretStandEntity;
import software.bernie.geckolib.core.animation.AnimationState;

public class CatapultTurretStandGeoModel extends LegacySiegeGeoModel<CatapultTurretStandEntity> {
   private static final ResourceLocation FIXED_MODEL = AWCoreIds.id("geo/entity/vehicle/catapult_stand_fixed.geo.json");
   private static final ResourceLocation TURRET_MODEL = AWCoreIds.id("geo/entity/vehicle/catapult_stand_turret.geo.json");
   private static final ResourceLocation FIXED_ANIMATION = AWCoreIds.id("animations/entity/vehicle/catapult_stand_fixed.animation.json");
   private static final ResourceLocation TURRET_ANIMATION = AWCoreIds.id("animations/entity/vehicle/catapult_stand_turret.animation.json");
   private static final ResourceLocation[] FIXED_TEXTURES = textures("catapult_stand_fixed");
   private static final ResourceLocation[] TURRET_TEXTURES = textures("catapult_stand_turret");

   public ResourceLocation getModelResource(CatapultTurretStandEntity animatable) {
      return animatable.hasFixedTurret() ? FIXED_MODEL : TURRET_MODEL;
   }

   public ResourceLocation getTextureResource(CatapultTurretStandEntity animatable) {
      return (animatable.hasFixedTurret() ? FIXED_TEXTURES : TURRET_TEXTURES)[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(CatapultTurretStandEntity animatable) {
      return animatable.hasFixedTurret() ? FIXED_ANIMATION : TURRET_ANIMATION;
   }

   public void handleAnimations(CatapultTurretStandEntity animatable, long instanceId, AnimationState<CatapultTurretStandEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
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
