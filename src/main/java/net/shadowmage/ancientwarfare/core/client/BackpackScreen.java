package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.BackpackMenu;

public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
   private static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   private static final ResourceLocation WIDGETS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");

   public BackpackScreen(BackpackMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 176;
      this.imageHeight = 8 + menu.getBackpackRows() * 18 + 8 + 76 + 8;
      this.titleLabelX = 1000;
      this.inventoryLabelY = 1000;
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawBackground(graphics);
      this.drawSlotGrid(graphics, 8, 8, 9, ((BackpackMenu)this.menu).getBackpackRows());
      int playerTop = 8 + ((BackpackMenu)this.menu).getBackpackRows() * 18 + 8;
      this.drawSlotGrid(graphics, 8, playerTop, 9, 3);
      this.drawSlotGrid(graphics, 8, playerTop + 58, 9, 1);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   private void drawBackground(GuiGraphics graphics) {
      this.drawNineSlice(graphics, BACKGROUND, 0, 0, this.imageWidth, this.imageHeight, 0, 0, 256, 240);
   }

   private void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            this.drawDecorativeSlot(graphics, x + col * 18, y + row * 18);
         }
      }
   }

   private void drawDecorativeSlot(GuiGraphics graphics, int x, int y) {
      graphics.blit(WIDGETS, this.leftPos + x - 1, this.topPos + y - 1, 152, 120, 18, 18);
   }

   private void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      graphics.blit(texture, this.leftPos + x, this.topPos + y, u, v, halfWidth, halfHeight);
      graphics.blit(texture, this.leftPos + x + halfWidth, this.topPos + y, u + texWidth - (width - halfWidth), v, width - halfWidth, halfHeight);
      graphics.blit(texture, this.leftPos + x, this.topPos + y + halfHeight, u, v + texHeight - (height - halfHeight), halfWidth, height - halfHeight);
      graphics.blit(
         texture,
         this.leftPos + x + halfWidth,
         this.topPos + y + halfHeight,
         u + texWidth - (width - halfWidth),
         v + texHeight - (height - halfHeight),
         width - halfWidth,
         height - halfHeight
      );
   }
}
