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
      this(containerId, playerInventory, findCart(playerInventory.f_35978_.m_9236_(), entityId));
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
      this.storage.m_5856_(playerInventory.f_35978_);
      this.upgrades.m_5856_(playerInventory.f_35978_);
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
      return Math.max(1, (this.storage.m_6643_() + 9 - 1) / 9);
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
            this.m_38897_(new ChestPushcartMenu.ScrollingStorageSlot(this.storage, visibleIndex, 8 + col * 18, 34 + row * 18));
         }
      }
   }

   private void addUpgradeSlots() {
      for (int i = 0; i < 6; i++) {
         this.m_38897_(new ChestPushcartMenu.VehicleUpgradeSlot(this.upgrades, i, 62 + i * 18, 92));
      }
   }

   private void setTopStorageRow(int row) {
      int maxTop = Math.max(0, this.getStorageRows() - this.visibleStorageRows);
      this.topStorageRow = Math.max(0, Math.min(maxTop, row));
      this.m_38946_();
   }

   private int storageIndexForVisibleSlot(int visibleIndex) {
      int visibleRow = visibleIndex / 9;
      int col = visibleIndex % 9;
      return (this.topStorageRow + visibleRow) * 9 + col;
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

   public boolean m_6366_(Player player, int id) {
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
         return super.m_6366_(player, id);
      }
   }

   public boolean m_6875_(Player player) {
      return this.storage.m_6542_(player);
   }

   public ItemStack m_7648_(Player player, int index) {
      if (index >= 0 && index < this.f_38839_.size()) {
         Slot slot = (Slot)this.f_38839_.get(index);
         if (slot != null && slot.m_6657_()) {
            ItemStack stack = slot.m_7993_();
            ItemStack original = stack.m_41777_();
            if (index < this.visibleStorageSlots) {
               if (!this.m_38903_(stack, this.playerStart, this.f_38839_.size(), true)) {
                  return ItemStack.f_41583_;
               }
            } else if (index < this.playerStart) {
               if (!this.m_38903_(stack, this.playerStart, this.f_38839_.size(), true)) {
                  return ItemStack.f_41583_;
               }
            } else if (isVehicleUpgradeStack(stack)) {
               if (!this.m_38903_(stack, this.upgradeStart, this.playerStart, false) && !this.moveIntoStorage(stack)) {
                  return ItemStack.f_41583_;
               }
            } else if (!this.moveIntoStorage(stack)) {
               return ItemStack.f_41583_;
            }

            if (stack.m_41619_()) {
               slot.m_269060_(ItemStack.f_41583_);
            } else {
               slot.m_6654_();
            }

            if (stack.m_41613_() == original.m_41613_()) {
               return ItemStack.f_41583_;
            } else {
               slot.m_142406_(player, stack);
               this.m_38946_();
               return original;
            }
         } else {
            return ItemStack.f_41583_;
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      this.storage.m_5785_(player);
      this.upgrades.m_5785_(player);
   }

   private boolean moveIntoStorage(ItemStack stack) {
      boolean changed = false;
      if (stack.m_41753_()) {
         for (int i = 0; i < this.storage.m_6643_() && !stack.m_41619_(); i++) {
            ItemStack stored = this.storage.m_8020_(i);
            if (!stored.m_41619_() && ItemStack.m_150942_(stack, stored)) {
               int room = Math.min(stored.m_41741_(), this.storage.m_6893_()) - stored.m_41613_();
               if (room > 0) {
                  int moved = Math.min(room, stack.m_41613_());
                  stored.m_41769_(moved);
                  stack.m_41774_(moved);
                  this.storage.m_6596_();
                  changed = true;
               }
            }
         }
      }

      for (int ix = 0; ix < this.storage.m_6643_() && !stack.m_41619_(); ix++) {
         if (this.storage.m_8020_(ix).m_41619_()) {
            this.storage.m_6836_(ix, stack.m_41620_(Math.min(stack.m_41741_(), this.storage.m_6893_())));
            changed = true;
         }
      }

      return changed;
   }

   private static boolean isVehicleUpgradeStack(ItemStack stack) {
      return WorksiteUpgrade.fromStack(stack) != null || stack.m_41720_() instanceof VehicleUpgradeItem || VehicleArmorItem.fromStack(stack) != null;
   }

   private static ChestPushcartEntity findCart(Level level, int entityId) {
      return level.m_6815_(entityId) instanceof ChestPushcartEntity cart ? cart : null;
   }

   private class ScrollingStorageSlot extends Slot {
      private final int visibleIndex;

      private ScrollingStorageSlot(Container container, int visibleIndex, int x, int y) {
         super(container, visibleIndex, x, y);
         this.visibleIndex = visibleIndex;
      }

      public int m_150661_() {
         return ChestPushcartMenu.this.storageIndexForVisibleSlot(this.visibleIndex);
      }

      public ItemStack m_7993_() {
         int slot = this.m_150661_();
         return slot < ChestPushcartMenu.this.storage.m_6643_() ? ChestPushcartMenu.this.storage.m_8020_(slot) : ItemStack.f_41583_;
      }

      public boolean m_6657_() {
         return !this.m_7993_().m_41619_();
      }

      public void m_5852_(ItemStack stack) {
         if (this.m_150661_() < ChestPushcartMenu.this.storage.m_6643_()) {
            ChestPushcartMenu.this.storage.m_6836_(this.m_150661_(), stack);
         }

         this.m_6654_();
      }

      public void m_269060_(ItemStack stack) {
         this.m_5852_(stack);
      }

      public ItemStack m_6201_(int amount) {
         return this.m_150661_() < ChestPushcartMenu.this.storage.m_6643_()
            ? ChestPushcartMenu.this.storage.m_7407_(this.m_150661_(), amount)
            : ItemStack.f_41583_;
      }

      public boolean m_5857_(ItemStack stack) {
         return this.m_150661_() < ChestPushcartMenu.this.storage.m_6643_() && ChestPushcartMenu.this.storage.m_7013_(this.m_150661_(), stack);
      }

      public void m_6654_() {
         ChestPushcartMenu.this.storage.m_6596_();
      }
   }

   private static class VehicleUpgradeSlot extends Slot {
      private VehicleUpgradeSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public boolean m_5857_(ItemStack stack) {
         return ChestPushcartMenu.isVehicleUpgradeStack(stack);
      }

      public int m_6641_() {
         return 1;
      }
   }
}
