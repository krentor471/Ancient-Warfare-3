package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.FishFarmMenu;

public class FishFarmScreen extends LegacyContainerScreen<FishFarmMenu> {
   private static final int TEXT_COLOR = 16777215;

   public FishFarmScreen(FishFarmMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 178, 240);
   }

   @Override
   protected void init() {
      super.init();
      this.legacyButton("sides", 8, this.imageHeight - 20, 50, 12, Component.translatable("guistrings.inventory.setsides"), button -> this.clickButton(0));
      this.legacyButton("bounds", 58, this.imageHeight - 20, 50, 12, Component.translatable("guistrings.automation.bounds"), button -> this.clickButton(1));
      this.legacyButton("control", 108, this.imageHeight - 20, 50, 12, Component.translatable("guistrings.automation.control"), button -> this.clickButton(2));
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawLabel(graphics, Component.translatable("guistrings.automation.top"), 8, 8, 162);
      this.drawLabel(graphics, Component.translatable("guistrings.automation.upgrades"), 8, 72, 162);
      this.drawLabel(graphics, this.playerInventoryTitle, 8, 128, 162);
   }

   private void clickButton(int buttonId) {
      if (this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((FishFarmMenu)this.menu).containerId, buttonId);
      }
   }

   private void drawLabel(GuiGraphics graphics, Component label, int x, int y, int width) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.font.width(label)));
      graphics.pose().pushPose();
      graphics.pose().translate((float)x, (float)y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, label, 0, 0, 16777215, false);
      graphics.pose().popPose();
   }
}
