package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.QuarryControlMenu;

public class QuarryControlScreen extends LegacyContainerScreen<QuarryControlMenu> {
   public QuarryControlScreen(QuarryControlMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 176, 40);
   }

   @Override
   protected void init() {
      super.init();
      this.addLegacyWidget(
         "toggle",
         new LegacyCheckbox(
            this.leftPos + 8,
            this.topPos + 12,
            16,
            Component.translatable("guistrings.automation.only_mine_ores"),
            ((QuarryControlMenu)this.menu).onlyMineOres(),
            checked -> {
               ((QuarryControlMenu)this.menu).previewButton(0);
               this.minecraft.gameMode.handleInventoryButtonClick(((QuarryControlMenu)this.menu).containerId, 0);
            }
         ),
         8,
         12,
         16,
         16
      );
   }
}
