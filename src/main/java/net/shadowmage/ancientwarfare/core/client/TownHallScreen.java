package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.faction.PlayerCountry;
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

   private static final int PANEL_WIDTH = 100;
   private static final String[] TAX_KEYS = {"low", "usual", "high"};
   private final net.minecraft.client.gui.components.Button[] taxButtons = new net.minecraft.client.gui.components.Button[3];
   private net.minecraft.client.gui.components.Button foundButton;
   private static final int VILLAGE_PANEL_HEIGHT = 104;
   private static final int FOUND_PANEL_HEIGHT = 74;
   private static final int COUNTRY_PANEL_HEIGHT = 40;

   protected void init() {
      super.init();
      int panelX = this.panelX();
      for (int i = 0; i < 3; i++) {
         int level = i;
         this.taxButtons[i] = this.addRenderableWidget(
            net.minecraft.client.gui.components.Button.builder(
                  Component.translatable("gui.ancientwarfare.town_hall.tax." + TAX_KEYS[i]),
                  button -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, level)
               )
               .bounds(panelX + 4 + i * 31, this.topPos + 82, 30, 16)
               .build()
         );
      }

      this.foundButton = this.addRenderableWidget(
         net.minecraft.client.gui.components.Button.builder(Component.translatable("gui.ancientwarfare.country.found_button"), button -> {
               this.openingDeathRecords = true;
               this.minecraft.setScreen(new CountryNameScreen(this));
            })
            .bounds(panelX + 4, this.topPos + 52, PANEL_WIDTH - 8, 16)
            .build()
      );
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
      boolean taken = this.menu.village().get(0) == 1;
      for (int i = 0; i < 3; i++) {
         this.taxButtons[i].visible = taken;
         this.taxButtons[i].active = this.menu.village().get(3) != i;
      }

      int country = this.menu.village().get(6);
      int countryY = taken ? VILLAGE_PANEL_HEIGHT : 0;
      this.foundButton.visible = country == 1;
      this.foundButton.active = this.menu.village().get(7) >= PlayerCountry.FOUND_RESIDENTS;
      this.foundButton.setY(this.topPos + countryY + 52);
      if (taken) {
         this.renderVillagePanel(graphics);
      }

      if (country == 1) {
         this.renderFoundPanel(graphics, countryY);
      } else if (country > 1) {
         this.renderCountryPanel(graphics, countryY);
      }

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

   /** Beside the hall: how the people of a village taken from a faction take their new lord. */
   private void renderVillagePanel(GuiGraphics graphics) {
      int x = this.panelX();
      int y = this.topPos;
      graphics.fill(x, y, x + PANEL_WIDTH, y + VILLAGE_PANEL_HEIGHT, 0xE0101010);
      graphics.drawString(this.font, Component.translatable(this.menu.village().get(6) == 2 ? "gui.ancientwarfare.town_hall.capital" : this.menu.village().get(6) == 3 ? "gui.ancientwarfare.town_hall.country_village" : "gui.ancientwarfare.town_hall.village"), x + 6, y + 6, TEXT_COLOR, false);
      int loyalty = this.menu.village().get(1);
      int order = this.menu.village().get(2);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.town_hall.loyalty", loyalty), x + 6, y + 22, meterColor(loyalty), false);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.town_hall.order", order), x + 6, y + 34, meterColor(order), false);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.town_hall.guards", this.menu.village().get(4)), x + 6, y + 46, TEXT_COLOR, false);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.town_hall.income", this.menu.village().get(5)), x + 6, y + 58, TEXT_COLOR, false);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.town_hall.tax"), x + 6, y + 70, 0xFFBFBFBF, false);
   }

   /** Below the village panel: what founding a country here takes. */
   private void renderFoundPanel(GuiGraphics graphics, int top) {
      int x = this.panelX();
      int y = this.topPos + top;
      graphics.fill(x, y, x + PANEL_WIDTH, y + FOUND_PANEL_HEIGHT, 0xE0101010);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.own"), x + 6, y + 6, TEXT_COLOR, false);
      int residents = this.menu.village().get(7);
      graphics.drawString(
         this.font,
         Component.translatable("gui.ancientwarfare.country.residents", residents, PlayerCountry.FOUND_RESIDENTS),
         x + 6,
         y + 22,
         residents >= PlayerCountry.FOUND_RESIDENTS ? 0xFF80FF80 : 0xFFFF5555,
         false
      );
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.cost", PlayerCountry.FOUND_COST), x + 6, y + 34, TEXT_COLOR, false);
   }

   /** Below the village panel: the country this hall belongs to. */
   private void renderCountryPanel(GuiGraphics graphics, int top) {
      int x = this.panelX();
      int y = this.topPos + top;
      graphics.fill(x, y, x + PANEL_WIDTH, y + COUNTRY_PANEL_HEIGHT, 0xE0101010);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.provinces", this.menu.village().get(8)), x + 6, y + 6, TEXT_COLOR, false);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.income", this.menu.village().get(9)), x + 6, y + 18, TEXT_COLOR, false);
      if (this.menu.village().get(6) == 3) {
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.country.to_capital"), x + 6, y + 30, 0xFFBFBFBF, false);
      }
   }

   /** Left of the hall's window, where item lists such as JEI's leave room. */
   private int panelX() {
      return Math.max(2, this.leftPos - PANEL_WIDTH - 4);
   }

   private static int meterColor(int value) {
      return value < 25 ? 0xFFFF5555 : value < 50 ? 0xFFFFD060 : 0xFF80FF80;
   }

   private void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            graphics.blit(WIDGETS, this.leftPos + x + col * 18 - 1, this.topPos + y + row * 18 - 1, 152, 120, 18, 18);
         }
      }
   }
}
