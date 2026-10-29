package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * The first minutes of a new world: before play begins in earnest, the towns, villages and roads
 * around spawn are raised at full speed, with no regard for tick time, while players wait safely.
 */
public final class FactionPrebuild {
   /** How far from spawn the world is finished before play, unless the player asks for more. */
   static final int RADIUS = 1000;
   /** The longest the usual prebuild is given, in ticks, before play begins anyway. */
   private static final long MAX_TICKS = 20L * 60L * 8L;
   /** A wider prebuild, asked for by command, may run this long: two hours. */
   private static final long WIDE_MAX_TICKS = 20L * 60L * 120L;
   private static int radius = 0;
   private static final int CHECK_TICKS = 20;

   private static boolean active;
   private static long started = -1L;
   private static ServerBossEvent bar;

   private FactionPrebuild() {
   }

   /** True while the world around spawn is still being raised. */
   public static boolean active() {
      return active;
   }

   /** True when this spot is inside the area finished before play. */
   public static boolean covers(ServerLevel level, int x, int z) {
      BlockPos spawn = level.getSharedSpawnPos();
      if (radius <= 0) {
         return true;
      }

      long dx = x - spawn.getX();
      long dz = z - spawn.getZ();
      return dx * dx + dz * dz <= (long)radius * radius;
   }

   /** Starts a prebuild out to this radius from spawn, 0 for the whole world. */
   public static void start(ServerLevel level, int wanted) {
      FactionWorldData data = FactionWorldData.get(level);
      data.setPrebuildRadius(wanted);
      data.setPrebuilt(false);
      started = -1L;
   }

   /** Ends a prebuild now; the rest is built as players come near. */
   public static void stop(ServerLevel level) {
      finish(level, FactionWorldData.get(level));
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      radius = data.prebuildRadius();
      if (data.prebuilt()) {
         if (active) {
            finish(level, data);
         }

         return;
      }

      if (level.getGameTime() % CHECK_TICKS != 0L) {
         return;
      }

      if (!data.historyDone()) {
         return;
      }

      if (started < 0L) {
         started = level.getGameTime();
      }

      int total = 0;
      int waiting = 0;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (FactionAI.isFort(settlement) || !covers(level, settlement.center().getX(), settlement.center().getZ())) {
            continue;
         }

         total++;
         if (!settlement.built() || settlement.plan().size() >= TownBuildings.MAX_WAITING) {
            waiting++;
         }
      }

      boolean roads = FactionRoads.unfinishedNear(level, data);

      long limit = radius <= 0 ? Long.MAX_VALUE / 2 : radius == RADIUS ? MAX_TICKS : WIDE_MAX_TICKS;
      if (waiting == 0 && !roads || level.getGameTime() - started > limit) {
         finish(level, data);
         return;
      }

      active = true;
      if (bar == null) {
         bar = new ServerBossEvent(Component.translatable("faction.ancientwarfare.prebuild"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
      }

      float done = total == 0 ? 1.0F : 1.0F - (float)waiting / total;
      float time = radius <= 0 ? 0.0F : (float)(level.getGameTime() - started) / limit;
      bar.setProgress(Math.max(0.0F, Math.min(1.0F, Math.max(done * 0.9F, time))));
      bar.setName(Component.translatable("faction.ancientwarfare.prebuild_count", total - waiting, total, FactionRoads.unfinishedCount(level, data)));
      for (ServerPlayer player : level.players()) {
         bar.addPlayer(player);
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false));
      }
   }

   private static void finish(ServerLevel level, FactionWorldData data) {
      active = false;
      data.setPrebuilt(true);
      // A new world raises the whole map before play; a later prebuild is asked for by command.
      data.setPrebuildRadius(0);
      if (bar != null) {
         bar.removeAllPlayers();
         bar = null;
      }
   }

   /** A new server starts from scratch. */
   public static void reset() {
      active = false;
      started = -1L;
      if (bar != null) {
         bar.removeAllPlayers();
         bar = null;
      }
   }
}
