package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.CannonMobileEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class CannonMobileGeoModel extends GeoModel<CannonMobileEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/cannon_mobile.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/cannon_mobile.animation.json");
   private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
      AWCoreIds.id("textures/entity/vehicle/cannon_mobile/cannon_mobile.png"),
      AWCoreIds.id("textures/entity/vehicle/cannon_mobile/cannon_mobile_iron_band_1.png"),
      AWCoreIds.id("textures/entity/vehicle/cannon_mobile/cannon_mobile_iron_band_2.png"),
      AWCoreIds.id("textures/entity/vehicle/cannon_mobile/cannon_mobile_iron.png"),
      AWCoreIds.id("textures/entity/vehicle/cannon_mobile/cannon_mobile_steel.png")
   };

   public ResourceLocation getModelResource(CannonMobileEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(CannonMobileEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(CannonMobileEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(CannonMobileEntity animatable, long instanceId, AnimationState<CannonMobileEntity> animationState) {
      super.handleAnimations(animatable, instanceId, animationState);
      CoreGeoBone cannonAxle = this.getAnimationProcessor().getBone("axleRight");
      if (cannonAxle != null) {
         cannonAxle.setRotX((float)Math.toRadians((double)(-animatable.getTurretPitch())));
      }
   }
}
