package net.shadowmage.ancientwarfare.core.structure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class StructureEmptyContainerLootResolver {
   private static final String PREFIX = "chests/";
   private static final Map<String, String> FACTIONS = factionMap();

   private StructureEmptyContainerLootResolver() {
   }

   public static StructureEmptyContainerLootResolver.Match resolve(Level level, BlockPos pos, String templatePath, String templateName) {
      String identity = normalize(templatePath + " " + templateName);
      boolean compoundSettlement = has(
         identity,
         "villagewith",
         "villageand",
         "smalltown",
         "coastaltown",
         "colonycity",
         "citylarge",
         "walledtown",
         "marketvillage",
         "islandsempirecity",
         "islandsnoggcity"
      );
      if (compoundSettlement) {
         String nearby = purposeFromNearbyBlocks(level, pos, identity);
         if (nearby != null) {
            return match(nearby, "nearby-blocks");
         }

         String faction = factionFromIdentity(identity);
         if (faction != null) {
            return match(faction, "faction-in-compound-settlement");
         }
      }

      String purpose = purposeFromIdentity(identity);
      if (purpose != null) {
         return match(purpose, "building-purpose");
      } else {
         String nearbyx = purposeFromNearbyBlocks(level, pos, identity);
         if (nearbyx != null) {
            return match(nearbyx, "nearby-blocks");
         } else {
            String faction = factionFromIdentity(identity);
            if (faction != null) {
               return match(faction, "faction");
            } else {
               String tier = tierFromIdentity(identity);
               return tier != null ? match(tier, "structure-value") : match("structure_basic", "basic-fallback");
            }
         }
      }
   }

   private static String purposeFromIdentity(String text) {
      if (has(text, "treasury", "treasure", "vault", "bank", "hoard", "royalcrypt")) {
         return has(text, "high", "royal", "grand", "great", "king", "emperor", "capital") ? "castle_treasury_high_value" : "castle_treasury_medium_value";
      } else if (has(text, "armoury", "armory", "arsenal", "weaponsmith")) {
         if (has(text, "diamond")) {
            return "castle_armoury_diamond";
         } else if (has(text, "gold")) {
            return "castle_armoury_gold";
         } else if (has(text, "chain")) {
            return "castle_armoury_chainmail";
         } else {
            return has(text, "stone", "leather", "wood") ? "castle_armoury_stone_leather" : "castle_armoury_iron";
         }
      } else if (has(text, "blacksmith", "blacksmiths", "smithy", "forgehouse", "forgeworks")) {
         return "village_blacksmith";
      } else if (has(text, "pantry", "kitchen", "bakery", "restaurant", "tavern", "inn", "feasthall", "foodstore")) {
         if (has(text, "butcher", "meat", "hunter")) {
            return "castle_pantry_mixed";
         } else {
            return has(text, "vegetable", "garden") ? "castle_pantry_vegetable" : "castle_pantry_mixed";
         }
      } else if (has(text, "library", "archive", "archivist", "scriptorium", "university", "school")) {
         return "village_library";
      } else if (has(text, "wizard", "magician", "mage", "sorcer", "alchemist", "enchant")) {
         return "village_magician";
      } else if (has(text, "church", "chapel", "cathedral", "abbey", "monastery", "monastary", "shrine")) {
         return "village_church";
      } else if (has(text, "glassmaker", "glassworker", "glassworks")) {
         return "village_craftsman_glass_worker";
      } else if (has(text, "potter", "pottery")) {
         return "village_craftsman_pottery";
      } else if (has(text, "carpenter", "woodworker")) {
         return carpenterTable(text);
      } else if (has(text, "lumberjack", "loggingcamp", "sawmill")) {
         return lumberTable(text);
      } else if (has(text, "mine", "mineshaft", "quarry", "excavation")) {
         return "village_mine";
      } else if (!has(text, "windmill", "waterwheelmill", "watermill", "millhouse", "grainmill", "mill") && !text.endsWith("millaws")) {
         if (has(text, "fisher", "fishery", "fishing", "fishfarm")) {
            return "village_fisherman";
         } else if (has(text, "farm", "orchard", "plantation", "ranch", "stables", "barn", "animalpen")) {
            return farmTable(text);
         } else if (has(text, "piratetreasure")) {
            return "pirate_treasure";
         } else if (has(text, "pirateprovision", "piratekitchen")) {
            return "pirate_provisions";
         } else if (has(text, "ballista")) {
            return "ammo_ballista";
         } else if (has(text, "catapult", "trebuchet")) {
            return "ammo_catapult_trebuchet";
         } else if (has(text, "cannon")) {
            return "ammo_cannon";
         } else if (has(text, "hwacha")) {
            return "ammo_hwacha";
         } else if (has(text, "grave", "cemetery", "cemetary", "tomb", "mausoleum", "crypt", "catacomb", "burial")) {
            return has(text, "royal", "grand", "great", "ancient", "palace")
               ? "gravestone_high_value"
               : (has(text, "small", "simple") ? "gravestone_low_value" : "gravestone_medium_value");
         } else if (has(text, "market", "merchant", "shop", "store", "trader")) {
            if (has(text, "tool", "smith")) {
               return "shop_iron_tools";
            } else if (has(text, "potion", "alchem", "herb")) {
               return "shop_potion_ingredients";
            } else {
               return has(text, "wool", "carpet", "cloth", "tailor") ? "shop_wool_carpet" : "shop_simple_bits";
            }
         } else {
            return null;
         }
      } else {
         return "village_mill";
      }
   }

   private static String purposeFromNearbyBlocks(Level level, BlockPos pos, String identity) {
      StructureEmptyContainerLootResolver.NearbyCounts counts = new StructureEmptyContainerLootResolver.NearbyCounts();
      MutableBlockPos cursor = new MutableBlockPos();

      for (int dx = -4; dx <= 4; dx++) {
         for (int dy = -3; dy <= 3; dy++) {
            for (int dz = -4; dz <= 4; dz++) {
               cursor.m_122178_(pos.m_123341_() + dx, pos.m_123342_() + dy, pos.m_123343_() + dz);
               BlockState state = level.m_8055_(cursor);
               ResourceLocation id = BuiltInRegistries.f_256975_.m_7981_(state.m_60734_());
               String name = id == null ? "" : id.m_135815_();
               if (has(name, "bookshelf", "lectern")) {
                  counts.books++;
               }

               if (has(name, "anvil", "smithing_table", "grindstone", "blast_furnace")) {
                  counts.smithing++;
               }

               if (has(name, "brewing_stand", "enchanting_table")) {
                  counts.magic++;
               }

               if (has(name, "hay_block", "composter", "farmland")) {
                  counts.farm++;
               }

               if (has(name, "wheat", "carrots", "potatoes", "beetroots", "melon", "pumpkin")) {
                  counts.crops++;
               }

               if (has(name, "smoker", "campfire", "cake")) {
                  counts.food++;
               }

               if (has(name, "furnace")) {
                  counts.furnaces++;
               }
            }
         }
      }

      if (counts.books >= 2) {
         return "village_library";
      } else if (counts.smithing >= 1) {
         return has(identity, "castle", "fort", "barracks", "guard") ? "castle_armoury_iron" : "village_blacksmith";
      } else if (counts.magic >= 1) {
         return "village_magician";
      } else if (counts.farm + counts.crops >= 3) {
         return farmTable(identity);
      } else {
         return counts.food < 1 && counts.furnaces < 2 ? null : "castle_pantry_mixed";
      }
   }

   private static String factionFromIdentity(String text) {
      for (Entry<String, String> faction : FACTIONS.entrySet()) {
         if (text.contains(faction.getKey())) {
            if ("vyncan".equals(faction.getValue())) {
               if (text.contains("jungle")) {
                  return "vyncan_jungle";
               }

               if (has(text, "mesa", "badlands", "terracotta")) {
                  return "vyncan_mesa";
               }
            }

            if ("xoltec".equals(faction.getValue()) && has(text, "terracotta", "mesa")) {
               return "xoltec_terracotta";
            }

            if ("zimba".equals(faction.getValue()) && has(text, "ancientmine", "ancientmines")) {
               return "zimba_ancient_mines";
            }

            return faction.getValue();
         }
      }

      return null;
   }

   private static String tierFromIdentity(String text) {
      if (has(text, "palace", "capital", "citadel", "royal", "grandcastle", "greatcastle", "boss")) {
         return "structure_high";
      } else if (has(text, "castle", "fortress", "fort", "tower", "dungeon", "mansion", "temple", "lair", "keep")) {
         return "structure_medium";
      } else {
         return has(text, "house", "hut", "village", "town", "camp") ? "structure_low" : null;
      }
   }

   private static String farmTable(String text) {
      if (has(text, "apple", "orchard")) {
         return "village_farm_apple";
      } else if (has(text, "chicken", "poultry", "pheasant", "turkey", "goose")) {
         return "village_farm_chicken";
      } else if (has(text, "cocoa")) {
         return "village_farm_cocoa";
      } else if (has(text, "cow", "cattle", "bison", "goat")) {
         return "village_farm_cow";
      } else if (has(text, "horse", "stable")) {
         return "village_farm_horse";
      } else if (has(text, "melon")) {
         return "village_farm_melon";
      } else if (has(text, "mushroom")) {
         return "village_farm_mushroom";
      } else if (has(text, "pig", "boar")) {
         return "village_farm_pig";
      } else if (has(text, "pumpkin")) {
         return "village_farm_pumpkin";
      } else if (has(text, "sheep", "wool")) {
         return "village_farm_sheep";
      } else if (has(text, "sugarcane", "sugar", "reed")) {
         return "village_farm_sugarcane";
      } else if (has(text, "wheat", "grain")) {
         return "village_farm_wheat";
      } else {
         return has(text, "vegetable", "potato", "carrot", "beetroot", "beans") ? "village_farm_vegetable" : "village_farm_mixed";
      }
   }

   private static String carpenterTable(String text) {
      if (text.contains("acacia")) {
         return "village_craftsman_carpenter_acacia";
      } else if (text.contains("birch")) {
         return "village_craftsman_carpenter_birch";
      } else if (has(text, "darkoak", "dark_oak")) {
         return "village_craftsman_carpenter_dark_oak";
      } else if (text.contains("jungle")) {
         return "village_craftsman_carpenter_jungle";
      } else {
         return text.contains("spruce") ? "village_craftsman_carpenter_spruce" : "village_craftsman_carpenter_oak";
      }
   }

   private static String lumberTable(String text) {
      if (text.contains("acacia")) {
         return "lumberjack_acacia";
      } else if (text.contains("birch")) {
         return "lumberjack_birch";
      } else if (has(text, "darkoak", "dark_oak")) {
         return "lumberjack_dark_oak";
      } else if (text.contains("jungle")) {
         return "lumberjack_jungle";
      } else {
         return text.contains("spruce") ? "lumberjack_spruce" : "lumberjack_oak";
      }
   }

   private static StructureEmptyContainerLootResolver.Match match(String table, String reason) {
      return new StructureEmptyContainerLootResolver.Match(new ResourceLocation("ancientwarfare", "chests/" + table), reason);
   }

   private static boolean has(String text, String... values) {
      for (String value : values) {
         if (text.contains(normalize(value))) {
            return true;
         }
      }

      return false;
   }

   private static String normalize(String text) {
      return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
   }

   private static Map<String, String> factionMap() {
      Map<String, String> result = new LinkedHashMap<>();

      for (String name : List.of(
         "witchbane",
         "minossian",
         "lizardman",
         "barbarian",
         "rakshasa",
         "guardian",
         "wizardly",
         "sealsker",
         "sarkonid",
         "buffloka",
         "vampire",
         "amazon",
         "brigand",
         "gremlin",
         "ishtari",
         "smingol",
         "vyncan",
         "xoltec",
         "zimba",
         "empire",
         "norska",
         "goblin",
         "undead",
         "pirate",
         "icelord",
         "monster",
         "dwarf",
         "coven",
         "malice",
         "giant",
         "gnome",
         "hobbit",
         "klown",
         "kong",
         "orc",
         "elf",
         "ent",
         "demon",
         "beast",
         "guild",
         "evil",
         "good"
      )) {
         result.put(name, name);
      }

      return Collections.unmodifiableMap(result);
   }

   public static record Match(ResourceLocation table, String reason) {
   }

   private static final class NearbyCounts {
      int books;
      int smithing;
      int magic;
      int farm;
      int crops;
      int food;
      int furnaces;
   }
}
