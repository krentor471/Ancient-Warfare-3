package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.faction.FactionDeals;
import net.shadowmage.ancientwarfare.core.faction.RelationState;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/** A deal with a faction's ruler: what each side puts on the scales, and the ruler's answer. */
public class DealScreen extends Screen {
   private static final int PANEL_WIDTH = 340;
   private static final int PANEL_HEIGHT = 254;
   private static final int COLUMN = 155;
   private static final int ROW = 22;
   private static final int TEXT_COLOR = -1;
   private static final int MUTED_COLOR = -4276546;
   private FactionDeals.View view;
   private int playerGold;
   private int tribute;
   private String playerWar = "";
   private int factionGold;
   private boolean treaty;
   private boolean trade;
   private String factionWar = "";
   /** The ruler's answer, until the offer changes. */
   private int verdict = FactionDeals.NONE;
   private Component answer = Component.empty();

   private DealScreen(FactionDeals.View view) {
      super(Component.translatable("gui.ancientwarfare.deal.title", DiplomacyScreen.factionName(view.faction())));
      this.view = view;
   }

   public static void accept(boolean open, FactionDeals.View view) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof DealScreen screen && screen.view.faction().equals(view.faction())) {
         screen.update(view);
      } else if (open) {
         minecraft.setScreen(new DealScreen(view));
      }
   }

   private void update(FactionDeals.View view) {
      this.view = view;
      FactionDeals.Offer offer = view.offer();
      this.playerGold = offer.playerGold();
      this.tribute = offer.tribute();
      this.playerWar = offer.playerWar();
      this.factionGold = offer.factionGold();
      this.treaty = offer.treaty();
      this.trade = offer.trade();
      this.factionWar = offer.factionWar();
      this.verdict = view.verdict();
      this.answer = view.answer();
      this.rebuildWidgets();
   }

   private FactionDeals.Offer offer() {
      return new FactionDeals.Offer(this.playerGold, this.tribute, this.playerWar, this.factionGold, this.treaty, this.trade, this.factionWar);
   }

   /** The offer changed: the last answer no longer holds. */
   private void changed() {
      this.verdict = FactionDeals.NONE;
      this.answer = Component.empty();
      this.rebuildWidgets();
   }

   @Override
   protected void init() {
      int left = (this.width - PANEL_WIDTH) / 2;
      int top = (this.height - PANEL_HEIGHT) / 2;
      int mine = left + 10;
      int theirs = left + PANEL_WIDTH - 10 - COLUMN;
      int y = top + 38;
      this.stepper(mine, y, () -> this.playerGold = Math.max(0, this.playerGold - FactionDeals.GOLD_STEP), () -> this.playerGold += FactionDeals.GOLD_STEP);
      this.stepper(mine, y + ROW, () -> this.tribute = Math.max(0, this.tribute - FactionDeals.TRIBUTE_STEP), () -> this.tribute += FactionDeals.TRIBUTE_STEP);
      this.cycle(mine, y + ROW * 2, this.view.playerTargets(), this.playerWar, next -> this.playerWar = next);
      this.stepper(theirs, y, () -> this.factionGold = Math.max(0, this.factionGold - FactionDeals.GOLD_STEP), () -> this.factionGold += FactionDeals.GOLD_STEP);
      RelationState state = this.view.treaty();
      Button treatyButton = Button.builder(this.check(this.treaty, Component.translatable("gui.ancientwarfare.deal.treaty." + state.serializedName())), ignored -> {
         this.treaty = !this.treaty;
         this.changed();
      }).bounds(theirs, y + ROW, COLUMN, 20).build();
      treatyButton.active = state != RelationState.ALLIANCE;
      this.addRenderableWidget(treatyButton);
      Button tradeButton = Button.builder(this.check(this.trade, Component.translatable("gui.ancientwarfare.deal.trade")), ignored -> {
         this.trade = !this.trade;
         this.changed();
      }).bounds(theirs, y + ROW * 2, COLUMN, 20).build();
      tradeButton.active = !this.view.trade();
      this.addRenderableWidget(tradeButton);
      this.cycle(theirs, y + ROW * 3, this.view.factionTargets(), this.factionWar, next -> this.factionWar = next);
      int bottom = top + PANEL_HEIGHT - 28;
      int width = (PANEL_WIDTH - 24) / 3;
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.deal.propose"), ignored -> AWCoreNetwork.sendDeal(this.view.faction(), 1, this.offer()))
            .bounds(left + 8, bottom, width, 20)
            .build()
      );
      Button strike = Button.builder(Component.translatable("gui.ancientwarfare.deal.strike"), ignored -> AWCoreNetwork.sendDeal(this.view.faction(), 2, this.offer()))
         .bounds(left + 12 + width, bottom, width, 20)
         .build();
      strike.active = this.verdict == FactionDeals.ACCEPT;
      this.addRenderableWidget(strike);
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.deal.back"), ignored -> AWCoreNetwork.sendDiplomacyOpen()).bounds(left + 16 + width * 2, bottom, width, 20).build()
      );
   }

   private Component check(boolean on, Component label) {
      return Component.literal(on ? "[✔] " : "[  ] ").append(label);
   }

   /** Minus and plus buttons at the right of a row whose text render() draws. */
   private void stepper(int x, int y, Runnable less, Runnable more) {
      this.addRenderableWidget(Button.builder(Component.literal("-"), ignored -> {
         less.run();
         this.changed();
      }).bounds(x + COLUMN - 42, y, 20, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("+"), ignored -> {
         more.run();
         this.changed();
      }).bounds(x + COLUMN - 20, y, 20, 20).build());
   }

   /** A button going round the possible enemies, and none. */
   private void cycle(int x, int y, List<String> targets, String current, java.util.function.Consumer<String> set) {
      Component target = current.isEmpty() ? Component.translatable("gui.ancientwarfare.deal.nobody") : DiplomacyScreen.factionName(current);
      Button button = Button.builder(Component.translatable("gui.ancientwarfare.deal.war", target), ignored -> {
         int at = targets.indexOf(current);
         set.accept(at + 1 < targets.size() ? targets.get(at + 1) : "");
         this.changed();
      }).bounds(x, y, COLUMN, 20).build();
      button.active = !targets.isEmpty();
      button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
         targets.isEmpty() ? "gui.ancientwarfare.deal.war_none" : x < this.width / 2 ? "gui.ancientwarfare.deal.war_mine.tip" : "gui.ancientwarfare.deal.war_theirs.tip",
         FactionDeals.WAR_PROMISE_DAYS
      )));
      this.addRenderableWidget(button);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = (this.width - PANEL_WIDTH) / 2;
      int top = (this.height - PANEL_HEIGHT) / 2;
      int mine = left + 10;
      int theirs = left + PANEL_WIDTH - 10 - COLUMN;
      graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101010);
      graphics.fill(left + PANEL_WIDTH / 2, top + 22, left + PANEL_WIDTH / 2 + 1, top + 38 + ROW * 4, 0xFF505050);
      graphics.drawCenteredString(this.font, this.title, left + PANEL_WIDTH / 2, top + 8, TEXT_COLOR);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.mine").withStyle(ChatFormatting.BOLD), mine, top + 24, 0xFFFFD27F);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.theirs").withStyle(ChatFormatting.BOLD), theirs, top + 24, 0xFFFFD27F);
      int y = top + 44;
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.gold", this.playerGold), mine, y, TEXT_COLOR);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.tribute", this.tribute, FactionDeals.TRIBUTE_WEEKS), mine, y + ROW, TEXT_COLOR);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.gold", this.factionGold), theirs, y, TEXT_COLOR);
      y = top + 38 + ROW * 4 + 6;
      int word = this.view.reliability();
      Component wordName = Component.translatable(
            "gui.ancientwarfare.deal.word." + (word <= FactionDeals.NO_TRUST ? "none" : word < 0 ? "doubtful" : word == 0 ? "untried" : word < 3 ? "good" : "iron")
         )
         .withStyle(word < 0 ? ChatFormatting.RED : word > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.word", wordName), mine, y, TEXT_COLOR);
      y += 12;
      if (this.view.promises().isEmpty()) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.promises_none"), mine, y, MUTED_COLOR);
      }

      for (Component promise : this.view.promises().subList(0, Math.min(3, this.view.promises().size()))) {
         graphics.drawString(this.font, Component.literal("• ").append(promise), mine, y, 0xFF9FD7FF);
         y += 10;
      }

      int color = switch (this.verdict) {
         case FactionDeals.ACCEPT, FactionDeals.DONE -> 0xFF80FF80;
         case FactionDeals.MORE -> 0xFFFFD27F;
         case FactionDeals.REFUSE -> 0xFFFF8080;
         default -> MUTED_COLOR;
      };
      Component shown = this.verdict == FactionDeals.NONE ? Component.translatable("gui.ancientwarfare.deal.hint") : this.answer;
      graphics.drawWordWrap(this.font, shown, mine, top + PANEL_HEIGHT - 58, PANEL_WIDTH - 20, color);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
