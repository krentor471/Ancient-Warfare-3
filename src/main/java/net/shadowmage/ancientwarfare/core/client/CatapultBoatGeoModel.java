package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.CatapultBoatEntity;
import software.bernie.geckolib.core.animation.AnimationState;

public class CatapultBoatGeoModel extends LegacySiegeGeoModel<CatapultBoatEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/catapult_boat.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/catapult_boat.animation.json");
   private static final ResourceLocation[] TEXTURES = textures("catapult_boat");

   public ResourceLocation getModelResource(CatapultBoatEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(CatapultBoatEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(CatapultBoatEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(CatapultBoatEntity animatable, long instanceId, AnimationState<CatapultBoatEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
      this.applyWheelRotation(animatable, state.getPartialTick());
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
