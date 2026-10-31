package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/** The letters page: envoys' and unbidden offers waiting on an answer, newest first; also the village head's page. */
public final class LetterScreen extends Screen {
   private static final int MARGIN = 24;
   private static final int LINE_HEIGHT = 10;
   private static final int PAPER = 0xFFE8DCB8;
   private static final int PAPER_EDGE = 0xFF6E5530;
   private static final int INK = 0xFF3A2A18;
   private static final int OLD_INK = 0xFF8C7C62;
   private static final int RULE = 0xFFB8A27A;

   private LetterLog.Kind kind;
   /** The village head's page: his lines alone, no letters. */
   private final List<Component> panel;
   private final List<FormattedCharSequence> lines = new ArrayList<>();
   private final List<Integer> colors = new ArrayList<>();
   private int scroll;

   private LetterScreen(LetterLog.Kind kind) {
      super(Component.translatable("gui.ancientwarfare.letters.title"));
      this.kind = kind;
      this.panel = null;
   }

   private LetterScreen(Component title, List<Component> panel) {
      super(title);
      this.kind = LetterLog.Kind.CHOICE;
      this.panel = panel;
   }

   public static void openPanel(Component title, List<Component> lines) {
      Minecraft.getInstance().setScreen(new LetterScreen(title, lines));
   }

   public static void open(LetterLog.Kind kind) {
      Minecraft.getInstance().setScreen(new LetterScreen(kind));
   }

   @Override
   protected void init() {
      this.clearWidgets();
      int x = MARGIN;
      if (this.panel != null) {
         this.lines.clear();
         this.colors.clear();
         for (Component line : this.panel) {
            for (FormattedCharSequence part : this.font.split(line, this.width - MARGIN * 2 - 16)) {
               this.lines.add(part);
               this.colors.add(INK);
            }
         }

         this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
         return;
      }

      for (LetterLog.Kind tab : LetterLog.Kind.values()) {
         long unread = LetterLog.letters().stream().filter(letter -> letter.kind == tab && !letter.read).count();
         Component label = Component.translatable("gui.ancientwarfare.letters." + tab.name().toLowerCase(java.util.Locale.ROOT));
         if (unread > 0) {
            label = label.copy().append(" (" + unread + ")");
         }

         if (tab == this.kind) {
            label = Component.literal("» ").append(label).append(" «").withStyle(net.minecraft.ChatFormatting.YELLOW);
         }

         Button button = Button.builder(label, pressed -> {
            this.kind = tab;
            this.scroll = 0;
            this.init();
         }).bounds(x, 2, 110, 18).build();
         this.addRenderableWidget(button);
         x += 114;
      }

      this.lines.clear();
      this.colors.clear();
      int width = this.width - MARGIN * 2 - 16;
      List<LetterLog.Letter> letters = LetterLog.letters();
      for (int i = letters.size() - 1; i >= 0; i--) {
         LetterLog.Letter letter = letters.get(i);
         if (letter.kind != this.kind) {
            continue;
         }

         int color = letter.read ? OLD_INK : INK;
         for (Component line : letter.lines) {
            for (FormattedCharSequence part : this.font.split(line, width)) {
               this.lines.add(part);
               this.colors.add(color);
            }
         }

         this.lines.add(null);
         this.colors.add(RULE);
      }

      if (this.lines.isEmpty()) {
         this.lines.add(Component.translatable("gui.ancientwarfare.letters.empty").getVisualOrderText());
         this.colors.add(OLD_INK);
      }

      this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
   }

   private int visibleLines() {
      return Math.max(1, (this.height - MARGIN - 8 - 16) / LINE_HEIGHT);
   }

   private int maxScroll() {
      return Math.max(0, this.lines.size() - this.visibleLines());
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = MARGIN;
      int top = MARGIN;
      int right = this.width - MARGIN;
      int bottom = this.height - 8;
      graphics.fill(left - 3, top - 3, right + 3, bottom + 3, PAPER_EDGE);
      graphics.fill(left, top, right, bottom, PAPER);
      if (this.panel != null) {
         graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, -1);
      }

      int y = top + 8;
      int end = Math.min(this.lines.size(), this.scroll + this.visibleLines());
      for (int i = this.scroll; i < end; i++) {
         if (this.lines.get(i) == null) {
            graphics.fill(left + 8, y + 4, right - 8, y + 5, RULE);
         } else {
            graphics.drawString(this.font, this.lines.get(i), left + 8, y, this.colors.get(i), false);
         }

         y += LINE_HEIGHT;
      }

      Style hovered = this.styleAt(mouseX, mouseY);
      if (hovered != null && hovered.getHoverEvent() != null) {
         graphics.renderComponentHoverEffect(this.font, hovered, mouseX, mouseY);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private Style styleAt(double mouseX, double mouseY) {
      int row = (int)((mouseY - MARGIN - 8) / LINE_HEIGHT);
      if (mouseY < MARGIN + 8 || row < 0 || row >= this.visibleLines()) {
         return null;
      }

      int index = this.scroll + row;
      if (index >= this.lines.size() || this.lines.get(index) == null) {
         return null;
      }

      return this.font.getSplitter().componentStyleAtWidth(this.lines.get(index), (int)mouseX - MARGIN - 8);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      Style style = this.styleAt(mouseX, mouseY);
      if (button == 0 && style != null && style.getClickEvent() != null) {
         this.markRead();
         LetterLog.prompted();
         this.onClose();
         this.handleComponentClicked(style);
         return true;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void markRead() {
      LetterLog.letters().stream().filter(letter -> letter.kind == this.kind).forEach(letter -> letter.read = true);
   }

   @Override
   public void onClose() {
      this.markRead();
      super.onClose();
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      this.scroll = Mth.clamp(this.scroll - (int)Math.signum(delta) * 3, 0, this.maxScroll());
      return true;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
