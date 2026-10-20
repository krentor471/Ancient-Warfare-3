package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags.Biomes;
import net.minecraftforge.common.Tags.Blocks;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformTagAccess;

public final class ForgeTagAccess implements PlatformTagAccess {
   @Override
   public boolean isOre(BlockState state) {
      return state.is(Blocks.ORES);
   }

   @Override
   public boolean isBiome(Holder<Biome> biome, PlatformTagAccess.BiomeTag tag) {
      return switch (tag) {
         case MOUNTAIN -> biome.is(Biomes.IS_MOUNTAIN);
         case PEAK -> biome.is(Biomes.IS_PEAK);
         case SLOPE -> biome.is(Biomes.IS_SLOPE);
         case CONIFEROUS -> biome.is(Biomes.IS_CONIFEROUS);
         case PLAINS -> biome.is(Biomes.IS_PLAINS);
         case DESERT -> biome.is(Biomes.IS_DESERT);
         case SWAMP -> biome.is(Biomes.IS_SWAMP);
         case SNOWY -> biome.is(Biomes.IS_SNOWY);
         case MUSHROOM -> biome.is(Biomes.IS_MUSHROOM);
         case WASTELAND -> biome.is(Biomes.IS_WASTELAND);
         case DEAD -> biome.is(Biomes.IS_DEAD);
         case SPOOKY -> biome.is(Biomes.IS_SPOOKY);
         case MAGICAL -> biome.is(Biomes.IS_MAGICAL);
      };
   }
}
