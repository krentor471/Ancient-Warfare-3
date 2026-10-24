package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

final class FoodSupplyGoal extends Goal {
   private final PlayerOwnedNpcEntity npc;

   FoodSupplyGoal(PlayerOwnedNpcEntity npc) {
      this.npc = npc;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean canUse() {
      return !this.npc.level().isClientSide && this.npc.isGettingFood() && !this.npc.isSleeping() && !this.npc.isFoodSupplyBlockedByDeployment();
   }

   public boolean canContinueToUse() {
      return this.canUse();
   }

   public void stop() {
      this.npc.getNavigation().stop();
   }
}
