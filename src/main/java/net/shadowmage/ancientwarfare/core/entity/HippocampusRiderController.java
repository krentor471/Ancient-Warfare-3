package net.shadowmage.ancientwarfare.core.entity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippocampusCompat;

public final class HippocampusRiderController {
   private static final Map<UUID, HippocampusRiderController.State> ACTIVE = new HashMap<>();
   private static final double ORBIT_RADIUS = 18.0;
   private static final double SURFACE_OFFSET = -0.55;
   private static final double TARGET_RANGE = 35.0;
   private static final double TARGET_VERTICAL_RANGE = 18.0;

   private HippocampusRiderController() {
   }

   public static boolean isHippocampusRider(CombatNpcEntity rider) {
      return rider != null && IceAndFireHippocampusCompat.isHippocampus(rider.m_20202_());
   }

   public static void tick(CombatNpcEntity rider) {
      if (!rider.m_9236_().f_46443_ && rider.m_9236_() instanceof ServerLevel) {
         Entity mount = rider.m_20202_();
         if (!IceAndFireHippocampusCompat.isHippocampus(mount)) {
            ACTIVE.remove(rider.m_20148_());
         } else {
            IceAndFireHippocampusCompat.prepareForRider(mount);
            rider.m_21573_().m_26573_();
            rider.m_20256_(Vec3.f_82478_);
            HippocampusRiderController.State state = ACTIVE.computeIfAbsent(
               rider.m_20148_(), id -> new HippocampusRiderController.State(rider.m_217043_().m_188500_() * Math.PI * 2.0)
            );
            PlayerOwnedNpcEntity commander = rider.getCombatGroupCommander().orElse(null);
            boolean groupMember = commander != null && commander.isHippocampusCombatGroupCommander();
            LivingEntity target = groupMember ? commander.m_5448_() : rider.m_5448_();
            if (rider.getCurrentCommand() != NpcCommandType.MOVE && (!NpcTargeting.canHarm(rider, target) || rider.m_20280_(target) > 1225.0) && !groupMember) {
               target = findTarget(rider);
               if (target != null) {
                  rider.m_6710_(target);
               }
            }

            if (rider.getCurrentCommand() != NpcCommandType.MOVE && NpcTargeting.canHarm(rider, target) && (groupMember || !(rider.m_20280_(target) > 1225.0))) {
               boolean trident = rider.m_21205_().m_150930_(Items.f_42713_);
               boolean ranged = trident || rider.m_21205_().m_41720_() instanceof BowItem || rider.m_21205_().m_41720_() instanceof CrossbowItem;
               if (ranged && !target.m_20148_().equals(state.orbitTarget)) {
                  state.orbitTarget = target.m_20148_();
                  state.angle = Math.atan2(mount.m_20189_() - target.m_20189_(), mount.m_20185_() - target.m_20185_());
                  state.clockwise = rider.m_217043_().m_188499_();
               }

               if (trident) {
                  Vec3 point = rangedSurfaceOrbitPoint(mount, target, state);
                  moveSurface(mount, point);
                  face(mount, target.m_20182_());
                  if (rider.m_9236_().m_46467_() >= state.nextShot) {
                     throwTrident(rider, target);
                     state.nextShot = rider.m_9236_().m_46467_() + 35L + (long)rider.m_217043_().m_188503_(20);
                  }
               } else if (ranged) {
                  Vec3 point = rangedSurfaceOrbitPoint(mount, target, state);
                  moveSurface(mount, point);
                  face(mount, target.m_20182_());
                  if (rider.m_9236_().m_46467_() >= state.nextShot) {
                     rider.m_6504_(target, 1.0F);
                     state.nextShot = rider.m_9236_().m_46467_() + 30L + (long)rider.m_217043_().m_188503_(20);
                  }
               } else {
                  state.angle = state.angle + (state.clockwise ? 0.07 : -0.07);
                  if (state.mode == HippocampusRiderController.Mode.SURFACE && rider.m_9236_().m_46467_() >= state.nextDive) {
                     state.mode = HippocampusRiderController.Mode.DIVE;
                     state.phaseTicks = 0;
                     state.divePoint = new Vec3(target.m_20185_(), target.m_20186_() - 1.35, target.m_20189_());
                  }

                  if (state.mode == HippocampusRiderController.Mode.DIVE) {
                     state.phaseTicks++;
                     moveTo(mount, state.divePoint, 0.42);
                     face(mount, target.m_20182_());
                     if (state.phaseTicks >= 36 || mount.m_20182_().m_82557_(state.divePoint) < 9.0) {
                        rider.m_7327_(target);
                        state.mode = HippocampusRiderController.Mode.RETURN;
                        state.phaseTicks = 0;
                     }
                  } else if (state.mode == HippocampusRiderController.Mode.RETURN) {
                     state.phaseTicks++;
                     Vec3 point = surfaceOrbitPoint(mount, target, state.angle);
                     moveTo(mount, point, 0.38);
                     face(mount, target.m_20182_());
                     if (state.phaseTicks >= 36 || mount.m_20182_().m_82557_(point) < 6.0) {
                        state.mode = HippocampusRiderController.Mode.SURFACE;
                        state.nextDive = rider.m_9236_().m_46467_() + 120L + (long)rider.m_217043_().m_188503_(160);
                     }
                  } else {
                     moveSurface(mount, surfaceOrbitPoint(mount, target, state.angle));
                     face(mount, target.m_20182_());
                  }
               }

               syncSeat(rider, mount);
               ACTIVE.put(rider.m_20148_(), state);
            } else {
               rider.m_6710_(null);
               if (groupMember) {
                  followFormation(rider, mount, commander);
               } else if (rider.getCurrentCommand() == NpcCommandType.MOVE) {
                  moveToCommand(rider, mount);
               } else if (rider.getCurrentCommand() == NpcCommandType.FOLLOW) {
                  followPlayer(rider, mount);
               } else {
                  Vec3 waterGoal = ensureWaterGoal(mount, state);
                  if (waterGoal != null) {
                     moveTo(mount, waterGoal);
                  } else {
                     holdSurface(mount);
                  }
               }

               syncSeat(rider, mount);
            }
         }
      }
   }

   private static void throwTrident(CombatNpcEntity rider, LivingEntity target) {
      ItemStack stack = rider.m_21205_().m_41777_();
      Map<Enchantment, Integer> enchants = EnchantmentHelper.m_44831_(stack);
      enchants.remove(Enchantments.f_44955_);
      enchants.remove(Enchantments.f_44957_);
      EnchantmentHelper.m_44865_(enchants, stack);
      ThrownTrident projectile = new ThrownTrident(rider.m_9236_(), rider, stack);
      Vec3 aim = target.m_20182_().m_82520_(0.0, (double)target.m_20206_() * 0.55, 0.0);
      Vec3 origin = rider.m_20182_().m_82520_(0.0, (double)rider.m_20206_() * 0.62, 0.0);
      projectile.m_6034_(origin.f_82479_, origin.f_82480_, origin.f_82481_);
      Vec3 direction = aim.m_82546_(origin);
      double horizontal = Math.sqrt(direction.f_82479_ * direction.f_82479_ + direction.f_82481_ * direction.f_82481_);
      projectile.m_6686_(direction.f_82479_, direction.f_82480_ + horizontal * 0.08, direction.f_82481_, 2.5F, 1.0F);
      projectile.f_36705_ = Pickup.DISALLOWED;
      rider.m_9236_().m_7967_(projectile);
      rider.m_21011_(InteractionHand.MAIN_HAND, true);
   }

   public static void detach(CombatNpcEntity rider) {
      ACTIVE.remove(rider.m_20148_());
      if (isHippocampusRider(rider)) {
         Entity mount = rider.m_20202_();
         rider.m_8127_();
         IceAndFireHippocampusCompat.restoreAfterRider(mount);
      }
   }

   public static void postServerTick(ServerLevel level) {
      for (Entity entity : level.m_8583_()) {
         if (entity instanceof CombatNpcEntity) {
            CombatNpcEntity rider = (CombatNpcEntity)entity;
            if (isHippocampusRider(rider)) {
               syncSeat(rider, rider.m_20202_());
            }
         }
      }
   }

   private static void followFormation(CombatNpcEntity rider, Entity mount, PlayerOwnedNpcEntity commander) {
      Vec3 destination = rider.getCombatGroupFormationPosition().orElse(null);
      if (destination != null) {
         moveSurface(mount, destination);
         mount.m_146922_(commander.m_146908_());
         mount.m_5618_(commander.m_146908_());
         mount.m_5616_(commander.m_146908_());
      }
   }

   private static void followPlayer(CombatNpcEntity rider, Entity mount) {
      Player player = rider.getFollowTargetPlayer().orElse(null);
      if (player == null) {
         holdSurface(mount);
      } else {
         if (mount.m_20280_(player) > 100.0) {
            Vec3 destination = new Vec3(player.m_20185_(), waterSurfaceY(mount) + -0.55, player.m_20189_());
            moveSurface(mount, destination);
            face(mount, player.m_20182_());
         } else {
            holdSurface(mount);
         }
      }
   }

   private static void moveToCommand(CombatNpcEntity rider, Entity mount) {
      BlockPos target = rider.getCommandTarget();
      if (BlockPos.f_121853_.equals(target)) {
         rider.clearCommand();
         holdSurface(mount);
      } else {
         Vec3 destination = new Vec3((double)target.m_123341_() + 0.5, waterSurfaceY(mount) + -0.55, (double)target.m_123343_() + 0.5);
         if (mount.m_20238_(destination) > 16.0) {
            moveSurface(mount, destination);
            face(mount, destination);
         } else {
            holdSurface(mount);
            rider.clearCommand();
         }
      }
   }

   private static void moveSurface(Entity mount, Vec3 point) {
      moveTo(mount, point);
   }

   private static void holdSurface(Entity mount) {
      double surface = waterSurfaceY(mount);
      double y = Double.isNaN(surface) ? mount.m_20186_() : surface + -0.55;
      moveTo(mount, new Vec3(mount.m_20185_(), y, mount.m_20189_()));
   }

   private static void moveTo(Entity mount, Vec3 point) {
      moveTo(mount, point, 0.28);
   }

   private static void moveTo(Entity mount, Vec3 point, double maxSpeed) {
      Vec3 delta = point.m_82546_(mount.m_20182_());
      if (delta.m_82556_() < 0.15) {
         mount.m_20256_(Vec3.f_82478_);
      } else {
         Vec3 velocity = delta.m_82541_().m_82490_(Math.min(maxSpeed, delta.m_82553_()));
         mount.m_20256_(velocity);
         mount.m_6478_(MoverType.SELF, velocity);
         mount.m_20256_(Vec3.f_82478_);
      }
   }

   private static void syncSeat(CombatNpcEntity rider, Entity mount) {
      rider.m_146922_(mount.m_146908_());
      rider.m_5618_(mount.m_146908_());
      rider.m_5616_(mount.m_6080_());
      rider.f_19789_ = 0.0F;
   }

   private static Vec3 surfaceOrbitPoint(Entity mount, LivingEntity target, double angle) {
      double x = target.m_20185_() + Math.cos(angle) * 18.0;
      double z = target.m_20189_() + Math.sin(angle) * 18.0;
      Vec3 local = waterPointNear(mount, x, z, 10);
      if (local != null) {
         return local;
      } else {
         Vec3 nearest = findNearestWater(mount, 32);
         return nearest != null ? nearest : new Vec3(mount.m_20185_(), mount.m_20186_(), mount.m_20189_());
      }
   }

   private static Vec3 rangedSurfaceOrbitPoint(Entity mount, LivingEntity target, HippocampusRiderController.State state) {
      double step = state.clockwise ? 0.07 : -0.07;
      double nextAngle = state.angle + step;
      Vec3 point = strictSurfaceOrbitPoint(mount, target, nextAngle);
      if (point != null && hasWaterRoute(mount, point)) {
         state.angle = nextAngle;
         return point;
      } else {
         state.clockwise = !state.clockwise;
         nextAngle = state.angle + (state.clockwise ? 0.07 : -0.07);
         point = strictSurfaceOrbitPoint(mount, target, nextAngle);
         if (point != null && hasWaterRoute(mount, point)) {
            state.angle = nextAngle;
            return point;
         } else {
            double surface = waterSurfaceY(mount);
            double y = Double.isNaN(surface) ? mount.m_20186_() : surface + -0.55;
            return new Vec3(mount.m_20185_(), y, mount.m_20189_());
         }
      }
   }

   private static Vec3 strictSurfaceOrbitPoint(Entity mount, LivingEntity target, double angle) {
      double x = target.m_20185_() + Math.cos(angle) * 18.0;
      double z = target.m_20189_() + Math.sin(angle) * 18.0;
      return waterPointNear(mount, x, z, 0);
   }

   private static boolean hasWaterRoute(Entity mount, Vec3 destination) {
      Vec3 start = mount.m_20182_();
      double horizontalDistance = Math.sqrt(destination.m_82531_(start.f_82479_, destination.f_82480_, start.f_82481_));
      int samples = Math.max(1, (int)Math.ceil(horizontalDistance / 0.5));

      for (int i = 1; i <= samples; i++) {
         double progress = (double)i / (double)samples;
         double x = start.f_82479_ + (destination.f_82479_ - start.f_82479_) * progress;
         double z = start.f_82481_ + (destination.f_82481_ - start.f_82481_) * progress;
         if (waterPointNear(mount, x, z, 0) == null) {
            return false;
         }
      }

      return true;
   }

   private static Vec3 ensureWaterGoal(Entity mount, HippocampusRiderController.State state) {
      if (waterSurfaceY(mount) == waterSurfaceY(mount)) {
         state.waterGoal = null;
         return null;
      } else {
         long time = mount.m_9236_().m_46467_();
         if (state.waterGoal == null || time >= state.nextWaterSearch) {
            state.waterGoal = findNearestWater(mount, 32);
            state.nextWaterSearch = time + 20L;
         }

         return state.waterGoal;
      }
   }

   private static Vec3 findNearestWater(Entity mount, int radius) {
      Vec3 best = null;
      double bestDistance = Double.MAX_VALUE;
      BlockPos center = BlockPos.m_274561_(mount.m_20185_(), mount.m_20186_(), mount.m_20189_());

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            BlockPos column = center.m_7918_(dx, 0, dz);

            for (int dy = 4; dy >= -6; dy--) {
               BlockPos pos = column.m_7918_(0, dy, 0);
               FluidState fluid = mount.m_9236_().m_6425_(pos);
               if (fluid.m_205070_(FluidTags.f_13131_)) {
                  BlockPos above = pos.m_7494_();
                  if (mount.m_9236_().m_6425_(above).m_76178_()) {
                     Vec3 point = new Vec3(
                        (double)pos.m_123341_() + 0.5,
                        (double)((float)pos.m_123342_() + fluid.m_76155_(mount.m_9236_(), pos)) + -0.55,
                        (double)pos.m_123343_() + 0.5
                     );
                     double distance = mount.m_20182_().m_82557_(point);
                     if (distance < bestDistance) {
                        bestDistance = distance;
                        best = point;
                     }
                     break;
                  }
               }
            }
         }
      }

      return best;
   }

   private static Vec3 waterPointNear(Entity mount, double x, double z, int radius) {
      BlockPos center = BlockPos.m_274561_(x, mount.m_20186_(), z);
      Vec3 best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            for (int dy = 4; dy >= -6; dy--) {
               BlockPos pos = center.m_7918_(dx, dy, dz);
               FluidState fluid = mount.m_9236_().m_6425_(pos);
               if (fluid.m_205070_(FluidTags.f_13131_) && mount.m_9236_().m_6425_(pos.m_7494_()).m_76178_()) {
                  Vec3 point = new Vec3(
                     (double)pos.m_123341_() + 0.5,
                     (double)((float)pos.m_123342_() + fluid.m_76155_(mount.m_9236_(), pos)) + -0.55,
                     (double)pos.m_123343_() + 0.5
                  );
                  double distance = point.m_82531_(x, point.f_82480_, z);
                  if (distance < bestDistance) {
                     bestDistance = distance;
                     best = point;
                  }
                  break;
               }
            }
         }
      }

      return best;
   }

   private static double waterSurfaceY(Entity mount) {
      BlockPos base = BlockPos.m_274561_(mount.m_20185_(), mount.m_20186_(), mount.m_20189_());

      for (int dy = 1; dy >= -2; dy--) {
         BlockPos pos = base.m_7918_(0, dy, 0);
         FluidState fluid = mount.m_9236_().m_6425_(pos);
         if (fluid.m_205070_(FluidTags.f_13131_)) {
            return (double)((float)pos.m_123342_() + fluid.m_76155_(mount.m_9236_(), pos));
         }
      }

      return Double.NaN;
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
            rider.m_20191_().m_82377_(35.0, 18.0, 35.0),
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
      SURFACE,
      DIVE,
      RETURN;
   }

   private static final class State {
      private boolean clockwise;
      private double angle;
      private UUID orbitTarget;
      private HippocampusRiderController.Mode mode = HippocampusRiderController.Mode.SURFACE;
      private int phaseTicks;
      private long nextDive = 100L;
      private long nextShot = 20L;
      private long nextWaterSearch;
      private Vec3 waterGoal;
      private Vec3 divePoint = Vec3.f_82478_;

      private State(double angle) {
         this.angle = angle;
         this.clockwise = angle >= Math.PI;
      }
   }
}
