package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.AnimalFarmBoundsMenu;

public class AnimalFarmBoundsScreen extends LegacyWorksiteBoundsScreen<AnimalFarmBoundsMenu> {
   public AnimalFarmBoundsScreen(AnimalFarmBoundsMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, menu::getBoundsMin, menu::getBoundsMax, menu::getWorksitePos, menu::previewButton);
   }
}
