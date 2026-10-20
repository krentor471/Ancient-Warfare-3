package net.shadowmage.ancientwarfare.core.common.worksite;

import net.minecraft.core.BlockPos;

public interface BoundedArea {
   BlockPos getBoundsMin();

   BlockPos getBoundsMax();

   boolean containsWorkTarget(BlockPos var1);
}
