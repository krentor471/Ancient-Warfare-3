package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.BatteringRamEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class BatteringRamGeoModel extends GeoModel<BatteringRamEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/battering_ram.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/battering_ram_legacy.animation.json");
   private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
      AWCoreIds.id("textures/entity/vehicle/battering_ram/battering_ram.png"),
      AWCoreIds.id("textures/entity/vehicle/battering_ram/battering_ram_iron_band_1.png"),
      AWCoreIds.id("textures/entity/vehicle/battering_ram/battering_ram_iron_band_2.png"),
      AWCoreIds.id("textures/entity/vehicle/battering_ram/battering_ram_iron.png"),
      AWCoreIds.id("textures/entity/vehicle/battering_ram/battering_ram_steel.png")
   };

   public ResourceLocation getModelResource(BatteringRamEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(BatteringRamEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(BatteringRamEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(BatteringRamEntity animatable, long instanceId, AnimationState<BatteringRamEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
      float wheel = Mth.lerp(state.getPartialTick(), animatable.wheelRotationPrev, animatable.wheelRotation);
      this.setRotX("FRWheelPivot", wheel);
      this.setRotX("FLWheelPivot", wheel);
      this.setRotX("RRWheelPivot", wheel);
      this.setRotX("RLWheelPivot", wheel);
      float log = 0.0F;
      int ticks = animatable.getAttackAnimationTicks();
      if (ticks > 0) {
         float elapsed = 16.0F - (float)ticks + state.getPartialTick();
         float progress = Mth.clamp(elapsed / 16.0F, 0.0F, 1.0F);
         if (progress < 0.34F) {
            log = Mth.lerp(progress / 0.34F, 0.0F, 30.0F);
         } else if (progress < 0.68F) {
            log = Mth.lerp((progress - 0.34F) / 0.34F, 30.0F, -30.0F);
         } else {
            log = Mth.lerp((progress - 0.68F) / 0.32F, -30.0F, 0.0F);
         }
      }

      this.setRotX("ramLog", -log);
      this.setRotX("ramRopeFL", log);
      this.setRotX("ramRopeFR", log);
      this.setRotX("ramRopeRR", log);
      this.setRotX("ramRopeRL", log);
   }

   private void setRotX(String name, float degrees) {
      CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
      if (bone != null) {
         bone.setRotX(degrees * (float) (Math.PI / 180.0));
      }
   }
}
