package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.faction.FactionDiplomacy;
import net.shadowmage.ancientwarfare.core.faction.RelationState;

public final class FactionStandingData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_standings";
   private final Map<UUID, Map<String, Integer>> standings = new HashMap<>();
   /** The player's agreement with each faction; missing means neutral. */
   private final Map<UUID, Map<String, RelationState>> treaties = new HashMap<>();
   /** Factions the player has met, the ones the diplomacy screen lists. */
   private final Map<UUID, Set<String>> known = new HashMap<>();

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

   /** At war: always hostile. Under a pact or alliance: never. Otherwise hostile below zero standing. */
   public boolean isHostile(UUID playerId, String factionName) {
      return switch (this.getTreaty(playerId, factionName)) {
         case WAR -> true;
         case PACT, ALLIANCE -> false;
         default -> this.getStanding(playerId, factionName) < 0;
      };
   }

   public RelationState getTreaty(UUID playerId, String factionName) {
      String faction = normalize(factionName);
      return FactionDiplomacy.isOutlaw(faction)
         ? RelationState.WAR
         : this.treaties.getOrDefault(playerId, Map.of()).getOrDefault(faction, RelationState.NEUTRAL);
   }

   public void setTreaty(UUID playerId, String factionName, RelationState state) {
      this.treaties.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(normalize(factionName), state);
      this.setDirty();
   }

   public Set<String> knownFactions(UUID playerId) {
      return Collections.unmodifiableSet(this.known.getOrDefault(playerId, Set.of()));
   }

   /** Returns true when the player had not met this faction before. */
   public boolean discover(UUID playerId, String factionName) {
      String faction = normalize(factionName);
      if (!FactionRegistry.isKnown(faction) || !this.known.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(faction)) {
         return false;
      }

      this.setDirty();
      return true;
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag players = new ListTag();
      Set<UUID> playerIds = new HashSet<>(this.standings.keySet());
      playerIds.addAll(this.treaties.keySet());
      playerIds.addAll(this.known.keySet());
      playerIds.forEach(playerId -> {
         CompoundTag playerTag = new CompoundTag();
         playerTag.putUUID("Player", playerId);
         CompoundTag values = new CompoundTag();
         this.standings.getOrDefault(playerId, Map.of()).forEach(values::putInt);
         playerTag.put("Standings", values);
         CompoundTag treatyTag = new CompoundTag();
         this.treaties.getOrDefault(playerId, Map.of()).forEach((faction, state) -> treatyTag.putString(faction, state.serializedName()));
         playerTag.put("Treaties", treatyTag);
         ListTag knownTag = new ListTag();
         this.known.getOrDefault(playerId, Set.of()).forEach(faction -> knownTag.add(StringTag.valueOf(faction)));
         playerTag.put("Known", knownTag);
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
            UUID playerId = playerTag.getUUID("Player");
            data.standings.put(playerId, values);
            CompoundTag treatyTag = playerTag.getCompound("Treaties");
            Map<String, RelationState> playerTreaties = new HashMap<>();
            treatyTag.getAllKeys().forEach(faction -> playerTreaties.put(faction, RelationState.byName(treatyTag.getString(faction))));
            data.treaties.put(playerId, playerTreaties);
            Set<String> knownFactions = new HashSet<>();
            playerTag.getList("Known", Tag.TAG_STRING).forEach(element -> knownFactions.add(element.getAsString()));
            data.known.put(playerId, knownFactions);
         }
      }

      return data;
   }

   private static String normalize(String value) {
      return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
   }
}
