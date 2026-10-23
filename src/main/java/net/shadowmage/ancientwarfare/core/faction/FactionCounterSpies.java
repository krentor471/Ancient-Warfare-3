package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.research.PlayerResearch;

/**
 * Other factions' spies in the player's villages. A faction that dislikes the player, or is at war
 * with them, now and then sends one. The village watch catches some, and many more once the
 * player has a secret service; a caught spy is brought before the player, who has them executed,
 * trades them back for a ransom, or tries to turn them into a double agent. A spy nobody catches
 * stirs up the village, and its order falls.
 */
public final class FactionCounterSpies {
   private static final double BASE_CHANCE = 0.08;
   private static final double HOSTILE_CHANCE = 0.1;
   private static final double WAR_CHANCE = 0.2;
   private static final double CATCH_CHANCE = 0.3;
   private static final double SECRET_SERVICE_CATCH = 0.4;
   private static final double CIPHERS_CATCH = 0.05;
   private static final int SABOTAGE_ORDER = 10;
   private static final int EXECUTED_STANDING = -10;
   /** Days a faction sends no spy after one of its own was executed. */
   private static final long DETERRED_DAYS = 7L;
   private static final int RANSOM = 30;
   private static final int RANSOM_STANDING = 5;
   private static final double TURN_CHANCE = 0.5;
   private static final double SECRET_SERVICE_TURN = 0.3;
   private static final long DOUBLE_AGENT_TICKS = FactionSimulation.DAY_TICKS * 6L;
   private static final int ESCAPED_STANDING = -5;
   private static final Map<Integer, Captive> CAPTIVES = new HashMap<>();
   private static final Map<String, Long> DETERRED = new HashMap<>();
   private static int nextId;

   private FactionCounterSpies() {
   }

   private record Captive(int id, UUID player, String faction, long day) {
   }

   /** Daily: each player with villages may find a spy among them. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      long day = data.lastSimulatedDay();
      CAPTIVES.values().removeIf(captive -> day - captive.day > 1L);
      Set<UUID> owners = new LinkedHashSet<>(data.villages().values());
      FactionStandingData standings = FactionStandingData.get(level);
      for (UUID owner : owners) {
         for (String faction : standings.knownFactions(owner)) {
            int standing = standings.getStanding(owner, faction);
            boolean war = standings.getTreaty(owner, faction) == RelationState.WAR;
            if (DETERRED.getOrDefault(owner + "|" + faction, -1L) >= day || standing >= 30 && !war) {
               continue;
            }

            double chance = BASE_CHANCE + (standing < 0 ? HOSTILE_CHANCE : 0.0) + (war ? WAR_CHANCE : 0.0);
            if (level.getRandom().nextDouble() < chance) {
               arrive(level, data, owner, faction);
               break;
            }
         }
      }
   }

   /** A spy of the faction reaches one of the player's villages. */
   public static boolean arrive(ServerLevel level, FactionWorldData data, UUID owner, String faction) {
      List<BlockPos> villages = new ArrayList<>();
      data.villages().forEach((pos, who) -> {
         if (who.equals(owner)) {
            villages.add(pos);
         }
      });
      if (villages.isEmpty()) {
         return false;
      }

      BlockPos village = villages.get(level.getRandom().nextInt(villages.size()));
      double catchChance = CATCH_CHANCE + (PlayerResearch.has(level, owner, PlayerResearch.SECRET_SERVICE) ? SECRET_SERVICE_CATCH : 0.0)
         + (PlayerResearch.has(level, owner, PlayerResearch.CIPHERS) ? CIPHERS_CATCH : 0.0);
      if (level.getRandom().nextDouble() >= catchChance) {
         FactionWorldData.VillageState state = data.villageState(village);
         if (state != null) {
            state.order = Math.max(0, state.order - SABOTAGE_ORDER);
            data.setDirty();
         }

         FactionNews.tellPlayer(level, owner, Component.translatable("counterspy.ancientwarfare.unrest", village.getX(), village.getZ()).withStyle(ChatFormatting.GRAY));
         return true;
      }

      Captive captive = new Captive(++nextId, owner, faction, data.lastSimulatedDay());
      CAPTIVES.put(captive.id, captive);
      ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
      if (player != null) {
         player.sendSystemMessage(
            Component.translatable("counterspy.ancientwarfare.caught", village.getX(), village.getZ(), FactionNews.factionName(faction)).withStyle(ChatFormatting.GOLD)
         );
         player.sendSystemMessage(
            Component.literal("  ")
               .append(FactionEnvoys.button("counterspy.ancientwarfare.execute", "/awspy execute " + captive.id, ChatFormatting.RED))
               .append("  ")
               .append(FactionEnvoys.button("counterspy.ancientwarfare.exchange", "/awspy exchange " + captive.id, ChatFormatting.YELLOW))
               .append("  ")
               .append(FactionEnvoys.button("counterspy.ancientwarfare.turn", "/awspy turn " + captive.id, ChatFormatting.AQUA))
         );
      }

      return true;
   }

   /** The player decides the caught spy's fate: "execute", "exchange" or "turn". */
   public static void judge(ServerPlayer player, int id, String verdict) {
      Captive captive = CAPTIVES.get(id);
      if (captive == null || !captive.player.equals(player.getUUID())) {
         player.sendSystemMessage(Component.translatable("counterspy.ancientwarfare.gone").withStyle(ChatFormatting.GRAY));
         return;
      }

      CAPTIVES.remove(id);
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionStandingData standings = FactionStandingData.get(level);
      Component name = FactionNews.factionName(captive.faction);
      switch (verdict) {
         case "execute" -> {
            standings.adjustStanding(player.getUUID(), captive.faction, EXECUTED_STANDING);
            DETERRED.put(player.getUUID() + "|" + captive.faction, data.lastSimulatedDay() + DETERRED_DAYS);
            player.sendSystemMessage(Component.translatable("counterspy.ancientwarfare.executed", name, DETERRED_DAYS).withStyle(ChatFormatting.RED));
         }
         case "exchange" -> {
            int ransom = Math.min(RANSOM, Math.max(0, data.treasury(captive.faction)));
            data.setTreasury(captive.faction, data.treasury(captive.faction) - ransom);
            Coins.give(player, ransom);
            standings.adjustStanding(player.getUUID(), captive.faction, RANSOM_STANDING);
            player.sendSystemMessage(Component.translatable("counterspy.ancientwarfare.exchanged", name, ransom).withStyle(ChatFormatting.YELLOW));
         }
         default -> {
            double chance = TURN_CHANCE + (PlayerResearch.has(player, PlayerResearch.SECRET_SERVICE) ? SECRET_SERVICE_TURN : 0.0);
            if (level.getRandom().nextDouble() < chance) {
               data.setIntel(player.getUUID(), captive.faction, Math.max(data.intelUntil(player.getUUID(), captive.faction), level.getGameTime()) + DOUBLE_AGENT_TICKS);
               player.sendSystemMessage(Component.translatable("counterspy.ancientwarfare.turned", name, DOUBLE_AGENT_TICKS / FactionSimulation.DAY_TICKS).withStyle(ChatFormatting.AQUA));
            } else {
               standings.adjustStanding(player.getUUID(), captive.faction, ESCAPED_STANDING);
               player.sendSystemMessage(Component.translatable("counterspy.ancientwarfare.escaped", name).withStyle(ChatFormatting.GRAY));
            }
         }
      }
   }
}
