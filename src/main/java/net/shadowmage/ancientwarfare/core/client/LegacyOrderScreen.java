package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class LegacyOrderScreen<T extends AbstractContainerMenu> extends LegacyContainerScreen<T> {
   private static final int PADDING = 3;
   private static final int SCROLLBAR_WIDTH = 12;
   protected final int orderViewportTop;
   protected final int orderViewportHeight;
   protected int scrollPixels;
   private boolean draggingOrderScrollbar;

   protected LegacyOrderScreen(T menu, Inventory inventory, Component title, int width, int height, int viewportTop, int viewportHeight) {
      super(menu, inventory, title, width, height);
      this.orderViewportTop = viewportTop;
      this.orderViewportHeight = viewportHeight;
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.registerLayoutRect("order viewport", 0, this.orderViewportTop, this.f_97726_, this.orderViewportHeight, false);
      this.clampOrderScroll();
   }

   protected abstract int orderContentHeight();

   protected boolean isOrderViewportActive() {
      return true;
   }

   protected final int maxOrderScroll() {
      return (int)Math.max(0L, (long)this.orderContentHeight() - (long)this.orderViewportHeight + 3L);
   }

   protected final void clampOrderScroll() {
      this.scrollPixels = Math.max(0, Math.min(this.scrollPixels, this.maxOrderScroll()));
   }

   protected final void drawOrderViewportBackground(GuiGraphics graphics) {
      this.clampOrderScroll();
      LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_ + this.orderViewportTop, this.f_97726_, this.orderViewportHeight);
      if (this.isOrderViewportActive()) {
         int x = this.f_97735_ + this.f_97726_ - 12;
         int y = this.f_97736_ + this.orderViewportTop;
         LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, x, y, 12, this.orderViewportHeight, 80, 120, 40, 128);
         int thumbHeight = this.orderThumbHeight();
         int trackHeight = this.orderViewportHeight - 2 - thumbHeight;
         int thumbY = y + 1 + (int)((long)trackHeight * (long)this.scrollPixels / (long)Math.max(1, this.maxOrderScroll()));
         LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, x + 1, thumbY, 10, thumbHeight, 120, 120, 32, 128);
      }
   }

   protected final void beginOrderViewport(GuiGraphics graphics) {
      graphics.m_280262_();
      graphics.m_280588_(
         this.f_97735_ + 3,
         this.f_97736_ + this.orderViewportTop + 3,
         this.f_97735_ + this.f_97726_ - 12,
         this.f_97736_ + this.orderViewportTop + this.orderViewportHeight - 3
      );
   }

   protected final void endOrderViewport(GuiGraphics graphics) {
      graphics.m_280262_();
      graphics.m_280618_();
   }

   protected final boolean insideOrderViewport(double mouseX, double mouseY) {
      return this.isOrderViewportActive()
         && inside(mouseX, mouseY, this.f_97735_ + 3, this.f_97736_ + this.orderViewportTop + 3, this.f_97726_ - 12 - 3, this.orderViewportHeight - 6);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      this.clampOrderScroll();
      if (button == 0
         && this.isOrderViewportActive()
         && this.maxOrderScroll() > 0
         && inside(mouseX, mouseY, this.f_97735_ + this.f_97726_ - 12, this.f_97736_ + this.orderViewportTop, 12, this.orderViewportHeight)) {
         this.draggingOrderScrollbar = true;
         this.scrollOrderToMouse(mouseY);
         return true;
      } else {
         return super.m_6375_(mouseX, mouseY, button);
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (this.isOrderViewportActive() && inside(mouseX, mouseY, this.f_97735_, this.f_97736_ + this.orderViewportTop, this.f_97726_, this.orderViewportHeight)
         )
       {
         this.clampOrderScroll();
         this.scrollPixels = Math.max(0, Math.min(this.maxOrderScroll(), this.scrollPixels - (int)Math.signum(delta) * 18));
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingOrderScrollbar && button == 0 && this.isOrderViewportActive()) {
         this.scrollOrderToMouse(mouseY);
         return true;
      } else {
         return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.draggingOrderScrollbar = false;
      }

      return super.m_6348_(mouseX, mouseY, button);
   }

   private int orderThumbHeight() {
      int trackHeight = this.orderViewportHeight - 2;
      return Math.min(trackHeight, Math.max(10, (int)((long)trackHeight * (long)trackHeight / (long)Math.max(trackHeight, this.orderContentHeight()))));
   }

   private void scrollOrderToMouse(double mouseY) {
      int thumbHeight = this.orderThumbHeight();
      double track = (double)Math.max(1, this.orderViewportHeight - 2 - thumbHeight);
      double position = mouseY - (double)this.f_97736_ - (double)this.orderViewportTop - 1.0 - (double)thumbHeight / 2.0;
      this.scrollPixels = (int)Math.round(Math.max(0.0, Math.min(1.0, position / track)) * (double)this.maxOrderScroll());
   }

   private static boolean inside(double x, double y, int left, int top, int width, int height) {
      return x >= (double)left && y >= (double)top && x < (double)(left + width) && y < (double)(top + height);
   }
}
