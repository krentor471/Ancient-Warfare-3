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
   protected void m_8099_() {
      super.m_8099_();
      this.f_21346_.m_25352_(2, new VerticalRangeTargetGoal(this, FactionNpcEntity.class, 10, false, false, this::canAttackTarget, 96.0));
      this.f_21346_.m_25352_(3, new VerticalRangeTargetGoal<LivingEntity>(this, LivingEntity.class, 10, false, false, this::canAttackTarget, 96.0));
   }

   private boolean canAttackTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target)
         && (NpcTargeting.isAutomaticHostile(this, target) || target instanceof FactionNpcEntity faction && faction.isHostileTowards(this));
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.setNpcType(this.getNpcType());
      this.setNpcSubtype(this.getNpcSubtype());
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
               new SimpleMenuProvider((id, inventory, opened) -> new NpcInventoryMenu(id, inventory, this), this.m_5446_()),
               buffer -> buffer.writeInt(this.m_19879_())
            );
         }

         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
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
   public Component m_5677_() {
      return Component.m_237115_("entity.ancientwarfare.modern_soldier");
   }
}
