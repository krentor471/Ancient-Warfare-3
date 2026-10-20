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
      return state.m_204336_(Blocks.ORES);
   }

   @Override
   public boolean isBiome(Holder<Biome> biome, PlatformTagAccess.BiomeTag tag) {
      return switch (tag) {
         case MOUNTAIN -> biome.m_203656_(Biomes.IS_MOUNTAIN);
         case PEAK -> biome.m_203656_(Biomes.IS_PEAK);
         case SLOPE -> biome.m_203656_(Biomes.IS_SLOPE);
         case CONIFEROUS -> biome.m_203656_(Biomes.IS_CONIFEROUS);
         case PLAINS -> biome.m_203656_(Biomes.IS_PLAINS);
         case DESERT -> biome.m_203656_(Biomes.IS_DESERT);
         case SWAMP -> biome.m_203656_(Biomes.IS_SWAMP);
         case SNOWY -> biome.m_203656_(Biomes.IS_SNOWY);
         case MUSHROOM -> biome.m_203656_(Biomes.IS_MUSHROOM);
         case WASTELAND -> biome.m_203656_(Biomes.IS_WASTELAND);
         case DEAD -> biome.m_203656_(Biomes.IS_DEAD);
         case SPOOKY -> biome.m_203656_(Biomes.IS_SPOOKY);
         case MAGICAL -> biome.m_203656_(Biomes.IS_MAGICAL);
      };
   }
}
