package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.tile.MailboxBlockEntity;

public class MailboxNameSelectScreen extends Screen {
   private static final int GUI_WIDTH = 256;
   private static final int GUI_HEIGHT = 80;
   private final Screen parent;
   private final MailboxBlockEntity box;
   private EditBox input;
   private int left;
   private int top;

   public MailboxNameSelectScreen(Screen parent, MailboxBlockEntity box) {
      super(Component.translatable("guistrings.automation.mailbox_name"));
      this.parent = parent;
      this.box = box;
   }

   protected void init() {
      this.left = (this.width - 256) / 2;
      this.top = (this.height - 80) / 2;
      String name = this.input == null ? this.box.getMailboxName() : this.input.getValue();
      this.input = new EditBox(this.font, this.left + 8, this.top + 24, 240, 16, this.title);
      this.input.setMaxLength(128);
      this.input.setValue(name);
      this.addRenderableWidget(this.input);
      this.addRenderableWidget(
         new LegacyTexturedButton(
            this.left + 8, this.top + 56, 112, 12, Component.translatable("guistrings.automation.mailbox_save_name"), button -> this.saveName()
         )
      );
      this.addRenderableWidget(new LegacyTexturedButton(this.left + 136, this.top + 56, 112, 12, Component.translatable("guistrings.npc.back"), button -> this.onClose()));
      this.setInitialFocus(this.input);
   }

   private void saveName() {
      String name = this.input.getValue().trim();
      this.box.setMailboxName(name);
      AWCorePlatform.sendMailboxConfig(this.box.getBlockPos(), 0, name, false);
      this.onClose();
   }

   public void tick() {
      this.input.tick();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if ((keyCode == 257 || keyCode == 335) && this.input.isFocused()) {
         this.saveName();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      LegacyGuiStyle.background(graphics, this.left, this.top, 256, 80);
      graphics.drawString(this.font, this.title, this.left + 8, this.top + 8, -1, false);
      super.render(graphics, mouseX, mouseY, partialTick);
   }
}
