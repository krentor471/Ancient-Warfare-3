package net.shadowmage.ancientwarfare.core.forge.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.core.faction.ContinentShape;

public final class AWCoreWorldgen {
   public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTIONS = DeferredRegister.create(
      Registries.DENSITY_FUNCTION_TYPE, "ancientwarfare"
   );

   static {
      DENSITY_FUNCTIONS.register("continent", ContinentShape.CODEC::codec);
   }

   private AWCoreWorldgen() {
   }
}
