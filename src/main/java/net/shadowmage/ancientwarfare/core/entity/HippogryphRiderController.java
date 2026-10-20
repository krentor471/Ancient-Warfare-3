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
      return rider != null && IceAndFireHippogryphCompat.isHippogryph(rider.m_20202_());
   }

   public static void tick(CombatNpcEntity rider) {
      if (!rider.m_9236_().f_46443_ && rider.m_9236_() instanceof ServerLevel) {
         Entity mount = rider.m_20202_();
         if (!IceAndFireHippogryphCompat.isHippogryph(mount)) {
            ACTIVE.remove(rider.m_20148_());
         } else {
            HippogryphRiderController.State state = ACTIVE.computeIfAbsent(
               rider.m_20148_(), id -> new HippogryphRiderController.State(rider.m_217043_().m_188500_() * Math.PI * 2.0)
            );
            PlayerOwnedNpcEntity commander = rider.getCombatGroupCommander().orElse(null);
            boolean groupMember = commander != null && commander.isHippogryphCombatGroupCommander();
            LivingEntity target = groupMember ? commander.m_5448_() : rider.m_5448_();
            if (rider.getCurrentCommand() != NpcCommandType.MOVE && !NpcTargeting.canHarm(rider, target) && !groupMember) {
               target = findTarget(rider);
               if (target != null) {
                  rider.m_6710_(target);
               }
            }

            if (rider.getCurrentCommand() != NpcCommandType.MOVE && NpcTargeting.canHarm(rider, target)) {
               state.angle = state.angle + (state.clockwise ? 0.08 : -0.08);
               boolean ranged = rider.m_21205_().m_41720_() instanceof BowItem || rider.m_21205_().m_41720_() instanceof CrossbowItem;
               if (ranged) {
                  IceAndFireHippogryphCompat.setFlying(mount, true);
                  Vec3 point = orbitPoint(target, state.angle);
                  moveTo(mount, point);
                  face(mount, orbitPoint(target, state.angle + (state.clockwise ? 0.15 : -0.15)));
                  if (rider.m_9236_().m_46467_() >= state.nextShot) {
                     rider.m_6504_(target, 1.0F);
                     state.nextShot = rider.m_9236_().m_46467_() + 28L + (long)rider.m_217043_().m_188503_(20);
                  }
               } else {
                  if (state.mode == HippogryphRiderController.Mode.ORBIT && rider.m_9236_().m_46467_() >= state.nextDive) {
                     state.mode = HippogryphRiderController.Mode.DIVE;
                     state.phaseTicks = 0;
                     state.phaseStart = mount.m_20182_();
                  }

                  if (state.mode == HippogryphRiderController.Mode.DIVE) {
                     state.phaseTicks++;
                     IceAndFireHippogryphCompat.setFlying(mount, true);
                     Vec3 point = target.m_20182_().m_82520_(0.0, (double)target.m_20206_() + 1.0, 0.0);
                     moveTo(mount, point);
                     face(mount, target.m_20182_());
                     if (state.phaseTicks >= 30) {
                        rider.m_7327_(target);
                        state.mode = HippogryphRiderController.Mode.ORBIT;
                        state.nextDive = rider.m_9236_().m_46467_() + 120L + (long)rider.m_217043_().m_188503_(160);
                     }
                  } else {
                     state.mode = HippogryphRiderController.Mode.ORBIT;
                     IceAndFireHippogryphCompat.setFlying(mount, true);
                     moveTo(mount, orbitPoint(target, state.angle));
                     face(mount, orbitPoint(target, state.angle + (state.clockwise ? 0.15 : -0.15)));
                  }
               }

               syncSeat(rider, mount);
               ACTIVE.put(rider.m_20148_(), state);
            } else {
               rider.m_6710_(null);
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
      ACTIVE.remove(rider.m_20148_());
      Entity mount = rider.m_20202_();
      if (IceAndFireHippogryphCompat.isHippogryph(mount)) {
         land(mount);
      }

      rider.m_8127_();
   }

   public static void postServerTick(ServerLevel level) {
      for (Entity entity : level.m_8583_()) {
         if (entity instanceof CombatNpcEntity) {
            CombatNpcEntity rider = (CombatNpcEntity)entity;
            if (isHippogryphRider(rider)) {
               Entity mount = rider.m_20202_();
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
         double distance = mount.m_20182_().m_82557_(destination);
         if (distance > 9.0) {
            IceAndFireHippogryphCompat.setFlying(mount, true);
            moveTo(mount, destination.m_82520_(0.0, 4.0, 0.0));
         } else {
            land(mount);
            moveTo(mount, destination);
         }

         mount.m_146922_(commander.m_146908_());
         mount.m_5618_(commander.m_146908_());
         mount.m_5616_(commander.m_146908_());
      }
   }

   private static void followPlayer(CombatNpcEntity rider, Entity mount) {
      Player player = rider.getFollowTargetPlayer().orElse(null);
      if (player == null) {
         land(mount);
      } else {
         double distanceSq = mount.m_20280_(player);
         if (distanceSq > 100.0) {
            IceAndFireHippogryphCompat.setFlying(mount, true);
            moveTo(mount, player.m_20182_().m_82520_(0.0, 4.0, 0.0));
            face(mount, player.m_20182_());
         } else if (distanceSq <= 9.0) {
            land(mount);
         }
      }
   }

   private static void moveToCommand(CombatNpcEntity rider, Entity mount) {
      BlockPos target = rider.getCommandTarget();
      if (BlockPos.f_121853_.equals(target)) {
         rider.clearCommand();
         land(mount);
      } else {
         Vec3 destination = Vec3.m_82539_(target);
         if (mount.m_20238_(destination) > 16.0) {
            IceAndFireHippogryphCompat.setFlying(mount, true);
            moveTo(mount, destination.m_82520_(0.0, 4.0, 0.0));
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
      mount.m_20256_(Vec3.f_82478_);
   }

   private static void moveTo(Entity mount, Vec3 point) {
      Vec3 delta = point.m_82546_(mount.m_20182_());
      if (delta.m_82556_() < 0.2) {
         mount.m_20256_(Vec3.f_82478_);
      } else {
         Vec3 velocity = delta.m_82541_().m_82490_(Math.min(0.35, delta.m_82553_()));
         mount.m_20256_(velocity);
         mount.m_6478_(MoverType.SELF, velocity);
      }
   }

   private static void syncSeat(CombatNpcEntity rider, Entity mount) {
      rider.m_146922_(mount.m_146908_());
      rider.m_5618_(mount.m_146908_());
      rider.m_5616_(mount.m_6080_());
      rider.f_19789_ = 0.0F;
   }

   private static Vec3 orbitPoint(LivingEntity target, double angle) {
      return target.m_20182_().m_82520_(Math.cos(angle) * 18.0, (double)target.m_20206_() + 8.0, Math.sin(angle) * 18.0);
   }

   private static void face(Entity mount, Vec3 point) {
      Vec3 delta = point.m_82546_(mount.m_20182_());
      if (delta.m_165925_() > 0.001) {
         float yaw = (float)Math.toDegrees(Math.atan2(-delta.f_82479_, delta.f_82481_));
         mount.m_146922_(yaw);
         mount.m_5618_(yaw);
         mount.m_5616_(yaw);
      }
   }

   private static LivingEntity findTarget(CombatNpcEntity rider) {
      return rider.m_9236_()
         .m_6443_(
            LivingEntity.class,
            rider.m_20191_().m_82377_(64.0, 32.0, 64.0),
            candidate -> candidate != rider && candidate.m_6084_() && isHostile(rider, candidate)
         )
         .stream()
         .min(Comparator.comparingDouble(rider::m_20280_))
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
      private Vec3 phaseStart = Vec3.f_82478_;

      private State(double angle) {
         this.angle = angle;
         this.clockwise = angle >= Math.PI;
         this.nextDive = 100L;
         this.nextShot = 20L;
      }
   }
}
