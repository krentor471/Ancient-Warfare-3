package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.common.npc.NpcShielding;

public class FactionSoldierNpcEntity extends FactionNpcEntity {
   public FactionSoldierNpcEntity(EntityType<? extends FactionSoldierNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.SOLDIER);
      this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
      this.applyFactionDefaults();
   }

   protected FactionSoldierNpcEntity(EntityType<? extends FactionSoldierNpcEntity> entityType, Level level, FactionNpcRole role, ItemStack weapon) {
      super(entityType, level, role);
      this.setItemInHand(InteractionHand.MAIN_HAND, weapon);
      this.applyFactionDefaults();
   }

   @Override
   protected boolean supportsCombatPromotion() {
      return !this.getNpcSubtype().endsWith(".elite");
   }

   public static Builder createAttributes() {
      return createMobAttributes()
         .add(Attributes.MAX_HEALTH, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.27)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.ATTACK_SPEED, 4.0)
         .add(Attributes.FOLLOW_RANGE, 32.0);
   }

   protected PathNavigation createNavigation(Level level) {
      AmphibiousPathNavigation navigation = new AmphibiousPathNavigation(this, level);
      navigation.setCanFloat(true);
      return navigation;
   }

   @Override
   protected boolean usesMorale() {
      return true;
   }

   @Override
   protected boolean isMoraleLeader() {
      return this instanceof FactionLeaderNpcEntity
         || this instanceof FactionLeaderEliteNpcEntity
         || net.shadowmage.ancientwarfare.core.faction.FactionHeraldry.isBearer(this);
   }

   protected void registerGoals() {
      this.addSharedGoals();
      this.goalSelector.addGoal(1, new FactionSoldierNpcEntity.ShieldDefenseGoal(this));
      this.goalSelector.addGoal(2, new FactionSoldierNpcEntity.FactionMeleeAttackGoal(this, this.playerSprintNavigationSpeed(), true));
      this.addFactionCombatTargets();
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      this.triggerMeleeAttackAnimation();
      this.swing(InteractionHand.MAIN_HAND, true);
      boolean hit = super.doHurtTarget(target);
      if (hit) {
         this.recordCombatAttack();
      }

      return hit;
   }

   protected static class FactionMeleeAttackGoal extends MeleeAttackGoal {
      private static final double DIRECT_STEER_SQ = 36.0;
      private static final int APPROACH_TICK_STRIDE = 3;
      private final FactionSoldierNpcEntity npc;
      private final double speed;
      private int weaponCooldown;

      protected FactionMeleeAttackGoal(FactionSoldierNpcEntity npc, double speedModifier, boolean followTargetEvenIfNotSeen) {
         super(npc, speedModifier, followTargetEvenIfNotSeen);
         this.npc = npc;
         this.speed = speedModifier;
      }

      public boolean canUse() {
         return !this.npc.isPassenger() && super.canUse();
      }

      public boolean canContinueToUse() {
         return !this.npc.isPassenger() && super.canContinueToUse();
      }

      public void tick() {
         if (this.weaponCooldown > 0) {
            this.weaponCooldown--;
         }

         LivingEntity target = this.npc.getTarget();
         if (target != null && this.closeIn(target)) {
            return;
         }

         if (target != null && !this.npc.getNavigation().isDone() && (this.npc.tickCount + this.npc.getId()) % APPROACH_TICK_STRIDE != 0) {
            // Still walking a path toward a target further off: the route rarely needs a refresh every tick.
            this.npc.getLookControl().setLookAt(target, 30.0F, 30.0F);
            return;
         }

         super.tick();
         if (target != null) {
            double holdDistance = NpcEquipmentResolver.meleeHoldDistance(this.npc);
            if (this.npc.distanceToSqr(target) <= holdDistance * holdDistance && this.npc.hasMeleeLineOfSight(target)) {
               this.npc.getNavigation().stop();
               this.npc.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
         }
      }

      /**
       * In the press of a melee the target is a few steps away in plain sight, so the soldier steps at it directly
       * instead of asking the pathfinder for a new route every few ticks. Returns false when a real path is needed.
       */
      private boolean closeIn(LivingEntity target) {
         double distanceSqr = this.npc.distanceToSqr(target);
         if (distanceSqr > DIRECT_STEER_SQ || Math.abs(target.getY() - this.npc.getY()) > 1.0 || !this.npc.hasMeleeLineOfSight(target)) {
            return false;
         }

         this.npc.getLookControl().setLookAt(target, 30.0F, 30.0F);
         double holdDistance = NpcEquipmentResolver.meleeHoldDistance(this.npc);
         this.npc.getNavigation().stop();
         if (distanceSqr > holdDistance * holdDistance) {
            this.npc.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), this.speed);
         }

         this.checkAndPerformAttack(target, distanceSqr);
         return true;
      }

      protected void checkAndPerformAttack(LivingEntity target, double distanceSqr) {
         if (distanceSqr <= this.getAttackReachSqr(target) && this.weaponCooldown <= 0 && this.npc.hasMeleeLineOfSight(target)) {
            this.npc.doHurtTarget(target);
            this.weaponCooldown = NpcEquipmentResolver.meleeAttackInterval(this.npc);
         }
      }

      protected double getAttackReachSqr(LivingEntity target) {
         double reach = NpcEquipmentResolver.meleeAttackReach(this.npc);
         return reach * reach;
      }
   }

   private static class ShieldDefenseGoal extends Goal {
      private static final int SHIELD_LINGER_TICKS = 20;
      private final FactionSoldierNpcEntity npc;
      private int lingerTicks;

      private ShieldDefenseGoal(FactionSoldierNpcEntity npc) {
         this.npc = npc;
      }

      public boolean canUse() {
         return !this.npc.isPassenger() && NpcShielding.shouldRaiseShield(this.npc, this.npc.getTarget());
      }

      public boolean canContinueToUse() {
         if (this.canUse()) {
            this.lingerTicks = 20;
            return true;
         } else {
            return this.lingerTicks-- > 0;
         }
      }

      public void start() {
         this.lingerTicks = 20;
         this.npc.setShielding(true);
      }

      public void tick() {
         LivingEntity target = this.npc.getTarget();
         if (target != null) {
            this.npc.getLookControl().setLookAt(target);
         }

         if (NpcShielding.shouldRaiseShield(this.npc, this.npc.getTarget())) {
            this.lingerTicks = 20;
         }

         this.npc.setShielding(this.lingerTicks > 0);
      }

      public void stop() {
         this.lingerTicks = 0;
         this.npc.setShielding(false);
      }
   }
}
