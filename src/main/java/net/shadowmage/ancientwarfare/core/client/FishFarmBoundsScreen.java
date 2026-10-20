package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.FishFarmBoundsMenu;

public class FishFarmBoundsScreen extends LegacyWorksiteBoundsScreen<FishFarmBoundsMenu> {
   public FishFarmBoundsScreen(FishFarmBoundsMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, menu::getBoundsMin, menu::getBoundsMax, menu::getWorksitePos, menu::previewButton);
   }
}
