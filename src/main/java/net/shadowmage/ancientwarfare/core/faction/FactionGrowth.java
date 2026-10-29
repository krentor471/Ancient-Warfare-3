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
 * Settlements put up for real the buildings they have decided on and paid for (see TownBuildings):
 * while a player is somewhere around, but not watching the spot, the settlement builds one a while
 * on open ground just outside its edge, clearing the trees off it if it stands in a wood, with a
 * path back to the middle. A building is taken from the faction's own buildings of that kind where
 * it has one, else from the world's own village houses of the climate, else it is a plain building
 * fitted out for its trade: a house with a bed, a well, a field, a pen with beasts, a shrine, a
 * stretch of palisade, a watch post or a gatehouse, a workshop with the tools of its craft.
 */
public final class FactionGrowth {
   /** People each piece built before buildings had kinds made room for; see TownBuildings.room. */
   static final int ROOM_PER_PIECE = 4;
   private static final long TICK_INTERVAL = 100L;
   private static final double NEAR = 256.0;
   private static final double TOO_CLOSE = 32.0;
   /** Spots for a new building are looked for in rings out from the middle of the settlement. */
   private static final int SPOT_START = 6;
   private static final int SPOT_STEP = 5;
   private static final int SPOT_RINGS = 32;
   private static final int SPOT_TRIES = 10;
   /** Open land kept around a new building: a lane between it and its neighbours. */
   private static final int SPOT_MARGIN = 2;
   private static final int MAX_SLOPE = 2;
   /** How far a piece's ground may be raised or cut to meet the town's level. */
   private static final int MAX_LIFT = 4;
   /** How far around a piece the land is sloped to its floor. */
   private static final int BLEND = 3;
   /** Ground samples needed to judge the level of a town built before its level was recorded. */
   private static final int FLOOR_SAMPLES = 12;
   private static final int PATH_STEPS = 64;
   /** Halls wider or taller than this would not sit beside a town's edge. */
   private static final int HALL_MAX_FOOTPRINT = 36;
   private static final int HALL_MAX_HEIGHT = 32;
   /** The most the ground under a hall may rise and fall before it is levelled. */
   private static final int HALL_MAX_SLOPE = 5;
   /** How high above a cleared spot trees are felled. */
   private static final int TREE_HEIGHT = 28;
   private static final List<String> HALL_EXCLUDED = List.of("ship", "boat", "ruin", "camp", "infested", "portal", "spawner", "islands", "tomb", "beach", "coastal");
   private static final Map<String, List<String>> HALL_TEMPLATES = new HashMap<>();
   private static final Map<String, List<net.minecraft.resources.ResourceLocation>> VANILLA_TEMPLATES = new HashMap<>();
   private static final String[] STYLES = {"plains", "desert", "savanna", "snowy", "taiga"};

   private FactionGrowth() {
   }

   enum Piece {
      HOUSE(7, 6, 5),
      WELL(3, 3, 5),
      STALL(3, 3, 4),
      LAMP(1, 1, 5),
      GARDEN(5, 5, 2),
      /** A stone watchtower. */
      TOWER(3, 3, 9),
      FIELD(9, 7, 2),
      PEN(7, 7, 3),
      SHRINE(3, 3, 4),
      PALISADE(7, 2, 5),
      WATCH_POST(3, 3, 8),
      GATEHOUSE(7, 3, 7),
      /** A plain hall fitted out for whatever trade it houses. */
      HALL(9, 7, 6),
      /** A mine head over a shaft down to a gallery of ore; see FactionTowns.digMine. */
      MINE(7, 7, 4);

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

   /** Daily: purses, buildings' yield and new buildings decided on; see TownBuildings. */
   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         // Growth earned before buildings had kinds: that many houses.
         while (settlement.growth > 0 && settlement.plan().size() < TownBuildings.MAX_WAITING) {
            settlement.plan().add(TownBuildings.Kind.HOUSE.key());
            settlement.growth--;
         }

         settlement.growth = 0;
      }

      TownBuildings.simulateDay(level, data);
   }

   private static boolean builds(FactionWorldData.Settlement settlement) {
      return settlement.built() && !settlement.plan().isEmpty() && !FactionDiplomacy.isOutlaw(settlement.faction()) && !FactionAI.isFort(settlement);
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % TICK_INTERVAL != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (builds(settlement) && nearestPlayer(level, settlement.center()) <= NEAR * NEAR && grow(level, data, settlement, false)) {
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

   /** The halls other parts of the mod look for in a town, by the building kind's key. */
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

   /** Puts up the settlement's next waiting building, if a fitting spot is found. */
   private static boolean grow(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, boolean watched) {
      TownBuildings.Kind kind = TownBuildings.Kind.byKey(settlement.plan().get(0));
      if (kind == null) {
         settlement.plan().remove(0);
         return false;
      }

      RandomSource random = level.getRandom();
      boolean built = false;
      List<String> own = kind.words.length == 0 ? List.of() : hallTemplates(settlement.faction(), kind);
      if (!own.isEmpty() && random.nextInt(3) > 0) {
         built = hall(level, data, settlement, kind, own, watched);
      }

      if (!built && !kind.vanilla.isEmpty()) {
         built = vanilla(level, data, settlement, kind, watched);
      }

      if (!built) {
         built = piece(level, data, settlement, kind, watched);
      }

      if (!built) {
         return false;
      }

      settlement.plan().remove(0);
      settlement.buildings().add(kind.key());
      settlement.grown++;
      data.setDirty();
      Component name = settlement.name().isEmpty() ? FactionNews.factionName(settlement.faction()) : settlement.nameComponent();
      for (ServerPlayer player : level.players()) {
         if (settlement.horizontalDistanceSquared(player.getX(), player.getZ()) <= NEAR * NEAR) {
            player.sendSystemMessage(Component.translatable("town.ancientwarfare.built", name, kind.title()).withStyle(ChatFormatting.GRAY));
         }
      }

      return true;
   }

   /** A plain piece of the settlement's own making for this kind of building. */
   private static boolean piece(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, TownBuildings.Kind kind, boolean watched) {
      Piece piece = kind.piece;
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
         case FIELD -> field(level, site, palette, random);
         case PEN -> pen(level, site, palette, random);
         case SHRINE -> shrine(level, site, random);
         case PALISADE -> palisade(level, site);
         case WATCH_POST -> watchPost(level, site, palette);
         case GATEHOUSE -> gatehouse(level, site);
         case HALL -> workHall(level, site, front, palette, kind, random);
         case MINE -> {
            net.shadowmage.ancientwarfare.core.structure.FactionTowns.digMine(level, site, random);
            yield site.offset(piece.width / 2, 0, -1);
         }
      };
      path(level, settlement, door.relative(front), center);
      settlement.extend(site, site.offset(piece.width - 1, piece.height, piece.depth - 1));
      if (piece == Piece.HALL) {
         settlement.halls().add(new FactionWorldData.Hall(kind.key(), site, site.offset(piece.width - 1, piece.height, piece.depth - 1)));
      }

      AncientWarfareCore.LOG.info("Settlement #{} of {} grows: a {} ({}) at {}", settlement.id(), settlement.faction(), kind, piece, site);
      return true;
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

   private static synchronized List<String> hallTemplates(String faction, TownBuildings.Kind kind) {
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

   /** The world's own village houses of a climate and kind: "village/plains/houses/plains_library_1" for library. */
   private static synchronized List<net.minecraft.resources.ResourceLocation> vanillaTemplates(ServerLevel level, String style, String kind) {
      return VANILLA_TEMPLATES.computeIfAbsent(style + "/" + kind, key -> {
         String prefix = "village/" + style + "/houses/";
         return level.getStructureManager()
            .listTemplates()
            .filter(id -> id.getNamespace().equals("minecraft") && id.getPath().startsWith(prefix) && id.getPath().substring(prefix.length()).contains(kind))
            .sorted()
            .toList();
      });
   }

   /** The climate whose village houses suit a spot. */
   private static String style(ServerLevel level, BlockPos pos) {
      net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(pos);
      if (biome.is(net.minecraft.world.level.biome.Biomes.DESERT) || level.getBlockState(pos).is(BlockTags.SAND)) {
         return "desert";
      }

      if (biome.is(net.minecraft.tags.BiomeTags.IS_SAVANNA)) {
         return "savanna";
      }

      if (biome.value().coldEnoughToSnow(pos)) {
         return "snowy";
      }

      if (biome.is(net.minecraft.tags.BiomeTags.IS_TAIGA)) {
         return "taiga";
      }

      return "plains";
   }

   /** Puts up one of the world's own village houses of this kind just outside the settlement. */
   private static boolean vanilla(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, TownBuildings.Kind kind, boolean watched) {
      RandomSource random = level.getRandom();
      String style = style(level, settlement.center().atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, settlement.center().getX(), settlement.center().getZ()) - 1));
      List<net.minecraft.resources.ResourceLocation> ids = vanillaTemplates(level, style, kind.vanilla);
      if (ids.isEmpty()) {
         ids = vanillaTemplates(level, STYLES[0], kind.vanilla);
      }

      if (ids.isEmpty()) {
         return false;
      }

      net.minecraft.resources.ResourceLocation id = ids.get(random.nextInt(ids.size()));
      net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template = level.getStructureManager().get(id).orElse(null);
      if (template == null) {
         return false;
      }

      BlockPos center = settlement.center();
      int floor = floor(level, data, settlement);
      net.minecraft.world.level.block.Rotation rotation = net.minecraft.world.level.block.Rotation.getRandom(random);
      net.minecraft.core.Vec3i turned = template.getSize(rotation);
      Spot spot = findSpot(level, data, settlement, turned.getX(), turned.getZ(), HALL_MAX_SLOPE, random, watched);
      if (spot != null) {
         int x = spot.x;
         int z = spot.z;
         int ground = spot.ground;
         boolean free = floor == FactionWorldData.NO_FLOOR || floor == FactionWorldData.FOLLOWS_GROUND;
         if (!free && Math.abs(ground - floor) <= MAX_LIFT) {
            ground = floor;
         }

         BlockPos corner = new BlockPos(x, ground, z);
         levelSpot(level, corner, turned.getX(), turned.getZ(), turned.getY());
         net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
            .setRotation(rotation)
            .addProcessor(net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor.INSTANCE)
            .addProcessor(net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor.STRUCTURE_BLOCK);
         // The template turns about its own corner; it is moved so its turned box starts at the spot's corner.
         net.minecraft.world.level.levelgen.structure.BoundingBox box = template.getBoundingBox(settings, BlockPos.ZERO);
         BlockPos origin = corner.offset(-box.minX(), 0, -box.minZ());
         template.placeInWorld(level, origin, origin, settings, random, Block.UPDATE_CLIENTS);
         BlockPos top = corner.offset(turned.getX() - 1, turned.getY(), turned.getZ() - 1);
         settlement.extend(corner, top);
         settlement.halls().add(new FactionWorldData.Hall(kind.key(), corner, top));
         BlockPos side = new BlockPos(
            Math.max(corner.getX() - 1, Math.min(top.getX() + 1, center.getX())), ground, Math.max(corner.getZ() - 1, Math.min(top.getZ() + 1, center.getZ()))
         );
         path(level, settlement, side, center);
         AncientWarfareCore.LOG.info("Settlement #{} of {} grows: a {} ({}) at {}", settlement.id(), settlement.faction(), kind, id, corner);
         return true;
      }

      return false;
   }

   /** Levels a spot for a building: the trees on and over it felled, open air above the ground, earth filled up below it, the land around sloped to it. */
   private static void levelSpot(ServerLevel level, BlockPos corner, int sizeX, int sizeZ, int height) {
      fellTrees(level, corner, sizeX, sizeZ);
      BlockState top = level.getBlockState(corner.offset(sizeX / 2, 0, sizeZ / 2));
      if (!natural(top)) {
         top = Blocks.GRASS_BLOCK.defaultBlockState();
      }

      for (int dx = 0; dx < sizeX; dx++) {
         for (int dz = 0; dz < sizeZ; dz++) {
            for (int dy = 1; dy <= height + 2; dy++) {
               set(level, corner.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }

            set(level, corner.offset(dx, 0, dz), top);
            for (int dy = -1; dy >= -MAX_LIFT - 2 && !level.getBlockState(corner.offset(dx, dy, dz)).isSolidRender(level, corner.offset(dx, dy, dz)); dy--) {
               set(level, corner.offset(dx, dy, dz), Blocks.DIRT.defaultBlockState());
            }
         }
      }

      BlockState fill = top.is(BlockTags.SAND) ? top : Blocks.DIRT.defaultBlockState();
      for (int dx = -BLEND; dx < sizeX + BLEND; dx++) {
         for (int dz = -BLEND; dz < sizeZ + BLEND; dz++) {
            int distance = Math.max(Math.max(-dx, dx - sizeX + 1), Math.max(-dz, dz - sizeZ + 1));
            if (distance > 0) {
               AwsWorldGenerator.blendColumn(level, corner.getX() + dx, corner.getZ() + dz, distance, corner.getY(), top, fill);
            }
         }
      }
   }

   /** Fells the trees standing on a spot and just around it: logs and leaves up to a tall tree's height. */
   private static void fellTrees(ServerLevel level, BlockPos corner, int sizeX, int sizeZ) {
      for (int dx = -2; dx < sizeX + 2; dx++) {
         for (int dz = -2; dz < sizeZ + 2; dz++) {
            for (int dy = -1; dy <= TREE_HEIGHT; dy++) {
               BlockPos pos = corner.offset(dx, dy, dz);
               BlockState state = level.getBlockState(pos);
               if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.VINE) || state.is(Blocks.BEE_NEST)) {
                  set(level, pos, Blocks.AIR.defaultBlockState());
               }
            }
         }
      }
   }

   /** Puts up one of the faction's buildings of this kind just outside the town, if a spot is found. */
   private static boolean hall(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, TownBuildings.Kind kind, List<String> paths, boolean watched) {
      RandomSource random = level.getRandom();
      AwsWorldGenTemplateRegistry.Template template = AwsWorldGenTemplateRegistry.templateForActivePath(paths.get(random.nextInt(paths.size())));
      if (template == null) {
         return false;
      }

      BlockPos center = settlement.center();
      int floor = floor(level, data, settlement);
      int span = Math.max(template.sizeX(), template.sizeZ());
      Spot spot = findSpot(level, data, settlement, span, span, HALL_MAX_SLOPE, random, watched);
      if (spot != null) {
         int x = spot.x;
         int z = spot.z;
         Direction facing = facing(x + span / 2, z + span / 2, center);
         int sizeX = template.rotatedSizeX(facing);
         int sizeZ = template.rotatedSizeZ(facing);
         Integer ground = spot.ground;
         boolean free = floor == FactionWorldData.NO_FLOOR || floor == FactionWorldData.FOLLOWS_GROUND;
         if (!free && Math.abs(ground - floor) <= MAX_LIFT) {
            ground = floor;
         }

         // Only the template's height is taken from its origin: its foundation sinks below the ground.
         BlockPos corner = new BlockPos(x, template.minFor(new BlockPos(x, ground + 1, z), facing).getY(), z);
         int maxX = x + sizeX - 1;
         int maxZ = z + sizeZ - 1;

         fellTrees(level, new BlockPos(x, ground, z), sizeX, sizeZ);
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
         AncientWarfareCore.LOG.info("Settlement #{} of {} grows: a {} ({}) at {}", settlement.id(), settlement.faction(), kind, template.path(), corner);
         return true;
      }

      AncientWarfareCore.LOG.debug("Settlement #{} found no spot for a {} ({})", settlement.id(), kind, template.path());
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

   /** For the debug command: the nearest settlement decides on a building if it has none waiting, and puts it up now. */
   public static void force(ServerLevel level, ServerPlayer player) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement nearest = null;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.built()
            && !FactionAI.isFort(settlement)
            && !FactionDiplomacy.isOutlaw(settlement.faction())
            && (nearest == null || settlement.horizontalDistanceSquared(player.getX(), player.getZ()) < nearest.horizontalDistanceSquared(player.getX(), player.getZ()))) {
            nearest = settlement;
         }
      }

      if (nearest == null) {
         player.sendSystemMessage(Component.literal("No settlement").withStyle(ChatFormatting.RED));
         return;
      }

      if (nearest.plan().isEmpty()) {
         TownBuildings.Kind next = TownBuildings.choose(nearest, TownBuildings.all(nearest), level.getRandom());
         if (next != null) {
            nearest.plan().add(next.key());
         }
      }

      String what = nearest.plan().isEmpty() ? "nothing" : nearest.plan().get(0);
      boolean built = !nearest.plan().isEmpty() && grow(level, data, nearest, true);
      player.sendSystemMessage(
         Component.literal("Settlement #" + nearest.id() + " " + (built ? "built a " + what + ", " + TownBuildings.total(nearest) + " buildings" : "found no spot for a " + what))
            .withStyle(built ? ChatFormatting.GREEN : ChatFormatting.RED)
      );
   }

   /** Flat open ground just outside the town's edge, clear of other settlements and of players' eyes. */
   private static BlockPos site(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, Piece piece, RandomSource random, boolean watched) {
      Spot spot = findSpot(level, data, settlement, piece.width, piece.depth, MAX_SLOPE, random, watched);
      if (spot == null) {
         return null;
      }

      // Built on the town's own level when the ground is near it, so the town does not sit on one level and its new houses on another.
      int floor = floor(level, data, settlement);
      boolean free = floor == FactionWorldData.NO_FLOOR || floor == FactionWorldData.FOLLOWS_GROUND;
      return new BlockPos(spot.x, !free && Math.abs(spot.ground - floor) <= MAX_LIFT ? floor : spot.ground, spot.z);
   }

   private record Spot(int x, int z, int ground) {
   }

   /**
    * Open ground for a building of this size as near the middle of the settlement as there is any:
    * rings outward from the middle, the first spot whose ground and a margin around it are free of
    * other buildings, water and steep slopes. Trees do not count against a spot; they are felled.
    */
   private static Spot findSpot(
      ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, int sizeX, int sizeZ, int maxSlope, RandomSource random, boolean watched
   ) {
      BlockPos center = settlement.center();
      for (int ring = 0; ring < SPOT_RINGS; ring++) {
         int radius = SPOT_START + ring * SPOT_STEP;
         double turn = random.nextDouble() * Math.PI * 2.0;
         for (int i = 0; i < SPOT_TRIES; i++) {
            double angle = turn + Math.PI * 2.0 * i / SPOT_TRIES;
            int x = center.getX() + (int)Math.round(Math.cos(angle) * radius) - sizeX / 2;
            int z = center.getZ() + (int)Math.round(Math.sin(angle) * radius) - sizeZ / 2;
            if (!loadedArea(level, x - BLEND - 1, z - BLEND - 1, x + sizeX + BLEND, z + sizeZ + BLEND) || crowdedByOthers(data, settlement, x, z, sizeX, sizeZ)) {
               continue;
            }

            if (!watched && nearestPlayer(level, new BlockPos(x + sizeX / 2, 0, z + sizeZ / 2)) < TOO_CLOSE * TOO_CLOSE) {
               continue;
            }

            Integer ground = openGround(level, x, z, sizeX, sizeZ, maxSlope);
            if (ground != null) {
               return new Spot(x, z, ground);
            }
         }
      }

      return null;
   }

   /** The middle ground level of a spot and a margin around it, or null where anything but open land, trees and paths is there or it is too steep. */
   private static Integer openGround(ServerLevel level, int x, int z, int sizeX, int sizeZ, int maxSlope) {
      List<Integer> heights = new java.util.ArrayList<>();
      for (int dx = -SPOT_MARGIN; dx < sizeX + SPOT_MARGIN; dx += 2) {
         for (int dz = -SPOT_MARGIN; dz < sizeZ + SPOT_MARGIN; dz += 2) {
            Integer y = groundUnderTrees(level, x + dx, z + dz);
            if (y == null || level.getBlockEntity(new BlockPos(x + dx, y, z + dz)) != null) {
               return null;
            }

            if (dx >= 0 && dz >= 0 && dx < sizeX && dz < sizeZ) {
               heights.add(y);
            }
         }
      }

      if (heights.isEmpty()) {
         return null;
      }

      heights.sort(Integer::compareTo);
      return heights.get(heights.size() - 1) - heights.get(0) > maxSlope ? null : heights.get(heights.size() / 2);
   }

   /** Whether the spot runs into another settlement. */
   private static boolean crowdedByOthers(FactionWorldData data, FactionWorldData.Settlement own, int x, int z, int width, int depth) {
      for (FactionWorldData.Settlement other : data.settlements()) {
         if (other != own && x + width + 1 >= other.min().getX() && x - 1 <= other.max().getX() && z + depth + 1 >= other.min().getZ() && z - 1 <= other.max().getZ()) {
            return true;
         }
      }

      return false;
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

   /** The level of the natural ground at a column, under any tree standing on it; null where something built or water is there. */
   private static Integer groundUnderTrees(ServerLevel level, int x, int z) {
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
      for (int i = 0; i < TREE_HEIGHT; i++, y--) {
         BlockPos pos = new BlockPos(x, y, z);
         BlockState state = level.getBlockState(pos);
         if (!level.getFluidState(pos).isEmpty()) {
            return null;
         }

         if (natural(state)) {
            return y;
         }

         if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES) && !state.canBeReplaced() && !state.is(Blocks.BEE_NEST)) {
            return null;
         }
      }

      return null;
   }

   private static boolean natural(BlockState state) {
      return state.is(Blocks.DIRT_PATH) || state.is(Blocks.GRAVEL) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.STONE) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.TERRACOTTA);
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

   /** Levels the spot: the trees felled, open air above the ground, earth filled up below it. */
   private static void clear(ServerLevel level, BlockPos site, Piece piece) {
      fellTrees(level, site, piece.width, piece.depth);
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

   /** A fenced field of crop rows either side of a water channel, a gate on the near side. */
   private static BlockPos field(ServerLevel level, BlockPos site, Palette palette, RandomSource random) {
      Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
      int width = Piece.FIELD.width;
      int depth = Piece.FIELD.depth;
      for (int dx = 0; dx < width; dx++) {
         Block crop = crops[random.nextInt(crops.length)];
         for (int dz = 0; dz < depth; dz++) {
            BlockPos pos = site.offset(dx, 0, dz);
            boolean edge = dx == 0 || dz == 0 || dx == width - 1 || dz == depth - 1;
            if (edge) {
               set(level, pos, Blocks.GRASS_BLOCK.defaultBlockState());
               set(level, pos.above(), dx == width / 2 && dz == 0 ? Blocks.OAK_FENCE_GATE.defaultBlockState() : palette.fence);
            } else if (dz == depth / 2) {
               set(level, pos, Blocks.WATER.defaultBlockState());
            } else {
               set(level, pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
               BlockState state = crop.defaultBlockState();
               if (crop instanceof CropBlock grown) {
                  state = grown.getStateForAge(random.nextInt(grown.getMaxAge() + 1));
               }

               set(level, pos.above(), state);
            }
         }
      }

      set(level, site.offset(0, 2, 0), Blocks.TORCH.defaultBlockState());
      set(level, site.offset(width - 1, 2, depth - 1), Blocks.TORCH.defaultBlockState());
      return site.offset(width / 2, 0, 0);
   }

   /** A fenced pen with a trough, a gate and a few beasts. */
   private static BlockPos pen(ServerLevel level, BlockPos site, Palette palette, RandomSource random) {
      int size = Piece.PEN.width;
      for (int dx = 0; dx < size; dx++) {
         for (int dz = 0; dz < size; dz++) {
            BlockPos pos = site.offset(dx, 0, dz);
            set(level, pos, Blocks.GRASS_BLOCK.defaultBlockState());
            if (dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1) {
               set(level, pos.above(), dx == size / 2 && dz == 0 ? Blocks.OAK_FENCE_GATE.defaultBlockState() : palette.fence);
            }
         }
      }

      set(level, site.offset(1, 1, size - 2), Blocks.HAY_BLOCK.defaultBlockState());
      set(level, site.offset(size - 2, 1, size - 2), Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
      net.minecraft.world.entity.EntityType<?>[] beasts = {
         net.minecraft.world.entity.EntityType.SHEEP, net.minecraft.world.entity.EntityType.COW, net.minecraft.world.entity.EntityType.PIG, net.minecraft.world.entity.EntityType.CHICKEN
      };
      net.minecraft.world.entity.EntityType<?> type = beasts[random.nextInt(beasts.length)];
      for (int i = 0; i < 2 + random.nextInt(3); i++) {
         net.minecraft.world.entity.Entity beast = type.create(level);
         if (beast instanceof net.minecraft.world.entity.Mob mob) {
            mob.moveTo(site.getX() + 2.5 + random.nextInt(3), site.getY() + 1, site.getZ() + 2.5 + random.nextInt(3), random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
         }
      }

      return site.offset(size / 2, 0, 0);
   }

   /** A wayside shrine: a stone plinth with candles and flowers. */
   private static BlockPos shrine(ServerLevel level, BlockPos site, RandomSource random) {
      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            set(level, site.offset(dx, 0, dz), Blocks.STONE_BRICKS.defaultBlockState());
            if ((dx == 0 || dx == 2) && (dz == 0 || dz == 2)) {
               set(level, site.offset(dx, 1, dz), Blocks.FLOWER_POT.defaultBlockState());
            }
         }
      }

      set(level, site.offset(1, 1, 1), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
      set(level, site.offset(1, 2, 1), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
      set(level, site.offset(1, 3, 1), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3).setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
      Block[] flowers = {Blocks.POTTED_POPPY, Blocks.POTTED_DANDELION, Blocks.POTTED_CORNFLOWER, Blocks.POTTED_OXEYE_DAISY};
      set(level, site.offset(0, 1, 0), flowers[random.nextInt(flowers.length)].defaultBlockState());
      set(level, site.offset(2, 1, 2), flowers[random.nextInt(flowers.length)].defaultBlockState());
      return site.offset(1, 0, 3);
   }

   /** A stretch of palisade: sharpened logs, a walk behind them, torches at the ends. */
   private static BlockPos palisade(ServerLevel level, BlockPos site) {
      int length = Piece.PALISADE.width;
      for (int dx = 0; dx < length; dx++) {
         for (int dy = 1; dy <= 4; dy++) {
            set(level, site.offset(dx, dy, 0), Blocks.SPRUCE_LOG.defaultBlockState());
         }

         set(level, site.offset(dx, 5, 0), Blocks.SPRUCE_FENCE.defaultBlockState());
         set(level, site.offset(dx, 1, 1), Blocks.COARSE_DIRT.defaultBlockState());
         set(level, site.offset(dx, 2, 1), Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
      }

      set(level, site.offset(0, 3, 1), Blocks.TORCH.defaultBlockState());
      set(level, site.offset(length - 1, 3, 1), Blocks.TORCH.defaultBlockState());
      return site.offset(length / 2, 0, 1);
   }

   /** A wooden watch post: four posts, a ladder, a railed platform with a lantern. */
   private static BlockPos watchPost(ServerLevel level, BlockPos site, Palette palette) {
      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            boolean corner = dx != 1 && dz != 1;
            if (corner) {
               for (int dy = 1; dy <= 5; dy++) {
                  set(level, site.offset(dx, dy, dz), palette.frame);
               }
            }

            set(level, site.offset(dx, 6, dz), palette.wall);
            if (dx != 1 || dz != 1) {
               set(level, site.offset(dx, 7, dz), palette.fence);
            }
         }
      }

      // A board up the north side for the ladder to lean on, and a hatch through the platform.
      BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH);
      for (int dy = 1; dy <= 6; dy++) {
         set(level, site.offset(1, dy, 0), palette.wall);
         set(level, site.offset(1, dy, 1), ladder);
      }

      set(level, site.offset(1, 8, 1), Blocks.LANTERN.defaultBlockState());
      return site.offset(1, 0, 3);
   }

   /** A stone gatehouse: two towers and an arch between them with a lantern over the way through. */
   private static BlockPos gatehouse(ServerLevel level, BlockPos site) {
      BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
      int width = Piece.GATEHOUSE.width;
      int depth = Piece.GATEHOUSE.depth;
      for (int dx = 0; dx < width; dx++) {
         boolean tower = dx < 2 || dx >= width - 2;
         for (int dz = 0; dz < depth; dz++) {
            set(level, site.offset(dx, 0, dz), tower ? stone : Blocks.GRAVEL.defaultBlockState());
            for (int dy = 1; dy <= 6; dy++) {
               if (tower || dy >= 4) {
                  set(level, site.offset(dx, dy, dz), stone);
               }
            }

            if (tower || dz != 1) {
               set(level, site.offset(dx, 7, dz), (dx + dz) % 2 == 0 ? Blocks.STONE_BRICK_WALL.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
         }
      }

      set(level, site.offset(width / 2, 3, 0), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
      set(level, site.offset(width / 2, 3, depth - 1), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
      return site.offset(width / 2, 0, depth);
   }

   /** A plain hall with a door toward the middle of town, fitted out for the trade it houses. */
   private static BlockPos workHall(ServerLevel level, BlockPos site, Direction front, Palette palette, TownBuildings.Kind kind, RandomSource random) {
      int width = Piece.HALL.width;
      int depth = Piece.HALL.depth;
      for (int dx = 0; dx < width; dx++) {
         for (int dz = 0; dz < depth; dz++) {
            boolean edgeX = dx == 0 || dx == width - 1;
            boolean edgeZ = dz == 0 || dz == depth - 1;
            set(level, site.offset(dx, 0, dz), palette.floor);
            for (int dy = 1; dy <= 4; dy++) {
               if (edgeX && edgeZ) {
                  set(level, site.offset(dx, dy, dz), palette.frame);
               } else if (edgeX || edgeZ) {
                  boolean window = dy == 2 && (edgeX ? dz % 3 == 0 : dx % 3 == 1);
                  set(level, site.offset(dx, dy, dz), window ? Blocks.GLASS_PANE.defaultBlockState() : palette.wall);
               }
            }

            set(level, site.offset(dx, 5, dz), palette.roof.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            if (dx > 0 && dz > 0 && dx < width - 1 && dz < depth - 1) {
               set(level, site.offset(dx, 5, dz), palette.wall);
               set(level, site.offset(dx, 6, dz), palette.roof.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            }
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
      set(level, site.offset(width / 2, 4, depth / 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
      // The tools of its trade along the back wall, away from the door.
      Block[] tools = tools(kind);
      int backZ = front == Direction.NORTH ? depth - 2 : 1;
      for (int dx = 1; dx < width - 1; dx++) {
         Block tool = tools[(dx - 1) % tools.length];
         set(level, site.offset(dx, 1, backZ), tool.defaultBlockState());
      }

      int sideX = front == Direction.WEST ? width - 2 : 1;
      for (int dz = 2; dz < depth - 2; dz++) {
         set(level, site.offset(sideX, 1, dz), tools[random.nextInt(tools.length)].defaultBlockState());
      }

      return door.below();
   }

   /** What stands in a plain hall of each trade. */
   private static Block[] tools(TownBuildings.Kind kind) {
      return switch (kind) {
         case SMITHY, WEAPONSMITH, ARMORER, ARMORY -> new Block[]{Blocks.ANVIL, Blocks.BLAST_FURNACE, Blocks.SMITHING_TABLE, Blocks.GRINDSTONE, Blocks.BARREL};
         case FLETCHER, ARCHERY -> new Block[]{Blocks.FLETCHING_TABLE, Blocks.TARGET, Blocks.HAY_BLOCK, Blocks.BARREL};
         case MASON -> new Block[]{Blocks.STONECUTTER, Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.BARREL};
         case TANNERY, WEAVER -> new Block[]{Blocks.LOOM, Blocks.CAULDRON, Blocks.WHITE_WOOL, Blocks.BARREL};
         case CARPENTER, WORKSHOP -> new Block[]{Blocks.CRAFTING_TABLE, Blocks.OAK_LOG, Blocks.CHEST, Blocks.BARREL};
         case BUTCHER -> new Block[]{Blocks.SMOKER, Blocks.BARREL, Blocks.HAY_BLOCK};
         case FISHERY -> new Block[]{Blocks.BARREL, Blocks.WATER_CAULDRON, Blocks.SMOKER};
         case MILL, GRANARY, WAREHOUSE -> new Block[]{Blocks.HAY_BLOCK, Blocks.BARREL, Blocks.CHEST, Blocks.COMPOSTER};
         case BAKERY -> new Block[]{Blocks.FURNACE, Blocks.SMOKER, Blocks.BARREL, Blocks.CRAFTING_TABLE};
         case BREWERY, TAVERN -> new Block[]{Blocks.BARREL, Blocks.BREWING_STAND, Blocks.CAULDRON, Blocks.OAK_STAIRS};
         case LIBRARY, SCHOOL -> new Block[]{Blocks.BOOKSHELF, Blocks.LECTERN, Blocks.BOOKSHELF, Blocks.ENCHANTING_TABLE};
         case CARTOGRAPHER -> new Block[]{Blocks.CARTOGRAPHY_TABLE, Blocks.BOOKSHELF, Blocks.LECTERN};
         case ALCHEMIST -> new Block[]{Blocks.BREWING_STAND, Blocks.CAULDRON, Blocks.BOOKSHELF, Blocks.FLOWER_POT};
         case TEMPLE, MONASTERY -> new Block[]{Blocks.LECTERN, Blocks.CANDLE, Blocks.CHISELED_STONE_BRICKS, Blocks.BOOKSHELF};
         case BATHHOUSE -> new Block[]{Blocks.WATER_CAULDRON, Blocks.CAULDRON, Blocks.SMOOTH_STONE};
         case BARRACKS -> new Block[]{Blocks.CHEST, Blocks.RED_BED, Blocks.BARREL, Blocks.GRINDSTONE};
         case STABLES -> new Block[]{Blocks.HAY_BLOCK, Blocks.BARREL, Blocks.WATER_CAULDRON};
         case MARKET, GUILD, CARAVANSERAI, TOWN_HALL -> new Block[]{Blocks.BARREL, Blocks.CHEST, Blocks.LECTERN, Blocks.BARREL};
         case ARENA -> new Block[]{Blocks.TARGET, Blocks.HAY_BLOCK, Blocks.GRINDSTONE};
         default -> new Block[]{Blocks.BARREL, Blocks.CRAFTING_TABLE, Blocks.CHEST};
      };
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
