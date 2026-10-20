package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgradeHelper;

public class WorksiteUpgradeSlot extends Slot {
   private final int upgradeSlotStart;
   private final int upgradeSlotCount;
   private final boolean quarry;

   public WorksiteUpgradeSlot(Container container, int slot, int x, int y, int upgradeSlotStart, int upgradeSlotCount, boolean quarry) {
      super(container, slot, x, y);
      this.upgradeSlotStart = upgradeSlotStart;
      this.upgradeSlotCount = upgradeSlotCount;
      this.quarry = quarry;
   }

   public boolean m_5857_(ItemStack stack) {
      return WorksiteUpgradeHelper.mayPlace(this.f_40218_, this.m_150661_(), stack, this.upgradeSlotStart, this.upgradeSlotCount, this.quarry);
   }

   public int m_6641_() {
      return 1;
   }
}
