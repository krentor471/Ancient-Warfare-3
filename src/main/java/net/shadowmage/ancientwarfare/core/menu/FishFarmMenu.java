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
import net.shadowmage.ancientwarfare.core.tile.FishFarmBlockEntity;

public class FishFarmMenu extends AbstractContainerMenu {
   public static final int SIDES_BUTTON = 0;
   public static final int BOUNDS_BUTTON = 1;
   public static final int CONTROL_BUTTON = 2;
   private final Container container;
   private final BlockPos pos;
   private final Level level;

   public FishFarmMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.FISH_FARM), containerId);
      this.pos = pos;
      this.level = playerInventory.f_35978_.m_9236_();
      if (this.level.m_7702_(pos) instanceof FishFarmBlockEntity fishFarm) {
         this.container = fishFarm;
      } else {
         this.container = new SimpleContainer(32);
      }

      this.container.m_5856_(playerInventory.f_35978_);
      this.addSlotGrid(this.container, 0, 8, 18, 9, 3, Slot::new);
      this.addSlotGrid(this.container, 27, 8, 84, 5, 1, (c, index, x, y) -> new WorksiteUpgradeSlot(c, index, x, y, 27, 5, false));
      this.addPlayerInventory(playerInventory, 8, 140);
   }

   public boolean m_6366_(Player player, int id) {
      if (id == 0 && !this.level.f_46443_) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, p) -> new WorksiteSideConfigMenu(containerId, inventory, this.pos),
               Component.m_237115_("container.ancientwarfare.worksite_side_config")
            ),
            buffer -> buffer.m_130064_(this.pos)
         );
         return true;
      } else if (id == 1 && !this.level.f_46443_ && this.level.m_7702_(this.pos) instanceof FishFarmBlockEntity fishFarm) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, p) -> fishFarm.createBoundsMenu(containerId, inventory),
               Component.m_237115_("container.ancientwarfare.fish_farm_bounds")
            ),
            buffer -> buffer.m_130064_(this.pos)
         );
         return true;
      } else if (id == 2 && !this.level.f_46443_ && this.level.m_7702_(this.pos) instanceof FishFarmBlockEntity fishFarm) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, p) -> fishFarm.createControlMenu(containerId, inventory),
               Component.m_237115_("container.ancientwarfare.fish_farm_control")
            ),
            buffer -> buffer.m_130064_(this.pos)
         );
         return true;
      } else {
         return super.m_6366_(player, id);
      }
   }

   private void addSlotGrid(Container container, int firstSlot, int x, int y, int columns, int rows, FishFarmMenu.SlotFactory factory) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            int index = firstSlot + row * columns + col;
            this.m_38897_(factory.create(container, index, x + col * 18, y + row * 18));
         }
      }
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

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.FISH_FARM));
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      this.container.m_5785_(player);
   }

   private interface SlotFactory {
      Slot create(Container var1, int var2, int var3, int var4);
   }
}
