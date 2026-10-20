package net.shadowmage.ancientwarfare.core.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

public final class AwsWorldGenerator {
   private static final int SITE_ATTEMPTS = 8;
   private static final int SITE_SEARCH_RADIUS = 32;
   private static final int MAX_TEMPLATE_ATTEMPTS = 16;
   private static boolean generating;

   private AwsWorldGenerator() {
   }

   public static boolean isGenerating() {
      return generating || NaturalGenerationContext.isActive();
   }

   public static boolean generateAt(ServerLevel level, ChunkPos chunk, boolean forced) {
      return NaturalGenerationContext.run(() -> {
         NaturalGenerationContext.markChunk(chunk);
         return generateInternalAt(level, chunk, forced);
      });
   }

   private static boolean generateInternalAt(ServerLevel level, ChunkPos chunk, boolean forced) {
      if (level.m_46472_() != Level.f_46428_) {
         return false;
      } else {
         long seed = level.m_7328_() ^ (long)chunk.f_45578_ * 341873128712L ^ (long)chunk.f_45579_ * 132897987541L;
         RandomSource random = RandomSource.m_216335_(seed);
         if (!forced && !isCandidateChunk(level.m_7328_(), chunk)) {
            return false;
         } else {
            BlockPos spawn = level.m_220360_();
            AwsWorldGenerationData data = AwsWorldGenerationData.get(level);
            Map<String, Integer> rejections = new LinkedHashMap<>();
            boolean oceanAttempt = false;
            int siteAttempts = forced ? 1 : 8;
            int siteAttempt = 0;

            AwsWorldGenTemplateRegistry.Template template;
            BlockPos origin;
            BlockPos min;
            int maxX;
            int maxZ;
            Direction facing;
            label237:
            while (true) {
               if (siteAttempt >= siteAttempts) {
                  if (oceanAttempt && !rejections.isEmpty()) {
                     AncientWarfareCore.LOG.info("Ocean AWS candidate {} exhausted bounded attempts: {}", chunk, rejections);
                  }

                  return false;
               }

               int x = siteAttempt == 0 ? chunk.m_45604_() + random.m_188503_(16) : chunk.m_151390_() + random.m_188503_(65) - 32;
               int z = siteAttempt == 0 ? chunk.m_45605_() + random.m_188503_(16) : chunk.m_151393_() + random.m_188503_(65) - 32;
               long dx = (long)x - (long)spawn.m_123341_();
               long dz = (long)z - (long)spawn.m_123343_();
               int spawnProtection = AWCorePlatform.gameplayConfig().awsSpawnProtectionDistance();
               if (forced || dx * dx + dz * dz >= (long)spawnProtection * (long)spawnProtection) {
                  int surfaceY = surfaceY(level, x, z);
                  if (surfaceY > level.m_141937_()) {
                     Holder<Biome> biome = level.m_204166_(new BlockPos(x, surfaceY, z));
                     boolean waterSite = level.m_6425_(new BlockPos(x, surfaceY, z)).m_205070_(FluidTags.f_13131_);
                     oceanAttempt |= waterSite;
                     if (waterSite
                        && !forced
                        && AWCorePlatform.gameplayConfig().largeAquaticAwsEnabled()
                        && LargeIslandWorldGenerator.tryQueue(level, x, surfaceY, z, random)) {
                        return true;
                     }

                     List<AwsWorldGenTemplateRegistry.Template> candidates = new ArrayList<>();

                     for (AwsWorldGenTemplateRegistry.Template templatex : AwsWorldGenTemplateRegistry.templates()) {
                        if (!templatex.requiresPhasedPlacement() && templatex.acceptsBiome(biome) && templatex.aquatic() == waterSite) {
                           candidates.add(templatex);
                        }
                     }

                     facing = Plane.HORIZONTAL.m_235690_(random);

                     for (int attempt = 0; attempt < 16 && !candidates.isEmpty(); attempt++) {
                        template = removeWeighted(candidates, random, data);
                        origin = new BlockPos(x, surfaceY + 1, z);
                        min = template.minFor(origin, facing);
                        maxX = min.m_123341_() + template.rotatedSizeX(facing) - 1;
                        maxZ = min.m_123343_() + template.rotatedSizeZ(facing) - 1;
                        String rejection = !template.fitsBuildHeight(min, level.m_141937_(), level.m_151558_())
                           ? "build_height"
                           : placementRejection(level, data, template, min, maxX, maxZ, x, z);
                        if (rejection == null && template.aquatic()) {
                           NaturalGenerationContext.markArea(min.m_7918_(-16, 0, -16), maxX + 16, maxZ + 16);
                        }

                        if (rejection == null
                           && (
                              !terrainAcceptable(level, template, min, maxX, maxZ, surfaceY)
                                 || template.aquatic() && !aquaticCellsSafe(level, template, min, facing)
                           )) {
                           rejection = template.aquatic() ? "water_coverage_depth_or_obstacle" : "terrain";
                        }

                        if (rejection != null) {
                           rejections.merge(rejection, 1, Integer::sum);
                        } else {
                           generating = true;

                           try {
                              NaturalGenerationContext.markArea(min.m_7918_(-16, 0, -16), maxX + 16, maxZ + 16);
                              level.m_46865_(min);
                              level.m_46865_(new BlockPos(maxX, min.m_123342_(), maxZ));
                              if (template.ground()) {
                                 levelTerrain(level, template, min, maxX, maxZ, surfaceY);
                              }

                              boolean clearAir = template.ground() && !template.preserveBlocks();
                              if (AwsTemplatePlacer.placeAtMin(level, min, template.path(), facing, clearAir)) {
                                 break label237;
                              }
                           } finally {
                              generating = false;
                           }
                        }
                     }
                  }
               }

               siteAttempt++;
            }

            data.add(
               new AwsWorldGenerationData.Entry(
                  template.path(), min.m_123341_(), min.m_123342_(), min.m_123343_(), maxX, min.m_123342_() + template.sizeY() - 1, maxZ, facing.m_122416_()
               )
            );
            AncientWarfareCore.LOG
               .info("Naturally generated AWS {} at {} facing {} after {} site attempt(s)", new Object[]{template.path(), origin, facing, siteAttempt + 1});
            return true;
         }
      }
   }

   public static boolean isCandidateChunk(long worldSeed, ChunkPos chunk) {
      int regionSize = AWCorePlatform.gameplayConfig().awsRegionChunks();
      int regionX = Math.floorDiv(chunk.f_45578_, regionSize);
      int regionZ = Math.floorDiv(chunk.f_45579_, regionSize);
      long regionSeed = worldSeed ^ (long)regionX * 341873128712L ^ (long)regionZ * 132897987541L ^ 72188768385693L;
      RandomSource random = RandomSource.m_216335_(regionSeed);
      int candidateX = regionX * regionSize + random.m_188503_(regionSize);
      int candidateZ = regionZ * regionSize + random.m_188503_(regionSize);
      return chunk.f_45578_ == candidateX && chunk.f_45579_ == candidateZ;
   }

   static int recentOccurrences(AwsWorldGenerationData data, String path) {
      int occurrences = 0;
      List<AwsWorldGenerationData.Entry> entries = data.entries();
      int aquatic = 0;

      for (int i = entries.size() - 1; i >= 0 && aquatic < 24; i--) {
         AwsWorldGenTemplateRegistry.Template template = AwsWorldGenTemplateRegistry.templateForActivePath(entries.get(i).template());
         if (template != null && template.aquatic()) {
            aquatic++;
            if (entries.get(i).template().equals(path)) {
               occurrences++;
            }
         }
      }

      return occurrences;
   }

   static AwsWorldGenTemplateRegistry.Template removeWeighted(
      List<AwsWorldGenTemplateRegistry.Template> candidates, RandomSource random, AwsWorldGenerationData data
   ) {
      int[] weights = new int[candidates.size()];
      long total = 0L;

      for (int i = 0; i < weights.length; i++) {
         AwsWorldGenTemplateRegistry.Template template = candidates.get(i);
         weights[i] = template.aquatic()
            ? OceanGenerationPolicy.selectionWeight(template.selectionWeight(), recentOccurrences(data, template.path()))
            : Math.max(1, template.selectionWeight());
         total += (long)weights[i];
      }

      long selected = Math.floorMod(random.m_188505_(), Math.max(1L, total));

      for (int i = 0; i < candidates.size(); i++) {
         selected -= (long)weights[i];
         if (selected < 0L) {
            return candidates.remove(i);
         }
      }

      return candidates.remove(candidates.size() - 1);
   }

   private static String placementRejection(
      ServerLevel level, AwsWorldGenerationData data, AwsWorldGenTemplateRegistry.Template template, BlockPos min, int maxX, int maxZ, int x, int z
   ) {
      for (AwtWorldGenerationData.TownTask town : AwtWorldGenerationData.get(level).towns()) {
         if (town.phase != AwtWorldGenerationData.Phase.FAILED) {
            AwtWorldGenTemplateRegistry.Template townTemplate = AwtWorldGenTemplateRegistry.templateForActivePath(town.path);
            int halfSize = townTemplate == null ? 192 : townTemplate.chunks() * 8;
            if (min.m_123341_() <= town.x + halfSize + 32
               && maxX >= town.x - halfSize - 32
               && min.m_123343_() <= town.z + halfSize + 32
               && maxZ >= town.z - halfSize - 32) {
               return "town_overlap";
            }
         }
      }

      for (AwsWorldGenerationData.Entry entry : data.entries()) {
         if (entry.intersects(min.m_123341_(), min.m_123343_(), maxX, maxZ, Math.max(4, template.border()))) {
            return "aws_overlap";
         }

         int minimumDistance = template.aquatic()
            ? AWCorePlatform.gameplayConfig().awsAquaticMinimumDistance()
            : AWCorePlatform.gameplayConfig().awsMinimumDistance();
         if (entry.horizontalDistanceSquared(x, z) < (double)((long)minimumDistance * (long)minimumDistance)) {
            return "minimum_distance";
         }

         if (entry.template().equals(template.path())) {
            long distance = (long)template.minDuplicateDistance();
            if (entry.horizontalDistanceSquared(x, z) < (double)(distance * distance)) {
               return "duplicate_distance";
            }
         }

         if (template.unique() && entry.template().equals(template.path())) {
            return "unique_template";
         }
      }

      return null;
   }

   private static boolean terrainAcceptable(ServerLevel level, AwsWorldGenTemplateRegistry.Template template, BlockPos min, int maxX, int maxZ, int targetY) {
      if (template.aquatic()) {
         return aquaticTerrainAcceptable(level, template, min, maxX, maxZ, targetY);
      } else {
         int minSurface = Integer.MAX_VALUE;
         int maxSurface = Integer.MIN_VALUE;
         int step = Math.max(1, Math.min(maxX - min.m_123341_() + 1, maxZ - min.m_123343_() + 1) / 8);

         for (int x = min.m_123341_(); x <= maxX; x += step) {
            for (int z = min.m_123343_(); z <= maxZ; z += step) {
               int surface = surfaceY(level, x, z);
               minSurface = Math.min(minSurface, surface);
               maxSurface = Math.max(maxSurface, surface);
               if (!level.m_6425_(new BlockPos(x, surface, z)).m_76178_()) {
                  return false;
               }
            }
         }

         int leveling = Math.max(2, template.leveling());
         int fill = Math.max(2, template.fill());
         return maxSurface - targetY <= leveling && targetY - minSurface <= fill && maxSurface - minSurface <= Math.max(leveling, fill);
      }
   }

   private static boolean aquaticTerrainAcceptable(
      ServerLevel level, AwsWorldGenTemplateRegistry.Template template, BlockPos min, int maxX, int maxZ, int waterY
   ) {
      int step = Math.max(2, Math.min(maxX - min.m_123341_() + 1, maxZ - min.m_123343_() + 1) / 8);
      int samples = 0;
      int waterSamples = 0;
      int suitableDepthSamples = 0;

      for (int x = min.m_123341_(); x <= maxX; x += step) {
         for (int z = min.m_123343_(); z <= maxZ; z += step) {
            samples++;
            int localSurface = surfaceY(level, x, z);
            BlockPos localPos = new BlockPos(x, localSurface, z);
            if (level.m_7702_(localPos) != null
               || !AwsTemplatePlacer.isOpenWaterTerrain(level.m_8055_(localPos)) && !AwsTemplatePlacer.isBasicNaturalTerrain(level.m_8055_(localPos))) {
               return false;
            }

            if (level.m_6425_(new BlockPos(x, localSurface, z)).m_205070_(FluidTags.f_13131_) && Math.abs(localSurface - waterY) <= 1) {
               waterSamples++;
               int floor = level.m_6924_(Types.OCEAN_FLOOR, x, z) - 1;
               int depth = localSurface - floor;
               BlockPos floorPos = new BlockPos(x, floor, z);
               if (level.m_7702_(floorPos) != null || !AwsTemplatePlacer.isBasicNaturalTerrain(level.m_8055_(floorPos))) {
                  return false;
               }

               if (OceanGenerationPolicy.depthAcceptable(template.island(), template.minWaterDepth(), template.maxWaterDepth(), depth)) {
                  suitableDepthSamples++;
               } else if (!template.island()) {
                  return false;
               }
            }
         }
      }

      return OceanGenerationPolicy.coverageAcceptable((long)waterSamples, (long)samples)
         && (!template.island() || OceanGenerationPolicy.islandDepthAcceptable((long)suitableDepthSamples, (long)waterSamples));
   }

   private static void levelTerrain(ServerLevel level, AwsWorldGenTemplateRegistry.Template template, BlockPos min, int maxX, int maxZ, int targetY) {
      BlockState top = level.m_8055_(new BlockPos(min.m_123341_(), targetY, min.m_123343_()));
      if (top.m_60795_() || !top.m_60819_().m_76178_()) {
         top = Blocks.f_50440_.m_49966_();
      }

      BlockState fill = !top.m_60713_(Blocks.f_49992_) && !top.m_60713_(Blocks.f_49993_) ? Blocks.f_50493_.m_49966_() : top;
      int clearTop = Math.min(level.m_151558_() - 1, targetY + Math.max(8, template.leveling()));

      for (int x = min.m_123341_(); x <= maxX; x++) {
         for (int z = min.m_123343_(); z <= maxZ; z++) {
            int oldSurface = surfaceY(level, x, z);

            for (int y = oldSurface; y > targetY && y <= clearTop; y--) {
               level.m_7731_(new BlockPos(x, y, z), Blocks.f_50016_.m_49966_(), 18);
            }

            for (int y = Math.min(oldSurface + 1, targetY); y < targetY; y++) {
               level.m_7731_(new BlockPos(x, y, z), fill, 2);
            }

            level.m_7731_(new BlockPos(x, targetY, z), top, 2);
         }
      }
   }

   private static boolean aquaticCellsSafe(ServerLevel level, AwsWorldGenTemplateRegistry.Template template, BlockPos min, Direction facing) {
      for (long packed : AwsTemplatePlacer.occupiedBlocks(min, template.path(), facing)) {
         BlockPos pos = BlockPos.m_122022_(packed);
         BlockState state = level.m_8055_(pos);
         if (state.m_155947_()
            || level.m_7702_(pos) != null
            || !AwsTemplatePlacer.isOpenWaterTerrain(state) && (!template.island() || !AwsTemplatePlacer.isBasicNaturalTerrain(state))) {
            return false;
         }
      }

      return AwsTemplatePlacer.aquaticGateCellsSafe(level, min, template.path(), facing, template.island());
   }

   private static int surfaceY(ServerLevel level, int x, int z) {
      NaturalGenerationContext.markChunk(new ChunkPos(x >> 4, z >> 4));
      return level.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
   }
}
