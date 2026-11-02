package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.shadowmage.ancientwarfare.core.faction.FactionSeasons;

/** Leaves turn gold and red in autumn and grass goes dun in winter; the world redraws when the season turns. */
public final class SeasonColors {
   private static FactionSeasons.Season shown;

   private SeasonColors() {
   }

   private static FactionSeasons.Season server = FactionSeasons.Season.SUMMER;

   /** The server's season, sent now and then. */
   public static void set(int season) {
      server = FactionSeasons.Season.values()[Math.floorMod(season, 4)];
   }

   private static FactionSeasons.Season season() {
      return server;
   }

   private static int mix(int from, int to, double share) {
      int r = (int)(((from >> 16) & 255) * (1 - share) + ((to >> 16) & 255) * share);
      int g = (int)(((from >> 8) & 255) * (1 - share) + ((to >> 8) & 255) * share);
      int b = (int)((from & 255) * (1 - share) + (to & 255) * share);
      return r << 16 | g << 8 | b;
   }

   static int foliage(int base, long seed) {
      return switch (season()) {
         case AUTUMN -> mix(base, (seed & 3) == 0 ? 0xB8321E : (seed & 3) == 1 ? 0xD9A21E : 0xC96A1C, 0.75);
         case WINTER -> mix(base, 0x7A6E52, 0.55);
         case SPRING -> mix(base, 0x7CD04A, 0.25);
         default -> base;
      };
   }

   static int grass(int base) {
      return switch (season()) {
         case AUTUMN -> mix(base, 0xA8973E, 0.4);
         case WINTER -> mix(base, 0x9A9478, 0.55);
         default -> base;
      };
   }

   public static void register(RegisterColorHandlersEvent.Block event) {
      event.register((state, level, pos, tint) -> {
         int base = level != null && pos != null ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor();
         return foliage(base, pos == null ? 0 : (pos.getX() * 31L + pos.getZ() * 17L + pos.getY()) >> 2);
      }, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.VINE);
      event.register((state, level, pos, tint) -> foliage(FoliageColor.getBirchColor(), pos == null ? 1 : 1), Blocks.BIRCH_LEAVES);
      event.register((state, level, pos, tint) -> grass(level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor()),
         Blocks.GRASS_BLOCK, Blocks.GRASS, Blocks.FERN, Blocks.TALL_GRASS, Blocks.LARGE_FERN, Blocks.SUGAR_CANE);
   }

   /** Redraws the world once the season has turned. */
   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.level.getGameTime() % 40L != 0L) {
         return;
      }

      FactionSeasons.Season now = season();
      if (shown != null && shown != now) {
         mc.levelRenderer.allChanged();
      }

      shown = now;
   }
}
