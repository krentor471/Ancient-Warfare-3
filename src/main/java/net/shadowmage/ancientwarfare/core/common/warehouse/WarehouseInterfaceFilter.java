package net.shadowmage.ancientwarfare.core.common.warehouse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class WarehouseInterfaceFilter {
   private static final String ITEM_TAG = "Item";
   private static final String QUANTITY_TAG = "Quantity";
   private ItemStack item = ItemStack.f_41583_;
   private int quantity;

   public ItemStack item() {
      return this.item;
   }

   public int quantity() {
      return this.quantity;
   }

   public void set(ItemStack stack, int quantity) {
      this.item = stack.m_41619_() ? ItemStack.f_41583_ : stack.m_255036_(1);
      this.quantity = Math.max(0, quantity);
   }

   public void adjustQuantity(int delta) {
      this.quantity = (int)Math.max(0L, Math.min(2147483647L, (long)this.quantity + (long)delta));
   }

   public boolean matches(ItemStack stack) {
      return !this.item.m_41619_() && !stack.m_41619_() && ItemStack.m_150942_(this.item, stack);
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.m_128405_("Quantity", this.quantity);
      if (!this.item.m_41619_()) {
         tag.m_128365_("Item", this.item.m_41739_(new CompoundTag()));
      }

      return tag;
   }

   public void load(CompoundTag tag) {
      this.quantity = Math.max(0, tag.m_128451_("Quantity"));
      this.item = tag.m_128441_("Item") ? ItemStack.m_41712_(tag.m_128469_("Item")) : ItemStack.f_41583_;
      if (!this.item.m_41619_()) {
         this.item.m_41764_(1);
      }
   }
}
