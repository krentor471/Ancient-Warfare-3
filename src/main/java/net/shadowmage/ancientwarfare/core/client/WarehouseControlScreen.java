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
   protected void m_7856_() {
      super.m_7856_();
      this.sortButton = this.legacyButton("sort type", 8, 8, 110, 12, this.sortMessage(), button -> {
         this.clickButton(0);
         button.m_93666_(this.sortMessage());
      });
      this.orderCheckbox = this.addLegacyWidget(
         "sort order",
         new LegacyCheckbox(
            this.f_97735_ + 122,
            this.f_97736_ + 6,
            16,
            this.fittedOrderMessage(),
            !((WarehouseControlMenu)this.f_97732_).isDescending(),
            checked -> this.clickButton(2)
         ),
         122,
         6,
         16,
         16
      );
      this.orderCheckbox.m_257544_(Tooltip.m_257550_(this.orderMessage()));
      this.searchBox = new EditBox(this.f_96547_, this.f_97735_ + 8, this.f_97736_ + 24, 162, 12, Component.m_237115_("guistrings.automation.search"));
      this.searchBox.m_94199_(64);
      this.searchBox.m_94144_(((WarehouseControlMenu)this.f_97732_).getSearch());
      this.searchBox.m_94151_(value -> {
         ((WarehouseControlMenu)this.f_97732_).setSearch(value);
         AWCorePlatform.setWarehouseSearch(value);
      });
      this.addLegacyWidget("search", this.searchBox, 8, 24, 162, 12);
      this.legacyButton("adjust bounds", 8, 220, 40, 12, Component.m_237115_("guistrings.automation.bounds"), button -> {
         if (this.f_96541_.f_91072_ != null) {
            this.f_96541_.f_91072_.m_105208_(((WarehouseControlMenu)this.f_97732_).f_38840_, 1);
         }
      });
      this.registerLayoutRect("warehouse item viewport", 0, 38, 178, 96, false);
   }

   private void clickButton(int id) {
      ((WarehouseControlMenu)this.f_97732_).m_6366_(null, id);
      if (this.f_96541_.f_91072_ != null) {
         this.f_96541_.f_91072_.m_105208_(((WarehouseControlMenu)this.f_97732_).f_38840_, id);
      }
   }

   private Component sortMessage() {
      return Component.m_237110_(
         "guistrings.automation.sort_type",
         new Object[]{Component.m_237115_("guistrings.automation.sort." + ((WarehouseControlMenu)this.f_97732_).getSortType().name().toLowerCase(Locale.ROOT))}
      );
   }

   private Component orderMessage() {
      return Component.m_237115_(
         ((WarehouseControlMenu)this.f_97732_).isDescending() ? "guistrings.automation.descending_short" : "guistrings.automation.ascending_short"
      );
   }

   private Component fittedOrderMessage() {
      String label = this.orderMessage().getString();
      int width = this.f_97726_ - 4 - 140;
      if (this.f_96547_.m_92895_(label) <= width) {
         return Component.m_237113_(label);
      } else {
         String suffix = "...";
         return Component.m_237113_(this.f_96547_.m_92834_(label, Math.max(0, width - this.f_96547_.m_92895_(suffix))) + suffix);
      }
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_ + 38, this.f_97726_, 96);

      for (int index = 0; index < 45; index++) {
         Slot slot = ((WarehouseControlMenu)this.f_97732_).m_38853_(index);
         if (slot.m_6657_()) {
            LegacyGuiStyle.slot(graphics, this.f_97735_, this.f_97736_, slot.f_40220_, slot.f_40221_);
         }
      }

      if (this.hasScrollableContent()) {
         this.drawScrollbar(graphics);
      }
   }

   @Override
   protected boolean renderMenuSlotBackground(Slot slot) {
      return ((WarehouseControlMenu)this.f_97732_).f_38839_.indexOf(slot) < 45 ? slot.m_6657_() : true;
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      this.sortButton.m_93666_(this.sortMessage());
      this.orderCheckbox.setChecked(!((WarehouseControlMenu)this.f_97732_).isDescending());
      this.orderCheckbox.m_93666_(this.fittedOrderMessage());
      this.orderCheckbox.m_257544_(Tooltip.m_257550_(this.orderMessage()));
      int color = ((WarehouseControlMenu)this.f_97732_).getTotalCapacity() > 0L
            && ((WarehouseControlMenu)this.f_97732_).getTotalItemCount() >= ((WarehouseControlMenu)this.f_97732_).getTotalCapacity()
         ? -52429
         : -12566464;
      Component storage = Component.m_237110_(
         "guistrings.warehouse.capacity",
         new Object[]{
            abbreviateCount(((WarehouseControlMenu)this.f_97732_).getTotalItemCount()),
            abbreviateCount(((WarehouseControlMenu)this.f_97732_).getTotalCapacity())
         }
      );
      this.drawFittedLabel(graphics, storage, 52, 221, 118, color);
      if (((WarehouseControlMenu)this.f_97732_).getOfflineStorageCount() > 0) {
         graphics.m_280056_(this.f_96547_, "!", 171, 221, -5215488, false);
      }
   }

   @Override
   protected void afterContainerRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderScaledDisplayCounts(graphics);
   }

   private void renderScaledDisplayCounts(GuiGraphics graphics) {
      graphics.m_280262_();
      RenderSystem.disableDepthTest();

      for (int index = 0; index < 45; index++) {
         ItemStack stack = ((WarehouseControlMenu)this.f_97732_).m_38853_(index).m_7993_();
         long quantity = ((WarehouseControlMenu)this.f_97732_).getDisplayCount(index);
         if (!stack.m_41619_() && quantity > 1L) {
            int x = this.f_97735_ + 4 + index % 9 * 18;
            int y = this.f_97736_ + 42 + index / 9 * 18;
            String count = abbreviateCount(quantity);
            int textWidth = this.f_96547_.m_92895_(count);
            float scale = Math.min(1.0F, 18.0F / (float)Math.max(1, textWidth));
            graphics.m_280168_().m_85836_();
            graphics.m_280168_().m_252880_((float)(x + 16) - (float)textWidth * scale, (float)(y + 16) - 8.0F * scale, 300.0F);
            graphics.m_280168_().m_85841_(scale, scale, 1.0F);
            graphics.m_280056_(this.f_96547_, count, 0, 0, -1, true);
            graphics.m_280168_().m_85849_();
         }
      }

      RenderSystem.enableDepthTest();
   }

   private void drawFittedLabel(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92852_(text)));
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)x, (float)y, 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, text, 0, 0, color, false);
      graphics.m_280168_().m_85849_();
   }

   protected void m_280072_(GuiGraphics graphics, int mouseX, int mouseY) {
      if (inside((double)mouseX, (double)mouseY, this.f_97735_ + 140, this.f_97736_ + 6, this.f_97726_ - 4 - 140, 16)) {
         graphics.m_280557_(this.f_96547_, this.orderMessage(), mouseX, mouseY);
      } else {
         int index = this.f_97734_ == null ? -1 : ((WarehouseControlMenu)this.f_97732_).f_38839_.indexOf(this.f_97734_);
         if (((WarehouseControlMenu)this.f_97732_).m_142621_().m_41619_() && index >= 0 && index < 45 && this.f_97734_.m_6657_()) {
            ItemStack stack = this.f_97734_.m_7993_();
            List<Component> lines = new ArrayList<>(this.m_280553_(stack));
            lines.add(Component.m_237110_("guistrings.warehouse.item_count", new Object[]{((WarehouseControlMenu)this.f_97732_).getDisplayCount(index)}));
            graphics.m_280677_(this.f_96547_, lines, stack.m_150921_(), mouseX, mouseY);
         } else if (inside((double)mouseX, (double)mouseY, this.f_97735_ + 52, this.f_97736_ + 220, 118, 13)) {
            List<Component> lines = new ArrayList<>();
            lines.add(
               Component.m_237110_(
                  "guistrings.warehouse.capacity",
                  new Object[]{((WarehouseControlMenu)this.f_97732_).getTotalItemCount(), ((WarehouseControlMenu)this.f_97732_).getTotalCapacity()}
               )
            );
            if (((WarehouseControlMenu)this.f_97732_).getOfflineStorageCount() > 0) {
               lines.add(
                  Component.m_237110_("guistrings.warehouse.offline_storage", new Object[]{((WarehouseControlMenu)this.f_97732_).getOfflineStorageCount()})
               );
            }

            graphics.m_280677_(this.f_96547_, lines, Optional.empty(), mouseX, mouseY);
         } else {
            super.m_280072_(graphics, mouseX, mouseY);
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

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (this.hasScrollableContent() && this.insideViewport(mouseX, mouseY)) {
         this.clickButton(delta > 0.0 ? 3 : 4);
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   protected void m_6597_(Slot slot, int slotId, int button, ClickType clickType) {
      if (slotId >= 0 && slotId < 45) {
         if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.THROW || clickType == ClickType.CLONE) {
            AWCorePlatform.clickWarehouseEntry(
               ((WarehouseControlMenu)this.f_97732_).f_38840_,
               slotId,
               button,
               clickType,
               ((WarehouseControlMenu)this.f_97732_).m_38853_(slotId).m_7993_().m_41777_(),
               ((WarehouseControlMenu)this.f_97732_).m_142621_().m_41777_()
            );
         }
      } else {
         super.m_6597_(slot, slotId, button, clickType);
      }
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (this.hasScrollableContent() && button == 0 && inside(mouseX, mouseY, this.f_97735_ + 166, this.f_97736_ + 38, 12, 96)) {
         this.draggingScrollbar = true;
         this.scrollToMouse(mouseY);
         return true;
      } else {
         return super.m_6375_(mouseX, mouseY, button);
      }
   }

   public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingScrollbar && this.hasScrollableContent()) {
         this.scrollToMouse(mouseY);
         return true;
      } else {
         return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      this.draggingScrollbar = false;
      return super.m_6348_(mouseX, mouseY, button);
   }

   private void drawScrollbar(GuiGraphics graphics) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + 166, this.f_97736_ + 38, 12, 96, 80, 120, 40, 128);
      int thumbHeight = this.thumbHeight();
      int thumbY = this.f_97736_ + 38 + 1;
      int max = ((WarehouseControlMenu)this.f_97732_).getMaxScrollOffset();
      if (max > 0) {
         thumbY += (int)((double)(94 - thumbHeight) * ((double)((WarehouseControlMenu)this.f_97732_).getScrollOffset() / (double)max));
      }

      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + 166 + 1, thumbY, 10, thumbHeight, 120, 120, 32, 128);
   }

   private int thumbHeight() {
      return Math.max(10, 8836 / this.contentHeight());
   }

   private void scrollToMouse(double mouseY) {
      int max = ((WarehouseControlMenu)this.f_97732_).getMaxScrollOffset();
      int thumbHeight = this.thumbHeight();
      double track = Math.max(1.0, (double)(94 - thumbHeight));
      double position = mouseY - (double)(this.f_97736_ + 38 + 1) - (double)thumbHeight / 2.0;
      int rows = (int)Math.round(Math.max(0.0, Math.min(1.0, position / track)) * ((double)max / 9.0));
      ((WarehouseControlMenu)this.f_97732_).scrollToRow(rows);
      AWCorePlatform.setWarehouseScrollRow(rows);
   }

   private int contentHeight() {
      long count = (long)((WarehouseControlMenu)this.f_97732_).getMatchedItemCount();
      long rows = count == 0L ? 0L : (count + 9L - 1L) / 9L;
      return (int)Math.min(2147483647L, 22L + Math.max(0L, rows - 1L) * 18L);
   }

   private boolean hasScrollableContent() {
      return this.contentHeight() > 96;
   }

   private boolean insideViewport(double x, double y) {
      return inside(x, y, this.f_97735_, this.f_97736_ + 38, this.f_97726_, 96);
   }

   private static boolean inside(double x, double y, int left, int top, int width, int height) {
      return x >= (double)left && x < (double)(left + width) && y >= (double)top && y < (double)(top + height);
   }
}
