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
               if (state.target == null
                  || gameTime >= state.nextSearch
                  || !isSafeBank(npc, mover, state.target)
                  || mover.position().distanceToSqr(state.target) <= 1.5625) {
                  state.target = findNearestBank(npc, mover);
                  state.nextSearch = gameTime + 15L;
               }

               npc.getNavigation().stop();
               if (mover instanceof Mob mob) {
                  mob.getNavigation().stop();
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
      Entity vehicle = npc.getVehicle();
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
   }
}
