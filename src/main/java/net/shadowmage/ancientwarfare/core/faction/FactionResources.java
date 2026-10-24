package net.shadowmage.ancientwarfare.core.faction;

import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * What a faction builds and arms with besides gold: wood from its villages' woodlands, stone and
 * iron from their mines. Every settlement brings in a little each day, more with more people; a
 * capital and its mines most of all. Villages and forts need wood and stone, a new soldier needs
 * iron for his arms, a bigger town both, and ships wood.
 *
 * Besides these, goods made for trade: grain from the fields, leather and wool from the herds,
 * coal and copper from the mines. Each faction is known for one of them and makes twice as much of
 * it; a little of each is used up every day, so stores of them settle where use meets work.
 */
public final class FactionResources {
   public static final int WOOD = 0;
   public static final int STONE = 1;
   public static final int IRON = 2;
   public static final int GRAIN = 3;
   public static final int LEATHER = 4;
   public static final int WOOL = 5;
   public static final int COAL = 6;
   public static final int COPPER = 7;
   public static final int KINDS = 8;
   /** The first of the goods made for trade. */
   private static final int TRADE_GOODS = GRAIN;
   public static final String[] NAMES = {"wood", "stone", "iron", "grain", "leather", "wool", "coal", "copper"};
   /** What an item of each kind is worth in silver to a faction with middling stores. */
   public static final double[] PRICE = {0.5, 0.5, 2.0, 0.3, 1.5, 1.0, 0.8, 1.2};
   /** Stores of each kind below which a faction feels the want of it. */
   public static final int[] NEED = {300, 300, 300, 400, 120, 120, 120, 80};
   /** The share of each trade good used up a day. */
   private static final double USE = 0.02;
   /** No store grows past this; what more comes in goes to waste. */
   private static final int MAX_STORE = 3000;
   private static final int[] STARTING = {200, 150, 150, 300, 80, 80, 100, 60};
   public static final int[] VILLAGE_COST = {30, 10, 0};
   public static final int[] FORT_COST = {20, 20, 5};
   public static final int[] SHIP_COST = {25, 0, 2};
   public static final int RECRUIT_IRON = 1;
   private static final int TIER_COST = 15;

   private FactionResources() {
   }

   static int[] startingStores() {
      return STARTING.clone();
   }

   /** One day's work in every settlement. */
   static void simulateDay(FactionWorldData data) {
      java.util.Set<String> factions = new java.util.HashSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         int[] made = output(settlement);
         int[] store = data.stores(settlement.faction());
         factions.add(settlement.faction());
         for (int kind = 0; kind < KINDS; kind++) {
            store[kind] = Math.min(MAX_STORE, store[kind] + made[kind]);
         }
      }

      for (String faction : factions) {
         int[] store = data.stores(faction);
         for (int kind = TRADE_GOODS; kind < KINDS; kind++) {
            store[kind] -= (int)Math.ceil(store[kind] * USE);
         }
      }

      data.setDirty();
   }

   /** The trade good the faction is known for. */
   public static int specialty(String faction) {
      return TRADE_GOODS + Math.floorMod(faction.hashCode(), KINDS - TRADE_GOODS);
   }

   /** What one settlement brings in a day, of every kind. */
   static int[] output(FactionWorldData.Settlement settlement) {
      int people = settlement.population();
      int mines = switch (settlement.kind()) {
         case FactionTowns.CAPITAL -> 2;
         case FactionTowns.VILLAGE, "town" -> 1;
         default -> 0;
      };
      boolean town = mines > 0;
      // Hill country is richer in stone and ore.
      int hills = settlement.built() && settlement.center().getY() > 95 ? 1 : 0;
      int village = town ? 1 : 0;
      int[] made = {
         village + people / 15,
         mines + hills + people / 30,
         mines + hills + people / 25,
         village * 2 + people / 10,
         village + people / 30,
         village + people / 30,
         mines + hills + people / 40,
         mines + hills
      };
      made[specialty(settlement.faction())] *= 2;
      return made;
   }

   public static boolean has(FactionWorldData data, String faction, int[] cost) {
      int[] store = data.stores(faction);
      for (int kind = 0; kind < cost.length; kind++) {
         if (store[kind] < cost[kind]) {
            return false;
         }
      }

      return true;
   }

   public static void spend(FactionWorldData data, String faction, int[] cost) {
      int[] store = data.stores(faction);
      for (int kind = 0; kind < cost.length; kind++) {
         store[kind] = Math.max(0, store[kind] - cost[kind]);
      }

      data.setDirty();
   }

   /** Wood and stone to raise a settlement to the next tier. */
   static int[] tierCost(int tier) {
      return new int[]{TIER_COST * (tier + 1), TIER_COST * (tier + 1), 0};
   }

   static boolean hasIron(FactionWorldData data, String faction) {
      return data.stores(faction)[IRON] >= RECRUIT_IRON;
   }

   static void spendIron(FactionWorldData data, String faction) {
      int[] store = data.stores(faction);
      store[IRON] = Math.max(0, store[IRON] - RECRUIT_IRON);
   }
}
