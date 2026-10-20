package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public final class UpkeepOrderData {
   public static final int DEFAULT_UPKEEP_AMOUNT = 6000;
   public static final int MIN_UPKEEP_AMOUNT = 200;
   public static final int MAX_UPKEEP_AMOUNT = 24000;
   public static final int UPKEEP_STEP = 200;
   private static final String ORDERS_TAG = "orders";
   private static final String POS_TAG = "upkeepPosition";
   private static final String DIMENSION_TAG = "dim";
   private static final String SIDE_TAG = "side";
   private static final String AMOUNT_TAG = "upkeepAmount";
   private static final String FOOD_TAG = "foodItem";
   private static final String FOOD_COUNT_TAG = "foodCount";

   private UpkeepOrderData() {
   }

   public static Optional<UpkeepOrderData.UpkeepEntry> getEntry(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      return orders != null && orders.contains("upkeepPosition")
         ? Optional.of(
            new UpkeepOrderData.UpkeepEntry(
               BlockPos.of(orders.getLong("upkeepPosition")),
               orders.getString("dim"),
               direction(orders.getInt("side")),
               clampAmount(orders.getInt("upkeepAmount"))
            )
         )
         : Optional.empty();
   }

   public static boolean setTarget(ItemStack stack, Level level, BlockPos pos, Direction side) {
      if (AWCoreRegistryAccess.is(stack, AWCoreIds.UPKEEP_ORDER) && level != null && pos != null) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity == null) {
            return false;
         } else {
            UpkeepOrderData.UpkeepEntry previous = getEntry(stack).orElse(null);
            setEntry(
               stack,
               new UpkeepOrderData.UpkeepEntry(
                  pos.immutable(),
                  level.dimension().location().toString(),
                  side == null ? Direction.DOWN : side,
                  previous == null ? 6000 : previous.upkeepAmount()
               )
            );
            return true;
         }
      } else {
         return false;
      }
   }

   public static void clear(ItemStack stack) {
      if (stack.hasTag()) {
         stack.getOrCreateTag().remove("orders");
      }
   }

   public static ItemStack getFoodTarget(ItemStack stack) {
      CompoundTag orders = getOrdersTag(stack);
      if (orders != null && orders.contains("foodItem", 10)) {
         ItemStack result = ItemStack.of(orders.getCompound("foodItem"));
         result.setCount(Math.max(1, Math.min(64, orders.getInt("foodCount"))));
         return result;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public static void setFoodTarget(ItemStack stack, ItemStack food) {
      CompoundTag orders = getOrdersTag(stack);
      if (orders != null) {
         if (food != null && !food.isEmpty()) {
            ItemStack one = food.copy();
            one.setCount(1);
            orders.put("foodItem", one.save(new CompoundTag()));
            orders.putInt("foodCount", Math.max(1, Math.min(64, food.getCount())));
         } else {
            orders.remove("foodItem");
            orders.remove("foodCount");
         }
      }
   }

   public static void changeFoodCount(ItemStack stack, int delta) {
      CompoundTag orders = getOrdersTag(stack);
      if (orders != null && orders.contains("foodItem", 10)) {
         int count = Math.max(1, Math.min(64, orders.getInt("foodCount") + delta));
         orders.putInt("foodCount", count);
      }
   }

   public static void cycleSide(ItemStack stack) {
      UpkeepOrderData.UpkeepEntry entry = getEntry(stack).orElse(null);
      if (entry != null) {
         Direction[] values = Direction.values();
         setEntry(
            stack, new UpkeepOrderData.UpkeepEntry(entry.pos(), entry.dimension(), values[(entry.side().ordinal() + 1) % values.length], entry.upkeepAmount())
         );
      }
   }

   public static void incrementAmount(ItemStack stack) {
      changeAmount(stack, 200);
   }

   public static void decrementAmount(ItemStack stack) {
      changeAmount(stack, -200);
   }

   private static void changeAmount(ItemStack stack, int delta) {
      UpkeepOrderData.UpkeepEntry entry = getEntry(stack).orElse(null);
      if (entry != null) {
         setEntry(stack, new UpkeepOrderData.UpkeepEntry(entry.pos(), entry.dimension(), entry.side(), clampAmount(entry.upkeepAmount() + delta)));
      }
   }

   private static void setEntry(ItemStack stack, UpkeepOrderData.UpkeepEntry entry) {
      CompoundTag orders = new CompoundTag();
      orders.putLong("upkeepPosition", entry.pos().asLong());
      orders.putString("dim", entry.dimension());
      orders.putInt("side", entry.side().ordinal());
      orders.putInt("upkeepAmount", clampAmount(entry.upkeepAmount()));
      ItemStack food = getFoodTarget(stack);
      if (!food.isEmpty()) {
         ItemStack one = food.copy();
         one.setCount(1);
         orders.put("foodItem", one.save(new CompoundTag()));
         orders.putInt("foodCount", food.getCount());
      }

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

   private static int clampAmount(int amount) {
      return amount <= 0 ? 6000 : Math.max(200, Math.min(24000, amount));
   }

   public static record UpkeepEntry(BlockPos pos, String dimension, Direction side, int upkeepAmount) {
   }
}
