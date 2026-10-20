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
   protected void m_7856_() {
      super.m_7856_();
      this.quantityInputs.clear();
      this.removeButtons.clear();
      this.registerLayoutRect("filter viewport", 0, 0, 178, 88, false);
      this.modeButton = this.legacyButton(
         "interface mode",
         8,
         92,
         48,
         14,
         Component.m_237115_(((WarehouseInterfaceMenu)this.f_97732_).getMode().translationKey()),
         button -> this.clickButton(20)
      );
      LegacyTexturedButton controllerButton = this.legacyButton("controller binding", 138, 92, 24, 14, Component.m_237113_(">"), button -> this.clickButton(21));
      controllerButton.m_257544_(Tooltip.m_257550_(Component.m_237115_("guistrings.warehouse.change_controller")));

      for (int index = 0; index < 9; index++) {
         int filterIndex = index;
         EditBox input = new EditBox(this.f_96547_, this.f_97735_ + 38, this.f_97736_, 40, 12, Component.m_237119_());
         input.m_94199_(10);
         input.m_94153_(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
         input.m_94151_(value -> this.setQuantity(filterIndex, value));
         this.quantityInputs.add((EditBox)this.m_7787_(input));
         LegacyTexturedButton remove = new LegacyTexturedButton(
            this.f_97735_ + 150, this.f_97736_, 12, 12, Component.m_237113_("-"), button -> this.removeFilter(filterIndex)
         );
         this.removeButtons.add((LegacyTexturedButton)this.m_7787_(remove));
         remove.m_257544_(Tooltip.m_257550_(Component.m_237115_("guistrings.warehouse.remove_filter")));
      }

      this.addButton = (LegacyTexturedButton)this.m_7787_(
         new LegacyTexturedButton(this.f_97735_ + 8, this.f_97736_, 95, 12, Component.m_237115_("guistrings.automation.new_filter"), button -> this.addFilter())
      );
      this.updateControls();
   }

   private void clickButton(int id) {
      ((WarehouseInterfaceMenu)this.f_97732_).previewQuantityButton(id);
      this.f_96541_.f_91072_.m_105208_(((WarehouseInterfaceMenu)this.f_97732_).f_38840_, id);
      this.updateControls();
   }

   private void setQuantity(int index, String value) {
      if (!this.updatingInputs && index < ((WarehouseInterfaceMenu)this.f_97732_).getFilterCount()) {
         int quantity;
         try {
            quantity = value.isEmpty() ? 0 : Math.min(Integer.MAX_VALUE, Integer.parseInt(value));
         } catch (NumberFormatException var5) {
            quantity = Integer.MAX_VALUE;
         }

         ItemStack item = ((WarehouseInterfaceMenu)this.f_97732_).getFilterItem(index);
         ((WarehouseInterfaceMenu)this.f_97732_).previewSetFilter(index, item, quantity);
         AWCorePlatform.setWarehouseInterfaceFilter(((WarehouseInterfaceMenu)this.f_97732_).getPos(), index, item, quantity);
      }
   }

   private void addFilter() {
      if (((WarehouseInterfaceMenu)this.f_97732_).getFilterCount() < 9) {
         ((WarehouseInterfaceMenu)this.f_97732_).previewAddFilter();
         AWCorePlatform.addWarehouseInterfaceFilter(((WarehouseInterfaceMenu)this.f_97732_).getPos());
         this.scrollPixels = this.getMaxScroll();
         this.updateControls();
      }
   }

   private void removeFilter(int index) {
      ((WarehouseInterfaceMenu)this.f_97732_).previewRemoveFilter(index);
      AWCorePlatform.removeWarehouseInterfaceFilter(((WarehouseInterfaceMenu)this.f_97732_).getPos(), index);
      this.scrollPixels = Math.min(this.scrollPixels, this.getMaxScroll());
      this.updateControls();
   }

   private void updateControls() {
      this.updatingInputs = true;
      this.modeButton.m_93666_(Component.m_237115_(((WarehouseInterfaceMenu)this.f_97732_).getMode().translationKey()));
      this.modeButton.m_257544_(Tooltip.m_257550_(Component.m_237115_(((WarehouseInterfaceMenu)this.f_97732_).getMode().translationKey())));
      this.scrollPixels = Math.min(this.scrollPixels, this.getMaxScroll());
      int count = ((WarehouseInterfaceMenu)this.f_97732_).getFilterCount();

      for (int index = 0; index < 9; index++) {
         int y = 8 + index * 18 - this.scrollPixels;
         boolean visible = index < count && y + 2 < 85 && y + 16 > 3;
         EditBox input = this.quantityInputs.get(index);
         input.m_252865_(this.f_97735_ + 38);
         input.m_253211_(this.f_97736_ + 0 + y + 3);
         input.f_93624_ = visible;
         input.f_93623_ = visible;
         if (!visible) {
            input.m_93692_(false);
         }

         String value = Integer.toString(((WarehouseInterfaceMenu)this.f_97732_).getFilterQuantity(index));
         if (!input.m_93696_() && !input.m_94155_().equals(value)) {
            input.m_94144_(value);
         }

         LegacyTexturedButton remove = this.removeButtons.get(index);
         remove.m_252865_(this.f_97735_ + 150);
         remove.m_253211_(this.f_97736_ + 0 + y + 3);
         remove.f_93624_ = visible;
         remove.f_93623_ = visible;
      }

      int addY = 8 + count * 18 - this.scrollPixels;
      boolean addVisible = count < 9 && addY < 85 && addY + 12 > 3;
      this.addButton.m_252865_(this.f_97735_ + 8);
      this.addButton.m_253211_(this.f_97736_ + 0 + addY);
      this.addButton.f_93624_ = addVisible;
      this.addButton.f_93623_ = addVisible;
      this.updatingInputs = false;
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_ + 0, this.f_97726_, 88);
      this.beginViewport(graphics);

      for (int index = 0; index < ((WarehouseInterfaceMenu)this.f_97732_).getFilterCount(); index++) {
         int y = 8 + index * 18 - this.scrollPixels;
         LegacyGuiStyle.slot(graphics, this.f_97735_, this.f_97736_, 8, 0 + y);
      }

      this.endViewport(graphics);
      if (this.hasScrollableContent()) {
         this.drawScrollbar(graphics);
      }
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      int stateColor = ((WarehouseInterfaceMenu)this.f_97732_).getConnectionStatus() == 1 ? -12566464 : -5215488;
      String status = Component.m_237115_("guistrings.warehouse.connection." + ((WarehouseInterfaceMenu)this.f_97732_).getConnectionStatus()).getString();
      graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(status, 48), 8, 112, stateColor, false);
      graphics.m_280056_(
         this.f_96547_,
         this.f_96547_
            .m_92834_(Component.m_237115_("guistrings.warehouse.interface_status." + ((WarehouseInterfaceMenu)this.f_97732_).getWorkStatus()).getString(), 48),
         126,
         112,
         stateColor,
         false
      );
      this.beginViewport(graphics);

      for (int index = 0; index < ((WarehouseInterfaceMenu)this.f_97732_).getFilterCount(); index++) {
         int y = 8 + index * 18 - this.scrollPixels;
         ItemStack stack = ((WarehouseInterfaceMenu)this.f_97732_).getFilterItem(index);
         if (!stack.m_41619_()) {
            graphics.m_280480_(stack, 8, 0 + y);
         }
      }

      this.endViewport(graphics);
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)(-this.f_97735_), (float)(-this.f_97736_), 0.0F);
      this.renderFilterControls(graphics, mouseX, mouseY);
      graphics.m_280168_().m_85849_();
   }

   private void renderFilterControls(GuiGraphics graphics, int mouseX, int mouseY) {
      this.beginViewport(graphics);
      int clippedMouseX = this.insideViewport((double)mouseX, (double)mouseY) ? mouseX : Integer.MIN_VALUE;
      int clippedMouseY = this.insideViewport((double)mouseX, (double)mouseY) ? mouseY : Integer.MIN_VALUE;

      for (EditBox input : this.quantityInputs) {
         input.m_88315_(graphics, clippedMouseX, clippedMouseY, this.framePartialTick);
      }

      for (LegacyTexturedButton remove : this.removeButtons) {
         remove.m_88315_(graphics, clippedMouseX, clippedMouseY, this.framePartialTick);
      }

      this.addButton.m_88315_(graphics, clippedMouseX, clippedMouseY, this.framePartialTick);
      this.endViewport(graphics);
   }

   @Override
   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.framePartialTick = partialTick;
      ((WarehouseInterfaceMenu)this.f_97732_).refreshClientFilters();
      this.updateControls();
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      int index = this.filterAt((double)mouseX, (double)mouseY);
      if (index >= 0 && !((WarehouseInterfaceMenu)this.f_97732_).getFilterItem(index).m_41619_()) {
         graphics.m_280153_(this.f_96547_, ((WarehouseInterfaceMenu)this.f_97732_).getFilterItem(index), mouseX, mouseY);
      }

      if (inside((double)mouseX, (double)mouseY, this.f_97735_ + 8, this.f_97736_ + 110, 48, 14)) {
         List<Component> lines = new ArrayList<>();
         lines.add(Component.m_237115_("guistrings.warehouse.connection." + ((WarehouseInterfaceMenu)this.f_97732_).getConnectionStatus()));
         if (((WarehouseInterfaceMenu)this.f_97732_).getControllerPos() != null) {
            lines.add(
               Component.m_237110_(
                  "guistrings.warehouse.controller_position",
                  new Object[]{
                     ((WarehouseInterfaceMenu)this.f_97732_).getControllerPos().m_123341_(),
                     ((WarehouseInterfaceMenu)this.f_97732_).getControllerPos().m_123342_(),
                     ((WarehouseInterfaceMenu)this.f_97732_).getControllerPos().m_123343_()
                  }
               )
            );
         }

         graphics.m_280677_(this.f_96547_, lines, Optional.empty(), mouseX, mouseY);
      }

      if (inside((double)mouseX, (double)mouseY, this.f_97735_ + 126, this.f_97736_ + 110, 48, 14)) {
         graphics.m_280557_(
            this.f_96547_,
            Component.m_237115_("guistrings.warehouse.interface_status." + ((WarehouseInterfaceMenu)this.f_97732_).getWorkStatus()),
            mouseX,
            mouseY
         );
      }
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (this.hasScrollableContent() && button == 0 && inside(mouseX, mouseY, this.f_97735_ + 166, this.f_97736_ + 0, 12, 88)) {
         this.draggingScrollbar = true;
         this.scrollToMouse(mouseY);
         return true;
      } else {
         int index = this.filterAt(mouseX, mouseY);
         if (index < 0 || button != 0 && button != 1) {
            if (this.insideViewport(mouseX, mouseY)) {
               return super.m_6375_(mouseX, mouseY, button);
            } else {
               for (EditBox input : this.quantityInputs) {
                  input.f_93623_ = false;
               }

               for (LegacyTexturedButton remove : this.removeButtons) {
                  remove.f_93623_ = false;
               }

               this.addButton.f_93623_ = false;

               boolean var17;
               try {
                  var17 = super.m_6375_(mouseX, mouseY, button);
               } finally {
                  for (EditBox input : this.quantityInputs) {
                     input.f_93623_ = input.f_93624_;
                  }

                  for (LegacyTexturedButton remove : this.removeButtons) {
                     remove.f_93623_ = remove.f_93624_;
                  }

                  this.addButton.f_93623_ = this.addButton.f_93624_;
               }

               return var17;
            }
         } else {
            ItemStack stack = button == 1 ? ItemStack.f_41583_ : ((WarehouseInterfaceMenu)this.f_97732_).m_142621_().m_41777_();
            if (!stack.m_41619_()) {
               stack.m_41764_(1);
            }

            int quantity = stack.m_41619_()
               ? 0
               : Math.max(
                  1,
                  ((WarehouseInterfaceMenu)this.f_97732_).getFilterQuantity(index) == 0 ? 64 : ((WarehouseInterfaceMenu)this.f_97732_).getFilterQuantity(index)
               );
            ((WarehouseInterfaceMenu)this.f_97732_).previewSetFilter(index, stack, quantity);
            AWCorePlatform.setWarehouseInterfaceFilter(((WarehouseInterfaceMenu)this.f_97732_).getPos(), index, stack, quantity);
            this.updateControls();
            return true;
         }
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (this.hasScrollableContent() && inside(mouseX, mouseY, this.f_97735_, this.f_97736_ + 0, this.f_97726_, 88)) {
         this.scrollPixels = Math.max(0, Math.min(this.getMaxScroll(), this.scrollPixels - (int)Math.signum(delta) * 18));
         this.updateControls();
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
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

   private int filterAt(double mouseX, double mouseY) {
      if (this.insideViewport(mouseX, mouseY) && !(mouseX < (double)(this.f_97735_ + 8 - 1)) && !(mouseX >= (double)(this.f_97735_ + 8 + 17))) {
         for (int index = 0; index < ((WarehouseInterfaceMenu)this.f_97732_).getFilterCount(); index++) {
            int y = this.f_97736_ + 0 + 7 + index * 18 - this.scrollPixels;
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
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + 166, this.f_97736_ + 0, 12, 88, 80, 120, 40, 128);
      int max = this.getMaxScroll();
      int handleHeight = max == 0 ? 86 : Math.max(10, 7396 / Math.max(86, this.contentHeight()));
      int handleY = this.f_97736_ + 0 + 1 + (max == 0 ? 0 : (86 - handleHeight) * this.scrollPixels / max);
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + 166 + 1, handleY, 10, handleHeight, 120, 120, 32, 128);
   }

   private void scrollToMouse(double mouseY) {
      int max = this.getMaxScroll();
      if (max <= 0) {
         this.scrollPixels = 0;
      } else {
         int handleHeight = Math.max(10, 7396 / Math.max(86, this.contentHeight()));
         double track = (double)(86 - handleHeight);
         double position = mouseY - (double)this.f_97736_ - 0.0 - 1.0 - (double)handleHeight / 2.0;
         this.scrollPixels = (int)Math.round(Math.max(0.0, Math.min(1.0, position / Math.max(1.0, track))) * (double)max);
      }

      this.updateControls();
   }

   private int contentHeight() {
      return 8 + ((WarehouseInterfaceMenu)this.f_97732_).getFilterCount() * 18 + (((WarehouseInterfaceMenu)this.f_97732_).getFilterCount() < 9 ? 12 : 0);
   }

   private int getMaxScroll() {
      return Math.max(0, this.contentHeight() - 88 + 3);
   }

   private boolean hasScrollableContent() {
      return this.getMaxScroll() > 0;
   }

   private void beginViewport(GuiGraphics graphics) {
      graphics.m_280262_();
      graphics.m_280588_(this.f_97735_ + 3, this.f_97736_ + 0 + 3, this.f_97735_ + 166, this.f_97736_ + 0 + 88 - 3);
   }

   private void endViewport(GuiGraphics graphics) {
      graphics.m_280262_();
      graphics.m_280618_();
   }

   private boolean insideViewport(double x, double y) {
      return inside(x, y, this.f_97735_ + 3, this.f_97736_ + 0 + 3, 163, 82);
   }

   private static boolean inside(double x, double y, int left, int top, int width, int height) {
      return x >= (double)left && x < (double)(left + width) && y >= (double)top && y < (double)(top + height);
   }
}
