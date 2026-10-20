package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.StructureScannerMenu;

public class StructureScannerScreen extends LegacyContainerScreen<StructureScannerMenu> {
   private EditBox name;
   private EditBox deleteName;

   public StructureScannerScreen(StructureScannerMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 256, 240);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      String savedName = this.name == null ? "My Structure" : this.name.m_94155_();
      String savedDeleteName = this.deleteName == null ? "" : this.deleteName.m_94155_();
      this.name = this.addNameField("template name", 112, Component.m_237115_("gui.ancientwarfare.structure_scanner.name"), savedName);
      this.name.m_257544_(Tooltip.m_257550_(Component.m_237115_("gui.ancientwarfare.structure_scanner.help")));
      this.deleteName = this.addNameField("delete name", 152, Component.m_237115_("gui.ancientwarfare.structure_scanner.delete"), savedDeleteName);
      this.legacyButton(
         "save template",
         193,
         114,
         55,
         12,
         Component.m_237115_("gui.ancientwarfare.structure_scanner.save"),
         button -> AWCorePlatform.saveStructureScan(((StructureScannerMenu)this.f_97732_).pos(), this.name.m_94155_())
      );
      this.legacyButton(
         "delete template",
         193,
         154,
         55,
         12,
         Component.m_237115_("gui.ancientwarfare.structure_scanner.delete"),
         button -> AWCorePlatform.manageStructureTemplate(((StructureScannerMenu)this.f_97732_).pos(), this.deleteName.m_94155_(), true)
      );
      this.legacyButton(
         "reset selection",
         193,
         212,
         55,
         12,
         Component.m_237115_("gui.ancientwarfare.structure_scanner.reset"),
         button -> AWCorePlatform.manageStructureTemplate(((StructureScannerMenu)this.f_97732_).pos(), "", false)
      );
      this.m_264313_(this.name);
   }

   private EditBox addNameField(String id, int y, Component label, String value) {
      EditBox field = new EditBox(this.f_96547_, this.f_97735_ + 8, this.f_97736_ + y, 160, 16, label);
      field.m_94199_(48);
      field.m_94144_(value);
      field.m_94202_(-1);
      return this.addLegacyWidget(id, field, 8, y, 160, 16);
   }

   protected void m_181908_() {
      this.name.m_94120_();
      this.deleteName.m_94120_();
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (this.m_7222_() instanceof EditBox field && keyCode != 256) {
         field.m_7933_(keyCode, scanCode, modifiers);
         return true;
      }

      return super.m_7933_(keyCode, scanCode, modifiers);
   }

   private String coords(BlockPos pos) {
      return pos == null ? "-" : pos.m_123341_() + ", " + pos.m_123342_() + ", " + pos.m_123343_();
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawLabel(graphics, this.f_96539_, 8, 8, 240);
      this.drawLabel(
         graphics,
         Component.m_237110_("gui.ancientwarfare.structure_scanner.first", new Object[]{this.coords(((StructureScannerMenu)this.f_97732_).firstPos())}),
         8,
         28,
         240
      );
      this.drawLabel(
         graphics,
         Component.m_237110_("gui.ancientwarfare.structure_scanner.second", new Object[]{this.coords(((StructureScannerMenu)this.f_97732_).secondPos())}),
         8,
         40,
         240
      );
      this.drawLabel(
         graphics,
         Component.m_237110_("gui.ancientwarfare.structure_scanner.origin", new Object[]{this.coords(((StructureScannerMenu)this.f_97732_).buildOrigin())}),
         8,
         52,
         240
      );
      if (((StructureScannerMenu)this.f_97732_).firstPos() != null && ((StructureScannerMenu)this.f_97732_).secondPos() != null) {
         long x = Math.abs(
               (long)((StructureScannerMenu)this.f_97732_).firstPos().m_123341_() - (long)((StructureScannerMenu)this.f_97732_).secondPos().m_123341_()
            )
            + 1L;
         long y = Math.abs(
               (long)((StructureScannerMenu)this.f_97732_).firstPos().m_123342_() - (long)((StructureScannerMenu)this.f_97732_).secondPos().m_123342_()
            )
            + 1L;
         long z = Math.abs(
               (long)((StructureScannerMenu)this.f_97732_).firstPos().m_123343_() - (long)((StructureScannerMenu)this.f_97732_).secondPos().m_123343_()
            )
            + 1L;
         this.drawLabel(graphics, Component.m_237110_("gui.ancientwarfare.structure_scanner.size", new Object[]{x, y, z, x * y * z}), 8, 68, 240);
      }

      this.drawLabel(
         graphics,
         Component.m_237110_("gui.ancientwarfare.structure_scanner.templates", new Object[]{((StructureScannerMenu)this.f_97732_).templateNames().size()}),
         8,
         80,
         240
      );
      this.drawLabel(graphics, Component.m_237115_("gui.ancientwarfare.structure_scanner.name"), 8, 100, 160);
      this.drawLabel(graphics, Component.m_237115_("gui.ancientwarfare.structure_scanner.delete"), 8, 140, 160);
   }

   private void drawLabel(GuiGraphics graphics, Component label, int x, int y, int width) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92852_(label)));
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)x, (float)y, 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, label, 0, 0, -1, false);
      graphics.m_280168_().m_85849_();
   }
}
