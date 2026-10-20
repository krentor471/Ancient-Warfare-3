package net.shadowmage.ancientwarfare.core.common.warehouse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class WarehouseInterfaceFilter {
   private static final String ITEM_TAG = "Item";
   private static final String QUANTITY_TAG = "Quantity";
   private ItemStack item = ItemStack.EMPTY;
   private int quantity;

   public ItemStack item() {
      return this.item;
   }

   public int quantity() {
      return this.quantity;
   }

   public void set(ItemStack stack, int quantity) {
      this.item = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
      this.quantity = Math.max(0, quantity);
   }

   public void adjustQuantity(int delta) {
      this.quantity = (int)Math.max(0L, Math.min(2147483647L, (long)this.quantity + (long)delta));
   }

   public boolean matches(ItemStack stack) {
      return !this.item.isEmpty() && !stack.isEmpty() && ItemStack.isSameItemSameTags(this.item, stack);
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("Quantity", this.quantity);
      if (!this.item.isEmpty()) {
         tag.put("Item", this.item.save(new CompoundTag()));
      }

      return tag;
   }

   public void load(CompoundTag tag) {
      this.quantity = Math.max(0, tag.getInt("Quantity"));
      this.item = tag.contains("Item") ? ItemStack.of(tag.getCompound("Item")) : ItemStack.EMPTY;
      if (!this.item.isEmpty()) {
         this.item.setCount(1);
      }
   }
}
