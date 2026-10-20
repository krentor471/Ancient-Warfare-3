package net.shadowmage.ancientwarfare.core.common.warehouse;

import net.minecraft.world.item.ItemStack;

public final class WarehouseItemKey {
   private final ItemStack prototype;
   private final int hash;

   public WarehouseItemKey(ItemStack stack) {
      this.prototype = stack.m_255036_(1);
      this.hash = 31 * this.prototype.m_41720_().hashCode() + (this.prototype.m_41783_() == null ? 0 : this.prototype.m_41783_().hashCode());
   }

   public ItemStack stack() {
      return this.prototype.m_41777_();
   }

   @Override
   public int hashCode() {
      return this.hash;
   }

   @Override
   public boolean equals(Object other) {
      if (other instanceof WarehouseItemKey key && ItemStack.m_150942_(this.prototype, key.prototype)) {
         return true;
      }

      return false;
   }
}
