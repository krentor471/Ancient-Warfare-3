package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class FactionStandingData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_standings";
   private final Map<UUID, Map<String, Integer>> standings = new HashMap<>();

   public static FactionStandingData get(ServerLevel level) {
      MinecraftServer server = level.m_7654_();
      return (FactionStandingData)server.m_129783_()
         .m_8895_()
         .m_164861_(FactionStandingData::load, FactionStandingData::new, "ancientwarfare_faction_standings");
   }

   public int getStanding(UUID playerId, String factionName) {
      String faction = normalize(factionName);
      return this.standings.getOrDefault(playerId, Map.of()).getOrDefault(faction, FactionRegistry.get(faction).defaultStanding());
   }

   public int setStanding(UUID playerId, String factionName, int standing) {
      String faction = normalize(factionName);
      this.standings.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(faction, standing);
      this.m_77762_();
      return standing;
   }

   public int adjustStanding(UUID playerId, String factionName, int adjustment) {
      FactionRegistry.FactionDefinition definition = FactionRegistry.get(factionName);
      return definition.standingCanChange() && adjustment != 0
         ? this.setStanding(playerId, factionName, this.getStanding(playerId, factionName) + adjustment)
         : this.getStanding(playerId, factionName);
   }

   public boolean isHostile(UUID playerId, String factionName) {
      return this.getStanding(playerId, factionName) < 0;
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag players = new ListTag();
      this.standings.forEach((playerId, factionStandings) -> {
         CompoundTag playerTag = new CompoundTag();
         playerTag.m_128362_("Player", playerId);
         CompoundTag values = new CompoundTag();
         factionStandings.forEach(values::m_128405_);
         playerTag.m_128365_("Standings", values);
         players.add(playerTag);
      });
      tag.m_128365_("Players", players);
      return tag;
   }

   private static FactionStandingData load(CompoundTag tag) {
      FactionStandingData data = new FactionStandingData();

      for (Tag entry : tag.m_128437_("Players", 10)) {
         CompoundTag playerTag = (CompoundTag)entry;
         if (playerTag.m_128403_("Player")) {
            Map<String, Integer> values = new HashMap<>();
            CompoundTag standingsTag = playerTag.m_128469_("Standings");
            standingsTag.m_128431_().forEach(faction -> values.put(normalize(faction), standingsTag.m_128451_(faction)));
            data.standings.put(playerTag.m_128342_("Player"), values);
         }
      }

      return data;
   }

   private static String normalize(String value) {
      return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
   }
}
