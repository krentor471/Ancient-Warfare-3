package net.shadowmage.ancientwarfare.core.faction;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * A player entering a new era. The world hears of it by the country's name, and every faction the
 * player knows thinks a little better of a people that has come so far.
 */
public final class CountryEras {
   /** Standing every known faction gains when the player enters a new era. */
   static final int ERA_STANDING = 5;

   private CountryEras() {
   }

   public static void reached(ServerLevel level, UUID owner, String era) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionNews.tell(
         level, Component.translatable("news.ancientwarfare.era", CountryDiplomacy.name(level, data, owner), Component.translatable("research.ancientwarfare." + era))
      );
      FactionStandingData standings = FactionStandingData.get(level);
      for (String faction : standings.knownFactions(owner)) {
         standings.adjustStanding(owner, faction, ERA_STANDING);
      }
   }
}
