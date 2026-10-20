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
      MinecraftServer server = level.getServer();
      return (FactionStandingData)server.overworld()
         .getDataStorage()
         .computeIfAbsent(FactionStandingData::load, FactionStandingData::new, "ancientwarfare_faction_standings");
   }

   public int getStanding(UUID playerId, String factionName) {
      String faction = normalize(factionName);
      return this.standings.getOrDefault(playerId, Map.of()).getOrDefault(faction, FactionRegistry.get(faction).defaultStanding());
   }

   public int setStanding(UUID playerId, String factionName, int standing) {
      String faction = normalize(factionName);
      this.standings.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(faction, standing);
      this.setDirty();
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

   public CompoundTag save(CompoundTag tag) {
      ListTag players = new ListTag();
      this.standings.forEach((playerId, factionStandings) -> {
         CompoundTag playerTag = new CompoundTag();
         playerTag.putUUID("Player", playerId);
         CompoundTag values = new CompoundTag();
         factionStandings.forEach(values::putInt);
         playerTag.put("Standings", values);
         players.add(playerTag);
      });
      tag.put("Players", players);
      return tag;
   }

   private static FactionStandingData load(CompoundTag tag) {
      FactionStandingData data = new FactionStandingData();

      for (Tag entry : tag.getList("Players", 10)) {
         CompoundTag playerTag = (CompoundTag)entry;
         if (playerTag.hasUUID("Player")) {
            Map<String, Integer> values = new HashMap<>();
            CompoundTag standingsTag = playerTag.getCompound("Standings");
            standingsTag.getAllKeys().forEach(faction -> values.put(normalize(faction), standingsTag.getInt(faction)));
            data.standings.put(playerTag.getUUID("Player"), values);
         }
      }

      return data;
   }

   private static String normalize(String value) {
      return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
   }
}
