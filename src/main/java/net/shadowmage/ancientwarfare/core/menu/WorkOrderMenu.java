package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.WorkOrderData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class WorkOrderMenu extends AbstractContainerMenu {
   public static final int BUTTON_TOGGLE_PRIORITY = 0;
   public static final int BUTTON_TOGGLE_SHIFT = 1;
   private static final int ENTRY_BUTTON_BASE = 10;
   private static final int ENTRY_BUTTON_STRIDE = 3;
   private final Player player;
   private final InteractionHand hand;

   public WorkOrderMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.WORK_ORDER_MENU), containerId);
      this.player = playerInventory.player;
      this.hand = hand;
      this.addPlayerInventorySlots(playerInventory, 47, 156);
   }

   public ItemStack getWorkOrderStack() {
      return this.player.getItemInHand(this.hand);
   }

   public List<WorkOrderData.WorkEntry> getEntries() {
      return WorkOrderData.getWorkEntries(this.getWorkOrderStack());
   }

   public WorkOrderData.PriorityType getPriorityType() {
      return WorkOrderData.getPriorityType(this.getWorkOrderStack());
   }

   public boolean isNightShift() {
      return WorkOrderData.isNightShift(this.getWorkOrderStack());
   }

   public boolean clickMenuButton(Player player, int id) {
      ItemStack stack = this.getWorkOrderStack();
      if (!AWCoreRegistryAccess.is(stack, AWCoreIds.WORK_ORDER)) {
         return false;
      } else if (id == 0) {
         WorkOrderData.togglePriority(stack);
         return true;
      } else if (id == 1) {
         WorkOrderData.toggleShift(stack);
         return true;
      } else {
         if (id >= 10) {
            int offset = id - 10;
            int index = offset / 3;
            int action = offset % 3;
            if (action == 0) {
               WorkOrderData.incrementWorkLength(stack, index);
               return true;
            }

            if (action == 1) {
               WorkOrderData.decrementWorkLength(stack, index);
               return true;
            }

            if (action == 2) {
               return WorkOrderData.removeEntry(stack, index);
            }
         }

         return false;
      }
   }

   public boolean stillValid(Player player) {
      return AWCoreRegistryAccess.is(player.getItemInHand(this.hand), AWCoreIds.WORK_ORDER);
   }

   public static int incrementButton(int index) {
      return 10 + index * 3;
   }

   public static int decrementButton(int index) {
      return 10 + index * 3 + 1;
   }

   public static int removeButton(int index) {
      return 10 + index * 3 + 2;
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   private void addPlayerInventorySlots(Inventory inventory, int x, int y) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(inventory, col, x + col * 18, y + 58));
      }
   }
}
