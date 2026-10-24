package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.menu.DraftingStationMenu;

public class DraftingStationScreen extends LegacyContainerScreen<DraftingStationMenu> {
   static final int PADDING = 3;
   static final int SCROLLBAR_WIDTH = 12;
   static final int MATERIAL_ROW_HEIGHT = 18;
   private static final int MATERIAL_X = 176;
   private static final int MATERIAL_Y = 104;
   private static final int MATERIAL_WIDTH = 224;
   private static final int MATERIAL_HEIGHT = 136;
   private static final int PREVIEW_X = 176;
   private static final int PREVIEW_Y = 8;
   private static final int PREVIEW_WIDTH = 224;
   private static final int PREVIEW_HEIGHT = 80;
   private final DraftingBuildingPreview buildingPreview;
   private int selected;
   private int materialScroll;
   private boolean draggingScrollbar;
   private boolean openingSelector;
   private ItemStack hoveredMaterial = ItemStack.EMPTY;

   public DraftingStationScreen(DraftingStationMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 400, 240);
      this.selected = menu.selectedTemplateIndex();
      this.buildingPreview = new DraftingBuildingPreview(menu);
      this.buildingPreview.setTemplate(this.selected);
   }

   @Override
   protected void init() {
      super.init();
      this.legacyButton("select structure", 8, 8, 95, 12, Component.translatable("gui.ancientwarfare.drafting.select"), button -> {
         this.openingSelector = true;
         this.minecraft.setScreen(new DraftingStationSelectScreen(this));
      });
      LegacyTexturedButton start = this.legacyButton("start crafting", 8, 32, 55, 12, Component.translatable("gui.ancientwarfare.drafting.start"), button -> {
         if (((DraftingStationMenu)this.menu).templateCount() > 0) {
            this.minecraft.gameMode.handleInventoryButtonClick(((DraftingStationMenu)this.menu).containerId, 1000);
         }
      });
      start.active = ((DraftingStationMenu)this.menu).templateCount() > 0;
      this.registerLayoutRect("selected structure", 8, 20, 160, 9, true);
      this.registerLayoutRect("material preview", 176, 104, 224, 136, false);
      this.registerLayoutRect("building preview", 176, 8, 224, 80, false);
      LegacyTexturedButton reset = this.legacyButton("reset preview", 378, 8, 14, 12, Component.literal("↺"), button -> this.buildingPreview.resetView());
      reset.setTooltip(Tooltip.create(Component.translatable("gui.ancientwarfare.drafting.preview_reset")));
      this.draggingScrollbar = false;
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.leftPos + 176, this.topPos + 8, 224, 80);
      this.buildingPreview.render(graphics, this.leftPos + 176 + 8, this.topPos + 8 + 16, 208, 60, mouseX, mouseY, partialTick);
      this.materialScroll = Math.max(0, Math.min(maxMaterialScroll((DraftingStationMenu)this.menu, this.selected, 136), this.materialScroll));
      this.hoveredMaterial = renderMaterialPanel(
         graphics,
         this.font,
         (DraftingStationMenu)this.menu,
         this.selected,
         this.leftPos + 176,
         this.topPos + 104,
         224,
         136,
         this.materialScroll,
         mouseX,
         mouseY
      );
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(
         this.font, fittedText(this.font, Component.translatable("gui.ancientwarfare.drafting.preview").getString(), 76), 184, 8, -1, false
      );
      graphics.drawString(this.font, fittedText(this.font, this.buildingPreview.dimensions().getString(), 106), 264, 8, -1, false);
      graphics.drawString(this.font, fittedText(this.font, this.selectedName(), 160), 8, 20, -1, false);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.drafting.output"), 8, 48, -1, false);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.drafting.input"), 8, 80, -1, false);
      graphics.drawString(
         this.font, fittedText(this.font, Component.translatable("gui.ancientwarfare.drafting.materials").getString(), 208), 184, 92, -1, false
      );
   }

   private String selectedName() {
      List<String> names = ((DraftingStationMenu)this.menu).templateNames();
      return names.isEmpty()
         ? Component.translatable("gui.ancientwarfare.drafting.none").getString()
         : names.get(Math.max(0, Math.min(this.selected, names.size() - 1)));
   }

   protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      if (!this.hoveredMaterial.isEmpty()) {
         graphics.renderTooltip(this.font, this.hoveredMaterial, mouseX, mouseY);
      } else if (mouseX >= this.leftPos + 8
         && mouseX < this.leftPos + 168
         && mouseY >= this.topPos + 20
         && mouseY < this.topPos + 29
         && this.font.width(this.selectedName()) > 160) {
         graphics.renderTooltip(this.font, Component.literal(this.selectedName()), mouseX, mouseY);
      } else {
         super.renderTooltip(graphics, mouseX, mouseY);
      }
   }

   public void chooseTemplate(int index) {
      if (index >= 0 && index < ((DraftingStationMenu)this.menu).templateCount()) {
         this.selected = index;
         this.materialScroll = 0;
         this.minecraft.gameMode.handleInventoryButtonClick(((DraftingStationMenu)this.menu).containerId, index);
         this.buildingPreview.setTemplate(index);
      }
   }

   public int selectedIndex() {
      return this.selected;
   }

   public DraftingStationMenu getMenu() {
      return (DraftingStationMenu)this.menu;
   }

   public boolean mouseClicked(double x, double y, int button) {
      if (this.buildingPreview.mouseClicked(x, y, button)) {
         return true;
      } else if (button == 0
         && insideScrollbar(x, y, this.leftPos + 176, this.topPos + 104, 224, 136)
         && maxMaterialScroll((DraftingStationMenu)this.menu, this.selected, 136) > 0) {
         this.draggingScrollbar = true;
         this.scrollToMouse(y);
         return true;
      } else {
         return super.mouseClicked(x, y, button);
      }
   }

   public boolean mouseScrolled(double x, double y, double delta) {
      if (this.buildingPreview.mouseScrolled(x, y, delta)) {
         return true;
      } else if (!insideMaterialViewport(x, y, this.leftPos + 176, this.topPos + 104, 224, 136)
         && !insideScrollbar(x, y, this.leftPos + 176, this.topPos + 104, 224, 136)) {
         return super.mouseScrolled(x, y, delta);
      } else {
         this.materialScroll = Math.max(
            0, Math.min(maxMaterialScroll((DraftingStationMenu)this.menu, this.selected, 136), this.materialScroll - (int)Math.signum(delta) * 18)
         );
         return true;
      }
   }

   public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
      if (this.buildingPreview.mouseDragged(x, y, button, dragX, dragY)) {
         return true;
      } else if (button == 0 && this.draggingScrollbar) {
         this.scrollToMouse(y);
         return true;
      } else {
         return super.mouseDragged(x, y, button, dragX, dragY);
      }
   }

   public boolean mouseReleased(double x, double y, int button) {
      boolean previewReleased = this.buildingPreview.mouseReleased(x, y, button);
      if (button == 0) {
         this.draggingScrollbar = false;
      }

      return previewReleased || super.mouseReleased(x, y, button);
   }

   private void scrollToMouse(double y) {
      int contentHeight = materialContentHeight((DraftingStationMenu)this.menu, this.selected);
      this.materialScroll = scrollOffsetAtMouse(
         y, this.topPos + 104, 136, contentHeight, maxMaterialScroll((DraftingStationMenu)this.menu, this.selected, 136)
      );
   }

   public void removed() {
      if (this.openingSelector) {
         this.openingSelector = false;
      } else {
         this.buildingPreview.close();
         super.removed();
      }
   }

   void selectorRemoved() {
      this.buildingPreview.close();
      super.removed();
   }

   static String fittedText(Font font, String text, int width) {
      return font.width(text) <= width ? text : font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
   }

   static int materialContentHeight(DraftingStationMenu menu, int index) {
      return 10 + menu.materials(index).size() * 18;
   }

   static int maxMaterialScroll(DraftingStationMenu menu, int index, int panelHeight) {
      return Math.max(0, materialContentHeight(menu, index) - panelHeight + 6);
   }

   static boolean insideMaterialViewport(double x, double y, int left, int top, int width, int height) {
      return x >= (double)(left + 3) && x < (double)(left + width - 12) && y >= (double)(top + 3) && y < (double)(top + height - 3);
   }

   static boolean insideScrollbar(double x, double y, int left, int top, int width, int height) {
      return x >= (double)(left + width - 12) && x < (double)(left + width) && y >= (double)top && y < (double)(top + height);
   }

   private static int thumbHeight(int panelHeight, int contentHeight) {
      return Math.min(panelHeight - 2, Math.max(10, (int)((long)(panelHeight - 2) * (long)(panelHeight - 6) / (long)Math.max(panelHeight - 6, contentHeight))));
   }

   static int scrollOffsetAtMouse(double y, int top, int panelHeight, int contentHeight, int maximum) {
      int thumb = thumbHeight(panelHeight, contentHeight);
      double fraction = (y - (double)top - 1.0 - (double)thumb / 2.0) / (double)Math.max(1, panelHeight - 2 - thumb);
      return (int)Math.round(Math.max(0.0, Math.min(1.0, fraction)) * (double)maximum);
   }

   static void drawScrollbar(GuiGraphics graphics, int left, int top, int width, int height, int contentHeight, int offset, int maximum) {
      int trackX = left + width - 12;
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, trackX, top, 12, height, 80, 120, 40, 128);
      int thumb = thumbHeight(height, contentHeight);
      int thumbY = top + 1 + (int)((long)(height - 2 - thumb) * (long)offset / (long)Math.max(1, maximum));
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, trackX + 1, thumbY, 10, thumb, 120, 120, 32, 128);
   }

   static ItemStack renderMaterialPanel(
      GuiGraphics graphics, Font font, DraftingStationMenu menu, int index, int left, int top, int width, int height, int offset, int mouseX, int mouseY
   ) {
      LegacyGuiStyle.background(graphics, left, top, width, height);
      drawScrollbar(graphics, left, top, width, height, materialContentHeight(menu, index), offset, maxMaterialScroll(menu, index, height));
      int right = left + width - 12;
      graphics.flush();
      graphics.enableScissor(left + 3, top + 3, right, top + height - 3);
      List<DraftingStationMenu.MaterialSummary> materials = menu.materials(index);
      int first = Math.max(0, (offset - 5) / 18);
      int last = Math.min(materials.size(), first + height / 18 + 3);
      ItemStack hovered = ItemStack.EMPTY;

      for (int row = first; row < last; row++) {
         int y = top + 3 + 5 + row * 18 - offset;
         ItemStack stack = materials.get(row).stack();
         LegacyGuiStyle.slot(graphics, left, top, 8, y - top);
         graphics.renderItem(stack, left + 3 + 5, y);
         if (insideMaterialViewport((double)mouseX, (double)mouseY, left, top, width, height) && mouseY >= y - 1 && mouseY < y + 17) {
            hovered = stack;
         }
      }

      graphics.flush();
      RenderSystem.disableDepthTest();

      for (int rowx = first; rowx < last; rowx++) {
         int y = top + 3 + 5 + rowx * 18 - offset;
         ItemStack stack = materials.get(rowx).stack();
         String count = Integer.toString(menu.remainingMaterialCount(index, rowx));
         int countX = right - 5 - font.width(count);
         int nameX = left + 3 + 25;
         graphics.drawString(font, fittedText(font, stack.getHoverName().getString(), Math.max(0, countX - nameX - 4)), nameX, y + 4, -1, false);
         graphics.drawString(font, count, countX, y + 4, -1, false);
      }

      graphics.flush();
      graphics.disableScissor();
      RenderSystem.enableDepthTest();
      return hovered;
   }
}
