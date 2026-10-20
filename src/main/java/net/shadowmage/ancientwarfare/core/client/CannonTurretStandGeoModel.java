package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.CannonTurretStandEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class CannonTurretStandGeoModel extends GeoModel<CannonTurretStandEntity> {
   private static final ResourceLocation FIXED_MODEL = AWCoreIds.id("geo/entity/vehicle/cannon_turret_stand_fixed.geo.json");
   private static final ResourceLocation TURRET_MODEL = AWCoreIds.id("geo/entity/vehicle/cannon_turret_stand_turret.geo.json");
   private static final ResourceLocation FIXED_ANIMATION = AWCoreIds.id("animations/entity/vehicle/cannon_turret_stand_fixed.animation.json");
   private static final ResourceLocation TURRET_ANIMATION = AWCoreIds.id("animations/entity/vehicle/cannon_turret_stand_turret.animation.json");
   private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
      AWCoreIds.id("textures/entity/vehicle/cannon_turret_stand/cannon_turret_stand.png"),
      AWCoreIds.id("textures/entity/vehicle/cannon_turret_stand/cannon_turret_stand_iron_band_1.png"),
      AWCoreIds.id("textures/entity/vehicle/cannon_turret_stand/cannon_turret_stand_iron_band_2.png"),
      AWCoreIds.id("textures/entity/vehicle/cannon_turret_stand/cannon_turret_stand_iron.png"),
      AWCoreIds.id("textures/entity/vehicle/cannon_turret_stand/cannon_turret_stand_steel.png")
   };

   public ResourceLocation getModelResource(CannonTurretStandEntity animatable) {
      return animatable.hasFixedTurret() ? FIXED_MODEL : TURRET_MODEL;
   }

   public ResourceLocation getTextureResource(CannonTurretStandEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(CannonTurretStandEntity animatable) {
      return animatable.hasFixedTurret() ? FIXED_ANIMATION : TURRET_ANIMATION;
   }

   public void handleAnimations(CannonTurretStandEntity animatable, long instanceId, AnimationState<CannonTurretStandEntity> animationState) {
      super.handleAnimations(animatable, instanceId, animationState);
      CoreGeoBone rotatingBeam = this.getAnimationProcessor().getBone("cannonBaseMid");
      if (rotatingBeam != null && !animatable.hasFixedTurret()) {
         rotatingBeam.setRotY((float)Math.toRadians((double)(180.0F - animatable.getTurretYaw())));
      }

      CoreGeoBone barrel = this.getAnimationProcessor().getBone("axleRight");
      if (barrel != null) {
         barrel.setRotX((float)Math.toRadians((double)(-animatable.getTurretPitch())));
      }
   }
}
