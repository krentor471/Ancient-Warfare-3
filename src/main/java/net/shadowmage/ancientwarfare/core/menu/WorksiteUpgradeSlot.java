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

   public boolean mayPlace(ItemStack stack) {
      return WorksiteUpgradeHelper.mayPlace(this.container, this.getContainerSlot(), stack, this.upgradeSlotStart, this.upgradeSlotCount, this.quarry);
   }

   public int getMaxStackSize() {
      return 1;
   }
}
