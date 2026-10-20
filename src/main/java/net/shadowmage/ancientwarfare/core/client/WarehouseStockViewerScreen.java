package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStockViewerMenu;

public class WarehouseStockViewerScreen extends LegacyContainerScreen<WarehouseStockViewerMenu> {
   private static final int STOCK_PANEL_HEIGHT = 86;
   private final List<LegacyTexturedButton> equalityButtons = new ArrayList<>();
   private LegacyTexturedButton matchButton;
   private LegacyTexturedButton binding;

   public WarehouseStockViewerScreen(WarehouseStockViewerMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, menu.isLinker() ? 250 : 178, 172);
   }

   @Override
   protected void init() {
      super.init();
      this.equalityButtons.clear();
      this.registerLayoutRect("stock list", 0, 0, this.imageWidth, 86, false);
      this.binding = this.legacyButton("controller binding", this.imageWidth - 28, 74, 20, 10, Component.literal(">"), button -> this.clickButton(31));
      if (((WarehouseStockViewerMenu)this.menu).isLinker()) {
         this.matchButton = this.legacyButton("match mode", 8, 74, 100, 10, this.matchLabel(), button -> this.clickButton(30));
      }

      for (int row = 0; row < 4; row++) {
         int index = row;
         int y = 10 + row * 16;
         LegacyTexturedButton clear = this.legacyButton(
            "clear filter " + row, this.imageWidth - 28, y, 12, 12, Component.literal("-"), button -> this.setFilter(index, ItemStack.EMPTY)
         );
         clear.setTooltip(Tooltip.create(Component.translatable("guistrings.warehouse.remove_filter")));
         if (((WarehouseStockViewerMenu)this.menu).isLinker()) {
            this.equalityButtons
               .add(
                  this.legacyButton(
                     "equality " + row,
                     139,
                     y,
                     16,
                     12,
                     Component.literal(((WarehouseStockViewerMenu)this.menu).getEquality(index)),
                     button -> this.clickButton(20 + index)
                  )
               );
            this.legacyButton("decrease " + row, 157, y, 12, 12, Component.literal("-"), button -> this.clickButton(100 + index * 2));
            this.legacyButton("increase " + row, 205, y, 12, 12, Component.literal("+"), button -> this.clickButton(100 + index * 2 + 1));
         }
      }
   }

   private Component matchLabel() {
      return Component.translatable("guistrings.warehouse.match." + (((WarehouseStockViewerMenu)this.menu).requiresAll() ? "all" : "any"));
   }

   private void clickButton(int id) {
      ((WarehouseStockViewerMenu)this.menu).previewButton(id);
      if (this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((WarehouseStockViewerMenu)this.menu).containerId, id);
      }
   }

   private void setFilter(int index, ItemStack filter) {
      ((WarehouseStockViewerMenu)this.menu).previewFilterItem(index, filter);
      AWCorePlatform.setWarehouseStockFilter(((WarehouseStockViewerMenu)this.menu).getPos(), index, filter);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int index = this.filterAt(mouseX, mouseY);
      if (index >= 0 && (button == 0 || button == 1)) {
         ItemStack filter = button == 1 ? ItemStack.EMPTY : ((WarehouseStockViewerMenu)this.menu).getCarried().copy();
         if (!filter.isEmpty()) {
            filter.setCount(1);
         }

         this.setFilter(index, filter);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private int filterAt(double mouseX, double mouseY) {
      int x = (int)mouseX - this.leftPos;
      int y = (int)mouseY - this.topPos;
      return x >= 8 && x < 26 && y >= 8 && y < 72 ? (y - 8) / 16 : -1;
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.leftPos, this.topPos, this.imageWidth, 86);

      for (int row = 0; row < 4; row++) {
         LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, 8, 8 + row * 16);
      }
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      for (int row = 0; row < 4; row++) {
         int y = 8 + row * 16;
         ItemStack stack = ((WarehouseStockViewerMenu)this.menu).getFilterItem(row);
         if (!stack.isEmpty()) {
            graphics.renderItem(stack, 8, y);
         }

         String name = stack.isEmpty() ? Component.translatable("guistrings.warehouse.empty_filter").getString() : stack.getHoverName().getString();
         graphics.drawString(this.font, this.truncate(name, ((WarehouseStockViewerMenu)this.menu).isLinker() ? 72 : 73), 28, y + 4, -1, false);
         this.drawCount(
            graphics,
            ((WarehouseStockViewerMenu)this.menu).getQuantity(row),
            ((WarehouseStockViewerMenu)this.menu).isLinker() ? 136 : 148,
            y + 4,
            ((WarehouseStockViewerMenu)this.menu).isLinker() ? 32 : 40
         );
         if (((WarehouseStockViewerMenu)this.menu).isLinker()) {
            this.drawCount(graphics, (long)((WarehouseStockViewerMenu)this.menu).getTarget(row), 202, y + 4, 30);
         }
      }

      int start = ((WarehouseStockViewerMenu)this.menu).isLinker() ? 112 : 8;
      int width = this.imageWidth - 32 - start;
      Component state = Component.translatable("guistrings.warehouse.connection." + ((WarehouseStockViewerMenu)this.menu).getConnectionStatus());
      graphics.drawString(
         this.font,
         this.truncate(state.getString(), width),
         start,
         75,
         ((WarehouseStockViewerMenu)this.menu).getConnectionStatus() == 1 ? -12566464 : -5215488,
         false
      );
   }

   private String truncate(String text, int width) {
      return this.font.width(text) <= width ? text : this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width("..."))) + "...";
   }

   private void drawCount(GuiGraphics graphics, long count, int right, int y, int width) {
      String text = formatCount(count);
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.font.width(text)));
      graphics.pose().pushPose();
      graphics.pose().translate((float)right - (float)this.font.width(text) * scale, (float)y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, 0, 0, -1, false);
      graphics.pose().popPose();
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      ((WarehouseStockViewerMenu)this.menu).refreshClientFilters();
      if (this.matchButton != null) {
         this.matchButton.setMessage(this.matchLabel());
      }

      this.binding
         .setTooltip(
            Tooltip.create(
               Component.translatable("guistrings.warehouse.change_controller")
                  .append("\n")
                  .append(Component.translatable("guistrings.warehouse.connection." + ((WarehouseStockViewerMenu)this.menu).getConnectionStatus()))
            )
         );

      for (int i = 0; i < this.equalityButtons.size(); i++) {
         this.equalityButtons.get(i).setMessage(Component.literal(((WarehouseStockViewerMenu)this.menu).getEquality(i)));
      }

      super.render(graphics, mouseX, mouseY, partialTick);
      int x = mouseX - this.leftPos;
      int y = mouseY - this.topPos;
      if (y >= 8 && y < 72) {
         int row = (y - 8) / 16;
         ItemStack item = ((WarehouseStockViewerMenu)this.menu).getFilterItem(row);
         if (x >= 8 && x < 102 && !item.isEmpty()) {
            graphics.renderTooltip(this.font, item, mouseX, mouseY);
         } else if (x >= 102 && x < (((WarehouseStockViewerMenu)this.menu).isLinker() ? 138 : 150)) {
            graphics.renderTooltip(
               this.font,
               Component.translatable("guistrings.warehouse.item_count", new Object[]{((WarehouseStockViewerMenu)this.menu).getQuantity(row)}),
               mouseX,
               mouseY
            );
         } else if (((WarehouseStockViewerMenu)this.menu).isLinker() && x >= 170 && x < 204) {
            graphics.renderTooltip(this.font, Component.literal(Integer.toString(((WarehouseStockViewerMenu)this.menu).getTarget(row))), mouseX, mouseY);
         }
      }

      if (y >= 74 && y < 84 && x >= (((WarehouseStockViewerMenu)this.menu).isLinker() ? 112 : 8) && x < this.imageWidth - 30) {
         List<Component> lines = new ArrayList<>();
         lines.add(Component.translatable("guistrings.warehouse.connection." + ((WarehouseStockViewerMenu)this.menu).getConnectionStatus()));
         if (((WarehouseStockViewerMenu)this.menu).getControllerPos() != null) {
            lines.add(
               Component.translatable(
                  "guistrings.warehouse.controller_position",
                  new Object[]{
                     ((WarehouseStockViewerMenu)this.menu).getControllerPos().getX(),
                     ((WarehouseStockViewerMenu)this.menu).getControllerPos().getY(),
                     ((WarehouseStockViewerMenu)this.menu).getControllerPos().getZ()
                  }
               )
            );
         }

         graphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
      }
   }

   private static String formatCount(long count) {
      if (count < 10000L) {
         return Long.toString(count);
      } else {
         String[] units = new String[]{"k", "M", "G", "T", "P", "E"};
         double scaled = (double)count / 1000.0;

         int unit;
         for (unit = 0; scaled >= 1000.0 && unit < units.length - 1; unit++) {
            scaled /= 1000.0;
         }

         return String.format(Locale.ROOT, "%.1f%s", scaled, units[unit]);
      }
   }
}
