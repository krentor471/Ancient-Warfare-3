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
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean canUse() {
      return this.npc.shouldFollowCombatGroupFormation()
         && this.npc.getCombatGroupFormationPosition().map(position -> this.moverPosition().distanceToSqr(position) > 2.25).orElse(false);
   }

   public boolean canContinueToUse() {
      return this.canUse();
   }

   public void start() {
      this.repathTicks = 0;
   }

   public void tick() {
      Vec3 destination = this.npc.getCombatGroupFormationPosition().orElse(null);
      if (destination != null) {
         this.npc.getLookControl().setLookAt(destination.x, destination.y, destination.z);
         if (!this.npc.isPassenger()) {
            if (this.repathTicks-- <= 0 || this.npc.getNavigation().isDone()) {
               double distance = this.npc.position().distanceTo(destination);
               double speed = distance > 12.0 ? this.npc.playerSprintNavigationSpeed() * 1.2 : this.npc.playerWalkNavigationSpeed();
               this.npc.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
               this.repathTicks = 8;
            }
         }
      }
   }

   public void stop() {
      this.repathTicks = 0;
      if (this.npc.shouldFollowCombatGroupFormation()) {
         this.npc.getNavigation().stop();
      }
   }

   private Vec3 moverPosition() {
      return this.npc.getVehicle() == null ? this.npc.position() : this.npc.getVehicle().position();
   }
}
