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
      this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean m_8036_() {
      if (this.canFollow() && !this.npc.m_20159_()) {
         this.player = this.npc.getFollowTargetPlayer().orElse(null);
         return this.player != null && this.npc.m_20280_(this.player) > (double)this.startDistanceSq;
      } else {
         return false;
      }
   }

   public boolean m_8045_() {
      return this.player != null && this.canFollow() && !this.npc.m_20159_() && this.npc.m_20280_(this.player) > (double)this.stopDistanceSq;
   }

   public void m_8056_() {
      this.repathTicks = 0;
      this.failedPathTicks = 0;
      this.teleportCooldown = 0;
   }

   public void m_8037_() {
      this.npc.m_21563_().m_24960_(this.player, 10.0F, (float)this.npc.m_8132_());
      if (this.teleportCooldown > 0) {
         this.teleportCooldown--;
      }

      double distanceSq = this.npc.m_20280_(this.player);
      if (distanceSq > 2304.0 && this.teleportCooldown <= 0) {
         this.teleportCooldown = 40;
         if (this.tryTeleportNearPlayer()) {
            this.failedPathTicks = 0;
            this.repathTicks = 10;
            return;
         }
      }

      if (this.repathTicks-- <= 0 || this.npc.m_21573_().m_26571_()) {
         boolean pathStarted = this.npc.m_21573_().m_5624_(this.player, this.npc.playerSprintNavigationSpeed());
         this.failedPathTicks = pathStarted ? 0 : this.failedPathTicks + 10;
         if (distanceSq > 576.0 && this.failedPathTicks >= 20 && this.teleportCooldown <= 0) {
            this.tryTeleportNearPlayer();
            this.failedPathTicks = 0;
            this.teleportCooldown = 40;
         }

         this.repathTicks = 10;
      }
   }

   public void m_8041_() {
      this.player = null;
      this.repathTicks = 0;
      this.failedPathTicks = 0;
      this.teleportCooldown = 0;
      this.npc.m_21573_().m_26573_();
   }

   private boolean canFollow() {
      if (this.npc.getCurrentCommand() == NpcCommandType.FOLLOW && !this.npc.isGuiInteractionPaused()) {
         LivingEntity target = this.npc.m_5448_();
         return this.npc.isFollowCombatRecallActive() || target == null || !target.m_6084_();
      } else {
         return false;
      }
   }

   private boolean tryTeleportNearPlayer() {
      BlockPos center = this.player.m_20183_();

      for (int attempt = 0; attempt < 12; attempt++) {
         int dx = this.npc.m_217043_().m_188503_(9) - 4;
         int dz = this.npc.m_217043_().m_188503_(9) - 4;
         int dy = this.npc.m_217043_().m_188503_(5) - 2;
         if (Math.abs(dx) >= 2 || Math.abs(dz) >= 2) {
            BlockPos pos = center.m_7918_(dx, dy, dz);
            if (this.npc.m_9236_().m_46805_(pos) && this.npc.m_9236_().m_6857_().m_61937_(pos)) {
               BlockPos floor = pos.m_7495_();
               if (this.npc.m_9236_().m_8055_(floor).m_60783_(this.npc.m_9236_(), floor, Direction.UP)) {
                  double x = (double)pos.m_123341_() + 0.5;
                  double y = (double)pos.m_123342_();
                  double z = (double)pos.m_123343_() + 0.5;
                  if (this.npc
                     .m_9236_()
                     .m_45756_(this.npc, this.npc.m_20191_().m_82386_(x - this.npc.m_20185_(), y - this.npc.m_20186_(), z - this.npc.m_20189_()))) {
                     this.npc.m_21573_().m_26573_();
                     this.npc.m_6021_(x, y, z);
                     this.npc.m_20256_(Vec3.f_82478_);
                     this.npc.f_19789_ = 0.0F;
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }
}
