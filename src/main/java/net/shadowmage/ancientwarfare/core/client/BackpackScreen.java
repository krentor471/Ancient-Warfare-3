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
      this.f_97726_ = 176;
      this.f_97727_ = 8 + menu.getBackpackRows() * 18 + 8 + 76 + 8;
      this.f_97728_ = 1000;
      this.f_97731_ = 1000;
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawBackground(graphics);
      this.drawSlotGrid(graphics, 8, 8, 9, ((BackpackMenu)this.f_97732_).getBackpackRows());
      int playerTop = 8 + ((BackpackMenu)this.f_97732_).getBackpackRows() * 18 + 8;
      this.drawSlotGrid(graphics, 8, playerTop, 9, 3);
      this.drawSlotGrid(graphics, 8, playerTop + 58, 9, 1);
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      this.m_280072_(graphics, mouseX, mouseY);
   }

   private void drawBackground(GuiGraphics graphics) {
      this.drawNineSlice(graphics, BACKGROUND, 0, 0, this.f_97726_, this.f_97727_, 0, 0, 256, 240);
   }

   private void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            this.drawDecorativeSlot(graphics, x + col * 18, y + row * 18);
         }
      }
   }

   private void drawDecorativeSlot(GuiGraphics graphics, int x, int y) {
      graphics.m_280218_(WIDGETS, this.f_97735_ + x - 1, this.f_97736_ + y - 1, 152, 120, 18, 18);
   }

   private void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      graphics.m_280218_(texture, this.f_97735_ + x, this.f_97736_ + y, u, v, halfWidth, halfHeight);
      graphics.m_280218_(texture, this.f_97735_ + x + halfWidth, this.f_97736_ + y, u + texWidth - (width - halfWidth), v, width - halfWidth, halfHeight);
      graphics.m_280218_(texture, this.f_97735_ + x, this.f_97736_ + y + halfHeight, u, v + texHeight - (height - halfHeight), halfWidth, height - halfHeight);
      graphics.m_280218_(
         texture,
         this.f_97735_ + x + halfWidth,
         this.f_97736_ + y + halfHeight,
         u + texWidth - (width - halfWidth),
         v + texHeight - (height - halfHeight),
         width - halfWidth,
         height - halfHeight
      );
   }
}
