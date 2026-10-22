package net.shadowmage.ancientwarfare.core.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.faction.FactionConquest;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/** The fate of a settlement whose garrison the player has just killed. */
public class ConquestScreen extends Screen {
   private static final int PANEL_WIDTH = 320;
   private static final int PANEL_HEIGHT = 196;
   private static final int BUTTON_WIDTH = 104;
   private static final int ROW_HEIGHT = 30;
   private static final int TEXT_COLOR = -1;
   private static final int MUTED_COLOR = -4276546;
   private final FactionConquest.Offer offer;

   private ConquestScreen(FactionConquest.Offer offer) {
      super(Component.translatable("gui.ancientwarfare.conquest.title"));
      this.offer = offer;
   }

   public static void open(FactionConquest.Offer offer) {
      Minecraft.getInstance().setScreen(new ConquestScreen(offer));
   }

   @Override
   protected void init() {
      int left = this.left();
      int top = this.top() + 42;
      FactionConquest.Choice[] choices = FactionConquest.Choice.values();

      for (int i = 0; i < choices.length; i++) {
         FactionConquest.Choice choice = choices[i];
         this.addRenderableWidget(
            Button.builder(Component.translatable("gui.ancientwarfare.conquest." + key(choice)), ignored -> {
               AWCoreNetwork.sendConquestChoice(this.offer.settlement(), choice);
               this.onClose();
            }).bounds(left + 10, top + i * ROW_HEIGHT, BUTTON_WIDTH, 20).build()
         );
      }

      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.conquest.later"), ignored -> this.onClose())
            .bounds(left + (PANEL_WIDTH - BUTTON_WIDTH) / 2, this.top() + PANEL_HEIGHT - 26, BUTTON_WIDTH, 20)
            .build()
      );
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = this.left();
      int top = this.top();
      graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101010);
      graphics.drawCenteredString(this.font, this.title, left + PANEL_WIDTH / 2, top + 8, TEXT_COLOR);
      Component faction = DiplomacyScreen.factionName(this.offer.faction());
      graphics.drawCenteredString(
         this.font,
         Component.translatable("gui.ancientwarfare.conquest.subtitle", faction, this.offer.name(), this.offer.population()),
         left + PANEL_WIDTH / 2,
         top + 22,
         MUTED_COLOR
      );
      FactionConquest.Choice[] choices = FactionConquest.Choice.values();
      int textX = left + BUTTON_WIDTH + 18;
      int textWidth = PANEL_WIDTH - BUTTON_WIDTH - 26;

      for (int i = 0; i < choices.length; i++) {
         Component text = Component.translatable("gui.ancientwarfare.conquest." + key(choices[i]) + ".desc", this.amount(choices[i]));
         graphics.drawWordWrap(this.font, text, textX, top + 42 + i * ROW_HEIGHT, textWidth, TEXT_COLOR);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private int amount(FactionConquest.Choice choice) {
      return switch (choice) {
         case ANNEX -> this.offer.tax();
         case RANSOM -> this.offer.ransom();
         case PLUNDER -> this.offer.loot();
         case RAZE -> this.offer.razeLoot();
      };
   }

   private static String key(FactionConquest.Choice choice) {
      return choice.name().toLowerCase(Locale.ROOT);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private int left() {
      return (this.width - PANEL_WIDTH) / 2;
   }

   private int top() {
      return (this.height - PANEL_HEIGHT) / 2;
   }
}
