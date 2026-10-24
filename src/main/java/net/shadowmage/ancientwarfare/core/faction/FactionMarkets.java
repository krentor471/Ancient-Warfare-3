package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Factions trade among themselves. Each day a faction short of a good buys it from a neighbour at
 * peace that has plenty, paying from its treasury at the seller's price; the trade warms them to
 * each other a little. A faction with no gold to pay goes without, and enemies never trade.
 */
public final class FactionMarkets {
   /** The most of one good a pair trades in a day. */
   private static final int MAX_LOAD = 60;
   /** A seller keeps this many times what it needs itself. */
   private static final int KEEP = 2;
   private static final double OPINION_CHANCE = 0.1;
   private static final double NEWS_CHANCE = 0.02;
   /** Opinion below which two factions will not trade even at peace. */
   private static final int MIN_OPINION = -30;

   private FactionMarkets() {
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      RandomSource random = level.getRandom();
      TreeSet<String> set = new TreeSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            set.add(settlement.faction());
         }
      }

      List<String> factions = new ArrayList<>(set);
      for (String buyer : factions) {
         int[] mine = data.stores(buyer);
         for (int kind = 0; kind < FactionResources.KINDS; kind++) {
            int need = FactionResources.NEED[kind];
            if (mine[kind] >= need) {
               continue;
            }

            // The richest seller at peace that will deal.
            String seller = null;
            int best = need * KEEP;
            for (String other : factions) {
               FactionDiplomacy.Relation relation = FactionDiplomacy.relation(level, buyer, other);
               if (!other.equals(buyer) && relation.state() != RelationState.WAR && relation.opinion() >= MIN_OPINION && data.stores(other)[kind] > best) {
                  best = data.stores(other)[kind];
                  seller = other;
               }
            }

            if (seller == null) {
               continue;
            }

            int[] theirs = data.stores(seller);
            double price = FactionResources.PRICE[kind];
            int load = Math.min(MAX_LOAD, Math.min(need - mine[kind], theirs[kind] - need * KEEP));
            load = Math.min(load, (int)(data.treasury(buyer) / Math.max(0.1, price) / 4));
            if (load <= 0) {
               continue;
            }

            int cost = (int)Math.ceil(load * price);
            mine[kind] += load;
            theirs[kind] -= load;
            data.setTreasury(buyer, data.treasury(buyer) - cost);
            data.setTreasury(seller, data.treasury(seller) + cost);
            if (random.nextDouble() < OPINION_CHANCE) {
               FactionDiplomacy.adjustOpinion(level, buyer, seller, 1);
               FactionDiplomacy.adjustOpinion(level, seller, buyer, 1);
            }

            if (random.nextDouble() < NEWS_CHANCE) {
               FactionNews.tell(
                  level,
                  Component.translatable("news.ancientwarfare.market", FactionNews.factionName(buyer), load, FactionDeals.goodsName(kind), FactionNews.factionName(seller))
               );
            }
         }
      }

      data.setDirty();
   }
}
