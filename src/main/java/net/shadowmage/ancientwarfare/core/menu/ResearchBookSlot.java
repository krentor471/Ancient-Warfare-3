package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class ResearchBookSlot extends Slot {
   public ResearchBookSlot(Container container, int slot, int x, int y) {
      super(container, slot, x, y);
   }

   public boolean m_5857_(ItemStack stack) {
      return AWCoreRegistryAccess.is(stack, AWCoreIds.RESEARCH_BOOK);
   }

   public int m_6641_() {
      return 1;
   }
}
