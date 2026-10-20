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
         return ItemStack.f_41583_;
      } else {
         ItemStack result = pair.blank().m_41777_();
         result.m_41764_(2);
         result.m_41751_(pair.template().m_41783_() == null ? null : pair.template().m_41783_().m_6426_());
         return result;
      }
   }

   public boolean m_8004_(int width, int height) {
      return width * height >= 2;
   }

   public RecipeSerializer<?> m_7707_() {
      return AWCoreRegistryAccess.recipeSerializer(AWCoreIds.id("order_copying"));
   }

   private static OrderCopyingRecipe.OrderPair getOrderPair(CraftingContainer container) {
      ItemStack first = ItemStack.f_41583_;
      ItemStack second = ItemStack.f_41583_;

      for (int i = 0; i < container.m_6643_(); i++) {
         ItemStack stack = container.m_8020_(i);
         if (!stack.m_41619_()) {
            if (first.m_41619_()) {
               first = stack;
            } else {
               if (!second.m_41619_()) {
                  return null;
               }

               second = stack;
            }
         }
      }

      if (first.m_41619_() || second.m_41619_() || first.m_41720_() != second.m_41720_() || !isCopyableOrder(first)) {
         return null;
      } else if (first.m_41782_() == second.m_41782_()) {
         return null;
      } else {
         return first.m_41782_() ? new OrderCopyingRecipe.OrderPair(first, second) : new OrderCopyingRecipe.OrderPair(second, first);
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
