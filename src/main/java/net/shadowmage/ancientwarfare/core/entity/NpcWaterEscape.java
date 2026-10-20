package net.shadowmage.ancientwarfare.core.entity;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireDragonCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippocampusCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippogryphCompat;

final class NpcWaterEscape {
   private static final int SEARCH_RADIUS = 18;
   private static final int SEARCH_DOWN = 5;
   private static final int SEARCH_UP = 4;
   private static final int REPATH_TICKS = 15;
   private static final double ARRIVAL_DISTANCE_SQ = 1.5625;
   private static final double BOX_EPSILON = 1.0E-4;
   private static final Map<NpcBaseEntity, NpcWaterEscape.EscapeState> STATES = new WeakHashMap<>();

   private NpcWaterEscape() {
   }

   static boolean tick(NpcBaseEntity npc) {
      if (!isEligible(npc)) {
         STATES.remove(npc);
         return false;
      } else {
         Entity mover = mover(npc);
         if (!isInWater(mover)) {
            STATES.remove(npc);
            return false;
         } else {
            NpcWaterEscape.EscapeState state = STATES.computeIfAbsent(npc, ignored -> new NpcWaterEscape.EscapeState());
            long gameTime = npc.m_9236_().m_46467_();
            LivingEntity combatTarget = npc.m_5448_();
            LivingEntity movementTarget = combatTarget;
            if (combatTarget == null || !combatTarget.m_6084_()) {
               LivingEntity var10000;
               label48: {
                  if (npc instanceof PlayerOwnedNpcEntity owned && owned.getCurrentCommand() == NpcCommandType.FOLLOW) {
                     var10000 = (LivingEntity)owned.getFollowTargetPlayer().orElse(null);
                     break label48;
                  }

                  var10000 = null;
               }

               movementTarget = var10000;
            }

            if (isUnountedCombatant(npc) && movementTarget != null && movementTarget.m_6084_()) {
               state.target = null;
               state.nextSearch = gameTime + 15L;
               npc.m_21573_().m_26573_();
               swimTowardCombatTarget(npc, movementTarget);
               return true;
            } else {
               if (state.target == null
                  || gameTime >= state.nextSearch
                  || !isSafeBank(npc, mover, state.target)
                  || mover.m_20182_().m_82557_(state.target) <= 1.5625) {
                  state.target = findNearestBank(npc, mover);
                  state.nextSearch = gameTime + 15L;
               }

               npc.m_21573_().m_26573_();
               if (mover instanceof Mob mob) {
                  mob.m_21573_().m_26573_();
               }

               if (state.target == null) {
                  keepAfloat(mover);
                  return true;
               } else {
                  swimToward(npc, mover, state.target);
                  return true;
               }
            }
         }
      }
   }

   static boolean isRecoveryActive(NpcBaseEntity npc) {
      return !isEligible(npc) ? false : isInWater(mover(npc));
   }

   private static boolean isEligible(NpcBaseEntity npc) {
      Entity vehicle = npc.m_20202_();
      if (vehicle instanceof Mob) {
         if (IceAndFireHippocampusCompat.isHippocampus(vehicle) || IceAndFireHippogryphCompat.isHippogryph(vehicle) || IceAndFireDragonCompat.isDragon(vehicle)
            )
          {
            return false;
         }

         if (npc instanceof CombatNpcEntity) {
            return true;
         }
      }

      if (!(npc instanceof CombatNpcEntity) && !(npc instanceof FactionSoldierNpcEntity)) {
         String subtype = npc.getNpcSubtype();
         return !FactionNpcRole.CAVALRY.id().equals(subtype) && !FactionNpcRole.MOUNTED_ARCHER.id().equals(subtype)
            ? npc instanceof FactionArcherNpcEntity || npc instanceof CombatNpcEntity && NpcEquipmentResolver.isRangedWeapon(npc.m_21205_())
            : true;
      } else {
         return true;
      }
   }

   private static boolean isUnountedCombatant(NpcBaseEntity npc) {
      return npc.m_20202_() == null && (npc instanceof CombatNpcEntity || npc instanceof FactionSoldierNpcEntity);
   }

   private static Entity mover(NpcBaseEntity npc) {
      return (Entity)(npc.m_20202_() instanceof Mob mount ? mount : npc);
   }

   private static boolean isInWater(Entity entity) {
      BlockPos feet = entity.m_20183_();
      return entity.m_20072_()
         || entity.m_9236_().m_6425_(feet).m_205070_(FluidTags.f_13131_)
         || entity.m_9236_().m_6425_(feet.m_7494_()).m_205070_(FluidTags.f_13131_);
   }

   private static Vec3 findNearestBank(NpcBaseEntity npc, Entity mover) {
      BlockPos origin = mover.m_20183_();

      for (int radius = 1; radius <= 18; radius++) {
         Vec3 best = null;
         double bestScore = Double.MAX_VALUE;

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
                  for (int dy = 4; dy >= -5; dy--) {
                     BlockPos support = origin.m_7918_(dx, dy, dz);
                     Vec3 candidate = bankPosition(npc, mover, support);
                     if (candidate != null) {
                        double score = mover.m_20182_().m_82557_(candidate) + Math.abs(candidate.f_82480_ - mover.m_20186_()) * 2.0;
                        if (score < bestScore) {
                           bestScore = score;
                           best = candidate;
                        }
                     }
                  }
               }
            }
         }

         if (best != null) {
            return best;
         }
      }

      return null;
   }

   private static Vec3 bankPosition(NpcBaseEntity npc, Entity mover, BlockPos supportPos) {
      Level level = mover.m_9236_();
      BlockState supportState = level.m_8055_(supportPos);
      VoxelShape supportShape = supportState.m_60812_(level, supportPos);
      if (!supportShape.m_83281_() && !level.m_6425_(supportPos.m_7494_()).m_205070_(FluidTags.f_13131_)) {
         double supportTop = (double)supportPos.m_123342_() + supportShape.m_83297_(Axis.Y);
         Vec3 destination = new Vec3((double)supportPos.m_123341_() + 0.5, supportTop, (double)supportPos.m_123343_() + 0.5);
         return touchesWater(level, supportPos, supportTop) && isSafeBank(npc, mover, destination) ? destination : null;
      } else {
         return null;
      }
   }

   private static boolean touchesWater(Level level, BlockPos supportPos, double supportTop) {
      int y = (int)Math.floor(supportTop);

      for (Direction direction : Plane.HORIZONTAL) {
         BlockPos side = new BlockPos(supportPos.m_123341_(), y, supportPos.m_123343_()).m_121945_(direction);
         if (level.m_6425_(side).m_205070_(FluidTags.f_13131_) || level.m_6425_(side.m_7495_()).m_205070_(FluidTags.f_13131_)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isSafeBank(NpcBaseEntity npc, Entity mover, Vec3 destination) {
      Level level = mover.m_9236_();
      double dx = destination.f_82479_ - mover.m_20185_();
      double dy = destination.f_82480_ - mover.m_20186_();
      double dz = destination.f_82481_ - mover.m_20189_();
      AABB moverBox = mover.m_20191_().m_82386_(dx, dy, dz);
      if (level.m_45756_(mover, moverBox.m_82406_(1.0E-4)) && level.m_6857_().m_61935_(moverBox) && hasCentralSupport(level, moverBox)) {
         if (mover != npc) {
            AABB riderBox = npc.m_20191_().m_82386_(dx, dy, dz);
            if (!level.m_45756_(npc, riderBox.m_82406_(1.0E-4))) {
               return false;
            }
         }

         BlockPos feet = BlockPos.m_274561_(destination.f_82479_, destination.f_82480_ + 0.05, destination.f_82481_);
         return !level.m_6425_(feet).m_205070_(FluidTags.f_13131_) && !level.m_6425_(feet.m_7494_()).m_205070_(FluidTags.f_13131_);
      } else {
         return false;
      }
   }

   private static boolean hasCentralSupport(Level level, AABB box) {
      return hasSupport(level, (box.f_82288_ + box.f_82291_) * 0.5, box.f_82289_, (box.f_82290_ + box.f_82293_) * 0.5);
   }

   private static boolean hasSupport(Level level, double x, double footY, double z) {
      BlockPos pos = BlockPos.m_274561_(x, footY - 0.05, z);
      VoxelShape shape = level.m_8055_(pos).m_60812_(level, pos);
      if (shape.m_83281_()) {
         return false;
      } else {
         double top = (double)pos.m_123342_() + shape.m_83297_(Axis.Y);
         return Math.abs(footY - top) <= 0.4;
      }
   }

   private static void swimToward(NpcBaseEntity npc, Entity mover, Vec3 destination) {
      Vec3 delta = destination.m_82546_(mover.m_20182_());
      Vec3 horizontal = new Vec3(delta.f_82479_, 0.0, delta.f_82481_);
      Vec3 direction = horizontal.m_82556_() < 1.0E-5 ? Vec3.f_82478_ : horizontal.m_82541_();
      double speed = mover == npc ? 0.18 : 0.22;
      Vec3 current = mover.m_20184_();
      double upward = Math.max(current.f_82480_ * 0.45, 0.08);
      if (delta.f_82480_ > 0.45 || mover.f_19862_) {
         upward = 0.2;
      }

      if (delta.f_82480_ < -1.5) {
         upward = Math.max(0.035, upward * 0.55);
      }

      mover.m_20334_(direction.f_82479_ * speed, Math.min(0.24, upward), direction.f_82481_ * speed);
      mover.f_19789_ = 0.0F;
      mover.f_19864_ = true;
      if (mover != npc) {
         npc.m_20256_(Vec3.f_82478_);
         npc.f_19789_ = 0.0F;
      }

      face(npc, mover, direction);
   }

   private static void swimTowardCombatTarget(NpcBaseEntity npc, LivingEntity target) {
      Vec3 delta = target.m_20182_().m_82520_(0.0, (double)target.m_20206_() * 0.35, 0.0).m_82546_(npc.m_20182_());
      Vec3 horizontal = new Vec3(delta.f_82479_, 0.0, delta.f_82481_);
      Vec3 direction = horizontal.m_82556_() < 1.0E-5 ? Vec3.f_82478_ : horizontal.m_82541_();
      double vertical = Math.max(-0.08, Math.min(0.08, delta.f_82480_ * 0.12));
      if (npc.f_19862_ && delta.f_82480_ > 0.35) {
         vertical = 0.12;
      }

      npc.m_20334_(direction.f_82479_ * 0.16, vertical, direction.f_82481_ * 0.16);
      npc.f_19789_ = 0.0F;
      npc.f_19864_ = true;
      face(npc, npc, direction);
   }

   private static void keepAfloat(Entity mover) {
      Vec3 current = mover.m_20184_();
      mover.m_20334_(current.f_82479_ * 0.65, Math.max(current.f_82480_, 0.09), current.f_82481_ * 0.65);
      mover.f_19789_ = 0.0F;
      mover.f_19864_ = true;
   }

   private static void face(NpcBaseEntity npc, Entity mover, Vec3 direction) {
      if (!(direction.m_165925_() < 1.0E-5)) {
         float yaw = (float)Math.toDegrees(Math.atan2(-direction.f_82479_, direction.f_82481_));
         mover.m_146922_(yaw);
         mover.m_5618_(yaw);
         mover.m_5616_(yaw);
         npc.m_146922_(yaw);
         npc.m_5618_(yaw);
         npc.m_5616_(yaw);
      }
   }

   private static final class EscapeState {
      private Vec3 target;
      private long nextSearch;
   }
}
