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

   public void m_5691_() {
      this.checked = !this.checked;
      this.onToggle.onToggle(this.checked);
   }

   public void setChecked(boolean checked) {
      this.checked = checked;
   }

   protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      RenderSystem.enableBlend();
      int stateY = this.f_93623_ ? (this.m_198029_() ? 200 : 160) : 120;
      LegacyGuiStyle.quarteredTexture(
         graphics, LegacyGuiStyle.WIDGETS, this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_, this.checked ? 40 : 0, stateY, 40, 40
      );
      graphics.m_280614_(
         Minecraft.m_91087_().f_91062_, this.m_6035_(), this.m_252754_() + this.f_93618_ + 2, this.m_252907_() + Math.max(0, (this.f_93619_ - 8) / 2), -1, true
      );
      RenderSystem.disableBlend();
   }

   protected void m_168797_(NarrationElementOutput output) {
      this.m_168802_(output);
   }

   @FunctionalInterface
   public interface OnToggle {
      void onToggle(boolean var1);
   }
}
