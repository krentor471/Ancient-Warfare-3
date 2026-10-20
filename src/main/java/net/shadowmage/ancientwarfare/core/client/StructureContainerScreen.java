package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.StructureContainerMenu;

public class StructureContainerScreen extends AbstractContainerScreen<StructureContainerMenu> {
   private static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   private static final ResourceLocation WIDGETS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");
   private static final int TEXT_COLOR = 16777215;

   public StructureContainerScreen(StructureContainerMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 176;
      this.imageHeight = 128 + menu.getContainerRows() * 18;
      this.titleLabelX = 8;
      this.titleLabelY = 6;
      this.inventoryLabelY = 20 + menu.getContainerRows() * 18;
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -3750202);
      graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, Math.min(this.imageHeight, 256));
      this.drawSlotGrid(graphics, 8, 18, 9, ((StructureContainerMenu)this.menu).getContainerRows());
      this.drawSlotGrid(graphics, 8, 32 + ((StructureContainerMenu)this.menu).getContainerRows() * 18, 9, 3);
      this.drawSlotGrid(graphics, 8, 90 + ((StructureContainerMenu)this.menu).getContainerRows() * 18, 9, 1);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, this.title, 8, 8, 16777215, false);
      graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 16777215, false);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   private void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            graphics.blit(WIDGETS, this.leftPos + x + col * 18 - 1, this.topPos + y + row * 18 - 1, 152, 120, 18, 18);
         }
      }
   }
}
