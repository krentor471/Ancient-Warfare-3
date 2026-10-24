package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/**
 * A map of the faction world around its centre (0, 0), for testing: who owns which land, where the
 * settlements, capitals and roads are. Operators and creative players can teleport by clicking it.
 */
public final class WorldMap {
   /** Blocks per map cell, and cells across the map. */
   public static final int STEP = 64;
   public static final int SIZE = 200;
   public static final int TELEPORT_Y = 90;
   /** How far off a marching army can be seen without spies. */
   private static final double SIGHT_RANGE = 160.0;
   /** With cartography: how far the player sees armies, from where they stand and from each of their villages. */
   private static final double SURVEYED_RANGE = 480.0;

   private static boolean sees(ServerLevel level, FactionWorldData data, ServerPlayer player, int x, int z) {
      if (player.distanceToSqr(x, player.getY(), z) <= SIGHT_RANGE * SIGHT_RANGE) {
         return true;
      } else if (!net.shadowmage.ancientwarfare.core.research.PlayerResearch.has(player, net.shadowmage.ancientwarfare.core.research.PlayerResearch.CARTOGRAPHY)) {
         return false;
      }

      if (player.distanceToSqr(x, player.getY(), z) <= SURVEYED_RANGE * SURVEYED_RANGE) {
         return true;
      }

      for (Map.Entry<BlockPos, java.util.UUID> village : data.villages().entrySet()) {
         if (player.getUUID().equals(village.getValue()) && village.getKey().distToCenterSqr(x, village.getKey().getY(), z) <= SURVEYED_RANGE * SURVEYED_RANGE) {
            return true;
         }
      }

      return false;
   }
   private static final int ROAD_POINT_STRIDE = 4;
   private static final List<String> MINE_WORDS = List.of("mine", "quarry", "smelt", "forge", "dig");
   private static final List<String> FARM_WORDS = List.of("farm", "field", "barn", "pig", "granary", "mill", "orchard", "coop", "coup", "ranch", "pasture", "stable");
   private static final List<String> TEMPLE_WORDS = List.of("temple", "shrine", "church", "chapel", "cathed", "monast", "altar", "reliqu", "abbey", "totem");
   private static final List<String> VILLAGE_WORDS = List.of("village", "settlement", "town", "hamlet");

   /** What the land of a map cell is, from the biome at its middle. */
   public static final byte TERRAIN_LAND = 0;
   public static final byte TERRAIN_WATER = 1;
   public static final byte TERRAIN_DEEP_WATER = 2;
   public static final byte TERRAIN_FOREST = 3;
   public static final byte TERRAIN_MOUNTAIN = 4;
   public static final byte TERRAIN_DESERT = 5;
   public static final byte TERRAIN_SNOW = 6;
   /** River pixels per cell side: the river mask is SIZE * RIVER_DETAIL across, a pixel every 16 blocks. */
   public static final int RIVER_DETAIL = 4;
   private static final int RIVER_STEP = STEP / RIVER_DETAIL;
   /** The surveyed terrain and rivers of each level's map; they never change, so they are surveyed once, off the server thread. */
   private static final Map<String, CompletableFuture<Survey>> TERRAIN = new ConcurrentHashMap<>();
   private static final Survey SURVEYING = new Survey(new byte[0], new byte[0]);

   /** terrain per cell, and the river mask as a deflated bitset, row by row. */
   private record Survey(byte[] terrain, byte[] rivers) {
   }

   private WorldMap() {
   }

   /** The level's map survey, or empty arrays while it is still running. */
   private static Survey survey(ServerLevel level, int originX, int originZ) {
      CompletableFuture<Survey> survey = TERRAIN.computeIfAbsent(
         level.dimension().location() + "@" + level.getSeed(), key -> CompletableFuture.supplyAsync(() -> doSurvey(level, originX, originZ))
      );
      return survey.getNow(SURVEYING);
   }

   private static Survey doSurvey(ServerLevel level, int originX, int originZ) {
      byte[] terrain = new byte[SIZE * SIZE];
      BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
      Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
      int quartY = QuartPos.fromBlock(level.getSeaLevel());

      for (int row = 0; row < SIZE; row++) {
         for (int column = 0; column < SIZE; column++) {
            int x = originX + column * STEP + STEP / 2;
            int z = originZ + row * STEP + STEP / 2;
            Holder<Biome> biome = source.getNoiseBiome(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z), sampler);
            // A river is drawn from the river mask; the cell itself is the land along it.
            for (int i = 0; i < RIVER_BANKS.length && biome.is(BiomeTags.IS_RIVER); i++) {
               biome = source.getNoiseBiome(QuartPos.fromBlock(x + RIVER_BANKS[i][0]), quartY, QuartPos.fromBlock(z + RIVER_BANKS[i][1]), sampler);
            }

            terrain[row * SIZE + column] = biome.is(BiomeTags.IS_RIVER) ? TERRAIN_LAND : terrainOf(biome);
         }
      }

      // Rivers every 16 blocks, from four samples each, so narrow ones are not missed.
      int size = SIZE * RIVER_DETAIL;
      byte[] mask = new byte[(size * size + 7) / 8];
      for (int py = 0; py < size; py++) {
         for (int px = 0; px < size; px++) {
            for (int sample = 0; sample < 4; sample++) {
               int x = originX + px * RIVER_STEP + (sample % 2 == 0 ? 4 : 12);
               int z = originZ + py * RIVER_STEP + (sample < 2 ? 4 : 12);
               if (source.getNoiseBiome(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z), sampler).is(BiomeTags.IS_RIVER)) {
                  int index = py * size + px;
                  mask[index >> 3] |= (byte)(1 << (index & 7));
                  break;
               }
            }
         }
      }

      return new Survey(terrain, deflate(mask));
   }

   /** Where to look for the land beside a river crossing a cell's middle. */
   private static final int[][] RIVER_BANKS = {{24, 0}, {-24, 0}, {0, 24}, {0, -24}, {24, 24}, {-24, -24}, {24, -24}, {-24, 24}};

   private static byte[] deflate(byte[] bytes) {
      java.util.zip.Deflater deflater = new java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION);
      deflater.setInput(bytes);
      deflater.finish();
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      while (!deflater.finished()) {
         out.write(buffer, 0, deflater.deflate(buffer));
      }

      deflater.end();
      return out.toByteArray();
   }

   /** The river mask from the map data: a bit per pixel, SIZE * RIVER_DETAIL across; empty when not yet surveyed. */
   public static boolean[] rivers(byte[] deflated) {
      int size = SIZE * RIVER_DETAIL;
      if (deflated.length == 0) {
         return new boolean[0];
      }

      byte[] mask = new byte[(size * size + 7) / 8];
      java.util.zip.Inflater inflater = new java.util.zip.Inflater();
      try {
         inflater.setInput(deflated);
         int read = 0;
         while (read < mask.length && !inflater.finished()) {
            int n = inflater.inflate(mask, read, mask.length - read);
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
               break;
            }

            read += n;
         }
      } catch (java.util.zip.DataFormatException e) {
         return new boolean[0];
      } finally {
         inflater.end();
      }

      boolean[] rivers = new boolean[size * size];
      for (int i = 0; i < rivers.length; i++) {
         rivers[i] = (mask[i >> 3] & 1 << (i & 7)) != 0;
      }

      return rivers;
   }

   private static byte terrainOf(Holder<Biome> biome) {
      if (biome.is(BiomeTags.IS_DEEP_OCEAN)) {
         return TERRAIN_DEEP_WATER;
      } else if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
         return TERRAIN_WATER;
      } else if (biome.is(BiomeTags.IS_MOUNTAIN)) {
         return TERRAIN_MOUNTAIN;
      }

      float temperature = biome.value().getBaseTemperature();
      if (temperature < 0.15F) {
         return TERRAIN_SNOW;
      } else if (temperature >= 1.9F || biome.is(BiomeTags.IS_BADLANDS)) {
         return TERRAIN_DESERT;
      } else if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_TAIGA)) {
         return TERRAIN_FOREST;
      }

      return TERRAIN_LAND;
   }

   public enum Kind {
      VILLAGE,
      FORT,
      TRADE_POST,
      GUARD_POST,
      TAVERN,
      FARM,
      MINE,
      TEMPLE,
      HOMESTEAD,
      ARMY,
      CAPITAL,
      CAPITAL_AREA,
      PLAYER_VILLAGE,
      OUTLAW,
      FLEET,
      SHRINE
   }

   /** name is a FactionNames code, or empty. */
   public static record Marker(int x, int z, Kind kind, int faction, String label, String name, int[] life) {
      public Marker(int x, int z, Kind kind, int faction, String label, String name) {
         this(x, z, kind, faction, label, name, new int[0]);
      }
   }

   /** Indices into Marker.life, and the flags in its last entry; see life(). */
   public static final int LIFE_PEOPLE = 0, LIFE_GARRISON = 1, LIFE_FOOD_DAYS = 2, LIFE_HEALTH = 3, LIFE_ORDER = 4, LIFE_LOYALTY = 5, LIFE_FLAGS = 6, LIFE_RANK = 7, LIFE_PRICES = 8;
   public static final int FLAG_FAMINE = 1, FLAG_PLAGUE = 2, FLAG_HUNGRY = 4, FLAG_TAKEN = 8, FLAG_PORT = 16;

   /** As life(), followed by the town's prices of the goods (see TownMarkets.priceList) where it has a market. */
   private static int[] life(ServerLevel level, FactionWorldData.Settlement settlement) {
      int[] life = life(settlement);
      int[] prices = TownMarkets.priceList(level, settlement.id());
      int[] all = java.util.Arrays.copyOf(life, life.length + prices.length);
      System.arraycopy(prices, 0, all, life.length, prices.length);
      return all;
   }

   private static int[] life(FactionWorldData.Settlement settlement) {
      int mouths = Math.max(1, settlement.population() + settlement.garrison());
      int flags = (settlement.famine() > 0 ? FLAG_FAMINE : 0)
         | (settlement.plague() > 0 ? FLAG_PLAGUE : 0)
         | (settlement.food() == 0 ? FLAG_HUNGRY : 0)
         | (settlement.capturedFrom().isEmpty() ? 0 : FLAG_TAKEN)
         | (settlement.port() == null ? 0 : FLAG_PORT);
      return new int[]{
         settlement.population(), settlement.garrison(), settlement.food() / mouths, settlement.health(), settlement.order(), settlement.loyalty(), flags, FactionPorts.rank(settlement)
      };
   }

   /**
    * owners holds an index into factions per cell, row by row from the north-west corner; 0 is wild
    * land. terrain holds one of the TERRAIN_ values per cell the same way, or is empty while it is
    * still being surveyed. marches hold x, z of each marching army and x, z of its target.
    */
   public static record Data(
      int originX, int originZ, byte[] owners, byte[] terrain, byte[] rivers, List<String> factions, List<Marker> markers, List<int[]> roads, List<int[]> marches, boolean canTeleport
   ) {
   }

   /** Before a name in the owner list, a player's country rather than a faction key. */
   public static final String COUNTRY_MARK = "@";

   /** How a province owner goes in the owner list: a player's land by its country's name. */
   private static String countryKey(FactionWorldData data, String owner) {
      return PlayerCountry.isLand(owner) ? COUNTRY_MARK + PlayerCountry.landName(data, owner) : owner;
   }

   /** An owner from the list, for people to read. */
   public static net.minecraft.network.chat.MutableComponent ownerName(String owner) {
      return owner.startsWith(COUNTRY_MARK)
         ? net.minecraft.network.chat.Component.literal(owner.substring(COUNTRY_MARK.length()))
         : net.minecraft.network.chat.Component.translatable("faction.ancientwarfare." + owner);
   }

   public static void open(ServerPlayer player) {
      AWCoreNetwork.sendWorldMap(player, build(player));
   }

   private static Data build(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      // Fixed on the world's centre, so the map does not shift as the player moves.
      int originX = -SIZE / 2 * STEP;
      int originZ = -SIZE / 2 * STEP;
      List<String> factions = new ArrayList<>();
      factions.add("");
      Map<String, Integer> indices = new HashMap<>();
      indices.put("", 0);
      byte[] owners = new byte[SIZE * SIZE];

      for (int row = 0; row < SIZE; row++) {
         for (int column = 0; column < SIZE; column++) {
            String owner = FactionRegions.ownerAt(level, originX + column * STEP + STEP / 2, originZ + row * STEP + STEP / 2);
            owners[row * SIZE + column] = (byte)index(countryKey(data, owner), factions, indices);
         }
      }

      List<Marker> markers = new ArrayList<>();
      int maxX = originX + SIZE * STEP;
      int maxZ = originZ + SIZE * STEP;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         BlockPos center = settlement.center();
         if (center.getX() >= originX && center.getX() < maxX && center.getZ() >= originZ && center.getZ() < maxZ) {
            markers.add(
               new Marker(
                  center.getX(),
                  center.getZ(),
                  kind(settlement),
                  index(settlement.faction(), factions, indices),
                  "#" + settlement.id() + " " + (settlement.built() ? templateName(settlement.template()) : settlement.kind() + " (not built yet)") + ", people " + settlement.population() + ", garrison " + settlement.garrison()
                     + (FactionDiplomacy.isOutlaw(settlement.faction()) ? ", lair level " + FactionBandits.level(settlement) : ""),
                  settlement.name(),
                  life(player.serverLevel(), settlement)
               )
            );
         }
      }

      // Each faith's holy site, beside the settlement that holds it.
      for (int[] site : FactionFaith.holySites(level, data)) {
         FactionWorldData.Settlement settlement = data.settlement(site[0]);
         BlockPos center = settlement.center();
         if (center.getX() >= originX && center.getX() < maxX && center.getZ() >= originZ && center.getZ() < maxZ) {
            markers.add(new Marker(center.getX() + 24, center.getZ() - 24, Kind.SHRINE, index(settlement.faction(), factions, indices), "", "faith:" + site[1] + ".0"));
         }
      }

      data.villages().forEach((pos, owner) -> {
         if (pos.getX() >= originX && pos.getX() < maxX && pos.getZ() >= originZ && pos.getZ() < maxZ) {
            ServerPlayer villageOwner = level.getServer().getPlayerList().getPlayer(owner);
            FactionWorldData.VillageState state = data.villageState(pos);
            int[] life = state == null ? new int[0] : new int[]{state.people, state.guards, -1, -1, state.order, state.loyalty, FLAG_TAKEN};
            FactionWorldData.Country country = data.country(owner);
            int ownerIndex = country == null ? 0 : index(COUNTRY_MARK + country.name, factions, indices);
            markers.add(
               new Marker(pos.getX(), pos.getZ(), Kind.PLAYER_VILLAGE, ownerIndex, villageOwner == null ? "player village" : villageOwner.getScoreboardName(), "", life)
            );
         }
      });
      List<int[]> roads = new ArrayList<>();

      for (FactionWorldData.Route route : data.routes()) {
         List<Long> points = route.points();
         if (points.size() < 2) {
            continue;
         }

         int count = (points.size() - 1) / ROAD_POINT_STRIDE + 2;
         int[] line = new int[count * 2];
         int n = 0;

         for (int i = 0; i < points.size(); i += ROAD_POINT_STRIDE) {
            line[n++] = FactionRoads.unpackX(points.get(i));
            line[n++] = FactionRoads.unpackZ(points.get(i));
         }

         long last = points.get(points.size() - 1);
         line[n++] = FactionRoads.unpackX(last);
         line[n++] = FactionRoads.unpackZ(last);
         roads.add(java.util.Arrays.copyOf(line, n));
      }

      List<int[]> marches = new ArrayList<>();

      boolean all = canTeleport(player);
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         int x = (int)Math.floor(campaign.x());
         int z = (int)Math.floor(campaign.z());
         // Only armies the player can see from where he stands, or that his spies report.
         if (!all
            && !FactionSpies.knows(level, data, player, campaign.faction())
            && !sees(level, data, player, x, z)
            && !(campaign.naval() && net.shadowmage.ancientwarfare.core.research.PlayerResearch.has(player, net.shadowmage.ancientwarfare.core.research.PlayerResearch.SEA_CHARTS))) {
            continue;
         }

         markers.add(
            new Marker(x, z, campaign.naval() ? Kind.FLEET : Kind.ARMY, index(campaign.faction(), factions, indices), campaign.size() + (campaign.naval() ? " aboard" : campaign.raid() ? " raiders" : " soldiers") + (campaign.relief() ? ", to relieve settlement #" + campaign.targetSettlement() : campaign.hunt() != 0 ? ", to cut off army #" + campaign.hunt() : ", against settlement #" + campaign.targetSettlement()), "")
         );

         for (FactionWorldData.Settlement settlement : data.settlements()) {
            if (settlement.id() == campaign.targetSettlement()) {
               marches.add(new int[]{x, z, settlement.center().getX(), settlement.center().getZ()});
            }
         }
      }

      // Assaults, raids and sieges being made ready, as the spies report them.
      for (FactionWorldData.Army army : data.armies()) {
         // With post stations the player always hears of an army marching on their own village.
         if (!all
            && !FactionSpies.knows(level, data, player, army.faction())
            && !(player.getUUID().equals(army.villageOwner())
               && net.shadowmage.ancientwarfare.core.research.PlayerResearch.has(player, net.shadowmage.ancientwarfare.core.research.PlayerResearch.POST_STATIONS))) {
            continue;
         }

         BlockPos target = army.target();
         FactionWorldData.Settlement home = null;
         for (FactionWorldData.Settlement settlement : data.settlements()) {
            if (settlement.faction().equals(army.faction())
               && settlement.id() != army.targetSettlement()
               && (home == null || settlement.horizontalDistanceSquared(target.getX(), target.getZ()) < home.horizontalDistanceSquared(target.getX(), target.getZ()))) {
               home = settlement;
            }
         }

         int x = home == null ? target.getX() : home.center().getX();
         int z = home == null ? target.getZ() : home.center().getZ();
         markers.add(
            new Marker(
               x,
               z,
               Kind.ARMY,
               index(army.faction(), factions, indices),
               army.size() + (army.siege() ? " soldiers, siege of " : army.isRaid() ? " raiders, raid on " : " soldiers, assault on ") + target.getX() + ", " + target.getZ(),
               ""
            )
         );
         marches.add(new int[]{x, z, target.getX(), target.getZ()});
      }

      Survey survey = survey(level, originX, originZ);
      return new Data(originX, originZ, owners, survey.terrain(), survey.rivers(), factions, markers, roads, marches, canTeleport(player));
   }

   private static String templateName(String template) {
      String name = template.substring(template.lastIndexOf('/') + 1);
      return name.endsWith(".aws") ? name.substring(0, name.length() - 4) : name;
   }

   private static int index(String faction, List<String> factions, Map<String, Integer> indices) {
      return indices.computeIfAbsent(faction, key -> {
         factions.add(key);
         return factions.size() - 1;
      });
   }

   /** A standing inn or tavern, where mercenaries can be hired. */
   static boolean isTavern(FactionWorldData.Settlement settlement) {
      return settlement.built() && kind(settlement) == Kind.TAVERN;
   }

   static boolean isTemple(FactionWorldData.Settlement settlement) {
      return settlement.built() && kind(settlement) == Kind.TEMPLE;
   }

   private static Kind kind(FactionWorldData.Settlement settlement) {
      if (FactionDiplomacy.isOutlaw(settlement.faction())) {
         return Kind.OUTLAW;
      }

      String name = templateName(settlement.template()).toLowerCase(Locale.ROOT);
      return switch (settlement.kind()) {
         case "fort" -> Kind.FORT;
         case FactionTrade.TRADE_POST -> name.contains("inn") || name.contains("tavern") ? Kind.TAVERN : Kind.TRADE_POST;
         case FactionTrade.GUARD_POST -> Kind.GUARD_POST;
         case "capital" -> Kind.CAPITAL;
         case "village" -> Kind.VILLAGE;
         default -> building(settlement.template(), name);
      };
   }

   /** What a settlement of one building is, by its building. */
   private static Kind building(String template, String name) {
      if (MINE_WORDS.stream().anyMatch(name::contains)) {
         return Kind.MINE;
      } else if (TEMPLE_WORDS.stream().anyMatch(name::contains)) {
         return Kind.TEMPLE;
      } else if (TemplateFactions.hasMilitaryName(template)) {
         return Kind.FORT;
      } else if (FARM_WORDS.stream().anyMatch(name::contains)) {
         return Kind.FARM;
      } else if (name.contains("inn") || name.contains("tavern")) {
         return Kind.TAVERN;
      } else if (VILLAGE_WORDS.stream().anyMatch(name::contains)) {
         return Kind.VILLAGE;
      }

      return Kind.HOMESTEAD;
   }

   public static boolean canTeleport(ServerPlayer player) {
      return player.hasPermissions(2) || player.isCreative();
   }

   /** Teleports to a clicked point at TELEPORT_Y, or just above the ground where that is higher. */
   public static void teleport(ServerPlayer player, int x, int z) {
      if (!canTeleport(player)) {
         return;
      }

      ServerLevel level = player.serverLevel();
      level.getChunk(x >> 4, z >> 4);
      int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
      int y = Math.max(TELEPORT_Y, ground + 1);
      player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
      player.sendSystemMessage(Component.translatable("gui.ancientwarfare.world_map.teleported", x, y, z));
   }
}
