package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.TrebuchetMobileEntity;
import software.bernie.geckolib.core.animation.AnimationState;

public class TrebuchetMobileGeoModel extends LegacySiegeGeoModel<TrebuchetMobileEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/trebuchet_mobile.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/trebuchet_mobile.animation.json");
   private static final ResourceLocation[] TEXTURES = textures("trebuchet_mobile");

   public ResourceLocation getModelResource(TrebuchetMobileEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(TrebuchetMobileEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(TrebuchetMobileEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(TrebuchetMobileEntity animatable, long instanceId, AnimationState<TrebuchetMobileEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
      this.applyWheelRotation(animatable, state.getPartialTick());
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
