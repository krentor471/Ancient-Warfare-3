package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

final class NpcOpenDoorGoal extends Goal {
   private static final int MAX_LOOKAHEAD_NODES = 3;
   private static final int MAX_DOOR_TICKS = 40;
   private static final double DOOR_LOOKAHEAD_DISTANCE_SQR = 2.25;
   private final NpcBaseEntity npc;
   private final PathNavigation navigation;
   private BlockPos doorPos = BlockPos.ZERO;
   private boolean hasDoor;
   private boolean passed;
   private boolean openedByNpc;
   private int forgetTicks;
   private float doorOpenDirX;
   private float doorOpenDirZ;

   NpcOpenDoorGoal(NpcBaseEntity npc) {
      this.npc = npc;
      this.navigation = npc.getNavigation();
   }

   public boolean canUse() {
      return !this.npc.level().isClientSide && this.navigation.isInProgress() && this.navigation.getNodeEvaluator().canOpenDoors() ? this.findDoorOnPath() : false;
   }

   public boolean canContinueToUse() {
      return this.hasDoor && !this.passed && this.forgetTicks > 0;
   }

   public void start() {
      this.passed = false;
      this.forgetTicks = 40;
      this.doorOpenDirX = (float)((double)this.doorPos.getX() + 0.5 - this.npc.getX());
      this.doorOpenDirZ = (float)((double)this.doorPos.getZ() + 0.5 - this.npc.getZ());
      this.openedByNpc = !this.isOpen();
      this.setOpen(true);
   }

   public void tick() {
      this.forgetTicks--;
      this.setOpen(true);
      float currentDoorX = (float)((double)this.doorPos.getX() + 0.5 - this.npc.getX());
      float currentDoorZ = (float)((double)this.doorPos.getZ() + 0.5 - this.npc.getZ());
      if (this.doorOpenDirX * currentDoorX + this.doorOpenDirZ * currentDoorZ < 0.0F) {
         this.passed = true;
      }
   }

   public void stop() {
      if (this.openedByNpc) {
         this.setOpen(false);
      }

      this.hasDoor = false;
      this.passed = false;
      this.openedByNpc = false;
      this.forgetTicks = 0;
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   private boolean findDoorOnPath() {
      Path path = this.navigation.getPath();
      if (path != null && !path.isDone()) {
         int start = path.getNextNodeIndex();
         int end = Math.min(path.getNodeCount(), start + 3);

         for (int index = start; index < end; index++) {
            Node node = path.getNode(index);
            BlockPos candidate = new BlockPos(node.x, node.y + 1, node.z);
            if (this.npc.distanceToSqr((double)candidate.getX(), this.npc.getY(), (double)candidate.getZ()) <= 2.25
               && DoorBlock.isWoodenDoor(this.npc.level(), candidate)) {
               this.doorPos = candidate;
               this.hasDoor = true;
               return true;
            }
         }

         this.hasDoor = false;
         return false;
      } else {
         return false;
      }
   }

   private boolean isOpen() {
      return this.hasDoor
         && this.npc.level().getBlockState(this.doorPos).getBlock() instanceof DoorBlock
         && (Boolean)this.npc.level().getBlockState(this.doorPos).getValue(DoorBlock.OPEN);
   }

   private void setOpen(boolean open) {
      if (this.hasDoor) {
         BlockState state = this.npc.level().getBlockState(this.doorPos);
         if (state.getBlock() instanceof DoorBlock door && state.hasProperty(DoorBlock.OPEN) && (Boolean)state.getValue(DoorBlock.OPEN) != open) {
            door.setOpen(this.npc, this.npc.level(), state, this.doorPos, open);
         }
      }
   }
}
