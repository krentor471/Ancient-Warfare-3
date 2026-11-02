package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The seasons on the land itself, the way seasons mods show them: in winter snow settles and ponds
 * freeze around the players even where the biome is mild, in spring it melts away again, and
 * nothing grows in the fields under the snow. Cold biomes keep their own snow all year.
 */
public final class SeasonWeather {
   private static final int RADIUS = 48;
   private static final int TRIES = 40;

   private SeasonWeather() {
   }

   static void tick(ServerLevel level) {
      if (level.getGameTime() % 4L != 0L || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
         return;
      }

      FactionSeasons.Season season = FactionSeasons.season(level);
      if (level.getGameTime() % 200L == 0L) {
         net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork.sendSeason(level, season.ordinal());
      }

      if (season != FactionSeasons.Season.WINTER && season != FactionSeasons.Season.SPRING) {
         return;
      }

      RandomSource random = level.getRandom();
      for (ServerPlayer player : level.players()) {
         for (int i = 0; i < TRIES; i++) {
            int x = player.getBlockX() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int z = player.getBlockZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
               continue;
            }

            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            BlockPos below = top.below();
            if (!level.getBiome(top).value().warmEnoughToRain(top)) {
               continue;
            }

            if (season == FactionSeasons.Season.WINTER) {
               freeze(level, top, below);
            } else {
               thaw(level, top, below);
            }
         }
      }
   }

   private static void freeze(ServerLevel level, BlockPos top, BlockPos below) {
      BlockState ground = level.getBlockState(below);
      if (ground.is(Blocks.WATER) && level.getFluidState(below).isSource()) {
         level.setBlockAndUpdate(below, Blocks.ICE.defaultBlockState());
      } else if (level.getBlockState(top).isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, top)) {
         level.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
      } else if (level.getBlockState(top).is(Blocks.SNOW) && level.getBlockState(top).getValue(SnowLayerBlock.LAYERS) < 2) {
         level.setBlockAndUpdate(top, level.getBlockState(top).setValue(SnowLayerBlock.LAYERS, 2));
      }
   }

   private static void thaw(ServerLevel level, BlockPos top, BlockPos below) {
      if (level.getBlockState(below).is(Blocks.SNOW)) {
         level.removeBlock(below, false);
      } else if (level.getBlockState(top).is(Blocks.SNOW)) {
         level.removeBlock(top, false);
      } else if (level.getBlockState(below).is(Blocks.ICE)) {
         level.setBlockAndUpdate(below, Blocks.WATER.defaultBlockState());
      }
   }

   /** Fields lie still in winter. */
   public static boolean frozen(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
      return level instanceof ServerLevel server && FactionSeasons.season(server) == FactionSeasons.Season.WINTER
         && server.getBiome(pos).value().warmEnoughToRain(pos);
   }
}
