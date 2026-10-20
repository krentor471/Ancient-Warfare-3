package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

final class NpcSuffocationEscape {
   private static final int HORIZONTAL_SEARCH_RADIUS = 5;
   private static final int[] Y_OFFSETS = new int[]{0, 1, 2, 3, 4, -1};
   private static final double BOX_EPSILON = 1.0E-4;
   private static final double SUPPORT_TOLERANCE = 0.35;

   private NpcSuffocationEscape() {
   }

   static boolean tryEscape(NpcBaseEntity npc) {
      Entity mover = escapeMover(npc);
      if (isSuffocating(npc) || mover != npc && isSuffocating(mover)) {
         Vec3 destination = findNearestSafePosition(npc, mover);
         if (destination == null) {
            stopMovement(npc, mover);
            return false;
         } else {
            stopMovement(npc, mover);
            mover.teleportTo(destination.x, destination.y, destination.z);
            mover.setDeltaMovement(Vec3.ZERO);
            mover.fallDistance = 0.0F;
            mover.hurtMarked = true;
            if (mover != npc) {
               npc.setDeltaMovement(Vec3.ZERO);
               npc.fallDistance = 0.0F;
               npc.hurtMarked = true;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static Entity escapeMover(NpcBaseEntity npc) {
      Entity vehicle = npc.getVehicle();
      return (Entity)(vehicle instanceof Mob ? vehicle : npc);
   }

   private static boolean isSuffocating(Entity entity) {
      return entity != null && entity.isAlive() && entity.isInWall();
   }

   private static Vec3 findNearestSafePosition(NpcBaseEntity npc, Entity mover) {
      BlockPos origin = mover.blockPosition();

      for (int yOffset : Y_OFFSETS) {
         for (int radius = 0; radius <= 5; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
               for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                  if (radius <= 0 || Math.max(Math.abs(xOffset), Math.abs(zOffset)) == radius) {
                     Vec3 candidate = new Vec3(
                        (double)(origin.getX() + xOffset) + 0.5, (double)(origin.getY() + yOffset), (double)(origin.getZ() + zOffset) + 0.5
                     );
                     if (isSafeDestination(npc, mover, candidate)) {
                        return candidate;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private static boolean isSafeDestination(NpcBaseEntity npc, Entity mover, Vec3 destination) {
      AABB currentBox = mover.getBoundingBox();
      double moveX = destination.x - mover.getX();
      double moveY = destination.y - mover.getY();
      double moveZ = destination.z - mover.getZ();
      AABB candidateBox = currentBox.move(moveX, moveY, moveZ);
      Level level = mover.level();
      if (!level.noCollision(mover, candidateBox.deflate(1.0E-4))) {
         return false;
      } else {
         if (mover != npc) {
            AABB riderBox = npc.getBoundingBox().move(moveX, moveY, moveZ);
            if (!level.noCollision(npc, riderBox.deflate(1.0E-4))) {
               return false;
            }
         }

         return !level.getWorldBorder().isWithinBounds(candidateBox)
            ? false
            : hasSupport(level, candidateBox.minX + 1.0E-4, candidateBox.minY, candidateBox.minZ + 1.0E-4)
               && hasSupport(level, candidateBox.maxX - 1.0E-4, candidateBox.minY, candidateBox.minZ + 1.0E-4)
               && hasSupport(level, candidateBox.minX + 1.0E-4, candidateBox.minY, candidateBox.maxZ - 1.0E-4)
               && hasSupport(level, candidateBox.maxX - 1.0E-4, candidateBox.minY, candidateBox.maxZ - 1.0E-4);
      }
   }

   private static boolean hasSupport(Level level, double x, double footY, double z) {
      BlockPos supportPos = BlockPos.containing(x, footY - 0.05, z);
      BlockState state = level.getBlockState(supportPos);
      VoxelShape shape = state.getCollisionShape(level, supportPos);
      if (shape.isEmpty()) {
         return false;
      } else {
         double supportTop = (double)supportPos.getY() + shape.max(Axis.Y);
         return footY - supportTop >= -1.0E-4 && footY - supportTop <= 0.35;
      }
   }

   private static void stopMovement(NpcBaseEntity npc, Entity mover) {
      npc.getNavigation().stop();
      npc.setDeltaMovement(Vec3.ZERO);
      if (mover instanceof Mob mob) {
         mob.getNavigation().stop();
         mob.setDeltaMovement(Vec3.ZERO);
      }
   }
}
