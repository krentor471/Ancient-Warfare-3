package net.shadowmage.ancientwarfare.core.common.engineering;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public interface CraftingRecipeMemory {
   @Nullable
   ResourceLocation getSelectedRecipeId();

   void setSelectedRecipeId(@Nullable ResourceLocation var1);
}
