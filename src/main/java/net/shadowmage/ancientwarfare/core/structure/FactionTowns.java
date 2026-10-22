package net.shadowmage.ancientwarfare.core.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.faction.TemplateFactions;

/**
 * Town templates made up for each faction, laid out by the AWT town planner: a capital and a village.
 * Factions with real town templates (Empire, Nogg, Norska, Sarkonid) use their walled town for the
 * capital and their unwalled town, shrunk, for villages. Every other faction gets a town put together
 * from its own buildings, sorted by name into a centre piece, workshops and halls, houses, and farms
 * around the edge.
 */
public final class FactionTowns {
   public static final String PREFIX = "generated/";
   public static final String CAPITAL = "capital";
   public static final String VILLAGE = "village";
   private static final String SMALL = "-small";
   private static final int REAL_CAPITAL_CHUNKS = 12;
   private static final int REAL_VILLAGE_CHUNKS = 6;
   private static final int MIN_BLOCK = 32;
   private static final int MAX_BLOCK = 96;
   /** Buildings wider than this do not fit a town block and stay standalone. */
   private static final int MAX_FOOTPRINT = 80;
   private static final int MAX_HEIGHT = 40;
   private static final int CAPITAL_MAIN = 8;
   private static final int FIELD_SIZE = 9;
   private static final int FIELD_SPACING = 12;
   private static final int CAPITAL_FIELDS = 12;
   private static final int VILLAGE_FIELDS = 6;
   private static final int MINE_SIZE = 7;
   private static final int MINE_DEPTH = 14;
   private static final int GALLERY_RADIUS = 4;
   private static final int CAPITAL_MINES = 2;
   private static final int VILLAGE_MINES = 1;
   private static final List<Block> CROPS = List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);
   private static final int VILLAGE_MAIN = 3;
   private static final int CAPITAL_GUARDS = 2;
   private static final List<String> EXCLUDED_WORDS = List.of(
      "ship", "boat", "fleet", "ruin", "infested", "portal", "invasion", "warparty", "spawner", "mines", "islands", "wizard", "graveyard", "tomb", "mausoleum"
   );
   private static final List<String> HOUSE_WORDS = List.of("house", "hut", "home", "igloo", "cottage", "lodge", "housing", "tent", "dwelling");
   private static final List<String> FARM_WORDS = List.of("farm", "field", "barn", "pigpen", "pigsty", "pantry", "storage", "granary", "mill", "stable", "orchard", "coup", "coop");
   private static final List<String> CENTRE_WORDS = List.of("capital", "palace", "town", "city", "settlement", "village", "great", "chieftain", "temple");
   private static final Map<String, List<String>> CACHE = new HashMap<>();

   private FactionTowns() {
   }

   public static String path(String faction, boolean capital) {
      return PREFIX + (capital ? CAPITAL : VILLAGE) + "/" + faction;
   }

   /** A capital laid out like a village, for a realm with no flat land wide enough for a full one. */
   public static String smallCapitalPath(String faction) {
      return PREFIX + CAPITAL + SMALL + "/" + faction;
   }

   public static boolean isGenerated(String path) {
      return path != null && path.startsWith(PREFIX);
   }

   public static boolean isCapital(String path) {
      return isGenerated(path) && path.startsWith(PREFIX + CAPITAL + "/");
   }

   public static String factionOf(String path) {
      return path.substring(path.lastIndexOf('/') + 1);
   }

   /** Whether the faction has buildings enough for this kind of town. */
   public static boolean canBuild(String faction, boolean capital) {
      try {
         return lines(path(faction, capital)).stream().anyMatch(line -> line.endsWith(":1") || line.endsWith(":2"));
      } catch (RuntimeException exception) {
         return false;
      }
   }

   public static synchronized List<String> lines(String path) {
      return CACHE.computeIfAbsent(path, key -> {
         String faction = factionOf(key);
         boolean capital = isCapital(key) && !key.contains(SMALL + "/");
         List<String> lines = real(faction, capital);
         return lines != null ? lines : generic(faction, capital);
      });
   }

   /** The faction's own walled (capital) or unwalled (village) town, resized; null when it has none. */
   private static List<String> real(String faction, boolean capital) {
      for (AwtWorldGenTemplateRegistry.Template template : AwtWorldGenTemplateRegistry.templates()) {
         String name = template.name().toLowerCase(Locale.ROOT);
         if (TemplateFactions.ofTown(template.name()).equals(faction) && name.contains("unwalled") != capital) {
            int chunks = capital ? Math.min(REAL_CAPITAL_CHUNKS, template.chunks()) : REAL_VILLAGE_CHUNKS;
            List<String> lines = new ArrayList<>();

            for (String line : AwtTownPlacer.readLines(template.path())) {
               String lower = line.trim().toLowerCase(Locale.ROOT);
               if (lower.startsWith("minsize=") || lower.startsWith("maxsize=")) {
                  line = line.substring(0, line.indexOf('=') + 1) + chunks;
               } else if (!capital && lower.startsWith("exteriorsize=")) {
                  line = "exteriorSize=1";
               }

               lines.add(line);
            }

            return lines;
         }
      }

      return null;
   }

   private record Building(String name, int footprint, int height, int people) {
   }

   private static List<String> generic(String faction, boolean capital) {
      List<Building> civil = new ArrayList<>();
      List<Building> guards = new ArrayList<>();

      for (String path : TemplateFactions.templatesOf(faction)) {
         AwsWorldGenTemplateRegistry.Template template = AwsWorldGenTemplateRegistry.templateForActivePath(path);
         String name = fileName(path);
         String lower = name.toLowerCase(Locale.ROOT);
         if (template == null || !template.ground() || EXCLUDED_WORDS.stream().anyMatch(lower::contains)) {
            continue;
         }

         int footprint = Math.max(template.sizeX(), template.sizeZ());
         if (footprint > MAX_FOOTPRINT) {
            continue;
         }

         TemplateFactions.TemplateInfo info = TemplateFactions.info(path);
         Building building = new Building(name, footprint, template.sizeY(), info.soldiers() + info.civilians());
         (TemplateFactions.hasMilitaryName(path) ? guards : civil).add(building);
      }

      if (civil.isEmpty()) {
         // A faction of camps and forts: its camps are its homes.
         civil.addAll(guards);
         guards.clear();
      }

      List<Building> houses = new ArrayList<>();
      List<Building> farms = new ArrayList<>();
      List<Building> other = new ArrayList<>();

      for (Building building : civil) {
         String lower = building.name().toLowerCase(Locale.ROOT);
         if (HOUSE_WORDS.stream().anyMatch(lower::contains)) {
            houses.add(building);
         } else if (FARM_WORDS.stream().anyMatch(lower::contains)) {
            farms.add(building);
         } else {
            other.add(building);
         }
      }

      // The centre piece: the capital takes the grandest building, a village its humblest whole village.
      Comparator<Building> grandeur = Comparator.comparingInt((Building building) -> centreScore(building)).thenComparingInt(Building::people);
      Building centre = capital
         ? other.stream().max(grandeur).orElse(null)
         : other.stream().filter(FactionTowns::isVillage).min(Comparator.comparingInt(Building::people))
            .or(() -> other.stream().filter(building -> centreScore(building) > 0).min(Comparator.comparingInt(Building::people)))
            .orElse(null);
      other.remove(centre);
      // Towers of trees and the like would loom over the streets; only the centre piece may stand that tall.
      other.removeIf(building -> building.height() > MAX_HEIGHT);
      houses.removeIf(building -> building.height() > MAX_HEIGHT);
      other.sort(Comparator.comparingInt(Building::footprint));
      if (houses.isEmpty()) {
         houses.addAll(other.subList(0, Math.min(3, other.size())));
         other.removeAll(houses);
      }

      if (houses.isEmpty() && centre == null) {
         return List.of();
      }

      List<Building> main = new ArrayList<>(other.subList(0, Math.min(capital ? CAPITAL_MAIN : VILLAGE_MAIN, other.size())));
      guards.sort(Comparator.comparingInt(Building::footprint));
      List<Building> exterior = new ArrayList<>(farms);
      if (capital) {
         exterior.addAll(guards.subList(0, Math.min(CAPITAL_GUARDS, guards.size())));
      }

      int largest = houses.stream().mapToInt(Building::footprint).max().orElse(0);
      if (centre != null) {
         largest = Math.max(largest, centre.footprint());
      }

      for (Building building : main) {
         largest = Math.max(largest, building.footprint());
      }

      int block = Math.max(MIN_BLOCK, Math.min(MAX_BLOCK, largest + 4));
      // Always a ring outside the streets: farms, or plain fields where the faction has none.
      int exteriorSize = 1;
      // Room for two blocks a side around the crossroads, plus the farm ring.
      int chunks = (2 * block + 20 + 32 * exteriorSize + 15) / 16 + (capital ? 1 : 0);
      List<String> lines = new ArrayList<>();
      lines.add("header:");
      lines.add("name=" + faction + (capital ? "Capital" : "Village"));
      lines.add("minSize=" + chunks);
      lines.add("maxSize=" + chunks);
      lines.add("townBlockSize=" + block);
      lines.add("townPlotSize=3");
      lines.add("wallStyle=0");
      lines.add("wallSize=0");
      lines.add("exteriorSize=" + exteriorSize);
      lines.add("exteriorPlotSize=1");
      lines.add("interiorEmptyPlotChance=" + (capital ? 5 : 25));
      lines.add(":endheader");
      section(lines, "uniqueStructures", centre == null ? List.of() : List.of(centre), false);
      section(lines, "mainStructures", main, false);
      section(lines, "houseStructures", houses, true);
      section(lines, "exteriorStructures", exterior, true);
      AncientWarfareCore.LOG.info("Generated {} town for {}: {}", capital ? "capital" : "village", faction, lines);
      return lines;
   }

   /** Whether the faction has farm buildings of its own; towns of those that do not get plain fields. */
   private static boolean hasFarms(String faction) {
      for (String path : TemplateFactions.templatesOf(faction)) {
         String lower = fileName(path).toLowerCase(Locale.ROOT);
         if (FARM_WORDS.stream().anyMatch(lower::contains) && EXCLUDED_WORDS.stream().noneMatch(lower::contains)) {
            return true;
         }
      }

      return false;
   }

   /**
    * The town's working grounds on its levelled ground outside the streets: mine shafts (unless a mine
    * building already stands) and fields of crops, each a ring of farmland around a water block,
    * wherever buildings and roads left room. Factions with farm buildings get half the fields.
    */
   static void plantFields(ServerLevel level, AwtTownPlacer.TownPlan plan, int floorY, String faction, boolean capital) {
      RandomSource random = RandomSource.create(plan.min().asLong());
      List<BlockPos> spots = outerSpots(plan, floorY, random);
      boolean hasMine = plan.buildings().stream().anyMatch(placement -> placement.templatePath().toLowerCase(Locale.ROOT).contains("mine"));
      int shafts = hasMine ? 0 : capital ? CAPITAL_MINES : VILLAGE_MINES;
      int dug = 0;
      Iterator<BlockPos> iterator = spots.iterator();

      while (iterator.hasNext() && dug < shafts) {
         BlockPos corner = iterator.next();
         if (fits(level, plan, corner, MINE_SIZE, false)) {
            digMine(level, corner, random);
            iterator.remove();
            dug++;
         }
      }

      int limit = capital ? CAPITAL_FIELDS : VILLAGE_FIELDS;
      if (hasFarms(faction)) {
         limit /= 2;
      }

      int planted = 0;

      for (BlockPos corner : spots) {
         if (planted >= limit) {
            break;
         }

         if (fits(level, plan, corner, FIELD_SIZE, true)) {
            Block crop = CROPS.get(random.nextInt(CROPS.size()));
            int middle = FIELD_SIZE / 2;

            for (int x = 0; x < FIELD_SIZE; x++) {
               for (int z = 0; z < FIELD_SIZE; z++) {
                  BlockPos ground = corner.offset(x, 0, z);
                  if (x == middle && z == middle) {
                     level.setBlock(ground, Blocks.WATER.defaultBlockState(), 2);
                  } else {
                     level.setBlock(ground, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), 2);
                     BlockState plant = crop.defaultBlockState();
                     if (crop instanceof CropBlock cropBlock) {
                        plant = cropBlock.getStateForAge(random.nextInt(cropBlock.getMaxAge() + 1));
                     }

                     level.setBlock(ground.above(), plant, 2);
                  }
               }
            }

            planted++;
         }
      }

      AncientWarfareCore.LOG.info("Dug {} mine(s) and planted {} field(s) around the {} town of {}", dug, planted, capital ? "capital" : "village", faction);
   }

   /** Corners on a grid over the town's outer ring, shuffled. */
   private static List<BlockPos> outerSpots(AwtTownPlacer.TownPlan plan, int floorY, RandomSource random) {
      int inner = 7 + 16;
      int innerMinX = plan.min().getX() + inner;
      int innerMinZ = plan.min().getZ() + inner;
      int innerMaxX = plan.min().getX() + plan.totalSize() - 1 - inner;
      int innerMaxZ = plan.min().getZ() + plan.totalSize() - 1 - inner;
      List<BlockPos> spots = new ArrayList<>();

      for (int dx = 2; dx + FIELD_SIZE < plan.totalSize() - 2; dx += FIELD_SPACING) {
         for (int dz = 2; dz + FIELD_SIZE < plan.totalSize() - 2; dz += FIELD_SPACING) {
            int x = plan.min().getX() + dx;
            int z = plan.min().getZ() + dz;
            boolean inside = x + FIELD_SIZE > innerMinX && x < innerMaxX && z + FIELD_SIZE > innerMinZ && z < innerMaxZ;
            if (!inside) {
               spots.add(new BlockPos(x, floorY, z));
            }
         }
      }

      Collections.shuffle(spots, new Random(random.nextLong()));
      return spots;
   }

   /**
    * A mine head: a timber-framed shed over a ladder shaft down to a gallery whose walls show coal,
    * iron, copper and now and then gold.
    */
   private static void digMine(ServerLevel level, BlockPos corner, RandomSource random) {
      int size = MINE_SIZE;
      int middle = size / 2;
      BlockState wall = Blocks.COBBLESTONE.defaultBlockState();
      BlockState post = Blocks.SPRUCE_LOG.defaultBlockState();
      BlockState roof = Blocks.SPRUCE_SLAB.defaultBlockState();
      BlockState air = Blocks.AIR.defaultBlockState();
      int bottom = corner.getY() - MINE_DEPTH;

      BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
      int reach = GALLERY_RADIUS;

      // The gallery at the foot of the shaft, three blocks high.
      for (int x = middle - reach - 1; x <= middle + reach + 1; x++) {
         for (int z = middle - reach - 1; z <= middle + reach + 1; z++) {
            for (int y = bottom - 1; y <= bottom + 3; y++) {
               BlockPos pos = new BlockPos(corner.getX() + x, y, corner.getZ() + z);
               boolean edge = Math.abs(x - middle) > reach || Math.abs(z - middle) > reach || y == bottom - 1 || y == bottom + 3;
               if (!edge) {
                  level.setBlock(pos, air, 2);
               } else if (y == bottom - 1 || y == bottom + 3 || random.nextInt(3) != 0) {
                  level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
               } else {
                  level.setBlock(pos, ore(random), 2);
               }
            }
         }
      }

      // The shaft: 3x3 open, lined with stone so caves and water stay out.
      for (int y = bottom + 3; y <= corner.getY(); y++) {
         for (int x = middle - 2; x <= middle + 2; x++) {
            for (int z = middle - 2; z <= middle + 2; z++) {
               boolean open = Math.abs(x - middle) <= 1 && Math.abs(z - middle) <= 1;
               level.setBlock(new BlockPos(corner.getX() + x, y, corner.getZ() + z), open ? air : wall, 2);
            }
         }
      }

      // A ladder down the north face, on a timber prop through the gallery.
      for (int y = bottom; y <= corner.getY(); y++) {
         if (y < bottom + 3) {
            level.setBlock(new BlockPos(corner.getX() + middle, y, corner.getZ() + middle - 2), post, 2);
         }

         level.setBlock(new BlockPos(corner.getX() + middle, y, corner.getZ() + middle - 1), ladder, 2);
      }

      for (int i = 0; i < 4; i++) {
         int dx = i < 2 ? -reach : reach;
         int dz = i % 2 == 0 ? -reach : reach;
         level.setBlock(new BlockPos(corner.getX() + middle + dx, bottom, corner.getZ() + middle + dz), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
         level.setBlock(new BlockPos(corner.getX() + middle + dx, bottom + 1, corner.getZ() + middle + dz), Blocks.TORCH.defaultBlockState(), 2);
      }

      for (int x = middle - reach; x <= middle + reach; x++) {
         level.setBlock(new BlockPos(corner.getX() + x, bottom, corner.getZ() + middle + 2), Blocks.RAIL.defaultBlockState(), 2);
      }

      // The shed: cobble floor, log posts at the corners, a slab roof.
      for (int x = 0; x < size; x++) {
         for (int z = 0; z < size; z++) {
            boolean shaft = Math.abs(x - middle) <= 1 && Math.abs(z - middle) <= 1;
            if (!shaft && (Math.abs(x - middle) > 2 || Math.abs(z - middle) > 2)) {
               level.setBlock(corner.offset(x, 0, z), wall, 2);
            }

            for (int y = 1; y <= 3; y++) {
               level.setBlock(corner.offset(x, y, z), air, 2);
            }

            boolean cornerPost = (x == 0 || x == size - 1) && (z == 0 || z == size - 1);
            if (cornerPost) {
               for (int y = 1; y <= 3; y++) {
                  level.setBlock(corner.offset(x, y, z), post, 2);
               }
            }

            level.setBlock(corner.offset(x, 4, z), roof, 2);
         }
      }

      level.setBlock(corner.offset(1, 1, 1), Blocks.CHEST.defaultBlockState(), 2);
      level.setBlock(corner.offset(size - 2, 1, 1), Blocks.BARREL.defaultBlockState(), 2);
      level.setBlock(corner.offset(1, 3, middle), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 2);
   }

   private static BlockState ore(RandomSource random) {
      int roll = random.nextInt(20);
      Block block = roll < 9 ? Blocks.COAL_ORE : roll < 15 ? Blocks.IRON_ORE : roll < 19 ? Blocks.COPPER_ORE : Blocks.GOLD_ORE;
      return block.defaultBlockState();
   }

   /** Room for a square of this size: nothing planned on it, plain ground (grass or dirt for fields), open sky above. */
   private static boolean fits(ServerLevel level, AwtTownPlacer.TownPlan plan, BlockPos corner, int size, boolean soil) {
      for (int x = -1; x <= size; x++) {
         for (int z = -1; z <= size; z++) {
            BlockPos ground = corner.offset(x, 0, z);
            if (plan.occupiedBlocks().contains(ground.asLong()) || plan.occupiedBlocks().contains(ground.above().asLong())) {
               return false;
            }

            BlockState state = level.getBlockState(ground);
            boolean plain = state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || !soil && (state.is(Blocks.STONE) || state.is(Blocks.SAND) || state.is(Blocks.COARSE_DIRT));
            if (!plain || !level.getBlockState(ground.above()).isAir() && !level.getBlockState(ground.above()).canBeReplaced()) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean isVillage(Building building) {
      String lower = building.name().toLowerCase(Locale.ROOT);
      return lower.contains("village") || lower.contains("settlement");
   }

   private static int centreScore(Building building) {
      String lower = building.name().toLowerCase(Locale.ROOT);
      return CENTRE_WORDS.stream().anyMatch(lower::contains) ? 1 : 0;
   }

   private static void section(List<String> lines, String name, List<Building> buildings, boolean weighted) {
      lines.add(name + ":");
      buildings.forEach(building -> lines.add(building.name() + (weighted ? ":2" : ":1")));
      lines.add(":end" + name);
   }

   private static String fileName(String path) {
      String name = path.substring(path.lastIndexOf('/') + 1);
      return name.endsWith(".aws") ? name.substring(0, name.length() - 4) : name;
   }
}
