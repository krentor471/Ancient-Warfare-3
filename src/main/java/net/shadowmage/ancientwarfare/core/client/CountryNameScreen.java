package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.faction.PlayerCountry;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/** Asks the name of the country a player founds at their town hall; the hall's window stays open behind it. */
public class CountryNameScreen extends Screen {
   private static final int WIDTH = 200;
   private final TownHallScreen parent;
   private EditBox name;
   private Button found;
   private boolean returningToParent;

   public CountryNameScreen(TownHallScreen parent) {
      super(Component.translatable("gui.ancientwarfare.country.name_title"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      int left = (this.width - WIDTH) / 2;
      int top = this.height / 2 - 30;
      this.name = new EditBox(this.font, left, top + 16, WIDTH, 18, Component.translatable("gui.ancientwarfare.country.name"));
      this.name.setMaxLength(PlayerCountry.MAX_NAME);
      this.name.setResponder(text -> this.found.active = !text.isBlank());
      this.addRenderableWidget(this.name);
      this.setInitialFocus(this.name);
      this.found = this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.country.found"), button -> this.found()).bounds(left, top + 42, WIDTH / 2 - 2, 20).build()
      );
      this.found.active = false;
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.cancel"), button -> this.onClose()).bounds(left + WIDTH / 2 + 2, top + 42, WIDTH / 2 - 2, 20).build()
      );
   }

   private void found() {
      if (!this.name.getValue().isBlank()) {
         AWCoreNetwork.sendFoundCountry(this.parent.getMenu().pos(), this.name.getValue());
         this.onClose();
      }
   }

   @Override
   public boolean keyPressed(int key, int scanCode, int modifiers) {
      if ((key == 257 || key == 335) && this.found.active) {
         this.found();
         return true;
      }

      return super.keyPressed(key, scanCode, modifiers);
   }

   @Override
   public void tick() {
      this.name.tick();
      if (this.minecraft.player == null || this.minecraft.player.containerMenu != this.parent.getMenu()) {
         this.minecraft.setScreen(null);
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public void onClose() {
      if (this.minecraft.player != null && this.minecraft.player.containerMenu == this.parent.getMenu()) {
         this.returningToParent = true;
         this.minecraft.setScreen(this.parent);
      } else {
         this.minecraft.setScreen(null);
      }
   }

   @Override
   public void removed() {
      if (!this.returningToParent) {
         this.parent.deathRecordsRemoved();
      }

      super.removed();
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
