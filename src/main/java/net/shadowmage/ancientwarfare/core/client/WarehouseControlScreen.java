package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.WarehouseControlMenu;

public class WarehouseControlScreen extends LegacyContainerScreen<WarehouseControlMenu> {
   private static final int DISPLAY_X = 4;
   private static final int DISPLAY_Y = 42;
   private static final int SCROLLBAR_X = 166;
   private static final int SCROLLBAR_Y = 38;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int SCROLLBAR_HEIGHT = 96;
   private static final int VIEW_SIZE = 94;
   private static final int ORDER_LABEL_X = 140;
   private EditBox searchBox;
   private LegacyTexturedButton sortButton;
   private LegacyCheckbox orderCheckbox;
   private boolean draggingScrollbar;

   public WarehouseControlScreen(WarehouseControlMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 178, 240);
   }

   @Override
   protected void init() {
      super.init();
      this.sortButton = this.legacyButton("sort type", 8, 8, 110, 12, this.sortMessage(), button -> {
         this.clickButton(0);
         button.setMessage(this.sortMessage());
      });
      this.orderCheckbox = this.addLegacyWidget(
         "sort order",
         new LegacyCheckbox(
            this.leftPos + 122,
            this.topPos + 6,
            16,
            this.fittedOrderMessage(),
            !((WarehouseControlMenu)this.menu).isDescending(),
            checked -> this.clickButton(2)
         ),
         122,
         6,
         16,
         16
      );
      this.orderCheckbox.setTooltip(Tooltip.create(this.orderMessage()));
      this.searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 24, 162, 12, Component.translatable("guistrings.automation.search"));
      this.searchBox.setMaxLength(64);
      this.searchBox.setValue(((WarehouseControlMenu)this.menu).getSearch());
      this.searchBox.setResponder(value -> {
         ((WarehouseControlMenu)this.menu).setSearch(value);
         AWCorePlatform.setWarehouseSearch(value);
      });
      this.addLegacyWidget("search", this.searchBox, 8, 24, 162, 12);
      this.legacyButton("adjust bounds", 8, 220, 40, 12, Component.translatable("guistrings.automation.bounds"), button -> {
         if (this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(((WarehouseControlMenu)this.menu).containerId, 1);
         }
      });
      this.registerLayoutRect("warehouse item viewport", 0, 38, 178, 96, false);
   }

   private void clickButton(int id) {
      ((WarehouseControlMenu)this.menu).clickMenuButton(null, id);
      if (this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((WarehouseControlMenu)this.menu).containerId, id);
      }
   }

   private Component sortMessage() {
      return Component.translatable(
         "guistrings.automation.sort_type",
         new Object[]{Component.translatable("guistrings.automation.sort." + ((WarehouseControlMenu)this.menu).getSortType().name().toLowerCase(Locale.ROOT))}
      );
   }

   private Component orderMessage() {
      return Component.translatable(
         ((WarehouseControlMenu)this.menu).isDescending() ? "guistrings.automation.descending_short" : "guistrings.automation.ascending_short"
      );
   }

   private Component fittedOrderMessage() {
      String label = this.orderMessage().getString();
      int width = this.imageWidth - 4 - 140;
      if (this.font.width(label) <= width) {
         return Component.literal(label);
      } else {
         String suffix = "...";
         return Component.literal(this.font.plainSubstrByWidth(label, Math.max(0, width - this.font.width(suffix))) + suffix);
      }
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.leftPos, this.topPos + 38, this.imageWidth, 96);

      for (int index = 0; index < 45; index++) {
         Slot slot = ((WarehouseControlMenu)this.menu).getSlot(index);
         if (slot.hasItem()) {
            LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, slot.x, slot.y);
         }
      }

      if (this.hasScrollableContent()) {
         this.drawScrollbar(graphics);
      }
   }

   @Override
   protected boolean renderMenuSlotBackground(Slot slot) {
      return ((WarehouseControlMenu)this.menu).slots.indexOf(slot) < 45 ? slot.hasItem() : true;
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      this.sortButton.setMessage(this.sortMessage());
      this.orderCheckbox.setChecked(!((WarehouseControlMenu)this.menu).isDescending());
      this.orderCheckbox.setMessage(this.fittedOrderMessage());
      this.orderCheckbox.setTooltip(Tooltip.create(this.orderMessage()));
      int color = ((WarehouseControlMenu)this.menu).getTotalCapacity() > 0L
            && ((WarehouseControlMenu)this.menu).getTotalItemCount() >= ((WarehouseControlMenu)this.menu).getTotalCapacity()
         ? -52429
         : -12566464;
      Component storage = Component.translatable(
         "guistrings.warehouse.capacity",
         new Object[]{
            abbreviateCount(((WarehouseControlMenu)this.menu).getTotalItemCount()),
            abbreviateCount(((WarehouseControlMenu)this.menu).getTotalCapacity())
         }
      );
      this.drawFittedLabel(graphics, storage, 52, 221, 118, color);
      if (((WarehouseControlMenu)this.menu).getOfflineStorageCount() > 0) {
         graphics.drawString(this.font, "!", 171, 221, -5215488, false);
      }
   }

   @Override
   protected void afterContainerRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderScaledDisplayCounts(graphics);
   }

   private void renderScaledDisplayCounts(GuiGraphics graphics) {
      graphics.flush();
      RenderSystem.disableDepthTest();

      for (int index = 0; index < 45; index++) {
         ItemStack stack = ((WarehouseControlMenu)this.menu).getSlot(index).getItem();
         long quantity = ((WarehouseControlMenu)this.menu).getDisplayCount(index);
         if (!stack.isEmpty() && quantity > 1L) {
            int x = this.leftPos + 4 + index % 9 * 18;
            int y = this.topPos + 42 + index / 9 * 18;
            String count = abbreviateCount(quantity);
            int textWidth = this.font.width(count);
            float scale = Math.min(1.0F, 18.0F / (float)Math.max(1, textWidth));
            graphics.pose().pushPose();
            graphics.pose().translate((float)(x + 16) - (float)textWidth * scale, (float)(y + 16) - 8.0F * scale, 300.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(this.font, count, 0, 0, -1, true);
            graphics.pose().popPose();
         }
      }

      RenderSystem.enableDepthTest();
   }

   private void drawFittedLabel(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.font.width(text)));
      graphics.pose().pushPose();
      graphics.pose().translate((float)x, (float)y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, 0, 0, color, false);
      graphics.pose().popPose();
   }

   protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      if (inside((double)mouseX, (double)mouseY, this.leftPos + 140, this.topPos + 6, this.imageWidth - 4 - 140, 16)) {
         graphics.renderTooltip(this.font, this.orderMessage(), mouseX, mouseY);
      } else {
         int index = this.hoveredSlot == null ? -1 : ((WarehouseControlMenu)this.menu).slots.indexOf(this.hoveredSlot);
         if (((WarehouseControlMenu)this.menu).getCarried().isEmpty() && index >= 0 && index < 45 && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            List<Component> lines = new ArrayList<>(this.getTooltipFromContainerItem(stack));
            lines.add(Component.translatable("guistrings.warehouse.item_count", new Object[]{((WarehouseControlMenu)this.menu).getDisplayCount(index)}));
            graphics.renderTooltip(this.font, lines, stack.getTooltipImage(), mouseX, mouseY);
         } else if (inside((double)mouseX, (double)mouseY, this.leftPos + 52, this.topPos + 220, 118, 13)) {
            List<Component> lines = new ArrayList<>();
            lines.add(
               Component.translatable(
                  "guistrings.warehouse.capacity",
                  new Object[]{((WarehouseControlMenu)this.menu).getTotalItemCount(), ((WarehouseControlMenu)this.menu).getTotalCapacity()}
               )
            );
            if (((WarehouseControlMenu)this.menu).getOfflineStorageCount() > 0) {
               lines.add(
                  Component.translatable("guistrings.warehouse.offline_storage", new Object[]{((WarehouseControlMenu)this.menu).getOfflineStorageCount()})
               );
            }

            graphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
         } else {
            super.renderTooltip(graphics, mouseX, mouseY);
         }
      }
   }

   private static String abbreviateCount(long quantity) {
      if (quantity < 1000L) {
         return Long.toString(quantity);
      } else {
         String[] units = new String[]{"k", "M", "G", "T", "P", "E"};
         double scaled = (double)quantity;

         int unit;
         for (unit = -1; scaled >= 1000.0 && unit < units.length - 1; unit++) {
            scaled /= 1000.0;
         }

         if (scaled >= 999.95 && unit < units.length - 1) {
            scaled /= 1000.0;
            unit++;
         }

         return (scaled >= 100.0 ? String.format(Locale.ROOT, "%.0f", scaled) : String.format(Locale.ROOT, "%.1f", scaled).replace(".0", "")) + units[unit];
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (this.hasScrollableContent() && this.insideViewport(mouseX, mouseY)) {
         this.clickButton(delta > 0.0 ? 3 : 4);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, delta);
      }
   }

   protected void slotClicked(Slot slot, int slotId, int button, ClickType clickType) {
      if (slotId >= 0 && slotId < 45) {
         if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.THROW || clickType == ClickType.CLONE) {
            AWCorePlatform.clickWarehouseEntry(
               ((WarehouseControlMenu)this.menu).containerId,
               slotId,
               button,
               clickType,
               ((WarehouseControlMenu)this.menu).getSlot(slotId).getItem().copy(),
               ((WarehouseControlMenu)this.menu).getCarried().copy()
            );
         }
      } else {
         super.slotClicked(slot, slotId, button, clickType);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.hasScrollableContent() && button == 0 && inside(mouseX, mouseY, this.leftPos + 166, this.topPos + 38, 12, 96)) {
         this.draggingScrollbar = true;
         this.scrollToMouse(mouseY);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingScrollbar && this.hasScrollableContent()) {
         this.scrollToMouse(mouseY);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.draggingScrollbar = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private void drawScrollbar(GuiGraphics graphics) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.leftPos + 166, this.topPos + 38, 12, 96, 80, 120, 40, 128);
      int thumbHeight = this.thumbHeight();
      int thumbY = this.topPos + 38 + 1;
      int max = ((WarehouseControlMenu)this.menu).getMaxScrollOffset();
      if (max > 0) {
         thumbY += (int)((double)(94 - thumbHeight) * ((double)((WarehouseControlMenu)this.menu).getScrollOffset() / (double)max));
      }

      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.leftPos + 166 + 1, thumbY, 10, thumbHeight, 120, 120, 32, 128);
   }

   private int thumbHeight() {
      return Math.max(10, 8836 / this.contentHeight());
   }

   private void scrollToMouse(double mouseY) {
      int max = ((WarehouseControlMenu)this.menu).getMaxScrollOffset();
      int thumbHeight = this.thumbHeight();
      double track = Math.max(1.0, (double)(94 - thumbHeight));
      double position = mouseY - (double)(this.topPos + 38 + 1) - (double)thumbHeight / 2.0;
      int rows = (int)Math.round(Math.max(0.0, Math.min(1.0, position / track)) * ((double)max / 9.0));
      ((WarehouseControlMenu)this.menu).scrollToRow(rows);
      AWCorePlatform.setWarehouseScrollRow(rows);
   }

   private int contentHeight() {
      long count = (long)((WarehouseControlMenu)this.menu).getMatchedItemCount();
      long rows = count == 0L ? 0L : (count + 9L - 1L) / 9L;
      return (int)Math.min(2147483647L, 22L + Math.max(0L, rows - 1L) * 18L);
   }

   private boolean hasScrollableContent() {
      return this.contentHeight() > 96;
   }

   private boolean insideViewport(double x, double y) {
      return inside(x, y, this.leftPos, this.topPos + 38, this.imageWidth, 96);
   }

   private static boolean inside(double x, double y, int left, int top, int width, int height) {
      return x >= (double)left && x < (double)(left + width) && y >= (double)top && y < (double)(top + height);
   }
}
