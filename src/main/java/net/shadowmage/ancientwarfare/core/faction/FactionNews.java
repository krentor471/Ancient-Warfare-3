package net.shadowmage.ancientwarfare.core.faction;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

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

   public static void captured(ServerLevel level, String winner, String loser, FactionWorldData.Settlement settlement) {
      tell(
         level,
         Component.translatable("news.ancientwarfare.captured", factionName(winner), factionName(loser), settlement.center().getX(), settlement.center().getZ())
      );
   }

   public static void founded(ServerLevel level, String faction, int x, int z) {
      tell(level, Component.translatable("news.ancientwarfare.founded", factionName(faction), x, z));
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
      level.getServer().getPlayerList().broadcastSystemMessage(message, false);
   }
}
