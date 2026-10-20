package net.shadowmage.ancientwarfare.core.structure;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformGameplayConfig;

public final class LargeIslandWorldGenerator {
   private static final int LARGE_ISLAND_ROLL = 8;
   private static final int MIN_LARGE_ISLAND_DISTANCE = 640;
   private static final int MAX_SITE_RETRIES = 3;
   private static final int MAX_TEMPLATE_ATTEMPTS = 16;
   private static final int MAX_REPLACEABLE_AQUATIC_AWS_AREA = 500;
   private static final Map<ServerLevel, LargeIslandWorldGenerator.Construction> CONSTRUCTIONS = new IdentityHashMap<>();

   private LargeIslandWorldGenerator() {
   }

   public static boolean active(ServerLevel level) {
      return LargeIslandWorldGenerationData.get(level).active() != null;
   }

   public static boolean isConstructingAt(ServerLevel level, BlockPos pos) {
      LargeIslandWorldGenerationData.Task task = LargeIslandWorldGenerationData.get(level).active();
      if (task == null) {
         return false;
      } else {
         AwsWorldGenTemplateRegistry.Template template = AwsWorldGenTemplateRegistry.templateForActivePath(task.path);
         Direction facing = Direction.from2DDataValue(task.facing);
         int sizeX = template != null ? template.rotatedSizeX(facing) : (facing.getAxis() == Axis.X ? task.planSizeZ : task.planSizeX);
         int sizeZ = template != null ? template.rotatedSizeZ(facing) : (facing.getAxis() == Axis.X ? task.planSizeX : task.planSizeZ);
         int sizeY = template != null ? template.sizeY() : task.planSizeY;
         return pos.getX() >= task.minX
            && pos.getX() < task.minX + sizeX
            && pos.getZ() >= task.minZ
            && pos.getZ() < task.minZ + sizeZ
            && pos.getY() >= task.minY
            && pos.getY() < task.minY + sizeY;
      }
   }

   public static String status(ServerLevel level) {
      LargeIslandWorldGenerationData.Task task = LargeIslandWorldGenerationData.get(level).active();
      if (task == null) {
         return "";
      } else {
         String message = "island="
            + task.name
            + ", phase="
            + task.phase
            + ", Y="
            + (task.minY + task.layer)
            + ", layer="
            + (task.layer + 1)
            + "/"
            + task.planSizeY
            + ", chunk="
            + task.progress
            + ", cell="
            + task.cellCursor
            + ", entity="
            + task.entityCursor;
         if (task.phase == LargeIslandWorldGenerationData.Phase.SURVEY) {
            message = message
               + ", water="
               + task.surveyWaterSamples
               + "/"
               + task.surveySamples
               + ", depth="
               + task.surveyDepthSamples
               + "/"
               + task.surveyWaterSamples
               + ", attempt="
               + (task.retryAttempt + 1)
               + "/3";
         }

         return task.lastError.isEmpty() ? message : message + ", paused: " + task.lastError;
      }
   }

   public static boolean resume(ServerLevel level) {
      LargeIslandWorldGenerationData data = LargeIslandWorldGenerationData.get(level);
      if (data.active() != null && !data.active().lastError.isEmpty()) {
         data.active().lastError = "";
         data.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public static void shutdown() {
      for (LargeIslandWorldGenerator.Construction construction : CONSTRUCTIONS.values()) {
         construction.window.releaseAll();
      }

      CONSTRUCTIONS.clear();
   }

   public static boolean tryQueue(ServerLevel level, int x, int waterY, int z, RandomSource random) {
      LargeIslandWorldGenerationData data = LargeIslandWorldGenerationData.get(level);
      return data.active() == null && random.nextInt(8) == 0 ? queueCandidate(level, x, waterY, z, random, null) : false;
   }

   private static boolean queueCandidate(ServerLevel level, int x, int waterY, int z, RandomSource random, LargeIslandWorldGenerationData.Task previous) {
      NaturalGenerationContext.markChunk(new ChunkPos(x >> 4, z >> 4));
      Holder<Biome> biome = level.getBiome(new BlockPos(x, waterY, z));
      List<AwsWorldGenTemplateRegistry.Template> candidates = new ArrayList<>();

      for (AwsWorldGenTemplateRegistry.Template template : AwsWorldGenTemplateRegistry.largeAquaticTemplates()) {
         if (template.acceptsBiome(biome) && (previous == null || !previous.rejectedTemplates.contains(template.path()))) {
            candidates.add(template);
         }
      }

      if (candidates.isEmpty()) {
         return false;
      } else {
         BlockPos origin = new BlockPos(x, waterY + 1, z);
         AwsWorldGenerationData aws = AwsWorldGenerationData.get(level);
         Map<String, Integer> rejections = new LinkedHashMap<>();

         for (int attempt = 0; attempt < 16 && !candidates.isEmpty(); attempt++) {
            AwsWorldGenTemplateRegistry.Template selected = AwsWorldGenerator.removeWeighted(candidates, random, aws);
            Direction facing = Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos min = selected.minFor(origin, facing);
            String rejection = spatialRejection(level, selected, min, facing, x, z);
            if (rejection == null) {
               LargeIslandWorldGenerationData.Task task = new LargeIslandWorldGenerationData.Task(
                  selected.path(), selected.name(), min.getX(), min.getY(), min.getZ(), facing.get2DDataValue()
               );
               task.anchorX = previous == null ? x : previous.anchorX;
               task.anchorY = previous == null ? waterY : previous.anchorY;
               task.anchorZ = previous == null ? z : previous.anchorZ;
               task.retrySeed = previous == null ? random.nextLong() : previous.retrySeed;
               if (task.retrySeed == 0L) {
                  task.retrySeed = 1L;
               }

               if (previous != null) {
                  task.retryAttempt = previous.retryAttempt + 1;
                  task.rejectedTemplates.addAll(previous.rejectedTemplates);
               }

               LargeIslandWorldGenerationData.get(level).setActive(task);
               AncientWarfareCore.LOG
                  .info(
                     "Queued phased large aquatic AWS {} at {} facing {}; attempt={}/{}",
                     new Object[]{selected.name(), origin, facing, task.retryAttempt + 1, 3}
                  );
               return true;
            }

            rejections.merge(rejection, 1, Integer::sum);
         }

         if (!rejections.isEmpty()) {
            AncientWarfareCore.LOG.info("Large aquatic AWS site {} exhausted {} template checks: {}", new Object[]{origin, 16, rejections});
         }

         return false;
      }
   }

   private static String spatialRejection(ServerLevel level, AwsWorldGenTemplateRegistry.Template selected, BlockPos min, Direction facing, int x, int z) {
      if (!selected.fitsBuildHeight(min, level.getMinBuildHeight(), level.getMaxBuildHeight())) {
         return "build_height";
      } else {
         int maxX = min.getX() + selected.rotatedSizeX(facing) - 1;
         int maxZ = min.getZ() + selected.rotatedSizeZ(facing) - 1;

         for (AwtWorldGenerationData.TownTask town : AwtWorldGenerationData.get(level).towns()) {
            if (town.phase != AwtWorldGenerationData.Phase.FAILED) {
               AwtWorldGenTemplateRegistry.Template townTemplate = AwtWorldGenTemplateRegistry.templateForActivePath(town.path);
               int halfSize = townTemplate == null ? 192 : townTemplate.chunks() * 8;
               if (min.getX() <= town.x + halfSize + 48
                  && maxX >= town.x - halfSize - 48
                  && min.getZ() <= town.z + halfSize + 48
                  && maxZ >= town.z - halfSize - 48) {
                  return "town_overlap";
               }
            }
         }

         for (AwsWorldGenerationData.Entry entry : AwsWorldGenerationData.get(level).entries()) {
            if (selected.unique() && entry.template().equals(selected.path())) {
               return "unique_template";
            }

            if (selected.island() && isReplaceableSmallAquaticAws(entry)) {
               if (entry.intersects(min.getX(), min.getZ(), maxX, maxZ, 0) && !fullyContained(entry, min, maxX, maxZ)) {
                  return "partial_small_structure_overlap";
               }
            } else {
               if (entry.intersects(min.getX(), min.getZ(), maxX, maxZ, Math.max(32, selected.border()))) {
                  return "aws_overlap";
               }

               AwsWorldGenTemplateRegistry.Template existing = AwsWorldGenTemplateRegistry.templateForActivePath(entry.template());
               int distanceLimit = existing != null && existing.requiresPhasedPlacement() ? 640 : AWCorePlatform.gameplayConfig().awsAquaticMinimumDistance();
               if (entry.horizontalDistanceSquared(x, z) < (double)((long)distanceLimit * (long)distanceLimit)) {
                  return "minimum_distance";
               }

               if (entry.template().equals(selected.path())) {
                  long distance = (long)selected.minDuplicateDistance();
                  if (entry.horizontalDistanceSquared(x, z) < (double)(distance * distance)) {
                     return "duplicate_distance";
                  }
               }
            }
         }

         return null;
      }
   }

   public static void tick(ServerLevel level) {
      try {
         NaturalGenerationContext.run(() -> tickInternal(level));
      } catch (RuntimeException var4) {
         LargeIslandWorldGenerationData data = LargeIslandWorldGenerationData.get(level);
         LargeIslandWorldGenerationData.Task task = data.active();
         if (task != null) {
            task.lastError = var4.getClass().getSimpleName() + ": " + var4.getMessage();
            data.setDirty();
            AncientWarfareCore.LOG
               .error(
                  "Paused island {} in {} at layer {}, chunk {}, cell {}; fix the cause and use /awworldgen resume",
                  new Object[]{task.name, task.phase, task.layer, task.progress, task.cellCursor, var4}
               );
         }

         release(level);
      }
   }

   private static void tickInternal(ServerLevel level) {
      LargeIslandWorldGenerationData data = LargeIslandWorldGenerationData.get(level);
      LargeIslandWorldGenerationData.Task task = data.active();
      if (task == null) {
         release(level);
      } else {
         AwsWorldGenTemplateRegistry.Template template = AwsWorldGenTemplateRegistry.templateForActivePath(task.path);
         if (template == null) {
            if (task.lastError.isEmpty()) {
               task.lastError = "Missing island template: " + task.path;
               data.setDirty();
               AncientWarfareCore.LOG.error("Paused island with missing template {}", task.path);
            }

            release(level);
         } else if (task.lastError.isEmpty()) {
            Direction facing = Direction.from2DDataValue(task.facing);
            BlockPos min = new BlockPos(task.minX, task.minY, task.minZ);
            int maxX = min.getX() + template.rotatedSizeX(facing) - 1;
            int maxZ = min.getZ() + template.rotatedSizeZ(facing) - 1;
            NaturalGenerationContext.markArea(min.offset(-32, 0, -32), maxX + 32, maxZ + 32);
            AwsTemplatePlacer.ChunkLayout layout = new AwsTemplatePlacer.ChunkLayout(
               Math.floorDiv(min.getX(), 16), Math.floorDiv(min.getZ(), 16), Math.floorDiv(maxX, 16), Math.floorDiv(maxZ, 16)
            );
            LargeIslandWorldGenerator.Construction construction = CONSTRUCTIONS.get(level);
            if (construction == null || construction.task != task) {
               release(level);
               construction = new LargeIslandWorldGenerator.Construction(task);
               CONSTRUCTIONS.put(level, construction);
               if (task.phase == LargeIslandWorldGenerationData.Phase.CLEANUP) {
                  transition(
                     task,
                     task.layeredBuild && !task.modernSurvey ? LargeIslandWorldGenerationData.Phase.PREPARE : LargeIslandWorldGenerationData.Phase.CLEANUP
                  );
                  if (task.layeredBuild) {
                     task.modernSurvey = true;
                  }

                  data.setDirty();
               }

               if (task.phase == LargeIslandWorldGenerationData.Phase.VALIDATE) {
                  transition(task, LargeIslandWorldGenerationData.Phase.VALIDATE);
                  task.entityCursor = 0;
                  data.setDirty();
               }
            }

            PlatformGameplayConfig config = AWCorePlatform.gameplayConfig();
            IslandWorkBudget budget = new IslandWorkBudget(config.largeIslandBlocksPerTick(), config.largeIslandTickBudgetMillis());
            int windowSize = config.largeIslandChunkWindowSize();
            if (!task.layeredBuild) {
               task.layeredBuild = true;
               if (task.phase == LargeIslandWorldGenerationData.Phase.CONFIGURE) {
                  task.legacyConfiguredChunks = task.progress;
               }

               if (task.phase == LargeIslandWorldGenerationData.Phase.BUILD) {
                  transition(task, LargeIslandWorldGenerationData.Phase.BUILD);
               } else if (task.phase != LargeIslandWorldGenerationData.Phase.SURVEY) {
                  transition(task, LargeIslandWorldGenerationData.Phase.CONNECT);
               }

               data.setDirty();
               AncientWarfareCore.LOG.info("Migrated legacy chunk-slice island {} to layered construction", task.name);
            }

            if (task.phase == LargeIslandWorldGenerationData.Phase.SURVEY) {
               if (!task.modernSurvey) {
                  transition(task, LargeIslandWorldGenerationData.Phase.SURVEY);
                  task.surveySamples = task.surveyWaterSamples = task.surveyDepthSamples = 0L;
                  task.modernSurvey = true;
                  data.setDirty();
               }

               if (prepareReplacements(level, construction, template, min, maxX, maxZ, budget)) {
                  if (task.progress < layout.count()) {
                     ChunkPos chunk = layout.chunk(task.progress);
                     if (construction.window.request(level, min, chunk, windowSize)) {
                        int startX = Math.max(min.getX(), chunk.getMinBlockX());
                        int startZ = Math.max(min.getZ(), chunk.getMinBlockZ());
                        int endX = Math.min(maxX, chunk.getMaxBlockX());
                        int endZ = Math.min(maxZ, chunk.getMaxBlockZ());
                        int firstX = startX + Math.min(2, endX - startX);
                        int firstZ = startZ + Math.min(2, endZ - startZ);
                        int columns = (endX - firstX) / 4 + 1;

                        for (int rows = (endZ - firstZ) / 4 + 1;
                           task.cellCursor < columns * rows;
                           construction.sampleY = construction.sampleWaterY = construction.sampleFloorY = Integer.MIN_VALUE
                        ) {
                           int x = firstX + task.cellCursor % columns * 4;
                           int z = firstZ + task.cellCursor / columns * 4;
                           int result = surveyColumn(level, construction, template, x, z, budget);
                           if (result < 0) {
                              data.setDirty();
                              return;
                           }

                           if (result == 3) {
                              rejectAndRetry(level, task, "protected_surface_or_seabed at " + x + "," + z);
                              return;
                           }

                           task.surveySamples++;
                           if (result > 0) {
                              task.surveyWaterSamples++;
                           }

                           if (result == 2) {
                              task.surveyDepthSamples++;
                           }

                           task.cellCursor++;
                        }

                        task.progress++;
                        task.cellCursor = 0;
                        data.setDirty();
                     }
                  } else if (OceanGenerationPolicy.coverageAcceptable(task.surveyWaterSamples, task.surveySamples)
                     && (
                        template.island()
                           ? OceanGenerationPolicy.islandDepthAcceptable(task.surveyDepthSamples, task.surveyWaterSamples)
                           : task.surveyDepthSamples == task.surveyWaterSamples
                     )) {
                     transition(task, LargeIslandWorldGenerationData.Phase.PREPARE);
                     data.setDirty();
                     AncientWarfareCore.LOG
                        .info(
                           "Accepted island survey for {}: water={}/{}, suitable_depth={}/{}; preparing {} layers",
                           new Object[]{
                              task.name, task.surveyWaterSamples, task.surveySamples, task.surveyDepthSamples, task.surveyWaterSamples, template.sizeY()
                           }
                        );
                  } else {
                     rejectAndRetry(
                        level,
                        task,
                        "water_coverage_or_depth: water="
                           + task.surveyWaterSamples
                           + "/"
                           + task.surveySamples
                           + ", suitable_depth="
                           + task.surveyDepthSamples
                           + "/"
                           + task.surveyWaterSamples
                     );
                  }
               }
            } else {
               if (construction.plan == null) {
                  construction.plan = AwsTemplatePlacer.constructionPlan(task.path, min, facing);
                  IslandLayerPlan index = construction.plan.index();
                  if (task.planSizeX != 0 && (task.planSizeX != index.sizeX() || task.planSizeY != index.sizeY() || task.planSizeZ != index.sizeZ())) {
                     throw new IllegalStateException("Island template dimensions changed during construction");
                  }

                  task.planSizeX = index.sizeX();
                  task.planSizeY = index.sizeY();
                  task.planSizeZ = index.sizeZ();
                  data.setDirty();
               }

               if (construction.plan.prepare(budget)) {
                  if (task.phase == LargeIslandWorldGenerationData.Phase.PREPARE) {
                     transition(task, LargeIslandWorldGenerationData.Phase.VALIDATE);
                     data.setDirty();
                  }

                  if (task.phase == LargeIslandWorldGenerationData.Phase.VALIDATE) {
                     if (!prepareReplacements(level, construction, template, min, maxX, maxZ, budget)) {
                        return;
                     }

                     if (!construction.replacementsValidated) {
                        while (task.entityCursor < construction.replacements.size()) {
                           for (AwsTemplatePlacer.IslandConstruction recorded = construction.replacements.get(task.entityCursor);
                              task.cellCursor < recorded.recordedVolumeSize();
                              task.cellCursor++
                           ) {
                              int resultx = recorded.recordedValidationStep(level, task.cellCursor, budget, construction.window, windowSize);
                              if (resultx < 0) {
                                 data.setDirty();
                                 return;
                              }

                              if (resultx > 0) {
                                 rejectAndRetry(level, task, "protected_small_structure: entry=" + task.entityCursor + ", cell=" + task.cellCursor);
                                 return;
                              }
                           }

                           task.entityCursor++;
                           task.cellCursor = 0;
                        }

                        task.entityCursor = 0;
                        construction.replacementsValidated = true;
                     }

                     while (task.layer < construction.plan.index().sizeY()) {
                        if (task.progress >= construction.plan.index().chunkCount()) {
                           if (!budget.tryScan()) {
                              data.setDirty();
                              return;
                           }

                           task.layer++;
                           task.progress = task.cellCursor = 0;
                        } else if (task.cellCursor >= construction.plan.sliceSize(task.layer, task.progress)) {
                           if (!budget.tryScan()) {
                              data.setDirty();
                              return;
                           }

                           task.progress++;
                           task.cellCursor = 0;
                        } else {
                           int resultxx = construction.plan
                              .validationStep(
                                 level,
                                 task.layer,
                                 task.progress,
                                 task.cellCursor,
                                 budget,
                                 construction.window,
                                 windowSize,
                                 construction.replacements,
                                 template.island()
                              );
                           if (resultxx < 0) {
                              data.setDirty();
                              return;
                           }

                           if (resultxx > 0) {
                              rejectAndRetry(
                                 level,
                                 task,
                                 "protected_block_or_vessel_collision: layer=" + task.layer + ", chunk=" + task.progress + ", cell=" + task.cellCursor
                              );
                              return;
                           }

                           task.cellCursor++;
                        }
                     }

                     while (task.entityCursor < construction.plan.entityCount()) {
                        int resultxxx = construction.plan
                           .validationEntityStep(level, task, budget, construction.window, windowSize, construction.replacements, template.island());
                        if (resultxxx < 0) {
                           data.setDirty();
                           return;
                        }

                        if (resultxxx > 0) {
                           rejectAndRetry(level, task, "protected_gate_volume: entity=" + task.entityCursor + ", cell=" + task.cellCursor);
                           return;
                        }
                     }

                     task.entityCursor = 0;
                     transition(task, LargeIslandWorldGenerationData.Phase.CLEANUP);
                     data.setDirty();
                  }

                  if (task.phase == LargeIslandWorldGenerationData.Phase.CLEANUP) {
                     cleanup(level, construction, template, min, facing, budget, windowSize);
                     data.setDirty();
                  } else {
                     while (budget.available()) {
                        if (task.phase == LargeIslandWorldGenerationData.Phase.ENTITIES) {
                           if (task.entityCursor >= construction.plan.entityCount()) {
                              transition(task, LargeIslandWorldGenerationData.Phase.FINAL_CONNECT);
                           } else {
                              if (!construction.plan.entityStep(level, task, budget, construction.window, windowSize)) {
                                 break;
                              }

                              data.setDirty();
                           }
                        } else {
                           if (!advanceCells(level, task, construction.plan, construction.window, budget, windowSize)) {
                              break;
                           }

                           switch (task.phase) {
                              case BUILD:
                                 transition(task, LargeIslandWorldGenerationData.Phase.CONNECT);
                                 break;
                              case CONNECT:
                                 transition(task, LargeIslandWorldGenerationData.Phase.ENTITIES);
                                 break;
                              case FINAL_CONNECT:
                                 transition(task, LargeIslandWorldGenerationData.Phase.CONFIGURE);
                                 break;
                              case CONFIGURE:
                                 AwsWorldGenerationData.get(level)
                                    .add(
                                       new AwsWorldGenerationData.Entry(
                                          task.path,
                                          min.getX(),
                                          min.getY(),
                                          min.getZ(),
                                          maxX,
                                          min.getY() + template.sizeY() - 1,
                                          maxZ,
                                          facing.get2DDataValue()
                                       )
                                    );
                                 AncientWarfareCore.LOG.info("Completed bottom-to-top large island {} at {}", task.name, min);
                                 data.setActive(null);
                                 release(level);
                                 return;
                              default:
                                 throw new IllegalStateException("Unexpected island phase " + task.phase);
                           }

                           data.setDirty();
                        }
                     }

                     data.setDirty();
                  }
               }
            }
         }
      }
   }

   private static void transition(LargeIslandWorldGenerationData.Task task, LargeIslandWorldGenerationData.Phase phase) {
      task.phase = phase;
      task.layer = 0;
      task.progress = 0;
      task.cellCursor = 0;
   }

   private static boolean advanceCells(
      ServerLevel level,
      LargeIslandWorldGenerationData.Task task,
      AwsTemplatePlacer.IslandConstruction plan,
      IslandChunkWindow window,
      IslandWorkBudget budget,
      int windowSize
   ) {
      while (task.layer < plan.index().sizeY()) {
         if (task.progress >= plan.index().chunkCount()) {
            if (!budget.tryScan()) {
               return false;
            }

            task.progress = 0;
            task.cellCursor = 0;
            task.layer++;
         } else {
            int multiplier = task.phase == LargeIslandWorldGenerationData.Phase.FINAL_CONNECT ? 5 : 1;
            if (task.phase == LargeIslandWorldGenerationData.Phase.CONFIGURE && task.progress < task.legacyConfiguredChunks) {
               if (!budget.tryScan()) {
                  return false;
               }

               task.progress++;
               task.cellCursor = 0;
            } else {
               int count = plan.sliceSize(task.layer, task.progress) * multiplier;
               if (task.cellCursor >= count) {
                  if (!budget.tryScan()) {
                     return false;
                  }

                  task.progress++;
                  task.cellCursor = 0;
               } else {
                  if (!plan.step(level, task.phase, task.layer, task.progress, task.cellCursor, budget, window, windowSize)) {
                     return false;
                  }

                  task.cellCursor++;
               }
            }
         }
      }

      return true;
   }

   private static int surveyColumn(
      ServerLevel level,
      LargeIslandWorldGenerator.Construction construction,
      AwsWorldGenTemplateRegistry.Template template,
      int x,
      int z,
      IslandWorkBudget budget
   ) {
      if (construction.sampleY == Integer.MIN_VALUE) {
         if (!budget.tryScan()) {
            return -1;
         }

         construction.sampleY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
      }

      while (construction.sampleWaterY == Integer.MIN_VALUE) {
         if (!budget.tryWork()) {
            return -1;
         }

         int y = construction.sampleY;
         if (y < level.getMinBuildHeight()) {
            return 0;
         }

         BlockPos pos = new BlockPos(x, y, z);
         BlockState state = level.getBlockState(pos);
         if (level.getBlockEntity(pos) != null || state.hasBlockEntity()) {
            return 3;
         }

         if (!matchesReplacement(level, construction, pos) && !state.isAir()) {
            if (!AwsTemplatePlacer.isBasicNaturalTerrain(state) && !AwsTemplatePlacer.isOpenWaterTerrain(state)) {
               return 3;
            }

            if (!level.getFluidState(pos).is(FluidTags.WATER)) {
               return 0;
            }

            int waterY = construction.task.minY + template.offsetY() - 1;
            if (Math.abs((long)y - (long)waterY) > 1L) {
               return 0;
            }

            construction.sampleWaterY = y;
            construction.sampleFloorY = level.getHeight(Types.OCEAN_FLOOR, x, z) - 1;
         } else {
            construction.sampleY--;
         }
      }

      while (construction.sampleFloorY >= level.getMinBuildHeight()) {
         if (!budget.tryWork()) {
            return -1;
         }

         BlockPos posx = new BlockPos(x, construction.sampleFloorY, z);
         BlockState statex = level.getBlockState(posx);
         if (level.getBlockEntity(posx) != null || statex.hasBlockEntity()) {
            return 3;
         }

         if (!matchesReplacement(level, construction, posx) && !AwsTemplatePlacer.isOpenWaterTerrain(statex)) {
            if (!AwsTemplatePlacer.isBasicNaturalTerrain(statex)) {
               return 3;
            }

            int depth = construction.sampleWaterY - construction.sampleFloorY;
            return OceanGenerationPolicy.depthAcceptable(template.island(), template.minWaterDepth(), template.maxWaterDepth(), depth) ? 2 : 1;
         }

         construction.sampleFloorY--;
      }

      return 1;
   }

   private static boolean matchesReplacement(ServerLevel level, LargeIslandWorldGenerator.Construction construction, BlockPos pos) {
      for (AwsTemplatePlacer.IslandConstruction replacement : construction.replacements) {
         if (replacement.matchesRecordedCell(level, pos)) {
            return true;
         }
      }

      return false;
   }

   private static boolean fullyContained(AwsWorldGenerationData.Entry entry, BlockPos min, int maxX, int maxZ) {
      return entry.minX() >= min.getX() && entry.maxX() <= maxX && entry.minZ() >= min.getZ() && entry.maxZ() <= maxZ;
   }

   private static boolean prepareReplacements(
      ServerLevel level,
      LargeIslandWorldGenerator.Construction construction,
      AwsWorldGenTemplateRegistry.Template template,
      BlockPos min,
      int maxX,
      int maxZ,
      IslandWorkBudget budget
   ) {
      if (construction.replacementsReady) {
         return true;
      } else {
         List<AwsWorldGenerationData.Entry> entries = AwsWorldGenerationData.get(level).entries();

         while (construction.replacementSearch < entries.size()) {
            if (!budget.tryScan()) {
               return false;
            }

            AwsWorldGenerationData.Entry entry = entries.get(construction.replacementSearch++);
            if (template.island()
               && isReplaceableSmallAquaticAws(entry)
               && entry.intersects(min.getX(), min.getZ(), maxX, maxZ, 0)
               && fullyContained(entry, min, maxX, maxZ)) {
               construction.replacements
                  .add(
                     AwsTemplatePlacer.constructionPlan(
                        entry.template(), new BlockPos(entry.minX(), entry.minY(), entry.minZ()), Direction.from2DDataValue(entry.facing())
                     )
                  );
            }
         }

         construction.replacementsReady = true;
         return true;
      }
   }

   private static void rejectAndRetry(ServerLevel level, LargeIslandWorldGenerationData.Task task, String reason) {
      AncientWarfareCore.LOG.info("Rejected large island {} before mutation: {}; attempt={}/{}", new Object[]{task.name, reason, task.retryAttempt + 1, 3});
      if (!task.rejectedTemplates.contains(task.path)) {
         task.rejectedTemplates.add(task.path);
      }

      LargeIslandWorldGenerationData.get(level).setActive(null);
      release(level);
      if (task.retrySeed != 0L && task.retryAttempt + 1 < 3) {
         RandomSource random = RandomSource.create(task.retrySeed ^ (long)(task.retryAttempt + 1) * 72188768385693L);
         int x = task.anchorX + random.nextInt(129) - 64;
         int z = task.anchorZ + random.nextInt(129) - 64;
         BlockPos spawn = level.getSharedSpawnPos();
         long dx = (long)x - (long)spawn.getX();
         long dz = (long)z - (long)spawn.getZ();
         int protection = AWCorePlatform.gameplayConfig().awsSpawnProtectionDistance();
         if (dx * dx + dz * dz < (long)protection * (long)protection) {
            x = task.anchorX;
            z = task.anchorZ;
         }

         if (!queueCandidate(level, x, task.anchorY, z, random, task)) {
            AncientWarfareCore.LOG.info("Large aquatic AWS bounded retry ended near {},{}: no eligible template/site", x, z);
         }
      }
   }

   private static boolean isReplaceableSmallAquaticAws(AwsWorldGenerationData.Entry entry) {
      AwsWorldGenTemplateRegistry.Template template = AwsWorldGenTemplateRegistry.templateForActivePath(entry.template());
      return template != null && template.aquatic() && (long)template.sizeX() * (long)template.sizeZ() <= 500L;
   }

   private static void cleanup(
      ServerLevel level,
      LargeIslandWorldGenerator.Construction construction,
      AwsWorldGenTemplateRegistry.Template island,
      BlockPos islandMin,
      Direction islandFacing,
      IslandWorkBudget budget,
      int windowSize
   ) {
      LargeIslandWorldGenerationData.Task task = construction.task;
      AwsWorldGenerationData data = AwsWorldGenerationData.get(level);
      int islandMaxX = islandMin.getX() + island.rotatedSizeX(islandFacing) - 1;
      int islandMaxZ = islandMin.getZ() + island.rotatedSizeZ(islandFacing) - 1;

      while (budget.available()) {
         if (construction.cleanupEntry == null) {
            while (construction.cleanupSearch < data.entries().size()) {
               if (!budget.tryScan()) {
                  return;
               }

               AwsWorldGenerationData.Entry entry = data.entries().get(construction.cleanupSearch);
               if (entry.intersects(islandMin.getX(), islandMin.getZ(), islandMaxX, islandMaxZ, 0)
                  && island.island()
                  && isReplaceableSmallAquaticAws(entry)
                  && fullyContained(entry, islandMin, islandMaxX, islandMaxZ)) {
                  construction.cleanupEntry = entry;
                  construction.cleanupPlan = AwsTemplatePlacer.constructionPlan(
                     entry.template(), new BlockPos(entry.minX(), entry.minY(), entry.minZ()), Direction.from2DDataValue(entry.facing())
                  );
                  break;
               }

               construction.cleanupSearch++;
            }

            if (construction.cleanupEntry == null) {
               transition(task, LargeIslandWorldGenerationData.Phase.BUILD);
               return;
            }
         }

         AwsWorldGenerationData.Entry entry = construction.cleanupEntry;
         NaturalGenerationContext.markArea(new BlockPos(entry.minX() - 16, 0, entry.minZ() - 16), entry.maxX() + 16, entry.maxZ() + 16);
         if (!construction.cleanupPlan.prepare(budget)) {
            return;
         }

         if (!advanceCells(level, task, construction.cleanupPlan, construction.window, budget, windowSize)) {
            return;
         }

         data.entries().remove(entry);
         data.setDirty();
         AncientWarfareCore.LOG.info("Removed small aquatic AWS {} for large island {}", entry.template(), task.name);
         construction.cleanupEntry = null;
         construction.cleanupPlan = null;
         transition(task, LargeIslandWorldGenerationData.Phase.CLEANUP);
      }
   }

   private static void release(ServerLevel level) {
      LargeIslandWorldGenerator.Construction removed = CONSTRUCTIONS.remove(level);
      if (removed != null) {
         removed.window.releaseAll();
      }
   }

   private static final class Construction {
      final LargeIslandWorldGenerationData.Task task;
      AwsTemplatePlacer.IslandConstruction plan;
      final IslandChunkWindow window = new IslandChunkWindow();
      AwsWorldGenerationData.Entry cleanupEntry;
      AwsTemplatePlacer.IslandConstruction cleanupPlan;
      int cleanupSearch;
      final List<AwsTemplatePlacer.IslandConstruction> replacements = new ArrayList<>();
      boolean replacementsReady;
      boolean replacementsValidated;
      int replacementSearch;
      int sampleY = Integer.MIN_VALUE;
      int sampleWaterY = Integer.MIN_VALUE;
      int sampleFloorY = Integer.MIN_VALUE;

      Construction(LargeIslandWorldGenerationData.Task task) {
         this.task = task;
      }
   }
}
