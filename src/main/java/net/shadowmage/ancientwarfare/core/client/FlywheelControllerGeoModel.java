package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.block.FlywheelControllerBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import net.shadowmage.ancientwarfare.core.tile.FlywheelControllerBlockEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class FlywheelControllerGeoModel extends GeoModel<FlywheelControllerBlockEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/block/automation/flywheel_controller_restored.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/block/automation/flywheel_controller_restored.animation.json");

   public ResourceLocation getModelResource(FlywheelControllerBlockEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(FlywheelControllerBlockEntity animatable) {
      TorqueTier tier = animatable.m_58900_().m_60734_() instanceof FlywheelControllerBlock block ? block.getTier() : TorqueTier.LIGHT;
      return AWCoreIds.id("textures/block/automation/flywheel_controller_" + tier.getId() + ".png");
   }

   public ResourceLocation getAnimationResource(FlywheelControllerBlockEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(FlywheelControllerBlockEntity animatable, long instanceId, AnimationState<FlywheelControllerBlockEntity> animationState) {
      super.handleAnimations(animatable, instanceId, animationState);
      if (animatable.m_58904_() != null) {
         float angle = animatable.isVisualActive()
            ? (float)Math.toRadians((double)(((float)animatable.m_58904_().m_46467_() + animationState.getPartialTick()) * 9.0F))
            : 0.0F;
         this.setRotZ("inputGear", angle);
         this.setRotZ("outputGear", angle);
         CoreGeoBone spindle = this.getAnimationProcessor().getBone("spindle");
         if (spindle != null) {
            spindle.setRotY(angle);
         }
      }
   }

   private void setRotZ(String boneName, float angle) {
      CoreGeoBone bone = this.getAnimationProcessor().getBone(boneName);
      if (bone != null) {
         bone.setRotZ(angle);
      }
   }
}
