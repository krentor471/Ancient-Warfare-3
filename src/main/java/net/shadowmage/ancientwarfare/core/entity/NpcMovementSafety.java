package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class NpcMovementSafety {
   private static final int MAX_SAFE_DROP = 1;

   private NpcMovementSafety() {
   }

   public static boolean canMoveSafely(Entity entity, Vec3 horizontalDirection) {
      Vec3 horizontal = new Vec3(horizontalDirection.x, 0.0, horizontalDirection.z);
      if (!(horizontal.lengthSqr() < 1.0E-6) && entity.onGround() && !entity.isInWaterOrBubble() && !entity.isInLava()) {
         Vec3 direction = horizontal.normalize();
         double lookAhead = Math.max(0.75, (double)entity.getBbWidth() * 0.6 + horizontal.length());
         Vec3 side = new Vec3(-direction.z, 0.0, direction.x).scale(Math.max(0.15, (double)entity.getBbWidth() * 0.32));
         Vec3 front = entity.position().add(direction.scale(lookAhead));
         double footY = entity.getY();
         if (!combatNpcMayEnterWater(entity)
            || !isWaterAt(entity, front.x, footY, front.z)
               && !isWaterAt(entity, front.x + side.x, footY, front.z + side.z)
               && !isWaterAt(entity, front.x - side.x, footY, front.z - side.z)) {
            boolean centreSupported = hasSafeSupport(entity, front.x, footY, front.z);
            boolean leftSupported = hasSafeSupport(entity, front.x + side.x, footY, front.z + side.z);
            boolean rightSupported = hasSafeSupport(entity, front.x - side.x, footY, front.z - side.z);
            return centreSupported && (leftSupported || rightSupported);
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public static boolean canMoveMountedSafely(Entity entity, Vec3 horizontalDirection) {
      Vec3 horizontal = new Vec3(horizontalDirection.x, 0.0, horizontalDirection.z);
      if (!(horizontal.lengthSqr() < 1.0E-6) && entity.onGround() && !entity.isInWaterOrBubble() && !entity.isInLava()) {
         Vec3 direction = horizontal.normalize();
         double lookAhead = Math.max(0.75, (double)entity.getBbWidth() * 0.6 + horizontal.length());
         Vec3 front = entity.position().add(direction.scale(lookAhead));
         double footY = entity.getY();
         if (isWaterAt(entity, front.x, footY, front.z)) {
            return false;
         } else {
            Double landingY = findSupportTop(entity, front.x, footY, front.z, 1.35);
            if (landingY == null) {
               AABB falling = entity.getBoundingBox().move(direction.x * lookAhead, 0.0, direction.z * lookAhead).deflate(1.0E-4);
               return entity.level().noCollision(entity, falling);
            } else {
               double verticalOffset = landingY - footY;
               if (verticalOffset > 1.0) {
                  return false;
               } else {
                  AABB projected = entity.getBoundingBox().move(direction.x * lookAhead, verticalOffset, direction.z * lookAhead).deflate(1.0E-4);
                  return entity.level().noCollision(entity, projected);
               }
            }
         }
      } else {
         return true;
      }
   }

   public static boolean canContinueMountedOrbit(Entity entity, Vec3 horizontalDirection) {
      Vec3 horizontal = new Vec3(horizontalDirection.x, 0.0, horizontalDirection.z);
      if (!(horizontal.lengthSqr() < 1.0E-6) && !entity.isInWaterOrBubble() && !entity.isInLava()) {
         Vec3 direction = horizontal.normalize();
         double lookAhead = Math.max(0.9, (double)entity.getBbWidth() * 0.65 + horizontal.length());
         Vec3 forward = direction.scale(lookAhead);
         Vec3 destination = entity.position().add(forward);
         double sideDistance = Math.max(0.18, (double)entity.getBbWidth() * 0.32);
         Vec3 side = new Vec3(-direction.z, 0.0, direction.x).scale(sideDistance);
         double footY = entity.getY();
         return !isWaterAt(entity, destination.x, footY, destination.z)
               && !isWaterAt(entity, destination.x + side.x, footY, destination.z + side.z)
               && !isWaterAt(entity, destination.x - side.x, footY, destination.z - side.z)
            ? canMoveMountedSafely(entity, direction)
            : false;
      } else {
         return false;
      }
   }

   private static boolean combatNpcMayEnterWater(Entity entity) {
      return entity instanceof CombatNpcEntity || entity instanceof FactionSoldierNpcEntity;
   }

   private static boolean isWaterAt(Entity entity, double x, double y, double z) {
      BlockPos pos = BlockPos.containing(x, y, z);
      return entity.level().getFluidState(pos).is(FluidTags.WATER) || entity.level().getFluidState(pos.below()).is(FluidTags.WATER);
   }

   private static boolean hasSafeSupport(Entity entity, double x, double footY, double z) {
      return hasSupportWithinDrop(entity, x, footY, z, 1.1);
   }

   private static Double findSupportTop(Entity entity, double x, double footY, double z, double maxDrop) {
      for (int drop = 0; drop <= 2; drop++) {
         BlockPos pos = BlockPos.containing(x, footY - 0.05 - (double)drop, z);
         BlockState state = entity.level().getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(entity.level(), pos);
         if (!shape.isEmpty()) {
            double supportTop = (double)pos.getY() + shape.max(Axis.Y);
            double dropDistance = footY - supportTop;
            if (dropDistance >= -0.6 && dropDistance <= maxDrop) {
               return supportTop;
            }
         }
      }

      return null;
   }

   private static boolean hasSupportWithinDrop(Entity entity, double x, double footY, double z, double maxDrop) {
      return findSupportTop(entity, x, footY, z, maxDrop) != null;
   }
}
