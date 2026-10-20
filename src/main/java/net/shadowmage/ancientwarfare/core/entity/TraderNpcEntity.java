package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;

public class TraderNpcEntity extends PlayerOwnedNpcEntity {
   public TraderNpcEntity(EntityType<? extends TraderNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.setNpcType("trader");
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.25).m_22268_(Attributes.f_22277_, 24.0);
   }

   protected void m_8099_() {
      this.f_21345_.m_25352_(0, new FloatGoal(this));
      this.f_21345_.m_25352_(2, new TraderNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.f_21345_.m_25352_(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.f_21345_.m_25352_(7, new RandomLookAroundGoal(this));
   }

   @Override
   public void m_8119_() {
      if (!this.m_9236_().f_46443_ && this.getCurrentCommand() == NpcCommandType.NONE && !this.isGettingFood() && !this.isGuiInteractionPaused()) {
         this.m_21573_().m_26573_();
      }

      super.m_8119_();
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
         if (!this.m_9236_().f_46443_) {
            if (this.hasCommandPermission(player)) {
               AWCorePlatform.openMenu(
                  player,
                  new SimpleMenuProvider((containerId, inventory, openedPlayer) -> new NpcInventoryMenu(containerId, inventory, this), this.m_5446_()),
                  buffer -> buffer.writeInt(this.m_19879_())
               );
            } else {
               player.m_5661_(Component.m_237115_("guistrings.npc.trade.not_ported_yet"), true);
            }
         }

         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      }
   }

   @Override
   public String getNpcType() {
      return "trader";
   }

   @Override
   public String getNpcSubtype() {
      return "";
   }

   @Override
   public Component m_5677_() {
      return Component.m_237115_("entity.ancientwarfare.trader");
   }

   private static class MoveToCommandGoal extends Goal {
      private final TraderNpcEntity trader;
      private final double speedModifier;
      private final double stopDistanceSq;
      private BlockPos target;

      private MoveToCommandGoal(TraderNpcEntity trader, double speedModifier, double stopDistance) {
         this.trader = trader;
         this.speedModifier = speedModifier;
         this.stopDistanceSq = stopDistance * stopDistance;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.trader.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.trader.getCommandTarget();
            return !BlockPos.f_121853_.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean m_8045_() {
         return this.trader.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void m_8037_() {
         this.trader.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
         this.trader
            .m_21573_()
            .m_26519_(
               (double)this.target.m_123341_() + 0.5,
               (double)this.target.m_123342_(),
               (double)this.target.m_123343_() + 0.5,
               this.trader.playerWalkNavigationSpeed()
            );
      }

      public void m_8041_() {
         if (this.trader.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.trader.clearCommand();
         }

         this.target = null;
         this.trader.m_21573_().m_26573_();
      }

      private double distanceToTargetSq() {
         return this.trader.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
      }
   }
}
