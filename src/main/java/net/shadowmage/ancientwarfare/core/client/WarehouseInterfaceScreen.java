package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.WarehouseInterfaceMenu;

public class WarehouseInterfaceScreen extends LegacyContainerScreen<WarehouseInterfaceMenu> {
   private static final int VIEW_HEIGHT = 88;
   private static final int VIEW_TOP = 0;
   private static final int FILTER_X = 8;
   private static final int QUANTITY_X = 38;
   private static final int REMOVE_X = 150;
   private static final int SCROLLBAR_X = 166;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int VIEW_PADDING = 3;
   private final List<EditBox> quantityInputs = new ArrayList<>();
   private final List<LegacyTexturedButton> removeButtons = new ArrayList<>();
   private LegacyTexturedButton addButton;
   private LegacyTexturedButton modeButton;
   private int scrollPixels;
   private boolean draggingScrollbar;
   private boolean updatingInputs;
   private float framePartialTick;

   public WarehouseInterfaceScreen(WarehouseInterfaceMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 178, 240);
   }

   @Override
   protected void init() {
      super.init();
      this.quantityInputs.clear();
      this.removeButtons.clear();
      this.registerLayoutRect("filter viewport", 0, 0, 178, 88, false);
      this.modeButton = this.legacyButton(
         "interface mode",
         8,
         92,
         48,
         14,
         Component.translatable(((WarehouseInterfaceMenu)this.menu).getMode().translationKey()),
         button -> this.clickButton(20)
      );
      LegacyTexturedButton controllerButton = this.legacyButton("controller binding", 138, 92, 24, 14, Component.literal(">"), button -> this.clickButton(21));
      controllerButton.setTooltip(Tooltip.create(Component.translatable("guistrings.warehouse.change_controller")));

      for (int index = 0; index < 9; index++) {
         int filterIndex = index;
         EditBox input = new EditBox(this.font, this.leftPos + 38, this.topPos, 40, 12, Component.empty());
         input.setMaxLength(10);
         input.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
         input.setResponder(value -> this.setQuantity(filterIndex, value));
         this.quantityInputs.add((EditBox)this.addWidget(input));
         LegacyTexturedButton remove = new LegacyTexturedButton(
            this.leftPos + 150, this.topPos, 12, 12, Component.literal("-"), button -> this.removeFilter(filterIndex)
         );
         this.removeButtons.add((LegacyTexturedButton)this.addWidget(remove));
         remove.setTooltip(Tooltip.create(Component.translatable("guistrings.warehouse.remove_filter")));
      }

      this.addButton = (LegacyTexturedButton)this.addWidget(
         new LegacyTexturedButton(this.leftPos + 8, this.topPos, 95, 12, Component.translatable("guistrings.automation.new_filter"), button -> this.addFilter())
      );
      this.updateControls();
   }

   private void clickButton(int id) {
      ((WarehouseInterfaceMenu)this.menu).previewQuantityButton(id);
      this.minecraft.gameMode.handleInventoryButtonClick(((WarehouseInterfaceMenu)this.menu).containerId, id);
      this.updateControls();
   }

   private void setQuantity(int index, String value) {
      if (!this.updatingInputs && index < ((WarehouseInterfaceMenu)this.menu).getFilterCount()) {
         int quantity;
         try {
            quantity = value.isEmpty() ? 0 : Math.min(Integer.MAX_VALUE, Integer.parseInt(value));
         } catch (NumberFormatException var5) {
            quantity = Integer.MAX_VALUE;
         }

         ItemStack item = ((WarehouseInterfaceMenu)this.menu).getFilterItem(index);
         ((WarehouseInterfaceMenu)this.menu).previewSetFilter(index, item, quantity);
         AWCorePlatform.setWarehouseInterfaceFilter(((WarehouseInterfaceMenu)this.menu).getPos(), index, item, quantity);
      }
   }

   private void addFilter() {
      if (((WarehouseInterfaceMenu)this.menu).getFilterCount() < 9) {
         ((WarehouseInterfaceMenu)this.menu).previewAddFilter();
         AWCorePlatform.addWarehouseInterfaceFilter(((WarehouseInterfaceMenu)this.menu).getPos());
         this.scrollPixels = this.getMaxScroll();
         this.updateControls();
      }
   }

   private void removeFilter(int index) {
      ((WarehouseInterfaceMenu)this.menu).previewRemoveFilter(index);
      AWCorePlatform.removeWarehouseInterfaceFilter(((WarehouseInterfaceMenu)this.menu).getPos(), index);
      this.scrollPixels = Math.min(this.scrollPixels, this.getMaxScroll());
      this.updateControls();
   }

   private void updateControls() {
      this.updatingInputs = true;
      this.modeButton.setMessage(Component.translatable(((WarehouseInterfaceMenu)this.menu).getMode().translationKey()));
      this.modeButton.setTooltip(Tooltip.create(Component.translatable(((WarehouseInterfaceMenu)this.menu).getMode().translationKey())));
      this.scrollPixels = Math.min(this.scrollPixels, this.getMaxScroll());
      int count = ((WarehouseInterfaceMenu)this.menu).getFilterCount();

      for (int index = 0; index < 9; index++) {
         int y = 8 + index * 18 - this.scrollPixels;
         boolean visible = index < count && y + 2 < 85 && y + 16 > 3;
         EditBox input = this.quantityInputs.get(index);
         input.setX(this.leftPos + 38);
         input.setY(this.topPos + 0 + y + 3);
         input.visible = visible;
         input.active = visible;
         if (!visible) {
            input.setFocused(false);
         }

         String value = Integer.toString(((WarehouseInterfaceMenu)this.menu).getFilterQuantity(index));
         if (!input.isFocused() && !input.getValue().equals(value)) {
            input.setValue(value);
         }

         LegacyTexturedButton remove = this.removeButtons.get(index);
         remove.setX(this.leftPos + 150);
         remove.setY(this.topPos + 0 + y + 3);
         remove.visible = visible;
         remove.active = visible;
      }

      int addY = 8 + count * 18 - this.scrollPixels;
      boolean addVisible = count < 9 && addY < 85 && addY + 12 > 3;
      this.addButton.setX(this.leftPos + 8);
      this.addButton.setY(this.topPos + 0 + addY);
      this.addButton.visible = addVisible;
      this.addButton.active = addVisible;
      this.updatingInputs = false;
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.leftPos, this.topPos + 0, this.imageWidth, 88);
      this.beginViewport(graphics);

      for (int index = 0; index < ((WarehouseInterfaceMenu)this.menu).getFilterCount(); index++) {
         int y = 8 + index * 18 - this.scrollPixels;
         LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, 8, 0 + y);
      }

      this.endViewport(graphics);
      if (this.hasScrollableContent()) {
         this.drawScrollbar(graphics);
      }
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      int stateColor = ((WarehouseInterfaceMenu)this.menu).getConnectionStatus() == 1 ? -12566464 : -5215488;
      String status = Component.translatable("guistrings.warehouse.connection." + ((WarehouseInterfaceMenu)this.menu).getConnectionStatus()).getString();
      graphics.drawString(this.font, this.font.plainSubstrByWidth(status, 48), 8, 112, stateColor, false);
      graphics.drawString(
         this.font,
         this.font
            .plainSubstrByWidth(Component.translatable("guistrings.warehouse.interface_status." + ((WarehouseInterfaceMenu)this.menu).getWorkStatus()).getString(), 48),
         126,
         112,
         stateColor,
         false
      );
      this.beginViewport(graphics);

      for (int index = 0; index < ((WarehouseInterfaceMenu)this.menu).getFilterCount(); index++) {
         int y = 8 + index * 18 - this.scrollPixels;
         ItemStack stack = ((WarehouseInterfaceMenu)this.menu).getFilterItem(index);
         if (!stack.isEmpty()) {
            graphics.renderItem(stack, 8, 0 + y);
         }
      }

      this.endViewport(graphics);
      graphics.pose().pushPose();
      graphics.pose().translate((float)(-this.leftPos), (float)(-this.topPos), 0.0F);
      this.renderFilterControls(graphics, mouseX, mouseY);
      graphics.pose().popPose();
   }

   private void renderFilterControls(GuiGraphics graphics, int mouseX, int mouseY) {
      this.beginViewport(graphics);
      int clippedMouseX = this.insideViewport((double)mouseX, (double)mouseY) ? mouseX : Integer.MIN_VALUE;
      int clippedMouseY = this.insideViewport((double)mouseX, (double)mouseY) ? mouseY : Integer.MIN_VALUE;

      for (EditBox input : this.quantityInputs) {
         input.render(graphics, clippedMouseX, clippedMouseY, this.framePartialTick);
      }

      for (LegacyTexturedButton remove : this.removeButtons) {
         remove.render(graphics, clippedMouseX, clippedMouseY, this.framePartialTick);
      }

      this.addButton.render(graphics, clippedMouseX, clippedMouseY, this.framePartialTick);
      this.endViewport(graphics);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.framePartialTick = partialTick;
      ((WarehouseInterfaceMenu)this.menu).refreshClientFilters();
      this.updateControls();
      super.render(graphics, mouseX, mouseY, partialTick);
      int index = this.filterAt((double)mouseX, (double)mouseY);
      if (index >= 0 && !((WarehouseInterfaceMenu)this.menu).getFilterItem(index).isEmpty()) {
         graphics.renderTooltip(this.font, ((WarehouseInterfaceMenu)this.menu).getFilterItem(index), mouseX, mouseY);
      }

      if (inside((double)mouseX, (double)mouseY, this.leftPos + 8, this.topPos + 110, 48, 14)) {
         List<Component> lines = new ArrayList<>();
         lines.add(Component.translatable("guistrings.warehouse.connection." + ((WarehouseInterfaceMenu)this.menu).getConnectionStatus()));
         if (((WarehouseInterfaceMenu)this.menu).getControllerPos() != null) {
            lines.add(
               Component.translatable(
                  "guistrings.warehouse.controller_position",
                  new Object[]{
                     ((WarehouseInterfaceMenu)this.menu).getControllerPos().getX(),
                     ((WarehouseInterfaceMenu)this.menu).getControllerPos().getY(),
                     ((WarehouseInterfaceMenu)this.menu).getControllerPos().getZ()
                  }
               )
            );
         }

         graphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
      }

      if (inside((double)mouseX, (double)mouseY, this.leftPos + 126, this.topPos + 110, 48, 14)) {
         graphics.renderTooltip(
            this.font,
            Component.translatable("guistrings.warehouse.interface_status." + ((WarehouseInterfaceMenu)this.menu).getWorkStatus()),
            mouseX,
            mouseY
         );
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.hasScrollableContent() && button == 0 && inside(mouseX, mouseY, this.leftPos + 166, this.topPos + 0, 12, 88)) {
         this.draggingScrollbar = true;
         this.scrollToMouse(mouseY);
         return true;
      } else {
         int index = this.filterAt(mouseX, mouseY);
         if (index < 0 || button != 0 && button != 1) {
            if (this.insideViewport(mouseX, mouseY)) {
               return super.mouseClicked(mouseX, mouseY, button);
            } else {
               for (EditBox input : this.quantityInputs) {
                  input.active = false;
               }

               for (LegacyTexturedButton remove : this.removeButtons) {
                  remove.active = false;
               }

               this.addButton.active = false;

               boolean var17;
               try {
                  var17 = super.mouseClicked(mouseX, mouseY, button);
               } finally {
                  for (EditBox input : this.quantityInputs) {
                     input.active = input.visible;
                  }

                  for (LegacyTexturedButton remove : this.removeButtons) {
                     remove.active = remove.visible;
                  }

                  this.addButton.active = this.addButton.visible;
               }

               return var17;
            }
         } else {
            ItemStack stack = button == 1 ? ItemStack.EMPTY : ((WarehouseInterfaceMenu)this.menu).getCarried().copy();
            if (!stack.isEmpty()) {
               stack.setCount(1);
            }

            int quantity = stack.isEmpty()
               ? 0
               : Math.max(
                  1,
                  ((WarehouseInterfaceMenu)this.menu).getFilterQuantity(index) == 0 ? 64 : ((WarehouseInterfaceMenu)this.menu).getFilterQuantity(index)
               );
            ((WarehouseInterfaceMenu)this.menu).previewSetFilter(index, stack, quantity);
            AWCorePlatform.setWarehouseInterfaceFilter(((WarehouseInterfaceMenu)this.menu).getPos(), index, stack, quantity);
            this.updateControls();
            return true;
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (this.hasScrollableContent() && inside(mouseX, mouseY, this.leftPos, this.topPos + 0, this.imageWidth, 88)) {
         this.scrollPixels = Math.max(0, Math.min(this.getMaxScroll(), this.scrollPixels - (int)Math.signum(delta) * 18));
         this.updateControls();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, delta);
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

   private int filterAt(double mouseX, double mouseY) {
      if (this.insideViewport(mouseX, mouseY) && !(mouseX < (double)(this.leftPos + 8 - 1)) && !(mouseX >= (double)(this.leftPos + 8 + 17))) {
         for (int index = 0; index < ((WarehouseInterfaceMenu)this.menu).getFilterCount(); index++) {
            int y = this.topPos + 0 + 7 + index * 18 - this.scrollPixels;
            if (mouseY >= (double)y && mouseY < (double)(y + 18)) {
               return index;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private void drawScrollbar(GuiGraphics graphics) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.leftPos + 166, this.topPos + 0, 12, 88, 80, 120, 40, 128);
      int max = this.getMaxScroll();
      int handleHeight = max == 0 ? 86 : Math.max(10, 7396 / Math.max(86, this.contentHeight()));
      int handleY = this.topPos + 0 + 1 + (max == 0 ? 0 : (86 - handleHeight) * this.scrollPixels / max);
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.leftPos + 166 + 1, handleY, 10, handleHeight, 120, 120, 32, 128);
   }

   private void scrollToMouse(double mouseY) {
      int max = this.getMaxScroll();
      if (max <= 0) {
         this.scrollPixels = 0;
      } else {
         int handleHeight = Math.max(10, 7396 / Math.max(86, this.contentHeight()));
         double track = (double)(86 - handleHeight);
         double position = mouseY - (double)this.topPos - 0.0 - 1.0 - (double)handleHeight / 2.0;
         this.scrollPixels = (int)Math.round(Math.max(0.0, Math.min(1.0, position / Math.max(1.0, track))) * (double)max);
      }

      this.updateControls();
   }

   private int contentHeight() {
      return 8 + ((WarehouseInterfaceMenu)this.menu).getFilterCount() * 18 + (((WarehouseInterfaceMenu)this.menu).getFilterCount() < 9 ? 12 : 0);
   }

   private int getMaxScroll() {
      return Math.max(0, this.contentHeight() - 88 + 3);
   }

   private boolean hasScrollableContent() {
      return this.getMaxScroll() > 0;
   }

   private void beginViewport(GuiGraphics graphics) {
      graphics.flush();
      graphics.enableScissor(this.leftPos + 3, this.topPos + 0 + 3, this.leftPos + 166, this.topPos + 0 + 88 - 3);
   }

   private void endViewport(GuiGraphics graphics) {
      graphics.flush();
      graphics.disableScissor();
   }

   private boolean insideViewport(double x, double y) {
      return inside(x, y, this.leftPos + 3, this.topPos + 0 + 3, 163, 82);
   }

   private static boolean inside(double x, double y, int left, int top, int width, int height) {
      return x >= (double)left && x < (double)(left + width) && y >= (double)top && y < (double)(top + height);
   }
}
