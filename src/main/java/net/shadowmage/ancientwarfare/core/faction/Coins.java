package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.shadowmage.ancientwarfare.core.common.npc.CoinMetal;
import net.shadowmage.ancientwarfare.core.item.CoinItem;

/**
 * Money between the player and the factions is paid in Ancient Warfare coins. Prices are counted in
 * silver coins; a gold coin is worth ten silver and a silver coin ten copper, and any mix pays, with
 * change given back in smaller coins.
 */
public final class Coins {
   private static final int COPPER = 1;
   private static final int SILVER = 10;
   private static final int GOLD = 100;
   private static final CoinMetal[] LARGEST_FIRST = {CoinMetal.GOLD, CoinMetal.SILVER, CoinMetal.COPPER};

   private Coins() {
   }

   private static int value(CoinMetal metal) {
      return switch (metal) {
         case GOLD, ANCIENT -> GOLD;
         case SILVER -> SILVER;
         case COPPER -> COPPER;
      };
   }

   private static boolean isCoin(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof CoinItem;
   }

   /** What the player's purse holds, in copper. */
   public static int purse(ServerPlayer player) {
      int total = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (isCoin(stack)) {
            total += value(CoinItem.getMetal(stack)) * stack.getCount();
         }
      }

      return total;
   }

   /** Takes a price of so many silver coins from the player, smallest coins first; false (and nothing taken) when he has too little. */
   public static boolean take(ServerPlayer player, int silver) {
      if (player.isCreative()) {
         return true;
      }

      int owed = silver * SILVER;
      if (purse(player) < owed) {
         return false;
      }

      for (int pass = LARGEST_FIRST.length - 1; pass >= 0 && owed > 0; pass--) {
         CoinMetal metal = LARGEST_FIRST[pass];
         int worth = value(metal);
         for (int slot = 0; slot < player.getInventory().getContainerSize() && owed > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isCoin(stack) && value(CoinItem.getMetal(stack)) == worth) {
               int used = Math.min(stack.getCount(), (owed + worth - 1) / worth);
               stack.shrink(used);
               owed -= used * worth;
            }
         }
      }

      // A big coin broken to pay a small sum: the change comes back.
      if (owed < 0) {
         giveCopper(player, -owed);
      }

      return true;
   }

   /** Gives the player so many silver coins' worth, in gold and silver. */
   public static void give(ServerPlayer player, int silver) {
      giveCopper(player, silver * SILVER);
   }

   private static void giveCopper(ServerPlayer player, int copper) {
      for (ItemStack stack : stacks(copper)) {
         player.getInventory().placeItemBackInInventory(stack);
      }
   }

   /** Puts so many silver coins' worth into a container (a town hall); what does not fit is lost. */
   public static void store(Container container, int silver) {
      for (ItemStack stack : stacks(silver * SILVER)) {
         HopperBlockEntity.addItem(null, container, stack, null);
      }
   }

   /** So many silver coins' worth, as stacks of coins. */
   public static List<ItemStack> silver(int silver) {
      return stacks(silver * SILVER);
   }

   private static List<ItemStack> stacks(int copper) {
      List<ItemStack> stacks = new ArrayList<>();
      for (CoinMetal metal : LARGEST_FIRST) {
         int count = copper / value(metal);
         copper -= count * value(metal);
         while (count > 0) {
            int size = Math.min(64, count);
            stacks.add(CoinItem.create(metal, size));
            count -= size;
         }
      }

      return stacks;
   }
}
