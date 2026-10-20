package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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
 * Two Voronoi layers, each with one jittered seed point per grid square: realms (REALM_SIZE) pick a
 * faction from the climate around their seed point, which is the faction's starting capital area;
 * provinces (PROVINCE_SIZE) are the unit of land. At the start only provinces within CORE_RADIUS of a
 * realm's seed belong to that realm's faction, the rest is wild land for factions to expand into.
 * Province owners are stored once decided, so expansion and conquest can change them.
 */
public final class FactionRegions {
   public static final int REALM_SIZE = 2048;
   public static final int PROVINCE_SIZE = 512;
   public static final int CORE_RADIUS = 800;
   /** Outlaws never own land and may camp anywhere. */
   public static final Set<String> OUTLAWS = Set.of("brigand", "pirate");
   private static final long REALM_SALT = 0x5EED_FAC7L;
   private static final long PROVINCE_SALT = 0x9A0B_1C3DL;
   private static final double WILD_CHANCE = 0.15;
   private static final int TOWN_SCORE = 6;
   private static final int MIN_FACTION_TEMPLATES = 5;
   private static final int[][] SAMPLE_OFFSETS = {{0, 0}, {320, 0}, {-320, 0}, {0, 320}, {0, -320}, {560, 560}, {-560, 560}, {560, -560}, {-560, -560}};
   private static final Map<ResourceKey<Biome>, Map<String, Integer>> BIOME_SCORES = new HashMap<>();
   private static Map<String, Integer> factionTemplateCounts;

   private FactionRegions() {
   }

   public static Province provinceAt(ServerLevel level, int x, int z) {
      Cell cell = nearestCell(level.getSeed(), x, z, PROVINCE_SIZE, PROVINCE_SALT);
      FactionWorldData data = FactionWorldData.get(level);
      String owner = data.provinceOwner(cell.key());
      if (owner == null) {
         Realm realm = realmAt(level, cell.seedX(), cell.seedZ());
         long dx = (long)cell.seedX() - realm.seedX();
         long dz = (long)cell.seedZ() - realm.seedZ();
         owner = dx * dx + dz * dz <= (long)CORE_RADIUS * CORE_RADIUS ? realm.faction() : "";
         data.setProvinceOwner(cell.key(), owner);
      }

      return new Province(cell.key(), cell.seedX(), cell.seedZ(), owner);
   }

   public static String ownerAt(ServerLevel level, int x, int z) {
      return provinceAt(level, x, z).owner();
   }

   /** The realm whose capital area covers this point; its faction is who the land starts with. */
   public static Realm realmAt(ServerLevel level, int x, int z) {
      Cell cell = nearestCell(level.getSeed(), x, z, REALM_SIZE, REALM_SALT);
      FactionWorldData data = FactionWorldData.get(level);
      String faction = data.realmFaction(cell.key());
      if (faction == null) {
         faction = pickOwner(level, cell.key(), cell.seedX(), cell.seedZ());
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
            RandomSource random = RandomSource.create(worldSeed ^ (long)(cellX + dx) * 7374110527L ^ (long)(cellZ + dz) * 9248861453L ^ salt);
            int margin = size / 6;
            int seedX = (cellX + dx) * size + margin + random.nextInt(size - 2 * margin);
            int seedZ = (cellZ + dz) * size + margin + random.nextInt(size - 2 * margin);
            long ox = (long)seedX - x;
            long oz = (long)seedZ - z;
            long distance = ox * ox + oz * oz;
            if (distance < bestDistance) {
               bestDistance = distance;
               best = new Cell(ChunkPos.asLong(cellX + dx, cellZ + dz), seedX, seedZ);
            }
         }
      }

      return best;
   }

   private static String pickOwner(ServerLevel level, long key, int seedX, int seedZ) {
      RandomSource random = RandomSource.create(level.getSeed() ^ key * 6364136223846793005L ^ 0x0A3E_2B1DL);
      if (random.nextDouble() < WILD_CHANCE) {
         return "";
      }

      Map<String, Integer> scores = new LinkedHashMap<>();

      for (int[] offset : SAMPLE_OFFSETS) {
         Holder<Biome> biome = level.getUncachedNoiseBiome((seedX + offset[0]) >> 2, 64 >> 2, (seedZ + offset[1]) >> 2);
         biomeScores(biome).forEach((faction, score) -> scores.merge(faction, score, Integer::sum));
      }

      // Square root, so a faction with many buildings for this climate (the Empire fits most forests)
      // is favoured without crowding out smaller cultures; factions with only a handful of buildings
      // never get a country they could not fill.
      double total = 0.0;

      for (Map.Entry<String, Integer> entry : scores.entrySet()) {
         if (factionTemplateCount(entry.getKey()) >= MIN_FACTION_TEMPLATES) {
            total += Math.sqrt(entry.getValue());
         }
      }

      if (total <= 0.0) {
         return "";
      }

      double pick = random.nextDouble() * total;

      for (Map.Entry<String, Integer> entry : scores.entrySet()) {
         if (factionTemplateCount(entry.getKey()) >= MIN_FACTION_TEMPLATES) {
            pick -= Math.sqrt(entry.getValue());
            if (pick < 0.0) {
               return entry.getKey();
            }
         }
      }

      return "";
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
