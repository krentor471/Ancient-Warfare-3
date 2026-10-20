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
         ListTag list = orders.m_128437_("entryList", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.m_128728_(i);
            entries.add(new WorkOrderData.WorkEntry(BlockPos.m_122022_(entry.m_128454_("pos")), entry.m_128461_("dim"), entry.m_128451_("length")));
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
         String dimension = level == null ? "" : level.m_46472_().m_135782_().toString();
         CompoundTag existingOrders = getOrdersTag(stack);
         if (existingOrders != null && !dimension.equals(existingOrders.m_128461_("dim"))) {
            entries.clear();
         }

         entries.add(new WorkOrderData.WorkEntry(pos.m_7949_(), dimension, 0));
         setWorkEntries(stack, entries);
         return true;
      } else {
         return false;
      }
   }

   public static String getDimension(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      return orders == null ? "" : orders.m_128461_("dim");
   }

   public static boolean matchesDimension(ItemStack stack, Level level) {
      String dimension = getDimension(stack);
      return dimension.isEmpty() || level != null && dimension.equals(level.m_46472_().m_135782_().toString());
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
      if (orders != null && orders.m_128441_("priorityType")) {
         int ordinal = orders.m_128451_("priorityType");
         WorkOrderData.PriorityType[] values = WorkOrderData.PriorityType.values();
         return ordinal >= 0 && ordinal < values.length ? values[ordinal] : WorkOrderData.PriorityType.ROUTE;
      } else {
         return WorkOrderData.PriorityType.ROUTE;
      }
   }

   public static void togglePriority(ItemStack stack) {
      WorkOrderData.PriorityType[] values = WorkOrderData.PriorityType.values();
      WorkOrderData.PriorityType next = values[(getPriorityType(stack).ordinal() + 1) % values.length];
      getOrCreateOrdersTag(stack).m_128405_("priorityType", next.ordinal());
   }

   public static boolean isNightShift(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      return orders != null && orders.m_128471_("nightShift");
   }

   public static void toggleShift(ItemStack stack) {
      getOrCreateOrdersTag(stack).m_128379_("nightShift", !isNightShift(stack));
   }

   public static void clear(ItemStack stack) {
      stack.m_41784_().m_128473_("orders");
   }

   public static void setEntries(ItemStack stack, List<BlockPos> entries) {
      CompoundTag orders = new CompoundTag();
      ListTag list = new ListTag();
      entries.stream().limit(8L).forEach(pos -> {
         CompoundTag entry = new CompoundTag();
         entry.m_128356_("pos", pos.m_121878_());
         entry.m_128405_("length", 0);
         list.add(entry);
      });
      CompoundTag current = getOrdersTag(stack);
      if (current != null) {
         orders.m_128359_("dim", current.m_128461_("dim"));
         orders.m_128405_("priorityType", current.m_128451_("priorityType"));
         orders.m_128379_("nightShift", current.m_128471_("nightShift"));
      }

      orders.m_128365_("entryList", list);
      stack.m_41784_().m_128365_("orders", orders);
   }

   public static void setWorkEntries(ItemStack stack, List<WorkOrderData.WorkEntry> entries) {
      CompoundTag current = getOrdersTag(stack);
      CompoundTag orders = new CompoundTag();
      if (current != null) {
         orders.m_128405_("priorityType", current.m_128451_("priorityType"));
         orders.m_128379_("nightShift", current.m_128471_("nightShift"));
      } else {
         orders.m_128405_("priorityType", WorkOrderData.PriorityType.ROUTE.ordinal());
         orders.m_128379_("nightShift", false);
      }

      ListTag list = new ListTag();
      entries.stream().limit(8L).forEach(workEntry -> {
         CompoundTag entry = new CompoundTag();
         entry.m_128356_("pos", workEntry.pos().m_121878_());
         entry.m_128359_("dim", workEntry.dimension());
         entry.m_128405_("length", Math.max(0, workEntry.workLength()));
         list.add(entry);
      });
      orders.m_128365_("entryList", list);
      stack.m_41784_().m_128365_("orders", orders);
   }

   private static CompoundTag getOrdersTag(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128425_("orders", 10) ? tag.m_128469_("orders") : null;
   }

   private static CompoundTag getOrCreateOrdersTag(ItemStack stack) {
      CompoundTag tag = stack.m_41784_();
      if (!tag.m_128425_("orders", 10)) {
         CompoundTag orders = new CompoundTag();
         orders.m_128365_("entryList", new ListTag());
         orders.m_128405_("priorityType", WorkOrderData.PriorityType.ROUTE.ordinal());
         orders.m_128379_("nightShift", false);
         tag.m_128365_("orders", orders);
      }

      return tag.m_128469_("orders");
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
