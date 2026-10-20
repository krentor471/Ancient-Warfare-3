package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.shadowmage.ancientwarfare.core.item.NpcSpawnerItem;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

public class PriestNpcEntity extends PlayerOwnedNpcEntity {
   private boolean resurrectionDeployment;

   public PriestNpcEntity(EntityType<? extends PriestNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.setNpcType("priest");
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.25).m_22268_(Attributes.f_22277_, 24.0);
   }

   protected void m_8099_() {
      this.f_21345_.m_25352_(0, new FloatGoal(this));
      this.f_21345_.m_25352_(2, new PriestNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.f_21345_.m_25352_(2, new PriestNpcEntity.ResurrectionGoal(this, 1.0, 4.0));
      this.f_21345_.m_25352_(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.f_21345_.m_25352_(7, new RandomLookAroundGoal(this));
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
      return "priest";
   }

   @Override
   public String getNpcSubtype() {
      return "";
   }

   @Override
   public Component m_5677_() {
      return Component.m_237115_("entity.ancientwarfare.priest");
   }

   boolean hasActiveResurrectionDeployment() {
      return this.resurrectionDeployment;
   }

   private static class MoveToCommandGoal extends Goal {
      private final PriestNpcEntity priest;
      private final double speedModifier;
      private final double stopDistanceSq;
      private BlockPos target;

      private MoveToCommandGoal(PriestNpcEntity priest, double speedModifier, double stopDistance) {
         this.priest = priest;
         this.speedModifier = speedModifier;
         this.stopDistanceSq = stopDistance * stopDistance;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.priest.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.priest.getCommandTarget();
            return !BlockPos.f_121853_.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean m_8045_() {
         return this.priest.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void m_8037_() {
         this.priest.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
         this.priest
            .m_21573_()
            .m_26519_(
               (double)this.target.m_123341_() + 0.5,
               (double)this.target.m_123342_(),
               (double)this.target.m_123343_() + 0.5,
               this.priest.playerWalkNavigationSpeed()
            );
      }

      public void m_8041_() {
         if (this.priest.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.priest.clearCommand();
         }

         this.target = null;
         this.priest.m_21573_().m_26573_();
      }

      private double distanceToTargetSq() {
         return this.priest.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
      }
   }

   private static class ResurrectionGoal extends Goal {
      private static final int CHECK_INTERVAL_TICKS = 200;
      private static final int RESURRECTION_TIME_TICKS = 100;
      private final PriestNpcEntity priest;
      private final double speedModifier;
      private final double actionDistanceSq;
      private TownHallBlockEntity.NpcDeathEntry entry;
      private int lastCheckTick = -200;
      private int resurrectionTicks;

      private ResurrectionGoal(PriestNpcEntity priest, double speedModifier, double actionDistance) {
         this.priest = priest;
         this.speedModifier = speedModifier;
         this.actionDistanceSq = actionDistance * actionDistance;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.priest.getCurrentCommand() != NpcCommandType.NONE || !this.priest.hasTownHall()) {
            return false;
         } else if (this.entry != null) {
            return true;
         } else if (this.priest.f_19797_ - this.lastCheckTick < 200) {
            return false;
         } else {
            this.lastCheckTick = this.priest.f_19797_;
            return this.townHall() != null && this.townHall().claimResurrection().map(claimed -> {
               this.entry = claimed;
               this.priest.resurrectionDeployment = true;
               return true;
            }).orElse(false);
         }
      }

      public boolean m_8045_() {
         return this.entry != null && this.priest.getCurrentCommand() == NpcCommandType.NONE && this.priest.hasTownHall();
      }

      public void m_8037_() {
         TownHallBlockEntity townHall = this.townHall();
         if (townHall != null && this.entry != null) {
            BlockPos pos = this.priest.getTownHallPos();
            double distanceSq = this.priest.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5);
            if (distanceSq > this.actionDistanceSq) {
               this.resurrectionTicks = 0;
               this.priest.m_21563_().m_24946_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5);
               this.priest
                  .m_21573_()
                  .m_26519_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5, this.priest.playerWalkNavigationSpeed());
            } else {
               this.priest.m_21573_().m_26573_();
               this.priest.m_21563_().m_24946_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5);
               if (this.resurrectionTicks % 20 == 0) {
                  this.priest.m_6674_(InteractionHand.MAIN_HAND);
                  this.priest.m_9236_().m_5594_(null, this.priest.m_20183_(), SoundEvents.f_11887_, SoundSource.NEUTRAL, 0.6F, 1.2F);
               }

               if (++this.resurrectionTicks >= 100) {
                  this.resurrect(townHall);
               }
            }
         } else {
            this.m_8041_();
         }
      }

      public void m_8041_() {
         TownHallBlockEntity townHall = this.townHall();
         if (townHall != null && this.entry != null) {
            townHall.releaseResurrection(this.entry);
         }

         this.entry = null;
         this.resurrectionTicks = 0;
         this.priest.resurrectionDeployment = false;
         this.priest.m_21573_().m_26573_();
      }

      private void resurrect(TownHallBlockEntity townHall) {
         PlayerOwnedNpcEntity restored = NpcSpawnerItem.createNpcFromStack(this.priest.m_9236_(), this.entry.spawnerStack()) instanceof PlayerOwnedNpcEntity ownedNpc
            ? ownedNpc
            : null;
         boolean spawned = false;
         if (restored != null) {
            restored.setOwner(this.priest.getOwner().orElse(null));
            restored.m_21153_(Math.max(1.0F, restored.m_21233_() / 2.0F));
            restored.m_7678_(this.priest.m_20185_(), this.priest.m_20186_(), this.priest.m_20189_(), this.priest.m_146908_(), this.priest.m_146909_());
            restored.m_20334_(0.0, 0.0, 0.0);
            spawned = this.priest.m_9236_().m_7967_(restored);
         }

         townHall.completeResurrection(this.entry, spawned);
         this.entry = null;
         this.resurrectionTicks = 0;
      }

      private TownHallBlockEntity townHall() {
         return this.priest.hasTownHall() && this.priest.m_9236_().m_7702_(this.priest.getTownHallPos()) instanceof TownHallBlockEntity townHall
            ? townHall
            : null;
      }
   }
}
