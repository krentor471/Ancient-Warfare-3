package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;

public class ModernSoldierNpcEntity extends WeaponOperatorNpcEntity {
   public ModernSoldierNpcEntity(EntityType<? extends ModernSoldierNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.setNpcType("modern_soldier");
      this.setNpcSubtype("modern_soldier");
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.targetSelector.addGoal(2, new VerticalRangeTargetGoal(this, FactionNpcEntity.class, 10, false, false, this::canAttackTarget, 96.0));
      this.targetSelector.addGoal(3, new VerticalRangeTargetGoal<LivingEntity>(this, LivingEntity.class, 10, false, false, this::canAttackTarget, 96.0));
   }

   private boolean canAttackTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target)
         && (NpcTargeting.isAutomaticHostile(this, target) || target instanceof FactionNpcEntity faction && faction.isHostileTowards(this));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setNpcType(this.getNpcType());
      this.setNpcSubtype(this.getNpcSubtype());
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
               new SimpleMenuProvider((id, inventory, opened) -> new NpcInventoryMenu(id, inventory, this), this.getDisplayName()),
               buffer -> buffer.writeInt(this.getId())
            );
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      }
   }

   @Override
   public boolean isSiegeEngineerProfession() {
      return false;
   }

   @Override
   public boolean isCombatGroupEligible() {
      return true;
   }

   @Override
   public boolean canReceiveAttackCommand() {
      return true;
   }

   @Override
   public String getNpcType() {
      return "modern_soldier";
   }

   @Override
   public String getNpcSubtype() {
      return "modern_soldier";
   }

   @Override
   public Component getTypeName() {
      return Component.translatable("entity.ancientwarfare.modern_soldier");
   }
}
