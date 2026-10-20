package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.RoutingOrderData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class RoutingOrderMenu extends AbstractContainerMenu {
   public static final int BUTTON_SIDE_BASE = 100;
   public static final int BUTTON_TYPE_BASE = 200;
   public static final int BUTTON_REMOVE_BASE = 300;
   public static final int BUTTON_MOVE_UP_BASE = 400;
   public static final int BUTTON_MOVE_DOWN_BASE = 500;
   public static final int BUTTON_IGNORE_DAMAGE_BASE = 600;
   public static final int BUTTON_IGNORE_TAG_BASE = 700;
   public static final int BUTTON_FILTER_SLOT_BASE = 1000;
   private final Inventory playerInventory;
   private final InteractionHand hand;

   public RoutingOrderMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.ROUTING_ORDER_MENU), containerId);
      this.playerInventory = playerInventory;
      this.hand = hand;
      this.addPlayerInventorySlots(playerInventory, 47, 156);
   }

   public ItemStack getRoutingOrderStack() {
      return this.playerInventory.player.getItemInHand(this.hand);
   }

   public List<RoutingOrderData.RouteEntry> getEntries() {
      return RoutingOrderData.getEntries(this.getRoutingOrderStack());
   }

   public boolean clickMenuButton(Player player, int id) {
      ItemStack stack = this.getRoutingOrderStack();
      if (!AWCoreRegistryAccess.is(stack, AWCoreIds.ROUTING_ORDER)) {
         return false;
      } else if (id >= 1000) {
         int encoded = id - 1000;
         return RoutingOrderData.setFilter(stack, encoded / 12, encoded % 12, this.getCarried());
      } else if (id >= 700) {
         RoutingOrderData.toggleIgnoreTag(stack, id - 700);
         return true;
      } else if (id >= 600) {
         RoutingOrderData.toggleIgnoreDamage(stack, id - 600);
         return true;
      } else if (id >= 500) {
         return RoutingOrderData.moveEntryDown(stack, id - 500);
      } else if (id >= 400) {
         return RoutingOrderData.moveEntryUp(stack, id - 400);
      } else if (id >= 100 && id < 200) {
         RoutingOrderData.cycleSide(stack, id - 100);
         return true;
      } else if (id >= 200 && id < 300) {
         RoutingOrderData.cycleRouteType(stack, id - 200);
         return true;
      } else {
         return id >= 300 ? RoutingOrderData.removeEntry(stack, id - 300) : false;
      }
   }

   public boolean stillValid(Player player) {
      return player.getItemInHand(this.hand) == this.getRoutingOrderStack() && AWCoreRegistryAccess.is(this.getRoutingOrderStack(), AWCoreIds.ROUTING_ORDER);
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public static int sideButton(int index) {
      return 100 + index;
   }

   public static int typeButton(int index) {
      return 200 + index;
   }

   public static int removeButton(int index) {
      return 300 + index;
   }

   public static int moveUpButton(int index) {
      return 400 + index;
   }

   public static int moveDownButton(int index) {
      return 500 + index;
   }

   public static int ignoreDamageButton(int index) {
      return 600 + index;
   }

   public static int ignoreTagButton(int index) {
      return 700 + index;
   }

   public static int filterSlotButton(int entryIndex, int slot) {
      return 1000 + entryIndex * 12 + slot;
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
