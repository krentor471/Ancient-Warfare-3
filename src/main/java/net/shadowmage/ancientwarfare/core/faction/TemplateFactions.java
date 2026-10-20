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
   private static Map<String, String> factions;

   private TemplateFactions() {
   }

   /** Faction of an AWS template, or "" when it has no faction spawners or the faction is unknown. */
   public static synchronized String ofTemplate(String path) {
      if (factions == null) {
         factions = load();
      }

      String faction = factions.getOrDefault(path, "");
      return FactionRegistry.isKnown(faction) ? faction : "";
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

   private static Map<String, String> load() {
      Map<String, String> loaded = new HashMap<>();

      try (InputStream stream = TemplateFactions.class.getResourceAsStream(RESOURCE)) {
         if (stream == null) {
            AncientWarfareCore.LOG.warn("Missing template faction index: {}", RESOURCE);
            return loaded;
         }

         try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.lines().filter(line -> !line.startsWith("#") && line.contains("=")).forEach(line -> {
               int split = line.lastIndexOf('=');
               loaded.put(line.substring(0, split).trim(), line.substring(split + 1).trim().toLowerCase(Locale.ROOT));
            });
         }
      } catch (IOException exception) {
         AncientWarfareCore.LOG.error("Failed to read template faction index", exception);
      }

      return loaded;
   }
}
