package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public final class TradeOrderData {
   public static final int MAX_TRADES = 8;
   public static final int TRADE_GRID_SIZE = 9;
   private static final String ORDERS_TAG = "orders";
   private static final String TRADE_LIST_TAG = "tradeList";
   private static final String INPUT_TAG = "input";
   private static final String OUTPUT_TAG = "output";
   private static final String INPUTS_TAG = "inputs";
   private static final String OUTPUTS_TAG = "outputs";
   private static final String SLOT_TAG = "slot";
   private static final String ROUTE_TARGET_TAG = "routeTarget";

   private TradeOrderData() {
   }

   public static List<TradeOrderData.TradeEntry> getTrades(ItemStack stack) {
      List<TradeOrderData.TradeEntry> trades = new ArrayList<>();
      CompoundTag orders = getOrdersTag(stack);
      if (orders == null) {
         return trades;
      } else {
         ListTag list = orders.getList("tradeList", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag trade = list.getCompound(i);
            trades.add(new TradeOrderData.TradeEntry(readStacks(trade, "inputs", "input"), readStacks(trade, "outputs", "output")));
         }

         return trades;
      }
   }

   public static boolean addTrade(ItemStack stack) {
      List<TradeOrderData.TradeEntry> trades = getTrades(stack);
      if (trades.size() >= 8) {
         return false;
      } else {
         trades.add(TradeOrderData.TradeEntry.empty());
         setTrades(stack, trades);
         return true;
      }
   }

   public static boolean moveTradeUp(ItemStack stack, int index) {
      List<TradeOrderData.TradeEntry> trades = getTrades(stack);
      if (index > 0 && index < trades.size()) {
         TradeOrderData.TradeEntry entry = trades.remove(index);
         trades.add(index - 1, entry);
         setTrades(stack, trades);
         return true;
      } else {
         return false;
      }
   }

   public static boolean moveTradeDown(ItemStack stack, int index) {
      List<TradeOrderData.TradeEntry> trades = getTrades(stack);
      if (index >= 0 && index < trades.size() - 1) {
         TradeOrderData.TradeEntry entry = trades.remove(index);
         trades.add(index + 1, entry);
         setTrades(stack, trades);
         return true;
      } else {
         return false;
      }
   }

   public static boolean setTradeSlot(ItemStack stack, int tradeIndex, boolean output, int slot, ItemStack value) {
      if (slot >= 0 && slot < 9) {
         List<TradeOrderData.TradeEntry> trades = getTrades(stack);
         if (tradeIndex >= 0 && tradeIndex < trades.size()) {
            TradeOrderData.TradeEntry entry = trades.get(tradeIndex);
            List<ItemStack> inputs = copyStacks(entry.inputs());
            List<ItemStack> outputs = copyStacks(entry.outputs());
            ItemStack ghost = value != null && !value.isEmpty() ? value.copy() : ItemStack.EMPTY;
            if (!ghost.isEmpty()) {
               ghost.setCount(Math.min(ghost.getCount(), ghost.getMaxStackSize()));
            }

            (output ? outputs : inputs).set(slot, ghost);
            trades.set(tradeIndex, new TradeOrderData.TradeEntry(inputs, outputs));
            setTrades(stack, trades);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean removeTrade(ItemStack stack, int index) {
      List<TradeOrderData.TradeEntry> trades = getTrades(stack);
      if (index >= 0 && index < trades.size()) {
         trades.remove(index);
         setTrades(stack, trades);
         return true;
      } else {
         return false;
      }
   }

   public static boolean isValidTradeOrder(ItemStack stack) {
      return getTrades(stack).stream().anyMatch(TradeOrderData.TradeEntry::isValid);
   }

   public static Optional<BlockPos> getRouteTarget(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      return orders != null && orders.contains("routeTarget", 4) ? Optional.of(BlockPos.of(orders.getLong("routeTarget"))) : Optional.empty();
   }

   public static void setRouteTarget(ItemStack stack, BlockPos target) {
      if (stack != null && !stack.isEmpty() && target != null) {
         CompoundTag root = stack.getOrCreateTag();
         CompoundTag orders = root.contains("orders", 10) ? root.getCompound("orders").copy() : new CompoundTag();
         orders.putLong("routeTarget", target.asLong());
         root.put("orders", orders);
      }
   }

   private static void setTrades(ItemStack stack, List<TradeOrderData.TradeEntry> trades) {
      CompoundTag root = stack.getOrCreateTag();
      CompoundTag orders = root.contains("orders", 10) ? root.getCompound("orders").copy() : new CompoundTag();
      ListTag list = new ListTag();
      trades.stream().limit(8L).forEach(entry -> {
         CompoundTag trade = new CompoundTag();
         writeStacks(trade, "inputs", entry.inputs());
         writeStacks(trade, "outputs", entry.outputs());
         list.add(trade);
      });
      orders.put("tradeList", list);
      root.put("orders", orders);
   }

   private static List<ItemStack> readStacks(CompoundTag trade, String listKey, String legacyKey) {
      List<ItemStack> stacks = emptyStacks();
      if (trade.contains(listKey, 9)) {
         ListTag list = trade.getList(listKey, 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag stackTag = list.getCompound(i);
            int slot = stackTag.getInt("slot");
            if (slot >= 0 && slot < 9) {
               stacks.set(slot, ItemStack.of(stackTag));
            }
         }
      } else if (trade.contains(legacyKey, 10)) {
         stacks.set(0, ItemStack.of(trade.getCompound(legacyKey)));
      }

      return stacks;
   }

   private static void writeStacks(CompoundTag trade, String listKey, List<ItemStack> stacks) {
      ListTag list = new ListTag();

      for (int i = 0; i < Math.min(9, stacks.size()); i++) {
         ItemStack stack = stacks.get(i);
         if (!stack.isEmpty()) {
            CompoundTag stackTag = stack.save(new CompoundTag());
            stackTag.putInt("slot", i);
            list.add(stackTag);
         }
      }

      trade.put(listKey, list);
   }

   private static List<ItemStack> emptyStacks() {
      List<ItemStack> stacks = new ArrayList<>();

      for (int i = 0; i < 9; i++) {
         stacks.add(ItemStack.EMPTY);
      }

      return stacks;
   }

   private static List<ItemStack> copyStacks(List<ItemStack> source) {
      List<ItemStack> stacks = emptyStacks();

      for (int i = 0; i < Math.min(9, source.size()); i++) {
         stacks.set(i, source.get(i).copy());
      }

      return stacks;
   }

   private static CompoundTag getOrdersTag(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("orders", 10) ? tag.getCompound("orders") : null;
   }

   public static record TradeEntry(List<ItemStack> inputs, List<ItemStack> outputs) {
      private static TradeOrderData.TradeEntry empty() {
         return new TradeOrderData.TradeEntry(TradeOrderData.emptyStacks(), TradeOrderData.emptyStacks());
      }

      public boolean isValid() {
         return this.inputs.stream().anyMatch(stack -> !stack.isEmpty()) && this.outputs.stream().anyMatch(stack -> !stack.isEmpty());
      }
   }
}
