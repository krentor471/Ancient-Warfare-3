package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.core.block.FlywheelControllerBlock;
import net.shadowmage.ancientwarfare.core.tile.FlywheelControllerBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class FlywheelControllerGeoRenderer extends GeoBlockRenderer<FlywheelControllerBlockEntity> {
   public FlywheelControllerGeoRenderer() {
      super(new FlywheelControllerGeoModel());
   }

   protected Direction getFacing(FlywheelControllerBlockEntity animatable) {
      return animatable.getBlockState().hasProperty(FlywheelControllerBlock.FACING)
         ? (Direction)animatable.getBlockState().getValue(FlywheelControllerBlock.FACING)
         : Direction.NORTH;
   }
}
