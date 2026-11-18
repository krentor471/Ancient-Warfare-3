package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import toughasnails.api.temperature.TemperatureHelper;
import toughasnails.api.temperature.TemperatureLevel;

/**
 * Tough As Nails: a settlement is warmer than the open land around it. Its hearths, ovens and
 * close-built walls take one step off the cold, so in winter a traveller reaches a town to warm up.
 */
public final class SeasonWarmth {
   private SeasonWarmth() {
   }

   public static void register() {
      TemperatureHelper.registerPositionalTemperatureModifier(SeasonWarmth::modify);
   }

   private static TemperatureLevel modify(Level level, BlockPos pos, TemperatureLevel current) {
      if ((current == TemperatureLevel.COLD || current == TemperatureLevel.ICY) && level instanceof ServerLevel server
         && FactionCrime.settlementAt(server, pos) != null) {
         return current.increment(1);
      }

      return current;
   }
}
