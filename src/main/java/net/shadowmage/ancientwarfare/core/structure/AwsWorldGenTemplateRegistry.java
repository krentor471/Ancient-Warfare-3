package net.shadowmage.ancientwarfare.core.structure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

public final class AwsWorldGenTemplateRegistry {
   private static final String RESOURCE_ROOT = "/assets/ancientwarfare/template/";
   private static final long PHASED_AQUATIC_VOLUME_THRESHOLD = 500000L;
   private static final Map<String, String> LEGACY_BIOMES = Map.ofEntries(
      Map.entry("minecraft:beaches", "minecraft:beach"),
      Map.entry("minecraft:stone_beach", "minecraft:stony_shore"),
      Map.entry("minecraft:cold_beach", "minecraft:snowy_beach"),
      Map.entry("minecraft:ice_flats", "minecraft:snowy_plains"),
      Map.entry("minecraft:taiga_cold", "minecraft:snowy_taiga"),
      Map.entry("minecraft:taiga_cold_hills", "minecraft:snowy_taiga"),
      Map.entry("minecraft:forest_hills", "minecraft:forest"),
      Map.entry("minecraft:birch_forest_hills", "minecraft:birch_forest"),
      Map.entry("minecraft:mutated_birch_forest", "minecraft:old_growth_birch_forest"),
      Map.entry("minecraft:mutated_birch_forest_hills", "minecraft:old_growth_birch_forest"),
      Map.entry("minecraft:roofed_forest", "minecraft:dark_forest"),
      Map.entry("minecraft:mutated_roofed_forest", "minecraft:dark_forest"),
      Map.entry("minecraft:swampland", "minecraft:swamp"),
      Map.entry("minecraft:extreme_hills", "minecraft:windswept_hills"),
      Map.entry("minecraft:smaller_extreme_hills", "minecraft:windswept_hills"),
      Map.entry("minecraft:mutated_extreme_hills", "minecraft:windswept_gravelly_hills"),
      Map.entry("minecraft:extreme_hills_with_trees", "minecraft:windswept_forest"),
      Map.entry("minecraft:ice_mountains", "minecraft:frozen_peaks"),
      Map.entry("minecraft:desert_hills", "minecraft:desert"),
      Map.entry("minecraft:jungle_hills", "minecraft:jungle"),
      Map.entry("minecraft:jungle_edge", "minecraft:sparse_jungle"),
      Map.entry("minecraft:redwood_taiga", "minecraft:old_growth_pine_taiga"),
      Map.entry("minecraft:redwood_taiga_hills", "minecraft:old_growth_spruce_taiga"),
      Map.entry("minecraft:mesa", "minecraft:badlands"),
      Map.entry("minecraft:mesa_rock", "minecraft:wooded_badlands"),
      Map.entry("minecraft:mesa_clear_rock", "minecraft:eroded_badlands"),
      Map.entry("minecraft:mutated_mesa", "minecraft:eroded_badlands"),
      Map.entry("minecraft:mutated_mesa_rock", "minecraft:wooded_badlands"),
      Map.entry("minecraft:mutated_mesa_clear_rock", "minecraft:eroded_badlands")
   );
   private static Map<String, AwsWorldGenTemplateRegistry.Template> builtInTemplates;
   private static List<AwsWorldGenTemplateRegistry.Template> templates;
   private static List<AwsWorldGenTemplateRegistry.Template> largeAquaticTemplates;
   private static final Set<String> WARNED_DEPTH_RANGES = new HashSet<>();

   private AwsWorldGenTemplateRegistry() {
   }

   public static synchronized List<AwsWorldGenTemplateRegistry.Template> templates() {
      if (templates == null) {
         templates = builtInTemplates()
            .values()
            .stream()
            .map(AwsWorldGenTemplateRegistry::withOverrides)
            .filter(AwsWorldGenTemplateRegistry.Template::worldGenEnabled)
            .filter(template -> template.ground() || template.aquatic())
            .filter(template -> !template.name().startsWith("EmptyWaterBalancingProxy"))
            .filter(template -> compatibleMods(template.mods()))
            .filter(template -> template.overworldAllowed())
            .filter(template -> template.selectionWeight() > 0)
            .toList();
         long aquatic = templates.stream().filter(AwsWorldGenTemplateRegistry.Template::aquatic).count();
         AncientWarfareCore.LOG.info("Loaded {} compatible AWS world-generation templates ({} aquatic)", templates.size(), aquatic);
      }

      return templates;
   }

   public static synchronized List<AwsWorldGenTemplateRegistry.Template> largeAquaticTemplates() {
      if (largeAquaticTemplates == null) {
         largeAquaticTemplates = templates().stream().filter(AwsWorldGenTemplateRegistry.Template::requiresPhasedPlacement).toList();
         AncientWarfareCore.LOG.info("Loaded {} large aquatic AWS templates for phased generation", largeAquaticTemplates.size());
      }

      return largeAquaticTemplates;
   }

   public static synchronized void invalidateCaches() {
      templates = null;
      largeAquaticTemplates = null;
   }

   public static synchronized AwsWorldGenTemplateRegistry.Template templateForActivePath(String path) {
      return builtInTemplates().get(path);
   }

   private static Map<String, AwsWorldGenTemplateRegistry.Template> builtInTemplates() {
      if (builtInTemplates == null) {
         Map<String, AwsWorldGenTemplateRegistry.Template> loaded = new LinkedHashMap<>();

         for (String path : AwsTemplateIndex.allTemplates()) {
            AwsWorldGenTemplateRegistry.Template template = load(path);
            if (template != null) {
               loaded.put(path, template);
            }
         }

         builtInTemplates = Collections.unmodifiableMap(loaded);
      }

      return builtInTemplates;
   }

   private static AwsWorldGenTemplateRegistry.Template withOverrides(AwsWorldGenTemplateRegistry.Template template) {
      String path = template.path();
      AwsWorldGenTemplateRegistry.Template overridden = new AwsWorldGenTemplateRegistry.Template(
         path,
         template.name(),
         template.type(),
         template.mods(),
         template.sizeX(),
         template.sizeY(),
         template.sizeZ(),
         template.offsetX(),
         template.offsetY(),
         template.offsetZ(),
         AWStructureTemplateOverrides.awsEnabled(path, template.worldGenEnabled()),
         template.preserveBlocks(),
         template.unique(),
         AWStructureTemplateOverrides.awsBiomeWhiteList(path, template.biomeWhiteList()),
         AWStructureTemplateOverrides.awsBiomes(path, template.biomes()),
         template.dimensionWhiteList(),
         template.dimensions(),
         AWStructureTemplateOverrides.awsWeight(path, template.selectionWeight()),
         template.minDuplicateDistance(),
         template.border(),
         template.leveling(),
         template.fill(),
         template.minWaterDepth(),
         template.maxWaterDepth()
      );
      warnInvalidDepthRange(overridden);
      return overridden;
   }

   private static AwsWorldGenTemplateRegistry.Template load(String path) {
      try {
         AwsWorldGenTemplateRegistry.Template var9;
         try (InputStream stream = AwsWorldGenTemplateRegistry.class.getResourceAsStream("/assets/ancientwarfare/template/" + path)) {
            if (stream == null) {
               return null;
            }

            List<String> lines;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
               lines = reader.lines().takeWhile(line -> !line.startsWith("#### LAYERS")).toList();
            }

            Map<String, String> values = lines.stream()
               .filter(line -> line.contains("=") && !line.startsWith("#"))
               .map(line -> line.split("=", 2))
               .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim(), (first, second) -> second));
            int[] size = triplet(values.getOrDefault("size", "1,1,1"));
            int[] offset = triplet(values.getOrDefault("offset", "0,0,0"));
            Set biomes = csv(values.getOrDefault("biomeList", "")).stream().map(value -> LEGACY_BIOMES.getOrDefault(value, value)).collect(Collectors.toSet());
            Set dimensions = csv(values.getOrDefault("dimensionList", "")).stream().map(AwsWorldGenTemplateRegistry::integer).collect(Collectors.toSet());
            AwsWorldGenTemplateRegistry.Template template = new AwsWorldGenTemplateRegistry.Template(
               path,
               values.getOrDefault("name", path),
               values.getOrDefault("type", ""),
               values.getOrDefault("mods", ""),
               size[0],
               size[1],
               size[2],
               offset[0],
               offset[1],
               offset[2],
               bool(values, "worldGenEnabled", false),
               bool(values, "preserveBlocks", false),
               bool(values, "unique", false),
               bool(values, "biomeWhiteList", true),
               biomes,
               bool(values, "dimensionWhiteList", true),
               dimensions,
               integer(values.getOrDefault("selectionWeight", "1")),
               integer(values.getOrDefault("minDuplicateDistance", "1")),
               integer(values.getOrDefault("border", "0")),
               integer(values.getOrDefault("leveling", "0")),
               integer(values.getOrDefault("fill", "0")),
               integer(values.getOrDefault("minWaterDepth", "0")),
               integer(values.getOrDefault("maxWaterDepth", "255"))
            );
            warnInvalidDepthRange(template);
            var9 = template;
         }

         return var9;
      } catch (RuntimeException | IOException var14) {
         AncientWarfareCore.LOG.warn("Unable to read AWS world-generation metadata from {}", path, var14);
         return null;
      }
   }

   private static void warnInvalidDepthRange(AwsWorldGenTemplateRegistry.Template template) {
      if (template.aquatic()
         && template.minWaterDepth() > template.maxWaterDepth()
         && WARNED_DEPTH_RANGES.add(template.path() + ":" + template.minWaterDepth() + ":" + template.maxWaterDepth())) {
         AncientWarfareCore.LOG
            .warn(
               "AWS template {} has invalid water-depth range {}..{}; generation uses {}..{} before island adaptation. Original metadata is retained",
               new Object[]{
                  template.path(),
                  template.minWaterDepth(),
                  template.maxWaterDepth(),
                  OceanGenerationPolicy.normalizedMinDepth(template.minWaterDepth(), template.maxWaterDepth()),
                  OceanGenerationPolicy.normalizedMaxDepth(template.minWaterDepth(), template.maxWaterDepth())
               }
            );
      }
   }

   private static boolean compatibleMods(String requirements) {
      if (requirements.isBlank()) {
         return true;
      } else {
         for (String requirement : requirements.toLowerCase(Locale.ROOT).split("[,; ]+")) {
            if (!requirement.isBlank() && !AWCorePlatform.isModLoaded(requirement)) {
               return false;
            }
         }

         return true;
      }
   }

   private static int[] triplet(String value) {
      int[] result = Arrays.stream(value.split(",")).map(String::trim).mapToInt(AwsWorldGenTemplateRegistry::integer).toArray();
      return result.length == 3 ? result : new int[]{1, 1, 1};
   }

   private static List<String> csv(String value) {
      if (value.isBlank()) {
         return List.of();
      } else {
         List<String> result = new ArrayList<>();

         for (String part : value.split(",")) {
            String normalized = part.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
               result.add(normalized);
            }
         }

         return result;
      }
   }

   private static int integer(String value) {
      try {
         return Integer.parseInt(value.trim());
      } catch (NumberFormatException var2) {
         return 0;
      }
   }

   private static boolean bool(Map<String, String> values, String key, boolean fallback) {
      return values.containsKey(key) ? Boolean.parseBoolean(values.get(key)) : fallback;
   }

   public static record Template(
      String path,
      String name,
      String type,
      String mods,
      int sizeX,
      int sizeY,
      int sizeZ,
      int offsetX,
      int offsetY,
      int offsetZ,
      boolean worldGenEnabled,
      boolean preserveBlocks,
      boolean unique,
      boolean biomeWhiteList,
      Set<String> biomes,
      boolean dimensionWhiteList,
      Set<Integer> dimensions,
      int selectionWeight,
      int minDuplicateDistance,
      int border,
      int leveling,
      int fill,
      int minWaterDepth,
      int maxWaterDepth
   ) {
      public boolean ground() {
         return "ground".equals(this.type);
      }

      public boolean aquatic() {
         return "water".equals(this.type) || "island".equals(this.type);
      }

      public boolean island() {
         return "island".equals(this.type);
      }

      public boolean requiresPhasedPlacement() {
         return this.aquatic()
            && (this.sizeX > 128 || this.sizeZ > 128 || this.sizeY > 192 || (long)this.sizeX * (long)this.sizeY * (long)this.sizeZ > 500000L);
      }

      public boolean fitsBuildHeight(BlockPos min, int minimumY, int maximumYExclusive) {
         return min.getY() >= minimumY && (long)min.getY() + (long)this.sizeY <= (long)maximumYExclusive;
      }

      public boolean overworldAllowed() {
         boolean listed = this.dimensions.contains(0);
         return this.dimensionWhiteList == listed;
      }

      public boolean acceptsBiome(Holder<Biome> biome) {
         ResourceLocation key = biome.unwrapKey().map(value -> value.location()).orElse(null);
         boolean listed = key != null && (this.biomes.contains(key.toString()) || this.matchesLegacyOceanFamily(key));
         if (!listed) {
            listed = BiomeFamilyMatcher.matchesAny(biome, this.biomes);
         }

         return this.biomeWhiteList == listed;
      }

      private boolean matchesLegacyOceanFamily(ResourceLocation key) {
         if (!key.getNamespace().equals("minecraft")) {
            return false;
         } else {
            String path = key.getPath();
            if (this.biomes.contains("minecraft:deep_ocean") && path.contains("deep_") && path.endsWith("ocean")) {
               return true;
            } else {
               return this.biomes.contains("minecraft:ocean") && path.endsWith("ocean") && !path.contains("deep_")
                  ? true
                  : this.biomes.contains("minecraft:frozen_ocean") && path.equals("deep_frozen_ocean");
            }
         }
      }

      public BlockPos minFor(BlockPos origin, Direction facing) {
         BlockPos first = origin.relative(facing.getCounterClockWise(), this.offsetX).relative(facing, this.offsetZ).below(this.offsetY);
         BlockPos second = first.relative(facing.getClockWise(), this.sizeX - 1).relative(facing, -(this.sizeZ - 1)).above(this.sizeY - 1);
         return new BlockPos(
            Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ())
         );
      }

      public int rotatedSizeX(Direction facing) {
         return facing.getAxis() == Axis.X ? this.sizeZ : this.sizeX;
      }

      public int rotatedSizeZ(Direction facing) {
         return facing.getAxis() == Axis.X ? this.sizeX : this.sizeZ;
      }
   }
}
