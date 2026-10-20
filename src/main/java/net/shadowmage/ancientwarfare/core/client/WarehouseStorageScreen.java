package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStorageMenu;

public class WarehouseStorageScreen extends LegacyContainerScreen<WarehouseStorageMenu> {
   private static final int VIEW_HEIGHT = 74;
   private static final int CONTENT_WIDTH = 166;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int VIEW_PADDING = 3;
   private int filterScroll;
   private int draggingViewport = -1;
   private double lastMouseX;
   private double lastMouseY;

   public WarehouseStorageScreen(WarehouseStorageMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 178, 240);
   }

   @Override
   protected void init() {
      super.init();
      this.registerLayoutRect("storage filters viewport", 0, 0, 178, 74, false);
      this.registerLayoutRect("storage quantity viewport", 0, 74, 178, 74, false);
      this.registerLayoutRect("storage status", 4, 148, 170, 8, false);
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.lastMouseX = (double)mouseX;
      this.lastMouseY = (double)mouseY;
      this.filterScroll = Math.min(this.filterScroll, this.maxFilterScroll());
      LegacyGuiStyle.background(graphics, this.leftPos, this.topPos, this.imageWidth, 74);
      LegacyGuiStyle.background(graphics, this.leftPos, this.topPos + 74, this.imageWidth, 74);
      this.renderFilters(graphics, mouseX, mouseY);
      this.renderEntries(graphics, mouseX, mouseY);
      this.drawScrollbar(graphics, 0, this.filterHeight(), this.filterScroll, this.maxFilterScroll());
      this.drawScrollbar(
         graphics, 74, this.entryHeight(), ((WarehouseStorageMenu)this.menu).getScrollPixels(), ((WarehouseStorageMenu)this.menu).getMaxScrollPixels()
      );
      LegacyGuiStyle.button(graphics, this.leftPos, this.topPos, 148, 148, 22, 6, mouseX, mouseY);
      graphics.pose().pushPose();
      graphics.pose().translate((float)(this.leftPos + 159) - (float)this.font.width("<>") * 0.25F, (float)(this.topPos + 149), 0.0F);
      graphics.pose().scale(0.5F, 0.5F, 1.0F);
      graphics.drawString(this.font, "<>", 0, 0, -1, true);
      graphics.pose().popPose();
   }

   private void renderFilters(GuiGraphics graphics, int mouseX, int mouseY) {
      this.beginViewport(graphics, 0);

      for (int index = 0; index < ((WarehouseStorageMenu)this.menu).getFilterCount(); index++) {
         int y = 8 + index * 18 - this.filterScroll;
         if (y + 18 > 0 && y < 74) {
            LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, 9, y + 1);
            ItemStack stack = ((WarehouseStorageMenu)this.menu).getFilterStack(index);
            if (!stack.isEmpty()) {
               graphics.renderItem(stack, this.leftPos + 9, this.topPos + y + 1);
            }

            Component name = (Component)(stack.isEmpty() ? Component.empty() : stack.getHoverName());
            graphics.drawString(this.font, this.font.plainSubstrByWidth(name.getString(), 118), this.leftPos + 28, this.topPos + y + 4, -1, false);
            LegacyGuiStyle.buttonWithText(graphics, this.font, this.leftPos, this.topPos, 150, y + 3, 12, 12, Component.literal("-"), mouseX, mouseY);
            if (this.inside((double)mouseX, (double)mouseY, 8, y, 18, 18)) {
               graphics.fill(this.leftPos + 9, this.topPos + y + 1, this.leftPos + 25, this.topPos + y + 17, -2130706433);
            }
         }
      }

      if (((WarehouseStorageMenu)this.menu).getFilterCount() < 10) {
         LegacyGuiStyle.buttonWithText(
            graphics,
            this.font,
            this.leftPos,
            this.topPos,
            8,
            this.newFilterY(),
            95,
            12,
            Component.translatable("guistrings.automation.new_filter"),
            mouseX,
            mouseY
         );
      }

      this.endViewport(graphics);
   }

   private void renderEntries(GuiGraphics graphics, int mouseX, int mouseY) {
      this.beginViewport(graphics, 74);

      for (int index = 0; index < 45; index++) {
         ItemStack stack = ((WarehouseStorageMenu)this.menu).getDisplayStack(index);
         if (!stack.isEmpty()) {
            int x = 5 + index % 9 * 18;
            int y = 79 + index / 9 * 18 - ((WarehouseStorageMenu)this.menu).getScrollPixels() % 18;
            if (y + 17 > 74 && y - 1 < 148) {
               LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, x, y);
               graphics.renderItem(stack, this.leftPos + x, this.topPos + y);
               graphics.renderItemDecorations(this.font, stack, this.leftPos + x, this.topPos + y, "");
               if (this.inside((double)mouseX, (double)mouseY, x - 1, y - 1, 18, 18)) {
                  graphics.fill(this.leftPos + x, this.topPos + y, this.leftPos + x + 16, this.topPos + y + 16, -2130706433);
               }
            }
         }
      }

      graphics.flush();
      RenderSystem.disableDepthTest();

      for (int indexx = 0; indexx < 45; indexx++) {
         long quantity = ((WarehouseStorageMenu)this.menu).getDisplayCount(indexx);
         if (!((WarehouseStorageMenu)this.menu).getDisplayStack(indexx).isEmpty() && quantity > 1L) {
            int x = 5 + indexx % 9 * 18;
            int y = 79 + indexx / 9 * 18 - ((WarehouseStorageMenu)this.menu).getScrollPixels() % 18;
            if (y + 17 > 74 && y - 1 < 148) {
               String count = shortCount(quantity);
               float scale = Math.min(1.0F, 16.0F / (float)Math.max(1, this.font.width(count)));
               graphics.pose().pushPose();
               graphics.pose()
                  .translate(
                     (float)(this.leftPos + x + 16) - (float)this.font.width(count) * scale, (float)(this.topPos + y + 16) - 9.0F * scale, 300.0F
                  );
               graphics.pose().scale(scale, scale, 1.0F);
               graphics.drawString(this.font, count, 0, 0, -1, true);
               graphics.pose().popPose();
            }
         }
      }

      this.endViewport(graphics);
      RenderSystem.enableDepthTest();
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      Component text = Component.translatable(
         "guistrings.warehouse.storage_capacity",
         new Object[]{shortCount(((WarehouseStorageMenu)this.menu).getStoredCount()), shortCount(((WarehouseStorageMenu)this.menu).getCapacity())}
      );
      float scale = Math.min(0.625F, 140.0F / (float)Math.max(1, this.font.width(text)));
      graphics.pose().pushPose();
      graphics.pose().translate(4.0F, 148.0F, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(
         this.font,
         text,
         0,
         0,
         ((WarehouseStorageMenu)this.menu).getStoredCount() >= ((WarehouseStorageMenu)this.menu).getCapacity() ? -43691 : -12566464,
         false
      );
      graphics.pose().popPose();
   }

   protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      if (this.inside((double)mouseX, (double)mouseY, 148, 148, 22, 6)) {
         graphics.renderTooltip(
            this.font,
            List.of(((WarehouseStorageMenu)this.menu).getConnectionDescription(), Component.translatable("guistrings.warehouse.connection_cycle")),
            Optional.empty(),
            mouseX,
            mouseY
         );
      } else if (this.inside((double)mouseX, (double)mouseY, 4, 148, 140, 8)) {
         graphics.renderTooltip(
            this.font,
            Component.translatable(
               "guistrings.warehouse.storage_capacity",
               new Object[]{((WarehouseStorageMenu)this.menu).getStoredCount(), ((WarehouseStorageMenu)this.menu).getCapacity()}
            ),
            mouseX,
            mouseY
         );
      } else {
         int filter = this.filterRowAt((double)mouseX, (double)mouseY);
         if (filter >= 0 && !((WarehouseStorageMenu)this.menu).getFilterStack(filter).isEmpty()) {
            ItemStack stack = ((WarehouseStorageMenu)this.menu).getFilterStack(filter);
            graphics.renderTooltip(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), mouseX, mouseY);
         } else {
            int entry = this.entryAt((double)mouseX, (double)mouseY);
            if (entry >= 0
               && !((WarehouseStorageMenu)this.menu).getDisplayStack(entry).isEmpty()
               && ((WarehouseStorageMenu)this.menu).getCarried().isEmpty()) {
               ItemStack stack = ((WarehouseStorageMenu)this.menu).getDisplayStack(entry);
               List<Component> lines = new ArrayList<>(this.getTooltipFromContainerItem(stack));
               lines.add(Component.translatable("guistrings.warehouse.item_count", new Object[]{((WarehouseStorageMenu)this.menu).getDisplayCount(entry)}));
               graphics.renderTooltip(this.font, lines, stack.getTooltipImage(), mouseX, mouseY);
            } else {
               super.renderTooltip(graphics, mouseX, mouseY);
            }
         }
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.inside(mouseX, mouseY, 166, 0, 12, 74) && this.maxFilterScroll() > 0) {
         this.draggingViewport = 0;
         this.scrollToMouse(mouseY);
         return true;
      } else if (button == 0 && this.inside(mouseX, mouseY, 166, 74, 12, 74) && ((WarehouseStorageMenu)this.menu).getMaxScrollPixels() > 0) {
         this.draggingViewport = 74;
         this.scrollToMouse(mouseY);
         return true;
      } else if (button == 0 && this.inside(mouseX, mouseY, 148, 148, 22, 6)) {
         if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(((WarehouseStorageMenu)this.menu).containerId, 0);
         }

         return true;
      } else if (!this.inside(mouseX, mouseY, 0, 0, 166, 74) || button != 0 && button != 1) {
         if (!this.inside(mouseX, mouseY, 0, 74, 166, 74)) {
            return super.mouseClicked(mouseX, mouseY, button);
         } else if (!this.insideViewport(mouseX, mouseY, 74)) {
            return true;
         } else {
            int entry = this.entryAt(mouseX, mouseY);
            if (button != 0 && button != 1) {
               if (button == 2 && entry >= 0) {
                  this.sendEntryClick(entry, button, ClickType.CLONE);
               }
            } else {
               this.sendEntryClick(entry, button, hasShiftDown() && entry >= 0 ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            }

            return true;
         }
      } else {
         int filter = this.filterRowAt(mouseX, mouseY);
         int y = 8 + filter * 18 - this.filterScroll;
         if (filter >= 0 && button == 0 && this.inside(mouseX, mouseY, 150, y + 3, 12, 12)) {
            this.sendFilterAction(2, filter);
         } else if (filter >= 0 && this.inside(mouseX, mouseY, 8, y, 18, 18)) {
            this.sendFilterAction(1, filter);
         } else if (button == 0
            && ((WarehouseStorageMenu)this.menu).getFilterCount() < 10
            && this.insideViewport(mouseX, mouseY, 0)
            && this.inside(mouseX, mouseY, 8, this.newFilterY(), 95, 12)) {
            this.sendFilterAction(0, 0);
         }

         return true;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      int entry = this.entryAt(this.lastMouseX, this.lastMouseY);
      if (entry >= 0 && this.minecraft != null) {
         if (this.minecraft.options.keyDrop.matches(keyCode, scanCode)) {
            this.sendEntryClick(entry, hasControlDown() ? 1 : 0, ClickType.THROW);
            return true;
         }

         if (this.minecraft.options.keyPickItem.matches(keyCode, scanCode)) {
            this.sendEntryClick(entry, 2, ClickType.CLONE);
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void sendFilterAction(int action, int index) {
      AWCorePlatform.changeWarehouseStorageFilter(
         ((WarehouseStorageMenu)this.menu).containerId,
         action,
         index,
         ((WarehouseStorageMenu)this.menu).getFilterCount(),
         ((WarehouseStorageMenu)this.menu).getFilterStack(index).copy(),
         ((WarehouseStorageMenu)this.menu).getCarried().copy()
      );
   }

   private void sendEntryClick(int index, int button, ClickType type) {
      AWCorePlatform.clickWarehouseStorageEntry(
         ((WarehouseStorageMenu)this.menu).containerId,
         index,
         button,
         type,
         ((WarehouseStorageMenu)this.menu).getDisplayStack(index).copy(),
         ((WarehouseStorageMenu)this.menu).getCarried().copy()
      );
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (this.inside(mouseX, mouseY, 0, 0, this.imageWidth, 74)) {
         this.filterScroll = Math.max(0, Math.min(this.maxFilterScroll(), this.filterScroll + (delta > 0.0 ? -18 : 18)));
         return true;
      } else if (this.inside(mouseX, mouseY, 0, 74, this.imageWidth, 74)) {
         this.setEntryPixels(((WarehouseStorageMenu)this.menu).getScrollPixels() + (delta > 0.0 ? -18 : 18));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, delta);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingViewport >= 0) {
         this.scrollToMouse(mouseY);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.draggingViewport = -1;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private void setEntryPixels(int pixels) {
      ((WarehouseStorageMenu)this.menu).scrollToPixel(pixels);
      AWCorePlatform.setWarehouseStorageScrollPixels(((WarehouseStorageMenu)this.menu).containerId, ((WarehouseStorageMenu)this.menu).getScrollPixels());
   }

   private void scrollToMouse(double mouseY) {
      int contentHeight = this.draggingViewport == 0 ? this.filterHeight() : this.entryHeight();
      int thumb = this.thumbHeight(contentHeight);
      double fraction = Math.max(
         0.0, Math.min(1.0, (mouseY - (double)this.topPos - (double)this.draggingViewport - 1.0 - (double)thumb / 2.0) / (double)Math.max(1, 72 - thumb))
      );
      if (this.draggingViewport == 0) {
         this.filterScroll = (int)Math.round(fraction * (double)this.maxFilterScroll());
      } else {
         this.setEntryPixels((int)Math.round(fraction * (double)((WarehouseStorageMenu)this.menu).getMaxScrollPixels()));
      }
   }

   private void drawScrollbar(GuiGraphics graphics, int y, int contentHeight, int offset, int maximum) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.leftPos + 166, this.topPos + y, 12, 74, 80, 120, 40, 128);
      int thumb = this.thumbHeight(contentHeight);
      int thumbY = this.topPos + y + 1 + (int)((double)((72 - thumb) * offset) / (double)Math.max(1, maximum));
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.leftPos + 166 + 1, thumbY, 10, thumb, 120, 120, 32, 128);
   }

   private int thumbHeight(int height) {
      return Math.max(10, Math.min(72, 5184 / Math.max(1, height)));
   }

   private int filterHeight() {
      return 8 + ((WarehouseStorageMenu)this.menu).getFilterCount() * 18 + (((WarehouseStorageMenu)this.menu).getFilterCount() < 10 ? 12 : 0);
   }

   private int maxFilterScroll() {
      return Math.max(0, this.filterHeight() - 74 + 3);
   }

   private int newFilterY() {
      return 8 + ((WarehouseStorageMenu)this.menu).getFilterCount() * 18 - this.filterScroll;
   }

   private int entryHeight() {
      return 22 + Math.max(0, (((WarehouseStorageMenu)this.menu).getEntryCount() + 8) / 9 - 1) * 18;
   }

   private int filterRowAt(double mouseX, double mouseY) {
      if (!this.insideViewport(mouseX, mouseY, 0)) {
         return -1;
      } else {
         int localY = (int)(mouseY - (double)this.topPos + (double)this.filterScroll - 8.0);
         int index = localY / 18;
         return localY >= 0 && index < ((WarehouseStorageMenu)this.menu).getFilterCount() ? index : -1;
      }
   }

   private int entryAt(double mouseX, double mouseY) {
      if (this.insideViewport(mouseX, mouseY, 74) && this.inside(mouseX, mouseY, 4, 74, 162, 74)) {
         int col = (int)(mouseX - (double)this.leftPos - 4.0) / 18;
         int localY = (int)(mouseY - (double)this.topPos - 74.0 - 4.0 + (double)(((WarehouseStorageMenu)this.menu).getScrollPixels() % 18));
         if (localY < 0) {
            return -1;
         } else {
            int row = localY / 18;
            int index = row * 9 + col;
            return index < 45 ? index : -1;
         }
      } else {
         return -1;
      }
   }

   private void beginViewport(GuiGraphics graphics, int y) {
      graphics.flush();
      graphics.enableScissor(this.leftPos + 3, this.topPos + y + 3, this.leftPos + 166, this.topPos + y + 74 - 3);
   }

   private void endViewport(GuiGraphics graphics) {
      graphics.flush();
      graphics.disableScissor();
   }

   private boolean insideViewport(double x, double y, int viewportY) {
      return this.inside(x, y, 3, viewportY + 3, 163, 68);
   }

   private boolean inside(double x, double y, int localX, int localY, int width, int height) {
      return x >= (double)(this.leftPos + localX)
         && x < (double)(this.leftPos + localX + width)
         && y >= (double)(this.topPos + localY)
         && y < (double)(this.topPos + localY + height);
   }

   private static String shortCount(long count) {
      if (count < 1000L) {
         return Long.toString(count);
      } else {
         String[] units = new String[]{"k", "M", "G", "T", "P", "E"};
         double value = (double)count;

         int unit;
         for (unit = -1; value >= 1000.0 && unit < units.length - 1; unit++) {
            value /= 1000.0;
         }

         return (value >= 100.0 ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.1f", value).replace(".0", "")) + units[unit];
      }
   }
}
