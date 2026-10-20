package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.menu.ResearchStationMenu;

public class ResearchStationScreen extends LegacyContainerScreen<ResearchStationMenu> {
   private static final int TEXT_COLOR = 16777215;
   private LegacyTexturedButton queueButton;

   public ResearchStationScreen(ResearchStationMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 178, 240);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.queueButton = this.legacyButton(
         "research queue",
         60,
         24,
         110,
         12,
         Component.m_237115_("guistrings.research.research_queue"),
         button -> this.f_96541_.m_91152_(new ResearchQueueScreen((ResearchStationMenu)this.f_97732_, this.f_96541_.f_91074_.m_150109_(), this.f_96539_))
      );
      this.registerLayoutRect("needed resource preview", 97, 97, 56, 56, false);
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.slots(graphics, this.f_97735_, this.f_97736_, 98, 98, 3, 3);
      this.drawProgressBar(graphics, 70, 56, 100, 12, ((ResearchStationMenu)this.f_97732_).getProgressRatio());
      this.drawNeededResources(graphics);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      String researcherName = ((ResearchStationMenu)this.f_97732_).getResearcherName();
      this.queueButton.f_93623_ = !researcherName.isEmpty();
      graphics.m_280614_(
         this.f_96547_,
         researcherName.isEmpty() ? Component.m_237115_("guistrings.research.no_researcher") : Component.m_237113_(researcherName),
         8,
         8,
         16777215,
         false
      );
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.current_goal").m_130946_(":"), 8, 44, 16777215, false);
      String currentResearch = ((ResearchStationMenu)this.f_97732_).getCurrentResearchName();
      graphics.m_280614_(
         this.f_96547_,
         currentResearch.isEmpty() ? Component.m_237115_("guistrings.research.no_research") : Component.m_237113_(currentResearch),
         80,
         44,
         16777215,
         false
      );
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.progress"), 8, 57, 16777215, false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.input"), 26, 84, 16777215, false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.needed"), 98, 84, 16777215, false);
   }

   private void drawNeededResources(GuiGraphics graphics) {
      List<ItemStack> resources = ((ResearchStationMenu)this.f_97732_).getCurrentResearchResources();

      for (int i = 0; i < resources.size() && i < 9; i++) {
         int x = this.f_97735_ + 98 + i % 3 * 18;
         int y = this.f_97736_ + 98 + i / 3 * 18;
         graphics.m_280480_(resources.get(i), x, y);
         graphics.m_280370_(this.f_96547_, resources.get(i), x, y);
      }
   }

   private void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + x, this.f_97736_ + y, width, height, 0, 0, 256, 40);
      int fillWidth = Math.max(0, Math.min(width - 6, Math.round((float)(width - 6) * progress)));
      if (fillWidth > 0) {
         LegacyGuiStyle.quarteredTexture(
            graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + x + 3, this.f_97736_ + y + 3, fillWidth, height - 6, 152, 234, 104, 10
         );
      }
   }
}
