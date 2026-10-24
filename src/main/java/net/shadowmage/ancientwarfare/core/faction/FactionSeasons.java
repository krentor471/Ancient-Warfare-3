package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * The year turns through four seasons of a few days each. Spring and summer bring the crops on,
 * autumn brings the harvest in and the harvest festivals with it, and winter yields little:
 * food runs short, prices climb, snow and rain come often and armies march slower, more so in
 * foul weather. Bad harvests strike in the growing months, and a town that went hungry sells dear.
 */
public final class FactionSeasons {
   public static final int SEASON_DAYS = 6;
   private static final double RAIN_MARCH = 0.85;
   private static final double FAMINE_PRICES = 1.25;
   private static final double WINTER_STORM_CHANCE = 0.5;

   private FactionSeasons() {
   }

   public enum Season {
      SPRING(0.9, 1.0, 1.0, 0.5, 0.5, 1.0),
      SUMMER(1.1, 1.0, 1.0, 1.5, 0.5, 1.0),
      AUTUMN(1.4, 0.95, 0.95, 2.0, 3.0, 1.0),
      WINTER(0.55, 0.75, 1.15, 0.0, 0.0, 0.5);

      /** Share of the usual yield a working person brings in. */
      final double yield;
      /** Pace of armies on the march. */
      final double march;
      /** Traders' prices. */
      final double prices;
      /** How likely a bad harvest is, as a share of the usual chance. */
      final double badHarvests;
      /** How likely a town holds a harvest festival, as a share of the usual chance. */
      final double festivals;
      /** How many travellers take to the roads. */
      final double travel;

      Season(double yield, double march, double prices, double badHarvests, double festivals, double travel) {
         this.yield = yield;
         this.march = march;
         this.prices = prices;
         this.badHarvests = badHarvests;
         this.festivals = festivals;
         this.travel = travel;
      }

      public String key() {
         return "season.ancientwarfare." + this.name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   /** The faction day, which the seasons follow. */
   public static long day(Level level) {
      return level instanceof ServerLevel server ? Math.max(0L, FactionWorldData.get(server).lastSimulatedDay()) : level.getDayTime() / FactionSimulation.DAY_TICKS;
   }

   public static Season season(Level level) {
      return Season.values()[(int)Math.floorMod(day(level) / SEASON_DAYS, 4L)];
   }

   /** Days left of the season, today included. */
   public static int daysLeft(Level level) {
      return SEASON_DAYS - (int)Math.floorMod(day(level), (long)SEASON_DAYS);
   }

   public static double yield(Level level) {
      return season(level).yield;
   }

   public static double badHarvests(Level level) {
      return season(level).badHarvests;
   }

   public static double festivals(Level level) {
      return season(level).festivals;
   }

   public static double travel(Level level) {
      return season(level).travel;
   }

   /** How fast armies march now: the season, and rain or snow on top of it. */
   public static double march(Level level) {
      return season(level).march * (level.isRaining() ? RAIN_MARCH : 1.0);
   }

   /** Traders' prices in a settlement: the season, and dearer where people went hungry. */
   public static double prices(Level level, FactionWorldData.Settlement settlement) {
      double factor = season(level).prices;
      return settlement != null && settlement.famine() > 0 ? factor * FAMINE_PRICES : factor;
   }

   /** Daily: a new season is told to everyone; winter days bring storms more often. */
   public static void simulateDay(ServerLevel level) {
      Season season = season(level);
      if (Math.floorMod(day(level), (long)SEASON_DAYS) == 0L && !FactionHistory.running()) {
         level.getServer()
            .getPlayerList()
            .broadcastSystemMessage(
               Component.translatable("season.ancientwarfare.begins", Component.translatable(season.key()), Component.translatable(season.key() + ".effect"))
                  .withStyle(ChatFormatting.AQUA),
               false
            );
      }

      if (season == Season.WINTER && !level.isRaining() && level.getRandom().nextDouble() < WINTER_STORM_CHANCE) {
         int length = 6000 + level.getRandom().nextInt(12000);
         level.setWeatherParameters(0, length, true, level.getRandom().nextInt(4) == 0);
      }
   }
}
