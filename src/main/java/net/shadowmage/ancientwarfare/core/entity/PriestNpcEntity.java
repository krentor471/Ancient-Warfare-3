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
      return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 24.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new PriestNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.goalSelector.addGoal(2, new PriestNpcEntity.ResurrectionGoal(this, 1.0, 4.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
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
      return "priest";
   }

   @Override
   public String getNpcSubtype() {
      return "";
   }

   @Override
   public Component getTypeName() {
      return Component.translatable("entity.ancientwarfare.priest");
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.priest.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.priest.getCommandTarget();
            return !BlockPos.ZERO.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean canContinueToUse() {
         return this.priest.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void tick() {
         this.priest.getLookControl().setLookAt((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
         this.priest
            .getNavigation()
            .moveTo(
               (double)this.target.getX() + 0.5,
               (double)this.target.getY(),
               (double)this.target.getZ() + 0.5,
               this.priest.playerWalkNavigationSpeed()
            );
      }

      public void stop() {
         if (this.priest.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.priest.clearCommand();
         }

         this.target = null;
         this.priest.getNavigation().stop();
      }

      private double distanceToTargetSq() {
         return this.priest.distanceToSqr((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.priest.getCurrentCommand() != NpcCommandType.NONE || !this.priest.hasTownHall()) {
            return false;
         } else if (this.entry != null) {
            return true;
         } else if (this.priest.tickCount - this.lastCheckTick < 200) {
            return false;
         } else {
            this.lastCheckTick = this.priest.tickCount;
            return this.townHall() != null && this.townHall().claimResurrection().map(claimed -> {
               this.entry = claimed;
               this.priest.resurrectionDeployment = true;
               return true;
            }).orElse(false);
         }
      }

      public boolean canContinueToUse() {
         return this.entry != null && this.priest.getCurrentCommand() == NpcCommandType.NONE && this.priest.hasTownHall();
      }

      public void tick() {
         TownHallBlockEntity townHall = this.townHall();
         if (townHall != null && this.entry != null) {
            BlockPos pos = this.priest.getTownHallPos();
            double distanceSq = this.priest.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
            if (distanceSq > this.actionDistanceSq) {
               this.resurrectionTicks = 0;
               this.priest.getLookControl().setLookAt((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5);
               this.priest
                  .getNavigation()
                  .moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, this.priest.playerWalkNavigationSpeed());
            } else {
               this.priest.getNavigation().stop();
               this.priest.getLookControl().setLookAt((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5);
               if (this.resurrectionTicks % 20 == 0) {
                  this.priest.swing(InteractionHand.MAIN_HAND);
                  this.priest.level().playSound(null, this.priest.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 0.6F, 1.2F);
               }

               if (++this.resurrectionTicks >= 100) {
                  this.resurrect(townHall);
               }
            }
         } else {
            this.stop();
         }
      }

      public void stop() {
         TownHallBlockEntity townHall = this.townHall();
         if (townHall != null && this.entry != null) {
            townHall.releaseResurrection(this.entry);
         }

         this.entry = null;
         this.resurrectionTicks = 0;
         this.priest.resurrectionDeployment = false;
         this.priest.getNavigation().stop();
      }

      private void resurrect(TownHallBlockEntity townHall) {
         PlayerOwnedNpcEntity restored = NpcSpawnerItem.createNpcFromStack(this.priest.level(), this.entry.spawnerStack()) instanceof PlayerOwnedNpcEntity ownedNpc
            ? ownedNpc
            : null;
         boolean spawned = false;
         if (restored != null) {
            restored.setOwner(this.priest.getOwner().orElse(null));
            restored.setHealth(Math.max(1.0F, restored.getMaxHealth() / 2.0F));
            restored.moveTo(this.priest.getX(), this.priest.getY(), this.priest.getZ(), this.priest.getYRot(), this.priest.getXRot());
            restored.setDeltaMovement(0.0, 0.0, 0.0);
            spawned = this.priest.level().addFreshEntity(restored);
         }

         townHall.completeResurrection(this.entry, spawned);
         this.entry = null;
         this.resurrectionTicks = 0;
      }

      private TownHallBlockEntity townHall() {
         return this.priest.hasTownHall() && this.priest.level().getBlockEntity(this.priest.getTownHallPos()) instanceof TownHallBlockEntity townHall
            ? townHall
            : null;
      }
   }
}
