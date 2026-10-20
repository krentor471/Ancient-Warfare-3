package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.Locale;

public enum CoinMetal {
   ANCIENT("ancient", 4479304),
   GOLD("gold", 16766720),
   SILVER("silver", 12632256),
   COPPER("copper", 12088115);

   private final String id;
   private final int color;

   private CoinMetal(String id, int color) {
      this.id = id;
      this.color = color;
   }

   public String id() {
      return this.id;
   }

   public int color() {
      return this.color;
   }

   public static CoinMetal byId(String id) {
      if (id != null && !id.isBlank()) {
         String normalized = id.toLowerCase(Locale.ROOT);

         for (CoinMetal metal : values()) {
            if (metal.id.equals(normalized)) {
               return metal;
            }
         }

         return COPPER;
      } else {
         return COPPER;
      }
   }
}
