package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.shadowmage.ancientwarfare.core.block.TorqueTransportBlock;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTransportKind;
import net.shadowmage.ancientwarfare.core.tile.TorqueTransportBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TorqueTransportGeoRenderer extends GeoBlockRenderer<TorqueTransportBlockEntity> {
   public TorqueTransportGeoRenderer() {
      super(new TorqueTransportGeoModel());
   }

   protected Direction getFacing(TorqueTransportBlockEntity animatable) {
      if (animatable.getBlockState().getBlock() instanceof TorqueTransportBlock block && block.getKind() != TorqueTransportKind.SHAFT) {
         return Direction.SOUTH;
      }

      return animatable.getBlockState().hasProperty(TorqueTransportBlock.FACING)
         ? (Direction)animatable.getBlockState().getValue(TorqueTransportBlock.FACING)
         : Direction.NORTH;
   }

   protected void rotateBlock(Direction facing, PoseStack poseStack) {
      if (facing.getAxis() == Axis.Y) {
         poseStack.translate(0.0, 0.5, 0.0);
         super.rotateBlock(facing, poseStack);
         poseStack.translate(0.0, -0.5, 0.0);
      } else {
         super.rotateBlock(facing, poseStack);
      }
   }
}
