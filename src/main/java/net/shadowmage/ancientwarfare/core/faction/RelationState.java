package net.shadowmage.ancientwarfare.core.faction;

import java.util.Locale;

/** Standing agreement between two factions, from open war to alliance. */
public enum RelationState {
   WAR,
   NEUTRAL,
   PACT,
   ALLIANCE;

   public String serializedName() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public static RelationState byName(String name) {
      for (RelationState state : values()) {
         if (state.serializedName().equals(name)) {
            return state;
         }
      }

      return NEUTRAL;
   }
}
