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
      this.pos = pos.m_7949_();
      this.level = inventory.f_35978_.m_9236_();
      this.storage = this.level.m_7702_(pos) instanceof WarehouseStorageBlockEntity block ? block : null;

      for (int i = 0; i < 55; i++) {
         this.m_38897_(new WarehouseStorageMenu.ViewSlot(this.view, i));
      }

      this.addPlayerInventory(inventory);
      this.addDataSlots(WarehouseDataSlots.longValue(() -> this.storedCount, value -> this.storedCount = value));
      this.addDataSlots(WarehouseDataSlots.longValue(() -> this.capacity, value -> this.capacity = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.entryCount, value -> this.entryCount = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.scrollOffset, value -> this.scrollOffset = value));
      this.addDataSlots(WarehouseDataSlots.intValue(() -> this.scrollPixels, value -> this.scrollPixels = value));
      this.m_38895_(new DataSlot() {
         public int m_6501_() {
            return WarehouseStorageMenu.this.filterCount;
         }

         public void m_6422_(int value) {
            WarehouseStorageMenu.this.filterCount = Math.max(0, Math.min(10, value));
         }
      });
      this.m_38895_(new DataSlot() {
         public int m_6501_() {
            return WarehouseStorageMenu.this.connectionStatus;
         }

         public void m_6422_(int value) {
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
         this.m_38895_(slot);
      }
   }

   private void addPlayerInventory(Inventory inventory) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 156 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(inventory, col, 8 + col * 18, 214));
      }
   }

   private void refreshView() {
      if (!this.level.f_46443_ && this.storage != null) {
         this.connectionStatus = this.storage.getMembership().getConnectionStatus(this.storage);
         BlockPos controller = this.storage.getMembership().getControllerPos();
         this.controllerCoordinates[0] = controller == null ? 0 : controller.m_123341_();
         this.controllerCoordinates[1] = controller == null ? 0 : controller.m_123342_();
         this.controllerCoordinates[2] = controller == null ? 0 : controller.m_123343_();
         long revision = this.storage.getInventoryRevision();
         if (revision != this.lastRevision) {
            this.storedCount = this.storage.getStoredCount();
            this.capacity = this.storage.getCapacity();
            List<ItemStack> filters = this.storage.getFilters();
            this.filterCount = filters.size();

            for (int i = 0; i < 10; i++) {
               this.view.m_6836_(i, i < filters.size() ? filters.get(i).m_41777_() : ItemStack.f_41583_);
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
               this.view.m_6836_(10 + i, entry == null ? ItemStack.f_41583_ : entry.stack().m_255036_(1));
               this.displayCounts[i] = entry == null ? 0L : entry.count();
            }

            this.pageDirty = false;
         }
      }
   }

   public void m_38946_() {
      this.refreshView();
      super.m_38946_();
   }

   public void scrollToRow(int row) {
      this.scrollToPixel((int)Math.max(0L, Math.min(2147483647L, (long)row * 18L)));
   }

   public void scrollToPixel(int pixel) {
      this.scrollPixels = Math.max(0, Math.min(this.getMaxScrollPixels(), pixel));
      this.scrollOffset = this.scrollPixels / 18 * 9;
      this.pageDirty = true;
      if (!this.level.f_46443_) {
         this.m_38946_();
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
      return index >= 0 && index < 10 ? this.view.m_8020_(index) : ItemStack.f_41583_;
   }

   public ItemStack getDisplayStack(int index) {
      return index >= 0 && index < 45 ? this.view.m_8020_(10 + index) : ItemStack.f_41583_;
   }

   public long getDisplayCount(int index) {
      return index >= 0 && index < 45 ? Math.max(0L, this.displayCounts[index]) : 0L;
   }

   public Component getConnectionDescription() {
      return this.connectionStatus == 0
         ? Component.m_237115_("guistrings.warehouse.connection_missing")
         : Component.m_237110_(
            this.connectionStatus == 2 ? "guistrings.warehouse.connection_offline" : "guistrings.warehouse.connection",
            new Object[]{this.controllerCoordinates[0], this.controllerCoordinates[1], this.controllerCoordinates[2]}
         );
   }

   public boolean m_6366_(Player player, int button) {
      if (this.canOperate(player) && button == 0) {
         boolean changed = this.storage.getMembership().cycleController(this.storage);
         this.m_38946_();
         return changed;
      } else {
         return false;
      }
   }

   private boolean canOperate(Player player) {
      return !this.level.f_46443_ && this.storage != null && player != null && !player.m_5833_() && player.f_36096_ == this && this.m_6875_(player);
   }

   public boolean m_6875_(Player player) {
      return this.level.f_46443_
            || this.storage != null && !this.storage.m_58901_() && this.level.m_46805_(this.pos) && this.level.m_7702_(this.pos) == this.storage
         ? m_38889_(ContainerLevelAccess.m_39289_(this.level, this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_SMALL))
            || m_38889_(ContainerLevelAccess.m_39289_(this.level, this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_MEDIUM))
            || m_38889_(ContainerLevelAccess.m_39289_(this.level, this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_LARGE))
         : false;
   }

   public void m_150399_(int slotId, int button, ClickType clickType, Player player) {
      if (this.level.f_46443_ || this.canOperate(player)) {
         if (slotId >= 0 && slotId < 55) {
            if (!this.level.f_46443_) {
               this.m_182423_();
            }
         } else {
            super.m_150399_(slotId, button, clickType, player);
         }
      }
   }

   public boolean handleVirtualClick(Player player, int index, int button, ClickType type, ItemStack expectedItem, ItemStack expectedCarried) {
      if (this.canOperate(player) && index >= -1 && index < 45 && allowed(type, button)) {
         this.refreshView();
         ItemStack actual = this.getDisplayStack(index);
         if (samePrototype(actual, expectedItem) && ItemStack.m_41728_(this.m_142621_(), expectedCarried)) {
            if (type == ClickType.PICKUP) {
               if (!this.m_142621_().m_41619_()) {
                  ItemStack carried = this.m_142621_();
                  int requested = button == 1 ? 1 : carried.m_41613_();
                  ItemStack remaining = this.storage.insertStack(carried.m_255036_(requested));
                  carried.m_41774_(requested - remaining.m_41613_());
                  this.m_142503_(carried.m_41619_() ? ItemStack.f_41583_ : carried);
               } else if (!actual.m_41619_()) {
                  int available = (int)Math.min(this.getDisplayCount(index), (long)actual.m_41741_());
                  this.m_142503_(this.storage.extractMatching(actual, button == 1 ? (available + 1) / 2 : available));
               }
            } else if (type == ClickType.QUICK_MOVE && !actual.m_41619_()) {
               this.extractToPlayer(player, actual);
            } else if (type == ClickType.THROW && this.m_142621_().m_41619_() && !actual.m_41619_()) {
               ItemStack dropped = this.storage.extractMatching(actual, button == 0 ? 1 : actual.m_41741_());
               if (!dropped.m_41619_()) {
                  player.m_36176_(dropped, true);
               }
            } else if (type == ClickType.CLONE && player.m_150110_().f_35937_ && this.m_142621_().m_41619_() && !actual.m_41619_()) {
               this.m_142503_(actual.m_255036_(actual.m_41741_()));
            }

            this.m_38946_();
            return true;
         } else {
            this.m_182423_();
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
            && ItemStack.m_41728_(this.m_142621_(), expectedCarried)
            && (action == 0 || index >= 0 && index < this.filterCount && samePrototype(this.getFilterStack(index), expectedFilter))) {
            if (action == 0) {
               if (!this.storage.addFilter(ItemStack.f_41583_)) {
                  return false;
               }
            } else if (action == 1) {
               List<ItemStack> filters = new ArrayList<>(this.storage.getFilters());
               filters.set(index, this.m_142621_().m_41619_() ? ItemStack.f_41583_ : this.m_142621_().m_255036_(1));
               this.storage.setFilters(filters);
            } else if (!this.storage.removeFilter(index)) {
               return false;
            }

            this.m_38946_();
            return true;
         } else {
            this.m_182423_();
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean samePrototype(ItemStack a, ItemStack b) {
      return a.m_41619_() ? b.m_41619_() : !b.m_41619_() && ItemStack.m_150942_(a, b);
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

      for (int i = 55; i < this.f_38839_.size() && amount < prototype.m_41741_(); i++) {
         Slot slot = (Slot)this.f_38839_.get(i);
         if (slot.m_5857_(prototype)) {
            ItemStack existing = slot.m_7993_();
            int limit = Math.min(slot.m_5866_(prototype), prototype.m_41741_());
            if (existing.m_41619_()) {
               amount += limit;
            } else if (ItemStack.m_150942_(existing, prototype)) {
               amount += Math.max(0, limit - existing.m_41613_());
            }
         }
      }

      if (amount <= 0) {
         return ItemStack.f_41583_;
      } else {
         ItemStack extracted = this.storage.extractMatching(prototype, Math.min(amount, prototype.m_41741_()));
         ItemStack remaining = extracted.m_41777_();
         this.m_38903_(remaining, 55, this.f_38839_.size(), false);
         if (!remaining.m_41619_()) {
            ItemStack leftover = this.storage.insertStack(remaining);
            if (!leftover.m_41619_()) {
               player.m_36176_(leftover, false);
            }
         }

         int moved = extracted.m_41613_() - remaining.m_41613_();
         return moved > 0 ? extracted.m_255036_(moved) : ItemStack.f_41583_;
      }
   }

   public ItemStack m_7648_(Player player, int index) {
      if (this.canOperate(player) && index >= 55 && index < this.f_38839_.size()) {
         Slot slot = (Slot)this.f_38839_.get(index);
         ItemStack stack = slot.m_7993_();
         if (stack.m_41619_()) {
            return ItemStack.f_41583_;
         } else {
            ItemStack original = stack.m_41777_();
            ItemStack remaining = this.storage.insertStack(original.m_41777_());
            if (remaining.m_41613_() == original.m_41613_()) {
               return ItemStack.f_41583_;
            } else {
               slot.m_269060_(remaining);
               slot.m_6654_();
               this.m_38946_();
               return original;
            }
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   private static final class ViewSlot extends Slot {
      private ViewSlot(SimpleContainer container, int index) {
         super(container, index, 0, 0);
      }

      public boolean m_6659_() {
         return false;
      }

      public boolean m_5857_(ItemStack stack) {
         return false;
      }

      public boolean m_8010_(Player player) {
         return false;
      }

      public ItemStack m_6201_(int amount) {
         return ItemStack.f_41583_;
      }
   }
}
