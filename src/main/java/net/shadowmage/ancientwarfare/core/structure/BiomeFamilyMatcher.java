package net.shadowmage.ancientwarfare.core.structure;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformTagAccess;

public final class BiomeFamilyMatcher {
   private BiomeFamilyMatcher() {
   }

   public static boolean matchesAny(Holder<Biome> biome, Set<String> configuredBiomeIds) {
      if (biome != null && configuredBiomeIds != null && !configuredBiomeIds.isEmpty()) {
         EnumSet<BiomeFamilyMatcher.Family> actual = familiesOf(biome);
         if (actual.isEmpty()) {
            return false;
         } else {
            for (String configuredId : configuredBiomeIds) {
               EnumSet<BiomeFamilyMatcher.Family> required = familiesOf(configuredId);
               if (!required.isEmpty() && actual.containsAll(required)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static EnumSet<BiomeFamilyMatcher.Family> familiesOf(Holder<Biome> biome) {
      EnumSet<BiomeFamilyMatcher.Family> families = EnumSet.noneOf(BiomeFamilyMatcher.Family.class);
      ResourceLocation key = biome.m_203543_().map(value -> value.m_135782_()).orElse(null);
      String path = key == null ? "" : key.m_135815_().toLowerCase(Locale.ROOT);
      if (biome.m_203656_(BiomeTags.f_207602_) || contains(path, "deep_ocean", "deep_frozen_ocean")) {
         families.add(BiomeFamilyMatcher.Family.DEEP_OCEAN);
      } else if (biome.m_203656_(BiomeTags.f_207603_) || oceanName(path)) {
         families.add(BiomeFamilyMatcher.Family.OCEAN);
      }

      if (biome.m_203656_(BiomeTags.f_207604_) || contains(path, "beach", "shore", "coast")) {
         families.add(BiomeFamilyMatcher.Family.BEACH);
      }

      if (biome.m_203656_(BiomeTags.f_207605_) || contains(path, "river", "stream")) {
         families.add(BiomeFamilyMatcher.Family.RIVER);
      }

      if (biome.m_203656_(BiomeTags.f_207607_) || contains(path, "badlands", "mesa")) {
         families.add(BiomeFamilyMatcher.Family.BADLANDS);
      }

      if (biome.m_203656_(BiomeTags.f_207606_)
         || AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.MOUNTAIN)
         || AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.PEAK)
         || AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.SLOPE)
         || contains(path, "mountain", "peak", "cliff", "highland", "alps", "plateau")) {
         families.add(BiomeFamilyMatcher.Family.MOUNTAIN);
      }

      boolean birch = contains(path, "birch");
      if (birch) {
         families.add(BiomeFamilyMatcher.Family.BIRCH_FOREST);
      } else if (biome.m_203656_(BiomeTags.f_207609_)
         || AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.CONIFEROUS)
         || contains(path, "taiga", "conifer", "redwood", "spruce", "pine")) {
         families.add(BiomeFamilyMatcher.Family.TAIGA);
      } else if (biome.m_203656_(BiomeTags.f_207610_) || contains(path, "jungle", "rainforest", "bamboo")) {
         families.add(BiomeFamilyMatcher.Family.JUNGLE);
      } else if (biome.m_203656_(BiomeTags.f_207611_) || contains(path, "forest", "woods", "woodland", "grove", "orchard")) {
         families.add(BiomeFamilyMatcher.Family.FOREST);
      }

      if (biome.m_203656_(BiomeTags.f_215816_) || contains(path, "savanna")) {
         families.add(BiomeFamilyMatcher.Family.SAVANNA);
      }

      if (AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.PLAINS)
         || contains(path, "plains", "meadow", "field", "pasture", "prairie", "steppe", "heath", "moorland")) {
         families.add(BiomeFamilyMatcher.Family.PLAINS);
      }

      if (AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.DESERT) || contains(path, "desert", "dune", "sahel")) {
         families.add(BiomeFamilyMatcher.Family.DESERT);
      }

      if (AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.SWAMP)
         || contains(path, "swamp", "bayou", "marsh", "wetland", "fen", "mangrove", "sludge")) {
         families.add(BiomeFamilyMatcher.Family.SWAMP);
      }

      if (AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.SNOWY) || contains(path, "snow", "frozen", "ice", "glacier", "tundra")) {
         families.add(BiomeFamilyMatcher.Family.SNOWY);
      }

      if (AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.MUSHROOM) || contains(path, "mushroom", "fungal")) {
         families.add(BiomeFamilyMatcher.Family.MUSHROOM);
      }

      if (AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.WASTELAND)
         || AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.DEAD)
         || contains(path, "wasteland", "dead_", "barren", "eroded")) {
         families.add(BiomeFamilyMatcher.Family.WASTELAND);
      }

      if (AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.SPOOKY)
         || contains(path, "spooky", "ghost", "witch", "ominous", "bloodied", "seeping")) {
         families.add(BiomeFamilyMatcher.Family.SPOOKY);
      }

      if (AWCorePlatform.isBiomeTagged(biome, PlatformTagAccess.BiomeTag.MAGICAL) || contains(path, "mystic", "magical", "enchanted")) {
         families.add(BiomeFamilyMatcher.Family.MAGICAL);
      }

      if (contains(path, "flower", "floral", "blossom", "cherry", "lavender")) {
         families.add(BiomeFamilyMatcher.Family.FLORAL);
      }

      if (!families.contains(BiomeFamilyMatcher.Family.SNOWY) && ((Biome)biome.m_203334_()).m_47554_() < 0.15F) {
         families.add(BiomeFamilyMatcher.Family.SNOWY);
      }

      return families;
   }

   private static EnumSet<BiomeFamilyMatcher.Family> familiesOf(String biomeId) {
      if (biomeId != null && !biomeId.isBlank()) {
         String path = biomeId.toLowerCase(Locale.ROOT);
         int separator = path.indexOf(58);
         if (separator >= 0 && separator + 1 < path.length()) {
            path = path.substring(separator + 1);
         }

         EnumSet<BiomeFamilyMatcher.Family> families = EnumSet.noneOf(BiomeFamilyMatcher.Family.class);
         if (contains(path, "deep_ocean", "deep_frozen_ocean")) {
            families.add(BiomeFamilyMatcher.Family.DEEP_OCEAN);
         } else if (oceanName(path)) {
            families.add(BiomeFamilyMatcher.Family.OCEAN);
         }

         if (contains(path, "beach", "shore", "coast")) {
            families.add(BiomeFamilyMatcher.Family.BEACH);
         }

         if (contains(path, "river", "stream")) {
            families.add(BiomeFamilyMatcher.Family.RIVER);
         }

         if (contains(path, "badlands", "mesa")) {
            families.add(BiomeFamilyMatcher.Family.BADLANDS);
         }

         if (contains(path, "mountain", "peak", "cliff", "highland", "alps", "plateau", "stone_hills")) {
            families.add(BiomeFamilyMatcher.Family.MOUNTAIN);
         }

         if (contains(path, "birch")) {
            families.add(BiomeFamilyMatcher.Family.BIRCH_FOREST);
         } else if (contains(path, "taiga", "conifer", "redwood", "spruce", "pine")) {
            families.add(BiomeFamilyMatcher.Family.TAIGA);
         } else if (contains(path, "jungle", "rainforest", "bamboo")) {
            families.add(BiomeFamilyMatcher.Family.JUNGLE);
         } else if (contains(path, "forest", "woods", "woodland", "grove", "orchard")) {
            families.add(BiomeFamilyMatcher.Family.FOREST);
         }

         if (contains(path, "savanna")) {
            families.add(BiomeFamilyMatcher.Family.SAVANNA);
         }

         if (contains(path, "plains", "meadow", "field", "pasture", "prairie", "steppe", "heath", "moorland")) {
            families.add(BiomeFamilyMatcher.Family.PLAINS);
         }

         if (contains(path, "desert", "dune", "sahel")) {
            families.add(BiomeFamilyMatcher.Family.DESERT);
         }

         if (contains(path, "swamp", "bayou", "marsh", "wetland", "fen", "mangrove", "sludge")) {
            families.add(BiomeFamilyMatcher.Family.SWAMP);
         }

         if (contains(path, "snow", "frozen", "ice", "glacier", "tundra", "cold_")) {
            families.add(BiomeFamilyMatcher.Family.SNOWY);
         }

         if (contains(path, "mushroom", "fungal")) {
            families.add(BiomeFamilyMatcher.Family.MUSHROOM);
         }

         if (contains(path, "wasteland", "dead_", "barren", "eroded")) {
            families.add(BiomeFamilyMatcher.Family.WASTELAND);
         }

         if (contains(path, "spooky", "ghost", "witch", "ominous", "bloodied", "seeping")) {
            families.add(BiomeFamilyMatcher.Family.SPOOKY);
         }

         if (contains(path, "mystic", "magical", "enchanted")) {
            families.add(BiomeFamilyMatcher.Family.MAGICAL);
         }

         if (contains(path, "flower", "floral", "blossom", "cherry", "lavender")) {
            families.add(BiomeFamilyMatcher.Family.FLORAL);
         }

         return families;
      } else {
         return EnumSet.noneOf(BiomeFamilyMatcher.Family.class);
      }
   }

   private static boolean oceanName(String path) {
      return path.equals("ocean") || path.endsWith("_ocean") || path.contains("ocean_") || path.contains("kelp_forest") || path.contains("coral_reef");
   }

   private static boolean contains(String value, String... fragments) {
      for (String fragment : fragments) {
         if (value.contains(fragment)) {
            return true;
         }
      }

      return false;
   }

   private static enum Family {
      OCEAN,
      DEEP_OCEAN,
      BEACH,
      RIVER,
      MOUNTAIN,
      BADLANDS,
      TAIGA,
      JUNGLE,
      FOREST,
      BIRCH_FOREST,
      SAVANNA,
      PLAINS,
      DESERT,
      SWAMP,
      SNOWY,
      MUSHROOM,
      WASTELAND,
      SPOOKY,
      MAGICAL,
      FLORAL;
   }
}
