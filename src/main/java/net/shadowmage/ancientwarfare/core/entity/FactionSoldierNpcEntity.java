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
import net.shadowmage.ancientwarfare.core.compat.IceAndFireEquipmentCompat;

public class FactionSoldierNpcEntity extends FactionNpcEntity {
   public FactionSoldierNpcEntity(EntityType<? extends FactionSoldierNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.SOLDIER);
      this.m_21008_(InteractionHand.MAIN_HAND, new ItemStack(Items.f_42383_));
      this.applyFactionDefaults();
   }

   protected FactionSoldierNpcEntity(EntityType<? extends FactionSoldierNpcEntity> entityType, Level level, FactionNpcRole role, ItemStack weapon) {
      super(entityType, level, role);
      this.m_21008_(InteractionHand.MAIN_HAND, weapon);
      this.applyFactionDefaults();
   }

   @Override
   protected boolean supportsCombatPromotion() {
      return !this.getNpcSubtype().endsWith(".elite");
   }

   public static Builder createAttributes() {
      return m_21552_()
         .m_22268_(Attributes.f_22276_, 24.0)
         .m_22268_(Attributes.f_22279_, 0.27)
         .m_22268_(Attributes.f_22281_, 3.0)
         .m_22268_(Attributes.f_22283_, 4.0)
         .m_22268_(Attributes.f_22277_, 32.0);
   }

   protected PathNavigation m_6037_(Level level) {
      AmphibiousPathNavigation navigation = new AmphibiousPathNavigation(this, level);
      navigation.m_7008_(true);
      return navigation;
   }

   protected void m_8099_() {
      this.addSharedGoals();
      this.f_21345_.m_25352_(1, new FactionSoldierNpcEntity.ShieldDefenseGoal(this));
      this.f_21345_.m_25352_(2, new FactionSoldierNpcEntity.FactionMeleeAttackGoal(this, this.playerSprintNavigationSpeed(), true));
      this.addFactionCombatTargets();
   }

   @Override
   public boolean m_7327_(Entity target) {
      this.triggerMeleeAttackAnimation();
      this.m_21011_(InteractionHand.MAIN_HAND, true);
      boolean hit = super.m_7327_(target);
      if (hit && target instanceof LivingEntity livingTarget) {
         IceAndFireEquipmentCompat.applySupportedMeleeHit(this.m_21205_(), livingTarget, this);
      }

      if (hit) {
         this.recordCombatAttack();
      }

      return hit;
   }

   protected static class FactionMeleeAttackGoal extends MeleeAttackGoal {
      private final FactionSoldierNpcEntity npc;
      private int weaponCooldown;

      protected FactionMeleeAttackGoal(FactionSoldierNpcEntity npc, double speedModifier, boolean followTargetEvenIfNotSeen) {
         super(npc, speedModifier, followTargetEvenIfNotSeen);
         this.npc = npc;
      }

      public boolean m_8036_() {
         return !this.npc.m_20159_() && super.m_8036_();
      }

      public boolean m_8045_() {
         return !this.npc.m_20159_() && super.m_8045_();
      }

      public void m_8037_() {
         if (this.weaponCooldown > 0) {
            this.weaponCooldown--;
         }

         super.m_8037_();
         LivingEntity target = this.npc.m_5448_();
         if (target != null) {
            double holdDistance = NpcEquipmentResolver.meleeHoldDistance(this.npc);
            if (this.npc.m_20280_(target) <= holdDistance * holdDistance && this.npc.hasMeleeLineOfSight(target)) {
               this.npc.m_21573_().m_26573_();
               this.npc.m_21563_().m_24960_(target, 30.0F, 30.0F);
            }
         }
      }

      protected void m_6739_(LivingEntity target, double distanceSqr) {
         if (distanceSqr <= this.m_6639_(target) && this.weaponCooldown <= 0 && this.npc.hasMeleeLineOfSight(target)) {
            this.npc.m_7327_(target);
            this.weaponCooldown = NpcEquipmentResolver.meleeAttackInterval(this.npc);
         }
      }

      protected double m_6639_(LivingEntity target) {
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

      public boolean m_8036_() {
         return !this.npc.m_20159_() && NpcShielding.shouldRaiseShield(this.npc, this.npc.m_5448_());
      }

      public boolean m_8045_() {
         if (this.m_8036_()) {
            this.lingerTicks = 20;
            return true;
         } else {
            return this.lingerTicks-- > 0;
         }
      }

      public void m_8056_() {
         this.lingerTicks = 20;
         this.npc.setShielding(true);
      }

      public void m_8037_() {
         LivingEntity target = this.npc.m_5448_();
         if (target != null) {
            this.npc.m_21563_().m_148051_(target);
         }

         if (NpcShielding.shouldRaiseShield(this.npc, this.npc.m_5448_())) {
            this.lingerTicks = 20;
         }

         this.npc.setShielding(this.lingerTicks > 0);
      }

      public void m_8041_() {
         this.lingerTicks = 0;
         this.npc.setShielding(false);
      }
   }
}
