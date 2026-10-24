package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.common.npc.CombatOrderData;
import net.shadowmage.ancientwarfare.core.menu.CombatOrderMenu;

public class CombatOrderScreen extends LegacyOrderScreen<CombatOrderMenu> {
   private static final int TEXT_COLOR = 16777215;
   private static final int ENTRY_TOP = 8;
   private static final int ENTRY_HEIGHT = 12;
   private int pressedButton = -1;

   public CombatOrderScreen(CombatOrderMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 256, 220, 0, 220);
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawOrderViewportBackground(graphics);
      List<BlockPos> entries = ((CombatOrderMenu)this.menu).getEntries();
      this.beginOrderViewport(graphics);

      for (int i = 0; i < entries.size(); i++) {
         this.drawEntry(graphics, entries.get(i), i, mouseX, mouseY);
      }

      if (entries.isEmpty()) {
         this.drawFittedText(graphics, Component.translatable("guistrings.npc.combat_order.empty"), 8, 10, this.imageWidth - 24);
      }

      this.endOrderViewport(graphics);
   }

   @Override
   protected int orderContentHeight() {
      return 8 + ((CombatOrderMenu)this.menu).getEntries().size() * 12;
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      if (((CombatOrderMenu)this.menu).getCarried().isEmpty() && this.insideOrderViewport((double)mouseX, (double)mouseY)) {
         List<BlockPos> entries = ((CombatOrderMenu)this.menu).getEntries();

         for (int index = 0; index < entries.size(); index++) {
            int y = 8 + index * 12 - this.scrollPixels;
            if (this.inside(mouseX - this.leftPos, mouseY - this.topPos, 8, y + 1, 108, 9)) {
               graphics.renderTooltip(this.font, Component.literal(coordinates(entries.get(index))), mouseX, mouseY);
               return;
            }
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.clampOrderScroll();
         int localX = (int)mouseX - this.leftPos;
         int localY = (int)mouseY - this.topPos;
         int clicked = this.getButtonAt(localX, localY);
         if (clicked >= 0) {
            this.pressedButton = clicked;
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CombatOrderData.removeEntry(((CombatOrderMenu)this.menu).getCombatOrderStack(), clicked - 10);
            if (this.minecraft.gameMode != null) {
               this.minecraft.gameMode.handleInventoryButtonClick(((CombatOrderMenu)this.menu).containerId, clicked);
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

   private void drawEntry(GuiGraphics graphics, BlockPos pos, int index, int mouseX, int mouseY) {
      int y = 8 + index * 12 - this.scrollPixels;
      this.drawFittedText(graphics, Component.literal(coordinates(pos)), 8, y + 1, 108);
      this.drawTexturedButton(graphics, 120, y, 12, 12, Component.literal("+"), false, false, mouseX, mouseY);
      this.drawTexturedButton(graphics, 132, y, 12, 12, Component.literal("-"), false, false, mouseX, mouseY);
      this.drawTexturedButton(
         graphics,
         144,
         y,
         60,
         12,
         Component.translatable("guistrings.npc.remove_work_point"),
         true,
         this.pressedButton == CombatOrderMenu.removeButton(index),
         mouseX,
         mouseY
      );
   }

   private int getButtonAt(int x, int y) {
      if (!this.insideOrderViewport((double)(this.leftPos + x), (double)(this.topPos + y))) {
         return -1;
      } else {
         int entryCount = ((CombatOrderMenu)this.menu).getEntries().size();

         for (int i = 0; i < entryCount; i++) {
            int entryY = 8 + i * 12 - this.scrollPixels;
            if (this.inside(x, y, 144, entryY, 60, 12)) {
               return CombatOrderMenu.removeButton(i);
            }
         }

         return -1;
      }
   }

   private boolean inside(int x, int y, int left, int top, int width, int height) {
      return x >= left && y >= top && x < left + width && y < top + height;
   }

   private static String coordinates(BlockPos pos) {
      return "X:" + pos.getX() + " Y:" + pos.getY() + " Z:" + pos.getZ();
   }

   private void drawFittedText(GuiGraphics graphics, Component text, int x, int y, int width) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.font.width(text)));
      graphics.pose().pushPose();
      graphics.pose().translate((float)(this.leftPos + x), (float)(this.topPos + y) + 9.0F * (1.0F - scale) / 2.0F, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, 0, 0, 16777215, false);
      graphics.pose().popPose();
   }

   private void drawTexturedButton(
      GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean enabled, boolean pressed, int mouseX, int mouseY
   ) {
      boolean hovered = this.insideOrderViewport((double)mouseX, (double)mouseY)
         && this.inside(mouseX - this.leftPos, mouseY - this.topPos, x, y, width, height);
      int stateY = !enabled ? 0 : (!pressed && !hovered ? 40 : 80);
      LegacyGuiStyle.nineSlice(graphics, this.leftPos, this.topPos, x, y, width, height, 0, stateY, 256, 40);
      int textWidth = this.font.width(label);
      float scale = textWidth <= width - 4 ? 1.0F : (float)(width - 4) / (float)textWidth;
      graphics.pose().pushPose();
      graphics.pose()
         .translate((float)(this.leftPos + x) + (scale == 1.0F ? (float)(width - textWidth) / 2.0F : 2.0F), (float)(this.topPos + y) + 2.0F, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      int textColor = !enabled ? -5658199 : (hovered ? -6250336 : 16777215);
      graphics.drawString(this.font, label, 0, 0, textColor, true);
      graphics.pose().popPose();
   }
}
