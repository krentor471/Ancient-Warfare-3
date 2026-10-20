package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface PlatformClientNetwork {
   void addWorkOrderPoint(BlockPos var1, Direction var2);

   void clearCommandBatonSelections();

   void setVehicleInput(float var1, float var2);

   void adjustVehicleWeapon(float var1, float var2);

   void fireVehicleWeapon();

   void sendTraderToRoute(int var1, BlockPos var2);
}
