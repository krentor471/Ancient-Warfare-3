package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/**
 * A map of the faction world around a player, for testing: who owns which land, where the
 * settlements, capitals and roads are. Operators and creative players can teleport by clicking it.
 */
public final class WorldMap {
   /** Blocks per map cell, and cells across the map. */
   public static final int STEP = 64;
   public static final int SIZE = 128;
   public static final int TELEPORT_Y = 90;
   private static final int ROAD_POINT_STRIDE = 4;

   private WorldMap() {
   }

   public enum Kind {
      VILLAGE,
      FORT,
      TRADE_POST,
      GUARD_POST,
      CAPITAL,
      CAPITAL_AREA,
      PLAYER_VILLAGE,
      OUTLAW
   }

   public static record Marker(int x, int z, Kind kind, int faction, String label) {
   }

   /** owners holds an index into factions per cell, row by row from the north-west corner; 0 is wild land. */
   public static record Data(int originX, int originZ, byte[] owners, List<String> factions, List<Marker> markers, List<int[]> roads, boolean canTeleport) {
   }

   public static void open(ServerPlayer player) {
      AWCoreNetwork.sendWorldMap(player, build(player));
   }

   private static Data build(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      int originX = Math.floorDiv(player.getBlockX(), STEP) * STEP - SIZE / 2 * STEP;
      int originZ = Math.floorDiv(player.getBlockZ(), STEP) * STEP - SIZE / 2 * STEP;
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
               )
            );
         }
      }

      for (int x = originX; x < maxX; x += FactionRegions.REALM_SIZE / 2) {
         for (int z = originZ; z < maxZ; z += FactionRegions.REALM_SIZE / 2) {
            FactionRegions.Realm realm = FactionRegions.realmAt(level, x, z);
            boolean inside = realm.seedX() >= originX && realm.seedX() < maxX && realm.seedZ() >= originZ && realm.seedZ() < maxZ;
            if (inside && !realm.faction().isEmpty() && markers.stream().noneMatch(m -> m.kind() == Kind.CAPITAL_AREA && m.x() == realm.seedX() && m.z() == realm.seedZ())) {
               markers.add(new Marker(realm.seedX(), realm.seedZ(), Kind.CAPITAL_AREA, index(realm.faction(), factions, indices), "capital area"));
            }
         }
      }

      data.villages().forEach((pos, owner) -> {
         if (pos.getX() >= originX && pos.getX() < maxX && pos.getZ() >= originZ && pos.getZ() < maxZ) {
            ServerPlayer villageOwner = level.getServer().getPlayerList().getPlayer(owner);
            markers.add(new Marker(pos.getX(), pos.getZ(), Kind.PLAYER_VILLAGE, 0, villageOwner == null ? "player village" : villageOwner.getScoreboardName()));
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

      return new Data(originX, originZ, owners, factions, markers, roads, canTeleport(player));
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

   private static Kind kind(FactionWorldData.Settlement settlement) {
      if (FactionDiplomacy.isOutlaw(settlement.faction())) {
         return Kind.OUTLAW;
      }

      return switch (settlement.kind()) {
         case "fort" -> Kind.FORT;
         case FactionTrade.TRADE_POST -> Kind.TRADE_POST;
         case FactionTrade.GUARD_POST -> Kind.GUARD_POST;
         case "capital" -> Kind.CAPITAL;
         default -> Kind.VILLAGE;
      };
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
