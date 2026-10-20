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
   protected void init() {
      super.init();
      this.registerLayoutRect("work priority", 8, 8, 100, 12, true);
      this.registerLayoutRect("work shift", 8, 24, 100, 12, true);
   }

   @Override
   protected int orderContentHeight() {
      return 8 + ((WorkOrderMenu)this.menu).getEntries().size() * 25;
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawButton(graphics, 8, 8, 100, this.priorityLabel(), mouseX, mouseY);
      this.drawButton(graphics, 8, 24, 100, this.shiftLabel(), mouseX, mouseY);
      this.drawOrderViewportBackground(graphics);
      this.beginOrderViewport(graphics);
      List<WorkOrderData.WorkEntry> entries = ((WorkOrderMenu)this.menu).getEntries();
      if (entries.isEmpty()) {
         this.drawFitted(graphics, Component.translatable("guistrings.npc.work_order.empty"), 8, this.orderViewportTop + 8, 230, -10461088);
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

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   private void drawEntry(GuiGraphics graphics, WorkOrderData.WorkEntry entry, int y, int mouseX, int mouseY) {
      LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, 8, y + 2);
      graphics.renderFakeItem(this.blockStack(entry.pos()), this.leftPos + 8, this.topPos + y + 2);
      this.drawFitted(graphics, Component.literal(this.formatPos(entry.pos())), 28, y, 138, -12566464);
      this.drawButton(graphics, 48, y + 10, 12, Component.literal("+"), mouseX, mouseY);
      this.drawButton(graphics, 60, y + 10, 12, Component.literal("-"), mouseX, mouseY);
      this.drawButton(graphics, 72, y + 10, 60, Component.translatable("guistrings.npc.remove_work_point"), mouseX, mouseY);
      this.drawFitted(graphics, Component.translatable("guistrings.npc.work_length"), 172, y, 60, -12566464);
      this.drawValue(graphics, 172, y + 10, 60, String.format(Locale.ROOT, "%.2f", (float)entry.workLength() / 1200.0F));
   }

   private void drawButton(GuiGraphics graphics, int x, int y, int width, Component label, int mouseX, int mouseY) {
      LegacyGuiStyle.buttonWithText(graphics, this.font, this.leftPos, this.topPos, x, y, width, 12, label, mouseX, mouseY);
   }

   private void drawValue(GuiGraphics graphics, int x, int y, int width, String value) {
      graphics.fill(this.leftPos + x, this.topPos + y, this.leftPos + x + width, this.topPos + y + 12, -1710619);
      graphics.fill(this.leftPos + x + 1, this.topPos + y + 1, this.leftPos + x + width - 1, this.topPos + y + 11, -16777216);
      this.drawFitted(graphics, Component.literal(value), x + 2, y + 2, width - 4, -1);
   }

   private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.font.width(text)));
      graphics.pose().pushPose();
      graphics.pose().translate((float)(this.leftPos + x), (float)(this.topPos + y), 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, 0, 0, color, false);
      graphics.pose().popPose();
   }

   protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      if (((WorkOrderMenu)this.menu).getCarried().isEmpty() && this.insideOrderViewport((double)mouseX, (double)mouseY)) {
         int x = mouseX - this.leftPos;
         int y = mouseY - this.topPos;
         List<WorkOrderData.WorkEntry> entries = ((WorkOrderMenu)this.menu).getEntries();

         for (int index = 0; index < entries.size(); index++) {
            WorkOrderData.WorkEntry entry = entries.get(index);
            int entryY = this.entryY(index);
            if (this.inside(x, y, 8, entryY + 2, 16, 16)) {
               graphics.renderTooltip(this.font, this.blockStack(entry.pos()), mouseX, mouseY);
               return;
            }

            if (this.inside(x, y, 28, entryY, 138, 9)) {
               graphics.renderTooltip(this.font, Component.literal(this.formatPos(entry.pos())), mouseX, mouseY);
               return;
            }
         }
      }

      super.renderTooltip(graphics, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.clampOrderScroll();
      if (button == 0) {
         int clicked = this.getButtonAt(mouseX, mouseY);
         if (clicked >= 0) {
            if (this.minecraft.gameMode != null) {
               this.minecraft.gameMode.handleInventoryButtonClick(((WorkOrderMenu)this.menu).containerId, clicked);
            }

            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private int getButtonAt(double mouseX, double mouseY) {
      int x = (int)mouseX - this.leftPos;
      int y = (int)mouseY - this.topPos;
      if (this.inside(x, y, 8, 8, 100, 12)) {
         return 0;
      } else if (this.inside(x, y, 8, 24, 100, 12)) {
         return 1;
      } else if (!this.insideOrderViewport(mouseX, mouseY)) {
         return -1;
      } else {
         for (int index = 0; index < ((WorkOrderMenu)this.menu).getEntries().size(); index++) {
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
      return switch (((WorkOrderMenu)this.menu).getPriorityType()) {
         case SITE_NEED -> Component.translatable("guistrings.npc.work_priority.site_need");
         case ROUTE -> Component.translatable("guistrings.npc.work_priority.route");
         case TIMED -> Component.translatable("guistrings.npc.work_priority.timed");
      };
   }

   private Component shiftLabel() {
      return ((WorkOrderMenu)this.menu).isNightShift()
         ? Component.translatable("guistrings.npc.work_shift.night")
         : Component.translatable("guistrings.npc.work_shift.day");
   }

   private ItemStack blockStack(BlockPos pos) {
      if (this.minecraft != null && this.minecraft.level != null && this.minecraft.level.hasChunkAt(pos)) {
         ItemStack stack = new ItemStack(this.minecraft.level.getBlockState(pos).getBlock());
         return stack.isEmpty() ? new ItemStack(Items.BARRIER) : stack;
      } else {
         return new ItemStack(Items.BARRIER);
      }
   }

   private String formatPos(BlockPos pos) {
      return "X:" + pos.getX() + " Y:" + pos.getY() + " Z:" + pos.getZ();
   }

   private boolean inside(int x, int y, int left, int top, int width, int height) {
      return x >= left && y >= top && x < left + width && y < top + height;
   }
}
