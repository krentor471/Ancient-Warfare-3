package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.block.TorqueTransportBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTransportKind;
import net.shadowmage.ancientwarfare.core.tile.TorqueTransportBlockEntity;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class TorqueTransportGeoModel extends GeoModel<TorqueTransportBlockEntity> {
   private static final ResourceLocation SHAFT_MODEL = AWCoreIds.id("geo/block/automation/torque_shaft.geo.json");
   private static final ResourceLocation JUNCTION_MODEL = AWCoreIds.id("geo/block/automation/torque_junction.geo.json");
   private static final ResourceLocation DISTRIBUTOR_MODEL = AWCoreIds.id("geo/block/automation/torque_distributor.geo.json");
   private static final ResourceLocation SHAFT_ANIMATION = AWCoreIds.id("animations/block/automation/torque_shaft.animation.json");
   private static final ResourceLocation JUNCTION_ANIMATION = AWCoreIds.id("animations/block/automation/torque_junction.animation.json");
   private static final ResourceLocation DISTRIBUTOR_ANIMATION = AWCoreIds.id("animations/block/automation/torque_distributor.animation.json");

   public ResourceLocation getModelResource(TorqueTransportBlockEntity animatable) {
      return this.kind(animatable) == TorqueTransportKind.JUNCTION
         ? JUNCTION_MODEL
         : (this.kind(animatable) == TorqueTransportKind.DISTRIBUTOR ? DISTRIBUTOR_MODEL : SHAFT_MODEL);
   }

   public ResourceLocation getTextureResource(TorqueTransportBlockEntity animatable) {
      TorqueTier tier = animatable.getBlockState().getBlock() instanceof TorqueTransportBlock block ? block.getTier() : TorqueTier.LIGHT;
      String prefix = this.kind(animatable) == TorqueTransportKind.JUNCTION
         ? "torque_junction_"
         : (this.kind(animatable) == TorqueTransportKind.DISTRIBUTOR ? "torque_distributor_" : "torque_shaft_");
      return AWCoreIds.id("textures/block/automation/" + prefix + this.tierName(tier) + ".png");
   }

   public ResourceLocation getAnimationResource(TorqueTransportBlockEntity animatable) {
      return this.kind(animatable) == TorqueTransportKind.JUNCTION
         ? JUNCTION_ANIMATION
         : (this.kind(animatable) == TorqueTransportKind.DISTRIBUTOR ? DISTRIBUTOR_ANIMATION : SHAFT_ANIMATION);
   }

   public void handleAnimations(TorqueTransportBlockEntity animatable, long instanceId, AnimationState<TorqueTransportBlockEntity> animationState) {
      TorqueTransportKind kind = this.kind(animatable);
      if (kind == TorqueTransportKind.JUNCTION || kind == TorqueTransportKind.DISTRIBUTOR) {
         String prefix = kind == TorqueTransportKind.JUNCTION ? "junction_" : "distributor_";
         Direction facing = (Direction)animatable.getBlockState().getValue(TorqueTransportBlock.FACING);
         Direction specialFace = kind == TorqueTransportKind.JUNCTION ? facing : facing.getOpposite();
         Direction modelSpecialFace = this.centerModelDirection(specialFace);

         for (Direction direction : Direction.values()) {
            this.setHidden(animatable, prefix + "center_" + direction.getName(), direction != modelSpecialFace);
         }

         this.setHidden(animatable, prefix + "east_rotating", !this.connected(animatable, Direction.EAST));
         this.setHidden(animatable, prefix + "west_rotating", !this.connected(animatable, Direction.WEST));
         this.setHidden(animatable, prefix + "up_rotating", !this.connected(animatable, Direction.UP));
         this.setHidden(animatable, prefix + "down_rotating", !this.connected(animatable, Direction.DOWN));
         this.setHidden(animatable, prefix + "north_rotating", !this.connected(animatable, Direction.SOUTH));
         this.setHidden(animatable, prefix + "south_rotating", !this.connected(animatable, Direction.NORTH));
      }

      super.handleAnimations(animatable, instanceId, animationState);
   }

   private Direction centerModelDirection(Direction worldDirection) {
      return worldDirection.getAxis().isHorizontal() ? worldDirection.getOpposite() : worldDirection;
   }

   private boolean isJunction(TorqueTransportBlockEntity animatable) {
      return this.kind(animatable) == TorqueTransportKind.JUNCTION;
   }

   private TorqueTransportKind kind(TorqueTransportBlockEntity animatable) {
      return animatable.getBlockState().getBlock() instanceof TorqueTransportBlock block ? block.getKind() : TorqueTransportKind.SHAFT;
   }

   private boolean connected(TorqueTransportBlockEntity animatable, Direction direction) {
      if (animatable.getBlockState().getBlock() instanceof TorqueTransportBlock block && block.isConnected(animatable.getBlockState(), direction)) {
         return true;
      }

      return false;
   }

   private void setHidden(TorqueTransportBlockEntity animatable, String name, boolean hidden) {
      this.getBone(name).ifPresent(bone -> bone.setHidden(hidden));
   }

   private String tierName(TorqueTier tier) {
      return tier.getId();
   }
}
