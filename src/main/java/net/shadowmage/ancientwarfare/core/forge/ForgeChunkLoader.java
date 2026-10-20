package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformChunkLoader;

public class ForgeChunkLoader implements PlatformChunkLoader {
   @Override
   public boolean setForced(ServerLevel level, BlockPos owner, ChunkPos chunk, boolean forced, boolean ticking) {
      return ForgeChunkManager.forceChunk(level, "ancientwarfare", owner, chunk.x, chunk.z, forced, ticking);
   }
}
