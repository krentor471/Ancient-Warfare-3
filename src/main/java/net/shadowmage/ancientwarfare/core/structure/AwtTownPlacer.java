package net.shadowmage.ancientwarfare.core.structure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public final class AwtTownPlacer {
   private static final String RESOURCE_ROOT = "/assets/ancientwarfare/template/";
   private static final int ROAD_SETBACK = 1;
   private static final Map<String, String> LEGACY_TEMPLATE_ALIASES = Map.ofEntries(
      Map.entry("empirecitywallballstaewalltower", "EmpireCityWallBallistaeWallTower"),
      Map.entry("empirecitygenwatchcommanderstower", "EmpireCityGenWatchCommanderTower"),
      Map.entry("empiretownstockbarn", "EmpireTownGenStockBarn"),
      Map.entry("noggtowngenruinedhouse", "NoggRuinedHouse"),
      Map.entry("smingoltowngenlargeinvasionforce", "NoggTownGenSmingolLargeInvasionForce"),
      Map.entry("norskatowngencraftsmanglasworker", "NorskaTownGenCraftsmanGlassWorker"),
      Map.entry("norskatowngentaverntheragnorok", "NorskaTownGenTavernBaldursBladdered"),
      Map.entry("sarkonidtowngenbank", "SarkonidTownGenInn")
   );
   private static Map<String, AwtTownPlacer.AwsTemplateRef> awsByName;

   private AwtTownPlacer() {
   }

   public static boolean place(Level level, BlockPos origin, String townPath, Direction facing) {
      if (townPath != null && !townPath.isBlank()) {
         try {
            AwtTownPlacer.TownTemplate template = AwtTownPlacer.TownTemplate.load(townPath);
            new AwtTownPlacer.Runtime(template, level, origin, facing).place();
            return true;
         } catch (RuntimeException var5) {
            AncientWarfareCore.LOG.warn("Unable to place AWT town {}", townPath, var5);
            return false;
         }
      } else {
         return false;
      }
   }

   public static AwtTownPlacer.TownPlan plan(BlockPos origin, String townPath, Direction facing) {
      if (townPath != null && !townPath.isBlank()) {
         AwtTownPlacer.TownTemplate template = AwtTownPlacer.TownTemplate.load(townPath);
         return new AwtTownPlacer.Runtime(template, null, origin, facing, true).plan(townPath);
      } else {
         throw new IllegalArgumentException("Missing AWT template path");
      }
   }

   public static boolean placePlannedTemplate(Level level, AwtTownPlacer.PlannedTemplate placement, Set<Long> protectedBlocks) {
      return AwsTemplatePlacer.placeAtMinPreserving(level, placement.min(), placement.templatePath(), placement.facing(), protectedBlocks);
   }

   private static AwtTownPlacer.AwsTemplateRef awsTemplate(String templateName) {
      if (awsByName == null) {
         awsByName = buildAwsNameIndex();
      }

      String normalized = normalizeTemplateName(templateName);
      String alias = LEGACY_TEMPLATE_ALIASES.get(normalized);
      return awsByName.get(normalizeTemplateName(alias == null ? templateName : alias));
   }

   private static Map<String, AwtTownPlacer.AwsTemplateRef> buildAwsNameIndex() {
      Map<String, AwtTownPlacer.AwsTemplateRef> result = new HashMap<>();

      for (String path : AwsTemplateIndex.allTemplates()) {
         try {
            AwtTownPlacer.AwsTemplateRef ref = AwtTownPlacer.AwsTemplateRef.load(path);
            putPreferredAwtTemplate(result, normalizeTemplateName(ref.name()), ref);
            String fileName = path.substring(path.lastIndexOf(47) + 1);
            if (fileName.endsWith(".aws")) {
               putPreferredAwtTemplate(result, normalizeTemplateName(fileName.substring(0, fileName.length() - 4)), ref);
            }
         } catch (RuntimeException var5) {
            AncientWarfareCore.LOG.debug("Unable to index AWS template {}", path, var5);
         }
      }

      return result;
   }

   private static void putPreferredAwtTemplate(Map<String, AwtTownPlacer.AwsTemplateRef> result, String name, AwtTownPlacer.AwsTemplateRef candidate) {
      AwtTownPlacer.AwsTemplateRef current = result.get(name);
      if (current == null || awtPathPriority(candidate.path()) > awtPathPriority(current.path())) {
         result.put(name, candidate);
      }
   }

   private static int awtPathPriority(String path) {
      String normalized = path.toLowerCase(Locale.ROOT).replace('\\', '/');
      if (normalized.contains("/towns/town structures/")) {
         return 3;
      } else if (normalized.contains("/towns/unique/") || normalized.contains("/towns/wall/")) {
         return 2;
      } else {
         return normalized.contains("/towns/") ? 1 : 0;
      }
   }

   private static String normalizeTemplateName(String name) {
      return name.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
   }

   private static List<String> readLines(String templatePath) {
      String resource = "/assets/ancientwarfare/template/" + templatePath;

      try {
         List var4;
         try (InputStream stream = AwtTownPlacer.class.getResourceAsStream(resource)) {
            if (stream == null) {
               throw new IllegalArgumentException("Missing template resource: " + resource);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
               var4 = reader.lines().toList();
            }
         }

         return var4;
      } catch (IOException var10) {
         throw new IllegalArgumentException("Unable to read template: " + templatePath, var10);
      }
   }

   private static int parseHeader(List<String> lines, int start, AwtTownPlacer.Builder builder) {
      int i;
      for (i = start; i < lines.size(); i++) {
         String line = lines.get(i).trim();
         String lower = line.toLowerCase(Locale.ROOT);
         if (lower.startsWith(":endheader")) {
            break;
         }

         int equals = line.indexOf(61);
         if (equals >= 0) {
            String key = line.substring(0, equals).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(equals + 1).trim();
            switch (key) {
               case "name":
                  builder.name = value;
                  break;
               case "minsize":
                  builder.minSize = parseInt(value, builder.minSize);
                  break;
               case "maxsize":
                  builder.maxSize = parseInt(value, builder.maxSize);
                  break;
               case "townblocksize":
                  builder.townBlockSize = parseInt(value, builder.townBlockSize);
                  break;
               case "townplotsize":
                  builder.townPlotSize = parseInt(value, builder.townPlotSize);
                  break;
               case "buildingexpansion":
                  builder.buildingExpansion = parseInt(value, builder.buildingExpansion);
                  break;
               case "wallstyle":
                  builder.wallStyle = parseInt(value, builder.wallStyle);
                  break;
               case "wallsize":
                  builder.wallSize = parseInt(value, builder.wallSize);
                  break;
               case "exteriorsize":
                  builder.exteriorSize = parseInt(value, builder.exteriorSize);
                  break;
               case "exteriorplotsize":
                  builder.exteriorPlotSize = parseInt(value, builder.exteriorPlotSize);
                  break;
               case "interioremptyplotchance":
                  builder.interiorEmptyPlotChance = parseInt(value, builder.interiorEmptyPlotChance);
                  break;
               case "roadblocks":
                  builder.roadBlocks = List.of(value.split("\\|"));
                  break;
               case "lamp":
                  builder.lamp = parseStructure(value, true);
            }
         }
      }

      return i;
   }

   private static int parseWalls(List<String> lines, int start, AwtTownPlacer.Builder builder) {
      int i;
      for (i = start; i < lines.size(); i++) {
         String line = lines.get(i).trim();
         if (line.toLowerCase(Locale.ROOT).startsWith(":endwalls")) {
            break;
         }

         String[] bits = line.split(":", -1);
         if (bits.length >= 4) {
            AwtTownPlacer.TownWallEntry entry = new AwtTownPlacer.TownWallEntry(bits[0], bits[1], parseInt(bits[2], 0), parseInt(bits[3], 1));
            builder.wallIds.put(entry.id(), entry);
            String var7 = entry.type().toLowerCase(Locale.ROOT);
            switch (var7) {
               case "corner":
                  builder.corners.add(entry);
                  break;
               case "wall":
                  builder.walls.add(entry);
                  break;
               case "gate":
                  builder.gates.add(entry);
                  break;
               case "lgate":
                  builder.leftGates.add(entry);
                  break;
               case "rgate":
                  builder.rightGates.add(entry);
            }
         }
      }

      return i;
   }

   private static int parseWallPatterns(List<String> lines, int start, AwtTownPlacer.Builder builder) {
      int i;
      for (i = start; i < lines.size(); i++) {
         String line = lines.get(i).trim();
         String lower = line.toLowerCase(Locale.ROOT);
         if (lower.startsWith(":endwallpatterns") || lower.startsWith(":endwallpaterns")) {
            break;
         }

         String[] bits = line.split(":", -1);
         if (bits.length >= 2) {
            builder.wallPatterns.put(parseInt(bits[0], 0), parseWallPattern(bits[1]));
         }
      }

      return i;
   }

   private static int parseStructures(List<String> lines, int start, String end, List<AwtTownPlacer.TownStructureEntry> entries, boolean weighted) {
      int i;
      for (i = start; i < lines.size(); i++) {
         String line = lines.get(i).trim();
         if (line.toLowerCase(Locale.ROOT).startsWith(end)) {
            break;
         }

         entries.add(parseStructure(line, weighted));
      }

      return i;
   }

   private static AwtTownPlacer.TownStructureEntry parseStructure(String line, boolean weighted) {
      String[] bits = line.split(":", -1);
      if (!bits[0].isBlank() && !"0".equals(bits[0])) {
         int weight = weighted && bits.length > 1 ? parseInt(bits[1], 1) : 1;
         return new AwtTownPlacer.TownStructureEntry(bits[0], Math.max(1, weight));
      } else {
         return new AwtTownPlacer.TownStructureEntry("", 0);
      }
   }

   private static int[] parseWallPattern(String line) {
      String[] bits = line.split("-", -1);
      int[] pattern = new int[bits.length];

      for (int i = 0; i < bits.length; i++) {
         pattern[i] = parseInt(bits[i], 0);
      }

      return pattern;
   }

   private static int[] parseTriplet(String value) {
      String[] parts = value.split(",");
      return new int[]{parseInt(parts[0], 1), parseInt(parts[1], 1), parseInt(parts[2], 1)};
   }

   private static int parseInt(String value, int fallback) {
      try {
         return Integer.parseInt(value.trim());
      } catch (NumberFormatException var3) {
         return fallback;
      }
   }

   private static record AwsTemplateRef(String path, String name, int sizeX, int sizeY, int sizeZ, int offsetX, int offsetY, int offsetZ) {
      static AwtTownPlacer.AwsTemplateRef load(String path) {
         String name = "";
         int[] size = new int[]{1, 1, 1};
         int[] offset = new int[]{0, 0, 0};

         for (String line : AwtTownPlacer.readLines(path)) {
            if (line.startsWith("name=")) {
               name = line.substring("name=".length()).trim();
            } else if (line.startsWith("size=")) {
               size = AwtTownPlacer.parseTriplet(line.substring("size=".length()));
            } else if (line.startsWith("offset=")) {
               offset = AwtTownPlacer.parseTriplet(line.substring("offset=".length()));
            }
         }

         if (name.isBlank()) {
            String fileName = path.substring(path.lastIndexOf(47) + 1);
            name = fileName.endsWith(".aws") ? fileName.substring(0, fileName.length() - 4) : fileName;
         }

         return new AwtTownPlacer.AwsTemplateRef(path, name, size[0], size[1], size[2], offset[0], offset[1], offset[2]);
      }

      int rotatedWidth(Direction facing) {
         return facing.getAxis() == Axis.Z ? this.sizeX : this.sizeZ;
      }

      int rotatedLength(Direction facing) {
         return facing.getAxis() == Axis.Z ? this.sizeZ : this.sizeX;
      }
   }

   private static final class Box {
      private int minX;
      private int minY;
      private int minZ;
      private int maxX;
      private int maxY;
      private int maxZ;

      private Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
         this.minX = minX;
         this.minY = minY;
         this.minZ = minZ;
         this.maxX = maxX;
         this.maxY = maxY;
         this.maxZ = maxZ;
      }

      private AwtTownPlacer.Box normalized() {
         return new AwtTownPlacer.Box(
            Math.min(this.minX, this.maxX),
            Math.min(this.minY, this.maxY),
            Math.min(this.minZ, this.maxZ),
            Math.max(this.minX, this.maxX),
            Math.max(this.minY, this.maxY),
            Math.max(this.minZ, this.maxZ)
         );
      }

      private AwtTownPlacer.Box copy() {
         return new AwtTownPlacer.Box(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
      }

      private int width() {
         return this.maxX - this.minX + 1;
      }

      private int length() {
         return this.maxZ - this.minZ + 1;
      }

      private int centerX() {
         return this.minX + this.width() / 2 - 1;
      }

      private int centerZ() {
         return this.minZ + this.length() / 2 - 1;
      }
   }

   private static final class Builder {
      private String name = "";
      private int minSize = 3;
      private int maxSize = 9;
      private int townBlockSize = 24;
      private int townPlotSize = 12;
      private int buildingExpansion = 0;
      private int wallStyle = 0;
      private int wallSize = 0;
      private int exteriorSize = 0;
      private int exteriorPlotSize = 12;
      private int interiorEmptyPlotChance = 0;
      private List<String> roadBlocks = List.of();
      private final Map<Integer, AwtTownPlacer.TownWallEntry> wallIds = new LinkedHashMap<>();
      private final List<AwtTownPlacer.TownWallEntry> corners = new ArrayList<>();
      private final List<AwtTownPlacer.TownWallEntry> walls = new ArrayList<>();
      private final List<AwtTownPlacer.TownWallEntry> gates = new ArrayList<>();
      private final List<AwtTownPlacer.TownWallEntry> leftGates = new ArrayList<>();
      private final List<AwtTownPlacer.TownWallEntry> rightGates = new ArrayList<>();
      private final Map<Integer, int[]> wallPatterns = new HashMap<>();
      private AwtTownPlacer.TownStructureEntry lamp;
      private final List<AwtTownPlacer.TownStructureEntry> uniqueStructures = new ArrayList<>();
      private final List<AwtTownPlacer.TownStructureEntry> mainStructures = new ArrayList<>();
      private final List<AwtTownPlacer.TownStructureEntry> houseStructures = new ArrayList<>();
      private final List<AwtTownPlacer.TownStructureEntry> cosmeticStructures = new ArrayList<>();
      private final List<AwtTownPlacer.TownStructureEntry> exteriorStructures = new ArrayList<>();

      private AwtTownPlacer.TownTemplate build() {
         return new AwtTownPlacer.TownTemplate(
            this.name,
            this.minSize,
            this.maxSize,
            this.townBlockSize,
            this.townPlotSize,
            this.buildingExpansion,
            this.wallStyle,
            this.wallSize,
            this.exteriorSize,
            this.exteriorPlotSize,
            this.interiorEmptyPlotChance,
            this.roadBlocks,
            this.wallIds,
            this.corners,
            this.walls,
            this.gates,
            this.leftGates,
            this.rightGates,
            this.wallPatterns,
            this.lamp,
            this.uniqueStructures,
            this.mainStructures,
            this.houseStructures,
            this.cosmeticStructures,
            this.exteriorStructures
         );
      }
   }

   private static enum GridDir {
      SOUTH(0, 1),
      WEST(-1, 0),
      NORTH(0, -1),
      EAST(1, 0);

      private final int x;
      private final int z;

      private GridDir(int x, int z) {
         this.x = x;
         this.z = z;
      }
   }

   public static record PlannedTemplate(String templatePath, BlockPos min, Direction facing) {
   }

   private static final class Runtime {
      private final AwtTownPlacer.TownTemplate town;
      private final Level level;
      private final BlockPos origin;
      private final Direction facing;
      private final Random random;
      private final boolean planning;
      private final List<BlockPos> plannedRoads = new ArrayList<>();
      private final List<AwtTownPlacer.PlannedTemplate> plannedWalls = new ArrayList<>();
      private final List<AwtTownPlacer.PlannedTemplate> plannedBuildings = new ArrayList<>();

      private Runtime(AwtTownPlacer.TownTemplate town, Level level, BlockPos origin, Direction facing) {
         this(town, level, origin, facing, false);
      }

      private Runtime(AwtTownPlacer.TownTemplate town, Level level, BlockPos origin, Direction facing, boolean planning) {
         this.town = town;
         this.level = level;
         this.origin = origin;
         this.facing = facing;
         this.random = new Random(origin.asLong());
         this.planning = planning;
      }

      private AwtTownPlacer.TownPlan plan(String path) {
         int chunks = Math.max(this.town.minSize(), this.town.maxSize());
         int totalSize = chunks * 16;
         int y = this.origin.getY();
         BlockPos min = this.origin.offset(-totalSize / 2, 0, -totalSize / 2);
         List<AwtTownPlacer.TownBlock> blocks = this.townBlocks(min, totalSize, y);
         List<AwtTownPlacer.TownBlock> exteriorBlocks = this.exteriorTownBlocks(min, totalSize, y);
         this.placeRoads(min, totalSize, y, blocks);
         this.placeWalls(min, totalSize, y);
         this.placeBuildings(min, totalSize, y, blocks, exteriorBlocks);
         Set<Long> occupiedBlocks = new HashSet<>();

         for (AwtTownPlacer.PlannedTemplate placement : this.plannedWalls) {
            occupiedBlocks.addAll(AwsTemplatePlacer.occupiedBlocks(placement.min(), placement.templatePath(), placement.facing()));
         }

         for (AwtTownPlacer.PlannedTemplate placement : this.plannedBuildings) {
            occupiedBlocks.addAll(AwsTemplatePlacer.occupiedBlocks(placement.min(), placement.templatePath(), placement.facing()));
         }

         return new AwtTownPlacer.TownPlan(
            path,
            this.town.name(),
            this.origin,
            min,
            totalSize,
            List.copyOf(this.plannedRoads),
            List.copyOf(this.plannedWalls),
            List.copyOf(this.plannedBuildings),
            Set.copyOf(occupiedBlocks)
         );
      }

      private void place() {
         this.level.removeBlock(this.origin, false);
         int chunks = Math.max(this.town.minSize(), this.town.maxSize());
         int totalSize = chunks * 16;
         int y = this.origin.getY();
         BlockPos min = this.origin.offset(-totalSize / 2, 0, -totalSize / 2);
         List<AwtTownPlacer.TownBlock> blocks = this.townBlocks(min, totalSize, y);
         List<AwtTownPlacer.TownBlock> exteriorBlocks = this.exteriorTownBlocks(min, totalSize, y);
         this.clearAndFloor(min, totalSize, y);
         this.placeRoads(min, totalSize, y, blocks);
         this.placeWalls(min, totalSize, y);
         this.placeBuildings(min, totalSize, y, blocks, exteriorBlocks);
      }

      private void clearAndFloor(BlockPos min, int totalSize, int y) {
         for (int x = 0; x < totalSize; x++) {
            for (int z = 0; z < totalSize; z++) {
               BlockPos floor = min.offset(x, -1, z);
               if (this.level.getBlockState(floor).isAir()) {
                  this.level.setBlock(floor, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
               }

               int clearTopY = this.level.getHeight(Types.WORLD_SURFACE, floor.getX(), floor.getZ()) - 1;

               for (int clearY = clearTopY; clearY >= y; clearY--) {
                  this.level.setBlock(new BlockPos(floor.getX(), clearY, floor.getZ()), Blocks.AIR.defaultBlockState(), 18);
               }
            }
         }
      }

      private void placeRoads(BlockPos min, int totalSize, int y, List<AwtTownPlacer.TownBlock> blocks) {
         int center = totalSize / 2;

         for (int i = 0; i < totalSize; i++) {
            for (int w = -1; w <= 1; w++) {
               BlockPos northSouth = min.offset(center + w, -1, i);
               BlockPos eastWest = min.offset(i, -1, center + w);
               this.roadBlock(northSouth.getX(), northSouth.getY(), northSouth.getZ());
               this.roadBlock(eastWest.getX(), eastWest.getY(), eastWest.getZ());
            }
         }

         for (AwtTownPlacer.TownBlock block : blocks) {
            this.placeRoads(block);
         }
      }

      private void placeRoads(AwtTownPlacer.TownBlock block) {
         int minX = block.bb.minX;
         int maxX = block.bb.maxX;
         if (block.hasRoadBorder(AwtTownPlacer.GridDir.WEST)) {
            minX--;
         }

         if (block.hasRoadBorder(AwtTownPlacer.GridDir.EAST)) {
            maxX++;
         }

         for (int x = minX; x <= maxX; x++) {
            if (block.hasRoadBorder(AwtTownPlacer.GridDir.NORTH)) {
               this.roadBlock(x, block.bb.minY - 1, block.bb.minZ - 1);
            }

            if (block.hasRoadBorder(AwtTownPlacer.GridDir.SOUTH)) {
               this.roadBlock(x, block.bb.minY - 1, block.bb.maxZ + 1);
            }
         }

         int minZ = block.bb.minZ;
         int maxZ = block.bb.maxZ;
         if (block.hasRoadBorder(AwtTownPlacer.GridDir.NORTH)) {
            minZ--;
         }

         if (block.hasRoadBorder(AwtTownPlacer.GridDir.SOUTH)) {
            maxZ++;
         }

         for (int z = minZ; z <= maxZ; z++) {
            if (block.hasRoadBorder(AwtTownPlacer.GridDir.WEST)) {
               this.roadBlock(block.bb.minX - 1, block.bb.minY - 1, z);
            }

            if (block.hasRoadBorder(AwtTownPlacer.GridDir.EAST)) {
               this.roadBlock(block.bb.maxX + 1, block.bb.minY - 1, z);
            }
         }
      }

      private void roadBlock(int x, int y, int z) {
         BlockPos pos = new BlockPos(x, y, z);
         if (this.planning) {
            this.plannedRoads.add(pos);
         } else {
            this.level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 3);
         }
      }

      private void placeWalls(BlockPos min, int totalSize, int y) {
         if (this.town.wallStyle() > 0 && !this.town.corners().isEmpty()) {
            int wallInset = 7 + this.town.exteriorSize() * 16;
            int wallMin = wallInset;
            int wallMax = totalSize - 10 - this.town.exteriorSize() * 16;
            if (wallMax > wallInset) {
               this.placeWallTemplate(this.weightedWall(this.town.corners()), min.offset(wallInset, 0, wallInset), Direction.SOUTH);
               this.placeWallTemplate(this.weightedWall(this.town.corners()), min.offset(wallMax, 0, wallInset), Direction.WEST);
               this.placeWallTemplate(this.weightedWall(this.town.corners()), min.offset(wallMax, 0, wallMax), Direction.NORTH);
               this.placeWallTemplate(this.weightedWall(this.town.corners()), min.offset(wallInset, 0, wallMax), Direction.EAST);
               if (this.town.wallStyle() > 1 && !this.town.walls().isEmpty()) {
                  int chunks = Math.max(1, (wallMax - wallInset + 1) / 16);

                  for (int i = 1; i < chunks - 1; i++) {
                     this.placeWallTemplate(this.wallFor(i, chunks), min.offset(wallMin + i * 16, 0, wallMin), Direction.SOUTH);
                     this.placeWallTemplate(this.wallFor(i, chunks), min.offset(wallMax, 0, wallMin + i * 16), Direction.WEST);
                     this.placeWallTemplate(this.wallFor(i, chunks), min.offset(wallMax - i * 16, 0, wallMax), Direction.NORTH);
                     this.placeWallTemplate(this.wallFor(i, chunks), min.offset(wallMin, 0, wallMax - i * 16), Direction.EAST);
                  }
               }
            }
         }
      }

      private AwtTownPlacer.TownWallEntry wallFor(int index, int chunks) {
         if (this.town.wallStyle() == 3) {
            int[] pattern = this.town.wallPatterns().get(chunks);
            if (pattern != null && index < pattern.length) {
               AwtTownPlacer.TownWallEntry patterned = this.town.wallIds().get(pattern[index]);
               if (patterned != null) {
                  return patterned;
               }
            }
         }

         if (chunks % 2 == 0) {
            int middle = chunks / 2;
            if (index == middle && !this.town.leftGates().isEmpty()) {
               return this.weightedWall(this.town.leftGates());
            }

            if (index == middle - 1 && !this.town.rightGates().isEmpty()) {
               return this.weightedWall(this.town.rightGates());
            }
         } else if (index == chunks / 2 && !this.town.gates().isEmpty()) {
            return this.weightedWall(this.town.gates());
         }

         return this.weightedWall(this.town.walls());
      }

      private void placeWallTemplate(AwtTownPlacer.TownWallEntry entry, BlockPos pos, Direction wallFacing) {
         AwtTownPlacer.AwsTemplateRef ref = AwtTownPlacer.awsTemplate(entry.templateName());
         if (ref == null) {
            AncientWarfareCore.LOG.debug("Skipping unresolved AWT wall template {}", entry.templateName());
         } else {
            BlockPos buildOrigin = pos.relative(wallFacing.getCounterClockWise(), 15 - ref.offsetX());
            if (this.planning) {
               this.plannedWalls.add(new AwtTownPlacer.PlannedTemplate(ref.path(), this.structureMin(buildOrigin, ref, wallFacing), wallFacing));
            } else {
               AwsTemplatePlacer.place(this.level, buildOrigin, ref.path(), wallFacing);
            }
         }
      }

      private AwtTownPlacer.TownWallEntry weightedWall(List<AwtTownPlacer.TownWallEntry> entries) {
         int total = entries.stream().mapToInt(entryx -> Math.max(1, entryx.weight())).sum();
         int roll = this.random.nextInt(total);

         for (AwtTownPlacer.TownWallEntry entry : entries) {
            roll -= Math.max(1, entry.weight());
            if (roll < 0) {
               return entry;
            }
         }

         return entries.get(0);
      }

      private void placeBuildings(BlockPos min, int totalSize, int y, List<AwtTownPlacer.TownBlock> blocks, List<AwtTownPlacer.TownBlock> exteriorBlocks) {
         blocks.sort((first, second) -> Float.compare(first.distanceFromCenter, second.distanceFromCenter));
         this.placeFixedStructures(blocks, this.town.uniqueStructures(), true);
         this.placeFixedStructures(blocks, this.town.mainStructures(), false);
         this.placeWeightedStructures(blocks, this.town.houseStructures(), false);
         this.placeWeightedStructures(blocks, this.town.cosmeticStructures(), true);
         this.placeExteriorStructures(exteriorBlocks, this.town.exteriorStructures(), min, totalSize);
         if (this.town.lamp() != null) {
            this.placeLampLine(min, totalSize, totalSize / 2);
         }
      }

      private List<AwtTownPlacer.TownBlock> exteriorTownBlocks(BlockPos min, int totalSize, int y) {
         if (this.town.exteriorSize() > 0 && !this.town.exteriorStructures().isEmpty()) {
            int maxX = min.getX() + totalSize - 1;
            int maxZ = min.getZ() + totalSize - 1;
            int centerX = min.getX() + totalSize / 2 - 1;
            int centerZ = min.getZ() + totalSize / 2 - 1;
            int exteriorMinX = min.getX() + 7;
            int exteriorMinZ = min.getZ() + 7;
            int exteriorMaxX = maxX - 9;
            int exteriorMaxZ = maxZ - 9;
            int wallMinX = exteriorMinX + this.town.exteriorSize() * 16;
            int wallMinZ = exteriorMinZ + this.town.exteriorSize() * 16;
            int wallMaxX = exteriorMaxX - this.town.exteriorSize() * 16;
            int wallMaxZ = exteriorMaxZ - this.town.exteriorSize() * 16;
            int blockSize = Math.max(8, this.town.exteriorSize() * 16);
            int plotSize = Math.max(1, this.town.exteriorPlotSize());
            List<AwtTownPlacer.TownBlock> result = new ArrayList<>();
            this.addExteriorRegion(
               result,
               AwtTownPlacer.GridDir.WEST,
               AwtTownPlacer.GridDir.NORTH,
               new AwtTownPlacer.Box(exteriorMinX, y, exteriorMinZ, centerX - 2, y + 20, wallMinZ - 1),
               blockSize,
               plotSize,
               centerX,
               centerZ
            );
            this.addExteriorRegion(
               result,
               AwtTownPlacer.GridDir.EAST,
               AwtTownPlacer.GridDir.NORTH,
               new AwtTownPlacer.Box(centerX + 1, y, exteriorMinZ, exteriorMaxX, y + 20, wallMinZ - 1),
               blockSize,
               plotSize,
               centerX,
               centerZ
            );
            this.addExteriorRegion(
               result,
               AwtTownPlacer.GridDir.WEST,
               AwtTownPlacer.GridDir.NORTH,
               new AwtTownPlacer.Box(exteriorMinX, y, wallMinZ, wallMinX - 1, y + 20, centerZ - 2),
               blockSize,
               plotSize,
               centerX,
               centerZ
            );
            this.addExteriorRegion(
               result,
               AwtTownPlacer.GridDir.EAST,
               AwtTownPlacer.GridDir.NORTH,
               new AwtTownPlacer.Box(wallMaxX + 1, y, wallMinZ, exteriorMaxX, y + 20, centerZ - 2),
               blockSize,
               plotSize,
               centerX,
               centerZ
            );
            this.addExteriorRegion(
               result,
               AwtTownPlacer.GridDir.WEST,
               AwtTownPlacer.GridDir.NORTH,
               new AwtTownPlacer.Box(exteriorMinX, y, centerZ + 1, wallMinX - 1, y + 20, wallMaxZ),
               blockSize,
               plotSize,
               centerX,
               centerZ
            );
            this.addExteriorRegion(
               result,
               AwtTownPlacer.GridDir.EAST,
               AwtTownPlacer.GridDir.NORTH,
               new AwtTownPlacer.Box(wallMaxX + 1, y, centerZ + 1, exteriorMaxX, y + 20, wallMaxZ),
               blockSize,
               plotSize,
               centerX,
               centerZ
            );
            this.addExteriorRegion(
               result,
               AwtTownPlacer.GridDir.WEST,
               AwtTownPlacer.GridDir.SOUTH,
               new AwtTownPlacer.Box(exteriorMinX, y, wallMaxZ + 1, centerX - 2, y + 20, exteriorMaxZ),
               blockSize,
               plotSize,
               centerX,
               centerZ
            );
            this.addExteriorRegion(
               result,
               AwtTownPlacer.GridDir.EAST,
               AwtTownPlacer.GridDir.SOUTH,
               new AwtTownPlacer.Box(centerX + 1, y, wallMaxZ + 1, exteriorMaxX, y + 20, exteriorMaxZ),
               blockSize,
               plotSize,
               centerX,
               centerZ
            );
            return result;
         } else {
            return List.of();
         }
      }

      private void addExteriorRegion(
         List<AwtTownPlacer.TownBlock> result,
         AwtTownPlacer.GridDir xDir,
         AwtTownPlacer.GridDir zDir,
         AwtTownPlacer.Box bounds,
         int blockSize,
         int plotSize,
         int centerX,
         int centerZ
      ) {
         if (bounds.maxX >= bounds.minX && bounds.maxZ >= bounds.minZ) {
            this.addQuadrantBlocks(result, xDir, zDir, bounds, new boolean[4], centerX, centerZ, blockSize, plotSize, false);
         }
      }

      private List<AwtTownPlacer.TownBlock> townBlocks(BlockPos min, int totalSize, int y) {
         int maxX = min.getX() + totalSize - 1;
         int maxZ = min.getZ() + totalSize - 1;
         int centerX = min.getX() + totalSize / 2 - 1;
         int centerZ = min.getZ() + totalSize / 2 - 1;
         int townMinX = min.getX() + 7 + this.town.exteriorSize() * 16 + this.town.wallSize();
         int townMinZ = min.getZ() + 7 + this.town.exteriorSize() * 16 + this.town.wallSize();
         int townMaxX = maxX - 9 - this.town.exteriorSize() * 16 - this.town.wallSize();
         int townMaxZ = maxZ - 9 - this.town.exteriorSize() * 16 - this.town.wallSize();
         List<AwtTownPlacer.TownBlock> blocks = new ArrayList<>();
         this.addQuadrantBlocks(
            blocks,
            AwtTownPlacer.GridDir.WEST,
            AwtTownPlacer.GridDir.NORTH,
            new AwtTownPlacer.Box(townMinX, y, townMinZ, centerX - 2, y + 20, centerZ - 2),
            new boolean[]{true, false, false, true},
            centerX,
            centerZ
         );
         this.addQuadrantBlocks(
            blocks,
            AwtTownPlacer.GridDir.EAST,
            AwtTownPlacer.GridDir.NORTH,
            new AwtTownPlacer.Box(centerX + 1, y, townMinZ, townMaxX, y + 20, centerZ - 2),
            new boolean[]{true, true, false, false},
            centerX,
            centerZ
         );
         this.addQuadrantBlocks(
            blocks,
            AwtTownPlacer.GridDir.EAST,
            AwtTownPlacer.GridDir.SOUTH,
            new AwtTownPlacer.Box(centerX + 1, y, centerZ + 1, townMaxX, y + 20, townMaxZ),
            new boolean[]{false, true, true, false},
            centerX,
            centerZ
         );
         this.addQuadrantBlocks(
            blocks,
            AwtTownPlacer.GridDir.WEST,
            AwtTownPlacer.GridDir.SOUTH,
            new AwtTownPlacer.Box(townMinX, y, centerZ + 1, centerX - 2, y + 20, townMaxZ),
            new boolean[]{false, false, true, true},
            centerX,
            centerZ
         );
         return blocks;
      }

      private void addQuadrantBlocks(
         List<AwtTownPlacer.TownBlock> result,
         AwtTownPlacer.GridDir xDir,
         AwtTownPlacer.GridDir zDir,
         AwtTownPlacer.Box bounds,
         boolean[] roadBorders,
         int centerX,
         int centerZ
      ) {
         int blockSize = Math.max(8, this.town.townBlockSize());
         int plotSize = Math.max(4, this.town.townPlotSize());
         this.addQuadrantBlocks(result, xDir, zDir, bounds, roadBorders, centerX, centerZ, blockSize, plotSize, true);
      }

      private void addQuadrantBlocks(
         List<AwtTownPlacer.TownBlock> result,
         AwtTownPlacer.GridDir xDir,
         AwtTownPlacer.GridDir zDir,
         AwtTownPlacer.Box bounds,
         boolean[] roadBorders,
         int centerX,
         int centerZ,
         int blockSize,
         int plotSize,
         boolean gridRoads
      ) {
         int totalWidth = bounds.maxX - bounds.minX;
         int totalLength = bounds.maxZ - bounds.minZ;
         int widthToUse = totalWidth - 1;
         int lengthToUse = totalLength - 1;
         int xDivs = 0;

         int zDivs;
         for (zDivs = 0; widthToUse > 0; xDivs++) {
            widthToUse -= blockSize + 2;
         }

         while (lengthToUse > 0) {
            lengthToUse -= blockSize + 2;
            zDivs++;
         }

         if (xDivs > 0 && zDivs > 0) {
            widthToUse = totalWidth;
            int xStart = xDir.x < 0 ? bounds.maxX - 1 : bounds.minX + 1;

            for (int x = 0; x < xDivs; x++) {
               int xSize = Math.min(widthToUse, blockSize);
               int xEnd = xStart + xDir.x * (xSize - 1);
               int xIndex = xDir == AwtTownPlacer.GridDir.WEST ? xDivs - 1 - x : x;
               int zStart = zDir.z < 0 ? bounds.maxZ - 1 : bounds.minZ + 1;
               lengthToUse = totalLength;

               for (int z = 0; z < zDivs; z++) {
                  int zSize = Math.min(lengthToUse, blockSize);
                  int zEnd = zStart + zDir.z * (zSize - 1);
                  int zIndex = zDir == AwtTownPlacer.GridDir.NORTH ? zDivs - 1 - z : z;
                  AwtTownPlacer.Box blockBox = new AwtTownPlacer.Box(xStart, bounds.minY, zStart, xEnd, bounds.maxY, zEnd).normalized();
                  boolean[] borders = gridRoads ? this.gridBorders(xDir, zDir, xIndex, zIndex, xDivs, zDivs) : new boolean[4];
                  float distance = this.distance(blockBox.centerX(), blockBox.centerZ(), centerX, centerZ);
                  result.add(new AwtTownPlacer.TownBlock(xDir, zDir, blockBox, xIndex, zIndex, borders, distance, plotSize));
                  lengthToUse -= blockSize + 2;
                  zStart = zEnd + zDir.z * 3;
               }

               widthToUse -= blockSize + 2;
               xStart = xEnd + xDir.x * 3;
            }
         }
      }

      private boolean[] gridBorders(AwtTownPlacer.GridDir xDir, AwtTownPlacer.GridDir zDir, int x, int z, int xDivs, int zDivs) {
         boolean[] borders = new boolean[4];
         if (zDir == AwtTownPlacer.GridDir.NORTH) {
            borders[AwtTownPlacer.GridDir.SOUTH.ordinal()] = true;
            borders[AwtTownPlacer.GridDir.NORTH.ordinal()] = z > 0;
         } else {
            borders[AwtTownPlacer.GridDir.NORTH.ordinal()] = true;
            borders[AwtTownPlacer.GridDir.SOUTH.ordinal()] = z < zDivs - 1;
         }

         if (xDir == AwtTownPlacer.GridDir.WEST) {
            borders[AwtTownPlacer.GridDir.EAST.ordinal()] = true;
            borders[AwtTownPlacer.GridDir.WEST.ordinal()] = x > 0;
         } else {
            borders[AwtTownPlacer.GridDir.WEST.ordinal()] = true;
            borders[AwtTownPlacer.GridDir.EAST.ordinal()] = x < xDivs - 1;
         }

         return borders;
      }

      private float distance(int x1, int z1, int x2, int z2) {
         int x = x1 - x2;
         int z = z1 - z2;
         return (float)Math.sqrt((double)(x * x + z * z));
      }

      private void placeFixedStructures(List<AwtTownPlacer.TownBlock> blocks, List<AwtTownPlacer.TownStructureEntry> entries, boolean singleRandom) {
         List<AwtTownPlacer.TownStructureEntry> remaining = entries.stream()
            .filter(entry -> !entry.templateName().isBlank() && entry.weight() > 0)
            .collect(Collectors.toCollection(ArrayList::new));

         for (AwtTownPlacer.TownBlock block : blocks) {
            for (AwtTownPlacer.TownPlot plot : block.plots) {
               if (!plot.closed && plot.hasRoadBorder() && !remaining.isEmpty()) {
                  int index = singleRandom ? this.random.nextInt(remaining.size()) : 0;
                  if (this.placeBuildingOnPlot(remaining.get(index), plot, false)) {
                     remaining.remove(index);
                     if (singleRandom) {
                        return;
                     }
                  }
               }
            }
         }
      }

      private void placeWeightedStructures(List<AwtTownPlacer.TownBlock> blocks, List<AwtTownPlacer.TownStructureEntry> entries, boolean center) {
         if (!entries.isEmpty()) {
            List<AwtTownPlacer.TownStructureEntry> expanded = this.weightedCopies(entries);

            for (AwtTownPlacer.TownBlock block : this.spreadBlocks(blocks)) {
               for (AwtTownPlacer.TownPlot plot : block.plots) {
                  if (!plot.closed && !plot.skipped && plot.hasRoadBorder()) {
                     if (!center && this.town.interiorEmptyPlotChance() > 0 && this.random.nextInt(100) < this.town.interiorEmptyPlotChance()) {
                        plot.skipped = true;
                     } else {
                        this.placeBuildingOnPlot(expanded.get(this.random.nextInt(expanded.size())), plot, center);
                     }
                  }
               }
            }
         }
      }

      private void placeExteriorStructures(List<AwtTownPlacer.TownBlock> blocks, List<AwtTownPlacer.TownStructureEntry> entries, BlockPos min, int totalSize) {
         if (!blocks.isEmpty() && !entries.isEmpty()) {
            List<AwtTownPlacer.TownStructureEntry> expanded = this.weightedCopies(entries);
            double exteriorHalf = Math.hypot((double)totalSize / 2.0 - 7.0, (double)totalSize / 2.0 - 7.0);
            double wallHalf = Math.max(0.0, (double)totalSize / 2.0 - 7.0 - (double)this.town.exteriorSize() * 16.0);
            double distanceRange = Math.max(1.0, exteriorHalf - wallHalf);
            int centerX = min.getX() + totalSize / 2 - 1;
            int centerZ = min.getZ() + totalSize / 2 - 1;

            for (AwtTownPlacer.TownBlock block : blocks) {
               for (AwtTownPlacer.TownPlot plot : block.plots) {
                  if (!plot.closed) {
                     double plotDistance = (double)this.distance(plot.bb.centerX(), plot.bb.centerZ(), centerX, centerZ) - wallHalf;
                     double chance = 1.0 - Math.max(0.0, Math.min(1.0, plotDistance / distanceRange));
                     chance *= chance;
                     if (this.random.nextDouble() < chance) {
                        this.placeBuildingOnPlot(expanded.get(this.random.nextInt(expanded.size())), plot, true);
                     }
                  }
               }
            }
         }
      }

      private List<AwtTownPlacer.TownStructureEntry> weightedCopies(List<AwtTownPlacer.TownStructureEntry> entries) {
         List<AwtTownPlacer.TownStructureEntry> result = new ArrayList<>();

         for (AwtTownPlacer.TownStructureEntry entry : entries) {
            if (!entry.templateName().isBlank() && entry.weight() > 0) {
               int copies = Math.max(1, entry.weight());

               for (int i = 0; i < copies; i++) {
                  result.add(entry);
               }
            }
         }

         return result;
      }

      private List<AwtTownPlacer.TownBlock> spreadBlocks(List<AwtTownPlacer.TownBlock> blocks) {
         List<AwtTownPlacer.TownBlock> result = new ArrayList<>(blocks);
         result.sort((first, second) -> {
            int bucketCompare = Integer.compare(Math.round(first.distanceFromCenter / 16.0F), Math.round(second.distanceFromCenter / 16.0F));
            if (bucketCompare != 0) {
               return bucketCompare;
            } else {
               long firstHash = this.blockOrderHash(first);
               long secondHash = this.blockOrderHash(second);
               return Long.compare(firstHash, secondHash);
            }
         });
         return result;
      }

      private long blockOrderHash(AwtTownPlacer.TownBlock block) {
         long value = this.origin.asLong();
         value ^= (long)block.x * 341873128712L;
         value ^= (long)block.z * 132897987541L;
         value ^= (long)block.xDir.ordinal() * 42317861L;
         value ^= (long)block.zDir.ordinal() * 374761393L;
         value ^= value >>> 33;
         value *= -49064778989728563L;
         return value ^ value >>> 33;
      }

      private boolean placeBuildingOnPlot(AwtTownPlacer.TownStructureEntry entry, AwtTownPlacer.TownPlot plot, boolean center) {
         AwtTownPlacer.AwsTemplateRef ref = AwtTownPlacer.awsTemplate(entry.templateName());
         if (ref == null) {
            AncientWarfareCore.LOG.debug("Skipping unresolved AWT structure template {}", entry.templateName());
            return false;
         } else {
            Direction buildingFacing = this.faceForPlot(plot).getOpposite();
            int width = ref.rotatedWidth(buildingFacing);
            int length = ref.rotatedLength(buildingFacing);
            int expansion = Math.max(0, this.town.buildingExpansion());
            int expandedWidth = width;
            int expandedLength = length;
            if (buildingFacing.getAxis() == Axis.Z) {
               expandedWidth = width + expansion;
               if (!center) {
                  expandedLength = length + 1;
               }
            } else {
               expandedLength = length + expansion;
               if (!center) {
                  expandedWidth = width + 1;
               }
            }

            if ((plot.width() < expandedWidth || plot.length() < expandedLength) && !plot.expand(expandedWidth, expandedLength)) {
               return false;
            } else {
               plot.markClosed();
               BlockPos min = this.placementMinForPlot(plot, ref, buildingFacing, width, length, center);
               if (this.planning) {
                  this.plannedBuildings.add(new AwtTownPlacer.PlannedTemplate(ref.path(), min, buildingFacing));
               } else {
                  AwsTemplatePlacer.placeAtMin(this.level, min, ref.path(), buildingFacing);
               }

               return true;
            }
         }
      }

      private Direction faceForPlot(AwtTownPlacer.TownPlot plot) {
         Direction face = Direction.from2DDataValue(this.random.nextInt(4));

         for (int i = 0; i < 4; i++) {
            face = face.getClockWise();
            if (plot.roadBorders[this.gridOrdinal(face)]) {
               return face;
            }
         }

         return face;
      }

      private int gridOrdinal(Direction direction) {
         return switch (direction) {
            case SOUTH -> AwtTownPlacer.GridDir.SOUTH.ordinal();
            case WEST -> AwtTownPlacer.GridDir.WEST.ordinal();
            case NORTH -> AwtTownPlacer.GridDir.NORTH.ordinal();
            case EAST -> AwtTownPlacer.GridDir.EAST.ordinal();
            default -> AwtTownPlacer.GridDir.SOUTH.ordinal();
         };
      }

      private BlockPos placementMinForPlot(AwtTownPlacer.TownPlot plot, AwtTownPlacer.AwsTemplateRef ref, Direction face, int width, int length, boolean center) {
         int extraWidth = Math.max(0, plot.width() - width);
         int extraLength = Math.max(0, plot.length() - length);
         int wAdj;
         int lAdj;
         if (center) {
            wAdj = extraWidth / 2;
            lAdj = extraLength / 2;
         } else {
            if (face.getAxis() == Axis.Z) {
               wAdj = extraWidth / 2;
            } else {
               wAdj = face == Direction.WEST ? extraWidth : 0;
            }

            if (face.getAxis() == Axis.X) {
               lAdj = extraLength / 2;
            } else {
               lAdj = face == Direction.NORTH ? extraLength : 0;
            }

            switch (face) {
               case SOUTH:
                  lAdj++;
                  break;
               case WEST:
                  wAdj--;
                  break;
               case NORTH:
                  lAdj--;
                  break;
               case EAST:
                  wAdj++;
            }
         }

         return new BlockPos(plot.bb.minX + wAdj, plot.bb.minY - ref.offsetY(), plot.bb.minZ + lAdj);
      }

      private BlockPos rearLeftCorner(BlockPos min, BlockPos max, Direction face) {
         return switch (face) {
            case SOUTH -> new BlockPos(max.getX(), min.getY(), max.getZ());
            case WEST -> new BlockPos(min.getX(), min.getY(), max.getZ());
            case NORTH -> min;
            case EAST -> new BlockPos(max.getX(), min.getY(), min.getZ());
            default -> min;
         };
      }

      private Direction directionTowardCenter(BlockPos pos) {
         int dx = this.origin.getX() - pos.getX();
         int dz = this.origin.getZ() - pos.getZ();
         if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
         } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
         }
      }

      private void placeLampLine(BlockPos min, int totalSize, int center) {
         AwtTownPlacer.AwsTemplateRef ref = AwtTownPlacer.awsTemplate(this.town.lamp().templateName());
         if (ref != null) {
            for (int i = 8; i < totalSize - 8; i += 12) {
               this.placeBuildingOrigin(ref, min.offset(center + 3, 0, i), Direction.SOUTH);
               this.placeBuildingOrigin(ref, min.offset(i, 0, center + 3), Direction.EAST);
            }
         }
      }

      private void placeBuildingOrigin(AwtTownPlacer.AwsTemplateRef ref, BlockPos pos, Direction direction) {
         if (this.planning) {
            this.plannedBuildings.add(new AwtTownPlacer.PlannedTemplate(ref.path(), this.structureMin(pos, ref, direction), direction));
         } else {
            AwsTemplatePlacer.place(this.level, pos, ref.path(), direction);
         }
      }

      private BlockPos structureMin(BlockPos origin, AwtTownPlacer.AwsTemplateRef ref, Direction direction) {
         BlockPos first = origin.relative(direction.getCounterClockWise(), ref.offsetX()).relative(direction, ref.offsetZ()).below(ref.offsetY());
         BlockPos second = first.relative(direction.getClockWise(), ref.sizeX() - 1).relative(direction, -(ref.sizeZ() - 1)).above(ref.sizeY() - 1);
         return new BlockPos(
            Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ())
         );
      }
   }

   private static final class TownBlock {
      private final AwtTownPlacer.GridDir xDir;
      private final AwtTownPlacer.GridDir zDir;
      private final AwtTownPlacer.Box bb;
      private final int x;
      private final int z;
      private final boolean[] roadBorders;
      private final float distanceFromCenter;
      private final List<AwtTownPlacer.TownPlot> plots = new ArrayList<>();
      private AwtTownPlacer.TownPlot[] plotArray;
      private int plotsWidth;
      private int plotsLength;

      private TownBlock(
         AwtTownPlacer.GridDir xDir,
         AwtTownPlacer.GridDir zDir,
         AwtTownPlacer.Box bb,
         int x,
         int z,
         boolean[] roadBorders,
         float distanceFromCenter,
         int plotSize
      ) {
         this.xDir = xDir;
         this.zDir = zDir;
         this.bb = bb;
         this.x = x;
         this.z = z;
         this.roadBorders = roadBorders;
         this.distanceFromCenter = distanceFromCenter;
         this.subdivide(plotSize);
      }

      private void subdivide(int plotSize) {
         int xWidth = this.bb.width();
         int zLength = this.bb.length();
         this.plotsWidth = xWidth / plotSize + (xWidth % plotSize == 0 ? 0 : 1);
         this.plotsLength = zLength / plotSize + (zLength % plotSize == 0 ? 0 : 1);
         this.plotArray = new AwtTownPlacer.TownPlot[this.plotsWidth * this.plotsLength];
         int xStart = this.xDir == AwtTownPlacer.GridDir.WEST ? this.bb.maxX : this.bb.minX;
         int widthToUse = xWidth;

         for (int px = 0; px < this.plotsWidth; px++) {
            int xSize = Math.min(widthToUse, plotSize);
            int xEnd = xStart + (xSize - 1) * this.xDir.x;
            int xIndex = this.xDir == AwtTownPlacer.GridDir.WEST ? this.plotsWidth - 1 - px : px;
            int zStart = this.zDir == AwtTownPlacer.GridDir.NORTH ? this.bb.maxZ : this.bb.minZ;
            int lengthToUse = zLength;

            for (int pz = 0; pz < this.plotsLength; pz++) {
               int zSize = Math.min(lengthToUse, plotSize);
               int zEnd = zStart + (zSize - 1) * this.zDir.z;
               int zIndex = this.zDir == AwtTownPlacer.GridDir.NORTH ? this.plotsLength - 1 - pz : pz;
               AwtTownPlacer.TownPlot plot = new AwtTownPlacer.TownPlot(
                  this, new AwtTownPlacer.Box(xStart, this.bb.minY, zStart, xEnd, this.bb.maxY, zEnd).normalized(), xIndex, zIndex
               );
               this.setRoadBorders(plot);
               this.plots.add(plot);
               this.plotArray[this.index(xIndex, zIndex)] = plot;
               lengthToUse -= plotSize;
               zStart = zEnd + this.zDir.z;
            }

            widthToUse -= plotSize;
            xStart = xEnd + this.xDir.x;
         }
      }

      private int index(int x, int z) {
         return z * this.plotsWidth + x;
      }

      private boolean hasRoadBorder(AwtTownPlacer.GridDir direction) {
         return this.roadBorders[direction.ordinal()];
      }

      private AwtTownPlacer.TownPlot plot(int x, int z) {
         return x >= 0 && z >= 0 && x < this.plotsWidth && z < this.plotsLength ? this.plotArray[this.index(x, z)] : null;
      }

      private void setRoadBorders(AwtTownPlacer.TownPlot plot) {
         if (this.roadBorders[AwtTownPlacer.GridDir.NORTH.ordinal()] && plot.z == 0) {
            plot.roadBorders[AwtTownPlacer.GridDir.NORTH.ordinal()] = true;
         }

         if (this.roadBorders[AwtTownPlacer.GridDir.SOUTH.ordinal()] && plot.z == this.plotsLength - 1) {
            plot.roadBorders[AwtTownPlacer.GridDir.SOUTH.ordinal()] = true;
         }

         if (this.roadBorders[AwtTownPlacer.GridDir.WEST.ordinal()] && plot.x == 0) {
            plot.roadBorders[AwtTownPlacer.GridDir.WEST.ordinal()] = true;
         }

         if (this.roadBorders[AwtTownPlacer.GridDir.EAST.ordinal()] && plot.x == this.plotsWidth - 1) {
            plot.roadBorders[AwtTownPlacer.GridDir.EAST.ordinal()] = true;
         }
      }
   }

   public static record TownPlan(
      String path,
      String name,
      BlockPos origin,
      BlockPos min,
      int totalSize,
      List<BlockPos> roads,
      List<AwtTownPlacer.PlannedTemplate> walls,
      List<AwtTownPlacer.PlannedTemplate> buildings,
      Set<Long> occupiedBlocks
   ) {
   }

   private static final class TownPlot {
      private final AwtTownPlacer.TownBlock block;
      private AwtTownPlacer.Box bb;
      private final int x;
      private final int z;
      private int minX;
      private int minZ;
      private int maxX;
      private int maxZ;
      private final boolean[] roadBorders = new boolean[4];
      private boolean closed;
      private boolean skipped;

      private TownPlot(AwtTownPlacer.TownBlock block, AwtTownPlacer.Box bb, int x, int z) {
         this.block = block;
         this.bb = bb;
         this.x = x;
         this.z = z;
         this.reseatMinMax();
      }

      private boolean hasRoadBorder() {
         for (boolean roadBorder : this.roadBorders) {
            if (roadBorder) {
               return true;
            }
         }

         return false;
      }

      private int width() {
         return this.bb.width();
      }

      private int length() {
         return this.bb.length();
      }

      private void markClosed() {
         for (int px = this.minX; px <= this.maxX; px++) {
            for (int pz = this.minZ; pz <= this.maxZ; pz++) {
               AwtTownPlacer.TownPlot plot = this.block.plot(px, pz);
               if (plot != null) {
                  plot.closed = true;
               }
            }
         }
      }

      private boolean expand(int xSize, int zSize) {
         AwtTownPlacer.Box oldBox = this.bb.copy();
         int oldMinX = this.minX;
         int oldMinZ = this.minZ;
         int oldMaxX = this.maxX;
         int oldMaxZ = this.maxZ;
         if (this.tryExpand(xSize, zSize)) {
            return true;
         } else {
            this.bb = oldBox;
            this.minX = oldMinX;
            this.minZ = oldMinZ;
            this.maxX = oldMaxX;
            this.maxZ = oldMaxZ;
            return false;
         }
      }

      private boolean tryExpand(int xSize, int zSize) {
         while (this.width() < xSize) {
            if (!this.expandEast() && !this.expandWest()) {
               return false;
            }
         }

         while (this.length() < zSize) {
            if (!this.expandNorth() && !this.expandSouth()) {
               return false;
            }
         }

         return true;
      }

      private boolean expandNorth() {
         if (this.minZ <= 0) {
            return false;
         } else {
            for (int px = this.minX; px <= this.maxX; px++) {
               AwtTownPlacer.TownPlot plot = this.block.plot(px, this.minZ - 1);
               if (plot == null || plot.closed) {
                  return false;
               }
            }

            this.minZ--;
            AwtTownPlacer.TownPlot plot = this.block.plot(this.x, this.minZ);
            this.bb.minZ = plot.bb.minZ;
            return true;
         }
      }

      private boolean expandSouth() {
         if (this.maxZ + 1 >= this.block.plotsLength) {
            return false;
         } else {
            for (int px = this.minX; px <= this.maxX; px++) {
               AwtTownPlacer.TownPlot plot = this.block.plot(px, this.maxZ + 1);
               if (plot == null || plot.closed) {
                  return false;
               }
            }

            this.maxZ++;
            AwtTownPlacer.TownPlot plot = this.block.plot(this.x, this.maxZ);
            this.bb.maxZ = plot.bb.maxZ;
            return true;
         }
      }

      private boolean expandEast() {
         if (this.minX <= 0) {
            return false;
         } else {
            for (int pz = this.minZ; pz <= this.maxZ; pz++) {
               AwtTownPlacer.TownPlot plot = this.block.plot(this.minX - 1, pz);
               if (plot == null || plot.closed) {
                  return false;
               }
            }

            this.minX--;
            AwtTownPlacer.TownPlot plot = this.block.plot(this.minX, this.z);
            this.bb.minX = plot.bb.minX;
            return true;
         }
      }

      private boolean expandWest() {
         if (this.maxX + 1 >= this.block.plotsWidth) {
            return false;
         } else {
            for (int pz = this.minZ; pz <= this.maxZ; pz++) {
               AwtTownPlacer.TownPlot plot = this.block.plot(this.maxX + 1, pz);
               if (plot == null || plot.closed) {
                  return false;
               }
            }

            this.maxX++;
            AwtTownPlacer.TownPlot plot = this.block.plot(this.maxX, this.z);
            this.bb.maxX = plot.bb.maxX;
            return true;
         }
      }

      private void reseatMinMax() {
         this.minX = this.x;
         this.minZ = this.z;
         this.maxX = this.x;
         this.maxZ = this.z;
      }
   }

   private static record TownStructureEntry(String templateName, int weight) {
   }

   private static record TownTemplate(
      String name,
      int minSize,
      int maxSize,
      int townBlockSize,
      int townPlotSize,
      int buildingExpansion,
      int wallStyle,
      int wallSize,
      int exteriorSize,
      int exteriorPlotSize,
      int interiorEmptyPlotChance,
      List<String> roadBlocks,
      Map<Integer, AwtTownPlacer.TownWallEntry> wallIds,
      List<AwtTownPlacer.TownWallEntry> corners,
      List<AwtTownPlacer.TownWallEntry> walls,
      List<AwtTownPlacer.TownWallEntry> gates,
      List<AwtTownPlacer.TownWallEntry> leftGates,
      List<AwtTownPlacer.TownWallEntry> rightGates,
      Map<Integer, int[]> wallPatterns,
      AwtTownPlacer.TownStructureEntry lamp,
      List<AwtTownPlacer.TownStructureEntry> uniqueStructures,
      List<AwtTownPlacer.TownStructureEntry> mainStructures,
      List<AwtTownPlacer.TownStructureEntry> houseStructures,
      List<AwtTownPlacer.TownStructureEntry> cosmeticStructures,
      List<AwtTownPlacer.TownStructureEntry> exteriorStructures
   ) {
      static AwtTownPlacer.TownTemplate load(String path) {
         AwtTownPlacer.Builder builder = new AwtTownPlacer.Builder();
         List<String> lines = AwtTownPlacer.readLines(path);

         for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("header:")) {
               i = AwtTownPlacer.parseHeader(lines, i + 1, builder);
            } else if (lower.startsWith("walls:")) {
               i = AwtTownPlacer.parseWalls(lines, i + 1, builder);
            } else if (lower.startsWith("wallpatterns:")) {
               i = AwtTownPlacer.parseWallPatterns(lines, i + 1, builder);
            } else if (lower.startsWith("uniquestructures:")) {
               i = AwtTownPlacer.parseStructures(lines, i + 1, ":enduniquestructures", builder.uniqueStructures, false);
            } else if (lower.startsWith("mainstructures:")) {
               i = AwtTownPlacer.parseStructures(lines, i + 1, ":endmainstructures", builder.mainStructures, false);
            } else if (lower.startsWith("housestructures:")) {
               i = AwtTownPlacer.parseStructures(lines, i + 1, ":endhousestructures", builder.houseStructures, true);
            } else if (lower.startsWith("cosmeticstructures:")) {
               i = AwtTownPlacer.parseStructures(lines, i + 1, ":endcosmeticstructures", builder.cosmeticStructures, true);
            } else if (lower.startsWith("exteriorstructures:")) {
               i = AwtTownPlacer.parseStructures(lines, i + 1, ":endexteriorstructures", builder.exteriorStructures, true);
            }
         }

         return builder.build();
      }
   }

   private static record TownWallEntry(String templateName, String type, int id, int weight) {
   }
}
