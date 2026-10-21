package net.shadowmage.ancientwarfare.core.faction;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/** World news: wars, treaties, conquests and new villages, told to every player in chat. */
public final class FactionNews {
   private FactionNews() {
   }

   public static Component factionName(String faction) {
      return Component.translatable("faction.ancientwarfare." + faction);
   }

   public static void relationChanged(ServerLevel level, String faction, String other, RelationState state) {
      tell(level, Component.translatable("news.ancientwarfare." + state.serializedName(), factionName(faction), factionName(other)));
   }

   /** A change of relation with its cause: the reason key's message takes both factions, then the extra arguments. */
   public static void relationChanged(ServerLevel level, String faction, String other, RelationState state, String reason, Object... extra) {
      Object[] args = new Object[2 + extra.length];
      args[0] = factionName(faction);
      args[1] = factionName(other);
      System.arraycopy(extra, 0, args, 2, extra.length);
      tell(level, Component.translatable("news.ancientwarfare." + state.serializedName() + "." + reason, args));
   }

   public static void ceded(ServerLevel level, String from, String to, int settlements) {
      tell(level, Component.translatable("news.ancientwarfare.ceded", factionName(from), factionName(to), settlements));
   }

   public static void tension(ServerLevel level, String faction, String other) {
      tell(level, Component.translatable("news.ancientwarfare.tension", factionName(faction), factionName(other)));
   }

   /** A settlement's name with a space before it, to follow a word such as "village" in news; empty when it has none. */
   public static Component named(FactionWorldData.Settlement settlement) {
      return settlement == null || settlement.name().isEmpty() ? Component.empty() : Component.literal(" ").append(settlement.nameComponent());
   }

   /** The name of the faction's settlement at a place, as named() gives it. */
   static Component namedAt(ServerLevel level, String faction, int x, int z) {
      FactionWorldData.Settlement nearest = null;

      for (FactionWorldData.Settlement settlement : FactionWorldData.get(level).settlements()) {
         if (settlement.faction().equals(faction)
            && settlement.contains(x, z, 32)
            && (nearest == null || settlement.horizontalDistanceSquared(x, z) < nearest.horizontalDistanceSquared(x, z))) {
            nearest = settlement;
         }
      }

      return named(nearest);
   }

   public static void captured(ServerLevel level, String winner, String loser, FactionWorldData.Settlement settlement) {
      if (FactionTowns.CAPITAL.equals(settlement.kind())) {
         tell(
            level,
            Component.translatable(
               "news.ancientwarfare.capital_captured",
               factionName(winner),
               factionName(loser),
               settlement.center().getX(),
               settlement.center().getZ(),
               named(settlement)
            )
         );
         return;
      }

      tell(
         level,
         Component.translatable(
            "news.ancientwarfare.captured", factionName(winner), factionName(loser), settlement.center().getX(), settlement.center().getZ(), named(settlement)
         )
      );
   }

   public static void founded(ServerLevel level, String faction, int x, int z) {
      tell(level, Component.translatable("news.ancientwarfare.founded", factionName(faction), x, z, namedAt(level, faction, x, z)));
   }

   public static void capitalBuilt(ServerLevel level, String faction, int x, int z) {
      tell(level, Component.translatable("news.ancientwarfare.capital_built", factionName(faction), x, z, namedAt(level, faction, x, z)));
   }

   public static void fortified(ServerLevel level, String faction, int x, int z) {
      tell(level, Component.translatable("news.ancientwarfare.fortified", factionName(faction), x, z, namedAt(level, faction, x, z)));
   }

   /** News for one player only, such as a raid on their village; lost while they are offline. */
   public static void tellPlayer(ServerLevel level, UUID playerId, Component message) {
      AncientWarfareCore.LOG.info("Faction news for {}: {}", playerId, message.getString());
      ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
      if (player != null) {
         player.sendSystemMessage(message);
      }
   }

   static void tell(ServerLevel level, Component message) {
      AncientWarfareCore.LOG.info("Faction news: {}", message.getString());
      FactionWorldData data = FactionWorldData.get(level);
      data.addChronicle(Math.max(0L, data.lastSimulatedDay()), Component.Serializer.toJson(message));
      if (FactionHistory.running()) {
         // The world's past: nobody was there to hear it.
         return;
      }

      level.getServer().getPlayerList().broadcastSystemMessage(message, false);
   }
}
