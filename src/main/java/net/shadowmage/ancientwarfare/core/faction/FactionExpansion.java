package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Factions grow outward: a faction with gold to spare and a settlement big enough to send settlers
 * claims a wild province next to its land and founds a village there. Where its land meets a rival
 * that is not an ally, a faction with gold to spare builds a border fort facing that rival. The claim
 * is immediate; villages and forts are built later, one at a time, while no player is near enough
 * to watch them appear.
 */
public final class FactionExpansion {
   private static final int FOUND_COST = 80;
   private static final int FOUND_COST_PER_PROVINCE = 8;
   private static final String EMPIRE = "empire";
   private static final int EMPIRE_MARKUP_PERCENT = 150;
   /** Each this many provinces held adds a day to the wait between new villages. */
   private static final int PROVINCES_PER_EXTRA_DAY = 12;
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
   private static final double CAPITAL_PLAYER_RANGE = 320.0;
   private static final int CAPITAL_SEARCH_STEP = 64;
   /** Twice (a full capital, then a small one) the country's middle and ten rings around it, out to 640 blocks. */
   private static final int CAPITAL_SEARCH_POINTS = 162;
   private static final int VILLAGE_SEARCH_STEP = 40;
   private static final int VILLAGE_SEARCH_POINTS = 33;
   /** Sites checked per build check; each loads the land under it. */
   private static final int SITE_CHECKS_PER_TICK = 3;
   private static final int SETTLEMENT_CLEARANCE = 32;
   /** How far each search got; not saved, so a restart searches again from the centre. */
   private static final Map<String, Integer> CAPITAL_SEARCH = new HashMap<>();
   private static final Map<FactionWorldData.PlannedSettlement, Integer> SITE_SEARCH = new HashMap<>();

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

         int provinces = data.provincesOf(faction).size();
         int cost = foundingCost(faction, provinces);
         if (!FactionDiplomacy.isOutlaw(faction)
            && data.treasury(faction) >= cost
            && day - data.lastFounding(faction) >= FOUND_INTERVAL_DAYS + provinces / PROVINCES_PER_EXTRA_DAY) {
            FactionWorldData.Settlement source = largestSettlement(data, faction);
            if (source != null && source.population >= MIN_SOURCE_POPULATION) {
               FactionRegions.Province province = pickProvince(level, data, faction);
               if (province != null) {
                  data.setProvinceOwner(province.key(), faction);
                  data.addPlannedSettlement(new FactionWorldData.PlannedSettlement(faction, province.seedX(), province.seedZ(), province.key(), day, false));
                  data.setTreasury(faction, data.treasury(faction) - cost);
                  source.population -= SETTLERS;
                  data.setLastFounding(faction, day);
                  AncientWarfareCore.LOG.info("Faction {} claimed province {},{} and plans a village at {}, {}", faction, province.cellX(), province.cellZ(), province.seedX(), province.seedZ());
               }
            }
         }
      }
   }

   /**
    * A new village costs more the more land the faction already holds, so big countries slow down
    * and small ones catch up; the Empire, with buildings for nearly every climate, pays extra.
    */
   public static int foundingCost(String faction, int provinces) {
      int cost = FOUND_COST + FOUND_COST_PER_PROVINCE * provinces;
      return EMPIRE.equals(faction) ? cost * EMPIRE_MARKUP_PERCENT / 100 : cost;
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

   /**
    * Builds at most one planned village or fort whose site no player is near. A village is laid out
    * as a small town on dry, flat land of its province; where none is found it falls back to one of
    * the faction's non-military buildings.
    */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % BUILD_CHECK_TICKS != 0L) {
         return;
      }

      tickCapitals(level, data);
      Iterator<FactionWorldData.PlannedSettlement> iterator = data.plannedSettlements().iterator();

      while (iterator.hasNext()) {
         FactionWorldData.PlannedSettlement plan = iterator.next();
         if (level.hasNearbyAlivePlayer(plan.x(), level.getSeaLevel(), plan.z(), BUILD_PLAYER_RANGE)) {
            continue;
         }

         if (!plan.fort()) {
            if (AwtWorldGenerator.busy(level)) {
               continue;
            }

            String path = FactionTowns.path(plan.faction(), false);
            if (FactionTowns.canBuild(plan.faction(), false)) {
               int chunks = AwtWorldGenerator.townChunks(path);
               int checked = SITE_SEARCH.getOrDefault(plan, 0);
               int[] site = null;

               for (int i = 0; i < SITE_CHECKS_PER_TICK && site == null && checked < VILLAGE_SEARCH_POINTS; i++, checked++) {
                  site = checkSite(level, data, plan.faction(), plan.x(), plan.z(), checked, VILLAGE_SEARCH_STEP, chunks);
               }

               SITE_SEARCH.put(plan, checked);
               if (site != null) {
                  iterator.remove();
                  SITE_SEARCH.remove(plan);
                  data.setDirty();
                  AwtWorldGenerator.queueFactionTown(level, path, site[0], site[1]);
                  return;
               } else if (checked < VILLAGE_SEARCH_POINTS) {
                  return;
               }

               SITE_SEARCH.remove(plan);
            }
         }

         iterator.remove();
         data.setDirty();
         boolean built = AwsWorldGenerator.generateSettlementAt(
            level, new ChunkPos(plan.x() >> 4, plan.z() >> 4), plan.faction(), plan.fort() ? AwsWorldGenerator.Site.FORT : AwsWorldGenerator.Site.VILLAGE
         );
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

   /** A generated town was finished: tell the world. */
   public static void townBuilt(ServerLevel level, String faction, String kind, int x, int z) {
      if (FactionTowns.CAPITAL.equals(kind)) {
         FactionNews.capitalBuilt(level, faction, x, z);
      } else {
         FactionNews.founded(level, faction, x, z);
      }
   }

   /**
    * Every faction with land and no capital gets one, built as a town on dry, flat land of its own
    * near the middle of its country while no player is near. Existing worlds get theirs the same way.
    */
   private static void tickCapitals(ServerLevel level, FactionWorldData data) {
      if (AwtWorldGenerator.busy(level)) {
         return;
      }

      for (String faction : new TreeSet<>(data.provinceOwners().values())) {
         int checked = CAPITAL_SEARCH.getOrDefault(faction, 0);
         if (faction.isEmpty() || FactionDiplomacy.isOutlaw(faction) || checked >= CAPITAL_SEARCH_POINTS || hasCapital(data, faction)
            || !FactionTowns.canBuild(faction, true)) {
            continue;
         }

         FactionRegions.Province middle = middleProvince(level, data, faction);
         if (middle == null || level.hasNearbyAlivePlayer(middle.seedX(), level.getSeaLevel(), middle.seedZ(), CAPITAL_PLAYER_RANGE)) {
            continue;
         }

         // A full capital first; where no land is wide and flat enough, a capital the size of a village.
         boolean small = checked >= CAPITAL_SEARCH_POINTS / 2;
         String path = small ? FactionTowns.smallCapitalPath(faction) : FactionTowns.path(faction, true);
         int chunks = AwtWorldGenerator.townChunks(path);

         for (int i = 0; i < SITE_CHECKS_PER_TICK && checked < CAPITAL_SEARCH_POINTS; i++, checked++) {
            if (!small && checked >= CAPITAL_SEARCH_POINTS / 2) {
               break;
            }

            int index = small ? checked - CAPITAL_SEARCH_POINTS / 2 : checked;
            int[] site = checkSite(level, data, faction, middle.seedX(), middle.seedZ(), index, CAPITAL_SEARCH_STEP, chunks);
            if (site != null) {
               // Keep counting: if the town fails to build here, the next search starts further on.
               CAPITAL_SEARCH.put(faction, checked + 1);
               AwtWorldGenerator.queueFactionTown(level, path, site[0], site[1]);
               AncientWarfareCore.LOG.info("Faction {} builds its capital at {}, {}", faction, site[0], site[1]);
               return;
            }
         }

         CAPITAL_SEARCH.put(faction, checked);
         if (checked >= CAPITAL_SEARCH_POINTS) {
            AncientWarfareCore.LOG.info("Faction {} found no dry, flat land for a capital near {}, {}", faction, middle.seedX(), middle.seedZ());
         }

         return;
      }
   }

   private static boolean hasCapital(FactionWorldData data, String faction) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (FactionTowns.CAPITAL.equals(settlement.kind()) && settlement.faction().equals(faction)) {
            return true;
         }
      }

      return false;
   }

   /** The faction's province nearest the middle of all its land. */
   private static FactionRegions.Province middleProvince(ServerLevel level, FactionWorldData data, String faction) {
      List<FactionRegions.Province> provinces = new ArrayList<>();
      double sumX = 0.0;
      double sumZ = 0.0;

      for (long key : data.provincesOf(faction)) {
         FactionRegions.Province province = FactionRegions.provinceByCell(level, ChunkPos.getX(key), ChunkPos.getZ(key));
         provinces.add(province);
         sumX += province.seedX();
         sumZ += province.seedZ();
      }

      if (provinces.isEmpty()) {
         return null;
      }

      double middleX = sumX / provinces.size();
      double middleZ = sumZ / provinces.size();
      return provinces.stream()
         .min(Comparator.comparingDouble(province -> Math.pow(province.seedX() - middleX, 2) + Math.pow(province.seedZ() - middleZ, 2)))
         .orElse(null);
   }

   /** The nearest site to x, z fit for a town of this size on anyone's land, or null; for the test command. */
   public static int[] findTownSite(ServerLevel level, int x, int z, int chunks) {
      FactionWorldData data = FactionWorldData.get(level);

      for (int index = 0; index < VILLAGE_SEARCH_POINTS; index++) {
         int[] site = checkSite(level, data, null, x, z, index, VILLAGE_SEARCH_STEP, chunks);
         if (site != null) {
            return site;
         }
      }

      return null;
   }

   /**
    * The index-th point of a spiral around x, z (the centre first, then rings of eight), when it is
    * the faction's own land (any land for a null faction), clear of other settlements, dry and flat enough for a town of this size.
    */
   private static int[] checkSite(ServerLevel level, FactionWorldData data, String faction, int x, int z, int index, int step, int chunks) {
      if (chunks <= 0) {
         return null;
      }

      int siteX = x;
      int siteZ = z;
      if (index > 0) {
         int ring = 1 + (index - 1) / 8;
         double angle = Math.PI / 4.0 * ((index - 1) % 8) + ring * 0.4;
         siteX = x + (int)Math.round(Math.cos(angle) * ring * step);
         siteZ = z + (int)Math.round(Math.sin(angle) * ring * step);
      }

      if (faction != null && !faction.equals(FactionRegions.ownerAt(level, siteX, siteZ))) {
         AncientWarfareCore.LOG.debug("Site {}, {} for {}: not its land", siteX, siteZ, faction);
         return null;
      }

      int clearance = chunks * 8 + SETTLEMENT_CLEARANCE;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.contains(siteX, siteZ, clearance)) {
            AncientWarfareCore.LOG.debug("Site {}, {} for {}: next to settlement #{}", siteX, siteZ, faction, settlement.id());
            return null;
         }
      }

      return AwtWorldGenerator.siteAcceptable(level, siteX, siteZ, chunks) ? new int[]{siteX, siteZ} : null;
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

   /**
    * A wild province next to faction's land, away from players. Provinces whose climate suits the
    * faction's buildings come first; once none is left it settles any other climate.
    */
   private static FactionRegions.Province pickProvince(ServerLevel level, FactionWorldData data, String faction) {
      List<FactionRegions.Province> suited = new ArrayList<>();
      List<FactionRegions.Province> other = new ArrayList<>();
      Set<Long> seen = new HashSet<>();

      for (long owned : data.provincesOf(faction)) {
         int cellX = ChunkPos.getX(owned);
         int cellZ = ChunkPos.getZ(owned);

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               if ((dx != 0 || dz != 0) && seen.add(ChunkPos.asLong(cellX + dx, cellZ + dz))) {
                  FactionRegions.Province province = FactionRegions.provinceByCell(level, cellX + dx, cellZ + dz);
                  if (province.owner().isEmpty() && !level.hasNearbyAlivePlayer(province.seedX(), level.getSeaLevel(), province.seedZ(), PLAN_PLAYER_RANGE)) {
                     (FactionRegions.suits(level, faction, province.seedX(), province.seedZ()) ? suited : other).add(province);
                  }
               }
            }
         }
      }

      List<FactionRegions.Province> candidates = suited.isEmpty() ? other : suited;
      if (candidates.isEmpty()) {
         return null;
      }

      RandomSource random = level.getRandom();
      return candidates.get(random.nextInt(candidates.size()));
   }
}
