package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
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
   private BlockPos pendingClose;
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
      this.closePendingDoor();
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
      // Never shut a door or gate on the NPC standing in it: it would be stuck inside the closed block. It is shut once
      // the NPC has stepped out instead, so a pen gate does not stay open behind a farmer.
      if (this.openedByNpc) {
         if (this.npc.getBoundingBox().intersects(new AABB(this.doorPos))) {
            this.pendingClose = this.doorPos;
         } else {
            this.setOpen(false);
         }
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
            // A gate sits at the node itself; for a door either half will do.
            for (int dy = 0; dy <= 1; dy++) {
               BlockPos candidate = new BlockPos(node.x, node.y + dy, node.z);
               if (this.npc.distanceToSqr((double)candidate.getX(), this.npc.getY(), (double)candidate.getZ()) <= 2.25
                  && isOpenable(this.npc.level().getBlockState(candidate))) {
                  this.doorPos = candidate;
                  this.hasDoor = true;
                  return true;
               }
            }
         }

         this.hasDoor = false;
         return false;
      } else {
         return false;
      }
   }

   /** Wooden doors and fence gates: what an NPC may open with its hands. */
   private static boolean isOpenable(BlockState state) {
      return state.getBlock() instanceof DoorBlock door && door.type().canOpenByHand() || state.getBlock() instanceof FenceGateBlock;
   }

   private void closePendingDoor() {
      if (this.pendingClose != null && !this.npc.getBoundingBox().intersects(new AABB(this.pendingClose))) {
         BlockPos current = this.doorPos;
         boolean had = this.hasDoor;
         this.doorPos = this.pendingClose;
         this.hasDoor = true;
         this.setOpen(false);
         this.doorPos = current;
         this.hasDoor = had;
         this.pendingClose = null;
      }
   }

   private boolean isOpen() {
      BlockState state = this.npc.level().getBlockState(this.doorPos);
      return this.hasDoor && isOpenable(state) && state.getValue(BlockStateProperties.OPEN);
   }

   private void setOpen(boolean open) {
      if (this.hasDoor) {
         BlockState state = this.npc.level().getBlockState(this.doorPos);
         if (state.getBlock() instanceof DoorBlock door && state.hasProperty(DoorBlock.OPEN) && (Boolean)state.getValue(DoorBlock.OPEN) != open) {
            door.setOpen(this.npc, this.npc.level(), state, this.doorPos, open);
         } else if (state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN) != open) {
            this.npc.level().setBlock(this.doorPos, state.setValue(FenceGateBlock.OPEN, open), 10);
            this.npc.level().playSound(null, this.doorPos, open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.npc.level().gameEvent(this.npc, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, this.doorPos);
         }
      }
   }
}
