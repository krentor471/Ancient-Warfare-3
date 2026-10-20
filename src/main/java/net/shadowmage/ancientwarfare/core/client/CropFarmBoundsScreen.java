package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.CropFarmBoundsMenu;

public class CropFarmBoundsScreen extends LegacyWorksiteBoundsScreen<CropFarmBoundsMenu> {
   public CropFarmBoundsScreen(CropFarmBoundsMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, menu::getBoundsMin, menu::getBoundsMax, menu::getWorksitePos, menu::previewButton);
   }
}
