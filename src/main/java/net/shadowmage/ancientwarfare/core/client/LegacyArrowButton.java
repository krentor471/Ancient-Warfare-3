package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class LegacyArrowButton extends AbstractButton {
   private final boolean right;
   private final LegacyArrowButton.OnPress onPress;

   public LegacyArrowButton(int x, int y, boolean right, LegacyArrowButton.OnPress onPress) {
      super(x, y, 10, 16, Component.m_237115_(right ? "gui.next" : "gui.previous"));
      this.right = right;
      this.onPress = onPress;
   }

   public void m_5691_() {
      this.onPress.onPress(this);
   }

   protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      LegacyGuiStyle.arrow(graphics, this.m_252754_(), this.m_252907_(), 0, 0, this.right);
   }

   protected void m_168797_(NarrationElementOutput output) {
      this.m_168802_(output);
   }

   @FunctionalInterface
   public interface OnPress {
      void onPress(LegacyArrowButton var1);
   }
}
