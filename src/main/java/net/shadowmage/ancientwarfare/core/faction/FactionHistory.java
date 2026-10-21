package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/**
 * The past of a new world, run once when it is first loaded: countries are decided across a wide
 * square around spawn (from the climate alone, no land is generated), every faction founds its
 * capital on paper, and some weeks of faction life are simulated, so the player starts in a world
 * that already has villages, wars and treaties behind it. Settlements are built as the player
 * nears them. Worlds that already have settlements only get their countries decided and capitals
 * founded; their history has been lived already.
 */
public final class FactionHistory {
   /** Half the width of the square whose countries are decided up front. */
   public static final int RADIUS = 5000;
   public static final int DAYS = 60;
   private static boolean running;

   private FactionHistory() {
   }

   /** True while the history is being simulated: its news is not told to anyone. */
   public static boolean running() {
      return running;
   }

   public static void runIfNeeded(ServerLevel level, FactionWorldData data) {
      if (data.historyDone()) {
         return;
      }

      long started = System.currentTimeMillis();
      boolean fresh = data.settlements().isEmpty();
      BlockPos spawn = level.getSharedSpawnPos();
      int size = FactionRegions.PROVINCE_SIZE;

      for (int cellX = Math.floorDiv(spawn.getX() - RADIUS, size); cellX <= Math.floorDiv(spawn.getX() + RADIUS, size); cellX++) {
         for (int cellZ = Math.floorDiv(spawn.getZ() - RADIUS, size); cellZ <= Math.floorDiv(spawn.getZ() + RADIUS, size); cellZ++) {
            FactionRegions.provinceByCell(level, cellX, cellZ);
         }
      }

      FactionExpansion.foundCapitals(level, data);
      if (fresh) {
         running = true;

         try {
            data.setLastSimulatedDay(Math.max(0L, data.lastSimulatedDay()));

            for (int day = 0; day < DAYS; day++) {
               data.setLastSimulatedDay(data.lastSimulatedDay() + 1);
               FactionSimulation.simulateDay(level, data);
            }
         } finally {
            running = false;
         }
      }

      data.setHistoryDone();
      Map<String, Integer> settlements = new HashMap<>();
      data.settlements().forEach(settlement -> settlements.merge(settlement.faction(), 1, Integer::sum));
      AncientWarfareCore.LOG
         .info("Faction history: {} days simulated in {} ms; settlements {}", fresh ? DAYS : 0, System.currentTimeMillis() - started, settlements);
   }
}
