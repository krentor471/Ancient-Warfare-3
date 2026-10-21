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
   /** The surveyed terrain of each level's map; it never changes, so it is surveyed once, off the server thread. */
   private static final Map<String, CompletableFuture<byte[]>> TERRAIN = new ConcurrentHashMap<>();

   private WorldMap() {
   }

   /** The level's map terrain, or an empty array while the survey is still running. */
   private static byte[] terrain(ServerLevel level, int originX, int originZ) {
      CompletableFuture<byte[]> survey = TERRAIN.computeIfAbsent(level.dimension().location() + "@" + level.getSeed(), key -> CompletableFuture.supplyAsync(() -> survey(level, originX, originZ)));
      return survey.getNow(new byte[0]);
   }

   private static byte[] survey(ServerLevel level, int originX, int originZ) {
      byte[] terrain = new byte[SIZE * SIZE];
      BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
      Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
      int quartY = QuartPos.fromBlock(level.getSeaLevel());

      for (int row = 0; row < SIZE; row++) {
         for (int column = 0; column < SIZE; column++) {
            int x = originX + column * STEP + STEP / 2;
            int z = originZ + row * STEP + STEP / 2;
            terrain[row * SIZE + column] = terrainOf(source.getNoiseBiome(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z), sampler));
         }
      }

      return terrain;
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
      OUTLAW
   }

   /** name is a FactionNames code, or empty. */
   public static record Marker(int x, int z, Kind kind, int faction, String label, String name) {
   }

   /**
    * owners holds an index into factions per cell, row by row from the north-west corner; 0 is wild
    * land. terrain holds one of the TERRAIN_ values per cell the same way, or is empty while it is
    * still being surveyed. marches hold x, z of each marching army and x, z of its target.
    */
   public static record Data(
      int originX, int originZ, byte[] owners, byte[] terrain, List<String> factions, List<Marker> markers, List<int[]> roads, List<int[]> marches, boolean canTeleport
   ) {
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
            owners[row * SIZE + column] = (byte)index(owner, factions, indices);
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
                  settlement.name()
               )
            );
         }
      }

      data.villages().forEach((pos, owner) -> {
         if (pos.getX() >= originX && pos.getX() < maxX && pos.getZ() >= originZ && pos.getZ() < maxZ) {
            ServerPlayer villageOwner = level.getServer().getPlayerList().getPlayer(owner);
            markers.add(new Marker(pos.getX(), pos.getZ(), Kind.PLAYER_VILLAGE, 0, villageOwner == null ? "player village" : villageOwner.getScoreboardName(), ""));
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

      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         int x = (int)Math.floor(campaign.x());
         int z = (int)Math.floor(campaign.z());
         markers.add(
            new Marker(x, z, Kind.ARMY, index(campaign.faction(), factions, indices), campaign.size() + (campaign.raid() ? " raiders" : " soldiers") + ", against settlement #" + campaign.targetSettlement(), "")
         );

         for (FactionWorldData.Settlement settlement : data.settlements()) {
            if (settlement.id() == campaign.targetSettlement()) {
               marches.add(new int[]{x, z, settlement.center().getX(), settlement.center().getZ()});
            }
         }
      }

      return new Data(originX, originZ, owners, terrain(level, originX, originZ), factions, markers, roads, marches, canTeleport(player));
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
