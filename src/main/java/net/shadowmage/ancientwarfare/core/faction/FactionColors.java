package net.shadowmage.ancientwarfare.core.faction;

import java.util.Map;
import net.minecraft.util.Mth;

/** Each faction's colour, on the world map and on its banners. */
public final class FactionColors {
   /** Hand-picked colours, so neighbours never look alike; factions not listed get one from their name. */
   private static final Map<String, Integer> COLORS = Map.ofEntries(
      Map.entry("empire", 0xFFC03A3A),
      Map.entry("nogg", 0xFF3A6FD0),
      Map.entry("norska", 0xFF7FB8E8),
      Map.entry("sarkonid", 0xFFE0A030),
      Map.entry("zimba", 0xFF8A5A2A),
      Map.entry("barbarian", 0xFF5FA03A),
      Map.entry("buffloka", 0xFFD8D050),
      Map.entry("witchbane", 0xFF9050C0),
      Map.entry("vyncan", 0xFF2E8A6A),
      Map.entry("kong", 0xFF4A4A9A),
      Map.entry("sealsker", 0xFF50B0B0),
      Map.entry("guild", 0xFFE070B0),
      Map.entry("amazon", 0xFFA0D070),
      Map.entry("reiksgard", 0xFFB07050),
      Map.entry("shakayana", 0xFFF08040),
      Map.entry("smingol", 0xFF708090),
      Map.entry("xoltec", 0xFFD04080),
      Map.entry("zamurai", 0xFFF0F0F0)
   );

   private FactionColors() {
   }

   /** A steady colour per faction, the same in every world, as 0xAARRGGBB. */
   public static int color(String faction) {
      Integer fixed = COLORS.get(faction);
      if (fixed != null) {
         return fixed;
      }

      int mixed = faction.hashCode() * 0x9E3779B9;
      float hue = ((mixed >>> 8) & 0xFFFF) / 65536.0F;
      return 0xFF000000 | Mth.hsvToRgb(hue, 0.6F, 0.8F);
   }
}
