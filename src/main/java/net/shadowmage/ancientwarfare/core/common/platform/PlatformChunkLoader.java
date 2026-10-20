package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public interface PlatformChunkLoader {
   boolean setForced(ServerLevel var1, BlockPos var2, ChunkPos var3, boolean var4, boolean var5);
}
