package net.shadowmage.ancientwarfare.core.faction;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

/**
 * Winter cools every mild biome below the snow line, on the server and on each client alike, so
 * what falls from the sky in winter is snow, not rain, and vanilla lets it settle and freezes the
 * water; the biomes get their own climate back when winter ends. Dry biomes (deserts) stay dry.
 */
public final class SeasonClimate {
   private static final float WINTER_TEMPERATURE = 0.05F;
   private static final Map<Biome, net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo> ORIGINAL = new IdentityHashMap<>();
   private static Field infoField;
   private static Field cacheField;
   private static sun.misc.Unsafe unsafe;
   private static boolean broken;

   private SeasonClimate() {
   }

   /** The biome's own climate, whatever the season has done to it. */
   public static synchronized boolean mild(Biome biome, net.minecraft.core.BlockPos pos) {
      net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo original = ORIGINAL.get(biome);
      if (original == null) {
         return biome.warmEnoughToRain(pos);
      }
      return original.climateSettings().hasPrecipitation() && original.climateSettings().temperature() >= 0.15F;
   }

   public static synchronized void apply(RegistryAccess access, boolean winter) {
      if (broken || access == null) {
         return;
      }
      try {
         init();
         Registry<Biome> biomes = access.registryOrThrow(Registries.BIOME);
         for (Biome biome : biomes) {
            net.minecraftforge.common.world.ModifiableBiomeInfo modifiable = biome.modifiableBiomeInfo();
            net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo current = modifiable.get();
            net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo original = ORIGINAL.computeIfAbsent(biome, b -> current);
            net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo wanted = original;
            Biome.ClimateSettings climate = original.climateSettings();
            if (winter && climate.hasPrecipitation() && climate.temperature() >= 0.15F) {
               wanted = new net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo(
                  new Biome.ClimateSettings(true, WINTER_TEMPERATURE, climate.temperatureModifier(), climate.downfall()),
                  original.effects(), original.generationSettings(), original.mobSpawnSettings());
            }
            if (current != wanted) {
               infoField.set(modifiable, wanted);
               if (cacheField != null) {
                  unsafe.putObject(biome, unsafe.objectFieldOffset(cacheField), ThreadLocal.withInitial(() -> {
                     it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap map = new it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap(1024, 0.25F) {
                        @Override
                        protected void rehash(int size) {
                        }
                     };
                     map.defaultReturnValue(Float.NaN);
                     return map;
                  }));
               }
            }
         }
      } catch (Throwable e) {
         broken = true;
         com.mojang.logging.LogUtils.getLogger().warn("Seasonal snow is off: {}", e.toString());
      }
   }

   private static void init() throws ReflectiveOperationException {
      if (infoField != null) {
         return;
      }
      Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
      for (Field field : Biome.class.getDeclaredFields()) {
         if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.getType() == ThreadLocal.class) {
            cacheField = field;
         }
      }
      Field found = null;
      for (Field field : net.minecraftforge.common.world.ModifiableBiomeInfo.class.getDeclaredFields()) {
         if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && !java.lang.reflect.Modifier.isFinal(field.getModifiers())
            && field.getType() == net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo.class) {
            found = field;
         }
      }
      if (found == null) {
         throw new NoSuchFieldException("modifiedBiomeInfo");
      }
      found.setAccessible(true);
      infoField = found;
   }
}
