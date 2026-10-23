package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * Diplomatic incidents. When the player, or one of the player's soldiers, kills a trader or a
 * townsman of a faction they are not at war with, the faction demands satisfaction: blood money,
 * or the culprit handed over to its justice. Refused, or left unanswered for a day, it bears a
 * grudge, and the world hears of it.
 */
public final class FactionIncidents {
   /** Blood money, in silver. */
   public static final int WERGILD = 200;
   private static final int PAID_STANDING = 3;
   private static final int SURRENDERED_STANDING = 5;
   private static final int REFUSED_STANDING = -20;
   private static final Map<Integer, Incident> OPEN = new HashMap<>();
   private static int nextId;

   private FactionIncidents() {
   }

   private record Incident(int id, UUID player, String faction, UUID culprit, long day) {
   }

   /** A faction's man died at the hands of the player (culprit null) or of one of their soldiers. */
   public static void onKilled(ServerLevel level, UUID player, FactionNpcEntity victim, Entity culprit) {
      String faction = victim.getFactionName();
      boolean civilian = FactionNpcRole.byId(victim.getNpcSubtype()).map(FactionNpcRole::isCivilian).orElse(false);
      FactionStandingData standings = FactionStandingData.get(level);
      if (!civilian || FactionDiplomacy.isOutlaw(faction) || standings.getTreaty(player, faction) == RelationState.WAR
         || OPEN.values().stream().anyMatch(open -> open.player.equals(player) && open.faction.equals(faction))) {
         return;
      }

      ServerPlayer owner = level.getServer().getPlayerList().getPlayer(player);
      if (owner == null) {
         return;
      }

      Incident incident = new Incident(++nextId, player, faction, culprit == null ? null : culprit.getUUID(), FactionWorldData.get(level).lastSimulatedDay());
      OPEN.put(incident.id, incident);
      Component name = FactionNews.factionName(faction);
      Component victimName = Component.translatable("incident.ancientwarfare.victim." + (FactionNpcRole.TRADER.id().equals(victim.getNpcSubtype()) ? "trader" : "townsman"));
      String who = culprit == null ? "you" : "soldier";
      owner.sendSystemMessage(
         Component.translatable("incident.ancientwarfare.demand." + who, name, victimName, (int)victim.getX(), (int)victim.getZ()).withStyle(ChatFormatting.GOLD)
      );
      MutableComponent buttons = Component.literal("  ")
         .append(FactionEnvoys.button("incident.ancientwarfare.pay", "/awincident pay " + incident.id, ChatFormatting.YELLOW))
         .append(Component.literal(" (").append(Component.translatable("gui.ancientwarfare.deal.gold_short", WERGILD / 10)).append(")").withStyle(ChatFormatting.GRAY));
      if (culprit != null) {
         buttons.append("  ").append(FactionEnvoys.button("incident.ancientwarfare.surrender", "/awincident surrender " + incident.id, ChatFormatting.AQUA));
      }

      owner.sendSystemMessage(buttons.append("  ").append(FactionEnvoys.button("incident.ancientwarfare.refuse", "/awincident refuse " + incident.id, ChatFormatting.RED)));
   }

   /** The player answers: "pay", "surrender" or "refuse". */
   public static void answer(ServerPlayer player, int id, String answer) {
      Incident incident = OPEN.get(id);
      if (incident == null || !incident.player.equals(player.getUUID())) {
         player.sendSystemMessage(Component.translatable("incident.ancientwarfare.gone").withStyle(ChatFormatting.GRAY));
         return;
      }

      ServerLevel level = player.serverLevel();
      FactionStandingData standings = FactionStandingData.get(level);
      Component name = FactionNews.factionName(incident.faction);
      switch (answer) {
         case "pay" -> {
            if (!Coins.take(player, WERGILD)) {
               player.sendSystemMessage(Component.translatable("incident.ancientwarfare.no_money", WERGILD / 10).withStyle(ChatFormatting.RED));
               return;
            }

            FactionWorldData data = FactionWorldData.get(level);
            data.setTreasury(incident.faction, data.treasury(incident.faction) + WERGILD);
            standings.adjustStanding(player.getUUID(), incident.faction, PAID_STANDING);
            player.sendSystemMessage(Component.translatable("incident.ancientwarfare.paid", name).withStyle(ChatFormatting.GREEN));
         }
         case "surrender" -> {
            Entity culprit = incident.culprit == null ? null : level.getEntity(incident.culprit);
            if (culprit == null || !culprit.isAlive()) {
               player.sendSystemMessage(Component.translatable("incident.ancientwarfare.no_culprit").withStyle(ChatFormatting.RED));
               return;
            }

            culprit.discard();
            standings.adjustStanding(player.getUUID(), incident.faction, SURRENDERED_STANDING);
            player.sendSystemMessage(Component.translatable("incident.ancientwarfare.surrendered", name).withStyle(ChatFormatting.GREEN));
         }
         default -> refuse(level, incident, player);
      }

      OPEN.remove(id);
   }

   private static void refuse(ServerLevel level, Incident incident, ServerPlayer player) {
      FactionStandingData.get(level).adjustStanding(incident.player, incident.faction, REFUSED_STANDING);
      Component who = player != null ? player.getDisplayName() : Component.literal("?");
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.incident_refused", FactionNews.factionName(incident.faction), who));
   }

   /** Daily: a demand left unanswered for a day counts as refused. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      long day = data.lastSimulatedDay();
      OPEN.values().removeIf(incident -> {
         if (day - incident.day < 1L) {
            return false;
         }

         refuse(level, incident, level.getServer().getPlayerList().getPlayer(incident.player));
         return true;
      });
   }
}
