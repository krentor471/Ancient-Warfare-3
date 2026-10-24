package net.shadowmage.ancientwarfare.core.faction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Settlement names in the style of each faction's culture, made of a first and a last syllable.
 * A name is kept as a code such as "norse:3.5"; its syllables are language entries, so the same
 * village reads Grimvik in English and Гримвик in Russian.
 */
public final class FactionNames {
   private static final int PREFIXES = 12;
   private static final int SUFFIXES = 8;
   private static final Map<String, String> CULTURES = Map.ofEntries(
      Map.entry("empire", "latin"),
      Map.entry("reiksgard", "germanic"),
      Map.entry("witchbane", "germanic"),
      Map.entry("norska", "norse"),
      Map.entry("barbarian", "norse"),
      Map.entry("sealsker", "norse"),
      Map.entry("sarkonid", "desert"),
      Map.entry("shakayana", "desert"),
      Map.entry("xoltec", "jungle"),
      Map.entry("amazon", "jungle"),
      Map.entry("zimba", "jungle"),
      Map.entry("kong", "jungle"),
      Map.entry("zamurai", "eastern"),
      Map.entry("smingol", "eastern"),
      Map.entry("nogg", "orcish"),
      Map.entry("buffloka", "orcish")
   );

   private FactionNames() {
   }

   public static String culture(String faction) {
      return CULTURES.getOrDefault(faction, "english");
   }

   /** A name for a new settlement of the faction, unlike any of the taken ones while names are left. */
   public static String pick(String faction, int id, Collection<String> taken) {
      String culture = culture(faction);
      Set<String> used = new HashSet<>(taken);
      int start = Math.floorMod((id + faction.hashCode()) * 0x9E3779B1, PREFIXES * SUFFIXES);

      for (int step = 0; step < PREFIXES * SUFFIXES; step++) {
         // 37 is coprime with 96, so the walk visits every name once.
         int n = Math.floorMod(start + step * 37, PREFIXES * SUFFIXES);
         String code = culture + ":" + n / SUFFIXES + "." + n % SUFFIXES;
         if (!used.contains(code)) {
            return code;
         }
      }

      return culture + ":" + start / SUFFIXES + "." + start % SUFFIXES;
   }

   /** The name a code stands for, or an empty component for an empty or broken code. */
   public static MutableComponent component(String code) {
      int colon = code.indexOf(':');
      int dot = code.indexOf('.', colon + 1);
      if (colon <= 0 || dot < 0) {
         return Component.empty();
      }

      String key = "name.ancientwarfare." + code.substring(0, colon) + ".";
      return Component.empty()
         .append(Component.translatable(key + "p" + code.substring(colon + 1, dot)))
         .append(Component.translatable(key + "s" + code.substring(dot + 1)));
   }
}
