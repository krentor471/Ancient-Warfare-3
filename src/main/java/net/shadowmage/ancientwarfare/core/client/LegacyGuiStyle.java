package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class LegacyGuiStyle {
   public static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   public static final ResourceLocation WIDGETS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");

   private LegacyGuiStyle() {
   }

   public static void background(GuiGraphics graphics, int left, int top, int width, int height) {
      quarteredTexture(graphics, BACKGROUND, left, top, width, height, 0, 0, 256, 240);
   }

   public static void slot(GuiGraphics graphics, int left, int top, int x, int y) {
      graphics.blit(WIDGETS, left + x - 1, top + y - 1, 152.0F, 120.0F, 18, 18, 256, 256);
   }

   public static void slots(GuiGraphics graphics, int left, int top, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            slot(graphics, left, top, x + col * 18, y + row * 18);
         }
      }
   }

   public static void arrow(GuiGraphics graphics, int left, int top, int x, int y, boolean right) {
      graphics.blit(WIDGETS, left + x, top + y, right ? 200.0F : 170.0F, 121.0F, 10, 16, 256, 256);
   }

   public static void button(GuiGraphics graphics, int left, int top, int x, int y, int width, int height, int mouseX, int mouseY) {
      int u = mouseX >= left + x && mouseX < left + x + width && mouseY >= top + y && mouseY < top + y + height ? 80 : 40;
      quarteredTexture(graphics, WIDGETS, left + x, top + y, width, height, 0, u, 256, 40);
   }

   public static void buttonWithText(
      GuiGraphics graphics, Font font, int left, int top, int x, int y, int width, int height, Component label, int mouseX, int mouseY
   ) {
      button(graphics, left, top, x, y, width, height, mouseX, mouseY);
      int textWidth = font.width(label);
      float scale = textWidth <= width - 4 ? 1.0F : (float)(width - 4) / (float)textWidth;
      graphics.pose().pushPose();
      graphics.pose()
         .translate(
            (float)(left + x) + (scale == 1.0F ? (float)(width - textWidth) / 2.0F : 2.0F),
            (float)(top + y) + Math.max(1.0F, ((float)height - 8.0F) / 2.0F),
            0.0F
         );
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(font, label, 0, 0, -1, true);
      graphics.pose().popPose();
   }

   public static void nineSlice(GuiGraphics graphics, int left, int top, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
      quarteredTexture(graphics, WIDGETS, left + x, top + y, width, height, u, v, textureWidth, textureHeight);
   }

   public static void quarteredTexture(
      GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight
   ) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      int rightWidth = width - halfWidth;
      int bottomHeight = height - halfHeight;
      int rightU = u + sourceWidth - rightWidth;
      int bottomV = v + sourceHeight - bottomHeight;
      graphics.blit(texture, x, y, (float)u, (float)v, halfWidth, halfHeight, 256, 256);
      graphics.blit(texture, x + halfWidth, y, (float)rightU, (float)v, rightWidth, halfHeight, 256, 256);
      graphics.blit(texture, x, y + halfHeight, (float)u, (float)bottomV, halfWidth, bottomHeight, 256, 256);
      graphics.blit(texture, x + halfWidth, y + halfHeight, (float)rightU, (float)bottomV, rightWidth, bottomHeight, 256, 256);
   }
}
