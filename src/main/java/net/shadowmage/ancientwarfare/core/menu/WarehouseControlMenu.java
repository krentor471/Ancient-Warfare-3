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
      this.pos = pos.m_7949_();
      this.level = playerInventory.f_35978_.m_9236_();
      this.warehouse = this.level.m_7702_(pos) instanceof WarehouseControlBlockEntity control ? control : null;
      this.inputContainer = (Container)(this.warehouse == null ? new SimpleContainer(9) : this.warehouse);
      this.inputContainer.m_5856_(playerInventory.f_35978_);

      for (int row = 0; row < 5; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new WarehouseControlMenu.DisplaySlot(this.displayContainer, row * 9 + col, 4 + col * 18, 42 + row * 18));
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

      this.m_38895_(new DataSlot() {
         public int m_6501_() {
            return WarehouseControlMenu.this.sortType.ordinal();
         }

         public void m_6422_(int value) {
            if (value >= 0 && value < WarehouseSortType.values().length) {
               WarehouseControlMenu.this.sortType = WarehouseSortType.values()[value];
            }
         }
      });
      this.m_38895_(new DataSlot() {
         public int m_6501_() {
            return WarehouseControlMenu.this.descending ? 1 : 0;
         }

         public void m_6422_(int value) {
            WarehouseControlMenu.this.descending = value != 0;
         }
      });
      this.refreshDisplay();
   }

   private void addDataSlots(DataSlot[] dataSlots) {
      for (DataSlot dataSlot : dataSlots) {
         this.m_38895_(dataSlot);
      }
   }

   public boolean m_6366_(Player player, int id) {
      if (this.level.f_46443_ || player != null && !player.m_5833_() && player.f_36096_ == this && this.m_6875_(player)) {
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
         } else if (id == 1 && this.warehouse != null && !this.level.f_46443_) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider(
                  (containerId, inventory, p) -> new WarehouseBoundsMenu(containerId, inventory, this.pos),
                  Component.m_237115_("container.ancientwarfare.warehouse_bounds")
               ),
               buffer -> buffer.m_130064_(this.pos)
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

   public void m_38946_() {
      this.refreshDisplay();
      super.m_38946_();
   }

   private void refreshAndBroadcastServer() {
      if (!this.level.f_46443_) {
         this.m_38946_();
      }
   }

   private void refreshDisplay() {
      if (!this.level.f_46443_ && this.warehouse != null) {
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
               this.displayContainer.m_6836_(slot, entry == null ? ItemStack.f_41583_ : entry.stack().m_255036_(1));
               this.displayCounts[slot] = entry == null ? 0L : entry.count();
            }

            this.pageDirty = false;
         }
      }
   }

   private void addPlayerInventory(Inventory playerInventory, int x, int y) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(playerInventory, col + row * 9 + 9, x + col * 18, y + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(playerInventory, col, x + col * 18, y + 58));
      }
   }

   public boolean m_6875_(Player player) {
      return (
            this.level.f_46443_
               || this.warehouse != null && !this.warehouse.m_58901_() && this.level.m_46805_(this.pos) && this.level.m_7702_(this.pos) == this.warehouse
         )
         && m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_CONTROL));
   }

   public void m_150399_(int slotId, int button, ClickType clickType, Player player) {
      if (this.level.f_46443_ || !player.m_5833_() && player.f_36096_ == this && this.m_6875_(player)) {
         if (slotId < 0 || slotId >= 45) {
            super.m_150399_(slotId, button, clickType, player);
         } else if (!this.handlingVirtualClick) {
            if (!this.level.f_46443_) {
               this.refreshDisplay();
               this.m_182423_();
            }
         } else if (!this.level.f_46443_ && this.warehouse != null) {
            ItemStack filter = this.displayContainer.m_8020_(slotId);
            if (clickType == ClickType.QUICK_MOVE) {
               this.m_7648_(player, slotId);
            } else if (clickType != ClickType.PICKUP || button != 0 && button != 1) {
               if (clickType == ClickType.THROW && this.m_142621_().m_41619_() && !filter.m_41619_() && (button == 0 || button == 1)) {
                  ItemStack extracted = this.warehouse.extractFromStorage(filter, button == 0 ? 1 : filter.m_41741_());
                  if (!extracted.m_41619_()) {
                     player.m_36176_(extracted, true);
                  }
               } else if (clickType == ClickType.CLONE && player.m_150110_().f_35937_ && this.m_142621_().m_41619_() && !filter.m_41619_()) {
                  this.m_142503_(filter.m_255036_(filter.m_41741_()));
               }
            } else if (this.m_142621_().m_41619_()) {
               if (!filter.m_41619_()) {
                  int available = (int)Math.min(this.getDisplayCount(slotId), (long)filter.m_41741_());
                  int requested = button == 1 ? (available + 1) / 2 : available;
                  if (requested > 0) {
                     this.m_142503_(this.warehouse.extractFromStorage(filter, requested));
                  }
               }
            } else {
               ItemStack carried = this.m_142621_();
               int requested = button == 1 ? 1 : carried.m_41613_();
               ItemStack remaining = this.warehouse.insertIntoStorage(carried.m_255036_(requested));
               carried.m_41774_(requested - remaining.m_41613_());
               this.m_142503_(carried.m_41619_() ? ItemStack.f_41583_ : carried);
            }

            this.m_38946_();
         }
      }
   }

   public boolean handleVirtualClick(Player player, int slotId, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
      if (!this.level.f_46443_
         && this.warehouse != null
         && !player.m_5833_()
         && player.f_36096_ == this
         && this.m_6875_(player)
         && slotId >= 0
         && slotId < 45
         && isVirtualClickAllowed(clickType, button)) {
         this.refreshDisplay();
         ItemStack actualItem = this.displayContainer.m_8020_(slotId);
         boolean sameItem = actualItem.m_41619_() ? expectedItem.m_41619_() : !expectedItem.m_41619_() && ItemStack.m_150942_(actualItem, expectedItem);
         if (sameItem && ItemStack.m_41728_(this.m_142621_(), expectedCarried)) {
            this.handlingVirtualClick = true;

            boolean var9;
            try {
               this.m_150399_(slotId, button, clickType, player);
               var9 = true;
            } finally {
               this.handlingVirtualClick = false;
            }

            return var9;
         } else {
            this.m_182423_();
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

   public ItemStack m_7648_(Player player, int index) {
      if (!this.level.f_46443_
         && this.warehouse != null
         && !player.m_5833_()
         && player.f_36096_ == this
         && this.m_6875_(player)
         && index >= 0
         && index < this.f_38839_.size()) {
         Slot slot = (Slot)this.f_38839_.get(index);
         if (!slot.m_6657_()) {
            return ItemStack.f_41583_;
         } else {
            ItemStack stack = slot.m_7993_();
            if (index < 45) {
               int amount = Math.min(stack.m_41741_(), this.playerCapacityFor(stack));
               if (amount <= 0) {
                  return ItemStack.f_41583_;
               } else {
                  ItemStack extracted = this.warehouse.extractFromStorage(stack, amount);
                  ItemStack moving = extracted.m_41777_();
                  this.m_38903_(moving, 45, this.f_38839_.size(), false);
                  int movedCount = extracted.m_41613_() - moving.m_41613_();
                  if (!moving.m_41619_()) {
                     ItemStack remainder = this.warehouse.insertIntoStorage(moving);
                     if (!remainder.m_41619_()) {
                        player.m_36176_(remainder, false);
                     }
                  }

                  this.m_38946_();
                  return movedCount == 0 ? ItemStack.f_41583_ : extracted.m_255036_(movedCount);
               }
            } else {
               ItemStack original = stack.m_41777_();
               ItemStack remaining = this.warehouse.insertIntoStorage(stack.m_41777_());
               int moved = stack.m_41613_() - remaining.m_41613_();
               if (moved <= 0) {
                  return ItemStack.f_41583_;
               } else {
                  slot.m_269060_(remaining.m_41619_() ? ItemStack.f_41583_ : remaining);
                  slot.m_6654_();
                  this.m_38946_();
                  return original;
               }
            }
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   private int playerCapacityFor(ItemStack stack) {
      int capacity = 0;

      for (int index = 45; index < this.f_38839_.size(); index++) {
         Slot slot = (Slot)this.f_38839_.get(index);
         if (slot.m_5857_(stack)) {
            ItemStack existing = slot.m_7993_();
            int limit = Math.min(stack.m_41741_(), slot.m_5866_(stack));
            if (existing.m_41619_()) {
               capacity += limit;
            } else if (ItemStack.m_150942_(existing, stack)) {
               capacity += Math.max(0, limit - existing.m_41613_());
            }

            if (capacity >= stack.m_41741_()) {
               return stack.m_41741_();
            }
         }
      }

      return capacity;
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      this.inputContainer.m_5785_(player);
   }

   private static final class DisplaySlot extends Slot {
      private DisplaySlot(Container container, int index, int x, int y) {
         super(container, index, x, y);
      }

      public boolean m_5857_(ItemStack stack) {
         return false;
      }

      public boolean m_8010_(Player player) {
         return false;
      }

      public boolean m_280329_() {
         return this.m_6657_();
      }

      public ItemStack m_6201_(int amount) {
         return ItemStack.f_41583_;
      }
   }
}
