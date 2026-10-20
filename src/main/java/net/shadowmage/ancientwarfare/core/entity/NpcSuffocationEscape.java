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
            mover.m_6021_(destination.f_82479_, destination.f_82480_, destination.f_82481_);
            mover.m_20256_(Vec3.f_82478_);
            mover.f_19789_ = 0.0F;
            mover.f_19864_ = true;
            if (mover != npc) {
               npc.m_20256_(Vec3.f_82478_);
               npc.f_19789_ = 0.0F;
               npc.f_19864_ = true;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static Entity escapeMover(NpcBaseEntity npc) {
      Entity vehicle = npc.m_20202_();
      return (Entity)(vehicle instanceof Mob ? vehicle : npc);
   }

   private static boolean isSuffocating(Entity entity) {
      return entity != null && entity.m_6084_() && entity.m_5830_();
   }

   private static Vec3 findNearestSafePosition(NpcBaseEntity npc, Entity mover) {
      BlockPos origin = mover.m_20183_();

      for (int yOffset : Y_OFFSETS) {
         for (int radius = 0; radius <= 5; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
               for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                  if (radius <= 0 || Math.max(Math.abs(xOffset), Math.abs(zOffset)) == radius) {
                     Vec3 candidate = new Vec3(
                        (double)(origin.m_123341_() + xOffset) + 0.5, (double)(origin.m_123342_() + yOffset), (double)(origin.m_123343_() + zOffset) + 0.5
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
      AABB currentBox = mover.m_20191_();
      double moveX = destination.f_82479_ - mover.m_20185_();
      double moveY = destination.f_82480_ - mover.m_20186_();
      double moveZ = destination.f_82481_ - mover.m_20189_();
      AABB candidateBox = currentBox.m_82386_(moveX, moveY, moveZ);
      Level level = mover.m_9236_();
      if (!level.m_45756_(mover, candidateBox.m_82406_(1.0E-4))) {
         return false;
      } else {
         if (mover != npc) {
            AABB riderBox = npc.m_20191_().m_82386_(moveX, moveY, moveZ);
            if (!level.m_45756_(npc, riderBox.m_82406_(1.0E-4))) {
               return false;
            }
         }

         return !level.m_6857_().m_61935_(candidateBox)
            ? false
            : hasSupport(level, candidateBox.f_82288_ + 1.0E-4, candidateBox.f_82289_, candidateBox.f_82290_ + 1.0E-4)
               && hasSupport(level, candidateBox.f_82291_ - 1.0E-4, candidateBox.f_82289_, candidateBox.f_82290_ + 1.0E-4)
               && hasSupport(level, candidateBox.f_82288_ + 1.0E-4, candidateBox.f_82289_, candidateBox.f_82293_ - 1.0E-4)
               && hasSupport(level, candidateBox.f_82291_ - 1.0E-4, candidateBox.f_82289_, candidateBox.f_82293_ - 1.0E-4);
      }
   }

   private static boolean hasSupport(Level level, double x, double footY, double z) {
      BlockPos supportPos = BlockPos.m_274561_(x, footY - 0.05, z);
      BlockState state = level.m_8055_(supportPos);
      VoxelShape shape = state.m_60812_(level, supportPos);
      if (shape.m_83281_()) {
         return false;
      } else {
         double supportTop = (double)supportPos.m_123342_() + shape.m_83297_(Axis.Y);
         return footY - supportTop >= -1.0E-4 && footY - supportTop <= 0.35;
      }
   }

   private static void stopMovement(NpcBaseEntity npc, Entity mover) {
      npc.m_21573_().m_26573_();
      npc.m_20256_(Vec3.f_82478_);
      if (mover instanceof Mob mob) {
         mob.m_21573_().m_26573_();
         mob.m_20256_(Vec3.f_82478_);
      }
   }
}
