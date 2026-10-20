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
      return animatable.m_58900_().m_61138_(FlywheelControllerBlock.FACING)
         ? (Direction)animatable.m_58900_().m_61143_(FlywheelControllerBlock.FACING)
         : Direction.NORTH;
   }
}
