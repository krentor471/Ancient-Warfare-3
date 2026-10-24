package net.shadowmage.ancientwarfare.core.common.warehouse;

import net.minecraft.world.item.ItemStack;

public final class WarehouseItemKey {
   private final ItemStack prototype;
   private final int hash;

   public WarehouseItemKey(ItemStack stack) {
      this.prototype = stack.copyWithCount(1);
      this.hash = 31 * this.prototype.getItem().hashCode() + (this.prototype.getTag() == null ? 0 : this.prototype.getTag().hashCode());
   }

   public ItemStack stack() {
      return this.prototype.copy();
   }

   @Override
   public int hashCode() {
      return this.hash;
   }

   @Override
   public boolean equals(Object other) {
      if (other instanceof WarehouseItemKey key && ItemStack.isSameItemSameTags(this.prototype, key.prototype)) {
         return true;
      }

      return false;
   }
}
