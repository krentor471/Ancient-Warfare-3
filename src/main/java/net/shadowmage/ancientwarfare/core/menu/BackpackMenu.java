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
      this.backpackStack = playerInventory.f_35978_.m_21120_(hand);
      this.backpackSlots = BackpackItem.getSlotCount(this.backpackStack);
      this.blockedHotbarSlot = hand == InteractionHand.MAIN_HAND ? playerInventory.f_35977_ : -1;
      this.backpackInventory = new SimpleContainer(this.backpackSlots) {
         public boolean m_7013_(int slot, ItemStack stack) {
            return !AWCoreRegistryAccess.is(stack, AWCoreIds.BACKPACK);
         }

         public void m_6596_() {
            super.m_6596_();
            BackpackMenu.this.saveBackpackItems();
         }
      };
      this.loadBackpackItems();

      for (int i = 0; i < this.backpackSlots; i++) {
         this.m_38897_(new BackpackMenu.BackpackSlot(this.backpackInventory, i, 8 + i % 9 * 18, 8 + i / 9 * 18));
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
            this.m_38897_(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         if (col == this.blockedHotbarSlot) {
            this.m_38897_(new BackpackMenu.LockedSlot(playerInventory, col, 8 + col * 18, top + 58));
         } else {
            this.m_38897_(new Slot(playerInventory, col, 8 + col * 18, top + 58));
         }
      }
   }

   private void loadBackpackItems() {
      CompoundTag tag = this.backpackStack.m_41784_();
      if (tag.m_128441_("backpackItems")) {
         CompoundTag itemsTag = tag.m_128469_("backpackItems");

         for (int i = 0; i < this.backpackSlots; i++) {
            if (itemsTag.m_128441_("slot" + i)) {
               this.backpackInventory.m_6836_(i, ItemStack.m_41712_(itemsTag.m_128469_("slot" + i)));
            }
         }
      }
   }

   private void saveBackpackItems() {
      CompoundTag itemsTag = new CompoundTag();

      for (int i = 0; i < this.backpackSlots; i++) {
         ItemStack stack = this.backpackInventory.m_8020_(i);
         if (!stack.m_41619_()) {
            itemsTag.m_128365_("slot" + i, stack.m_41739_(new CompoundTag()));
         }
      }

      this.backpackStack.m_41784_().m_128365_("backpackItems", itemsTag);
   }

   public boolean m_6875_(Player player) {
      return player.m_21120_(this.hand) == this.backpackStack && AWCoreRegistryAccess.is(this.backpackStack, AWCoreIds.BACKPACK);
   }

   public ItemStack m_7648_(Player player, int index) {
      ItemStack result = ItemStack.f_41583_;
      Slot slot = (Slot)this.f_38839_.get(index);
      if (slot != null && slot.m_6657_()) {
         ItemStack stack = slot.m_7993_();
         result = stack.m_41777_();
         if (index < this.backpackSlots) {
            if (!this.m_38903_(stack, this.backpackSlots, this.f_38839_.size(), true)) {
               return ItemStack.f_41583_;
            }
         } else {
            if (AWCoreRegistryAccess.is(stack, AWCoreIds.BACKPACK)) {
               return ItemStack.f_41583_;
            }

            if (!this.m_38903_(stack, 0, this.backpackSlots, false)) {
               return ItemStack.f_41583_;
            }
         }

         if (stack.m_41619_()) {
            slot.m_5852_(ItemStack.f_41583_);
         } else {
            slot.m_6654_();
         }
      }

      return result;
   }

   public void m_6877_(Player player) {
      this.saveBackpackItems();
      super.m_6877_(player);
   }

   private static class BackpackSlot extends Slot {
      private BackpackSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public boolean m_5857_(ItemStack stack) {
         return !AWCoreRegistryAccess.is(stack, AWCoreIds.BACKPACK);
      }
   }

   private static class LockedSlot extends Slot {
      private LockedSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public boolean m_8010_(Player player) {
         return false;
      }

      public boolean m_5857_(ItemStack stack) {
         return false;
      }
   }
}
