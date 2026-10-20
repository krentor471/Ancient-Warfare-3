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
      super(Component.m_237115_("guistrings.automation.mailbox_name"));
      this.parent = parent;
      this.box = box;
   }

   protected void m_7856_() {
      this.left = (this.f_96543_ - 256) / 2;
      this.top = (this.f_96544_ - 80) / 2;
      String name = this.input == null ? this.box.getMailboxName() : this.input.m_94155_();
      this.input = new EditBox(this.f_96547_, this.left + 8, this.top + 24, 240, 16, this.f_96539_);
      this.input.m_94199_(128);
      this.input.m_94144_(name);
      this.m_142416_(this.input);
      this.m_142416_(
         new LegacyTexturedButton(
            this.left + 8, this.top + 56, 112, 12, Component.m_237115_("guistrings.automation.mailbox_save_name"), button -> this.saveName()
         )
      );
      this.m_142416_(new LegacyTexturedButton(this.left + 136, this.top + 56, 112, 12, Component.m_237115_("guistrings.npc.back"), button -> this.m_7379_()));
      this.m_264313_(this.input);
   }

   private void saveName() {
      String name = this.input.m_94155_().trim();
      this.box.setMailboxName(name);
      AWCorePlatform.sendMailboxConfig(this.box.m_58899_(), 0, name, false);
      this.m_7379_();
   }

   public void m_86600_() {
      this.input.m_94120_();
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if ((keyCode == 257 || keyCode == 335) && this.input.m_93696_()) {
         this.saveName();
         return true;
      } else {
         return super.m_7933_(keyCode, scanCode, modifiers);
      }
   }

   public void m_7379_() {
      this.f_96541_.m_91152_(this.parent);
   }

   public boolean m_7043_() {
      return false;
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      LegacyGuiStyle.background(graphics, this.left, this.top, 256, 80);
      graphics.m_280614_(this.f_96547_, this.f_96539_, this.left + 8, this.top + 8, -1, false);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
   }
}
