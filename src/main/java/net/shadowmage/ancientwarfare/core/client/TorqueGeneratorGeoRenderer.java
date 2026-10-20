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
      if (!animatable.m_58900_().m_61138_(TorqueGeneratorBlock.FACING)) {
         return Direction.NORTH;
      } else {
         Direction facing = (Direction)animatable.m_58900_().m_61143_(TorqueGeneratorBlock.FACING);
         if (animatable.m_58900_().m_60734_() instanceof TorqueGeneratorBlock block && block.getKind() == TorqueGeneratorKind.WINDMILL) {
            return facing.m_122424_();
         }

         return facing;
      }
   }
}
