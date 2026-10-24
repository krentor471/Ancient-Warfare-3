package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.TradeOrderData;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.TradeOrderMenu;

public class TradeOrderScreen extends LegacyOrderScreen<TradeOrderMenu> {
   private static final int PANEL_WIDTH = 256;
   private static final int PANEL_HEIGHT = 240;
   private static final int LIST_Y = 24;
   private static final int LIST_HEIGHT = 128;
   private static final int ENTRY_TOP = 8;
   private static final int ENTRY_HEIGHT = 59;
   private static final int INPUT_X = 8;
   private static final int OUTPUT_X = 71;
   private static final int CONTROL_X = 129;
   private static final int CONTROL_WIDTH = 55;
   private static final int SLOT = 18;
   private static final int TEXT = -1;
   private static final int MUTED = -4671304;
   private int pressedButton = -1;
   private int mode;
   private EditBox routeX;
   private EditBox routeY;
   private EditBox routeZ;
   private Component routeStatus = Component.empty();

   public TradeOrderScreen(TradeOrderMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 256, 240, 24, 128);
   }

   @Override
   protected void init() {
      super.init();
      BlockPos target = ((TradeOrderMenu)this.menu).getRouteTarget();
      this.routeX = this.addRouteBox(this.leftPos + 50, this.topPos + 52, target.getX());
      this.routeY = this.addRouteBox(this.leftPos + 50, this.topPos + 78, target.getY());
      this.routeZ = this.addRouteBox(this.leftPos + 50, this.topPos + 104, target.getZ());
      this.updateRouteWidgetVisibility();
   }

   private EditBox addRouteBox(int x, int y, int value) {
      EditBox box = new EditBox(this.font, x, y, 158, 18, Component.empty());
      box.setMaxLength(12);
      box.setTextColor(-1);
      box.setTextColorUneditable(-4671304);
      box.setFilter(text -> text.matches("-?[0-9]*"));
      box.setValue(Integer.toString(value));
      return this.addLegacyWidget("route coordinate " + y, box, x - this.leftPos, y - this.topPos, 158, 18);
   }

   private void updateRouteWidgetVisibility() {
      boolean visible = this.mode == 1;
      this.routeX.setVisible(visible);
      this.routeY.setVisible(visible);
      this.routeZ.setVisible(visible);
      this.routeX.setEditable(visible);
      this.routeY.setEditable(visible);
      this.routeZ.setEditable(visible);
      if (!visible) {
         this.routeX.setFocused(false);
         this.routeY.setFocused(false);
         this.routeZ.setFocused(false);
      }
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawOrderViewportBackground(graphics);
      this.drawTabs(graphics, mouseX, mouseY);
      if (this.mode == 0) {
         this.drawTrades(graphics, mouseX, mouseY);
      } else if (this.mode == 1) {
         this.drawRoute(graphics, mouseX, mouseY);
      } else {
         this.drawCenteredFitted(graphics, Component.translatable("guistrings.npc.trade_order.restock_unavailable"), 8, 76, this.imageWidth - 24, 12, -4671304);
      }
   }

   @Override
   protected int orderContentHeight() {
      return 8 + ((TradeOrderMenu)this.menu).getTrades().size() * 59 + 12;
   }

   @Override
   protected boolean isOrderViewportActive() {
      return this.mode == 0;
   }

   private void drawRoute(GuiGraphics graphics, int mouseX, int mouseY) {
      Component heading = (Component)(this.routeStatus.getString().isEmpty() ? Component.translatable("guistrings.npc.trade_order.route_title") : this.routeStatus);
      this.drawCenteredFitted(graphics, heading, 8, 34, this.imageWidth - 24, 12, this.routeStatus.getString().isEmpty() ? -1 : -34953);
      graphics.drawString(this.font, Component.literal("X"), this.leftPos + 30, this.topPos + 57, -1, false);
      graphics.drawString(this.font, Component.literal("Y"), this.leftPos + 30, this.topPos + 83, -1, false);
      graphics.drawString(this.font, Component.literal("Z"), this.leftPos + 30, this.topPos + 109, -1, false);
      if (((TradeOrderMenu)this.menu).isTraderBound()) {
         this.drawButton(graphics, 81, 130, 94, 16, Component.translatable("guistrings.npc.trade_order.depart"), false, mouseX, mouseY);
      } else {
         this.drawCenteredFitted(graphics, Component.translatable("guistrings.npc.trade_order.route_unbound_hint"), 8, 132, this.imageWidth - 24, 12, -4671304);
      }
   }

   private void depart() {
      try {
         int x = Integer.parseInt(this.routeX.getValue());
         int y = Integer.parseInt(this.routeY.getValue());
         int z = Integer.parseInt(this.routeZ.getValue());
         this.routeStatus = Component.empty();
         AWCorePlatform.sendTraderToRoute(((TradeOrderMenu)this.menu).getTraderEntityId(), new BlockPos(x, y, z));
      } catch (NumberFormatException var4) {
         this.routeStatus = Component.translatable("guistrings.npc.trade_order.invalid_coordinates");
      }
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      ItemStack hovered = this.hoveredTradeStack(mouseX, mouseY);
      if (!hovered.isEmpty()) {
         graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
      }
   }

   private void drawTabs(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawTab(graphics, 8, Component.translatable("guistrings.npc.trade.trade"), 0, mouseX, mouseY);
      this.drawTab(graphics, 83, Component.translatable("guistrings.npc.trade_order.route"), 1, mouseX, mouseY);
      this.drawTab(graphics, 158, Component.translatable("guistrings.npc.trade_order.restock"), 2, mouseX, mouseY);
   }

   private void drawTab(GuiGraphics graphics, int x, Component label, int tabMode, int mouseX, int mouseY) {
      if (this.mode == tabMode) {
         LegacyGuiStyle.nineSlice(graphics, this.leftPos, this.topPos, x, 8, 75, 12, 0, 0, 256, 40);
         this.drawCenteredFitted(graphics, label, x + 2, 9, 71, 10, -1);
      } else {
         LegacyGuiStyle.buttonWithText(graphics, this.font, this.leftPos, this.topPos, x, 8, 75, 12, label, mouseX, mouseY);
      }
   }

   private void drawTrades(GuiGraphics graphics, int mouseX, int mouseY) {
      List<TradeOrderData.TradeEntry> trades = ((TradeOrderMenu)this.menu).getTrades();
      this.clampOrderScroll();
      this.beginOrderViewport(graphics);

      for (int index = 0; index < trades.size(); index++) {
         int y = 32 + index * 59 - this.scrollPixels;
         if (y + 59 >= 24 && y <= 152) {
            this.drawTrade(graphics, trades.get(index), index, y, mouseX, mouseY);
         }
      }

      int addY = 32 + trades.size() * 59 - this.scrollPixels;
      if (addY < 152 && addY + 12 > 24) {
         this.drawButton(graphics, 8, addY, this.imageWidth - 24, 12, Component.translatable("guistrings.npc.trade.add"), this.pressedButton == 1, mouseX, mouseY);
      }

      this.endOrderViewport(graphics);
   }

   private void drawTrade(GuiGraphics graphics, TradeOrderData.TradeEntry trade, int index, int y, int mouseX, int mouseY) {
      this.drawGrid(graphics, 8, y, trade.inputs());
      graphics.drawString(this.font, Component.literal(">"), this.leftPos + 63, this.topPos + y + 20, -1, true);
      this.drawGrid(graphics, 71, y, trade.outputs());
      this.drawButton(
         graphics,
         129,
         y - 1,
         55,
         12,
         Component.translatable("guistrings.npc.trade_order.move_up"),
         this.pressedButton == TradeOrderMenu.moveUpButton(index),
         mouseX,
         mouseY
      );
      this.drawButton(
         graphics,
         129,
         y + 20,
         55,
         12,
         Component.translatable("guistrings.npc.trade_order.remove"),
         this.pressedButton == TradeOrderMenu.removeButton(index),
         mouseX,
         mouseY
      );
      this.drawButton(
         graphics,
         129,
         y + 41,
         55,
         12,
         Component.translatable("guistrings.npc.trade_order.move_down"),
         this.pressedButton == TradeOrderMenu.moveDownButton(index),
         mouseX,
         mouseY
      );
      graphics.fill(this.leftPos + 3, this.topPos + y + 55, this.leftPos + this.imageWidth - 12, this.topPos + y + 56, -16777216);
   }

   private void drawGrid(GuiGraphics graphics, int x, int y, List<ItemStack> stacks) {
      for (int slot = 0; slot < 9; slot++) {
         int sx = x + slot % 3 * 18;
         int sy = y + slot / 3 * 18;
         LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, sx, sy);
         ItemStack stack = stacks.get(slot);
         if (!stack.isEmpty()) {
            graphics.renderFakeItem(stack, this.leftPos + sx, this.topPos + sy);
            graphics.renderItemDecorations(this.font, stack, this.leftPos + sx, this.topPos + sy);
         }
      }
   }

   private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean pressed, int mouseX, int mouseY) {
      boolean hovered = inBounds((double)(mouseX - this.leftPos), (double)(mouseY - this.topPos), x, y, width, height)
         && (this.mode != 0 || this.insideOrderViewport((double)mouseX, (double)mouseY));
      LegacyGuiStyle.nineSlice(graphics, this.leftPos, this.topPos, x, y, width, height, 0, !pressed && !hovered ? 40 : 80, 256, 40);
      this.drawCenteredFitted(graphics, label, x + 2, y + 1, width - 4, height - 2, -1);
   }

   private void drawCenteredFitted(GuiGraphics graphics, Component text, int x, int y, int width, int height, int color) {
      int textWidth = this.font.width(text);
      float scale = textWidth <= width ? 1.0F : (float)width / (float)textWidth;
      int scaledWidth = Math.round((float)textWidth * scale);
      int scaledHeight = Math.round(9.0F * scale);
      graphics.pose().pushPose();
      graphics.pose()
         .translate((float)(this.leftPos + x) + (float)(width - scaledWidth) / 2.0F, (float)(this.topPos + y) + (float)(height - scaledHeight) / 2.0F, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, 0, 0, color, false);
      graphics.pose().popPose();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.clampOrderScroll();
         int clicked = this.getButtonAt(mouseX, mouseY);
         if (clicked == -20) {
            this.depart();
            return true;
         }

         if (clicked <= -10) {
            this.mode = -clicked - 10;
            this.scrollPixels = 0;
            this.pressedButton = -1;
            this.updateRouteWidgetVisibility();
            return true;
         }

         if (clicked >= 0) {
            this.pressedButton = clicked;
            if (this.minecraft.gameMode != null) {
               this.minecraft.gameMode.handleInventoryButtonClick(((TradeOrderMenu)this.menu).containerId, clicked);
            }

            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.pressedButton = -1;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private int getButtonAt(double mouseX, double mouseY) {
      int x = (int)mouseX - this.leftPos;
      int y = (int)mouseY - this.topPos;
      if (inBounds((double)x, (double)y, 8, 8, 75, 12)) {
         return -10;
      } else if (inBounds((double)x, (double)y, 83, 8, 75, 12)) {
         return -11;
      } else if (inBounds((double)x, (double)y, 158, 8, 75, 12)) {
         return -12;
      } else if (this.mode == 1 && ((TradeOrderMenu)this.menu).isTraderBound() && inBounds((double)x, (double)y, 81, 130, 94, 16)) {
         return -20;
      } else if (this.mode == 0 && this.insideOrderViewport(mouseX, mouseY)) {
         List<TradeOrderData.TradeEntry> trades = ((TradeOrderMenu)this.menu).getTrades();

         for (int index = 0; index < trades.size(); index++) {
            int entryY = 32 + index * 59 - this.scrollPixels;
            if (entryY + 59 >= 24 && entryY <= 152) {
               int slot = this.slotAt(x, y, 8, entryY);
               if (slot >= 0) {
                  return TradeOrderMenu.inputSlotButton(index, slot);
               }

               slot = this.slotAt(x, y, 71, entryY);
               if (slot >= 0) {
                  return TradeOrderMenu.outputSlotButton(index, slot);
               }

               if (inBounds((double)x, (double)y, 129, entryY - 1, 55, 12)) {
                  return TradeOrderMenu.moveUpButton(index);
               }

               if (inBounds((double)x, (double)y, 129, entryY + 20, 55, 12)) {
                  return TradeOrderMenu.removeButton(index);
               }

               if (inBounds((double)x, (double)y, 129, entryY + 41, 55, 12)) {
                  return TradeOrderMenu.moveDownButton(index);
               }
            }
         }

         int addY = 32 + trades.size() * 59 - this.scrollPixels;
         return inBounds((double)x, (double)y, 8, addY, this.imageWidth - 24, 12) ? 1 : -1;
      } else {
         return -1;
      }
   }

   private int slotAt(int x, int y, int slotX, int slotY) {
      for (int slot = 0; slot < 9; slot++) {
         int sx = slotX + slot % 3 * 18;
         int sy = slotY + slot / 3 * 18;
         if (inBounds((double)x, (double)y, sx - 1, sy - 1, 18, 18)) {
            return slot;
         }
      }

      return -1;
   }

   private ItemStack hoveredTradeStack(int mouseX, int mouseY) {
      if (this.mode == 0 && ((TradeOrderMenu)this.menu).getCarried().isEmpty() && this.insideOrderViewport((double)mouseX, (double)mouseY)) {
         int x = mouseX - this.leftPos;
         int y = mouseY - this.topPos;
         List<TradeOrderData.TradeEntry> trades = ((TradeOrderMenu)this.menu).getTrades();

         for (int index = 0; index < trades.size(); index++) {
            int entryY = 32 + index * 59 - this.scrollPixels;
            int slot = this.slotAt(x, y, 8, entryY);
            if (slot >= 0) {
               return trades.get(index).inputs().get(slot);
            }

            slot = this.slotAt(x, y, 71, entryY);
            if (slot >= 0) {
               return trades.get(index).outputs().get(slot);
            }
         }

         return ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
      return mouseX >= (double)x && mouseY >= (double)y && mouseX < (double)(x + width) && mouseY < (double)(y + height);
   }
}
