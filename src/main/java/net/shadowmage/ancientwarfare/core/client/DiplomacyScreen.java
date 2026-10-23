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
import net.shadowmage.ancientwarfare.core.faction.FactionPeople;
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
   private static final int MAX_WIDTH = 560;
   private static final int MAX_HEIGHT = 340;
   /** Below this height the header is a single line, without the large portrait. */
   private static final int ROOMY_HEIGHT = 300;
   private static final int DEAL_ROW = 17;
   /** The height of the ruler's card above the court, with the gap below it. */
   private static final int RULER_CARD = 50;
   /** The height of a courtier's card, with the gap below it. */
   private static final int COURT_CARD = 42;
   private static final int ROW_HEIGHT = 26;
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
   private int bond;
   /** A tooltip found while drawing the court, drawn last so nothing covers it. */
   private List<Component> courtTip;
   /** The same for the ruler's portrait in the header. */
   private List<Component> headerTip;
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
         screen.bond = offer.bond();
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
      this.bond = FactionDeals.NO_BOND;
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
      return new FactionDeals.Offer(this.playerGold, this.tribute, this.playerWar, this.factionGold, this.treaty, this.trade, this.factionWar, this.goods, this.bond);
   }

   /** The offer changed: the last answer no longer holds. */
   private void changed() {
      this.verdict = FactionDeals.NONE;
      this.answer = Component.empty();
      this.rebuildWidgets();
   }

   // ---------------------------------------------------------------- layout

   private int panelWidth() {
      return Math.min(MAX_WIDTH, this.width - 8);
   }

   private int left() {
      return (this.width - this.panelWidth()) / 2;
   }

   private int panelHeight() {
      return Math.min(MAX_HEIGHT, this.height - 4);
   }

   /** Whether there is room for the large header with the ruler's portrait. */
   private boolean roomy() {
      return this.panelHeight() >= ROOMY_HEIGHT;
   }

   private int listWidth() {
      return this.panelWidth() >= 500 ? 156 : 132;
   }

   private int listTop() {
      return this.top() + 26;
   }

   private int visibleRows() {
      return Math.max(1, (this.top() + this.panelHeight() - 8 - this.listTop()) / ROW_HEIGHT);
   }

   private int tabY() {
      return this.top() + (this.roomy() ? 74 : 52);
   }

   private int top() {
      return Math.max(2, (this.height - this.panelHeight()) / 2);
   }

   private int contentX() {
      return this.left() + this.listWidth() + 14;
   }

   private int contentWidth() {
      return this.panelWidth() - this.listWidth() - 22;
   }

   /** Where a tab's own content begins, below the faction's header and the tabs. */
   private int contentY() {
      return this.tabY() + 21;
   }

   @Override
   protected void init() {
      if (this.selectedEntry() == null && !this.entries.isEmpty()) {
         this.selected = this.entries.get(0).faction();
      }

      this.scroll = Math.max(0, Math.min(this.scroll, this.entries.size() - this.visibleRows()));
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
      for (int i = 0; i < gifts.length && (i + 1) * FactionCourt.COURT_VIEW <= entry.court().length; i++) {
         PlayerDiplomacy.Action action = gifts[i];
         int x = this.contentX() + (i % 2) * (cardWidth + 4);
         int y = this.contentY() + RULER_CARD + (i / 2) * COURT_CARD;
         Button button = Button.builder(Component.translatable("gui.ancientwarfare.diplomacy.court_gift", FactionCourt.COURTIER_GIFT / 10), ignored -> AWCoreNetwork.sendDiplomacyAction(entry.faction(), action))
            .bounds(x + cardWidth - 44, y + 24, 40, 14)
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
      // The goods are a table below, changed by clicking its cells.
      this.cycle(mine, y + DEAL_ROW * 2, column, this.deal.playerTargets(), this.playerWar, next -> this.playerWar = next, "gui.ancientwarfare.deal.war_mine.tip");
      RelationState state = this.deal.treaty();
      // One button cycles through what binds the sides: the next treaty, vassalage, or the player's fealty.
      boolean canTreaty = state != RelationState.ALLIANCE;
      Component bondLabel = this.treaty
         ? check(true, Component.translatable("gui.ancientwarfare.deal.treaty." + state.serializedName()))
         : this.bond == FactionDeals.VASSAL
            ? check(true, Component.translatable("gui.ancientwarfare.deal.vassal"))
            : this.bond == FactionDeals.FEALTY
               ? check(true, Component.translatable("gui.ancientwarfare.deal.fealty"))
               : this.bond == FactionDeals.MARRIAGE
               ? check(true, Component.translatable("gui.ancientwarfare.deal.marriage"))
               : this.bond == FactionDeals.GIVE_DAUGHTER
               ? check(true, Component.translatable("gui.ancientwarfare.deal.give_daughter"))
               : check(false, canTreaty ? Component.translatable("gui.ancientwarfare.deal.treaty." + state.serializedName()) : Component.translatable("gui.ancientwarfare.deal.bond"));
      Button treatyButton = Button.builder(bondLabel, ignored -> {
         if (this.treaty) {
            this.treaty = false;
            this.bond = FactionDeals.VASSAL;
         } else if (this.bond == FactionDeals.VASSAL) {
            this.bond = FactionDeals.FEALTY;
         } else if (this.bond == FactionDeals.FEALTY) {
            this.bond = FactionDeals.MARRIAGE;
         } else if (this.bond == FactionDeals.MARRIAGE) {
            this.bond = FactionDeals.GIVE_DAUGHTER;
         } else if (this.bond == FactionDeals.GIVE_DAUGHTER) {
            this.bond = FactionDeals.NO_BOND;
         } else if (canTreaty) {
            this.treaty = true;
         } else {
            this.bond = FactionDeals.VASSAL;
         }

         this.changed();
      }).bounds(theirs, y + DEAL_ROW, column, 16).build();
      treatyButton.setTooltip(Tooltip.create(Component.translatable("gui.ancientwarfare.deal.bond.tip")));
      this.addRenderableWidget(treatyButton);
      Button tradeButton = Button.builder(check(this.trade, Component.translatable("gui.ancientwarfare.deal.trade")), ignored -> {
         this.trade = !this.trade;
         this.changed();
      }).bounds(theirs, y + DEAL_ROW * 2, column, 16).build();
      tradeButton.active = !this.deal.trade();
      this.addRenderableWidget(tradeButton);
      this.cycle(theirs, y + DEAL_ROW * 3, column, this.deal.factionTargets(), this.factionWar, next -> this.factionWar = next, "gui.ancientwarfare.deal.war_theirs.tip");
      int bottom = y + DEAL_ROW * 3;
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
      int radius = Math.max(30, Math.min(72, height / 2 - 12));
      int cx = this.contentX() + Math.max(radius + 34, (this.contentWidth() - 104) / 2);
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
      if (button == 0 && mouseX >= left + 6 && mouseX < left + this.listWidth() + 4 && mouseY >= this.listTop()) {
         int row = (int)((mouseY - this.listTop()) / ROW_HEIGHT);
         if (row < this.visibleRows() && this.scroll + row < this.entries.size()) {
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

      if (tab == DEAL && this.deal != null && (button == 0 || button == 1)) {
         int cell = this.goodsCell(mouseX, mouseY);
         PlayerDiplomacy.Entry entry = this.selectedEntry();
         if (cell >= 0 && entry != null && entry.leaderNear()) {
            int step = (hasShiftDown() ? 4 : 1) * (button == 0 ? 1 : -1);
            this.goods[cell] = Math.max(0, this.goods[cell] + step);
            this.playClick();
            this.changed();
            return true;
         }
      }

      int tabY = this.tabY();
      if (button == 0 && mouseY >= tabY && mouseY < tabY + 16 && this.selectedEntry() != null) {
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
      int max = Math.max(0, this.entries.size() - this.visibleRows());
      int next = Math.max(0, Math.min(max, this.scroll - (int)Math.signum(delta)));
      if (next != this.scroll) {
         this.scroll = next;
      }

      return true;
   }

   /** The tabs share the width of the page evenly. */
   private int tabWidth(int i) {
      return (this.contentWidth() - 2 * (TABS.length - 1)) / TABS.length;
   }

   private int tabX(int i) {
      return this.contentX() + i * (this.tabWidth(i) + 2);
   }

   // ---------------------------------------------------------------- drawing

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = this.left();
      int top = this.top();
      int width = this.panelWidth();
      int height = this.panelHeight();
      // A dark wooden board with a brass rim, a parchment-dark page inside.
      graphics.fill(left - 3, top - 3, left + width + 3, top + height + 3, 0xFF0B0907);
      graphics.fill(left - 2, top - 2, left + width + 2, top + height + 2, FRAME_COLOR);
      graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0xFF4A3B26);
      graphics.fill(left, top, left + width, top + height, WOOD_COLOR);
      graphics.fill(left + 4, top + 20, left + width - 4, top + height - 4, PAPER_COLOR);
      graphics.fill(left + 4, top + 19, left + width - 4, top + 20, FRAME_COLOR);
      Component title = this.title.copy().withStyle(ChatFormatting.BOLD);
      int titleWidth = this.font.width(title);
      graphics.drawCenteredString(this.font, title, left + width / 2, top + 6, GOLD_COLOR);
      // Brass rules on either side of the title.
      graphics.fill(left + 10, top + 9, left + width / 2 - titleWidth / 2 - 8, top + 10, 0xFF6A5330);
      graphics.fill(left + width / 2 + titleWidth / 2 + 8, top + 9, left + width - 10, top + 10, 0xFF6A5330);
      graphics.fill(left + this.listWidth() + 6, top + 24, left + this.listWidth() + 7, top + height - 8, 0xFF4A3B26);
      this.renderList(graphics, mouseX, mouseY);
      PlayerDiplomacy.Entry entry = this.selectedEntry();
      if (this.entries.isEmpty()) {
         graphics.drawWordWrap(this.font, Component.translatable("gui.ancientwarfare.diplomacy.none"), this.contentX(), top + 30, this.contentWidth(), MUTED_COLOR);
      } else if (entry != null) {
         this.renderHeader(graphics, entry, mouseX, mouseY);
         switch (tab) {
            case COURT -> this.renderCourt(graphics, entry, mouseX, mouseY);
            case DEAL -> this.renderDeal(graphics, entry, mouseX, mouseY);
            case INTRIGUE -> this.renderIntrigue(graphics, entry, mouseX, mouseY);
            case TIES -> this.renderTies(graphics, entry, mouseX, mouseY);
            default -> this.renderOverview(graphics, entry, mouseX, mouseY);
         }
      }

      super.render(graphics, mouseX, mouseY, partialTick);
      if (this.headerTip != null) {
         graphics.renderComponentTooltip(this.font, this.headerTip, mouseX, mouseY);
         this.headerTip = null;
      }
   }

   /** Each faction a card: its ruler's face, its name and treaty, and what it thinks of the player. */
   private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
      int x = this.left() + 8;
      int y = this.listTop();
      int width = this.listWidth() - 4;
      int rows = this.visibleRows();
      for (int i = 0; i < rows && this.scroll + i < this.entries.size(); i++) {
         PlayerDiplomacy.Entry entry = this.entries.get(this.scroll + i);
         int rowY = y + i * ROW_HEIGHT;
         int rowHeight = ROW_HEIGHT - 2;
         int color = 0xFF000000 | FactionColors.color(entry.faction());
         boolean chosen = entry.faction().equals(this.selected);
         boolean hover = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + rowHeight;
         int background = chosen ? 0xFF3A2E1E : hover ? 0xFF2C241A : CARD_COLOR;
         if (entry.treaty() == RelationState.WAR && !chosen) {
            background = hover ? 0xFF3A2020 : 0xFF2A1A18;
         }

         graphics.fill(x, rowY, x + width, rowY + rowHeight, background);
         graphics.fill(x, rowY, x + 3, rowY + rowHeight, color);
         if (chosen) {
            outline(graphics, x, rowY, width, rowHeight, GOLD_COLOR);
         }

         int[] person = entry.person();
         if (person.length >= 4) {
            PersonPortrait.draw(graphics, entry.faction(), person[0], person[1], person[3] != 0, PersonPortrait.NOBLE, 2, x + 6, rowY + 3, 18);
         }

         int textX = x + 28;
         graphics.drawString(this.font, factionName(entry.faction()), textX, rowY + 3, chosen ? GOLD_COLOR : TEXT_COLOR);
         graphics.drawString(this.font, stateName(entry.treaty()), textX, rowY + 13, MUTED_COLOR);
         Component standing = standingText(entry.standing());
         graphics.drawString(this.font, standing, x + width - 3 - this.font.width(standing), rowY + 3, TEXT_COLOR);
         this.standingBar(graphics, x + width - 27, rowY + 16, 24, 2, entry.standing());
      }

      if (this.entries.size() > rows) {
         int track = rows * ROW_HEIGHT - 2;
         int thumb = Math.max(12, track * rows / this.entries.size());
         int at = (track - thumb) * this.scroll / Math.max(1, this.entries.size() - rows);
         graphics.fill(x + width + 1, y, x + width + 3, y + track, 0xFF2C241A);
         graphics.fill(x + width + 1, y + at, x + width + 3, y + at + thumb, FRAME_COLOR);
      }
   }

   /**
    * The faction's name large in its colours beside its ruler's crowned portrait, who rules it, the
    * treaty and what it thinks of the player; then the tabs.
    */
   private void renderHeader(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int mouseX, int mouseY) {
      int x = this.contentX();
      int y = this.top() + 24;
      int right = this.contentX() + this.contentWidth();
      boolean roomy = this.roomy();
      int portrait = roomy ? 42 : 24;
      int color = 0xFF000000 | FactionColors.color(entry.faction());
      int[] person = entry.person();
      if (person.length >= 4) {
         PersonPortrait.draw(graphics, entry.faction(), person[0], person[1], person[3] != 0, PersonPortrait.NOBLE, 2, x, y + 1, portrait);
         if (mouseX >= x && mouseX < x + portrait && mouseY >= y && mouseY < y + portrait && person.length >= 5) {
            List<Component> lines = new ArrayList<>(characterTip(person[4]));
            lines.add(0, FactionRulers.name(entry.ruler()).copy().withStyle(ChatFormatting.GOLD));
            lines.add(1, Component.translatable("gui.ancientwarfare.diplomacy.age", person[1]).append(" · ").append(Component.translatable("gui.ancientwarfare.diplomacy.reign", person[2])).withStyle(ChatFormatting.GRAY));
            this.headerTip = lines;
         }
      } else {
         graphics.fill(x, y + 1, x + portrait, y + 1 + portrait, color);
      }

      int textX = x + portrait + 6;
      graphics.pose().pushPose();
      graphics.pose().translate(textX, y + 1, 0.0F);
      graphics.pose().scale(1.5F, 1.5F, 1.0F);
      graphics.drawString(this.font, factionName(entry.faction()), 0, 0, TEXT_COLOR);
      graphics.pose().popPose();
      Component ruler = Component.translatable("gui.ancientwarfare.diplomacy.ruler", FactionRulers.name(entry.ruler()), FactionRulers.traitName(entry.trait()));
      graphics.drawString(this.font, ruler, textX, y + 15, GOLD_COLOR);
      // Whether the leader is near, or on the deal tab how far the player's word is taken, at the top right.
      Component leader = Component.translatable(entry.leaderNear() ? "gui.ancientwarfare.diplomacy.leader_here" : "gui.ancientwarfare.diplomacy.leader_away_short");
      int statusY = y + 2;
      if (tab == DEAL && this.deal != null && entry.leaderNear()) {
         this.renderWord(graphics, right, statusY, !roomy, mouseX, mouseY);
      } else {
         int leaderX = right - this.font.width(leader) - 10;
         graphics.fill(leaderX, statusY + 2, leaderX + 5, statusY + 7, entry.leaderNear() ? GOOD_COLOR : 0xFF5A5248);
         graphics.drawString(this.font, leader, leaderX + 9, statusY, entry.leaderNear() ? GOOD_COLOR : MUTED_COLOR);
      }

      if (roomy) {
         // The treaty as a seal, and the player's standing as a bar.
         int chipY = y + 28;
         Component state = stateName(entry.treaty());
         int chipWidth = this.font.width(state) + 8;
         graphics.fill(textX, chipY, textX + chipWidth, chipY + 12, 0xFF000000 | stateColor(entry.treaty()) & 0xFF7F7F7F);
         graphics.fill(textX + 1, chipY + 1, textX + chipWidth - 1, chipY + 11, CARD_COLOR);
         graphics.drawString(this.font, state, textX + 4, chipY + 2, TEXT_COLOR);
         int barX = textX + chipWidth + 8;
         Component opinion = Component.translatable("gui.ancientwarfare.diplomacy.opinion");
         graphics.drawString(this.font, opinion, barX, chipY + 2, MUTED_COLOR);
         barX += this.font.width(opinion) + 5;
         Component standing = standingText(entry.standing());
         int barWidth = Math.max(30, Math.min(100, right - barX - this.font.width(standing) - 6));
         this.standingBar(graphics, barX, chipY + 3, barWidth, 5, entry.standing());
         graphics.drawString(this.font, standing, barX + barWidth + 5, chipY + 2, TEXT_COLOR);
      }

      int tabY = this.tabY();
      graphics.fill(x, tabY + 15, right, tabY + 16, FRAME_COLOR);
      for (int i = 0; i < TABS.length; i++) {
         int tabX = this.tabX(i);
         int width = this.tabWidth(i);
         boolean active = tab == i;
         boolean hover = mouseX >= tabX && mouseX < tabX + width && mouseY >= tabY && mouseY < tabY + 16;
         graphics.fill(tabX, tabY, tabX + width, tabY + (active ? 16 : 15), active ? FRAME_COLOR : 0xFF3A2E1E);
         graphics.fill(tabX + 1, tabY + 1, tabX + width - 1, tabY + (active ? 16 : 15), active ? 0xFF3A2E1E : hover ? 0xFF2C241A : CARD_COLOR);
         if (active) {
            graphics.fill(tabX + 1, tabY + 1, tabX + width - 1, tabY + 2, GOLD_COLOR);
         }

         graphics.drawCenteredString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.tab." + TABS[i]), tabX + width / 2, tabY + 4, active ? GOLD_COLOR : hover ? TEXT_COLOR : MUTED_COLOR);
      }
   }

   /** A heading over part of a tab, with a brass rule after it. */
   private int section(GuiGraphics graphics, Component label, int x, int y, int width) {
      graphics.drawString(this.font, label, x, y, GOLD_COLOR);
      graphics.fill(x + this.font.width(label) + 4, y + 4, x + width, y + 5, 0xFF4A3B26);
      return y + 12;
   }

   private void renderOverview(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int mouseX, int mouseY) {
      int x = this.contentX();
      int y = this.contentY();
      int width = this.contentWidth();
      int actions = this.top() + this.panelHeight() - 8 - 3 * 21;
      if (!this.roomy()) {
         // No room in the header for the standing: it heads the overview instead.
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.opinion"), x, y, MUTED_COLOR);
         int barX = x + 70;
         int barWidth = width - 70 - 34;
         this.standingBar(graphics, barX, y + 1, barWidth, 6, entry.standing());
         graphics.drawString(this.font, standingText(entry.standing()), barX + barWidth + 6, y, TEXT_COLOR);
         y += 12;
      }

      // Treasury, stores and towns, each in its own small card.
      int[] stores = entry.stores();
      if (stores.length >= 4) {
         ItemStack[] icons = {CoinItem.create(CoinMetal.GOLD), new ItemStack(Items.OAK_LOG), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.BELL)};
         int[] amounts = {stores[0] / 10, stores[1], stores[2], stores[3], entry.settlements()};
         String[] tips = {"treasury", "wood", "stone", "iron", "towns"};
         int step = (width + 3) / icons.length;
         for (int i = 0; i < icons.length; i++) {
            int cardX = x + i * step;
            graphics.fill(cardX, y, cardX + step - 3, y + 20, CARD_COLOR);
            outline(graphics, cardX, y, step - 3, 20, 0xFF3A2E1E);
            graphics.renderItem(icons[i], cardX + 2, y + 2);
            graphics.drawString(this.font, Integer.toString(amounts[i]), cardX + 20, y + 6, TEXT_COLOR);
            if (mouseX >= cardX && mouseX < cardX + step - 3 && mouseY >= y && mouseY < y + 20) {
               List<Component> tip = new ArrayList<>();
               tip.add(Component.translatable("gui.ancientwarfare.diplomacy.stat." + tips[i]));
               if (!this.roomy()) {
                  // No room for the trade goods' own row: they are listed here.
                  tip.addAll(this.tradeGoodsTip(stores));
               }

               this.headerTip = tip;
            }
         }

         y += this.roomy() ? 22 : 22;
         int kinds = net.shadowmage.ancientwarfare.core.faction.FactionResources.KINDS;
         if (this.roomy() && stores.length == kinds + 2) {
            int first = net.shadowmage.ancientwarfare.core.faction.FactionResources.GRAIN;
            int count = kinds - first;
            int goodsStep = (width + 3) / count;
            for (int i = 0; i < count; i++) {
               int kind = first + i;
               int cardX = x + i * goodsStep;
               graphics.fill(cardX, y, cardX + goodsStep - 3, y + 20, CARD_COLOR);
               outline(graphics, cardX, y, goodsStep - 3, 20, kind == stores[stores.length - 1] ? GOLD_COLOR : 0xFF3A2E1E);
               graphics.renderItem(GOODS_ICONS[kind], cardX + 2, y + 2);
               graphics.drawString(this.font, Integer.toString(stores[1 + kind]), cardX + 20, y + 6, TEXT_COLOR);
               if (mouseX >= cardX && mouseX < cardX + goodsStep - 3 && mouseY >= y && mouseY < y + 20) {
                  List<Component> tip = new ArrayList<>();
                  tip.add(Component.translatable("trade.ancientwarfare.good." + net.shadowmage.ancientwarfare.core.faction.FactionResources.NAMES[kind]));
                  if (kind == stores[stores.length - 1]) {
                     tip.add(Component.translatable("gui.ancientwarfare.deal.goods_specialty").withStyle(ChatFormatting.YELLOW));
                  }

                  this.headerTip = tip;
               }
            }

            y += 22;
         }

         y += 3;
      }

      // Headings only where there is room for them.
      if (this.roomy()) {
         y = this.section(graphics, Component.translatable("gui.ancientwarfare.diplomacy.section.affairs"), x, y, width);
      }

      Component trade = entry.trade()
         ? Component.translatable("gui.ancientwarfare.diplomacy.trade", entry.routes())
         : Component.translatable("gui.ancientwarfare.diplomacy.trade_none");
      if (!this.roomy()) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.state", stateName(entry.treaty())), x, y, TEXT_COLOR);
         graphics.drawString(this.font, trade, x + width / 2, y, entry.trade() ? GOOD_COLOR : MUTED_COLOR);
      } else {
         graphics.drawString(this.font, Component.literal("◆ ").append(trade), x, y, entry.trade() ? GOOD_COLOR : MUTED_COLOR);
      }

      y += 12;
      if (entry.wars().isEmpty()) {
         graphics.drawString(this.font, Component.literal("◆ ").append(Component.translatable("gui.ancientwarfare.diplomacy.wars_none")), x, y, MUTED_COLOR);
         y += 12;
      } else {
         Component label = Component.literal("◆ ").append(Component.translatable("gui.ancientwarfare.diplomacy.wars_label"));
         graphics.drawString(this.font, label, x, y, BAD_COLOR);
         int wx = x + this.font.width(label) + 4;
         int wy = y;
         for (String war : entry.wars()) {
            Component name = factionName(war);
            int w = this.font.width(name) + 12;
            if (wx + w > x + width) {
               wx = x + 8;
               wy += 12;
            }

            int warColor = 0xFF000000 | FactionColors.color(war);
            graphics.fill(wx, wy - 1, wx + w, wy + 10, 0xFF2A1A18);
            graphics.fill(wx, wy - 1, wx + 3, wy + 10, warColor);
            graphics.drawString(this.font, name, wx + 7, wy + 1, TEXT_COLOR);
            wx += w + 3;
         }

         y = wy + 13;
      }

      // With room to spare, the ruler himself: his years, his health and his character.
      int[] person = entry.person();
      if (this.roomy() && person.length >= 5) {
         y = this.section(graphics, Component.translatable("gui.ancientwarfare.diplomacy.section.ruler"), x, y + 2, width);
         boolean ill = person[3] != 0;
         MutableComponent life = Component.literal("◆ ")
            .append(Component.translatable("gui.ancientwarfare.diplomacy.age", person[1]))
            .append(" · ")
            .append(Component.translatable("gui.ancientwarfare.diplomacy.reign", person[2]))
            .append(" · ");
         life.append(
            Component.translatable(ill ? "gui.ancientwarfare.diplomacy.health.ill" : person[1] >= 60 ? "gui.ancientwarfare.diplomacy.health.old" : "gui.ancientwarfare.diplomacy.health.well")
               .withStyle(ill ? ChatFormatting.RED : person[1] >= 60 ? ChatFormatting.GRAY : ChatFormatting.GREEN)
         );
         graphics.drawString(this.font, life, x, y, TEXT_COLOR);
         y += 12;
         graphics.drawString(this.font, "◆", x, y, TEXT_COLOR);
         this.courtTip = null;
         this.renderCharacter(graphics, person[4], x + this.font.width("◆ "), y, mouseX, mouseY);
         if (this.courtTip != null) {
            this.headerTip = this.courtTip;
         }

         y += 12;
      }

      // Bonds of blood, feasts and grudges, as many as fit above the actions.
      if (!entry.bonds().isEmpty() && this.roomy() && y + 22 < actions) {
         y = this.section(graphics, Component.translatable("gui.ancientwarfare.diplomacy.section.bonds"), x, y + 2, width);
      }

      if (!entry.bonds().isEmpty()) {
         for (Component bond : entry.bonds()) {
            int lines = this.font.split(bond, width - 8).size();
            if (y + lines * this.font.lineHeight > actions - 3) {
               break;
            }

            graphics.drawString(this.font, "•", x, y, MUTED_COLOR);
            graphics.drawWordWrap(this.font, bond, x + 8, y, width - 8, TEXT_COLOR);
            y += lines * this.font.lineHeight + 2;
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

   /** The ruler's own card above the court: his portrait, age, reign and health. */
   private void renderRuler(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int mouseX, int mouseY) {
      int x = this.contentX();
      int y = this.contentY();
      int width = this.contentWidth();
      int[] person = entry.person();
      graphics.fill(x, y, x + width, y + RULER_CARD - 4, CARD_COLOR);
      outline(graphics, x, y, width, RULER_CARD - 4, 0xFF6A5330);
      if (person.length < 4) {
         return;
      }

      PersonPortrait.draw(graphics, entry.faction(), person[0], person[1], person[3] != 0, PersonPortrait.NOBLE, 2, x + 3, y + 7, 30);
      int textX = x + 38;
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.ruler_card", FactionRulers.name(entry.ruler()), FactionRulers.traitName(entry.trait())), textX, y + 3, GOLD_COLOR);
      graphics.drawString(
         this.font,
         Component.translatable("gui.ancientwarfare.diplomacy.age", person[1]).append(" · ").append(Component.translatable("gui.ancientwarfare.diplomacy.reign", person[2])),
         textX,
         y + 13,
         TEXT_COLOR
      );
      boolean ill = person[3] != 0;
      graphics.drawString(this.font, Component.translatable(ill ? "gui.ancientwarfare.diplomacy.health.ill" : person[1] >= 60 ? "gui.ancientwarfare.diplomacy.health.old" : "gui.ancientwarfare.diplomacy.health.well"), textX, y + 23, ill ? BAD_COLOR : person[1] >= 60 ? MUTED_COLOR : GOOD_COLOR);
      if (person.length >= 5) {
         this.renderCharacter(graphics, person[4], textX, y + 33, mouseX, mouseY);
      }

      this.renderFamily(graphics, entry, x + width - 3, y + 7, mouseX, mouseY);
   }

   /** Traits of character in a line, virtues green and vices red; hovering tells what each does. */
   private void renderCharacter(GuiGraphics graphics, int character, int x, int y, int mouseX, int mouseY) {
      MutableComponent line = Component.empty();
      List<FactionPeople.Character> traits = FactionPeople.traits(character);
      for (int i = 0; i < traits.size(); i++) {
         if (i > 0) {
            line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
         }

         line.append(traits.get(i).title().copy().withStyle(traits.get(i).virtue ? ChatFormatting.GREEN : ChatFormatting.RED));
      }

      graphics.drawString(this.font, line, x, y, TEXT_COLOR);
      if (mouseX >= x && mouseX < x + this.font.width(line) && mouseY >= y - 1 && mouseY < y + 9) {
         this.courtTip = characterTip(character);
      }
   }

   private static List<Component> characterTip(int character) {
      List<Component> lines = new ArrayList<>();
      for (FactionPeople.Character trait : FactionPeople.traits(character)) {
         lines.add(trait.title().copy().withStyle(trait.virtue ? ChatFormatting.GREEN : ChatFormatting.RED));
         lines.add(trait.description().copy().withStyle(ChatFormatting.GRAY));
      }

      return lines;
   }

   /** The ruler's wife and children at the right of his card, the children small, three to a row. */
   private void renderFamily(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int right, int y, int mouseX, int mouseY) {
      int[] family = entry.family();
      if (family.length < 1) {
         return;
      }

      boolean wife = family[0] != 0;
      int people = (family.length - 1) / 3;
      Component tip = null;
      int childrenX = right - 3 * 15;
      for (int i = 0; i < people; i++) {
         boolean female = family[1 + i * 3] != 0;
         int look = family[2 + i * 3];
         int age = family[3 + i * 3];
         int px;
         int py;
         int size;
         if (wife && i == 0) {
            size = 30;
            px = childrenX - 4 - size;
            py = y;
         } else {
            int child = i - (wife ? 1 : 0);
            size = 14;
            px = childrenX + (child % 3) * 15;
            py = y + (child / 3) * 16;
         }

         PersonPortrait.draw(graphics, entry.faction(), look, age, false, female ? PersonPortrait.WOMAN : PersonPortrait.NOBLE, 0, px, py, size);
         if (mouseX >= px && mouseX < px + size && mouseY >= py && mouseY < py + size && i < entry.familyTips().size()) {
            tip = entry.familyTips().get(i);
         }
      }

      if (tip != null) {
         this.courtTip = List.of(tip);
      }
   }

   /** The court as four cards, each courtier's mind and favour; then what the faction wants. */
   private void renderCourt(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int mouseX, int mouseY) {
      this.courtTip = null;
      this.renderRuler(graphics, entry, mouseX, mouseY);
      FactionCourt.Role[] roles = FactionCourt.Role.values();
      int[] court = entry.court();
      int view = FactionCourt.COURT_VIEW;
      int cardWidth = (this.contentWidth() - 4) / 2;
      for (int i = 0; i < roles.length && (i + 1) * view <= court.length; i++) {
         int x = this.contentX() + (i % 2) * (cardWidth + 4);
         int y = this.contentY() + RULER_CARD + (i / 2) * COURT_CARD;
         graphics.fill(x, y, x + cardWidth, y + COURT_CARD - 4, CARD_COLOR);
         outline(graphics, x, y, cardWidth, COURT_CARD - 4, 0xFF4A3B26);
         graphics.drawString(this.font, FactionCourt.roleName(roles[i]), x + 4, y + 3, GOLD_COLOR);
         graphics.drawString(this.font, FactionRulers.name(court[i * view]).copy().append(" (" + court[i * view + 4] + ")"), x + 4, y + 13, TEXT_COLOR);
         graphics.drawString(this.font, FactionRulers.traitName(court[i * view + 1]), x + 4, y + 23, MUTED_COLOR);
         this.standingBar(graphics, x + 4, y + 33, cardWidth - 54, 3, court[i * view + 2] * 2);
         boolean heir = roles[i] == FactionCourt.Role.HEIR;
         PersonPortrait.draw(graphics, entry.faction(), court[i * view + 3], court[i * view + 4], false, heir ? PersonPortrait.NOBLE : PersonPortrait.COURT, heir ? 1 : 0, x + cardWidth - 23, y + 2, 20);
         if (mouseX >= x + cardWidth - 23 && mouseX < x + cardWidth - 3 && mouseY >= y + 2 && mouseY < y + 22) {
            List<Component> lines = new ArrayList<>(characterTip(court[i * view + 5]));
            lines.add(0, FactionRulers.name(court[i * view]).copy().withStyle(ChatFormatting.GOLD));
            this.courtTip = lines;
         }
      }

      int x = this.contentX();
      int y = this.contentY() + RULER_CARD + 2 * COURT_CARD + 4;
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.goals").withStyle(ChatFormatting.BOLD), x, y, GOLD_COLOR);
      y += 12;
      if (entry.goals().isEmpty()) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.goals_none"), x, y, MUTED_COLOR);
      }

      for (Component goal : entry.goals()) {
         Component line = Component.literal("◆ ").append(goal);
         if (y + this.font.split(line, this.contentWidth()).size() * this.font.lineHeight > this.top() + this.panelHeight() - 6) {
            break;
         }

         graphics.drawWordWrap(this.font, line, x, y, this.contentWidth(), 0xFF9FD7FF);
         y += this.font.split(line, this.contentWidth()).size() * this.font.lineHeight + 3;
      }

      if (this.courtTip != null) {
         graphics.renderComponentTooltip(this.font, this.courtTip, mouseX, mouseY);
      }
   }

   private static final ItemStack[] GOODS_ICONS = {
      new ItemStack(Items.OAK_LOG), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.WHEAT),
      new ItemStack(Items.LEATHER), new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.COAL), new ItemStack(Items.COPPER_INGOT)
   };

   /** The trade goods' stores, one a line, the faction's own marked. */
   private List<Component> tradeGoodsTip(int[] stores) {
      List<Component> lines = new ArrayList<>();
      int kinds = net.shadowmage.ancientwarfare.core.faction.FactionResources.KINDS;
      if (stores.length != kinds + 2) {
         return lines;
      }

      lines.add(Component.translatable("gui.ancientwarfare.diplomacy.stat.goods").withStyle(ChatFormatting.GOLD));
      for (int kind = net.shadowmage.ancientwarfare.core.faction.FactionResources.GRAIN; kind < kinds; kind++) {
         MutableComponent line = Component.translatable("trade.ancientwarfare.good." + net.shadowmage.ancientwarfare.core.faction.FactionResources.NAMES[kind]).append(": " + stores[1 + kind]);
         if (kind == stores[stores.length - 1]) {
            line.append(" ★");
         }

         lines.add(line.withStyle(kind == stores[stores.length - 1] ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
      }

      return lines;
   }

   /** The top of the goods table on the deal tab. */
   private int goodsY() {
      return this.contentY() + 14 + DEAL_ROW * 4 + 4;
   }

   private int goodsLabelWidth() {
      return Math.max(this.font.width(Component.translatable("gui.ancientwarfare.deal.goods_mine")), this.font.width(Component.translatable("gui.ancientwarfare.deal.goods_theirs"))) + 6;
   }

   private int goodsCellWidth() {
      return (this.contentWidth() - this.goodsLabelWidth()) / FactionDeals.KINDS;
   }

   /** The goods table's cell under the mouse: the kind, plus KINDS for what the faction gives; -1 for none, -2 - kind for a heading. */
   private int goodsCell(double mouseX, double mouseY) {
      int x = this.contentX() + this.goodsLabelWidth();
      int y = this.goodsY();
      int cell = this.goodsCellWidth();
      if (mouseX < x || mouseX >= x + cell * FactionDeals.KINDS || mouseY < y || mouseY >= y + 43) {
         return -1;
      }

      int kind = (int)((mouseX - x) / cell);
      if (mouseY < y + 18) {
         return -2 - kind;
      }

      return mouseY < y + 31 ? kind : FactionDeals.KINDS + kind;
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
      graphics.fill(x - 2, y - 1, x + column + 2, y + 14 + DEAL_ROW * 4, CARD_COLOR);
      graphics.fill(theirs - 2, y - 1, theirs + column + 2, y + 14 + DEAL_ROW * 4, CARD_COLOR);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.mine").withStyle(ChatFormatting.BOLD), x + 2, y + 2, GOLD_COLOR);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.theirs").withStyle(ChatFormatting.BOLD), theirs + 2, y + 2, GOLD_COLOR);
      y += 14;
      ItemStack coin = CoinItem.create(CoinMetal.GOLD);
      this.amountRow(graphics, coin, Component.translatable("gui.ancientwarfare.deal.gold_short", this.playerGold), x, y);
      this.amountRow(graphics, CoinItem.create(CoinMetal.SILVER), Component.translatable("gui.ancientwarfare.deal.tribute", this.tribute), x, y + DEAL_ROW);
      this.amountRow(graphics, coin, Component.translatable("gui.ancientwarfare.deal.gold_short", this.factionGold), theirs, y);
      if (mouseX >= x && mouseX < x + column - 36 && mouseY >= y + DEAL_ROW && mouseY < y + DEAL_ROW + 16) {
         graphics.renderTooltip(this.font, Component.translatable("gui.ancientwarfare.deal.tribute.tip", FactionDeals.TRIBUTE_WEEKS), mouseX, mouseY);
      }

      // The goods: a column for each kind, what the player gives above, what the faction gives below.
      int gy = this.goodsY();
      int label = this.goodsLabelWidth();
      int cell = this.goodsCellWidth();
      int[] stores = entry.stores();
      int specialty = stores.length == FactionDeals.KINDS + 2 ? stores[stores.length - 1] : -1;
      graphics.fill(x - 2, gy - 2, x + label + cell * FactionDeals.KINDS + 1, gy + 44, CARD_COLOR);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.goods_mine"), x + 1, gy + 20, MUTED_COLOR);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.deal.goods_theirs"), x + 1, gy + 33, MUTED_COLOR);
      int hovered = this.goodsCell(mouseX, mouseY);
      for (int kind = 0; kind < FactionDeals.KINDS; kind++) {
         int cx = x + label + kind * cell;
         if (kind == specialty) {
            outline(graphics, cx, gy - 1, cell - 1, 18, GOLD_COLOR);
         }

         graphics.renderItem(GOODS_ICONS[kind], cx + (cell - 17) / 2, gy);
         for (int side = 0; side < 2; side++) {
            int slot = side * FactionDeals.KINDS + kind;
            int cy = gy + 18 + side * 13;
            boolean hover = hovered == slot;
            graphics.fill(cx, cy, cx + cell - 1, cy + 12, hover ? 0xFF3A2E1E : 0xFF18130E);
            int amount = this.goods[slot] * FactionDeals.GOODS_STEP;
            String text = amount > 0 ? Integer.toString(amount) : "·";
            graphics.drawCenteredString(this.font, text, cx + cell / 2, cy + 2, amount > 0 ? (side == 0 ? BAD_COLOR : GOOD_COLOR) : MUTED_COLOR);
         }
      }

      if (hovered != -1) {
         int kind = hovered >= 0 ? hovered % FactionDeals.KINDS : -2 - hovered;
         List<Component> lines = new ArrayList<>();
         lines.add(Component.translatable("trade.ancientwarfare.good." + net.shadowmage.ancientwarfare.core.faction.FactionResources.NAMES[kind]).withStyle(ChatFormatting.GOLD));
         if (1 + kind < stores.length) {
            lines.add(Component.translatable("gui.ancientwarfare.deal.goods_store", stores[1 + kind]).withStyle(ChatFormatting.GRAY));
         }

         if (kind == specialty) {
            lines.add(Component.translatable("gui.ancientwarfare.deal.goods_specialty").withStyle(ChatFormatting.YELLOW));
         }

         lines.add(Component.translatable("gui.ancientwarfare.deal.goods_hint", FactionDeals.GOODS_STEP, FactionDeals.GOODS_STEP * 4).withStyle(ChatFormatting.DARK_GRAY));
         this.headerTip = lines;
      }

      int answerY = gy + 47;
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
   private void renderWord(GuiGraphics graphics, int right, int y, boolean brief, int mouseX, int mouseY) {
      int word = this.deal.reliability();
      Component wordName = Component.translatable(
            "gui.ancientwarfare.deal.word." + (word <= FactionDeals.NO_TRUST ? "none" : word < 0 ? "doubtful" : word == 0 ? "untried" : word < 3 ? "good" : "iron")
         )
         .withStyle(word < 0 ? ChatFormatting.RED : word > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY);
      // Where room is short, the promises are only on hover.
      MutableComponent wordLine = Component.translatable("gui.ancientwarfare.deal.word", wordName);
      if (!brief) {
         wordLine.append(Component.translatable("gui.ancientwarfare.deal.promise_count", this.deal.promises().size()).withStyle(ChatFormatting.GRAY));
      }

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
