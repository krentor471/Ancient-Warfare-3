package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.shadowmage.ancientwarfare.core.faction.FactionRulers;
import net.shadowmage.ancientwarfare.core.faction.FactionSpies;
import net.shadowmage.ancientwarfare.core.faction.FactionTournaments;
import net.shadowmage.ancientwarfare.core.faction.PlayerDiplomacy;
import net.shadowmage.ancientwarfare.core.faction.RelationState;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/** The player's relations with the factions they have met, and the offers they can make. */
public class DiplomacyScreen extends Screen {
   private static final int PANEL_WIDTH = 340;
   private static final int PANEL_HEIGHT = 232;
   private static final int LIST_WIDTH = 110;
   private static final int ROW_HEIGHT = 20;
   private static final int VISIBLE_ROWS = 9;
   private static final int TEXT_COLOR = -1;
   private static final int MUTED_COLOR = -4276546;
   private List<PlayerDiplomacy.Entry> entries;
   private String selected;
   private int scroll;
   /** Showing the court and what the faction wants instead of its treaties. */
   private static boolean courtView;
   private static final int COURT_ROW = 26;

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

   @Override
   protected void init() {
      if (this.selectedEntry() == null && !this.entries.isEmpty()) {
         this.selected = this.entries.get(0).faction();
      }

      int left = this.left();
      int top = this.top();
      this.scroll = Math.max(0, Math.min(this.scroll, this.entries.size() - VISIBLE_ROWS));

      for (int i = 0; i < VISIBLE_ROWS && this.scroll + i < this.entries.size(); i++) {
         PlayerDiplomacy.Entry entry = this.entries.get(this.scroll + i);
         Button button = Button.builder(factionName(entry.faction()), ignored -> {
            this.selected = entry.faction();
            this.rebuildWidgets();
         }).bounds(left + 8, top + 24 + i * ROW_HEIGHT, LIST_WIDTH - 8, ROW_HEIGHT - 2).build();
         button.active = !entry.faction().equals(this.selected);
         this.addRenderableWidget(button);
      }

      PlayerDiplomacy.Entry entry = this.selectedEntry();
      if (entry != null) {
         this.addRenderableWidget(
            Button.builder(Component.translatable(courtView ? "gui.ancientwarfare.diplomacy.treaties" : "gui.ancientwarfare.diplomacy.court"), ignored -> {
               courtView = !courtView;
               this.rebuildWidgets();
            }).bounds(left + PANEL_WIDTH - 70, top + 22, 62, 14).build()
         );
      }

      if (entry != null && courtView) {
         PlayerDiplomacy.Action[] gifts = {
            PlayerDiplomacy.Action.COURT_HEIR, PlayerDiplomacy.Action.COURT_MARSHAL, PlayerDiplomacy.Action.COURT_TREASURER, PlayerDiplomacy.Action.COURT_PRIEST
         };
         for (int i = 0; i < gifts.length && i * 3 + 2 < entry.court().length; i++) {
            PlayerDiplomacy.Action action = gifts[i];
            Button button = Button.builder(
                  Component.translatable("gui.ancientwarfare.diplomacy.court_gift", net.shadowmage.ancientwarfare.core.faction.FactionCourt.COURTIER_GIFT),
                  ignored -> AWCoreNetwork.sendDiplomacyAction(entry.faction(), action)
               )
               .bounds(left + PANEL_WIDTH - 70, top + 44 + i * COURT_ROW, 62, 18)
               .build();
            button.active = entry.leaderNear();
            if (!entry.leaderNear()) {
               button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.ancientwarfare.diplomacy.leader_away")));
            }

            this.addRenderableWidget(button);
         }
      } else if (entry != null) {
         int x = left + LIST_WIDTH + 10;
         int y = top + PANEL_HEIGHT - 78;
         int width = (PANEL_WIDTH - LIST_WIDTH - 24) / 3;
         this.addAction(entry, PlayerDiplomacy.Action.PEACE, x, y, width, entry.treaty() == RelationState.WAR);
         this.addAction(entry, PlayerDiplomacy.Action.PACT, x + width + 2, y, width, entry.treaty() == RelationState.NEUTRAL);
         this.addAction(entry, PlayerDiplomacy.Action.ALLIANCE, x + (width + 2) * 2, y, width, entry.treaty() == RelationState.PACT);
         this.addAction(entry, PlayerDiplomacy.Action.GIFT, x, y + 22, width, true);
         this.addAction(entry, PlayerDiplomacy.Action.TRADE, x + width + 2, y + 22, width, entry.trade() || entry.treaty() != RelationState.WAR);
         this.addAction(entry, PlayerDiplomacy.Action.WAR, x + (width + 2) * 2, y + 22, width, entry.treaty() != RelationState.WAR);
         int half = (width * 3 + 2) / 2;
         this.addAction(entry, PlayerDiplomacy.Action.SPY, x, y + 44, half, true);
         this.addAction(entry, PlayerDiplomacy.Action.TOURNAMENT, x + half + 2, y + 44, half, entry.treaty() != RelationState.WAR);
      }
   }

   private void addAction(PlayerDiplomacy.Entry entry, PlayerDiplomacy.Action action, int x, int y, int width, boolean applicable) {
      String key = "gui.ancientwarfare.diplomacy.action." + action.name().toLowerCase(java.util.Locale.ROOT);
      if (action == PlayerDiplomacy.Action.TRADE && entry.trade()) {
         key += "_end";
      }

      Component label = action == PlayerDiplomacy.Action.GIFT
         ? Component.translatable(key, PlayerDiplomacy.GIFT_EMERALDS)
         : action == PlayerDiplomacy.Action.SPY
            ? Component.translatable(key, FactionSpies.SPY_EMERALDS)
            : action == PlayerDiplomacy.Action.TOURNAMENT ? Component.translatable(key, FactionTournaments.ENTRY_EMERALDS) : Component.translatable(key);
      Button button = Button.builder(label, ignored -> AWCoreNetwork.sendDiplomacyAction(entry.faction(), action)).bounds(x, y, width, 20).build();
      boolean anywhere = action == PlayerDiplomacy.Action.WAR || action == PlayerDiplomacy.Action.SPY || action == PlayerDiplomacy.Action.TRADE && entry.trade();
      String research = PlayerDiplomacy.research(action);
      boolean known = research == null || ClientResearch.has(research);
      button.active = applicable && known && (anywhere || entry.leaderNear());
      if (!known) {
         button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.shadowmage.ancientwarfare.core.research.PlayerResearch.needs(research)));
      }

      this.addRenderableWidget(button);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = this.left();
      int top = this.top();
      graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101010);
      graphics.fill(left + LIST_WIDTH + 3, top + 22, left + LIST_WIDTH + 4, top + PANEL_HEIGHT - 8, 0xFF505050);
      graphics.drawCenteredString(this.font, this.title, left + PANEL_WIDTH / 2, top + 8, TEXT_COLOR);
      if (this.entries.isEmpty()) {
         graphics.drawWordWrap(
            this.font, Component.translatable("gui.ancientwarfare.diplomacy.none"), left + 12, top + 30, PANEL_WIDTH - 24, MUTED_COLOR
         );
      }

      PlayerDiplomacy.Entry entry = this.selectedEntry();
      if (entry != null && courtView) {
         this.renderCourt(graphics, entry, left + LIST_WIDTH + 10, top + 26);
      } else if (entry != null) {
         this.renderDetails(graphics, entry, left + LIST_WIDTH + 10, top + 26);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void renderDetails(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int x, int y) {
      int width = PANEL_WIDTH - LIST_WIDTH - 20;
      graphics.drawString(this.font, factionName(entry.faction()).copy().withStyle(ChatFormatting.BOLD), x, y, TEXT_COLOR);
      y += 14;
      graphics.drawString(
         this.font,
         Component.translatable("gui.ancientwarfare.diplomacy.ruler", FactionRulers.name(entry.ruler()), FactionRulers.traitName(entry.trait())),
         x,
         y,
         0xFFFFD27F
      );
      y += 11;
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.state", stateName(entry.treaty())), x, y, TEXT_COLOR);
      y += 11;
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.standing", standingText(entry.standing())), x, y, TEXT_COLOR);
      y += 11;
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.settlements", entry.settlements()), x, y, TEXT_COLOR);
      y += 11;
      int[] stores = entry.stores();
      if (stores.length >= 4) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.stores", stores[0], stores[1], stores[2], stores[3]), x, y, MUTED_COLOR);
         y += 11;
      }

      Component trade = entry.trade()
         ? Component.translatable("gui.ancientwarfare.diplomacy.trade", entry.routes())
         : Component.translatable("gui.ancientwarfare.diplomacy.trade_none");
      graphics.drawString(this.font, trade, x, y, entry.trade() ? 0xFF80FF80 : MUTED_COLOR);
      y += 11;
      Component wars;
      if (entry.wars().isEmpty()) {
         wars = Component.translatable("gui.ancientwarfare.diplomacy.wars_none");
      } else {
         List<Component> names = new ArrayList<>();
         entry.wars().forEach(war -> names.add(factionName(war)));
         wars = Component.translatable("gui.ancientwarfare.diplomacy.wars", joined(names));
      }

      graphics.drawWordWrap(this.font, wars, x, y, width, MUTED_COLOR);
      y = this.top() + PANEL_HEIGHT - 92;
      Component leader = Component.translatable(entry.leaderNear() ? "gui.ancientwarfare.diplomacy.leader_here" : "gui.ancientwarfare.diplomacy.leader_away");
      graphics.drawString(this.font, leader, x, y, entry.leaderNear() ? 0xFF80FF80 : MUTED_COLOR);
   }

   /** The court: each courtier's role, name and mind and how they think of the player; then the faction's goals. */
   private void renderCourt(GuiGraphics graphics, PlayerDiplomacy.Entry entry, int x, int y) {
      int width = PANEL_WIDTH - LIST_WIDTH - 20;
      graphics.drawString(this.font, factionName(entry.faction()).copy().withStyle(ChatFormatting.BOLD), x, y, TEXT_COLOR);
      y += 18;
      net.shadowmage.ancientwarfare.core.faction.FactionCourt.Role[] roles = net.shadowmage.ancientwarfare.core.faction.FactionCourt.Role.values();
      int[] court = entry.court();
      for (int i = 0; i < roles.length && i * 3 + 2 < court.length; i++) {
         graphics.drawString(
            this.font,
            Component.translatable(
               "gui.ancientwarfare.diplomacy.courtier",
               net.shadowmage.ancientwarfare.core.faction.FactionCourt.roleName(roles[i]),
               FactionRulers.name(court[i * 3])
            ),
            x,
            y,
            0xFFFFD27F
         );
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.favor", FactionRulers.traitName(court[i * 3 + 1]), standingText(court[i * 3 + 2])), x, y + 10, MUTED_COLOR);
         y += COURT_ROW;
      }

      y += 2;
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.goals"), x, y, TEXT_COLOR);
      y += 11;
      if (entry.goals().isEmpty()) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.diplomacy.goals_none"), x, y, MUTED_COLOR);
      }

      for (Component goal : entry.goals()) {
         Component line = Component.literal("• ").append(goal);
         graphics.drawWordWrap(this.font, line, x, y, width, 0xFF9FD7FF);
         y += this.font.split(line, width).size() * this.font.lineHeight + 2;
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int max = Math.max(0, this.entries.size() - VISIBLE_ROWS);
      int next = Math.max(0, Math.min(max, this.scroll - (int)Math.signum(delta)));
      if (next != this.scroll) {
         this.scroll = next;
         this.rebuildWidgets();
      }

      return true;
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

   private int left() {
      return (this.width - PANEL_WIDTH) / 2;
   }

   private int top() {
      return (this.height - PANEL_HEIGHT) / 2;
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

   private static Component joined(List<Component> parts) {
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
