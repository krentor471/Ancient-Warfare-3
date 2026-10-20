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
      this.imageWidth = 230;
      this.imageHeight = 174;
      this.titleLabelX = 8;
      this.titleLabelY = 6;
      this.inventoryLabelY = 80;
   }

   protected void init() {
      super.init();
      this.addRenderableWidget(
         new LegacyTexturedButton(
            this.leftPos + 176, this.topPos + 22, 46, 24, Component.translatable("gui.ancientwarfare.town_hall.open_records"), button -> {
               this.openingDeathRecords = true;
               this.minecraft.setScreen(new TownHallDeathScreen(this));
            }
         )
      );
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      this.drawSlotGrid(graphics, 8, 22, 9, 3);
      this.drawSlotGrid(graphics, 8, 92, 9, 3);
      this.drawSlotGrid(graphics, 8, 150, 9, 1);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 16777215, false);
      graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 16777215, false);
      graphics.drawString(
         this.font,
         DraftingStationScreen.fittedText(
            this.font,
            Component.translatable("gui.ancientwarfare.town_hall.record_count", new Object[]{((TownHallMenu)this.menu).getDeathNotices().size()}).getString(),
            44
         ),
         178,
         52,
         16777215,
         false
      );
   }

   public void removed() {
      if (this.openingDeathRecords) {
         this.openingDeathRecords = false;
      } else {
         super.removed();
      }
   }

   void deathRecordsRemoved() {
      super.removed();
   }

   public TownHallMenu getMenu() {
      return (TownHallMenu)this.menu;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   private void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            graphics.blit(WIDGETS, this.leftPos + x + col * 18 - 1, this.topPos + y + row * 18 - 1, 152, 120, 18, 18);
         }
      }
   }
}
