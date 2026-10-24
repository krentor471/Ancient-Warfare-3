package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.core.block.WindmillBladeRunningBlock;
import net.shadowmage.ancientwarfare.core.tile.WindmillBladeBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WindmillBladeGeoRenderer extends GeoBlockRenderer<WindmillBladeBlockEntity> {
   public WindmillBladeGeoRenderer() {
      super(new WindmillBladeGeoModel());
   }

   protected Direction getFacing(WindmillBladeBlockEntity blade) {
      return blade.getBlockState().hasProperty(WindmillBladeRunningBlock.FACING)
         ? (Direction)blade.getBlockState().getValue(WindmillBladeRunningBlock.FACING)
         : Direction.NORTH;
   }
}
