package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.shadowmage.ancientwarfare.core.faction.PlayerCountry;
import net.shadowmage.ancientwarfare.core.faction.RelationState;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/**
 * The player's country at a glance: its treasury, tax and income, its villages with their people,
 * loyalty and order, and the factions along its borders with the treaty and standing with each.
 */
public final class CountryScreen extends Screen {
   private static final int PANEL_WIDTH = 380;
   private static final int PANEL_HEIGHT = 226;
   private static final int ROW = 11;
   private static final int ROWS = 11;
   private static final int TEXT = 0xFFFFFF;
   private static final int MUTED = 0xA0A0A0;
   private static final int HEAD = 0xFFD860;
   private static final String[] TAX_KEYS = {"low", "usual", "high"};
   private PlayerCountry.CountryView view;
   private int villageScroll;
   private int neighbourScroll;

   private CountryScreen(PlayerCountry.CountryView view) {
      super(Component.literal(view.name()));
      this.view = view;
   }

   /** Fresh data from the server: opens the window, or refreshes it when already open. */
   public static void show(PlayerCountry.CountryView view) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof CountryScreen screen) {
         screen.view = view;
         screen.rebuildWidgets();
      } else {
         minecraft.setScreen(new CountryScreen(view));
      }
   }

   private int left() {
      return (this.width - PANEL_WIDTH) / 2;
   }

   private int top() {
      return Math.max(4, (this.height - PANEL_HEIGHT) / 2);
   }

   @Override
   protected void init() {
      int left = this.left();
      int bottom = this.top() + PANEL_HEIGHT - 24;
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.diplomacy.title"), button -> AWCoreNetwork.sendDiplomacyOpen())
            .bounds(left + 8, bottom, 110, 18)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.country.refresh"), button -> AWCoreNetwork.sendCountryOpen()).bounds(left + 122, bottom, 90, 18).build()
      );
      this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(left + PANEL_WIDTH - 98, bottom, 90, 18).build());
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int step = delta > 0 ? -1 : 1;
      if (mouseX < this.left() + PANEL_WIDTH / 2) {
         this.villageScroll = clampScroll(this.villageScroll + step, this.view.villages().size());
      } else {
         this.neighbourScroll = clampScroll(this.neighbourScroll + step, this.view.neighbours().size());
      }

      return true;
   }

   private static int clampScroll(int scroll, int size) {
      return Math.max(0, Math.min(scroll, size - ROWS));
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = this.left();
      int top = this.top();
      graphics.fill(left - 2, top - 2, left + PANEL_WIDTH + 2, top + PANEL_HEIGHT + 2, 0xFF5A4A30);
      graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0201A14);
      PlayerCountry.CountryView view = this.view;
      graphics.drawCenteredString(this.font, Component.literal(view.name()).withStyle(ChatFormatting.BOLD), left + PANEL_WIDTH / 2, top + 7, HEAD);
      int x = left + 8;
      int y = top + 22;
      graphics.drawString(
         this.font,
         Component.translatable("gui.ancientwarfare.country.summary", view.days(), view.capital().getX(), view.capital().getZ(), view.provinces()),
         x,
         y,
         MUTED,
         false
      );
      y += ROW + 1;
      Component treasury = view.treasury() < 0
         ? Component.translatable("gui.ancientwarfare.country.treasury_far")
         : Component.translatable("gui.ancientwarfare.country.treasury", view.treasury());
      graphics.drawString(
         this.font,
         Component.translatable(
            "gui.ancientwarfare.country.money",
            treasury,
            view.income(),
            Component.translatable("gui.ancientwarfare.town_hall.tax." + TAX_KEYS[Math.floorMod(view.taxLevel(), TAX_KEYS.length)])
         ),
         x,
         y,
         TEXT,
         false
      );
      y += ROW + 6;
      int half = PANEL_WIDTH / 2;
      graphics.fill(left + half - 1, y - 2, left + half, top + PANEL_HEIGHT - 30, 0xFF5A4A30);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.villages", view.villages().size()), x, y, HEAD, false);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.neighbours", view.neighbours().size()), left + half + 8, y, HEAD, false);
      y += ROW + 2;
      this.renderVillages(graphics, x, y, half - 16);
      this.renderNeighbours(graphics, left + half + 8, y, half - 16);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void renderVillages(GuiGraphics graphics, int x, int y, int width) {
      List<PlayerCountry.CountryView.Village> villages = this.view.villages();
      for (int index = this.villageScroll; index < Math.min(villages.size(), this.villageScroll + ROWS); index++) {
         PlayerCountry.CountryView.Village village = villages.get(index);
         MutableComponent where = Component.literal(village.pos().getX() + ", " + village.pos().getZ());
         if (village.capital()) {
            where = Component.literal("★ ").withStyle(ChatFormatting.GOLD).append(where.withStyle(ChatFormatting.WHITE));
         }

         graphics.drawString(this.font, where, x, y, TEXT, false);
         Component stats = Component.translatable("gui.ancientwarfare.country.village_stats", village.people(), village.loyalty(), village.order());
         graphics.drawString(this.font, stats, x + width - this.font.width(stats), y, meterColor(Math.min(village.loyalty(), village.order())), false);
         y += ROW;
      }

      if (villages.isEmpty()) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.no_villages"), x, y, MUTED, false);
      }
   }

   private void renderNeighbours(GuiGraphics graphics, int x, int y, int width) {
      List<PlayerCountry.CountryView.Neighbour> neighbours = this.view.neighbours();
      RelationState[] states = RelationState.values();
      for (int index = this.neighbourScroll; index < Math.min(neighbours.size(), this.neighbourScroll + ROWS); index++) {
         PlayerCountry.CountryView.Neighbour neighbour = neighbours.get(index);
         graphics.drawString(this.font, DiplomacyScreen.factionName(neighbour.faction()), x, y, TEXT, false);
         MutableComponent state = DiplomacyScreen.stateName(states[Math.floorMod(neighbour.treaty(), states.length)]).copy();
         if (neighbour.trade()) {
            state.append(Component.literal(" $").withStyle(ChatFormatting.YELLOW));
         }

         state.append(Component.literal(" " + (neighbour.standing() > 0 ? "+" : "") + neighbour.standing()).withStyle(ChatFormatting.GRAY));
         graphics.drawString(this.font, state, x + width - this.font.width(state), y, TEXT, false);
         y += ROW;
      }

      if (neighbours.isEmpty()) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.no_neighbours"), x, y, MUTED, false);
      } else {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.border_hint"), x, this.top() + PANEL_HEIGHT - 40, MUTED, false);
      }
   }

   private static int meterColor(int value) {
      return value < 25 ? 0xFF5555 : value < 50 ? 0xFFD060 : 0x80FF80;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
