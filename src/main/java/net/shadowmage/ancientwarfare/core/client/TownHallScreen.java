package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.TownHallMenu;

public class TownHallScreen extends AbstractContainerScreen<TownHallMenu> {
   private static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   private static final ResourceLocation WIDGETS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");
   private static final int TEXT_COLOR = 16777215;
   private boolean openingDeathRecords;

   public TownHallScreen(TownHallMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.f_97726_ = 230;
      this.f_97727_ = 174;
      this.f_97728_ = 8;
      this.f_97729_ = 6;
      this.f_97731_ = 80;
   }

   protected void m_7856_() {
      super.m_7856_();
      this.m_142416_(
         new LegacyTexturedButton(
            this.f_97735_ + 176, this.f_97736_ + 22, 46, 24, Component.m_237115_("gui.ancientwarfare.town_hall.open_records"), button -> {
               this.openingDeathRecords = true;
               this.f_96541_.m_91152_(new TownHallDeathScreen(this));
            }
         )
      );
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.m_280218_(BACKGROUND, this.f_97735_, this.f_97736_, 0, 0, this.f_97726_, this.f_97727_);
      this.drawSlotGrid(graphics, 8, 22, 9, 3);
      this.drawSlotGrid(graphics, 8, 92, 9, 3);
      this.drawSlotGrid(graphics, 8, 150, 9, 1);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, this.f_96539_, this.f_97728_, this.f_97729_, 16777215, false);
      graphics.m_280614_(this.f_96547_, this.f_169604_, 8, this.f_97731_, 16777215, false);
      graphics.m_280056_(
         this.f_96547_,
         DraftingStationScreen.fittedText(
            this.f_96547_,
            Component.m_237110_("gui.ancientwarfare.town_hall.record_count", new Object[]{((TownHallMenu)this.f_97732_).getDeathNotices().size()}).getString(),
            44
         ),
         178,
         52,
         16777215,
         false
      );
   }

   public void m_7861_() {
      if (this.openingDeathRecords) {
         this.openingDeathRecords = false;
      } else {
         super.m_7861_();
      }
   }

   void deathRecordsRemoved() {
      super.m_7861_();
   }

   public TownHallMenu getMenu() {
      return (TownHallMenu)this.f_97732_;
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      this.m_280072_(graphics, mouseX, mouseY);
   }

   private void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            graphics.m_280218_(WIDGETS, this.f_97735_ + x + col * 18 - 1, this.f_97736_ + y + row * 18 - 1, 152, 120, 18, 18);
         }
      }
   }
}
