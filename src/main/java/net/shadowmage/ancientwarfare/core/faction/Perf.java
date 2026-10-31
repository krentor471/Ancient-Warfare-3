package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Map;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/** Time spent by each part of the world's life, logged now and then with the slowest first. */
public final class Perf {
   private static final Map<String, long[]> TIMES = new HashMap<>();
   private static long windowStart = System.nanoTime();

   private Perf() {
   }

   public static void run(String name, Runnable work) {
      long start = System.nanoTime();
      try {
         work.run();
      } finally {
         long spent = System.nanoTime() - start;
         long[] time = TIMES.computeIfAbsent(name, key -> new long[2]);
         time[0] += spent;
         time[1] = Math.max(time[1], spent);
      }
   }

   /** Every half minute: the parts that took the most, total and longest single tick, in ms. */
   public static void report() {
      long now = System.nanoTime();
      if (now - windowStart < 30_000_000_000L) {
         return;
      }

      StringBuilder line = new StringBuilder("AW perf 30s:");
      TIMES.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0])).limit(10)
         .forEach(entry -> line.append(String.format(" %s=%.0f/%.0f", entry.getKey(), entry.getValue()[0] / 1e6, entry.getValue()[1] / 1e6)));
      AncientWarfareCore.LOG.info(line.toString());
      TIMES.clear();
      windowStart = now;
   }
}
