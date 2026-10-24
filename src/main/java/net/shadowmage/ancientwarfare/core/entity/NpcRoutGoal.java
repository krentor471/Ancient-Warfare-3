package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** A broken soldier runs away from where the danger came from until its morale recovers. */
final class NpcRoutGoal extends Goal {
   private static final double FLEE_SPEED = 1.3;
   private static final int RETRY_TICKS = 20;
   private final NpcBaseEntity npc;
   private int retryDelay;

   NpcRoutGoal(NpcBaseEntity npc) {
      this.npc = npc;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      return this.npc.isRouting() && !this.npc.isPassenger();
   }

   @Override
   public void start() {
      this.runAway();
   }

   @Override
   public void tick() {
      if (this.retryDelay > 0) {
         this.retryDelay--;
      } else if (this.npc.getNavigation().isDone() && (this.npc.tickCount + this.npc.getId()) % RETRY_TICKS == 0) {
         this.runAway();
      }
   }

   @Override
   public void stop() {
      this.npc.getNavigation().stop();
   }

   private void runAway() {
      Vec3 away = DefaultRandomPos.getPosAway(this.npc, 16, 7, this.npc.getRoutThreat());
      if (away != null) {
         this.npc.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
      }

      // Hemmed in or out of room: wait a moment before looking for a way out again.
      this.retryDelay = RETRY_TICKS;
   }
}
