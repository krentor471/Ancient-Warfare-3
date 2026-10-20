package net.shadowmage.ancientwarfare.core.common.torque;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface TorqueDemandNode {
   double getNetworkTorqueDemand(Direction var1, Set<BlockPos> var2, int var3);
}
