package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.QuarryControlMenu;

public class QuarryControlScreen extends LegacyContainerScreen<QuarryControlMenu> {
   public QuarryControlScreen(QuarryControlMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 176, 40);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.addLegacyWidget(
         "toggle",
         new LegacyCheckbox(
            this.f_97735_ + 8,
            this.f_97736_ + 12,
            16,
            Component.m_237115_("guistrings.automation.only_mine_ores"),
            ((QuarryControlMenu)this.f_97732_).onlyMineOres(),
            checked -> {
               ((QuarryControlMenu)this.f_97732_).previewButton(0);
               this.f_96541_.f_91072_.m_105208_(((QuarryControlMenu)this.f_97732_).f_38840_, 0);
            }
         ),
         8,
         12,
         16,
         16
      );
   }
}
