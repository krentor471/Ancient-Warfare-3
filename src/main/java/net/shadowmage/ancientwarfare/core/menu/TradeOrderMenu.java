package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.TradeOrderData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.TraderNpcEntity;

public class TradeOrderMenu extends AbstractContainerMenu {
   public static final int BUTTON_ADD_TRADE = 1;
   public static final int BUTTON_MOVE_UP_BASE = 200;
   public static final int BUTTON_MOVE_DOWN_BASE = 300;
   public static final int BUTTON_REMOVE_BASE = 100;
   public static final int BUTTON_INPUT_SLOT_BASE = 1000;
   public static final int BUTTON_OUTPUT_SLOT_BASE = 2000;
   private final Inventory playerInventory;
   private final InteractionHand hand;
   private final int traderEntityId;

   public TradeOrderMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
      this(containerId, playerInventory, hand, -1);
   }

   public TradeOrderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
      this(containerId, playerInventory, (InteractionHand)buffer.readEnum(InteractionHand.class), buffer.readInt());
   }

   public TradeOrderMenu(int containerId, Inventory playerInventory, InteractionHand hand, int traderEntityId) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.TRADE_ORDER_MENU), containerId);
      this.playerInventory = playerInventory;
      this.hand = hand;
      this.traderEntityId = traderEntityId;
      this.addPlayerInventorySlots(playerInventory, 47, 156);
   }

   public ItemStack getTradeOrderStack() {
      if (this.traderEntityId >= 0) {
         return this.playerInventory.player.level().getEntity(this.traderEntityId) instanceof TraderNpcEntity trader
            ? trader.getOrdersStack()
            : ItemStack.EMPTY;
      } else {
         return this.playerInventory.player.getItemInHand(this.hand);
      }
   }

   public int getTraderEntityId() {
      return this.traderEntityId;
   }

   public boolean isTraderBound() {
      return this.traderEntityId >= 0;
   }

   public BlockPos getRouteTarget() {
      return TradeOrderData.getRouteTarget(this.getTradeOrderStack())
         .orElseGet(
            () -> (this.traderEntityId < 0 ? null : this.playerInventory.player.level().getEntity(this.traderEntityId)) instanceof TraderNpcEntity trader
                  ? trader.blockPosition()
                  : this.playerInventory.player.blockPosition()
         );
   }

   public List<TradeOrderData.TradeEntry> getTrades() {
      return TradeOrderData.getTrades(this.getTradeOrderStack());
   }

   public boolean clickMenuButton(Player player, int id) {
      ItemStack stack = this.getTradeOrderStack();
      if (!AWCoreRegistryAccess.is(stack, AWCoreIds.TRADE_ORDER)) {
         return false;
      } else if (id == 1) {
         return TradeOrderData.addTrade(stack);
      } else if (id >= 1000 && id < 2000) {
         int encoded = id - 1000;
         return TradeOrderData.setTradeSlot(stack, encoded / 9, false, encoded % 9, this.getCarried());
      } else if (id >= 2000) {
         int encoded = id - 2000;
         return TradeOrderData.setTradeSlot(stack, encoded / 9, true, encoded % 9, this.getCarried());
      } else if (id >= 300) {
         return TradeOrderData.moveTradeDown(stack, id - 300);
      } else if (id >= 200) {
         return TradeOrderData.moveTradeUp(stack, id - 200);
      } else {
         return id >= 100 ? TradeOrderData.removeTrade(stack, id - 100) : false;
      }
   }

   public boolean stillValid(Player player) {
      if (this.traderEntityId >= 0) {
         if (player.level().getEntity(this.traderEntityId) instanceof TraderNpcEntity trader
            && trader.isAlive()
            && trader.hasCommandPermission(player)
            && trader.distanceToSqr(player) < 64.0
            && AWCoreRegistryAccess.is(trader.getOrdersStack(), AWCoreIds.TRADE_ORDER)) {
            return true;
         }

         return false;
      } else {
         return player.getItemInHand(this.hand) == this.getTradeOrderStack() && AWCoreRegistryAccess.is(this.getTradeOrderStack(), AWCoreIds.TRADE_ORDER);
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public static int removeButton(int index) {
      return 100 + index;
   }

   public static int moveUpButton(int index) {
      return 200 + index;
   }

   public static int moveDownButton(int index) {
      return 300 + index;
   }

   public static int inputSlotButton(int tradeIndex, int slot) {
      return 1000 + tradeIndex * 9 + slot;
   }

   public static int outputSlotButton(int tradeIndex, int slot) {
      return 2000 + tradeIndex * 9 + slot;
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
