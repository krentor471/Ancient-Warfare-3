package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.EngineeringStationMenu;

public class EngineeringStationScreen extends LegacyContainerScreen<EngineeringStationMenu> {
   private static final int TEXT_COLOR = 16777215;
   private LegacyArrowButton previousRecipe;
   private LegacyArrowButton nextRecipe;

   public EngineeringStationScreen(EngineeringStationMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 176, 192);
   }

   @Override
   protected void init() {
      super.init();
      this.previousRecipe = this.addLegacyWidget(
         "previous recipe", new LegacyArrowButton(this.leftPos + 122, this.topPos + 26, false, button -> this.clickRecipeButton(1000)), 122, 26, 10, 16
      );
      this.nextRecipe = this.addLegacyWidget(
         "next recipe", new LegacyArrowButton(this.leftPos + 152, this.topPos + 26, true, button -> this.clickRecipeButton(1001)), 152, 26, 10, 16
      );
      this.updateRecipeButtons();
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.renderItem(new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.RESEARCH_BOOK)), this.leftPos + 8, this.topPos + 8);
      this.updateRecipeButtons();
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, this.playerInventoryTitle, 8, 100, 16777215, false);
      String locked = ((EngineeringStationMenu)this.menu).getLockedResearchName();
      if (!locked.isEmpty()) {
         graphics.drawString(this.font, Component.translatable("guistrings.research.requires", new Object[]{locked}), 8, 104, 16777215, false);
      }
   }

   private void clickRecipeButton(int id) {
      if (this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((EngineeringStationMenu)this.menu).containerId, id);
      }
   }

   private void updateRecipeButtons() {
      boolean visible = ((EngineeringStationMenu)this.menu).hasMultipleRecipes();
      this.previousRecipe.visible = this.previousRecipe.active = visible;
      this.nextRecipe.visible = this.nextRecipe.active = visible;
   }
}
