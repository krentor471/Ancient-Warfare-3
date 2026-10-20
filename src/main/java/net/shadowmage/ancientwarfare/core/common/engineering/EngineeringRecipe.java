package net.shadowmage.ancientwarfare.core.common.engineering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public record EngineeringRecipe(ResourceLocation id, EngineeringIngredient[] ingredients, boolean shapeless, ItemStack result, @Nullable String requiredResearch)
   implements Recipe<CraftingContainer> {
   public boolean matches(ItemStack[] grid) {
      return this.consumptionCounts(grid) != null;
   }

   @Nullable
   public int[] consumptionCounts(ItemStack[] grid) {
      if (grid.length == 9 && this.ingredients.length == 9) {
         if (this.shapeless) {
            return this.shapelessConsumption(grid);
         } else {
            int[] counts = new int[9];

            for (int slot = 0; slot < this.ingredients.length; slot++) {
               EngineeringIngredient ingredient = this.ingredients[slot];
               ItemStack stack = grid[slot];
               if (ingredient.isEmpty()) {
                  if (!stack.m_41619_()) {
                     return null;
                  }
               } else {
                  if (!ingredient.test(stack)) {
                     return null;
                  }

                  counts[slot] = ingredient.count();
               }
            }

            return counts;
         }
      } else {
         return null;
      }
   }

   @Nullable
   private int[] shapelessConsumption(ItemStack[] grid) {
      List<Integer> occupied = new ArrayList<>();

      for (int slot = 0; slot < grid.length; slot++) {
         if (!grid[slot].m_41619_()) {
            occupied.add(slot);
         }
      }

      List<EngineeringIngredient> required = Arrays.stream(this.ingredients).filter(ingredient -> !ingredient.isEmpty()).toList();
      if (occupied.size() != required.size()) {
         return null;
      } else {
         int[] counts = new int[9];
         return this.assignShapeless(0, required, occupied, new boolean[9], grid, counts) ? counts : null;
      }
   }

   private boolean assignShapeless(int index, List<EngineeringIngredient> required, List<Integer> occupied, boolean[] used, ItemStack[] grid, int[] counts) {
      if (index >= required.size()) {
         return true;
      } else {
         EngineeringIngredient ingredient = required.get(index);

         for (int slot : occupied) {
            if (!used[slot] && ingredient.test(grid[slot])) {
               used[slot] = true;
               counts[slot] = ingredient.count();
               if (this.assignShapeless(index + 1, required, occupied, used, grid, counts)) {
                  return true;
               }

               counts[slot] = 0;
               used[slot] = false;
            }
         }

         return false;
      }
   }

   public ItemStack assemble() {
      return this.result.m_41777_();
   }

   public boolean matches(CraftingContainer container, Level level) {
      if (container.m_39347_() == 3 && container.m_39346_() == 3) {
         ItemStack[] grid = new ItemStack[9];

         for (int slot = 0; slot < grid.length; slot++) {
            grid[slot] = container.m_8020_(slot);
         }

         return this.matches(grid);
      } else {
         return false;
      }
   }

   public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
      return this.assemble();
   }

   public boolean m_8004_(int width, int height) {
      return width >= 3 && height >= 3;
   }

   public ItemStack m_8043_(RegistryAccess registryAccess) {
      return this.result.m_41777_();
   }

   public NonNullList<Ingredient> m_7527_() {
      NonNullList<Ingredient> result = NonNullList.m_122780_(this.ingredients.length, Ingredient.f_43901_);

      for (int slot = 0; slot < this.ingredients.length; slot++) {
         result.set(slot, this.ingredients[slot].ingredient());
      }

      return result;
   }

   public ResourceLocation m_6423_() {
      return this.id;
   }

   public RecipeSerializer<?> m_7707_() {
      return AWCoreRegistryAccess.recipeSerializer(AWCoreIds.id("engineering"));
   }

   public RecipeType<?> m_6671_() {
      return AWCoreRegistryAccess.recipeType(AWCoreIds.id("engineering"));
   }

   public boolean m_5598_() {
      return true;
   }
}
