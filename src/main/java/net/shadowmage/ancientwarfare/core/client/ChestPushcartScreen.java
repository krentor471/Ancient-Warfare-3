package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.ChestPushcartMenu;

public class ChestPushcartScreen extends AbstractContainerScreen<ChestPushcartMenu> {
   private static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   private static final ResourceLocation WIDGETS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");
   private static final int TEXT_COLOR = 16777215;
   private Button upButton;
   private Button downButton;

   public ChestPushcartScreen(ChestPushcartMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 195;
      this.imageHeight = 212;
      this.titleLabelX = 8;
      this.titleLabelY = 8;
      this.inventoryLabelY = 116;
   }

   protected void init() {
      super.init();
      this.upButton = (Button)this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
         this.minecraft.gameMode.handleInventoryButtonClick(((ChestPushcartMenu)this.menu).containerId, ChestPushcartMenu.scrollUpButtonId());
         ((ChestPushcartMenu)this.menu).scrollStorageRows(-1);
         this.updateScrollButtons();
      }).bounds(this.leftPos + 171, this.topPos + 33, 16, 16).build());
      this.downButton = (Button)this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
         this.minecraft.gameMode.handleInventoryButtonClick(((ChestPushcartMenu)this.menu).containerId, ChestPushcartMenu.scrollDownButtonId());
         ((ChestPushcartMenu)this.menu).scrollStorageRows(1);
         this.updateScrollButtons();
      }).bounds(this.leftPos + 171, this.topPos + 71, 16, 16).build());
      this.addRenderableWidget(Button.builder(Component.translatable("guistrings.vehicle.pack"), button -> {
         this.minecraft.gameMode.handleInventoryButtonClick(((ChestPushcartMenu)this.menu).containerId, ChestPushcartMenu.packButtonId());
         this.minecraft.player.closeContainer();
      }).bounds(this.leftPos + 8, this.topPos + 91, 45, 18).build());
      this.updateScrollButtons();
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawNineSlice(graphics, BACKGROUND, 0, 0, this.imageWidth, this.imageHeight, 0, 0, 256, 240);
      this.drawSlotGrid(graphics, 8, 34, 9, ((ChestPushcartMenu)this.menu).getVisibleStorageRows());
      this.drawSlotGrid(graphics, 62, 92, 6, 1);
      this.drawSlotGrid(graphics, 8, 128, 9, 3);
      this.drawSlotGrid(graphics, 8, 186, 9, 1);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 16777215, false);
      if (((ChestPushcartMenu)this.menu).getStorageRows() > ((ChestPushcartMenu)this.menu).getVisibleStorageRows()) {
         graphics.drawString(
            this.font,
            Component.translatable(
               "guistrings.vehicle.storage_rows",
               new Object[]{((ChestPushcartMenu)this.menu).getTopStorageRow() + 1, ((ChestPushcartMenu)this.menu).getStorageRows()}
            ),
            8,
            23,
            16777215,
            false
         );
      }

      graphics.drawString(this.font, Component.translatable("guistrings.automation.upgrades"), 62, 82, 16777215, false);
      graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 16777215, false);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   private void updateScrollButtons() {
      if (this.upButton != null) {
         this.upButton.visible = ((ChestPushcartMenu)this.menu).getStorageRows() > ((ChestPushcartMenu)this.menu).getVisibleStorageRows();
         this.upButton.active = this.upButton.visible && ((ChestPushcartMenu)this.menu).canScrollUp();
      }

      if (this.downButton != null) {
         this.downButton.visible = ((ChestPushcartMenu)this.menu).getStorageRows() > ((ChestPushcartMenu)this.menu).getVisibleStorageRows();
         this.downButton.active = this.downButton.visible && ((ChestPushcartMenu)this.menu).canScrollDown();
      }
   }

   private void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            graphics.blit(WIDGETS, this.leftPos + x + col * 18 - 1, this.topPos + y + row * 18 - 1, 152, 120, 18, 18);
         }
      }
   }

   private void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      graphics.blit(texture, this.leftPos + x, this.topPos + y, u, v, halfWidth, halfHeight);
      graphics.blit(texture, this.leftPos + x + halfWidth, this.topPos + y, u + texWidth - (width - halfWidth), v, width - halfWidth, halfHeight);
      graphics.blit(texture, this.leftPos + x, this.topPos + y + halfHeight, u, v + texHeight - (height - halfHeight), halfWidth, height - halfHeight);
      graphics.blit(
         texture,
         this.leftPos + x + halfWidth,
         this.topPos + y + halfHeight,
         u + texWidth - (width - halfWidth),
         v + texHeight - (height - halfHeight),
         width - halfWidth,
         height - halfHeight
      );
   }
}
