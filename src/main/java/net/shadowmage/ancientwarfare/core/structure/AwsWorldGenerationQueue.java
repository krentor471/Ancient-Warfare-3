package net.shadowmage.ancientwarfare.core.structure;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public final class AwsWorldGenerationQueue extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_aws_queue";
   private final LongLinkedOpenHashSet pending = new LongLinkedOpenHashSet();
   private final LongOpenHashSet attempted = new LongOpenHashSet();

   public static AwsWorldGenerationQueue get(ServerLevel level) {
      return (AwsWorldGenerationQueue)level.getDataStorage().computeIfAbsent(AwsWorldGenerationQueue::load, AwsWorldGenerationQueue::new, "ancientwarfare_aws_queue");
   }

   public boolean enqueue(ChunkPos chunk) {
      long key = chunk.toLong();
      if (!this.attempted.contains(key) && this.pending.add(key)) {
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public ChunkPos peek() {
      return this.pending.isEmpty() ? null : new ChunkPos(this.pending.firstLong());
   }

   public void markAttempted(ChunkPos chunk) {
      boolean removed = this.pending.remove(chunk.toLong());
      boolean added = this.attempted.add(chunk.toLong());
      if (removed || added) {
         this.setDirty();
      }
   }

   public int pendingCount() {
      return this.pending.size();
   }

   public boolean wasAttempted(ChunkPos chunk) {
      return this.attempted.contains(chunk.toLong());
   }

   public CompoundTag save(CompoundTag tag) {
      tag.putLongArray("Pending", this.pending.toLongArray());
      tag.putLongArray("Attempted", this.attempted.toLongArray());
      return tag;
   }

   public static AwsWorldGenerationQueue load(CompoundTag tag) {
      AwsWorldGenerationQueue queue = new AwsWorldGenerationQueue();

      for (long key : tag.getLongArray("Attempted")) {
         queue.attempted.add(key);
      }

      for (long key : tag.getLongArray("Pending")) {
         if (!queue.attempted.contains(key)) {
            queue.pending.add(key);
         }
      }

      return queue;
   }
}
