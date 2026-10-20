package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.npc.RoutingOrderData;
import net.shadowmage.ancientwarfare.core.menu.RoutingOrderMenu;
import net.shadowmage.ancientwarfare.core.tile.BasicWorksiteBlockEntity;

public class RoutingOrderScreen extends LegacyOrderScreen<RoutingOrderMenu> {
   private static final int LIST_HEIGHT = 148;
   private static final int FIRST_ROW_Y = 12;
   private static final int ROW_HEIGHT = 54;
   private static final int TEXT = -1;
   private int pressedButton = -1;

   public RoutingOrderScreen(RoutingOrderMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 256, 240, 0, 148);
   }

   @Override
   protected int orderContentHeight() {
      return 12 + ((RoutingOrderMenu)this.menu).getEntries().size() * 54;
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawOrderViewportBackground(graphics);
      this.clampOrderScroll();
      this.beginOrderViewport(graphics);
      List<RoutingOrderData.RouteEntry> entries = ((RoutingOrderMenu)this.menu).getEntries();
      if (entries.isEmpty()) {
         this.drawFitted(graphics, Component.translatable("guistrings.npc.routing_order.empty"), 8, 12, 230);
      }

      for (int i = 0; i < entries.size(); i++) {
         int y = this.rowY(i);
         if (y + 54 > 3 && y < 145) {
            this.drawEntry(graphics, entries.get(i), i, y, mouseX, mouseY);
         }
      }

      this.endOrderViewport(graphics);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   @Override
   protected void afterContainerRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (((RoutingOrderMenu)this.menu).getCarried().isEmpty() && this.insideOrderViewport((double)mouseX, (double)mouseY)) {
         List<RoutingOrderData.RouteEntry> entries = ((RoutingOrderMenu)this.menu).getEntries();

         for (int i = 0; i < entries.size(); i++) {
            RoutingOrderData.RouteEntry entry = entries.get(i);
            int y = this.rowY(i);

            for (int slot = 0; slot < 12; slot++) {
               if (this.inBounds((double)mouseX, (double)mouseY, 9 + slot * 18, y + 24, 18, 18)) {
                  ItemStack stack = entry.filters().get(slot);
                  if (!stack.isEmpty()) {
                     graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                  }

                  return;
               }
            }

            Component tooltip = null;
            if (this.inBounds((double)mouseX, (double)mouseY, 8, y, 108, 9)) {
               tooltip = this.blockName(entry);
            } else if (this.inBounds((double)mouseX, (double)mouseY, 120, y, 108, 9)) {
               tooltip = Component.literal(this.formatPos(entry.pos()));
            } else if (this.inBounds((double)mouseX, (double)mouseY, 8, y + 10, 55, 12)) {
               tooltip = this.sideLabel(entry);
            } else if (this.inBounds((double)mouseX, (double)mouseY, 65, y + 10, 79, 12)) {
               tooltip = this.routeTypeTooltip(entry);
            } else if (this.inBounds((double)mouseX, (double)mouseY, 146, y + 10, 38, 12)) {
               tooltip = this.damageLabel(entry);
            } else if (this.inBounds((double)mouseX, (double)mouseY, 186, y + 10, 38, 12)) {
               tooltip = this.tagLabel(entry);
            } else if (this.inBounds((double)mouseX, (double)mouseY, 229, y, 12, 9)) {
               tooltip = Component.translatable("guistrings.npc.trade_order.remove");
            } else if (this.inBounds((double)mouseX, (double)mouseY, 229, y + 15, 12, 12)) {
               tooltip = Component.translatable("guistrings.npc.trade_order.move_up");
            } else if (this.inBounds((double)mouseX, (double)mouseY, 229, y + 29, 12, 12)) {
               tooltip = Component.translatable("guistrings.npc.trade_order.move_down");
            }

            if (tooltip != null) {
               graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
               return;
            }
         }
      }
   }

   private void drawEntry(GuiGraphics graphics, RoutingOrderData.RouteEntry entry, int index, int y, int mouseX, int mouseY) {
      this.drawFitted(graphics, this.blockName(entry), 8, y, 108);
      this.drawFitted(graphics, Component.literal(this.formatPos(entry.pos())), 120, y, 108);
      this.drawButton(
         graphics,
         8,
         y + 10,
         55,
         12,
         Component.translatable("guistrings.inventory.direction." + entry.side().getName()),
         RoutingOrderMenu.sideButton(index),
         mouseX,
         mouseY
      );
      this.drawButton(graphics, 65, y + 10, 79, 12, Component.translatable(entry.routeType().translationKey()), RoutingOrderMenu.typeButton(index), mouseX, mouseY);
      Component damage = Component.translatable("guistrings.npc.routing_order.damage_label");
      Component tag = Component.literal("NBT");
      if (entry.ignoreDamage()) {
         damage = damage.copy().withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.STRIKETHROUGH});
      }

      if (entry.ignoreTag()) {
         tag = tag.copy().withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.STRIKETHROUGH});
      }

      this.drawButton(graphics, 146, y + 10, 38, 12, damage, RoutingOrderMenu.ignoreDamageButton(index), mouseX, mouseY);
      this.drawButton(graphics, 186, y + 10, 38, 12, tag, RoutingOrderMenu.ignoreTagButton(index), mouseX, mouseY);

      for (int slot = 0; slot < 12; slot++) {
         int x = 9 + slot * 18;
         LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, x + 1, y + 25);
         ItemStack stack = entry.filters().get(slot);
         if (!stack.isEmpty()) {
            graphics.renderFakeItem(stack, this.leftPos + x + 1, this.topPos + y + 25);
            graphics.renderItemDecorations(this.font, stack, this.leftPos + x + 1, this.topPos + y + 25);
         }
      }

      this.drawButton(graphics, 229, y, 12, 9, Component.literal("X"), RoutingOrderMenu.removeButton(index), mouseX, mouseY);
      this.drawButton(graphics, 229, y + 15, 12, 12, Component.literal("^"), RoutingOrderMenu.moveUpButton(index), mouseX, mouseY);
      this.drawButton(graphics, 229, y + 29, 12, 12, Component.literal("v"), RoutingOrderMenu.moveDownButton(index), mouseX, mouseY);
      graphics.fill(this.leftPos + 3, this.topPos + y + 47, this.leftPos + 243, this.topPos + y + 48, -16777216);
   }

   private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, Component label, int id, int mouseX, int mouseY) {
      boolean pointerVisible = this.insideOrderViewport((double)mouseX, (double)mouseY);
      int pointerX = pointerVisible ? mouseX : Integer.MIN_VALUE;
      int pointerY = pointerVisible ? mouseY : Integer.MIN_VALUE;
      LegacyGuiStyle.button(graphics, this.leftPos, this.topPos, x, y, width, height, pointerX, pointerY);
      int textWidth = this.font.width(label);
      float scale = Math.min(1.0F, Math.min((float)(width - 4) / (float)Math.max(1, textWidth), (float)(height - 2) / 9.0F));
      graphics.pose().pushPose();
      graphics.pose()
         .translate(
            (float)(this.leftPos + x) + ((float)width - (float)textWidth * scale) / 2.0F,
            (float)(this.topPos + y) + ((float)height - 9.0F * scale) / 2.0F,
            0.0F
         );
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, label, 0, 0, this.pressedButton == id ? -96 : -1, false);
      graphics.pose().popPose();
   }

   private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int maxWidth) {
      float scale = Math.min(1.0F, (float)maxWidth / (float)Math.max(1, this.font.width(text)));
      graphics.pose().pushPose();
      graphics.pose().translate((float)(this.leftPos + x), (float)(this.topPos + y), 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, 0, 0, -1, false);
      graphics.pose().popPose();
   }

   private Component blockName(RoutingOrderData.RouteEntry entry) {
      if (this.minecraft != null && this.minecraft.level != null && this.minecraft.level.hasChunkAt(entry.pos())) {
         BlockState state = this.minecraft.level.getBlockState(entry.pos());
         return state.getBlock().getName();
      } else {
         return Component.translatable("guistrings.npc.routing_order.unknown_block");
      }
   }

   private Component sideLabel(RoutingOrderData.RouteEntry entry) {
      Component worldSide = Component.translatable("guistrings.inventory.direction." + entry.side().getName());
      if (this.minecraft != null && this.minecraft.level != null && this.minecraft.level.getBlockEntity(entry.pos()) instanceof BasicWorksiteBlockEntity worksite) {
         Component relativeSide = Component.translatable(worksite.getRelativeSide(entry.side()).translationKey());
         Component access = Component.translatable(worksite.getSideAccess(worksite.getRelativeSide(entry.side())).translationKey());
         return worldSide.copy().append(" / ").append(relativeSide).append(" / ").append(access);
      } else {
         return worldSide;
      }
   }

   private Component routeTypeTooltip(RoutingOrderData.RouteEntry entry) {
      boolean containerToCourier = switch (entry.routeType()) {
         case FILL_COURIER_TO, WITHDRAW_ALL_OF, WITHDRAW_ALL_EXCEPT, WITHDRAW_RATIO, WITHDRAW_EXACT, TAKE_MINIMUM -> true;
         default -> false;
      };
      return Component.literal(containerToCourier ? "C > NPC: " : "NPC > C: ").append(Component.translatable(entry.routeType().translationKey()));
   }

   private Component damageLabel(RoutingOrderData.RouteEntry entry) {
      return Component.translatable(entry.ignoreDamage() ? "guistrings.npc.routing_order.damage_off" : "guistrings.npc.routing_order.damage_on");
   }

   private Component tagLabel(RoutingOrderData.RouteEntry entry) {
      return Component.translatable(entry.ignoreTag() ? "guistrings.npc.routing_order.tag_off" : "guistrings.npc.routing_order.tag_on");
   }

   private String formatPos(BlockPos pos) {
      return "X: " + pos.getX() + "  Y: " + pos.getY() + "  Z: " + pos.getZ();
   }

   private int rowY(int index) {
      return 12 + index * 54 - this.scrollPixels;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.clampOrderScroll();
      if (button == 0 && this.insideOrderViewport(mouseX, mouseY)) {
         this.pressedButton = this.getButtonAt(mouseX, mouseY);
         if (this.pressedButton >= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(((RoutingOrderMenu)this.menu).containerId, this.pressedButton);
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
      if (!this.insideOrderViewport(mouseX, mouseY)) {
         return -1;
      } else {
         List<RoutingOrderData.RouteEntry> entries = ((RoutingOrderMenu)this.menu).getEntries();

         for (int i = 0; i < entries.size(); i++) {
            int y = this.rowY(i);
            if (y + 54 > 3 && y < 145) {
               if (this.inBounds(mouseX, mouseY, 8, y + 10, 55, 12)) {
                  return RoutingOrderMenu.sideButton(i);
               }

               if (this.inBounds(mouseX, mouseY, 65, y + 10, 79, 12)) {
                  return RoutingOrderMenu.typeButton(i);
               }

               if (this.inBounds(mouseX, mouseY, 146, y + 10, 38, 12)) {
                  return RoutingOrderMenu.ignoreDamageButton(i);
               }

               if (this.inBounds(mouseX, mouseY, 186, y + 10, 38, 12)) {
                  return RoutingOrderMenu.ignoreTagButton(i);
               }

               if (this.inBounds(mouseX, mouseY, 229, y, 12, 9)) {
                  return RoutingOrderMenu.removeButton(i);
               }

               if (this.inBounds(mouseX, mouseY, 229, y + 15, 12, 12)) {
                  return RoutingOrderMenu.moveUpButton(i);
               }

               if (this.inBounds(mouseX, mouseY, 229, y + 29, 12, 12)) {
                  return RoutingOrderMenu.moveDownButton(i);
               }

               for (int slot = 0; slot < 12; slot++) {
                  if (this.inBounds(mouseX, mouseY, 9 + slot * 18, y + 24, 18, 18)) {
                     return RoutingOrderMenu.filterSlotButton(i, slot);
                  }
               }
            }
         }

         return -1;
      }
   }

   private boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
      return mouseX >= (double)(this.leftPos + x)
         && mouseY >= (double)(this.topPos + y)
         && mouseX < (double)(this.leftPos + x + width)
         && mouseY < (double)(this.topPos + y + height);
   }
}
