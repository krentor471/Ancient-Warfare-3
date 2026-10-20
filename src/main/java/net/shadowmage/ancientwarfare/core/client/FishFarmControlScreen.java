package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.FishFarmControlMenu;

public class FishFarmControlScreen extends LegacyContainerScreen<FishFarmControlMenu> {
   public FishFarmControlScreen(FishFarmControlMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 168, 48);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.addLegacyWidget(
         "fish toggle",
         new LegacyCheckbox(
            this.f_97735_ + 8,
            this.f_97736_ + 8,
            16,
            Component.m_237115_("guistrings.automation.harvest_fish"),
            ((FishFarmControlMenu)this.f_97732_).harvestFish(),
            checked -> {
               ((FishFarmControlMenu)this.f_97732_).previewButton(0);
               this.f_96541_.f_91072_.m_105208_(((FishFarmControlMenu)this.f_97732_).f_38840_, 0);
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
            this.f_97735_ + 8,
            this.f_97736_ + 24,
            16,
            Component.m_237115_("guistrings.automation.harvest_ink"),
            ((FishFarmControlMenu)this.f_97732_).harvestInk(),
            checked -> {
               ((FishFarmControlMenu)this.f_97732_).previewButton(1);
               this.f_96541_.f_91072_.m_105208_(((FishFarmControlMenu)this.f_97732_).f_38840_, 1);
            }
         ),
         8,
         24,
         16,
         16
      );
   }
}
