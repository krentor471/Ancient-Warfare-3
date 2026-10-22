package net.shadowmage.ancientwarfare.core.faction;

import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * What a faction builds and arms with besides gold: wood from its villages' woodlands, stone and
 * iron from their mines. Every settlement brings in a little each day, more with more people; a
 * capital and its mines most of all. Villages and forts need wood and stone, a new soldier needs
 * iron for his arms, a bigger town both, and ships wood.
 */
public final class FactionResources {
   public static final int WOOD = 0;
   public static final int STONE = 1;
   public static final int IRON = 2;
   private static final int KINDS = 3;
   /** No store grows past this; what more comes in goes to waste. */
   private static final int MAX_STORE = 3000;
   private static final int[] STARTING = {200, 150, 150};
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
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         int[] made = output(settlement);
         int[] store = data.stores(settlement.faction());
         for (int kind = 0; kind < KINDS; kind++) {
            store[kind] = Math.min(MAX_STORE, store[kind] + made[kind]);
         }
      }

      data.setDirty();
   }

   /** What one settlement brings in a day: wood, stone, iron. */
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
      return new int[]{
         (town ? 1 : 0) + people / 15,
         mines + hills + people / 30,
         mines + hills + people / 25
      };
   }

   public static boolean has(FactionWorldData data, String faction, int[] cost) {
      int[] store = data.stores(faction);
      for (int kind = 0; kind < KINDS; kind++) {
         if (store[kind] < cost[kind]) {
            return false;
         }
      }

      return true;
   }

   public static void spend(FactionWorldData data, String faction, int[] cost) {
      int[] store = data.stores(faction);
      for (int kind = 0; kind < KINDS; kind++) {
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
