package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.MailboxMenu;

public class MailboxScreen extends LegacyContainerScreen<MailboxMenu> {
   private static final int GUI_WIDTH = 178;
   private static final int GUI_HEIGHT = 224;
   private static final int BUTTON_X = 95;
   private static final int BUTTON_WIDTH = 75;
   private static final int BUTTON_HEIGHT = 12;
   private LegacyCheckbox autoExport;
   private LegacyCheckbox privateBox;

   public MailboxScreen(MailboxMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 178, 224);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.registerLayoutRect("mailbox name", 8, 8, 83, 9, true);
      this.registerLayoutRect("target name", 8, 56, 83, 9, true);
      this.legacyButton("select name", 95, 8, 75, 12, Component.m_237115_("guistrings.automation.mailbox_name_select"), button -> this.openNames(false));
      this.legacyButton("select target", 95, 56, 75, 12, Component.m_237115_("guistrings.automation.mailbox_target_select"), button -> this.openNames(true));
      this.legacyButton("set sides", 95, 192, 75, 12, Component.m_237115_("guistrings.inventory.setsides"), button -> {
         if (this.f_96541_.f_91072_ != null) {
            this.f_96541_.f_91072_.m_105208_(((MailboxMenu)this.f_97732_).f_38840_, 0);
         }
      });
      this.autoExport = this.addLegacyWidget(
         "auto export",
         new LegacyCheckbox(
            this.f_97735_ + 8,
            this.f_97736_ + 224 - 32,
            12,
            Component.m_237115_("guistrings.automation.auto_output"),
            ((MailboxMenu)this.f_97732_).box.isAutoExport(),
            checked -> {
               ((MailboxMenu)this.f_97732_).box.setAutoExport(checked);
               AWCorePlatform.sendMailboxConfig(((MailboxMenu)this.f_97732_).box.m_58899_(), 3, "", checked);
            }
         ),
         8,
         192,
         12,
         12
      );
      this.privateBox = this.addLegacyWidget(
         "private mailbox",
         new LegacyCheckbox(
            this.f_97735_ + 8,
            this.f_97736_ + 224 - 20,
            12,
            Component.m_237115_("guistrings.automation.private_mailbox"),
            ((MailboxMenu)this.f_97732_).box.isPrivateBox(),
            checked -> {
               ((MailboxMenu)this.f_97732_).box.setPrivateBox(checked);
               AWCorePlatform.sendMailboxConfig(((MailboxMenu)this.f_97732_).box.m_58899_(), 2, "", checked);
            }
         ),
         8,
         204,
         12,
         12
      );
   }

   private void openNames(boolean target) {
      this.f_96541_
         .m_91152_(
            (Screen)(target
               ? new MailboxTargetScreen(this, ((MailboxMenu)this.f_97732_).box)
               : new MailboxNameSelectScreen(this, ((MailboxMenu)this.f_97732_).box))
         );
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280056_(this.f_96547_, this.fittedName(((MailboxMenu)this.f_97732_).box.getMailboxName()), 8, 8, -12566464, false);
      graphics.m_280056_(this.f_96547_, this.fittedName(((MailboxMenu)this.f_97732_).box.getTargetName()), 8, 56, -12566464, false);
      this.autoExport.setChecked(((MailboxMenu)this.f_97732_).box.isAutoExport());
      this.privateBox.setChecked(((MailboxMenu)this.f_97732_).box.isPrivateBox());
   }

   private String fittedName(String value) {
      int maxWidth = 83;
      if (this.f_96547_.m_92895_(value) <= maxWidth) {
         return value;
      } else {
         String ellipsis = "...";
         return this.f_96547_.m_92834_(value, maxWidth - this.f_96547_.m_92895_(ellipsis)) + ellipsis;
      }
   }
}
