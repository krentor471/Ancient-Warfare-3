package net.shadowmage.ancientwarfare.core.forge.config;

import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

public final class AWLootConfig {
   public static final ForgeConfigSpec SPEC;
   private static final DoubleValue GLOBAL_ROLL_MULTIPLIER;
   private static final DoubleValue GLOBAL_COUNT_MULTIPLIER;
   private static final BooleanValue PREVENT_DUPLICATE_RESEARCH;
   private static final BooleanValue FILL_UNCONFIGURED_STRUCTURE_CONTAINERS;
   private static final ConfigValue<List<? extends String>> DISABLED_TABLES;
   private static final ConfigValue<List<? extends String>> TABLE_ROLL_MULTIPLIERS;

   private AWLootConfig() {
   }

   public static int effectiveRolls(ResourceLocation table, int baseRolls, long seed) {
      if (table != null && !disabled(table)) {
         double multiplier = (Double)GLOBAL_ROLL_MULTIPLIER.get() * tableMultiplier(table);
         double exact = (double)Math.max(0, baseRolls) * multiplier;
         int rolls = (int)Math.floor(exact);
         double fraction = exact - (double)rolls;
         if (fraction > 0.0) {
            RandomSource random = RandomSource.create(seed ^ (long)table.hashCode() ^ 5931043124968374604L);
            if (random.nextDouble() < fraction) {
               rolls++;
            }
         }

         return Math.max(0, rolls);
      } else {
         return 0;
      }
   }

   public static double itemCountMultiplier() {
      return (Double)GLOBAL_COUNT_MULTIPLIER.get();
   }

   public static boolean preventDuplicateResearchNotes() {
      return (Boolean)PREVENT_DUPLICATE_RESEARCH.get();
   }

   public static boolean fillUnconfiguredStructureContainers() {
      return (Boolean)FILL_UNCONFIGURED_STRUCTURE_CONTAINERS.get();
   }

   private static boolean disabled(ResourceLocation table) {
      String target = table.toString();

      for (String configured : (List<String>)DISABLED_TABLES.get()) {
         if (target.equals(configured.trim().toLowerCase(Locale.ROOT))) {
            return true;
         }
      }

      return false;
   }

   private static double tableMultiplier(ResourceLocation table) {
      String target = table.toString();

      for (String configured : (List<String>)TABLE_ROLL_MULTIPLIERS.get()) {
         int separator = configured.lastIndexOf(61);
         if (separator > 0 && target.equals(configured.substring(0, separator).trim().toLowerCase(Locale.ROOT))) {
            try {
               return Math.max(0.0, Math.min(100.0, Double.parseDouble(configured.substring(separator + 1).trim())));
            } catch (NumberFormatException var6) {
               return 1.0;
            }
         }
      }

      return 1.0;
   }

   private static boolean validTableId(Object value) {
      if (value instanceof String text && ResourceLocation.tryParse(text.trim().toLowerCase(Locale.ROOT)) != null) {
         return true;
      }

      return false;
   }

   private static boolean validMultiplierEntry(Object value) {
      if (!(value instanceof String text)) {
         return false;
      } else {
         int separator = text.lastIndexOf(61);
         if (separator > 0 && ResourceLocation.tryParse(text.substring(0, separator).trim().toLowerCase(Locale.ROOT)) != null) {
            try {
               double multiplier = Double.parseDouble(text.substring(separator + 1).trim());
               return multiplier >= 0.0 && multiplier <= 100.0;
            } catch (NumberFormatException var5) {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   static {
      Builder builder = new Builder();
      builder.comment(
            new String[]{
               "Ancient Warfare structure loot settings.",
               "For full item, weight and pool editing use config/ancientwarfare-loot-tables/overrides.",
               "All settings are server-authoritative in multiplayer."
            }
         )
         .push("structure_loot");
      GLOBAL_ROLL_MULTIPLIER = builder.comment(
            new String[]{
               "Multiplier for the number of complete loot-table executions requested by a structure container.",
               "This multiplies legacy lootRolls/lootLevel behavior; it does not rewrite rolls inside JSON pools.",
               "0 disables generated structure loot globally. Default: 1.0"
            }
         )
         .defineInRange("globalRollMultiplier", 1.0, 0.0, 100.0);
      GLOBAL_COUNT_MULTIPLIER = builder.comment(
            new String[]{
               "Multiplier applied to generated stack counts after a table is rolled.", "Fractional results use deterministic random rounding. Default: 1.0"
            }
         )
         .defineInRange("globalItemCountMultiplier", 1.0, 0.0, 100.0);
      PREVENT_DUPLICATE_RESEARCH = builder.comment("Avoid duplicate research notes in one container. Default preserves the current remastered behavior.")
         .define("preventDuplicateResearchNotes", true);
      FILL_UNCONFIGURED_STRUCTURE_CONTAINERS = builder.comment(
            new String[]{
               "Fill otherwise empty containers in naturally generated AWS/AWT structures with a table inferred from the building purpose, nearby workstations and faction.",
               "Existing configured loot and valid fixed template contents are never replaced. Player-built structures remain empty."
            }
         )
         .define("fillUnconfiguredStructureContainers", true);
      DISABLED_TABLES = builder.comment(
            new String[]{"Loot tables that should generate nothing.", "Use full ids, for example: [\"ancientwarfare:chests/empire\"]"}
         )
         .defineListAllowEmpty("disabledTables", List.of(), AWLootConfig::validTableId);
      TABLE_ROLL_MULTIPLIERS = builder.comment(
            new String[]{
               "Optional per-table execution multipliers, applied after globalRollMultiplier.",
               "Format: [\"ancientwarfare:chests/empire=0.5\", \"ancientwarfare:chests/dwarf=2.0\"]"
            }
         )
         .defineListAllowEmpty("tableRollMultipliers", List.of(), AWLootConfig::validMultiplierEntry);
      builder.pop();
      SPEC = builder.build();
   }
}
