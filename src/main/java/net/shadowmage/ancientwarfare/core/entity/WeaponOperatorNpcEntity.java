package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
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
import net.shadowmage.ancientwarfare.core.common.siege.SiegeWeaponControl;

public abstract class WeaponOperatorNpcEntity extends PlayerOwnedNpcEntity {
   protected final SiegeEngineerBrain siegeBrain = new SiegeEngineerBrain(this);

   protected WeaponOperatorNpcEntity(EntityType<? extends WeaponOperatorNpcEntity> type, Level level) {
      super(type, level);
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 120.0);
   }

   @Override
   public boolean isCombatGroupEligible() {
      return true;
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new WeaponOperatorNpcEntity.MoveToCommandGoal(this));
      this.goalSelector.addGoal(5, new CombatGroupFollowGoal(this));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   public Optional<SiegeWeaponControl> getSiegeWeapon() {
      return this.siegeBrain.getSiegeWeapon();
   }

   public void setSiegeWeapon(SiegeWeaponControl weapon) {
      this.siegeBrain.setSiegeWeapon(weapon);
   }

   public void clearSiegeWeapon() {
      this.siegeBrain.clearSiegeWeapon();
   }

   @Override
   protected boolean hasActiveWeaponDeployment() {
      return this.getSiegeWeapon().filter(weapon -> {
         if (weapon instanceof Entity entity && (!entity.isAlive() || !this.isSiegeEngineerProfession() && this.getVehicle() != entity)) {
            return false;
         }

         return true;
      }).isPresent();
   }

   @Override
   public void aiStep() {
      if (!this.level().isClientSide) {
         this.siegeBrain.getSiegeWeapon().filter(weapon -> {
            if (weapon instanceof Entity entity && (!entity.isAlive() || !this.isSiegeEngineerProfession() && this.getVehicle() != entity)) {
               return true;
            }

            return false;
         }).ifPresent(weapon -> this.clearSiegeWeapon());
      }

      super.aiStep();
      if (this.isCombatGroupCommander()) {
         CombatGroupController.tick(this);
      }
   }

   private static final class MoveToCommandGoal extends Goal {
      private final WeaponOperatorNpcEntity npc;
      private BlockPos target;

      private MoveToCommandGoal(WeaponOperatorNpcEntity npc) {
         this.npc = npc;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.npc.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.npc.getCommandTarget();
            return !BlockPos.ZERO.equals(this.target) && this.distanceToTargetSq() > 4.0;
         }
      }

      public boolean canContinueToUse() {
         return this.npc.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > 4.0;
      }

      public void tick() {
         this.npc.getLookControl().setLookAt((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
         this.npc.getNavigation().moveTo((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5, 1.0);
      }

      public void stop() {
         if (this.npc.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= 4.0) {
            this.npc.completeMoveCommand();
         }

         this.target = null;
         this.npc.getNavigation().stop();
      }

      private double distanceToTargetSq() {
         return this.npc.distanceToSqr((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
      }
   }
}
