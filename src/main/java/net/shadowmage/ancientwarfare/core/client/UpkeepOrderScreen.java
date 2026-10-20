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
   protected void m_7856_() {
      super.m_7856_();
      this.registerLayoutRect("upkeep target panel", 0, 0, this.f_97726_, 58, false);
      this.registerLayoutRect("upkeep food panel", 0, 60, this.f_97726_, 54, false);
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
      LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_, this.f_97726_, 58);
      UpkeepOrderData.UpkeepEntry entry = ((UpkeepOrderMenu)this.f_97732_).getEntry().orElse(null);
      if (entry == null) {
         this.drawFitted(graphics, Component.m_237115_("guistrings.npc.upkeep_order.empty"), 8, 8, 240, -10461088);
      } else {
         this.drawTarget(graphics, entry, mouseX, mouseY);
         LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_ + 60, this.f_97726_, 54);
         this.drawFoodTarget(graphics, mouseX, mouseY);
      }
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, Component.m_237115_("container.inventory"), 47, 144, -12566464, false);
   }

   private void drawTarget(GuiGraphics graphics, UpkeepOrderData.UpkeepEntry entry, int mouseX, int mouseY) {
      LegacyGuiStyle.slot(graphics, this.f_97735_, this.f_97736_, 8, 10);
      graphics.m_280203_(this.blockStack(entry.pos()), this.f_97735_ + 8, this.f_97736_ + 10);
      this.drawFitted(graphics, Component.m_237113_(this.formatPos(entry.pos())), 30, 8, 126, -12566464);
      this.drawButton(graphics, 101, 18, 55, Component.m_237115_("guistrings.inventory.direction." + entry.side().m_122433_()), mouseX, mouseY);
      this.drawFitted(graphics, Component.m_237115_("guistrings.npc.upkeep_time"), 166, 8, 74, -12566464);
      this.drawValue(graphics, 166, 18, 60, this.amount(entry));
      this.drawButton(graphics, 30, 34, 60, Component.m_237115_("guistrings.npc.upkeep_order.clear"), mouseX, mouseY);
      this.drawButton(graphics, 166, 34, 12, Component.m_237113_("-"), mouseX, mouseY);
      this.drawButton(graphics, 180, 34, 12, Component.m_237113_("+"), mouseX, mouseY);
   }

   private void drawFoodTarget(GuiGraphics graphics, int mouseX, int mouseY) {
      ItemStack food = ((UpkeepOrderMenu)this.f_97732_).getFoodTarget();
      LegacyGuiStyle.slot(graphics, this.f_97735_, this.f_97736_, 8, 70);
      ItemStack icon = food.m_41619_() ? new ItemStack(Items.f_42406_) : food;
      graphics.m_280203_(icon, this.f_97735_ + 8, this.f_97736_ + 70);
      if (!food.m_41619_()) {
         graphics.m_280370_(this.f_96547_, food, this.f_97735_ + 8, this.f_97736_ + 70);
      }

      Component description = food.m_41619_()
         ? Component.m_237115_("guistrings.npc.upkeep_order.any_food")
         : Component.m_237110_("guistrings.npc.upkeep_order.food_target", new Object[]{food.m_41786_(), food.m_41613_()});
      this.drawFitted(graphics, description, 30, 72, 124, food.m_41619_() ? -10461088 : -12566464);
      this.drawButton(graphics, 166, 78, 12, Component.m_237113_("-"), mouseX, mouseY);
      this.drawButton(graphics, 180, 78, 12, Component.m_237113_("+"), mouseX, mouseY);
      this.drawButton(graphics, 196, 78, 44, Component.m_237115_("guistrings.npc.upkeep_order.food_plus"), mouseX, mouseY);
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
      UpkeepOrderData.UpkeepEntry entry = ((UpkeepOrderMenu)this.f_97732_).getEntry().orElse(null);
      if (entry != null && ((UpkeepOrderMenu)this.f_97732_).m_142621_().m_41619_()) {
         int x = mouseX - this.f_97735_;
         int y = mouseY - this.f_97736_;
         if (this.inside(x, y, 8, 10, 16, 16)) {
            graphics.m_280153_(this.f_96547_, this.blockStack(entry.pos()), mouseX, mouseY);
            return;
         }

         if (this.inside(x, y, 30, 8, 126, 9)) {
            graphics.m_280557_(this.f_96547_, Component.m_237113_(this.formatPos(entry.pos())), mouseX, mouseY);
            return;
         }

         if (this.inside(x, y, 166, 18, 60, 12)) {
            graphics.m_280557_(this.f_96547_, Component.m_237110_("guistrings.npc.upkeep_order.amount", new Object[]{this.amount(entry)}), mouseX, mouseY);
            return;
         }

         if (this.inside(x, y, 8, 70, 16, 16) || this.inside(x, y, 30, 72, 124, 9)) {
            ItemStack food = ((UpkeepOrderMenu)this.f_97732_).getFoodTarget();
            if (food.m_41619_()) {
               graphics.m_280557_(this.f_96547_, Component.m_237115_("guistrings.npc.upkeep_order.any_food"), mouseX, mouseY);
            } else {
               graphics.m_280153_(this.f_96547_, food, mouseX, mouseY);
            }

            return;
         }
      }

      super.m_280072_(graphics, mouseX, mouseY);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0 && ((UpkeepOrderMenu)this.f_97732_).getEntry().isPresent()) {
         int clicked = this.getButtonAt((int)mouseX - this.f_97735_, (int)mouseY - this.f_97736_);
         if (clicked >= 0) {
            if (this.f_96541_.f_91072_ != null) {
               this.f_96541_.f_91072_.m_105208_(((UpkeepOrderMenu)this.f_97732_).f_38840_, clicked);
            }

            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
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
      if (this.f_96541_ != null && this.f_96541_.f_91073_ != null && this.f_96541_.f_91073_.m_46805_(pos)) {
         ItemStack stack = new ItemStack(this.f_96541_.f_91073_.m_8055_(pos).m_60734_());
         return stack.m_41619_() ? new ItemStack(Items.f_42127_) : stack;
      } else {
         return new ItemStack(Items.f_42127_);
      }
   }

   private String amount(UpkeepOrderData.UpkeepEntry entry) {
      return String.format(Locale.ROOT, "%.2f", (float)entry.upkeepAmount() / 1200.0F);
   }

   private String formatPos(BlockPos pos) {
      return "X:" + pos.m_123341_() + " Y:" + pos.m_123342_() + " Z:" + pos.m_123343_();
   }

   private boolean inside(int x, int y, int left, int top, int width, int height) {
      return x >= left && y >= top && x < left + width && y < top + height;
   }
}
