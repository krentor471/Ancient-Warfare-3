package net.shadowmage.ancientwarfare.core.structure;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public final class IslandChunkWindow {
   private static final TicketType<BlockPos> TICKET = TicketType.m_9462_("ancientwarfare:island_work", Comparator.comparingLong(BlockPos::m_121878_));
   private static final int FULL_TICKET_DISTANCE = 0;
   private final LinkedHashMap<Long, ChunkPos> tickets = new LinkedHashMap<>(16, 0.75F, true);
   private ServerLevel level;
   private BlockPos owner;

   public boolean request(ServerLevel requestedLevel, BlockPos requestedOwner, ChunkPos chunk, int maxSize) {
      return this.requestGroup(requestedLevel, requestedOwner, List.of(chunk), maxSize);
   }

   public boolean requestGroup(ServerLevel requestedLevel, BlockPos requestedOwner, List<ChunkPos> indispensable, int maxSize) {
      Objects.requireNonNull(requestedLevel, "Island chunk window level");
      Objects.requireNonNull(requestedOwner, "Island chunk window owner");
      Objects.requireNonNull(indispensable, "Indispensable island chunks");
      if (this.level != requestedLevel || !requestedOwner.equals(this.owner)) {
         this.releaseAll();
         this.level = requestedLevel;
         this.owner = requestedOwner.m_7949_();
      }

      Map<Long, ChunkPos> required = new LinkedHashMap<>();

      for (ChunkPos chunk : indispensable) {
         Objects.requireNonNull(chunk, "Indispensable island chunk");
         required.put(chunk.m_45588_(), chunk);
         NaturalGenerationContext.markArea(new BlockPos(chunk.m_45604_() - 16, 0, chunk.m_45605_() - 16), chunk.m_45608_() + 16, chunk.m_45609_() + 16);
      }

      int effectiveLimit = Math.max(Math.max(1, maxSize), required.size());
      int missing = 0;

      for (long key : required.keySet()) {
         if (this.tickets.get(key) == null) {
            missing++;
         }
      }

      this.trimToSize(effectiveLimit - missing, required.keySet());

      for (Entry<Long, ChunkPos> entry : required.entrySet()) {
         if (!this.tickets.containsKey(entry.getKey())) {
            this.tickets.put(entry.getKey(), entry.getValue());

            try {
               this.level.m_7726_().addRegionTicket(TICKET, entry.getValue(), 0, this.owner, false);
            } catch (RuntimeException var11) {
               throw new IllegalStateException("Unable to request non-blocking island chunk " + entry.getValue() + " for " + this.owner, var11);
            }
         }
      }

      for (ChunkPos chunk : required.values()) {
         if (this.level.m_7726_().m_7131_(chunk.f_45578_, chunk.f_45579_) == null) {
            return false;
         }
      }

      return true;
   }

   private void trimToSize(int targetSize, Set<Long> indispensable) {
      Iterator<Entry<Long, ChunkPos>> iterator = this.tickets.entrySet().iterator();

      while (this.tickets.size() > targetSize && iterator.hasNext()) {
         Entry<Long, ChunkPos> entry = iterator.next();
         if (!indispensable.contains(entry.getKey())) {
            this.level.m_7726_().removeRegionTicket(TICKET, entry.getValue(), 0, this.owner, false);
            iterator.remove();
         }
      }
   }

   public void releaseAll() {
      if (this.level != null && this.owner != null) {
         for (ChunkPos chunk : this.tickets.values()) {
            try {
               this.level.m_7726_().removeRegionTicket(TICKET, chunk, 0, this.owner, false);
            } catch (RuntimeException var4) {
               AncientWarfareCore.LOG.warn("Unable to release island chunk {} for {}", new Object[]{chunk, this.owner, var4});
            }
         }
      }

      this.tickets.clear();
      this.level = null;
      this.owner = null;
   }

   public int size() {
      return this.tickets.size();
   }
}
