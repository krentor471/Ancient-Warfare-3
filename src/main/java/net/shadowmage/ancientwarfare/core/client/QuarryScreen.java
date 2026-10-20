package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.QuarryMenu;

public class QuarryScreen extends LegacyContainerScreen<QuarryMenu> {
   private static final int TEXT_COLOR = 16777215;

   public QuarryScreen(QuarryMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 178, 240);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.legacyButton("sides", 8, this.f_97727_ - 20, 50, 12, Component.m_237115_("guistrings.inventory.setsides"), button -> this.clickButton(0));
      this.legacyButton("bounds", 58, this.f_97727_ - 20, 50, 12, Component.m_237115_("guistrings.automation.bounds"), button -> this.clickButton(1));
      this.legacyButton("control", 108, this.f_97727_ - 20, 50, 12, Component.m_237115_("guistrings.automation.control"), button -> this.clickButton(2));
   }

   private void clickButton(int id) {
      if (this.f_96541_.f_91072_ != null) {
         this.f_96541_.f_91072_.m_105208_(((QuarryMenu)this.f_97732_).f_38840_, id);
      }
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawLabel(graphics, Component.m_237115_("guistrings.automation.resources"), 8, 8, 162);
      this.drawLabel(graphics, Component.m_237115_("guistrings.automation.upgrades"), 8, 114, 68);
      this.drawLabel(graphics, this.f_169604_, 8, 128, 68);
   }

   private void drawLabel(GuiGraphics graphics, Component label, int x, int y, int width) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92852_(label)));
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)x, (float)y, 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, label, 0, 0, 16777215, false);
      graphics.m_280168_().m_85849_();
   }
}
