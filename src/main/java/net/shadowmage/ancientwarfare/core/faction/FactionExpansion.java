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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Factions grow outward: a faction with gold to spare and a settlement big enough to send settlers
 * founds a village in an empty province of its own or claims a wild one next to its land. Where its
 * land meets a rival that is not an ally, a faction with gold to spare builds a border fort facing
 * that rival. Capitals and villages live on paper first: they grow and fight in the daily
 * simulation and are laid out as towns, nearest to a player first, before the player gets there.
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
   /** Settlements further than this from every player stay on paper. */
   private static final double BUILD_REACH = 1500.0;
   private static final int BUILD_CHECK_TICKS = 100;
   /** Towns are laid out more often than the rest is looked at: a site is mostly ruled out from the noise, cheaply. */
   private static final int LAYOUT_CHECK_TICKS = 20;
   public static final int FORT_COST = 60;
   /** A new fort's garrison is capped at this, whatever its template spawns. */
   public static final int FORT_GARRISON = 12;
   private static final int FORT_INTERVAL_DAYS = 4;
   /** How far from its own province's seed toward the rival's a fort stands. */
   private static final double FORT_BORDER_SHARE = 0.6;
   private static final String FORT_KEY = "fort:";
   /** Spots where a fort could not be built since the server started, by chunk; no fort is planned within FAILED_FORT_RANGE of one. */
   private static final Set<Long> FAILED_FORTS = new HashSet<>();
   private static final int FAILED_FORT_RANGE = 64;
   private static final int CAPITAL_POPULATION = 60;
   private static final int CAPITAL_GARRISON = 15;
   private static final int CAPITAL_SEARCH_STEP = 64;
   /** Twice (a full capital, then a small one) the country's middle and ten rings around it, out to 640 blocks. */
   private static final int CAPITAL_SEARCH_POINTS = 162;
   private static final int VILLAGE_SEARCH_STEP = 40;
   private static final int VILLAGE_SEARCH_POINTS = 33;
   /** Sites checked per layout check; most are ruled out from the noise, the rest load the land under them. */
   private static final int SITE_CHECKS_PER_TICK = 6;
   private static final int SETTLEMENT_CLEARANCE = 32;
   /** How far each settlement's site search got, by settlement; not saved, so a restart searches again from the centre. */
   private static final Map<Integer, Integer> SITE_SEARCH = new HashMap<>();
   /** Capitals that found no site this session. */
   private static final Set<Integer> GIVEN_UP = new HashSet<>();

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
                  data.addUnbuiltSettlement(
                     faction, FactionTowns.VILLAGE, province.seedX(), level.getSeaLevel(), province.seedZ(), province.key(), level.getGameTime(), 0, SETTLERS
                  );
                  data.setTreasury(faction, data.treasury(faction) - cost);
                  source.population -= SETTLERS;
                  data.setLastFounding(faction, day);
                  FactionNews.founded(level, faction, province.seedX(), province.seedZ());
                  AncientWarfareCore.LOG.info("Faction {} founded a village in province {},{} near {}, {}", faction, province.cellX(), province.cellZ(), province.seedX(), province.seedZ());
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

                  if ((anywhere || !level.hasNearbyAlivePlayer(x, level.getSeaLevel(), z, PLAN_PLAYER_RANGE))
                     && !nearFailedFort(x, z)
                     && !nearPlannedFort(data, x, z)
                     && AwtWorldGenerator.roughSiteAcceptable(level, x, z, 1)) {
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
    * Founds capitals on paper, builds at most one planned fort no player is near, and lays out the
    * unbuilt settlement nearest to a player (but not in sight of one) as a town.
    */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % BUILD_CHECK_TICKS != 0L) {
         if (level.getGameTime() % LAYOUT_CHECK_TICKS == 0L) {
            buildNearest(level, data);
         }

         return;
      }

      foundCapitals(level, data);
      Iterator<FactionWorldData.PlannedSettlement> iterator = data.plannedSettlements().iterator();

      while (iterator.hasNext()) {
         FactionWorldData.PlannedSettlement plan = iterator.next();
         if (!plan.fort()) {
            // A village planned before villages lived on paper.
            iterator.remove();
            data.addUnbuiltSettlement(plan.faction(), FactionTowns.VILLAGE, plan.x(), level.getSeaLevel(), plan.z(), plan.province(), level.getGameTime(), 0, SETTLERS);
            continue;
         }

         if (level.hasNearbyAlivePlayer(plan.x(), level.getSeaLevel(), plan.z(), BUILD_PLAYER_RANGE) || !withinReach(level, plan.x(), plan.z())) {
            continue;
         }

         iterator.remove();
         data.setDirty();
         if (nearFailedFort(plan.x(), plan.z())) {
            // Another faction's fort already failed on this spot; the land will not have changed.
            AncientWarfareCore.LOG.info("Faction {} drops its planned fort at {}, {}: no room there", plan.faction(), plan.x(), plan.z());
            continue;
         }

         boolean built = AwsWorldGenerator.generateSettlementAt(level, new ChunkPos(plan.x() >> 4, plan.z() >> 4), plan.faction(), AwsWorldGenerator.Site.FORT);
         if (built) {
            FactionNews.fortified(level, plan.faction(), plan.x(), plan.z());
         }

         if (!built) {
            FAILED_FORTS.add(ChunkPos.asLong(plan.x() >> 4, plan.z() >> 4));
         }

         AncientWarfareCore.LOG.info("Faction {} {} its planned fort at {}, {}", plan.faction(), built ? "built" : "could not build", plan.x(), plan.z());
         return;
      }

      buildNearest(level, data);
   }

   /** True when any faction already plans a fort this close: two rivals meeting on a border pick the same midpoint. */
   private static boolean nearPlannedFort(FactionWorldData data, int x, int z) {
      for (FactionWorldData.PlannedSettlement plan : data.plannedSettlements()) {
         if (plan.fort() && Math.abs(plan.x() - x) < FAILED_FORT_RANGE && Math.abs(plan.z() - z) < FAILED_FORT_RANGE) {
            return true;
         }
      }

      return false;
   }

   private static boolean nearFailedFort(int x, int z) {
      int range = FAILED_FORT_RANGE >> 4;
      for (int dx = -range; dx <= range; dx++) {
         for (int dz = -range; dz <= range; dz++) {
            if (FAILED_FORTS.contains(ChunkPos.asLong((x >> 4) + dx, (z >> 4) + dz))) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean withinReach(ServerLevel level, double x, double z) {
      for (ServerPlayer player : level.players()) {
         double dx = player.getX() - x;
         double dz = player.getZ() - z;
         if (dx * dx + dz * dz <= BUILD_REACH * BUILD_REACH) {
            return true;
         }
      }

      return false;
   }

   /** Every faction with land and no capital founds one on paper, near the middle of its country. */
   public static void foundCapitals(ServerLevel level, FactionWorldData data) {
      for (String faction : new TreeSet<>(data.provinceOwners().values())) {
         if (faction.isEmpty() || FactionDiplomacy.isOutlaw(faction) || hasCapital(data, faction) || !FactionTowns.canBuild(faction, true)) {
            continue;
         }

         FactionRegions.Province middle = middleProvince(level, data, faction);
         if (middle != null) {
            data.addUnbuiltSettlement(
               faction, FactionTowns.CAPITAL, middle.seedX(), level.getSeaLevel(), middle.seedZ(), middle.key(), level.getGameTime(), CAPITAL_GARRISON, CAPITAL_POPULATION
            );
            AncientWarfareCore.LOG.info("Faction {} founds its capital near {}, {}", faction, middle.seedX(), middle.seedZ());
         }
      }
   }

   /**
    * Lays out the unbuilt settlement nearest to any player, as long as it is within reach of one
    * and none is close enough to watch, so the land ahead of a player fills first. Its site is searched near where it was
    * founded on the faction's own dry, flat land, a few spots per check. A capital that finds no
    * room for a full town looks again for room for one the size of a village, then stays on paper;
    * a village that finds none becomes a single dry-land building of its faction.
    */
   private static void buildNearest(ServerLevel level, FactionWorldData data) {
      if (AwtWorldGenerator.busy(level) || level.players().isEmpty()) {
         return;
      }

      FactionWorldData.Settlement target = null;
      double best = Double.MAX_VALUE;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.built() || GIVEN_UP.contains(settlement.id())) {
            continue;
         }

         double nearest = Double.MAX_VALUE;

         for (ServerPlayer player : level.players()) {
            nearest = Math.min(nearest, settlement.horizontalDistanceSquared(player.getX(), player.getZ()));
         }

         if (nearest >= BUILD_PLAYER_RANGE * BUILD_PLAYER_RANGE && nearest <= BUILD_REACH * BUILD_REACH && nearest < best) {
            best = nearest;
            target = settlement;
         }
      }

      if (target == null) {
         return;
      }

      String faction = target.faction();
      boolean capital = FactionTowns.CAPITAL.equals(target.kind());
      int points = capital ? CAPITAL_SEARCH_POINTS : VILLAGE_SEARCH_POINTS;
      int checked = SITE_SEARCH.getOrDefault(target.id(), 0);
      boolean small = capital && checked >= points / 2;
      String path = small ? FactionTowns.smallCapitalPath(faction) : FactionTowns.path(faction, capital);
      int chunks = FactionTowns.canBuild(faction, capital) ? AwtWorldGenerator.townChunks(path) : 0;
      BlockPos center = target.center();

      for (int i = 0; i < SITE_CHECKS_PER_TICK && checked < points && chunks > 0; i++, checked++) {
         if (capital && !small && checked >= points / 2) {
            break;
         }

         int index = small ? checked - points / 2 : checked;
         int[] site = checkSite(level, data, faction, center.getX(), center.getZ(), index, capital ? CAPITAL_SEARCH_STEP : VILLAGE_SEARCH_STEP, chunks, target);
         if (site != null) {
            // Keep counting: if the town fails to build here, the next search starts further on.
            SITE_SEARCH.put(target.id(), checked + 1);
            AwtWorldGenerator.queueFactionTown(level, path, site[0], site[1], target.id());
            AncientWarfareCore.LOG.info("Faction {} lays out its {} #{} at {}, {}", faction, target.kind(), target.id(), site[0], site[1]);
            return;
         }
      }

      if (chunks > 0 && checked < points) {
         SITE_SEARCH.put(target.id(), checked);
         return;
      }

      SITE_SEARCH.remove(target.id());
      if (capital) {
         GIVEN_UP.add(target.id());
         AncientWarfareCore.LOG.info("Faction {} found no dry, flat land for its capital near {}, {}; it stays on paper", faction, center.getX(), center.getZ());
         return;
      }

      data.removeSettlement(target);
      boolean built = AwsWorldGenerator.generateSettlementAt(level, new ChunkPos(center.getX() >> 4, center.getZ() >> 4), faction, AwsWorldGenerator.Site.VILLAGE);
      AncientWarfareCore.LOG.info("Faction {} {} a single building for village #{} near {}, {}", faction, built ? "built" : "could not build", target.id(), center.getX(), center.getZ());
   }

   /** A generated town nobody founded (the test command) was finished: tell the world. */
   public static void townBuilt(ServerLevel level, String faction, String kind, int x, int z) {
      if (FactionTowns.CAPITAL.equals(kind)) {
         FactionNews.capitalBuilt(level, faction, x, z);
      } else {
         FactionNews.founded(level, faction, x, z);
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
      // Not on top of a village already standing there, unless every province has one.
      Set<Long> settled = new HashSet<>();
      data.settlements().forEach(settlement -> settled.add(settlement.province()));
      if (provinces.stream().anyMatch(province -> !settled.contains(province.key()))) {
         provinces.removeIf(province -> settled.contains(province.key()));
      }

      return provinces.stream()
         .min(Comparator.comparingDouble(province -> Math.pow(province.seedX() - middleX, 2) + Math.pow(province.seedZ() - middleZ, 2)))
         .orElse(null);
   }

   /** The nearest site to x, z fit for a town of this size on anyone's land, or null; for the test command. */
   public static int[] findTownSite(ServerLevel level, int x, int z, int chunks) {
      FactionWorldData data = FactionWorldData.get(level);

      for (int index = 0; index < VILLAGE_SEARCH_POINTS; index++) {
         int[] site = checkSite(level, data, null, x, z, index, VILLAGE_SEARCH_STEP, chunks, null);
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
   private static int[] checkSite(
      ServerLevel level, FactionWorldData data, String faction, int x, int z, int index, int step, int chunks, FactionWorldData.Settlement building
   ) {
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
         if (settlement != building && settlement.contains(siteX, siteZ, clearance)) {
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
    * Where the faction's next village goes: one of its own provinces that has no settlement yet,
    * else a wild province next to its land. Wild provinces whose climate suits the faction's
    * buildings come first; once none is left it settles any other climate. The village exists on
    * paper until a player comes near, so players nearby do not matter here.
    */
   private static FactionRegions.Province pickProvince(ServerLevel level, FactionWorldData data, String faction) {
      List<FactionRegions.Province> empty = new ArrayList<>();
      List<FactionRegions.Province> suited = new ArrayList<>();
      List<FactionRegions.Province> other = new ArrayList<>();
      Set<Long> settled = new HashSet<>();
      data.settlements().forEach(settlement -> settled.add(settlement.province()));
      Set<Long> seen = new HashSet<>();

      for (long owned : data.provincesOf(faction)) {
         int cellX = ChunkPos.getX(owned);
         int cellZ = ChunkPos.getZ(owned);
         if (!settled.contains(owned)) {
            empty.add(FactionRegions.provinceByCell(level, cellX, cellZ));
         }

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               if ((dx != 0 || dz != 0) && seen.add(ChunkPos.asLong(cellX + dx, cellZ + dz))) {
                  FactionRegions.Province province = FactionRegions.provinceByCell(level, cellX + dx, cellZ + dz);
                  if (province.owner().isEmpty()) {
                     (FactionRegions.suits(level, faction, province.seedX(), province.seedZ()) ? suited : other).add(province);
                  }
               }
            }
         }
      }

      List<FactionRegions.Province> candidates = !empty.isEmpty() ? empty : !suited.isEmpty() ? suited : other;
      if (candidates.isEmpty()) {
         return null;
      }

      RandomSource random = level.getRandom();
      return candidates.get(random.nextInt(candidates.size()));
   }
}
