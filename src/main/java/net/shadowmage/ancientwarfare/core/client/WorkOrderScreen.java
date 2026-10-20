package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.npc.WorkOrderData;
import net.shadowmage.ancientwarfare.core.menu.WorkOrderMenu;

public class WorkOrderScreen extends LegacyOrderScreen<WorkOrderMenu> {
   private static final int ENTRY_TOP = 8;
   private static final int ENTRY_HEIGHT = 25;
   private static final int TEXT = -12566464;

   public WorkOrderScreen(WorkOrderMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 256, 240, 40, 110);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.registerLayoutRect("work priority", 8, 8, 100, 12, true);
      this.registerLayoutRect("work shift", 8, 24, 100, 12, true);
   }

   @Override
   protected int orderContentHeight() {
      return 8 + ((WorkOrderMenu)this.f_97732_).getEntries().size() * 25;
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawButton(graphics, 8, 8, 100, this.priorityLabel(), mouseX, mouseY);
      this.drawButton(graphics, 8, 24, 100, this.shiftLabel(), mouseX, mouseY);
      this.drawOrderViewportBackground(graphics);
      this.beginOrderViewport(graphics);
      List<WorkOrderData.WorkEntry> entries = ((WorkOrderMenu)this.f_97732_).getEntries();
      if (entries.isEmpty()) {
         this.drawFitted(graphics, Component.m_237115_("guistrings.npc.work_order.empty"), 8, this.orderViewportTop + 8, 230, -10461088);
      }

      int hoverX = this.insideOrderViewport((double)mouseX, (double)mouseY) ? mouseX : Integer.MIN_VALUE;
      int hoverY = this.insideOrderViewport((double)mouseX, (double)mouseY) ? mouseY : Integer.MIN_VALUE;

      for (int index = 0; index < entries.size(); index++) {
         int y = this.entryY(index);
         if (y + 25 >= this.orderViewportTop && y < this.orderViewportTop + this.orderViewportHeight) {
            this.drawEntry(graphics, entries.get(index), y, hoverX, hoverY);
         }
      }

      this.endOrderViewport(graphics);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   private void drawEntry(GuiGraphics graphics, WorkOrderData.WorkEntry entry, int y, int mouseX, int mouseY) {
      LegacyGuiStyle.slot(graphics, this.f_97735_, this.f_97736_, 8, y + 2);
      graphics.m_280203_(this.blockStack(entry.pos()), this.f_97735_ + 8, this.f_97736_ + y + 2);
      this.drawFitted(graphics, Component.m_237113_(this.formatPos(entry.pos())), 28, y, 138, -12566464);
      this.drawButton(graphics, 48, y + 10, 12, Component.m_237113_("+"), mouseX, mouseY);
      this.drawButton(graphics, 60, y + 10, 12, Component.m_237113_("-"), mouseX, mouseY);
      this.drawButton(graphics, 72, y + 10, 60, Component.m_237115_("guistrings.npc.remove_work_point"), mouseX, mouseY);
      this.drawFitted(graphics, Component.m_237115_("guistrings.npc.work_length"), 172, y, 60, -12566464);
      this.drawValue(graphics, 172, y + 10, 60, String.format(Locale.ROOT, "%.2f", (float)entry.workLength() / 1200.0F));
   }

   private void drawButton(GuiGraphics graphics, int x, int y, int width, Component label, int mouseX, int mouseY) {
      LegacyGuiStyle.buttonWithText(graphics, this.f_96547_, this.f_97735_, this.f_97736_, x, y, width, 12, label, mouseX, mouseY);
   }

   private void drawValue(GuiGraphics graphics, int x, int y, int width, String value) {
      graphics.m_280509_(this.f_97735_ + x, this.f_97736_ + y, this.f_97735_ + x + width, this.f_97736_ + y + 12, -1710619);
      graphics.m_280509_(this.f_97735_ + x + 1, this.f_97736_ + y + 1, this.f_97735_ + x + width - 1, this.f_97736_ + y + 11, -16777216);
      this.drawFitted(graphics, Component.m_237113_(value), x + 2, y + 2, width - 4, -1);
   }

   private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92852_(text)));
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)(this.f_97735_ + x), (float)(this.f_97736_ + y), 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, text, 0, 0, color, false);
      graphics.m_280168_().m_85849_();
   }

   protected void m_280072_(GuiGraphics graphics, int mouseX, int mouseY) {
      if (((WorkOrderMenu)this.f_97732_).m_142621_().m_41619_() && this.insideOrderViewport((double)mouseX, (double)mouseY)) {
         int x = mouseX - this.f_97735_;
         int y = mouseY - this.f_97736_;
         List<WorkOrderData.WorkEntry> entries = ((WorkOrderMenu)this.f_97732_).getEntries();

         for (int index = 0; index < entries.size(); index++) {
            WorkOrderData.WorkEntry entry = entries.get(index);
            int entryY = this.entryY(index);
            if (this.inside(x, y, 8, entryY + 2, 16, 16)) {
               graphics.m_280153_(this.f_96547_, this.blockStack(entry.pos()), mouseX, mouseY);
               return;
            }

            if (this.inside(x, y, 28, entryY, 138, 9)) {
               graphics.m_280557_(this.f_96547_, Component.m_237113_(this.formatPos(entry.pos())), mouseX, mouseY);
               return;
            }
         }
      }

      super.m_280072_(graphics, mouseX, mouseY);
   }

   @Override
   public boolean m_6375_(double mouseX, double mouseY, int button) {
      this.clampOrderScroll();
      if (button == 0) {
         int clicked = this.getButtonAt(mouseX, mouseY);
         if (clicked >= 0) {
            if (this.f_96541_.f_91072_ != null) {
               this.f_96541_.f_91072_.m_105208_(((WorkOrderMenu)this.f_97732_).f_38840_, clicked);
            }

            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   private int getButtonAt(double mouseX, double mouseY) {
      int x = (int)mouseX - this.f_97735_;
      int y = (int)mouseY - this.f_97736_;
      if (this.inside(x, y, 8, 8, 100, 12)) {
         return 0;
      } else if (this.inside(x, y, 8, 24, 100, 12)) {
         return 1;
      } else if (!this.insideOrderViewport(mouseX, mouseY)) {
         return -1;
      } else {
         for (int index = 0; index < ((WorkOrderMenu)this.f_97732_).getEntries().size(); index++) {
            int entryY = this.entryY(index);
            if (this.inside(x, y, 48, entryY + 10, 12, 12)) {
               return WorkOrderMenu.incrementButton(index);
            }

            if (this.inside(x, y, 60, entryY + 10, 12, 12)) {
               return WorkOrderMenu.decrementButton(index);
            }

            if (this.inside(x, y, 72, entryY + 10, 60, 12)) {
               return WorkOrderMenu.removeButton(index);
            }
         }

         return -1;
      }
   }

   private int entryY(int index) {
      return this.orderViewportTop + 8 + index * 25 - this.scrollPixels;
   }

   private Component priorityLabel() {
      return switch (((WorkOrderMenu)this.f_97732_).getPriorityType()) {
         case SITE_NEED -> Component.m_237115_("guistrings.npc.work_priority.site_need");
         case ROUTE -> Component.m_237115_("guistrings.npc.work_priority.route");
         case TIMED -> Component.m_237115_("guistrings.npc.work_priority.timed");
      };
   }

   private Component shiftLabel() {
      return ((WorkOrderMenu)this.f_97732_).isNightShift()
         ? Component.m_237115_("guistrings.npc.work_shift.night")
         : Component.m_237115_("guistrings.npc.work_shift.day");
   }

   private ItemStack blockStack(BlockPos pos) {
      if (this.f_96541_ != null && this.f_96541_.f_91073_ != null && this.f_96541_.f_91073_.m_46805_(pos)) {
         ItemStack stack = new ItemStack(this.f_96541_.f_91073_.m_8055_(pos).m_60734_());
         return stack.m_41619_() ? new ItemStack(Items.f_42127_) : stack;
      } else {
         return new ItemStack(Items.f_42127_);
      }
   }

   private String formatPos(BlockPos pos) {
      return "X:" + pos.m_123341_() + " Y:" + pos.m_123342_() + " Z:" + pos.m_123343_();
   }

   private boolean inside(int x, int y, int left, int top, int width, int height) {
      return x >= left && y >= top && x < left + width && y < top + height;
   }
}
