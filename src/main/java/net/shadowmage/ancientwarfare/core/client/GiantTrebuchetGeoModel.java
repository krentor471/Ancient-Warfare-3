package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.GiantTrebuchetEntity;
import software.bernie.geckolib.core.animation.AnimationState;

public class GiantTrebuchetGeoModel extends LegacySiegeGeoModel<GiantTrebuchetEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/trebuchet_fixed.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/trebuchet_fixed.animation.json");
   private static final ResourceLocation[] TEXTURES = textures("giant_trebuchet");

   public ResourceLocation getModelResource(GiantTrebuchetEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(GiantTrebuchetEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(GiantTrebuchetEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(GiantTrebuchetEntity animatable, long instanceId, AnimationState<GiantTrebuchetEntity> state) {
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
