package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

public class TownHallMenu extends AbstractContainerMenu {
   private static final int TOWN_HALL_SLOTS = 27;
   private final Container container;
   private final BlockPos pos;

   public TownHallMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.TOWN_HALL_MENU), containerId);
      this.pos = pos;
      Level level = playerInventory.player.level();
      if (level.getBlockEntity(pos) instanceof TownHallBlockEntity townHall) {
         this.container = townHall;
      } else {
         this.container = new SimpleContainer(27);
      }

      this.container.startOpen(playerInventory.player);
      this.addSlotGrid(this.container, 0, 8, 22, 9, 3);
      this.addPlayerInventory(playerInventory, 8, 92);
   }

   public List<TownHallBlockEntity.NpcDeathEntry> getDeathNotices() {
      return this.container instanceof TownHallBlockEntity townHall ? townHall.getDeathNotices() : List.of();
   }

   private void addSlotGrid(Container container, int firstSlot, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            int index = firstSlot + row * columns + col;
            if (index < container.getContainerSize()) {
               this.addSlot(new Slot(container, index, x + col * 18, y + row * 18));
            }
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
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.TOWN_HALL));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (index >= 0 && index < this.slots.size()) {
         Slot slot = (Slot)this.slots.get(index);
         if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index < 27) {
               if (!this.moveItemStackTo(stack, 27, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(stack, 0, 27, false)) {
               return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
               slot.setByPlayer(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }

            if (stack.getCount() == original.getCount()) {
               return ItemStack.EMPTY;
            } else {
               slot.onTake(player, stack);
               this.broadcastChanges();
               return original;
            }
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);
   }
}
