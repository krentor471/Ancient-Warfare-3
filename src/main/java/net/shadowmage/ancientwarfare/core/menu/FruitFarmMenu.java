package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.FruitFarmBlockEntity;

public class FruitFarmMenu extends AbstractContainerMenu {
   public static final int SIDES_BUTTON = 0;
   public static final int BOUNDS_BUTTON = 1;
   private final Container container;
   private final BlockPos pos;
   private final Level level;

   public FruitFarmMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.FRUIT_FARM), containerId);
      this.pos = pos;
      this.level = playerInventory.player.level();
      if (this.level.getBlockEntity(pos) instanceof FruitFarmBlockEntity fruitFarm) {
         this.container = fruitFarm;
      } else {
         this.container = new SimpleContainer(38);
      }

      this.container.startOpen(playerInventory.player);
      this.addSlotGrid(this.container, 0, 8, 18, 9, 3, Slot::new);
      this.addSlotGrid(this.container, 27, 8, 84, 3, 1, (x$0, x$1, x$2, x$3) -> new FruitFarmMenu.PlantableSlot(x$0, x$1, x$2, x$3));
      this.addSlotGrid(this.container, 33, 80, 84, 5, 1, (c, index, x, y) -> new WorksiteUpgradeSlot(c, index, x, y, 33, 5, false));
      this.addSlotGrid(this.container, 30, 8, 114, 3, 1, (x$0, x$1, x$2, x$3) -> new FruitFarmMenu.MiscSlot(x$0, x$1, x$2, x$3));
      this.addPlayerInventory(playerInventory, 8, 140);
   }

   public boolean clickMenuButton(Player player, int id) {
      if (id == 0 && !this.level.isClientSide) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, p) -> new WorksiteSideConfigMenu(containerId, inventory, this.pos),
               Component.translatable("container.ancientwarfare.worksite_side_config")
            ),
            buffer -> buffer.writeBlockPos(this.pos)
         );
         return true;
      } else if (id == 1 && !this.level.isClientSide && this.level.getBlockEntity(this.pos) instanceof FruitFarmBlockEntity fruitFarm) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, p) -> fruitFarm.createBoundsMenu(containerId, inventory),
               Component.translatable("container.ancientwarfare.fruit_farm_bounds")
            ),
            buffer -> buffer.writeBlockPos(this.pos)
         );
         return true;
      } else {
         return super.clickMenuButton(player, id);
      }
   }

   private void addSlotGrid(Container container, int firstSlot, int x, int y, int columns, int rows, FruitFarmMenu.SlotFactory factory) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            int index = firstSlot + row * columns + col;
            this.addSlot(factory.create(container, index, x + col * 18, y + row * 18));
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
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.FRUIT_FARM));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);
   }

   private class MiscSlot extends Slot {
      private MiscSlot(Container container, int index, int x, int y) {
         super(container, index, x, y);
      }

      public boolean mayPlace(ItemStack stack) {
         if (this.container instanceof FruitFarmBlockEntity fruitFarm && fruitFarm.isMiscItem(stack)) {
            return true;
         }

         return false;
      }
   }

   private class PlantableSlot extends Slot {
      private PlantableSlot(Container container, int index, int x, int y) {
         super(container, index, x, y);
      }

      public boolean mayPlace(ItemStack stack) {
         if (this.container instanceof FruitFarmBlockEntity fruitFarm && fruitFarm.isPlantable(stack)) {
            return true;
         }

         return false;
      }
   }

   private interface SlotFactory {
      Slot create(Container var1, int var2, int var3, int var4);
   }
}
