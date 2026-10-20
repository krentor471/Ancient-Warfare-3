package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CombatOrderData {
   public static final int MAX_ENTRIES = 32;
   private static final String ORDERS_TAG = "orders";
   private static final String ENTRIES_TAG = "pointList";
   private static final String DIMENSION_TAG = "dim";

   private CombatOrderData() {
   }

   public static List<BlockPos> getEntries(ItemStack stack) {
      List<BlockPos> entries = new ArrayList<>();
      CompoundTag orders = getOrdersTag(stack);
      if (orders == null) {
         return entries;
      } else {
         ListTag list = orders.m_128437_("pointList", 4);

         for (int i = 0; i < list.size(); i++) {
            entries.add(BlockPos.m_122022_(((LongTag)list.get(i)).m_7046_()));
         }

         return entries;
      }
   }

   public static boolean addEntry(ItemStack stack, Level level, BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         String dimension = level == null ? "" : level.m_46472_().m_135782_().toString();
         CompoundTag orders = getOrdersTag(stack);
         if (orders != null && !dimension.equals(orders.m_128461_("dim"))) {
            clear(stack);
         }

         List<BlockPos> entries = getEntries(stack);
         if (entries.size() >= 32) {
            return false;
         } else {
            entries.add(pos.m_7949_());
            setEntries(stack, dimension, entries);
            return true;
         }
      }
   }

   public static boolean removeEntry(ItemStack stack, int index) {
      List<BlockPos> entries = getEntries(stack);
      if (index >= 0 && index < entries.size()) {
         entries.remove(index);
         setEntries(stack, getDimension(stack), entries);
         return true;
      } else {
         return false;
      }
   }

   public static void clear(ItemStack stack) {
      stack.m_41784_().m_128473_("orders");
   }

   public static String getDimension(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      return orders == null ? "" : orders.m_128461_("dim");
   }

   public static boolean matchesDimension(ItemStack stack, Level level) {
      String dimension = getDimension(stack);
      return dimension.isEmpty() || level != null && dimension.equals(level.m_46472_().m_135782_().toString());
   }

   private static void setEntries(ItemStack stack, String dimension, List<BlockPos> entries) {
      CompoundTag orders = new CompoundTag();
      orders.m_128359_("dim", dimension == null ? "" : dimension);
      ListTag list = new ListTag();
      entries.stream().limit(32L).forEach(pos -> list.add(LongTag.m_128882_(pos.m_121878_())));
      orders.m_128365_("pointList", list);
      stack.m_41784_().m_128365_("orders", orders);
   }

   private static CompoundTag getOrdersTag(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128425_("orders", 10) ? tag.m_128469_("orders") : null;
   }
}
