package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.TrebuchetFixedEntity;
import software.bernie.geckolib.core.animation.AnimationState;

public class TrebuchetFixedGeoModel extends LegacySiegeGeoModel<TrebuchetFixedEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/trebuchet_fixed.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/trebuchet_fixed.animation.json");
   private static final ResourceLocation[] TEXTURES = textures("trebuchet_fixed");

   public ResourceLocation getModelResource(TrebuchetFixedEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(TrebuchetFixedEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(TrebuchetFixedEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(TrebuchetFixedEntity animatable, long instanceId, AnimationState<TrebuchetFixedEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
      this.applyTrebuchetAnimation(animatable, state.getPartialTick());
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
