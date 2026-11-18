package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/** A townsperson tells the player of something happening in the town; the player answers with a button. */
public class EncounterScreen extends Screen {
   private static final int PANEL_WIDTH = 300;
   private static final int ROW_HEIGHT = 26;
   private final AWCoreNetwork.EncounterPacket packet;

   private EncounterScreen(AWCoreNetwork.EncounterPacket packet) {
      super(packet.speaker());
      this.packet = packet;
   }

   public static void open(AWCoreNetwork.EncounterPacket packet) {
      Minecraft.getInstance().setScreen(new EncounterScreen(packet));
   }

   private int textHeight() {
      return this.font.split(this.packet.text(), PANEL_WIDTH - 20).size() * 10;
   }

   private int panelHeight() {
      return 34 + this.textHeight() + 10 + this.packet.keys().size() * ROW_HEIGHT + 8;
   }

   @Override
   protected void init() {
      int left = (this.width - PANEL_WIDTH) / 2;
      int top = (this.height - this.panelHeight()) / 2 + 34 + this.textHeight() + 10;
      for (int i = 0; i < this.packet.keys().size(); i++) {
         String key = this.packet.keys().get(i);
         Component label = this.packet.labels().get(i);
         Component cost = this.packet.costs().get(i);
         Component text = cost.getString().isEmpty() ? label : label.copy().append(" (").append(cost).append(")");
         this.addRenderableWidget(AwUi.button(text, ignored -> {
            Minecraft.getInstance().player.connection.sendCommand("awtown " + this.packet.id() + " " + key);
            this.onClose();
         }).bounds(left + 10, top + i * ROW_HEIGHT, PANEL_WIDTH - 20, 20).build());
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = (this.width - PANEL_WIDTH) / 2;
      int top = (this.height - this.panelHeight()) / 2;
      graphics.fill(left, top, left + PANEL_WIDTH, top + this.panelHeight(), 0xE0101010);
      graphics.drawCenteredString(this.font, this.title, left + PANEL_WIDTH / 2, top + 10, 0xFFFFD27F);
      graphics.drawWordWrap(this.font, this.packet.text(), left + 10, top + 30, PANEL_WIDTH - 20, -1);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
