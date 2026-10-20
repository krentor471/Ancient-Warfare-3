package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.FactionTradeListRegistry;
import net.shadowmage.ancientwarfare.core.menu.FactionTradeMenu;

public class FactionTradeScreen extends AbstractContainerScreen<FactionTradeMenu> {
   private static final int PANEL_WIDTH = 256;
   private static final int PANEL_HEIGHT = 240;
   private static final int VIEW_TOP = 0;
   private static final int VIEW_HEIGHT = 148;
   private static final int VIEW_BOTTOM = 148;
   private static final int ENTRY_HEIGHT = 59;
   private static final int ENTRY_START_Y = 8;
   private static final int SLOT = 18;
   private static final int GRID_SIZE = 9;
   private static final int INPUT_X = 8;
   private static final int OUTPUT_X = 71;
   private static final int ACTION_X = 133;
   private static final int SCROLLBAR_X = 244;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int PLAYER_INV_X = 8;
   private static final int PLAYER_INV_Y = 160;
   private static final int TEXT_COLOR = -1;
   private static final int MUTED_COLOR = -4276546;
   private static final int READY_COLOR = -2559808;
   private static final int UNAVAILABLE_COLOR = -20312;
   private static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   private static final ResourceLocation BUTTONS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");
   private int scrollPixels;
   private int pressedButton = -1;
   private boolean draggingScrollbar;

   public FactionTradeScreen(FactionTradeMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.f_97726_ = 256;
      this.f_97727_ = 240;
      this.f_97728_ = 10000;
      this.f_97729_ = 10000;
      this.f_97730_ = 10000;
      this.f_97731_ = 10000;
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      this.m_280072_(graphics, mouseX, mouseY);
      ItemStack hovered = this.hoveredTradeStack((double)mouseX, (double)mouseY);
      if (!hovered.m_41619_()) {
         graphics.m_280153_(this.f_96547_, hovered, mouseX, mouseY);
      }
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.drawQuarteredTexture(graphics, BACKGROUND, 0, 0, 256, 240, 0, 0, 256, 240);
      this.drawQuarteredTexture(graphics, BACKGROUND, 0, 0, 256, 148, 0, 0, 256, 240);
      this.drawTrades(graphics, mouseX, mouseY);
      this.drawPlayerInventorySlots(graphics);
      this.drawScrollbar(graphics);
   }

   private void drawTrades(GuiGraphics graphics, int mouseX, int mouseY) {
      List<FactionTradeListRegistry.FactionTradeEntry> trades = ((FactionTradeMenu)this.f_97732_).getTrades();
      this.scrollPixels = Math.min(this.scrollPixels, this.maxScrollPixels(trades.size()));
      graphics.m_280588_(this.f_97735_, this.f_97736_ + 0, this.f_97735_ + 244, this.f_97736_ + 148);
      if (trades.isEmpty()) {
         graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.npc.trade.no_trades"), this.f_97735_ + 8, this.f_97736_ + 8, -4276546, false);
      }

      for (int index = 0; index < trades.size(); index++) {
         int y = 8 + index * 59 - this.scrollPixels;
         if (y >= 148) {
            break;
         }

         if (y + 59 > 0) {
            this.drawTrade(graphics, trades.get(index), index, y, mouseX, mouseY);
         }
      }

      graphics.m_280618_();
   }

   private void drawTrade(GuiGraphics graphics, FactionTradeListRegistry.FactionTradeEntry trade, int index, int y, int mouseX, int mouseY) {
      this.drawStackGrid(graphics, 8, y, trade.inputs());
      this.drawStackGrid(graphics, 71, y, trade.outputs());
      graphics.m_280056_(this.f_96547_, ">", this.f_97735_ + 63, this.f_97736_ + y + 20, -1, false);
      boolean canTrade = ((FactionTradeMenu)this.f_97732_).canTrade(index);
      Component state = Component.m_237115_(canTrade ? "guistrings.npc.trade.available" : "guistrings.npc.trade.unavailable");
      this.drawFitted(graphics, state, 133, y, 70, canTrade ? -2559808 : -20312);
      this.drawButton(
         graphics,
         133,
         y + 17,
         70,
         20,
         Component.m_237115_("guistrings.npc.trade.trade"),
         true,
         this.pressedButton == FactionTradeMenu.tradeButton(index),
         mouseX,
         mouseY
      );
      graphics.m_280656_(this.f_97735_, this.f_97735_ + 256 - 1, this.f_97736_ + y + 55, -16777216);
   }

   private void drawStackGrid(GuiGraphics graphics, int x, int y, List<ItemStack> stacks) {
      for (int slot = 0; slot < 9; slot++) {
         int slotX = x + slot % 3 * 18;
         int slotY = y + slot / 3 * 18;
         this.drawSlot(graphics, slotX, slotY);
         if (slot < stacks.size() && !stacks.get(slot).m_41619_()) {
            ItemStack stack = stacks.get(slot);
            graphics.m_280203_(stack, this.f_97735_ + slotX, this.f_97736_ + slotY);
            graphics.m_280370_(this.f_96547_, stack, this.f_97735_ + slotX, this.f_97736_ + slotY);
         }
      }
   }

   private void drawPlayerInventorySlots(GuiGraphics graphics) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.drawSlot(graphics, 8 + col * 18, 160 + row * 18);
         }
      }

      for (int col = 0; col < 9; col++) {
         this.drawSlot(graphics, 8 + col * 18, 218);
      }
   }

   private void drawSlot(GuiGraphics graphics, int x, int y) {
      graphics.m_280163_(BUTTONS, this.f_97735_ + x - 1, this.f_97736_ + y - 1, 152.0F, 120.0F, 18, 18, 256, 256);
   }

   private void drawScrollbar(GuiGraphics graphics) {
      this.drawQuarteredTexture(graphics, BUTTONS, 244, 0, 12, 148, 80, 120, 40, 128);
      int max = this.maxScrollPixels(((FactionTradeMenu)this.f_97732_).getTrades().size());
      int thumbHeight = this.thumbHeight(((FactionTradeMenu)this.f_97732_).getTrades().size());
      int thumbY = 1;
      if (max > 0) {
         thumbY += (int)Math.round((double)(146 - thumbHeight) * (double)this.scrollPixels / (double)max);
      }

      this.drawQuarteredTexture(graphics, BUTTONS, 245, thumbY, 10, thumbHeight, 120, 120, 32, 128);
   }

   private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean enabled, boolean pressed, int mouseX, int mouseY) {
      boolean hovered = enabled && inBounds((double)mouseX, (double)mouseY, this.f_97735_ + x, this.f_97736_ + y, width, height);
      int textureY = enabled && !pressed ? (hovered ? 80 : 40) : 0;
      this.drawQuarteredTexture(graphics, BUTTONS, x, y, width, height, 0, textureY, 256, 40);
      this.drawCenteredFitted(graphics, label, x, y, width, height, hovered ? -6250497 : -1);
   }

   private void drawQuarteredTexture(
      GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight
   ) {
      int leftWidth = width / 2;
      int rightWidth = width - leftWidth;
      int topHeight = height / 2;
      int bottomHeight = height - topHeight;
      int sourceLeft = sourceWidth / 2;
      int sourceRight = sourceWidth - sourceLeft;
      int sourceTop = sourceHeight / 2;
      int sourceBottom = sourceHeight - sourceTop;
      graphics.m_280411_(texture, this.f_97735_ + x, this.f_97736_ + y, leftWidth, topHeight, (float)u, (float)v, sourceLeft, sourceTop, 256, 256);
      graphics.m_280411_(
         texture, this.f_97735_ + x + leftWidth, this.f_97736_ + y, rightWidth, topHeight, (float)(u + sourceLeft), (float)v, sourceRight, sourceTop, 256, 256
      );
      graphics.m_280411_(
         texture,
         this.f_97735_ + x,
         this.f_97736_ + y + topHeight,
         leftWidth,
         bottomHeight,
         (float)u,
         (float)(v + sourceTop),
         sourceLeft,
         sourceBottom,
         256,
         256
      );
      graphics.m_280411_(
         texture,
         this.f_97735_ + x + leftWidth,
         this.f_97736_ + y + topHeight,
         rightWidth,
         bottomHeight,
         (float)(u + sourceLeft),
         (float)(v + sourceTop),
         sourceRight,
         sourceBottom,
         256,
         256
      );
   }

   private void drawCenteredFitted(GuiGraphics graphics, Component text, int x, int y, int width, int height, int color) {
      int textWidth = this.f_96547_.m_92852_(text);
      float scale = textWidth <= width - 4 ? 1.0F : (float)(width - 4) / (float)textWidth;
      int scaledWidth = Math.round((float)textWidth * scale);
      int scaledHeight = Math.round(9.0F * scale);
      graphics.m_280168_().m_85836_();
      graphics.m_280168_()
         .m_252880_((float)(this.f_97735_ + x) + (float)(width - scaledWidth) / 2.0F, (float)(this.f_97736_ + y) + (float)(height - scaledHeight) / 2.0F, 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, text, 0, 0, color, false);
      graphics.m_280168_().m_85849_();
   }

   private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
      int textWidth = this.f_96547_.m_92852_(text);
      if (textWidth <= maxWidth) {
         graphics.m_280614_(this.f_96547_, text, this.f_97735_ + x, this.f_97736_ + y, color, false);
      } else {
         float scale = (float)maxWidth / (float)textWidth;
         graphics.m_280168_().m_85836_();
         graphics.m_280168_().m_252880_((float)(this.f_97735_ + x), (float)(this.f_97736_ + y), 0.0F);
         graphics.m_280168_().m_85841_(scale, scale, 1.0F);
         graphics.m_280614_(this.f_96547_, text, 0, 0, color, false);
         graphics.m_280168_().m_85849_();
      }
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0 && inBounds(mouseX, mouseY, this.f_97735_ + 244, this.f_97736_ + 0, 12, 148)) {
         this.draggingScrollbar = true;
         this.updateScrollFromMouse(mouseY);
         return true;
      } else {
         if (button == 0) {
            this.pressedButton = this.getButtonAt(mouseX, mouseY);
            if (this.pressedButton >= 0) {
               this.f_96541_.f_91072_.m_105208_(((FactionTradeMenu)this.f_97732_).f_38840_, this.pressedButton);
               return true;
            }
         }

         return super.m_6375_(mouseX, mouseY, button);
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (inBounds(mouseX, mouseY, this.f_97735_, this.f_97736_ + 0, 256, 148)) {
         this.scrollPixels = Math.max(
            0, Math.min(this.maxScrollPixels(((FactionTradeMenu)this.f_97732_).getTrades().size()), this.scrollPixels - (int)Math.signum(delta) * 18)
         );
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingScrollbar) {
         this.updateScrollFromMouse(mouseY);
         return true;
      } else {
         return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      this.pressedButton = -1;
      this.draggingScrollbar = false;
      return super.m_6348_(mouseX, mouseY, button);
   }

   private int getButtonAt(double mouseX, double mouseY) {
      List<FactionTradeListRegistry.FactionTradeEntry> trades = ((FactionTradeMenu)this.f_97732_).getTrades();

      for (int index = 0; index < trades.size(); index++) {
         int y = 8 + index * 59 - this.scrollPixels;
         if (y >= 148) {
            break;
         }

         if (y + 59 > 0 && inBounds(mouseX, mouseY, this.f_97735_ + 133, this.f_97736_ + y + 17, 70, 20)) {
            return FactionTradeMenu.tradeButton(index);
         }
      }

      return -1;
   }

   private ItemStack hoveredTradeStack(double mouseX, double mouseY) {
      if (!inBounds(mouseX, mouseY, this.f_97735_, this.f_97736_ + 0, 244, 148)) {
         return ItemStack.f_41583_;
      } else {
         List<FactionTradeListRegistry.FactionTradeEntry> trades = ((FactionTradeMenu)this.f_97732_).getTrades();

         for (int index = 0; index < trades.size(); index++) {
            int y = 8 + index * 59 - this.scrollPixels;
            if (y >= 148) {
               break;
            }

            if (y + 59 > 0) {
               ItemStack stack = this.stackAt(mouseX, mouseY, 8, y, trades.get(index).inputs());
               if (!stack.m_41619_()) {
                  return stack;
               }

               stack = this.stackAt(mouseX, mouseY, 71, y, trades.get(index).outputs());
               if (!stack.m_41619_()) {
                  return stack;
               }
            }
         }

         return ItemStack.f_41583_;
      }
   }

   private ItemStack stackAt(double mouseX, double mouseY, int x, int y, List<ItemStack> stacks) {
      for (int slot = 0; slot < Math.min(9, stacks.size()); slot++) {
         int slotX = x + slot % 3 * 18;
         int slotY = y + slot / 3 * 18;
         if (inBounds(mouseX, mouseY, this.f_97735_ + slotX - 1, this.f_97736_ + slotY - 1, 18, 18)) {
            return stacks.get(slot);
         }
      }

      return ItemStack.f_41583_;
   }

   private void updateScrollFromMouse(double mouseY) {
      int max = this.maxScrollPixels(((FactionTradeMenu)this.f_97732_).getTrades().size());
      int thumbHeight = this.thumbHeight(((FactionTradeMenu)this.f_97732_).getTrades().size());
      if (max <= 0) {
         this.scrollPixels = 0;
      } else {
         int travel = 146 - thumbHeight;
         double local = Math.max(0.0, Math.min((double)travel, mouseY - (double)(this.f_97736_ + 0 + 1) - (double)thumbHeight / 2.0));
         this.scrollPixels = (int)Math.round(local / (double)travel * (double)max);
      }
   }

   private int maxScrollPixels(int tradeCount) {
      return Math.max(0, 8 + tradeCount * 59 - 148);
   }

   private int thumbHeight(int tradeCount) {
      int contentHeight = 8 + tradeCount * 59;
      int trackHeight = 146;
      return contentHeight <= 148 ? trackHeight : Math.max(10, (int)Math.round((double)trackHeight * (double)trackHeight / (double)contentHeight));
   }

   private static boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
      return mouseX >= (double)x && mouseY >= (double)y && mouseX < (double)(x + width) && mouseY < (double)(y + height);
   }
}
