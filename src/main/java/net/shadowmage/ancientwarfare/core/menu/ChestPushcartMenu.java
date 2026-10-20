package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgrade;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;
import net.shadowmage.ancientwarfare.core.item.VehicleArmorItem;
import net.shadowmage.ancientwarfare.core.item.VehicleUpgradeItem;

public class ChestPushcartMenu extends AbstractContainerMenu {
   private static final int VISIBLE_STORAGE_ROWS = 3;
   private static final int COLUMNS = 9;
   private static final int VISIBLE_STORAGE_SLOTS = 27;
   private static final int STORAGE_X = 8;
   private static final int STORAGE_Y = 34;
   private static final int UPGRADE_X = 62;
   private static final int UPGRADE_Y = 92;
   private static final int PLAYER_X = 8;
   private static final int PLAYER_Y = 128;
   private static final int SCROLL_UP_BUTTON = 0;
   private static final int SCROLL_DOWN_BUTTON = 1;
   private static final int PACK_BUTTON = 2;
   private final Container storage;
   private final Container upgrades;
   private final ChestPushcartEntity cart;
   private final int visibleStorageRows;
   private final int visibleStorageSlots;
   private final int upgradeStart;
   private final int playerStart;
   private int topStorageRow;

   public ChestPushcartMenu(int containerId, Inventory playerInventory, int entityId) {
      this(containerId, playerInventory, findCart(playerInventory.player.level(), entityId));
   }

   public ChestPushcartMenu(int containerId, Inventory playerInventory, ChestPushcartEntity cart) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.CHEST_PUSHCART), containerId);
      this.cart = cart;
      this.storage = (Container)(cart == null ? new SimpleContainer(216) : cart);
      this.upgrades = (Container)(cart == null ? new SimpleContainer(6) : cart.upgrades());
      this.visibleStorageRows = Math.min(3, Math.max(1, this.getStorageRows()));
      this.visibleStorageSlots = this.visibleStorageRows * 9;
      this.upgradeStart = this.visibleStorageSlots;
      this.playerStart = this.upgradeStart + 6;
      this.storage.startOpen(playerInventory.player);
      this.upgrades.startOpen(playerInventory.player);
      this.addStorageSlots();
      this.addUpgradeSlots();
      this.addPlayerInventory(playerInventory, 8, 128);
   }

   public int getTopStorageRow() {
      return this.topStorageRow;
   }

   public int getVisibleStorageRows() {
      return this.visibleStorageRows;
   }

   public int getStorageRows() {
      return Math.max(1, (this.storage.getContainerSize() + 9 - 1) / 9);
   }

   public void scrollStorageRows(int amount) {
      this.setTopStorageRow(this.topStorageRow + amount);
   }

   public boolean canScrollUp() {
      return this.topStorageRow > 0;
   }

   public boolean canScrollDown() {
      return this.topStorageRow < this.getStorageRows() - this.visibleStorageRows;
   }

   public static int scrollUpButtonId() {
      return 0;
   }

   public static int scrollDownButtonId() {
      return 1;
   }

   public static int packButtonId() {
      return 2;
   }

   private void addStorageSlots() {
      for (int row = 0; row < this.visibleStorageRows; row++) {
         for (int col = 0; col < 9; col++) {
            int visibleIndex = row * 9 + col;
            this.addSlot(new ChestPushcartMenu.ScrollingStorageSlot(this.storage, visibleIndex, 8 + col * 18, 34 + row * 18));
         }
      }
   }

   private void addUpgradeSlots() {
      for (int i = 0; i < 6; i++) {
         this.addSlot(new ChestPushcartMenu.VehicleUpgradeSlot(this.upgrades, i, 62 + i * 18, 92));
      }
   }

   private void setTopStorageRow(int row) {
      int maxTop = Math.max(0, this.getStorageRows() - this.visibleStorageRows);
      this.topStorageRow = Math.max(0, Math.min(maxTop, row));
      this.broadcastChanges();
   }

   private int storageIndexForVisibleSlot(int visibleIndex) {
      int visibleRow = visibleIndex / 9;
      int col = visibleIndex % 9;
      return (this.topStorageRow + visibleRow) * 9 + col;
   }

   private void addPlayerInventory(Inventory playerInventory, int x, int y) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x + col * 18, y + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, x + col * 18, y + 58));
      }
   }

   public boolean clickMenuButton(Player player, int id) {
      if (id == 0) {
         this.scrollStorageRows(-1);
         return true;
      } else if (id == 1) {
         this.scrollStorageRows(1);
         return true;
      } else if (id == 2 && this.cart != null) {
         this.cart.packIntoItem(player);
         return true;
      } else {
         return super.clickMenuButton(player, id);
      }
   }

   public boolean stillValid(Player player) {
      return this.storage.stillValid(player);
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (index >= 0 && index < this.slots.size()) {
         Slot slot = (Slot)this.slots.get(index);
         if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index < this.visibleStorageSlots) {
               if (!this.moveItemStackTo(stack, this.playerStart, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (index < this.playerStart) {
               if (!this.moveItemStackTo(stack, this.playerStart, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (isVehicleUpgradeStack(stack)) {
               if (!this.moveItemStackTo(stack, this.upgradeStart, this.playerStart, false) && !this.moveIntoStorage(stack)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveIntoStorage(stack)) {
               return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
               slot.setByPlayer(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }

            if (stack.getCount() == original.getCount()) {
               return ItemStack.EMPTY;
            } else {
               slot.onTake(player, stack);
               this.broadcastChanges();
               return original;
            }
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public void removed(Player player) {
      super.removed(player);
      this.storage.stopOpen(player);
      this.upgrades.stopOpen(player);
   }

   private boolean moveIntoStorage(ItemStack stack) {
      boolean changed = false;
      if (stack.isStackable()) {
         for (int i = 0; i < this.storage.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack stored = this.storage.getItem(i);
            if (!stored.isEmpty() && ItemStack.isSameItemSameTags(stack, stored)) {
               int room = Math.min(stored.getMaxStackSize(), this.storage.getMaxStackSize()) - stored.getCount();
               if (room > 0) {
                  int moved = Math.min(room, stack.getCount());
                  stored.grow(moved);
                  stack.shrink(moved);
                  this.storage.setChanged();
                  changed = true;
               }
            }
         }
      }

      for (int ix = 0; ix < this.storage.getContainerSize() && !stack.isEmpty(); ix++) {
         if (this.storage.getItem(ix).isEmpty()) {
            this.storage.setItem(ix, stack.split(Math.min(stack.getMaxStackSize(), this.storage.getMaxStackSize())));
            changed = true;
         }
      }

      return changed;
   }

   private static boolean isVehicleUpgradeStack(ItemStack stack) {
      return WorksiteUpgrade.fromStack(stack) != null || stack.getItem() instanceof VehicleUpgradeItem || VehicleArmorItem.fromStack(stack) != null;
   }

   private static ChestPushcartEntity findCart(Level level, int entityId) {
      return level.getEntity(entityId) instanceof ChestPushcartEntity cart ? cart : null;
   }

   private class ScrollingStorageSlot extends Slot {
      private final int visibleIndex;

      private ScrollingStorageSlot(Container container, int visibleIndex, int x, int y) {
         super(container, visibleIndex, x, y);
         this.visibleIndex = visibleIndex;
      }

      public int getContainerSlot() {
         return ChestPushcartMenu.this.storageIndexForVisibleSlot(this.visibleIndex);
      }

      public ItemStack getItem() {
         int slot = this.getContainerSlot();
         return slot < ChestPushcartMenu.this.storage.getContainerSize() ? ChestPushcartMenu.this.storage.getItem(slot) : ItemStack.EMPTY;
      }

      public boolean hasItem() {
         return !this.getItem().isEmpty();
      }

      public void set(ItemStack stack) {
         if (this.getContainerSlot() < ChestPushcartMenu.this.storage.getContainerSize()) {
            ChestPushcartMenu.this.storage.setItem(this.getContainerSlot(), stack);
         }

         this.setChanged();
      }

      public void setByPlayer(ItemStack stack) {
         this.set(stack);
      }

      public ItemStack remove(int amount) {
         return this.getContainerSlot() < ChestPushcartMenu.this.storage.getContainerSize()
            ? ChestPushcartMenu.this.storage.removeItem(this.getContainerSlot(), amount)
            : ItemStack.EMPTY;
      }

      public boolean mayPlace(ItemStack stack) {
         return this.getContainerSlot() < ChestPushcartMenu.this.storage.getContainerSize() && ChestPushcartMenu.this.storage.canPlaceItem(this.getContainerSlot(), stack);
      }

      public void setChanged() {
         ChestPushcartMenu.this.storage.setChanged();
      }
   }

   private static class VehicleUpgradeSlot extends Slot {
      private VehicleUpgradeSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public boolean mayPlace(ItemStack stack) {
         return ChestPushcartMenu.isVehicleUpgradeStack(stack);
      }

      public int getMaxStackSize() {
         return 1;
      }
   }
}
