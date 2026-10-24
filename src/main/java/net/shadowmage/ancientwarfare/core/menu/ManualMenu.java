package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class ManualMenu extends AbstractContainerMenu {
   public ManualMenu(int containerId) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.MANUAL), containerId);
   }

   public boolean stillValid(Player player) {
      return true;
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }
}
