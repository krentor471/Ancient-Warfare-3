package net.shadowmage.ancientwarfare.core.common.torque;

public enum TorqueTier {
   LIGHT("light", 1000.0, 100000.0, 1.0),
   MEDIUM("medium", 2000.0, 200000.0, 1.0),
   HEAVY("heavy", 5000.0, 500000.0, 1.0);

   private final String id;
   private final double transferLimit;
   private final double storagePerBlock;
   private final double efficiency;

   private TorqueTier(String id, double transferLimit, double storagePerBlock, double efficiency) {
      this.id = id;
      this.transferLimit = transferLimit;
      this.storagePerBlock = storagePerBlock;
      this.efficiency = efficiency;
   }

   public String getId() {
      return this.id;
   }

   public double getTransferLimit() {
      return this.transferLimit;
   }

   public double getStoragePerBlock() {
      return this.storagePerBlock;
   }

   public double getEfficiency() {
      return this.efficiency;
   }

   public TorqueCell createCell() {
      return new TorqueCell(this.transferLimit, this.transferLimit, this.transferLimit, this.efficiency);
   }
}
