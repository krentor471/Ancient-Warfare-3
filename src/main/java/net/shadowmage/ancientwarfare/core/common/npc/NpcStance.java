package net.shadowmage.ancientwarfare.core.common.npc;

/**
 * How eagerly a soldier fights on its own. Defensive is the old behaviour: it answers enemies near its post and does not
 * chase them far. Aggressive chases as far as it likes, passive never starts a fight; an explicit attack order works in
 * every stance.
 */
public enum NpcStance {
   AGGRESSIVE("aggressive"),
   DEFENSIVE("defensive"),
   PASSIVE("passive");

   private final String name;

   NpcStance(String name) {
      this.name = name;
   }

   public String serializedName() {
      return this.name;
   }

   public String translationKey() {
      return "guistrings.npc.stance." + this.name;
   }

   public NpcStance next() {
      NpcStance[] values = values();
      return values[(this.ordinal() + 1) % values.length];
   }

   public static NpcStance byName(String name) {
      for (NpcStance stance : values()) {
         if (stance.name.equals(name)) {
            return stance;
         }
      }

      return DEFENSIVE;
   }
}
