package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RoutingOrderData {
   public static final int MAX_ENTRIES = 16;
   public static final int FILTER_SLOTS = 12;
   private static final String ORDERS_TAG = "orders";
   private static final String ENTRIES_TAG = "entryList";
   private static final String POS_TAG = "pos";
   private static final String DIMENSION_TAG = "dim";
   private static final String SIDE_TAG = "side";
   private static final String TYPE_TAG = "type";
   private static final String FILTER_LIST_TAG = "filterList";
   private static final String SLOT_TAG = "slot";
   private static final String IGNORE_DAMAGE_TAG = "ignoreDamage";
   private static final String IGNORE_TAG_TAG = "ignoreTag";

   private RoutingOrderData() {
   }

   public static List<RoutingOrderData.RouteEntry> getEntries(ItemStack stack) {
      List<RoutingOrderData.RouteEntry> entries = new ArrayList<>();
      CompoundTag orders = getOrdersTag(stack);
      if (orders == null) {
         return entries;
      } else {
         ListTag list = orders.getList("entryList", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            entries.add(
               new RoutingOrderData.RouteEntry(
                  BlockPos.of(entry.getLong("pos")),
                  entry.getString("dim"),
                  direction(entry.getInt("side")),
                  routeType(entry.getInt("type")),
                  readFilters(entry),
                  entry.getBoolean("ignoreDamage"),
                  entry.getBoolean("ignoreTag")
               )
            );
         }

         return entries;
      }
   }

   public static boolean addEntry(ItemStack stack, Level level, BlockPos pos, Direction side) {
      List<RoutingOrderData.RouteEntry> entries = getEntries(stack);
      if (pos != null && entries.size() < 16) {
         String dimension = level == null ? "" : level.dimension().location().toString();
         entries.add(
            new RoutingOrderData.RouteEntry(
               pos.immutable(), dimension, side == null ? Direction.DOWN : side, RoutingOrderData.RouteType.FILL_TARGET_TO, emptyFilters(), false, false
            )
         );
         setEntries(stack, entries);
         return true;
      } else {
         return false;
      }
   }

   public static boolean removeEntry(ItemStack stack, int index) {
      List<RoutingOrderData.RouteEntry> entries = getEntries(stack);
      if (index >= 0 && index < entries.size()) {
         entries.remove(index);
         setEntries(stack, entries);
         return true;
      } else {
         return false;
      }
   }

   public static void cycleSide(ItemStack stack, int index) {
      List<RoutingOrderData.RouteEntry> entries = getEntries(stack);
      if (index >= 0 && index < entries.size()) {
         RoutingOrderData.RouteEntry entry = entries.get(index);
         Direction[] values = Direction.values();
         entries.set(
            index,
            new RoutingOrderData.RouteEntry(
               entry.pos(),
               entry.dimension(),
               values[(entry.side().ordinal() + 1) % values.length],
               entry.routeType(),
               entry.filters(),
               entry.ignoreDamage(),
               entry.ignoreTag()
            )
         );
         setEntries(stack, entries);
      }
   }

   public static void cycleRouteType(ItemStack stack, int index) {
      List<RoutingOrderData.RouteEntry> entries = getEntries(stack);
      if (index >= 0 && index < entries.size()) {
         RoutingOrderData.RouteEntry entry = entries.get(index);
         RoutingOrderData.RouteType[] values = RoutingOrderData.RouteType.values();
         entries.set(
            index,
            new RoutingOrderData.RouteEntry(
               entry.pos(),
               entry.dimension(),
               entry.side(),
               values[(entry.routeType().ordinal() + 1) % values.length],
               entry.filters(),
               entry.ignoreDamage(),
               entry.ignoreTag()
            )
         );
         setEntries(stack, entries);
      }
   }

   public static void toggleIgnoreDamage(ItemStack stack, int index) {
      List<RoutingOrderData.RouteEntry> entries = getEntries(stack);
      if (index >= 0 && index < entries.size()) {
         RoutingOrderData.RouteEntry entry = entries.get(index);
         entries.set(
            index,
            new RoutingOrderData.RouteEntry(
               entry.pos(), entry.dimension(), entry.side(), entry.routeType(), entry.filters(), !entry.ignoreDamage(), entry.ignoreTag()
            )
         );
         setEntries(stack, entries);
      }
   }

   public static void toggleIgnoreTag(ItemStack stack, int index) {
      List<RoutingOrderData.RouteEntry> entries = getEntries(stack);
      if (index >= 0 && index < entries.size()) {
         RoutingOrderData.RouteEntry entry = entries.get(index);
         entries.set(
            index,
            new RoutingOrderData.RouteEntry(
               entry.pos(), entry.dimension(), entry.side(), entry.routeType(), entry.filters(), entry.ignoreDamage(), !entry.ignoreTag()
            )
         );
         setEntries(stack, entries);
      }
   }

   public static boolean moveEntryUp(ItemStack stack, int index) {
      List<RoutingOrderData.RouteEntry> entries = getEntries(stack);
      if (index > 0 && index < entries.size()) {
         RoutingOrderData.RouteEntry entry = entries.remove(index);
         entries.add(index - 1, entry);
         setEntries(stack, entries);
         return true;
      } else {
         return false;
      }
   }

   public static boolean moveEntryDown(ItemStack stack, int index) {
      List<RoutingOrderData.RouteEntry> entries = getEntries(stack);
      if (index >= 0 && index < entries.size() - 1) {
         RoutingOrderData.RouteEntry entry = entries.remove(index);
         entries.add(index + 1, entry);
         setEntries(stack, entries);
         return true;
      } else {
         return false;
      }
   }

   public static boolean setFilter(ItemStack stack, int entryIndex, int filterSlot, ItemStack value) {
      if (filterSlot >= 0 && filterSlot < 12) {
         List<RoutingOrderData.RouteEntry> entries = getEntries(stack);
         if (entryIndex >= 0 && entryIndex < entries.size()) {
            RoutingOrderData.RouteEntry entry = entries.get(entryIndex);
            List<ItemStack> filters = copyFilters(entry.filters());
            ItemStack ghost = value != null && !value.isEmpty() ? value.copy() : ItemStack.EMPTY;
            if (!ghost.isEmpty()) {
               ghost.setCount(Math.min(ghost.getCount(), ghost.getMaxStackSize()));
            }

            filters.set(filterSlot, ghost);
            entries.set(
               entryIndex,
               new RoutingOrderData.RouteEntry(
                  entry.pos(), entry.dimension(), entry.side(), entry.routeType(), filters, entry.ignoreDamage(), entry.ignoreTag()
               )
            );
            setEntries(stack, entries);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void setEntries(ItemStack stack, List<RoutingOrderData.RouteEntry> entries) {
      CompoundTag orders = new CompoundTag();
      ListTag list = new ListTag();
      entries.stream().limit(16L).forEach(routeEntry -> {
         CompoundTag entry = new CompoundTag();
         entry.putLong("pos", routeEntry.pos().asLong());
         entry.putString("dim", routeEntry.dimension());
         entry.putInt("side", routeEntry.side().ordinal());
         entry.putInt("type", routeEntry.routeType().ordinal());
         entry.putBoolean("ignoreDamage", routeEntry.ignoreDamage());
         entry.putBoolean("ignoreTag", routeEntry.ignoreTag());
         writeFilters(entry, routeEntry.filters());
         list.add(entry);
      });
      orders.put("entryList", list);
      stack.getOrCreateTag().put("orders", orders);
   }

   private static CompoundTag getOrdersTag(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("orders", 10) ? tag.getCompound("orders") : null;
   }

   private static Direction direction(int ordinal) {
      Direction[] values = Direction.values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Direction.DOWN;
   }

   private static RoutingOrderData.RouteType routeType(int ordinal) {
      RoutingOrderData.RouteType[] values = RoutingOrderData.RouteType.values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : RoutingOrderData.RouteType.FILL_TARGET_TO;
   }

   private static List<ItemStack> emptyFilters() {
      List<ItemStack> filters = new ArrayList<>();

      for (int i = 0; i < 12; i++) {
         filters.add(ItemStack.EMPTY);
      }

      return filters;
   }

   private static List<ItemStack> copyFilters(List<ItemStack> source) {
      List<ItemStack> filters = emptyFilters();

      for (int i = 0; i < Math.min(12, source.size()); i++) {
         filters.set(i, source.get(i).copy());
      }

      return filters;
   }

   private static List<ItemStack> readFilters(CompoundTag entry) {
      List<ItemStack> filters = emptyFilters();
      ListTag list = entry.getList("filterList", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag stackTag = list.getCompound(i);
         int slot = stackTag.getInt("slot");
         if (slot >= 0 && slot < 12) {
            filters.set(slot, ItemStack.of(stackTag));
         }
      }

      return filters;
   }

   private static void writeFilters(CompoundTag entry, List<ItemStack> filters) {
      ListTag list = new ListTag();

      for (int i = 0; i < Math.min(12, filters.size()); i++) {
         ItemStack filter = filters.get(i);
         if (!filter.isEmpty()) {
            CompoundTag stackTag = filter.save(new CompoundTag());
            stackTag.putInt("slot", i);
            list.add(stackTag);
         }
      }

      entry.put("filterList", list);
   }

   public static record RouteEntry(
      BlockPos pos, String dimension, Direction side, RoutingOrderData.RouteType routeType, List<ItemStack> filters, boolean ignoreDamage, boolean ignoreTag
   ) {
   }

   public static enum RouteType {
      FILL_TARGET_TO("route.fill.upto"),
      FILL_COURIER_TO("route.take.upto"),
      DEPOSIT_ALL_OF("route.deposit.match"),
      WITHDRAW_ALL_OF("route.withdraw.match"),
      DEPOSIT_ALL_EXCEPT("route.deposit.no_match"),
      WITHDRAW_ALL_EXCEPT("route.withdraw.no_match"),
      DEPOSIT_RATIO("route.deposit.ratio"),
      WITHDRAW_RATIO("route.withdraw.ratio"),
      DEPOSIT_EXACT("route.deposit.exact"),
      WITHDRAW_EXACT("route.withdraw.exact"),
      FILL_MINIMUM("route.fill.minimum"),
      TAKE_MINIMUM("route.take.minimum");

      private final String translationKey;

      private RouteType(String translationKey) {
         this.translationKey = translationKey;
      }

      public String translationKey() {
         return this.translationKey;
      }
   }
}
