package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class LegacyArrowButton extends AbstractButton {
   private final boolean right;
   private final LegacyArrowButton.OnPress onPress;

   public LegacyArrowButton(int x, int y, boolean right, LegacyArrowButton.OnPress onPress) {
      super(x, y, 10, 16, Component.translatable(right ? "gui.next" : "gui.previous"));
      this.right = right;
      this.onPress = onPress;
   }

   public void onPress() {
      this.onPress.onPress(this);
   }

   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      LegacyGuiStyle.arrow(graphics, this.getX(), this.getY(), 0, 0, this.right);
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }

   @FunctionalInterface
   public interface OnPress {
      void onPress(LegacyArrowButton var1);
   }
}
