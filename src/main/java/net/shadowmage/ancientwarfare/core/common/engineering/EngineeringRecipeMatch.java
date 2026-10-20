package net.shadowmage.ancientwarfare.core.common.engineering;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

public record EngineeringRecipeMatch(@Nullable EngineeringRecipe recipe) {
   public static final EngineeringRecipeMatch NONE = new EngineeringRecipeMatch(null);

   public static EngineeringRecipeMatch recipe(EngineeringRecipe recipe) {
      return new EngineeringRecipeMatch(recipe);
   }

   public boolean isPresent() {
      return this.recipe != null;
   }

   public ItemStack result() {
      return this.recipe == null ? ItemStack.EMPTY : this.recipe.assemble();
   }

   @Nullable
   public String requiredResearch() {
      return this.recipe == null ? null : this.recipe.requiredResearch();
   }
}
