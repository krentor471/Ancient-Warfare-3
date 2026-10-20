package net.shadowmage.ancientwarfare.core.common.engineering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public final class CraftingRecipeSelection {
   private CraftingRecipeSelection() {
   }

   public static List<Recipe<CraftingContainer>> matching(Level level, CraftingContainer crafting, Set<String> completedResearch) {
      List<Recipe<CraftingContainer>> recipes = new ArrayList<>();
      RecipeType<EngineeringRecipe> engineeringType = AWCoreRegistryAccess.recipeType(AWCoreIds.id("engineering"));

      for (EngineeringRecipe recipe : level.getRecipeManager().getRecipesFor(engineeringType, crafting, level)) {
         String required = recipe.requiredResearch();
         if (required == null || EngineeringRecipeLogic.hasCompletedResearch(completedResearch, required)) {
            recipes.add(recipe);
         }
      }

      recipes.addAll(level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, crafting, level));
      recipes.sort(Comparator.comparing(recipex -> recipex.getId().toString()));
      return recipes;
   }

   @Nullable
   public static Recipe<CraftingContainer> select(List<Recipe<CraftingContainer>> recipes, @Nullable ResourceLocation selectedId) {
      if (recipes.isEmpty()) {
         return null;
      } else {
         if (selectedId != null) {
            for (Recipe<CraftingContainer> recipe : recipes) {
               if (recipe.getId().equals(selectedId)) {
                  return recipe;
               }
            }
         }

         return recipes.get(0);
      }
   }

   @Nullable
   public static ResourceLocation cycle(List<Recipe<CraftingContainer>> recipes, @Nullable ResourceLocation selectedId, int direction) {
      if (recipes.isEmpty()) {
         return null;
      } else {
         int current = 0;
         if (selectedId != null) {
            for (int index = 0; index < recipes.size(); index++) {
               if (recipes.get(index).getId().equals(selectedId)) {
                  current = index;
                  break;
               }
            }
         }

         return recipes.get(Math.floorMod(current + direction, recipes.size())).getId();
      }
   }

   public static ItemStack result(@Nullable Recipe<CraftingContainer> recipe, CraftingContainer crafting, Level level) {
      return recipe == null ? ItemStack.EMPTY : recipe.assemble(crafting, level.registryAccess());
   }

   @Nullable
   public static Recipe<CraftingContainer> normalize(List<Recipe<CraftingContainer>> recipes, CraftingRecipeMemory memory) {
      Recipe<CraftingContainer> selected = select(recipes, memory.getSelectedRecipeId());
      ResourceLocation normalizedId = selected == null ? null : selected.getId();
      if (!Objects.equals(normalizedId, memory.getSelectedRecipeId())) {
         memory.setSelectedRecipeId(normalizedId);
      }

      return selected;
   }
}
