package net.shadowmage.ancientwarfare.core.entity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireDragonCompat;

public final class DragonRiderController {
   private static final Map<UUID, DragonRiderController.State> ACTIVE = new HashMap<>();
   private static final double ORBIT_RADIUS = 32.0;
   private static final double ORBIT_HEIGHT = 10.0;
   private static final double MIN_FLIGHT_GROUND_CLEARANCE = 6.0;
   private static final double MIN_TRANSIT_GROUND_CLEARANCE = 2.5;
   private static final int MAX_TARGET_COVER_BLOCKS = 3;
   private static final int DRAGON_GRIEF_INTERVAL_TICKS = 5;
   private static final int TAKEOFF_TICKS = 40;
   private static final int ALIGN_TICKS = 20;
   private static final int DIVE_TICKS = 28;
   private static final int CLIMB_TICKS = 36;
   private static final int BREATH_APPROACH_TICKS = 24;
   private static final int BREATH_TICKS = 80;

   private DragonRiderController() {
   }

   public static boolean attach(PlayerOwnedNpcEntity rider, Entity dragon) {
      if (!IceAndFireDragonCompat.isDragon(dragon)) {
         return false;
      } else {
         IceAndFireDragonCompat.detachRealPassenger(rider);
         if (!IceAndFireDragonCompat.canBeMountedBy(dragon, rider.getOwner().orElse(null))) {
            return false;
         } else {
            if (dragon instanceof Mob mob) {
               mob.setPersistenceRequired();
            }

            IceAndFireDragonCompat.prepareForRider(dragon);
            IceAndFireDragonCompat.stopNativeAI(dragon);
            rider.setDragonRiderMount(dragon);
            ACTIVE.put(
               rider.getUUID(), new DragonRiderController.State(dragon.getUUID(), rider.level().getGameTime(), rider.getRandom().nextDouble() * Math.PI * 2.0)
            );
            return true;
         }
      }
   }

   public static void detach(CombatNpcEntity rider) {
      if (rider != null) {
         Entity dragon = findMount(rider);
         if (IceAndFireDragonCompat.isDragon(dragon)) {
            IceAndFireDragonCompat.clearNativeTarget(dragon);
            IceAndFireDragonCompat.setTackling(dragon, false);
            IceAndFireDragonCompat.setBreathingFire(dragon, false, null);
            IceAndFireDragonCompat.setFlying(dragon, false);
            IceAndFireDragonCompat.setHovering(dragon, false);
            dragon.setDeltaMovement(Vec3.ZERO);
         }

         rider.clearDragonRiderMount();
         rider.setNoGravity(false);
         rider.fallDistance = 0.0F;
         ACTIVE.remove(rider.getUUID());
      }
   }

   public static void tick(CombatNpcEntity rider) {
      if (!rider.level().isClientSide && rider.level() instanceof ServerLevel level) {
         Entity vehicle = rider.isLogicalDragonRider() ? findMount(rider) : rider.getVehicle();
         if (vehicle != null && IceAndFireDragonCompat.isDragon(vehicle) && rider.getDragonRiderMountId() < 0) {
            rider.setDragonRiderMountId(vehicle.getId());
            AncientWarfareCore.LOG.info("Restored dragon rider {} -> dragon {}", rider.getId(), vehicle.getUUID());
         }

         if (IceAndFireDragonCompat.isDragon(rider.getVehicle())) {
            Entity realVehicle = rider.getVehicle();
            IceAndFireDragonCompat.detachRealPassenger(rider);
            if (!rider.isLogicalDragonRider()) {
               attach(rider, realVehicle);
            }

            vehicle = findMount(rider);
         }

         DragonRiderController.State state = ACTIVE.get(rider.getUUID());
         if (state == null) {
            if (!IceAndFireDragonCompat.isDragon(vehicle)) {
               if (rider.getDragonRiderMountUuid() != null) {
                  ACTIVE.put(
                     rider.getUUID(),
                     new DragonRiderController.State(rider.getDragonRiderMountUuid(), rider.level().getGameTime(), rider.getRandom().nextDouble() * Math.PI * 2.0)
                  );
                  rider.setTarget(null);
                  rider.setNoGravity(false);
                  return;
               }

               return;
            }

            IceAndFireDragonCompat.prepareForRider(vehicle);
            IceAndFireDragonCompat.stopNativeAI(vehicle);
            rider.setDragonRiderMount(vehicle);
            state = new DragonRiderController.State(vehicle.getUUID(), rider.level().getGameTime(), rider.getRandom().nextDouble() * Math.PI * 2.0);
            ACTIVE.put(rider.getUUID(), state);
            AncientWarfareCore.LOG.info("Restored dragon rider {} -> dragon {}", rider.getId(), vehicle.getUUID());
         }

         if (IceAndFireDragonCompat.isDragon(vehicle) && vehicle.isAlive() && rider.isAlive()) {
            IceAndFireDragonCompat.clearNativeTarget(vehicle);
            Vec3 seat = IceAndFireDragonCompat.riderPosition(vehicle);
            rider.setPos(seat.x, seat.y, seat.z);
            rider.fallDistance = 0.0F;
            rider.setDeltaMovement(Vec3.ZERO);
            rider.setNoGravity(true);
            rider.setYRot(vehicle.getYRot());
            rider.setYBodyRot(vehicle.getYRot());
            rider.setYHeadRot(vehicle.getYRot());
            PlayerOwnedNpcEntity groupCommander = rider.getCombatGroupCommander().orElse(null);
            boolean dragonGroupMember = groupCommander != null && groupCommander.isDragonRiderCombatGroupCommander();
            LivingEntity target = dragonGroupMember ? groupCommander.getTarget() : rider.getTarget();
            if (!dragonGroupMember && rider.getCurrentCommand() != NpcCommandType.MOVE && !NpcTargeting.canHarm(rider, target)) {
               target = findHostileTarget(rider);
               if (target != null) {
                  rider.setTarget(target);
               }
            } else if (dragonGroupMember && NpcTargeting.canHarm(rider, target)) {
               rider.setTarget(target);
            }

            if (NpcTargeting.canHarm(rider, target) && isSurfaceReachableTarget(level, target)) {
               LivingEntity attackTarget = dragonGroupMember ? findNearbyAttackTarget(rider, target) : target;
               if (attackTarget == null) {
                  attackTarget = target;
               }

               state.attackTargetId = attackTarget.getUUID();
               state.angle = state.angle + (state.clockwise ? 0.075 : -0.075);
               if (state.mode == DragonRiderController.Mode.GROUND) {
                  state.mode = DragonRiderController.Mode.TAKEOFF;
                  state.phaseTicks = 0;
                  state.phaseStart = vehicle.position();
                  state.nextDive = rider.level().getGameTime() + 240L + (long)rider.getRandom().nextInt(240);
                  AncientWarfareCore.LOG.info("Dragon rider {} entering ORBIT around target {}", rider.getId(), target.getId());
                  IceAndFireDragonCompat.setFlying(vehicle, true);
                  IceAndFireDragonCompat.setHovering(vehicle, false);
               }

               if (state.mode == DragonRiderController.Mode.TAKEOFF) {
                  state.phaseTicks++;
                  if (state.phaseTicks >= 40) {
                     state.mode = DragonRiderController.Mode.ORBIT;
                     state.phaseTicks = 0;
                     state.nextDive = rider.level().getGameTime() + 240L + (long)rider.getRandom().nextInt(240);
                     AncientWarfareCore.LOG.info("Dragon rider {} entering ORBIT around target {}", rider.getId(), target.getId());
                  }
               } else if (state.mode == DragonRiderController.Mode.ORBIT) {
                  IceAndFireDragonCompat.setFlying(vehicle, true);
                  IceAndFireDragonCompat.setHovering(vehicle, false);
                  if (isRangedDragonRider(rider) && rider.level().getGameTime() >= state.nextRangedAttack) {
                     rider.performRangedAttack(attackTarget, 1.0F);
                     state.nextRangedAttack = rider.level().getGameTime() + 30L + (long)rider.getRandom().nextInt(25);
                     AncientWarfareCore.LOG.info("Dragon rider {} fired ranged attack at target {}", rider.getId(), target.getId());
                  }

                  if (rider.level().getGameTime() >= state.nextDive) {
                     state.mode = DragonRiderController.Mode.ALIGN;
                     state.phaseTicks = 0;
                     state.phaseStart = vehicle.position();
                  }
               } else if (state.mode == DragonRiderController.Mode.ALIGN) {
                  state.phaseTicks++;
                  if (state.phaseTicks >= 20) {
                     boolean forcedSwitch = state.lastAttack != DragonRiderController.Attack.NONE && state.consecutiveAttacks >= 3;
                     state.breathAttack = forcedSwitch ? state.lastAttack != DragonRiderController.Attack.BREATH : rider.getRandom().nextBoolean();
                     state.mode = state.breathAttack ? DragonRiderController.Mode.BREATH_APPROACH : DragonRiderController.Mode.DIVE;
                     state.phaseTicks = 0;
                     if (!state.breathAttack) {
                        if (state.lastAttack == DragonRiderController.Attack.DIVE) {
                           state.consecutiveAttacks++;
                        } else {
                           state.consecutiveAttacks = 1;
                        }

                        state.lastAttack = DragonRiderController.Attack.DIVE;
                        AncientWarfareCore.LOG.info("Dragon rider {} entering DIVE on target {}", rider.getId(), target.getId());
                        IceAndFireDragonCompat.setTackling(vehicle, true);
                     } else {
                        if (state.lastAttack == DragonRiderController.Attack.BREATH) {
                           state.consecutiveAttacks++;
                        } else {
                           state.consecutiveAttacks = 1;
                        }

                        state.lastAttack = DragonRiderController.Attack.BREATH;
                        AncientWarfareCore.LOG.info("Dragon rider {} entering BREATH on target {}", rider.getId(), target.getId());
                        IceAndFireDragonCompat.setTackling(vehicle, false);
                     }
                  }
               } else if (state.mode == DragonRiderController.Mode.BREATH_APPROACH) {
                  state.phaseTicks++;
                  if (state.phaseTicks >= 24) {
                     state.mode = DragonRiderController.Mode.BREATH;
                     state.phaseTicks = 0;
                  }
               } else if (state.mode == DragonRiderController.Mode.BREATH) {
                  state.phaseTicks++;
                  if (state.phaseTicks == 1) {
                     IceAndFireDragonCompat.startBreathingFire(vehicle);
                  }

                  if (state.phaseTicks >= 80) {
                     state.mode = DragonRiderController.Mode.CLIMB;
                     state.phaseTicks = 0;
                     state.phaseStart = vehicle.position();
                     IceAndFireDragonCompat.setBreathingFire(vehicle, false, null);
                     IceAndFireDragonCompat.setFlying(vehicle, true);
                  }
               } else if (state.mode == DragonRiderController.Mode.DIVE) {
                  state.phaseTicks++;
                  if (state.phaseTicks == 18) {
                     IceAndFireDragonCompat.bite(vehicle, attackTarget);
                     rider.swing(InteractionHand.MAIN_HAND, true);
                     rider.doHurtTarget(attackTarget);
                  }

                  if (state.phaseTicks >= 28) {
                     state.mode = DragonRiderController.Mode.CLIMB;
                     state.phaseTicks = 0;
                     state.phaseStart = vehicle.position();
                     IceAndFireDragonCompat.setTackling(vehicle, false);
                     IceAndFireDragonCompat.setBreathingFire(vehicle, false, null);
                     IceAndFireDragonCompat.setFlying(vehicle, true);
                  }
               } else if (state.mode == DragonRiderController.Mode.CLIMB) {
                  state.phaseTicks++;
                  if (state.phaseTicks >= 36) {
                     state.mode = DragonRiderController.Mode.ORBIT;
                     state.phaseTicks = 0;
                     state.nextDive = rider.level().getGameTime() + 300L + (long)rider.getRandom().nextInt(300);
                  }
               }

               ACTIVE.put(rider.getUUID(), state);
            } else {
               rider.setTarget(null);
               state.mode = DragonRiderController.Mode.GROUND;
               IceAndFireDragonCompat.setTackling(vehicle, false);
               IceAndFireDragonCompat.setBreathingFire(vehicle, false, null);
               IceAndFireDragonCompat.setFlying(vehicle, false);
               IceAndFireDragonCompat.setHovering(vehicle, false);
               if (!dragonGroupMember) {
                  vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.55, 0.8, 0.55));
               }

               ACTIVE.put(rider.getUUID(), state);
            }
         } else if (rider.isAlive() && rider.getDragonRiderMountUuid() != null && vehicle == null) {
            rider.setNoGravity(false);
            rider.setTarget(null);
         } else {
            rider.clearDragonRiderMount();
            rider.setNoGravity(false);
            rider.fallDistance = 0.0F;
            ACTIVE.remove(rider.getUUID());
         }
      }
   }

   public static void postServerTick(ServerLevel level) {
      ACTIVE.entrySet()
         .removeIf(
            entry -> {
               Entity entity = null;

               for (Entity candidate : level.getAllEntities()) {
                  if (candidate.getUUID().equals(entry.getKey())) {
                     entity = candidate;
                     break;
                  }
               }

               if (entity == null) {
                  return false;
               } else {
                  if (entity instanceof CombatNpcEntity rider && rider.isAlive()) {
                     Entity dragon = rider.isLogicalDragonRider() ? findMount(rider) : rider.getVehicle();
                     if (!IceAndFireDragonCompat.isDragon(dragon)) {
                        return rider.getDragonRiderMountUuid() == null;
                     }

                     if (rider.getDragonRiderMountId() < 0) {
                        rider.setDragonRiderMountId(dragon.getId());
                     }

                     IceAndFireDragonCompat.clearNativeTarget(dragon);
                     DragonRiderController.State state = ACTIVE.get(rider.getUUID());
                     LivingEntity target = rider.getTarget();
                     PlayerOwnedNpcEntity groupCommander = rider.getCombatGroupCommander().orElse(null);
                     boolean dragonGroupMember = groupCommander != null && groupCommander.isDragonRiderCombatGroupCommander();
                     if (dragonGroupMember && !NpcTargeting.canHarm(rider, groupCommander.getTarget())) {
                        tickFormationMovement(rider, dragon, groupCommander, state);
                        syncRiderSeat(rider, dragon);
                        return false;
                     }

                     if (!dragonGroupMember && rider.getCurrentCommand() == NpcCommandType.MOVE) {
                        rider.setTarget(null);
                        BlockPos commandTarget = rider.getCommandTarget();
                        if (!BlockPos.ZERO.equals(commandTarget)) {
                           Vec3 destination = Vec3.atBottomCenterOf(commandTarget).add(0.0, 5.0, 0.0);
                           if (dragon.position().distanceToSqr(destination) > 16.0) {
                              IceAndFireDragonCompat.setFlying(dragon, true);
                              IceAndFireDragonCompat.setHovering(dragon, false);
                              IceAndFireDragonCompat.setFlightTarget(dragon, destination);
                              moveDragon(dragon, destination, rider, 0.0);
                           } else {
                              IceAndFireDragonCompat.setFlying(dragon, false);
                              IceAndFireDragonCompat.setHovering(dragon, false);
                              dragon.setDeltaMovement(Vec3.ZERO);
                              rider.clearCommand();
                           }

                           syncRiderSeat(rider, dragon);
                           return false;
                        }
                     }

                     if (!dragonGroupMember && !NpcTargeting.canHarm(rider, target) && rider.getCurrentCommand() == NpcCommandType.FOLLOW) {
                        Player player = rider.getFollowTargetPlayer().orElse(null);
                        if (player != null && dragon.distanceToSqr(player) > 100.0) {
                           Vec3 destination = player.position().add(0.0, 5.0, 0.0);
                           IceAndFireDragonCompat.setFlying(dragon, true);
                           IceAndFireDragonCompat.setHovering(dragon, false);
                           IceAndFireDragonCompat.setFlightTarget(dragon, destination);
                           moveDragon(dragon, destination, player, 0.0);
                           dragon.hasImpulse = true;
                        } else {
                           IceAndFireDragonCompat.setFlying(dragon, false);
                           IceAndFireDragonCompat.setHovering(dragon, false);
                           dragon.setDeltaMovement(Vec3.ZERO);
                        }

                        syncRiderSeat(rider, dragon);
                        return false;
                     }

                     if (state != null && NpcTargeting.canHarm(rider, target) && isSurfaceReachableTarget(level, target)) {
                        LivingEntity attackTarget = target;
                        if (state.attackTargetId != null
                           && level.getEntity(state.attackTargetId) instanceof LivingEntity living
                           && NpcTargeting.canHarm(rider, living)) {
                           attackTarget = living;
                        }

                        Vec3 orbit = orbitPoint(level, dragon, target, state.angle);
                        Vec3 point;
                        if (state.mode == DragonRiderController.Mode.ALIGN) {
                           point = state.phaseStart;
                        } else if (state.mode == DragonRiderController.Mode.TAKEOFF || state.mode == DragonRiderController.Mode.CLIMB) {
                           double duration = state.mode == DragonRiderController.Mode.TAKEOFF ? 40.0 : 36.0;
                           double progress = Math.min(1.0, (double)state.phaseTicks / duration);
                           point = safeFlightPoint(level, dragon, lerp(state.phaseStart, orbit, progress), Mth.lerp(progress, 2.5, 6.0));
                        } else if (state.mode == DragonRiderController.Mode.BREATH_APPROACH || state.mode == DragonRiderController.Mode.BREATH) {
                           Vec3 toward = dragon.position().subtract(attackTarget.position());
                           Vec3 horizontal = toward.horizontalDistanceSqr() > 0.001 ? new Vec3(toward.x, 0.0, toward.z).normalize() : new Vec3(1.0, 0.0, 0.0);
                           Vec3 breathPoint = attackTarget.position()
                              .add(horizontal.scale(12.0))
                              .add(0.0, (double)attackTarget.getBbHeight() + 6.0, 0.0);
                           point = safeFlightPoint(level, dragon, breathPoint, 6.0);
                        } else if (state.mode == DragonRiderController.Mode.DIVE) {
                           Vec3 bitePoint = attackTarget.position().add(0.0, (double)attackTarget.getBbHeight() + 1.0, 0.0);
                           point = lerp(state.phaseStart, bitePoint, Math.min(1.0, (double)state.phaseTicks / 28.0));
                        } else {
                           point = orbit;
                        }

                        if (state.mode == DragonRiderController.Mode.ALIGN) {
                           point = safeFlightPoint(level, dragon, point, 6.0);
                        }

                        if (state.mode == DragonRiderController.Mode.ALIGN) {
                           IceAndFireDragonCompat.setFlightTarget(dragon, point);
                           dragon.setPos(point.x, point.y, point.z);
                           dragon.setDeltaMovement(Vec3.ZERO);
                           dragon.hasImpulse = true;
                        } else {
                           IceAndFireDragonCompat.setFlightTarget(dragon, point);
                           moveDragon(dragon, point, target, state.angle);
                        }

                        dragon.setPos(point.x, point.y, point.z);
                        dragon.setDeltaMovement(Vec3.ZERO);
                        dragon.hasImpulse = true;
                        if (state.mode == DragonRiderController.Mode.ALIGN) {
                           faceSmooth(dragon, targetAimPoint(target), 10.0F);
                        } else if (state.mode == DragonRiderController.Mode.BREATH_APPROACH) {
                           faceSmooth3d(dragon, targetAimPoint(attackTarget), 12.0F);
                        } else if (state.mode == DragonRiderController.Mode.BREATH) {
                           faceExact3d(dragon, targetAimPoint(attackTarget));
                        } else if (state.mode == DragonRiderController.Mode.DIVE) {
                           face(dragon, attackTarget.position());
                        } else if (state.mode == DragonRiderController.Mode.TAKEOFF || state.mode == DragonRiderController.Mode.CLIMB) {
                           face(dragon, point);
                        } else if (state.mode == DragonRiderController.Mode.ORBIT) {
                           face(dragon, orbitPoint(level, dragon, target, state.angle + (state.clockwise ? 0.12 : -0.12)));
                        }

                        if (state.mode == DragonRiderController.Mode.BREATH) {
                           IceAndFireDragonCompat.setBreathingFire(dragon, true, attackTarget);
                           IceAndFireDragonCompat.stimulateFireAt(dragon, targetAimPoint(attackTarget));
                        }
                     }

                     syncRiderSeat(rider, dragon);
                     return false;
                  }

                  return true;
               }
            }
         );
   }

   private static void syncRiderSeat(CombatNpcEntity rider, Entity dragon) {
      Vec3 seat = IceAndFireDragonCompat.riderPosition(dragon);
      rider.setPos(seat.x, seat.y, seat.z);
      rider.fallDistance = 0.0F;
      rider.setDeltaMovement(Vec3.ZERO);
      rider.setNoGravity(true);
   }

   private static void tickFormationMovement(CombatNpcEntity rider, Entity dragon, PlayerOwnedNpcEntity commander, DragonRiderController.State state) {
      if (state != null) {
         Vec3 destination = rider.getCombatGroupFormationPosition().orElse(null);
         if (destination == null) {
            state.formationMode = DragonRiderController.FormationMode.GROUND;
            IceAndFireDragonCompat.setFlying(dragon, false);
            IceAndFireDragonCompat.setHovering(dragon, false);
            dragon.setDeltaMovement(Vec3.ZERO);
         } else {
            double horizontalDistance = dragon.position().subtract(destination).horizontalDistance();
            if (state.formationMode == DragonRiderController.FormationMode.GROUND && horizontalDistance > 3.0) {
               state.formationMode = DragonRiderController.FormationMode.FORMATION_TAKEOFF;
               state.formationTicks = 0;
               state.formationStart = dragon.position();
               state.formationCruiseY = Math.max(dragon.getY(), destination.y) + 8.0;
            }

            Vec3 point;
            if (state.formationMode == DragonRiderController.FormationMode.FORMATION_TAKEOFF) {
               state.formationTicks++;
               double progress = Math.min(1.0, (double)state.formationTicks / 30.0);
               point = lerp(state.formationStart, new Vec3(state.formationStart.x, state.formationCruiseY, state.formationStart.z), progress);
               IceAndFireDragonCompat.setFlying(dragon, true);
               IceAndFireDragonCompat.setHovering(dragon, false);
               if (progress >= 1.0) {
                  state.formationMode = DragonRiderController.FormationMode.FORMATION_MOVE;
                  state.formationTicks = 0;
               }
            } else if (state.formationMode == DragonRiderController.FormationMode.FORMATION_MOVE) {
               point = new Vec3(destination.x, state.formationCruiseY, destination.z);
               IceAndFireDragonCompat.setFlying(dragon, true);
               IceAndFireDragonCompat.setHovering(dragon, false);
               if (dragon.position().distanceToSqr(point) < 9.0) {
                  state.formationMode = DragonRiderController.FormationMode.FORMATION_LAND;
                  state.formationTicks = 0;
                  state.formationStart = dragon.position();
               }
            } else if (state.formationMode == DragonRiderController.FormationMode.FORMATION_LAND) {
               state.formationTicks++;
               double progress = Math.min(1.0, (double)state.formationTicks / 30.0);
               point = lerp(state.formationStart, destination, progress);
               IceAndFireDragonCompat.setFlying(dragon, progress < 1.0);
               IceAndFireDragonCompat.setHovering(dragon, false);
               if (progress >= 1.0) {
                  state.formationMode = DragonRiderController.FormationMode.GROUND;
                  state.formationTicks = 0;
               }
            } else {
               point = destination;
               IceAndFireDragonCompat.setFlying(dragon, false);
               IceAndFireDragonCompat.setHovering(dragon, false);
            }

            IceAndFireDragonCompat.setFlightTarget(dragon, point);
            dragon.setPos(point.x, point.y, point.z);
            dragon.setDeltaMovement(Vec3.ZERO);
            dragon.hasImpulse = true;
            if (state.formationMode != DragonRiderController.FormationMode.GROUND) {
               breakFlightBlocks(dragon);
            }

            dragon.setYRot(commander.getYRot());
            dragon.setYBodyRot(commander.getYRot());
            dragon.setYHeadRot(commander.getYRot());
            dragon.setXRot(0.0F);
         }
      }
   }

   public static void postClientTick(Iterable<? extends Entity> entities) {
      for (Entity entity : entities) {
         if (entity instanceof CombatNpcEntity) {
            CombatNpcEntity rider = (CombatNpcEntity)entity;
            Entity dragon = rider.isLogicalDragonRider() ? findMount(rider) : rider.getVehicle();
            if (IceAndFireDragonCompat.isDragon(dragon)) {
               Vec3 seat = IceAndFireDragonCompat.riderPosition(dragon);
               rider.setPos(seat.x, seat.y, seat.z);
               rider.fallDistance = 0.0F;
               rider.setDeltaMovement(Vec3.ZERO);
               rider.setNoGravity(true);
            }
         }
      }
   }

   private static Vec3 orbitPoint(ServerLevel level, Entity dragon, LivingEntity target, double angle) {
      Vec3 requested = target.position().add(Math.cos(angle) * 32.0, (double)target.getBbHeight() + 10.0, Math.sin(angle) * 32.0);
      return safeFlightPoint(level, dragon, requested, 6.0);
   }

   private static Vec3 safeFlightPoint(ServerLevel level, Entity dragon, Vec3 requested, double clearance) {
      if (requested != null && Double.isFinite(requested.x) && Double.isFinite(requested.y) && Double.isFinite(requested.z)) {
         double safeY = Math.max(requested.y, (double)level.getMinBuildHeight() + 2.0);
         BlockPos column = BlockPos.containing(requested.x, safeY, requested.z);
         if (level.hasChunkAt(column)) {
            int surfaceY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            safeY = Math.max(safeY, (double)surfaceY + clearance);
         }

         double ceiling = (double)level.getMaxBuildHeight() - Math.max(4.0, (double)dragon.getBbHeight() + 2.0);
         safeY = Math.min(safeY, ceiling);
         return new Vec3(requested.x, safeY, requested.z);
      } else {
         return dragon.position();
      }
   }

   private static boolean isSurfaceReachableTarget(ServerLevel level, LivingEntity target) {
      BlockPos column = target.blockPosition();
      if (!level.hasChunkAt(column)) {
         return false;
      } else {
         int surfaceY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
         int firstY = Mth.ceil(target.getBoundingBox().maxY + 0.01);
         int coverBlocks = 0;
         MutableBlockPos cursor = new MutableBlockPos(column.getX(), firstY, column.getZ());

         for (int y = firstY; y < surfaceY; y++) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            boolean covered = state.isCollisionShapeFullBlock(level, cursor) || !state.getFluidState().isEmpty();
            if (covered) {
               if (++coverBlocks > 3) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public static boolean canAcquireTarget(CombatNpcEntity rider, LivingEntity target) {
      if (rider != null && target != null && rider.isLogicalDragonRider()) {
         if (rider.level() instanceof ServerLevel level && isSurfaceReachableTarget(level, target)) {
            return true;
         }

         return false;
      } else {
         return true;
      }
   }

   private static Entity findMount(PlayerOwnedNpcEntity rider) {
      UUID uuid = rider.getDragonRiderMountUuid();
      if (uuid == null) {
         return rider.level().getEntity(rider.getDragonRiderMountId());
      } else {
         if (rider.level() instanceof ServerLevel server) {
            for (ServerLevel level : server.getServer().getAllLevels()) {
               Entity entity = level.getEntity(uuid);
               if (entity != null) {
                  return entity;
               }
            }
         }

         return rider.level().getEntity(rider.getDragonRiderMountId());
      }
   }

   public static Entity getLogicalMount(PlayerOwnedNpcEntity rider) {
      if (rider != null && rider.isLogicalDragonRider()) {
         Entity mount = findMount(rider);
         return IceAndFireDragonCompat.isDragon(mount) && mount.isAlive() ? mount : null;
      } else {
         return null;
      }
   }

   private static void face(Entity dragon, Vec3 point) {
      Vec3 d = point.subtract(dragon.position());
      if (d.horizontalDistanceSqr() > 0.001) {
         dragon.setYRot((float)Math.toDegrees(Math.atan2(-d.x, d.z)));
         dragon.setYHeadRot(dragon.getYRot());
      }
   }

   private static void faceSmooth(Entity dragon, Vec3 point, float maxTurn) {
      Vec3 d = point.subtract(dragon.position());
      if (!(d.horizontalDistanceSqr() <= 0.001)) {
         float desired = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
         float current = dragon.getYRot();
         float delta = Mth.wrapDegrees(desired - current);
         float next = current + Mth.clamp(delta, -maxTurn, maxTurn);
         dragon.setYRot(next);
         dragon.setYBodyRot(next);
         dragon.setYHeadRot(next);
      }
   }

   private static void faceSmooth3d(Entity dragon, Vec3 point, float maxTurn) {
      Vec3 d = point.subtract(dragon.position());
      double horizontal = Math.max(0.001, d.horizontalDistance());
      float desiredYaw = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
      float desiredPitch = (float)(-Math.toDegrees(Math.atan2(d.y, horizontal)));
      float yaw = dragon.getYRot() + Mth.clamp(Mth.wrapDegrees(desiredYaw - dragon.getYRot()), -maxTurn, maxTurn);
      float pitch = dragon.getXRot() + Mth.clamp(Mth.wrapDegrees(desiredPitch - dragon.getXRot()), -maxTurn, maxTurn);
      dragon.setYRot(yaw);
      dragon.setYBodyRot(yaw);
      dragon.setYHeadRot(yaw);
      dragon.setXRot(pitch);
   }

   private static void faceExact3d(Entity dragon, Vec3 point) {
      Vec3 d = point.subtract(dragon.position());
      double horizontal = Math.max(0.001, d.horizontalDistance());
      float yaw = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
      float pitch = (float)(-Math.toDegrees(Math.atan2(d.y, horizontal)));
      dragon.setYRot(yaw);
      dragon.setYBodyRot(yaw);
      dragon.setYHeadRot(yaw);
      dragon.setXRot(pitch);
   }

   private static Vec3 targetAimPoint(LivingEntity target) {
      return target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0);
   }

   private static void moveDragon(Entity dragon, Vec3 point, LivingEntity target, double angle) {
      Vec3 current = dragon.position();
      Vec3 delta = point.subtract(current);
      double distance = delta.length();
      double maxSpeed = 0.34;
      Vec3 desired = distance < 0.05 ? Vec3.ZERO : delta.scale(Math.min(maxSpeed / distance, 1.0));
      Vec3 old = dragon.getDeltaMovement();
      Vec3 velocity = old.scale(0.82).add(desired.scale(0.18));
      if (velocity.lengthSqr() > maxSpeed * maxSpeed) {
         velocity = velocity.normalize().scale(maxSpeed);
      }

      dragon.setNoGravity(true);
      dragon.setDeltaMovement(velocity);
      dragon.move(MoverType.SELF, velocity);
      breakFlightBlocks(dragon);
      Vec3 tangent = velocity.horizontalDistanceSqr() > 1.0E-5 ? velocity : new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
      dragon.setYRot((float)Math.toDegrees(Math.atan2(-tangent.x, tangent.z)));
      dragon.setYHeadRot(dragon.getYRot());
   }

   private static void breakFlightBlocks(Entity dragon) {
      if (dragon.tickCount % 5 == 0) {
         IceAndFireDragonCompat.breakNearbyBlocks(dragon);
      }
   }

   private static boolean isHostile(CombatNpcEntity rider, LivingEntity target) {
      return NpcTargeting.canHarm(rider, target)
         && (NpcTargeting.isAutomaticHostile(rider, target) || target instanceof FactionNpcEntity faction && faction.isHostileTowards(rider));
   }

   private static LivingEntity findHostileTarget(CombatNpcEntity rider) {
      AABB search = rider.getBoundingBox().inflate(96.0, 48.0, 96.0);
      return rider.level()
         .getEntitiesOfClass(
            LivingEntity.class,
            search,
            candidate -> candidate != rider
                  && candidate.isAlive()
                  && isHostile(rider, candidate)
                  && (!(rider.level() instanceof ServerLevel server) || isSurfaceReachableTarget(server, candidate))
         )
         .stream()
         .min(Comparator.comparingDouble(rider::distanceToSqr))
         .orElse(null);
   }

   private static LivingEntity findNearbyAttackTarget(CombatNpcEntity rider, LivingEntity anchor) {
      AABB search = anchor.getBoundingBox().inflate(10.0);
      return rider.level()
         .getEntitiesOfClass(
            LivingEntity.class,
            search,
            candidate -> candidate != rider
                  && candidate != anchor
                  && candidate.isAlive()
                  && isHostile(rider, candidate)
                  && (!(rider.level() instanceof ServerLevel server) || isSurfaceReachableTarget(server, candidate))
         )
         .stream()
         .min(Comparator.comparingDouble(rider::distanceToSqr))
         .orElse(anchor);
   }

   private static Vec3 lerp(Vec3 from, Vec3 to, double amount) {
      return from.lerp(to, amount);
   }

   private static boolean isRangedDragonRider(CombatNpcEntity rider) {
      return rider.getMainHandItem().getItem() instanceof BowItem || rider.getMainHandItem().getItem() instanceof CrossbowItem;
   }

   private static enum Attack {
      NONE,
      DIVE,
      BREATH;
   }

   private static enum FormationMode {
      GROUND,
      FORMATION_TAKEOFF,
      FORMATION_MOVE,
      FORMATION_LAND;
   }

   private static enum Mode {
      GROUND,
      TAKEOFF,
      ORBIT,
      ALIGN,
      BREATH_APPROACH,
      BREATH,
      DIVE,
      CLIMB;
   }

   private static final class State {
      private final UUID dragonId;
      private final boolean clockwise;
      private double angle;
      private DragonRiderController.Mode mode = DragonRiderController.Mode.GROUND;
      private long nextDive;
      private long nextRangedAttack;
      private int phaseTicks;
      private Vec3 phaseStart = Vec3.ZERO;
      private boolean breathAttack;
      private DragonRiderController.Attack lastAttack = DragonRiderController.Attack.NONE;
      private int consecutiveAttacks;
      private UUID attackTargetId;
      private DragonRiderController.FormationMode formationMode = DragonRiderController.FormationMode.GROUND;
      private int formationTicks;
      private double formationCruiseY;
      private Vec3 formationStart = Vec3.ZERO;

      private State(UUID dragonId, long ignored, double angle) {
         this.dragonId = dragonId;
         this.angle = angle;
         this.clockwise = angle >= Math.PI;
      }
   }
}
