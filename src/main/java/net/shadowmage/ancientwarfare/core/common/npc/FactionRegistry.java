package net.shadowmage.ancientwarfare.core.common.npc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public final class FactionRegistry {
   private static final String RESOURCE_PATH = "/data/ancientwarfare/npc/factions.json";
   private static final FactionRegistry.FactionDefinition EMPTY = new FactionRegistry.FactionDefinition("", 0, false, 0, 0, Map.of());
   private static Map<String, FactionRegistry.FactionDefinition> factions;

   private FactionRegistry() {
   }

   public static FactionRegistry.FactionDefinition get(String factionName) {
      ensureLoaded();
      return factions.getOrDefault(normalize(factionName), EMPTY);
   }

   public static boolean isHostile(String factionName, String otherFactionName) {
      String faction = normalize(factionName);
      String other = normalize(otherFactionName);
      return !faction.equals(other) && get(faction).hostileTowards(other);
   }

   public static boolean isSameFaction(String factionName, String otherFactionName) {
      String faction = normalize(factionName);
      String other = normalize(otherFactionName);
      return !faction.isEmpty() && faction.equals(other);
   }

   public static boolean isKnown(String factionName) {
      ensureLoaded();
      return factions.containsKey(normalize(factionName));
   }

   public static Set<String> factionNames() {
      ensureLoaded();
      return factions.keySet();
   }

   private static void ensureLoaded() {
      if (factions == null) {
         Map<String, FactionRegistry.FactionDefinition> loaded = new HashMap<>();

         try {
            label86: {
               try (InputStream stream = FactionRegistry.class.getResourceAsStream("/data/ancientwarfare/npc/factions.json")) {
                  if (stream != null) {
                     JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                     JsonObject defaults = object(root, "defaults");
                     FactionRegistry.StandingSettings defaultStanding = standingSettings(defaults, new FactionRegistry.StandingSettings(0, false, 0, 0));
                     Map<String, Boolean> defaultRelations = relations(defaults);
                     JsonElement factionElements = root.get("factions");
                     if (factionElements != null && factionElements.isJsonArray()) {
                        for (JsonElement element : factionElements.getAsJsonArray()) {
                           if (element.isJsonObject()) {
                              JsonObject faction = element.getAsJsonObject();
                              String name = string(faction, "name", "");
                              if (!name.isBlank()) {
                                 FactionRegistry.StandingSettings standing = standingSettings(faction, defaultStanding);
                                 Map<String, Boolean> factionRelations = new HashMap<>(defaultRelations);
                                 factionRelations.putAll(relations(faction));
                                 factionRelations.remove(normalize(name));
                                 loaded.put(
                                    normalize(name),
                                    new FactionRegistry.FactionDefinition(
                                       normalize(name),
                                       standing.defaultStanding(),
                                       standing.canChange(),
                                       standing.killChange(),
                                       standing.tradeChange(),
                                       Map.copyOf(factionRelations)
                                    )
                                 );
                              }
                           }
                        }
                     }

                     AncientWarfareCore.LOG.info("Loaded {} Ancient Warfare factions", loaded.size());
                     break label86;
                  }

                  AncientWarfareCore.LOG.warn("Missing faction registry resource: {}", "/data/ancientwarfare/npc/factions.json");
                  factions = Map.of();
               }

               return;
            }
         } catch (Exception var15) {
            AncientWarfareCore.LOG.error("Failed to load faction registry", var15);
         }

         factions = Map.copyOf(loaded);
      }
   }

   private static FactionRegistry.StandingSettings standingSettings(JsonObject source, FactionRegistry.StandingSettings fallback) {
      JsonObject settings = object(source, "standing_settings");
      int defaultStanding = integer(settings, "player_default_standing", fallback.defaultStanding());
      boolean canChange = bool(settings, "standing_can_change", fallback.canChange());
      JsonObject changes = object(settings, "standing_changes");
      return new FactionRegistry.StandingSettings(
         defaultStanding, canChange, integer(changes, "kill", fallback.killChange()), integer(changes, "trade", fallback.tradeChange())
      );
   }

   private static Map<String, Boolean> relations(JsonObject source) {
      Map<String, Boolean> result = new HashMap<>();
      JsonObject relations = object(source, "hostile_towards_factions");

      for (Entry<String, JsonElement> entry : relations.entrySet()) {
         if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isBoolean()) {
            result.put(normalize(entry.getKey()), entry.getValue().getAsBoolean());
         }
      }

      return result;
   }

   private static JsonObject object(JsonObject parent, String key) {
      JsonElement element = parent.get(key);
      return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
   }

   private static String string(JsonObject object, String key, String fallback) {
      JsonElement element = object.get(key);
      return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
   }

   private static int integer(JsonObject object, String key, int fallback) {
      JsonElement element = object.get(key);
      return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
   }

   private static boolean bool(JsonObject object, String key, boolean fallback) {
      JsonElement element = object.get(key);
      return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
   }

   private static String normalize(String value) {
      return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
   }

   public static record FactionDefinition(
      String name, int defaultStanding, boolean standingCanChange, int killStandingChange, int tradeStandingChange, Map<String, Boolean> hostileRelations
   ) {
      public boolean hostileTowards(String otherFaction) {
         return this.hostileRelations.getOrDefault(FactionRegistry.normalize(otherFaction), false);
      }
   }

   private static record StandingSettings(int defaultStanding, boolean canChange, int killChange, int tradeChange) {
   }
}
