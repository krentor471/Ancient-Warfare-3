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
      List<BlockPos> entries = ((CombatOrderMenu)this.f_97732_).getEntries();
      this.beginOrderViewport(graphics);

      for (int i = 0; i < entries.size(); i++) {
         this.drawEntry(graphics, entries.get(i), i, mouseX, mouseY);
      }

      if (entries.isEmpty()) {
         this.drawFittedText(graphics, Component.m_237115_("guistrings.npc.combat_order.empty"), 8, 10, this.f_97726_ - 24);
      }

      this.endOrderViewport(graphics);
   }

   @Override
   protected int orderContentHeight() {
      return 8 + ((CombatOrderMenu)this.f_97732_).getEntries().size() * 12;
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   @Override
   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      if (((CombatOrderMenu)this.f_97732_).m_142621_().m_41619_() && this.insideOrderViewport((double)mouseX, (double)mouseY)) {
         List<BlockPos> entries = ((CombatOrderMenu)this.f_97732_).getEntries();

         for (int index = 0; index < entries.size(); index++) {
            int y = 8 + index * 12 - this.scrollPixels;
            if (this.inside(mouseX - this.f_97735_, mouseY - this.f_97736_, 8, y + 1, 108, 9)) {
               graphics.m_280557_(this.f_96547_, Component.m_237113_(coordinates(entries.get(index))), mouseX, mouseY);
               return;
            }
         }
      }
   }

   @Override
   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.clampOrderScroll();
         int localX = (int)mouseX - this.f_97735_;
         int localY = (int)mouseY - this.f_97736_;
         int clicked = this.getButtonAt(localX, localY);
         if (clicked >= 0) {
            this.pressedButton = clicked;
            this.f_96541_.m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
            CombatOrderData.removeEntry(((CombatOrderMenu)this.f_97732_).getCombatOrderStack(), clicked - 10);
            if (this.f_96541_.f_91072_ != null) {
               this.f_96541_.f_91072_.m_105208_(((CombatOrderMenu)this.f_97732_).f_38840_, clicked);
            }

            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   @Override
   public boolean m_6348_(double mouseX, double mouseY, int button) {
      this.pressedButton = -1;
      return super.m_6348_(mouseX, mouseY, button);
   }

   private void drawEntry(GuiGraphics graphics, BlockPos pos, int index, int mouseX, int mouseY) {
      int y = 8 + index * 12 - this.scrollPixels;
      this.drawFittedText(graphics, Component.m_237113_(coordinates(pos)), 8, y + 1, 108);
      this.drawTexturedButton(graphics, 120, y, 12, 12, Component.m_237113_("+"), false, false, mouseX, mouseY);
      this.drawTexturedButton(graphics, 132, y, 12, 12, Component.m_237113_("-"), false, false, mouseX, mouseY);
      this.drawTexturedButton(
         graphics,
         144,
         y,
         60,
         12,
         Component.m_237115_("guistrings.npc.remove_work_point"),
         true,
         this.pressedButton == CombatOrderMenu.removeButton(index),
         mouseX,
         mouseY
      );
   }

   private int getButtonAt(int x, int y) {
      if (!this.insideOrderViewport((double)(this.f_97735_ + x), (double)(this.f_97736_ + y))) {
         return -1;
      } else {
         int entryCount = ((CombatOrderMenu)this.f_97732_).getEntries().size();

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
      return "X:" + pos.m_123341_() + " Y:" + pos.m_123342_() + " Z:" + pos.m_123343_();
   }

   private void drawFittedText(GuiGraphics graphics, Component text, int x, int y, int width) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92852_(text)));
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)(this.f_97735_ + x), (float)(this.f_97736_ + y) + 9.0F * (1.0F - scale) / 2.0F, 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, text, 0, 0, 16777215, false);
      graphics.m_280168_().m_85849_();
   }

   private void drawTexturedButton(
      GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean enabled, boolean pressed, int mouseX, int mouseY
   ) {
      boolean hovered = this.insideOrderViewport((double)mouseX, (double)mouseY)
         && this.inside(mouseX - this.f_97735_, mouseY - this.f_97736_, x, y, width, height);
      int stateY = !enabled ? 0 : (!pressed && !hovered ? 40 : 80);
      LegacyGuiStyle.nineSlice(graphics, this.f_97735_, this.f_97736_, x, y, width, height, 0, stateY, 256, 40);
      int textWidth = this.f_96547_.m_92852_(label);
      float scale = textWidth <= width - 4 ? 1.0F : (float)(width - 4) / (float)textWidth;
      graphics.m_280168_().m_85836_();
      graphics.m_280168_()
         .m_252880_((float)(this.f_97735_ + x) + (scale == 1.0F ? (float)(width - textWidth) / 2.0F : 2.0F), (float)(this.f_97736_ + y) + 2.0F, 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      int textColor = !enabled ? -5658199 : (hovered ? -6250336 : 16777215);
      graphics.m_280614_(this.f_96547_, label, 0, 0, textColor, true);
      graphics.m_280168_().m_85849_();
   }
}
