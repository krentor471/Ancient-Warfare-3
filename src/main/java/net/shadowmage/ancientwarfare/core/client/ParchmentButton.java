package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A button drawn like the tabs of the diplomacy window: dark wood, a gold rim when hovered or chosen. */
public class ParchmentButton extends Button {
   private static final int TEXT = 0xFFF2E8D5;
   private static final int MUTED = 0xFFA89F8E;
   private static final int GOLD = 0xFFE8C15A;
   private static final int FRAME = 0xFF8A6A2E;
   private static final int CARD = 0xFF221C16;
   private final java.util.function.BooleanSupplier chosen;

   public ParchmentButton(int x, int y, int width, int height, Component label, OnPress press, java.util.function.BooleanSupplier chosen) {
      super(x, y, width, height, label, press, DEFAULT_NARRATION);
      this.chosen = chosen;
   }

   public ParchmentButton(int x, int y, int width, int height, Component label, OnPress press) {
      this(x, y, width, height, label, press, () -> false);
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      boolean on = this.chosen.getAsBoolean();
      boolean hover = this.isHoveredOrFocused();
      int x = this.getX();
      int y = this.getY();
      graphics.fill(x, y, x + this.width, y + this.height, on || hover ? FRAME : 0xFF3A2E1E);
      graphics.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, on ? 0xFF3A2E1E : hover ? 0xFF2C241A : CARD);
      int color = !this.active ? MUTED : on ? GOLD : hover ? 0xFFFFFFFF : TEXT;
      var font = Minecraft.getInstance().font;
      graphics.drawCenteredString(font, this.getMessage(), x + this.width / 2, y + (this.height - 8) / 2, color);
   }
}
