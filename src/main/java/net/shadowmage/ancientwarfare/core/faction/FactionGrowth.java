package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplatePlacer;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenTemplateRegistry;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Settlements that do well grow by themselves. A day of a full, orderly, fed town adds to its
 * growth; while a player is somewhere around, but not watching the spot, the town builds one piece
 * a while on flat open ground just outside its edge: a house with a bed, a well, a market stall,
 * a lamp post or a garden, with a path back to the middle. Every third piece is, where the town
 * lacks one and its faction has the building, a hall: a tavern, a smithy, stables or a guild hall.
 * Each piece widens the town's bounds and makes room for a few more people.
 */
public final class FactionGrowth {
   /** People each built piece makes room for; see FactionSimulation.capacity. */
   static final int ROOM_PER_PIECE = 4;
   private static final int MAX_PENDING = 6;
   private static final int MAX_GROWN = 12;
   /** Each rank a town rises lets it build this many pieces more. */
   private static final int GROWN_PER_RANK = 6;
   private static final long TICK_INTERVAL = 200L;
   private static final double NEAR = 160.0;
   private static final double TOO_CLOSE = 24.0;
   private static final int SITE_TRIES = 24;
   private static final int GAP = 3;
   private static final int REACH = 8;
   private static final int MAX_SLOPE = 2;
   /** How far a piece's ground may be raised or cut to meet the town's level. */
   private static final int MAX_LIFT = 4;
   /** How far around a piece the land is sloped to its floor. */
   private static final int BLEND = 3;
   /** Ground samples needed to judge the level of a town built before its level was recorded. */
   private static final int FLOOR_SAMPLES = 12;
   private static final int PATH_STEPS = 64;
   /** Every this many pieces, a hall the town lacks instead. */
   private static final int HALL_EVERY = 3;
   private static final int HALL_TRIES = 32;
   /** Halls wider or taller than this would not sit beside a town's edge. */
   private static final int HALL_MAX_FOOTPRINT = 36;
   private static final int HALL_MAX_HEIGHT = 32;
   /** The most the ground under a hall may rise and fall before it is levelled. */
   private static final int HALL_MAX_SLOPE = 5;
   private static final List<String> HALL_EXCLUDED = List.of("ship", "boat", "ruin", "camp", "infested", "portal", "spawner", "islands", "tomb", "beach", "coastal");
   private static final Map<String, List<String>> HALL_TEMPLATES = new HashMap<>();
   private static final Piece[] ORDER = {
      Piece.HOUSE, Piece.LAMP, Piece.GARDEN, Piece.HOUSE, Piece.WELL, Piece.HOUSE, Piece.STALL, Piece.LAMP, Piece.HOUSE, Piece.GARDEN, Piece.HOUSE, Piece.STALL
   };

   private FactionGrowth() {
   }

   private enum Piece {
      HOUSE(7, 6, 5),
      WELL(3, 3, 5),
      STALL(3, 3, 4),
      LAMP(1, 1, 5),
      GARDEN(5, 5, 2),
      /** A stone watchtower: only a town that has become a city builds them. */
      TOWER(3, 3, 9);

      final int width;
      final int depth;
      final int height;

      Piece(int width, int depth, int height) {
         this.width = width;
         this.depth = depth;
         this.height = height;
      }
   }

   private record Palette(BlockState wall, BlockState frame, BlockState floor, BlockState roof, BlockState fence, Block door) {
      static Palette of(ServerLevel level, BlockPos pos) {
         BlockState ground = level.getBlockState(pos);
         if (ground.is(BlockTags.SAND)) {
            return new Palette(
               Blocks.SANDSTONE.defaultBlockState(),
               Blocks.CUT_SANDSTONE.defaultBlockState(),
               Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
               Blocks.SANDSTONE_SLAB.defaultBlockState(),
               Blocks.JUNGLE_FENCE.defaultBlockState(),
               Blocks.JUNGLE_DOOR
            );
         }

         if (level.getBiome(pos).value().coldEnoughToSnow(pos)) {
            return new Palette(
               Blocks.SPRUCE_PLANKS.defaultBlockState(),
               Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
               Blocks.COBBLESTONE.defaultBlockState(),
               Blocks.SPRUCE_SLAB.defaultBlockState(),
               Blocks.SPRUCE_FENCE.defaultBlockState(),
               Blocks.SPRUCE_DOOR
            );
         }

         return new Palette(
            Blocks.OAK_PLANKS.defaultBlockState(),
            Blocks.OAK_LOG.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.SPRUCE_SLAB.defaultBlockState(),
            Blocks.OAK_FENCE.defaultBlockState(),
            Blocks.OAK_DOOR
         );
      }
   }

   /** Daily: a town that is nearly full, fed and orderly gets a piece more to build. */
   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (thriving(data, settlement) && settlement.growth < MAX_PENDING && settlement.grown + settlement.growth < MAX_GROWN + GROWN_PER_RANK * settlement.tier()) {
            settlement.growth++;
            data.setDirty();
         }
      }
   }

   private static boolean thriving(FactionWorldData data, FactionWorldData.Settlement settlement) {
      return FactionTownLife.isTown(settlement)
         && settlement.famine() == 0
         && settlement.plague() == 0
         && settlement.order() >= 50
         && settlement.population() * 4 >= FactionSimulation.capacity(settlement) * 3
         && !FactionTownLife.underAttack(data, settlement);
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % TICK_INTERVAL != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.growth > 0 && FactionTownLife.isTown(settlement) && nearestPlayer(level, settlement.center()) <= NEAR * NEAR && grow(level, data, settlement, false)) {
            return;
         }
      }
   }

   private static double nearestPlayer(ServerLevel level, BlockPos pos) {
      double best = Double.MAX_VALUE;
      for (ServerPlayer player : level.players()) {
         double dx = player.getX() - pos.getX() - 0.5;
         double dz = player.getZ() - pos.getZ() - 0.5;
         best = Math.min(best, dx * dx + dz * dz);
      }

      return best;
   }

   /** The halls a town puts up, in the order it wants them, and the words their buildings are named by. */
   public enum HallKind {
      TAVERN("tavern", "inn"),
      SMITHY("smith", "forge"),
      STABLES("stable"),
      GUILD("guild");

      final List<String> words;

      HallKind(String... words) {
         this.words = List.of(words);
      }

      public String key() {
         return this.name().toLowerCase(java.util.Locale.ROOT);
      }

      boolean names(String name) {
         String lower = name.toLowerCase(java.util.Locale.ROOT);
         return this.words.stream().anyMatch(lower::contains);
      }
   }

   /** Builds the town's next piece, if a fitting spot is found. */
   private static boolean grow(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, boolean watched) {
      if (settlement.grown % HALL_EVERY == HALL_EVERY - 1) {
         HallKind kind = wantedHall(settlement);
         if (kind != null && hall(level, data, settlement, kind, watched)) {
            settlement.grown++;
            settlement.growth = Math.max(0, settlement.growth - 1);
            data.setDirty();
            return true;
         }
      }

      // A city puts up a watchtower every fourth piece.
      Piece piece = settlement.tier() >= FactionPorts.CITY && settlement.grown % 4 == 3 ? Piece.TOWER : ORDER[settlement.grown % ORDER.length];
      RandomSource random = level.getRandom();
      BlockPos site = site(level, data, settlement, piece, random, watched);
      if (site == null) {
         return false;
      }

      BlockPos center = settlement.center();
      Direction front = facing(site.getX() + piece.width / 2, site.getZ() + piece.depth / 2, center);
      Palette palette = Palette.of(level, site.offset(piece.width / 2, 0, piece.depth / 2));
      clear(level, site, piece);
      BlockPos door = switch (piece) {
         case HOUSE -> house(level, site, front, palette);
         case WELL -> well(level, site, palette);
         case STALL -> stall(level, site, palette, random);
         case LAMP -> lamp(level, site, palette);
         case GARDEN -> garden(level, site, palette, random);
         case TOWER -> tower(level, site, palette);
      };
      path(level, settlement, door.relative(front), center);
      settlement.extend(site, site.offset(piece.width - 1, piece.height, piece.depth - 1));
      settlement.grown++;
      settlement.growth = Math.max(0, settlement.growth - 1);
      data.setDirty();
      if (piece == Piece.HOUSE) {
         Component name = settlement.name().isEmpty() ? FactionNews.factionName(settlement.faction()) : settlement.nameComponent();
         for (ServerPlayer player : level.players()) {
            if (settlement.horizontalDistanceSquared(player.getX(), player.getZ()) <= NEAR * NEAR) {
               player.sendSystemMessage(Component.translatable("town.ancientwarfare.grew", name).withStyle(ChatFormatting.GRAY));
            }
         }
      }

      AncientWarfareCore.LOG.info("Settlement #{} of {} grows: a {} at {}", settlement.id(), settlement.faction(), piece, site);
      return true;
   }

   /** The first hall the town has none of and its faction has a building for, or null. */
   private static HallKind wantedHall(FactionWorldData.Settlement settlement) {
      for (HallKind kind : HallKind.values()) {
         if (!hasHall(settlement, kind) && !hallTemplates(settlement.faction(), kind).isEmpty()) {
            return kind;
         }
      }

      return null;
   }

   /**
    * Whether the town has put up such a hall as it grew, or is itself one such building. A laid-out
    * town's list of buildings says only what it may have, not what was built, so a big town can come
    * to have a second tavern.
    */
   public static boolean hasHall(FactionWorldData.Settlement settlement, HallKind kind) {
      if (settlement.halls().stream().anyMatch(hall -> hall.kind().equals(kind.key()))) {
         return true;
      }

      String template = settlement.template();
      return template != null && !FactionTowns.isGenerated(template) && kind.names(template.substring(template.lastIndexOf('/') + 1));
   }

   private static synchronized List<String> hallTemplates(String faction, HallKind kind) {
      return HALL_TEMPLATES.computeIfAbsent(faction + "/" + kind.key(), key -> {
         List<String> found = new java.util.ArrayList<>();
         for (String path : TemplateFactions.templatesOf(faction)) {
            AwsWorldGenTemplateRegistry.Template template = AwsWorldGenTemplateRegistry.templateForActivePath(path);
            String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(java.util.Locale.ROOT);
            if (template != null
               && template.ground()
               && !template.aquatic()
               && kind.names(name)
               && HALL_EXCLUDED.stream().noneMatch(name::contains)
               && Math.max(template.sizeX(), template.sizeZ()) <= HALL_MAX_FOOTPRINT
               && template.sizeY() <= HALL_MAX_HEIGHT) {
               found.add(path);
            }
         }

         return found;
      });
   }

   /** Puts up one of the faction's buildings of this kind just outside the town, if a spot is found. */
   private static boolean hall(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, HallKind kind, boolean watched) {
      RandomSource random = level.getRandom();
      List<String> paths = hallTemplates(settlement.faction(), kind);
      AwsWorldGenTemplateRegistry.Template template = AwsWorldGenTemplateRegistry.templateForActivePath(paths.get(random.nextInt(paths.size())));
      if (template == null) {
         return false;
      }

      BlockPos min = settlement.min();
      BlockPos max = settlement.max();
      BlockPos center = settlement.center();
      int floor = floor(level, data, settlement);
      int span = Math.max(template.sizeX(), template.sizeZ());
      for (int i = 0; i < HALL_TRIES; i++) {
         int out = GAP + random.nextInt(REACH);
         int x;
         int z;
         switch (random.nextInt(4)) {
            case 0 -> {
               x = min.getX() + random.nextInt(max.getX() - min.getX() + 1) - span / 2;
               z = min.getZ() - out - span;
            }
            case 1 -> {
               x = min.getX() + random.nextInt(max.getX() - min.getX() + 1) - span / 2;
               z = max.getZ() + out + 1;
            }
            case 2 -> {
               x = min.getX() - out - span;
               z = min.getZ() + random.nextInt(max.getZ() - min.getZ() + 1) - span / 2;
            }
            default -> {
               x = max.getX() + out + 1;
               z = min.getZ() + random.nextInt(max.getZ() - min.getZ() + 1) - span / 2;
            }
         }

         // The span-wide square the hall turns within; it faces the middle of town.
         if (!loadedArea(level, x - BLEND - 1, z - BLEND - 1, x + span + BLEND, z + span + BLEND) || crowded(data, x, z, span, span)) {
            continue;
         }

         if (!watched && nearestPlayer(level, new BlockPos(x + span / 2, 0, z + span / 2)) < TOO_CLOSE * TOO_CLOSE) {
            continue;
         }

         Direction facing = facing(x + span / 2, z + span / 2, center);
         int sizeX = template.rotatedSizeX(facing);
         int sizeZ = template.rotatedSizeZ(facing);
         Integer ground = hallGround(level, x, z, sizeX, sizeZ);
         if (ground == null) {
            continue;
         }

         boolean free = floor == FactionWorldData.NO_FLOOR || floor == FactionWorldData.FOLLOWS_GROUND;
         if (!free && Math.abs(ground - floor) <= MAX_LIFT) {
            ground = floor;
         }

         // Only the template's height is taken from its origin: its foundation sinks below the ground.
         BlockPos corner = new BlockPos(x, template.minFor(new BlockPos(x, ground + 1, z), facing).getY(), z);
         int maxX = x + sizeX - 1;
         int maxZ = z + sizeZ - 1;

         AwsWorldGenerator.levelTerrain(level, template, corner, maxX, maxZ, ground);
         if (!AwsTemplatePlacer.placeAtMin(level, corner, template.path(), facing, true)) {
            return false;
         }

         BlockPos top = new BlockPos(maxX, corner.getY() + template.sizeY() - 1, maxZ);
         settlement.extend(corner, top);
         settlement.halls().add(new FactionWorldData.Hall(kind.key(), corner, top));
         // A path from the hall's side that faces the town back to the middle.
         BlockPos side = new BlockPos(
            Math.max(corner.getX() - 1, Math.min(maxX + 1, center.getX())), ground, Math.max(corner.getZ() - 1, Math.min(maxZ + 1, center.getZ()))
         );
         path(level, settlement, side, center);
         AncientWarfareCore.LOG.info("Settlement #{} of {} grows: a {} hall ({}) at {}", settlement.id(), settlement.faction(), kind, template.path(), corner);
         Component name = settlement.name().isEmpty() ? FactionNews.factionName(settlement.faction()) : settlement.nameComponent();
         for (ServerPlayer player : level.players()) {
            if (settlement.horizontalDistanceSquared(player.getX(), player.getZ()) <= NEAR * NEAR) {
               player.sendSystemMessage(Component.translatable("town.ancientwarfare.hall." + kind.key(), name).withStyle(ChatFormatting.GRAY));
            }
         }

         return true;
      }

      AncientWarfareCore.LOG.debug("Settlement #{} found no spot for a {} hall ({})", settlement.id(), kind, template.path());
      return false;
   }

   private static boolean loadedArea(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
      for (int x = minX; x <= maxX + 15; x += 16) {
         for (int z = minZ; z <= maxZ + 15; z += 16) {
            if (!level.isLoaded(new BlockPos(Math.min(x, maxX), 64, Math.min(z, maxZ)))) {
               return false;
            }
         }
      }

      return true;
   }

   /** The middle ground level under a hall, or null where the land is too steep or wet. */
   private static Integer hallGround(ServerLevel level, int x, int z, int sizeX, int sizeZ) {
      List<Integer> heights = new java.util.ArrayList<>();
      for (int dx = 0; dx < sizeX; dx += 2) {
         for (int dz = 0; dz < sizeZ; dz += 2) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
            BlockPos pos = new BlockPos(x + dx, y, z + dz);
            if (!level.getFluidState(pos).isEmpty() || level.getBlockEntity(pos) != null) {
               return null;
            }

            BlockState state = level.getBlockState(pos);
            // Trees are cleared with the site; anything else that is not plain ground is somebody's building.
            if (!natural(state) && !state.is(BlockTags.LOGS)) {
               return null;
            }

            heights.add(natural(state) ? y : y - 1);
         }
      }

      heights.sort(Integer::compareTo);
      return heights.get(heights.size() - 1) - heights.get(0) > HALL_MAX_SLOPE ? null : heights.get(heights.size() / 2);
   }

   /** For the debug command: the nearest town grows by one piece now. */
   public static void force(ServerLevel level, ServerPlayer player) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement nearest = null;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (FactionTownLife.isTown(settlement) && (nearest == null || settlement.horizontalDistanceSquared(player.getX(), player.getZ()) < nearest.horizontalDistanceSquared(player.getX(), player.getZ()))) {
            nearest = settlement;
         }
      }

      if (nearest == null) {
         player.sendSystemMessage(Component.literal("No town").withStyle(ChatFormatting.RED));
         return;
      }

      boolean built = grow(level, data, nearest, true);
      player.sendSystemMessage(
         Component.literal("Settlement #" + nearest.id() + " " + (built ? "grew to " + nearest.grown + " pieces" : "found no spot to grow")).withStyle(built ? ChatFormatting.GREEN : ChatFormatting.RED)
      );
   }

   /** Flat open ground just outside the town's edge, clear of other settlements and of players' eyes. */
   private static BlockPos site(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, Piece piece, RandomSource random, boolean watched) {
      BlockPos min = settlement.min();
      BlockPos max = settlement.max();
      int floor = floor(level, data, settlement);
      BlockPos fallback = null;
      for (int i = 0; i < SITE_TRIES; i++) {
         int out = GAP + random.nextInt(REACH);
         int alongX = min.getX() + random.nextInt(max.getX() - min.getX() + 1) - piece.width / 2;
         int alongZ = min.getZ() + random.nextInt(max.getZ() - min.getZ() + 1) - piece.depth / 2;
         int x;
         int z;
         switch (random.nextInt(4)) {
            case 0 -> {
               x = alongX;
               z = min.getZ() - out - piece.depth;
            }
            case 1 -> {
               x = alongX;
               z = max.getZ() + out + 1;
            }
            case 2 -> {
               x = min.getX() - out - piece.width;
               z = alongZ;
            }
            default -> {
               x = max.getX() + out + 1;
               z = alongZ;
            }
         }

         if (!loaded(level, x, z, piece) || crowded(data, x, z, piece)) {
            continue;
         }

         if (!watched && nearestPlayer(level, new BlockPos(x + piece.width / 2, 0, z + piece.depth / 2)) < TOO_CLOSE * TOO_CLOSE) {
            continue;
         }

         Integer y = flatGround(level, x, z, piece);
         if (y == null) {
            continue;
         }

         // Built on the town's own level when the ground is near it, so the town does not sit on one level and its new houses on another.
         boolean free = floor == FactionWorldData.NO_FLOOR || floor == FactionWorldData.FOLLOWS_GROUND;
         if (free || Math.abs(y - floor) <= MAX_LIFT) {
            return new BlockPos(x, free ? y : floor, z);
         }

         if (fallback == null) {
            fallback = new BlockPos(x, y, z);
         }
      }

      return fallback;
   }

   /** The level the town stands on: recorded when it was built, else the commonest ground level inside it. */
   private static int floor(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement) {
      if (settlement.floor() != FactionWorldData.NO_FLOOR) {
         return settlement.floor();
      }

      Map<Integer, Integer> counts = new HashMap<>();
      BlockPos min = settlement.min();
      BlockPos max = settlement.max();
      int step = Math.max(2, Math.max(max.getX() - min.getX(), max.getZ() - min.getZ()) / 12);
      for (int x = min.getX(); x <= max.getX(); x += step) {
         for (int z = min.getZ(); z <= max.getZ(); z += step) {
            if (!level.isLoaded(new BlockPos(x, 64, z))) {
               continue;
            }

            counts.merge(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, 1, Integer::sum);
         }
      }

      if (counts.values().stream().mapToInt(Integer::intValue).sum() < FLOOR_SAMPLES) {
         return FactionWorldData.NO_FLOOR;
      }

      int floor = counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(FactionWorldData.NO_FLOOR);
      settlement.setFloor(floor);
      data.setDirty();
      return floor;
   }

   private static boolean loaded(ServerLevel level, int x, int z, Piece piece) {
      // Wide enough for the sloped land around the piece too.
      for (int dx = -BLEND - 1; dx <= piece.width + BLEND; dx += piece.width + 2 * BLEND + 1) {
         for (int dz = -BLEND - 1; dz <= piece.depth + BLEND; dz += piece.depth + 2 * BLEND + 1) {
            if (!level.isLoaded(new BlockPos(x + dx, 64, z + dz))) {
               return false;
            }
         }
      }

      return true;
   }

   /** Whether the spot runs into a settlement, its own included. */
   private static boolean crowded(FactionWorldData data, int x, int z, Piece piece) {
      return crowded(data, x, z, piece.width, piece.depth);
   }

   private static boolean crowded(FactionWorldData data, int x, int z, int width, int depth) {
      for (FactionWorldData.Settlement other : data.settlements()) {
         if (x + width + 1 >= other.min().getX() && x - 1 <= other.max().getX() && z + depth + 1 >= other.min().getZ() && z - 1 <= other.max().getZ()) {
            return true;
         }
      }

      return false;
   }

   /** The ground level of the spot, or null where it is steep, wet, wooded or built on. */
   private static Integer flatGround(ServerLevel level, int x, int z, Piece piece) {
      int low = Integer.MAX_VALUE;
      int high = Integer.MIN_VALUE;
      long sum = 0;
      for (int dx = 0; dx < piece.width; dx++) {
         for (int dz = 0; dz < piece.depth; dz++) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
            if (!natural(level.getBlockState(new BlockPos(x + dx, y, z + dz)))) {
               return null;
            }

            low = Math.min(low, y);
            high = Math.max(high, y);
            sum += y;
         }
      }

      return high - low > MAX_SLOPE ? null : (int)Math.round((double)sum / (piece.width * piece.depth));
   }

   private static boolean natural(BlockState state) {
      return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.STONE) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.TERRACOTTA);
   }

   /** The side of a piece that looks toward the middle of the town. */
   private static Direction facing(int x, int z, BlockPos center) {
      int dx = center.getX() - x;
      int dz = center.getZ() - z;
      return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
   }

   private static void set(ServerLevel level, BlockPos pos, BlockState state) {
      level.setBlock(pos, state, Block.UPDATE_CLIENTS);
   }

   /** Levels the spot: open air above the ground, earth filled up below it. */
   private static void clear(ServerLevel level, BlockPos site, Piece piece) {
      BlockState top = level.getBlockState(site.offset(piece.width / 2, 0, piece.depth / 2));
      if (!natural(top)) {
         top = Blocks.GRASS_BLOCK.defaultBlockState();
      }

      for (int dx = 0; dx < piece.width; dx++) {
         for (int dz = 0; dz < piece.depth; dz++) {
            for (int dy = 1; dy <= Math.max(piece.height, MAX_LIFT) + 1; dy++) {
               set(level, site.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }

            set(level, site.offset(dx, 0, dz), top);
            for (int dy = -1; dy >= -MAX_LIFT - 2 && !level.getBlockState(site.offset(dx, dy, dz)).isSolidRender(level, site.offset(dx, dy, dz)); dy--) {
               set(level, site.offset(dx, dy, dz), Blocks.DIRT.defaultBlockState());
            }
         }
      }

      // The land around is sloped to the new floor, so a raised or sunken piece has no wall of earth beside it.
      BlockState fill = top.is(BlockTags.SAND) ? top : Blocks.DIRT.defaultBlockState();
      for (int dx = -BLEND; dx < piece.width + BLEND; dx++) {
         for (int dz = -BLEND; dz < piece.depth + BLEND; dz++) {
            int distance = Math.max(Math.max(-dx, dx - piece.width + 1), Math.max(-dz, dz - piece.depth + 1));
            if (distance > 0) {
               AwsWorldGenerator.blendColumn(level, site.getX() + dx, site.getZ() + dz, distance, site.getY(), top, fill);
            }
         }
      }
   }

   /** A one-room house with its door toward the middle of town, a bed, a lantern and a barrel. Returns the doorstep. */
   private static BlockPos house(ServerLevel level, BlockPos site, Direction front, Palette palette) {
      int width = Piece.HOUSE.width;
      int depth = Piece.HOUSE.depth;
      for (int dx = 0; dx < width; dx++) {
         for (int dz = 0; dz < depth; dz++) {
            boolean edgeX = dx == 0 || dx == width - 1;
            boolean edgeZ = dz == 0 || dz == depth - 1;
            set(level, site.offset(dx, 0, dz), palette.floor);
            for (int dy = 1; dy <= 3; dy++) {
               if (edgeX && edgeZ) {
                  set(level, site.offset(dx, dy, dz), palette.frame);
               } else if (edgeX || edgeZ) {
                  boolean window = dy == 2 && (edgeX ? dz == depth / 2 : dx == width / 2);
                  set(level, site.offset(dx, dy, dz), window ? Blocks.GLASS.defaultBlockState() : palette.wall);
               }
            }

            set(level, site.offset(dx, 4, dz), palette.roof.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
         }
      }

      BlockPos door = switch (front) {
         case NORTH -> site.offset(width / 2, 1, 0);
         case SOUTH -> site.offset(width / 2, 1, depth - 1);
         case WEST -> site.offset(0, 1, depth / 2);
         default -> site.offset(width - 1, 1, depth / 2);
      };
      BlockState lower = palette.door.defaultBlockState().setValue(DoorBlock.FACING, front.getOpposite());
      set(level, door, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
      set(level, door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

      // A bed in a back corner, head against the wall, away from the door.
      boolean left = front == Direction.EAST || front == Direction.SOUTH;
      int bedX = left ? 1 : width - 2;
      int bedZ = front == Direction.NORTH ? depth - 2 : 1;
      Direction toHead = front == Direction.NORTH ? Direction.SOUTH : Direction.NORTH;
      BlockPos head = site.offset(bedX, 1, bedZ);
      BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, toHead);
      set(level, head, bed.setValue(BedBlock.PART, BedPart.HEAD));
      set(level, head.relative(toHead.getOpposite()), bed.setValue(BedBlock.PART, BedPart.FOOT));
      set(level, site.offset(width - 1 - bedX, 1, bedZ), Blocks.BARREL.defaultBlockState());
      set(level, site.offset(width - 1 - bedX, 2, bedZ), Blocks.LANTERN.defaultBlockState());
      return door.below();
   }

   private static BlockPos well(ServerLevel level, BlockPos site, Palette palette) {
      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            BlockPos pos = site.offset(dx, 0, dz);
            if (dx == 1 && dz == 1) {
               set(level, pos.below(3), Blocks.COBBLESTONE.defaultBlockState());
               set(level, pos.below(2), Blocks.WATER.defaultBlockState());
               set(level, pos.below(), Blocks.WATER.defaultBlockState());
               set(level, pos, Blocks.WATER.defaultBlockState());
               continue;
            }

            set(level, pos, Blocks.COBBLESTONE.defaultBlockState());
            set(level, pos.above(), Blocks.COBBLESTONE.defaultBlockState());
            if (dx != 1 && dz != 1) {
               set(level, pos.above(2), palette.fence);
               set(level, pos.above(3), palette.fence);
            }
         }
      }

      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            set(level, site.offset(dx, 4, dz), palette.roof.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
         }
      }

      return site.offset(1, 0, 1);
   }

   private static BlockPos stall(ServerLevel level, BlockPos site, Palette palette, RandomSource random) {
      Block[] cloth = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.WHITE_WOOL};
      BlockState roof = cloth[random.nextInt(cloth.length)].defaultBlockState();
      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            if (dx != 1 && dz != 1) {
               set(level, site.offset(dx, 1, dz), palette.fence);
               set(level, site.offset(dx, 2, dz), palette.fence);
            }

            set(level, site.offset(dx, 3, dz), roof);
         }
      }

      set(level, site.offset(1, 1, 1), Blocks.BARREL.defaultBlockState());
      set(level, site.offset(1, 1, 0), Blocks.HAY_BLOCK.defaultBlockState());
      return site.offset(1, 0, 1);
   }

   private static BlockPos lamp(ServerLevel level, BlockPos site, Palette palette) {
      for (int dy = 1; dy <= 3; dy++) {
         set(level, site.above(dy), palette.fence);
      }

      set(level, site.above(4), Blocks.LANTERN.defaultBlockState());
      return site;
   }

   private static BlockPos garden(ServerLevel level, BlockPos site, Palette palette, RandomSource random) {
      BlockState crop = random.nextBoolean() ? Blocks.WHEAT.defaultBlockState() : random.nextBoolean() ? Blocks.CARROTS.defaultBlockState() : Blocks.POTATOES.defaultBlockState();
      for (int dx = 0; dx < 5; dx++) {
         for (int dz = 0; dz < 5; dz++) {
            BlockPos pos = site.offset(dx, 0, dz);
            if (dx == 0 || dz == 0 || dx == 4 || dz == 4) {
               set(level, pos, palette.frame);
            } else if (dx == 2 && dz == 2) {
               set(level, pos, Blocks.WATER.defaultBlockState());
            } else {
               set(level, pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
               set(level, pos.above(), crop.setValue(CropBlock.AGE, 2 + random.nextInt(6)));
            }
         }
      }

      return site.offset(2, 0, 2);
   }

   /** A stone watchtower with a ladder up the inside, a lookout with low walls and a lantern on top. */
   private static BlockPos tower(ServerLevel level, BlockPos site, Palette palette) {
      BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            boolean middle = dx == 1 && dz == 1;
            set(level, site.offset(dx, 0, dz), stone);
            for (int dy = 1; dy <= 6; dy++) {
               if (!middle) {
                  boolean slit = dy == 4 && (dx == 1 || dz == 1);
                  set(level, site.offset(dx, dy, dz), slit ? Blocks.AIR.defaultBlockState() : stone);
               }
            }

            set(level, site.offset(dx, 7, dz), middle ? Blocks.AIR.defaultBlockState() : stone);
            if (!middle) {
               set(level, site.offset(dx, 8, dz), Blocks.STONE_BRICK_WALL.defaultBlockState());
            }
         }
      }

      // The way in faces south; a ladder goes up the middle to the lookout.
      set(level, site.offset(1, 1, 2), Blocks.AIR.defaultBlockState());
      set(level, site.offset(1, 2, 2), Blocks.AIR.defaultBlockState());
      BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH);
      for (int dy = 1; dy <= 7; dy++) {
         set(level, site.offset(1, dy, 1), ladder);
      }

      set(level, site.offset(0, 9, 0), Blocks.LANTERN.defaultBlockState());
      set(level, site.offset(2, 9, 2), Blocks.LANTERN.defaultBlockState());
      return site.offset(1, 0, 3);
   }

   /** A trodden path from the new piece toward the middle, until it meets the town. */
   private static void path(ServerLevel level, FactionWorldData.Settlement settlement, BlockPos from, BlockPos center) {
      int x = from.getX();
      int z = from.getZ();
      for (int i = 0; i < PATH_STEPS && !settlement.contains(x, z, 0); i++) {
         BlockPos column = new BlockPos(x, 64, z);
         if (!level.isLoaded(column)) {
            return;
         }

         BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
         BlockState state = level.getBlockState(ground);
         if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)) {
            set(level, ground, Blocks.DIRT_PATH.defaultBlockState());
            BlockState above = level.getBlockState(ground.above());
            if (above.canBeReplaced() && !above.liquid()) {
               set(level, ground.above(), Blocks.AIR.defaultBlockState());
            }
         }

         int dx = center.getX() - x;
         int dz = center.getZ() - z;
         if (Math.abs(dx) >= Math.abs(dz)) {
            x += Integer.signum(dx);
         } else {
            z += Integer.signum(dz);
         }
      }
   }
}
