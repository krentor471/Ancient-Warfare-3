package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.npc.CoinMetal;
import net.shadowmage.ancientwarfare.core.faction.FactionColors;
import net.shadowmage.ancientwarfare.core.faction.FactionCourt;
import net.shadowmage.ancientwarfare.core.faction.FactionDeals;
import net.shadowmage.ancientwarfare.core.faction.FactionRulers;
import net.shadowmage.ancientwarfare.core.faction.FactionSpies;
import net.shadowmage.ancientwarfare.core.faction.FactionTournaments;
import net.shadowmage.ancientwarfare.core.faction.PlayerDiplomacy;
import net.shadowmage.ancientwarfare.core.faction.RelationState;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;
import net.shadowmage.ancientwarfare.core.item.CoinItem;
import net.shadowmage.ancientwarfare.core.research.PlayerResearch;

/**
 * The player's relations with the factions they have met. On the left the factions, each in its
 * colours with how it thinks of the player; on the right the chosen faction in three tabs: an
 * overview with what the player can do, its court and what it wants, and a deal with its ruler.
 */
public class DiplomacyScreen extends Screen {
   private static final int PANEL_WIDTH = 420;
   private static final int MAX_HEIGHT = 272;
   private static final int DEAL_ROW = 17;
   private static final int LIST_WIDTH = 124;
   private static final int ROW_HEIGHT = 22;
   private static final int VISIBLE_ROWS = 10;
   private static final int TEXT_COLOR = 0xFFF2E8D5;
   private static final int MUTED_COLOR = 0xFFA89F8E;
   private static final int GOLD_COLOR = 0xFFE8C15A;
   private static final int FRAME_COLOR = 0xFF8A6A2E;
   private static final int WOOD_COLOR = 0xFF2A2018;
   private static final int PAPER_COLOR = 0xF0151210;
   private static final int CARD_COLOR = 0xFF221C16;
   private static final int GOOD_COLOR = 0xFF7FD67F;
   private static final int BAD_COLOR = 0xFFE57070;
   private static final int OVERVIEW = 0;
   private static final int COURT = 1;
   private static final int DEAL = 2;
   private static final int INTRIGUE = 3;
   private static final int TIES = 4;
   private static final String[] TABS = {"overview", "court", "deal", "intrigue", "ties"};
   private static final int INTRIGUE_ROW = 26;
   /** The tab shown, kept between openings. */
   private static int tab;
   private List<PlayerDiplomacy.Entry> entries;
   private String selected;
   private int scroll;
   private FactionDeals.View deal;
   private int playerGold;
   private int tribute;
   private String playerWar = "";
   private int factionGold;
   private boolean treaty;
   private boolean trade;
   private String factionWar = "";
   private int[] goods = new int[FactionDeals.GOODS];
   /** The ruler's answer, until the offer changes. */
   private int verdict = FactionDeals.NONE;
   private Component answer = Component.empty();

   private DiplomacyScreen(String focus, List<PlayerDiplomacy.Entry> entries) {
      super(Component.translatable("gui.ancientwarfare.diplomacy.title"));
      this.entries = entries;
      this.selected = focus;
   }

   /** Called with fresh data from the server: opens the screen, or refreshes it when already open. */
   public static void accept(String focus, boolean open, List<PlayerDiplomacy.Entry> entries) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof DiplomacyScreen screen) {
         screen.entries = entries;
         if (!focus.isEmpty()) {
            screen.selected = focus;
         }

         screen.rebuildWidgets();
      } else if (open) {
         minecraft.setScreen(new DiplomacyScreen(focus, entries));
      }
   }

   /** The deal tab's data, with the ruler's answer to the last offer. */
   public static void acceptDeal(FactionDeals.View view) {
      if (Minecraft.getInstance().screen instanceof DiplomacyScreen screen && view.faction().equals(screen.selected)) {
         screen.deal = view;
         FactionDeals.Offer offer = view.offer();
         screen.playerGold = offer.playerGold();
         screen.tribute = offer.tribute();
         screen.playerWar = offer.playerWar();
         screen.factionGold = offer.factionGold();
         screen.treaty = offer.treaty();
         screen.trade = offer.trade();
         screen.factionWar = offer.factionWar();
         screen.goods = offer.goods().clone();
         screen.verdict = view.verdict();
         screen.answer = view.answer();
         screen.rebuildWidgets();
      }
   }

   private void select(String faction) {
      this.selected = faction;
      this.deal = null;
      this.resetOffer();
      this.rebuildWidgets();
      if (tab == DEAL) {
         this.requestDeal();
      }
   }

   private void resetOffer() {
      this.playerGold = 0;
      this.tribute = 0;
      this.playerWar = "";
      this.factionGold = 0;
      this.treaty = false;
      this.trade = false;
      this.factionWar = "";
      this.goods = new int[FactionDeals.GOODS];
      this.verdict = FactionDeals.NONE;
      this.answer = Component.empty();
   }

   private void requestDeal() {
      if (this.selected != null) {
         AWCoreNetwork.sendDeal(this.selected, 0, FactionDeals.Offer.EMPTY);
      }
   }

   private FactionDeals.Offer offer() {
      return new FactionDeals.Offer(this.playerGold, this.tribute, this.playerWar, this.factionGold, this.treaty, this.trade, this.factionWar, this.goods);
   }

   /** The offer changed: the last answer no longer holds. */
   private void changed() {
      this.verdict = FactionDeals.NONE;
      this.answer = Component.empty();
      this.rebuildWidgets();
   }

   // ---------------------------------------------------------------- layout

   private int left() {
      return (this.width - PANEL_WIDTH) / 2;
   }

   private int panelHeight() {
      return Math.min(MAX_HEIGHT, this.height - 4);
   }

   private int top() {
      return Math.max(2, (this.height - this.panelHeight()) / 2);
   }

   private int contentX() {
      return this.left() + LIST_WIDTH + 14;
   }

   private int contentWidth() {
      return PANEL_WIDTH - LIST_WIDTH - 22;
   }

   /** Where a tab's own content begins, below the faction's header and the tabs. */
   private int contentY() {
      return this.top() + 66;
   }

   @Override
   protected void init() {
      if (this.selectedEntry() == null && !this.entries.isEmpty()) {
         this.selected = this.entries.get(0).faction();
      }

      this.scroll = Math.max(0, Math.min(this.scroll, this.entries.size() - VISIBLE_ROWS));
      PlayerDiplomacy.Entry entry = this.selectedEntry();
      if (entry == null) {
         return;
      }

      if (tab == DEAL && this.deal == null) {
         this.requestDeal();
      }

      switch (tab) {
         case COURT -> this.initCourt(entry);
         case DEAL -> this.initDeal(entry);
         case INTRIGUE -> this.initIntrigue(entry);
         case TIES -> {
         }
         default -> this.initOverview(entry);
      }
   }

   private void initOverview(PlayerDiplomacy.Entry entry) {
      int x = this.contentX();
      int width = (this.contentWidth() - 4) / 3;
      int y = this.top() + this.panelHeight() - 8 - 3 * 21;
      PlayerDiplomacy.Action[][] grid = {
         {PlayerDiplomacy.Action.PEACE, PlayerDiplomacy.Action.PACT, PlayerDiplomacy.Action.ALLIANCE},
         {PlayerDiplomacy.Action.TRADE, PlayerDiplomacy.Action.WAR, PlayerDiplomacy.Action.TOURNAMENT},
         {PlayerDiplomacy.Action.GIFT, PlayerDiplomacy.Action.SUPPLY, PlayerDiplomacy.Action.MEDIATE}
      };
      for (int row = 0; row < grid.length; row++) {
         for (int column = 0; column < grid[row].length; column++) {
            PlayerDiplomacy.Action action = grid[row][column];
            this.addAction(entry, action, x + column * (width + 2), y + row * 21, grid[row].length == 1 ? width * 2 + 2 : width, applicable(entry, action));
         }
      }
   }

   private static boolean applicable(PlayerDiplomacy.Entry entry, PlayerDiplomacy.Action action) {
      return switch (action) {
         case PEACE -> entry.treaty() == RelationState.WAR;
         case PACT -> entry.treaty() == RelationState.NEUTRAL;
         case ALLIANCE -> entry.treaty() == RelationState.PACT;
         case TRADE -> entry.trade() || entry.treaty() != RelationState.WAR;
         case WAR, TOURNAMENT, SUPPLY -> entry.treaty() != RelationState.WAR;
         case MEDIATE -> entry.treaty() != RelationState.WAR && !entry.wars().isEmpty();
         default -> true;
      };
   }

   private void addAction(PlayerDiplomacy.Entry entry, PlayerDiplomacy.Action action, int x, int y, int width, boolean applicable) {
      String key = "gui.ancientwarfare.diplomacy.action." + action.name().toLowerCase(Locale.ROOT);
      if (action == PlayerDiplomacy.Action.TRADE && entry.trade()) {
         key += "_end";
      }

      Component label = switch (action) {
         case GIFT -> Component.translatable(key, PlayerDiplomacy.GIFT_EMERALDS / 10);
         case SPY -> Component.translatable(key, FactionSpies.SPY_EMERALDS);
         case TOURNAMENT -> Component.translatable(key, FactionTournaments.ENTRY_EMERALDS);
         case SUPPLY -> Component.translatable(key, FactionCourt.SUPPLY_AMOUNT);
         default -> Component.translatable(key);
      };
      Button button = Button.builder(label, ignored -> AWCoreNetwork.sendDiplomacyAction(entry.faction(), action)).bounds(x, y, width, 19).build();
      boolean anywhere = action == PlayerDiplomacy.Action.WAR || action == PlayerDiplomacy.Action.SPY || action == PlayerDiplomacy.Action.TRADE && entry.trade();
      String research = PlayerDiplomacy.research(action);
      boolean known = research == null || ClientResearch.has(research);
      button.active = applicable && known && (anywhere || entry.leaderNear());
      if (!known) {
         button.setTooltip(Tooltip.create(PlayerResearch.needs(research)));
      } else if (action == PlayerDiplomacy.Action.SUPPLY || action == PlayerDiplomacy.Action.MEDIATE || action == PlayerDiplomacy.Action.GIFT) {
         button.setTooltip(Tooltip.create(Component.translatable(key + ".tip")));
      } else if (!anywhere && !entry.leaderNear()) {
         button.setTooltip(Tooltip.create(Component.translatable("gui.ancientwarfare.diplomacy.leader_away")));
      }

      this.addRenderableWidget(button);
   }

   private void initCourt(PlayerDiplomacy.Entry entry) {
      PlayerDiplomacy.Action[] gifts = {
         PlayerDiplomacy.Action.COURT_HEIR, PlayerDiplomacy.Action.COURT_MARSHAL, PlayerDiplomacy.Action.COURT_TREASURER, PlayerDiplomacy.Action.COURT_PRIEST
      };
      int cardWidth = (this.contentWidth() - 4) / 2;
      for (int i = 0; i < gifts.length && i * 3 + 2 < entry.court().length; i++) {
         PlayerDiplomacy.Action action = gifts[i];
         int x = this.contentX() + (i % 2) * (cardWidth + 4);
         int y = this.contentY() + (i / 2) * 50;
         Button button = Button.builder(Component.translatable("gui.ancientwarfare.diplomacy.court_gift", FactionCourt.COURTIER_GIFT / 10), ignored -> AWCoreNetwork.sendDiplomacyAction(entry.faction(), action))
            .bounds(x + cardWidth - 58, y + 30, 54, 14)
            .build();
         button.active = entry.leaderNear();
         button.setTooltip(Tooltip.create(Component.translatable(entry.leaderNear() ? "gui.ancientwarfare.diplomacy.court_gift.tip" : "gui.ancientwarfare.diplomacy.leader_away")));
         this.addRenderableWidget(button);
      }
   }

   private void initDeal(PlayerDiplomacy.Entry entry) {
      if (this.deal == null || !entry.leaderNear()) {
         return;
      }

      int column = (this.contentWidth() - 8) / 2;
      int mine = this.contentX();
      int theirs = mine + column + 8;
      int y = this.contentY() + 14;
      this.stepper(mine, y, column, () -> this.playerGold = Math.max(0, this.playerGold - FactionDeals.GOLD_STEP), () -> this.playerGold += FactionDeals.GOLD_STEP);
      this.stepper(mine, y + DEAL_ROW, column, () -> this.tribute = Math.max(0, this.tribute - FactionDeals.TRIBUTE_STEP), () -> this.tribute += FactionDeals.TRIBUTE_STEP);
      this.stepper(theirs, y, column, () -> this.factionGold = Math.max(0, this.factionGold - FactionDeals.GOLD_STEP), () -> this.factionGold += FactionDeals.GOLD_STEP);
      for (int kind = 0; kind < FactionDeals.KINDS; kind++) {
         int mineSlot = kind;
         int theirSlot = FactionDeals.KINDS + kind;
         this.stepper(mine, y + DEAL_ROW * (2 + kind), column, () -> this.goods[mineSlot] = Math.max(0, this.goods[mineSlot] - 1), () -> this.goods[mineSlot]++);
         this.stepper(theirs, y + DEAL_ROW * (1 + kind), column, () -> this.goods[theirSlot] = Math.max(0, this.goods[theirSlot] - 1), () -> this.goods[theirSlot]++);
      }

      this.cycle(mine, y + DEAL_ROW * 5, column, this.deal.playerTargets(), this.playerWar, next -> this.playerWar = next, "gui.ancientwarfare.deal.war_mine.tip");
      RelationState state = this.deal.treaty();
      Button treatyButton = Button.builder(check(this.treaty, Component.translatable("gui.ancientwarfare.deal.treaty." + state.serializedName())), ignored -> {
         this.treaty = !this.treaty;
         this.changed();
      }).bounds(theirs, y + DEAL_ROW * 4, column, 16).build();
      treatyButton.active = state != RelationState.ALLIANCE;
      this.addRenderableWidget(treatyButton);
      Button tradeButton = Button.builder(check(this.trade, Component.translatable("gui.ancientwarfare.deal.trade")), ignored -> {
         this.trade = !this.trade;
         this.changed();
      }).bounds(theirs, y + DEAL_ROW * 5, column, 16).build();
      tradeButton.active = !this.deal.trade();
      this.addRenderableWidget(tradeButton);
      this.cycle(theirs, y + DEAL_ROW * 6, column, this.deal.factionTargets(), this.factionWar, next -> this.factionWar = next, "gui.ancientwarfare.deal.war_theirs.tip");
      int bottom = y + DEAL_ROW * 6;
      int half = (column - 2) / 2;
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.deal.propose"), ignored -> AWCoreNetwork.sendDeal(entry.faction(), 1, this.offer()))
            .bounds(mine, bottom, half, 16)
            .build()
      );
      Button strike = Button.builder(Component.translatable("gui.ancientwarfare.deal.strike").withStyle(ChatFormatting.GOLD), ignored -> AWCoreNetwork.sendDeal(entry.faction(), 2, this.offer()))
         .bounds(mine + half + 2, bottom, half, 16)
         .build();
      strike.active = this.verdict == FactionDeals.ACCEPT;
      this.addRenderableWidget(strike);
   }

   /** The spy's report first, then the agents' secret work. */
   private static final PlayerDiplomacy.Action[] INTRIGUES = {
      PlayerDiplomacy.Action.SPY,
      PlayerDiplomacy.Action.STEAL,
      PlayerDiplomacy.Action.BRIBE,
      PlayerDiplomacy.Action.ARSON,
      PlayerDiplomacy.Action.RUMOUR,
      PlayerDiplomacy.Action.FORGERY
   };

   private static boolean pending(PlayerDiplomacy.Entry entry, PlayerDiplomacy.Action action) {
      return action == PlayerDiplomacy.Action.SPY ? (entry.pending() & PlayerDiplomacy.SPY_PENDING) != 0 : (entry.pending() & 1 << action.op().ordinal()) != 0;
   }

   private void initIntrigue(PlayerDiplomacy.Entry entry) {
      boolean known = ClientResearch.has(PlayerResearch.ESPIONAGE);
      for (int i = 0; i < INTRIGUES.length; i++) {
         PlayerDiplomacy.Action action = INTRIGUES[i];
         boolean away = pending(entry, action);
         Button button = Button.builder(
               Component.translatable(away ? "gui.ancientwarfare.intrigue.away" : "gui.ancientwarfare.intrigue.send"),
               ignored -> AWCoreNetwork.sendDiplomacyAction(entry.faction(), action)
            )
            .bounds(this.contentX() + this.contentWidth() - 66, this.contentY() + i * INTRIGUE_ROW + 3, 64, 18)
            .build();
         button.active = known && !away;
         if (!known) {
            button.setTooltip(Tooltip.create(PlayerResearch.needs(PlayerResearch.ESPIONAGE)));
         }

         this.addRenderableWidget(button);
      }
   }

   private void renderIntrigue(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int mouseX, int mouseY) {
      int x = this.contentX();
      int width = this.contentWidth();
      boolean secret = ClientResearch.has(PlayerResearch.SECRET_SERVICE);
      boolean ciphers = ClientResearch.has(PlayerResearch.CIPHERS);
      for (int i = 0; i < INTRIGUES.length; i++) {
         PlayerDiplomacy.Action action = INTRIGUES[i];
         int y = this.contentY() + i * INTRIGUE_ROW;
         graphics.fill(x - 2, y, x + width, y + INTRIGUE_ROW - 2, CARD_COLOR);
         String key = action == PlayerDiplomacy.Action.SPY ? "spy" : action.op().key();
         graphics.drawString(this.font, Component.translatable("intrigue.ancientwarfare.op." + key), x + 2, y + 3, GOLD_COLOR);
         Component detail;
         if (action == PlayerDiplomacy.Action.SPY) {
            detail = Component.translatable("gui.ancientwarfare.intrigue.spy_detail", FactionSpies.SPY_EMERALDS);
         } else {
            int chance = (int)Math.round(net.shadowmage.ancientwarfare.core.faction.FactionIntrigue.chance(action.op(), entry.trait(), secret, ciphers) * 100);
            detail = Component.translatable("gui.ancientwarfare.intrigue.detail", action.op().cost, chance);
         }

         graphics.drawString(this.font, detail, x + 2, y + 13, MUTED_COLOR);
         if (mouseX >= x && mouseX < x + width - 68 && mouseY >= y && mouseY < y + INTRIGUE_ROW - 2) {
            graphics.renderTooltip(this.font, this.font.split(Component.translatable("intrigue.ancientwarfare.op." + key + ".tip"), 200), mouseX, mouseY);
         }
      }
   }

   /** Where the faction at this index of the list sits on the ring of the ties diagram. */
   private int[] node(int index) {
      // Room below for the legend.
      int height = this.top() + this.panelHeight() - 22 - this.contentY();
      int radius = Math.max(30, Math.min(52, height / 2 - 12));
      int cx = this.contentX() + radius + 34;
      int cy = this.contentY() + height / 2;
      double angle = -Math.PI / 2 + Math.PI * 2 * index / Math.max(1, this.entries.size());
      return new int[]{cx + (int)Math.round(Math.cos(angle) * radius), cy + (int)Math.round(Math.sin(angle) * radius)};
   }

   private static int stateColor(RelationState state) {
      return switch (state) {
         case WAR -> 0xFFE05050;
         case PACT -> 0xFF50C8E0;
         case ALLIANCE -> 0xFF60E060;
         default -> 0xFF606060;
      };
   }

   /** Every war, pact and alliance between the factions the player knows, drawn on a ring; the chosen one's ties brightest. */
   private void renderTies(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int mouseX, int mouseY) {
      List<String> names = new ArrayList<>();
      this.entries.forEach(other -> names.add(other.faction()));
      for (int pass = 0; pass < 2; pass++) {
         for (int i = 0; i < this.entries.size(); i++) {
            PlayerDiplomacy.Entry from = this.entries.get(i);
            boolean chosen = from.faction().equals(this.selected);
            for (int t = 0; t < from.ties().size() && t * 2 + 1 < from.tieData().length; t++) {
               int j = names.indexOf(from.ties().get(t));
               RelationState state = RelationState.values()[Math.floorMod(from.tieData()[t * 2], RelationState.values().length)];
               boolean involved = chosen || this.entries.get(Math.max(0, j)).faction().equals(this.selected);
               // The chosen faction's ties go on top, on the second pass.
               if (j < 0 || j <= i && !chosen || state == RelationState.NEUTRAL || involved != (pass == 1)) {
                  continue;
               }

               int color = stateColor(state);
               if (!involved) {
                  color = (color & 0x00FFFFFF) | 0x50000000;
               }

               line(graphics, this.node(i), this.node(j), color, involved ? 2 : 1);
            }
         }
      }

      for (int i = 0; i < this.entries.size(); i++) {
         PlayerDiplomacy.Entry node = this.entries.get(i);
         int[] at = this.node(i);
         boolean chosen = node.faction().equals(this.selected);
         if (chosen) {
            graphics.fill(at[0] - 5, at[1] - 5, at[0] + 5, at[1] + 5, GOLD_COLOR);
         }

         graphics.fill(at[0] - 4, at[1] - 4, at[0] + 4, at[1] + 4, 0xFF000000 | FactionColors.color(node.faction()));
         Component label = factionName(node.faction());
         int ringX = at[0] - this.node(0)[0];
         int labelX = at[0] - this.font.width(label) / 2;
         int labelY = at[1] + (at[1] >= this.node(0)[1] + 4 || ringX != 0 ? 6 : -14);
         graphics.drawString(this.font, label, labelX, labelY, chosen ? GOLD_COLOR : MUTED_COLOR);
      }

      // The chosen faction's ties, one per line, beside the ring.
      int x = this.contentX() + this.contentWidth() - 100;
      int y = this.contentY();
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.ties_of").withStyle(ChatFormatting.BOLD), x, y, GOLD_COLOR);
      y += 12;
      for (int t = 0; t < entry.ties().size() && t * 2 + 1 < entry.tieData().length; t++) {
         RelationState state = RelationState.values()[Math.floorMod(entry.tieData()[t * 2], RelationState.values().length)];
         int opinion = entry.tieData()[t * 2 + 1];
         String other = entry.ties().get(t);
         graphics.fill(x, y + 1, x + 4, y + 8, 0xFF000000 | FactionColors.color(other));
         graphics.drawString(this.font, factionName(other), x + 7, y, TEXT_COLOR);
         Component mood = Component.literal(opinion > 0 ? "+" + opinion : Integer.toString(opinion));
         graphics.drawString(this.font, mood, x + 100 - this.font.width(mood), y, opinion < -20 ? BAD_COLOR : opinion > 20 ? GOOD_COLOR : MUTED_COLOR);
         graphics.fill(x + 7, y + 9, x + 100, y + 10, stateColor(state));
         y += 13;
      }

      int legendY = this.top() + this.panelHeight() - 18;
      int lx = this.contentX();
      RelationState[] shown = {RelationState.WAR, RelationState.PACT, RelationState.ALLIANCE};
      for (RelationState state : shown) {
         graphics.fill(lx, legendY + 3, lx + 10, legendY + 5, stateColor(state));
         Component name = stateName(state);
         graphics.drawString(this.font, name, lx + 13, legendY, MUTED_COLOR);
         lx += 13 + this.font.width(name) + 10;
      }
   }

   private static void line(GuiGraphics graphics, int[] from, int[] to, int color, int thickness) {
      int dx = to[0] - from[0];
      int dy = to[1] - from[1];
      int steps = Math.max(Math.abs(dx), Math.abs(dy));
      for (int i = 0; i <= steps; i++) {
         int x = from[0] + dx * i / Math.max(1, steps);
         int y = from[1] + dy * i / Math.max(1, steps);
         graphics.fill(x, y, x + thickness, y + thickness, color);
      }
   }

   private static Component check(boolean on, Component label) {
      return Component.literal(on ? "■ " : "□ ").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY).append(label.copy().withStyle(ChatFormatting.WHITE));
   }

   /** Minus and plus at the right of a row whose icon and amount render() draws. */
   private void stepper(int x, int y, int width, Runnable less, Runnable more) {
      this.addRenderableWidget(Button.builder(Component.literal("-"), ignored -> {
         less.run();
         this.changed();
      }).bounds(x + width - 34, y, 16, 16).build());
      this.addRenderableWidget(Button.builder(Component.literal("+"), ignored -> {
         more.run();
         this.changed();
      }).bounds(x + width - 16, y, 16, 16).build());
   }

   /** A button going round the possible enemies, and none. */
   private void cycle(int x, int y, int width, List<String> targets, String current, Consumer<String> set, String tip) {
      Component target = current.isEmpty() ? Component.translatable("gui.ancientwarfare.deal.nobody") : factionName(current);
      Button button = Button.builder(Component.translatable("gui.ancientwarfare.deal.war", target), ignored -> {
         int at = targets.indexOf(current);
         set.accept(at + 1 < targets.size() ? targets.get(at + 1) : "");
         this.changed();
      }).bounds(x, y, width, 16).build();
      button.active = !targets.isEmpty();
      button.setTooltip(Tooltip.create(Component.translatable(targets.isEmpty() ? "gui.ancientwarfare.deal.war_none" : tip, FactionDeals.WAR_PROMISE_DAYS)));
      this.addRenderableWidget(button);
   }

   // ---------------------------------------------------------------- input

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int left = this.left();
      int top = this.top();
      if (button == 0 && mouseX >= left + 6 && mouseX < left + LIST_WIDTH + 4 && mouseY >= top + 26) {
         int row = (int)((mouseY - top - 26) / ROW_HEIGHT);
         if (row < VISIBLE_ROWS && this.scroll + row < this.entries.size()) {
            String faction = this.entries.get(this.scroll + row).faction();
            if (!faction.equals(this.selected)) {
               this.playClick();
               this.select(faction);
            }

            return true;
         }
      }

      if (button == 0 && tab == TIES && this.selectedEntry() != null) {
         for (int i = 0; i < this.entries.size(); i++) {
            int[] node = this.node(i);
            if (Math.abs(mouseX - node[0]) <= 5 && Math.abs(mouseY - node[1]) <= 5) {
               String faction = this.entries.get(i).faction();
               if (!faction.equals(this.selected)) {
                  this.playClick();
                  this.select(faction);
               }

               return true;
            }
         }
      }

      int tabY = this.top() + 48;
      if (button == 0 && mouseY >= tabY && mouseY < tabY + 14 && this.selectedEntry() != null) {
         for (int i = 0; i < TABS.length; i++) {
            int x = this.tabX(i);
            if (mouseX >= x && mouseX < x + this.tabWidth(i)) {
               if (tab != i) {
                  this.playClick();
                  tab = i;
                  this.rebuildWidgets();
               }

               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void playClick() {
      Minecraft.getInstance()
         .getSoundManager()
         .play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int max = Math.max(0, this.entries.size() - VISIBLE_ROWS);
      int next = Math.max(0, Math.min(max, this.scroll - (int)Math.signum(delta)));
      if (next != this.scroll) {
         this.scroll = next;
      }

      return true;
   }

   private int tabWidth(int i) {
      return this.font.width(Component.translatable("gui.ancientwarfare.diplomacy.tab." + TABS[i])) + 16;
   }

   private int tabX(int i) {
      int x = this.contentX();
      for (int j = 0; j < i; j++) {
         x += this.tabWidth(j) + 3;
      }

      return x;
   }

   // ---------------------------------------------------------------- drawing

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = this.left();
      int top = this.top();
      // A dark wooden board with a brass rim, a parchment-dark page inside.
      graphics.fill(left - 2, top - 2, left + PANEL_WIDTH + 2, top + this.panelHeight() + 2, 0xFF0B0907);
      graphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + this.panelHeight() + 1, FRAME_COLOR);
      graphics.fill(left, top, left + PANEL_WIDTH, top + this.panelHeight(), WOOD_COLOR);
      graphics.fill(left + 4, top + 20, left + PANEL_WIDTH - 4, top + this.panelHeight() - 4, PAPER_COLOR);
      graphics.fill(left + 4, top + 19, left + PANEL_WIDTH - 4, top + 20, FRAME_COLOR);
      graphics.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), left + PANEL_WIDTH / 2, top + 6, GOLD_COLOR);
      graphics.fill(left + LIST_WIDTH + 6, top + 24, left + LIST_WIDTH + 7, top + this.panelHeight() - 8, 0xFF4A3B26);
      this.renderList(graphics, mouseX, mouseY);
      PlayerDiplomacy.Entry entry = this.selectedEntry();
      if (this.entries.isEmpty()) {
         graphics.drawWordWrap(this.font, Component.translatable("gui.ancientwarfare.diplomacy.none"), this.contentX(), top + 30, this.contentWidth(), MUTED_COLOR);
      } else if (entry != null) {
         this.renderHeader(graphics, entry, mouseX, mouseY);
         switch (tab) {
            case COURT -> this.renderCourt(graphics, entry);
            case DEAL -> this.renderDeal(graphics, entry, mouseX, mouseY);
            case INTRIGUE -> this.renderIntrigue(graphics, entry, mouseX, mouseY);
            case TIES -> this.renderTies(graphics, entry, mouseX, mouseY);
            default -> this.renderOverview(graphics, entry, mouseX, mouseY);
         }
      }

      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
      int x = this.left() + 8;
      int y = this.top() + 26;
      int width = LIST_WIDTH - 4;
      for (int i = 0; i < VISIBLE_ROWS && this.scroll + i < this.entries.size(); i++) {
         PlayerDiplomacy.Entry entry = this.entries.get(this.scroll + i);
         int rowY = y + i * ROW_HEIGHT;
         int color = 0xFF000000 | FactionColors.color(entry.faction());
         boolean chosen = entry.faction().equals(this.selected);
         boolean hover = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT - 2;
         graphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT - 2, chosen ? 0xFF3A2E1E : hover ? 0xFF2C241A : CARD_COLOR);
         if (chosen) {
            outline(graphics, x, rowY, width, ROW_HEIGHT - 2, GOLD_COLOR);
         }

         graphics.fill(x + 2, rowY + 2, x + 6, rowY + ROW_HEIGHT - 4, color);
         graphics.drawString(this.font, factionName(entry.faction()), x + 10, rowY + 2, chosen ? GOLD_COLOR : TEXT_COLOR);
         graphics.drawString(this.font, stateName(entry.treaty()), x + 10, rowY + 11, MUTED_COLOR);
         Component standing = standingText(entry.standing());
         graphics.drawString(this.font, standing, x + width - 3 - this.font.width(standing), rowY + 6, TEXT_COLOR);
      }

      if (this.entries.size() > VISIBLE_ROWS) {
         int track = VISIBLE_ROWS * ROW_HEIGHT - 2;
         int thumb = Math.max(12, track * VISIBLE_ROWS / this.entries.size());
         int at = (track - thumb) * this.scroll / Math.max(1, this.entries.size() - VISIBLE_ROWS);
         graphics.fill(x + width + 1, y + at, x + width + 3, y + at + thumb, FRAME_COLOR);
      }
   }

   /** The faction's name large in its colours, its ruler, and the tabs. */
   private void renderHeader(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int mouseX, int mouseY) {
      int x = this.contentX();
      int y = this.top() + 24;
      int color = 0xFF000000 | FactionColors.color(entry.faction());
      graphics.fill(x - 4, y - 1, x - 2, y + 20, color);
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0.0F);
      graphics.pose().scale(1.5F, 1.5F, 1.0F);
      graphics.drawString(this.font, factionName(entry.faction()), 0, 0, TEXT_COLOR);
      graphics.pose().popPose();
      graphics.drawString(
         this.font,
         Component.translatable("gui.ancientwarfare.diplomacy.ruler", FactionRulers.name(entry.ruler()), FactionRulers.traitName(entry.trait())),
         x,
         y + 14,
         GOLD_COLOR
      );
      Component leader = Component.translatable(entry.leaderNear() ? "gui.ancientwarfare.diplomacy.leader_here" : "gui.ancientwarfare.diplomacy.leader_away_short");
      int right = this.contentX() + this.contentWidth();
      if (tab == DEAL && this.deal != null && entry.leaderNear()) {
         this.renderWord(graphics, right, y + 2, mouseX, mouseY);
      } else {
         graphics.drawString(this.font, leader, right - this.font.width(leader), y + 2, entry.leaderNear() ? GOOD_COLOR : MUTED_COLOR);
      }
      int tabY = this.top() + 48;
      graphics.fill(x - 2, tabY + 14, right, tabY + 15, FRAME_COLOR);
      for (int i = 0; i < TABS.length; i++) {
         int tabX = this.tabX(i);
         int width = this.tabWidth(i);
         boolean active = tab == i;
         boolean hover = mouseX >= tabX && mouseX < tabX + width && mouseY >= tabY && mouseY < tabY + 14;
         graphics.fill(tabX, tabY, tabX + width, tabY + 15, active ? FRAME_COLOR : 0xFF3A2E1E);
         graphics.fill(tabX + 1, tabY + 1, tabX + width - 1, tabY + 15, active ? 0xFF3A2E1E : hover ? 0xFF2C241A : CARD_COLOR);
         graphics.drawCenteredString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.tab." + TABS[i]), tabX + width / 2, tabY + 4, active ? GOLD_COLOR : MUTED_COLOR);
      }
   }

   private void renderOverview(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int mouseX, int mouseY) {
      int x = this.contentX();
      int y = this.contentY();
      int width = this.contentWidth();
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.opinion"), x, y, MUTED_COLOR);
      int barX = x + 70;
      int barWidth = width - 70 - 34;
      this.standingBar(graphics, barX, y + 1, barWidth, 6, entry.standing());
      graphics.drawString(this.font, standingText(entry.standing()), barX + barWidth + 6, y, TEXT_COLOR);
      y += 13;
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.state", stateName(entry.treaty())), x, y, TEXT_COLOR);
      Component trade = entry.trade()
         ? Component.translatable("gui.ancientwarfare.diplomacy.trade", entry.routes())
         : Component.translatable("gui.ancientwarfare.diplomacy.trade_none");
      graphics.drawString(this.font, trade, x + width / 2, y, entry.trade() ? GOOD_COLOR : MUTED_COLOR);
      y += 14;
      // Treasury, stores and towns, each with its picture.
      int[] stores = entry.stores();
      if (stores.length >= 4) {
         ItemStack[] icons = {CoinItem.create(CoinMetal.GOLD), new ItemStack(Items.OAK_LOG), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.BELL)};
         int[] amounts = {stores[0] / 10, stores[1], stores[2], stores[3], entry.settlements()};
         String[] tips = {"treasury", "wood", "stone", "iron", "towns"};
         int step = width / icons.length;
         graphics.fill(x - 2, y - 2, x + width, y + 18, CARD_COLOR);
         for (int i = 0; i < icons.length; i++) {
            int iconX = x + i * step;
            graphics.renderItem(icons[i], iconX, y);
            graphics.drawString(this.font, Integer.toString(amounts[i]), iconX + 18, y + 5, TEXT_COLOR);
            if (mouseX >= iconX && mouseX < iconX + step && mouseY >= y && mouseY < y + 16) {
               graphics.renderTooltip(this.font, Component.translatable("gui.ancientwarfare.diplomacy.stat." + tips[i]), mouseX, mouseY);
            }
         }

         y += 24;
      }

      if (entry.wars().isEmpty()) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.wars_none"), x, y, MUTED_COLOR);
      } else {
         Component label = Component.translatable("gui.ancientwarfare.diplomacy.wars_label");
         graphics.drawString(this.font, label, x, y, BAD_COLOR);
         int wx = x + this.font.width(label) + 4;
         int wy = y;
         for (String war : entry.wars()) {
            Component name = factionName(war);
            int w = this.font.width(name) + 10;
            if (wx + w > x + width) {
               wx = x + 8;
               wy += 11;
            }

            graphics.fill(wx, wy, wx + 6, wy + 7, 0xFF000000 | FactionColors.color(war));
            graphics.drawString(this.font, name, wx + 8, wy, TEXT_COLOR);
            wx += w + 4;
         }
      }
   }

   /** From hatred on the left through red and yellow to friendship in green on the right. */
   private void standingBar(GuiGraphics graphics, int x, int y, int width, int height, int standing) {
      graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
      for (int i = 0; i < width; i++) {
         float t = i / (float)Math.max(1, width - 1);
         int red = t < 0.5F ? 0xD0 : (int)(0xD0 * (1.0F - (t - 0.5F) * 2.0F) + 0x50 * (t - 0.5F) * 2.0F);
         int green = t < 0.5F ? (int)(0x40 + 0x90 * t * 2.0F) : 0xD0;
         graphics.fill(x + i, y, x + i + 1, y + height, 0xFF000000 | red << 16 | green << 8 | 0x40);
      }

      graphics.fill(x + width / 2, y - 1, x + width / 2 + 1, y + height + 1, 0x80000000);
      int at = x + Mth.clamp((standing + 100) * (width - 1) / 200, 0, width - 1);
      graphics.fill(at - 1, y - 2, at + 2, y + height + 2, 0xFFFFFFFF);
   }

   /** The court as four cards, each courtier's mind and favour; then what the faction wants. */
   private void renderCourt(GuiGraphics graphics, PlayerDiplomacy.Entry entry) {
      FactionCourt.Role[] roles = FactionCourt.Role.values();
      int[] court = entry.court();
      int cardWidth = (this.contentWidth() - 4) / 2;
      for (int i = 0; i < roles.length && i * 3 + 2 < court.length; i++) {
         int x = this.contentX() + (i % 2) * (cardWidth + 4);
         int y = this.contentY() + (i / 2) * 50;
         graphics.fill(x, y, x + cardWidth, y + 46, CARD_COLOR);
         outline(graphics, x, y, cardWidth, 46, 0xFF4A3B26);
         graphics.drawString(this.font, FactionCourt.roleName(roles[i]), x + 4, y + 3, GOLD_COLOR);
         graphics.drawString(this.font, FactionRulers.name(court[i * 3]), x + 4, y + 13, TEXT_COLOR);
         graphics.drawString(this.font, FactionRulers.traitName(court[i * 3 + 1]), x + 4, y + 23, MUTED_COLOR);
         this.standingBar(graphics, x + 4, y + 37, cardWidth - 66, 4, court[i * 3 + 2] * 2);
      }

      int x = this.contentX();
      int y = this.contentY() + 104;
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.goals").withStyle(ChatFormatting.BOLD), x, y, GOLD_COLOR);
      y += 12;
      if (entry.goals().isEmpty()) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.goals_none"), x, y, MUTED_COLOR);
      }

      for (Component goal : entry.goals()) {
         Component line = Component.literal("◆ ").append(goal);
         graphics.drawWordWrap(this.font, line, x, y, this.contentWidth(), 0xFF9FD7FF);
         y += this.font.split(line, this.contentWidth()).size() * this.font.lineHeight + 3;
      }
   }

   private void renderDeal(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int mouseX, int mouseY) {
      int x = this.contentX();
      int y = this.contentY();
      if (!entry.leaderNear()) {
         graphics.drawWordWrap(this.font, Component.translatable("gui.ancientwarfare.deal.need_leader"), x, y + 4, this.contentWidth(), MUTED_COLOR);
         return;
      } else if (this.deal == null) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.loading"), x, y + 4, MUTED_COLOR);
         return;
      }

      int column = (this.contentWidth() - 8) / 2;
      int theirs = x + column + 8;
      graphics.fill(x - 2, y - 1, x + column + 2, y + 12 + DEAL_ROW * 7 + 1, CARD_COLOR);
      graphics.fill(theirs - 2, y - 1, theirs + column + 2, y + 12 + DEAL_ROW * 7 + 1, CARD_COLOR);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.mine").withStyle(ChatFormatting.BOLD), x + 2, y + 2, GOLD_COLOR);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.theirs").withStyle(ChatFormatting.BOLD), theirs + 2, y + 2, GOLD_COLOR);
      y += 14;
      ItemStack coin = CoinItem.create(CoinMetal.GOLD);
      ItemStack[] goodsIcons = {new ItemStack(Items.OAK_LOG), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.IRON_INGOT)};
      this.amountRow(graphics, coin, Component.translatable("gui.ancientwarfare.deal.gold_short", this.playerGold), x, y);
      this.amountRow(graphics, CoinItem.create(CoinMetal.SILVER), Component.translatable("gui.ancientwarfare.deal.tribute", this.tribute), x, y + DEAL_ROW);
      this.amountRow(graphics, coin, Component.translatable("gui.ancientwarfare.deal.gold_short", this.factionGold), theirs, y);
      if (mouseX >= x && mouseX < x + column - 36 && mouseY >= y + DEAL_ROW && mouseY < y + DEAL_ROW + 16) {
         graphics.renderTooltip(this.font, Component.translatable("gui.ancientwarfare.deal.tribute.tip", FactionDeals.TRIBUTE_WEEKS), mouseX, mouseY);
      }
      for (int kind = 0; kind < FactionDeals.KINDS; kind++) {
         this.amountRow(graphics, goodsIcons[kind], Component.literal("× " + this.goods[kind] * FactionDeals.GOODS_STEP), x, y + DEAL_ROW * (2 + kind));
         this.amountRow(graphics, goodsIcons[kind], Component.literal("× " + this.goods[FactionDeals.KINDS + kind] * FactionDeals.GOODS_STEP), theirs, y + DEAL_ROW * (1 + kind));
      }

      int answerY = y + DEAL_ROW * 7 + 4;
      int color = switch (this.verdict) {
         case FactionDeals.ACCEPT, FactionDeals.DONE -> GOOD_COLOR;
         case FactionDeals.MORE -> GOLD_COLOR;
         case FactionDeals.REFUSE -> BAD_COLOR;
         default -> MUTED_COLOR;
      };
      Component shown = this.verdict == FactionDeals.NONE ? Component.translatable("gui.ancientwarfare.deal.hint") : this.answer;
      graphics.drawWordWrap(this.font, shown, x, answerY, this.contentWidth(), color);
   }

   /** How far factions take the player's word, with their promises on hover. */
   private void renderWord(GuiGraphics graphics, int right, int y, int mouseX, int mouseY) {
      int word = this.deal.reliability();
      Component wordName = Component.translatable(
            "gui.ancientwarfare.deal.word." + (word <= FactionDeals.NO_TRUST ? "none" : word < 0 ? "doubtful" : word == 0 ? "untried" : word < 3 ? "good" : "iron")
         )
         .withStyle(word < 0 ? ChatFormatting.RED : word > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY);
      Component wordLine = Component.translatable("gui.ancientwarfare.deal.word", wordName)
         .append(Component.translatable("gui.ancientwarfare.deal.promise_count", this.deal.promises().size()).withStyle(ChatFormatting.GRAY));
      int x = right - this.font.width(wordLine);
      graphics.drawString(this.font, wordLine, x, y, TEXT_COLOR);
      if (mouseX >= x && mouseX < right && mouseY >= y && mouseY < y + 9) {
         List<Component> lines = new ArrayList<>(this.deal.promises());
         if (lines.isEmpty()) {
            lines.add(Component.translatable("gui.ancientwarfare.deal.promises_none"));
         }

         graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
      }
   }

   private void amountRow(GuiGraphics graphics, ItemStack icon, Component amount, int x, int y) {
      graphics.renderItem(icon, x, y);
      graphics.drawString(this.font, amount, x + 19, y + 4, TEXT_COLOR);
   }

   private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
      graphics.fill(x, y, x + width, y + 1, color);
      graphics.fill(x, y + height - 1, x + width, y + height, color);
      graphics.fill(x, y, x + 1, y + height, color);
      graphics.fill(x + width - 1, y, x + width, y + height, color);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private PlayerDiplomacy.Entry selectedEntry() {
      for (PlayerDiplomacy.Entry entry : this.entries) {
         if (entry.faction().equals(this.selected)) {
            return entry;
         }
      }

      return null;
   }

   static Component factionName(String faction) {
      return Component.translatable("faction.ancientwarfare." + faction);
   }

   static Component stateName(RelationState state) {
      ChatFormatting color = switch (state) {
         case WAR -> ChatFormatting.RED;
         case PACT -> ChatFormatting.AQUA;
         case ALLIANCE -> ChatFormatting.GREEN;
         default -> ChatFormatting.GRAY;
      };
      return Component.translatable("gui.ancientwarfare.diplomacy.state." + state.serializedName()).withStyle(color);
   }

   private static Component standingText(int standing) {
      ChatFormatting color = standing < 0 ? ChatFormatting.RED : standing >= PlayerDiplomacy.PACT_STANDING ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
      return Component.literal(standing > 0 ? "+" + standing : Integer.toString(standing)).withStyle(color);
   }

   static Component joined(List<Component> parts) {
      MutableComponent result = Component.empty();
      for (int i = 0; i < parts.size(); i++) {
         if (i > 0) {
            result.append(", ");
         }

         result.append(parts.get(i));
      }

      return result;
   }
}
