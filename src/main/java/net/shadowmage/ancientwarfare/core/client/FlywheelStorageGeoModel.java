package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.tile.FlywheelStorageBlockEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class FlywheelStorageGeoModel extends GeoModel<FlywheelStorageBlockEntity> {
   private static final ResourceLocation EMPTY_ANIMATION = AWCoreIds.id("animations/block/automation/flywheel_storage_restored.animation.json");
   private final boolean windows;

   public FlywheelStorageGeoModel(boolean windows) {
      this.windows = windows;
   }

   public ResourceLocation getModelResource(FlywheelStorageBlockEntity animatable) {
      FlywheelStorageBlockEntity.StorageRenderState state = animatable.getStorageRenderState();
      String size = state.size().name().toLowerCase();
      String layer = state.layer().name().toLowerCase();
      return AWCoreIds.id("geo/block/automation/flywheel_storage_" + size + "_" + layer + (this.windows ? "_windows.geo.json" : "_solid.geo.json"));
   }

   public ResourceLocation getTextureResource(FlywheelStorageBlockEntity animatable) {
      String size = animatable.getStorageRenderState().size().name().toLowerCase();
      return AWCoreIds.id("textures/block/automation/flywheel_storage_" + size + "_" + animatable.getTier().getId() + ".png");
   }

   public ResourceLocation getAnimationResource(FlywheelStorageBlockEntity animatable) {
      return EMPTY_ANIMATION;
   }

   public RenderType getRenderType(FlywheelStorageBlockEntity animatable, ResourceLocation texture) {
      return this.windows ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture);
   }

   public void handleAnimations(FlywheelStorageBlockEntity animatable, long instanceId, AnimationState<FlywheelStorageBlockEntity> animationState) {
      super.handleAnimations(animatable, instanceId, animationState);
      if (!this.windows && animatable.getLevel() != null) {
         CoreGeoBone spindle = this.getAnimationProcessor().getBone("spindle");
         if (spindle != null) {
            float ticks = (float)animatable.getLevel().getGameTime() + animationState.getPartialTick();
            spindle.setRotY((float)Math.toRadians((double)(ticks * 7.5F)));
         }
      }
   }
}
