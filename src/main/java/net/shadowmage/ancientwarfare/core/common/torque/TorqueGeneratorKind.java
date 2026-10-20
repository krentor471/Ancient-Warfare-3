package net.shadowmage.ancientwarfare.core.common.torque;

public enum TorqueGeneratorKind {
   STIRLING("stirling_generator"),
   WATERWHEEL("waterwheel_generator"),
   WINDMILL("windmill_generator");

   private final String serializedName;

   private TorqueGeneratorKind(String serializedName) {
      this.serializedName = serializedName;
   }

   public String serializedName() {
      return this.serializedName;
   }
}
