package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class LegacyCheckbox extends AbstractButton {
   private boolean checked;
   private final LegacyCheckbox.OnToggle onToggle;

   public LegacyCheckbox(int x, int y, int size, Component label, boolean checked, LegacyCheckbox.OnToggle onToggle) {
      super(x, y, size, size, label);
      this.checked = checked;
      this.onToggle = onToggle;
   }

   public void onPress() {
      this.checked = !this.checked;
      this.onToggle.onToggle(this.checked);
   }

   public void setChecked(boolean checked) {
      this.checked = checked;
   }

   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      RenderSystem.enableBlend();
      int stateY = this.active ? (this.isHoveredOrFocused() ? 200 : 160) : 120;
      LegacyGuiStyle.quarteredTexture(
         graphics, LegacyGuiStyle.WIDGETS, this.getX(), this.getY(), this.width, this.height, this.checked ? 40 : 0, stateY, 40, 40
      );
      graphics.drawString(
         Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width + 2, this.getY() + Math.max(0, (this.height - 8) / 2), -1, true
      );
      RenderSystem.disableBlend();
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }

   @FunctionalInterface
   public interface OnToggle {
      void onToggle(boolean var1);
   }
}
