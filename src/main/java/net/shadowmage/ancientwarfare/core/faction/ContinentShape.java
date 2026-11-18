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
   private static final int ISLANDS = 9;
   /** How far out the isles lie, and how big they are, as parts of the continent's radius. */
   private static final double ISLAND_RING = 1.32;
   private static final double ISLAND_SIZE = 0.035;
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
      double value = Mth.lerp(Mth.smoothstep(sea), land(noise), SEA);
      return sea > 0.0 ? Math.max(value, island(x, z, noise)) : value;
   }

   /** Isles in a ring off the coast, where pirates make their dens: land at an isle's heart, sea at its edge. */
   private double island(double x, double z, double noise) {
      double best = SEA;
      for (int i = 0; i < ISLANDS; i++) {
         // Fixed for the world shape, spread round the ring with an uneven step so they do not look laid out.
         double angle = Math.PI * 2.0 * i / ISLANDS + Math.sin(i * 12.9898) * 0.35;
         double ring = this.radius * (ISLAND_RING + 0.08 * Math.sin(i * 78.233));
         double cx = Math.cos(angle) * ring;
         double cz = Math.sin(angle) * ring;
         double size = this.radius * ISLAND_SIZE * (0.7 + 0.6 * Math.abs(Math.sin(i * 3.7)));
         double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)) / size + noise * 0.25;
         if (d < 1.0) {
            best = Math.max(best, Mth.lerp(Mth.smoothstep(d), 0.25, SEA));
         }
      }

      return best;
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
