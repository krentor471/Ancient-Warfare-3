package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/** The chronicle of the world: its news day by day, newest first, opened from the world map. */
public final class ChronicleScreen extends Screen {
   private static final int MARGIN = 24;
   private static final int LINE_HEIGHT = 10;
   private static final int PAPER = 0xFFE8DCB8;
   private static final int PAPER_EDGE = 0xFF6E5530;
   private static final int INK = 0xFF3A2A18;
   private static final int DAY_INK = 0xFF8A2A1A;

   public static record Entry(long day, Component text) {
   }

   private final List<Entry> entries;
   /** Wrapped lines with their colours, rebuilt for the screen width. */
   private final List<FormattedCharSequence> lines = new ArrayList<>();
   private final List<Integer> lineColors = new ArrayList<>();
   private int scroll;

   private ChronicleScreen(List<Entry> entries) {
      super(Component.translatable("gui.ancientwarfare.chronicle.title"));
      this.entries = entries;
   }

   public static void open(List<Entry> entries) {
      Minecraft.getInstance().setScreen(new ChronicleScreen(entries));
   }

   @Override
   protected void init() {
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.chronicle.back"), button -> AWCoreNetwork.sendWorldMapOpen()).bounds(MARGIN, 2, 70, 18).build()
      );
      this.lines.clear();
      this.lineColors.clear();
      int width = this.width - MARGIN * 2 - 16;
      long day = Long.MIN_VALUE;

      for (int i = this.entries.size() - 1; i >= 0; i--) {
         Entry entry = this.entries.get(i);
         if (entry.day() != day) {
            if (day != Long.MIN_VALUE) {
               this.add(FormattedCharSequence.EMPTY, INK);
            }

            day = entry.day();
            this.add(Component.translatable("gui.ancientwarfare.chronicle.day", day).getVisualOrderText(), DAY_INK);
         }

         for (FormattedCharSequence line : this.font.split(entry.text(), width)) {
            this.add(line, INK);
         }
      }

      if (this.entries.isEmpty()) {
         this.add(Component.translatable("gui.ancientwarfare.chronicle.empty").getVisualOrderText(), INK);
      }

      this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
   }

   private void add(FormattedCharSequence line, int color) {
      this.lines.add(line);
      this.lineColors.add(color);
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
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, -1);
      int y = top + 8;
      int end = Math.min(this.lines.size(), this.scroll + this.visibleLines());

      for (int i = this.scroll; i < end; i++) {
         graphics.drawString(this.font, this.lines.get(i), left + 8, y, this.lineColors.get(i), false);
         y += LINE_HEIGHT;
      }

      if (this.maxScroll() > 0) {
         // A thin bar along the right edge shows where in the chronicle the page is.
         int track = bottom - top - 8;
         int thumb = Math.max(10, track * this.visibleLines() / this.lines.size());
         int at = top + 4 + (track - thumb) * this.scroll / this.maxScroll();
         graphics.fill(right - 5, at, right - 2, at + thumb, PAPER_EDGE);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
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
