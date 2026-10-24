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
   protected void init() {
      super.init();
      this.registerLayoutRect("mailbox name", 8, 8, 83, 9, true);
      this.registerLayoutRect("target name", 8, 56, 83, 9, true);
      this.legacyButton("select name", 95, 8, 75, 12, Component.translatable("guistrings.automation.mailbox_name_select"), button -> this.openNames(false));
      this.legacyButton("select target", 95, 56, 75, 12, Component.translatable("guistrings.automation.mailbox_target_select"), button -> this.openNames(true));
      this.legacyButton("set sides", 95, 192, 75, 12, Component.translatable("guistrings.inventory.setsides"), button -> {
         if (this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(((MailboxMenu)this.menu).containerId, 0);
         }
      });
      this.autoExport = this.addLegacyWidget(
         "auto export",
         new LegacyCheckbox(
            this.leftPos + 8,
            this.topPos + 224 - 32,
            12,
            Component.translatable("guistrings.automation.auto_output"),
            ((MailboxMenu)this.menu).box.isAutoExport(),
            checked -> {
               ((MailboxMenu)this.menu).box.setAutoExport(checked);
               AWCorePlatform.sendMailboxConfig(((MailboxMenu)this.menu).box.getBlockPos(), 3, "", checked);
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
            this.leftPos + 8,
            this.topPos + 224 - 20,
            12,
            Component.translatable("guistrings.automation.private_mailbox"),
            ((MailboxMenu)this.menu).box.isPrivateBox(),
            checked -> {
               ((MailboxMenu)this.menu).box.setPrivateBox(checked);
               AWCorePlatform.sendMailboxConfig(((MailboxMenu)this.menu).box.getBlockPos(), 2, "", checked);
            }
         ),
         8,
         204,
         12,
         12
      );
   }

   private void openNames(boolean target) {
      this.minecraft
         .setScreen(
            (Screen)(target
               ? new MailboxTargetScreen(this, ((MailboxMenu)this.menu).box)
               : new MailboxNameSelectScreen(this, ((MailboxMenu)this.menu).box))
         );
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, this.fittedName(((MailboxMenu)this.menu).box.getMailboxName()), 8, 8, -12566464, false);
      graphics.drawString(this.font, this.fittedName(((MailboxMenu)this.menu).box.getTargetName()), 8, 56, -12566464, false);
      this.autoExport.setChecked(((MailboxMenu)this.menu).box.isAutoExport());
      this.privateBox.setChecked(((MailboxMenu)this.menu).box.isPrivateBox());
   }

   private String fittedName(String value) {
      int maxWidth = 83;
      if (this.font.width(value) <= maxWidth) {
         return value;
      } else {
         String ellipsis = "...";
         return this.font.plainSubstrByWidth(value, maxWidth - this.font.width(ellipsis)) + ellipsis;
      }
   }
}
