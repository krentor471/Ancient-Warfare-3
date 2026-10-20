package net.shadowmage.ancientwarfare.core.common.torque;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;

public interface TorqueAccess {
   double getMaxTorque(@Nullable Direction var1);

   double getTorqueStored(@Nullable Direction var1);

   double addTorque(@Nullable Direction var1, double var2);

   double drainTorque(@Nullable Direction var1, double var2);

   double getMaxTorqueOutput(Direction var1);

   double getMaxTorqueInput(@Nullable Direction var1);

   boolean canOutputTorque(Direction var1);

   boolean canInputTorque(@Nullable Direction var1);

   default boolean useOutputRotation(@Nullable Direction side) {
      return false;
   }

   default float getClientOutputRotation(Direction side, float delta) {
      return 0.0F;
   }
}
