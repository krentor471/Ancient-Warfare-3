package net.shadowmage.ancientwarfare.core.structure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public final class AwtWorldGenTemplateRegistry {
   private static final String ROOT = "/assets/ancientwarfare/template/";
   private static final Map<String, String> LEGACY_BIOMES = Map.ofEntries(
      Map.entry("minecraft:forest_hills", "minecraft:forest"),
      Map.entry("minecraft:birch_forest_hills", "minecraft:birch_forest"),
      Map.entry("minecraft:taiga_cold", "minecraft:snowy_taiga"),
      Map.entry("minecraft:ice_flats", "minecraft:snowy_plains"),
      Map.entry("minecraft:desert_hills", "minecraft:desert"),
      Map.entry("minecraft:mutated_desert", "minecraft:desert"),
      Map.entry("minecraft:swampland", "minecraft:swamp")
   );
   private static Map<String, AwtWorldGenTemplateRegistry.Template> builtInTemplates;
   private static List<AwtWorldGenTemplateRegistry.Template> templates;

   private AwtWorldGenTemplateRegistry() {
   }

   public static synchronized List<AwtWorldGenTemplateRegistry.Template> templates() {
      if (templates == null) {
         templates = builtInTemplates()
            .values()
            .stream()
            .filter(template -> AWStructureTemplateOverrides.awtEnabled(template.path()))
            .map(AwtWorldGenTemplateRegistry::withOverrides)
            .filter(template -> template.weight() > 0)
            .toList();
         AncientWarfareCore.LOG.info("Loaded {} AWT world-generation templates", templates.size());
      }

      return templates;
   }

   public static synchronized void invalidateCaches() {
      templates = null;
   }

   public static synchronized AwtWorldGenTemplateRegistry.Template templateForActivePath(String path) {
      return builtInTemplates().get(path);
   }

   private static Map<String, AwtWorldGenTemplateRegistry.Template> builtInTemplates() {
      if (builtInTemplates == null) {
         Map<String, AwtWorldGenTemplateRegistry.Template> loaded = new LinkedHashMap<>();

         for (String path : AwtTemplateIndex.allTemplates()) {
            AwtWorldGenTemplateRegistry.Template template = load(path);
            if (template != null) {
               loaded.put(path, template);
            }
         }

         builtInTemplates = Collections.unmodifiableMap(loaded);
      }

      return builtInTemplates;
   }

   private static AwtWorldGenTemplateRegistry.Template withOverrides(AwtWorldGenTemplateRegistry.Template template) {
      String path = template.path();
      return new AwtWorldGenTemplateRegistry.Template(
         path,
         template.name(),
         AWStructureTemplateOverrides.awtWeight(path, template.weight()),
         template.chunks(),
         AWStructureTemplateOverrides.awtBiomeWhiteList(path, template.biomeWhiteList()),
         AWStructureTemplateOverrides.awtBiomes(path, template.biomes())
      );
   }

   private static AwtWorldGenTemplateRegistry.Template load(String path) {
      try {
         AwtWorldGenTemplateRegistry.Template var5;
         try (InputStream stream = AwtWorldGenTemplateRegistry.class.getResourceAsStream("/assets/ancientwarfare/template/" + path)) {
            if (stream == null) {
               return null;
            }

            List<String> lines;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
               lines = reader.lines().takeWhile(line -> !line.trim().equalsIgnoreCase(":endheader")).toList();
            }

            Map<String, String> values = lines.stream()
               .filter(line -> line.contains("=") && !line.startsWith("#"))
               .map(line -> line.split("=", 2))
               .collect(Collectors.toMap(parts -> parts[0].trim().toLowerCase(Locale.ROOT), parts -> parts[1].trim(), (first, second) -> second));
            Set biomes = csv(values.getOrDefault("biomelist", "")).stream().map(value -> LEGACY_BIOMES.getOrDefault(value, value)).collect(Collectors.toSet());
            var5 = new AwtWorldGenTemplateRegistry.Template(
               path,
               values.getOrDefault("name", fileName(path)),
               integer(values.getOrDefault("selectionweight", "1")),
               integer(values.getOrDefault("maxsize", values.getOrDefault("minsize", "8"))),
               Boolean.parseBoolean(values.getOrDefault("biomewhitelist", "true")),
               biomes
            );
         }

         return var5;
      } catch (RuntimeException | IOException var10) {
         AncientWarfareCore.LOG.warn("Unable to read AWT metadata from {}", path, var10);
         return null;
      }
   }

   private static List<String> csv(String value) {
      return value.isBlank()
         ? List.of()
         : Arrays.stream(value.split(",")).map(String::trim).map(text -> text.toLowerCase(Locale.ROOT)).filter(text -> !text.isBlank()).toList();
   }

   private static int integer(String value) {
      try {
         return Integer.parseInt(value.trim());
      } catch (NumberFormatException var2) {
         return 1;
      }
   }

   private static String fileName(String path) {
      String name = path.substring(path.lastIndexOf(47) + 1);
      return name.endsWith(".awt") ? name.substring(0, name.length() - 4) : name;
   }

   public static record Template(String path, String name, int weight, int chunks, boolean biomeWhiteList, Set<String> biomes) {
      public boolean acceptsBiome(Holder<Biome> biome) {
         ResourceLocation key = biome.unwrapKey().map(value -> value.location()).orElse(null);
         boolean listed = key != null && this.biomes.contains(key.toString());
         if (!listed) {
            listed = BiomeFamilyMatcher.matchesAny(biome, this.biomes);
         }

         return this.biomeWhiteList == listed;
      }
   }
}
