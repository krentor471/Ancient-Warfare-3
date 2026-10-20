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
      this.imageWidth = width;
      this.imageHeight = height;
      this.titleLabelX = 10000;
      this.inventoryLabelY = 10000;
   }

   protected void init() {
      this.layoutRects.clear();
      super.init();

      for (int i = 0; i < this.menu.slots.size(); i++) {
         Slot slot = (Slot)this.menu.slots.get(i);
         if (slot.isActive()) {
            this.registerLayoutRect("slot " + i, slot.x, slot.y, 16, 16, true);
         }
      }
   }

   protected final <W extends AbstractWidget> W addLegacyWidget(String id, W widget, int x, int y, int width, int height) {
      this.registerLayoutRect(id, x, y, width, height, true);
      return (W)this.addRenderableWidget(widget);
   }

   protected final void registerLayoutRect(String id, int x, int y, int width, int height, boolean checkOverlap) {
      LegacyContainerScreen.LayoutRect next = new LegacyContainerScreen.LayoutRect(id, x, y, width, height, checkOverlap);
      if (x < 0 || y < 0 || x + width > this.imageWidth || y + height > this.imageHeight) {
         AncientWarfareCore.LOG
            .warn("Legacy GUI element {} lies outside {}x{} at {},{} {}x{}", new Object[]{id, this.imageWidth, this.imageHeight, x, y, width, height});
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
      return this.addLegacyWidget(id, new LegacyTexturedButton(this.leftPos + x, this.topPos + y, width, height, message, onPress), x, y, width, height);
   }

   protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

      for (Slot slot : this.menu.slots) {
         if (slot.isActive() && this.renderMenuSlotBackground(slot)) {
            LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, slot.x, slot.y);
         }
      }

      this.renderLegacyBackground(graphics, partialTick, mouseX, mouseY);
   }

   protected boolean renderMenuSlotBackground(Slot slot) {
      return true;
   }

   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      this.beforeContainerRender(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.afterContainerRender(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
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
