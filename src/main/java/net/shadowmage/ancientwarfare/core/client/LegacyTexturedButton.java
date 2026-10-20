package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class LegacyTexturedButton extends AbstractButton {
   private final LegacyTexturedButton.OnPress onPress;

   public LegacyTexturedButton(int x, int y, int width, int height, Component message, LegacyTexturedButton.OnPress onPress) {
      super(x, y, width, height, message);
      this.onPress = onPress;
      Font font = Minecraft.getInstance().font;
      if (font.width(message) > width - 4) {
         this.setTooltip(Tooltip.create(message));
      }
   }

   public void onPress() {
      this.onPress.onPress(this);
   }

   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      RenderSystem.enableBlend();
      int stateY = this.active ? (this.isHoveredOrFocused() ? 80 : 40) : 0;
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.getX(), this.getY(), this.width, this.height, 0, stateY, 256, 40);
      Font font = Minecraft.getInstance().font;
      int textWidth = font.width(this.getMessage());
      float scale = textWidth <= this.width - 4 ? 1.0F : (float)(this.width - 4) / (float)textWidth;
      int color = this.active && this.isHoveredOrFocused() ? -6250336 : -1;
      graphics.pose().pushPose();
      graphics.pose().translate((float)this.getX() + (float)this.width / 2.0F, (float)this.getY() + (float)this.height / 2.0F, 1.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawCenteredString(font, this.getMessage(), 0, -4, color);
      graphics.pose().popPose();
      RenderSystem.disableBlend();
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }

   @FunctionalInterface
   public interface OnPress {
      void onPress(LegacyTexturedButton var1);
   }
}
