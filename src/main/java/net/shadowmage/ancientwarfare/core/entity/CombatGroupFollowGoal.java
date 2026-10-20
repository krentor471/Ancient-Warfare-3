package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.phys.Vec3;

final class CombatGroupFollowGoal extends Goal {
   private static final double STOP_DISTANCE_SQ = 2.25;
   private final PlayerOwnedNpcEntity npc;
   private int repathTicks;

   CombatGroupFollowGoal(PlayerOwnedNpcEntity npc) {
      this.npc = npc;
      this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean m_8036_() {
      return this.npc.shouldFollowCombatGroupFormation()
         && this.npc.getCombatGroupFormationPosition().map(position -> this.moverPosition().m_82557_(position) > 2.25).orElse(false);
   }

   public boolean m_8045_() {
      return this.m_8036_();
   }

   public void m_8056_() {
      this.repathTicks = 0;
   }

   public void m_8037_() {
      Vec3 destination = this.npc.getCombatGroupFormationPosition().orElse(null);
      if (destination != null) {
         this.npc.m_21563_().m_24946_(destination.f_82479_, destination.f_82480_, destination.f_82481_);
         if (!this.npc.m_20159_()) {
            if (this.repathTicks-- <= 0 || this.npc.m_21573_().m_26571_()) {
               double distance = this.npc.m_20182_().m_82554_(destination);
               double speed = distance > 12.0 ? this.npc.playerSprintNavigationSpeed() * 1.2 : this.npc.playerWalkNavigationSpeed();
               this.npc.m_21573_().m_26519_(destination.f_82479_, destination.f_82480_, destination.f_82481_, speed);
               this.repathTicks = 8;
            }
         }
      }
   }

   public void m_8041_() {
      this.repathTicks = 0;
      if (this.npc.shouldFollowCombatGroupFormation()) {
         this.npc.m_21573_().m_26573_();
      }
   }

   private Vec3 moverPosition() {
      return this.npc.m_20202_() == null ? this.npc.m_20182_() : this.npc.m_20202_().m_20182_();
   }
}
