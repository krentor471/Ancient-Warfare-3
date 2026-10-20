package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public abstract class LegacyContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
   private final List<LegacyContainerScreen.LayoutRect> layoutRects = new ArrayList<>();

   protected LegacyContainerScreen(T menu, Inventory inventory, Component title, int width, int height) {
      super(menu, inventory, title);
      this.f_97726_ = width;
      this.f_97727_ = height;
      this.f_97728_ = 10000;
      this.f_97731_ = 10000;
   }

   protected void m_7856_() {
      this.layoutRects.clear();
      super.m_7856_();

      for (int i = 0; i < this.f_97732_.f_38839_.size(); i++) {
         Slot slot = (Slot)this.f_97732_.f_38839_.get(i);
         if (slot.m_6659_()) {
            this.registerLayoutRect("slot " + i, slot.f_40220_, slot.f_40221_, 16, 16, true);
         }
      }
   }

   protected final <W extends AbstractWidget> W addLegacyWidget(String id, W widget, int x, int y, int width, int height) {
      this.registerLayoutRect(id, x, y, width, height, true);
      return (W)this.m_142416_(widget);
   }

   protected final void registerLayoutRect(String id, int x, int y, int width, int height, boolean checkOverlap) {
      LegacyContainerScreen.LayoutRect next = new LegacyContainerScreen.LayoutRect(id, x, y, width, height, checkOverlap);
      if (x < 0 || y < 0 || x + width > this.f_97726_ || y + height > this.f_97727_) {
         AncientWarfareCore.LOG
            .warn("Legacy GUI element {} lies outside {}x{} at {},{} {}x{}", new Object[]{id, this.f_97726_, this.f_97727_, x, y, width, height});
      }

      if (checkOverlap) {
         for (LegacyContainerScreen.LayoutRect current : this.layoutRects) {
            if (current.checkOverlap && next.intersects(current)) {
               AncientWarfareCore.LOG.warn("Legacy GUI elements {} and {} overlap", current.id, id);
            }
         }
      }

      this.layoutRects.add(next);
   }

   protected final LegacyTexturedButton legacyButton(String id, int x, int y, int width, int height, Component message, LegacyTexturedButton.OnPress onPress) {
      return this.addLegacyWidget(id, new LegacyTexturedButton(this.f_97735_ + x, this.f_97736_ + y, width, height, message, onPress), x, y, width, height);
   }

   protected final void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_, this.f_97726_, this.f_97727_);

      for (Slot slot : this.f_97732_.f_38839_) {
         if (slot.m_6659_() && this.renderMenuSlotBackground(slot)) {
            LegacyGuiStyle.slot(graphics, this.f_97735_, this.f_97736_, slot.f_40220_, slot.f_40221_);
         }
      }

      this.renderLegacyBackground(graphics, partialTick, mouseX, mouseY);
   }

   protected boolean renderMenuSlotBackground(Slot slot) {
      return true;
   }

   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      this.beforeContainerRender(graphics, mouseX, mouseY, partialTick);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      this.afterContainerRender(graphics, mouseX, mouseY, partialTick);
      this.m_280072_(graphics, mouseX, mouseY);
   }

   protected void beforeContainerRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }

   protected void afterContainerRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }

   private static record LayoutRect(String id, int x, int y, int width, int height, boolean checkOverlap) {
      private boolean intersects(LegacyContainerScreen.LayoutRect other) {
         return this.x < other.x + other.width && this.x + this.width > other.x && this.y < other.y + other.height && this.y + this.height > other.y;
      }
   }
}
