package net.shadowmage.ancientwarfare.core.common.torque;

public class TorqueCell {
   private final double maxInput;
   private final double maxOutput;
   private final double maxTorque;
   private final double efficiency;
   private double torque;

   public TorqueCell(double maxInput, double maxOutput, double maxTorque, double efficiency) {
      this.maxInput = requireFiniteNonNegative(maxInput, "maxInput");
      this.maxOutput = requireFiniteNonNegative(maxOutput, "maxOutput");
      this.maxTorque = requireFiniteNonNegative(maxTorque, "maxTorque");
      this.efficiency = requireFiniteNonNegative(efficiency, "efficiency");
   }

   public double getMaxInput() {
      return this.maxInput;
   }

   public double getMaxOutput() {
      return this.maxOutput;
   }

   public double getMaxTorque() {
      return this.maxTorque;
   }

   public double getEfficiency() {
      return this.efficiency;
   }

   public double getTorque() {
      return this.torque;
   }

   public void setTorque(double torque) {
      this.torque = this.clampToCapacity(torque);
   }

   public double addTorque(double amount) {
      double accepted = Math.min(this.getMaxTickInput(), requireFiniteNonNegative(amount, "amount"));
      this.torque += accepted;
      return accepted;
   }

   public double drainTorque(double amount) {
      double drained = Math.min(this.getMaxTickOutput(), requireFiniteNonNegative(amount, "amount"));
      this.torque -= drained;
      return drained;
   }

   public double getMaxTickInput() {
      return Math.min(this.maxInput, Math.max(0.0, this.maxTorque - this.torque));
   }

   public double getMaxTickOutput() {
      return Math.min(this.maxOutput, this.torque);
   }

   public double getPercentFull() {
      return this.maxTorque > 0.0 ? this.torque / this.maxTorque : 0.0;
   }

   private double clampToCapacity(double value) {
      return Math.max(0.0, Math.min(this.maxTorque, requireFiniteNonNegative(value, "torque")));
   }

   private static double requireFiniteNonNegative(double value, String name) {
      if (Double.isFinite(value) && !(value < 0.0)) {
         return value;
      } else {
         throw new IllegalArgumentException(name + " must be finite and non-negative");
      }
   }
}
