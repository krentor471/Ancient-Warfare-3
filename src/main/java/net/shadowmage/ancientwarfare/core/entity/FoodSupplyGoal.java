package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

final class FoodSupplyGoal extends Goal {
   private final PlayerOwnedNpcEntity npc;

   FoodSupplyGoal(PlayerOwnedNpcEntity npc) {
      this.npc = npc;
      this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean m_8036_() {
      return !this.npc.m_9236_().f_46443_ && this.npc.isGettingFood() && !this.npc.m_5803_() && !this.npc.isFoodSupplyBlockedByDeployment();
   }

   public boolean m_8045_() {
      return this.m_8036_();
   }

   public void m_8041_() {
      this.npc.m_21573_().m_26573_();
   }
}
