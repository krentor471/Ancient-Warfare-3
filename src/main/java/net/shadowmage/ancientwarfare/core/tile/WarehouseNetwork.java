package net.shadowmage.ancientwarfare.core.tile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class WarehouseNetwork {
   private static final Map<Level, WarehouseNetwork.State> STATES = new WeakHashMap<>();

   private WarehouseNetwork() {
   }

   public static long revision(Level level) {
      return STATES.computeIfAbsent(level, ignored -> new WarehouseNetwork.State()).revision;
   }

   public static void changed(Level level) {
      if (level != null && !level.f_46443_) {
         STATES.computeIfAbsent(level, ignored -> new WarehouseNetwork.State()).revision++;
      }
   }

   public static void register(BlockEntity member) {
      Level level = member.m_58904_();
      if (level != null && !level.f_46443_) {
         if (member instanceof WarehouseControlBlockEntity control) {
            STATES.computeIfAbsent(level, ignored -> new WarehouseNetwork.State()).controls.put(member.m_58899_(), new WeakReference<>(control));
         }

         changed(level);
      }
   }

   public static List<BlockEntity> loadedEntities(Level level, BlockPos min, BlockPos max) {
      List<BlockEntity> result = new ArrayList<>();
      if (!(level instanceof ServerLevel server)) {
         return result;
      } else {
         for (int x = min.m_123341_() >> 4; x <= max.m_123341_() >> 4; x++) {
            for (int z = min.m_123343_() >> 4; z <= max.m_123343_() >> 4; z++) {
               LevelChunk chunk = server.m_7726_().m_7131_(x, z);
               if (chunk != null) {
                  for (BlockEntity entity : new ArrayList(chunk.m_62954_().values())) {
                     BlockPos p = entity.m_58899_();
                     if (!entity.m_58901_()
                        && p.m_123341_() >= min.m_123341_()
                        && p.m_123341_() <= max.m_123341_()
                        && p.m_123342_() >= min.m_123342_()
                        && p.m_123342_() <= max.m_123342_()
                        && p.m_123343_() >= min.m_123343_()
                        && p.m_123343_() <= max.m_123343_()) {
                        result.add(entity);
                     }
                  }
               }
            }
         }

         return result;
      }
   }

   public static WarehouseControlBlockEntity loadedController(Level level, BlockPos pos) {
      if (level != null && pos != null) {
         BlockEntity entity;
         if (level instanceof ServerLevel server) {
            LevelChunk chunk = server.m_7726_().m_7131_(pos.m_123341_() >> 4, pos.m_123343_() >> 4);
            entity = chunk == null ? null : chunk.m_7702_(pos);
         } else {
            entity = level.m_46805_(pos) ? level.m_7702_(pos) : null;
         }

         if (entity instanceof WarehouseControlBlockEntity c && !c.m_58901_()) {
            return c;
         }

         return null;
      } else {
         return null;
      }
   }

   public static boolean accepts(WarehouseControlBlockEntity control, BlockEntity member) {
      return control.containsWorkTarget(member.m_58899_())
         || member instanceof WarehouseCraftingStationBlockEntity && member.m_58899_().m_7495_().equals(control.m_58899_());
   }

   public static List<WarehouseControlBlockEntity> controllersFor(BlockEntity member) {
      Level level = member.m_58904_();
      if (level != null && !level.f_46443_) {
         WarehouseNetwork.State state = STATES.computeIfAbsent(level, ignored -> new WarehouseNetwork.State());

         for (BlockEntity entity : loadedEntities(level, member.m_58899_().m_7918_(-16, -4, -16), member.m_58899_().m_7918_(16, 4, 16))) {
            if (entity instanceof WarehouseControlBlockEntity control) {
               state.controls.put(control.m_58899_(), new WeakReference<>(control));
            }
         }

         List<WarehouseControlBlockEntity> result = new ArrayList<>();
         state.controls.entrySet().removeIf(entryx -> ((WeakReference)entryx.getValue()).get() == null);

         for (Entry<BlockPos, WeakReference<WarehouseControlBlockEntity>> entry : state.controls.entrySet()) {
            WarehouseControlBlockEntity control = entry.getValue().get();
            if (control != null && loadedController(level, entry.getKey()) == control && accepts(control, member)) {
               result.add(control);
            }
         }

         result.sort(
            Comparator.<WarehouseControlBlockEntity>comparingDouble(c -> c.m_58899_().m_123331_(member.m_58899_()))
               .thenComparingInt(c -> c.m_58899_().m_123341_())
               .thenComparingInt(c -> c.m_58899_().m_123342_())
               .thenComparingInt(c -> c.m_58899_().m_123343_())
         );
         return result;
      } else {
         return List.of();
      }
   }

   private static final class State {
      long revision;
      final Map<BlockPos, WeakReference<WarehouseControlBlockEntity>> controls = new HashMap<>();
   }
}
