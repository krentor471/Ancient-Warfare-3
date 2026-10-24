package net.shadowmage.ancientwarfare.core.tile;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueDemandNode;

final class TorqueNetworkDemand {
   private static final int MAX_SEARCH_DEPTH = 128;

   private TorqueNetworkDemand() {
   }

   static double downstreamDemand(Level level, BlockPos sourcePos, Direction outputSide) {
      Set<BlockPos> visited = new HashSet<>();
      visited.add(sourcePos);
      return downstreamDemand(level, sourcePos.relative(outputSide), outputSide.getOpposite(), visited, 0);
   }

   static double downstreamDemand(Level level, BlockPos targetPos, Direction targetSide, Set<BlockPos> visited, int depth) {
      if (depth < 128 && visited.add(targetPos)) {
         BlockEntity blockEntity = level.getBlockEntity(targetPos);
         if (!(blockEntity instanceof TorqueAccess access) || !access.canInputTorque(targetSide)) {
            return 0.0;
         }

         if (blockEntity instanceof TorqueDemandNode demandNode) {
            return demandNode.getNetworkTorqueDemand(targetSide, visited, depth + 1);
         }

         return Math.max(0.0, access.getMaxTorqueInput(targetSide));
      } else {
         return 0.0;
      }
   }
}
