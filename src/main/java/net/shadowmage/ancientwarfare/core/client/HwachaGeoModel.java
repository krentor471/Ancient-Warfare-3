package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.HwachaEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class HwachaGeoModel extends GeoModel<HwachaEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/hwacha.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/hwacha_legacy.animation.json");
   private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
      AWCoreIds.id("textures/entity/vehicle/hwacha/hwacha.png"),
      AWCoreIds.id("textures/entity/vehicle/hwacha/hwacha_iron_band_1.png"),
      AWCoreIds.id("textures/entity/vehicle/hwacha/hwacha_iron_band_2.png"),
      AWCoreIds.id("textures/entity/vehicle/hwacha/hwacha_iron.png"),
      AWCoreIds.id("textures/entity/vehicle/hwacha/hwacha_steel.png")
   };

   public ResourceLocation getModelResource(HwachaEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(HwachaEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(HwachaEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(HwachaEntity animatable, long instanceId, AnimationState<HwachaEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
      float partialTick = state.getPartialTick();
      float wheel = Mth.m_14179_(partialTick, animatable.wheelRotationPrev, animatable.wheelRotation);
      this.setRotX("FRWheelPivot", wheel);
      this.setRotX("FLWheelPivot", wheel);
      float pitch = animatable.m_20184_().m_165925_() > 4.0E-4 ? 8.0F : 39.0F;
      this.setRotX("frontAxle", pitch);
   }

   private void setRotX(String name, float degrees) {
      CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
      if (bone != null) {
         bone.setRotX(degrees * (float) (Math.PI / 180.0));
      }
   }
}
