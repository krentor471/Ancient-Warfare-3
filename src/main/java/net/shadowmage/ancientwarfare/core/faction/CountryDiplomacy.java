package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * How the factions deal with a player's country as a neighbour. Along a shared border feelings
 * drift: plain neighbours grow wary of each other, while a pact, an alliance or trade draws them
 * closer. Treaties are kept: when a faction falls on a player, the player's allies go to war with
 * it, and when the player goes to war, allies who think well enough of them join in. The world
 * hears of wars made on a country, by the country's name.
 */
public final class CountryDiplomacy {
   /** Daily drift along a shared border, and where it stops. */
   static final int BORDER_FRICTION = 1;
   static final int FRICTION_FLOOR = -40;
   static final int FRIENDSHIP_DRIFT = 1;
   static final int FRIENDSHIP_CEILING = 80;
   /** An ally joins a war the player starts only when it thinks this well of them. */
   static final int JOIN_WAR_STANDING = 70;
   /** Standing an ally gains for standing by the player, and loses for leaving them alone when called. */
   static final int STOOD_BY_STANDING = 10;
   private static final int[][] BESIDE = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};

   private CountryDiplomacy() {
   }

   /** The country's name for news and messages, or the player's own name when they have none. */
   static Component name(ServerLevel level, FactionWorldData data, UUID owner) {
      FactionWorldData.Country country = data.country(owner);
      if (country != null) {
         return Component.literal(country.name);
      }

      ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
      return player != null ? player.getDisplayName() : Component.translatable("country.ancientwarfare.someone");
   }

   /** The factions whose land touches each country's, with how many provinces they share a border along. */
   static Map<String, Integer> neighbours(FactionWorldData data, UUID owner) {
      String land = PlayerCountry.land(owner);
      Map<Long, String> owners = data.provinceOwners();
      Map<String, Integer> found = new HashMap<>();
      for (long province : data.provincesOf(land)) {
         int cellX = ChunkPos.getX(province);
         int cellZ = ChunkPos.getZ(province);
         for (int[] offset : BESIDE) {
            String other = owners.get(ChunkPos.asLong(cellX + offset[0], cellZ + offset[1]));
            if (other != null && !other.isEmpty() && !PlayerCountry.isLand(other) && !FactionDiplomacy.isOutlaw(other)) {
               found.merge(other, 1, Integer::sum);
            }
         }
      }

      return found;
   }

   /** Daily: feelings along each country's borders drift with the treaties between them. */
   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionStandingData standings = FactionStandingData.get(level);
      for (UUID owner : Set.copyOf(data.countries().keySet())) {
         for (String faction : neighbours(data, owner).keySet()) {
            standings.discover(owner, faction);
            RelationState treaty = standings.getTreaty(owner, faction);
            int standing = standings.getStanding(owner, faction);
            boolean friendly = treaty == RelationState.PACT || treaty == RelationState.ALLIANCE || standings.hasTrade(owner, faction);
            if (friendly && standing < FRIENDSHIP_CEILING) {
               standings.setStanding(owner, faction, Math.min(FRIENDSHIP_CEILING, standing + FRIENDSHIP_DRIFT));
            } else if (!friendly && treaty != RelationState.WAR && standing > FRICTION_FLOOR
               && !net.shadowmage.ancientwarfare.core.research.PlayerResearch.has(level, owner, net.shadowmage.ancientwarfare.core.research.PlayerResearch.MISSIONARIES)) {
               // Herds stray, woodcutters cross the line, each side blames the other.
               standings.setStanding(owner, faction, Math.max(FRICTION_FLOOR, standing - BORDER_FRICTION));
            }
         }
      }
   }

   /** A faction went to war with a player: the world hears of it, and the player's allies take up arms against it. */
   public static void warDeclaredOn(ServerLevel level, UUID owner, String aggressor) {
      FactionWorldData data = FactionWorldData.get(level);
      if (data.country(owner) != null) {
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.war_on_country", FactionNews.factionName(aggressor), name(level, data, owner)));
      }

      FactionStandingData standings = FactionStandingData.get(level);
      for (String ally : allies(standings, owner, aggressor)) {
         RelationState withAggressor = FactionDiplomacy.relation(level, ally, aggressor).state();
         if (withAggressor == RelationState.ALLIANCE) {
            // Allied to both sides: it stays out of it.
            FactionNews.tellPlayer(
               level, owner, Component.translatable("diplomacy.ancientwarfare.ally_torn", FactionNews.factionName(ally), FactionNews.factionName(aggressor))
            );
            continue;
         }

         if (withAggressor == RelationState.WAR || !FactionDiplomacy.setState(level, ally, aggressor, RelationState.WAR)) {
            continue;
         }

         standings.adjustStanding(owner, ally, STOOD_BY_STANDING);
         FactionNews.tell(
            level,
            Component.translatable("news.ancientwarfare.war.country_ally", FactionNews.factionName(ally), FactionNews.factionName(aggressor), name(level, data, owner))
         );
         AncientWarfareCore.LOG.info("{} stands by its ally {} against {}", ally, owner, aggressor);
      }
   }

   /** The player went to war: allies who think well of them join in, the others stay out of it. */
   public static void warDeclaredBy(ServerPlayer player, String target) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionStandingData standings = FactionStandingData.get(level);
      UUID owner = player.getUUID();
      for (String ally : allies(standings, owner, target)) {
         Component allyName = FactionNews.factionName(ally);
         RelationState withTarget = FactionDiplomacy.relation(level, ally, target).state();
         if (withTarget == RelationState.WAR) {
            player.sendSystemMessage(Component.translatable("diplomacy.ancientwarfare.ally_already", allyName).withStyle(ChatFormatting.GREEN));
         } else if (withTarget == RelationState.ALLIANCE || standings.getStanding(owner, ally) < JOIN_WAR_STANDING && !FactionVassals.isVassal(level, owner, ally)) {
            // Bound to the other side, or not fond enough of the player to bleed for them.
            player.sendSystemMessage(
               Component.translatable("diplomacy.ancientwarfare.ally_declined", allyName, standings.getStanding(owner, ally), JOIN_WAR_STANDING)
                  .withStyle(ChatFormatting.YELLOW)
            );
         } else if (FactionDiplomacy.setState(level, ally, target, RelationState.WAR)) {
            FactionNews.tell(
               level,
               Component.translatable("news.ancientwarfare.war.country_call", allyName, FactionNews.factionName(target), name(level, data, owner))
            );
         }
      }
   }

   /** The factions in alliance with the player, other than the one named. */
   private static Set<String> allies(FactionStandingData standings, UUID owner, String other) {
      Set<String> allies = new TreeSet<>();
      for (String faction : FactionRegistry.factionNames()) {
         if (!faction.equals(other) && !FactionDiplomacy.isOutlaw(faction) && standings.getTreaty(owner, faction) == RelationState.ALLIANCE) {
            allies.add(faction);
         }
      }

      return allies;
   }
}
