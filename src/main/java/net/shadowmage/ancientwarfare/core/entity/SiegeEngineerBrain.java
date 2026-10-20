package net.shadowmage.ancientwarfare.core.entity;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.siege.SiegeWeaponControl;

final class SiegeEngineerBrain {
   static final double SIEGE_SEARCH_RANGE = 32.0;
   static final double ENEMY_SEARCH_RANGE = 96.0;
   static final double OPERATE_DISTANCE = 3.0;
   private final NpcBaseEntity engineer;
   private SiegeWeaponControl siegeWeapon;

   SiegeEngineerBrain(NpcBaseEntity engineer) {
      this.engineer = engineer;
   }

   Optional<SiegeWeaponControl> getSiegeWeapon() {
      return Optional.ofNullable(this.siegeWeapon);
   }

   void setSiegeWeapon(SiegeWeaponControl siegeWeapon) {
      this.siegeWeapon = siegeWeapon;
   }

   void clearSiegeWeapon() {
      if (this.siegeWeapon != null) {
         this.siegeWeapon.stopOperation(this.engineer);
      }

      this.siegeWeapon = null;
   }

   Optional<Entity> findNearestWeapon() {
      AABB searchBox = this.engineer.getBoundingBox().inflate(32.0);
      return this.engineer.level().getEntities(this.engineer, searchBox, entity -> {
         if (entity instanceof SiegeWeaponControl control && control.canBeOperatedBy(this.engineer)) {
            return true;
         }

         return false;
      }).stream().min(Comparator.comparingDouble(this.engineer::distanceToSqr));
   }

   boolean isCloseEnoughToOperate(Entity weaponEntity) {
      return this.engineer.distanceToSqr(weaponEntity) <= 9.0;
   }

   LivingEntity chooseTarget(Predicate<LivingEntity> targetFilter) {
      SiegeWeaponControl weapon = this.siegeWeapon;
      if (weapon == null) {
         return null;
      } else {
         LivingEntity currentTarget;
         boolean validCurrentTarget;
         boolean var10000;
         label35: {
            currentTarget = this.engineer.getTarget();
            validCurrentTarget = currentTarget != null
               && currentTarget.isAlive()
               && currentTarget != this.engineer
               && !this.engineer.isAlliedTo(currentTarget)
               && targetFilter.test(currentTarget)
               && this.canTrackTarget(weapon, currentTarget);
            if (this.engineer instanceof PlayerOwnedNpcEntity owned && owned.getCurrentCommand() == NpcCommandType.ATTACK) {
               var10000 = true;
               break label35;
            }

            var10000 = false;
         }

         boolean forcedAttackCommand = var10000;
         if (forcedAttackCommand && validCurrentTarget) {
            return currentTarget;
         } else if (validCurrentTarget && weapon.canAttackTarget(currentTarget)) {
            return currentTarget;
         } else {
            AABB searchBox = this.engineer.getBoundingBox().inflate(96.0);
            return this.engineer
               .level()
               .getEntitiesOfClass(
                  LivingEntity.class,
                  searchBox,
                  target -> target != this.engineer
                        && !this.engineer.isAlliedTo(target)
                        && targetFilter.test(target)
                        && this.engineer.distanceToSqr(target) <= 9216.0
                        && this.canTrackTarget(weapon, target)
                        && (weapon.isMobile() || weapon.canAttackTarget(target))
               )
               .stream()
               .min(
                  Comparator.<LivingEntity>comparingInt(target -> weapon.canAttackTarget(target) ? 0 : 1)
                     .thenComparingInt(SiegeEngineerBrain::targetPriority)
                     .thenComparingDouble(this.engineer::distanceToSqr)
               )
               .orElse(null);
         }
      }
   }

   private boolean canTrackTarget(SiegeWeaponControl weapon, LivingEntity target) {
      if (!this.withinSearchDistance(target)) {
         return false;
      } else if (weapon instanceof Entity weaponEntity) {
         double maximumRange = weapon instanceof BatteringRamEntity ? 96.0 : Math.min(96.0, weapon.maximumAttackRange());
         return weaponEntity.distanceToSqr(target) <= maximumRange * maximumRange;
      } else {
         return true;
      }
   }

   private boolean withinSearchDistance(LivingEntity target) {
      return this.engineer.distanceToSqr(target) <= 9216.0;
   }

   private static int targetPriority(LivingEntity target) {
      return target instanceof NpcBaseEntity ? 0 : 1;
   }

   void tickWeapon(Predicate<LivingEntity> targetFilter, SiegeEngineerBrain.FireDelay fireDelay) {
      SiegeWeaponControl weapon = this.siegeWeapon;
      if (weapon != null && weapon.canBeOperatedBy(this.engineer)) {
         if (weapon instanceof Entity weaponEntity && this.engineer.getVehicle() != weaponEntity && !weapon.beginOperation(this.engineer)) {
            this.clearSiegeWeapon();
            return;
         }

         if (weapon instanceof BatteringRamEntity ram) {
            GateEntity gate = ram.chooseGateTarget(this.engineer, targetFilter);
            if (gate != null) {
               this.engineer.getLookControl().setLookAt(gate);
               ram.aimAtGate(this.engineer, gate);
               if (fireDelay.value > 0) {
                  fireDelay.value--;
               } else if (ram.fireAtGate(this.engineer, gate)) {
                  this.engineer.swing(InteractionHand.MAIN_HAND);
                  fireDelay.value = 20;
               }

               return;
            }
         }

         LivingEntity target = this.chooseTarget(targetFilter);
         if (target == null) {
            if (weapon instanceof BatteringRamEntity ramx) {
               ramx.stopNpcControl();
            }
         } else {
            this.engineer.getLookControl().setLookAt(target);
            weapon.aimAt(this.engineer, target);
            if (weapon instanceof ChestPushcartEntity cart && weapon.isMobile() && !(weapon instanceof BatteringRamEntity)) {
               double distance = cart.distanceToSqr(target);
               double min = weapon.minimumAttackRange();
               double max = weapon.maximumAttackRange();
               double advanceUntil = max * 0.85;
               double retreatUntil = min + Math.max(4.0, min * 0.5);
               float forward = 0.0F;
               if (distance > advanceUntil * advanceUntil) {
                  forward = 1.0F;
               } else if (distance < retreatUntil * retreatUntil) {
                  forward = -1.0F;
               }

               cart.setVehicleInput(forward, 0.0F);
            }

            if (fireDelay.value > 0) {
               fireDelay.value--;
            } else {
               if (weapon.fireAt(this.engineer, target)) {
                  this.engineer.swing(InteractionHand.MAIN_HAND);
                  fireDelay.value = weapon instanceof HwachaEntity ? 5 : 20;
               }
            }
         }
      } else {
         this.clearSiegeWeapon();
      }
   }

   static boolean canOperateAsEntity(Entity entity) {
      return entity instanceof SiegeWeaponControl;
   }

   static final class FireDelay {
      int value;
   }
}
