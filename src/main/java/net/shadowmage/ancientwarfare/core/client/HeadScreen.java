package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * The village head's window: a framed panel in the middle of the screen with what he says on top
 * and every choice he offers as a real button below it, two to a row, with its hint on hover.
 */
public final class HeadScreen extends Screen {
   private static final int WIDTH = 340;
   private static final int FRAME = 0xFF6E5530;
   private static final int BACK = 0xF0201810;
   private static final int TITLE_BAND = 0xFF3A2A18;
   private static final int TEXT = 0xFFE8DCB8;
   private static final int BUTTON_HEIGHT = 18;

   private record Choice(Component label, String command, Component hint) {
   }

   private final List<Component> source;
   private final List<FormattedCharSequence> text = new ArrayList<>();
   private final List<Choice> choices = new ArrayList<>();
   private int top;
   private int panelHeight;

   private HeadScreen(Component title, List<Component> lines) {
      super(title);
      this.source = lines;
   }

   public static void open(Component title, List<Component> lines) {
      Minecraft.getInstance().setScreen(new HeadScreen(title, lines));
   }

   @Override
   protected void init() {
      this.text.clear();
      this.choices.clear();
      int inner = WIDTH - 20;
      for (Component line : this.source) {
         MutableComponent words = Component.empty();
         Map<String, Choice> found = new LinkedHashMap<>();
         Map<String, MutableComponent> labels = new LinkedHashMap<>();
         line.visit((style, piece) -> {
            ClickEvent click = style.getClickEvent();
            if (click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND) {
               labels.computeIfAbsent(click.getValue(), key -> Component.empty()).append(Component.literal(piece).withStyle(style.withClickEvent(null).withHoverEvent(null)));
               HoverEvent hover = style.getHoverEvent();
               Component hint = hover != null && hover.getAction() == HoverEvent.Action.SHOW_TEXT ? hover.getValue(HoverEvent.Action.SHOW_TEXT) : null;
               found.putIfAbsent(click.getValue(), new Choice(Component.empty(), click.getValue(), hint));
            } else {
               words.append(Component.literal(piece).withStyle(style));
            }

            return Optional.empty();
         }, Style.EMPTY);
         String plain = words.getString().trim();
         if (!plain.isEmpty() && !plain.equals("-")) {
            this.text.addAll(this.font.split(words, inner));
         }

         for (Choice choice : found.values()) {
            String label = labels.get(choice.command()).getString().trim();
            if (label.startsWith("[") && label.endsWith("]")) {
               label = label.substring(1, label.length() - 1);
            }

            this.choices.add(new Choice(Component.literal(label).withStyle(labels.get(choice.command()).getStyle()), choice.command(), choice.hint()));
         }
      }

      int rows = (this.choices.size() + 1) / 2;
      this.panelHeight = 28 + this.text.size() * 10 + 8 + rows * (BUTTON_HEIGHT + 3) + 26;
      this.top = Math.max(4, (this.height - this.panelHeight) / 2);
      int left = (this.width - WIDTH) / 2;
      int y = this.top + 28 + this.text.size() * 10 + 8;
      int half = (WIDTH - 24) / 2;
      for (int i = 0; i < this.choices.size(); i++) {
         Choice choice = this.choices.get(i);
         int bx = left + 10 + (i % 2) * (half + 4);
         int by = y + (i / 2) * (BUTTON_HEIGHT + 3);
         Component label = this.font.width(choice.label()) > half - 8
            ? Component.literal(this.font.plainSubstrByWidth(choice.label().getString(), half - 14) + "…")
            : choice.label();
         Button button = Button.builder(label, pressed -> {
            LetterLog.prompted();
            this.onClose();
            String command = choice.command().startsWith("/") ? choice.command().substring(1) : choice.command();
            if (Minecraft.getInstance().player != null) {
               Minecraft.getInstance().player.connection.sendCommand(command);
            }
         }).bounds(bx, by, half, BUTTON_HEIGHT).build();
         if (choice.hint() != null) {
            button.setTooltip(Tooltip.create(Component.empty().append(choice.label()).append("\n").append(choice.hint())));
         } else if (label != choice.label()) {
            button.setTooltip(Tooltip.create(choice.label()));
         }

         this.addRenderableWidget(button);
      }

      this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), pressed -> this.onClose())
         .bounds(left + WIDTH / 2 - 40, this.top + this.panelHeight - 22, 80, BUTTON_HEIGHT).build());
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = (this.width - WIDTH) / 2;
      graphics.fill(left - 2, this.top - 2, left + WIDTH + 2, this.top + this.panelHeight + 2, FRAME);
      graphics.fill(left, this.top, left + WIDTH, this.top + this.panelHeight, BACK);
      graphics.fill(left, this.top, left + WIDTH, this.top + 20, TITLE_BAND);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, this.top + 6, 0xFFF2C14E);
      int y = this.top + 28;
      for (FormattedCharSequence line : this.text) {
         graphics.drawString(this.font, line, left + 10, y, TEXT, false);
         y += 10;
      }

      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
