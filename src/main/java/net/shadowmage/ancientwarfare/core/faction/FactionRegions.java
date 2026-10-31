package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenTemplateRegistry;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenTemplateRegistry;

/**
 * Splits the overworld into provinces held by one faction or left wild, so a faction's structures
 * cluster into one country instead of a patchwork.
 *
 * Two Voronoi layers, each with one jittered seed point per grid square: realms (realmSize) pick a
 * faction from the climate around their seed point, which is the faction's starting capital area;
 * provinces (provinceSize) are the unit of land. At the start only provinces within coreRadius of a
 * realm's seed belong to that realm's faction, the rest is wild land for factions to expand into.
 * Province owners are stored once decided, so expansion and conquest can change them.
 */
public final class FactionRegions {
   private static final int BASE_REALM_SIZE = 2048;
   private static final int BASE_PROVINCE_SIZE = 512;
   private static final int BASE_CORE_RADIUS = 800;
   /** Sizes for the loaded world, scaled by its region scale. */
   private static volatile int scale = 100;
   /** Outlaws never own land and may camp anywhere. */
   public static final Set<String> OUTLAWS = Set.of("brigand", "pirate");
   private static final long REALM_SALT = 0x5EED_FAC7L;
   private static final long PROVINCE_SALT = 0x9A0B_1C3DL;
   private static final int TOWN_SCORE = 6;
   /** Realms around the middle of the world shared out as homes, each way. */
   private static final int HOME_CELLS = 4;
   /** How many realms from its home a country reaches at most; farther land starts wild. */
   private static final int HOME_REACH = 3;
   private static final int REALMS_PER_COUNTRY = 2;
   private static final int MIN_FACTION_TEMPLATES = 5;
   private static final int[][] SAMPLE_OFFSETS = {{0, 0}, {320, 0}, {-320, 0}, {0, 320}, {0, -320}, {560, 560}, {-560, 560}, {560, -560}, {-560, -560}};
   private static final Map<ResourceKey<Biome>, Map<String, Integer>> BIOME_SCORES = new HashMap<>();
   private static Map<String, Integer> factionTemplateCounts;

   private FactionRegions() {
   }

   static void setScale(int percent) {
      scale = percent;
   }

   /** The region scale of the loaded world, in percent. */
   public static int scale() {
      return scale;
   }

   public static int provinceSize() {
      return BASE_PROVINCE_SIZE * scale / 100;
   }

   public static int realmSize() {
      return BASE_REALM_SIZE * scale / 100;
   }

   private static int coreRadius() {
      return BASE_CORE_RADIUS * scale / 100;
   }

   public static Province provinceAt(ServerLevel level, int x, int z) {
      Cell cell = nearestCell(level.getSeed(), x, z, provinceSize(), PROVINCE_SALT);
      FactionWorldData data = FactionWorldData.get(level);
      String owner = data.provinceOwner(cell.key());
      if (owner == null) {
         Realm realm = realmAt(level, cell.seedX(), cell.seedZ());
         long dx = (long)cell.seedX() - realm.seedX();
         long dz = (long)cell.seedZ() - realm.seedZ();
         owner = dx * dx + dz * dz <= (long)coreRadius() * coreRadius() ? realm.faction() : "";
         data.setProvinceOwner(cell.key(), owner);
      }

      return new Province(cell.key(), cell.seedX(), cell.seedZ(), owner);
   }

   public static String ownerAt(ServerLevel level, int x, int z) {
      return provinceAt(level, x, z).owner();
   }

   /** The realm whose capital area covers this point; its faction is who the land starts with. */
   public static Realm realmAt(ServerLevel level, int x, int z) {
      Cell cell = nearestCell(level.getSeed(), x, z, realmSize(), REALM_SALT);
      FactionWorldData data = FactionWorldData.get(level);
      String faction = data.realmFaction(cell.key());
      if (faction == null) {
         faction = pickOwner(level, data, cell.key(), cell.seedX(), cell.seedZ());
         data.setRealmFaction(cell.key(), faction);
      }

      return new Realm(cell.key(), cell.seedX(), cell.seedZ(), faction);
   }

   /** Whether a structure of templateFaction may be generated on land held by owner. */
   public static boolean allows(String owner, String templateFaction) {
      return templateFaction.isEmpty() || OUTLAWS.contains(templateFaction) || templateFaction.equals(owner);
   }

   private static Cell nearestCell(long worldSeed, int x, int z, int size, long salt) {
      int cellX = Math.floorDiv(x, size);
      int cellZ = Math.floorDiv(z, size);
      Cell best = null;
      long bestDistance = Long.MAX_VALUE;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            Cell cell = cell(worldSeed, cellX + dx, cellZ + dz, size, salt);
            long ox = (long)cell.seedX() - x;
            long oz = (long)cell.seedZ() - z;
            long distance = ox * ox + oz * oz;
            if (distance < bestDistance) {
               bestDistance = distance;
               best = cell;
            }
         }
      }

      return best;
   }

   private static Cell cell(long worldSeed, int cellX, int cellZ, int size, long salt) {
      RandomSource random = RandomSource.create(worldSeed ^ (long)cellX * 7374110527L ^ (long)cellZ * 9248861453L ^ salt);
      int margin = size / 6;
      int seedX = cellX * size + margin + random.nextInt(size - 2 * margin);
      int seedZ = cellZ * size + margin + random.nextInt(size - 2 * margin);
      return new Cell(ChunkPos.asLong(cellX, cellZ), seedX, seedZ);
   }

   /** The province of a grid cell, decided if it was not yet. */
   public static Province provinceByCell(ServerLevel level, int cellX, int cellZ) {
      Cell cell = cell(level.getSeed(), cellX, cellZ, provinceSize(), PROVINCE_SALT);
      return provinceAt(level, cell.seedX(), cell.seedZ());
   }

   /** Whether any of faction's buildings fit the climate at this point. */
   public static boolean suits(ServerLevel level, String faction, int x, int z) {
      return biomeScores(level.getUncachedNoiseBiome(x >> 2, 64 >> 2, z >> 2)).getOrDefault(faction, 0) > 0;
   }

   private static String pickOwner(ServerLevel level, FactionWorldData data, long key, int seedX, int seedZ) {
      int sea = 0;

      for (int[] offset : SAMPLE_OFFSETS) {
         Holder<Biome> biome = level.getUncachedNoiseBiome((seedX + offset[0]) >> 2, 64 >> 2, (seedZ + offset[1]) >> 2);
         if (biome.is(BiomeTags.IS_OCEAN)) {
            sea++;
         }
      }

      if (sea * 2 > SAMPLE_OFFSETS.length) {
         // Mostly open sea: nobody's country.
         return "";
      }

      // Each country is the land grown out from its home: one piece, never scattered over the world.
      homes(level);
      return lands.getOrDefault(key, "");
   }

   private static long homesSeed;
   private static Map<String, int[]> homes;
   /** The faction of each realm cell, grown out a ring at a time from the homes over land. */
   private static Map<Long, String> lands;

   /**
    * Where each country begins, the same for every run of a world: the land realms around the
    * world's middle are shared out one per faction, each faction taking the realm that suits its
    * buildings best among those farthest from the homes already given.
    */
   static synchronized Map<String, int[]> homes(ServerLevel level) {
      if (homes != null && homesSeed == level.getSeed()) {
         return homes;
      }

      List<String> factions = new ArrayList<>();
      for (AwsWorldGenTemplateRegistry.Template template : AwsWorldGenTemplateRegistry.templates()) {
         String faction = TemplateFactions.ofTemplate(template.path());
         if (!faction.isEmpty() && !OUTLAWS.contains(faction) && !factions.contains(faction) && factionTemplateCount(faction) >= MIN_FACTION_TEMPLATES) {
            factions.add(faction);
         }
      }

      factions.sort(java.util.Comparator.comparingInt(FactionRegions::factionTemplateCount).reversed().thenComparing(faction -> faction));
      List<Cell> land = new ArrayList<>();
      Map<Cell, Map<String, Integer>> fit = new HashMap<>();
      for (int cx = -HOME_CELLS; cx < HOME_CELLS; cx++) {
         for (int cz = -HOME_CELLS; cz < HOME_CELLS; cz++) {
            Cell cell = cell(level.getSeed(), cx, cz, realmSize(), REALM_SALT);
            Map<String, Integer> scores = new HashMap<>();
            int sea = 0;
            for (int[] offset : SAMPLE_OFFSETS) {
               Holder<Biome> biome = level.getUncachedNoiseBiome((cell.seedX() + offset[0]) >> 2, 64 >> 2, (cell.seedZ() + offset[1]) >> 2);
               if (biome.is(BiomeTags.IS_OCEAN)) {
                  sea++;
               } else {
                  biomeScores(biome).forEach((faction, score) -> scores.merge(faction, score, Integer::sum));
               }
            }

            if (sea * 2 <= SAMPLE_OFFSETS.length) {
               land.add(cell);
               fit.put(cell, scores);
            }
         }
      }

      Map<String, int[]> placed = new LinkedHashMap<>();
      // Room for every country to hold a few realms: on a small world fewer countries, not crumbs of many.
      int countries = Math.max(2, land.size() / REALMS_PER_COUNTRY);
      for (String faction : factions) {
         if (placed.size() >= countries) {
            break;
         }

         Cell best = null;
         double bestValue = -1.0;
         for (Cell cell : land) {
            double apart = Double.MAX_VALUE;
            for (int[] other : placed.values()) {
               apart = Math.min(apart, Math.hypot(other[0] - cell.seedX(), other[1] - cell.seedZ()));
            }

            // Away from the others first, then the climate its buildings fit.
            double value = (placed.isEmpty() ? 1.0 : apart) * (1.0 + Math.sqrt(fit.get(cell).getOrDefault(faction, 0)));
            if (value > bestValue && !taken(placed, cell)) {
               bestValue = value;
               best = cell;
            }
         }

         if (best != null) {
            placed.put(faction, new int[]{best.seedX(), best.seedZ()});
         }
      }

      // From every home at once, one step at a time to the land beside it, so countries meet at borders.
      Map<Long, String> owned = new HashMap<>();
      java.util.Set<Long> landKeys = new java.util.HashSet<>();
      land.forEach(cell -> landKeys.add(cell.key()));
      List<Long> ring = new ArrayList<>();
      for (Map.Entry<String, int[]> home : placed.entrySet()) {
         long key = ChunkPos.asLong(Math.floorDiv(home.getValue()[0], realmSize()), Math.floorDiv(home.getValue()[1], realmSize()));
         owned.put(key, home.getKey());
         ring.add(key);
      }

      for (int step = 0; step < HOME_REACH && !ring.isEmpty(); step++) {
         List<Long> next = new ArrayList<>();
         for (long key : ring) {
            int x = ChunkPos.getX(key);
            int z = ChunkPos.getZ(key);
            for (int[] side : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
               long beside = ChunkPos.asLong(x + side[0], z + side[1]);
               if (landKeys.contains(beside) && !owned.containsKey(beside)) {
                  owned.put(beside, owned.get(key));
                  next.add(beside);
               }
            }
         }

         ring = next;
      }

      lands = owned;
      net.shadowmage.ancientwarfare.core.AncientWarfareCore.LOG.info("Country homes {}; lands {}", placed.keySet(), owned.entrySet().stream().map(e -> ChunkPos.getX(e.getKey()) + "," + ChunkPos.getZ(e.getKey()) + "=" + e.getValue()).toList());
      homes = placed;
      homesSeed = level.getSeed();
      return placed;
   }

   private static boolean taken(Map<String, int[]> placed, Cell cell) {
      return placed.values().stream().anyMatch(home -> home[0] == cell.seedX() && home[1] == cell.seedZ());
   }

   /** Ground world-generation templates per faction, in any climate. */
   private static synchronized int factionTemplateCount(String faction) {
      if (factionTemplateCounts == null) {
         Map<String, Integer> counts = new HashMap<>();

         for (AwsWorldGenTemplateRegistry.Template template : AwsWorldGenTemplateRegistry.templates()) {
            if (template.ground()) {
               counts.merge(TemplateFactions.ofTemplate(template.path()), 1, Integer::sum);
            }
         }

         factionTemplateCounts = counts;
      }

      return factionTemplateCounts.getOrDefault(faction, 0);
   }

   /** How well each faction's buildings fit a biome: one point per ground template, more for a town. */
   private static synchronized Map<String, Integer> biomeScores(Holder<Biome> biome) {
      ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
      Map<String, Integer> cached = key == null ? null : BIOME_SCORES.get(key);
      if (cached != null) {
         return cached;
      }

      Map<String, Integer> scores = new LinkedHashMap<>();

      for (AwsWorldGenTemplateRegistry.Template template : AwsWorldGenTemplateRegistry.templates()) {
         if (template.ground() && template.acceptsBiome(biome)) {
            String faction = TemplateFactions.ofTemplate(template.path());
            if (!faction.isEmpty() && !OUTLAWS.contains(faction)) {
               scores.merge(faction, 1, Integer::sum);
            }
         }
      }

      for (AwtWorldGenTemplateRegistry.Template template : AwtWorldGenTemplateRegistry.templates()) {
         String faction = TemplateFactions.ofTown(template.name());
         if (!faction.isEmpty() && template.acceptsBiome(biome)) {
            scores.merge(faction, TOWN_SCORE, Integer::sum);
         }
      }

      if (key != null) {
         BIOME_SCORES.put(key, scores);
      }

      return scores;
   }

   private static record Cell(long key, int seedX, int seedZ) {
   }

   public static record Realm(long key, int seedX, int seedZ, String faction) {
   }

   public static record Province(long key, int seedX, int seedZ, String owner) {
      public int cellX() {
         return ChunkPos.getX(this.key);
      }

      public int cellZ() {
         return ChunkPos.getZ(this.key);
      }
   }
}
