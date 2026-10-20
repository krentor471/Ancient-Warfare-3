package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.core.common.inventory.ItemHandlerAccess;

public interface PlatformInventoryAccess {
   ItemHandlerAccess find(BlockEntity var1, Direction var2);
}
