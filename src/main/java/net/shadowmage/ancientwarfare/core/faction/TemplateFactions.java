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

   /** Whether the template suits a border fort: a tower, castle, camp and the like of its faction. */
   public static synchronized boolean isMilitary(String path) {
      if (military == null) {
         info(path);
         military = findMilitary(templates);
      }

      return military.contains(path);
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
