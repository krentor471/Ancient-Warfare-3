package net.shadowmage.ancientwarfare.core.faction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;

/** Which faction a structure template belongs to, from the index built by tools/worldgen/template_factions.py. */
public final class TemplateFactions {
   private static final String RESOURCE = "/assets/ancientwarfare/template_factions.txt";
   private static final TemplateInfo NEUTRAL = new TemplateInfo("", 0, 0);
   private static Map<String, TemplateInfo> templates;

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
