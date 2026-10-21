package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator;

/**
 * Factions grow outward: a faction with gold to spare and a settlement big enough to send settlers
 * claims a wild province next to its land and founds a village there. Where its land meets a rival
 * that is not an ally, a faction with gold to spare builds a border fort facing that rival. The claim
 * is immediate; villages and forts are built later, one at a time, while no player is near enough
 * to watch them appear.
 */
public final class FactionExpansion {
   private static final int FOUND_COST = 80;
   private static final int SETTLERS = 8;
   private static final int MIN_SOURCE_POPULATION = 16;
   private static final int FOUND_INTERVAL_DAYS = 3;
   private static final double PLAN_PLAYER_RANGE = 256.0;
   private static final double BUILD_PLAYER_RANGE = 192.0;
   private static final int BUILD_CHECK_TICKS = 100;
   public static final int FORT_COST = 60;
   /** A new fort's garrison is capped at this, whatever its template spawns. */
   public static final int FORT_GARRISON = 12;
   private static final int FORT_INTERVAL_DAYS = 4;
   /** How far from its own province's seed toward the rival's a fort stands. */
   private static final double FORT_BORDER_SHARE = 0.6;
   private static final String FORT_KEY = "fort:";

   private FactionExpansion() {
   }

   /** One day of expansion: every faction that can afford it claims at most one province. */
   public static void simulateDay(ServerLevel level, FactionWorldData data, long day) {
      Set<String> factions = new HashSet<>();
      data.settlements().forEach(settlement -> factions.add(settlement.faction()));

      for (String faction : factions) {
         // Securing the border comes before new villages.
         if (!FactionDiplomacy.isOutlaw(faction)
            && data.treasury(faction) >= FORT_COST
            && day - data.lastFounding(FORT_KEY + faction) >= FORT_INTERVAL_DAYS
            && planFort(level, data, faction, day, false) != null) {
            data.setTreasury(faction, data.treasury(faction) - FORT_COST);
         }

         if (!FactionDiplomacy.isOutlaw(faction)
            && data.treasury(faction) >= FOUND_COST
            && day - data.lastFounding(faction) >= FOUND_INTERVAL_DAYS) {
            FactionWorldData.Settlement source = largestSettlement(data, faction);
            if (source != null && source.population >= MIN_SOURCE_POPULATION) {
               FactionRegions.Province province = pickProvince(level, data, faction);
               if (province != null) {
                  data.setProvinceOwner(province.key(), faction);
                  data.addPlannedSettlement(new FactionWorldData.PlannedSettlement(faction, province.seedX(), province.seedZ(), province.key(), day, false));
                  data.setTreasury(faction, data.treasury(faction) - FOUND_COST);
                  source.population -= SETTLERS;
                  data.setLastFounding(faction, day);
                  AncientWarfareCore.LOG.info("Faction {} claimed province {},{} and plans a village at {}, {}", faction, province.cellX(), province.cellZ(), province.seedX(), province.seedZ());
               }
            }
         }
      }
   }

   /**
    * Plans a fort in one of the faction's provinces that borders a rival (any other faction that is
    * not its ally) and has no fort yet, placed toward that rival. Returns the plan, or null when no
    * border needs one. Away from players unless anywhere is set (for the debug command).
    */
   public static FactionWorldData.PlannedSettlement planFort(ServerLevel level, FactionWorldData data, String faction, long day, boolean anywhere) {
      Map<Long, String> owners = data.provinceOwners();
      List<FactionWorldData.PlannedSettlement> candidates = new ArrayList<>();

      for (long owned : data.provincesOf(faction)) {
         if (hasFort(data, owned)) {
            continue;
         }

         int cellX = ChunkPos.getX(owned);
         int cellZ = ChunkPos.getZ(owned);
         FactionRegions.Province home = FactionRegions.provinceByCell(level, cellX, cellZ);

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               String rival = owners.get(ChunkPos.asLong(cellX + dx, cellZ + dz));
               if ((dx != 0 || dz != 0)
                  && rival != null
                  && !rival.isEmpty()
                  && !rival.equals(faction)
                  && FactionDiplomacy.relation(level, faction, rival).state() != RelationState.ALLIANCE) {
                  FactionRegions.Province across = FactionRegions.provinceByCell(level, cellX + dx, cellZ + dz);
                  int x = (int)Math.round(home.seedX() + (across.seedX() - home.seedX()) * FORT_BORDER_SHARE);
                  int z = (int)Math.round(home.seedZ() + (across.seedZ() - home.seedZ()) * FORT_BORDER_SHARE);
                  if (FactionRegions.provinceAt(level, x, z).key() != owned) {
                     x = (home.seedX() + across.seedX()) / 2;
                     z = (home.seedZ() + across.seedZ()) / 2;
                  }

                  if (anywhere || !level.hasNearbyAlivePlayer(x, level.getSeaLevel(), z, PLAN_PLAYER_RANGE)) {
                     candidates.add(new FactionWorldData.PlannedSettlement(faction, x, z, owned, day, true));
                  }
               }
            }
         }
      }

      if (candidates.isEmpty()) {
         return null;
      }

      FactionWorldData.PlannedSettlement plan = candidates.get(level.getRandom().nextInt(candidates.size()));
      data.addPlannedSettlement(plan);
      data.setLastFounding(FORT_KEY + faction, day);
      AncientWarfareCore.LOG.info("Faction {} plans a border fort at {}, {}", faction, plan.x(), plan.z());
      return plan;
   }

   private static boolean hasFort(FactionWorldData data, long province) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.province() == province && "fort".equals(settlement.kind())) {
            return true;
         }
      }

      for (FactionWorldData.PlannedSettlement plan : data.plannedSettlements()) {
         if (plan.province() == province && plan.fort()) {
            return true;
         }
      }

      return false;
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
            boolean built = AwsWorldGenerator.generateSettlementAt(level, new ChunkPos(plan.x() >> 4, plan.z() >> 4), plan.faction(), plan.fort());
            if (built && plan.fort()) {
               FactionNews.fortified(level, plan.faction(), plan.x(), plan.z());
            } else if (built) {
               FactionNews.founded(level, plan.faction(), plan.x(), plan.z());
            }

            AncientWarfareCore.LOG
               .info("Faction {} {} its planned {} at {}, {}", plan.faction(), built ? "built" : "could not build", plan.fort() ? "fort" : "village", plan.x(), plan.z());
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
