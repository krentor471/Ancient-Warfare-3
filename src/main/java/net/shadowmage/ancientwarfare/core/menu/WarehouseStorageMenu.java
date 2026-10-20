package net.shadowmage.ancientwarfare.core.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseEntry;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStorageBlockEntity;

public class WarehouseStorageMenu extends AbstractContainerMenu {
   public static final int FILTER_SLOT_COUNT = 10;
   public static final int DISPLAY_COLUMNS = 9;
   public static final int DISPLAY_ROWS = 5;
   public static final int DISPLAY_SLOT_COUNT = 45;
   public static final int PLAYER_START = 55;
   public static final int CONNECTION_BUTTON = 0;
   private final SimpleContainer view = new SimpleContainer(55);
   private final long[] displayCounts = new long[45];
   private final WarehouseStorageBlockEntity storage;
   private final BlockPos pos;
   private final Level level;
   private final int[] controllerCoordinates = new int[3];
   private int connectionStatus;
   private int filterCount;
   private int entryCount;
   private int scrollOffset;
   private int scrollPixels;
   private long storedCount;
   private long capacity;
   private long lastRevision = Long.MIN_VALUE;
   private boolean pageDirty = true;
   private List<WarehouseEntry> entries = List.of();

   public WarehouseStorageMenu(int containerId, Inventory inventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_STORAGE), containerId);
      this.pos = pos.immutable();
      this.level = inventory.player.level();
      this.storage = this.level.getBlockEntity(pos) instanceof WarehouseStorageBlockEntity block ? block : null;

      for (int i = 0; i < 55; i++) {
         this.addSlot(new WarehouseStorageMenu.ViewSlot(this.view, i));
      }

      this.addPlayerInventory(inventory);
      this.addDataSlots(WarehouseDataSlots.longValue(() -> this.storedCount, value -> this.storedCount = value));
      this.addDataSlots(WarehouseDataSlots.longValue(() -> this.capacity, value -> this.capacity = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.entryCount, value -> this.entryCount = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.scrollOffset, value -> this.scrollOffset = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.scrollPixels, value -> this.scrollPixels = value));
      this.addDataSlot(new DataSlot() {
         public int get() {
            return WarehouseStorageMenu.this.filterCount;
         }

         public void set(int value) {
            WarehouseStorageMenu.this.filterCount = Math.max(0, Math.min(10, value));
         }
      });
      this.addDataSlot(new DataSlot() {
         public int get() {
            return WarehouseStorageMenu.this.connectionStatus;
         }

         public void set(int value) {
            WarehouseStorageMenu.this.connectionStatus = value;
         }
      });

      for (int i = 0; i < this.controllerCoordinates.length; i++) {
         int axis = i;
         this.addDataSlots(WarehouseDataSlots.intValue(() -> this.controllerCoordinates[axis], value -> this.controllerCoordinates[axis] = value));
      }

      for (int i = 0; i < 45; i++) {
         int entry = i;
         this.addDataSlots(WarehouseDataSlots.longValue(() -> this.displayCounts[entry], value -> this.displayCounts[entry] = value));
      }

      this.refreshView();
   }

   private void addDataSlots(DataSlot[] data) {
      for (DataSlot slot : data) {
         this.addDataSlot(slot);
      }
   }

   private void addPlayerInventory(Inventory inventory) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 156 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(inventory, col, 8 + col * 18, 214));
      }
   }

   private void refreshView() {
      if (!this.level.isClientSide && this.storage != null) {
         this.connectionStatus = this.storage.getMembership().getConnectionStatus(this.storage);
         BlockPos controller = this.storage.getMembership().getControllerPos();
         this.controllerCoordinates[0] = controller == null ? 0 : controller.getX();
         this.controllerCoordinates[1] = controller == null ? 0 : controller.getY();
         this.controllerCoordinates[2] = controller == null ? 0 : controller.getZ();
         long revision = this.storage.getInventoryRevision();
         if (revision != this.lastRevision) {
            this.storedCount = this.storage.getStoredCount();
            this.capacity = this.storage.getCapacity();
            List<ItemStack> filters = this.storage.getFilters();
            this.filterCount = filters.size();

            for (int i = 0; i < 10; i++) {
               this.view.setItem(i, i < filters.size() ? filters.get(i).copy() : ItemStack.EMPTY);
            }

            this.entries = this.storage.getEntries();
            this.entryCount = this.entries.size();
            this.scrollPixels = Math.min(this.scrollPixels, this.getMaxScrollPixels());
            this.scrollOffset = this.scrollPixels / 18 * 9;
            this.lastRevision = revision;
            this.pageDirty = true;
         }

         if (this.pageDirty) {
            for (int i = 0; i < 45; i++) {
               WarehouseEntry entry = this.scrollOffset + i < this.entries.size() ? this.entries.get(this.scrollOffset + i) : null;
               this.view.setItem(10 + i, entry == null ? ItemStack.EMPTY : entry.stack().copyWithCount(1));
               this.displayCounts[i] = entry == null ? 0L : entry.count();
            }

            this.pageDirty = false;
         }
      }
   }

   public void broadcastChanges() {
      this.refreshView();
      super.broadcastChanges();
   }

   public void scrollToRow(int row) {
      this.scrollToPixel((int)Math.max(0L, Math.min(2147483647L, (long)row * 18L)));
   }

   public void scrollToPixel(int pixel) {
      this.scrollPixels = Math.max(0, Math.min(this.getMaxScrollPixels(), pixel));
      this.scrollOffset = this.scrollPixels / 18 * 9;
      this.pageDirty = true;
      if (!this.level.isClientSide) {
         this.broadcastChanges();
      }
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public int getFilterCount() {
      return this.filterCount;
   }

   public int getEntryCount() {
      return this.entryCount;
   }

   public int getScrollOffset() {
      return this.scrollOffset;
   }

   public int getScrollPixels() {
      return this.scrollPixels;
   }

   public int getMaxScrollOffset() {
      return this.getMaxScrollPixels() / 18 * 9;
   }

   public int getMaxScrollPixels() {
      long rows = ((long)this.entryCount + 9L - 1L) / 9L;
      return (int)Math.min(2147483647L, Math.max(0L, 22L + Math.max(0L, rows - 1L) * 18L - 71L));
   }

   public long getStoredCount() {
      return Math.max(0L, this.storedCount);
   }

   public long getCapacity() {
      return Math.max(0L, this.capacity);
   }

   public ItemStack getFilterStack(int index) {
      return index >= 0 && index < 10 ? this.view.getItem(index) : ItemStack.EMPTY;
   }

   public ItemStack getDisplayStack(int index) {
      return index >= 0 && index < 45 ? this.view.getItem(10 + index) : ItemStack.EMPTY;
   }

   public long getDisplayCount(int index) {
      return index >= 0 && index < 45 ? Math.max(0L, this.displayCounts[index]) : 0L;
   }

   public Component getConnectionDescription() {
      return this.connectionStatus == 0
         ? Component.translatable("guistrings.warehouse.connection_missing")
         : Component.translatable(
            this.connectionStatus == 2 ? "guistrings.warehouse.connection_offline" : "guistrings.warehouse.connection",
            new Object[]{this.controllerCoordinates[0], this.controllerCoordinates[1], this.controllerCoordinates[2]}
         );
   }

   public boolean clickMenuButton(Player player, int button) {
      if (this.canOperate(player) && button == 0) {
         boolean changed = this.storage.getMembership().cycleController(this.storage);
         this.broadcastChanges();
         return changed;
      } else {
         return false;
      }
   }

   private boolean canOperate(Player player) {
      return !this.level.isClientSide && this.storage != null && player != null && !player.isSpectator() && player.containerMenu == this && this.stillValid(player);
   }

   public boolean stillValid(Player player) {
      return this.level.isClientSide
            || this.storage != null && !this.storage.isRemoved() && this.level.hasChunkAt(this.pos) && this.level.getBlockEntity(this.pos) == this.storage
         ? stillValid(ContainerLevelAccess.create(this.level, this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_SMALL))
            || stillValid(ContainerLevelAccess.create(this.level, this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_MEDIUM))
            || stillValid(ContainerLevelAccess.create(this.level, this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_LARGE))
         : false;
   }

   public void clicked(int slotId, int button, ClickType clickType, Player player) {
      if (this.level.isClientSide || this.canOperate(player)) {
         if (slotId >= 0 && slotId < 55) {
            if (!this.level.isClientSide) {
               this.broadcastFullState();
            }
         } else {
            super.clicked(slotId, button, clickType, player);
         }
      }
   }

   public boolean handleVirtualClick(Player player, int index, int button, ClickType type, ItemStack expectedItem, ItemStack expectedCarried) {
      if (this.canOperate(player) && index >= -1 && index < 45 && allowed(type, button)) {
         this.refreshView();
         ItemStack actual = this.getDisplayStack(index);
         if (samePrototype(actual, expectedItem) && ItemStack.matches(this.getCarried(), expectedCarried)) {
            if (type == ClickType.PICKUP) {
               if (!this.getCarried().isEmpty()) {
                  ItemStack carried = this.getCarried();
                  int requested = button == 1 ? 1 : carried.getCount();
                  ItemStack remaining = this.storage.insertStack(carried.copyWithCount(requested));
                  carried.shrink(requested - remaining.getCount());
                  this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
               } else if (!actual.isEmpty()) {
                  int available = (int)Math.min(this.getDisplayCount(index), (long)actual.getMaxStackSize());
                  this.setCarried(this.storage.extractMatching(actual, button == 1 ? (available + 1) / 2 : available));
               }
            } else if (type == ClickType.QUICK_MOVE && !actual.isEmpty()) {
               this.extractToPlayer(player, actual);
            } else if (type == ClickType.THROW && this.getCarried().isEmpty() && !actual.isEmpty()) {
               ItemStack dropped = this.storage.extractMatching(actual, button == 0 ? 1 : actual.getMaxStackSize());
               if (!dropped.isEmpty()) {
                  player.drop(dropped, true);
               }
            } else if (type == ClickType.CLONE && player.getAbilities().instabuild && this.getCarried().isEmpty() && !actual.isEmpty()) {
               this.setCarried(actual.copyWithCount(actual.getMaxStackSize()));
            }

            this.broadcastChanges();
            return true;
         } else {
            this.broadcastFullState();
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean handleFilterAction(Player player, int action, int index, int expectedCount, ItemStack expectedFilter, ItemStack expectedCarried) {
      if (this.canOperate(player) && action >= 0 && action <= 2) {
         this.refreshView();
         if (expectedCount == this.filterCount
            && ItemStack.matches(this.getCarried(), expectedCarried)
            && (action == 0 || index >= 0 && index < this.filterCount && samePrototype(this.getFilterStack(index), expectedFilter))) {
            if (action == 0) {
               if (!this.storage.addFilter(ItemStack.EMPTY)) {
                  return false;
               }
            } else if (action == 1) {
               List<ItemStack> filters = new ArrayList<>(this.storage.getFilters());
               filters.set(index, this.getCarried().isEmpty() ? ItemStack.EMPTY : this.getCarried().copyWithCount(1));
               this.storage.setFilters(filters);
            } else if (!this.storage.removeFilter(index)) {
               return false;
            }

            this.broadcastChanges();
            return true;
         } else {
            this.broadcastFullState();
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean samePrototype(ItemStack a, ItemStack b) {
      return a.isEmpty() ? b.isEmpty() : !b.isEmpty() && ItemStack.isSameItemSameTags(a, b);
   }

   private static boolean allowed(ClickType type, int button) {
      return switch (type) {
         case PICKUP, QUICK_MOVE, THROW -> button == 0 || button == 1;
         case CLONE -> button == 2;
         default -> false;
      };
   }

   private ItemStack extractToPlayer(Player player, ItemStack prototype) {
      int amount = 0;

      for (int i = 55; i < this.slots.size() && amount < prototype.getMaxStackSize(); i++) {
         Slot slot = (Slot)this.slots.get(i);
         if (slot.mayPlace(prototype)) {
            ItemStack existing = slot.getItem();
            int limit = Math.min(slot.getMaxStackSize(prototype), prototype.getMaxStackSize());
            if (existing.isEmpty()) {
               amount += limit;
            } else if (ItemStack.isSameItemSameTags(existing, prototype)) {
               amount += Math.max(0, limit - existing.getCount());
            }
         }
      }

      if (amount <= 0) {
         return ItemStack.EMPTY;
      } else {
         ItemStack extracted = this.storage.extractMatching(prototype, Math.min(amount, prototype.getMaxStackSize()));
         ItemStack remaining = extracted.copy();
         this.moveItemStackTo(remaining, 55, this.slots.size(), false);
         if (!remaining.isEmpty()) {
            ItemStack leftover = this.storage.insertStack(remaining);
            if (!leftover.isEmpty()) {
               player.drop(leftover, false);
            }
         }

         int moved = extracted.getCount() - remaining.getCount();
         return moved > 0 ? extracted.copyWithCount(moved) : ItemStack.EMPTY;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (this.canOperate(player) && index >= 55 && index < this.slots.size()) {
         Slot slot = (Slot)this.slots.get(index);
         ItemStack stack = slot.getItem();
         if (stack.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            ItemStack original = stack.copy();
            ItemStack remaining = this.storage.insertStack(original.copy());
            if (remaining.getCount() == original.getCount()) {
               return ItemStack.EMPTY;
            } else {
               slot.setByPlayer(remaining);
               slot.setChanged();
               this.broadcastChanges();
               return original;
            }
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static final class ViewSlot extends Slot {
      private ViewSlot(SimpleContainer container, int index) {
         super(container, index, 0, 0);
      }

      public boolean isActive() {
         return false;
      }

      public boolean mayPlace(ItemStack stack) {
         return false;
      }

      public boolean mayPickup(Player player) {
         return false;
      }

      public ItemStack remove(int amount) {
         return ItemStack.EMPTY;
      }
   }
}
