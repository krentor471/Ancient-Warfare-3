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

   /** Compensation for a caravan robbed on the player's land, and the price of an apology for troops over the border, in silver. */
   public static final int CARAVAN_COMPENSATION = 150;
   public static final int BORDER_APOLOGY = 100;
   /** How far from a player's village a road counts as their land. */
   private static final int LAND_RANGE = 160;
   private static final int BORDER_STANDING = -5;
   /** Soldiers of one player in a faction's settlement that count as an army over the border. */
   public static final int BORDER_SOLDIERS = 3;
   private static final long BORDER_GRACE_TICKS = 2400L;
   private static final long BORDER_CHECK_TICKS = 200L;
   /** When each player was warned about their troops in a faction's settlement, by player and faction. */
   private static final Map<String, Long> WARNED = new HashMap<>();

   private record Incident(int id, UUID player, String faction, UUID culprit, long day, String kind, int price) {
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

      Incident incident = new Incident(++nextId, player, faction, culprit == null ? null : culprit.getUUID(), FactionWorldData.get(level).lastSimulatedDay(), "killing", WERGILD);
      OPEN.put(incident.id, incident);
      Component name = FactionNews.factionName(faction);
      Component victimName = Component.translatable("incident.ancientwarfare.victim." + (FactionNpcRole.TRADER.id().equals(victim.getNpcSubtype()) ? "trader" : "townsman"));
      String who = culprit == null ? "you" : "soldier";
      owner.sendSystemMessage(
         Component.translatable("incident.ancientwarfare.demand." + who, name, victimName, (int)victim.getX(), (int)victim.getZ()).withStyle(ChatFormatting.GOLD)
      );
      buttons(owner, incident);
   }

   private static void buttons(ServerPlayer owner, Incident incident) {
      MutableComponent buttons = Component.literal("  ")
         .append(FactionEnvoys.button("incident.ancientwarfare.pay." + incident.kind, "/awincident pay " + incident.id, ChatFormatting.YELLOW))
         .append(Component.literal(" (").append(Component.translatable("gui.ancientwarfare.deal.gold_short", incident.price / 10)).append(")").withStyle(ChatFormatting.GRAY));
      if (incident.culprit != null) {
         buttons.append("  ").append(FactionEnvoys.button("incident.ancientwarfare.surrender", "/awincident surrender " + incident.id, ChatFormatting.AQUA));
      }

      owner.sendSystemMessage(buttons.append("  ").append(FactionEnvoys.button("incident.ancientwarfare.refuse", "/awincident refuse " + incident.id, ChatFormatting.RED)));
   }

   /** A faction's caravan was robbed by outlaws near one of the player's villages: the faction holds the player answerable for their roads. */
   public static boolean onCaravanRobbed(ServerLevel level, String faction, double x, double z) {
      FactionWorldData data = FactionWorldData.get(level);
      UUID owner = null;
      double best = (double)LAND_RANGE * LAND_RANGE;
      for (Map.Entry<net.minecraft.core.BlockPos, UUID> village : data.villages().entrySet()) {
         double dx = village.getKey().getX() - x;
         double dz = village.getKey().getZ() - z;
         if (dx * dx + dz * dz < best) {
            best = dx * dx + dz * dz;
            owner = village.getValue();
         }
      }

      return owner != null
         && open(level, owner, faction, "caravan", CARAVAN_COMPENSATION, Component.translatable("incident.ancientwarfare.demand.caravan", FactionNews.factionName(faction), (int)x, (int)z));
   }

   /** Opens a demand of the given kind, unless one from the faction is already waiting or the player is at war with it. */
   private static boolean open(ServerLevel level, UUID player, String faction, String kind, int price, Component demand) {
      ServerPlayer owner = level.getServer().getPlayerList().getPlayer(player);
      if (owner == null || FactionDiplomacy.isOutlaw(faction) || FactionStandingData.get(level).getTreaty(player, faction) == RelationState.WAR
         || OPEN.values().stream().anyMatch(open -> open.player.equals(player) && open.faction.equals(faction))) {
         return false;
      }

      Incident incident = new Incident(++nextId, player, faction, null, FactionWorldData.get(level).lastSimulatedDay(), kind, price);
      OPEN.put(incident.id, incident);
      owner.sendSystemMessage(demand.copy().withStyle(ChatFormatting.GOLD));
      buttons(owner, incident);
      return true;
   }

   /** Now and then: a player's soldiers standing in a faction's settlement, without a treaty, are an army over the border. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % BORDER_CHECK_TICKS != 0L || level.players().isEmpty()) {
         return;
      }

      FactionStandingData standings = FactionStandingData.get(level);
      Map<String, Integer> counts = new HashMap<>();
      Map<String, FactionWorldData.Settlement> where = new HashMap<>();
      for (ServerPlayer player : level.players()) {
         for (net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity soldier : level.getEntitiesOfClass(
            net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity.class, player.getBoundingBox().inflate(256.0),
            npc -> npc.getOwner().filter(player.getUUID()::equals).isPresent()
         )) {
            for (FactionWorldData.Settlement settlement : data.settlements()) {
               String faction = settlement.faction();
               RelationState treaty = standings.getTreaty(player.getUUID(), faction);
               if (!FactionDiplomacy.isOutlaw(faction) && treaty == RelationState.NEUTRAL && settlement.horizontalDistanceSquared(soldier.getX(), soldier.getZ()) < 48 * 48) {
                  String key = player.getUUID() + "|" + faction;
                  counts.merge(key, 1, Integer::sum);
                  where.put(key, settlement);
                  break;
               }
            }
         }
      }

      long now = level.getGameTime();
      WARNED.keySet().removeIf(key -> !counts.containsKey(key));
      counts.forEach((key, count) -> {
         if (count < BORDER_SOLDIERS) {
            return;
         }

         UUID player = UUID.fromString(key.substring(0, key.indexOf('|')));
         String faction = key.substring(key.indexOf('|') + 1);
         FactionWorldData.Settlement settlement = where.get(key);
         Long warned = WARNED.get(key);
         if (warned == null) {
            WARNED.put(key, now);
            FactionNews.tellPlayer(
               level, player,
               Component.translatable("incident.ancientwarfare.border_warning", count, FactionNews.factionName(faction), FactionNews.named(settlement)).withStyle(ChatFormatting.YELLOW)
            );
         } else if (now - warned >= BORDER_GRACE_TICKS && now - warned < BORDER_GRACE_TICKS + BORDER_CHECK_TICKS) {
            standings.adjustStanding(player, faction, BORDER_STANDING);
            open(level, player, faction, "border", BORDER_APOLOGY, Component.translatable("incident.ancientwarfare.demand.border", FactionNews.factionName(faction), count));
         }
      });
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
            if (!Coins.take(player, incident.price)) {
               player.sendSystemMessage(Component.translatable("incident.ancientwarfare.no_money", incident.price / 10).withStyle(ChatFormatting.RED));
               return;
            }

            FactionWorldData data = FactionWorldData.get(level);
            data.setTreasury(incident.faction, data.treasury(incident.faction) + incident.price);
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
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.incident_refused." + incident.kind, FactionNews.factionName(incident.faction), who));
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
