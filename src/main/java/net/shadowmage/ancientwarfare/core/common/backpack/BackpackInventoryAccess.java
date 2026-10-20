package net.shadowmage.ancientwarfare.core.common.backpack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class BackpackInventoryAccess {
   private BackpackInventoryAccess() {
   }

   public static SimpleContainer load(ItemStack backpack) {
      int slots = BackpackData.getSlotCount(backpack);
      SimpleContainer inventory = new SimpleContainer(slots);
      CompoundTag tag = backpack.m_41784_();
      if (!tag.m_128441_("backpackItems")) {
         return inventory;
      } else {
         CompoundTag itemsTag = tag.m_128469_("backpackItems");

         for (int i = 0; i < slots; i++) {
            if (itemsTag.m_128441_("slot" + i)) {
               inventory.m_6836_(i, ItemStack.m_41712_(itemsTag.m_128469_("slot" + i)));
            }
         }

         return inventory;
      }
   }

   public static void save(ItemStack backpack, SimpleContainer inventory) {
      CompoundTag itemsTag = new CompoundTag();

      for (int i = 0; i < inventory.m_6643_(); i++) {
         ItemStack stack = inventory.m_8020_(i);
         if (!stack.m_41619_()) {
            itemsTag.m_128365_("slot" + i, stack.m_41739_(new CompoundTag()));
         }
      }

      backpack.m_41784_().m_128365_("backpackItems", itemsTag);
   }
}
