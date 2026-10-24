package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

/**
 * Trade between settlements. Each settlement keeps routes to its nearest trading partners: its own
 * faction's settlements and those of factions it is not at war with. A route pays both treasuries
 * every day, more between bigger settlements; war blocks it, and outlaws camped near a road with no
 * guard post on it rob some days' caravans.
 */
public final class FactionTrade {
   public static final String TRADE_POST = "tradepost";
   public static final String GUARD_POST = "guardpost";
   /** How far a settlement looks for trading partners, at the old region scale; it grows with the scale. */
   private static final int BASE_ROUTE_RANGE = 1200;
   /** Settlements this close are parts of one town and do not trade over a road. */
   private static final int MIN_ROUTE = 96;
   private static final int LINKS = 3;
   private static final int INCOME_DIVISOR = 20;
   private static final int ROBBERY_RANGE = 320;
   private static final double ROBBERY_CHANCE = 0.3;
   /** Daily trade with a player's village raises the faction's standing up to here, not further. */
   private static final int TRADE_STANDING_CAP = 40;

   private FactionTrade() {
   }

   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      updateRoutes(level, data);
      FactionStandingData standings = FactionStandingData.get(level);

      for (FactionWorldData.Route route : new ArrayList<>(data.routes())) {
         FactionWorldData.Settlement a = data.endpoint(route, true);
         FactionWorldData.Settlement b = data.endpoint(route, false);
         if (a == null || b == null || partOfOlder(data, a) || route.village() == null && partOfOlder(data, b)) {
            data.removeRoute(route);
            continue;
         }

         if (route.village() != null) {
            if (!standings.hasTrade(route.owner(), a.faction())) {
               data.removeRoute(route);
            } else {
               payVillage(level, data, standings, route, a, b);
            }

            continue;
         }

         route.blocked = !canTrade(level, a.faction(), b.faction());
         route.income = 0;
         if (!route.blocked) {
            int gold = 1 + (a.population + b.population) / INCOME_DIVISOR;
            if (robbed(level, data, a, b)) {
               AncientWarfareCore.LOG.debug("Outlaws robbed the caravan between #{} and #{}", a.id(), b.id());
            } else {
               route.income = gold;
               data.setTreasury(a.faction(), data.treasury(a.faction()) + (gold + 1) / 2);
               data.setTreasury(b.faction(), data.treasury(b.faction()) + gold / 2);
            }
         }
      }

      data.setDirty();
   }

   /** Links every trading settlement to its nearest partners that have no route yet. */
   public static void updateRoutes(ServerLevel level, FactionWorldData data) {
      List<FactionWorldData.Settlement> traders = new ArrayList<>();

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (trades(settlement) && !partOfOlder(data, settlement)) {
            traders.add(settlement);
         }
      }

      for (FactionWorldData.Settlement settlement : traders) {
         List<FactionWorldData.Settlement> partners = new ArrayList<>();

         for (FactionWorldData.Settlement other : traders) {
            double distance = settlement.horizontalDistanceSquared(other.center().getX(), other.center().getZ());
            if (other != settlement
               && distance >= (double)MIN_ROUTE * MIN_ROUTE
               && distance <= (double)routeRange() * routeRange()
               && canTrade(level, settlement.faction(), other.faction())) {
               partners.add(other);
            }
         }

         partners.sort(Comparator.comparingDouble(other -> settlement.horizontalDistanceSquared(other.center().getX(), other.center().getZ())));

         for (FactionWorldData.Settlement partner : partners.subList(0, Math.min(LINKS, partners.size()))) {
            if (data.route(settlement.id(), partner.id()) == null) {
               data.addRoute(settlement.id(), partner.id());
            }
         }
      }

      updatePlayerRoutes(level, data, traders);
   }

   /** Links each player village to the nearest settlements of the factions its owner trades with. */
   private static void updatePlayerRoutes(ServerLevel level, FactionWorldData data, List<FactionWorldData.Settlement> traders) {
      Map<UUID, Set<String>> agreements = FactionStandingData.get(level).tradeAgreements();

      for (Map.Entry<BlockPos, UUID> village : data.villages().entrySet()) {
         Set<String> partners = agreements.getOrDefault(village.getValue(), Set.of());
         if (partners.isEmpty()) {
            continue;
         }

         BlockPos pos = village.getKey();
         List<FactionWorldData.Settlement> candidates = new ArrayList<>();

         for (FactionWorldData.Settlement settlement : traders) {
            double distance = settlement.horizontalDistanceSquared(pos.getX(), pos.getZ());
            if (partners.contains(settlement.faction())
               && distance >= (double)MIN_ROUTE * MIN_ROUTE
               && distance <= (double)routeRange() * routeRange()) {
               candidates.add(settlement);
            }
         }

         candidates.sort(Comparator.comparingDouble(settlement -> settlement.horizontalDistanceSquared(pos.getX(), pos.getZ())));

         for (FactionWorldData.Settlement partner : candidates.subList(0, Math.min(LINKS, candidates.size()))) {
            if (data.playerRoute(partner.id(), pos) == null) {
               data.addPlayerRoute(partner.id(), pos, village.getValue());
            }
         }
      }
   }

   /**
    * A day's trade between a faction settlement and a player's village: the faction's half goes to
    * its treasury, the player's half to the town hall as coins, and steady trade warms the faction.
    */
   private static void payVillage(
      ServerLevel level, FactionWorldData data, FactionStandingData standings, FactionWorldData.Route route, FactionWorldData.Settlement settlement, FactionWorldData.Settlement village
   ) {
      route.blocked = false;
      route.income = 0;
      int gold = 1 + (settlement.population + village.population) / INCOME_DIVISOR;
      if (robbed(level, data, settlement, village)) {
         return;
      }

      route.income = gold;
      data.setTreasury(settlement.faction(), data.treasury(settlement.faction()) + (gold + 1) / 2);
      if (level.getBlockEntity(route.village()) instanceof TownHallBlockEntity townHall) {
         // Whatever does not fit in a full town hall is lost.
         Coins.store(townHall, Math.max(1, gold / 2));
      }

      if (standings.getStanding(route.owner(), settlement.faction()) < TRADE_STANDING_CAP) {
         standings.adjustStanding(route.owner(), settlement.faction(), 1);
      }
   }

   /** Trade routes the player's villages keep with this faction. */
   public static int playerRoutes(FactionWorldData data, UUID owner, String faction) {
      int count = 0;

      for (FactionWorldData.Route route : data.routes()) {
         FactionWorldData.Settlement settlement = route.village() == null ? null : data.settlement(route.a());
         if (settlement != null && owner.equals(route.owner()) && settlement.faction().equals(faction)) {
            count++;
         }
      }

      return count;
   }

   /** A building of a town another settlement already speaks for: the town trades once, under its oldest settlement. */
   private static boolean partOfOlder(FactionWorldData data, FactionWorldData.Settlement settlement) {
      for (FactionWorldData.Settlement other : data.settlements()) {
         if (other.id() < settlement.id()
            && trades(other)
            && settlement.horizontalDistanceSquared(other.center().getX(), other.center().getZ()) < (double)MIN_ROUTE * MIN_ROUTE) {
            return true;
         }
      }

      return false;
   }

   private static int routeRange() {
      return BASE_ROUTE_RANGE * FactionRegions.scale() / 100;
   }

   public static boolean canTrade(ServerLevel level, String faction, String other) {
      return !FactionDiplomacy.isOutlaw(faction)
         && !FactionDiplomacy.isOutlaw(other)
         && (faction.equals(other) || FactionDiplomacy.relation(level, faction, other).state() != RelationState.WAR);
   }

   /** Villages and towns trade; forts and roadside posts serve the roads instead. */
   public static boolean trades(FactionWorldData.Settlement settlement) {
      String kind = settlement.kind();
      return !FactionDiplomacy.isOutlaw(settlement.faction())
         && !"fort".equals(kind)
         && !TRADE_POST.equals(kind)
         && !GUARD_POST.equals(kind);
   }

   /** Outlaws near the road's middle rob the day's caravan sometimes, unless a manned guard post watches that stretch. */
   private static boolean robbed(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement a, FactionWorldData.Settlement b) {
      double midX = (a.center().getX() + b.center().getX()) / 2.0;
      double midZ = (a.center().getZ() + b.center().getZ()) / 2.0;
      boolean outlaws = false;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.horizontalDistanceSquared(midX, midZ) <= (double)ROBBERY_RANGE * ROBBERY_RANGE) {
            if (GUARD_POST.equals(settlement.kind()) && settlement.garrison() > 0) {
               return false;
            }

            outlaws |= FactionDiplomacy.isOutlaw(settlement.faction());
         }
      }

      return outlaws && level.getRandom().nextDouble() < ROBBERY_CHANCE;
   }

   public static int length(FactionWorldData data, FactionWorldData.Route route) {
      FactionWorldData.Settlement a = data.endpoint(route, true);
      FactionWorldData.Settlement b = data.endpoint(route, false);
      return a == null || b == null ? 0 : (int)Math.sqrt(a.horizontalDistanceSquared(b.center().getX(), b.center().getZ()));
   }
}
