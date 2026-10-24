package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.BackpackItem;

public class BackpackMenu extends AbstractContainerMenu {
   private final InteractionHand hand;
   private final ItemStack backpackStack;
   private final SimpleContainer backpackInventory;
   private final int backpackSlots;
   private final int blockedHotbarSlot;

   public BackpackMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.BACKPACK), containerId);
      this.hand = hand;
      this.backpackStack = playerInventory.player.getItemInHand(hand);
      this.backpackSlots = BackpackItem.getSlotCount(this.backpackStack);
      this.blockedHotbarSlot = hand == InteractionHand.MAIN_HAND ? playerInventory.selected : -1;
      this.backpackInventory = new SimpleContainer(this.backpackSlots) {
         public boolean canPlaceItem(int slot, ItemStack stack) {
            return !AWCoreRegistryAccess.is(stack, AWCoreIds.BACKPACK);
         }

         public void setChanged() {
            super.setChanged();
            BackpackMenu.this.saveBackpackItems();
         }
      };
      this.loadBackpackItems();

      for (int i = 0; i < this.backpackSlots; i++) {
         this.addSlot(new BackpackMenu.BackpackSlot(this.backpackInventory, i, 8 + i % 9 * 18, 8 + i / 9 * 18));
      }

      this.addPlayerInventory(playerInventory, 8 + this.backpackSlots / 9 * 18 + 8);
   }

   public int getBlockedHotbarSlot() {
      return this.blockedHotbarSlot;
   }

   public int getBackpackRows() {
      return this.backpackSlots / 9;
   }

   private void addPlayerInventory(Inventory playerInventory, int top) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         if (col == this.blockedHotbarSlot) {
            this.addSlot(new BackpackMenu.LockedSlot(playerInventory, col, 8 + col * 18, top + 58));
         } else {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, top + 58));
         }
      }
   }

   private void loadBackpackItems() {
      CompoundTag tag = this.backpackStack.getOrCreateTag();
      if (tag.contains("backpackItems")) {
         CompoundTag itemsTag = tag.getCompound("backpackItems");

         for (int i = 0; i < this.backpackSlots; i++) {
            if (itemsTag.contains("slot" + i)) {
               this.backpackInventory.setItem(i, ItemStack.of(itemsTag.getCompound("slot" + i)));
            }
         }
      }
   }

   private void saveBackpackItems() {
      CompoundTag itemsTag = new CompoundTag();

      for (int i = 0; i < this.backpackSlots; i++) {
         ItemStack stack = this.backpackInventory.getItem(i);
         if (!stack.isEmpty()) {
            itemsTag.put("slot" + i, stack.save(new CompoundTag()));
         }
      }

      this.backpackStack.getOrCreateTag().put("backpackItems", itemsTag);
   }

   public boolean stillValid(Player player) {
      return player.getItemInHand(this.hand) == this.backpackStack && AWCoreRegistryAccess.is(this.backpackStack, AWCoreIds.BACKPACK);
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack result = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         result = stack.copy();
         if (index < this.backpackSlots) {
            if (!this.moveItemStackTo(stack, this.backpackSlots, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (AWCoreRegistryAccess.is(stack, AWCoreIds.BACKPACK)) {
               return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(stack, 0, this.backpackSlots, false)) {
               return ItemStack.EMPTY;
            }
         }

         if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return result;
   }

   public void removed(Player player) {
      this.saveBackpackItems();
      super.removed(player);
   }

   private static class BackpackSlot extends Slot {
      private BackpackSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public boolean mayPlace(ItemStack stack) {
         return !AWCoreRegistryAccess.is(stack, AWCoreIds.BACKPACK);
      }
   }

   private static class LockedSlot extends Slot {
      private LockedSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public boolean mayPickup(Player player) {
         return false;
      }

      public boolean mayPlace(ItemStack stack) {
         return false;
      }
   }
}
