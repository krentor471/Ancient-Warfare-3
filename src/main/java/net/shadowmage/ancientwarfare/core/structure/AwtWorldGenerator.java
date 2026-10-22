package net.shadowmage.ancientwarfare.core.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.faction.FactionExpansion;
import net.shadowmage.ancientwarfare.core.faction.FactionRegions;
import net.shadowmage.ancientwarfare.core.faction.FactionSimulation;
import net.shadowmage.ancientwarfare.core.faction.FactionWorldData;
import net.shadowmage.ancientwarfare.core.faction.TemplateFactions;
import net.shadowmage.ancientwarfare.core.tile.ProtectionFlagBlockEntity;

public final class AwtWorldGenerator {
   private static final int MAX_TOWN_RADIUS_CHUNKS = 11;
   private static final int SURVEY_POINTS_PER_TICK = 64;
   private static final int CLEAR_COLUMNS_PER_TICK = 192;
   private static final int ROAD_BLOCKS_PER_TICK = 384;
   private static final int MAX_SURFACE_RANGE = 18;
   private static final int FACTION_SITE_RANGE = 15;
   private static final int FACTION_TOWN_SURVEY_RANGE = 28;
   private static final int SITE_ATTEMPTS = 12;
   private static final int SITE_SEARCH_RADIUS = 192;
   private static final int QUICK_SURVEY_GRID = 5;
   private static String cachedPlanKey = "";
   private static AwtTownPlacer.TownPlan cachedPlan;
   private static final Set<Long> FORCED_CHUNKS = new HashSet<>();
   private static BlockPos forcedOwner;

   private AwtWorldGenerator() {
   }

   public static ChunkPos candidateChunk(long seed, ChunkPos observedChunk) {
      int regionChunks = AWCorePlatform.gameplayConfig().awtRegionChunks();
      int regionX = Math.floorDiv(observedChunk.x, regionChunks);
      int regionZ = Math.floorDiv(observedChunk.z, regionChunks);
      RandomSource random = RandomSource.create(seed ^ (long)regionX * 341873128712L ^ (long)regionZ * 132897987541L ^ 2809927653L);
      int x = regionX * regionChunks + 16 + random.nextInt(regionChunks - 32);
      int z = regionZ * regionChunks + 16 + random.nextInt(regionChunks - 32);
      return new ChunkPos(x, z);
   }

   public static boolean isCandidateChunk(long seed, ChunkPos chunk) {
      return candidateChunk(seed, chunk).equals(chunk);
   }

   public static boolean queueAt(ServerLevel level, ChunkPos chunk, boolean forced, String nameFilter) {
      return NaturalGenerationContext.run(() -> {
         NaturalGenerationContext.markChunk(chunk);
         return queueInternalAt(level, chunk, forced, nameFilter);
      });
   }

   private static boolean queueInternalAt(ServerLevel level, ChunkPos chunk, boolean forced, String nameFilter) {
      if (level.dimension() != Level.OVERWORLD) {
         return false;
      } else if (LargeIslandWorldGenerator.active(level)) {
         return false;
      } else {
         AwtWorldGenerationData data = AwtWorldGenerationData.get(level);
         if (data.active() != null) {
            return false;
         } else {
            if (!forced) {
               if (data.wasCandidateAttempted(chunk.x, chunk.z)) {
                  return false;
               }

               data.markCandidateAttempted(chunk.x, chunk.z);
               AncientWarfareCore.LOG.info("Testing natural AWT candidate chunk {}, {}", chunk.x, chunk.z);
            } else {
               level.getChunk(chunk.x, chunk.z);
            }

            String filter = normalize(nameFilter);
            AwtWorldGenerator.TownSite site = findTownSite(level, data, chunk, forced, filter);
            if (site == null) {
               AncientWarfareCore.LOG
                  .info("Rejected natural AWT candidate chunk {}, {}: no compatible, sufficiently flat site found", chunk.x, chunk.z);
               return false;
            } else {
               int x = site.x();
               int y = site.y();
               int z = site.z();
               AwtWorldGenTemplateRegistry.Template selected = site.template();
               int halfSize = selected.chunks() * 8;
               int townMinX = x - halfSize;
               int townMinZ = z - halfSize;
               int townMaxX = x + halfSize - 1;
               int townMaxZ = z + halfSize - 1;

               for (AwsWorldGenerationData.Entry entry : AwsWorldGenerationData.get(level).entries()) {
                  if (entry.intersects(townMinX, townMinZ, townMaxX, townMaxZ, 24)) {
                     AncientWarfareCore.LOG.info("AWT candidate {}, {} claims priority over existing AWS {}", new Object[]{x, z, entry.template()});
                  }
               }

               data.add(new AwtWorldGenerationData.TownTask(selected.path(), selected.name(), x, y, z));
               AncientWarfareCore.LOG
                  .info("Queued phased AWT town {} at {}, {}, {} after {} site attempt(s)", new Object[]{selected.name(), x, y, z, site.attempts()});
               return true;
            }
         }
      }
   }

   /**
    * Whether a faction town of this many chunks fits here: dry, and flat enough for the town planner.
    * The land is loaded (generated if need be) first, as heights of unloaded land read as the void.
    */
   public static boolean siteAcceptable(ServerLevel level, int x, int z, int chunks) {
      if (!roughSiteAcceptable(level, x, z, chunks)) {
         return false;
      }

      return NaturalGenerationContext.run(() -> {
         int halfSize = Math.max(16, chunks * 8);
         List<Integer> samples = new ArrayList<>(25);

         for (int gx = 0; gx < 5; gx++) {
            for (int gz = 0; gz < 5; gz++) {
               int sx = x - halfSize + halfSize * 2 * gx / 4;
               int sz = z - halfSize + halfSize * 2 * gz / 4;
               NaturalGenerationContext.markChunk(new ChunkPos(sx >> 4, sz >> 4));
               int y = level.getChunk(sx >> 4, sz >> 4).getHeight(Types.MOTION_BLOCKING_NO_LEAVES, sx & 15, sz & 15);
               if (!level.getFluidState(new BlockPos(sx, y, sz)).isEmpty()) {
                  AncientWarfareCore.LOG.debug("Faction town site {}, {}: water at {}, {}", x, z, sx, sz);
                  return false;
               }

               samples.add(y);
            }
         }

         // Stricter than the town survey's own check, which samples more finely and might still refuse.
         samples.sort(Integer::compareTo);
         AncientWarfareCore.LOG.debug("Faction town site {}, {}: heights {}", x, z, samples);
         return percentile(samples, 0.1) + FACTION_SITE_RANGE >= percentile(samples, 0.9);
      });
   }

   /**
    * The same look at a site from the world generator's noise alone, without generating the land:
    * sea over the terrain or too steep a spread of heights rules it out at once. Lakes, rivers cut
    * by carvers and the like only show in the real check that follows, which loads the land.
    */
   public static boolean roughSiteAcceptable(ServerLevel level, int x, int z, int chunks) {
      net.minecraft.world.level.chunk.ChunkGenerator generator = level.getChunkSource().getGenerator();
      net.minecraft.world.level.levelgen.RandomState random = level.getChunkSource().randomState();
      int halfSize = Math.max(16, chunks * 8);
      List<Integer> samples = new ArrayList<>(25);

      for (int gx = 0; gx < 5; gx++) {
         for (int gz = 0; gz < 5; gz++) {
            int sx = x - halfSize + halfSize * 2 * gx / 4;
            int sz = z - halfSize + halfSize * 2 * gz / 4;
            int surface = generator.getBaseHeight(sx, sz, Types.WORLD_SURFACE_WG, level, random);
            int floor = generator.getBaseHeight(sx, sz, Types.OCEAN_FLOOR_WG, level, random);
            if (surface > floor) {
               AncientWarfareCore.LOG.debug("Faction town site {}, {}: sea at {}, {} (noise)", x, z, sx, sz);
               return false;
            }

            samples.add(surface);
         }
      }

      samples.sort(Integer::compareTo);
      return percentile(samples, 0.1) + FACTION_SITE_RANGE >= percentile(samples, 0.9);
   }

   /** Size in chunks of a faction town template; 0 when it cannot be read. */
   public static int townChunks(String path) {
      try {
         return AwtTownPlacer.plan(BlockPos.ZERO, path, Direction.NORTH).totalSize() / 16;
      } catch (RuntimeException exception) {
         return 0;
      }
   }

   /** Whether a town is being built right now; only one is built at a time. */
   public static boolean busy(ServerLevel level) {
      return AwtWorldGenerationData.get(level).active() != null;
   }

   /** Queues a faction's generated town (see FactionTowns) at a site already checked; false while another town is being built. */
   public static boolean queueFactionTown(ServerLevel level, String path, int x, int z) {
      return queueFactionTown(level, path, x, z, 0);
   }

   /** As queueFactionTown, for a faction settlement that exists on paper and stands once the town is done. */
   public static boolean queueFactionTown(ServerLevel level, String path, int x, int z, int settlement) {
      AwtWorldGenerationData data = AwtWorldGenerationData.get(level);
      if (data.active() != null || LargeIslandWorldGenerator.active(level)) {
         return false;
      }

      int y = NaturalGenerationContext.run(() -> level.getChunk(x >> 4, z >> 4).getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1);
      AwtWorldGenerationData.TownTask task = new AwtWorldGenerationData.TownTask(path, path, x, y, z);
      task.settlement = settlement;
      data.add(task);
      AncientWarfareCore.LOG.info("Queued faction town {} at {}, {}, {}", new Object[]{path, x, y, z});
      return true;
   }

   private static AwtWorldGenerator.TownSite findTownSite(ServerLevel level, AwtWorldGenerationData data, ChunkPos chunk, boolean forced, String filter) {
      RandomSource random = RandomSource.create(level.getSeed() ^ chunk.toLong() ^ 51366271457596L);
      int attempts = forced ? 1 : 12;
      BlockPos spawn = level.getSharedSpawnPos();

      for (int attempt = 0; attempt < attempts; attempt++) {
         int x = attempt == 0 ? chunk.getMiddleBlockX() : chunk.getMiddleBlockX() + random.nextInt(385) - 192;
         int z = attempt == 0 ? chunk.getMiddleBlockZ() : chunk.getMiddleBlockZ() + random.nextInt(385) - 192;
         if (forced
            || distanceSquared(x, z, spawn.getX(), spawn.getZ())
               >= (long)AWCorePlatform.gameplayConfig().awtSpawnProtectionDistance() * (long)AWCorePlatform.gameplayConfig().awtSpawnProtectionDistance()) {
            boolean nearTown = false;

            for (AwtWorldGenerationData.TownTask town : data.towns()) {
               if (town.phase != AwtWorldGenerationData.Phase.FAILED
                  && town.distanceSquared(x, z)
                     < (double)((long)AWCorePlatform.gameplayConfig().awtMinimumDistance() * (long)AWCorePlatform.gameplayConfig().awtMinimumDistance())) {
                  nearTown = true;
                  break;
               }
            }

            if (!nearTown) {
               int y = surface(level, x, z) + 1;
               BlockPos center = new BlockPos(x, y, z);
               if (level.getFluidState(center.below()).isEmpty()) {
                  Holder<Biome> biome = level.getBiome(center);
                  List<AwtWorldGenTemplateRegistry.Template> candidates = new ArrayList<>();

                  String regionOwner = FactionRegions.ownerAt(level, x, z);

                  for (AwtWorldGenTemplateRegistry.Template template : AwtWorldGenTemplateRegistry.templates()) {
                     if ((filter.isBlank() || normalize(template.name()).contains(filter))
                        && (forced || template.acceptsBiome(biome) && FactionRegions.allows(regionOwner, TemplateFactions.ofTown(template.name())))) {
                        candidates.add(template);
                     }
                  }

                  if (!candidates.isEmpty()) {
                     AwtWorldGenTemplateRegistry.Template selected = selectWeighted(candidates, random);
                     if (forced || quickTerrainAcceptable(level, x, z, selected.chunks())) {
                        return new AwtWorldGenerator.TownSite(x, y, z, selected, attempt + 1);
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private static AwtWorldGenTemplateRegistry.Template selectWeighted(List<AwtWorldGenTemplateRegistry.Template> candidates, RandomSource random) {
      int totalWeight = candidates.stream().mapToInt(template -> Math.max(1, template.weight())).sum();
      int roll = random.nextInt(Math.max(1, totalWeight));

      for (AwtWorldGenTemplateRegistry.Template candidate : candidates) {
         roll -= Math.max(1, candidate.weight());
         if (roll < 0) {
            return candidate;
         }
      }

      return candidates.get(candidates.size() - 1);
   }

   private static boolean quickTerrainAcceptable(ServerLevel level, int centerX, int centerZ, int chunks) {
      int halfSize = Math.max(16, chunks * 8);
      List<Integer> samples = new ArrayList<>(25);

      for (int gx = 0; gx < 5; gx++) {
         for (int gz = 0; gz < 5; gz++) {
            int x = centerX - halfSize + Math.round((float)halfSize * 2.0F * (float)gx / 4.0F);
            int z = centerZ - halfSize + Math.round((float)halfSize * 2.0F * (float)gz / 4.0F);
            int y = surface(level, x, z);
            if (!level.getFluidState(new BlockPos(x, y, z)).isEmpty()) {
               return false;
            }

            samples.add(y);
         }
      }

      samples.sort(Integer::compareTo);
      return percentile(samples, 0.1) + 18 >= percentile(samples, 0.9);
   }

   public static void tick(ServerLevel level) {
      NaturalGenerationContext.run(() -> tickInternal(level));
   }

   private static void tickInternal(ServerLevel level) {
      AwtWorldGenerationData data = AwtWorldGenerationData.get(level);
      if (data.removeFailed()) {
         releaseForcedChunks(level);
      }

      AwtWorldGenerationData.TownTask task = data.active();
      if (task == null) {
         releaseForcedChunks(level);
      } else {
         AwtWorldGenTemplateRegistry.Template template = AwtWorldGenTemplateRegistry.templates()
            .stream()
            .filter(candidate -> candidate.path().equals(task.path))
            .findFirst()
            .orElse(null);

         AwtTownPlacer.TownPlan plan;
         try {
            plan = plan(task);
         } catch (RuntimeException var6) {
            task.phase = AwtWorldGenerationData.Phase.FAILED;
            data.setDirty();
            AncientWarfareCore.LOG.warn("Unable to rebuild phased AWT plan {}", task.path, var6);
            return;
         }

         switch (task.phase) {
            case SURVEY:
               survey(level, data, task, plan);
               break;
            case CLEAR:
               clear(level, data, task, plan);
               break;
            case ROADS:
               roads(level, data, task, plan);
               break;
            case WALLS:
               templates(level, data, task, plan, plan.walls(), AwtWorldGenerationData.Phase.BUILDINGS);
               break;
            case BUILDINGS:
               templates(level, data, task, plan, plan.buildings(), AwtWorldGenerationData.Phase.COMPLETE);
         }
      }
   }

   private static AwtTownPlacer.TownPlan plan(AwtWorldGenerationData.TownTask task) {
      String key = task.path + "@" + task.x + "," + task.y + "," + task.z;
      if (!key.equals(cachedPlanKey) || cachedPlan == null) {
         cachedPlan = AwtTownPlacer.plan(new BlockPos(task.x, task.y, task.z), task.path, Direction.NORTH);
         cachedPlanKey = key;
      }

      return cachedPlan;
   }

   private static void survey(ServerLevel level, AwtWorldGenerationData data, AwtWorldGenerationData.TownTask task, AwtTownPlacer.TownPlan plan) {
      int samplesPerSide = Math.max(4, plan.totalSize() / 8);
      int total = samplesPerSide * samplesPerSide;
      int end = Math.min(total, task.progress + SURVEY_POINTS_PER_TICK);

      for (int index = task.progress; index < end; index++) {
         int sx = index % samplesPerSide;
         int sz = index / samplesPerSide;
         int x = plan.min().getX() + Math.min(plan.totalSize() - 1, sx * 8);
         int z = plan.min().getZ() + Math.min(plan.totalSize() - 1, sz * 8);
         if (!ensureLoaded(level, task, Set.of(ChunkPos.asLong(x >> 4, z >> 4)))) {
            break;
         }

         int y = surface(level, x, z);
         task.minSurface = Math.min(task.minSurface, y);
         task.maxSurface = Math.max(task.maxSurface, y);
         task.surfaceSamples.add(y);
         task.progress = index + 1;
      }

      if (task.progress >= total) {
         task.surfaceSamples.sort(Integer::compareTo);
         int low = percentile(task.surfaceSamples, 0.1);
         int median = percentile(task.surfaceSamples, 0.5);
         int high = percentile(task.surfaceSamples, 0.9);
         int representativeRange = high - low;
         // A faction must build its capital and villages somewhere in its own hilly land; their sites were checked already.
         int maxRange = FactionTowns.isGenerated(task.path) ? FACTION_TOWN_SURVEY_RANGE : MAX_SURFACE_RANGE;
         if (representativeRange > maxRange) {
            task.phase = AwtWorldGenerationData.Phase.FAILED;
            AncientWarfareCore.LOG
               .info(
                  "Rejected queued AWT town {} at {}, {} during survey: representative surface range {} exceeds {} (raw {}..{})",
                  new Object[]{task.name, task.x, task.z, representativeRange, maxRange, task.minSurface, task.maxSurface}
               );
         } else {
            task.y = median + 1;
            task.phase = AwtWorldGenerationData.Phase.CLEAR;
            AncientWarfareCore.LOG
               .info(
                  "Accepted AWT terrain for {} at {}, {}: level {}, representative range {}, raw {}..{}",
                  new Object[]{task.name, task.x, task.z, task.y, representativeRange, task.minSurface, task.maxSurface}
               );
         }

         task.progress = 0;
         task.surfaceSamples.clear();
      }

      data.setDirty();
   }

   private static int percentile(List<Integer> sorted, double percentile) {
      if (sorted.isEmpty()) {
         return 0;
      } else {
         int index = (int)Math.round((double)(sorted.size() - 1) * percentile);
         return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
      }
   }

   private static void clear(ServerLevel level, AwtWorldGenerationData data, AwtWorldGenerationData.TownTask task, AwtTownPlacer.TownPlan plan) {
      int total = plan.totalSize() * plan.totalSize();
      int end = Math.min(total, task.progress + CLEAR_COLUMNS_PER_TICK);
      int floorY = task.y - 1;

      for (int index = task.progress; index < end; index++) {
         int lx = index % plan.totalSize();
         int lz = index / plan.totalSize();
         int x = plan.min().getX() + lx;
         int z = plan.min().getZ() + lz;
         if (!ensureLoaded(level, task, Set.of(ChunkPos.asLong(x >> 4, z >> 4)))) {
            break;
         }

         int oldY = surface(level, x, z);
         int clearTopY = level.getHeight(Types.WORLD_SURFACE, x, z) - 1;
         BlockState terrain = terrainBelowFluid(level, x, oldY, z);
         BlockState top = !terrain.is(Blocks.SAND) && !terrain.is(Blocks.RED_SAND) ? Blocks.GRASS_BLOCK.defaultBlockState() : terrain;
         BlockState fill = !top.is(Blocks.SAND) && !top.is(Blocks.RED_SAND) ? Blocks.DIRT.defaultBlockState() : top;

         for (int y = clearTopY; y > floorY; y--) {
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 18);
         }

         for (int y = Math.min(oldY + 1, floorY); y < floorY; y++) {
            level.setBlock(new BlockPos(x, y, z), fill, 2);
         }

         supportFoundationColumn(level, x, floorY - 1, z);
         level.setBlock(new BlockPos(x, floorY, z), top, 2);
         task.progress = index + 1;
      }

      if (task.progress >= total) {
         task.phase = AwtWorldGenerationData.Phase.ROADS;
         task.progress = 0;
      }

      data.setDirty();
   }

   private static void roads(ServerLevel level, AwtWorldGenerationData data, AwtWorldGenerationData.TownTask task, AwtTownPlacer.TownPlan plan) {
      int end = Math.min(plan.roads().size(), task.progress + ROAD_BLOCKS_PER_TICK);

      for (int index = task.progress; index < end; index++) {
         BlockPos pos = plan.roads().get(index);
         if (!ensureLoaded(level, task, Set.of(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4)))) {
            break;
         }

         level.setBlock(new BlockPos(pos.getX(), task.y - 1, pos.getZ()), Blocks.GRAVEL.defaultBlockState(), 2);
         task.progress = index + 1;
      }

      if (task.progress >= plan.roads().size()) {
         task.phase = AwtWorldGenerationData.Phase.WALLS;
         task.progress = 0;
      }

      data.setDirty();
   }

   private static void supportFoundationColumn(ServerLevel level, int x, int startY, int z) {
      BlockState support = Blocks.SANDSTONE.defaultBlockState();

      for (int y = startY; y >= level.getMinBuildHeight(); y--) {
         BlockPos pos = new BlockPos(x, y, z);
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.getFluidState().isEmpty()) {
            break;
         }

         level.setBlock(pos, support, 2);
      }
   }

   private static void templates(
      ServerLevel level,
      AwtWorldGenerationData data,
      AwtWorldGenerationData.TownTask task,
      AwtTownPlacer.TownPlan plan,
      List<AwtTownPlacer.PlannedTemplate> placements,
      AwtWorldGenerationData.Phase next
   ) {
      if (task.progress < placements.size()) {
         AwtTownPlacer.PlannedTemplate old = placements.get(task.progress);
         if (!ensureLoaded(level, task, AwsTemplatePlacer.requiredChunks(old.min(), old.templatePath(), old.facing()))) {
            return;
         }

         AwtTownPlacer.placePlannedTemplate(level, old, plan.occupiedBlocks());
         task.progress++;
      }

      if (task.progress >= placements.size()) {
         task.phase = next;
         task.progress = 0;
         if (next == AwtWorldGenerationData.Phase.COMPLETE) {
            NaturalGenerationContext.markArea(
               plan.min().offset(-16, 0, -16), plan.min().getX() + plan.totalSize() + 15, plan.min().getZ() + plan.totalSize() + 15
            );
            AwsTemplatePlacer.refreshHorizontalConnections(level, plan.occupiedBlocks());
            configureTownFlags(level, plan);
            registerSettlement(level, task, plan);
            releaseForcedChunks(level);
            AncientWarfareCore.LOG.info("Completed phased AWT town {} at {}, {}, {}", new Object[]{task.name, task.x, task.y, task.z});
         }
      }

      data.setDirty();
   }

   private static void configureTownFlags(ServerLevel level, AwtTownPlacer.TownPlan plan) {
      BlockPos min = new BlockPos(plan.min().getX(), level.getMinBuildHeight(), plan.min().getZ());
      BlockPos max = new BlockPos(plan.min().getX() + plan.totalSize() - 1, level.getMaxBuildHeight() - 1, plan.min().getZ() + plan.totalSize() - 1);

      for (AwtTownPlacer.PlannedTemplate placement : concat(plan.walls(), plan.buildings())) {
         for (long packed : AwsTemplatePlacer.occupiedBlocks(placement.min(), placement.templatePath(), placement.facing())) {
            BlockPos pos = BlockPos.of(packed);
            if (level.getBlockEntity(pos) instanceof ProtectionFlagBlockEntity flag) {
               flag.configureTerritory(min, max);
            }
         }
      }
   }

   private static List<AwtTownPlacer.PlannedTemplate> concat(List<AwtTownPlacer.PlannedTemplate> first, List<AwtTownPlacer.PlannedTemplate> second) {
      List<AwtTownPlacer.PlannedTemplate> result = new ArrayList<>(first.size() + second.size());
      result.addAll(first);
      result.addAll(second);
      return result;
   }

   private static int surface(ServerLevel level, int x, int z) {
      NaturalGenerationContext.markChunk(new ChunkPos(x >> 4, z >> 4));
      return level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
   }

   private static BlockState terrainBelowFluid(ServerLevel level, int x, int startY, int z) {
      for (int y = startY; y >= level.getMinBuildHeight(); y--) {
         BlockState state = level.getBlockState(new BlockPos(x, y, z));
         if (!state.isAir() && state.getFluidState().isEmpty()) {
            return state;
         }
      }

      return Blocks.GRASS_BLOCK.defaultBlockState();
   }

   private static boolean ensureLoaded(ServerLevel level, AwtWorldGenerationData.TownTask task, Set<Long> chunks) {
      for (long value : chunks) {
         ChunkPos chunk = new ChunkPos(value);
         NaturalGenerationContext.markArea(new BlockPos(chunk.getMinBlockX() - 16, 0, chunk.getMinBlockZ() - 16), chunk.getMaxBlockX() + 16, chunk.getMaxBlockZ() + 16);
      }

      BlockPos owner = new BlockPos(task.x, task.y, task.z);
      if (!owner.equals(forcedOwner) || !FORCED_CHUNKS.equals(chunks)) {
         releaseForcedChunks(level);
         forcedOwner = owner;

         for (long value : chunks) {
            ChunkPos chunk = new ChunkPos(value);
            if (AWCorePlatform.setChunkForced(level, owner, chunk, true, true)) {
               FORCED_CHUNKS.add(value);
            }
         }
      }

      for (long valuex : chunks) {
         ChunkPos chunk = new ChunkPos(valuex);
         if (!level.hasChunk(chunk.x, chunk.z)) {
            return false;
         }
      }

      return true;
   }

   private static void releaseForcedChunks(ServerLevel level) {
      if (forcedOwner != null) {
         for (long value : Set.copyOf(FORCED_CHUNKS)) {
            AWCorePlatform.setChunkForced(level, forcedOwner, new ChunkPos(value), false, true);
         }
      }

      FORCED_CHUNKS.clear();
      forcedOwner = null;
   }

   private static long distanceSquared(int x1, int z1, int x2, int z2) {
      long dx = (long)x1 - (long)x2;
      long dz = (long)z1 - (long)z2;
      return dx * dx + dz * dz;
   }

   private static void registerSettlement(ServerLevel level, AwtWorldGenerationData.TownTask task, AwtTownPlacer.TownPlan plan) {
      boolean generated = FactionTowns.isGenerated(task.path);
      String faction = generated ? FactionTowns.factionOf(task.path) : TemplateFactions.ofTown(task.name);
      if (!faction.isEmpty()) {
         int soldiers = 0;
         int civilians = 0;

         for (List<AwtTownPlacer.PlannedTemplate> placements : List.of(plan.walls(), plan.buildings())) {
            for (AwtTownPlacer.PlannedTemplate placement : placements) {
               TemplateFactions.TemplateInfo info = TemplateFactions.info(placement.templatePath());
               if (info.faction().equals(faction)) {
                  soldiers += info.soldiers();
                  civilians += info.civilians();
               }
            }
         }

         BlockPos min = plan.min();
         BlockPos max = min.offset(plan.totalSize() - 1, 0, plan.totalSize() - 1);
         String kind = !generated ? "town" : FactionTowns.isCapital(task.path) ? FactionTowns.CAPITAL : FactionTowns.VILLAGE;
         FactionWorldData data = FactionWorldData.get(level);
         FactionWorldData.Settlement founded = task.settlement == 0 ? null : data.settlement(task.settlement);
         if (founded != null && !founded.built()) {
            // A settlement that lived on paper now stands; its people and garrison are the simulated ones.
            founded.place(task.path, min, max);
            data.setDirty();
            FactionTowns.plantFields(level, plan, task.y - 1, faction, FactionTowns.CAPITAL.equals(kind));
            return;
         }

         data.addSettlement(
               faction,
               kind,
               task.path,
               min,
               max,
               FactionRegions.provinceAt(level, task.x, task.z).key(),
               level.getGameTime(),
               soldiers,
               FactionSimulation.startingPopulation(soldiers, civilians)
            );
         if (generated) {
            FactionTowns.plantFields(level, plan, task.y - 1, faction, FactionTowns.CAPITAL.equals(kind));
            FactionExpansion.townBuilt(level, faction, kind, task.x, task.z);
         }
      }
   }

   private static String normalize(String value) {
      return value == null ? "" : value.toLowerCase().replace(" ", "").replace("_", "").replace("-", "");
   }

   private static record TownSite(int x, int y, int z, AwtWorldGenTemplateRegistry.Template template, int attempts) {
   }
}
