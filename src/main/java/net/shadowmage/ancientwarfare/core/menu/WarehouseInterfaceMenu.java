package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseInterfaceFilter;
import net.shadowmage.ancientwarfare.core.tile.WarehouseInterfaceBlockEntity;

public class WarehouseInterfaceMenu extends AbstractContainerMenu {
   public static final int FILTER_COUNT = 9;
   public static final int QUANTITY_BUTTON_BASE = 100;
   public static final int MODE_BUTTON = 20;
   public static final int CONTROLLER_BUTTON = 21;
   private final WarehouseInterfaceBlockEntity warehouseInterface;
   private final Container inventoryContainer;
   private final BlockPos pos;
   private final Level level;
   private final ItemStack[] filterItems = new ItemStack[9];
   private final int[] filterQuantities = new int[9];
   private int filterCount;
   private int mode;
   private int connectionStatus;
   private int workStatus;
   private int controllerX;
   private int controllerY;
   private int controllerZ;
   private long lastConfigurationRevision;

   public WarehouseInterfaceMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_INTERFACE), containerId);
      this.pos = pos;
      this.level = playerInventory.player.level();
      this.warehouseInterface = this.level.getBlockEntity(pos) instanceof WarehouseInterfaceBlockEntity iface ? iface : null;
      this.inventoryContainer = (Container)(this.warehouseInterface == null ? new SimpleContainer(9) : this.warehouseInterface);
      this.refreshFilters();
      this.inventoryContainer.startOpen(playerInventory.player);
      this.addSlotGrid(this.inventoryContainer, 0, 62, 96, 3, 3);
      this.addPlayerInventory(playerInventory, 8, 158);

      for (DataSlot slot : WarehouseDataSlots.intValue(() -> this.mode, value -> this.mode = value)) {
         this.addDataSlot(slot);
      }

      for (DataSlot slot : WarehouseDataSlots.intValue(() -> this.connectionStatus, value -> this.connectionStatus = value)) {
         this.addDataSlot(slot);
      }

      for (DataSlot slot : WarehouseDataSlots.intValue(() -> this.workStatus, value -> this.workStatus = value)) {
         this.addDataSlot(slot);
      }

      for (DataSlot slot : WarehouseDataSlots.intValue(() -> this.controllerX, value -> this.controllerX = value)) {
         this.addDataSlot(slot);
      }

      for (DataSlot slot : WarehouseDataSlots.intValue(() -> this.controllerY, value -> this.controllerY = value)) {
         this.addDataSlot(slot);
      }

      for (DataSlot slot : WarehouseDataSlots.intValue(() -> this.controllerZ, value -> this.controllerZ = value)) {
         this.addDataSlot(slot);
      }
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.level.isClientSide || player != null && !player.isSpectator() && player.containerMenu == this && this.stillValid(player)) {
         if (id == 20) {
            if (this.level.isClientSide) {
               this.mode = (this.mode + 1) % WarehouseInterfaceBlockEntity.Mode.values().length;
            } else if (this.warehouseInterface != null) {
               this.warehouseInterface.cycleMode();
               this.broadcastChanges();
            }

            return true;
         } else if (id == 21) {
            if (!this.level.isClientSide && this.warehouseInterface != null) {
               this.warehouseInterface.cycleController();
               this.broadcastChanges();
            }

            return true;
         } else {
            int offset = id - 100;
            if (offset >= 0 && offset < 18) {
               int index = offset / 2;
               int delta = offset % 2 == 0 ? -1 : 1;
               if (this.level.isClientSide) {
                  this.filterQuantities[index] = (int)Math.max(0L, Math.min(2147483647L, (long)this.filterQuantities[index] + (long)delta));
                  return true;
               } else if (this.warehouseInterface != null) {
                  this.warehouseInterface.adjustFilterQuantity(index, delta);
                  this.refreshFilters();
                  this.broadcastChanges();
                  return true;
               } else {
                  return false;
               }
            } else {
               return super.clickMenuButton(player, id);
            }
         }
      } else {
         return false;
      }
   }

   public void previewQuantityButton(int id) {
      this.clickMenuButton(null, id);
   }

   public void previewFilterItem(int index, ItemStack stack) {
      if (index >= 0 && index < 9) {
         this.filterItems[index] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
         if (stack.isEmpty()) {
            this.filterQuantities[index] = 0;
         } else if (this.filterQuantities[index] <= 0) {
            this.filterQuantities[index] = 64;
         }
      }
   }

   public ItemStack getFilterItem(int index) {
      return index >= 0 && index < this.filterItems.length ? this.filterItems[index] : ItemStack.EMPTY;
   }

   public int getFilterQuantity(int index) {
      return index >= 0 && index < this.filterQuantities.length ? this.filterQuantities[index] : 0;
   }

   public int getFilterCount() {
      return this.filterCount;
   }

   public void previewSetFilter(int index, ItemStack stack, int quantity) {
      if (index >= 0 && index < this.filterCount) {
         this.filterItems[index] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
         this.filterQuantities[index] = Math.max(0, quantity);
      }
   }

   public void previewAddFilter() {
      if (this.filterCount < 9) {
         this.filterItems[this.filterCount] = ItemStack.EMPTY;
         this.filterQuantities[this.filterCount] = 64;
         this.filterCount++;
      }
   }

   public void previewRemoveFilter(int index) {
      if (index >= 0 && index < this.filterCount) {
         for (int i = index; i < this.filterCount - 1; i++) {
            this.filterItems[i] = this.filterItems[i + 1];
            this.filterQuantities[i] = this.filterQuantities[i + 1];
         }

         this.filterCount--;
         this.filterItems[this.filterCount] = ItemStack.EMPTY;
         this.filterQuantities[this.filterCount] = 0;
      }
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public WarehouseInterfaceBlockEntity.Mode getMode() {
      return this.mode >= 0 && this.mode < WarehouseInterfaceBlockEntity.Mode.values().length
         ? WarehouseInterfaceBlockEntity.Mode.values()[this.mode]
         : WarehouseInterfaceBlockEntity.Mode.STOCK;
   }

   public int getConnectionStatus() {
      return this.connectionStatus;
   }

   public int getWorkStatus() {
      return this.workStatus;
   }

   public BlockPos getControllerPos() {
      return this.connectionStatus == 0 ? null : new BlockPos(this.controllerX, this.controllerY, this.controllerZ);
   }

   public void refreshClientFilters() {
      if (this.level.isClientSide && this.warehouseInterface != null && this.lastConfigurationRevision != this.warehouseInterface.getConfigurationRevision()) {
         this.refreshFilterContents();
      }
   }

   public void broadcastChanges() {
      if (!this.level.isClientSide) {
         this.refreshFilters();
      }

      super.broadcastChanges();
   }

   private void refreshFilters() {
      this.refreshFilterContents();
      this.mode = this.warehouseInterface == null ? 0 : this.warehouseInterface.getMode().ordinal();
      this.connectionStatus = this.warehouseInterface == null ? 0 : this.warehouseInterface.getConnectionStatus();
      this.workStatus = this.warehouseInterface == null ? 0 : this.warehouseInterface.getWorkStatus();
      BlockPos controller = this.warehouseInterface == null ? null : this.warehouseInterface.getControllerPos();
      this.controllerX = controller == null ? 0 : controller.getX();
      this.controllerY = controller == null ? 0 : controller.getY();
      this.controllerZ = controller == null ? 0 : controller.getZ();
   }

   private void refreshFilterContents() {
      this.filterCount = this.warehouseInterface == null ? 0 : this.warehouseInterface.getFilterCount();
      this.lastConfigurationRevision = this.warehouseInterface == null ? 0L : this.warehouseInterface.getConfigurationRevision();

      for (int i = 0; i < 9; i++) {
         WarehouseInterfaceFilter filter = this.warehouseInterface == null ? null : this.warehouseInterface.getFilter(i);
         this.filterItems[i] = filter == null ? ItemStack.EMPTY : filter.item().copy();
         this.filterQuantities[i] = filter == null ? 0 : filter.quantity();
      }
   }

   private void addSlotGrid(Container container, int firstSlot, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            int index = firstSlot + row * columns + col;
            this.addSlot(new Slot(container, index, x + col * 18, y + row * 18));
         }
      }
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

   public boolean stillValid(Player player) {
      return this.warehouseInterface != null
         && this.level.getBlockEntity(this.pos) == this.warehouseInterface
         && stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_INTERFACE));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (index >= 0 && index < this.slots.size()) {
         ItemStack copy = ItemStack.EMPTY;
         Slot slot = (Slot)this.slots.get(index);
         if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < 9) {
               if (!this.moveItemStackTo(stack, 9, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(stack, 0, 9, false)) {
               return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
               slot.setByPlayer(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }
         }

         return copy;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public void removed(Player player) {
      super.removed(player);
      this.inventoryContainer.stopOpen(player);
   }
}
