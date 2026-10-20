package net.shadowmage.ancientwarfare.core.common.torque;

public record TorqueStats(double stored, double input, double output, double loss) {
   public static final TorqueStats EMPTY = new TorqueStats(0.0, 0.0, 0.0, 0.0);

   public TorqueStats(double stored, double input, double output, double loss) {
      stored = finiteNonNegative(stored);
      input = finiteNonNegative(input);
      output = finiteNonNegative(output);
      loss = finiteNonNegative(loss);
      this.stored = stored;
      this.input = input;
      this.output = output;
      this.loss = loss;
   }

   public TorqueStats withStored(double stored) {
      return new TorqueStats(stored, this.input, this.output, this.loss);
   }

   public TorqueStats addInput(double amount) {
      return new TorqueStats(this.stored, this.input + finiteNonNegative(amount), this.output, this.loss);
   }

   public TorqueStats addOutput(double amount) {
      return new TorqueStats(this.stored, this.input, this.output + finiteNonNegative(amount), this.loss);
   }

   public TorqueStats addLoss(double amount) {
      return new TorqueStats(this.stored, this.input, this.output, this.loss + finiteNonNegative(amount));
   }

   private static double finiteNonNegative(double value) {
      return Double.isFinite(value) && !(value < 0.0) ? value : 0.0;
   }
}
