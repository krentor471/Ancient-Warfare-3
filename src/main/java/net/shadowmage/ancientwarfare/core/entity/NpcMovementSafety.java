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
      Vec3 horizontal = new Vec3(horizontalDirection.f_82479_, 0.0, horizontalDirection.f_82481_);
      if (!(horizontal.m_82556_() < 1.0E-6) && entity.m_20096_() && !entity.m_20072_() && !entity.m_20077_()) {
         Vec3 direction = horizontal.m_82541_();
         double lookAhead = Math.max(0.75, (double)entity.m_20205_() * 0.6 + horizontal.m_82553_());
         Vec3 side = new Vec3(-direction.f_82481_, 0.0, direction.f_82479_).m_82490_(Math.max(0.15, (double)entity.m_20205_() * 0.32));
         Vec3 front = entity.m_20182_().m_82549_(direction.m_82490_(lookAhead));
         double footY = entity.m_20186_();
         if (!combatNpcMayEnterWater(entity)
            || !isWaterAt(entity, front.f_82479_, footY, front.f_82481_)
               && !isWaterAt(entity, front.f_82479_ + side.f_82479_, footY, front.f_82481_ + side.f_82481_)
               && !isWaterAt(entity, front.f_82479_ - side.f_82479_, footY, front.f_82481_ - side.f_82481_)) {
            boolean centreSupported = hasSafeSupport(entity, front.f_82479_, footY, front.f_82481_);
            boolean leftSupported = hasSafeSupport(entity, front.f_82479_ + side.f_82479_, footY, front.f_82481_ + side.f_82481_);
            boolean rightSupported = hasSafeSupport(entity, front.f_82479_ - side.f_82479_, footY, front.f_82481_ - side.f_82481_);
            return centreSupported && (leftSupported || rightSupported);
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public static boolean canMoveMountedSafely(Entity entity, Vec3 horizontalDirection) {
      Vec3 horizontal = new Vec3(horizontalDirection.f_82479_, 0.0, horizontalDirection.f_82481_);
      if (!(horizontal.m_82556_() < 1.0E-6) && entity.m_20096_() && !entity.m_20072_() && !entity.m_20077_()) {
         Vec3 direction = horizontal.m_82541_();
         double lookAhead = Math.max(0.75, (double)entity.m_20205_() * 0.6 + horizontal.m_82553_());
         Vec3 front = entity.m_20182_().m_82549_(direction.m_82490_(lookAhead));
         double footY = entity.m_20186_();
         if (isWaterAt(entity, front.f_82479_, footY, front.f_82481_)) {
            return false;
         } else {
            Double landingY = findSupportTop(entity, front.f_82479_, footY, front.f_82481_, 1.35);
            if (landingY == null) {
               AABB falling = entity.m_20191_().m_82386_(direction.f_82479_ * lookAhead, 0.0, direction.f_82481_ * lookAhead).m_82406_(1.0E-4);
               return entity.m_9236_().m_45756_(entity, falling);
            } else {
               double verticalOffset = landingY - footY;
               if (verticalOffset > 1.0) {
                  return false;
               } else {
                  AABB projected = entity.m_20191_().m_82386_(direction.f_82479_ * lookAhead, verticalOffset, direction.f_82481_ * lookAhead).m_82406_(1.0E-4);
                  return entity.m_9236_().m_45756_(entity, projected);
               }
            }
         }
      } else {
         return true;
      }
   }

   public static boolean canContinueMountedOrbit(Entity entity, Vec3 horizontalDirection) {
      Vec3 horizontal = new Vec3(horizontalDirection.f_82479_, 0.0, horizontalDirection.f_82481_);
      if (!(horizontal.m_82556_() < 1.0E-6) && !entity.m_20072_() && !entity.m_20077_()) {
         Vec3 direction = horizontal.m_82541_();
         double lookAhead = Math.max(0.9, (double)entity.m_20205_() * 0.65 + horizontal.m_82553_());
         Vec3 forward = direction.m_82490_(lookAhead);
         Vec3 destination = entity.m_20182_().m_82549_(forward);
         double sideDistance = Math.max(0.18, (double)entity.m_20205_() * 0.32);
         Vec3 side = new Vec3(-direction.f_82481_, 0.0, direction.f_82479_).m_82490_(sideDistance);
         double footY = entity.m_20186_();
         return !isWaterAt(entity, destination.f_82479_, footY, destination.f_82481_)
               && !isWaterAt(entity, destination.f_82479_ + side.f_82479_, footY, destination.f_82481_ + side.f_82481_)
               && !isWaterAt(entity, destination.f_82479_ - side.f_82479_, footY, destination.f_82481_ - side.f_82481_)
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
      BlockPos pos = BlockPos.m_274561_(x, y, z);
      return entity.m_9236_().m_6425_(pos).m_205070_(FluidTags.f_13131_) || entity.m_9236_().m_6425_(pos.m_7495_()).m_205070_(FluidTags.f_13131_);
   }

   private static boolean hasSafeSupport(Entity entity, double x, double footY, double z) {
      return hasSupportWithinDrop(entity, x, footY, z, 1.1);
   }

   private static Double findSupportTop(Entity entity, double x, double footY, double z, double maxDrop) {
      for (int drop = 0; drop <= 2; drop++) {
         BlockPos pos = BlockPos.m_274561_(x, footY - 0.05 - (double)drop, z);
         BlockState state = entity.m_9236_().m_8055_(pos);
         VoxelShape shape = state.m_60812_(entity.m_9236_(), pos);
         if (!shape.m_83281_()) {
            double supportTop = (double)pos.m_123342_() + shape.m_83297_(Axis.Y);
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
