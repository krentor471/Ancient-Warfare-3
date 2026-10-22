package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.shadowmage.ancientwarfare.core.faction.FactionQuests;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/** A town's quest board: today's tasks with a button to take each, and the player's own tasks to hand in or give up. */
public final class QuestBoardScreen extends Screen {
   private static final int PANEL_WIDTH = 380;
   private static final int LINE_HEIGHT = 10;
   private static final int BUTTON_WIDTH = 74;
   private static final int BUTTON_HEIGHT = 18;
   private static final int PAPER = 0xFFE8DCB8;
   private static final int PAPER_EDGE = 0xFF6E5530;
   private static final int INK = 0xFF3A2A18;
   private static final int HEADING_INK = 0xFF8A2A1A;
   private static final int DETAIL_INK = 0xFF5E6A2A;
   private static final int FADED_INK = 0xFF8A7A60;

   private record Text(List<FormattedCharSequence> lines, int color, int x, int y) {
   }

   private final BlockPos board;
   private final Component note;
   private final List<FactionQuests.Line> offers;
   private final List<FactionQuests.Line> active;
   private final List<Text> texts = new ArrayList<>();
   private int left;
   private int right;
   private int top;
   private int bottom;
   private int scroll;
   private int contentHeight;

   private QuestBoardScreen(BlockPos board, Component title, Component note, List<FactionQuests.Line> offers, List<FactionQuests.Line> active) {
      super(title);
      this.board = board;
      this.note = note;
      this.offers = offers;
      this.active = active;
   }

   public static void open(BlockPos board, Component title, Component note, List<FactionQuests.Line> offers, List<FactionQuests.Line> active) {
      QuestBoardScreen screen = new QuestBoardScreen(board, title, note, offers, active);
      // A refresh after a button keeps the page where it was.
      if (Minecraft.getInstance().screen instanceof QuestBoardScreen old && old.board.equals(board)) {
         screen.scroll = old.scroll;
      }

      Minecraft.getInstance().setScreen(screen);
   }

   @Override
   protected void init() {
      this.texts.clear();
      int width = Math.min(PANEL_WIDTH, this.width - 24);
      this.left = (this.width - width) / 2;
      this.right = this.left + width;
      this.top = 22;
      int textWidth = width - 16 - BUTTON_WIDTH - 8;
      int buttonX = this.right - BUTTON_WIDTH - 6;
      int y = this.top + 6 - this.scroll;
      y = this.heading(Component.translatable("gui.ancientwarfare.quest_board.offers"), y);
      if (!this.note.getString().isEmpty()) {
         y = this.text(this.note, FADED_INK, width - 16, y) + 4;
      } else if (this.offers.isEmpty()) {
         y = this.text(Component.translatable("gui.ancientwarfare.quest_board.no_offers"), FADED_INK, width - 16, y) + 4;
      }

      boolean full = this.active.size() >= FactionQuests.MAX_TAKEN;
      for (FactionQuests.Line offer : this.offers) {
         int start = y;
         boolean open = offer.state() == FactionQuests.Line.OPEN;
         y = this.text(offer.text(), open ? INK : FADED_INK, textWidth, y);
         y = this.text(offer.detail(), DETAIL_INK, textWidth, y);
         String label = switch (offer.state()) {
            case FactionQuests.Line.TAKEN -> "gui.ancientwarfare.quest_board.taken";
            case FactionQuests.Line.DONE -> "gui.ancientwarfare.quest_board.done";
            default -> "gui.ancientwarfare.quest_board.take";
         };
         Button button = Button.builder(
               Component.translatable(label), b -> AWCoreNetwork.sendQuestAction(this.board, AWCoreNetwork.QuestActionPacket.TAKE, offer.id())
            )
            .bounds(buttonX, start, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
         button.active = open && !full;
         this.addRenderableWidget(button);
         y = Math.max(y, start + BUTTON_HEIGHT) + 6;
      }

      y += 4;
      y = this.heading(Component.translatable("gui.ancientwarfare.quest_board.mine", this.active.size(), FactionQuests.MAX_TAKEN), y);
      if (this.active.isEmpty()) {
         y = this.text(Component.translatable("gui.ancientwarfare.quest_board.no_quests"), FADED_INK, width - 16, y) + 4;
      }

      for (FactionQuests.Line quest : this.active) {
         int start = y;
         y = this.text(quest.text(), INK, textWidth, y);
         y = this.text(quest.detail(), DETAIL_INK, textWidth, y);
         int buttonY = start;
         if (quest.state() == FactionQuests.Line.ACTIVE_HERE) {
            this.addRenderableWidget(
               Button.builder(
                     Component.translatable("gui.ancientwarfare.quest_board.turn_in"),
                     b -> AWCoreNetwork.sendQuestAction(this.board, AWCoreNetwork.QuestActionPacket.TURN_IN, quest.id())
                  )
                  .bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build()
            );
            buttonY += BUTTON_HEIGHT + 2;
         }

         this.addRenderableWidget(
            Button.builder(
                  Component.translatable("gui.ancientwarfare.quest_board.abandon"),
                  b -> AWCoreNetwork.sendQuestAction(this.board, AWCoreNetwork.QuestActionPacket.ABANDON, quest.id())
               )
               .bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
               .build()
         );
         y = Math.max(y, buttonY + BUTTON_HEIGHT) + 6;
      }

      this.contentHeight = y + 2 + this.scroll - this.top;
      this.bottom = Math.min(y + 2, this.height - 6);
      int maxScroll = Math.max(0, this.contentHeight - (this.height - 6 - this.top));
      if (this.scroll > maxScroll) {
         this.scroll = maxScroll;
         this.rebuildWidgets();
         return;
      }

      // Buttons scrolled out of the page are hidden.
      for (var child : this.children()) {
         if (child instanceof Button button) {
            button.visible = button.getY() >= this.top && button.getY() + BUTTON_HEIGHT <= this.bottom;
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int maxScroll = Math.max(0, this.contentHeight - (this.height - 6 - this.top));
      int next = Math.max(0, Math.min(maxScroll, this.scroll - (int)Math.signum(delta) * 20));
      if (next != this.scroll) {
         this.scroll = next;
         this.rebuildWidgets();
      }

      return true;
   }

   private int heading(Component text, int y) {
      this.texts.add(new Text(List.of(text.getVisualOrderText()), HEADING_INK, this.left + 8, y));
      return y + LINE_HEIGHT + 4;
   }

   private int text(Component text, int color, int width, int y) {
      List<FormattedCharSequence> lines = this.font.split(text, width);
      this.texts.add(new Text(lines, color, this.left + 8, y));
      return y + lines.size() * LINE_HEIGHT;
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      graphics.fill(this.left - 3, this.top - 3, this.right + 3, this.bottom + 3, PAPER_EDGE);
      graphics.fill(this.left, this.top, this.right, this.bottom, PAPER);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, -1);
      graphics.enableScissor(this.left, this.top, this.right, this.bottom);
      for (Text text : this.texts) {
         int y = text.y();
         for (FormattedCharSequence line : text.lines()) {
            graphics.drawString(this.font, line, text.x(), y, text.color(), false);
            y += LINE_HEIGHT;
         }
      }

      graphics.disableScissor();
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
