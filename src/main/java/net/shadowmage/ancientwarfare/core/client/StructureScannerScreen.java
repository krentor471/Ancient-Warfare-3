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
   protected void init() {
      super.init();
      String savedName = this.name == null ? "My Structure" : this.name.getValue();
      String savedDeleteName = this.deleteName == null ? "" : this.deleteName.getValue();
      this.name = this.addNameField("template name", 112, Component.translatable("gui.ancientwarfare.structure_scanner.name"), savedName);
      this.name.setTooltip(Tooltip.create(Component.translatable("gui.ancientwarfare.structure_scanner.help")));
      this.deleteName = this.addNameField("delete name", 152, Component.translatable("gui.ancientwarfare.structure_scanner.delete"), savedDeleteName);
      this.legacyButton(
         "save template",
         193,
         114,
         55,
         12,
         Component.translatable("gui.ancientwarfare.structure_scanner.save"),
         button -> AWCorePlatform.saveStructureScan(((StructureScannerMenu)this.menu).pos(), this.name.getValue())
      );
      this.legacyButton(
         "delete template",
         193,
         154,
         55,
         12,
         Component.translatable("gui.ancientwarfare.structure_scanner.delete"),
         button -> AWCorePlatform.manageStructureTemplate(((StructureScannerMenu)this.menu).pos(), this.deleteName.getValue(), true)
      );
      this.legacyButton(
         "reset selection",
         193,
         212,
         55,
         12,
         Component.translatable("gui.ancientwarfare.structure_scanner.reset"),
         button -> AWCorePlatform.manageStructureTemplate(((StructureScannerMenu)this.menu).pos(), "", false)
      );
      this.setInitialFocus(this.name);
   }

   private EditBox addNameField(String id, int y, Component label, String value) {
      EditBox field = new EditBox(this.font, this.leftPos + 8, this.topPos + y, 160, 16, label);
      field.setMaxLength(48);
      field.setValue(value);
      field.setTextColor(-1);
      return this.addLegacyWidget(id, field, 8, y, 160, 16);
   }

   protected void containerTick() {
      this.name.tick();
      this.deleteName.tick();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.getFocused() instanceof EditBox field && keyCode != 256) {
         field.keyPressed(keyCode, scanCode, modifiers);
         return true;
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private String coords(BlockPos pos) {
      return pos == null ? "-" : pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawLabel(graphics, this.title, 8, 8, 240);
      this.drawLabel(
         graphics,
         Component.translatable("gui.ancientwarfare.structure_scanner.first", new Object[]{this.coords(((StructureScannerMenu)this.menu).firstPos())}),
         8,
         28,
         240
      );
      this.drawLabel(
         graphics,
         Component.translatable("gui.ancientwarfare.structure_scanner.second", new Object[]{this.coords(((StructureScannerMenu)this.menu).secondPos())}),
         8,
         40,
         240
      );
      this.drawLabel(
         graphics,
         Component.translatable("gui.ancientwarfare.structure_scanner.origin", new Object[]{this.coords(((StructureScannerMenu)this.menu).buildOrigin())}),
         8,
         52,
         240
      );
      if (((StructureScannerMenu)this.menu).firstPos() != null && ((StructureScannerMenu)this.menu).secondPos() != null) {
         long x = Math.abs(
               (long)((StructureScannerMenu)this.menu).firstPos().getX() - (long)((StructureScannerMenu)this.menu).secondPos().getX()
            )
            + 1L;
         long y = Math.abs(
               (long)((StructureScannerMenu)this.menu).firstPos().getY() - (long)((StructureScannerMenu)this.menu).secondPos().getY()
            )
            + 1L;
         long z = Math.abs(
               (long)((StructureScannerMenu)this.menu).firstPos().getZ() - (long)((StructureScannerMenu)this.menu).secondPos().getZ()
            )
            + 1L;
         this.drawLabel(graphics, Component.translatable("gui.ancientwarfare.structure_scanner.size", new Object[]{x, y, z, x * y * z}), 8, 68, 240);
      }

      this.drawLabel(
         graphics,
         Component.translatable("gui.ancientwarfare.structure_scanner.templates", new Object[]{((StructureScannerMenu)this.menu).templateNames().size()}),
         8,
         80,
         240
      );
      this.drawLabel(graphics, Component.translatable("gui.ancientwarfare.structure_scanner.name"), 8, 100, 160);
      this.drawLabel(graphics, Component.translatable("gui.ancientwarfare.structure_scanner.delete"), 8, 140, 160);
   }

   private void drawLabel(GuiGraphics graphics, Component label, int x, int y, int width) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.font.width(label)));
      graphics.pose().pushPose();
      graphics.pose().translate((float)x, (float)y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, label, 0, 0, -1, false);
      graphics.pose().popPose();
   }
}
