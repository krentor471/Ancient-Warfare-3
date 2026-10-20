package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.core.block.HandCrankedGeneratorBlock;
import net.shadowmage.ancientwarfare.core.tile.HandCrankedGeneratorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HandCrankedGeneratorGeoRenderer extends GeoBlockRenderer<HandCrankedGeneratorBlockEntity> {
   public HandCrankedGeneratorGeoRenderer() {
      super(new HandCrankedGeneratorGeoModel());
   }

   protected Direction getFacing(HandCrankedGeneratorBlockEntity animatable) {
      return animatable.getBlockState().hasProperty(HandCrankedGeneratorBlock.FACING)
         ? (Direction)animatable.getBlockState().getValue(HandCrankedGeneratorBlock.FACING)
         : Direction.NORTH;
   }
}
