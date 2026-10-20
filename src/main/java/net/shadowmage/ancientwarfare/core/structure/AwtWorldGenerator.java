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
import net.shadowmage.ancientwarfare.core.tile.ProtectionFlagBlockEntity;

public final class AwtWorldGenerator {
   private static final int MAX_TOWN_RADIUS_CHUNKS = 11;
   private static final int SURVEY_POINTS_PER_TICK = 32;
   private static final int CLEAR_COLUMNS_PER_TICK = 96;
   private static final int ROAD_BLOCKS_PER_TICK = 192;
   private static final int MAX_SURFACE_RANGE = 18;
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
      int regionX = Math.floorDiv(observedChunk.f_45578_, regionChunks);
      int regionZ = Math.floorDiv(observedChunk.f_45579_, regionChunks);
      RandomSource random = RandomSource.m_216335_(seed ^ (long)regionX * 341873128712L ^ (long)regionZ * 132897987541L ^ 2809927653L);
      int x = regionX * regionChunks + 16 + random.m_188503_(regionChunks - 32);
      int z = regionZ * regionChunks + 16 + random.m_188503_(regionChunks - 32);
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
      if (level.m_46472_() != Level.f_46428_) {
         return false;
      } else if (LargeIslandWorldGenerator.active(level)) {
         return false;
      } else {
         AwtWorldGenerationData data = AwtWorldGenerationData.get(level);
         if (data.active() != null) {
            return false;
         } else {
            if (!forced) {
               if (data.wasCandidateAttempted(chunk.f_45578_, chunk.f_45579_)) {
                  return false;
               }

               data.markCandidateAttempted(chunk.f_45578_, chunk.f_45579_);
               AncientWarfareCore.LOG.info("Testing natural AWT candidate chunk {}, {}", chunk.f_45578_, chunk.f_45579_);
            } else {
               level.m_6325_(chunk.f_45578_, chunk.f_45579_);
            }

            String filter = normalize(nameFilter);
            AwtWorldGenerator.TownSite site = findTownSite(level, data, chunk, forced, filter);
            if (site == null) {
               AncientWarfareCore.LOG
                  .info("Rejected natural AWT candidate chunk {}, {}: no compatible, sufficiently flat site found", chunk.f_45578_, chunk.f_45579_);
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

   private static AwtWorldGenerator.TownSite findTownSite(ServerLevel level, AwtWorldGenerationData data, ChunkPos chunk, boolean forced, String filter) {
      RandomSource random = RandomSource.m_216335_(level.m_7328_() ^ chunk.m_45588_() ^ 51366271457596L);
      int attempts = forced ? 1 : 12;
      BlockPos spawn = level.m_220360_();

      for (int attempt = 0; attempt < attempts; attempt++) {
         int x = attempt == 0 ? chunk.m_151390_() : chunk.m_151390_() + random.m_188503_(385) - 192;
         int z = attempt == 0 ? chunk.m_151393_() : chunk.m_151393_() + random.m_188503_(385) - 192;
         if (forced
            || distanceSquared(x, z, spawn.m_123341_(), spawn.m_123343_())
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
               if (level.m_6425_(center.m_7495_()).m_76178_()) {
                  Holder<Biome> biome = level.m_204166_(center);
                  List<AwtWorldGenTemplateRegistry.Template> candidates = new ArrayList<>();

                  for (AwtWorldGenTemplateRegistry.Template template : AwtWorldGenTemplateRegistry.templates()) {
                     if ((filter.isBlank() || normalize(template.name()).contains(filter)) && (forced || template.acceptsBiome(biome))) {
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
      int roll = random.m_188503_(Math.max(1, totalWeight));

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
            if (!level.m_6425_(new BlockPos(x, y, z)).m_76178_()) {
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
            data.m_77762_();
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
      int end = Math.min(total, task.progress + 32);

      for (int index = task.progress; index < end; index++) {
         int sx = index % samplesPerSide;
         int sz = index / samplesPerSide;
         int x = plan.min().m_123341_() + Math.min(plan.totalSize() - 1, sx * 8);
         int z = plan.min().m_123343_() + Math.min(plan.totalSize() - 1, sz * 8);
         if (!ensureLoaded(level, task, Set.of(ChunkPos.m_45589_(x >> 4, z >> 4)))) {
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
         if (representativeRange > 18) {
            task.phase = AwtWorldGenerationData.Phase.FAILED;
            AncientWarfareCore.LOG
               .info(
                  "Rejected queued AWT town {} at {}, {} during survey: representative surface range {} exceeds {} (raw {}..{})",
                  new Object[]{task.name, task.x, task.z, representativeRange, 18, task.minSurface, task.maxSurface}
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

      data.m_77762_();
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
      int end = Math.min(total, task.progress + 96);
      int floorY = task.y - 1;

      for (int index = task.progress; index < end; index++) {
         int lx = index % plan.totalSize();
         int lz = index / plan.totalSize();
         int x = plan.min().m_123341_() + lx;
         int z = plan.min().m_123343_() + lz;
         if (!ensureLoaded(level, task, Set.of(ChunkPos.m_45589_(x >> 4, z >> 4)))) {
            break;
         }

         int oldY = surface(level, x, z);
         int clearTopY = level.m_6924_(Types.WORLD_SURFACE, x, z) - 1;
         BlockState terrain = terrainBelowFluid(level, x, oldY, z);
         BlockState top = !terrain.m_60713_(Blocks.f_49992_) && !terrain.m_60713_(Blocks.f_49993_) ? Blocks.f_50440_.m_49966_() : terrain;
         BlockState fill = !top.m_60713_(Blocks.f_49992_) && !top.m_60713_(Blocks.f_49993_) ? Blocks.f_50493_.m_49966_() : top;

         for (int y = clearTopY; y > floorY; y--) {
            level.m_7731_(new BlockPos(x, y, z), Blocks.f_50016_.m_49966_(), 18);
         }

         for (int y = Math.min(oldY + 1, floorY); y < floorY; y++) {
            level.m_7731_(new BlockPos(x, y, z), fill, 2);
         }

         supportFoundationColumn(level, x, floorY - 1, z);
         level.m_7731_(new BlockPos(x, floorY, z), top, 2);
         task.progress = index + 1;
      }

      if (task.progress >= total) {
         task.phase = AwtWorldGenerationData.Phase.ROADS;
         task.progress = 0;
      }

      data.m_77762_();
   }

   private static void roads(ServerLevel level, AwtWorldGenerationData data, AwtWorldGenerationData.TownTask task, AwtTownPlacer.TownPlan plan) {
      int end = Math.min(plan.roads().size(), task.progress + 192);

      for (int index = task.progress; index < end; index++) {
         BlockPos pos = plan.roads().get(index);
         if (!ensureLoaded(level, task, Set.of(ChunkPos.m_45589_(pos.m_123341_() >> 4, pos.m_123343_() >> 4)))) {
            break;
         }

         level.m_7731_(new BlockPos(pos.m_123341_(), task.y - 1, pos.m_123343_()), Blocks.f_49994_.m_49966_(), 2);
         task.progress = index + 1;
      }

      if (task.progress >= plan.roads().size()) {
         task.phase = AwtWorldGenerationData.Phase.WALLS;
         task.progress = 0;
      }

      data.m_77762_();
   }

   private static void supportFoundationColumn(ServerLevel level, int x, int startY, int z) {
      BlockState support = Blocks.f_50062_.m_49966_();

      for (int y = startY; y >= level.m_141937_(); y--) {
         BlockPos pos = new BlockPos(x, y, z);
         BlockState state = level.m_8055_(pos);
         if (!state.m_60795_() && state.m_60819_().m_76178_()) {
            break;
         }

         level.m_7731_(pos, support, 2);
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
               plan.min().m_7918_(-16, 0, -16), plan.min().m_123341_() + plan.totalSize() + 15, plan.min().m_123343_() + plan.totalSize() + 15
            );
            AwsTemplatePlacer.refreshHorizontalConnections(level, plan.occupiedBlocks());
            configureTownFlags(level, plan);
            releaseForcedChunks(level);
            AncientWarfareCore.LOG.info("Completed phased AWT town {} at {}, {}, {}", new Object[]{task.name, task.x, task.y, task.z});
         }
      }

      data.m_77762_();
   }

   private static void configureTownFlags(ServerLevel level, AwtTownPlacer.TownPlan plan) {
      BlockPos min = new BlockPos(plan.min().m_123341_(), level.m_141937_(), plan.min().m_123343_());
      BlockPos max = new BlockPos(plan.min().m_123341_() + plan.totalSize() - 1, level.m_151558_() - 1, plan.min().m_123343_() + plan.totalSize() - 1);

      for (AwtTownPlacer.PlannedTemplate placement : concat(plan.walls(), plan.buildings())) {
         for (long packed : AwsTemplatePlacer.occupiedBlocks(placement.min(), placement.templatePath(), placement.facing())) {
            BlockPos pos = BlockPos.m_122022_(packed);
            if (level.m_7702_(pos) instanceof ProtectionFlagBlockEntity flag) {
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
      return level.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
   }

   private static BlockState terrainBelowFluid(ServerLevel level, int x, int startY, int z) {
      for (int y = startY; y >= level.m_141937_(); y--) {
         BlockState state = level.m_8055_(new BlockPos(x, y, z));
         if (!state.m_60795_() && state.m_60819_().m_76178_()) {
            return state;
         }
      }

      return Blocks.f_50440_.m_49966_();
   }

   private static boolean ensureLoaded(ServerLevel level, AwtWorldGenerationData.TownTask task, Set<Long> chunks) {
      for (long value : chunks) {
         ChunkPos chunk = new ChunkPos(value);
         NaturalGenerationContext.markArea(new BlockPos(chunk.m_45604_() - 16, 0, chunk.m_45605_() - 16), chunk.m_45608_() + 16, chunk.m_45609_() + 16);
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
         if (!level.m_7232_(chunk.f_45578_, chunk.f_45579_)) {
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

   private static String normalize(String value) {
      return value == null ? "" : value.toLowerCase().replace(" ", "").replace("_", "").replace("-", "");
   }

   private static record TownSite(int x, int y, int z, AwtWorldGenTemplateRegistry.Template template, int attempts) {
   }
}
