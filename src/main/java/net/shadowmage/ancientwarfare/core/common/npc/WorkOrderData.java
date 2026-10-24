package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class WorkOrderData {
   public static final int MAX_ENTRIES = 8;
   public static final int WORK_LENGTH_STEP = 1200;
   private static final String ORDERS_TAG = "orders";
   private static final String ENTRIES_TAG = "entryList";
   private static final String POS_TAG = "pos";
   private static final String DIMENSION_TAG = "dim";
   private static final String LENGTH_TAG = "length";
   private static final String PRIORITY_TAG = "priorityType";
   private static final String NIGHT_SHIFT_TAG = "nightShift";

   private WorkOrderData() {
   }

   public static List<BlockPos> getEntries(ItemStack stack) {
      return getWorkEntries(stack).stream().map(WorkOrderData.WorkEntry::pos).toList();
   }

   public static List<WorkOrderData.WorkEntry> getWorkEntries(ItemStack stack) {
      List<WorkOrderData.WorkEntry> entries = new ArrayList<>();
      CompoundTag orders = getOrdersTag(stack);
      if (orders == null) {
         return entries;
      } else {
         ListTag list = orders.getList("entryList", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            entries.add(new WorkOrderData.WorkEntry(BlockPos.of(entry.getLong("pos")), entry.getString("dim"), entry.getInt("length")));
         }

         return entries;
      }
   }

   public static boolean addEntry(ItemStack stack, BlockPos pos) {
      return addEntry(stack, null, pos);
   }

   public static boolean addEntry(ItemStack stack, Level level, BlockPos pos) {
      List<WorkOrderData.WorkEntry> entries = getWorkEntries(stack);
      if (pos != null && !entries.stream().anyMatch(entry -> entry.pos().equals(pos)) && entries.size() < 8) {
         String dimension = level == null ? "" : level.dimension().location().toString();
         CompoundTag existingOrders = getOrdersTag(stack);
         if (existingOrders != null && !dimension.equals(existingOrders.getString("dim"))) {
            entries.clear();
         }

         entries.add(new WorkOrderData.WorkEntry(pos.immutable(), dimension, 0));
         setWorkEntries(stack, entries);
         return true;
      } else {
         return false;
      }
   }

   public static String getDimension(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      return orders == null ? "" : orders.getString("dim");
   }

   public static boolean matchesDimension(ItemStack stack, Level level) {
      String dimension = getDimension(stack);
      return dimension.isEmpty() || level != null && dimension.equals(level.dimension().location().toString());
   }

   public static boolean removeEntry(ItemStack stack, BlockPos pos) {
      List<WorkOrderData.WorkEntry> entries = getWorkEntries(stack);
      boolean removed = entries.removeIf(entry -> entry.pos().equals(pos));
      if (removed) {
         setWorkEntries(stack, entries);
      }

      return removed;
   }

   public static boolean removeEntry(ItemStack stack, int index) {
      List<WorkOrderData.WorkEntry> entries = getWorkEntries(stack);
      if (index >= 0 && index < entries.size()) {
         entries.remove(index);
         setWorkEntries(stack, entries);
         return true;
      } else {
         return false;
      }
   }

   public static void incrementWorkLength(ItemStack stack, int index) {
      updateWorkLength(stack, index, 1200);
   }

   public static void decrementWorkLength(ItemStack stack, int index) {
      updateWorkLength(stack, index, -1200);
   }

   public static WorkOrderData.PriorityType getPriorityType(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      if (orders != null && orders.contains("priorityType")) {
         int ordinal = orders.getInt("priorityType");
         WorkOrderData.PriorityType[] values = WorkOrderData.PriorityType.values();
         return ordinal >= 0 && ordinal < values.length ? values[ordinal] : WorkOrderData.PriorityType.ROUTE;
      } else {
         return WorkOrderData.PriorityType.ROUTE;
      }
   }

   public static void togglePriority(ItemStack stack) {
      WorkOrderData.PriorityType[] values = WorkOrderData.PriorityType.values();
      WorkOrderData.PriorityType next = values[(getPriorityType(stack).ordinal() + 1) % values.length];
      getOrCreateOrdersTag(stack).putInt("priorityType", next.ordinal());
   }

   public static boolean isNightShift(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      return orders != null && orders.getBoolean("nightShift");
   }

   public static void toggleShift(ItemStack stack) {
      getOrCreateOrdersTag(stack).putBoolean("nightShift", !isNightShift(stack));
   }

   public static void clear(ItemStack stack) {
      stack.getOrCreateTag().remove("orders");
   }

   public static void setEntries(ItemStack stack, List<BlockPos> entries) {
      CompoundTag orders = new CompoundTag();
      ListTag list = new ListTag();
      entries.stream().limit(8L).forEach(pos -> {
         CompoundTag entry = new CompoundTag();
         entry.putLong("pos", pos.asLong());
         entry.putInt("length", 0);
         list.add(entry);
      });
      CompoundTag current = getOrdersTag(stack);
      if (current != null) {
         orders.putString("dim", current.getString("dim"));
         orders.putInt("priorityType", current.getInt("priorityType"));
         orders.putBoolean("nightShift", current.getBoolean("nightShift"));
      }

      orders.put("entryList", list);
      stack.getOrCreateTag().put("orders", orders);
   }

   public static void setWorkEntries(ItemStack stack, List<WorkOrderData.WorkEntry> entries) {
      CompoundTag current = getOrdersTag(stack);
      CompoundTag orders = new CompoundTag();
      if (current != null) {
         orders.putInt("priorityType", current.getInt("priorityType"));
         orders.putBoolean("nightShift", current.getBoolean("nightShift"));
      } else {
         orders.putInt("priorityType", WorkOrderData.PriorityType.ROUTE.ordinal());
         orders.putBoolean("nightShift", false);
      }

      ListTag list = new ListTag();
      entries.stream().limit(8L).forEach(workEntry -> {
         CompoundTag entry = new CompoundTag();
         entry.putLong("pos", workEntry.pos().asLong());
         entry.putString("dim", workEntry.dimension());
         entry.putInt("length", Math.max(0, workEntry.workLength()));
         list.add(entry);
      });
      orders.put("entryList", list);
      stack.getOrCreateTag().put("orders", orders);
   }

   private static CompoundTag getOrdersTag(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("orders", 10) ? tag.getCompound("orders") : null;
   }

   private static CompoundTag getOrCreateOrdersTag(ItemStack stack) {
      CompoundTag tag = stack.getOrCreateTag();
      if (!tag.contains("orders", 10)) {
         CompoundTag orders = new CompoundTag();
         orders.put("entryList", new ListTag());
         orders.putInt("priorityType", WorkOrderData.PriorityType.ROUTE.ordinal());
         orders.putBoolean("nightShift", false);
         tag.put("orders", orders);
      }

      return tag.getCompound("orders");
   }

   private static void updateWorkLength(ItemStack stack, int index, int delta) {
      List<WorkOrderData.WorkEntry> entries = getWorkEntries(stack);
      if (index >= 0 && index < entries.size()) {
         WorkOrderData.WorkEntry entry = entries.get(index);
         entries.set(index, new WorkOrderData.WorkEntry(entry.pos(), entry.dimension(), Math.max(0, entry.workLength() + delta)));
         setWorkEntries(stack, entries);
      }
   }

   public static enum PriorityType {
      SITE_NEED,
      ROUTE,
      TIMED;
   }

   public static record WorkEntry(BlockPos pos, String dimension, int workLength) {
   }
}
