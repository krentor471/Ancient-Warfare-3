package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.WarehouseCraftingStationMenu;

public class WarehouseCraftingStationScreen extends LegacyContainerScreen<WarehouseCraftingStationMenu> {
   private static final int TEXT_COLOR = 16777215;
   private LegacyArrowButton previousRecipe;
   private LegacyArrowButton nextRecipe;
   private LegacyCheckbox autoStore;
   private LegacyTexturedButton connection;

   public WarehouseCraftingStationScreen(WarehouseCraftingStationMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 176, 190);
   }

   @Override
   protected void init() {
      super.init();
      this.legacyButton("configure sides", 8, 50, 38, 12, Component.translatable("guistrings.automation.sides"), button -> {
         if (this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(((WarehouseCraftingStationMenu)this.menu).containerId, 0);
         }
      });
      this.previousRecipe = this.addLegacyWidget(
         "previous recipe", new LegacyArrowButton(this.leftPos + 122, this.topPos + 26, false, button -> this.clickRecipeButton(1)), 122, 26, 10, 16
      );
      this.nextRecipe = this.addLegacyWidget(
         "next recipe", new LegacyArrowButton(this.leftPos + 152, this.topPos + 26, true, button -> this.clickRecipeButton(2)), 152, 26, 10, 16
      );
      this.autoStore = this.addLegacyWidget(
         "auto store output",
         new LegacyCheckbox(
            this.leftPos + 8,
            this.topPos + 92,
            10,
            Component.literal(this.font.plainSubstrByWidth(Component.translatable("guistrings.warehouse.auto_store").getString(), 124)),
            ((WarehouseCraftingStationMenu)this.menu).isAutoStoreOutput(),
            checked -> this.clickRecipeButton(3)
         ),
         8,
         92,
         10,
         10
      );
      this.autoStore.setTooltip(Tooltip.create(Component.translatable("guistrings.warehouse.auto_store")));
      this.connection = this.legacyButton("warehouse connection", 148, 92, 20, 10, Component.literal(">"), button -> this.clickRecipeButton(4));
      this.updateRecipeButtons();
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.updateRecipeButtons();
      this.autoStore.setChecked(((WarehouseCraftingStationMenu)this.menu).isAutoStoreOutput());
      this.connection
         .setTooltip(
            Tooltip.create(
               Component.translatable("guistrings.warehouse.connection_cycle")
                  .append("\n")
                  .append(((WarehouseCraftingStationMenu)this.menu).getConnectionDescription())
                  .append("\n")
                  .append(
                     Component.translatable(
                        "guistrings.automation.worksite_status",
                        new Object[]{
                           Component.translatable("guistrings.warehouse.crafting_work"),
                           ((WarehouseCraftingStationMenu)this.menu).getStoredEnergy(),
                           ((WarehouseCraftingStationMenu)this.menu).getMaxEnergy()
                        }
                     )
                  )
            )
         );
      graphics.renderItem(new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.RESEARCH_BOOK)), this.leftPos + 8, this.topPos + 8);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, Component.translatable("guistrings.automation.output"), 8, 64, 16777215, false);
      Component state = Component.translatable(
         ((WarehouseCraftingStationMenu)this.menu).hasWarehouse()
            ? "guistrings.warehouse.connected"
            : (
               ((WarehouseCraftingStationMenu)this.menu).getConnectionStatus() == 2
                  ? "guistrings.warehouse.offline"
                  : "guistrings.warehouse.connection_missing"
            )
      );
      String label = this.font.plainSubstrByWidth(state.getString(), 118);
      graphics.drawString(this.font, label, 50, 64, ((WarehouseCraftingStationMenu)this.menu).hasWarehouse() ? 16777215 : 16724787, false);
   }

   private void clickRecipeButton(int id) {
      if (this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((WarehouseCraftingStationMenu)this.menu).containerId, id);
      }
   }

   private void updateRecipeButtons() {
      boolean visible = ((WarehouseCraftingStationMenu)this.menu).hasMultipleRecipes();
      this.previousRecipe.visible = this.previousRecipe.active = visible;
      this.nextRecipe.visible = this.nextRecipe.active = visible;
   }
}
