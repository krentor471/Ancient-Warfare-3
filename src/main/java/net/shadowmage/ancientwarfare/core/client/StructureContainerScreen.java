package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.StructureContainerMenu;

public class StructureContainerScreen extends AbstractContainerScreen<StructureContainerMenu> {
   private static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   private static final ResourceLocation WIDGETS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");
   private static final int TEXT_COLOR = 16777215;

   public StructureContainerScreen(StructureContainerMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.f_97726_ = 176;
      this.f_97727_ = 128 + menu.getContainerRows() * 18;
      this.f_97728_ = 8;
      this.f_97729_ = 6;
      this.f_97731_ = 20 + menu.getContainerRows() * 18;
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.m_280509_(this.f_97735_, this.f_97736_, this.f_97735_ + this.f_97726_, this.f_97736_ + this.f_97727_, -3750202);
      graphics.m_280218_(BACKGROUND, this.f_97735_, this.f_97736_, 0, 0, this.f_97726_, Math.min(this.f_97727_, 256));
      this.drawSlotGrid(graphics, 8, 18, 9, ((StructureContainerMenu)this.f_97732_).getContainerRows());
      this.drawSlotGrid(graphics, 8, 32 + ((StructureContainerMenu)this.f_97732_).getContainerRows() * 18, 9, 3);
      this.drawSlotGrid(graphics, 8, 90 + ((StructureContainerMenu)this.f_97732_).getContainerRows() * 18, 9, 1);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, this.f_96539_, 8, 8, 16777215, false);
      graphics.m_280614_(this.f_96547_, this.f_169604_, 8, this.f_97731_, 16777215, false);
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      this.m_280072_(graphics, mouseX, mouseY);
   }

   private void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            graphics.m_280218_(WIDGETS, this.f_97735_ + x + col * 18 - 1, this.f_97736_ + y + row * 18 - 1, 152, 120, 18, 18);
         }
      }
   }
}
