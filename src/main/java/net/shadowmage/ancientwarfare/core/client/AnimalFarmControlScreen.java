package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.AnimalFarmControlMenu;

public class AnimalFarmControlScreen extends LegacyContainerScreen<AnimalFarmControlMenu> {
   private static final int ROW_Y = 20;
   private static final int ROW_HEIGHT = 14;

   public AnimalFarmControlScreen(AnimalFarmControlMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 168, 154);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();

      for (int row = 0; row < 8; row++) {
         int y = 20 + row * 14;
         this.addLimitButton(110, y, "-", row * 2);
         this.addLimitButton(148, y, "+", row * 2 + 1);
         this.registerLayoutRect("animal name " + row, 8, y + 2, 98, 9, true);
         this.registerLayoutRect("animal limit " + row, 124, y, 24, 12, true);
      }

      this.legacyButton("previous page", 50, 136, 20, 12, Component.m_237113_("<"), button -> this.clickButton(1000));
      this.legacyButton("next page", 98, 136, 20, 12, Component.m_237113_(">"), button -> this.clickButton(1001));
      this.registerLayoutRect("page label", 72, 138, 24, 9, true);
   }

   private void addLimitButton(int x, int y, String label, int id) {
      this.legacyButton("animal adjustment " + id, x, y, 12, 12, Component.m_237113_(label), button -> this.clickButton(id));
   }

   private void clickButton(int id) {
      ((AnimalFarmControlMenu)this.f_97732_).previewButton(id);
      this.f_96541_.f_91072_.m_105208_(((AnimalFarmControlMenu)this.f_97732_).f_38840_, id);
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      for (int row = 0; row < 8; row++) {
         int y = this.f_97736_ + 20 + row * 14;
         graphics.m_280509_(this.f_97735_ + 124, y, this.f_97735_ + 148, y + 12, -1644826);
         graphics.m_280509_(this.f_97735_ + 125, y + 1, this.f_97735_ + 147, y + 11, -16777216);
      }
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawLabel(graphics, Component.m_237115_("guistrings.automation.animal_limits"), 8, 6, 152);

      for (int row = 0; row < 8; row++) {
         ResourceLocation animalType = ((AnimalFarmControlMenu)this.f_97732_).getAnimalType(row);
         if (animalType != null) {
            int y = 20 + row * 14 + 2;
            this.drawLabel(graphics, Component.m_237115_("entity." + animalType.m_135827_() + "." + animalType.m_135815_()), 8, y, 98);
            Component limit = Component.m_237113_(Integer.toString(((AnimalFarmControlMenu)this.f_97732_).getAnimalLimit(row)));
            this.drawCenteredLabel(graphics, limit, 136, y, 20);
         }
      }

      this.drawCenteredLabel(
         graphics,
         Component.m_237113_(((AnimalFarmControlMenu)this.f_97732_).getPage() + 1 + "/" + ((AnimalFarmControlMenu)this.f_97732_).getPageCount()),
         84,
         138,
         24
      );
   }

   private void drawLabel(GuiGraphics graphics, Component label, int x, int y, int width) {
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)x, (float)y, 0.0F);
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92852_(label)));
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, label, 0, 0, -1, false);
      graphics.m_280168_().m_85849_();
   }

   private void drawCenteredLabel(GuiGraphics graphics, Component label, int centerX, int y, int width) {
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)centerX, (float)y, 0.0F);
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92852_(label)));
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280653_(this.f_96547_, label, 0, 0, -1);
      graphics.m_280168_().m_85849_();
   }
}
