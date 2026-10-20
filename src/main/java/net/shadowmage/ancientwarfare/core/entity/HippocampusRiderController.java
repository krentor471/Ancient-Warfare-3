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
      return rider != null && IceAndFireHippocampusCompat.isHippocampus(rider.getVehicle());
   }

   public static void tick(CombatNpcEntity rider) {
      if (!rider.level().isClientSide && rider.level() instanceof ServerLevel) {
         Entity mount = rider.getVehicle();
         if (!IceAndFireHippocampusCompat.isHippocampus(mount)) {
            ACTIVE.remove(rider.getUUID());
         } else {
            IceAndFireHippocampusCompat.prepareForRider(mount);
            rider.getNavigation().stop();
            rider.setDeltaMovement(Vec3.ZERO);
            HippocampusRiderController.State state = ACTIVE.computeIfAbsent(
               rider.getUUID(), id -> new HippocampusRiderController.State(rider.getRandom().nextDouble() * Math.PI * 2.0)
            );
            PlayerOwnedNpcEntity commander = rider.getCombatGroupCommander().orElse(null);
            boolean groupMember = commander != null && commander.isHippocampusCombatGroupCommander();
            LivingEntity target = groupMember ? commander.getTarget() : rider.getTarget();
            if (rider.getCurrentCommand() != NpcCommandType.MOVE && (!NpcTargeting.canHarm(rider, target) || rider.distanceToSqr(target) > 1225.0) && !groupMember) {
               target = findTarget(rider);
               if (target != null) {
                  rider.setTarget(target);
               }
            }

            if (rider.getCurrentCommand() != NpcCommandType.MOVE && NpcTargeting.canHarm(rider, target) && (groupMember || !(rider.distanceToSqr(target) > 1225.0))) {
               boolean trident = rider.getMainHandItem().is(Items.TRIDENT);
               boolean ranged = trident || rider.getMainHandItem().getItem() instanceof BowItem || rider.getMainHandItem().getItem() instanceof CrossbowItem;
               if (ranged && !target.getUUID().equals(state.orbitTarget)) {
                  state.orbitTarget = target.getUUID();
                  state.angle = Math.atan2(mount.getZ() - target.getZ(), mount.getX() - target.getX());
                  state.clockwise = rider.getRandom().nextBoolean();
               }

               if (trident) {
                  Vec3 point = rangedSurfaceOrbitPoint(mount, target, state);
                  moveSurface(mount, point);
                  face(mount, target.position());
                  if (rider.level().getGameTime() >= state.nextShot) {
                     throwTrident(rider, target);
                     state.nextShot = rider.level().getGameTime() + 35L + (long)rider.getRandom().nextInt(20);
                  }
               } else if (ranged) {
                  Vec3 point = rangedSurfaceOrbitPoint(mount, target, state);
                  moveSurface(mount, point);
                  face(mount, target.position());
                  if (rider.level().getGameTime() >= state.nextShot) {
                     rider.performRangedAttack(target, 1.0F);
                     state.nextShot = rider.level().getGameTime() + 30L + (long)rider.getRandom().nextInt(20);
                  }
               } else {
                  state.angle = state.angle + (state.clockwise ? 0.07 : -0.07);
                  if (state.mode == HippocampusRiderController.Mode.SURFACE && rider.level().getGameTime() >= state.nextDive) {
                     state.mode = HippocampusRiderController.Mode.DIVE;
                     state.phaseTicks = 0;
                     state.divePoint = new Vec3(target.getX(), target.getY() - 1.35, target.getZ());
                  }

                  if (state.mode == HippocampusRiderController.Mode.DIVE) {
                     state.phaseTicks++;
                     moveTo(mount, state.divePoint, 0.42);
                     face(mount, target.position());
                     if (state.phaseTicks >= 36 || mount.position().distanceToSqr(state.divePoint) < 9.0) {
                        rider.doHurtTarget(target);
                        state.mode = HippocampusRiderController.Mode.RETURN;
                        state.phaseTicks = 0;
                     }
                  } else if (state.mode == HippocampusRiderController.Mode.RETURN) {
                     state.phaseTicks++;
                     Vec3 point = surfaceOrbitPoint(mount, target, state.angle);
                     moveTo(mount, point, 0.38);
                     face(mount, target.position());
                     if (state.phaseTicks >= 36 || mount.position().distanceToSqr(point) < 6.0) {
                        state.mode = HippocampusRiderController.Mode.SURFACE;
                        state.nextDive = rider.level().getGameTime() + 120L + (long)rider.getRandom().nextInt(160);
                     }
                  } else {
                     moveSurface(mount, surfaceOrbitPoint(mount, target, state.angle));
                     face(mount, target.position());
                  }
               }

               syncSeat(rider, mount);
               ACTIVE.put(rider.getUUID(), state);
            } else {
               rider.setTarget(null);
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
      ItemStack stack = rider.getMainHandItem().copy();
      Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
      enchants.remove(Enchantments.LOYALTY);
      enchants.remove(Enchantments.RIPTIDE);
      EnchantmentHelper.setEnchantments(enchants, stack);
      ThrownTrident projectile = new ThrownTrident(rider.level(), rider, stack);
      Vec3 aim = target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0);
      Vec3 origin = rider.position().add(0.0, (double)rider.getBbHeight() * 0.62, 0.0);
      projectile.setPos(origin.x, origin.y, origin.z);
      Vec3 direction = aim.subtract(origin);
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      projectile.shoot(direction.x, direction.y + horizontal * 0.08, direction.z, 2.5F, 1.0F);
      projectile.pickup = Pickup.DISALLOWED;
      rider.level().addFreshEntity(projectile);
      rider.swing(InteractionHand.MAIN_HAND, true);
   }

   public static void detach(CombatNpcEntity rider) {
      ACTIVE.remove(rider.getUUID());
      if (isHippocampusRider(rider)) {
         Entity mount = rider.getVehicle();
         rider.stopRiding();
         IceAndFireHippocampusCompat.restoreAfterRider(mount);
      }
   }

   public static void postServerTick(ServerLevel level) {
      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof CombatNpcEntity) {
            CombatNpcEntity rider = (CombatNpcEntity)entity;
            if (isHippocampusRider(rider)) {
               syncSeat(rider, rider.getVehicle());
            }
         }
      }
   }

   private static void followFormation(CombatNpcEntity rider, Entity mount, PlayerOwnedNpcEntity commander) {
      Vec3 destination = rider.getCombatGroupFormationPosition().orElse(null);
      if (destination != null) {
         moveSurface(mount, destination);
         mount.setYRot(commander.getYRot());
         mount.setYBodyRot(commander.getYRot());
         mount.setYHeadRot(commander.getYRot());
      }
   }

   private static void followPlayer(CombatNpcEntity rider, Entity mount) {
      Player player = rider.getFollowTargetPlayer().orElse(null);
      if (player == null) {
         holdSurface(mount);
      } else {
         if (mount.distanceToSqr(player) > 100.0) {
            Vec3 destination = new Vec3(player.getX(), waterSurfaceY(mount) + -0.55, player.getZ());
            moveSurface(mount, destination);
            face(mount, player.position());
         } else {
            holdSurface(mount);
         }
      }
   }

   private static void moveToCommand(CombatNpcEntity rider, Entity mount) {
      BlockPos target = rider.getCommandTarget();
      if (BlockPos.ZERO.equals(target)) {
         rider.clearCommand();
         holdSurface(mount);
      } else {
         Vec3 destination = new Vec3((double)target.getX() + 0.5, waterSurfaceY(mount) + -0.55, (double)target.getZ() + 0.5);
         if (mount.distanceToSqr(destination) > 16.0) {
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
      double y = Double.isNaN(surface) ? mount.getY() : surface + -0.55;
      moveTo(mount, new Vec3(mount.getX(), y, mount.getZ()));
   }

   private static void moveTo(Entity mount, Vec3 point) {
      moveTo(mount, point, 0.28);
   }

   private static void moveTo(Entity mount, Vec3 point, double maxSpeed) {
      Vec3 delta = point.subtract(mount.position());
      if (delta.lengthSqr() < 0.15) {
         mount.setDeltaMovement(Vec3.ZERO);
      } else {
         Vec3 velocity = delta.normalize().scale(Math.min(maxSpeed, delta.length()));
         mount.setDeltaMovement(velocity);
         mount.move(MoverType.SELF, velocity);
         mount.setDeltaMovement(Vec3.ZERO);
      }
   }

   private static void syncSeat(CombatNpcEntity rider, Entity mount) {
      rider.setYRot(mount.getYRot());
      rider.setYBodyRot(mount.getYRot());
      rider.setYHeadRot(mount.getYHeadRot());
      rider.fallDistance = 0.0F;
   }

   private static Vec3 surfaceOrbitPoint(Entity mount, LivingEntity target, double angle) {
      double x = target.getX() + Math.cos(angle) * 18.0;
      double z = target.getZ() + Math.sin(angle) * 18.0;
      Vec3 local = waterPointNear(mount, x, z, 10);
      if (local != null) {
         return local;
      } else {
         Vec3 nearest = findNearestWater(mount, 32);
         return nearest != null ? nearest : new Vec3(mount.getX(), mount.getY(), mount.getZ());
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
            double y = Double.isNaN(surface) ? mount.getY() : surface + -0.55;
            return new Vec3(mount.getX(), y, mount.getZ());
         }
      }
   }

   private static Vec3 strictSurfaceOrbitPoint(Entity mount, LivingEntity target, double angle) {
      double x = target.getX() + Math.cos(angle) * 18.0;
      double z = target.getZ() + Math.sin(angle) * 18.0;
      return waterPointNear(mount, x, z, 0);
   }

   private static boolean hasWaterRoute(Entity mount, Vec3 destination) {
      Vec3 start = mount.position();
      double horizontalDistance = Math.sqrt(destination.distanceToSqr(start.x, destination.y, start.z));
      int samples = Math.max(1, (int)Math.ceil(horizontalDistance / 0.5));

      for (int i = 1; i <= samples; i++) {
         double progress = (double)i / (double)samples;
         double x = start.x + (destination.x - start.x) * progress;
         double z = start.z + (destination.z - start.z) * progress;
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
         long time = mount.level().getGameTime();
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
      BlockPos center = BlockPos.containing(mount.getX(), mount.getY(), mount.getZ());

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            BlockPos column = center.offset(dx, 0, dz);

            for (int dy = 4; dy >= -6; dy--) {
               BlockPos pos = column.offset(0, dy, 0);
               FluidState fluid = mount.level().getFluidState(pos);
               if (fluid.is(FluidTags.WATER)) {
                  BlockPos above = pos.above();
                  if (mount.level().getFluidState(above).isEmpty()) {
                     Vec3 point = new Vec3(
                        (double)pos.getX() + 0.5,
                        (double)((float)pos.getY() + fluid.getHeight(mount.level(), pos)) + -0.55,
                        (double)pos.getZ() + 0.5
                     );
                     double distance = mount.position().distanceToSqr(point);
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
      BlockPos center = BlockPos.containing(x, mount.getY(), z);
      Vec3 best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            for (int dy = 4; dy >= -6; dy--) {
               BlockPos pos = center.offset(dx, dy, dz);
               FluidState fluid = mount.level().getFluidState(pos);
               if (fluid.is(FluidTags.WATER) && mount.level().getFluidState(pos.above()).isEmpty()) {
                  Vec3 point = new Vec3(
                     (double)pos.getX() + 0.5,
                     (double)((float)pos.getY() + fluid.getHeight(mount.level(), pos)) + -0.55,
                     (double)pos.getZ() + 0.5
                  );
                  double distance = point.distanceToSqr(x, point.y, z);
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
      BlockPos base = BlockPos.containing(mount.getX(), mount.getY(), mount.getZ());

      for (int dy = 1; dy >= -2; dy--) {
         BlockPos pos = base.offset(0, dy, 0);
         FluidState fluid = mount.level().getFluidState(pos);
         if (fluid.is(FluidTags.WATER)) {
            return (double)((float)pos.getY() + fluid.getHeight(mount.level(), pos));
         }
      }

      return Double.NaN;
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
            rider.getBoundingBox().inflate(35.0, 18.0, 35.0),
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
      private Vec3 divePoint = Vec3.ZERO;

      private State(double angle) {
         this.angle = angle;
         this.clockwise = angle >= Math.PI;
      }
   }
}
