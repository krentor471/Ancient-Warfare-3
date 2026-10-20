package net.shadowmage.ancientwarfare.core.common.warehouse;

import net.minecraft.world.item.ItemStack;

public record WarehouseEntry(ItemStack stack, long count) {
   public WarehouseEntry(ItemStack stack, long count) {
      stack = stack.copyWithCount(1);
      this.stack = stack;
      this.count = count;
   }
}
