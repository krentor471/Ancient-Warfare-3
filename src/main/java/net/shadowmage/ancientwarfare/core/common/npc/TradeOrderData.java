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
         ListTag list = orders.m_128437_("tradeList", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag trade = list.m_128728_(i);
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
            ItemStack ghost = value != null && !value.m_41619_() ? value.m_41777_() : ItemStack.f_41583_;
            if (!ghost.m_41619_()) {
               ghost.m_41764_(Math.min(ghost.m_41613_(), ghost.m_41741_()));
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
      return orders != null && orders.m_128425_("routeTarget", 4) ? Optional.of(BlockPos.m_122022_(orders.m_128454_("routeTarget"))) : Optional.empty();
   }

   public static void setRouteTarget(ItemStack stack, BlockPos target) {
      if (stack != null && !stack.m_41619_() && target != null) {
         CompoundTag root = stack.m_41784_();
         CompoundTag orders = root.m_128425_("orders", 10) ? root.m_128469_("orders").m_6426_() : new CompoundTag();
         orders.m_128356_("routeTarget", target.m_121878_());
         root.m_128365_("orders", orders);
      }
   }

   private static void setTrades(ItemStack stack, List<TradeOrderData.TradeEntry> trades) {
      CompoundTag root = stack.m_41784_();
      CompoundTag orders = root.m_128425_("orders", 10) ? root.m_128469_("orders").m_6426_() : new CompoundTag();
      ListTag list = new ListTag();
      trades.stream().limit(8L).forEach(entry -> {
         CompoundTag trade = new CompoundTag();
         writeStacks(trade, "inputs", entry.inputs());
         writeStacks(trade, "outputs", entry.outputs());
         list.add(trade);
      });
      orders.m_128365_("tradeList", list);
      root.m_128365_("orders", orders);
   }

   private static List<ItemStack> readStacks(CompoundTag trade, String listKey, String legacyKey) {
      List<ItemStack> stacks = emptyStacks();
      if (trade.m_128425_(listKey, 9)) {
         ListTag list = trade.m_128437_(listKey, 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag stackTag = list.m_128728_(i);
            int slot = stackTag.m_128451_("slot");
            if (slot >= 0 && slot < 9) {
               stacks.set(slot, ItemStack.m_41712_(stackTag));
            }
         }
      } else if (trade.m_128425_(legacyKey, 10)) {
         stacks.set(0, ItemStack.m_41712_(trade.m_128469_(legacyKey)));
      }

      return stacks;
   }

   private static void writeStacks(CompoundTag trade, String listKey, List<ItemStack> stacks) {
      ListTag list = new ListTag();

      for (int i = 0; i < Math.min(9, stacks.size()); i++) {
         ItemStack stack = stacks.get(i);
         if (!stack.m_41619_()) {
            CompoundTag stackTag = stack.m_41739_(new CompoundTag());
            stackTag.m_128405_("slot", i);
            list.add(stackTag);
         }
      }

      trade.m_128365_(listKey, list);
   }

   private static List<ItemStack> emptyStacks() {
      List<ItemStack> stacks = new ArrayList<>();

      for (int i = 0; i < 9; i++) {
         stacks.add(ItemStack.f_41583_);
      }

      return stacks;
   }

   private static List<ItemStack> copyStacks(List<ItemStack> source) {
      List<ItemStack> stacks = emptyStacks();

      for (int i = 0; i < Math.min(9, source.size()); i++) {
         stacks.set(i, source.get(i).m_41777_());
      }

      return stacks;
   }

   private static CompoundTag getOrdersTag(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128425_("orders", 10) ? tag.m_128469_("orders") : null;
   }

   public static record TradeEntry(List<ItemStack> inputs, List<ItemStack> outputs) {
      private static TradeOrderData.TradeEntry empty() {
         return new TradeOrderData.TradeEntry(TradeOrderData.emptyStacks(), TradeOrderData.emptyStacks());
      }

      public boolean isValid() {
         return this.inputs.stream().anyMatch(stack -> !stack.m_41619_()) && this.outputs.stream().anyMatch(stack -> !stack.m_41619_());
      }
   }
}
