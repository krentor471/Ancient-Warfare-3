package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.TransportBoatEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class TransportBoatGeoModel extends GeoModel<TransportBoatEntity> {
   private static final ResourceLocation MODEL = AWCoreIds.id("geo/entity/vehicle/transport_boat.geo.json");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/entity/vehicle/transport_boat_legacy.animation.json");
   private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
      AWCoreIds.id("textures/entity/vehicle/transport_boat/transport_boat.png"),
      AWCoreIds.id("textures/entity/vehicle/transport_boat/transport_boat_iron_band_1.png"),
      AWCoreIds.id("textures/entity/vehicle/transport_boat/transport_boat_iron_band_2.png"),
      AWCoreIds.id("textures/entity/vehicle/transport_boat/transport_boat_iron.png"),
      AWCoreIds.id("textures/entity/vehicle/transport_boat/transport_boat_steel.png")
   };

   public ResourceLocation getModelResource(TransportBoatEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(TransportBoatEntity animatable) {
      return TEXTURES[animatable.getMaterialLevel()];
   }

   public ResourceLocation getAnimationResource(TransportBoatEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(TransportBoatEntity animatable, long instanceId, AnimationState<TransportBoatEntity> state) {
      super.handleAnimations(animatable, instanceId, state);
      float wheel = Mth.lerp(state.getPartialTick(), animatable.wheelRotationPrev, animatable.wheelRotation);
      CoreGeoBone paddleAxle = this.getAnimationProcessor().getBone("padleAxle");
      if (paddleAxle != null) {
         paddleAxle.setRotX(wheel * (float) (Math.PI / 180.0));
      }
   }
}
