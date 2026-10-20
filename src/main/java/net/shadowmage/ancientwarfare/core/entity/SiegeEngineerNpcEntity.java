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
      return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 120.0);
   }

   @Override
   public boolean isSiegeEngineerProfession() {
      return true;
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(3, new SiegeEngineerNpcEntity.OperateSiegeWeaponGoal(this));
      this.goalSelector.addGoal(4, new SiegeEngineerNpcEntity.FindSiegeWeaponGoal(this));
      this.targetSelector.addGoal(2, new VerticalRangeTargetGoal<>(this, FactionNpcEntity.class, 10, false, false, this::isHostileFactionNpc, 96.0));
      this.targetSelector.addGoal(3, new VerticalRangeTargetGoal<LivingEntity>(this, LivingEntity.class, 10, false, false, this::isAutomaticSiegeTarget, 96.0));
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (this.tryAcceptFood(player, hand)) {
         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (player.getItemInHand(hand).getItem() instanceof CommandBatonItem) {
         if (!this.level().isClientSide && this.hasCommandPermission(player)) {
            CommandBatonItem.toggleNpcSelection(player.getItemInHand(hand), player, this);
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         if (!this.level().isClientSide && this.hasCommandPermission(player)) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider((containerId, inventory, openedPlayer) -> new NpcInventoryMenu(containerId, inventory, this), this.getDisplayName()),
               buffer -> buffer.writeInt(this.getId())
            );
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
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
   public Component getTypeName() {
      return Component.translatable("entity.ancientwarfare.siege_engineer");
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
      return NpcTargeting.canHarm(this, target) && (target == this.getTarget() || this.isAutomaticSiegeTarget(target));
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
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

      public boolean canContinueToUse() {
         return this.targetEntity != null
            && this.targetEntity.isAlive()
            && this.targetWeapon != null
            && this.targetWeapon.canBeOperatedBy(this.engineer)
            && this.engineer.getSiegeWeapon().isEmpty();
      }

      public void tick() {
         this.engineer.getLookControl().setLookAt(this.targetEntity);
         if (!this.engineer.siegeBrain.isCloseEnoughToOperate(this.targetEntity)) {
            this.engineer.getNavigation().moveTo(this.targetEntity, 1.0);
         } else if (this.targetWeapon.beginOperation(this.engineer)) {
            this.engineer.setSiegeWeapon(this.targetWeapon);
         }
      }

      public void stop() {
         this.targetEntity = null;
         this.targetWeapon = null;
         this.engineer.getNavigation().stop();
      }
   }

   private static class OperateSiegeWeaponGoal extends Goal {
      private final SiegeEngineerNpcEntity engineer;
      private final SiegeEngineerBrain.FireDelay fireDelay = new SiegeEngineerBrain.FireDelay();

      private OperateSiegeWeaponGoal(SiegeEngineerNpcEntity engineer) {
         this.engineer = engineer;
         this.setFlags(EnumSet.of(Flag.LOOK));
      }

      public boolean canUse() {
         return this.engineer.getSiegeWeapon().isPresent();
      }

      public boolean canContinueToUse() {
         return this.canUse();
      }

      public void tick() {
         this.engineer.siegeBrain.tickWeapon(this.engineer::canAttackTarget, this.fireDelay);
      }

      public void stop() {
         this.fireDelay.value = 0;
      }
   }
}
