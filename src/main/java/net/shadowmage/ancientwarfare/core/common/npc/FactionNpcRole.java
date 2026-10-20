package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum FactionNpcRole {
   SOLDIER("soldier", false),
   SOLDIER_ELITE("soldier.elite", false),
   ARCHER("archer", false),
   ARCHER_ELITE("archer.elite", false),
   CAVALRY("cavalry", false),
   MOUNTED_ARCHER("mounted_archer", false),
   LEADER("leader", false),
   LEADER_ELITE("leader.elite", false),
   SIEGE_ENGINEER("siege_engineer", false),
   PRIEST("priest", false),
   SPELLCASTER("spellcaster", false),
   BARD("bard", true),
   TRADER("trader", true),
   CIVILIAN_MALE("civilian.male", true),
   CIVILIAN_FEMALE("civilian.female", true);

   public static final String DEFAULT_FACTION = "brigand";
   public static final List<FactionNpcRole> CREATIVE_ROLES = List.of(
      SOLDIER,
      SOLDIER_ELITE,
      ARCHER,
      ARCHER_ELITE,
      CAVALRY,
      MOUNTED_ARCHER,
      LEADER,
      LEADER_ELITE,
      SIEGE_ENGINEER,
      CIVILIAN_MALE,
      CIVILIAN_FEMALE,
      PRIEST,
      SPELLCASTER,
      BARD,
      TRADER
   );
   private final String id;
   private final boolean civilian;

   private FactionNpcRole(String id, boolean civilian) {
      this.id = id;
      this.civilian = civilian;
   }

   public String id() {
      return this.id;
   }

   public boolean isCivilian() {
      return this.civilian;
   }

   public static Optional<FactionNpcRole> byId(String id) {
      return Arrays.stream(values()).filter(role -> role.id.equals(id)).findFirst();
   }
}
