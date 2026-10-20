package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStockViewerMenu;

public class WarehouseStockViewerScreen extends LegacyContainerScreen<WarehouseStockViewerMenu> {
   private static final int STOCK_PANEL_HEIGHT = 86;
   private final List<LegacyTexturedButton> equalityButtons = new ArrayList<>();
   private LegacyTexturedButton matchButton;
   private LegacyTexturedButton binding;

   public WarehouseStockViewerScreen(WarehouseStockViewerMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, menu.isLinker() ? 250 : 178, 172);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.equalityButtons.clear();
      this.registerLayoutRect("stock list", 0, 0, this.f_97726_, 86, false);
      this.binding = this.legacyButton("controller binding", this.f_97726_ - 28, 74, 20, 10, Component.m_237113_(">"), button -> this.clickButton(31));
      if (((WarehouseStockViewerMenu)this.f_97732_).isLinker()) {
         this.matchButton = this.legacyButton("match mode", 8, 74, 100, 10, this.matchLabel(), button -> this.clickButton(30));
      }

      for (int row = 0; row < 4; row++) {
         int index = row;
         int y = 10 + row * 16;
         LegacyTexturedButton clear = this.legacyButton(
            "clear filter " + row, this.f_97726_ - 28, y, 12, 12, Component.m_237113_("-"), button -> this.setFilter(index, ItemStack.f_41583_)
         );
         clear.m_257544_(Tooltip.m_257550_(Component.m_237115_("guistrings.warehouse.remove_filter")));
         if (((WarehouseStockViewerMenu)this.f_97732_).isLinker()) {
            this.equalityButtons
               .add(
                  this.legacyButton(
                     "equality " + row,
                     139,
                     y,
                     16,
                     12,
                     Component.m_237113_(((WarehouseStockViewerMenu)this.f_97732_).getEquality(index)),
                     button -> this.clickButton(20 + index)
                  )
               );
            this.legacyButton("decrease " + row, 157, y, 12, 12, Component.m_237113_("-"), button -> this.clickButton(100 + index * 2));
            this.legacyButton("increase " + row, 205, y, 12, 12, Component.m_237113_("+"), button -> this.clickButton(100 + index * 2 + 1));
         }
      }
   }

   private Component matchLabel() {
      return Component.m_237115_("guistrings.warehouse.match." + (((WarehouseStockViewerMenu)this.f_97732_).requiresAll() ? "all" : "any"));
   }

   private void clickButton(int id) {
      ((WarehouseStockViewerMenu)this.f_97732_).previewButton(id);
      if (this.f_96541_.f_91072_ != null) {
         this.f_96541_.f_91072_.m_105208_(((WarehouseStockViewerMenu)this.f_97732_).f_38840_, id);
      }
   }

   private void setFilter(int index, ItemStack filter) {
      ((WarehouseStockViewerMenu)this.f_97732_).previewFilterItem(index, filter);
      AWCorePlatform.setWarehouseStockFilter(((WarehouseStockViewerMenu)this.f_97732_).getPos(), index, filter);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      int index = this.filterAt(mouseX, mouseY);
      if (index >= 0 && (button == 0 || button == 1)) {
         ItemStack filter = button == 1 ? ItemStack.f_41583_ : ((WarehouseStockViewerMenu)this.f_97732_).m_142621_().m_41777_();
         if (!filter.m_41619_()) {
            filter.m_41764_(1);
         }

         this.setFilter(index, filter);
         return true;
      } else {
         return super.m_6375_(mouseX, mouseY, button);
      }
   }

   private int filterAt(double mouseX, double mouseY) {
      int x = (int)mouseX - this.f_97735_;
      int y = (int)mouseY - this.f_97736_;
      return x >= 8 && x < 26 && y >= 8 && y < 72 ? (y - 8) / 16 : -1;
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_, this.f_97726_, 86);

      for (int row = 0; row < 4; row++) {
         LegacyGuiStyle.slot(graphics, this.f_97735_, this.f_97736_, 8, 8 + row * 16);
      }
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      for (int row = 0; row < 4; row++) {
         int y = 8 + row * 16;
         ItemStack stack = ((WarehouseStockViewerMenu)this.f_97732_).getFilterItem(row);
         if (!stack.m_41619_()) {
            graphics.m_280480_(stack, 8, y);
         }

         String name = stack.m_41619_() ? Component.m_237115_("guistrings.warehouse.empty_filter").getString() : stack.m_41786_().getString();
         graphics.m_280056_(this.f_96547_, this.truncate(name, ((WarehouseStockViewerMenu)this.f_97732_).isLinker() ? 72 : 73), 28, y + 4, -1, false);
         this.drawCount(
            graphics,
            ((WarehouseStockViewerMenu)this.f_97732_).getQuantity(row),
            ((WarehouseStockViewerMenu)this.f_97732_).isLinker() ? 136 : 148,
            y + 4,
            ((WarehouseStockViewerMenu)this.f_97732_).isLinker() ? 32 : 40
         );
         if (((WarehouseStockViewerMenu)this.f_97732_).isLinker()) {
            this.drawCount(graphics, (long)((WarehouseStockViewerMenu)this.f_97732_).getTarget(row), 202, y + 4, 30);
         }
      }

      int start = ((WarehouseStockViewerMenu)this.f_97732_).isLinker() ? 112 : 8;
      int width = this.f_97726_ - 32 - start;
      Component state = Component.m_237115_("guistrings.warehouse.connection." + ((WarehouseStockViewerMenu)this.f_97732_).getConnectionStatus());
      graphics.m_280056_(
         this.f_96547_,
         this.truncate(state.getString(), width),
         start,
         75,
         ((WarehouseStockViewerMenu)this.f_97732_).getConnectionStatus() == 1 ? -12566464 : -5215488,
         false
      );
   }

   private String truncate(String text, int width) {
      return this.f_96547_.m_92895_(text) <= width ? text : this.f_96547_.m_92834_(text, Math.max(0, width - this.f_96547_.m_92895_("..."))) + "...";
   }

   private void drawCount(GuiGraphics graphics, long count, int right, int y, int width) {
      String text = formatCount(count);
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92895_(text)));
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)right - (float)this.f_96547_.m_92895_(text) * scale, (float)y, 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280056_(this.f_96547_, text, 0, 0, -1, false);
      graphics.m_280168_().m_85849_();
   }

   @Override
   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      ((WarehouseStockViewerMenu)this.f_97732_).refreshClientFilters();
      if (this.matchButton != null) {
         this.matchButton.m_93666_(this.matchLabel());
      }

      this.binding
         .m_257544_(
            Tooltip.m_257550_(
               Component.m_237115_("guistrings.warehouse.change_controller")
                  .m_130946_("\n")
                  .m_7220_(Component.m_237115_("guistrings.warehouse.connection." + ((WarehouseStockViewerMenu)this.f_97732_).getConnectionStatus()))
            )
         );

      for (int i = 0; i < this.equalityButtons.size(); i++) {
         this.equalityButtons.get(i).m_93666_(Component.m_237113_(((WarehouseStockViewerMenu)this.f_97732_).getEquality(i)));
      }

      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      int x = mouseX - this.f_97735_;
      int y = mouseY - this.f_97736_;
      if (y >= 8 && y < 72) {
         int row = (y - 8) / 16;
         ItemStack item = ((WarehouseStockViewerMenu)this.f_97732_).getFilterItem(row);
         if (x >= 8 && x < 102 && !item.m_41619_()) {
            graphics.m_280153_(this.f_96547_, item, mouseX, mouseY);
         } else if (x >= 102 && x < (((WarehouseStockViewerMenu)this.f_97732_).isLinker() ? 138 : 150)) {
            graphics.m_280557_(
               this.f_96547_,
               Component.m_237110_("guistrings.warehouse.item_count", new Object[]{((WarehouseStockViewerMenu)this.f_97732_).getQuantity(row)}),
               mouseX,
               mouseY
            );
         } else if (((WarehouseStockViewerMenu)this.f_97732_).isLinker() && x >= 170 && x < 204) {
            graphics.m_280557_(this.f_96547_, Component.m_237113_(Integer.toString(((WarehouseStockViewerMenu)this.f_97732_).getTarget(row))), mouseX, mouseY);
         }
      }

      if (y >= 74 && y < 84 && x >= (((WarehouseStockViewerMenu)this.f_97732_).isLinker() ? 112 : 8) && x < this.f_97726_ - 30) {
         List<Component> lines = new ArrayList<>();
         lines.add(Component.m_237115_("guistrings.warehouse.connection." + ((WarehouseStockViewerMenu)this.f_97732_).getConnectionStatus()));
         if (((WarehouseStockViewerMenu)this.f_97732_).getControllerPos() != null) {
            lines.add(
               Component.m_237110_(
                  "guistrings.warehouse.controller_position",
                  new Object[]{
                     ((WarehouseStockViewerMenu)this.f_97732_).getControllerPos().m_123341_(),
                     ((WarehouseStockViewerMenu)this.f_97732_).getControllerPos().m_123342_(),
                     ((WarehouseStockViewerMenu)this.f_97732_).getControllerPos().m_123343_()
                  }
               )
            );
         }

         graphics.m_280677_(this.f_96547_, lines, Optional.empty(), mouseX, mouseY);
      }
   }

   private static String formatCount(long count) {
      if (count < 10000L) {
         return Long.toString(count);
      } else {
         String[] units = new String[]{"k", "M", "G", "T", "P", "E"};
         double scaled = (double)count / 1000.0;

         int unit;
         for (unit = 0; scaled >= 1000.0 && unit < units.length - 1; unit++) {
            scaled /= 1000.0;
         }

         return String.format(Locale.ROOT, "%.1f%s", scaled, units[unit]);
      }
   }
}
