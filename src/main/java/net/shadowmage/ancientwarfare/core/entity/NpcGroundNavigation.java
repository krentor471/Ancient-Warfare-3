package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/** Ground navigation for NPCs that plans through closed fence gates the way it does through doors, so pens are not walls. */
class NpcGroundNavigation extends GroundPathNavigation {
   NpcGroundNavigation(Mob mob, Level level) {
      super(mob, level);
   }

   @Override
   protected PathFinder createPathFinder(int maxVisitedNodes) {
      this.nodeEvaluator = new GateNodeEvaluator();
      this.nodeEvaluator.setCanPassDoors(true);
      return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
   }

   private static final class GateNodeEvaluator extends WalkNodeEvaluator {
      @Override
      public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
         if (this.canOpenDoors()) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN)) {
               // NpcOpenDoorGoal opens it on the way, as with a wooden door.
               return BlockPathTypes.DOOR_WOOD_CLOSED;
            }
         }

         return super.getBlockPathType(level, x, y, z);
      }
   }
}
