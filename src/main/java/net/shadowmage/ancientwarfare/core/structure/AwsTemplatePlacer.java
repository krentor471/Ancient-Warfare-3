package net.shadowmage.ancientwarfare.core.structure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.block.AdvancedLootChestBlock;
import net.shadowmage.ancientwarfare.core.block.BenchStructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.GateProxyBlock;
import net.shadowmage.ancientwarfare.core.block.StructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.TallStructureDecorBlock;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireStructureLootCompat;
import net.shadowmage.ancientwarfare.core.compat.LegacyIceAndFireStructureCompat;
import net.shadowmage.ancientwarfare.core.compat.SlashBladeStructureLootCompat;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;
import net.shadowmage.ancientwarfare.core.tile.AdvancedSpawnerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.GateProxyBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.ProtectionFlagBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.StructureContainerBlockEntity;

public final class AwsTemplatePlacer {
   private static final String RESOURCE_ROOT = "/assets/ancientwarfare/template/";
   private static final String SURVIVAL_INDEX = "/assets/ancientwarfare/survival_template_index.txt";
   private static final Map<String, AwsTemplatePlacer.AwsTemplate> TEMPLATE_CACHE = new ConcurrentHashMap<>();
   private static volatile List<AwsTemplatePlacer.TemplateInfo> cachedSurvivalTemplates;
   private static final Direction[] HORIZONTAL = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

   private AwsTemplatePlacer() {
   }

   public static List<AwsTemplatePlacer.TemplateInfo> survivalTemplates() {
      List<AwsTemplatePlacer.TemplateInfo> cached = cachedSurvivalTemplates;
      if (cached != null) {
         return cached;
      } else {
         List<AwsTemplatePlacer.TemplateInfo> loaded = readSurvivalIndex();
         if (loaded.isEmpty()) {
            loaded = AwsTemplateIndex.allTemplates().stream().map(path -> {
               try {
                  AwsTemplatePlacer.AwsTemplate template = AwsTemplatePlacer.AwsTemplate.load(path);
                  return template.survival ? new AwsTemplatePlacer.TemplateInfo(template.name, path) : null;
               } catch (RuntimeException var2) {
                  AncientWarfareCore.LOG.warn("Unable to index survival AWS template {}", path, var2);
                  return null;
               }
            }).filter(Objects::nonNull).sorted(Comparator.comparing(AwsTemplatePlacer.TemplateInfo::name, String.CASE_INSENSITIVE_ORDER)).toList();
         }

         cachedSurvivalTemplates = loaded;
         return loaded;
      }
   }

   private static List<AwsTemplatePlacer.TemplateInfo> readSurvivalIndex() {
      try {
         List var2;
         try (InputStream stream = AwsTemplatePlacer.class.getResourceAsStream("/assets/ancientwarfare/survival_template_index.txt")) {
            if (stream == null) {
               return List.of();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
               var2 = reader.lines().map(String::trim).filter(line -> !line.isBlank()).map(line -> {
                  String[] parts = line.split("\\t", 2);
                  return new AwsTemplatePlacer.TemplateInfo(parts[1].trim(), parts[0].trim());
               }).sorted(Comparator.comparing(AwsTemplatePlacer.TemplateInfo::name, String.CASE_INSENSITIVE_ORDER)).toList();
            }
         }

         return var2;
      } catch (RuntimeException | IOException var8) {
         AncientWarfareCore.LOG.warn("Unable to read lightweight survival template index", var8);
         return List.of();
      }
   }

   public static List<AwsTemplatePlacer.ResourceInfo> templateResources(String templatePath) {
      return templatePath != null && !templatePath.isBlank() ? template(templatePath).resources() : List.of();
   }

   public static boolean isBasicNaturalTerrain(BlockState state) {
      if (state != null && !state.m_60795_() && !state.m_60713_(Blocks.f_50752_) && !state.m_155947_()) {
         Block block = state.m_60734_();
         return state.m_60713_(Blocks.f_49990_)
            || state.m_60713_(Blocks.f_49991_)
            || state.m_60713_(Blocks.f_50575_)
            || state.m_60713_(Blocks.f_50576_)
            || state.m_60713_(Blocks.f_50037_)
            || state.m_60713_(Blocks.f_50038_)
            || state.m_60713_(Blocks.f_50567_)
            || block instanceof CoralBlock
            || block instanceof BaseCoralPlantTypeBlock
            || state.m_60713_(Blocks.f_50579_)
            || state.m_60713_(Blocks.f_50580_)
            || state.m_60713_(Blocks.f_50581_)
            || state.m_60713_(Blocks.f_50582_)
            || state.m_60713_(Blocks.f_50583_)
            || state.m_204336_(BlockTags.f_144274_)
            || state.m_204336_(BlockTags.f_13061_)
            || state.m_204336_(BlockTags.f_13062_)
            || state.m_204336_(BlockTags.f_13029_)
            || state.m_204336_(BlockTags.f_144279_)
            || state.m_204336_(BlockTags.f_13047_)
            || state.m_204336_(BlockTags.f_278394_)
            || state.m_204336_(BlockTags.f_13035_)
            || state.m_204336_(BlockTags.f_13104_)
            || state.m_204336_(BlockTags.f_13041_)
            || state.m_204336_(BlockTags.f_13077_)
            || state.m_204336_(BlockTags.f_144262_)
            || state.m_204336_(BlockTags.f_144258_)
            || state.m_204336_(BlockTags.f_144264_)
            || state.m_204336_(BlockTags.f_13043_)
            || state.m_204336_(BlockTags.f_144260_)
            || state.m_204336_(BlockTags.f_144263_)
            || state.m_204336_(BlockTags.f_144261_)
            || state.m_204336_(BlockTags.f_144259_)
            || block == Blocks.f_49994_
            || block == Blocks.f_50129_
            || block == Blocks.f_220864_
            || block == Blocks.f_220834_
            || block == Blocks.f_50135_
            || block == Blocks.f_50136_
            || block == Blocks.f_50259_
            || block == Blocks.f_152497_
            || block == Blocks.f_152537_
            || block == Blocks.f_152588_
            || block == Blocks.f_50450_;
      } else {
         return false;
      }
   }

   public static boolean isOpenWaterTerrain(BlockState state) {
      return state != null
         && !state.m_155947_()
         && (
            state.m_60795_()
               || state.m_60713_(Blocks.f_49990_)
               || state.m_60713_(Blocks.f_50575_)
               || state.m_60713_(Blocks.f_50576_)
               || state.m_60713_(Blocks.f_50037_)
               || state.m_60713_(Blocks.f_50038_)
         );
   }

   public static AwsTemplatePlacer.PreviewData preview(String templatePath, BlockPos origin, Direction facing) {
      return template(templatePath).preview(origin, facing);
   }

   public static boolean place(Level level, BlockPos origin, String templatePath, Direction facing) {
      return place(level, origin, templatePath, facing, true);
   }

   public static boolean placePlayerBuilt(Level level, BlockPos origin, String templatePath, Direction facing) {
      return place(level, origin, templatePath, facing, false);
   }

   public static int incrementalVolume(String templatePath) {
      AwsTemplatePlacer.AwsTemplate template = template(templatePath);
      return template.sizeX() * template.sizeY() * template.sizeZ();
   }

   public static AwsTemplatePlacer.IncrementalStep clearPlayerBuiltStep(
      ServerLevel level, BlockPos origin, String templatePath, Direction facing, int cursor, int budget, BlockPos builderPos
   ) {
      return template(templatePath).clearIncremental(level, origin, facing, cursor, budget, builderPos);
   }

   public static AwsTemplatePlacer.IncrementalStep placePlayerBuiltStep(
      ServerLevel level, BlockPos origin, String templatePath, Direction facing, int cursor, int budget, BlockPos builderPos
   ) {
      return template(templatePath).placeIncremental(level, origin, facing, cursor, budget, builderPos);
   }

   public static void finishPlayerBuiltIncremental(ServerLevel level, BlockPos origin, String templatePath, Direction facing) {
      template(templatePath).finishIncremental(level, origin, facing);
   }

   private static boolean place(Level level, BlockPos origin, String templatePath, Direction facing, boolean fillUnconfiguredContainers) {
      if (templatePath != null && !templatePath.isBlank()) {
         try {
            AwsTemplatePlacer.AwsTemplate template = template(templatePath);
            template.place(level, origin, facing, fillUnconfiguredContainers);
            return true;
         } catch (RuntimeException var6) {
            AncientWarfareCore.LOG.warn("Unable to place AWS template {}", templatePath, var6);
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean placeAtMin(Level level, BlockPos min, String templatePath, Direction facing) {
      return placeAtMin(level, min, templatePath, facing, true);
   }

   public static boolean placeAtMin(Level level, BlockPos min, String templatePath, Direction facing, boolean clearAir) {
      if (templatePath != null && !templatePath.isBlank()) {
         try {
            AwsTemplatePlacer.AwsTemplate template = template(templatePath);
            template.placeAtMin(level, min, facing, clearAir, true);
            return true;
         } catch (RuntimeException var6) {
            AncientWarfareCore.LOG.warn("Unable to place AWS template {} at explicit min", templatePath, var6);
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean placeAtMinPreserving(Level level, BlockPos min, String templatePath, Direction facing, Set<Long> protectedBlocks) {
      if (templatePath != null && !templatePath.isBlank()) {
         try {
            AwsTemplatePlacer.AwsTemplate template = template(templatePath);
            template.placeAtMinPreserving(level, min, facing, protectedBlocks, true);
            return true;
         } catch (RuntimeException var6) {
            AncientWarfareCore.LOG.warn("Unable to place protected AWS template {} at explicit min", templatePath, var6);
            return false;
         }
      } else {
         return false;
      }
   }

   public static Set<Long> occupiedBlocks(BlockPos min, String templatePath, Direction facing) {
      AwsTemplatePlacer.AwsTemplate template = template(templatePath);
      return template.occupiedBlocks(min, facing);
   }

   public static Set<Long> requiredChunks(BlockPos min, String templatePath, Direction facing) {
      AwsTemplatePlacer.AwsTemplate template = template(templatePath);
      int sizeX = facing.m_122434_() == Axis.X ? template.sizeZ() : template.sizeX();
      int sizeZ = facing.m_122434_() == Axis.X ? template.sizeX() : template.sizeZ();
      int minChunkX = Math.floorDiv(min.m_123341_(), 16);
      int maxChunkX = Math.floorDiv(min.m_123341_() + sizeX - 1, 16);
      int minChunkZ = Math.floorDiv(min.m_123343_(), 16);
      int maxChunkZ = Math.floorDiv(min.m_123343_() + sizeZ - 1, 16);
      Set<Long> chunks = new HashSet<>();

      for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
         for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            chunks.add(ChunkPos.m_45589_(chunkX, chunkZ));
         }
      }

      return chunks;
   }

   public static AwsTemplatePlacer.ChunkLayout chunkLayout(BlockPos min, String templatePath, Direction facing) {
      AwsTemplatePlacer.AwsTemplate template = template(templatePath);
      int sizeX = facing.m_122434_() == Axis.X ? template.sizeZ() : template.sizeX();
      int sizeZ = facing.m_122434_() == Axis.X ? template.sizeX() : template.sizeZ();
      return new AwsTemplatePlacer.ChunkLayout(
         Math.floorDiv(min.m_123341_(), 16),
         Math.floorDiv(min.m_123343_(), 16),
         Math.floorDiv(min.m_123341_() + sizeX - 1, 16),
         Math.floorDiv(min.m_123343_() + sizeZ - 1, 16)
      );
   }

   public static boolean placeChunk(ServerLevel level, BlockPos min, String templatePath, Direction facing, ChunkPos chunk) {
      try {
         template(templatePath).placeChunk(level, min, facing, chunk);
         return true;
      } catch (RuntimeException var6) {
         AncientWarfareCore.LOG.warn("Unable to place phased AWS chunk {} for {}", new Object[]{chunk, templatePath, var6});
         return false;
      }
   }

   public static void finishPhasedPlacement(ServerLevel level, BlockPos min, String templatePath, Direction facing) {
      AwsTemplatePlacer.AwsTemplate template = template(templatePath);
      template.finishPhasedPlacement(level, min, facing);
      refreshHorizontalConnections(level, template.occupiedBlocks(min, facing));
   }

   public static void connectChunk(ServerLevel level, BlockPos min, String templatePath, Direction facing, ChunkPos chunk) {
      template(templatePath).connectChunk(level, min, facing, chunk);
   }

   public static void configureChunk(ServerLevel level, BlockPos min, String templatePath, Direction facing, ChunkPos chunk) {
      template(templatePath).configureChunk(level, min, facing, chunk, true);
   }

   private static AwsTemplatePlacer.AwsTemplate template(String templatePath) {
      return TEMPLATE_CACHE.computeIfAbsent(templatePath, AwsTemplatePlacer.AwsTemplate::load);
   }

   public static AwsTemplatePlacer.IslandConstruction constructionPlan(String path, BlockPos min, Direction facing) {
      return new AwsTemplatePlacer.IslandConstruction(template(path), min, facing);
   }

   public static boolean aquaticGateCellsSafe(ServerLevel level, BlockPos min, String path, Direction facing, boolean allowNaturalTerrain) {
      AwsTemplatePlacer.AwsTemplate template = template(path);
      int turns = (facing.m_122416_() + 2) % 4;

      for (AwsTemplatePlacer.AwsTemplate.EntityRule entityRule : template.entities) {
         if ("awGate".equals(entityRule.plugin)) {
            BlockPos anchor = AwsTemplatePlacer.AwsTemplate.EntityRule.rotateAnchor(
                  AwsTemplatePlacer.AwsTemplate.EntityRule.decodeLegacyBlockPos(
                     AwsTemplatePlacer.AwsTemplate.EntityRule.parseLong(entityRule.data, "position", 0L)
                  ),
                  turns,
                  template.sizeX,
                  template.sizeZ
               )
               .m_121955_(min);
            BlockPos first = AwsTemplatePlacer.AwsTemplate.EntityRule.rotateOffset(
                  AwsTemplatePlacer.AwsTemplate.EntityRule.parseRelativePos(entityRule.data, "pos1"), turns
               )
               .m_121955_(anchor);
            BlockPos second = AwsTemplatePlacer.AwsTemplate.EntityRule.rotateOffset(
                  AwsTemplatePlacer.AwsTemplate.EntityRule.parseRelativePos(entityRule.data, "pos2"), turns
               )
               .m_121955_(anchor);
            int minX = Math.min(first.m_123341_(), second.m_123341_());
            int maxX = Math.max(first.m_123341_(), second.m_123341_());
            int minY = Math.min(first.m_123342_(), second.m_123342_());
            int maxY = Math.max(first.m_123342_(), second.m_123342_());
            int minZ = Math.min(first.m_123343_(), second.m_123343_());
            int maxZ = Math.max(first.m_123343_(), second.m_123343_());
            if (minY < level.m_141937_() || maxY >= level.m_151558_()) {
               return false;
            }

            NaturalGenerationContext.markArea(new BlockPos(minX - 16, minY, minZ - 16), maxX + 16, maxZ + 16);

            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  for (int x = minX; x <= maxX; x++) {
                     BlockPos pos = new BlockPos(x, y, z);
                     BlockState existing = level.m_8055_(pos);
                     if (level.m_7702_(pos) != null
                        || existing.m_155947_()
                        || !isOpenWaterTerrain(existing) && (!allowNaturalTerrain || !isBasicNaturalTerrain(existing))) {
                        return false;
                     }
                  }
               }
            }
         }
      }

      return true;
   }

   private static List<ChunkPos> surroundingChunks(BlockPos pos) {
      return surroundingChunks(pos, 2);
   }

   private static List<ChunkPos> surroundingChunks(BlockPos pos, int radius) {
      List<ChunkPos> result = new ArrayList<>(9);

      for (int z = Math.floorDiv(pos.m_123343_() - radius, 16); z <= Math.floorDiv(pos.m_123343_() + radius, 16); z++) {
         for (int x = Math.floorDiv(pos.m_123341_() - radius, 16); x <= Math.floorDiv(pos.m_123341_() + radius, 16); x++) {
            result.add(new ChunkPos(x, z));
         }
      }

      return result;
   }

   private static void refreshConnection(Level level, BlockPos pos) {
      BlockState state = level.m_8055_(pos);
      if (state.m_60734_() instanceof BenchStructureDecorBlock) {
         refreshBenchConnections(level, Set.of(pos));
      } else if (hasHorizontalConnections(state)) {
         BlockState connected = state;

         for (Direction direction : HORIZONTAL) {
            BlockPos neighbor = pos.m_121945_(direction);
            connected = connected.m_60728_(direction, level.m_8055_(neighbor), level, pos, neighbor);
         }

         if (connected != state) {
            level.m_7731_(pos, connected, 18);
         }
      }
   }

   public static void refreshHorizontalConnections(Level level, Set<Long> occupiedBlocks) {
      Set<BlockPos> refreshPositions = new HashSet<>();

      for (long packed : occupiedBlocks) {
         BlockPos pos = BlockPos.m_122022_(packed);
         refreshPositions.add(pos);

         for (Direction direction : Plane.HORIZONTAL) {
            refreshPositions.add(pos.m_121945_(direction));
         }
      }

      for (BlockPos pos : refreshPositions) {
         BlockState state = level.m_8055_(pos);
         if (!(state.m_60734_() instanceof BenchStructureDecorBlock) && hasHorizontalConnections(state)) {
            BlockState connected = state;

            for (Direction direction : Plane.HORIZONTAL) {
               BlockPos neighborPos = pos.m_121945_(direction);
               connected = connected.m_60728_(direction, level.m_8055_(neighborPos), level, pos, neighborPos);
            }

            if (connected != state) {
               level.m_7731_(pos, connected, 18);
            }
         }
      }

      refreshBenchConnections(level, refreshPositions);
   }

   private static void refreshBenchConnections(Level level, Set<BlockPos> refreshPositions) {
      for (BlockPos pos : refreshPositions) {
         BlockState state = level.m_8055_(pos);
         Block axis = state.m_60734_();
         if (axis instanceof BenchStructureDecorBlock) {
            BenchStructureDecorBlock bench = (BenchStructureDecorBlock)axis;
            Axis axisx = (Axis)state.m_61143_(BenchStructureDecorBlock.AXIS);
            BenchStructureDecorBlock.Legs legs;
            if (axisx == Axis.X) {
               boolean first = sameAxisBench(level, bench, state, pos.m_122024_());
               boolean second = sameAxisBench(level, bench, state, pos.m_122029_());
               legs = first && !second
                  ? BenchStructureDecorBlock.Legs.LEFT
                  : (
                     !first && second
                        ? BenchStructureDecorBlock.Legs.RIGHT
                        : (!first ? BenchStructureDecorBlock.Legs.BOTH : BenchStructureDecorBlock.Legs.NONE)
                  );
            } else {
               boolean south = sameAxisBench(level, bench, state, pos.m_122019_());
               boolean north = sameAxisBench(level, bench, state, pos.m_122012_());
               legs = north && !south
                  ? BenchStructureDecorBlock.Legs.RIGHT
                  : (!north && south ? BenchStructureDecorBlock.Legs.LEFT : (!north ? BenchStructureDecorBlock.Legs.BOTH : BenchStructureDecorBlock.Legs.NONE));
            }

            BlockState corrected = (BlockState)state.m_61124_(BenchStructureDecorBlock.LEGS, legs);
            if (corrected != state) {
               level.m_7731_(pos, corrected, 18);
            }
         }
      }
   }

   private static boolean sameAxisBench(BlockGetter level, BenchStructureDecorBlock bench, BlockState state, BlockPos neighborPos) {
      BlockState neighbor = level.m_8055_(neighborPos);
      return neighbor.m_60734_() == bench && neighbor.m_61143_(BenchStructureDecorBlock.AXIS) == state.m_61143_(BenchStructureDecorBlock.AXIS);
   }

   private static boolean hasHorizontalConnections(BlockState state) {
      return state.m_60734_() instanceof WallBlock
         || state.m_60734_() instanceof FenceBlock
         || state.m_60734_() instanceof IronBarsBlock
         || state.m_60734_() instanceof BenchStructureDecorBlock
         || state.m_61138_(BlockStateProperties.f_61368_)
            && state.m_61138_(BlockStateProperties.f_61369_)
            && state.m_61138_(BlockStateProperties.f_61370_)
            && state.m_61138_(BlockStateProperties.f_61371_);
   }

   private static Rotation rotationFor(Direction facing) {
      return switch (facing) {
         case EAST -> Rotation.CLOCKWISE_90;
         case SOUTH -> Rotation.CLOCKWISE_180;
         case WEST -> Rotation.COUNTERCLOCKWISE_90;
         default -> Rotation.NONE;
      };
   }

   private static record AwsTemplate(
      int sizeX,
      int sizeY,
      int sizeZ,
      int offsetX,
      int offsetY,
      int offsetZ,
      int[][][] layers,
      Map<Integer, AwsTemplatePlacer.AwsTemplate.RuleState> rules,
      List<AwsTemplatePlacer.AwsTemplate.EntityRule> entities,
      boolean survival,
      boolean unique,
      String name,
      String path
   ) {
      static AwsTemplatePlacer.AwsTemplate load(String templatePath) {
         List<String> lines = readLines(templatePath);
         int[] size = new int[]{1, 1, 1};
         int[] offset = new int[]{0, 0, 0};

         for (String line : lines) {
            if (line.startsWith("size=")) {
               size = parseTriplet(line.substring("size=".length()));
            } else if (line.startsWith("offset=")) {
               offset = parseTriplet(line.substring("offset=".length()));
            }
         }

         int[][][] layers = new int[size[1]][size[2]][size[0]];
         parseLayers(lines, size, layers);
         Map<Integer, AwsTemplatePlacer.AwsTemplate.RuleState> rules = parseRules(lines);
         List<AwsTemplatePlacer.AwsTemplate.EntityRule> entities = parseEntities(lines);
         boolean survival = lines.stream().anyMatch(linex -> linex.trim().equalsIgnoreCase("survival=true"));
         boolean unique = lines.stream().anyMatch(linex -> linex.trim().equalsIgnoreCase("unique=true"));
         String name = lines.stream().filter(linex -> linex.startsWith("name=")).map(linex -> linex.substring(5).trim()).findFirst().orElse(templatePath);
         return new AwsTemplatePlacer.AwsTemplate(
            size[0], size[1], size[2], offset[0], offset[1], offset[2], layers, rules, entities, survival, unique, name, templatePath
         );
      }

      List<AwsTemplatePlacer.ResourceInfo> resources() {
         Map<ResourceLocation, Integer> counts = new HashMap<>();

         for (int[][] layer : this.layers) {
            for (int[] row : layer) {
               for (int rule : row) {
                  AwsTemplatePlacer.AwsTemplate.RuleState ruleState = this.rules.get(rule);
                  if (ruleState != null && !ruleState.state().m_60795_() && !ruleState.isInvisibleProxy()) {
                     Item item = ruleState.state().m_60734_().m_5456_();
                     if (item != Items.f_41852_) {
                        counts.merge(BuiltInRegistries.f_257033_.m_7981_(item), 1, Integer::sum);
                     }
                  }
               }
            }
         }

         return counts.entrySet()
            .stream()
            .sorted(Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
            .map(entry -> new AwsTemplatePlacer.ResourceInfo(entry.getKey(), entry.getValue()))
            .toList();
      }

      AwsTemplatePlacer.PreviewData preview(BlockPos origin, Direction facing) {
         BlockPos min = this.structureMin(origin, facing);
         int turns = (facing.m_122416_() + 2) % 4;
         Rotation rotation = AwsTemplatePlacer.rotationFor(facing);
         List<AwsTemplatePlacer.PreviewBlock> blocks = new ArrayList<>();

         for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
               for (int x = 0; x < this.sizeX; x++) {
                  AwsTemplatePlacer.AwsTemplate.RuleState rule = this.rules.get(this.layers[y][z][x]);
                  if (rule != null && !rule.state().m_60795_() && !rule.isInvisibleProxy()) {
                     BlockPos target = this.rotateInArea(new BlockPos(x, y, z), turns).m_121955_(min);
                     blocks.add(new AwsTemplatePlacer.PreviewBlock(target, rule.state().m_60717_(rotation)));
                  }
               }
            }
         }

         return new AwsTemplatePlacer.PreviewData(min, this.structureMax(min, facing), List.copyOf(blocks));
      }

      void place(Level level, BlockPos origin, Direction facing, boolean fillUnconfiguredContainers) {
         this.placeAtMin(level, this.structureMin(origin, facing), facing, true, fillUnconfiguredContainers);
      }

      private AwsTemplatePlacer.IncrementalStep clearIncremental(
         ServerLevel level, BlockPos origin, Direction facing, int cursor, int budget, BlockPos builderPos
      ) {
         BlockPos min = this.structureMin(origin, facing);
         int turns = (facing.m_122416_() + 2) % 4;
         int volume = this.sizeX * this.sizeY * this.sizeZ;
         int next = Math.max(0, cursor);
         int used = 0;
         int inspected = 0;
         int inspectionLimit = Math.max(256, Math.max(0, budget) * 16);

         while (next < volume && used < Math.max(0, budget) && inspected++ < inspectionLimit) {
            BlockPos local = this.localFromIndex(next++);
            AwsTemplatePlacer.AwsTemplate.RuleState templateRule = this.rules.get(this.layers[local.m_123342_()][local.m_123343_()][local.m_123341_()]);
            if (templateRule != null && !templateRule.state().m_60795_() && !templateRule.isInvisibleProxy()) {
               BlockPos target = this.rotateInArea(local, turns).m_121955_(min);
               BlockState existing = level.m_8055_(target);
               if (!target.equals(builderPos)
                  && level.m_7702_(target) == null
                  && AwsTemplatePlacer.isBasicNaturalTerrain(existing)
                  && level.m_7731_(target, Blocks.f_50016_.m_49966_(), 18)) {
                  used++;
               }
            }
         }

         return new AwsTemplatePlacer.IncrementalStep(next, used);
      }

      private AwsTemplatePlacer.IncrementalStep placeIncremental(
         ServerLevel level, BlockPos origin, Direction facing, int cursor, int budget, BlockPos builderPos
      ) {
         BlockPos min = this.structureMin(origin, facing);
         int turns = (facing.m_122416_() + 2) % 4;
         Rotation rotation = AwsTemplatePlacer.rotationFor(facing);
         int volume = this.sizeX * this.sizeY * this.sizeZ;
         int next = Math.max(0, cursor);
         int used = 0;

         while (next < volume && used < Math.max(0, budget)) {
            BlockPos local = this.localFromIndex(next++);
            AwsTemplatePlacer.AwsTemplate.RuleState ruleState = this.rules.get(this.layers[local.m_123342_()][local.m_123343_()][local.m_123341_()]);
            if (ruleState != null && !ruleState.state().m_60795_() && !ruleState.isInvisibleProxy()) {
               BlockPos target = this.rotateInArea(local, turns).m_121955_(min);
               if (!target.equals(builderPos)) {
                  BlockState intended = ruleState.state().m_60717_(rotation);
                  BlockState existing = level.m_8055_(target);
                  if (!existing.equals(intended)
                     && (existing.m_60795_() || level.m_7702_(target) == null && AwsTemplatePlacer.isBasicNaturalTerrain(existing))
                     && level.m_7731_(target, intended, 18)) {
                     used++;
                  }
               }
            }
         }

         return new AwsTemplatePlacer.IncrementalStep(next, used);
      }

      private void finishIncremental(ServerLevel level, BlockPos origin, Direction facing) {
         BlockPos min = this.structureMin(origin, facing);
         int turns = (facing.m_122416_() + 2) % 4;
         Rotation rotation = AwsTemplatePlacer.rotationFor(facing);
         List<AwsTemplatePlacer.AwsTemplate.PlacedRule> placedRules = new ArrayList<>();
         level.m_7471_(origin, false);

         for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
               for (int x = 0; x < this.sizeX; x++) {
                  AwsTemplatePlacer.AwsTemplate.RuleState ruleState = this.rules.get(this.layers[y][z][x]);
                  if (ruleState != null && !ruleState.state().m_60795_() && !ruleState.isInvisibleProxy()) {
                     BlockPos target = this.rotateInArea(new BlockPos(x, y, z), turns).m_121955_(min);
                     if (target.equals(origin)) {
                        level.m_7731_(target, ruleState.state().m_60717_(rotation), 18);
                     }

                     placedRules.add(new AwsTemplatePlacer.AwsTemplate.PlacedRule(target, ruleState));
                  }
               }
            }
         }

         refreshHorizontalConnections(level, placedRules);
         normalizeDoors(level, placedRules);
         this.connectStructureChests(level, min, turns);

         for (AwsTemplatePlacer.AwsTemplate.PlacedRule placed : placedRules) {
            BlockPos target = placed.pos();
            AwsTemplatePlacer.AwsTemplate.RuleState ruleState = placed.rule();
            if (level.m_7702_(target) instanceof ProtectionFlagBlockEntity flag) {
               flag.loadFromOldTemplateData(ruleState.data());
               flag.configureTerritory(min, this.structureMax(min, facing));
            } else if (level.m_7702_(target) instanceof AdvancedSpawnerBlockEntity spawner) {
               spawner.loadFromOldTemplateData(ruleState.data());
            } else {
               this.configureStructureLoot(level, target, ruleState.data(), false);
            }
         }

         for (AwsTemplatePlacer.AwsTemplate.EntityRule entity : this.entities) {
            entity.place(level, min, turns, this.sizeX, this.sizeZ);
         }
      }

      private BlockPos localFromIndex(int index) {
         int layerSize = this.sizeX * this.sizeZ;
         int y = index / layerSize;
         int withinLayer = index % layerSize;
         int z = withinLayer / this.sizeX;
         int x = withinLayer % this.sizeX;
         return new BlockPos(x, y, z);
      }

      void placeAtMin(Level level, BlockPos min, Direction facing, boolean clearAir, boolean fillUnconfiguredContainers) {
         this.placeAtMin(level, min, facing, clearAir, Set.of(), false, fillUnconfiguredContainers);
      }

      void placeAtMinPreserving(Level level, BlockPos min, Direction facing, Set<Long> protectedBlocks, boolean fillUnconfiguredContainers) {
         this.placeAtMin(level, min, facing, true, protectedBlocks, true, fillUnconfiguredContainers);
      }

      private void placeAtMin(
         Level level, BlockPos min, Direction facing, boolean clearAir, Set<Long> protectedBlocks, boolean templateAirOnly, boolean fillUnconfiguredContainers
      ) {
         int turns = (facing.m_122416_() + 2) % 4;
         Rotation rotation = AwsTemplatePlacer.rotationFor(facing);
         List<AwsTemplatePlacer.AwsTemplate.PlacedRule> placedRules = new ArrayList<>();
         if (clearAir) {
            this.clearTemplateVolume(level, min, turns, protectedBlocks, templateAirOnly);
         }

         for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
               for (int x = 0; x < this.sizeX; x++) {
                  int rule = this.layers[y][z][x];
                  if (rule != 0) {
                     AwsTemplatePlacer.AwsTemplate.RuleState ruleState = this.rules.get(rule);
                     if (ruleState != null && !ruleState.state().m_60795_() && !ruleState.isInvisibleProxy()) {
                        BlockState state = ruleState.state();
                        BlockPos local = new BlockPos(x, y, z);
                        BlockPos target = this.rotateInArea(local, turns).m_121955_(min);
                        level.m_7731_(target, state.m_60717_(rotation), 18);
                        placedRules.add(new AwsTemplatePlacer.AwsTemplate.PlacedRule(target, ruleState));
                     }
                  }
               }
            }
         }

         refreshHorizontalConnections(level, placedRules);
         normalizeDoors(level, placedRules);
         this.connectStructureChests(level, min, turns);

         for (AwsTemplatePlacer.AwsTemplate.PlacedRule placed : placedRules) {
            BlockPos target = placed.pos();
            AwsTemplatePlacer.AwsTemplate.RuleState ruleState = placed.rule();
            if (level.m_7702_(target) instanceof ProtectionFlagBlockEntity flag) {
               flag.loadFromOldTemplateData(ruleState.data());
               flag.configureTerritory(min, this.structureMax(min, facing));
            } else if (level.m_7702_(target) instanceof AdvancedSpawnerBlockEntity spawner) {
               spawner.loadFromOldTemplateData(ruleState.data());
            } else {
               this.configureStructureLoot(level, target, ruleState.data(), fillUnconfiguredContainers);
            }
         }

         if (!this.entities.isEmpty() && level instanceof ServerLevel serverLevel) {
            List<AwsTemplatePlacer.AwsTemplate.EntityRule> pendingEntities = List.copyOf(this.entities);
            serverLevel.m_7654_().execute(() -> {
               AncientWarfareCore.LOG.info("Placing {} AWS entity rules", pendingEntities.size());

               for (AwsTemplatePlacer.AwsTemplate.EntityRule entity : pendingEntities) {
                  entity.place(serverLevel, min, turns, this.sizeX, this.sizeZ);
               }
            });
         }
      }

      private void placeChunk(ServerLevel level, BlockPos min, Direction facing, ChunkPos chunk) {
         int turns = (facing.m_122416_() + 2) % 4;
         Rotation rotation = AwsTemplatePlacer.rotationFor(facing);
         List<AwsTemplatePlacer.AwsTemplate.PlacedRule> placedRules = new ArrayList<>();
         int chunkMinX = chunk.m_45604_();
         int chunkMaxX = chunk.m_45608_();
         int chunkMinZ = chunk.m_45605_();
         int chunkMaxZ = chunk.m_45609_();

         for (int z = 0; z < this.sizeZ; z++) {
            for (int x = 0; x < this.sizeX; x++) {
               BlockPos column = this.rotateInArea(new BlockPos(x, 0, z), turns).m_121955_(min);
               if (column.m_123341_() >= chunkMinX && column.m_123341_() <= chunkMaxX && column.m_123343_() >= chunkMinZ && column.m_123343_() <= chunkMaxZ) {
                  for (int y = 0; y < this.sizeY; y++) {
                     int rule = this.layers[y][z][x];
                     if (rule != 0) {
                        AwsTemplatePlacer.AwsTemplate.RuleState ruleState = this.rules.get(rule);
                        if (ruleState != null && !ruleState.state().m_60795_() && !ruleState.isInvisibleProxy()) {
                           BlockPos target = this.rotateInArea(new BlockPos(x, y, z), turns).m_121955_(min);
                           level.m_7731_(target, ruleState.state().m_60717_(rotation), 18);
                           placedRules.add(new AwsTemplatePlacer.AwsTemplate.PlacedRule(target, ruleState));
                        }
                     }
                  }
               }
            }
         }

         refreshHorizontalConnections(level, placedRules);
         normalizeDoors(level, placedRules);
      }

      private void finishPhasedPlacement(ServerLevel level, BlockPos min, Direction facing) {
         int turns = (facing.m_122416_() + 2) % 4;

         for (AwsTemplatePlacer.AwsTemplate.EntityRule entity : this.entities) {
            entity.place(level, min, turns, this.sizeX, this.sizeZ);
         }
      }

      private void connectChunk(ServerLevel level, BlockPos min, Direction facing, ChunkPos chunk) {
         int turns = (facing.m_122416_() + 2) % 4;
         int chunkMinX = chunk.m_45604_();
         int chunkMaxX = chunk.m_45608_();
         int chunkMinZ = chunk.m_45605_();
         int chunkMaxZ = chunk.m_45609_();
         Set<Long> refresh = new HashSet<>();

         for (int z = 0; z < this.sizeZ; z++) {
            for (int x = 0; x < this.sizeX; x++) {
               BlockPos column = this.rotateInArea(new BlockPos(x, 0, z), turns).m_121955_(min);
               if (column.m_123341_() >= chunkMinX && column.m_123341_() <= chunkMaxX && column.m_123343_() >= chunkMinZ && column.m_123343_() <= chunkMaxZ) {
                  for (int y = 0; y < this.sizeY; y++) {
                     BlockPos target = this.rotateInArea(new BlockPos(x, y, z), turns).m_121955_(min);
                     refresh.add(target.m_121878_());
                     BlockState state = level.m_8055_(target);
                     if (state.m_60734_() instanceof ChestBlock && state.m_61143_(ChestBlock.f_51479_) == ChestType.SINGLE) {
                        Direction partnerDirection = ((Direction)state.m_61143_(ChestBlock.f_51478_)).m_122427_();
                        BlockPos partnerPos = target.m_121945_(partnerDirection);
                        BlockState partner = level.m_8055_(partnerPos);
                        if (partner.m_60713_(state.m_60734_())
                           && partner.m_61143_(ChestBlock.f_51479_) == ChestType.SINGLE
                           && partner.m_61143_(ChestBlock.f_51478_) == state.m_61143_(ChestBlock.f_51478_)) {
                           level.m_7731_(target, (BlockState)state.m_61124_(ChestBlock.f_51479_, ChestType.LEFT), 2);
                           level.m_7731_(partnerPos, (BlockState)partner.m_61124_(ChestBlock.f_51479_, ChestType.RIGHT), 2);
                        }
                     }
                  }
               }
            }
         }

         AwsTemplatePlacer.refreshHorizontalConnections(level, refresh);
      }

      private void configureChunk(ServerLevel level, BlockPos min, Direction facing, ChunkPos chunk, boolean fillUnconfiguredContainers) {
         int turns = (facing.m_122416_() + 2) % 4;
         int chunkMinX = chunk.m_45604_();
         int chunkMaxX = chunk.m_45608_();
         int chunkMinZ = chunk.m_45605_();
         int chunkMaxZ = chunk.m_45609_();

         for (int z = 0; z < this.sizeZ; z++) {
            for (int x = 0; x < this.sizeX; x++) {
               BlockPos column = this.rotateInArea(new BlockPos(x, 0, z), turns).m_121955_(min);
               if (column.m_123341_() >= chunkMinX && column.m_123341_() <= chunkMaxX && column.m_123343_() >= chunkMinZ && column.m_123343_() <= chunkMaxZ) {
                  for (int y = 0; y < this.sizeY; y++) {
                     AwsTemplatePlacer.AwsTemplate.RuleState ruleState = this.rules.get(this.layers[y][z][x]);
                     if (ruleState != null && !ruleState.state().m_60795_() && !ruleState.isInvisibleProxy()) {
                        BlockPos target = this.rotateInArea(new BlockPos(x, y, z), turns).m_121955_(min);
                        if (level.m_7702_(target) instanceof ProtectionFlagBlockEntity flag) {
                           flag.loadFromOldTemplateData(ruleState.data());
                           flag.configureTerritory(min, this.structureMax(min, facing));
                        } else if (level.m_7702_(target) instanceof AdvancedSpawnerBlockEntity spawner) {
                           spawner.loadFromOldTemplateData(ruleState.data());
                        } else {
                           this.configureStructureLoot(level, target, ruleState.data(), fillUnconfiguredContainers);
                        }
                     }
                  }
               }
            }
         }
      }

      private BlockPos structureMax(BlockPos min, Direction facing) {
         return min.m_7918_(this.rotatedSizeX(facing) - 1, this.sizeY - 1, this.rotatedSizeZ(facing) - 1);
      }

      private int rotatedSizeX(Direction facing) {
         return facing.m_122434_() == Axis.X ? this.sizeZ : this.sizeX;
      }

      private int rotatedSizeZ(Direction facing) {
         return facing.m_122434_() == Axis.X ? this.sizeX : this.sizeZ;
      }

      private static void refreshHorizontalConnections(Level level, List<AwsTemplatePlacer.AwsTemplate.PlacedRule> placedRules) {
         Set<Long> refreshPositions = new HashSet<>();

         for (AwsTemplatePlacer.AwsTemplate.PlacedRule placed : placedRules) {
            BlockPos pos = placed.pos();
            refreshPositions.add(pos.m_121878_());
         }

         AwsTemplatePlacer.refreshHorizontalConnections(level, refreshPositions);
      }

      private static void normalizeDoors(Level level, List<AwsTemplatePlacer.AwsTemplate.PlacedRule> placedRules) {
         Set<BlockPos> lowerDoors = new HashSet<>();

         for (AwsTemplatePlacer.AwsTemplate.PlacedRule placed : placedRules) {
            BlockState state = level.m_8055_(placed.pos());
            if (state.m_60734_() instanceof DoorBlock && state.m_61138_(DoorBlock.f_52730_)) {
               lowerDoors.add(state.m_61143_(DoorBlock.f_52730_) == DoubleBlockHalf.LOWER ? placed.pos() : placed.pos().m_7495_());
            }
         }

         for (BlockPos lowerPos : lowerDoors) {
            BlockPos upperPos = lowerPos.m_7494_();
            BlockState lower = level.m_8055_(lowerPos);
            BlockState upper = level.m_8055_(upperPos);
            if (lower.m_60734_() instanceof DoorBlock
               && upper.m_60713_(lower.m_60734_())
               && lower.m_61143_(DoorBlock.f_52730_) == DoubleBlockHalf.LOWER
               && upper.m_61143_(DoorBlock.f_52730_) == DoubleBlockHalf.UPPER) {
               BlockState normalizedLower = (BlockState)lower.m_61124_(DoorBlock.f_52728_, (DoorHingeSide)upper.m_61143_(DoorBlock.f_52728_));
               BlockState normalizedUpper = (BlockState)((BlockState)((BlockState)upper.m_61124_(
                        DoorBlock.f_52726_, (Direction)lower.m_61143_(DoorBlock.f_52726_)
                     ))
                     .m_61124_(DoorBlock.f_52727_, (Boolean)lower.m_61143_(DoorBlock.f_52727_)))
                  .m_61124_(DoorBlock.f_52729_, (Boolean)lower.m_61143_(DoorBlock.f_52729_));
               level.m_7731_(lowerPos, normalizedLower, 18);
               level.m_7731_(upperPos, normalizedUpper, 18);
            }
         }
      }

      private Set<Long> occupiedBlocks(BlockPos min, Direction facing) {
         int turns = (facing.m_122416_() + 2) % 4;
         Set<Long> occupied = new HashSet<>();

         for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
               for (int x = 0; x < this.sizeX; x++) {
                  if (this.isSolidRule(this.layers[y][z][x])) {
                     occupied.add(this.rotateInArea(new BlockPos(x, y, z), turns).m_121955_(min).m_121878_());
                  }
               }
            }
         }

         return occupied;
      }

      private void connectStructureChests(Level level, BlockPos min, int turns) {
         Set<Long> templateChestPositions = new HashSet<>();

         for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
               for (int x = 0; x < this.sizeX; x++) {
                  BlockPos pos = this.rotateInArea(new BlockPos(x, y, z), turns).m_121955_(min);
                  if (level.m_8055_(pos).m_60734_() instanceof ChestBlock) {
                     templateChestPositions.add(pos.m_121878_());
                  }
               }
            }
         }

         for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
               for (int xx = 0; xx < this.sizeX; xx++) {
                  BlockPos pos = this.rotateInArea(new BlockPos(xx, y, z), turns).m_121955_(min);
                  BlockState state = level.m_8055_(pos);
                  if (state.m_60734_() instanceof ChestBlock && state.m_61143_(ChestBlock.f_51479_) == ChestType.SINGLE) {
                     Direction partnerDirection = ((Direction)state.m_61143_(ChestBlock.f_51478_)).m_122427_();
                     BlockPos partnerPos = pos.m_121945_(partnerDirection);
                     if (templateChestPositions.contains(partnerPos.m_121878_())) {
                        BlockState partner = level.m_8055_(partnerPos);
                        if (partner.m_60713_(state.m_60734_())
                           && partner.m_61143_(ChestBlock.f_51479_) == ChestType.SINGLE
                           && partner.m_61143_(ChestBlock.f_51478_) == state.m_61143_(ChestBlock.f_51478_)) {
                           level.m_7731_(pos, (BlockState)state.m_61124_(ChestBlock.f_51479_, ChestType.LEFT), 2);
                           level.m_7731_(partnerPos, (BlockState)partner.m_61124_(ChestBlock.f_51479_, ChestType.RIGHT), 2);
                        }
                     }
                  }
               }
            }
         }
      }

      private void configureStructureLoot(Level level, BlockPos target, String data, boolean fillUnconfiguredContainers) {
         AwsTemplatePlacer.AwsTemplate.LootDefinition loot = AwsTemplatePlacer.AwsTemplate.LootDefinition.fromOldTemplate(data);
         boolean fixedRewardContents = data.contains("Items:[{");
         CompoundTag legacyLootSettings = parseLegacyLootSettings(data);
         if (level.m_7702_(target) instanceof ChestBlockEntity chest) {
            if (!legacyLootSettings.m_128456_()) {
               AdvancedLootChestBlock.storeLegacyLootSettings(chest, legacyLootSettings);
            }

            if (loot != null) {
               AdvancedLootChestBlock.configureLoot(chest, loot.table(), loot.rolls(), level.f_46441_.m_188505_());
               AncientWarfareCore.LOG
                  .debug(
                     "AWS loot configured: template={}, pos={}, targetBlock={}, blockEntity={}, table={}, rolls={}, fixedItems={}",
                     new Object[]{
                        this.name, target, level.m_8055_(target).m_60734_(), chest.getClass().getSimpleName(), loot.table(), loot.rolls(), fixedRewardContents
                     }
                  );
            } else if (fixedRewardContents) {
               restoreFixedContents(chest, data);
               AncientWarfareCore.LOG
                  .debug(
                     "AWS fixed container restored: template={}, pos={}, targetBlock={}, blockEntity={}",
                     new Object[]{this.name, target, level.m_8055_(target).m_60734_(), chest.getClass().getSimpleName()}
                  );
            }

            if (loot == null
               && fillUnconfiguredContainers
               && AWCorePlatform.gameplayConfig().fillUnconfiguredStructureContainers()
               && isEmpty(chest)
               && !AdvancedLootChestBlock.hasConfiguredLoot(chest)) {
               StructureEmptyContainerLootResolver.Match inferred = StructureEmptyContainerLootResolver.resolve(level, target, this.path, this.name);
               AdvancedLootChestBlock.configureBasicLoot(chest, inferred.table(), level.f_46441_.m_188505_());
               loot = new AwsTemplatePlacer.AwsTemplate.LootDefinition(inferred.table(), 1);
               AncientWarfareCore.LOG
                  .debug(
                     "AWS empty chest assigned inferred loot: template={}, pos={}, table={}, reason={}",
                     new Object[]{this.name, target, inferred.table(), inferred.reason()}
                  );
            }

            if (loot != null || fixedRewardContents) {
               IceAndFireStructureLootCompat.configure(
                  chest, IceAndFireStructureLootCompat.settingsFor(this.path, this.name, this.unique, data), level.f_46441_.m_188505_()
               );
               SlashBladeStructureLootCompat.configure(chest, level.f_46441_.m_188505_());
            }
         } else if (level.m_7702_(target) instanceof StructureContainerBlockEntity container) {
            if (!legacyLootSettings.m_128456_()) {
               container.storeLegacyLootSettings(legacyLootSettings);
            }

            if (loot != null) {
               container.configureStructureLoot(loot.table(), loot.rolls(), level.f_46441_.m_188505_());
               AncientWarfareCore.LOG
                  .debug(
                     "AWS structure container loot configured: template={}, pos={}, block={}, container={}, table={}, rolls={}, fixedItems={}",
                     new Object[]{
                        this.name,
                        target,
                        level.m_8055_(target).m_60734_(),
                        container.getClass().getSimpleName(),
                        loot.table(),
                        loot.rolls(),
                        fixedRewardContents
                     }
                  );
            } else if (fixedRewardContents) {
               restoreFixedContents(container, data);
               AncientWarfareCore.LOG
                  .debug(
                     "AWS fixed structure container restored: template={}, pos={}, block={}, container={}",
                     new Object[]{this.name, target, level.m_8055_(target).m_60734_(), container.getClass().getSimpleName()}
                  );
            }

            if (loot == null && fillUnconfiguredContainers && AWCorePlatform.gameplayConfig().fillUnconfiguredStructureContainers() && isEmpty(container)) {
               StructureEmptyContainerLootResolver.Match inferred = StructureEmptyContainerLootResolver.resolve(level, target, this.path, this.name);
               container.configureStructureLoot(inferred.table(), 1, level.f_46441_.m_188505_());
               loot = new AwsTemplatePlacer.AwsTemplate.LootDefinition(inferred.table(), 1);
               AncientWarfareCore.LOG
                  .debug(
                     "AWS empty structure container assigned inferred loot: template={}, pos={}, table={}, reason={}",
                     new Object[]{this.name, target, inferred.table(), inferred.reason()}
                  );
            }

            if (loot != null || fixedRewardContents) {
               container.configureIceAndFireLoot(IceAndFireStructureLootCompat.settingsFor(this.path, this.name, this.unique, data), level.f_46441_.m_188505_());
               container.configureSlashBladeLoot(level.f_46441_.m_188505_());
            }
         } else if (data.contains("lootSettings:") || data.contains("LootTable:\"")) {
            AncientWarfareCore.LOG
               .warn(
                  "AWS loot metadata has no compatible container block entity: template={}, pos={}, block={}, dataPrefix={}",
                  new Object[]{this.name, target, level.m_8055_(target).m_60734_(), data.substring(0, Math.min(180, data.length()))}
               );
         }
      }

      private static boolean isEmpty(Container container) {
         for (int slot = 0; slot < container.m_6643_(); slot++) {
            if (!container.m_8020_(slot).m_41619_()) {
               return false;
            }
         }

         return true;
      }

      private static CompoundTag parseLegacyLootSettings(String data) {
         String settings = extractBalanced(data, "lootSettings:", '{', '}');
         if (settings.isBlank()) {
            return new CompoundTag();
         } else {
            try {
               return TagParser.m_129359_(
                  settings.replace("ancientwarfarenpc:", "ancientwarfare:")
                     .replace("ancientwarfarestructure:", "ancientwarfare:")
                     .replace("ancientwarfarevehicle:", "ancientwarfare:")
                     .replace("ancientwarfareautomation:", "ancientwarfare:")
               );
            } catch (Exception var3) {
               AncientWarfareCore.LOG.warn("Unable to preserve legacy lootSettings payload", var3);
               return new CompoundTag();
            }
         }
      }

      private static void restoreFixedContents(Container container, String data) {
         String teData = extractBalanced(data, "teData:", '{', '}');
         if (!teData.isBlank() && teData.contains("Items:[")) {
            try {
               String normalized = teData.replace("ancientwarfarenpc:", "ancientwarfare:")
                  .replace("ancientwarfarestructure:", "ancientwarfare:")
                  .replace("ancientwarfarevehicle:", "ancientwarfare:")
                  .replace("ancientwarfareautomation:", "ancientwarfare:");
               CompoundTag tag = TagParser.m_129359_(normalized);
               ListTag items = tag.m_128437_("Items", 10);
               container.m_6211_();

               for (int index = 0; index < items.size(); index++) {
                  CompoundTag itemTag = items.m_128728_(index);
                  ItemStack stack = ItemStack.m_41712_(itemTag);
                  if (!stack.m_41619_()) {
                     insertFixedStack(container, stack, Math.max(1, stack.m_41613_()));
                  }
               }

               if (container instanceof BlockEntity blockEntity) {
                  blockEntity.m_6596_();
               }
            } catch (Exception var9) {
               AncientWarfareCore.LOG.warn("Unable to restore fixed AWS container contents", var9);
            }
         }
      }

      private static void insertFixedStack(Container container, ItemStack source, int count) {
         int remaining = count;

         for (int slot = 0; slot < container.m_6643_() && remaining > 0; slot++) {
            ItemStack existing = container.m_8020_(slot);
            if (!existing.m_41619_() && ItemStack.m_150942_(existing, source) && existing.m_41613_() < existing.m_41741_()) {
               int moved = Math.min(remaining, existing.m_41741_() - existing.m_41613_());
               existing.m_41769_(moved);
               remaining -= moved;
            }
         }

         for (int slotx = 0; slotx < container.m_6643_() && remaining > 0; slotx++) {
            if (container.m_8020_(slotx).m_41619_()) {
               ItemStack split = source.m_41777_();
               split.m_41764_(Math.min(remaining, split.m_41741_()));
               container.m_6836_(slotx, split);
               remaining -= split.m_41613_();
            }
         }
      }

      private void clearTemplateVolume(Level level, BlockPos min, int turns, Set<Long> protectedBlocks, boolean templateAirOnly) {
         BlockState air = Blocks.f_50016_.m_49966_();

         for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
               for (int x = 0; x < this.sizeX; x++) {
                  if (!templateAirOnly || !this.isSolidRule(this.layers[y][z][x])) {
                     BlockPos target = this.rotateInArea(new BlockPos(x, y, z), turns).m_121955_(min);
                     if (!protectedBlocks.contains(target.m_121878_()) && !level.m_8055_(target).m_60795_()) {
                        level.m_7731_(target, air, 18);
                     }
                  }
               }
            }
         }
      }

      private boolean isSolidRule(int rule) {
         if (rule == 0) {
            return false;
         } else {
            AwsTemplatePlacer.AwsTemplate.RuleState ruleState = this.rules.get(rule);
            return ruleState != null && !ruleState.state().m_60795_() && !ruleState.isInvisibleProxy();
         }
      }

      private static List<AwsTemplatePlacer.AwsTemplate.EntityRule> parseEntities(List<String> lines) {
         List<AwsTemplatePlacer.AwsTemplate.EntityRule> entities = new ArrayList<>();

         for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals("entity:")) {
               String plugin = "";
               StringBuilder data = new StringBuilder();
               boolean inData = false;
               i++;

               for (; i < lines.size() && !lines.get(i).equals(":endentity"); i++) {
                  String line = lines.get(i).trim();
                  if (line.startsWith("plugin=")) {
                     plugin = line.substring("plugin=".length());
                  } else if (line.equals("data:")) {
                     inData = true;
                  } else if (line.equals(":enddata")) {
                     inData = false;
                  } else if (inData) {
                     data.append(line);
                  }
               }

               if ("awGate".equals(plugin) || "AWVehicle".equals(plugin)) {
                  entities.add(new AwsTemplatePlacer.AwsTemplate.EntityRule(plugin, data.toString()));
               }
            }
         }

         return entities;
      }

      private BlockPos structureMin(BlockPos origin, Direction facing) {
         BlockPos c1 = origin.m_5484_(facing.m_122428_(), this.offsetX).m_5484_(facing, this.offsetZ).m_6630_(-this.offsetY);
         BlockPos c2 = c1.m_5484_(facing.m_122427_(), this.sizeX - 1).m_5484_(facing, -(this.sizeZ - 1)).m_6630_(this.sizeY - 1);
         return new BlockPos(Math.min(c1.m_123341_(), c2.m_123341_()), Math.min(c1.m_123342_(), c2.m_123342_()), Math.min(c1.m_123343_(), c2.m_123343_()));
      }

      private BlockPos rotateInArea(BlockPos pos, int turns) {
         int currentXSize = this.sizeX;
         int currentZSize = this.sizeZ;
         int nextXSize = currentXSize;
         int nextZSize = currentZSize;
         int x = pos.m_123341_();
         int z = pos.m_123343_();
         int nextX = x;
         int nextZ = z;
         int positiveTurns = turns > 0 ? turns : 4 + turns;

         for (int i = 0; i < positiveTurns; i++) {
            x = currentZSize - 1 - nextZ;
            z = nextX;
            nextX = x;
            nextZ = z;
            currentZSize = nextXSize;
            nextXSize = nextZSize;
            nextZSize = currentZSize;
         }

         return new BlockPos(x, pos.m_123342_(), z);
      }

      private static List<String> readLines(String templatePath) {
         String resource = "/assets/ancientwarfare/template/" + templatePath;
         InputStream templateStream = AwsTemplatePlacer.class.getResourceAsStream(resource);
         if (templateStream == null) {
            String classpathResource = resource.startsWith("/") ? resource.substring(1) : resource;
            ClassLoader coreLoader = AwsTemplatePlacer.class.getClassLoader();
            templateStream = coreLoader == null ? null : coreLoader.getResourceAsStream(classpathResource);
            if (templateStream == null) {
               ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
               if (contextLoader != coreLoader && contextLoader != null) {
                  templateStream = contextLoader.getResourceAsStream(classpathResource);
               }
            }

            if (templateStream == null) {
               templateStream = openForgeModResource(classpathResource);
            }

            if (templateStream == null) {
               ClassLoader[] loaders = new ClassLoader[]{coreLoader, Thread.currentThread().getContextClassLoader()};

               for (ClassLoader loader : loaders) {
                  if (loader != null) {
                     Enumeration<URL> urls;
                     try {
                        urls = loader.getResources(classpathResource);
                     } catch (IOException var18) {
                        continue;
                     }

                     while (urls.hasMoreElements() && templateStream == null) {
                        try {
                           templateStream = urls.nextElement().openStream();
                        } catch (IOException var15) {
                        }
                     }

                     if (templateStream != null) {
                        break;
                     }
                  }
               }
            }
         }

         try {
            InputStream stream = templateStream;

            List var22;
            try {
               if (stream == null) {
                  throw new IllegalArgumentException("Missing AWS template resource: " + resource);
               }

               try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                  var22 = reader.lines().toList();
               }
            } catch (Throwable var16) {
               if (templateStream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var12) {
                     var16.addSuppressed(var12);
                  }
               }

               throw var16;
            }

            if (templateStream != null) {
               templateStream.close();
            }

            return var22;
         } catch (IOException var17) {
            throw new IllegalArgumentException("Unable to read AWS template: " + templatePath, var17);
         }
      }

      private static InputStream openForgeModResource(String resource) {
         try {
            Class<?> modListType = Class.forName("net.minecraftforge.fml.ModList");
            Object modList = modListType.getMethod("get").invoke(null);
            Class<?> modFileType = Class.forName("net.minecraftforge.forgespi.locating.IModFile");
            Method findResource = modFileType.getMethod("findResource", String[].class);
            InputStream[] result = new InputStream[]{null};
            Consumer<Object> visitor = modFile -> {
               if (result[0] == null) {
                  try {
                     if (findResource.invoke(modFile, resource.split("/")) instanceof Path path && Files.isRegularFile(path)) {
                        result[0] = Files.newInputStream(path);
                     }
                  } catch (IOException | ReflectiveOperationException var6x) {
                  }
               }
            };
            modListType.getMethod("forEachModFile", Consumer.class).invoke(modList, visitor);
            return result[0];
         } catch (LinkageError | ReflectiveOperationException var7) {
            return null;
         }
      }

      private static int[] parseTriplet(String value) {
         String[] parts = value.split(",");
         return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())};
      }

      private static void parseLayers(List<String> lines, int[] size, int[][][] layers) {
         for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("layer:")) {
               List<Integer> ys = parseLayerRange(line.substring("layer:".length()).trim());
               List<String> layerLines = new ArrayList<>();
               i++;

               for (; i < lines.size() && !lines.get(i).equals(":endlayer"); i++) {
                  String dataLine = lines.get(i).trim();
                  if (!dataLine.isBlank()) {
                     layerLines.add(dataLine);
                  }
               }

               for (int y : ys) {
                  if (y >= 0 && y < size[1]) {
                     List<List<Integer>> rows = parseLayerRows(layerLines);

                     for (int z = 0; z < Math.min(rows.size(), size[2]); z++) {
                        List<Integer> row = rows.get(z);

                        for (int x = 0; x < Math.min(row.size(), size[0]); x++) {
                           layers[y][z][x] = row.get(x);
                        }
                     }
                  }
               }
            }
         }
      }

      private static List<Integer> parseLayerRange(String value) {
         List<Integer> result = new ArrayList<>();
         if (value.contains("-")) {
            String[] parts = value.split("-");
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());

            for (int y = start; y <= end; y++) {
               result.add(y);
            }
         } else {
            result.add(Integer.parseInt(value.trim()));
         }

         return result;
      }

      private static List<List<Integer>> parseLayerRows(List<String> layerLines) {
         List<List<Integer>> rows = new ArrayList<>();

         for (String line : layerLines) {
            String row = line.trim();
            if (!row.isBlank()) {
               int repeat = 1;
               int repeatIndex = row.indexOf(120);
               if (repeatIndex > 0) {
                  repeat = Integer.parseInt(row.substring(0, repeatIndex));
                  row = row.substring(repeatIndex + 1);
               }

               List<Integer> blocks = parseBlocks(row);

               for (int i = 0; i < repeat; i++) {
                  rows.add(blocks);
               }
            }
         }

         return rows;
      }

      private static List<Integer> parseBlocks(String row) {
         List<Integer> blocks = new ArrayList<>();

         for (String token : row.split(",")) {
            String blockPart = token.trim();
            if (!blockPart.isBlank()) {
               String[] blockDef = blockPart.split("\\|");
               int value = Integer.parseInt(blockDef[0]);
               int repeats = blockDef.length > 1 ? Integer.parseInt(blockDef[1]) : 1;

               for (int i = 0; i < repeats; i++) {
                  blocks.add(value);
               }
            }
         }

         return blocks;
      }

      private static Map<Integer, AwsTemplatePlacer.AwsTemplate.RuleState> parseRules(List<String> lines) {
         Map<Integer, AwsTemplatePlacer.AwsTemplate.RuleState> rules = new HashMap<>();

         for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals("rule:")) {
               String plugin = "";
               int number = 0;
               StringBuilder data = new StringBuilder();
               boolean inData = false;
               i++;

               for (; i < lines.size() && !lines.get(i).equals(":endrule"); i++) {
                  String line = lines.get(i).trim();
                  if (line.startsWith("plugin=")) {
                     plugin = line.substring("plugin=".length());
                  } else if (line.startsWith("number=")) {
                     number = Integer.parseInt(line.substring("number=".length()));
                  } else if (line.equals("data:")) {
                     inData = true;
                  } else if (line.equals(":enddata")) {
                     inData = false;
                  } else if (inData) {
                     data.append(line);
                  }
               }

               String ruleData = data.toString();
               BlockState state = parseRuleState(plugin, ruleData);
               if (number > 0 && state != null) {
                  rules.put(number, new AwsTemplatePlacer.AwsTemplate.RuleState(state, ruleData, plugin));
               }
            }
         }

         return rules;
      }

      private static BlockState parseRuleState(String plugin, String data) {
         if (!isBlockStatePlugin(plugin)) {
            return null;
         } else {
            String blockName = extract(data, "blockName:\"", "\"");
            if (blockName.isBlank()) {
               return null;
            } else {
               String properties = extract(data, "properties:{", "}");
               BlockState state = blockState(blockName, properties, data);
               if (state.m_60795_()) {
                  return state;
               } else {
                  if (!properties.isBlank()) {
                     state = applyProperties(state, properties);
                     state = applyLegacyFurnitureFacing(state, data);
                     state = applyLegacySlabProperties(state, properties);
                     state = applyLegacyMultipartProperties(state, properties);
                  }

                  if ("vanillaSkull".equals(plugin)) {
                     String rotation = extract(data, "skullRotation:", ",");
                     if (!rotation.isBlank() && state.m_61138_(SkullBlock.f_56314_)) {
                        state = (BlockState)state.m_61124_(SkullBlock.f_56314_, Integer.parseInt(rotation));
                     }
                  }

                  return state;
               }
            }
         }
      }

      private static BlockState applyLegacyFurnitureFacing(BlockState state, String data) {
         if ((state.m_60734_() instanceof StructureDecorBlock || state.m_60734_() instanceof TallStructureDecorBlock)
            && state.m_61138_(HorizontalDirectionalBlock.f_54117_)) {
            String facing = extract(data, "},facing:\"", "\"");

            try {
               Direction direction = Direction.m_122402_(facing);
               return direction != null && direction.m_122434_().m_122479_()
                  ? (BlockState)state.m_61124_(HorizontalDirectionalBlock.f_54117_, direction)
                  : state;
            } catch (IllegalArgumentException var4) {
               return state;
            }
         } else {
            return state;
         }
      }

      private static BlockState applyLegacySlabProperties(BlockState state, String properties) {
         if (state.m_60734_() instanceof SlabBlock && state.m_61138_(SlabBlock.f_56353_)) {
            String var2 = propertyValue(properties, "half");

            return switch (var2) {
               case "top" -> (BlockState)state.m_61124_(SlabBlock.f_56353_, SlabType.TOP);
               case "bottom" -> (BlockState)state.m_61124_(SlabBlock.f_56353_, SlabType.BOTTOM);
               default -> state;
            };
         } else {
            return state;
         }
      }

      private static BlockState applyLegacyMultipartProperties(BlockState state, String properties) {
         if (state.m_60734_() instanceof TallStructureDecorBlock && state.m_61138_(TallStructureDecorBlock.HALF)) {
            String part = propertyValue(properties, "part");
            String visible = propertyValue(properties, "visible");
            boolean upper = "top".equals(part) || "false".equals(visible);
            return (BlockState)state.m_61124_(TallStructureDecorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
         } else {
            return state;
         }
      }

      private static boolean isBlockStatePlugin(String plugin) {
         return switch (plugin) {
            case "vanillaBlocks", "vanillaSkull", "blockTile", "inventory", "doors", "bed", "vine", "vanillaSign", "vanillaFlowerPot", "banner", "fluid", "coffin", "flag" -> true;
            default -> false;
         };
      }

      private static BlockState blockState(String oldName, String properties, String data) {
         String name = remapBlockName(oldName, properties, data);
         ResourceLocation id = ResourceLocation.m_135820_(name);
         if (id != null && BuiltInRegistries.f_256975_.m_7804_(id)) {
            return ((Block)BuiltInRegistries.f_256975_.m_7745_(id)).m_49966_();
         } else {
            AncientWarfareCore.LOG.debug("Skipping unmapped AWS block {}", oldName);
            return Blocks.f_50016_.m_49966_();
         }
      }

      private static String remapBlockName(String name, String properties, String data) {
         if (name.startsWith("ancientwarfarestructure:")) {
            return remapAncientWarfareBlock(name);
         } else if (name.startsWith("iceandfire:")) {
            return LegacyIceAndFireStructureCompat.remapBlock(name);
         } else {
            return switch (name) {
               case "minecraft:chest", "minecraft:trapped_chest" -> "minecraft:chest";
               case "minecraft:brick_block" -> "minecraft:bricks";
               case "minecraft:nether_brick" -> "minecraft:nether_bricks";
               case "minecraft:noteblock" -> "minecraft:note_block";
               case "minecraft:lit_furnace" -> "minecraft:furnace";
               case "minecraft:reeds" -> "minecraft:sugar_cane";
               case "minecraft:snow_layer" -> "minecraft:snow";
               case "minecraft:sapling" -> legacySapling(properties);
               case "minecraft:wall_banner" -> legacyWallBanner(data);
               case "minecraft:grass" -> "minecraft:grass_block";
               case "minecraft:grass_path" -> "minecraft:dirt_path";
               case "minecraft:skull" -> "minecraft:skeleton_skull";
               case "minecraft:fence" -> "minecraft:oak_fence";
               case "minecraft:fence_gate" -> "minecraft:oak_fence_gate";
               case "minecraft:planks" -> "minecraft:oak_planks";
               case "minecraft:wooden_door" -> "minecraft:oak_door";
               case "minecraft:trapdoor" -> "minecraft:oak_trapdoor";
               case "minecraft:wooden_button" -> "minecraft:oak_button";
               case "minecraft:wooden_pressure_plate" -> "minecraft:oak_pressure_plate";
               case "minecraft:wall_sign" -> "minecraft:oak_wall_sign";
               case "minecraft:standing_sign" -> "minecraft:oak_sign";
               case "minecraft:bed" -> "minecraft:red_bed";
               case "minecraft:lit_pumpkin" -> "minecraft:jack_o_lantern";
               case "minecraft:lit_redstone_lamp" -> "minecraft:redstone_lamp";
               case "minecraft:web" -> "minecraft:cobweb";
               case "minecraft:waterlily" -> "minecraft:lily_pad";
               case "minecraft:deadbush" -> "minecraft:dead_bush";
               case "minecraft:torch" -> wallMounted(properties) ? "minecraft:wall_torch" : "minecraft:torch";
               case "minecraft:redstone_torch" -> wallMounted(properties) ? "minecraft:redstone_wall_torch" : "minecraft:redstone_torch";
               case "minecraft:stonebrick" -> stoneBrick(properties);
               case "minecraft:monster_egg" -> infestedStoneBrick(properties);
               case "minecraft:log" -> woodBlock(properties, false);
               case "minecraft:log2" -> woodBlock(properties, true);
               case "minecraft:leaves" -> leavesBlock(properties, false);
               case "minecraft:leaves2" -> leavesBlock(properties, true);
               case "minecraft:wooden_slab" -> woodenSlab(properties);
               case "minecraft:double_wooden_slab" -> woodenPlanks(properties);
               case "minecraft:stone_slab" -> stoneSlab(properties);
               case "minecraft:stone_slab2" -> redSandstoneSlab(properties);
               case "minecraft:double_stone_slab" -> doubleStoneSlab(properties);
               case "minecraft:double_stone_slab2" -> "minecraft:red_sandstone";
               case "minecraft:hardened_clay" -> "minecraft:terracotta";
               case "minecraft:stained_hardened_clay" -> coloredBlock(properties, "terracotta");
               case "minecraft:concrete" -> coloredBlock(properties, "concrete");
               case "minecraft:concrete_powder" -> coloredBlock(properties, "concrete_powder");
               case "minecraft:wool" -> coloredBlock(properties, "wool");
               case "minecraft:carpet" -> coloredBlock(properties, "carpet");
               case "minecraft:stained_glass" -> coloredBlock(properties, "stained_glass");
               case "minecraft:stained_glass_pane" -> coloredBlock(properties, "stained_glass_pane");
               case "minecraft:tallgrass" -> tallGrass(properties);
               case "minecraft:red_flower" -> redFlower(properties);
               case "minecraft:yellow_flower" -> "minecraft:dandelion";
               case "minecraft:double_plant" -> doublePlant(properties);
               default -> name;
            };
         }
      }

      private static String legacySapling(String properties) {
         String var1 = propertyValue(properties, "type");

         return switch (var1) {
            case "spruce" -> "minecraft:spruce_sapling";
            case "birch" -> "minecraft:birch_sapling";
            case "jungle" -> "minecraft:jungle_sapling";
            case "acacia" -> "minecraft:acacia_sapling";
            case "dark_oak" -> "minecraft:dark_oak_sapling";
            default -> "minecraft:oak_sapling";
         };
      }

      private static String legacyWallBanner(String data) {
         int base = parseLegacyInt(data, "Base", 15);

         String color = switch (base) {
            case 0 -> "black";
            case 1 -> "red";
            case 2 -> "green";
            case 3 -> "brown";
            case 4 -> "blue";
            case 5 -> "purple";
            case 6 -> "cyan";
            case 7 -> "light_gray";
            case 8 -> "gray";
            case 9 -> "pink";
            case 10 -> "lime";
            case 11 -> "yellow";
            case 12 -> "light_blue";
            case 13 -> "magenta";
            case 14 -> "orange";
            default -> "white";
         };
         return "minecraft:" + color + "_wall_banner";
      }

      private static int parseLegacyInt(String data, String key, int fallback) {
         String marker = key + ":";
         int start = data.indexOf(marker);
         if (start < 0) {
            return fallback;
         } else {
            start += marker.length();
            int end = start;

            while (end < data.length() && (data.charAt(end) == '-' || Character.isDigit(data.charAt(end)))) {
               end++;
            }

            try {
               return end > start ? Integer.parseInt(data.substring(start, end)) : fallback;
            } catch (NumberFormatException var7) {
               return fallback;
            }
         }
      }

      private static boolean wallMounted(String properties) {
         String facing = propertyValue(properties, "facing");
         return !facing.isBlank() && !"up".equals(facing);
      }

      private static String remapAncientWarfareBlock(String name) {
         String path = name.substring("ancientwarfarestructure:".length());

         return switch (path) {
            case "gravestone" -> "ancientwarfare:gravestone_1";
            case "urn" -> "ancientwarfare:urn";
            case "advanced_spawner" -> "ancientwarfare:advanced_spawner";
            case "protection_flag" -> "ancientwarfare:protection_flag";
            case "gate_proxy" -> "minecraft:air";
            case "advanced_loot_chest" -> "minecraft:chest";
            case "loot_basket" -> "ancientwarfare:loot_basket";
            case "wooden_coffin" -> "ancientwarfare:wooden_coffin";
            case "stone_coffin" -> "ancientwarfare:stone_coffin";
            default -> "ancientwarfare:" + path;
         };
      }

      private static String stoneBrick(String properties) {
         String var1 = propertyValue(properties, "variant");

         return switch (var1) {
            case "mossy_stonebrick" -> "minecraft:mossy_stone_bricks";
            case "cracked_stonebrick" -> "minecraft:cracked_stone_bricks";
            case "chiseled_stonebrick" -> "minecraft:chiseled_stone_bricks";
            default -> "minecraft:stone_bricks";
         };
      }

      private static String infestedStoneBrick(String properties) {
         String var1 = propertyValue(properties, "variant");

         return switch (var1) {
            case "mossy_brick" -> "minecraft:infested_mossy_stone_bricks";
            case "cracked_brick" -> "minecraft:infested_cracked_stone_bricks";
            case "chiseled_brick" -> "minecraft:infested_chiseled_stone_bricks";
            case "stone" -> "minecraft:infested_stone";
            case "cobblestone" -> "minecraft:infested_cobblestone";
            default -> "minecraft:infested_stone_bricks";
         };
      }

      private static String woodBlock(String properties, boolean secondSet) {
         String var2 = propertyValue(properties, "variant");

         return switch (var2) {
            case "spruce" -> "minecraft:spruce_log";
            case "birch" -> "minecraft:birch_log";
            case "jungle" -> "minecraft:jungle_log";
            case "acacia" -> "minecraft:acacia_log";
            case "dark_oak" -> "minecraft:dark_oak_log";
            default -> secondSet ? "minecraft:acacia_log" : "minecraft:oak_log";
         };
      }

      private static String leavesBlock(String properties, boolean secondSet) {
         String var2 = propertyValue(properties, "variant");

         return switch (var2) {
            case "spruce" -> "minecraft:spruce_leaves";
            case "birch" -> "minecraft:birch_leaves";
            case "jungle" -> "minecraft:jungle_leaves";
            case "acacia" -> "minecraft:acacia_leaves";
            case "dark_oak" -> "minecraft:dark_oak_leaves";
            default -> secondSet ? "minecraft:acacia_leaves" : "minecraft:oak_leaves";
         };
      }

      private static String woodenSlab(String properties) {
         return "minecraft:" + woodPrefix(properties) + "_slab";
      }

      private static String woodenPlanks(String properties) {
         return "minecraft:" + woodPrefix(properties) + "_planks";
      }

      private static String woodPrefix(String properties) {
         String var1 = propertyValue(properties, "variant");

         return switch (var1) {
            case "spruce" -> "spruce";
            case "birch" -> "birch";
            case "jungle" -> "jungle";
            case "acacia" -> "acacia";
            case "dark_oak" -> "dark_oak";
            default -> "oak";
         };
      }

      private static String stoneSlab(String properties) {
         String var1 = propertyValue(properties, "variant");

         return switch (var1) {
            case "cobblestone" -> "minecraft:cobblestone_slab";
            case "stone_brick" -> "minecraft:stone_brick_slab";
            case "brick" -> "minecraft:brick_slab";
            case "nether_brick" -> "minecraft:nether_brick_slab";
            case "quartz" -> "minecraft:quartz_slab";
            case "sandstone" -> "minecraft:sandstone_slab";
            case "wood_old" -> "minecraft:oak_slab";
            case "wood_new" -> "minecraft:acacia_slab";
            default -> "minecraft:smooth_stone_slab";
         };
      }

      private static String redSandstoneSlab(String properties) {
         return "minecraft:red_sandstone_slab";
      }

      private static String doubleStoneSlab(String properties) {
         String var1 = propertyValue(properties, "variant");

         return switch (var1) {
            case "cobblestone" -> "minecraft:cobblestone";
            case "stone_brick" -> "minecraft:stone_bricks";
            case "brick" -> "minecraft:bricks";
            case "nether_brick" -> "minecraft:nether_bricks";
            case "quartz" -> "minecraft:quartz_block";
            case "sandstone" -> "minecraft:sandstone";
            case "wood_old" -> "minecraft:oak_planks";
            case "wood_new" -> "minecraft:acacia_planks";
            default -> "minecraft:smooth_stone";
         };
      }

      private static String coloredBlock(String properties, String suffix) {
         String color = propertyValue(properties, "color");
         if ("silver".equals(color)) {
            color = "light_gray";
         }

         if (color.isBlank()) {
            color = "white";
         }

         return "minecraft:" + color + "_" + suffix;
      }

      private static String tallGrass(String properties) {
         String var1 = propertyValue(properties, "type");

         return switch (var1) {
            case "dead_bush" -> "minecraft:dead_bush";
            case "fern" -> "minecraft:fern";
            default -> "minecraft:grass";
         };
      }

      private static String redFlower(String properties) {
         String var1 = propertyValue(properties, "type");

         return switch (var1) {
            case "blue_orchid" -> "minecraft:blue_orchid";
            case "allium" -> "minecraft:allium";
            case "houstonia" -> "minecraft:azure_bluet";
            case "red_tulip" -> "minecraft:red_tulip";
            case "orange_tulip" -> "minecraft:orange_tulip";
            case "white_tulip" -> "minecraft:white_tulip";
            case "pink_tulip" -> "minecraft:pink_tulip";
            case "oxeye_daisy" -> "minecraft:oxeye_daisy";
            default -> "minecraft:poppy";
         };
      }

      private static String doublePlant(String properties) {
         String var1 = propertyValue(properties, "variant");

         return switch (var1) {
            case "syringa" -> "minecraft:lilac";
            case "double_grass" -> "minecraft:tall_grass";
            case "double_fern" -> "minecraft:large_fern";
            case "rose" -> "minecraft:rose_bush";
            case "paeonia" -> "minecraft:peony";
            default -> "minecraft:sunflower";
         };
      }

      private static BlockState applyProperties(BlockState state, String properties) {
         for (String entry : properties.split(",")) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
               String name = parts[0].replace("\"", "").trim().toLowerCase(Locale.ROOT);
               String value = parts[1].replace("\"", "").trim().toLowerCase(Locale.ROOT);
               if (!"legs".equals(name)
                  && !"variant".equals(name)
                  && !"type".equals(name)
                  && !"color".equals(name)
                  && !"decayable".equals(name)
                  && !"check_decay".equals(name)
                  && !"occupied".equals(name)) {
                  state = applyProperty(state, name, value);
               }
            }
         }

         return state;
      }

      private static String propertyValue(String properties, String name) {
         for (String entry : properties.split(",")) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2 && parts[0].replace("\"", "").trim().equals(name)) {
               return parts[1].replace("\"", "").trim().toLowerCase(Locale.ROOT);
            }
         }

         return "";
      }

      private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, String name, String value) {
         Property<T> property = property(state, name);
         return property == null ? state : property.m_6215_(value).map(v -> (BlockState)state.m_61124_(property, v)).orElse(state);
      }

      private static <T extends Comparable<T>> Property<T> property(BlockState state, String name) {
         for (Property<?> property : state.m_61147_()) {
            if (property.m_61708_().equals(name)) {
               return (Property<T>)property;
            }
         }

         return null;
      }

      private static String extract(String text, String start, String end) {
         int startIndex = text.indexOf(start);
         if (startIndex < 0) {
            return "";
         } else {
            startIndex += start.length();
            int endIndex = text.indexOf(end, startIndex);
            return endIndex < 0 ? text.substring(startIndex) : text.substring(startIndex, endIndex);
         }
      }

      private static String extractBalanced(String text, String marker, char open, char close) {
         int markerIndex = text.indexOf(marker);
         if (markerIndex < 0) {
            return "";
         } else {
            int start = text.indexOf(open, markerIndex + marker.length());
            if (start < 0) {
               return "";
            } else {
               int depth = 0;
               boolean quoted = false;
               boolean escaped = false;

               for (int index = start; index < text.length(); index++) {
                  char current = text.charAt(index);
                  if (quoted) {
                     if (escaped) {
                        escaped = false;
                     } else if (current == '\\') {
                        escaped = true;
                     } else if (current == '"') {
                        quoted = false;
                     }
                  } else if (current == '"') {
                     quoted = true;
                  } else if (current == open) {
                     depth++;
                  } else if (current == close) {
                     if (--depth == 0) {
                        return text.substring(start, index + 1);
                     }
                  }
               }

               return "";
            }
         }
      }

      private static record EntityRule(String plugin, String data) {
         void place(Level level, BlockPos min, int turns, int sizeX, int sizeZ) {
            BlockPos anchor = rotateAnchor(decodeLegacyBlockPos(parseLong(this.data, "position", 0L)), turns, sizeX, sizeZ).m_121955_(min);
            if ("awGate".equals(this.plugin)) {
               this.placeGate(level, anchor, turns);
            } else if ("AWVehicle".equals(this.plugin)) {
               this.placeVehicle(level, anchor, turns);
            }
         }

         private void placeGate(Level level, BlockPos anchor, int turns) {
            BlockPos first = rotateOffset(parseRelativePos(this.data, "pos1"), turns).m_121955_(anchor);
            BlockPos second = rotateOffset(parseRelativePos(this.data, "pos2"), turns).m_121955_(anchor);
            Direction facing = Direction.m_122376_(parseInt(this.data, "orientation", Direction.NORTH.ordinal()));

            for (int i = 0; i < turns; i++) {
               facing = facing.m_122427_();
            }

            GateEntity gate = (GateEntity)AWCoreRegistryAccess.entityType(AWCoreIds.DYNAMIC_GATE).m_20615_(level);
            if (gate != null) {
               String gateType = AwsTemplatePlacer.AwsTemplate.extract(this.data, "gateType:\"", "\"");
               gate.configure(first, second, facing);
               gate.setMaterial(gateType.toLowerCase(Locale.ROOT).contains("iron") ? GateEntity.Material.IRON : GateEntity.Material.WOOD);
               gate.setOpeningStyle(gateOpeningStyle(gateType));
               clearGateArea(level, first, second);
               level.m_7967_(gate);
               gate.ensureProxyBlocks();
               AncientWarfareCore.LOG.info("Placed AWS gate {} from {} to {}", new Object[]{gateType, first, second});
            }
         }

         private void placeVehicle(Level level, BlockPos anchor, int turns) {
            this.placeVehicle(level, anchor, turns, null);
         }

         private void placeVehicle(Level level, BlockPos anchor, int turns, UUID stableId) {
            int vehicleType = parseInt(this.data, "vehType", -1);
            EntityType<?> type = vehicleEntityType(vehicleType);
            if (type == null) {
               AncientWarfareCore.LOG.debug("Skipping unsupported AWS vehicle type {}", vehicleType);
            } else {
               Entity vehicle = type.m_20615_(level);
               if (vehicle != null) {
                  if (stableId != null) {
                     vehicle.m_20084_(stableId);
                  }

                  double xOffset = parseDouble(this.data, "xOffset", 0.5);
                  double yOffset = parseDouble(this.data, "yOffset", 0.0);
                  double zOffset = parseDouble(this.data, "zOffset", 0.5);
                  double rotatedX = xOffset;
                  double rotatedZ = zOffset;

                  for (int i = 0; i < turns; i++) {
                     double previousX = rotatedX;
                     rotatedX = 1.0 - rotatedZ;
                     rotatedZ = previousX;
                  }

                  float yaw = (float)(parseDouble(this.data, "rotation", 0.0) - 90.0 * (double)turns);
                  vehicle.m_7678_((double)anchor.m_123341_() + rotatedX, (double)anchor.m_123342_() + yOffset, (double)anchor.m_123343_() + rotatedZ, yaw, 0.0F);
                  if (level.m_7967_(vehicle)) {
                     AncientWarfareCore.LOG
                        .info(
                           "Placed AWS vehicle type {} as {} at {}", new Object[]{vehicleType, BuiltInRegistries.f_256780_.m_7981_(type), vehicle.m_20182_()}
                        );
                  } else {
                     if (stableId != null) {
                        throw new IllegalStateException("Failed to add AWS vehicle " + stableId);
                     }

                     AncientWarfareCore.LOG.warn("Failed to add AWS vehicle type {} at {}", vehicleType, vehicle.m_20182_());
                  }
               }
            }
         }

         private static GateEntity.OpeningStyle gateOpeningStyle(String gateType) {
            return switch (gateType) {
               case "gate.singleWood", "gate.singleIron" -> GateEntity.OpeningStyle.SINGLE_SLIDING;
               case "gate.doubleWood", "gate.doubleIron" -> GateEntity.OpeningStyle.DOUBLE_SLIDING;
               case "gate.drawbridge" -> GateEntity.OpeningStyle.DRAWBRIDGE;
               default -> GateEntity.OpeningStyle.VERTICAL;
            };
         }

         private static EntityType<?> vehicleEntityType(int oldType) {
            return switch (oldType) {
               case 0, 1 -> AWCoreRegistryAccess.entityType(AWCoreIds.CATAPULT_TURRET_STAND);
               case 2, 3 -> AWCoreRegistryAccess.entityType(AWCoreIds.CATAPULT_MOBILE);
               case 4 -> AWCoreRegistryAccess.entityType(AWCoreIds.BALLISTA_STAND_FIXED);
               case 5 -> AWCoreRegistryAccess.entityType(AWCoreIds.BALLISTA_TURRET_STAND);
               case 6, 7 -> AWCoreRegistryAccess.entityType(AWCoreIds.BALLISTA_MOBILE);
               case 8 -> AWCoreRegistryAccess.entityType(AWCoreIds.BATTERING_RAM);
               case 9, 10 -> AWCoreRegistryAccess.entityType(AWCoreIds.CANNON_TURRET_STAND);
               case 11 -> AWCoreRegistryAccess.entityType(AWCoreIds.CANNON_MOBILE);
               case 12 -> AWCoreRegistryAccess.entityType(AWCoreIds.HWACHA);
               case 13 -> AWCoreRegistryAccess.entityType(AWCoreIds.TREBUCHET_FIXED);
               case 14 -> AWCoreRegistryAccess.entityType(AWCoreIds.TREBUCHET_TURRET);
               case 15 -> AWCoreRegistryAccess.entityType(AWCoreIds.TREBUCHET_MOBILE);
               case 16 -> AWCoreRegistryAccess.entityType(AWCoreIds.GIANT_TREBUCHET);
               case 17 -> AWCoreRegistryAccess.entityType(AWCoreIds.CHEST_PUSHCART);
               case 18 -> AWCoreRegistryAccess.entityType(AWCoreIds.BALLISTA_BOAT);
               case 19 -> AWCoreRegistryAccess.entityType(AWCoreIds.CATAPULT_BOAT);
               case 20 -> AWCoreRegistryAccess.entityType(AWCoreIds.TRANSPORT_BOAT);
               default -> null;
            };
         }

         private static void clearGateArea(Level level, BlockPos first, BlockPos second) {
            BlockPos min = new BlockPos(
               Math.min(first.m_123341_(), second.m_123341_()),
               Math.min(first.m_123342_(), second.m_123342_()),
               Math.min(first.m_123343_(), second.m_123343_())
            );
            BlockPos max = new BlockPos(
               Math.max(first.m_123341_(), second.m_123341_()),
               Math.max(first.m_123342_(), second.m_123342_()),
               Math.max(first.m_123343_(), second.m_123343_())
            );

            for (BlockPos pos : BlockPos.m_121940_(min, max)) {
               level.m_7471_(pos, false);
            }
         }

         private static BlockPos parseRelativePos(String text, String key) {
            String value = AwsTemplatePlacer.AwsTemplate.extract(text, key + ":{", "}");
            return new BlockPos(parseInt(value, "x", 0), parseInt(value, "y", 0), parseInt(value, "z", 0));
         }

         private static BlockPos rotateOffset(BlockPos pos, int turns) {
            for (int i = 0; i < turns; i++) {
               pos = new BlockPos(-pos.m_123343_(), pos.m_123342_(), pos.m_123341_());
            }

            return pos;
         }

         private static BlockPos rotateAnchor(BlockPos pos, int turns, int sizeX, int sizeZ) {
            int currentXSize = sizeX;
            int currentZSize = sizeZ;
            int x = pos.m_123341_();
            int z = pos.m_123343_();

            for (int i = 0; i < turns; i++) {
               int nextX = currentZSize - 1 - z;
               z = x;
               x = nextX;
               int oldXSize = currentXSize;
               currentXSize = currentZSize;
               currentZSize = oldXSize;
            }

            return new BlockPos(x, pos.m_123342_(), z);
         }

         private static BlockPos decodeLegacyBlockPos(long packed) {
            int x = (int)(packed >> 38);
            int y = (int)(packed >> 26 & 4095L);
            int z = (int)(packed & 67108863L);
            if (y >= 2048) {
               y -= 4096;
            }

            if (z >= 33554432) {
               z -= 67108864;
            }

            return new BlockPos(x, y, z);
         }

         private static int parseInt(String text, String key, int fallback) {
            return (int)parseLong(text, key, (long)fallback);
         }

         private static long parseLong(String text, String key, long fallback) {
            String value = numberValue(text, key);
            if (value.isBlank()) {
               return fallback;
            } else {
               try {
                  return Long.parseLong(value.replace("L", "").replace("b", ""));
               } catch (NumberFormatException var6) {
                  return fallback;
               }
            }
         }

         private static double parseDouble(String text, String key, double fallback) {
            String value = numberValue(text, key);
            if (value.isBlank()) {
               return fallback;
            } else {
               try {
                  return Double.parseDouble(value.replace("f", "").replace("d", ""));
               } catch (NumberFormatException var6) {
                  return fallback;
               }
            }
         }

         private static String numberValue(String text, String key) {
            int start = text.indexOf(key + ":");
            if (start < 0) {
               return "";
            } else {
               start += key.length() + 1;
               int end = start;

               while (end < text.length() && "-+.0123456789EeLfbds".indexOf(text.charAt(end)) >= 0) {
                  end++;
               }

               return text.substring(start, end);
            }
         }
      }

      private static record LootDefinition(ResourceLocation table, int rolls) {
         private static final ResourceLocation SIMPLE_DUNGEON = new ResourceLocation("minecraft", "chests/simple_dungeon");

         static AwsTemplatePlacer.AwsTemplate.LootDefinition fromOldTemplate(String data) {
            String settings = AwsTemplatePlacer.AwsTemplate.extractBalanced(data, "lootSettings:", '{', '}');
            String lootData = settings.isBlank() ? data : settings;
            int level = parseSimpleInt(lootData, "lootLevel", parseSimpleInt(data, "lootLevel", 0));
            String oldTable = AwsTemplatePlacer.AwsTemplate.extract(lootData, "lootTableName:\"", "\"");
            if (oldTable.isBlank()) {
               oldTable = AwsTemplatePlacer.AwsTemplate.extract(lootData, "LootTable:\"", "\"");
            }

            boolean hasLoot = lootData.contains("hasLoot:1b")
               || lootData.contains("hasLoot:true")
               || data.contains("hasLoot:1b")
               || data.contains("hasLoot:true");
            if (oldTable.isBlank() || oldTable.equals("minecraft:") || !hasLoot && !lootData.contains("LootTable:\"")) {
               return switch (level) {
                  case 1, 2, 3 -> new AwsTemplatePlacer.AwsTemplate.LootDefinition(SIMPLE_DUNGEON, level);
                  default -> null;
               };
            } else {
               int rolls = Math.max(1, parseSimpleInt(lootData, "lootRolls", parseSimpleInt(data, "lootRolls", 1)));
               return new AwsTemplatePlacer.AwsTemplate.LootDefinition(mapOldTable(oldTable), rolls);
            }
         }

         private static ResourceLocation mapOldTable(String oldTable) {
            return StructureLootTierResolver.resolveLegacyTable(oldTable);
         }

         private static int parseSimpleInt(String text, String key, int fallback) {
            int start = text.indexOf(key + ":");
            if (start < 0) {
               return fallback;
            } else {
               start += key.length() + 1;
               int end = start;

               while (end < text.length() && Character.isDigit(text.charAt(end))) {
                  end++;
               }

               try {
                  return Integer.parseInt(text.substring(start, end));
               } catch (NumberFormatException var6) {
                  return fallback;
               }
            }
         }
      }

      private static record PlacedRule(BlockPos pos, AwsTemplatePlacer.AwsTemplate.RuleState rule) {
      }

      private static record RuleState(BlockState state, String data, String plugin) {
         boolean isInvisibleProxy() {
            return this.data.contains("mainBlockPos:") && !this.data.contains("mainBlock:1b")
               ? !(this.state.m_60734_() instanceof TallStructureDecorBlock)
               : "coffin".equals(this.plugin) && this.data.contains("invisible:\"true\"");
         }
      }
   }

   public static record ChunkLayout(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
      public int width() {
         return this.maxChunkX - this.minChunkX + 1;
      }

      public int depth() {
         return this.maxChunkZ - this.minChunkZ + 1;
      }

      public int count() {
         return this.width() * this.depth();
      }

      public ChunkPos chunk(int index) {
         return new ChunkPos(this.minChunkX + index % this.width(), this.minChunkZ + index / this.width());
      }
   }

   public static record IncrementalStep(int cursor, int workUsed) {
   }

   public static final class IslandConstruction {
      private final AwsTemplatePlacer.AwsTemplate template;
      private final BlockPos min;
      private final Direction facing;
      private final int turns;
      private final IslandLayerPlan index;
      private final Map<Integer, BlockState> rotated = new HashMap<>();

      private IslandConstruction(AwsTemplatePlacer.AwsTemplate template, BlockPos min, Direction facing) {
         this.template = template;
         this.min = min;
         this.facing = facing;
         this.turns = (facing.m_122416_() + 2) % 4;
         this.index = new IslandLayerPlan(template.sizeX, template.sizeY, template.sizeZ, min.m_123341_(), min.m_123343_(), this.turns);
      }

      public IslandLayerPlan index() {
         return this.index;
      }

      public boolean prepare(IslandWorkBudget budget) {
         int horizontal = this.template.sizeX * this.template.sizeZ;
         return this.index.prepare(budget, linear -> {
            int y = linear / horizontal;
            int cell = linear % horizontal;
            return this.template.isSolidRule(this.template.layers[y][cell / this.template.sizeX][cell % this.template.sizeX]);
         });
      }

      public ChunkPos chunk(int number) {
         return new ChunkPos(this.index.minChunkX() + number % this.index.chunkWidth(), this.index.minChunkZ() + number / this.index.chunkWidth());
      }

      public int sliceSize(int layer, int chunk) {
         return this.index.cells(layer, chunk).length;
      }

      private BlockPos target(int layer, int chunk, int cell) {
         int local = this.index.cells(layer, chunk)[cell];
         return this.template.rotateInArea(new BlockPos(local % this.template.sizeX, layer, local / this.template.sizeX), this.turns).m_121955_(this.min);
      }

      private AwsTemplatePlacer.AwsTemplate.RuleState rule(int layer, int chunk, int cell) {
         int local = this.index.cells(layer, chunk)[cell];
         return this.template.rules.get(this.template.layers[layer][local / this.template.sizeX][local % this.template.sizeX]);
      }

      private BlockState intended(int layer, int chunk, int cell) {
         int local = this.index.cells(layer, chunk)[cell];
         int id = this.template.layers[layer][local / this.template.sizeX][local % this.template.sizeX];
         return this.rotated.computeIfAbsent(id, key -> this.template.rules.get(key).state.m_60717_(AwsTemplatePlacer.rotationFor(this.facing)));
      }

      public boolean matchesRecordedCell(Level level, BlockPos pos) {
         BlockState expected = this.expectedAt(pos);
         return expected != null && !expected.m_155947_() && level.m_7702_(pos) == null && matchesRecordedState(level.m_8055_(pos), expected);
      }

      public int recordedVolumeSize() {
         return Math.multiplyExact(Math.multiplyExact(this.template.sizeX, this.template.sizeY), this.template.sizeZ);
      }

      public int recordedValidationStep(ServerLevel level, int cursor, IslandWorkBudget budget, IslandChunkWindow window, int windowSize) {
         if (cursor >= 0 && cursor < this.recordedVolumeSize()) {
            int horizontal = this.template.sizeX * this.template.sizeZ;
            int y = cursor / horizontal;
            int cell = cursor % horizontal;
            BlockPos pos = this.template.rotateInArea(new BlockPos(cell % this.template.sizeX, y, cell / this.template.sizeX), this.turns).m_121955_(this.min);
            if (window.request(level, this.min, new ChunkPos(pos), windowSize) && budget.tryWork()) {
               BlockState existing = level.m_8055_(pos);
               if (level.m_7702_(pos) == null && !existing.m_155947_()) {
                  return !AwsTemplatePlacer.isOpenWaterTerrain(existing)
                        && !AwsTemplatePlacer.isBasicNaturalTerrain(existing)
                        && !this.matchesRecordedCell(level, pos)
                     ? 1
                     : 0;
               } else {
                  return 1;
               }
            } else {
               return -1;
            }
         } else {
            throw new IndexOutOfBoundsException("Recorded AWS cursor: " + cursor);
         }
      }

      public int validationStep(
         ServerLevel level,
         int layer,
         int chunk,
         int cursor,
         IslandWorkBudget budget,
         IslandChunkWindow window,
         int windowSize,
         List<AwsTemplatePlacer.IslandConstruction> replaceablePlans,
         boolean allowNaturalTerrain
      ) {
         BlockPos pos = this.target(layer, chunk, cursor);
         if (window.request(level, this.min, new ChunkPos(pos), windowSize) && budget.tryWork()) {
            BlockState existing = level.m_8055_(pos);
            if (level.m_7702_(pos) == null && !existing.m_155947_()) {
               if (!AwsTemplatePlacer.isOpenWaterTerrain(existing) && (!allowNaturalTerrain || !AwsTemplatePlacer.isBasicNaturalTerrain(existing))) {
                  for (AwsTemplatePlacer.IslandConstruction recorded : replaceablePlans) {
                     if (recorded.matchesRecordedCell(level, pos)) {
                        return 0;
                     }
                  }

                  return 1;
               } else {
                  return 0;
               }
            } else {
               return 1;
            }
         } else {
            return -1;
         }
      }

      public int validationEntityStep(
         ServerLevel level,
         LargeIslandWorldGenerationData.Task task,
         IslandWorkBudget budget,
         IslandChunkWindow window,
         int windowSize,
         List<AwsTemplatePlacer.IslandConstruction> replaceablePlans,
         boolean allowNaturalTerrain
      ) {
         AwsTemplatePlacer.AwsTemplate.EntityRule entityRule = this.template.entities.get(task.entityCursor);
         if (!"awGate".equals(entityRule.plugin)) {
            if (!budget.tryScan()) {
               return -1;
            } else {
               task.entityCursor++;
               task.cellCursor = 0;
               return 0;
            }
         } else {
            BlockPos anchor = AwsTemplatePlacer.AwsTemplate.EntityRule.rotateAnchor(
                  AwsTemplatePlacer.AwsTemplate.EntityRule.decodeLegacyBlockPos(
                     AwsTemplatePlacer.AwsTemplate.EntityRule.parseLong(entityRule.data, "position", 0L)
                  ),
                  this.turns,
                  this.template.sizeX,
                  this.template.sizeZ
               )
               .m_121955_(this.min);
            BlockPos first = AwsTemplatePlacer.AwsTemplate.EntityRule.rotateOffset(
                  AwsTemplatePlacer.AwsTemplate.EntityRule.parseRelativePos(entityRule.data, "pos1"), this.turns
               )
               .m_121955_(anchor);
            BlockPos second = AwsTemplatePlacer.AwsTemplate.EntityRule.rotateOffset(
                  AwsTemplatePlacer.AwsTemplate.EntityRule.parseRelativePos(entityRule.data, "pos2"), this.turns
               )
               .m_121955_(anchor);
            BlockPos gateMin = new BlockPos(
               Math.min(first.m_123341_(), second.m_123341_()),
               Math.min(first.m_123342_(), second.m_123342_()),
               Math.min(first.m_123343_(), second.m_123343_())
            );
            int width = Math.abs(first.m_123341_() - second.m_123341_()) + 1;
            int depth = Math.abs(first.m_123343_() - second.m_123343_()) + 1;
            int height = Math.abs(first.m_123342_() - second.m_123342_()) + 1;
            int volume = Math.multiplyExact(Math.multiplyExact(width, depth), height);
            if (task.cellCursor >= volume) {
               if (!budget.tryScan()) {
                  return -1;
               } else {
                  task.entityCursor++;
                  task.cellCursor = 0;
                  return 0;
               }
            } else {
               int cursor = task.cellCursor;
               BlockPos pos = gateMin.m_7918_(cursor % width, cursor / width / depth, cursor / width % depth);
               if (window.request(level, this.min, new ChunkPos(pos), windowSize) && budget.tryWork()) {
                  UUID id = UUID.nameUUIDFromBytes(
                     (level.m_46472_().m_135782_() + ":" + this.min + ":" + this.template.path + ":" + task.entityCursor).getBytes(StandardCharsets.UTF_8)
                  );
                  if (!isOwnGateProxy(level, pos, id)) {
                     BlockState existing = level.m_8055_(pos);
                     if (level.m_7702_(pos) != null || existing.m_155947_()) {
                        return 1;
                     }

                     boolean allowed = AwsTemplatePlacer.isOpenWaterTerrain(existing)
                        || allowNaturalTerrain && AwsTemplatePlacer.isBasicNaturalTerrain(existing);
                     if (!allowed) {
                        for (AwsTemplatePlacer.IslandConstruction recorded : replaceablePlans) {
                           if (recorded.matchesRecordedCell(level, pos)) {
                              allowed = true;
                              break;
                           }
                        }
                     }

                     if (!allowed) {
                        return 1;
                     }
                  }

                  task.cellCursor++;
                  return 0;
               } else {
                  return -1;
               }
            }
         }
      }

      private BlockState expectedAt(BlockPos pos) {
         int dx = pos.m_123341_() - this.min.m_123341_();
         int dz = pos.m_123343_() - this.min.m_123343_();
         int y = pos.m_123342_() - this.min.m_123342_();
         int x;
         int z;
         switch (this.turns) {
            case 1:
               x = dz;
               z = this.template.sizeZ - 1 - dx;
               break;
            case 2:
               x = this.template.sizeX - 1 - dx;
               z = this.template.sizeZ - 1 - dz;
               break;
            case 3:
               x = this.template.sizeX - 1 - dz;
               z = dx;
               break;
            default:
               x = dx;
               z = dz;
         }

         if (x >= 0 && x < this.template.sizeX && z >= 0 && z < this.template.sizeZ && y >= 0 && y < this.template.sizeY) {
            int id = this.template.layers[y][z][x];
            return !this.template.isSolidRule(id)
               ? null
               : this.rotated.computeIfAbsent(id, key -> this.template.rules.get(key).state.m_60717_(AwsTemplatePlacer.rotationFor(this.facing)));
         } else {
            return null;
         }
      }

      private static boolean matchesRecordedState(BlockState existing, BlockState expected) {
         if (existing.m_60734_() != expected.m_60734_()) {
            return false;
         } else {
            for (Property<?> property : expected.m_61147_()) {
               String name = property.m_61708_();
               boolean dynamic = "waterlogged".equals(name)
                  || "open".equals(name)
                  || "powered".equals(name)
                  || "lit".equals(name)
                  || AwsTemplatePlacer.hasHorizontalConnections(expected)
                     && ("north".equals(name) || "east".equals(name) || "south".equals(name) || "west".equals(name) || "up".equals(name))
                  || expected.m_60734_() instanceof BenchStructureDecorBlock && "legs".equals(name)
                  || expected.m_60734_() instanceof StairBlock && "shape".equals(name);
               if (!dynamic && (!existing.m_61138_(property) || !existing.m_61143_(property).equals(expected.m_61143_(property)))) {
                  return false;
               }
            }

            return true;
         }
      }

      private static void requireSafeOverwrite(Level level, BlockPos pos, BlockState existing) {
         if (level.m_7702_(pos) != null || existing.m_155947_() || !existing.m_60795_() && !AwsTemplatePlacer.isBasicNaturalTerrain(existing)) {
            throw new IllegalStateException("Protected block appeared during island construction at " + pos);
         }
      }

      private static boolean isOwnGateProxy(Level level, BlockPos pos, UUID id) {
         return level.m_8055_(pos).m_60713_(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY))
            && level.m_7702_(pos) instanceof GateProxyBlockEntity proxy
            && proxy.isGate(id);
      }

      private void requireSafeGateOverwrite(Level level, BlockPos pos, UUID id) {
         if (!isOwnGateProxy(level, pos, id)) {
            BlockState existing = level.m_8055_(pos);
            if (level.m_7702_(pos) != null || existing.m_155947_() || !this.matchesRecordedCell(level, pos)) {
               requireSafeOverwrite(level, pos, existing);
            }
         }
      }

      public boolean step(
         ServerLevel level,
         LargeIslandWorldGenerationData.Phase phase,
         int layer,
         int chunk,
         int cursor,
         IslandWorkBudget budget,
         IslandChunkWindow window,
         int windowSize
      ) {
         boolean finalConnections = phase == LargeIslandWorldGenerationData.Phase.FINAL_CONNECT;
         int cell = finalConnections ? cursor / 5 : cursor;
         BlockPos pos = this.target(layer, chunk, cell);
         BlockState expected = this.intended(layer, chunk, cell);
         if (phase == LargeIslandWorldGenerationData.Phase.CONFIGURE && !expected.m_155947_()) {
            return budget.tryScan();
         } else if (phase == LargeIslandWorldGenerationData.Phase.CONNECT
            && expected.m_60815_()
            && !(expected.m_60734_() instanceof FallingBlock)
            && !expected.m_155947_()) {
            return budget.tryScan();
         } else {
            if (finalConnections) {
               int neighbor = cursor % 5;
               if (neighbor > 0) {
                  pos = pos.m_121945_(AwsTemplatePlacer.HORIZONTAL[neighbor - 1]);
               }

               if (this.knownNonConnector(pos)) {
                  return budget.tryScan();
               }
            }

            boolean neighbors = phase == LargeIslandWorldGenerationData.Phase.CONNECT
               || finalConnections
               || phase == LargeIslandWorldGenerationData.Phase.CLEANUP
               || phase == LargeIslandWorldGenerationData.Phase.BUILD;
            List<ChunkPos> required = phase == LargeIslandWorldGenerationData.Phase.CONFIGURE
               ? AwsTemplatePlacer.surroundingChunks(pos, 4)
               : (neighbors ? AwsTemplatePlacer.surroundingChunks(pos) : List.of(new ChunkPos(pos)));
            if (window.requestGroup(level, this.min, required, windowSize) && budget.tryWork()) {
               switch (phase) {
                  case BUILD:
                     if (!(expected.m_60734_() instanceof FallingBlock)) {
                        BlockState existing = level.m_8055_(pos);
                        if (existing.m_60734_() != expected.m_60734_()) {
                           requireSafeOverwrite(level, pos, existing);
                           level.m_7731_(pos, expected, 18);
                        }
                     }
                     break;
                  case CLEANUP:
                     if (this.matchesRecordedCell(level, pos)) {
                        level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 18);
                     }
                     break;
                  case CONNECT:
                     BlockState state = level.m_8055_(pos);
                     BlockState desired = this.intended(layer, chunk, cell);
                     if ((
                           state.m_60795_()
                              || desired.m_60734_() instanceof FallingBlock
                              || !state.m_60819_().m_76178_() && state.m_60734_() != desired.m_60734_()
                        )
                        && state.m_60734_() != desired.m_60734_()) {
                        requireSafeOverwrite(level, pos, state);
                        state = desired;
                        level.m_7731_(pos, desired, 18);
                     }

                     if (state.m_60734_() instanceof DoorBlock) {
                        AwsTemplatePlacer.AwsTemplate.normalizeDoors(
                           level, List.of(new AwsTemplatePlacer.AwsTemplate.PlacedRule(pos, this.rule(layer, chunk, cell)))
                        );
                     }

                     connectChest(level, pos);
                     AwsTemplatePlacer.refreshConnection(level, pos);
                     break;
                  case CONFIGURE:
                     BlockEntity blockEntity = level.m_7702_(pos);
                     if (blockEntity instanceof ProtectionFlagBlockEntity flag) {
                        flag.loadFromOldTemplateData(this.rule(layer, chunk, cell).data);
                        flag.configureTerritory(this.min, this.template.structureMax(this.min, this.facing));
                     } else if (blockEntity instanceof AdvancedSpawnerBlockEntity spawner) {
                        spawner.loadFromOldTemplateData(this.rule(layer, chunk, cell).data);
                     } else if (blockEntity != null) {
                        this.template.configureStructureLoot(level, pos, this.rule(layer, chunk, cell).data, true);
                     }
                     break;
                  case FINAL_CONNECT:
                     AwsTemplatePlacer.refreshConnection(level, pos);
                     break;
                  default:
                     throw new IllegalArgumentException("Not a cell phase: " + phase);
               }

               return true;
            } else {
               return false;
            }
         }
      }

      private boolean knownNonConnector(BlockPos pos) {
         int dx = pos.m_123341_() - this.min.m_123341_();
         int dz = pos.m_123343_() - this.min.m_123343_();
         int y = pos.m_123342_() - this.min.m_123342_();
         int x;
         int z;
         switch (this.turns) {
            case 1:
               x = dz;
               z = this.template.sizeZ - 1 - dx;
               break;
            case 2:
               x = this.template.sizeX - 1 - dx;
               z = this.template.sizeZ - 1 - dz;
               break;
            case 3:
               x = this.template.sizeX - 1 - dz;
               z = dx;
               break;
            default:
               x = dx;
               z = dz;
         }

         if (x >= 0 && x < this.template.sizeX && z >= 0 && z < this.template.sizeZ && y >= 0 && y < this.template.sizeY) {
            int id = this.template.layers[y][z][x];
            return this.template.isSolidRule(id) && !AwsTemplatePlacer.hasHorizontalConnections(this.template.rules.get(id).state);
         } else {
            return false;
         }
      }

      private static void connectChest(ServerLevel level, BlockPos pos) {
         BlockState state = level.m_8055_(pos);
         if (state.m_60734_() instanceof ChestBlock && state.m_61143_(ChestBlock.f_51479_) == ChestType.SINGLE) {
            BlockPos partnerPos = pos.m_121945_(((Direction)state.m_61143_(ChestBlock.f_51478_)).m_122427_());
            BlockState partner = level.m_8055_(partnerPos);
            if (partner.m_60713_(state.m_60734_())
               && partner.m_61143_(ChestBlock.f_51479_) == ChestType.SINGLE
               && partner.m_61143_(ChestBlock.f_51478_) == state.m_61143_(ChestBlock.f_51478_)) {
               level.m_7731_(pos, (BlockState)state.m_61124_(ChestBlock.f_51479_, ChestType.LEFT), 18);
               level.m_7731_(partnerPos, (BlockState)partner.m_61124_(ChestBlock.f_51479_, ChestType.RIGHT), 18);
            }
         }
      }

      public int entityCount() {
         return this.template.entities.size();
      }

      public boolean entityStep(ServerLevel level, LargeIslandWorldGenerationData.Task task, IslandWorkBudget budget, IslandChunkWindow window, int windowSize) {
         AwsTemplatePlacer.AwsTemplate.EntityRule entityRule = this.template.entities.get(task.entityCursor);
         BlockPos anchor = AwsTemplatePlacer.AwsTemplate.EntityRule.rotateAnchor(
               AwsTemplatePlacer.AwsTemplate.EntityRule.decodeLegacyBlockPos(
                  AwsTemplatePlacer.AwsTemplate.EntityRule.parseLong(entityRule.data, "position", 0L)
               ),
               this.turns,
               this.template.sizeX,
               this.template.sizeZ
            )
            .m_121955_(this.min);
         UUID id = UUID.nameUUIDFromBytes(
            (level.m_46472_().m_135782_() + ":" + this.min + ":" + this.template.path + ":" + task.entityCursor).getBytes(StandardCharsets.UTF_8)
         );
         if (!"awGate".equals(entityRule.plugin)) {
            double offsetX = AwsTemplatePlacer.AwsTemplate.EntityRule.parseDouble(entityRule.data, "xOffset", 0.5);
            double offsetZ = AwsTemplatePlacer.AwsTemplate.EntityRule.parseDouble(entityRule.data, "zOffset", 0.5);

            for (int i = 0; i < this.turns; i++) {
               double oldX = offsetX;
               offsetX = 1.0 - offsetZ;
               offsetZ = oldX;
            }

            ChunkPos actualChunk = new ChunkPos(
               BlockPos.m_274561_((double)anchor.m_123341_() + offsetX, (double)anchor.m_123342_(), (double)anchor.m_123343_() + offsetZ)
            );
            if (window.requestGroup(level, this.min, List.of(new ChunkPos(anchor), actualChunk), windowSize)
               && level.m_143319_(actualChunk.m_45588_())
               && budget.tryWork()) {
               if (level.m_8791_(id) == null) {
                  entityRule.placeVehicle(level, anchor, this.turns, id);
               }

               task.entityCursor++;
               task.cellCursor = 0;
               return true;
            } else {
               return false;
            }
         } else {
            BlockPos first = AwsTemplatePlacer.AwsTemplate.EntityRule.rotateOffset(
                  AwsTemplatePlacer.AwsTemplate.EntityRule.parseRelativePos(entityRule.data, "pos1"), this.turns
               )
               .m_121955_(anchor);
            BlockPos second = AwsTemplatePlacer.AwsTemplate.EntityRule.rotateOffset(
                  AwsTemplatePlacer.AwsTemplate.EntityRule.parseRelativePos(entityRule.data, "pos2"), this.turns
               )
               .m_121955_(anchor);
            BlockPos gateMin = new BlockPos(
               Math.min(first.m_123341_(), second.m_123341_()),
               Math.min(first.m_123342_(), second.m_123342_()),
               Math.min(first.m_123343_(), second.m_123343_())
            );
            int width = Math.abs(first.m_123341_() - second.m_123341_()) + 1;
            int depth = Math.abs(first.m_123343_() - second.m_123343_()) + 1;
            int height = Math.abs(first.m_123342_() - second.m_123342_()) + 1;
            int volume = Math.multiplyExact(Math.multiplyExact(width, depth), height);
            int totalCells = Math.multiplyExact(volume, 2);
            BlockPos center = new BlockPos(
               Math.floorDiv(first.m_123341_() + second.m_123341_() + 1, 2), gateMin.m_123342_(), Math.floorDiv(first.m_123343_() + second.m_123343_() + 1, 2)
            );
            int cursor = Math.min(task.cellCursor, totalCells);
            int withinGate = cursor % volume;
            BlockPos pos = gateMin.m_7918_(withinGate % width, withinGate / width / depth, withinGate / width % depth);
            List<ChunkPos> required = new ArrayList<>(AwsTemplatePlacer.surroundingChunks(cursor < totalCells ? pos : center));
            required.add(new ChunkPos(center));
            if (window.requestGroup(level, this.min, required, windowSize) && level.m_143319_(new ChunkPos(center).m_45588_()) && budget.tryWork()) {
               if (cursor < volume) {
                  this.requireSafeGateOverwrite(level, pos, id);
               } else if (cursor < totalCells && !isOwnGateProxy(level, pos, id)) {
                  throw new IllegalStateException("Gate proxy changed during island construction at " + pos);
               }

               Entity existing = level.m_8791_(id);
               GateEntity gate;
               if (existing instanceof GateEntity found) {
                  gate = found;
               } else {
                  if (existing != null) {
                     throw new IllegalStateException("AWS entity UUID occupied: " + id);
                  }

                  gate = (GateEntity)AWCoreRegistryAccess.entityType(AWCoreIds.DYNAMIC_GATE).m_20615_(level);
                  if (gate == null) {
                     throw new IllegalStateException("Unable to create AWS gate");
                  }

                  gate.m_20084_(id);
                  Direction gateFacing = Direction.m_122376_(
                     AwsTemplatePlacer.AwsTemplate.EntityRule.parseInt(entityRule.data, "orientation", Direction.NORTH.ordinal())
                  );

                  for (int i = 0; i < this.turns; i++) {
                     gateFacing = gateFacing.m_122427_();
                  }

                  gate.configure(first, second, gateFacing);
                  String type = AwsTemplatePlacer.AwsTemplate.extract(entityRule.data, "gateType:\"", "\"");
                  gate.setMaterial(type.toLowerCase(Locale.ROOT).contains("iron") ? GateEntity.Material.IRON : GateEntity.Material.WOOD);
                  gate.setOpeningStyle(AwsTemplatePlacer.AwsTemplate.EntityRule.gateOpeningStyle(type));
                  if (!level.m_7967_(gate)) {
                     throw new IllegalStateException("Unable to add AWS gate " + id);
                  }
               }

               if (cursor < volume) {
                  level.m_7731_(pos, AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY).m_49966_(), 18);
                  if (level.m_7702_(pos) instanceof GateProxyBlockEntity proxy) {
                     proxy.setGateUuid(id);
                  }

                  if (level.m_276867_(pos)) {
                     gate.getPersistentData().m_128379_("IslandGatePowered", true);
                  }

                  task.cellCursor++;
               } else if (cursor < totalCells) {
                  BlockState proxyState = level.m_8055_(pos);
                  if (isOwnGateProxy(level, pos, id)) {
                     level.m_7731_(pos, (BlockState)proxyState.m_61124_(GateProxyBlock.OPEN, gate.getPersistentData().m_128471_("IslandGatePowered")), 18);
                     if (level.m_7702_(pos) instanceof GateProxyBlockEntity proxy) {
                        proxy.setGateUuid(id);
                     }
                  }

                  task.cellCursor++;
               } else {
                  gate.finishWorldgenProxySetup(gate.getPersistentData().m_128471_("IslandGatePowered"));
                  gate.getPersistentData().m_128473_("IslandGatePowered");
                  task.entityCursor++;
                  task.cellCursor = 0;
               }

               return true;
            } else {
               return false;
            }
         }
      }
   }

   public static record PreviewBlock(BlockPos pos, BlockState state) {
   }

   public static record PreviewData(BlockPos min, BlockPos max, List<AwsTemplatePlacer.PreviewBlock> blocks) {
   }

   public static record ResourceInfo(ResourceLocation itemId, int count) {
   }

   public static record TemplateInfo(String name, String path) {
   }
}
