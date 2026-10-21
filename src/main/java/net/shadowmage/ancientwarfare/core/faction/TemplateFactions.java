package net.shadowmage.ancientwarfare.core.faction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;

/** Which faction a structure template belongs to, from the index built by tools/worldgen/template_factions.py. */
public final class TemplateFactions {
   private static final String RESOURCE = "/assets/ancientwarfare/template_factions.txt";
   private static final TemplateInfo NEUTRAL = new TemplateInfo("", 0, 0);
   /** Name parts that mark a template as a military building, a fort for border defence. */
   private static final List<String> MILITARY_WORDS = List.of(
      "fort", "tower", "castle", "bastion", "broch", "outpost", "garrison", "watch", "barrack", "stronghold", "citadel", "camp", "gate", "palisade",
      "encampment", "guard", "sentry", "wall", "patrol", "invasion"
   );
   /** Factions without a named military building fall back to their most guarded templates. */
   private static final int FALLBACK_MILITARY = 3;
   private static final List<String> TRADE_WORDS = List.of("inn", "tavern", "trader", "trading", "merchant", "market", "coaching", "stable", "caravan", "emporium");
   private static final List<String> GUARD_WORDS = List.of("watch", "guard", "toll", "sentry", "outpost", "beacon", "patrol", "camp", "tower");
   private static final List<String> LARGE_WORDS = List.of("castle", "fortress", "citadel", "palace", "capital", "village", "town", "city", "ship", "boat", "wizard", "church");
   /** Name parts of faction-less templates that are no roadside inn, whatever else their name says. */
   private static final List<String> NEUTRAL_EXCLUDED = List.of("ruin", "island", "smuggler", "nest", "dungeon", "pirate");
   private static Map<String, TemplateInfo> templates;
   private static Set<String> military;

   private TemplateFactions() {
   }

   /** Faction of an AWS template, or "" when it has no faction spawners or the faction is unknown. */
   public static String ofTemplate(String path) {
      return info(path).faction();
   }

   /** Faction and starting garrison and people of an AWS template. */
   public static synchronized TemplateInfo info(String path) {
      if (templates == null) {
         templates = load();
      }

      TemplateInfo info = templates.getOrDefault(path, NEUTRAL);
      return FactionRegistry.isKnown(info.faction()) ? info : NEUTRAL;
   }

   /** Whether the template's name marks it as a military building: a tower, fort, camp and the like. */
   public static boolean hasMilitaryName(String path) {
      String name = name(path);
      return !name.contains("wizard") && MILITARY_WORDS.stream().anyMatch(name::contains);
   }

   /** Every template path of a faction, in no particular order. */
   public static synchronized List<String> templatesOf(String faction) {
      if (templates == null) {
         templates = load();
      }

      List<String> paths = new ArrayList<>();
      templates.forEach((path, info) -> {
         if (info.faction().equals(faction)) {
            paths.add(path);
         }
      });
      paths.sort(null);
      return paths;
   }

   /** Whether the template suits a border fort: a tower, castle, camp and the like of its faction. */
   public static synchronized boolean isMilitary(String path) {
      if (military == null) {
         info(path);
         military = findMilitary(templates);
      }

      return military.contains(path);
   }

   /** A roadside inn, tavern, market or trader's place; never a whole village. */
   public static boolean isTradePost(String path) {
      String name = name(path);
      return !info(path).faction().isEmpty() && TRADE_WORDS.stream().anyMatch(name::contains) && LARGE_WORDS.stream().noneMatch(name::contains);
   }

   /** An inn or tavern of no faction, which any faction without one of its own may keep by its roads. */
   public static boolean isNeutralTradePost(String path) {
      String name = name(path);
      return info(path).faction().isEmpty()
         && (name.contains("inn") || name.contains("tavern"))
         && LARGE_WORDS.stream().noneMatch(name::contains)
         && NEUTRAL_EXCLUDED.stream().noneMatch(name::contains);
   }

   /** A small guard building for a road: a watchtower, toll gate, outpost; the faction's military buildings when it has none. */
   public static boolean isGuardPost(String path) {
      String name = name(path);
      String faction = info(path).faction();
      if (faction.isEmpty()) {
         return false;
      } else if (GUARD_WORDS.stream().anyMatch(name::contains) && LARGE_WORDS.stream().noneMatch(name::contains)) {
         return true;
      }

      return isMilitary(path) && !hasGuardPost(faction);
   }

   private static synchronized boolean hasGuardPost(String faction) {
      for (Map.Entry<String, TemplateInfo> entry : templates.entrySet()) {
         String name = name(entry.getKey());
         if (entry.getValue().faction().equals(faction) && GUARD_WORDS.stream().anyMatch(name::contains) && LARGE_WORDS.stream().noneMatch(name::contains)) {
            return true;
         }
      }

      return false;
   }

   private static String name(String path) {
      return path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
   }

   private static Set<String> findMilitary(Map<String, TemplateInfo> all) {
      Set<String> found = new HashSet<>();
      Map<String, List<String>> byFaction = new HashMap<>();
      all.forEach((path, info) -> {
         String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
         if (!name.contains("wizard") && MILITARY_WORDS.stream().anyMatch(name::contains)) {
            found.add(path);
         }

         byFaction.computeIfAbsent(info.faction(), ignored -> new ArrayList<>()).add(path);
      });
      byFaction.forEach((faction, paths) -> {
         if (paths.stream().noneMatch(found::contains)) {
            paths.stream().sorted(Comparator.comparingInt((String path) -> all.get(path).soldiers()).reversed()).limit(FALLBACK_MILITARY).forEach(found::add);
         }
      });
      return found;
   }

   /** Faction of an AWT town, taken from its name prefix (EmpireWalledCity is empire's). */
   public static String ofTown(String townName) {
      String name = townName == null ? "" : townName.toLowerCase(Locale.ROOT);
      String best = "";

      for (String faction : FactionRegistry.factionNames()) {
         if (name.startsWith(faction) && faction.length() > best.length()) {
            best = faction;
         }
      }

      return best;
   }

   private static Map<String, TemplateInfo> load() {
      Map<String, TemplateInfo> loaded = new HashMap<>();

      try (InputStream stream = TemplateFactions.class.getResourceAsStream(RESOURCE)) {
         if (stream == null) {
            AncientWarfareCore.LOG.warn("Missing template faction index: {}", RESOURCE);
            return loaded;
         }

         try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.lines().filter(line -> !line.startsWith("#") && line.contains("=")).forEach(line -> {
               int split = line.lastIndexOf('=');
               String[] fields = line.substring(split + 1).trim().split(";");
               loaded.put(
                  line.substring(0, split).trim(),
                  new TemplateInfo(fields[0].toLowerCase(Locale.ROOT), fields.length > 1 ? parse(fields[1]) : 0, fields.length > 2 ? parse(fields[2]) : 0)
               );
            });
         }
      } catch (IOException exception) {
         AncientWarfareCore.LOG.error("Failed to read template faction index", exception);
      }

      return loaded;
   }

   private static int parse(String value) {
      try {
         return Integer.parseInt(value.trim());
      } catch (NumberFormatException exception) {
         return 0;
      }
   }

   public static record TemplateInfo(String faction, int soldiers, int civilians) {
   }
}
