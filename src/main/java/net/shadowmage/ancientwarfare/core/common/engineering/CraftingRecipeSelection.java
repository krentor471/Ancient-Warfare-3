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

      for (EngineeringRecipe recipe : level.m_7465_().m_44056_(engineeringType, crafting, level)) {
         String required = recipe.requiredResearch();
         if (required == null || EngineeringRecipeLogic.hasCompletedResearch(completedResearch, required)) {
            recipes.add(recipe);
         }
      }

      recipes.addAll(level.m_7465_().m_44056_(RecipeType.f_44107_, crafting, level));
      recipes.sort(Comparator.comparing(recipex -> recipex.m_6423_().toString()));
      return recipes;
   }

   @Nullable
   public static Recipe<CraftingContainer> select(List<Recipe<CraftingContainer>> recipes, @Nullable ResourceLocation selectedId) {
      if (recipes.isEmpty()) {
         return null;
      } else {
         if (selectedId != null) {
            for (Recipe<CraftingContainer> recipe : recipes) {
               if (recipe.m_6423_().equals(selectedId)) {
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
               if (recipes.get(index).m_6423_().equals(selectedId)) {
                  current = index;
                  break;
               }
            }
         }

         return recipes.get(Math.floorMod(current + direction, recipes.size())).m_6423_();
      }
   }

   public static ItemStack result(@Nullable Recipe<CraftingContainer> recipe, CraftingContainer crafting, Level level) {
      return recipe == null ? ItemStack.f_41583_ : recipe.m_5874_(crafting, level.m_9598_());
   }

   @Nullable
   public static Recipe<CraftingContainer> normalize(List<Recipe<CraftingContainer>> recipes, CraftingRecipeMemory memory) {
      Recipe<CraftingContainer> selected = select(recipes, memory.getSelectedRecipeId());
      ResourceLocation normalizedId = selected == null ? null : selected.m_6423_();
      if (!Objects.equals(normalizedId, memory.getSelectedRecipeId())) {
         memory.setSelectedRecipeId(normalizedId);
      }

      return selected;
   }
}
