package net.shadowmage.ancientwarfare.core.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

public final class IceAndFireStructureLootCompat {
   private static final String MOD_ID = "iceandfire";
   private static final String THEME_TAG = "AWIceAndFireLootTheme";
   private static final String TIER_TAG = "AWIceAndFireLootTier";
   private static final String SEED_TAG = "AWIceAndFireLootSeed";

   private IceAndFireStructureLootCompat() {
   }

   public static IceAndFireStructureLootCompat.Settings settingsFor(String templatePath, String templateName, boolean unique, String blockData) {
      if (!loaded()) {
         return null;
      } else {
         String context = (templatePath + " " + templateName + " " + lootTableName(blockData)).toLowerCase(Locale.ROOT);
         IceAndFireStructureLootCompat.Theme theme = theme(context);
         int tier = lootTier(blockData, context, unique);
         return new IceAndFireStructureLootCompat.Settings(theme.id, tier);
      }
   }

   public static void configure(ChestBlockEntity chest, IceAndFireStructureLootCompat.Settings settings, long seed) {
      if (loaded() && settings != null) {
         CompoundTag data = AWCorePlatform.blockEntityData(chest);
         data.m_128359_("AWIceAndFireLootTheme", settings.theme());
         data.m_128405_("AWIceAndFireLootTier", settings.tier());
         data.m_128356_("AWIceAndFireLootSeed", seed);
         chest.m_6596_();
      }
   }

   public static boolean hasPendingLoot(ChestBlockEntity chest) {
      return AWCorePlatform.blockEntityData(chest).m_128441_("AWIceAndFireLootTheme");
   }

   public static List<ItemStack> consumeChestLoot(ChestBlockEntity chest) {
      CompoundTag data = AWCorePlatform.blockEntityData(chest);
      if (!data.m_128441_("AWIceAndFireLootTheme")) {
         return List.of();
      } else {
         String theme = data.m_128461_("AWIceAndFireLootTheme");
         int tier = Math.max(1, data.m_128451_("AWIceAndFireLootTier"));
         long seed = data.m_128454_("AWIceAndFireLootSeed");
         data.m_128473_("AWIceAndFireLootTheme");
         data.m_128473_("AWIceAndFireLootTier");
         data.m_128473_("AWIceAndFireLootSeed");
         chest.m_6596_();
         return chest.m_58904_() instanceof ServerLevel serverLevel ? roll(serverLevel, theme, tier, seed) : List.of();
      }
   }

   public static List<ItemStack> roll(ServerLevel level, String themeName, int tier, long seed) {
      if (!loaded()) {
         return List.of();
      } else {
         IceAndFireStructureLootCompat.Theme theme = IceAndFireStructureLootCompat.Theme.byId(themeName);
         int clampedTier = Math.max(1, Math.min(3, tier));
         RandomSource random = RandomSource.m_216335_(seed == 0L ? level.f_46441_.m_188505_() : seed);
         List<ItemStack> rewards = new ArrayList<>();
         float primaryChance = Math.min(0.48F, theme.baseChance + (float)(clampedTier - 1) * 0.07F);
         if (random.m_188501_() < primaryChance) {
            addRoll(rewards, theme, clampedTier, random);
         }

         if (clampedTier >= 2 && random.m_188501_() < (clampedTier == 3 ? 0.18F : 0.08F)) {
            addRoll(rewards, theme, clampedTier, random);
         }

         if (clampedTier == 3 && random.m_188501_() < 0.03F) {
            addRoll(rewards, theme, clampedTier, random);
         }

         return rewards;
      }
   }

   private static void addRoll(List<ItemStack> rewards, IceAndFireStructureLootCompat.Theme theme, int tier, RandomSource random) {
      List<IceAndFireStructureLootCompat.Entry> available = theme.entries
         .stream()
         .filter(entryx -> entryx.minTier <= tier && item(entryx.itemId) != null)
         .toList();
      int totalWeight = available.stream().mapToInt(IceAndFireStructureLootCompat.Entry::weight).sum();
      if (totalWeight > 0) {
         int selection = random.m_188503_(totalWeight);

         for (IceAndFireStructureLootCompat.Entry entry : available) {
            selection -= entry.weight;
            if (selection < 0) {
               Item item = item(entry.itemId);
               if (item != null) {
                  int count = entry.minCount + random.m_188503_(entry.maxCount - entry.minCount + 1);
                  rewards.add(new ItemStack(item, count));
               }

               return;
            }
         }
      }
   }

   private static Item item(String path) {
      ResourceLocation id = new ResourceLocation("iceandfire", path);
      return BuiltInRegistries.f_257033_.m_7804_(id) ? (Item)BuiltInRegistries.f_257033_.m_7745_(id) : null;
   }

   private static boolean loaded() {
      return AWCorePlatform.isModLoaded("iceandfire");
   }

   private static String lootTableName(String data) {
      int start = data.indexOf("lootTableName:\"");
      if (start < 0) {
         return "";
      } else {
         start += "lootTableName:\"".length();
         int end = data.indexOf(34, start);
         return end < 0 ? "" : data.substring(start, end);
      }
   }

   private static int lootTier(String data, String context, boolean unique) {
      int tier = simpleInt(data, "lootLevel", 0);
      if (tier <= 0) {
         if (containsAny(
            context,
            "gorgon",
            "cyclop",
            "hydra",
            "dragonlair",
            "dragon_lair",
            "dragonroost",
            "dragon_roost",
            "iceandfirelair",
            "high_value",
            "treasure_hoard",
            "treasury_high",
            "boss",
            "emperor",
            "palace",
            "great_fortress",
            "greatcastle",
            "dragonlair",
            "dragon_lair"
         )) {
            tier = 3;
         } else if (containsAny(context, "medium_value", "treasury", "castle", "fortress", "temple", "mausoleum", "crypt", "tomb", "wizard", "armoury")) {
            tier = 2;
         } else {
            tier = 1;
         }
      }

      if (unique && tier < 2) {
         tier = 2;
      }

      return Math.max(1, Math.min(3, tier));
   }

   private static int simpleInt(String text, String key, int fallback) {
      int start = text.indexOf(key + ":");
      if (start < 0) {
         return fallback;
      } else {
         start += key.length() + 1;
         int end = start;

         while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
         }

         try {
            return Integer.parseInt(text.substring(start, end));
         } catch (NumberFormatException var6) {
            return fallback;
         }
      }
   }

   private static IceAndFireStructureLootCompat.Theme theme(String context) {
      if (containsAny(context, "gorgon")) {
         return IceAndFireStructureLootCompat.Theme.GORGON;
      } else if (containsAny(context, "cyclop")) {
         return IceAndFireStructureLootCompat.Theme.CYCLOPS;
      } else if (containsAny(
         context, "fire_dragon", "ice_dragon", "lightning_dragon", "dragonlair", "dragon_lair", "dragonroost", "dragon_roost", "iceandfirelair"
      )) {
         return IceAndFireStructureLootCompat.Theme.DRAGON;
      } else if (containsAny(context, "hydra", "swamp", "marsh")) {
         return IceAndFireStructureLootCompat.Theme.SWAMP;
      } else if (containsAny(context, "dread", "undead", "necro", "crypt", "mausoleum", "cemetery", "cemetary", "grave", "tomb", "vampire")) {
         return IceAndFireStructureLootCompat.Theme.UNDEAD;
      } else if (containsAny(context, "sea_serpent", "seaserpent", "ocean", "coastal", "beach", "ship", "pirate", "island", "siren")) {
         return IceAndFireStructureLootCompat.Theme.OCEAN;
      } else if (containsAny(context, "deathworm", "desert", "sarkonid", "sand", "pyramid")) {
         return IceAndFireStructureLootCompat.Theme.DESERT;
      } else if (containsAny(context, "amphithere", "jungle")) {
         return IceAndFireStructureLootCompat.Theme.JUNGLE;
      } else if (containsAny(context, "frozen", "snow", "icecastle", "icelord", "glacier")) {
         return IceAndFireStructureLootCompat.Theme.ICE;
      } else if (containsAny(context, "hippogryph", "mountain", "highland")) {
         return IceAndFireStructureLootCompat.Theme.MOUNTAIN;
      } else if (containsAny(context, "library", "wizard", "mage", "archive", "coven", "witch", "altar")) {
         return IceAndFireStructureLootCompat.Theme.ARCANE;
      } else {
         return containsAny(context, "armoury", "armory", "blacksmith", "barracks", "fortress", "castle", "guard", "tower")
            ? IceAndFireStructureLootCompat.Theme.MILITARY
            : IceAndFireStructureLootCompat.Theme.GENERAL;
      }
   }

   private static boolean containsAny(String text, String... needles) {
      for (String needle : needles) {
         if (text.contains(needle)) {
            return true;
         }
      }

      return false;
   }

   private static record Entry(String itemId, int weight, int minCount, int maxCount, int minTier) {
   }

   public static record Settings(String theme, int tier) {
   }

   private static enum Theme {
      GENERAL(
         "general",
         0.12F,
         List.of(
            e("manuscript", 42, 1, 3, 1),
            e("silver_nugget", 30, 2, 7, 1),
            e("silver_ingot", 18, 1, 2, 1),
            e("chain_link", 10, 1, 4, 1),
            e("sapphire_gem", 4, 1, 2, 2)
         )
      ),
      MILITARY(
         "military",
         0.16F,
         List.of(
            e("silver_ingot", 31, 1, 3, 1),
            e("silver_nugget", 20, 3, 8, 1),
            e("chain_link", 18, 2, 6, 1),
            e("dragonbone", 13, 1, 3, 2),
            e("silver_sword", 5, 1, 1, 2),
            e("silver_axe", 4, 1, 1, 2),
            e("troll_tusk", 5, 1, 2, 2),
            e("dragonbone_sword", 1, 1, 1, 3)
         )
      ),
      ARCANE(
         "arcane",
         0.2F,
         List.of(
            e("manuscript", 48, 1, 5, 1),
            e("pixie_dust", 18, 1, 3, 1),
            e("sapphire_gem", 15, 1, 3, 1),
            e("silver_ingot", 10, 1, 2, 1),
            e("fire_dragon_blood", 2, 1, 1, 2),
            e("ice_dragon_blood", 2, 1, 1, 2),
            e("lightning_dragon_blood", 2, 1, 1, 2),
            e("dragon_flute", 1, 1, 1, 3),
            e("pixie_wand", 1, 1, 1, 3)
         )
      ),
      UNDEAD(
         "undead",
         0.18F,
         List.of(
            e("witherbone", 30, 1, 4, 1),
            e("dread_shard", 27, 1, 4, 1),
            e("chain_link", 18, 2, 6, 1),
            e("silver_ingot", 12, 1, 2, 1),
            e("manuscript", 9, 1, 3, 1),
            e("dread_sword", 2, 1, 1, 2),
            e("dread_knight_sword", 1, 1, 1, 3)
         )
      ),
      DESERT(
         "desert",
         0.18F,
         List.of(
            e("deathworm_chitin_yellow", 28, 1, 4, 1),
            e("deathworm_chitin_red", 15, 1, 3, 1),
            e("deathworm_chitin_white", 10, 1, 3, 1),
            e("silver_ingot", 16, 1, 2, 1),
            e("manuscript", 20, 1, 4, 1),
            e("deathworm_tounge", 7, 1, 2, 2),
            e("deathworm_egg", 2, 1, 1, 3),
            e("deathworm_egg_giant", 1, 1, 1, 3)
         )
      ),
      OCEAN(
         "ocean",
         0.2F,
         List.of(
            e("sea_serpent_fang", 25, 1, 3, 1),
            e("shiny_scales", 18, 1, 3, 1),
            e("sea_serpent_scales_blue", 10, 1, 4, 1),
            e("sea_serpent_scales_green", 10, 1, 4, 1),
            e("sea_serpent_scales_bronze", 8, 1, 4, 1),
            e("silver_ingot", 13, 1, 3, 1),
            e("manuscript", 10, 1, 3, 1),
            e("siren_tear", 4, 1, 1, 2),
            e("siren_flute", 1, 1, 1, 3),
            e("tide_trident", 1, 1, 1, 3)
         )
      ),
      JUNGLE(
         "jungle",
         0.17F,
         List.of(
            e("amphithere_feather", 34, 1, 4, 1),
            e("pixie_dust", 20, 1, 3, 1),
            e("manuscript", 22, 1, 4, 1),
            e("rotten_egg", 12, 1, 1, 1),
            e("silver_ingot", 8, 1, 2, 1),
            e("amphithere_macuahuitl", 2, 1, 1, 3),
            e("stymphalian_bird_dagger", 2, 1, 1, 3)
         )
      ),
      SWAMP(
         "swamp",
         0.18F,
         List.of(
            e("stymphalian_bird_feather", 34, 1, 5, 1),
            e("hydra_fang", 17, 1, 3, 1),
            e("rotten_egg", 17, 1, 1, 1),
            e("manuscript", 17, 1, 3, 1),
            e("silver_ingot", 10, 1, 2, 1),
            e("hydra_arrow", 4, 2, 6, 2),
            e("hydra_heart", 1, 1, 1, 3)
         )
      ),
      ICE(
         "ice",
         0.2F,
         List.of(
            e("sapphire_gem", 31, 1, 4, 1),
            e("silver_ingot", 24, 1, 3, 1),
            e("manuscript", 18, 1, 4, 1),
            e("dragonscales_white", 10, 1, 3, 2),
            e("dragonscales_sapphire", 8, 1, 3, 2),
            e("ice_dragon_blood", 5, 1, 1, 2),
            e("ice_dragon_heart", 1, 1, 1, 3)
         )
      ),
      MOUNTAIN(
         "mountain",
         0.16F,
         List.of(
            e("troll_tusk", 26, 1, 3, 1),
            e("troll_leather_mountain", 22, 1, 4, 1),
            e("silver_ingot", 20, 1, 3, 1),
            e("dragonbone", 15, 1, 3, 1),
            e("manuscript", 12, 1, 3, 1),
            e("iron_hippogryph_armor", 3, 1, 1, 2),
            e("hippogryph_egg", 1, 1, 1, 3),
            e("diamond_hippogryph_armor", 1, 1, 1, 3)
         )
      ),
      DRAGON(
         "dragon",
         0.32F,
         List.of(
            e("dragonbone", 31, 1, 5, 1),
            e("manuscript", 17, 1, 4, 1),
            e("dragonscales_red", 7, 1, 4, 1),
            e("dragonscales_bronze", 7, 1, 4, 1),
            e("dragonscales_blue", 7, 1, 4, 1),
            e("dragonscales_white", 7, 1, 4, 1),
            e("dragonscales_sapphire", 7, 1, 4, 1),
            e("dragonscales_electric", 7, 1, 4, 1),
            e("fire_dragon_blood", 3, 1, 1, 2),
            e("ice_dragon_blood", 3, 1, 1, 2),
            e("lightning_dragon_blood", 3, 1, 1, 2),
            e("fire_dragon_heart", 1, 1, 1, 3),
            e("ice_dragon_heart", 1, 1, 1, 3),
            e("lightning_dragon_heart", 1, 1, 1, 3),
            e("dragon_flute", 1, 1, 1, 3)
         )
      ),
      GORGON(
         "gorgon",
         0.3F,
         List.of(
            e("silver_ingot", 31, 1, 4, 1),
            e("manuscript", 30, 1, 5, 1),
            e("chain_link", 18, 2, 6, 1),
            e("sapphire_gem", 15, 1, 3, 1),
            e("gorgon_head", 2, 1, 1, 3)
         )
      ),
      CYCLOPS(
         "cyclops",
         0.26F,
         List.of(
            e("dragonbone", 29, 1, 5, 1),
            e("troll_tusk", 20, 1, 3, 1),
            e("silver_ingot", 18, 1, 3, 1),
            e("manuscript", 17, 1, 4, 1),
            e("witherbone", 12, 1, 4, 1),
            e("cyclops_eye", 2, 1, 1, 3)
         )
      );

      private final String id;
      private final float baseChance;
      private final List<IceAndFireStructureLootCompat.Entry> entries;

      private Theme(String id, float baseChance, List<IceAndFireStructureLootCompat.Entry> entries) {
         this.id = id;
         this.baseChance = baseChance;
         this.entries = entries;
      }

      private static IceAndFireStructureLootCompat.Theme byId(String id) {
         for (IceAndFireStructureLootCompat.Theme theme : values()) {
            if (theme.id.equals(id)) {
               return theme;
            }
         }

         return GENERAL;
      }

      private static IceAndFireStructureLootCompat.Entry e(String itemId, int weight, int minCount, int maxCount, int minTier) {
         return new IceAndFireStructureLootCompat.Entry(itemId, weight, minCount, maxCount, minTier);
      }
   }
}
