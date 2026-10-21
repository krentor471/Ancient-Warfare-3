package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/**
 * Trade between settlements. Each settlement keeps routes to its nearest trading partners: its own
 * faction's settlements and those of factions it is not at war with. A route pays both treasuries
 * every day, more between bigger settlements; war blocks it, and outlaws camped near a road with no
 * guard post on it rob some days' caravans.
 */
public final class FactionTrade {
   public static final String TRADE_POST = "tradepost";
   public static final String GUARD_POST = "guardpost";
   private static final int ROUTE_RANGE = 1200;
   /** Settlements this close are parts of one town and do not trade over a road. */
   private static final int MIN_ROUTE = 96;
   private static final int LINKS = 2;
   private static final int INCOME_DIVISOR = 20;
   private static final int ROBBERY_RANGE = 320;
   private static final double ROBBERY_CHANCE = 0.3;

   private FactionTrade() {
   }

   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      updateRoutes(level, data);

      for (FactionWorldData.Route route : new ArrayList<>(data.routes())) {
         FactionWorldData.Settlement a = data.settlement(route.a());
         FactionWorldData.Settlement b = data.settlement(route.b());
         if (a == null || b == null || route.road() == FactionWorldData.RoadState.NONE && (partOfOlder(data, a) || partOfOlder(data, b))) {
            data.removeRoute(route);
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
               && distance <= (double)ROUTE_RANGE * ROUTE_RANGE
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
      FactionWorldData.Settlement a = data.settlement(route.a());
      FactionWorldData.Settlement b = data.settlement(route.b());
      return a == null || b == null ? 0 : (int)Math.sqrt(a.horizontalDistanceSquared(b.center().getX(), b.center().getZ()));
   }
}
