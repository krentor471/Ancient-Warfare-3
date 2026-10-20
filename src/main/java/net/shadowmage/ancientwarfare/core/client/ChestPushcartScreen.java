package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.ChestPushcartMenu;

public class ChestPushcartScreen extends AbstractContainerScreen<ChestPushcartMenu> {
   private static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   private static final ResourceLocation WIDGETS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");
   private static final int TEXT_COLOR = 16777215;
   private Button upButton;
   private Button downButton;

   public ChestPushcartScreen(ChestPushcartMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.f_97726_ = 195;
      this.f_97727_ = 212;
      this.f_97728_ = 8;
      this.f_97729_ = 8;
      this.f_97731_ = 116;
   }

   protected void m_7856_() {
      super.m_7856_();
      this.upButton = (Button)this.m_142416_(Button.m_253074_(Component.m_237113_("-"), button -> {
         this.f_96541_.f_91072_.m_105208_(((ChestPushcartMenu)this.f_97732_).f_38840_, ChestPushcartMenu.scrollUpButtonId());
         ((ChestPushcartMenu)this.f_97732_).scrollStorageRows(-1);
         this.updateScrollButtons();
      }).m_252987_(this.f_97735_ + 171, this.f_97736_ + 33, 16, 16).m_253136_());
      this.downButton = (Button)this.m_142416_(Button.m_253074_(Component.m_237113_("+"), button -> {
         this.f_96541_.f_91072_.m_105208_(((ChestPushcartMenu)this.f_97732_).f_38840_, ChestPushcartMenu.scrollDownButtonId());
         ((ChestPushcartMenu)this.f_97732_).scrollStorageRows(1);
         this.updateScrollButtons();
      }).m_252987_(this.f_97735_ + 171, this.f_97736_ + 71, 16, 16).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237115_("guistrings.vehicle.pack"), button -> {
         this.f_96541_.f_91072_.m_105208_(((ChestPushcartMenu)this.f_97732_).f_38840_, ChestPushcartMenu.packButtonId());
         this.f_96541_.f_91074_.m_6915_();
      }).m_252987_(this.f_97735_ + 8, this.f_97736_ + 91, 45, 18).m_253136_());
      this.updateScrollButtons();
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawNineSlice(graphics, BACKGROUND, 0, 0, this.f_97726_, this.f_97727_, 0, 0, 256, 240);
      this.drawSlotGrid(graphics, 8, 34, 9, ((ChestPushcartMenu)this.f_97732_).getVisibleStorageRows());
      this.drawSlotGrid(graphics, 62, 92, 6, 1);
      this.drawSlotGrid(graphics, 8, 128, 9, 3);
      this.drawSlotGrid(graphics, 8, 186, 9, 1);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, this.f_96539_, this.f_97728_, this.f_97729_, 16777215, false);
      if (((ChestPushcartMenu)this.f_97732_).getStorageRows() > ((ChestPushcartMenu)this.f_97732_).getVisibleStorageRows()) {
         graphics.m_280614_(
            this.f_96547_,
            Component.m_237110_(
               "guistrings.vehicle.storage_rows",
               new Object[]{((ChestPushcartMenu)this.f_97732_).getTopStorageRow() + 1, ((ChestPushcartMenu)this.f_97732_).getStorageRows()}
            ),
            8,
            23,
            16777215,
            false
         );
      }

      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.automation.upgrades"), 62, 82, 16777215, false);
      graphics.m_280614_(this.f_96547_, this.f_169604_, 8, this.f_97731_, 16777215, false);
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      this.m_280072_(graphics, mouseX, mouseY);
   }

   private void updateScrollButtons() {
      if (this.upButton != null) {
         this.upButton.f_93624_ = ((ChestPushcartMenu)this.f_97732_).getStorageRows() > ((ChestPushcartMenu)this.f_97732_).getVisibleStorageRows();
         this.upButton.f_93623_ = this.upButton.f_93624_ && ((ChestPushcartMenu)this.f_97732_).canScrollUp();
      }

      if (this.downButton != null) {
         this.downButton.f_93624_ = ((ChestPushcartMenu)this.f_97732_).getStorageRows() > ((ChestPushcartMenu)this.f_97732_).getVisibleStorageRows();
         this.downButton.f_93623_ = this.downButton.f_93624_ && ((ChestPushcartMenu)this.f_97732_).canScrollDown();
      }
   }

   private void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            graphics.m_280218_(WIDGETS, this.f_97735_ + x + col * 18 - 1, this.f_97736_ + y + row * 18 - 1, 152, 120, 18, 18);
         }
      }
   }

   private void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      graphics.m_280218_(texture, this.f_97735_ + x, this.f_97736_ + y, u, v, halfWidth, halfHeight);
      graphics.m_280218_(texture, this.f_97735_ + x + halfWidth, this.f_97736_ + y, u + texWidth - (width - halfWidth), v, width - halfWidth, halfHeight);
      graphics.m_280218_(texture, this.f_97735_ + x, this.f_97736_ + y + halfHeight, u, v + texHeight - (height - halfHeight), halfWidth, height - halfHeight);
      graphics.m_280218_(
         texture,
         this.f_97735_ + x + halfWidth,
         this.f_97736_ + y + halfHeight,
         u + texWidth - (width - halfWidth),
         v + texHeight - (height - halfHeight),
         width - halfWidth,
         height - halfHeight
      );
   }
}
