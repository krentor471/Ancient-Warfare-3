package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.tile.HandCrankedGeneratorBlockEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class HandCrankedGeneratorGeoModel extends GeoModel<HandCrankedGeneratorBlockEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/block/automation/hand_cranked_generator.geo.json");
   private static final ResourceLocation TEXTURE = AWCoreIds.id("textures/block/automation/hand_cranked_generator.png");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/block/automation/generator.animation.json");

   public ResourceLocation getModelResource(HandCrankedGeneratorBlockEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(HandCrankedGeneratorBlockEntity animatable) {
      return TEXTURE;
   }

   public ResourceLocation getAnimationResource(HandCrankedGeneratorBlockEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(HandCrankedGeneratorBlockEntity animatable, long instanceId, AnimationState<HandCrankedGeneratorBlockEntity> animationState) {
      super.handleAnimations(animatable, instanceId, animationState);
      float angle = animatable.getCrankActiveTicks() > 0 && animatable.getLevel() != null
         ? (float)Math.toRadians((double)(((float)animatable.getLevel().getGameTime() + animationState.getPartialTick()) * 30.0F))
         : 0.0F;
      CoreGeoBone inputGear = this.getAnimationProcessor().getBone("input_gear");
      if (inputGear != null) {
         inputGear.setRotY(-angle);
      }

      CoreGeoBone outputGear = this.getAnimationProcessor().getBone("output_gear");
      if (outputGear != null) {
         outputGear.setRotZ(-angle);
      }
   }
}
