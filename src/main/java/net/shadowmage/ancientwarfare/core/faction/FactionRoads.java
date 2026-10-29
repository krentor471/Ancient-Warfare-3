package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerationData;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.IslandChunkWindow;

/**
 * Lays the road of each trade route: a three-wide track of path and gravel following the land,
 * with plank bridges over short water. Several roads are laid at once, a short stretch at a time
 * from one end on land loaded in the background ahead of the work, wherever
 * no player is near enough to see it appear, steering around steep ground, settlements and other
 * buildings toward the far end. A finished road gets roadside posts every hundred and eighty blocks
 * or so, taverns, guard posts and trade posts in turn, built by the faction of the nearer end. A
 * failed road is tried again now and then.
 */
public final class FactionRoads {
   private static final int STEP = 4;
   private static final int SEGMENT_STEPS = 4;
   private static final int TICK_INTERVAL = 10;
   /** One roadside post at most this often, as each is a whole building. */
   private static final int POST_INTERVAL = 40;
   private static final double PLAYER_RANGE = 192.0;
   /** Roads within this distance of a player are built first, and several stretches at a time. */
   private static final double NEAR_PLAYER_ROADS = 1500.0;
   /** Roads built at once, each by its own crew with its land loading in the background. */
   private static final int ROAD_CREWS = 8;
   /** Stretches one crew lays per call, as long as its land is loaded. */
   private static final int MAX_STRETCHES = 4;
   private static final long ROAD_BUDGET_NANOS = 12_000_000L;
   /** Blocks around the road's end one stretch may touch: its four steps, the road's width and ramps. */
   private static final int STRETCH_REACH = STEP * SEGMENT_STEPS + 8;
   /** How far ahead the next land is asked for. */
   private static final int LOOK_AHEAD = 40;
   /** Marks road tickets apart from other chunk windows, which use real positions. */
   private static final int ROAD_TICKET_Y = -30000;
   /** The land each road under way holds loaded, by route. */
   private static final Map<Integer, IslandChunkWindow> WORK = new HashMap<>();
   /** Highest fill or cut that evens out one road cell. */
   private static final int MAX_RAMP = 6;
   /** Longest bridge, in blocks of water crossed. */
   private static final int MAX_WATER_RUN = 48;
   /** Failed roads are tried again once a session, and then every this many days. */
   private static final long RETRY_DAYS = 10L;
   /** Day each failed road was last tried; not saved, so a restart tries them all once more. */
   private static final Map<Integer, Long> FAILED_ON = new HashMap<>();
   private static final int ARRIVE_MARGIN = 6;
   private static final int OBSTACLE_MARGIN = 3;
   private static final int VILLAGE_RADIUS = 32;
   /** Steering choices either side of straight at the target, in steps of 22.5 degrees. */
   private static final int TURNS = 3;
   private static final double TURN_ANGLE = Math.PI / 8.0;
   /** Road points are recorded every two steps. */
   private static final int POINT_SPACING = STEP * 2;
   /** Road length between posts; they come in turn, so each kind stands every three times this. */
   private static final int POST_SPACING = 900;
   /** Posts keep this far from either end of the road. */
   private static final int POST_END_GAP = 80;
   private static final int POST_OFFSET = 22;
   private static final int POST_OFFSET_STEP = 14;
   /** Sites tried for a post: both sides of the road, at two distances. */
   private static final int POST_ATTEMPTS = 4;

   private FactionRoads() {
   }

   public static long pack(int x, int z) {
      return (long)x << 32 | z & 0xFFFFFFFFL;
   }

   public static int unpackX(long point) {
      return (int)(point >> 32);
   }

   public static int unpackZ(long point) {
      return (int)point;
   }

   /** True while a road with both ends built and inside the prebuild area is still unfinished. */
   static boolean unfinishedNear(ServerLevel level, FactionWorldData data) {
      for (FactionWorldData.Route route : data.routes()) {
         if (route.blocked() || route.road() == FactionWorldData.RoadState.DONE || route.road() == FactionWorldData.RoadState.FAILED) {
            continue;
         }

         FactionWorldData.Settlement from = data.endpoint(route, true);
         FactionWorldData.Settlement to = data.endpoint(route, false);
         if (from != null && to != null && from.built() && to.built()
            && FactionPrebuild.covers(level, from.center().getX(), from.center().getZ())
            && FactionPrebuild.covers(level, to.center().getX(), to.center().getZ())) {
            return true;
         }
      }

      return false;
   }

   /** Lays one stretch of one unfinished road no player is near. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % TICK_INTERVAL != 0L) {
         return;
      }

      for (FactionWorldData.Route route : level.getGameTime() % POST_INTERVAL == 0L ? data.routes() : List.<FactionWorldData.Route>of()) {
         // Counted afresh rather than by the posts flag, so roads finished before posts stood closer get the rest.
         if (route.road() == FactionWorldData.RoadState.DONE && route.postsBuilt < postSpots(route).size() && buildPost(level, data, route)) {
            data.setDirty();
            return;
         }
      }

      List<FactionWorldData.Route> wanted = new ArrayList<>();
      Map<Integer, Double> distances = new HashMap<>();

      for (FactionWorldData.Route route : data.routes()) {
         if (route.road() == FactionWorldData.RoadState.FAILED) {
            // The land is the same, but bridges may have grown longer or a settlement moved since.
            Long failed = FAILED_ON.get(route.id());
            if (failed != null && data.lastSimulatedDay() - failed < RETRY_DAYS) {
               continue;
            }

            FAILED_ON.put(route.id(), data.lastSimulatedDay());
            route.resetRoad();
         }

         if (route.blocked() || route.road() == FactionWorldData.RoadState.DONE) {
            continue;
         }

         FactionWorldData.Settlement from = data.endpoint(route, true);
         FactionWorldData.Settlement to = data.endpoint(route, false);
         if (from == null || to == null || !from.built() || !to.built()) {
            // A road leads to where the settlement stands, so it waits until both ends are built.
            continue;
         }

         int startX = route.road() == FactionWorldData.RoadState.NONE ? from.center().getX() : route.cursorX;
         int startZ = route.road() == FactionWorldData.RoadState.NONE ? from.center().getZ() : route.cursorZ;
         if (!FactionPrebuild.active() && level.hasNearbyAlivePlayer(startX, level.getSeaLevel(), startZ, PLAYER_RANGE)) {
            continue;
         }

         // The roads a player will soon walk come first, nearest first; the rest of the world in turn.
         double distance = NEAR_PLAYER_ROADS * NEAR_PLAYER_ROADS;
         for (ServerPlayer player : level.players()) {
            double dx = player.getX() - startX;
            double dz = player.getZ() - startZ;
            distance = Math.min(distance, dx * dx + dz * dz);
         }

         // Roads already under way keep their crew, so they are finished rather than all begun.
         if (WORK.containsKey(route.id())) {
            distance = Math.min(distance, NEAR_PLAYER_ROADS * NEAR_PLAYER_ROADS - 1.0);
         }

         distances.put(route.id(), distance);
         wanted.add(route);
      }

      wanted.sort(java.util.Comparator.comparingDouble(route -> distances.get(route.id())));
      List<FactionWorldData.Route> crews = wanted.subList(0, Math.min(FactionPrebuild.active() ? ROAD_CREWS * 3 : ROAD_CREWS, wanted.size()));
      java.util.Set<Integer> working = new java.util.HashSet<>();
      crews.forEach(route -> working.add(route.id()));
      WORK.entrySet().removeIf(entry -> {
         if (!working.contains(entry.getKey())) {
            entry.getValue().releaseAll();
            return true;
         }

         return false;
      });

      long deadline = System.nanoTime() + (FactionPrebuild.active() ? ROAD_BUDGET_NANOS * 4 : ROAD_BUDGET_NANOS);
      for (FactionWorldData.Route route : crews) {
         FactionWorldData.Settlement from = data.endpoint(route, true);
         FactionWorldData.Settlement to = data.endpoint(route, false);
         if (route.road() == FactionWorldData.RoadState.NONE) {
            route.cursorX = from.center().getX();
            route.cursorZ = from.center().getZ();
            route.road = FactionWorldData.RoadState.BUILDING;
         }

         IslandChunkWindow window = WORK.computeIfAbsent(route.id(), id -> new IslandChunkWindow());
         // Each stretch is laid only on land already loaded, while the land ahead loads in the background.
         for (int i = 0; i < MAX_STRETCHES && route.road() == FactionWorldData.RoadState.BUILDING && System.nanoTime() < deadline; i++) {
            if (!landReady(level, window, route, to)) {
               break;
            }

            buildStretch(level, data, route, from, to);
         }

         if (route.road() != FactionWorldData.RoadState.BUILDING) {
            window.releaseAll();
            WORK.remove(route.id());
         }
      }

      data.setDirty();
   }

   /** Asks for the land around the road's end and a little ahead; true once the land around its end is loaded. */
   private static boolean landReady(ServerLevel level, IslandChunkWindow window, FactionWorldData.Route route, FactionWorldData.Settlement to) {
      int x = route.cursorX;
      int z = route.cursorZ;
      double dx = to.center().getX() - x;
      double dz = to.center().getZ() - z;
      double length = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
      int aheadX = x + (int)Math.round(dx / length * LOOK_AHEAD);
      int aheadZ = z + (int)Math.round(dz / length * LOOK_AHEAD);
      java.util.LinkedHashSet<ChunkPos> here = chunksAround(x, z);
      java.util.LinkedHashSet<ChunkPos> all = new java.util.LinkedHashSet<>(here);
      all.addAll(chunksAround(aheadX, aheadZ));
      window.requestGroup(level, new BlockPos(route.id(), ROAD_TICKET_Y, 0), new ArrayList<>(all), all.size());
      return here.stream().allMatch(chunk -> level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null);
   }

   private static java.util.LinkedHashSet<ChunkPos> chunksAround(int x, int z) {
      java.util.LinkedHashSet<ChunkPos> chunks = new java.util.LinkedHashSet<>();
      for (int cx = (x - STRETCH_REACH) >> 4; cx <= (x + STRETCH_REACH) >> 4; cx++) {
         for (int cz = (z - STRETCH_REACH) >> 4; cz <= (z + STRETCH_REACH) >> 4; cz++) {
            chunks.add(new ChunkPos(cx, cz));
         }
      }

      return chunks;
   }

   private static void buildStretch(ServerLevel level, FactionWorldData data, FactionWorldData.Route route, FactionWorldData.Settlement from, FactionWorldData.Settlement to) {
      int targetX = to.center().getX();
      int targetZ = to.center().getZ();
      int maxSteps = FactionTrade.length(data, route) * 3 / STEP + 50;
      List<int[]> obstacles = obstacles(level, data, route.cursorX, route.cursorZ, from, to);

      for (int i = 0; i < SEGMENT_STEPS; i++) {
         if (to.contains(route.cursorX, route.cursorZ, ARRIVE_MARGIN)) {
            route.road = FactionWorldData.RoadState.DONE;
            route.points.add(pack(route.cursorX, route.cursorZ));
            AncientWarfareCore.LOG.info("Road #{} between settlements #{} and #{} finished, {} steps", route.id(), route.a(), route.b(), route.steps);
            return;
         }

         if (route.steps > maxSteps) {
            fail(route, "too long");
            return;
         }

         int[] next = chooseStep(level, route.cursorX, route.cursorZ, targetX, targetZ, obstacles);
         if (next == null) {
            fail(route, "no way forward");
            return;
         }

         if (!lay(level, route, route.cursorX, route.cursorZ, next[0], next[1], obstacles)) {
            fail(route, "water too wide");
            return;
         }

         route.cursorX = next[0];
         route.cursorZ = next[1];
         route.steps++;
         if (route.steps % 2 == 0) {
            route.points.add(pack(route.cursorX, route.cursorZ));
         }
      }
   }

   /** Where the road's posts stand: indices into its points, trade post first, then guard post, in turn. */
   public static List<Integer> postSpots(FactionWorldData.Route route) {
      List<Integer> spots = new ArrayList<>();
      int every = POST_SPACING / POINT_SPACING;
      int gap = POST_END_GAP / POINT_SPACING;

      for (int index = every; index < route.points().size() - gap; index += every) {
         spots.add(index);
      }

      return spots;
   }

   /** Builds the road's next post if no player is near it. Returns true when it tried one. */
   private static boolean buildPost(ServerLevel level, FactionWorldData data, FactionWorldData.Route route) {
      List<Integer> spots = postSpots(route);
      if (route.postsBuilt >= spots.size()) {
         route.posts = true;
         return false;
      }

      int index = spots.get(route.postsBuilt);
      List<Long> points = route.points();
      long point = points.get(index);
      int x = unpackX(point);
      int z = unpackZ(point);
      if (level.hasNearbyAlivePlayer(x, level.getSeaLevel(), z, PLAYER_RANGE)) {
         return false;
      }

      long before = points.get(Math.max(0, index - 1));
      long after = points.get(Math.min(points.size() - 1, index + 1));
      double dx = unpackX(after) - unpackX(before);
      double dz = unpackZ(after) - unpackZ(before);
      double length = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
      int side = route.postsBuilt % 2 == 0 ? 1 : -1;
      FactionWorldData.Settlement a = data.endpoint(route, true);
      FactionWorldData.Settlement b = data.endpoint(route, false);
      route.postsBuilt++;
      if (a == null || b == null) {
         return true;
      }

      String faction = a.horizontalDistanceSquared(x, z) <= b.horizontalDistanceSquared(x, z) ? a.faction() : b.faction();
      // In turn: a trading post, a guard post, a tavern.
      AwsWorldGenerator.Site wanted = switch (route.postsBuilt % 3) {
         case 1 -> AwsWorldGenerator.Site.TRADE_POST;
         case 2 -> AwsWorldGenerator.Site.GUARD_POST;
         default -> AwsWorldGenerator.Site.TAVERN;
      };
      AwsWorldGenerator.Site site = wanted;
      boolean built = false;

      // Either side of the road, near and a little further off; where none fits, the road goes without.
      for (int attempt = 0; attempt < POST_ATTEMPTS && !built; attempt++) {
         int tried = attempt;
         int along = side * (tried % 2 == 0 ? 1 : -1);
         int offset = POST_OFFSET + tried / 2 * POST_OFFSET_STEP;
         int siteX = x + (int)Math.round(-dz / length * offset * along);
         int siteZ = z + (int)Math.round(dx / length * offset * along);
         built = AwsWorldGenerator.generateSettlementAt(level, new ChunkPos(siteX >> 4, siteZ >> 4), faction, site);
      }

      AncientWarfareCore.LOG.info("Road #{}: {} {} {} near {}, {}", route.id(), faction, built ? "built a" : "could not build a", site.name().toLowerCase(java.util.Locale.ROOT), x, z);
      return true;
   }

   private static void fail(FactionWorldData.Route route, String reason) {
      route.road = FactionWorldData.RoadState.FAILED;
      AncientWarfareCore.LOG.info("Road #{} between settlements #{} and #{} stopped after {} steps: {}", route.id(), route.a(), route.b(), route.steps, reason);
   }

   /** The cheapest step toward the target: gentle slopes, dry land, straight on, and no buildings in the way. */
   private static int[] chooseStep(ServerLevel level, int x, int z, int targetX, int targetZ, List<int[]> obstacles) {
      double heading = Math.atan2(targetZ - z, targetX - x);
      int here = ground(level, x, z);
      int[] best = null;
      double bestCost = Double.MAX_VALUE;

      for (int turn = -TURNS; turn <= TURNS; turn++) {
         double angle = heading + turn * TURN_ANGLE;
         int nx = x + (int)Math.round(Math.cos(angle) * STEP);
         int nz = z + (int)Math.round(Math.sin(angle) * STEP);
         int y = ground(level, nx, nz);
         BlockState state = level.getBlockState(new BlockPos(nx, y, nz));
         int climb = Math.abs(y - here);
         double cost = turn * turn * 0.4 + (climb <= 1 ? climb : climb * climb * 3.0);
         if (state.getFluidState().is(FluidTags.LAVA)) {
            continue;
         }

         if (state.getFluidState().is(FluidTags.WATER)) {
            cost += 12.0;
         }

         if (blocked(obstacles, nx, nz)) {
            // Passing a building's edge is allowed (it is not paved) but costly, so the road goes round.
            cost += 200.0;
         }

         if (cost < bestCost) {
            bestCost = cost;
            best = new int[]{nx, nz};
         }
      }

      return best;
   }

   /** Paves the line between two steps, three wide. Returns false when the road has crossed too much water. */
   private static boolean lay(ServerLevel level, FactionWorldData.Route route, int x0, int z0, int x1, int z1, List<int[]> obstacles) {
      double dx = x1 - x0;
      double dz = z1 - z0;
      double length = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
      double sideX = -dz / length;
      double sideZ = dx / length;

      BlockPos[] previous = new BlockPos[3];

      for (int t = 1; t <= (int)Math.round(length); t++) {
         double cx = x0 + dx * t / length;
         double cz = z0 + dz * t / length;
         boolean water = false;

         for (int side = -1; side <= 1; side++) {
            int x = (int)Math.floor(cx + sideX * side + 0.5);
            int z = (int)Math.floor(cz + sideZ * side + 0.5);
            if (!blocked(obstacles, x, z) && !insideEndpoint(obstacles, x, z)) {
               water |= pave(level, x, z) && side == 0;
               BlockPos here = new BlockPos(x, ground(level, x, z), z);
               BlockPos before = previous[side + 1];
               if (before != null && Math.abs(here.getY() - before.getY()) >= 2) {
                  here = ramp(level, here, before.getY() + Integer.signum(here.getY() - before.getY()));
               }

               previous[side + 1] = here;
            } else {
               previous[side + 1] = null;
            }
         }

         route.waterRun = water ? route.waterRun + 1 : 0;
         if (route.waterRun > MAX_WATER_RUN) {
            return false;
         }
      }

      return true;
   }

   /**
    * Fills or cuts a road cell to one block from its neighbour, so a slope of any height becomes a ramp
    * anyone can walk: each next cell is measured against this one. Leaves cells it would have to cut
    * through trees, buildings or deep rock as they are.
    */
   private static BlockPos ramp(ServerLevel level, BlockPos cell, int targetY) {
      int depth = cell.getY() - targetY;
      if (Math.abs(depth) > MAX_RAMP) {
         return cell;
      }

      for (int y = Math.min(cell.getY(), targetY) + 1; y <= Math.max(cell.getY(), targetY) + 2; y++) {
         BlockState state = level.getBlockState(cell.atY(y));
         if (state.hasBlockEntity() || state.is(BlockTags.LOGS)) {
            return cell;
         }
      }

      BlockState top = level.getBlockState(cell);
      Block fill = top.is(Blocks.OAK_PLANKS) || top.is(Blocks.GRAVEL) ? top.getBlock() : Blocks.COARSE_DIRT;

      for (int y = targetY; y > cell.getY(); y--) {
         level.setBlock(cell.atY(y), fill.defaultBlockState(), Block.UPDATE_CLIENTS);
      }

      if (depth > 0) {
         // Cut down: the road surface goes on whatever the cut uncovered.
         Block surface = surface(level.getBlockState(cell.atY(targetY)));
         if (surface != null) {
            level.setBlock(cell.atY(targetY), surface.defaultBlockState(), Block.UPDATE_CLIENTS);
         }
      }

      for (int y = targetY + 1; y <= Math.max(cell.getY(), targetY) + 2; y++) {
         BlockState state = level.getBlockState(cell.atY(y));
         if (!state.isAir() && state.getFluidState().isEmpty()) {
            level.setBlock(cell.atY(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
         }
      }

      return cell.atY(targetY);
   }

   /** Paves one column; returns true when it bridged water. */
   private static boolean pave(ServerLevel level, int x, int z) {
      int y = ground(level, x, z);
      BlockPos pos = new BlockPos(x, y, z);
      BlockState state = level.getBlockState(pos);
      if (level.getBlockState(pos.above()).is(BlockTags.LOGS)) {
         return false;
      }

      boolean water = state.getFluidState().is(FluidTags.WATER);
      Block road = water ? Blocks.OAK_PLANKS : surface(state);
      if (road == null) {
         return false;
      }

      level.setBlock(pos, road.defaultBlockState(), Block.UPDATE_CLIENTS);

      for (int up = 1; up <= 3; up++) {
         BlockPos above = pos.above(up);
         BlockState over = level.getBlockState(above);
         if (!over.isAir() && over.getFluidState().isEmpty() && (over.canBeReplaced() || up <= 2 && over.is(BlockTags.LEAVES))) {
            level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
         }
      }

      return water;
   }

   /** What a natural ground block becomes on the road; null leaves it as it is (buildings, farmland, ice). */
   private static Block surface(BlockState state) {
      if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
         || state.is(Blocks.ROOTED_DIRT)) {
         return Blocks.DIRT_PATH;
      } else if (state.is(Blocks.DIRT_PATH) || state.is(Blocks.GRAVEL)) {
         return null;
      } else if (state.is(BlockTags.SAND) || state.is(Blocks.SNOW_BLOCK) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.CLAY)
         || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.MUD)) {
         return Blocks.GRAVEL;
      }

      return null;
   }

   /** Y of the top solid ground or water at x, z, below trees and plants. Loads the chunk. */
   private static int ground(ServerLevel level, int x, int z) {
      level.getChunk(x >> 4, z >> 4);
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

      while (y > level.getMinBuildHeight()) {
         BlockState state = level.getBlockState(new BlockPos(x, y, z));
         if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES) && !(state.canBeReplaced() && state.getFluidState().isEmpty())) {
            break;
         }

         y--;
      }

      return y;
   }

   /** Areas the road may not cross near x, z: settlements, other buildings and player villages, as {minX, minZ, maxX, maxZ}. */
   private static List<int[]> obstacles(ServerLevel level, FactionWorldData data, int x, int z, FactionWorldData.Settlement from, FactionWorldData.Settlement to) {
      int reach = STEP * SEGMENT_STEPS + 64;
      List<int[]> obstacles = new ArrayList<>();

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement != from && settlement != to && settlement.horizontalDistanceSquared(x, z) < (double)(reach + 128) * (reach + 128)) {
            obstacles.add(
               new int[]{
                  settlement.min().getX() - OBSTACLE_MARGIN,
                  settlement.min().getZ() - OBSTACLE_MARGIN,
                  settlement.max().getX() + OBSTACLE_MARGIN,
                  settlement.max().getZ() + OBSTACLE_MARGIN
               }
            );
         }
      }

      for (AwsWorldGenerationData.Entry entry : AwsWorldGenerationData.get(level).entries()) {
         boolean endpoint = from.contains(entry.minX(), entry.minZ(), 0) || to.contains(entry.minX(), entry.minZ(), 0);
         if (!endpoint && entry.horizontalDistanceSquared(x, z) < (double)(reach + 128) * (reach + 128)) {
            obstacles.add(
               new int[]{entry.minX() - OBSTACLE_MARGIN, entry.minZ() - OBSTACLE_MARGIN, entry.maxX() + OBSTACLE_MARGIN, entry.maxZ() + OBSTACLE_MARGIN}
            );
         }
      }

      data.villages().keySet().forEach(village -> {
         if (!from.contains(village.getX(), village.getZ(), 0) && !to.contains(village.getX(), village.getZ(), 0) && village.distToCenterSqr(x, village.getY(), z) < (double)(reach + VILLAGE_RADIUS) * (reach + VILLAGE_RADIUS)) {
            obstacles.add(
               new int[]{village.getX() - VILLAGE_RADIUS, village.getZ() - VILLAGE_RADIUS, village.getX() + VILLAGE_RADIUS, village.getZ() + VILLAGE_RADIUS}
            );
         }
      });
      // The road starts and ends inside its settlements but does not pave them.
      obstacles.add(new int[]{from.min().getX(), from.min().getZ(), from.max().getX(), from.max().getZ(), 1});
      obstacles.add(new int[]{to.min().getX(), to.min().getZ(), to.max().getX(), to.max().getZ(), 1});
      return obstacles;
   }

   /** Inside an obstacle; an endpoint (marked with a fifth value) is never paved but may be walked through. */
   private static boolean blocked(List<int[]> obstacles, int x, int z) {
      for (int[] box : obstacles) {
         if (box.length == 4 && x >= box[0] && x <= box[2] && z >= box[1] && z <= box[3]) {
            return true;
         }
      }

      return false;
   }

   private static boolean insideEndpoint(List<int[]> obstacles, int x, int z) {
      for (int[] box : obstacles) {
         if (box.length == 5 && x >= box[0] && x <= box[2] && z >= box[1] && z <= box[3]) {
            return true;
         }
      }

      return false;
   }
}
