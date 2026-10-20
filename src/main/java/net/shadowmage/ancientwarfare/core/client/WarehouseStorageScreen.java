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
   protected void m_7856_() {
      super.m_7856_();
      this.registerLayoutRect("storage filters viewport", 0, 0, 178, 74, false);
      this.registerLayoutRect("storage quantity viewport", 0, 74, 178, 74, false);
      this.registerLayoutRect("storage status", 4, 148, 170, 8, false);
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.lastMouseX = (double)mouseX;
      this.lastMouseY = (double)mouseY;
      this.filterScroll = Math.min(this.filterScroll, this.maxFilterScroll());
      LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_, this.f_97726_, 74);
      LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_ + 74, this.f_97726_, 74);
      this.renderFilters(graphics, mouseX, mouseY);
      this.renderEntries(graphics, mouseX, mouseY);
      this.drawScrollbar(graphics, 0, this.filterHeight(), this.filterScroll, this.maxFilterScroll());
      this.drawScrollbar(
         graphics, 74, this.entryHeight(), ((WarehouseStorageMenu)this.f_97732_).getScrollPixels(), ((WarehouseStorageMenu)this.f_97732_).getMaxScrollPixels()
      );
      LegacyGuiStyle.button(graphics, this.f_97735_, this.f_97736_, 148, 148, 22, 6, mouseX, mouseY);
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)(this.f_97735_ + 159) - (float)this.f_96547_.m_92895_("<>") * 0.25F, (float)(this.f_97736_ + 149), 0.0F);
      graphics.m_280168_().m_85841_(0.5F, 0.5F, 1.0F);
      graphics.m_280056_(this.f_96547_, "<>", 0, 0, -1, true);
      graphics.m_280168_().m_85849_();
   }

   private void renderFilters(GuiGraphics graphics, int mouseX, int mouseY) {
      this.beginViewport(graphics, 0);

      for (int index = 0; index < ((WarehouseStorageMenu)this.f_97732_).getFilterCount(); index++) {
         int y = 8 + index * 18 - this.filterScroll;
         if (y + 18 > 0 && y < 74) {
            LegacyGuiStyle.slot(graphics, this.f_97735_, this.f_97736_, 9, y + 1);
            ItemStack stack = ((WarehouseStorageMenu)this.f_97732_).getFilterStack(index);
            if (!stack.m_41619_()) {
               graphics.m_280480_(stack, this.f_97735_ + 9, this.f_97736_ + y + 1);
            }

            Component name = (Component)(stack.m_41619_() ? Component.m_237119_() : stack.m_41786_());
            graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(name.getString(), 118), this.f_97735_ + 28, this.f_97736_ + y + 4, -1, false);
            LegacyGuiStyle.buttonWithText(graphics, this.f_96547_, this.f_97735_, this.f_97736_, 150, y + 3, 12, 12, Component.m_237113_("-"), mouseX, mouseY);
            if (this.inside((double)mouseX, (double)mouseY, 8, y, 18, 18)) {
               graphics.m_280509_(this.f_97735_ + 9, this.f_97736_ + y + 1, this.f_97735_ + 25, this.f_97736_ + y + 17, -2130706433);
            }
         }
      }

      if (((WarehouseStorageMenu)this.f_97732_).getFilterCount() < 10) {
         LegacyGuiStyle.buttonWithText(
            graphics,
            this.f_96547_,
            this.f_97735_,
            this.f_97736_,
            8,
            this.newFilterY(),
            95,
            12,
            Component.m_237115_("guistrings.automation.new_filter"),
            mouseX,
            mouseY
         );
      }

      this.endViewport(graphics);
   }

   private void renderEntries(GuiGraphics graphics, int mouseX, int mouseY) {
      this.beginViewport(graphics, 74);

      for (int index = 0; index < 45; index++) {
         ItemStack stack = ((WarehouseStorageMenu)this.f_97732_).getDisplayStack(index);
         if (!stack.m_41619_()) {
            int x = 5 + index % 9 * 18;
            int y = 79 + index / 9 * 18 - ((WarehouseStorageMenu)this.f_97732_).getScrollPixels() % 18;
            if (y + 17 > 74 && y - 1 < 148) {
               LegacyGuiStyle.slot(graphics, this.f_97735_, this.f_97736_, x, y);
               graphics.m_280480_(stack, this.f_97735_ + x, this.f_97736_ + y);
               graphics.m_280302_(this.f_96547_, stack, this.f_97735_ + x, this.f_97736_ + y, "");
               if (this.inside((double)mouseX, (double)mouseY, x - 1, y - 1, 18, 18)) {
                  graphics.m_280509_(this.f_97735_ + x, this.f_97736_ + y, this.f_97735_ + x + 16, this.f_97736_ + y + 16, -2130706433);
               }
            }
         }
      }

      graphics.m_280262_();
      RenderSystem.disableDepthTest();

      for (int indexx = 0; indexx < 45; indexx++) {
         long quantity = ((WarehouseStorageMenu)this.f_97732_).getDisplayCount(indexx);
         if (!((WarehouseStorageMenu)this.f_97732_).getDisplayStack(indexx).m_41619_() && quantity > 1L) {
            int x = 5 + indexx % 9 * 18;
            int y = 79 + indexx / 9 * 18 - ((WarehouseStorageMenu)this.f_97732_).getScrollPixels() % 18;
            if (y + 17 > 74 && y - 1 < 148) {
               String count = shortCount(quantity);
               float scale = Math.min(1.0F, 16.0F / (float)Math.max(1, this.f_96547_.m_92895_(count)));
               graphics.m_280168_().m_85836_();
               graphics.m_280168_()
                  .m_252880_(
                     (float)(this.f_97735_ + x + 16) - (float)this.f_96547_.m_92895_(count) * scale, (float)(this.f_97736_ + y + 16) - 9.0F * scale, 300.0F
                  );
               graphics.m_280168_().m_85841_(scale, scale, 1.0F);
               graphics.m_280056_(this.f_96547_, count, 0, 0, -1, true);
               graphics.m_280168_().m_85849_();
            }
         }
      }

      this.endViewport(graphics);
      RenderSystem.enableDepthTest();
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      Component text = Component.m_237110_(
         "guistrings.warehouse.storage_capacity",
         new Object[]{shortCount(((WarehouseStorageMenu)this.f_97732_).getStoredCount()), shortCount(((WarehouseStorageMenu)this.f_97732_).getCapacity())}
      );
      float scale = Math.min(0.625F, 140.0F / (float)Math.max(1, this.f_96547_.m_92852_(text)));
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_(4.0F, 148.0F, 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(
         this.f_96547_,
         text,
         0,
         0,
         ((WarehouseStorageMenu)this.f_97732_).getStoredCount() >= ((WarehouseStorageMenu)this.f_97732_).getCapacity() ? -43691 : -12566464,
         false
      );
      graphics.m_280168_().m_85849_();
   }

   protected void m_280072_(GuiGraphics graphics, int mouseX, int mouseY) {
      if (this.inside((double)mouseX, (double)mouseY, 148, 148, 22, 6)) {
         graphics.m_280677_(
            this.f_96547_,
            List.of(((WarehouseStorageMenu)this.f_97732_).getConnectionDescription(), Component.m_237115_("guistrings.warehouse.connection_cycle")),
            Optional.empty(),
            mouseX,
            mouseY
         );
      } else if (this.inside((double)mouseX, (double)mouseY, 4, 148, 140, 8)) {
         graphics.m_280557_(
            this.f_96547_,
            Component.m_237110_(
               "guistrings.warehouse.storage_capacity",
               new Object[]{((WarehouseStorageMenu)this.f_97732_).getStoredCount(), ((WarehouseStorageMenu)this.f_97732_).getCapacity()}
            ),
            mouseX,
            mouseY
         );
      } else {
         int filter = this.filterRowAt((double)mouseX, (double)mouseY);
         if (filter >= 0 && !((WarehouseStorageMenu)this.f_97732_).getFilterStack(filter).m_41619_()) {
            ItemStack stack = ((WarehouseStorageMenu)this.f_97732_).getFilterStack(filter);
            graphics.m_280677_(this.f_96547_, this.m_280553_(stack), stack.m_150921_(), mouseX, mouseY);
         } else {
            int entry = this.entryAt((double)mouseX, (double)mouseY);
            if (entry >= 0
               && !((WarehouseStorageMenu)this.f_97732_).getDisplayStack(entry).m_41619_()
               && ((WarehouseStorageMenu)this.f_97732_).m_142621_().m_41619_()) {
               ItemStack stack = ((WarehouseStorageMenu)this.f_97732_).getDisplayStack(entry);
               List<Component> lines = new ArrayList<>(this.m_280553_(stack));
               lines.add(Component.m_237110_("guistrings.warehouse.item_count", new Object[]{((WarehouseStorageMenu)this.f_97732_).getDisplayCount(entry)}));
               graphics.m_280677_(this.f_96547_, lines, stack.m_150921_(), mouseX, mouseY);
            } else {
               super.m_280072_(graphics, mouseX, mouseY);
            }
         }
      }
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0 && this.inside(mouseX, mouseY, 166, 0, 12, 74) && this.maxFilterScroll() > 0) {
         this.draggingViewport = 0;
         this.scrollToMouse(mouseY);
         return true;
      } else if (button == 0 && this.inside(mouseX, mouseY, 166, 74, 12, 74) && ((WarehouseStorageMenu)this.f_97732_).getMaxScrollPixels() > 0) {
         this.draggingViewport = 74;
         this.scrollToMouse(mouseY);
         return true;
      } else if (button == 0 && this.inside(mouseX, mouseY, 148, 148, 22, 6)) {
         if (this.f_96541_ != null && this.f_96541_.f_91072_ != null) {
            this.f_96541_.f_91072_.m_105208_(((WarehouseStorageMenu)this.f_97732_).f_38840_, 0);
         }

         return true;
      } else if (!this.inside(mouseX, mouseY, 0, 0, 166, 74) || button != 0 && button != 1) {
         if (!this.inside(mouseX, mouseY, 0, 74, 166, 74)) {
            return super.m_6375_(mouseX, mouseY, button);
         } else if (!this.insideViewport(mouseX, mouseY, 74)) {
            return true;
         } else {
            int entry = this.entryAt(mouseX, mouseY);
            if (button != 0 && button != 1) {
               if (button == 2 && entry >= 0) {
                  this.sendEntryClick(entry, button, ClickType.CLONE);
               }
            } else {
               this.sendEntryClick(entry, button, m_96638_() && entry >= 0 ? ClickType.QUICK_MOVE : ClickType.PICKUP);
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
            && ((WarehouseStorageMenu)this.f_97732_).getFilterCount() < 10
            && this.insideViewport(mouseX, mouseY, 0)
            && this.inside(mouseX, mouseY, 8, this.newFilterY(), 95, 12)) {
            this.sendFilterAction(0, 0);
         }

         return true;
      }
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      int entry = this.entryAt(this.lastMouseX, this.lastMouseY);
      if (entry >= 0 && this.f_96541_ != null) {
         if (this.f_96541_.f_91066_.f_92094_.m_90832_(keyCode, scanCode)) {
            this.sendEntryClick(entry, m_96637_() ? 1 : 0, ClickType.THROW);
            return true;
         }

         if (this.f_96541_.f_91066_.f_92097_.m_90832_(keyCode, scanCode)) {
            this.sendEntryClick(entry, 2, ClickType.CLONE);
            return true;
         }
      }

      return super.m_7933_(keyCode, scanCode, modifiers);
   }

   private void sendFilterAction(int action, int index) {
      AWCorePlatform.changeWarehouseStorageFilter(
         ((WarehouseStorageMenu)this.f_97732_).f_38840_,
         action,
         index,
         ((WarehouseStorageMenu)this.f_97732_).getFilterCount(),
         ((WarehouseStorageMenu)this.f_97732_).getFilterStack(index).m_41777_(),
         ((WarehouseStorageMenu)this.f_97732_).m_142621_().m_41777_()
      );
   }

   private void sendEntryClick(int index, int button, ClickType type) {
      AWCorePlatform.clickWarehouseStorageEntry(
         ((WarehouseStorageMenu)this.f_97732_).f_38840_,
         index,
         button,
         type,
         ((WarehouseStorageMenu)this.f_97732_).getDisplayStack(index).m_41777_(),
         ((WarehouseStorageMenu)this.f_97732_).m_142621_().m_41777_()
      );
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (this.inside(mouseX, mouseY, 0, 0, this.f_97726_, 74)) {
         this.filterScroll = Math.max(0, Math.min(this.maxFilterScroll(), this.filterScroll + (delta > 0.0 ? -18 : 18)));
         return true;
      } else if (this.inside(mouseX, mouseY, 0, 74, this.f_97726_, 74)) {
         this.setEntryPixels(((WarehouseStorageMenu)this.f_97732_).getScrollPixels() + (delta > 0.0 ? -18 : 18));
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingViewport >= 0) {
         this.scrollToMouse(mouseY);
         return true;
      } else {
         return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      this.draggingViewport = -1;
      return super.m_6348_(mouseX, mouseY, button);
   }

   private void setEntryPixels(int pixels) {
      ((WarehouseStorageMenu)this.f_97732_).scrollToPixel(pixels);
      AWCorePlatform.setWarehouseStorageScrollPixels(((WarehouseStorageMenu)this.f_97732_).f_38840_, ((WarehouseStorageMenu)this.f_97732_).getScrollPixels());
   }

   private void scrollToMouse(double mouseY) {
      int contentHeight = this.draggingViewport == 0 ? this.filterHeight() : this.entryHeight();
      int thumb = this.thumbHeight(contentHeight);
      double fraction = Math.max(
         0.0, Math.min(1.0, (mouseY - (double)this.f_97736_ - (double)this.draggingViewport - 1.0 - (double)thumb / 2.0) / (double)Math.max(1, 72 - thumb))
      );
      if (this.draggingViewport == 0) {
         this.filterScroll = (int)Math.round(fraction * (double)this.maxFilterScroll());
      } else {
         this.setEntryPixels((int)Math.round(fraction * (double)((WarehouseStorageMenu)this.f_97732_).getMaxScrollPixels()));
      }
   }

   private void drawScrollbar(GuiGraphics graphics, int y, int contentHeight, int offset, int maximum) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + 166, this.f_97736_ + y, 12, 74, 80, 120, 40, 128);
      int thumb = this.thumbHeight(contentHeight);
      int thumbY = this.f_97736_ + y + 1 + (int)((double)((72 - thumb) * offset) / (double)Math.max(1, maximum));
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + 166 + 1, thumbY, 10, thumb, 120, 120, 32, 128);
   }

   private int thumbHeight(int height) {
      return Math.max(10, Math.min(72, 5184 / Math.max(1, height)));
   }

   private int filterHeight() {
      return 8 + ((WarehouseStorageMenu)this.f_97732_).getFilterCount() * 18 + (((WarehouseStorageMenu)this.f_97732_).getFilterCount() < 10 ? 12 : 0);
   }

   private int maxFilterScroll() {
      return Math.max(0, this.filterHeight() - 74 + 3);
   }

   private int newFilterY() {
      return 8 + ((WarehouseStorageMenu)this.f_97732_).getFilterCount() * 18 - this.filterScroll;
   }

   private int entryHeight() {
      return 22 + Math.max(0, (((WarehouseStorageMenu)this.f_97732_).getEntryCount() + 8) / 9 - 1) * 18;
   }

   private int filterRowAt(double mouseX, double mouseY) {
      if (!this.insideViewport(mouseX, mouseY, 0)) {
         return -1;
      } else {
         int localY = (int)(mouseY - (double)this.f_97736_ + (double)this.filterScroll - 8.0);
         int index = localY / 18;
         return localY >= 0 && index < ((WarehouseStorageMenu)this.f_97732_).getFilterCount() ? index : -1;
      }
   }

   private int entryAt(double mouseX, double mouseY) {
      if (this.insideViewport(mouseX, mouseY, 74) && this.inside(mouseX, mouseY, 4, 74, 162, 74)) {
         int col = (int)(mouseX - (double)this.f_97735_ - 4.0) / 18;
         int localY = (int)(mouseY - (double)this.f_97736_ - 74.0 - 4.0 + (double)(((WarehouseStorageMenu)this.f_97732_).getScrollPixels() % 18));
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
      graphics.m_280262_();
      graphics.m_280588_(this.f_97735_ + 3, this.f_97736_ + y + 3, this.f_97735_ + 166, this.f_97736_ + y + 74 - 3);
   }

   private void endViewport(GuiGraphics graphics) {
      graphics.m_280262_();
      graphics.m_280618_();
   }

   private boolean insideViewport(double x, double y, int viewportY) {
      return this.inside(x, y, 3, viewportY + 3, 163, 68);
   }

   private boolean inside(double x, double y, int localX, int localY, int width, int height) {
      return x >= (double)(this.f_97735_ + localX)
         && x < (double)(this.f_97735_ + localX + width)
         && y >= (double)(this.f_97736_ + localY)
         && y < (double)(this.f_97736_ + localY + height);
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
