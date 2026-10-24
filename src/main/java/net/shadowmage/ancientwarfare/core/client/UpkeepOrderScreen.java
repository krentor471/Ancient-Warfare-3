package net.shadowmage.ancientwarfare.core.client;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.npc.UpkeepOrderData;
import net.shadowmage.ancientwarfare.core.menu.UpkeepOrderMenu;

public class UpkeepOrderScreen extends LegacyContainerScreen<UpkeepOrderMenu> {
   private static final int TEXT = -12566464;
   private static final int MUTED = -10461088;
   private static final int FOOD_Y = 70;

   public UpkeepOrderScreen(UpkeepOrderMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 256, 240);
   }

   @Override
   protected void init() {
      super.init();
      this.registerLayoutRect("upkeep target panel", 0, 0, this.imageWidth, 58, false);
      this.registerLayoutRect("upkeep food panel", 0, 60, this.imageWidth, 54, false);
      this.registerLayoutRect("upkeep target", 8, 10, 16, 16, true);
      this.registerLayoutRect("upkeep side", 101, 18, 55, 12, true);
      this.registerLayoutRect("upkeep clear", 30, 34, 60, 12, true);
      this.registerLayoutRect("upkeep amount", 166, 18, 60, 12, true);
      this.registerLayoutRect("upkeep amount minus", 166, 34, 12, 12, true);
      this.registerLayoutRect("upkeep amount plus", 180, 34, 12, 12, true);
      this.registerLayoutRect("upkeep food filter", 8, 70, 16, 16, true);
      this.registerLayoutRect("upkeep food minus", 166, 78, 12, 12, true);
      this.registerLayoutRect("upkeep food plus", 180, 78, 12, 12, true);
      this.registerLayoutRect("upkeep food assign", 196, 78, 44, 12, true);
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.leftPos, this.topPos, this.imageWidth, 58);
      UpkeepOrderData.UpkeepEntry entry = ((UpkeepOrderMenu)this.menu).getEntry().orElse(null);
      if (entry == null) {
         this.drawFitted(graphics, Component.translatable("guistrings.npc.upkeep_order.empty"), 8, 8, 240, -10461088);
      } else {
         this.drawTarget(graphics, entry, mouseX, mouseY);
         LegacyGuiStyle.background(graphics, this.leftPos, this.topPos + 60, this.imageWidth, 54);
         this.drawFoodTarget(graphics, mouseX, mouseY);
      }
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, Component.translatable("container.inventory"), 47, 144, -12566464, false);
   }

   private void drawTarget(GuiGraphics graphics, UpkeepOrderData.UpkeepEntry entry, int mouseX, int mouseY) {
      LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, 8, 10);
      graphics.renderFakeItem(this.blockStack(entry.pos()), this.leftPos + 8, this.topPos + 10);
      this.drawFitted(graphics, Component.literal(this.formatPos(entry.pos())), 30, 8, 126, -12566464);
      this.drawButton(graphics, 101, 18, 55, Component.translatable("guistrings.inventory.direction." + entry.side().getName()), mouseX, mouseY);
      this.drawFitted(graphics, Component.translatable("guistrings.npc.upkeep_time"), 166, 8, 74, -12566464);
      this.drawValue(graphics, 166, 18, 60, this.amount(entry));
      this.drawButton(graphics, 30, 34, 60, Component.translatable("guistrings.npc.upkeep_order.clear"), mouseX, mouseY);
      this.drawButton(graphics, 166, 34, 12, Component.literal("-"), mouseX, mouseY);
      this.drawButton(graphics, 180, 34, 12, Component.literal("+"), mouseX, mouseY);
   }

   private void drawFoodTarget(GuiGraphics graphics, int mouseX, int mouseY) {
      ItemStack food = ((UpkeepOrderMenu)this.menu).getFoodTarget();
      LegacyGuiStyle.slot(graphics, this.leftPos, this.topPos, 8, 70);
      ItemStack icon = food.isEmpty() ? new ItemStack(Items.BREAD) : food;
      graphics.renderFakeItem(icon, this.leftPos + 8, this.topPos + 70);
      if (!food.isEmpty()) {
         graphics.renderItemDecorations(this.font, food, this.leftPos + 8, this.topPos + 70);
      }

      Component description = food.isEmpty()
         ? Component.translatable("guistrings.npc.upkeep_order.any_food")
         : Component.translatable("guistrings.npc.upkeep_order.food_target", new Object[]{food.getHoverName(), food.getCount()});
      this.drawFitted(graphics, description, 30, 72, 124, food.isEmpty() ? -10461088 : -12566464);
      this.drawButton(graphics, 166, 78, 12, Component.literal("-"), mouseX, mouseY);
      this.drawButton(graphics, 180, 78, 12, Component.literal("+"), mouseX, mouseY);
      this.drawButton(graphics, 196, 78, 44, Component.translatable("guistrings.npc.upkeep_order.food_plus"), mouseX, mouseY);
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
      UpkeepOrderData.UpkeepEntry entry = ((UpkeepOrderMenu)this.menu).getEntry().orElse(null);
      if (entry != null && ((UpkeepOrderMenu)this.menu).getCarried().isEmpty()) {
         int x = mouseX - this.leftPos;
         int y = mouseY - this.topPos;
         if (this.inside(x, y, 8, 10, 16, 16)) {
            graphics.renderTooltip(this.font, this.blockStack(entry.pos()), mouseX, mouseY);
            return;
         }

         if (this.inside(x, y, 30, 8, 126, 9)) {
            graphics.renderTooltip(this.font, Component.literal(this.formatPos(entry.pos())), mouseX, mouseY);
            return;
         }

         if (this.inside(x, y, 166, 18, 60, 12)) {
            graphics.renderTooltip(this.font, Component.translatable("guistrings.npc.upkeep_order.amount", new Object[]{this.amount(entry)}), mouseX, mouseY);
            return;
         }

         if (this.inside(x, y, 8, 70, 16, 16) || this.inside(x, y, 30, 72, 124, 9)) {
            ItemStack food = ((UpkeepOrderMenu)this.menu).getFoodTarget();
            if (food.isEmpty()) {
               graphics.renderTooltip(this.font, Component.translatable("guistrings.npc.upkeep_order.any_food"), mouseX, mouseY);
            } else {
               graphics.renderTooltip(this.font, food, mouseX, mouseY);
            }

            return;
         }
      }

      super.renderTooltip(graphics, mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && ((UpkeepOrderMenu)this.menu).getEntry().isPresent()) {
         int clicked = this.getButtonAt((int)mouseX - this.leftPos, (int)mouseY - this.topPos);
         if (clicked >= 0) {
            if (this.minecraft.gameMode != null) {
               this.minecraft.gameMode.handleInventoryButtonClick(((UpkeepOrderMenu)this.menu).containerId, clicked);
            }

            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private int getButtonAt(int x, int y) {
      if (this.inside(x, y, 101, 18, 55, 12)) {
         return 0;
      } else if (this.inside(x, y, 30, 34, 60, 12)) {
         return 3;
      } else if (this.inside(x, y, 166, 34, 12, 12)) {
         return 2;
      } else if (this.inside(x, y, 180, 34, 12, 12)) {
         return 1;
      } else if (this.inside(x, y, 7, 69, 18, 18)) {
         return 6;
      } else if (this.inside(x, y, 166, 78, 12, 12)) {
         return 5;
      } else if (this.inside(x, y, 180, 78, 12, 12)) {
         return 4;
      } else {
         return this.inside(x, y, 196, 78, 44, 12) ? 6 : -1;
      }
   }

   private ItemStack blockStack(BlockPos pos) {
      if (this.minecraft != null && this.minecraft.level != null && this.minecraft.level.hasChunkAt(pos)) {
         ItemStack stack = new ItemStack(this.minecraft.level.getBlockState(pos).getBlock());
         return stack.isEmpty() ? new ItemStack(Items.BARRIER) : stack;
      } else {
         return new ItemStack(Items.BARRIER);
      }
   }

   private String amount(UpkeepOrderData.UpkeepEntry entry) {
      return String.format(Locale.ROOT, "%.2f", (float)entry.upkeepAmount() / 1200.0F);
   }

   private String formatPos(BlockPos pos) {
      return "X:" + pos.getX() + " Y:" + pos.getY() + " Z:" + pos.getZ();
   }

   private boolean inside(int x, int y, int left, int top, int width, int height) {
      return x >= left && y >= top && x < left + width && y < top + height;
   }
}
