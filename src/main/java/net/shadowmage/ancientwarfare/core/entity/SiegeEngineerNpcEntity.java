package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.siege.SiegeWeaponControl;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;

public class SiegeEngineerNpcEntity extends WeaponOperatorNpcEntity {
   private static final double SIEGE_VERTICAL_TARGET_RANGE = 96.0;

   public SiegeEngineerNpcEntity(EntityType<? extends SiegeEngineerNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.setNpcType("siege_engineer");
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.25).m_22268_(Attributes.f_22277_, 120.0);
   }

   @Override
   public boolean isSiegeEngineerProfession() {
      return true;
   }

   @Override
   protected void m_8099_() {
      super.m_8099_();
      this.f_21345_.m_25352_(3, new SiegeEngineerNpcEntity.OperateSiegeWeaponGoal(this));
      this.f_21345_.m_25352_(4, new SiegeEngineerNpcEntity.FindSiegeWeaponGoal(this));
      this.f_21346_.m_25352_(2, new VerticalRangeTargetGoal(this, FactionNpcEntity.class, 10, false, false, this::isHostileFactionNpc, 96.0));
      this.f_21346_.m_25352_(3, new VerticalRangeTargetGoal<LivingEntity>(this, LivingEntity.class, 10, false, false, this::isAutomaticSiegeTarget, 96.0));
   }

   protected InteractionResult m_6071_(Player player, InteractionHand hand) {
      if (this.tryAcceptFood(player, hand)) {
         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      } else if (player.m_21120_(hand).m_41720_() instanceof CommandBatonItem) {
         if (!this.m_9236_().f_46443_ && this.hasCommandPermission(player)) {
            CommandBatonItem.toggleNpcSelection(player.m_21120_(hand), player, this);
         }

         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      } else {
         if (!this.m_9236_().f_46443_ && this.hasCommandPermission(player)) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider((containerId, inventory, openedPlayer) -> new NpcInventoryMenu(containerId, inventory, this), this.m_5446_()),
               buffer -> buffer.writeInt(this.m_19879_())
            );
         }

         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      }
   }

   @Override
   public String getNpcType() {
      return "siege_engineer";
   }

   @Override
   public String getNpcSubtype() {
      return "";
   }

   @Override
   public Component m_5677_() {
      return Component.m_237115_("entity.ancientwarfare.siege_engineer");
   }

   @Override
   public Optional<SiegeWeaponControl> getSiegeWeapon() {
      return this.siegeBrain.getSiegeWeapon();
   }

   @Override
   public void setSiegeWeapon(SiegeWeaponControl siegeWeapon) {
      this.siegeBrain.setSiegeWeapon(siegeWeapon);
   }

   @Override
   public void clearSiegeWeapon() {
      this.siegeBrain.clearSiegeWeapon();
   }

   @Override
   protected boolean hasActiveWeaponDeployment() {
      return this.getSiegeWeapon().isPresent() || this.siegeBrain.findNearestWeapon().isPresent();
   }

   private boolean canAttackTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target) && (target == this.m_5448_() || this.isAutomaticSiegeTarget(target));
   }

   private boolean isAutomaticSiegeTarget(LivingEntity target) {
      return NpcTargeting.isAutomaticHostile(this, target) || this.isHostileFactionNpc(target);
   }

   private boolean isHostileFactionNpc(LivingEntity target) {
      if (target instanceof FactionNpcEntity factionNpc && NpcTargeting.canHarm(this, target) && factionNpc.isHostileTowards(this)) {
         return true;
      }

      return false;
   }

   private static class FindSiegeWeaponGoal extends Goal {
      private final SiegeEngineerNpcEntity engineer;
      private Entity targetEntity;
      private SiegeWeaponControl targetWeapon;
      private int rescanDelay;

      private FindSiegeWeaponGoal(SiegeEngineerNpcEntity engineer) {
         this.engineer = engineer;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.engineer.getSiegeWeapon().isPresent()) {
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
            && this.engineer.getSiegeWeapon().isEmpty();
      }

      public void m_8037_() {
         this.engineer.m_21563_().m_148051_(this.targetEntity);
         if (!this.engineer.siegeBrain.isCloseEnoughToOperate(this.targetEntity)) {
            this.engineer.m_21573_().m_5624_(this.targetEntity, 1.0);
         } else if (this.targetWeapon.beginOperation(this.engineer)) {
            this.engineer.setSiegeWeapon(this.targetWeapon);
         }
      }

      public void m_8041_() {
         this.targetEntity = null;
         this.targetWeapon = null;
         this.engineer.m_21573_().m_26573_();
      }
   }

   private static class OperateSiegeWeaponGoal extends Goal {
      private final SiegeEngineerNpcEntity engineer;
      private final SiegeEngineerBrain.FireDelay fireDelay = new SiegeEngineerBrain.FireDelay();

      private OperateSiegeWeaponGoal(SiegeEngineerNpcEntity engineer) {
         this.engineer = engineer;
         this.m_7021_(EnumSet.of(Flag.LOOK));
      }

      public boolean m_8036_() {
         return this.engineer.getSiegeWeapon().isPresent();
      }

      public boolean m_8045_() {
         return this.m_8036_();
      }

      public void m_8037_() {
         this.engineer.siegeBrain.tickWeapon(this.engineer::canAttackTarget, this.fireDelay);
      }

      public void m_8041_() {
         this.fireDelay.value = 0;
      }
   }
}
