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
      if (level != null && !level.isClientSide) {
         STATES.computeIfAbsent(level, ignored -> new WarehouseNetwork.State()).revision++;
      }
   }

   public static void register(BlockEntity member) {
      Level level = member.getLevel();
      if (level != null && !level.isClientSide) {
         if (member instanceof WarehouseControlBlockEntity control) {
            STATES.computeIfAbsent(level, ignored -> new WarehouseNetwork.State()).controls.put(member.getBlockPos(), new WeakReference<>(control));
         }

         changed(level);
      }
   }

   public static List<BlockEntity> loadedEntities(Level level, BlockPos min, BlockPos max) {
      List<BlockEntity> result = new ArrayList<>();
      if (!(level instanceof ServerLevel server)) {
         return result;
      } else {
         for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
               LevelChunk chunk = server.getChunkSource().getChunkNow(x, z);
               if (chunk != null) {
                  for (BlockEntity entity : new ArrayList(chunk.getBlockEntities().values())) {
                     BlockPos p = entity.getBlockPos();
                     if (!entity.isRemoved()
                        && p.getX() >= min.getX()
                        && p.getX() <= max.getX()
                        && p.getY() >= min.getY()
                        && p.getY() <= max.getY()
                        && p.getZ() >= min.getZ()
                        && p.getZ() <= max.getZ()) {
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
            LevelChunk chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            entity = chunk == null ? null : chunk.getBlockEntity(pos);
         } else {
            entity = level.hasChunkAt(pos) ? level.getBlockEntity(pos) : null;
         }

         if (entity instanceof WarehouseControlBlockEntity c && !c.isRemoved()) {
            return c;
         }

         return null;
      } else {
         return null;
      }
   }

   public static boolean accepts(WarehouseControlBlockEntity control, BlockEntity member) {
      return control.containsWorkTarget(member.getBlockPos())
         || member instanceof WarehouseCraftingStationBlockEntity && member.getBlockPos().below().equals(control.getBlockPos());
   }

   public static List<WarehouseControlBlockEntity> controllersFor(BlockEntity member) {
      Level level = member.getLevel();
      if (level != null && !level.isClientSide) {
         WarehouseNetwork.State state = STATES.computeIfAbsent(level, ignored -> new WarehouseNetwork.State());

         for (BlockEntity entity : loadedEntities(level, member.getBlockPos().offset(-16, -4, -16), member.getBlockPos().offset(16, 4, 16))) {
            if (entity instanceof WarehouseControlBlockEntity control) {
               state.controls.put(control.getBlockPos(), new WeakReference<>(control));
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
            Comparator.<WarehouseControlBlockEntity>comparingDouble(c -> c.getBlockPos().distSqr(member.getBlockPos()))
               .thenComparingInt(c -> c.getBlockPos().getX())
               .thenComparingInt(c -> c.getBlockPos().getY())
               .thenComparingInt(c -> c.getBlockPos().getZ())
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
