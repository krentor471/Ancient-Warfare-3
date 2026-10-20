package net.shadowmage.ancientwarfare.core.common.warehouse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class WarehouseStockFilter {
   private static final String ITEM_TAG = "Item";
   private static final String TARGET_TAG = "Target";
   private static final String EQUALITY_TAG = "Equality";
   private ItemStack item = ItemStack.f_41583_;
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
      this.item = stack.m_41619_() ? ItemStack.f_41583_ : stack.m_255036_(1);
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
      return !this.item.m_41619_() && this.equality.test(this.quantity, this.target);
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      if (!this.item.m_41619_()) {
         tag.m_128365_("Item", this.item.m_41739_(new CompoundTag()));
      }

      tag.m_128405_("Target", this.target);
      tag.m_128405_("Equality", this.equality.ordinal());
      return tag;
   }

   public void load(CompoundTag tag) {
      this.item = tag.m_128441_("Item") ? ItemStack.m_41712_(tag.m_128469_("Item")) : ItemStack.f_41583_;
      if (!this.item.m_41619_()) {
         this.item.m_41764_(1);
      }

      this.target = Math.max(0, tag.m_128451_("Target"));
      int index = tag.m_128451_("Equality");
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
