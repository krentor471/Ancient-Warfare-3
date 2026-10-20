package net.shadowmage.ancientwarfare.core.common.npc;

public enum BardInstrumentType {
   LUTE("lute"),
   FLUTE("flute"),
   HARP("harp"),
   DRUM("drum");

   private final String id;

   private BardInstrumentType(String id) {
      this.id = id;
   }

   public String id() {
      return this.id;
   }

   public static BardInstrumentType byId(String id) {
      if (id != null) {
         for (BardInstrumentType type : values()) {
            if (type.id.equals(id)) {
               return type;
            }
         }
      }

      return LUTE;
   }
}
