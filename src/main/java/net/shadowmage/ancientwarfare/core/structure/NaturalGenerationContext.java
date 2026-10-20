package net.shadowmage.ancientwarfare.core.structure;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class NaturalGenerationContext {
   private static final ThreadLocal<NaturalGenerationContext.Scope> CURRENT = new ThreadLocal<>();

   private NaturalGenerationContext() {
   }

   public static boolean isActive() {
      return CURRENT.get() != null;
   }

   public static boolean isInternalChunk(ChunkPos chunk) {
      NaturalGenerationContext.Scope scope = CURRENT.get();
      return scope != null && (scope.chunks.contains(chunk.m_45588_()) || scope.areas.stream().anyMatch(area -> area.contains(chunk)));
   }

   public static void markChunk(ChunkPos chunk) {
      NaturalGenerationContext.Scope scope = CURRENT.get();
      if (scope != null) {
         scope.chunks.add(chunk.m_45588_());
      }
   }

   public static void markChunk(BlockPos pos) {
      markChunk(new ChunkPos(pos));
   }

   public static void markArea(BlockPos min, int maxX, int maxZ) {
      NaturalGenerationContext.Scope scope = CURRENT.get();
      if (scope != null && maxX >= min.m_123341_() && maxZ >= min.m_123343_()) {
         scope.areas.add(new NaturalGenerationContext.ChunkArea(min.m_123341_() >> 4, min.m_123343_() >> 4, maxX >> 4, maxZ >> 4));
      }
   }

   public static <T> T run(Supplier<T> action) {
      NaturalGenerationContext.Scope previous = CURRENT.get();
      if (previous == null) {
         CURRENT.set(new NaturalGenerationContext.Scope());
      }

      Object var2;
      try {
         var2 = action.get();
      } finally {
         if (previous == null) {
            CURRENT.remove();
         }
      }

      return (T)var2;
   }

   public static void run(Runnable action) {
      run(() -> {
         action.run();
         return null;
      });
   }

   private static record ChunkArea(int minX, int minZ, int maxX, int maxZ) {
      private boolean contains(ChunkPos chunk) {
         return chunk.f_45578_ >= this.minX && chunk.f_45578_ <= this.maxX && chunk.f_45579_ >= this.minZ && chunk.f_45579_ <= this.maxZ;
      }
   }

   private static final class Scope {
      private final LongOpenHashSet chunks = new LongOpenHashSet();
      private final List<NaturalGenerationContext.ChunkArea> areas = new ArrayList<>();
   }
}
