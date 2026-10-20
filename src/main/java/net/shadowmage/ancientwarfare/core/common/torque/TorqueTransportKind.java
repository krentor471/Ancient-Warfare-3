package net.shadowmage.ancientwarfare.core.common.torque;

public enum TorqueTransportKind {
   SHAFT("torque_shaft"),
   JUNCTION("torque_junction"),
   DISTRIBUTOR("torque_distributor");

   private final String id;

   private TorqueTransportKind(String id) {
      this.id = id;
   }

   public String getId() {
      return this.id;
   }
}
