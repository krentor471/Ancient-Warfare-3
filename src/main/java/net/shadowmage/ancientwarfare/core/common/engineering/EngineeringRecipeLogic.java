package net.shadowmage.ancientwarfare.core.common.engineering;

import java.util.Set;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public final class EngineeringRecipeLogic {
   private EngineeringRecipeLogic() {
   }

   public static EngineeringRecipeMatch match(ItemStack[] grid) {
      return EngineeringRecipeRegistry.match(grid);
   }

   public static EngineeringRecipeMatch match(Level level, CraftingContainer crafting) {
      if (level == null) {
         return EngineeringRecipeMatch.NONE;
      } else {
         RecipeType<EngineeringRecipe> type = AWCoreRegistryAccess.recipeType(AWCoreIds.id("engineering"));
         return level.getRecipeManager().getRecipeFor(type, crafting, level).map(EngineeringRecipeMatch::recipe).orElse(EngineeringRecipeMatch.NONE);
      }
   }

   public static boolean hasCompletedResearch(Set<String> completed, String required) {
      if (!completed.contains(required) && !completed.contains("ancientwarfare:" + required)) {
         for (String id : completed) {
            if (id.equals(required) || id.endsWith(":" + required)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }
}
