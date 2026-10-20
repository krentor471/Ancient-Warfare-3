package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgrade;

public class WorksiteUpgradeItem extends Item {
   private final WorksiteUpgrade upgrade;

   public WorksiteUpgradeItem(WorksiteUpgrade upgrade) {
      super(new Properties().m_41487_(1));
      this.upgrade = upgrade;
   }

   public WorksiteUpgrade getUpgrade() {
      return this.upgrade;
   }
}
