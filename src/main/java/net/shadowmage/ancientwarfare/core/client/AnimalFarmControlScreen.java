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
   protected void init() {
      super.init();

      for (int row = 0; row < 8; row++) {
         int y = 20 + row * 14;
         this.addLimitButton(110, y, "-", row * 2);
         this.addLimitButton(148, y, "+", row * 2 + 1);
         this.registerLayoutRect("animal name " + row, 8, y + 2, 98, 9, true);
         this.registerLayoutRect("animal limit " + row, 124, y, 24, 12, true);
      }

      this.legacyButton("previous page", 50, 136, 20, 12, Component.literal("<"), button -> this.clickButton(1000));
      this.legacyButton("next page", 98, 136, 20, 12, Component.literal(">"), button -> this.clickButton(1001));
      this.registerLayoutRect("page label", 72, 138, 24, 9, true);
   }

   private void addLimitButton(int x, int y, String label, int id) {
      this.legacyButton("animal adjustment " + id, x, y, 12, 12, Component.literal(label), button -> this.clickButton(id));
   }

   private void clickButton(int id) {
      ((AnimalFarmControlMenu)this.menu).previewButton(id);
      this.minecraft.gameMode.handleInventoryButtonClick(((AnimalFarmControlMenu)this.menu).containerId, id);
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      for (int row = 0; row < 8; row++) {
         int y = this.topPos + 20 + row * 14;
         graphics.fill(this.leftPos + 124, y, this.leftPos + 148, y + 12, -1644826);
         graphics.fill(this.leftPos + 125, y + 1, this.leftPos + 147, y + 11, -16777216);
      }
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawLabel(graphics, Component.translatable("guistrings.automation.animal_limits"), 8, 6, 152);

      for (int row = 0; row < 8; row++) {
         ResourceLocation animalType = ((AnimalFarmControlMenu)this.menu).getAnimalType(row);
         if (animalType != null) {
            int y = 20 + row * 14 + 2;
            this.drawLabel(graphics, Component.translatable("entity." + animalType.getNamespace() + "." + animalType.getPath()), 8, y, 98);
            Component limit = Component.literal(Integer.toString(((AnimalFarmControlMenu)this.menu).getAnimalLimit(row)));
            this.drawCenteredLabel(graphics, limit, 136, y, 20);
         }
      }

      this.drawCenteredLabel(
         graphics,
         Component.literal(((AnimalFarmControlMenu)this.menu).getPage() + 1 + "/" + ((AnimalFarmControlMenu)this.menu).getPageCount()),
         84,
         138,
         24
      );
   }

   private void drawLabel(GuiGraphics graphics, Component label, int x, int y, int width) {
      graphics.pose().pushPose();
      graphics.pose().translate((float)x, (float)y, 0.0F);
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.font.width(label)));
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, label, 0, 0, -1, false);
      graphics.pose().popPose();
   }

   private void drawCenteredLabel(GuiGraphics graphics, Component label, int centerX, int y, int width) {
      graphics.pose().pushPose();
      graphics.pose().translate((float)centerX, (float)y, 0.0F);
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.font.width(label)));
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawCenteredString(this.font, label, 0, 0, -1);
      graphics.pose().popPose();
   }
}
