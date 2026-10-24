package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseEntry;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseSortType;
import net.shadowmage.ancientwarfare.core.tile.WarehouseControlBlockEntity;

public class WarehouseControlMenu extends AbstractContainerMenu {
   public static final int DISPLAY_COLUMNS = 9;
   public static final int DISPLAY_ROWS = 5;
   public static final int DISPLAY_SLOT_COUNT = 45;
   public static final int SORT_BUTTON = 0;
   public static final int BOUNDS_BUTTON = 1;
   public static final int ORDER_BUTTON = 2;
   public static final int SCROLL_UP_BUTTON = 3;
   public static final int SCROLL_DOWN_BUTTON = 4;
   public static final int SCROLL_TO_ROW_BASE = 100;
   private static final int PLAYER_START = 45;
   private final WarehouseControlBlockEntity warehouse;
   private final Container inputContainer;
   private final SimpleContainer displayContainer = new SimpleContainer(45);
   private final long[] displayCounts = new long[45];
   private final BlockPos pos;
   private final Level level;
   private WarehouseSortType sortType = WarehouseSortType.NAME;
   private boolean descending;
   private String search = "";
   private int scrollOffset;
   private int matchedItemCount;
   private long totalItemCount;
   private long totalCapacity;
   private int occupiedSlots;
   private int totalSlots;
   private int offlineStorages;
   private long lastRevision = Long.MIN_VALUE;
   private boolean queryDirty = true;
   private boolean pageDirty = true;
   private boolean handlingVirtualClick;
   private List<WarehouseEntry> entries = List.of();

   public WarehouseControlMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_CONTROL), containerId);
      this.pos = pos.immutable();
      this.level = playerInventory.player.level();
      this.warehouse = this.level.getBlockEntity(pos) instanceof WarehouseControlBlockEntity control ? control : null;
      this.inputContainer = (Container)(this.warehouse == null ? new SimpleContainer(9) : this.warehouse);
      this.inputContainer.startOpen(playerInventory.player);

      for (int row = 0; row < 5; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new WarehouseControlMenu.DisplaySlot(this.displayContainer, row * 9 + col, 4 + col * 18, 42 + row * 18));
         }
      }

      this.addPlayerInventory(playerInventory, 8, 142);
      this.addDataSlots(WarehouseDataSlots.longValue(() -> this.totalItemCount, value -> this.totalItemCount = value));
      this.addDataSlots(WarehouseDataSlots.longValue(() -> this.totalCapacity, value -> this.totalCapacity = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.occupiedSlots, value -> this.occupiedSlots = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.totalSlots, value -> this.totalSlots = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.offlineStorages, value -> this.offlineStorages = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.matchedItemCount, value -> this.matchedItemCount = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.scrollOffset, value -> this.scrollOffset = value));

      for (int index = 0; index < 45; index++) {
         int slotIndex = index;
         this.addDataSlots(WarehouseDataSlots.longValue(() -> this.displayCounts[slotIndex], value -> this.displayCounts[slotIndex] = value));
      }

      this.addDataSlot(new DataSlot() {
         public int get() {
            return WarehouseControlMenu.this.sortType.ordinal();
         }

         public void set(int value) {
            if (value >= 0 && value < WarehouseSortType.values().length) {
               WarehouseControlMenu.this.sortType = WarehouseSortType.values()[value];
            }
         }
      });
      this.addDataSlot(new DataSlot() {
         public int get() {
            return WarehouseControlMenu.this.descending ? 1 : 0;
         }

         public void set(int value) {
            WarehouseControlMenu.this.descending = value != 0;
         }
      });
      this.refreshDisplay();
   }

   private void addDataSlots(DataSlot[] dataSlots) {
      for (DataSlot dataSlot : dataSlots) {
         this.addDataSlot(dataSlot);
      }
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.level.isClientSide || player != null && !player.isSpectator() && player.containerMenu == this && this.stillValid(player)) {
         if (id == 0) {
            this.sortType = this.sortType.next();
            this.scrollOffset = 0;
            this.queryDirty = true;
            this.refreshAndBroadcastServer();
            return true;
         } else if (id == 2) {
            this.descending = !this.descending;
            this.queryDirty = true;
            this.refreshAndBroadcastServer();
            return true;
         } else if (id == 3 || id == 4) {
            this.scrollBy(id == 3 ? -9 : 9);
            return true;
         } else if (id == 1 && this.warehouse != null && !this.level.isClientSide) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider(
                  (containerId, inventory, p) -> new WarehouseBoundsMenu(containerId, inventory, this.pos),
                  Component.translatable("container.ancientwarfare.warehouse_bounds")
               ),
               buffer -> buffer.writeBlockPos(this.pos)
            );
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void setSearch(String search) {
      String value = search == null ? "" : search;
      value = value.substring(0, Math.min(64, value.length()));
      if (!this.search.equals(value)) {
         this.search = value;
         this.scrollOffset = 0;
         this.queryDirty = true;
         this.refreshAndBroadcastServer();
      }
   }

   public String getSearch() {
      return this.search;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public void scrollBy(int amount) {
      this.scrollOffset = (int)Math.max(0L, Math.min((long)this.getMaxScrollOffset(), (long)this.scrollOffset + (long)amount));
      this.pageDirty = true;
      this.refreshAndBroadcastServer();
   }

   public void scrollToRow(int row) {
      this.scrollOffset = (int)Math.max(0L, Math.min((long)this.getMaxScrollOffset(), (long)row * 9L));
      this.pageDirty = true;
      this.refreshAndBroadcastServer();
   }

   public WarehouseSortType getSortType() {
      return this.sortType;
   }

   public boolean isDescending() {
      return this.descending;
   }

   public int getCurrentStored() {
      return this.occupiedSlots;
   }

   public int getMaxStorage() {
      return this.totalSlots;
   }

   public long getTotalItemCount() {
      return this.totalItemCount;
   }

   public long getTotalCapacity() {
      return this.totalCapacity;
   }

   public int getOfflineStorageCount() {
      return this.offlineStorages;
   }

   public long getDisplayCount(int index) {
      return index >= 0 && index < 45 ? Math.max(0L, this.displayCounts[index]) : 0L;
   }

   public int getScrollOffset() {
      return this.scrollOffset;
   }

   public int getMatchedItemCount() {
      return this.matchedItemCount;
   }

   public int getMaxScrollOffset() {
      long rows = ((long)this.matchedItemCount + 9L - 1L) / 9L;
      return (int)Math.min(2147483647L, Math.max(0L, (rows - 5L) * 9L));
   }

   public void broadcastChanges() {
      this.refreshDisplay();
      super.broadcastChanges();
   }

   private void refreshAndBroadcastServer() {
      if (!this.level.isClientSide) {
         this.broadcastChanges();
      }
   }

   private void refreshDisplay() {
      if (!this.level.isClientSide && this.warehouse != null) {
         long revision = this.warehouse.getInventoryRevision();
         if (revision != this.lastRevision) {
            this.queryDirty = true;
            this.totalItemCount = this.warehouse.getTotalItemCount();
            this.totalCapacity = this.warehouse.getTotalCapacity();
            this.occupiedSlots = this.warehouse.getOccupiedSlotCount();
            this.totalSlots = this.warehouse.getTotalSlotCount();
            this.offlineStorages = this.warehouse.getOfflineStorageCount();
            this.lastRevision = revision;
         }

         if (this.queryDirty) {
            this.entries = this.warehouse.getFilteredEntries(this.search, this.sortType, this.descending);
            this.matchedItemCount = this.entries.size();
            this.scrollOffset = Math.max(0, Math.min(this.getMaxScrollOffset(), this.scrollOffset));
            this.queryDirty = false;
            this.pageDirty = true;
         }

         if (this.pageDirty) {
            for (int slot = 0; slot < 45; slot++) {
               int index = this.scrollOffset + slot;
               WarehouseEntry entry = index < this.entries.size() ? this.entries.get(index) : null;
               this.displayContainer.setItem(slot, entry == null ? ItemStack.EMPTY : entry.stack().copyWithCount(1));
               this.displayCounts[slot] = entry == null ? 0L : entry.count();
            }

            this.pageDirty = false;
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
      return (
            this.level.isClientSide
               || this.warehouse != null && !this.warehouse.isRemoved() && this.level.hasChunkAt(this.pos) && this.level.getBlockEntity(this.pos) == this.warehouse
         )
         && stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_CONTROL));
   }

   public void clicked(int slotId, int button, ClickType clickType, Player player) {
      if (this.level.isClientSide || !player.isSpectator() && player.containerMenu == this && this.stillValid(player)) {
         if (slotId < 0 || slotId >= 45) {
            super.clicked(slotId, button, clickType, player);
         } else if (!this.handlingVirtualClick) {
            if (!this.level.isClientSide) {
               this.refreshDisplay();
               this.broadcastFullState();
            }
         } else if (!this.level.isClientSide && this.warehouse != null) {
            ItemStack filter = this.displayContainer.getItem(slotId);
            if (clickType == ClickType.QUICK_MOVE) {
               this.quickMoveStack(player, slotId);
            } else if (clickType != ClickType.PICKUP || button != 0 && button != 1) {
               if (clickType == ClickType.THROW && this.getCarried().isEmpty() && !filter.isEmpty() && (button == 0 || button == 1)) {
                  ItemStack extracted = this.warehouse.extractFromStorage(filter, button == 0 ? 1 : filter.getMaxStackSize());
                  if (!extracted.isEmpty()) {
                     player.drop(extracted, true);
                  }
               } else if (clickType == ClickType.CLONE && player.getAbilities().instabuild && this.getCarried().isEmpty() && !filter.isEmpty()) {
                  this.setCarried(filter.copyWithCount(filter.getMaxStackSize()));
               }
            } else if (this.getCarried().isEmpty()) {
               if (!filter.isEmpty()) {
                  int available = (int)Math.min(this.getDisplayCount(slotId), (long)filter.getMaxStackSize());
                  int requested = button == 1 ? (available + 1) / 2 : available;
                  if (requested > 0) {
                     this.setCarried(this.warehouse.extractFromStorage(filter, requested));
                  }
               }
            } else {
               ItemStack carried = this.getCarried();
               int requested = button == 1 ? 1 : carried.getCount();
               ItemStack remaining = this.warehouse.insertIntoStorage(carried.copyWithCount(requested));
               carried.shrink(requested - remaining.getCount());
               this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            }

            this.broadcastChanges();
         }
      }
   }

   public boolean handleVirtualClick(Player player, int slotId, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
      if (!this.level.isClientSide
         && this.warehouse != null
         && !player.isSpectator()
         && player.containerMenu == this
         && this.stillValid(player)
         && slotId >= 0
         && slotId < 45
         && isVirtualClickAllowed(clickType, button)) {
         this.refreshDisplay();
         ItemStack actualItem = this.displayContainer.getItem(slotId);
         boolean sameItem = actualItem.isEmpty() ? expectedItem.isEmpty() : !expectedItem.isEmpty() && ItemStack.isSameItemSameTags(actualItem, expectedItem);
         if (sameItem && ItemStack.matches(this.getCarried(), expectedCarried)) {
            this.handlingVirtualClick = true;

            boolean var9;
            try {
               this.clicked(slotId, button, clickType, player);
               var9 = true;
            } finally {
               this.handlingVirtualClick = false;
            }

            return var9;
         } else {
            this.broadcastFullState();
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean isVirtualClickAllowed(ClickType clickType, int button) {
      return switch (clickType) {
         case PICKUP, QUICK_MOVE, THROW -> button == 0 || button == 1;
         case CLONE -> button == 2;
         default -> false;
      };
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (!this.level.isClientSide
         && this.warehouse != null
         && !player.isSpectator()
         && player.containerMenu == this
         && this.stillValid(player)
         && index >= 0
         && index < this.slots.size()) {
         Slot slot = (Slot)this.slots.get(index);
         if (!slot.hasItem()) {
            return ItemStack.EMPTY;
         } else {
            ItemStack stack = slot.getItem();
            if (index < 45) {
               int amount = Math.min(stack.getMaxStackSize(), this.playerCapacityFor(stack));
               if (amount <= 0) {
                  return ItemStack.EMPTY;
               } else {
                  ItemStack extracted = this.warehouse.extractFromStorage(stack, amount);
                  ItemStack moving = extracted.copy();
                  this.moveItemStackTo(moving, 45, this.slots.size(), false);
                  int movedCount = extracted.getCount() - moving.getCount();
                  if (!moving.isEmpty()) {
                     ItemStack remainder = this.warehouse.insertIntoStorage(moving);
                     if (!remainder.isEmpty()) {
                        player.drop(remainder, false);
                     }
                  }

                  this.broadcastChanges();
                  return movedCount == 0 ? ItemStack.EMPTY : extracted.copyWithCount(movedCount);
               }
            } else {
               ItemStack original = stack.copy();
               ItemStack remaining = this.warehouse.insertIntoStorage(stack.copy());
               int moved = stack.getCount() - remaining.getCount();
               if (moved <= 0) {
                  return ItemStack.EMPTY;
               } else {
                  slot.setByPlayer(remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                  slot.setChanged();
                  this.broadcastChanges();
                  return original;
               }
            }
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   private int playerCapacityFor(ItemStack stack) {
      int capacity = 0;

      for (int index = 45; index < this.slots.size(); index++) {
         Slot slot = (Slot)this.slots.get(index);
         if (slot.mayPlace(stack)) {
            ItemStack existing = slot.getItem();
            int limit = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
            if (existing.isEmpty()) {
               capacity += limit;
            } else if (ItemStack.isSameItemSameTags(existing, stack)) {
               capacity += Math.max(0, limit - existing.getCount());
            }

            if (capacity >= stack.getMaxStackSize()) {
               return stack.getMaxStackSize();
            }
         }
      }

      return capacity;
   }

   public void removed(Player player) {
      super.removed(player);
      this.inputContainer.stopOpen(player);
   }

   private static final class DisplaySlot extends Slot {
      private DisplaySlot(Container container, int index, int x, int y) {
         super(container, index, x, y);
      }

      public boolean mayPlace(ItemStack stack) {
         return false;
      }

      public boolean mayPickup(Player player) {
         return false;
      }

      public boolean isHighlightable() {
         return this.hasItem();
      }

      public ItemStack remove(int amount) {
         return ItemStack.EMPTY;
      }
   }
}
