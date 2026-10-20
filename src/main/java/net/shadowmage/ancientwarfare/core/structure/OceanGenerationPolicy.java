package net.shadowmage.ancientwarfare.core.structure;

public final class OceanGenerationPolicy {
   private OceanGenerationPolicy() {
   }

   public static int normalizedMinDepth(int min, int max) {
      return min > max ? 1 : Math.max(1, min);
   }

   public static int normalizedMaxDepth(int min, int max) {
      return Math.max(normalizedMinDepth(min, max), max);
   }

   public static boolean depthAcceptable(boolean island, int min, int max, int depth) {
      int minimum = normalizedMinDepth(min, max);
      int maximum = normalizedMaxDepth(min, max);
      if (island) {
         minimum = (int)Math.max(1L, Math.min(8L, ((long)minimum + 1L) / 2L));
         maximum = (int)Math.min(2147483647L, Math.max((long)minimum, (long)maximum * 2L + 8L));
      }

      return depth >= minimum && depth <= maximum;
   }

   public static boolean coverageAcceptable(long water, long total) {
      return meetsPercentage(water, total, 75);
   }

   public static boolean islandDepthAcceptable(long suitable, long water) {
      return meetsPercentage(suitable, water, 70);
   }

   public static int selectionWeight(int base, int recentOccurrences) {
      long divisor = 1L + (long)Math.max(0, recentOccurrences) * 2L;
      return (int)Math.max(1L, (long)base / divisor);
   }

   private static boolean meetsPercentage(long count, long total, int percentage) {
      if (total > 0L && count >= 0L && count <= total) {
         long threshold = total / 100L * (long)percentage + (total % 100L * (long)percentage + 99L) / 100L;
         return count >= threshold;
      } else {
         return false;
      }
   }
}
