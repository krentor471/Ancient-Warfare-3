package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * One look for the mod's own windows: a dark wooden panel with a gold-trimmed frame, a title band,
 * and flat buttons that light up gold under the mouse, in place of the grey vanilla ones.
 */
public final class AwUi {
   public static final int TEXT = 0xFFF2E8D5;
   public static final int MUTED = 0xFFA89F8E;
   public static final int GOLD = 0xFFE8C15A;
   public static final int FRAME = 0xFF8A6A2E;
   public static final int FRAME_DARK = 0xFF3A2A14;
   public static final int PANEL = 0xF0151210;
   public static final int BAND = 0xFF2A2018;
   public static final int BUTTON = 0xFF2A2018;
   public static final int BUTTON_HOVER = 0xFF4A3820;
   public static final int BUTTON_OFF = 0xFF1A1612;

   private AwUi() {
   }

   /** A framed panel: dark outer line, gold trim, dark body, small corner studs. */
   public static void panel(GuiGraphics g, int x, int y, int w, int h) {
      g.fill(x - 3, y - 3, x + w + 3, y + h + 3, FRAME_DARK);
      g.fill(x - 2, y - 2, x + w + 2, y + h + 2, FRAME);
      g.fill(x - 1, y - 1, x + w + 1, y + h + 1, FRAME_DARK);
      g.fill(x, y, x + w, y + h, PANEL);
      for (int[] c : new int[][]{{x - 3, y - 3}, {x + w - 1, y - 3}, {x - 3, y + h - 1}, {x + w - 1, y + h - 1}}) {
         g.fill(c[0], c[1], c[0] + 4, c[1] + 4, GOLD);
      }
   }

   /** The panel's title band with the title centred in gold, and a thin gold rule under it. */
   public static void title(GuiGraphics g, Font font, Component title, int x, int y, int w) {
      g.fill(x, y, x + w, y + 18, BAND);
      g.fill(x + 6, y + 18, x + w - 6, y + 19, FRAME);
      g.drawCenteredString(font, title, x + w / 2, y + 5, GOLD);
   }

   public static Builder button(Component message, Button.OnPress onPress) {
      return new Builder(message, onPress);
   }

   /** A flat button in the panel's colours. */
   public static final class AwButton extends Button {
      AwButton(int x, int y, int w, int h, Component message, OnPress onPress) {
         super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
      }

      @Override
      protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         boolean hover = this.active && this.isHoveredOrFocused();
         g.fill(x, y, x + w, y + h, hover ? GOLD : FRAME);
         g.fill(x + 1, y + 1, x + w - 1, y + h - 1, !this.active ? BUTTON_OFF : hover ? BUTTON_HOVER : BUTTON);
         g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x20FFFFFF);
         Font font = Minecraft.getInstance().font;
         int color = !this.active ? MUTED : hover ? 0xFFFFF4C8 : TEXT;
         this.renderScrollingString(g, font, 3, color);
      }
   }

   public static final class Builder {
      private final Component message;
      private final Button.OnPress onPress;
      private int x;
      private int y;
      private int width = 150;
      private int height = 20;
      private Tooltip tooltip;

      Builder(Component message, Button.OnPress onPress) {
         this.message = message;
         this.onPress = onPress;
      }

      public Builder bounds(int x, int y, int width, int height) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         return this;
      }

      public Builder pos(int x, int y) {
         this.x = x;
         this.y = y;
         return this;
      }

      public Builder size(int width, int height) {
         this.width = width;
         this.height = height;
         return this;
      }

      public Builder width(int width) {
         this.width = width;
         return this;
      }

      public Builder tooltip(Tooltip tooltip) {
         this.tooltip = tooltip;
         return this;
      }

      public Button build() {
         Button button = new AwButton(this.x, this.y, this.width, this.height, this.message, this.onPress);
         if (this.tooltip != null) {
            button.setTooltip(this.tooltip);
         }

         return button;
      }
   }
}
