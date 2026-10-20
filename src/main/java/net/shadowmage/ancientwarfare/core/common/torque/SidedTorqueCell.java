package net.shadowmage.ancientwarfare.core.common.torque;

import net.minecraft.core.Direction;

public class SidedTorqueCell extends TorqueCell {
   private final Direction side;
   private final TorqueAccess owner;

   public SidedTorqueCell(double maxInput, double maxOutput, double maxTorque, double efficiency, Direction side, TorqueAccess owner) {
      super(maxInput, maxOutput, maxTorque, efficiency);
      this.side = side;
      this.owner = owner;
   }

   public Direction getSide() {
      return this.side;
   }

   @Override
   public double addTorque(double amount) {
      return this.owner.canInputTorque(this.side) ? super.addTorque(amount) : 0.0;
   }

   @Override
   public double drainTorque(double amount) {
      return this.owner.canOutputTorque(this.side) ? super.drainTorque(amount) : 0.0;
   }

   @Override
   public double getMaxTickInput() {
      return this.owner.canInputTorque(this.side) ? super.getMaxTickInput() : 0.0;
   }

   @Override
   public double getMaxTickOutput() {
      return this.owner.canOutputTorque(this.side) ? super.getMaxTickOutput() : 0.0;
   }
}
