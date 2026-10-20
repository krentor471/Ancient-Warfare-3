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

   public boolean m_6875_(Player player) {
      return true;
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }
}
