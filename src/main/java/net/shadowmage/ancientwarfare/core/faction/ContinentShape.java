package net.shadowmage.ancientwarfare.core.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Continentalness of the "AW3: continent" world type: the usual noise, pushed towards land, inside
 * a rough circle around the world's centre, and open sea beyond it. The noise also wobbles the
 * coastline so it is not a circle.
 */
public record ContinentShape(DensityFunction input, double radius) implements DensityFunction {
   private static final double LAND_SCALE = 0.7;
   private static final double LAND_SHIFT = 0.3;
   private static final double SEA = -0.75;
   private static final double COAST_START = 0.8;
   private static final double COAST_WIDTH = 0.35;
   private static final double COAST_WOBBLE = 0.12;
   private static final MapCodec<ContinentShape> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(ContinentShape::input),
            Codec.doubleRange(16.0, 1.0E6).fieldOf("radius").forGetter(ContinentShape::radius)
         )
         .apply(instance, ContinentShape::new)
   );
   public static final KeyDispatchDataCodec<ContinentShape> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

   @Override
   public double compute(FunctionContext context) {
      double noise = this.input.compute(context);
      double x = context.blockX();
      double z = context.blockZ();
      double distance = Math.sqrt(x * x + z * z) / this.radius + noise * COAST_WOBBLE;
      double sea = Mth.clamp((distance - COAST_START) / COAST_WIDTH, 0.0, 1.0);
      return Mth.lerp(Mth.smoothstep(sea), land(noise), SEA);
   }

   private static double land(double noise) {
      return noise * LAND_SCALE + LAND_SHIFT;
   }

   @Override
   public void fillArray(double[] array, ContextProvider provider) {
      provider.fillAllDirectly(array, this);
   }

   @Override
   public DensityFunction mapAll(Visitor visitor) {
      return visitor.apply(new ContinentShape(this.input.mapAll(visitor), this.radius));
   }

   @Override
   public double minValue() {
      return Math.min(land(this.input.minValue()), SEA);
   }

   @Override
   public double maxValue() {
      return Math.max(land(this.input.maxValue()), SEA);
   }

   @Override
   public KeyDispatchDataCodec<? extends DensityFunction> codec() {
      return CODEC;
   }
}
