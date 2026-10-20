package net.shadowmage.ancientwarfare.core.forge.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;

public final class AWCoreRecipeTypes {
   public static final DeferredRegister<RecipeType<?>> REGISTER = DeferredRegister.create(Registries.RECIPE_TYPE, "ancientwarfare");
   public static final RegistryObject<RecipeType<EngineeringRecipe>> ENGINEERING = REGISTER.register("engineering", () -> new RecipeType<EngineeringRecipe>() {
         @Override
         public String toString() {
            return "ancientwarfare:engineering";
         }
      });

   private AWCoreRecipeTypes() {
   }
}
