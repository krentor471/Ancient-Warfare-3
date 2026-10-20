package net.shadowmage.ancientwarfare.core.common.engineering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class EngineeringRecipeRegistry {
   private static List<EngineeringRecipe> recipes = List.of();

   private EngineeringRecipeRegistry() {
   }

   public static void setRecipes(List<EngineeringRecipe> loadedRecipes) {
      List<EngineeringRecipe> sorted = new ArrayList<>(loadedRecipes);
      sorted.sort(Comparator.comparing(recipe -> recipe.id().toString()));
      recipes = List.copyOf(sorted);
   }

   public static List<EngineeringRecipe> all() {
      return recipes;
   }

   public static EngineeringRecipeMatch match(ItemStack[] grid) {
      for (EngineeringRecipe recipe : recipes) {
         if (recipe.matches(grid)) {
            return EngineeringRecipeMatch.recipe(recipe);
         }
      }

      return EngineeringRecipeMatch.NONE;
   }

   public static List<EngineeringRecipe> recipesForResearch(String researchId) {
      return researchId == null ? List.of() : recipes.stream().filter(recipe -> researchId.equals(recipe.requiredResearch())).toList();
   }
}
