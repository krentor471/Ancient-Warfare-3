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
         ListTag list = orders.getList("pointList", 4);

         for (int i = 0; i < list.size(); i++) {
            entries.add(BlockPos.of(((LongTag)list.get(i)).getAsLong()));
         }

         return entries;
      }
   }

   public static boolean addEntry(ItemStack stack, Level level, BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         String dimension = level == null ? "" : level.dimension().location().toString();
         CompoundTag orders = getOrdersTag(stack);
         if (orders != null && !dimension.equals(orders.getString("dim"))) {
            clear(stack);
         }

         List<BlockPos> entries = getEntries(stack);
         if (entries.size() >= 32) {
            return false;
         } else {
            entries.add(pos.immutable());
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
      stack.getOrCreateTag().remove("orders");
   }

   public static String getDimension(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      return orders == null ? "" : orders.getString("dim");
   }

   public static boolean matchesDimension(ItemStack stack, Level level) {
      String dimension = getDimension(stack);
      return dimension.isEmpty() || level != null && dimension.equals(level.dimension().location().toString());
   }

   private static void setEntries(ItemStack stack, String dimension, List<BlockPos> entries) {
      CompoundTag orders = new CompoundTag();
      orders.putString("dim", dimension == null ? "" : dimension);
      ListTag list = new ListTag();
      entries.stream().limit(32L).forEach(pos -> list.add(LongTag.valueOf(pos.asLong())));
      orders.put("pointList", list);
      stack.getOrCreateTag().put("orders", orders);
   }

   private static CompoundTag getOrdersTag(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("orders", 10) ? tag.getCompound("orders") : null;
   }
}
