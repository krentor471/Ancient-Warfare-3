package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ChestPushcartGeoModel extends GeoModel<ChestPushcartEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/chest_pushcart.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/chest_pushcart_legacy.animation.json");
   private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/chest_pushcart.png"),
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/chest_pushcart_iron_band_1.png"),
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/chest_pushcart_iron_band_2.png"),
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/chest_pushcart_iron_body.png"),
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/chest_pushcart_steel_body.png")
   };

   public ResourceLocation getModelResource(ChestPushcartEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(ChestPushcartEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(ChestPushcartEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(ChestPushcartEntity animatable, long instanceId, AnimationState<ChestPushcartEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
      float wheel = Mth.lerp(state.getPartialTick(), animatable.wheelRotationPrev, animatable.wheelRotation);
      this.setRotX("FRWheelPivot", wheel);
      this.setRotX("FLWheelPivot", wheel);
      this.setRotX("RRWheelPivot", wheel);
      this.setRotX("RLWheelPivot", wheel);
      float hitch = animatable.getDeltaMovement().horizontalDistanceSqr() > 4.0E-4 ? 0.0F : 25.0F;
      float delta = hitch - 25.0F;
      this.setRotX("cartHitchLeft", delta);
      this.setRotX("cartHitchRight", delta);
      this.setRotX("cartHitchFront", delta);
   }

   private void setRotX(String name, float degrees) {
      CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
      if (bone != null) {
         bone.setRotX(degrees * (float) (Math.PI / 180.0));
      }
   }
}
