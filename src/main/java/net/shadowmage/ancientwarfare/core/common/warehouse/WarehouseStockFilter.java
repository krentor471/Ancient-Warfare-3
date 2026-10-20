package net.shadowmage.ancientwarfare.core.common.warehouse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class WarehouseStockFilter {
   private static final String ITEM_TAG = "Item";
   private static final String TARGET_TAG = "Target";
   private static final String EQUALITY_TAG = "Equality";
   private ItemStack item = ItemStack.EMPTY;
   private long quantity;
   private int target;
   private WarehouseStockFilter.Equality equality = WarehouseStockFilter.Equality.EQUAL;

   public ItemStack item() {
      return this.item;
   }

   public long quantity() {
      return this.quantity;
   }

   public int target() {
      return this.target;
   }

   public WarehouseStockFilter.Equality equality() {
      return this.equality;
   }

   public void setItem(ItemStack stack) {
      this.item = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
   }

   public void setQuantity(long quantity) {
      this.quantity = Math.max(0L, quantity);
   }

   public void adjustTarget(int delta) {
      this.target = (int)Math.max(0L, Math.min(2147483647L, (long)this.target + (long)delta));
   }

   public void cycleEquality() {
      this.equality = this.equality.next();
   }

   public boolean passes() {
      return !this.item.isEmpty() && this.equality.test(this.quantity, this.target);
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      if (!this.item.isEmpty()) {
         tag.put("Item", this.item.save(new CompoundTag()));
      }

      tag.putInt("Target", this.target);
      tag.putInt("Equality", this.equality.ordinal());
      return tag;
   }

   public void load(CompoundTag tag) {
      this.item = tag.contains("Item") ? ItemStack.of(tag.getCompound("Item")) : ItemStack.EMPTY;
      if (!this.item.isEmpty()) {
         this.item.setCount(1);
      }

      this.target = Math.max(0, tag.getInt("Target"));
      int index = tag.getInt("Equality");
      this.equality = index >= 0 && index < WarehouseStockFilter.Equality.values().length
         ? WarehouseStockFilter.Equality.values()[index]
         : WarehouseStockFilter.Equality.EQUAL;
   }

   public static enum Equality {
      EQUAL("=") {
         @Override
         boolean test(long quantity, int target) {
            return quantity == (long)target;
         }
      },
      GREATER(">") {
         @Override
         boolean test(long quantity, int target) {
            return quantity > (long)target;
         }
      },
      LESS("<") {
         @Override
         boolean test(long quantity, int target) {
            return quantity < (long)target;
         }
      },
      GREATER_EQUAL(">=") {
         @Override
         boolean test(long quantity, int target) {
            return quantity >= (long)target;
         }
      },
      LESS_EQUAL("<=") {
         @Override
         boolean test(long quantity, int target) {
            return quantity <= (long)target;
         }
      };

      private final String label;

      private Equality(String label) {
         this.label = label;
      }

      public String label() {
         return this.label;
      }

      public WarehouseStockFilter.Equality next() {
         return values()[(this.ordinal() + 1) % values().length];
      }

      abstract boolean test(long var1, int var3);
   }
}
