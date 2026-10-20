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
   protected void m_7856_() {
      super.m_7856_();
      this.legacyButton("configure sides", 8, 50, 38, 12, Component.m_237115_("guistrings.automation.sides"), button -> {
         if (this.f_96541_.f_91072_ != null) {
            this.f_96541_.f_91072_.m_105208_(((WarehouseCraftingStationMenu)this.f_97732_).f_38840_, 0);
         }
      });
      this.previousRecipe = this.addLegacyWidget(
         "previous recipe", new LegacyArrowButton(this.f_97735_ + 122, this.f_97736_ + 26, false, button -> this.clickRecipeButton(1)), 122, 26, 10, 16
      );
      this.nextRecipe = this.addLegacyWidget(
         "next recipe", new LegacyArrowButton(this.f_97735_ + 152, this.f_97736_ + 26, true, button -> this.clickRecipeButton(2)), 152, 26, 10, 16
      );
      this.autoStore = this.addLegacyWidget(
         "auto store output",
         new LegacyCheckbox(
            this.f_97735_ + 8,
            this.f_97736_ + 92,
            10,
            Component.m_237113_(this.f_96547_.m_92834_(Component.m_237115_("guistrings.warehouse.auto_store").getString(), 124)),
            ((WarehouseCraftingStationMenu)this.f_97732_).isAutoStoreOutput(),
            checked -> this.clickRecipeButton(3)
         ),
         8,
         92,
         10,
         10
      );
      this.autoStore.m_257544_(Tooltip.m_257550_(Component.m_237115_("guistrings.warehouse.auto_store")));
      this.connection = this.legacyButton("warehouse connection", 148, 92, 20, 10, Component.m_237113_(">"), button -> this.clickRecipeButton(4));
      this.updateRecipeButtons();
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.updateRecipeButtons();
      this.autoStore.setChecked(((WarehouseCraftingStationMenu)this.f_97732_).isAutoStoreOutput());
      this.connection
         .m_257544_(
            Tooltip.m_257550_(
               Component.m_237115_("guistrings.warehouse.connection_cycle")
                  .m_130946_("\n")
                  .m_7220_(((WarehouseCraftingStationMenu)this.f_97732_).getConnectionDescription())
                  .m_130946_("\n")
                  .m_7220_(
                     Component.m_237110_(
                        "guistrings.automation.worksite_status",
                        new Object[]{
                           Component.m_237115_("guistrings.warehouse.crafting_work"),
                           ((WarehouseCraftingStationMenu)this.f_97732_).getStoredEnergy(),
                           ((WarehouseCraftingStationMenu)this.f_97732_).getMaxEnergy()
                        }
                     )
                  )
            )
         );
      graphics.m_280480_(new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.RESEARCH_BOOK)), this.f_97735_ + 8, this.f_97736_ + 8);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.automation.output"), 8, 64, 16777215, false);
      Component state = Component.m_237115_(
         ((WarehouseCraftingStationMenu)this.f_97732_).hasWarehouse()
            ? "guistrings.warehouse.connected"
            : (
               ((WarehouseCraftingStationMenu)this.f_97732_).getConnectionStatus() == 2
                  ? "guistrings.warehouse.offline"
                  : "guistrings.warehouse.connection_missing"
            )
      );
      String label = this.f_96547_.m_92834_(state.getString(), 118);
      graphics.m_280056_(this.f_96547_, label, 50, 64, ((WarehouseCraftingStationMenu)this.f_97732_).hasWarehouse() ? 16777215 : 16724787, false);
   }

   private void clickRecipeButton(int id) {
      if (this.f_96541_.f_91072_ != null) {
         this.f_96541_.f_91072_.m_105208_(((WarehouseCraftingStationMenu)this.f_97732_).f_38840_, id);
      }
   }

   private void updateRecipeButtons() {
      boolean visible = ((WarehouseCraftingStationMenu)this.f_97732_).hasMultipleRecipes();
      this.previousRecipe.f_93624_ = this.previousRecipe.f_93623_ = visible;
      this.nextRecipe.f_93624_ = this.nextRecipe.f_93623_ = visible;
   }
}
