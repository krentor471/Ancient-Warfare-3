package net.shadowmage.ancientwarfare.core.menu;

import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.UpkeepOrderData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class UpkeepOrderMenu extends AbstractContainerMenu {
   public static final int BUTTON_CYCLE_SIDE = 0;
   public static final int BUTTON_AMOUNT_PLUS = 1;
   public static final int BUTTON_AMOUNT_MINUS = 2;
   public static final int BUTTON_CLEAR = 3;
   public static final int BUTTON_FOOD_PLUS = 4;
   public static final int BUTTON_FOOD_MINUS = 5;
   public static final int BUTTON_FOOD_SLOT = 6;
   private final Player player;
   private final InteractionHand hand;

   public UpkeepOrderMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.UPKEEP_ORDER_MENU), containerId);
      this.player = playerInventory.f_35978_;
      this.hand = hand;
      this.addPlayerInventorySlots(playerInventory, 47, 156);
   }

   public ItemStack getUpkeepOrderStack() {
      return this.player.m_21120_(this.hand);
   }

   public Optional<UpkeepOrderData.UpkeepEntry> getEntry() {
      return UpkeepOrderData.getEntry(this.getUpkeepOrderStack());
   }

   public ItemStack getFoodTarget() {
      return UpkeepOrderData.getFoodTarget(this.getUpkeepOrderStack());
   }

   public boolean m_6366_(Player player, int id) {
      ItemStack stack = this.getUpkeepOrderStack();
      if (!AWCoreRegistryAccess.is(stack, AWCoreIds.UPKEEP_ORDER)) {
         return false;
      } else if (id == 0) {
         UpkeepOrderData.cycleSide(stack);
         return true;
      } else if (id == 1) {
         UpkeepOrderData.incrementAmount(stack);
         return true;
      } else if (id == 2) {
         UpkeepOrderData.decrementAmount(stack);
         return true;
      } else if (id == 3) {
         UpkeepOrderData.clear(stack);
         return true;
      } else if (id == 4) {
         UpkeepOrderData.changeFoodCount(stack, 1);
         return true;
      } else if (id == 5) {
         UpkeepOrderData.changeFoodCount(stack, -1);
         return true;
      } else if (id == 6) {
         UpkeepOrderData.setFoodTarget(stack, this.m_142621_());
         return true;
      } else {
         return false;
      }
   }

   public boolean m_6875_(Player player) {
      return AWCoreRegistryAccess.is(player.m_21120_(this.hand), AWCoreIds.UPKEEP_ORDER);
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }

   private void addPlayerInventorySlots(Inventory inventory, int x, int y) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(inventory, col, x + col * 18, y + 58));
      }
   }
}
