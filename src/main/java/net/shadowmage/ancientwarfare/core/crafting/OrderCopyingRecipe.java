package net.shadowmage.ancientwarfare.core.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class OrderCopyingRecipe extends CustomRecipe {
   public OrderCopyingRecipe(ResourceLocation id, CraftingBookCategory category) {
      super(id, category);
   }

   public boolean matches(CraftingContainer container, Level level) {
      return getOrderPair(container) != null;
   }

   public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
      OrderCopyingRecipe.OrderPair pair = getOrderPair(container);
      if (pair == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack result = pair.blank().copy();
         result.setCount(2);
         result.setTag(pair.template().getTag() == null ? null : pair.template().getTag().copy());
         return result;
      }
   }

   public boolean canCraftInDimensions(int width, int height) {
      return width * height >= 2;
   }

   public RecipeSerializer<?> getSerializer() {
      return AWCoreRegistryAccess.recipeSerializer(AWCoreIds.id("order_copying"));
   }

   private static OrderCopyingRecipe.OrderPair getOrderPair(CraftingContainer container) {
      ItemStack first = ItemStack.EMPTY;
      ItemStack second = ItemStack.EMPTY;

      for (int i = 0; i < container.getContainerSize(); i++) {
         ItemStack stack = container.getItem(i);
         if (!stack.isEmpty()) {
            if (first.isEmpty()) {
               first = stack;
            } else {
               if (!second.isEmpty()) {
                  return null;
               }

               second = stack;
            }
         }
      }

      if (first.isEmpty() || second.isEmpty() || first.getItem() != second.getItem() || !isCopyableOrder(first)) {
         return null;
      } else if (first.hasTag() == second.hasTag()) {
         return null;
      } else {
         return first.hasTag() ? new OrderCopyingRecipe.OrderPair(first, second) : new OrderCopyingRecipe.OrderPair(second, first);
      }
   }

   private static boolean isCopyableOrder(ItemStack stack) {
      return AWCoreRegistryAccess.is(stack, AWCoreIds.WORK_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.ROUTING_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.UPKEEP_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.TRADE_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.COMBAT_ORDER);
   }

   private static record OrderPair(ItemStack template, ItemStack blank) {
   }
}
