package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.FishFarmControlMenu;

public class FishFarmControlScreen extends LegacyContainerScreen<FishFarmControlMenu> {
   public FishFarmControlScreen(FishFarmControlMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 168, 48);
   }

   @Override
   protected void init() {
      super.init();
      this.addLegacyWidget(
         "fish toggle",
         new LegacyCheckbox(
            this.leftPos + 8,
            this.topPos + 8,
            16,
            Component.translatable("guistrings.automation.harvest_fish"),
            ((FishFarmControlMenu)this.menu).harvestFish(),
            checked -> {
               ((FishFarmControlMenu)this.menu).previewButton(0);
               this.minecraft.gameMode.handleInventoryButtonClick(((FishFarmControlMenu)this.menu).containerId, 0);
            }
         ),
         8,
         8,
         16,
         16
      );
      this.addLegacyWidget(
         "ink toggle",
         new LegacyCheckbox(
            this.leftPos + 8,
            this.topPos + 24,
            16,
            Component.translatable("guistrings.automation.harvest_ink"),
            ((FishFarmControlMenu)this.menu).harvestInk(),
            checked -> {
               ((FishFarmControlMenu)this.menu).previewButton(1);
               this.minecraft.gameMode.handleInventoryButtonClick(((FishFarmControlMenu)this.menu).containerId, 1);
            }
         ),
         8,
         24,
         16,
         16
      );
   }
}
