package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.TrebuchetTurretEntity;
import software.bernie.geckolib.core.animation.AnimationState;

public class TrebuchetTurretGeoModel extends LegacySiegeGeoModel<TrebuchetTurretEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/trebuchet_turret.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/trebuchet_turret.animation.json");
   private static final ResourceLocation[] TEXTURES = textures("trebuchet_turret");

   public ResourceLocation getModelResource(TrebuchetTurretEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(TrebuchetTurretEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(TrebuchetTurretEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(TrebuchetTurretEntity animatable, long instanceId, AnimationState<TrebuchetTurretEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
      this.applyTurretRotation(animatable);
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
