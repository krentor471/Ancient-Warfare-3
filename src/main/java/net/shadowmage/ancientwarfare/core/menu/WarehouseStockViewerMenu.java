package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.core.BlockPos;
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
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseStockFilter;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStockViewerBlockEntity;

public class WarehouseStockViewerMenu extends AbstractContainerMenu {
   public static final int FILTER_COUNT = 4;
   public static final int TARGET_BUTTON_BASE = 100;
   public static final int EQUALITY_BUTTON_BASE = 20;
   public static final int MATCH_BUTTON = 30;
   public static final int CONTROLLER_BUTTON = 31;
   private final BlockPos pos;
   private final boolean linker;
   private final Level level;
   private final WarehouseStockViewerBlockEntity viewer;
   private final ItemStack[] filterItems = new ItemStack[4];
   private final long[] quantities = new long[4];
   private final int[] targets = new int[4];
   private final String[] equalities = new String[4];
   private int requireAll;
   private int connectionStatus;
   private int controllerX;
   private int controllerY;
   private int controllerZ;
   private long lastConfigurationRevision;

   public WarehouseStockViewerMenu(int containerId, Inventory playerInventory, BlockPos pos, boolean linker) {
      super(AWCoreRegistryAccess.menu(linker ? AWCoreIds.WAREHOUSE_STOCK_LINKER : AWCoreIds.WAREHOUSE_STOCK_VIEWER), containerId);
      this.pos = pos;
      this.linker = linker;
      this.level = playerInventory.player.level();
      this.viewer = this.level.getBlockEntity(pos) instanceof WarehouseStockViewerBlockEntity stockViewer ? stockViewer : null;
      this.refreshFilters();
      this.addPlayerInventory(playerInventory, linker ? 47 : 8, 88);

      for (int i = 0; i < 4; i++) {
         int index = i;
         for (DataSlot slot : WarehouseDataSlots.longValue(() -> this.quantities[index], value -> this.quantities[index] = value)) {
            this.addDataSlot(slot);
         }

         for (DataSlot slot : WarehouseDataSlots.intValue(() -> this.targets[index], value -> this.targets[index] = value)) {
            this.addDataSlot(slot);
         }
      }

      for (DataSlot slot : WarehouseDataSlots.intValue(() -> this.requireAll, value -> this.requireAll = value)) {
         this.addDataSlot(slot);
      }

      for (DataSlot slot : WarehouseDataSlots.intValue(() -> this.connectionStatus, value -> this.connectionStatus = value)) {
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
      if (this.viewer != null && (this.level.isClientSide || player != null && !player.isSpectator() && player.containerMenu == this && this.stillValid(player))) {
         if (id == 30 && this.linker) {
            if (this.level.isClientSide) {
               this.requireAll = this.requireAll == 0 ? 1 : 0;
            } else {
               this.viewer.toggleRequireAll();
               this.broadcastChanges();
            }

            return true;
         } else if (id == 31) {
            if (!this.level.isClientSide) {
               this.viewer.cycleController();
               this.broadcastChanges();
            }

            return true;
         } else if (id >= 100 && id < 108) {
            int offset = id - 100;
            if (this.level.isClientSide) {
               int index = offset / 2;
               this.targets[index] = (int)Math.max(0L, Math.min(2147483647L, (long)this.targets[index] + (long)(offset % 2 == 0 ? -1 : 1)));
            } else {
               this.viewer.adjustTarget(offset / 2, offset % 2 == 0 ? -1 : 1);
               this.broadcastChanges();
            }

            return true;
         } else if (id >= 20 && id < 24) {
            int index = id - 20;
            if (this.level.isClientSide) {
               for (WarehouseStockFilter.Equality equality : WarehouseStockFilter.Equality.values()) {
                  if (equality.label().equals(this.equalities[index])) {
                     this.equalities[index] = equality.next().label();
                     break;
                  }
               }
            } else {
               this.viewer.cycleEquality(index);
               this.broadcastChanges();
            }

            return true;
         } else {
            return super.clickMenuButton(player, id);
         }
      } else {
         return false;
      }
   }

   public void previewButton(int id) {
      this.clickMenuButton(null, id);
   }

   public void previewFilterItem(int index, ItemStack stack) {
      if (index >= 0 && index < 4) {
         this.filterItems[index] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
      }
   }

   public void broadcastChanges() {
      if (!this.level.isClientSide) {
         this.refreshFilters();
      }

      super.broadcastChanges();
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public boolean isLinker() {
      return this.linker;
   }

   public ItemStack getFilterItem(int index) {
      return index >= 0 && index < this.filterItems.length ? this.filterItems[index] : ItemStack.EMPTY;
   }

   public long getQuantity(int index) {
      return index >= 0 && index < this.quantities.length ? this.quantities[index] : 0L;
   }

   public int getTarget(int index) {
      return index >= 0 && index < this.targets.length ? this.targets[index] : 0;
   }

   public String getEquality(int index) {
      return index >= 0 && index < this.equalities.length ? this.equalities[index] : "=";
   }

   public boolean requiresAll() {
      return this.requireAll != 0;
   }

   public int getConnectionStatus() {
      return this.connectionStatus;
   }

   public BlockPos getControllerPos() {
      return this.connectionStatus == 0 ? null : new BlockPos(this.controllerX, this.controllerY, this.controllerZ);
   }

   public void refreshClientFilters() {
      if (this.level.isClientSide && this.viewer != null && this.lastConfigurationRevision != this.viewer.getConfigurationRevision()) {
         this.refreshFilterContents(false);
      }
   }

   private void refreshFilters() {
      this.requireAll = this.viewer != null && this.viewer.requiresAll() ? 1 : 0;
      this.connectionStatus = this.viewer == null ? 0 : this.viewer.getConnectionStatus();
      BlockPos controller = this.viewer == null ? null : this.viewer.getControllerPos();
      this.controllerX = controller == null ? 0 : controller.getX();
      this.controllerY = controller == null ? 0 : controller.getY();
      this.controllerZ = controller == null ? 0 : controller.getZ();
      this.refreshFilterContents(true);
   }

   private void refreshFilterContents(boolean updateQuantity) {
      this.lastConfigurationRevision = this.viewer == null ? 0L : this.viewer.getConfigurationRevision();

      for (int i = 0; i < 4; i++) {
         WarehouseStockFilter filter = this.viewer == null ? null : this.viewer.getFilter(i);
         this.filterItems[i] = filter == null ? ItemStack.EMPTY : filter.item().copy();
         if (updateQuantity) {
            this.quantities[i] = filter == null ? 0L : filter.quantity();
         }

         this.targets[i] = filter == null ? 0 : filter.target();
         this.equalities[i] = filter == null ? "=" : filter.equality().label();
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
      return this.viewer != null
         && this.level.getBlockEntity(this.pos) == this.viewer
         && stillValid(
            ContainerLevelAccess.create(player.level(), this.pos),
            player,
            AWCoreRegistryAccess.block(this.linker ? AWCoreIds.WAREHOUSE_STOCK_LINKER : AWCoreIds.WAREHOUSE_STOCK_VIEWER)
         );
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }
}
