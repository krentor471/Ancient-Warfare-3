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
      return m_21552_()
         .m_22268_(Attributes.f_22276_, 30.0)
         .m_22268_(Attributes.f_22279_, 0.3)
         .m_22268_(Attributes.f_22277_, 120.0)
         .m_22268_(Attributes.f_22281_, 1.0);
   }

   protected void m_8099_() {
      this.addSharedGoals();
      this.f_21345_.m_25352_(2, new FactionSiegeEngineerNpcEntity.OperateSiegeWeaponGoal(this));
      this.f_21345_.m_25352_(3, new FactionSiegeEngineerNpcEntity.FindSiegeWeaponGoal(this));
      this.addFactionCombatTargets(false);
   }

   private boolean canAttackSiegeTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target) && (target == this.m_5448_() || this.isHostileTowards(target) || NpcTargeting.isAutomaticHostile(this, target));
   }

   private static class FindSiegeWeaponGoal extends Goal {
      private final FactionSiegeEngineerNpcEntity engineer;
      private Entity targetEntity;
      private SiegeWeaponControl targetWeapon;
      private int rescanDelay;

      private FindSiegeWeaponGoal(FactionSiegeEngineerNpcEntity engineer) {
         this.engineer = engineer;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
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

      public boolean m_8045_() {
         return this.targetEntity != null
            && this.targetEntity.m_6084_()
            && this.targetWeapon != null
            && this.targetWeapon.canBeOperatedBy(this.engineer)
            && this.engineer.siegeBrain.getSiegeWeapon().isEmpty();
      }

      public void m_8037_() {
         this.engineer.m_21563_().m_148051_(this.targetEntity);
         if (!this.engineer.siegeBrain.isCloseEnoughToOperate(this.targetEntity)) {
            this.engineer.m_21573_().m_5624_(this.targetEntity, this.engineer.playerWalkNavigationSpeed());
         } else if (this.targetWeapon.beginOperation(this.engineer)) {
            this.engineer.siegeBrain.setSiegeWeapon(this.targetWeapon);
         }
      }

      public void m_8041_() {
         this.targetEntity = null;
         this.targetWeapon = null;
         this.engineer.m_21573_().m_26573_();
      }
   }

   private static class OperateSiegeWeaponGoal extends Goal {
      private final FactionSiegeEngineerNpcEntity engineer;
      private final SiegeEngineerBrain.FireDelay fireDelay = new SiegeEngineerBrain.FireDelay();

      private OperateSiegeWeaponGoal(FactionSiegeEngineerNpcEntity engineer) {
         this.engineer = engineer;
         this.m_7021_(EnumSet.of(Flag.LOOK));
      }

      public boolean m_8036_() {
         return this.engineer.siegeBrain.getSiegeWeapon().isPresent();
      }

      public boolean m_8045_() {
         return this.m_8036_();
      }

      public void m_8037_() {
         this.engineer.siegeBrain.tickWeapon(this.engineer::canAttackSiegeTarget, this.fireDelay);
      }

      public void m_8041_() {
         this.fireDelay.value = 0;
      }
   }
}
