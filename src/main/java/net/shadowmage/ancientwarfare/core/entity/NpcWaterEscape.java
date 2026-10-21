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

final class NpcWaterEscape {
   private static final int SEARCH_RADIUS = 18;
   private static final int SEARCH_DOWN = 5;
   private static final int SEARCH_UP = 4;
   private static final int REPATH_TICKS = 15;
   private static final double ARRIVAL_DISTANCE_SQ = 1.5625;
   private static final double BOX_EPSILON = 1.0E-4;
   private static final long DESTINATION_MEMORY_TICKS = 200L;
   private static final double INLAND_STEP = 2.5;
   private static final long INLAND_TICKS = 30L;
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
            NpcWaterEscape.EscapeState left = STATES.get(npc);
            if (left != null && left.inland != null && npc.level().getGameTime() < left.inlandUntil && mover.position().distanceToSqr(left.inland) > 0.36) {
               // Just out of the water: walk a few steps on, or the path starts from the water block behind and pulls back in.
               walkToward(npc, mover, left.inland);
               return true;
            }

            STATES.remove(npc);
            return false;
         } else {
            NpcWaterEscape.EscapeState state = STATES.computeIfAbsent(npc, ignored -> new NpcWaterEscape.EscapeState());
            long gameTime = npc.level().getGameTime();
            LivingEntity combatTarget = npc.getTarget();
            LivingEntity movementTarget = combatTarget;
            if (combatTarget == null || !combatTarget.isAlive()) {
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

            if (isUnountedCombatant(npc) && movementTarget != null && movementTarget.isAlive()) {
               state.target = null;
               state.nextSearch = gameTime + 15L;
               npc.getNavigation().stop();
               swimTowardCombatTarget(npc, movementTarget);
               return true;
            } else {
               Vec3 destination = destination(npc, mover, state, gameTime);
               if (state.target == null
                  || gameTime >= state.nextSearch
                  || !isSafeBank(npc, mover, state.target)
                  || mover.position().distanceToSqr(state.target) <= 1.5625) {
                  // Heading somewhere: climb out on the far side, not back where the NPC came from.
                  state.target = destination != null ? findBankToward(npc, mover, destination) : findNearestBank(npc, mover);
                  state.nextSearch = gameTime + 15L;
               }

               npc.getNavigation().stop();
               if (mover instanceof Mob mob) {
                  mob.getNavigation().stop();
               }

               if (state.target != null) {
                  swimToward(npc, mover, state.target);
                  if (destination != null) {
                     Vec3 onward = new Vec3(destination.x - state.target.x, 0.0, destination.z - state.target.z);
                     state.inland = onward.lengthSqr() > 4.0 ? state.target.add(onward.normalize().scale(INLAND_STEP)) : state.target;
                     state.inlandUntil = gameTime + INLAND_TICKS;
                  }
               } else if (destination != null) {
                  // No bank in sight that is nearer the goal: swim straight on across.
                  swimToward(npc, mover, new Vec3(destination.x, mover.getY(), destination.z));
               } else {
                  keepAfloat(mover);
               }

               return true;
            }
         }
      }
   }

   static boolean isRecoveryActive(NpcBaseEntity npc) {
      return !isEligible(npc) ? false : isInWater(mover(npc));
   }

   private static boolean isEligible(NpcBaseEntity npc) {
      Entity vehicle = npc.getVehicle();
      if (vehicle instanceof Mob) {
         if (npc instanceof CombatNpcEntity) {
            return true;
         }
      }

      if (!(npc instanceof CombatNpcEntity) && !(npc instanceof FactionSoldierNpcEntity)) {
         String subtype = npc.getNpcSubtype();
         return !FactionNpcRole.CAVALRY.id().equals(subtype) && !FactionNpcRole.MOUNTED_ARCHER.id().equals(subtype)
            ? npc instanceof FactionArcherNpcEntity || npc instanceof CombatNpcEntity && NpcEquipmentResolver.isRangedWeapon(npc.getMainHandItem())
            : true;
      } else {
         return true;
      }
   }

   private static boolean isUnountedCombatant(NpcBaseEntity npc) {
      return npc.getVehicle() == null && (npc instanceof CombatNpcEntity || npc instanceof FactionSoldierNpcEntity);
   }

   private static Entity mover(NpcBaseEntity npc) {
      return (Entity)(npc.getVehicle() instanceof Mob mount ? mount : npc);
   }

   private static boolean isInWater(Entity entity) {
      BlockPos feet = entity.blockPosition();
      return entity.isInWaterOrBubble()
         || entity.level().getFluidState(feet).is(FluidTags.WATER)
         || entity.level().getFluidState(feet.above()).is(FluidTags.WATER);
   }

   /**
    * Where the NPC was going when it got into the water: its ordered move or post, or the end of the path it was
    * walking. Remembered for a while, since swimming cancels the path.
    */
   private static Vec3 destination(NpcBaseEntity npc, Entity mover, NpcWaterEscape.EscapeState state, long gameTime) {
      if (npc instanceof PlayerOwnedNpcEntity owned
         && (owned.getCurrentCommand() == NpcCommandType.MOVE || owned.isHoldingPosition())
         && !BlockPos.ZERO.equals(owned.getCommandTarget())) {
         return Vec3.atBottomCenterOf(owned.getCommandTarget());
      }

      BlockPos pathEnd = mover instanceof Mob mob && mob != npc && mob.getNavigation().getTargetPos() != null
         ? mob.getNavigation().getTargetPos()
         : npc.getNavigation().getTargetPos();
      if (pathEnd != null && !npc.getNavigation().isDone()) {
         state.destination = Vec3.atBottomCenterOf(pathEnd);
         state.destinationUntil = gameTime + DESTINATION_MEMORY_TICKS;
      }

      if (state.destination != null && gameTime > state.destinationUntil) {
         state.destination = null;
      }

      return state.destination;
   }

   /** Bank that brings the NPC nearest its destination; null when every bank in reach lies behind it. */
   private static Vec3 findBankToward(NpcBaseEntity npc, Entity mover, Vec3 destination) {
      BlockPos origin = mover.blockPosition();
      double here = horizontalDistance(mover.position(), destination);
      Vec3 best = null;
      double bestScore = Double.MAX_VALUE;

      for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
         for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
            // Only the half of the circle facing the destination can bring the NPC closer.
            if ((destination.x - mover.getX()) * dx + (destination.z - mover.getZ()) * dz > 0.0) {
               for (int dy = SEARCH_UP; dy >= -SEARCH_DOWN; dy--) {
                  Vec3 candidate = bankPosition(npc, mover, origin.offset(dx, dy, dz));
                  if (candidate != null) {
                     double left = horizontalDistance(candidate, destination);
                     double score = left + horizontalDistance(mover.position(), candidate) * 0.35;
                     if (left < here - 1.0 && score < bestScore) {
                        bestScore = score;
                        best = candidate;
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private static double horizontalDistance(Vec3 a, Vec3 b) {
      return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
   }

   private static Vec3 findNearestBank(NpcBaseEntity npc, Entity mover) {
      BlockPos origin = mover.blockPosition();

      for (int radius = 1; radius <= 18; radius++) {
         Vec3 best = null;
         double bestScore = Double.MAX_VALUE;

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
                  for (int dy = 4; dy >= -5; dy--) {
                     BlockPos support = origin.offset(dx, dy, dz);
                     Vec3 candidate = bankPosition(npc, mover, support);
                     if (candidate != null) {
                        double score = mover.position().distanceToSqr(candidate) + Math.abs(candidate.y - mover.getY()) * 2.0;
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
      Level level = mover.level();
      BlockState supportState = level.getBlockState(supportPos);
      VoxelShape supportShape = supportState.getCollisionShape(level, supportPos);
      if (!supportShape.isEmpty() && !level.getFluidState(supportPos.above()).is(FluidTags.WATER)) {
         double supportTop = (double)supportPos.getY() + supportShape.max(Axis.Y);
         Vec3 destination = new Vec3((double)supportPos.getX() + 0.5, supportTop, (double)supportPos.getZ() + 0.5);
         return touchesWater(level, supportPos, supportTop) && isSafeBank(npc, mover, destination) ? destination : null;
      } else {
         return null;
      }
   }

   private static boolean touchesWater(Level level, BlockPos supportPos, double supportTop) {
      int y = (int)Math.floor(supportTop);

      for (Direction direction : Plane.HORIZONTAL) {
         BlockPos side = new BlockPos(supportPos.getX(), y, supportPos.getZ()).relative(direction);
         if (level.getFluidState(side).is(FluidTags.WATER) || level.getFluidState(side.below()).is(FluidTags.WATER)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isSafeBank(NpcBaseEntity npc, Entity mover, Vec3 destination) {
      Level level = mover.level();
      double dx = destination.x - mover.getX();
      double dy = destination.y - mover.getY();
      double dz = destination.z - mover.getZ();
      AABB moverBox = mover.getBoundingBox().move(dx, dy, dz);
      if (level.noCollision(mover, moverBox.deflate(1.0E-4)) && level.getWorldBorder().isWithinBounds(moverBox) && hasCentralSupport(level, moverBox)) {
         if (mover != npc) {
            AABB riderBox = npc.getBoundingBox().move(dx, dy, dz);
            if (!level.noCollision(npc, riderBox.deflate(1.0E-4))) {
               return false;
            }
         }

         BlockPos feet = BlockPos.containing(destination.x, destination.y + 0.05, destination.z);
         return !level.getFluidState(feet).is(FluidTags.WATER) && !level.getFluidState(feet.above()).is(FluidTags.WATER);
      } else {
         return false;
      }
   }

   private static boolean hasCentralSupport(Level level, AABB box) {
      return hasSupport(level, (box.minX + box.maxX) * 0.5, box.minY, (box.minZ + box.maxZ) * 0.5);
   }

   private static boolean hasSupport(Level level, double x, double footY, double z) {
      BlockPos pos = BlockPos.containing(x, footY - 0.05, z);
      VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
      if (shape.isEmpty()) {
         return false;
      } else {
         double top = (double)pos.getY() + shape.max(Axis.Y);
         return Math.abs(footY - top) <= 0.4;
      }
   }

   private static void swimToward(NpcBaseEntity npc, Entity mover, Vec3 destination) {
      Vec3 delta = destination.subtract(mover.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      Vec3 direction = horizontal.lengthSqr() < 1.0E-5 ? Vec3.ZERO : horizontal.normalize();
      double speed = mover == npc ? 0.18 : 0.22;
      Vec3 current = mover.getDeltaMovement();
      double upward = Math.max(current.y * 0.45, 0.08);
      if (delta.y > 0.45 || mover.horizontalCollision) {
         upward = 0.2;
      }

      if (delta.y < -1.5) {
         upward = Math.max(0.035, upward * 0.55);
      }

      mover.setDeltaMovement(direction.x * speed, Math.min(0.24, upward), direction.z * speed);
      mover.fallDistance = 0.0F;
      mover.hurtMarked = true;
      if (mover != npc) {
         npc.setDeltaMovement(Vec3.ZERO);
         npc.fallDistance = 0.0F;
      }

      face(npc, mover, direction);
   }

   private static void swimTowardCombatTarget(NpcBaseEntity npc, LivingEntity target) {
      Vec3 delta = target.position().add(0.0, (double)target.getBbHeight() * 0.35, 0.0).subtract(npc.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      Vec3 direction = horizontal.lengthSqr() < 1.0E-5 ? Vec3.ZERO : horizontal.normalize();
      double vertical = Math.max(-0.08, Math.min(0.08, delta.y * 0.12));
      if (npc.horizontalCollision && delta.y > 0.35) {
         vertical = 0.12;
      }

      npc.setDeltaMovement(direction.x * 0.16, vertical, direction.z * 0.16);
      npc.fallDistance = 0.0F;
      npc.hurtMarked = true;
      face(npc, npc, direction);
   }

   private static void walkToward(NpcBaseEntity npc, Entity mover, Vec3 destination) {
      Vec3 delta = destination.subtract(mover.position());
      Vec3 direction = new Vec3(delta.x, 0.0, delta.z).normalize();
      npc.getNavigation().stop();
      if (mover instanceof Mob mob) {
         mob.getNavigation().stop();
      }

      Vec3 current = mover.getDeltaMovement();
      mover.setDeltaMovement(direction.x * 0.2, current.y, direction.z * 0.2);
      mover.hurtMarked = true;
      face(npc, mover, direction);
   }

   private static void keepAfloat(Entity mover) {
      Vec3 current = mover.getDeltaMovement();
      mover.setDeltaMovement(current.x * 0.65, Math.max(current.y, 0.09), current.z * 0.65);
      mover.fallDistance = 0.0F;
      mover.hurtMarked = true;
   }

   private static void face(NpcBaseEntity npc, Entity mover, Vec3 direction) {
      if (!(direction.horizontalDistanceSqr() < 1.0E-5)) {
         float yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
         mover.setYRot(yaw);
         mover.setYBodyRot(yaw);
         mover.setYHeadRot(yaw);
         npc.setYRot(yaw);
         npc.setYBodyRot(yaw);
         npc.setYHeadRot(yaw);
      }
   }

   private static final class EscapeState {
      private Vec3 target;
      private long nextSearch;
      private Vec3 destination;
      private long destinationUntil;
      /** Spot a little past the bank to walk to once out of the water. */
      private Vec3 inland;
      private long inlandUntil;
   }
}
