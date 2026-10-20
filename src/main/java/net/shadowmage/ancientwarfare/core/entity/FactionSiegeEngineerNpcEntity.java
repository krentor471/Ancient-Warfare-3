package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.common.siege.SiegeAmmoSupplier;
import net.shadowmage.ancientwarfare.core.common.siege.SiegeWeaponControl;

public class FactionSiegeEngineerNpcEntity extends FactionNpcEntity implements SiegeAmmoSupplier {
   private static final double SIEGE_VERTICAL_TARGET_RANGE = 96.0;
   private final SiegeEngineerBrain siegeBrain = new SiegeEngineerBrain(this);

   public FactionSiegeEngineerNpcEntity(EntityType<? extends FactionSiegeEngineerNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.SIEGE_ENGINEER);
   }

   public static Builder createAttributes() {
      return createMobAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.FOLLOW_RANGE, 120.0)
         .add(Attributes.ATTACK_DAMAGE, 1.0);
   }

   protected void registerGoals() {
      this.addSharedGoals();
      this.goalSelector.addGoal(2, new FactionSiegeEngineerNpcEntity.OperateSiegeWeaponGoal(this));
      this.goalSelector.addGoal(3, new FactionSiegeEngineerNpcEntity.FindSiegeWeaponGoal(this));
      this.addFactionCombatTargets(false);
   }

   private boolean canAttackSiegeTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target) && (target == this.getTarget() || this.isHostileTowards(target) || NpcTargeting.isAutomaticHostile(this, target));
   }

   private static class FindSiegeWeaponGoal extends Goal {
      private final FactionSiegeEngineerNpcEntity engineer;
      private Entity targetEntity;
      private SiegeWeaponControl targetWeapon;
      private int rescanDelay;

      private FindSiegeWeaponGoal(FactionSiegeEngineerNpcEntity engineer) {
         this.engineer = engineer;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.engineer.siegeBrain.getSiegeWeapon().isPresent()) {
            return false;
         } else if (this.rescanDelay > 0) {
            this.rescanDelay--;
            return false;
         } else {
            this.rescanDelay = 40;
            return this.engineer.siegeBrain.findNearestWeapon().filter(SiegeEngineerBrain::canOperateAsEntity).map(entity -> {
               this.targetEntity = entity;
               this.targetWeapon = (SiegeWeaponControl)entity;
               return true;
            }).orElse(false);
         }
      }

      public boolean canContinueToUse() {
         return this.targetEntity != null
            && this.targetEntity.isAlive()
            && this.targetWeapon != null
            && this.targetWeapon.canBeOperatedBy(this.engineer)
            && this.engineer.siegeBrain.getSiegeWeapon().isEmpty();
      }

      public void tick() {
         this.engineer.getLookControl().setLookAt(this.targetEntity);
         if (!this.engineer.siegeBrain.isCloseEnoughToOperate(this.targetEntity)) {
            this.engineer.getNavigation().moveTo(this.targetEntity, this.engineer.playerWalkNavigationSpeed());
         } else if (this.targetWeapon.beginOperation(this.engineer)) {
            this.engineer.siegeBrain.setSiegeWeapon(this.targetWeapon);
         }
      }

      public void stop() {
         this.targetEntity = null;
         this.targetWeapon = null;
         this.engineer.getNavigation().stop();
      }
   }

   private static class OperateSiegeWeaponGoal extends Goal {
      private final FactionSiegeEngineerNpcEntity engineer;
      private final SiegeEngineerBrain.FireDelay fireDelay = new SiegeEngineerBrain.FireDelay();

      private OperateSiegeWeaponGoal(FactionSiegeEngineerNpcEntity engineer) {
         this.engineer = engineer;
         this.setFlags(EnumSet.of(Flag.LOOK));
      }

      public boolean canUse() {
         return this.engineer.siegeBrain.getSiegeWeapon().isPresent();
      }

      public boolean canContinueToUse() {
         return this.canUse();
      }

      public void tick() {
         this.engineer.siegeBrain.tickWeapon(this.engineer::canAttackSiegeTarget, this.fireDelay);
      }

      public void stop() {
         this.fireDelay.value = 0;
      }
   }
}
