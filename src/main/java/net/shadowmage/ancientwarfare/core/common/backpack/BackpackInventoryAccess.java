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
      CompoundTag tag = backpack.getOrCreateTag();
      if (!tag.contains("backpackItems")) {
         return inventory;
      } else {
         CompoundTag itemsTag = tag.getCompound("backpackItems");

         for (int i = 0; i < slots; i++) {
            if (itemsTag.contains("slot" + i)) {
               inventory.setItem(i, ItemStack.of(itemsTag.getCompound("slot" + i)));
            }
         }

         return inventory;
      }
   }

   public static void save(ItemStack backpack, SimpleContainer inventory) {
      CompoundTag itemsTag = new CompoundTag();

      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (!stack.isEmpty()) {
            itemsTag.put("slot" + i, stack.save(new CompoundTag()));
         }
      }

      backpack.getOrCreateTag().put("backpackItems", itemsTag);
   }
}
