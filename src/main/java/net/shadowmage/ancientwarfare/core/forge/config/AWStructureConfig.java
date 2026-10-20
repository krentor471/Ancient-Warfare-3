package net.shadowmage.ancientwarfare.core.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public final class AWStructureConfig {
   public static final ForgeConfigSpec SPEC;
   private static final BooleanValue ENABLE_AWS;
   private static final BooleanValue ENABLE_LARGE_AQUATIC_AWS;
   private static final IntValue AWS_REGION_CHUNKS;
   private static final IntValue AWS_SPAWN_PROTECTION;
   private static final IntValue AWS_MIN_DISTANCE;
   private static final IntValue AWS_AQUATIC_MIN_DISTANCE;
   private static final IntValue AWS_CANDIDATE_INTERVAL;
   private static final IntValue LARGE_ISLAND_BLOCKS_PER_TICK;
   private static final IntValue LARGE_ISLAND_TICK_BUDGET_MILLIS;
   private static final IntValue LARGE_ISLAND_CHUNK_WINDOW_SIZE;
   private static final BooleanValue ENABLE_AWT;
   private static final IntValue AWT_REGION_CHUNKS;
   private static final IntValue AWT_SPAWN_PROTECTION;
   private static final IntValue AWT_MIN_DISTANCE;

   private AWStructureConfig() {
   }

   public static boolean awsEnabled() {
      return (Boolean)ENABLE_AWS.get();
   }

   public static boolean largeAquaticAwsEnabled() {
      return (Boolean)ENABLE_LARGE_AQUATIC_AWS.get();
   }

   public static int awsRegionChunks() {
      return (Integer)AWS_REGION_CHUNKS.get();
   }

   public static int awsSpawnProtectionDistance() {
      return (Integer)AWS_SPAWN_PROTECTION.get();
   }

   public static int awsMinimumDistance() {
      return (Integer)AWS_MIN_DISTANCE.get();
   }

   public static int awsAquaticMinimumDistance() {
      return (Integer)AWS_AQUATIC_MIN_DISTANCE.get();
   }

   public static int awsCandidateIntervalTicks() {
      return (Integer)AWS_CANDIDATE_INTERVAL.get();
   }

   public static int largeIslandBlocksPerTick() {
      return (Integer)LARGE_ISLAND_BLOCKS_PER_TICK.get();
   }

   public static int largeIslandTickBudgetMillis() {
      return (Integer)LARGE_ISLAND_TICK_BUDGET_MILLIS.get();
   }

   public static int largeIslandChunkWindowSize() {
      return (Integer)LARGE_ISLAND_CHUNK_WINDOW_SIZE.get();
   }

   public static boolean awtEnabled() {
      return (Boolean)ENABLE_AWT.get();
   }

   public static int awtRegionChunks() {
      return (Integer)AWT_REGION_CHUNKS.get();
   }

   public static int awtSpawnProtectionDistance() {
      return (Integer)AWT_SPAWN_PROTECTION.get();
   }

   public static int awtMinimumDistance() {
      return (Integer)AWT_MIN_DISTANCE.get();
   }

   static {
      Builder builder = new Builder();
      builder.comment(
            new String[]{
               "Ancient Warfare AWS structure and AWT town natural-generation settings.",
               "These settings are server-authoritative. Saving this file applies changes automatically.",
               "Per-template enable, weight and biome overrides are stored under",
               "config/ancientwarfare-structure-generation/overrides.json."
            }
         )
         .push("natural_generation");
      builder.comment("Standalone AWS structures").push("aws");
      ENABLE_AWS = builder.comment("Generate standalone AWS structures naturally. Creative placement and commands still work when false.")
         .define("enabled", true);
      ENABLE_LARGE_AQUATIC_AWS = builder.comment("Allow very large aquatic/island AWS templates to use phased natural generation.")
         .define("enableLargeAquaticStructures", true);
      AWS_REGION_CHUNKS = builder.comment(
            new String[]{
               "One deterministic AWS candidate chunk is selected in every square region of this size.",
               "12 means roughly one candidate per 12x12=144 chunks. Smaller values generate more often."
            }
         )
         .defineInRange("candidateRegionSizeChunks", 12, 2, 256);
      AWS_SPAWN_PROTECTION = builder.comment("Minimum block distance from world spawn for natural AWS structures.")
         .defineInRange("spawnProtectionDistance", 400, 0, 100000);
      AWS_MIN_DISTANCE = builder.comment("Minimum block distance between ordinary AWS structures.").defineInRange("minimumStructureDistance", 192, 0, 100000);
      AWS_AQUATIC_MIN_DISTANCE = builder.comment("Minimum block distance between aquatic AWS structures.")
         .defineInRange("minimumAquaticStructureDistance", 320, 0, 100000);
      AWS_CANDIDATE_INTERVAL = builder.comment(
            new String[]{
               "Minimum server ticks between queued AWS generation attempts, including chunk pre-generation.",
               "Pending candidates survive world saves/restarts. This limits frequency, not the cost of one large template."
            }
         )
         .defineInRange("candidateIntervalTicks", 20, 1, 1200);
      builder.comment(
            new String[]{
               "Performance limits for phased large aquatic/island AWS construction.",
               "Saving these values applies them to an in-progress construction automatically."
            }
         )
         .push("large_island_construction");
      LARGE_ISLAND_BLOCKS_PER_TICK = builder.comment("Maximum block positions processed per server tick. Lower values reduce generation spikes.")
         .defineInRange("blocksPerTick", 1024, 16, 16384);
      LARGE_ISLAND_TICK_BUDGET_MILLIS = builder.comment(
            new String[]{
               "Target time budget in milliseconds per server tick for large-island construction.",
               "Construction also stops at blocksPerTick; one block operation may exceed this target."
            }
         )
         .defineInRange("tickBudgetMillis", 3, 1, 20);
      LARGE_ISLAND_CHUNK_WINDOW_SIZE = builder.comment(
            new String[]{
               "LRU limit for temporary FULL construction chunk tickets, not the island size.",
               "An indivisible boundary operation may retain a few necessary neighbours even with a smaller limit.",
               "Minecraft may also load dependency chunks around the requested chunks."
            }
         )
         .defineInRange("chunkWindowSize", 9, 1, 32);
      builder.pop();
      builder.pop();
      builder.comment("AWT generated towns").push("awt");
      ENABLE_AWT = builder.comment("Generate AWT towns naturally. Creative placement and commands still work when false.").define("enabled", true);
      AWT_REGION_CHUNKS = builder.comment(
            new String[]{
               "One deterministic AWT candidate chunk is selected in every square region of this size.",
               "96 means roughly one candidate per 96x96=9216 chunks. Smaller values generate more often."
            }
         )
         .defineInRange("candidateRegionSizeChunks", 96, 40, 1024);
      AWT_SPAWN_PROTECTION = builder.comment("Minimum block distance from world spawn for naturally generated AWT towns.")
         .defineInRange("spawnProtectionDistance", 1200, 0, 100000);
      AWT_MIN_DISTANCE = builder.comment("Minimum block distance between AWT towns.").defineInRange("minimumTownDistance", 1800, 0, 100000);
      builder.pop();
      builder.pop();
      SPEC = builder.build();
   }
}
