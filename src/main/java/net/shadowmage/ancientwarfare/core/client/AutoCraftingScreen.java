package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.AutoCraftingMenu;

public class AutoCraftingScreen extends LegacyContainerScreen<AutoCraftingMenu> {
   private LegacyArrowButton previousRecipe;
   private LegacyArrowButton nextRecipe;

   public AutoCraftingScreen(AutoCraftingMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 176, 216);
   }

   @Override
   protected void init() {
      super.init();
      this.legacyButton("craft", 125, 44, 36, 12, Component.translatable("guistrings.automation.craft"), button -> {
         if (this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(((AutoCraftingMenu)this.menu).containerId, 1);
         }
      });
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

   private void clickRecipeButton(int id) {
      if (this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((AutoCraftingMenu)this.menu).containerId, id);
      }
   }

   private void updateRecipeButtons() {
      boolean visible = ((AutoCraftingMenu)this.menu).hasMultipleRecipes();
      this.previousRecipe.visible = this.previousRecipe.active = visible;
      this.nextRecipe.visible = this.nextRecipe.active = visible;
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }
}
