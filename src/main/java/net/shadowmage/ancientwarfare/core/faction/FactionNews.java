package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

   private static void tell(ServerLevel level, Component message) {
      AncientWarfareCore.LOG.info("Faction news: {}", message.getString());
      level.getServer().getPlayerList().broadcastSystemMessage(message, false);
   }
}
