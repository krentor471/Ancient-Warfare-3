package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator;

/**
 * Factions grow outward: a faction with gold to spare and a settlement big enough to send settlers
 * claims a wild province next to its land and founds a village there. The claim is immediate; the
 * village is built later, one at a time, while no player is near enough to watch it appear.
 */
public final class FactionExpansion {
   private static final int FOUND_COST = 80;
   private static final int SETTLERS = 8;
   private static final int MIN_SOURCE_POPULATION = 16;
   private static final int FOUND_INTERVAL_DAYS = 3;
   private static final double PLAN_PLAYER_RANGE = 256.0;
   private static final double BUILD_PLAYER_RANGE = 192.0;
   private static final int BUILD_CHECK_TICKS = 100;

   private FactionExpansion() {
   }

   /** One day of expansion: every faction that can afford it claims at most one province. */
   public static void simulateDay(ServerLevel level, FactionWorldData data, long day) {
      Set<String> factions = new HashSet<>();
      data.settlements().forEach(settlement -> factions.add(settlement.faction()));

      for (String faction : factions) {
         if (!FactionDiplomacy.isOutlaw(faction)
            && data.treasury(faction) >= FOUND_COST
            && day - data.lastFounding(faction) >= FOUND_INTERVAL_DAYS) {
            FactionWorldData.Settlement source = largestSettlement(data, faction);
            if (source != null && source.population >= MIN_SOURCE_POPULATION) {
               FactionRegions.Province province = pickProvince(level, data, faction);
               if (province != null) {
                  data.setProvinceOwner(province.key(), faction);
                  data.addPlannedSettlement(new FactionWorldData.PlannedSettlement(faction, province.seedX(), province.seedZ(), province.key(), day));
                  data.setTreasury(faction, data.treasury(faction) - FOUND_COST);
                  source.population -= SETTLERS;
                  data.setLastFounding(faction, day);
                  AncientWarfareCore.LOG.info("Faction {} claimed province {},{} and plans a village at {}, {}", faction, province.cellX(), province.cellZ(), province.seedX(), province.seedZ());
               }
            }
         }
      }
   }

   /** Builds at most one planned village whose site no player is near. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % BUILD_CHECK_TICKS != 0L) {
         return;
      }

      Iterator<FactionWorldData.PlannedSettlement> iterator = data.plannedSettlements().iterator();

      while (iterator.hasNext()) {
         FactionWorldData.PlannedSettlement plan = iterator.next();
         if (!level.hasNearbyAlivePlayer(plan.x(), level.getSeaLevel(), plan.z(), BUILD_PLAYER_RANGE)) {
            iterator.remove();
            data.setDirty();
            boolean built = AwsWorldGenerator.generateSettlementAt(level, new ChunkPos(plan.x() >> 4, plan.z() >> 4), plan.faction());
            AncientWarfareCore.LOG.info("Faction {} {} its planned village at {}, {}", plan.faction(), built ? "built" : "could not build", plan.x(), plan.z());
            return;
         }
      }
   }

   private static FactionWorldData.Settlement largestSettlement(FactionWorldData data, String faction) {
      FactionWorldData.Settlement best = null;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction) && (best == null || settlement.population > best.population)) {
            best = settlement;
         }
      }

      return best;
   }

   /** A wild province next to faction's land whose climate suits its buildings, away from players. */
   private static FactionRegions.Province pickProvince(ServerLevel level, FactionWorldData data, String faction) {
      List<FactionRegions.Province> candidates = new ArrayList<>();
      Set<Long> seen = new HashSet<>();

      for (long owned : data.provincesOf(faction)) {
         int cellX = ChunkPos.getX(owned);
         int cellZ = ChunkPos.getZ(owned);

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               if ((dx != 0 || dz != 0) && seen.add(ChunkPos.asLong(cellX + dx, cellZ + dz))) {
                  FactionRegions.Province province = FactionRegions.provinceByCell(level, cellX + dx, cellZ + dz);
                  if (province.owner().isEmpty()
                     && !level.hasNearbyAlivePlayer(province.seedX(), level.getSeaLevel(), province.seedZ(), PLAN_PLAYER_RANGE)
                     && FactionRegions.suits(level, faction, province.seedX(), province.seedZ())) {
                     candidates.add(province);
                  }
               }
            }
         }
      }

      if (candidates.isEmpty()) {
         return null;
      }

      RandomSource random = level.getRandom();
      return candidates.get(random.nextInt(candidates.size()));
   }
}
