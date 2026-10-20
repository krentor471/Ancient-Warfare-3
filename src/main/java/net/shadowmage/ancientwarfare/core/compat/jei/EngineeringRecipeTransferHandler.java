package net.shadowmage.ancientwarfare.core.compat.jei;

import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;
import net.shadowmage.ancientwarfare.core.menu.AutoCraftingMenu;
import net.shadowmage.ancientwarfare.core.menu.EngineeringStationMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseCraftingStationMenu;

public final class EngineeringRecipeTransferHandler implements IRecipeTransferHandler<AbstractContainerMenu, EngineeringRecipe> {
   private final IRecipeTransferHandlerHelper helper;
   private final Class<? extends AbstractContainerMenu> containerClass;

   public EngineeringRecipeTransferHandler(IRecipeTransferHandlerHelper helper, Class<? extends AbstractContainerMenu> containerClass) {
      this.helper = helper;
      this.containerClass = containerClass;
   }

   public Class<? extends AbstractContainerMenu> getContainerClass() {
      return this.containerClass;
   }

   public Optional<MenuType<AbstractContainerMenu>> getMenuType() {
      return Optional.empty();
   }

   public RecipeType<EngineeringRecipe> getRecipeType() {
      return EngineeringRecipeCategory.TYPE;
   }

   public IRecipeTransferError transferRecipe(
      AbstractContainerMenu container, EngineeringRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer
   ) {
      if (!(container instanceof EngineeringStationMenu) && !(container instanceof AutoCraftingMenu) && !(container instanceof WarehouseCraftingStationMenu)) {
         return this.helper.createUserErrorWithTooltip(Component.translatable("jei.ancientwarfare.invalid_container"));
      } else {
         if (doTransfer) {
            AWCoreNetwork.sendTransferEngineeringRecipe(recipe.id());
         }

         return null;
      }
   }
}
