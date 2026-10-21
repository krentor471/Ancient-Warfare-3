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
import net.shadowmage.ancientwarfare.core.faction.FactionExpansion;
import net.shadowmage.ancientwarfare.core.faction.FactionRegions;
import net.shadowmage.ancientwarfare.core.faction.FactionSimulation;
import net.shadowmage.ancientwarfare.core.faction.FactionWorldData;
import net.shadowmage.ancientwarfare.core.faction.TemplateFactions;

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
         return generateInternalAt(level, chunk, forced, null, Site.SETTLEMENT);
      });
   }

   /** Builds one of faction's templates near chunk, for a settlement the faction founded. */
   public static boolean generateSettlementAt(ServerLevel level, ChunkPos chunk, String faction) {
      return generateSettlementAt(level, chunk, faction, false);
   }

   /** What a faction is building: the kind decides which of its templates fit and how the settlement is registered. */
   public enum Site {
      SETTLEMENT("structure"),
      /** A village a faction founded that could not be laid out as a town: one dry-land, non-military building. */
      VILLAGE("structure"),
      FORT("fort"),
      TRADE_POST("tradepost"),
      GUARD_POST("guardpost");

      private final String kind;

      Site(String kind) {
         this.kind = kind;
      }

      public String kind() {
         return this.kind;
      }

      boolean accepts(String path) {
         return switch (this) {
            case SETTLEMENT -> true;
            case VILLAGE -> !TemplateFactions.hasMilitaryName(path) && !TemplateFactions.isMilitary(path);
            case FORT -> TemplateFactions.isMilitary(path);
            case TRADE_POST -> TemplateFactions.isTradePost(path);
            case GUARD_POST -> TemplateFactions.isGuardPost(path);
         };
      }

      /** Garrison a new site starts with, from its template's soldiers. */
      int garrison(int soldiers) {
         return switch (this) {
            case SETTLEMENT, VILLAGE -> soldiers;
            case FORT -> Math.min(soldiers, FactionExpansion.FORT_GARRISON);
            case TRADE_POST -> Math.min(soldiers, 2);
            case GUARD_POST -> Math.min(soldiers, 6);
         };
      }
   }

   public static boolean generateSettlementAt(ServerLevel level, ChunkPos chunk, String faction, boolean fort) {
      return generateSettlementAt(level, chunk, faction, fort ? Site.FORT : Site.SETTLEMENT);
   }

   /** As generateSettlementAt, building only the faction's templates that suit the site. */
   public static boolean generateSettlementAt(ServerLevel level, ChunkPos chunk, String faction, Site site) {
      return NaturalGenerationContext.run(() -> {
         // The site may be far from any player: load the land first, heights read as the void otherwise.
         for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
               NaturalGenerationContext.markChunk(new ChunkPos(chunk.x + dx, chunk.z + dz));
               level.getChunk(chunk.x + dx, chunk.z + dz);
            }
         }

         return generateInternalAt(level, chunk, false, faction, site);
      });
   }

   private static boolean generateInternalAt(ServerLevel level, ChunkPos chunk, boolean forced, String requiredFaction, Site site) {
      if (level.dimension() != Level.OVERWORLD) {
         return false;
      } else {
         long seed = level.getSeed() ^ (long)chunk.x * 341873128712L ^ (long)chunk.z * 132897987541L;
         RandomSource random = RandomSource.create(seed);
         if (!forced && requiredFaction == null && !isCandidateChunk(level.getSeed(), chunk)) {
            return false;
         } else {
            BlockPos spawn = level.getSharedSpawnPos();
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

                  if (requiredFaction != null) {
                     AncientWarfareCore.LOG.info("No {} template fit near {}: {}", requiredFaction, chunk, rejections);
                  }

                  return false;
               }

               // Roadside posts keep close to their spot so they do not land on the road.
               int spread = site == Site.TRADE_POST || site == Site.GUARD_POST ? 8 : 32;
               int x = siteAttempt == 0 ? chunk.getMinBlockX() + random.nextInt(16) : chunk.getMiddleBlockX() + random.nextInt(spread * 2 + 1) - spread;
               int z = siteAttempt == 0 ? chunk.getMinBlockZ() + random.nextInt(16) : chunk.getMiddleBlockZ() + random.nextInt(spread * 2 + 1) - spread;
               long dx = (long)x - (long)spawn.getX();
               long dz = (long)z - (long)spawn.getZ();
               int spawnProtection = AWCorePlatform.gameplayConfig().awsSpawnProtectionDistance();
               if (forced || dx * dx + dz * dz >= (long)spawnProtection * (long)spawnProtection) {
                  int surfaceY = surfaceY(level, x, z);
                  if (surfaceY > level.getMinBuildHeight()) {
                     Holder<Biome> biome = level.getBiome(new BlockPos(x, surfaceY, z));
                     boolean waterSite = level.getFluidState(new BlockPos(x, surfaceY, z)).is(FluidTags.WATER);
                     oceanAttempt |= waterSite;
                     if (waterSite
                        && !forced
                        && requiredFaction == null
                        && AWCorePlatform.gameplayConfig().largeAquaticAwsEnabled()
                        && LargeIslandWorldGenerator.tryQueue(level, x, surfaceY, z, random)) {
                        return true;
                     }

                     List<AwsWorldGenTemplateRegistry.Template> candidates = new ArrayList<>();
                     String regionOwner = FactionRegions.ownerAt(level, x, z);

                     for (AwsWorldGenTemplateRegistry.Template templatex : AwsWorldGenTemplateRegistry.templates()) {
                        if (!templatex.requiresPhasedPlacement()
                           && templatex.acceptsBiome(biome)
                           && templatex.aquatic() == waterSite
                           && !(waterSite && site != Site.SETTLEMENT && site != Site.FORT)
                           && FactionRegions.allows(regionOwner, TemplateFactions.ofTemplate(templatex.path()))
                           && (requiredFaction == null || requiredFaction.equals(TemplateFactions.ofTemplate(templatex.path())))
                           && site.accepts(templatex.path())) {
                           candidates.add(templatex);
                        }
                     }

                     if (candidates.isEmpty() && site != Site.SETTLEMENT && !waterSite) {
                        // Borders and roads cross every climate: forts and posts may stand outside the biomes their faction usually builds in.
                        for (AwsWorldGenTemplateRegistry.Template templatex : AwsWorldGenTemplateRegistry.templates()) {
                           if (!templatex.requiresPhasedPlacement()
                              && !templatex.aquatic()
                              && requiredFaction.equals(TemplateFactions.ofTemplate(templatex.path()))
                              && site.accepts(templatex.path())) {
                              candidates.add(templatex);
                           }
                        }
                     }

                     if (candidates.isEmpty()) {
                        rejections.merge("no_template_for_" + biome.unwrapKey().map(key -> key.location().getPath()).orElse("biome"), 1, Integer::sum);
                     }

                     facing = Plane.HORIZONTAL.getRandomDirection(random);

                     for (int attempt = 0; attempt < 16 && !candidates.isEmpty(); attempt++) {
                        template = removeWeighted(candidates, random, data);
                        origin = new BlockPos(x, surfaceY + 1, z);
                        min = template.minFor(origin, facing);
                        maxX = min.getX() + template.rotatedSizeX(facing) - 1;
                        maxZ = min.getZ() + template.rotatedSizeZ(facing) - 1;
                        String rejection = !template.fitsBuildHeight(min, level.getMinBuildHeight(), level.getMaxBuildHeight())
                           ? "build_height"
                           : placementRejection(level, data, template, min, maxX, maxZ, x, z);
                        if (rejection == null && template.aquatic()) {
                           NaturalGenerationContext.markArea(min.offset(-16, 0, -16), maxX + 16, maxZ + 16);
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
                              NaturalGenerationContext.markArea(min.offset(-16, 0, -16), maxX + 16, maxZ + 16);
                              level.getChunk(min);
                              level.getChunk(new BlockPos(maxX, min.getY(), maxZ));
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
                  template.path(), min.getX(), min.getY(), min.getZ(), maxX, min.getY() + template.sizeY() - 1, maxZ, facing.get2DDataValue()
               )
            );
            TemplateFactions.TemplateInfo info = TemplateFactions.info(template.path());
            if (!info.faction().isEmpty()) {
               FactionWorldData.get(level)
                  .addSettlement(
                     info.faction(),
                     site.kind(),
                     template.path(),
                     min,
                     new BlockPos(maxX, min.getY() + template.sizeY() - 1, maxZ),
                     FactionRegions.provinceAt(level, origin.getX(), origin.getZ()).key(),
                     level.getGameTime(),
                     site.garrison(info.soldiers()),
                     site == Site.SETTLEMENT || site == Site.VILLAGE ? FactionSimulation.startingPopulation(info.soldiers(), info.civilians()) : FactionSimulation.startingPopulation(0, 0)
                  );
            }

            AncientWarfareCore.LOG
               .info("Naturally generated AWS {} at {} facing {} after {} site attempt(s)", new Object[]{template.path(), origin, facing, siteAttempt + 1});
            return true;
         }
      }
   }

   public static boolean isCandidateChunk(long worldSeed, ChunkPos chunk) {
      int regionSize = AWCorePlatform.gameplayConfig().awsRegionChunks();
      int regionX = Math.floorDiv(chunk.x, regionSize);
      int regionZ = Math.floorDiv(chunk.z, regionSize);
      long regionSeed = worldSeed ^ (long)regionX * 341873128712L ^ (long)regionZ * 132897987541L ^ 72188768385693L;
      RandomSource random = RandomSource.create(regionSeed);
      int candidateX = regionX * regionSize + random.nextInt(regionSize);
      int candidateZ = regionZ * regionSize + random.nextInt(regionSize);
      return chunk.x == candidateX && chunk.z == candidateZ;
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

      long selected = Math.floorMod(random.nextLong(), Math.max(1L, total));

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
            if (min.getX() <= town.x + halfSize + 32
               && maxX >= town.x - halfSize - 32
               && min.getZ() <= town.z + halfSize + 32
               && maxZ >= town.z - halfSize - 32) {
               return "town_overlap";
            }
         }
      }

      for (AwsWorldGenerationData.Entry entry : data.entries()) {
         if (entry.intersects(min.getX(), min.getZ(), maxX, maxZ, Math.max(4, template.border()))) {
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
         int step = Math.max(1, Math.min(maxX - min.getX() + 1, maxZ - min.getZ() + 1) / 8);

         for (int x = min.getX(); x <= maxX; x += step) {
            for (int z = min.getZ(); z <= maxZ; z += step) {
               int surface = surfaceY(level, x, z);
               minSurface = Math.min(minSurface, surface);
               maxSurface = Math.max(maxSurface, surface);
               if (!level.getFluidState(new BlockPos(x, surface, z)).isEmpty()) {
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
      int step = Math.max(2, Math.min(maxX - min.getX() + 1, maxZ - min.getZ() + 1) / 8);
      int samples = 0;
      int waterSamples = 0;
      int suitableDepthSamples = 0;

      for (int x = min.getX(); x <= maxX; x += step) {
         for (int z = min.getZ(); z <= maxZ; z += step) {
            samples++;
            int localSurface = surfaceY(level, x, z);
            BlockPos localPos = new BlockPos(x, localSurface, z);
            if (level.getBlockEntity(localPos) != null
               || !AwsTemplatePlacer.isOpenWaterTerrain(level.getBlockState(localPos)) && !AwsTemplatePlacer.isBasicNaturalTerrain(level.getBlockState(localPos))) {
               return false;
            }

            if (level.getFluidState(new BlockPos(x, localSurface, z)).is(FluidTags.WATER) && Math.abs(localSurface - waterY) <= 1) {
               waterSamples++;
               int floor = level.getHeight(Types.OCEAN_FLOOR, x, z) - 1;
               int depth = localSurface - floor;
               BlockPos floorPos = new BlockPos(x, floor, z);
               if (level.getBlockEntity(floorPos) != null || !AwsTemplatePlacer.isBasicNaturalTerrain(level.getBlockState(floorPos))) {
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
      BlockState top = level.getBlockState(new BlockPos(min.getX(), targetY, min.getZ()));
      if (top.isAir() || !top.getFluidState().isEmpty()) {
         top = Blocks.GRASS_BLOCK.defaultBlockState();
      }

      BlockState fill = !top.is(Blocks.SAND) && !top.is(Blocks.RED_SAND) ? Blocks.DIRT.defaultBlockState() : top;
      int clearTop = Math.min(level.getMaxBuildHeight() - 1, targetY + Math.max(8, template.leveling()));

      for (int x = min.getX(); x <= maxX; x++) {
         for (int z = min.getZ(); z <= maxZ; z++) {
            int oldSurface = surfaceY(level, x, z);

            for (int y = oldSurface; y > targetY && y <= clearTop; y--) {
               level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 18);
            }

            for (int y = Math.min(oldSurface + 1, targetY); y < targetY; y++) {
               level.setBlock(new BlockPos(x, y, z), fill, 2);
            }

            level.setBlock(new BlockPos(x, targetY, z), top, 2);
         }
      }
   }

   private static boolean aquaticCellsSafe(ServerLevel level, AwsWorldGenTemplateRegistry.Template template, BlockPos min, Direction facing) {
      for (long packed : AwsTemplatePlacer.occupiedBlocks(min, template.path(), facing)) {
         BlockPos pos = BlockPos.of(packed);
         BlockState state = level.getBlockState(pos);
         if (state.hasBlockEntity()
            || level.getBlockEntity(pos) != null
            || !AwsTemplatePlacer.isOpenWaterTerrain(state) && (!template.island() || !AwsTemplatePlacer.isBasicNaturalTerrain(state))) {
            return false;
         }
      }

      return AwsTemplatePlacer.aquaticGateCellsSafe(level, min, template.path(), facing, template.island());
   }

   private static int surfaceY(ServerLevel level, int x, int z) {
      NaturalGenerationContext.markChunk(new ChunkPos(x >> 4, z >> 4));
      return level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
   }
}
