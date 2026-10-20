package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public interface PlatformTagAccess {
   boolean isOre(BlockState var1);

   boolean isBiome(Holder<Biome> var1, PlatformTagAccess.BiomeTag var2);

   public static enum BiomeTag {
      MOUNTAIN,
      PEAK,
      SLOPE,
      CONIFEROUS,
      PLAINS,
      DESERT,
      SWAMP,
      SNOWY,
      MUSHROOM,
      WASTELAND,
      DEAD,
      SPOOKY,
      MAGICAL;
   }
}
