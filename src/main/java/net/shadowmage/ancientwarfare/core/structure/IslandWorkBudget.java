package net.shadowmage.ancientwarfare.core.structure;

import java.util.function.LongSupplier;

public final class IslandWorkBudget {
   private final int workLimit;
   private final int scanLimit;
   private final LongSupplier clock;
   private final long deadline;
   private int work;
   private int scans;

   public IslandWorkBudget(int workLimit, int millis) {
      this(workLimit, Math.max(256, Math.multiplyExact(Math.max(1, workLimit), 32)), (long)Math.max(1, millis) * 1000000L, System::nanoTime);
   }

   public IslandWorkBudget(int workLimit, int scanLimit, long nanos, LongSupplier clock) {
      this.workLimit = Math.max(1, workLimit);
      this.scanLimit = Math.max(1, scanLimit);
      this.clock = clock;
      this.deadline = clock.getAsLong() + Math.max(1L, nanos);
   }

   public boolean available() {
      return this.work < this.workLimit && this.scans < this.scanLimit && this.clock.getAsLong() - this.deadline < 0L;
   }

   public boolean tryScan() {
      if (!this.available()) {
         return false;
      } else {
         this.scans++;
         return true;
      }
   }

   public boolean tryWork() {
      if (!this.available()) {
         return false;
      } else {
         this.work++;
         return true;
      }
   }

   public int workUsed() {
      return this.work;
   }

   public int scansUsed() {
      return this.scans;
   }
}
