package net.shadowmage.ancientwarfare.core.tile;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class ChunkLoaderBlockEntity extends BlockEntity {
   private static final int REFRESH_INTERVAL = 40;
   private static final int RADIUS = 1;
   private final Set<Long> forcedChunks = new HashSet<>();
   private int refreshCooldown;

   public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.CHUNK_LOADER_SIMPLE), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ChunkLoaderBlockEntity chunkLoader) {
      if (level instanceof ServerLevel serverLevel) {
         if (chunkLoader.refreshCooldown > 0) {
            chunkLoader.refreshCooldown--;
         } else {
            chunkLoader.refreshCooldown = 40;
            chunkLoader.refreshChunkLoading(serverLevel);
         }
      }
   }

   public void releaseChunkLoading() {
      if (this.f_58857_ instanceof ServerLevel serverLevel) {
         this.setForcedChunks(serverLevel, Set.of());
      }
   }

   private void refreshChunkLoading(ServerLevel serverLevel) {
      this.setForcedChunks(serverLevel, this.desiredForcedChunks());
   }

   private void setForcedChunks(ServerLevel serverLevel, Set<Long> desired) {
      for (long chunk : Set.copyOf(this.forcedChunks)) {
         if (!desired.contains(chunk)) {
            AWCorePlatform.setChunkForced(serverLevel, this.f_58858_, new ChunkPos(chunk), false, true);
            this.forcedChunks.remove(chunk);
         }
      }

      for (long chunkx : desired) {
         if (!this.forcedChunks.contains(chunkx) && AWCorePlatform.setChunkForced(serverLevel, this.f_58858_, new ChunkPos(chunkx), true, true)) {
            this.forcedChunks.add(chunkx);
         }
      }
   }

   private Set<Long> desiredForcedChunks() {
      Set<Long> chunks = new HashSet<>();
      ChunkPos center = new ChunkPos(this.f_58858_);

      for (int chunkX = center.f_45578_ - 1; chunkX <= center.f_45578_ + 1; chunkX++) {
         for (int chunkZ = center.f_45579_ - 1; chunkZ <= center.f_45579_ + 1; chunkZ++) {
            chunks.add(ChunkPos.m_45589_(chunkX, chunkZ));
         }
      }

      return chunks;
   }
}
