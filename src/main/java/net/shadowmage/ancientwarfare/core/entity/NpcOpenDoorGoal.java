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
   private BlockPos doorPos = BlockPos.f_121853_;
   private boolean hasDoor;
   private boolean passed;
   private boolean openedByNpc;
   private int forgetTicks;
   private float doorOpenDirX;
   private float doorOpenDirZ;

   NpcOpenDoorGoal(NpcBaseEntity npc) {
      this.npc = npc;
      this.navigation = npc.m_21573_();
   }

   public boolean m_8036_() {
      return !this.npc.m_9236_().f_46443_ && this.navigation.m_26572_() && this.navigation.m_26575_().m_77360_() ? this.findDoorOnPath() : false;
   }

   public boolean m_8045_() {
      return this.hasDoor && !this.passed && this.forgetTicks > 0;
   }

   public void m_8056_() {
      this.passed = false;
      this.forgetTicks = 40;
      this.doorOpenDirX = (float)((double)this.doorPos.m_123341_() + 0.5 - this.npc.m_20185_());
      this.doorOpenDirZ = (float)((double)this.doorPos.m_123343_() + 0.5 - this.npc.m_20189_());
      this.openedByNpc = !this.isOpen();
      this.setOpen(true);
   }

   public void m_8037_() {
      this.forgetTicks--;
      this.setOpen(true);
      float currentDoorX = (float)((double)this.doorPos.m_123341_() + 0.5 - this.npc.m_20185_());
      float currentDoorZ = (float)((double)this.doorPos.m_123343_() + 0.5 - this.npc.m_20189_());
      if (this.doorOpenDirX * currentDoorX + this.doorOpenDirZ * currentDoorZ < 0.0F) {
         this.passed = true;
      }
   }

   public void m_8041_() {
      if (this.openedByNpc) {
         this.setOpen(false);
      }

      this.hasDoor = false;
      this.passed = false;
      this.openedByNpc = false;
      this.forgetTicks = 0;
   }

   public boolean m_183429_() {
      return true;
   }

   private boolean findDoorOnPath() {
      Path path = this.navigation.m_26570_();
      if (path != null && !path.m_77392_()) {
         int start = path.m_77399_();
         int end = Math.min(path.m_77398_(), start + 3);

         for (int index = start; index < end; index++) {
            Node node = path.m_77375_(index);
            BlockPos candidate = new BlockPos(node.f_77271_, node.f_77272_ + 1, node.f_77273_);
            if (this.npc.m_20275_((double)candidate.m_123341_(), this.npc.m_20186_(), (double)candidate.m_123343_()) <= 2.25
               && DoorBlock.m_52745_(this.npc.m_9236_(), candidate)) {
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
         && this.npc.m_9236_().m_8055_(this.doorPos).m_60734_() instanceof DoorBlock
         && (Boolean)this.npc.m_9236_().m_8055_(this.doorPos).m_61143_(DoorBlock.f_52727_);
   }

   private void setOpen(boolean open) {
      if (this.hasDoor) {
         BlockState state = this.npc.m_9236_().m_8055_(this.doorPos);
         if (state.m_60734_() instanceof DoorBlock door && state.m_61138_(DoorBlock.f_52727_) && (Boolean)state.m_61143_(DoorBlock.f_52727_) != open) {
            door.m_153165_(this.npc, this.npc.m_9236_(), state, this.doorPos, open);
         }
      }
   }
}
