package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.core.block.TorqueGeneratorBlock;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueGeneratorKind;
import net.shadowmage.ancientwarfare.core.tile.TorqueGeneratorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TorqueGeneratorGeoRenderer extends GeoBlockRenderer<TorqueGeneratorBlockEntity> {
   public TorqueGeneratorGeoRenderer() {
      super(new TorqueGeneratorGeoModel());
   }

   protected Direction getFacing(TorqueGeneratorBlockEntity animatable) {
      if (!animatable.getBlockState().hasProperty(TorqueGeneratorBlock.FACING)) {
         return Direction.NORTH;
      } else {
         Direction facing = (Direction)animatable.getBlockState().getValue(TorqueGeneratorBlock.FACING);
         if (animatable.getBlockState().getBlock() instanceof TorqueGeneratorBlock block && block.getKind() == TorqueGeneratorKind.WINDMILL) {
            return facing.getOpposite();
         }

         return facing;
      }
   }
}
