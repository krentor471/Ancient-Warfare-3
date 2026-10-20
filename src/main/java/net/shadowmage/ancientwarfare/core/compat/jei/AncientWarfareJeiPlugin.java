package net.shadowmage.ancientwarfare.core.compat.jei;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipeRegistry;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreMenuTypes;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreRecipeTypes;
import net.shadowmage.ancientwarfare.core.menu.AutoCraftingMenu;
import net.shadowmage.ancientwarfare.core.menu.EngineeringStationMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseCraftingStationMenu;

@JeiPlugin
public final class AncientWarfareJeiPlugin implements IModPlugin {
   private static final ResourceLocation UID = AWCoreIds.id("jei_plugin");

   public ResourceLocation getPluginUid() {
      return UID;
   }

   public void registerCategories(IRecipeCategoryRegistration registration) {
      registration.addRecipeCategories(new IRecipeCategory[]{new EngineeringRecipeCategory(registration.getJeiHelpers().getGuiHelper())});
   }

   public void registerRecipes(IRecipeRegistration registration) {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      List<EngineeringRecipe> recipes = connection == null
         ? EngineeringRecipeRegistry.all()
         : connection.getRecipeManager().getAllRecipesFor((RecipeType)AWCoreRecipeTypes.ENGINEERING.get());
      registration.addRecipes(EngineeringRecipeCategory.TYPE, recipes);
   }

   public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
      registration.addRecipeCatalysts(
         EngineeringRecipeCategory.TYPE,
         new ItemLike[]{
            AWCoreRegistryAccess.block(AWCoreIds.ENGINEERING_STATION),
            AWCoreRegistryAccess.block(AWCoreIds.AUTO_CRAFTING),
            AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_CRAFTING)
         }
      );
      registration.addRecipeCatalysts(
         RecipeTypes.CRAFTING,
         new ItemLike[]{
            AWCoreRegistryAccess.block(AWCoreIds.ENGINEERING_STATION),
            AWCoreRegistryAccess.block(AWCoreIds.AUTO_CRAFTING),
            AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_CRAFTING)
         }
      );
   }

   public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
      IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
      registration.addRecipeTransferHandler(new EngineeringRecipeTransferHandler(helper, EngineeringStationMenu.class), EngineeringRecipeCategory.TYPE);
      registration.addRecipeTransferHandler(new EngineeringRecipeTransferHandler(helper, AutoCraftingMenu.class), EngineeringRecipeCategory.TYPE);
      registration.addRecipeTransferHandler(new EngineeringRecipeTransferHandler(helper, WarehouseCraftingStationMenu.class), EngineeringRecipeCategory.TYPE);
      registration.addRecipeTransferHandler(
         EngineeringStationMenu.class, (MenuType)AWCoreMenuTypes.ENGINEERING_STATION.get(), RecipeTypes.CRAFTING, 1, 9, 11, 54
      );
      registration.addRecipeTransferHandler(AutoCraftingMenu.class, (MenuType)AWCoreMenuTypes.AUTO_CRAFTING.get(), RecipeTypes.CRAFTING, 2, 9, 20, 54);
      registration.addRecipeTransferHandler(
         WarehouseCraftingStationMenu.class, (MenuType)AWCoreMenuTypes.WAREHOUSE_CRAFTING.get(), RecipeTypes.CRAFTING, 2, 9, 20, 36
      );
   }
}
