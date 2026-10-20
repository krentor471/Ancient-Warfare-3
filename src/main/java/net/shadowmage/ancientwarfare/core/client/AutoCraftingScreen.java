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
   protected void m_7856_() {
      super.m_7856_();
      this.legacyButton("craft", 125, 44, 36, 12, Component.m_237115_("guistrings.automation.craft"), button -> {
         if (this.f_96541_.f_91072_ != null) {
            this.f_96541_.f_91072_.m_105208_(((AutoCraftingMenu)this.f_97732_).f_38840_, 1);
         }
      });
      this.previousRecipe = this.addLegacyWidget(
         "previous recipe", new LegacyArrowButton(this.f_97735_ + 122, this.f_97736_ + 26, false, button -> this.clickRecipeButton(1000)), 122, 26, 10, 16
      );
      this.nextRecipe = this.addLegacyWidget(
         "next recipe", new LegacyArrowButton(this.f_97735_ + 152, this.f_97736_ + 26, true, button -> this.clickRecipeButton(1001)), 152, 26, 10, 16
      );
      this.updateRecipeButtons();
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.m_280480_(new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.RESEARCH_BOOK)), this.f_97735_ + 8, this.f_97736_ + 8);
      this.updateRecipeButtons();
   }

   private void clickRecipeButton(int id) {
      if (this.f_96541_.f_91072_ != null) {
         this.f_96541_.f_91072_.m_105208_(((AutoCraftingMenu)this.f_97732_).f_38840_, id);
      }
   }

   private void updateRecipeButtons() {
      boolean visible = ((AutoCraftingMenu)this.f_97732_).hasMultipleRecipes();
      this.previousRecipe.f_93624_ = this.previousRecipe.f_93623_ = visible;
      this.nextRecipe.f_93624_ = this.nextRecipe.f_93623_ = visible;
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
   }
}
