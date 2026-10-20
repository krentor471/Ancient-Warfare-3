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
   protected void init() {
      super.init();
      this.queueButton = this.legacyButton(
         "research queue",
         60,
         24,
         110,
         12,
         Component.translatable("guistrings.research.research_queue"),
         button -> this.minecraft.setScreen(new ResearchQueueScreen((ResearchStationMenu)this.menu, this.minecraft.player.getInventory(), this.title))
      );
      this.registerLayoutRect("needed resource preview", 97, 97, 56, 56, false);
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.slots(graphics, this.leftPos, this.topPos, 98, 98, 3, 3);
      this.drawProgressBar(graphics, 70, 56, 100, 12, ((ResearchStationMenu)this.menu).getProgressRatio());
      this.drawNeededResources(graphics);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      String researcherName = ((ResearchStationMenu)this.menu).getResearcherName();
      this.queueButton.active = !researcherName.isEmpty();
      graphics.drawString(
         this.font,
         researcherName.isEmpty() ? Component.translatable("guistrings.research.no_researcher") : Component.literal(researcherName),
         8,
         8,
         16777215,
         false
      );
      graphics.drawString(this.font, Component.translatable("guistrings.research.current_goal").append(":"), 8, 44, 16777215, false);
      String currentResearch = ((ResearchStationMenu)this.menu).getCurrentResearchName();
      graphics.drawString(
         this.font,
         currentResearch.isEmpty() ? Component.translatable("guistrings.research.no_research") : Component.literal(currentResearch),
         80,
         44,
         16777215,
         false
      );
      graphics.drawString(this.font, Component.translatable("guistrings.research.progress"), 8, 57, 16777215, false);
      graphics.drawString(this.font, Component.translatable("guistrings.research.input"), 26, 84, 16777215, false);
      graphics.drawString(this.font, Component.translatable("guistrings.research.needed"), 98, 84, 16777215, false);
   }

   private void drawNeededResources(GuiGraphics graphics) {
      List<ItemStack> resources = ((ResearchStationMenu)this.menu).getCurrentResearchResources();

      for (int i = 0; i < resources.size() && i < 9; i++) {
         int x = this.leftPos + 98 + i % 3 * 18;
         int y = this.topPos + 98 + i / 3 * 18;
         graphics.renderItem(resources.get(i), x, y);
         graphics.renderItemDecorations(this.font, resources.get(i), x, y);
      }
   }

   private void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.leftPos + x, this.topPos + y, width, height, 0, 0, 256, 40);
      int fillWidth = Math.max(0, Math.min(width - 6, Math.round((float)(width - 6) * progress)));
      if (fillWidth > 0) {
         LegacyGuiStyle.quarteredTexture(
            graphics, LegacyGuiStyle.WIDGETS, this.leftPos + x + 3, this.topPos + y + 3, fillWidth, height - 6, 152, 234, 104, 10
         );
      }
   }
}
