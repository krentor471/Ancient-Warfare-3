package net.shadowmage.ancientwarfare.core.forge.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipeSerializer;
import net.shadowmage.ancientwarfare.core.crafting.OrderCopyingRecipe;

public final class AWCoreRecipeSerializers {
   public static final DeferredRegister<RecipeSerializer<?>> REGISTER = DeferredRegister.create(Registries.f_256764_, "ancientwarfare");
   public static final RegistryObject<RecipeSerializer<OrderCopyingRecipe>> ORDER_COPYING = REGISTER.register(
      "order_copying", () -> new SimpleCraftingRecipeSerializer(OrderCopyingRecipe::new)
   );
   public static final RegistryObject<RecipeSerializer<EngineeringRecipe>> ENGINEERING = REGISTER.register("engineering", EngineeringRecipeSerializer::new);

   private AWCoreRecipeSerializers() {
   }
}
