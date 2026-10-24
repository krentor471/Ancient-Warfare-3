package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;

final class FollowPlayerGoal extends Goal {
   private final PlayerOwnedNpcEntity npc;
   private final float stopDistanceSq;
   private final float startDistanceSq;
   private Player player;
   private int repathTicks;
   private int failedPathTicks;
   private int teleportCooldown;
   private static final double ACTIVE_TELEPORT_DISTANCE_SQ = 2304.0;

   FollowPlayerGoal(PlayerOwnedNpcEntity npc, float stopDistance, float startDistance) {
      this.npc = npc;
      this.stopDistanceSq = stopDistance * stopDistance;
      this.startDistanceSq = startDistance * startDistance;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean canUse() {
      if (this.canFollow() && !this.npc.isPassenger()) {
         this.player = this.npc.getFollowTargetPlayer().orElse(null);
         return this.player != null && this.npc.distanceToSqr(this.player) > (double)this.startDistanceSq;
      } else {
         return false;
      }
   }

   public boolean canContinueToUse() {
      return this.player != null && this.canFollow() && !this.npc.isPassenger() && this.npc.distanceToSqr(this.player) > (double)this.stopDistanceSq;
   }

   public void start() {
      this.repathTicks = 0;
      this.failedPathTicks = 0;
      this.teleportCooldown = 0;
   }

   public void tick() {
      this.npc.getLookControl().setLookAt(this.player, 10.0F, (float)this.npc.getMaxHeadXRot());
      if (this.teleportCooldown > 0) {
         this.teleportCooldown--;
      }

      double distanceSq = this.npc.distanceToSqr(this.player);
      if (distanceSq > 2304.0 && this.teleportCooldown <= 0) {
         this.teleportCooldown = 40;
         if (this.tryTeleportNearPlayer()) {
            this.failedPathTicks = 0;
            this.repathTicks = 10;
            return;
         }
      }

      if (this.repathTicks-- <= 0 || this.npc.getNavigation().isDone()) {
         boolean pathStarted = this.npc.getNavigation().moveTo(this.player, this.npc.playerSprintNavigationSpeed());
         this.failedPathTicks = pathStarted ? 0 : this.failedPathTicks + 10;
         if (distanceSq > 576.0 && this.failedPathTicks >= 20 && this.teleportCooldown <= 0) {
            this.tryTeleportNearPlayer();
            this.failedPathTicks = 0;
            this.teleportCooldown = 40;
         }

         this.repathTicks = 10;
      }
   }

   public void stop() {
      this.player = null;
      this.repathTicks = 0;
      this.failedPathTicks = 0;
      this.teleportCooldown = 0;
      this.npc.getNavigation().stop();
   }

   private boolean canFollow() {
      if (this.npc.getCurrentCommand() == NpcCommandType.FOLLOW && !this.npc.isGuiInteractionPaused()) {
         LivingEntity target = this.npc.getTarget();
         return this.npc.isFollowCombatRecallActive() || target == null || !target.isAlive();
      } else {
         return false;
      }
   }

   private boolean tryTeleportNearPlayer() {
      BlockPos center = this.player.blockPosition();

      for (int attempt = 0; attempt < 12; attempt++) {
         int dx = this.npc.getRandom().nextInt(9) - 4;
         int dz = this.npc.getRandom().nextInt(9) - 4;
         int dy = this.npc.getRandom().nextInt(5) - 2;
         if (Math.abs(dx) >= 2 || Math.abs(dz) >= 2) {
            BlockPos pos = center.offset(dx, dy, dz);
            if (this.npc.level().hasChunkAt(pos) && this.npc.level().getWorldBorder().isWithinBounds(pos)) {
               BlockPos floor = pos.below();
               if (this.npc.level().getBlockState(floor).isFaceSturdy(this.npc.level(), floor, Direction.UP)) {
                  double x = (double)pos.getX() + 0.5;
                  double y = (double)pos.getY();
                  double z = (double)pos.getZ() + 0.5;
                  if (this.npc
                     .level()
                     .noCollision(this.npc, this.npc.getBoundingBox().move(x - this.npc.getX(), y - this.npc.getY(), z - this.npc.getZ()))) {
                     this.npc.getNavigation().stop();
                     this.npc.teleportTo(x, y, z);
                     this.npc.setDeltaMovement(Vec3.ZERO);
                     this.npc.fallDistance = 0.0F;
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }
}
