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
               mob.m_21530_();
            }

            IceAndFireDragonCompat.prepareForRider(dragon);
            IceAndFireDragonCompat.stopNativeAI(dragon);
            rider.setDragonRiderMount(dragon);
            ACTIVE.put(
               rider.m_20148_(), new DragonRiderController.State(dragon.m_20148_(), rider.m_9236_().m_46467_(), rider.m_217043_().m_188500_() * Math.PI * 2.0)
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
            dragon.m_20256_(Vec3.f_82478_);
         }

         rider.clearDragonRiderMount();
         rider.m_20242_(false);
         rider.f_19789_ = 0.0F;
         ACTIVE.remove(rider.m_20148_());
      }
   }

   public static void tick(CombatNpcEntity rider) {
      if (!rider.m_9236_().f_46443_ && rider.m_9236_() instanceof ServerLevel level) {
         Entity vehicle = rider.isLogicalDragonRider() ? findMount(rider) : rider.m_20202_();
         if (vehicle != null && IceAndFireDragonCompat.isDragon(vehicle) && rider.getDragonRiderMountId() < 0) {
            rider.setDragonRiderMountId(vehicle.m_19879_());
            AncientWarfareCore.LOG.info("Restored dragon rider {} -> dragon {}", rider.m_19879_(), vehicle.m_20148_());
         }

         if (IceAndFireDragonCompat.isDragon(rider.m_20202_())) {
            Entity realVehicle = rider.m_20202_();
            IceAndFireDragonCompat.detachRealPassenger(rider);
            if (!rider.isLogicalDragonRider()) {
               attach(rider, realVehicle);
            }

            vehicle = findMount(rider);
         }

         DragonRiderController.State state = ACTIVE.get(rider.m_20148_());
         if (state == null) {
            if (!IceAndFireDragonCompat.isDragon(vehicle)) {
               if (rider.getDragonRiderMountUuid() != null) {
                  ACTIVE.put(
                     rider.m_20148_(),
                     new DragonRiderController.State(rider.getDragonRiderMountUuid(), rider.m_9236_().m_46467_(), rider.m_217043_().m_188500_() * Math.PI * 2.0)
                  );
                  rider.m_6710_(null);
                  rider.m_20242_(false);
                  return;
               }

               return;
            }

            IceAndFireDragonCompat.prepareForRider(vehicle);
            IceAndFireDragonCompat.stopNativeAI(vehicle);
            rider.setDragonRiderMount(vehicle);
            state = new DragonRiderController.State(vehicle.m_20148_(), rider.m_9236_().m_46467_(), rider.m_217043_().m_188500_() * Math.PI * 2.0);
            ACTIVE.put(rider.m_20148_(), state);
            AncientWarfareCore.LOG.info("Restored dragon rider {} -> dragon {}", rider.m_19879_(), vehicle.m_20148_());
         }

         if (IceAndFireDragonCompat.isDragon(vehicle) && vehicle.m_6084_() && rider.m_6084_()) {
            IceAndFireDragonCompat.clearNativeTarget(vehicle);
            Vec3 seat = IceAndFireDragonCompat.riderPosition(vehicle);
            rider.m_6034_(seat.f_82479_, seat.f_82480_, seat.f_82481_);
            rider.f_19789_ = 0.0F;
            rider.m_20256_(Vec3.f_82478_);
            rider.m_20242_(true);
            rider.m_146922_(vehicle.m_146908_());
            rider.m_5618_(vehicle.m_146908_());
            rider.m_5616_(vehicle.m_146908_());
            PlayerOwnedNpcEntity groupCommander = rider.getCombatGroupCommander().orElse(null);
            boolean dragonGroupMember = groupCommander != null && groupCommander.isDragonRiderCombatGroupCommander();
            LivingEntity target = dragonGroupMember ? groupCommander.m_5448_() : rider.m_5448_();
            if (!dragonGroupMember && rider.getCurrentCommand() != NpcCommandType.MOVE && !NpcTargeting.canHarm(rider, target)) {
               target = findHostileTarget(rider);
               if (target != null) {
                  rider.m_6710_(target);
               }
            } else if (dragonGroupMember && NpcTargeting.canHarm(rider, target)) {
               rider.m_6710_(target);
            }

            if (NpcTargeting.canHarm(rider, target) && isSurfaceReachableTarget(level, target)) {
               LivingEntity attackTarget = dragonGroupMember ? findNearbyAttackTarget(rider, target) : target;
               if (attackTarget == null) {
                  attackTarget = target;
               }

               state.attackTargetId = attackTarget.m_20148_();
               state.angle = state.angle + (state.clockwise ? 0.075 : -0.075);
               if (state.mode == DragonRiderController.Mode.GROUND) {
                  state.mode = DragonRiderController.Mode.TAKEOFF;
                  state.phaseTicks = 0;
                  state.phaseStart = vehicle.m_20182_();
                  state.nextDive = rider.m_9236_().m_46467_() + 240L + (long)rider.m_217043_().m_188503_(240);
                  AncientWarfareCore.LOG.info("Dragon rider {} entering ORBIT around target {}", rider.m_19879_(), target.m_19879_());
                  IceAndFireDragonCompat.setFlying(vehicle, true);
                  IceAndFireDragonCompat.setHovering(vehicle, false);
               }

               if (state.mode == DragonRiderController.Mode.TAKEOFF) {
                  state.phaseTicks++;
                  if (state.phaseTicks >= 40) {
                     state.mode = DragonRiderController.Mode.ORBIT;
                     state.phaseTicks = 0;
                     state.nextDive = rider.m_9236_().m_46467_() + 240L + (long)rider.m_217043_().m_188503_(240);
                     AncientWarfareCore.LOG.info("Dragon rider {} entering ORBIT around target {}", rider.m_19879_(), target.m_19879_());
                  }
               } else if (state.mode == DragonRiderController.Mode.ORBIT) {
                  IceAndFireDragonCompat.setFlying(vehicle, true);
                  IceAndFireDragonCompat.setHovering(vehicle, false);
                  if (isRangedDragonRider(rider) && rider.m_9236_().m_46467_() >= state.nextRangedAttack) {
                     rider.m_6504_(attackTarget, 1.0F);
                     state.nextRangedAttack = rider.m_9236_().m_46467_() + 30L + (long)rider.m_217043_().m_188503_(25);
                     AncientWarfareCore.LOG.info("Dragon rider {} fired ranged attack at target {}", rider.m_19879_(), target.m_19879_());
                  }

                  if (rider.m_9236_().m_46467_() >= state.nextDive) {
                     state.mode = DragonRiderController.Mode.ALIGN;
                     state.phaseTicks = 0;
                     state.phaseStart = vehicle.m_20182_();
                  }
               } else if (state.mode == DragonRiderController.Mode.ALIGN) {
                  state.phaseTicks++;
                  if (state.phaseTicks >= 20) {
                     boolean forcedSwitch = state.lastAttack != DragonRiderController.Attack.NONE && state.consecutiveAttacks >= 3;
                     state.breathAttack = forcedSwitch ? state.lastAttack != DragonRiderController.Attack.BREATH : rider.m_217043_().m_188499_();
                     state.mode = state.breathAttack ? DragonRiderController.Mode.BREATH_APPROACH : DragonRiderController.Mode.DIVE;
                     state.phaseTicks = 0;
                     if (!state.breathAttack) {
                        if (state.lastAttack == DragonRiderController.Attack.DIVE) {
                           state.consecutiveAttacks++;
                        } else {
                           state.consecutiveAttacks = 1;
                        }

                        state.lastAttack = DragonRiderController.Attack.DIVE;
                        AncientWarfareCore.LOG.info("Dragon rider {} entering DIVE on target {}", rider.m_19879_(), target.m_19879_());
                        IceAndFireDragonCompat.setTackling(vehicle, true);
                     } else {
                        if (state.lastAttack == DragonRiderController.Attack.BREATH) {
                           state.consecutiveAttacks++;
                        } else {
                           state.consecutiveAttacks = 1;
                        }

                        state.lastAttack = DragonRiderController.Attack.BREATH;
                        AncientWarfareCore.LOG.info("Dragon rider {} entering BREATH on target {}", rider.m_19879_(), target.m_19879_());
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
                     state.phaseStart = vehicle.m_20182_();
                     IceAndFireDragonCompat.setBreathingFire(vehicle, false, null);
                     IceAndFireDragonCompat.setFlying(vehicle, true);
                  }
               } else if (state.mode == DragonRiderController.Mode.DIVE) {
                  state.phaseTicks++;
                  if (state.phaseTicks == 18) {
                     IceAndFireDragonCompat.bite(vehicle, attackTarget);
                     rider.m_21011_(InteractionHand.MAIN_HAND, true);
                     rider.m_7327_(attackTarget);
                  }

                  if (state.phaseTicks >= 28) {
                     state.mode = DragonRiderController.Mode.CLIMB;
                     state.phaseTicks = 0;
                     state.phaseStart = vehicle.m_20182_();
                     IceAndFireDragonCompat.setTackling(vehicle, false);
                     IceAndFireDragonCompat.setBreathingFire(vehicle, false, null);
                     IceAndFireDragonCompat.setFlying(vehicle, true);
                  }
               } else if (state.mode == DragonRiderController.Mode.CLIMB) {
                  state.phaseTicks++;
                  if (state.phaseTicks >= 36) {
                     state.mode = DragonRiderController.Mode.ORBIT;
                     state.phaseTicks = 0;
                     state.nextDive = rider.m_9236_().m_46467_() + 300L + (long)rider.m_217043_().m_188503_(300);
                  }
               }

               ACTIVE.put(rider.m_20148_(), state);
            } else {
               rider.m_6710_(null);
               state.mode = DragonRiderController.Mode.GROUND;
               IceAndFireDragonCompat.setTackling(vehicle, false);
               IceAndFireDragonCompat.setBreathingFire(vehicle, false, null);
               IceAndFireDragonCompat.setFlying(vehicle, false);
               IceAndFireDragonCompat.setHovering(vehicle, false);
               if (!dragonGroupMember) {
                  vehicle.m_20256_(vehicle.m_20184_().m_82542_(0.55, 0.8, 0.55));
               }

               ACTIVE.put(rider.m_20148_(), state);
            }
         } else if (rider.m_6084_() && rider.getDragonRiderMountUuid() != null && vehicle == null) {
            rider.m_20242_(false);
            rider.m_6710_(null);
         } else {
            rider.clearDragonRiderMount();
            rider.m_20242_(false);
            rider.f_19789_ = 0.0F;
            ACTIVE.remove(rider.m_20148_());
         }
      }
   }

   public static void postServerTick(ServerLevel level) {
      ACTIVE.entrySet()
         .removeIf(
            entry -> {
               Entity entity = null;

               for (Entity candidate : level.m_8583_()) {
                  if (candidate.m_20148_().equals(entry.getKey())) {
                     entity = candidate;
                     break;
                  }
               }

               if (entity == null) {
                  return false;
               } else {
                  if (entity instanceof CombatNpcEntity rider && rider.m_6084_()) {
                     Entity dragon = rider.isLogicalDragonRider() ? findMount(rider) : rider.m_20202_();
                     if (!IceAndFireDragonCompat.isDragon(dragon)) {
                        return rider.getDragonRiderMountUuid() == null;
                     }

                     if (rider.getDragonRiderMountId() < 0) {
                        rider.setDragonRiderMountId(dragon.m_19879_());
                     }

                     IceAndFireDragonCompat.clearNativeTarget(dragon);
                     DragonRiderController.State state = ACTIVE.get(rider.m_20148_());
                     LivingEntity target = rider.m_5448_();
                     PlayerOwnedNpcEntity groupCommander = rider.getCombatGroupCommander().orElse(null);
                     boolean dragonGroupMember = groupCommander != null && groupCommander.isDragonRiderCombatGroupCommander();
                     if (dragonGroupMember && !NpcTargeting.canHarm(rider, groupCommander.m_5448_())) {
                        tickFormationMovement(rider, dragon, groupCommander, state);
                        syncRiderSeat(rider, dragon);
                        return false;
                     }

                     if (!dragonGroupMember && rider.getCurrentCommand() == NpcCommandType.MOVE) {
                        rider.m_6710_(null);
                        BlockPos commandTarget = rider.getCommandTarget();
                        if (!BlockPos.f_121853_.equals(commandTarget)) {
                           Vec3 destination = Vec3.m_82539_(commandTarget).m_82520_(0.0, 5.0, 0.0);
                           if (dragon.m_20182_().m_82557_(destination) > 16.0) {
                              IceAndFireDragonCompat.setFlying(dragon, true);
                              IceAndFireDragonCompat.setHovering(dragon, false);
                              IceAndFireDragonCompat.setFlightTarget(dragon, destination);
                              moveDragon(dragon, destination, rider, 0.0);
                           } else {
                              IceAndFireDragonCompat.setFlying(dragon, false);
                              IceAndFireDragonCompat.setHovering(dragon, false);
                              dragon.m_20256_(Vec3.f_82478_);
                              rider.clearCommand();
                           }

                           syncRiderSeat(rider, dragon);
                           return false;
                        }
                     }

                     if (!dragonGroupMember && !NpcTargeting.canHarm(rider, target) && rider.getCurrentCommand() == NpcCommandType.FOLLOW) {
                        Player player = rider.getFollowTargetPlayer().orElse(null);
                        if (player != null && dragon.m_20280_(player) > 100.0) {
                           Vec3 destination = player.m_20182_().m_82520_(0.0, 5.0, 0.0);
                           IceAndFireDragonCompat.setFlying(dragon, true);
                           IceAndFireDragonCompat.setHovering(dragon, false);
                           IceAndFireDragonCompat.setFlightTarget(dragon, destination);
                           moveDragon(dragon, destination, player, 0.0);
                           dragon.f_19812_ = true;
                        } else {
                           IceAndFireDragonCompat.setFlying(dragon, false);
                           IceAndFireDragonCompat.setHovering(dragon, false);
                           dragon.m_20256_(Vec3.f_82478_);
                        }

                        syncRiderSeat(rider, dragon);
                        return false;
                     }

                     if (state != null && NpcTargeting.canHarm(rider, target) && isSurfaceReachableTarget(level, target)) {
                        LivingEntity attackTarget = target;
                        if (state.attackTargetId != null
                           && level.m_8791_(state.attackTargetId) instanceof LivingEntity living
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
                           point = safeFlightPoint(level, dragon, lerp(state.phaseStart, orbit, progress), Mth.m_14139_(progress, 2.5, 6.0));
                        } else if (state.mode == DragonRiderController.Mode.BREATH_APPROACH || state.mode == DragonRiderController.Mode.BREATH) {
                           Vec3 toward = dragon.m_20182_().m_82546_(attackTarget.m_20182_());
                           Vec3 horizontal = toward.m_165925_() > 0.001 ? new Vec3(toward.f_82479_, 0.0, toward.f_82481_).m_82541_() : new Vec3(1.0, 0.0, 0.0);
                           Vec3 breathPoint = attackTarget.m_20182_()
                              .m_82549_(horizontal.m_82490_(12.0))
                              .m_82520_(0.0, (double)attackTarget.m_20206_() + 6.0, 0.0);
                           point = safeFlightPoint(level, dragon, breathPoint, 6.0);
                        } else if (state.mode == DragonRiderController.Mode.DIVE) {
                           Vec3 bitePoint = attackTarget.m_20182_().m_82520_(0.0, (double)attackTarget.m_20206_() + 1.0, 0.0);
                           point = lerp(state.phaseStart, bitePoint, Math.min(1.0, (double)state.phaseTicks / 28.0));
                        } else {
                           point = orbit;
                        }

                        if (state.mode == DragonRiderController.Mode.ALIGN) {
                           point = safeFlightPoint(level, dragon, point, 6.0);
                        }

                        if (state.mode == DragonRiderController.Mode.ALIGN) {
                           IceAndFireDragonCompat.setFlightTarget(dragon, point);
                           dragon.m_6034_(point.f_82479_, point.f_82480_, point.f_82481_);
                           dragon.m_20256_(Vec3.f_82478_);
                           dragon.f_19812_ = true;
                        } else {
                           IceAndFireDragonCompat.setFlightTarget(dragon, point);
                           moveDragon(dragon, point, target, state.angle);
                        }

                        dragon.m_6034_(point.f_82479_, point.f_82480_, point.f_82481_);
                        dragon.m_20256_(Vec3.f_82478_);
                        dragon.f_19812_ = true;
                        if (state.mode == DragonRiderController.Mode.ALIGN) {
                           faceSmooth(dragon, targetAimPoint(target), 10.0F);
                        } else if (state.mode == DragonRiderController.Mode.BREATH_APPROACH) {
                           faceSmooth3d(dragon, targetAimPoint(attackTarget), 12.0F);
                        } else if (state.mode == DragonRiderController.Mode.BREATH) {
                           faceExact3d(dragon, targetAimPoint(attackTarget));
                        } else if (state.mode == DragonRiderController.Mode.DIVE) {
                           face(dragon, attackTarget.m_20182_());
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
      rider.m_6034_(seat.f_82479_, seat.f_82480_, seat.f_82481_);
      rider.f_19789_ = 0.0F;
      rider.m_20256_(Vec3.f_82478_);
      rider.m_20242_(true);
   }

   private static void tickFormationMovement(CombatNpcEntity rider, Entity dragon, PlayerOwnedNpcEntity commander, DragonRiderController.State state) {
      if (state != null) {
         Vec3 destination = rider.getCombatGroupFormationPosition().orElse(null);
         if (destination == null) {
            state.formationMode = DragonRiderController.FormationMode.GROUND;
            IceAndFireDragonCompat.setFlying(dragon, false);
            IceAndFireDragonCompat.setHovering(dragon, false);
            dragon.m_20256_(Vec3.f_82478_);
         } else {
            double horizontalDistance = dragon.m_20182_().m_82546_(destination).m_165924_();
            if (state.formationMode == DragonRiderController.FormationMode.GROUND && horizontalDistance > 3.0) {
               state.formationMode = DragonRiderController.FormationMode.FORMATION_TAKEOFF;
               state.formationTicks = 0;
               state.formationStart = dragon.m_20182_();
               state.formationCruiseY = Math.max(dragon.m_20186_(), destination.f_82480_) + 8.0;
            }

            Vec3 point;
            if (state.formationMode == DragonRiderController.FormationMode.FORMATION_TAKEOFF) {
               state.formationTicks++;
               double progress = Math.min(1.0, (double)state.formationTicks / 30.0);
               point = lerp(state.formationStart, new Vec3(state.formationStart.f_82479_, state.formationCruiseY, state.formationStart.f_82481_), progress);
               IceAndFireDragonCompat.setFlying(dragon, true);
               IceAndFireDragonCompat.setHovering(dragon, false);
               if (progress >= 1.0) {
                  state.formationMode = DragonRiderController.FormationMode.FORMATION_MOVE;
                  state.formationTicks = 0;
               }
            } else if (state.formationMode == DragonRiderController.FormationMode.FORMATION_MOVE) {
               point = new Vec3(destination.f_82479_, state.formationCruiseY, destination.f_82481_);
               IceAndFireDragonCompat.setFlying(dragon, true);
               IceAndFireDragonCompat.setHovering(dragon, false);
               if (dragon.m_20182_().m_82557_(point) < 9.0) {
                  state.formationMode = DragonRiderController.FormationMode.FORMATION_LAND;
                  state.formationTicks = 0;
                  state.formationStart = dragon.m_20182_();
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
            dragon.m_6034_(point.f_82479_, point.f_82480_, point.f_82481_);
            dragon.m_20256_(Vec3.f_82478_);
            dragon.f_19812_ = true;
            if (state.formationMode != DragonRiderController.FormationMode.GROUND) {
               breakFlightBlocks(dragon);
            }

            dragon.m_146922_(commander.m_146908_());
            dragon.m_5618_(commander.m_146908_());
            dragon.m_5616_(commander.m_146908_());
            dragon.m_146926_(0.0F);
         }
      }
   }

   public static void postClientTick(Iterable<? extends Entity> entities) {
      for (Entity entity : entities) {
         if (entity instanceof CombatNpcEntity) {
            CombatNpcEntity rider = (CombatNpcEntity)entity;
            Entity dragon = rider.isLogicalDragonRider() ? findMount(rider) : rider.m_20202_();
            if (IceAndFireDragonCompat.isDragon(dragon)) {
               Vec3 seat = IceAndFireDragonCompat.riderPosition(dragon);
               rider.m_6034_(seat.f_82479_, seat.f_82480_, seat.f_82481_);
               rider.f_19789_ = 0.0F;
               rider.m_20256_(Vec3.f_82478_);
               rider.m_20242_(true);
            }
         }
      }
   }

   private static Vec3 orbitPoint(ServerLevel level, Entity dragon, LivingEntity target, double angle) {
      Vec3 requested = target.m_20182_().m_82520_(Math.cos(angle) * 32.0, (double)target.m_20206_() + 10.0, Math.sin(angle) * 32.0);
      return safeFlightPoint(level, dragon, requested, 6.0);
   }

   private static Vec3 safeFlightPoint(ServerLevel level, Entity dragon, Vec3 requested, double clearance) {
      if (requested != null && Double.isFinite(requested.f_82479_) && Double.isFinite(requested.f_82480_) && Double.isFinite(requested.f_82481_)) {
         double safeY = Math.max(requested.f_82480_, (double)level.m_141937_() + 2.0);
         BlockPos column = BlockPos.m_274561_(requested.f_82479_, safeY, requested.f_82481_);
         if (level.m_46805_(column)) {
            int surfaceY = level.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, column.m_123341_(), column.m_123343_());
            safeY = Math.max(safeY, (double)surfaceY + clearance);
         }

         double ceiling = (double)level.m_151558_() - Math.max(4.0, (double)dragon.m_20206_() + 2.0);
         safeY = Math.min(safeY, ceiling);
         return new Vec3(requested.f_82479_, safeY, requested.f_82481_);
      } else {
         return dragon.m_20182_();
      }
   }

   private static boolean isSurfaceReachableTarget(ServerLevel level, LivingEntity target) {
      BlockPos column = target.m_20183_();
      if (!level.m_46805_(column)) {
         return false;
      } else {
         int surfaceY = level.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, column.m_123341_(), column.m_123343_());
         int firstY = Mth.m_14165_(target.m_20191_().f_82292_ + 0.01);
         int coverBlocks = 0;
         MutableBlockPos cursor = new MutableBlockPos(column.m_123341_(), firstY, column.m_123343_());

         for (int y = firstY; y < surfaceY; y++) {
            cursor.m_142448_(y);
            BlockState state = level.m_8055_(cursor);
            boolean covered = state.m_60838_(level, cursor) || !state.m_60819_().m_76178_();
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
         if (rider.m_9236_() instanceof ServerLevel level && isSurfaceReachableTarget(level, target)) {
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
         return rider.m_9236_().m_6815_(rider.getDragonRiderMountId());
      } else {
         if (rider.m_9236_() instanceof ServerLevel server) {
            for (ServerLevel level : server.m_7654_().m_129785_()) {
               Entity entity = level.m_8791_(uuid);
               if (entity != null) {
                  return entity;
               }
            }
         }

         return rider.m_9236_().m_6815_(rider.getDragonRiderMountId());
      }
   }

   public static Entity getLogicalMount(PlayerOwnedNpcEntity rider) {
      if (rider != null && rider.isLogicalDragonRider()) {
         Entity mount = findMount(rider);
         return IceAndFireDragonCompat.isDragon(mount) && mount.m_6084_() ? mount : null;
      } else {
         return null;
      }
   }

   private static void face(Entity dragon, Vec3 point) {
      Vec3 d = point.m_82546_(dragon.m_20182_());
      if (d.m_165925_() > 0.001) {
         dragon.m_146922_((float)Math.toDegrees(Math.atan2(-d.f_82479_, d.f_82481_)));
         dragon.m_5616_(dragon.m_146908_());
      }
   }

   private static void faceSmooth(Entity dragon, Vec3 point, float maxTurn) {
      Vec3 d = point.m_82546_(dragon.m_20182_());
      if (!(d.m_165925_() <= 0.001)) {
         float desired = (float)Math.toDegrees(Math.atan2(-d.f_82479_, d.f_82481_));
         float current = dragon.m_146908_();
         float delta = Mth.m_14177_(desired - current);
         float next = current + Mth.m_14036_(delta, -maxTurn, maxTurn);
         dragon.m_146922_(next);
         dragon.m_5618_(next);
         dragon.m_5616_(next);
      }
   }

   private static void faceSmooth3d(Entity dragon, Vec3 point, float maxTurn) {
      Vec3 d = point.m_82546_(dragon.m_20182_());
      double horizontal = Math.max(0.001, d.m_165924_());
      float desiredYaw = (float)Math.toDegrees(Math.atan2(-d.f_82479_, d.f_82481_));
      float desiredPitch = (float)(-Math.toDegrees(Math.atan2(d.f_82480_, horizontal)));
      float yaw = dragon.m_146908_() + Mth.m_14036_(Mth.m_14177_(desiredYaw - dragon.m_146908_()), -maxTurn, maxTurn);
      float pitch = dragon.m_146909_() + Mth.m_14036_(Mth.m_14177_(desiredPitch - dragon.m_146909_()), -maxTurn, maxTurn);
      dragon.m_146922_(yaw);
      dragon.m_5618_(yaw);
      dragon.m_5616_(yaw);
      dragon.m_146926_(pitch);
   }

   private static void faceExact3d(Entity dragon, Vec3 point) {
      Vec3 d = point.m_82546_(dragon.m_20182_());
      double horizontal = Math.max(0.001, d.m_165924_());
      float yaw = (float)Math.toDegrees(Math.atan2(-d.f_82479_, d.f_82481_));
      float pitch = (float)(-Math.toDegrees(Math.atan2(d.f_82480_, horizontal)));
      dragon.m_146922_(yaw);
      dragon.m_5618_(yaw);
      dragon.m_5616_(yaw);
      dragon.m_146926_(pitch);
   }

   private static Vec3 targetAimPoint(LivingEntity target) {
      return target.m_20182_().m_82520_(0.0, (double)target.m_20206_() * 0.55, 0.0);
   }

   private static void moveDragon(Entity dragon, Vec3 point, LivingEntity target, double angle) {
      Vec3 current = dragon.m_20182_();
      Vec3 delta = point.m_82546_(current);
      double distance = delta.m_82553_();
      double maxSpeed = 0.34;
      Vec3 desired = distance < 0.05 ? Vec3.f_82478_ : delta.m_82490_(Math.min(maxSpeed / distance, 1.0));
      Vec3 old = dragon.m_20184_();
      Vec3 velocity = old.m_82490_(0.82).m_82549_(desired.m_82490_(0.18));
      if (velocity.m_82556_() > maxSpeed * maxSpeed) {
         velocity = velocity.m_82541_().m_82490_(maxSpeed);
      }

      dragon.m_20242_(true);
      dragon.m_20256_(velocity);
      dragon.m_6478_(MoverType.SELF, velocity);
      breakFlightBlocks(dragon);
      Vec3 tangent = velocity.m_165925_() > 1.0E-5 ? velocity : new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
      dragon.m_146922_((float)Math.toDegrees(Math.atan2(-tangent.f_82479_, tangent.f_82481_)));
      dragon.m_5616_(dragon.m_146908_());
   }

   private static void breakFlightBlocks(Entity dragon) {
      if (dragon.f_19797_ % 5 == 0) {
         IceAndFireDragonCompat.breakNearbyBlocks(dragon);
      }
   }

   private static boolean isHostile(CombatNpcEntity rider, LivingEntity target) {
      return NpcTargeting.canHarm(rider, target)
         && (NpcTargeting.isAutomaticHostile(rider, target) || target instanceof FactionNpcEntity faction && faction.isHostileTowards(rider));
   }

   private static LivingEntity findHostileTarget(CombatNpcEntity rider) {
      AABB search = rider.m_20191_().m_82377_(96.0, 48.0, 96.0);
      return rider.m_9236_()
         .m_6443_(
            LivingEntity.class,
            search,
            candidate -> candidate != rider
                  && candidate.m_6084_()
                  && isHostile(rider, candidate)
                  && (!(rider.m_9236_() instanceof ServerLevel server) || isSurfaceReachableTarget(server, candidate))
         )
         .stream()
         .min(Comparator.comparingDouble(rider::m_20280_))
         .orElse(null);
   }

   private static LivingEntity findNearbyAttackTarget(CombatNpcEntity rider, LivingEntity anchor) {
      AABB search = anchor.m_20191_().m_82400_(10.0);
      return rider.m_9236_()
         .m_6443_(
            LivingEntity.class,
            search,
            candidate -> candidate != rider
                  && candidate != anchor
                  && candidate.m_6084_()
                  && isHostile(rider, candidate)
                  && (!(rider.m_9236_() instanceof ServerLevel server) || isSurfaceReachableTarget(server, candidate))
         )
         .stream()
         .min(Comparator.comparingDouble(rider::m_20280_))
         .orElse(anchor);
   }

   private static Vec3 lerp(Vec3 from, Vec3 to, double amount) {
      return from.m_165921_(to, amount);
   }

   private static boolean isRangedDragonRider(CombatNpcEntity rider) {
      return rider.m_21205_().m_41720_() instanceof BowItem || rider.m_21205_().m_41720_() instanceof CrossbowItem;
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
      private Vec3 phaseStart = Vec3.f_82478_;
      private boolean breathAttack;
      private DragonRiderController.Attack lastAttack = DragonRiderController.Attack.NONE;
      private int consecutiveAttacks;
      private UUID attackTargetId;
      private DragonRiderController.FormationMode formationMode = DragonRiderController.FormationMode.GROUND;
      private int formationTicks;
      private double formationCruiseY;
      private Vec3 formationStart = Vec3.f_82478_;

      private State(UUID dragonId, long ignored, double angle) {
         this.dragonId = dragonId;
         this.angle = angle;
         this.clockwise = angle >= Math.PI;
      }
   }
}
