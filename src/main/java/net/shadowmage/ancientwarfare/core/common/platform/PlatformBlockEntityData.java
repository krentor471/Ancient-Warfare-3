package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface PlatformBlockEntityData {
   CompoundTag data(BlockEntity var1);
}
