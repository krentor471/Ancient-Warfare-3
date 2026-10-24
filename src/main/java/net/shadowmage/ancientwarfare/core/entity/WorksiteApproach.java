package net.shadowmage.ancientwarfare.core.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Where a worker should stand to work a worksite. Walking at the worksite block itself sends the pathfinder to the top
 * of the block, so workers climbed onto it, pushed into the walls around it or walked at a roof forever. Instead the
 * worker picks a free spot next to the block, within working reach, that it really has a path to.
 */
final class WorksiteApproach {
   private static final int MAX_PATH_CHECKS = 4;

   private WorksiteApproach() {
   }

   /** A reachable spot to stand at, nearest to the worker first, or null when the worksite cannot be reached. */
   @Nullable
   static BlockPos find(Mob worker, BlockPos site, double reachSq) {
      Level level = worker.level();
      List<BlockPos> candidates = new ArrayList<>();

      for (int dy = -1; dy <= 1; dy++) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               if ((dx != 0 || dz != 0) && dx * dx + dy * dy + dz * dz < reachSq) {
                  BlockPos spot = site.offset(dx, dy, dz);
                  if (canStandAt(level, spot)) {
                     candidates.add(spot);
                  }
               }
            }
         }
      }

      // The block itself may be a floor to stand on, as for a site sunk into the ground.
      if (canStandAt(level, site.above())) {
         candidates.add(site.above());
      }

      // Nearest to the worker, but a spot right beside the block beats a corner one: a worker that stops a little short of
      // a corner spot is already out of reach.
      candidates.sort(
         Comparator.comparingDouble(
            spot -> Math.sqrt(worker.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5)) + 2.0 * spot.distSqr(site)
         )
      );
      int checks = 0;

      for (BlockPos spot : candidates) {
         if (worker.blockPosition().equals(spot)) {
            return spot;
         }

         if (checks++ >= MAX_PATH_CHECKS) {
            break;
         }

         Path path = worker.getNavigation().createPath(spot, 0);
         if (path != null && path.canReach()) {
            return spot;
         }
      }

      return null;
   }

   /** True once the worker stands on (or right by) its spot: the path may end a little short of the block's centre. */
   static boolean isAt(Mob worker, @Nullable BlockPos spot) {
      return spot != null && worker.getNavigation().isDone() && worker.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) <= 1.5;
   }

   /** Standing in a door or gate: never a place to stop and work, or it stays open behind the worker. */
   static boolean inDoorway(Mob worker) {
      BlockState state = worker.level().getBlockState(worker.blockPosition());
      return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock;
   }

   /** Solid ground below, room for the body, and no water or lava to stand in. */
   static boolean canStandAt(Level level, BlockPos pos) {
      // Farmland, slabs and paths are lower than a full block but still carry a worker; fences and walls are not floor.
      VoxelShape ground = level.getBlockState(pos.below()).getCollisionShape(level, pos.below());
      return !ground.isEmpty()
         && ground.max(Axis.Y) >= 0.5
         && ground.max(Axis.Y) <= 1.0
         && isClear(level, pos)
         && isClear(level, pos.above());
   }

   private static boolean isClear(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
   }
}
