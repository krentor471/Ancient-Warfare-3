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
      Font font = Minecraft.m_91087_().f_91062_;
      if (font.m_92852_(message) > width - 4) {
         this.m_257544_(Tooltip.m_257550_(message));
      }
   }

   public void m_5691_() {
      this.onPress.onPress(this);
   }

   protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      RenderSystem.enableBlend();
      int stateY = this.f_93623_ ? (this.m_198029_() ? 80 : 40) : 0;
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_, 0, stateY, 256, 40);
      Font font = Minecraft.m_91087_().f_91062_;
      int textWidth = font.m_92852_(this.m_6035_());
      float scale = textWidth <= this.f_93618_ - 4 ? 1.0F : (float)(this.f_93618_ - 4) / (float)textWidth;
      int color = this.f_93623_ && this.m_198029_() ? -6250336 : -1;
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)this.m_252754_() + (float)this.f_93618_ / 2.0F, (float)this.m_252907_() + (float)this.f_93619_ / 2.0F, 1.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280653_(font, this.m_6035_(), 0, -4, color);
      graphics.m_280168_().m_85849_();
      RenderSystem.disableBlend();
   }

   protected void m_168797_(NarrationElementOutput output) {
      this.m_168802_(output);
   }

   @FunctionalInterface
   public interface OnPress {
      void onPress(LegacyTexturedButton var1);
   }
}
