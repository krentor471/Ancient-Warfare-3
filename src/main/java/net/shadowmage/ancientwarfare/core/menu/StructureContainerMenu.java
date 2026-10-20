package net.shadowmage.ancientwarfare.core.menu;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.StructureContainerBlockEntity;

public class StructureContainerMenu extends AbstractContainerMenu {
   private final Container container;
   private final BlockPos pos;
   private final int containerRows;
   private final int containerSlotCount;
   private final int playerStart;

   public StructureContainerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.STRUCTURE_CONTAINER), containerId);
      this.pos = pos;
      Level level = playerInventory.player.level();
      BlockEntity blockEntity = level.getBlockEntity(pos);
      int expectedSize = StructureContainerBlockEntity.containerSize(level.getBlockState(pos));
      if (blockEntity instanceof StructureContainerBlockEntity structureContainer) {
         this.container = structureContainer;
      } else {
         this.container = new SimpleContainer(expectedSize);
      }

      this.containerSlotCount = expectedSize;
      this.playerStart = this.containerSlotCount;
      this.containerRows = Math.max(1, (int)Math.ceil((double)this.containerSlotCount / 9.0));
      this.container.startOpen(playerInventory.player);
      this.addSlotGrid(this.container, 0, 8, 18, 9, this.containerRows);
      this.addPlayerInventory(playerInventory, 8, 32 + this.containerRows * 18);
   }

   public int getContainerRows() {
      return this.containerRows;
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
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.LOOT_BASKET))
         || stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.URN))
         || stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN))
         || stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN))
         || stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.ADVANCED_LOOT_CHEST));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (index >= 0 && index < this.slots.size()) {
         Slot slot = (Slot)this.slots.get(index);
         if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index < this.containerSlotCount) {
               if (!this.moveItemStackTo(stack, this.playerStart, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(stack, 0, this.containerSlotCount, false)) {
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
