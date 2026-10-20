package net.shadowmage.ancientwarfare.core.entity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippogryphCompat;

public final class HippogryphRiderController {
   private static final Map<UUID, HippogryphRiderController.State> ACTIVE = new HashMap<>();
   private static final double ORBIT_RADIUS = 18.0;
   private static final double ORBIT_HEIGHT = 8.0;

   private HippogryphRiderController() {
   }

   public static boolean isHippogryphRider(CombatNpcEntity rider) {
      return rider != null && IceAndFireHippogryphCompat.isHippogryph(rider.getVehicle());
   }

   public static void tick(CombatNpcEntity rider) {
      if (!rider.level().isClientSide && rider.level() instanceof ServerLevel) {
         Entity mount = rider.getVehicle();
         if (!IceAndFireHippogryphCompat.isHippogryph(mount)) {
            ACTIVE.remove(rider.getUUID());
         } else {
            HippogryphRiderController.State state = ACTIVE.computeIfAbsent(
               rider.getUUID(), id -> new HippogryphRiderController.State(rider.getRandom().nextDouble() * Math.PI * 2.0)
            );
            PlayerOwnedNpcEntity commander = rider.getCombatGroupCommander().orElse(null);
            boolean groupMember = commander != null && commander.isHippogryphCombatGroupCommander();
            LivingEntity target = groupMember ? commander.getTarget() : rider.getTarget();
            if (rider.getCurrentCommand() != NpcCommandType.MOVE && !NpcTargeting.canHarm(rider, target) && !groupMember) {
               target = findTarget(rider);
               if (target != null) {
                  rider.setTarget(target);
               }
            }

            if (rider.getCurrentCommand() != NpcCommandType.MOVE && NpcTargeting.canHarm(rider, target)) {
               state.angle = state.angle + (state.clockwise ? 0.08 : -0.08);
               boolean ranged = rider.getMainHandItem().getItem() instanceof BowItem || rider.getMainHandItem().getItem() instanceof CrossbowItem;
               if (ranged) {
                  IceAndFireHippogryphCompat.setFlying(mount, true);
                  Vec3 point = orbitPoint(target, state.angle);
                  moveTo(mount, point);
                  face(mount, orbitPoint(target, state.angle + (state.clockwise ? 0.15 : -0.15)));
                  if (rider.level().getGameTime() >= state.nextShot) {
                     rider.performRangedAttack(target, 1.0F);
                     state.nextShot = rider.level().getGameTime() + 28L + (long)rider.getRandom().nextInt(20);
                  }
               } else {
                  if (state.mode == HippogryphRiderController.Mode.ORBIT && rider.level().getGameTime() >= state.nextDive) {
                     state.mode = HippogryphRiderController.Mode.DIVE;
                     state.phaseTicks = 0;
                     state.phaseStart = mount.position();
                  }

                  if (state.mode == HippogryphRiderController.Mode.DIVE) {
                     state.phaseTicks++;
                     IceAndFireHippogryphCompat.setFlying(mount, true);
                     Vec3 point = target.position().add(0.0, (double)target.getBbHeight() + 1.0, 0.0);
                     moveTo(mount, point);
                     face(mount, target.position());
                     if (state.phaseTicks >= 30) {
                        rider.doHurtTarget(target);
                        state.mode = HippogryphRiderController.Mode.ORBIT;
                        state.nextDive = rider.level().getGameTime() + 120L + (long)rider.getRandom().nextInt(160);
                     }
                  } else {
                     state.mode = HippogryphRiderController.Mode.ORBIT;
                     IceAndFireHippogryphCompat.setFlying(mount, true);
                     moveTo(mount, orbitPoint(target, state.angle));
                     face(mount, orbitPoint(target, state.angle + (state.clockwise ? 0.15 : -0.15)));
                  }
               }

               syncSeat(rider, mount);
               ACTIVE.put(rider.getUUID(), state);
            } else {
               rider.setTarget(null);
               if (groupMember) {
                  followFormation(rider, mount, commander, state);
               } else if (rider.getCurrentCommand() == NpcCommandType.MOVE) {
                  moveToCommand(rider, mount);
               } else if (rider.getCurrentCommand() == NpcCommandType.FOLLOW) {
                  followPlayer(rider, mount);
               } else {
                  land(mount);
               }

               syncSeat(rider, mount);
            }
         }
      }
   }

   public static void detach(CombatNpcEntity rider) {
      ACTIVE.remove(rider.getUUID());
      Entity mount = rider.getVehicle();
      if (IceAndFireHippogryphCompat.isHippogryph(mount)) {
         land(mount);
      }

      rider.stopRiding();
   }

   public static void postServerTick(ServerLevel level) {
      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof CombatNpcEntity) {
            CombatNpcEntity rider = (CombatNpcEntity)entity;
            if (isHippogryphRider(rider)) {
               Entity mount = rider.getVehicle();
               if (mount != null) {
                  syncSeat(rider, mount);
               }
            }
         }
      }
   }

   private static void followFormation(CombatNpcEntity rider, Entity mount, PlayerOwnedNpcEntity commander, HippogryphRiderController.State state) {
      Vec3 destination = rider.getCombatGroupFormationPosition().orElse(null);
      if (destination == null) {
         land(mount);
      } else {
         double distance = mount.position().distanceToSqr(destination);
         if (distance > 9.0) {
            IceAndFireHippogryphCompat.setFlying(mount, true);
            moveTo(mount, destination.add(0.0, 4.0, 0.0));
         } else {
            land(mount);
            moveTo(mount, destination);
         }

         mount.setYRot(commander.getYRot());
         mount.setYBodyRot(commander.getYRot());
         mount.setYHeadRot(commander.getYRot());
      }
   }

   private static void followPlayer(CombatNpcEntity rider, Entity mount) {
      Player player = rider.getFollowTargetPlayer().orElse(null);
      if (player == null) {
         land(mount);
      } else {
         double distanceSq = mount.distanceToSqr(player);
         if (distanceSq > 100.0) {
            IceAndFireHippogryphCompat.setFlying(mount, true);
            moveTo(mount, player.position().add(0.0, 4.0, 0.0));
            face(mount, player.position());
         } else if (distanceSq <= 9.0) {
            land(mount);
         }
      }
   }

   private static void moveToCommand(CombatNpcEntity rider, Entity mount) {
      BlockPos target = rider.getCommandTarget();
      if (BlockPos.ZERO.equals(target)) {
         rider.clearCommand();
         land(mount);
      } else {
         Vec3 destination = Vec3.atBottomCenterOf(target);
         if (mount.distanceToSqr(destination) > 16.0) {
            IceAndFireHippogryphCompat.setFlying(mount, true);
            moveTo(mount, destination.add(0.0, 4.0, 0.0));
            face(mount, destination);
         } else {
            land(mount);
            rider.clearCommand();
         }
      }
   }

   private static void land(Entity mount) {
      IceAndFireHippogryphCompat.setFlying(mount, false);
      IceAndFireHippogryphCompat.setHovering(mount, false);
      mount.setDeltaMovement(Vec3.ZERO);
   }

   private static void moveTo(Entity mount, Vec3 point) {
      Vec3 delta = point.subtract(mount.position());
      if (delta.lengthSqr() < 0.2) {
         mount.setDeltaMovement(Vec3.ZERO);
      } else {
         Vec3 velocity = delta.normalize().scale(Math.min(0.35, delta.length()));
         mount.setDeltaMovement(velocity);
         mount.move(MoverType.SELF, velocity);
      }
   }

   private static void syncSeat(CombatNpcEntity rider, Entity mount) {
      rider.setYRot(mount.getYRot());
      rider.setYBodyRot(mount.getYRot());
      rider.setYHeadRot(mount.getYHeadRot());
      rider.fallDistance = 0.0F;
   }

   private static Vec3 orbitPoint(LivingEntity target, double angle) {
      return target.position().add(Math.cos(angle) * 18.0, (double)target.getBbHeight() + 8.0, Math.sin(angle) * 18.0);
   }

   private static void face(Entity mount, Vec3 point) {
      Vec3 delta = point.subtract(mount.position());
      if (delta.horizontalDistanceSqr() > 0.001) {
         float yaw = (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
         mount.setYRot(yaw);
         mount.setYBodyRot(yaw);
         mount.setYHeadRot(yaw);
      }
   }

   private static LivingEntity findTarget(CombatNpcEntity rider) {
      return rider.level()
         .getEntitiesOfClass(
            LivingEntity.class,
            rider.getBoundingBox().inflate(64.0, 32.0, 64.0),
            candidate -> candidate != rider && candidate.isAlive() && isHostile(rider, candidate)
         )
         .stream()
         .min(Comparator.comparingDouble(rider::distanceToSqr))
         .orElse(null);
   }

   private static boolean isHostile(CombatNpcEntity rider, LivingEntity target) {
      return NpcTargeting.canHarm(rider, target)
         && (NpcTargeting.isAutomaticHostile(rider, target) || target instanceof FactionNpcEntity faction && faction.isHostileTowards(rider));
   }

   private static enum Mode {
      ORBIT,
      DIVE;
   }

   private static final class State {
      private final boolean clockwise;
      private double angle;
      private HippogryphRiderController.Mode mode = HippogryphRiderController.Mode.ORBIT;
      private int phaseTicks;
      private long nextDive;
      private long nextShot;
      private Vec3 phaseStart = Vec3.ZERO;

      private State(double angle) {
         this.angle = angle;
         this.clockwise = angle >= Math.PI;
         this.nextDive = 100L;
         this.nextShot = 20L;
      }
   }
}
