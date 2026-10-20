package net.shadowmage.ancientwarfare.core.structure;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

public final class StructureLootTierResolver {
   public static final ResourceLocation BASIC = new ResourceLocation("ancientwarfare", "chests/structure_basic");
   public static final ResourceLocation LOW = new ResourceLocation("ancientwarfare", "chests/structure_low");
   public static final ResourceLocation MEDIUM = new ResourceLocation("ancientwarfare", "chests/structure_medium");
   public static final ResourceLocation HIGH = new ResourceLocation("ancientwarfare", "chests/structure_high");

   private StructureLootTierResolver() {
   }

   public static ResourceLocation resolveLegacyTable(String oldTable) {
      return resolveLegacyTable(ResourceLocation.m_135820_(oldTable), oldTable);
   }

   public static ResourceLocation resolveStoredTable(ResourceLocation table) {
      return table == null ? table : resolveLegacyTable(table, table.toString());
   }

   private static ResourceLocation resolveLegacyTable(ResourceLocation parsed, String rawName) {
      if (parsed != null) {
         String namespace = parsed.m_135827_();
         if ("ancientwarfare".equals(namespace) || "minecraft".equals(namespace)) {
            return parsed;
         }

         if ("ancientwarfarenpc".equals(namespace)
            || "ancientwarfarestructure".equals(namespace)
            || "ancientwarfarevehicle".equals(namespace)
            || "ancientwarfareautomation".equals(namespace)) {
            return new ResourceLocation("ancientwarfare", parsed.m_135815_());
         }
      }

      String name = rawName == null ? "" : rawName.toLowerCase(Locale.ROOT);
      if (name.contains("high")
         || name.contains("treasure")
         || name.contains("diamond")
         || name.contains("dragon")
         || name.contains("legendary")
         || name.contains("royal")
         || name.contains("end_city")
         || name.contains("woodland_mansion")
         || name.contains("treasury")
         || name.contains("hoard")) {
         return HIGH;
      } else {
         return !name.contains("medium")
               && !name.contains("armoury")
               && !name.contains("monster")
               && !name.contains("undead")
               && !name.contains("empire")
               && !name.contains("norska")
               && !name.contains("nogg")
               && !name.contains("sarkonid")
               && !name.contains("iron")
               && !name.contains("weapon")
               && !name.contains("simple_dungeon")
               && !name.contains("village_blacksmith")
               && !name.contains("desert_pyramid")
               && !name.contains("jungle_temple")
               && !name.contains("stronghold")
               && !name.contains("nether_bridge")
               && !name.contains("abandoned_mineshaft")
               && !name.contains("wizard_tower")
               && !name.contains("cyclops")
               && !name.contains("cavern")
               && !name.contains("crypt")
            ? LOW
            : MEDIUM;
      }
   }
}
