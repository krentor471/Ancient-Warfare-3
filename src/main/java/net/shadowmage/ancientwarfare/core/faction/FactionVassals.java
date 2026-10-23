package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * Vassals and overlords. A weak faction can be brought, by a deal, to swear fealty to the player: it
 * stays the player's ally, pays tribute every week and must join every war the player wages, but its
 * discontent grows day by day, and gifts are what keep it down; at the end of its patience it rises
 * against the player. The other way round, the player can swear fealty to a strong faction: they pay
 * it tribute every week, it stands by them in war, and others think twice before attacking them.
 */
public final class FactionVassals extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_vassals";
   private static final long WEEK_DAYS = 7L;
   /** A vassal's weekly tribute: this share of its treasury, within the bounds below, in silver. */
   private static final int TRIBUTE_SHARE = 20;
   private static final int MIN_TRIBUTE = 20;
   private static final int MAX_TRIBUTE = 500;
   public static final int MAX_DISCONTENT = 100;
   private static final int DAILY_DISCONTENT = 2;
   private static final int FOND_STANDING = 60;
   private static final int GIFT_DISCONTENT = 20;
   private static final int RESTLESS = 70;
   private static final double RESTLESS_REVOLT = 0.05;
   private static final int REVOLT_STANDING = -40;
   private static final int RELEASED_STANDING = 20;
   /** The player's weekly tribute to their overlord, in silver. */
   public static final int FEALTY_TRIBUTE = 100;
   private static final int MISSED_STANDING = -10;
   private static final int MAX_MISSED = 2;
   private static final int RENOUNCED_STANDING = -40;
   /** How much less likely a faction is to attack a player under another's protection. */
   public static final double PROTECTION = 0.3;
   /** A faction with this many settlements or more is too proud to bow; one with fewer is too weak to protect. */
   public static final int PROUD_SETTLEMENTS = 25;
   public static final int PROTECTOR_SETTLEMENTS = 15;
   /** Villages a player needs to be taken for a lord. */
   public static final int LORD_VILLAGES = 2;

   private static final class Vassal {
      int discontent;
      long since;
   }

   private static final class Fealty {
      String faction;
      long due;
      int missed;
   }

   private final Map<UUID, Map<String, Vassal>> vassals = new HashMap<>();
   private final Map<UUID, Fealty> overlords = new HashMap<>();
   /** Tribute owed to players who were away when it was paid, in silver. */
   private final Map<UUID, Integer> owed = new HashMap<>();

   public static FactionVassals get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionVassals::load, FactionVassals::new, DATA_NAME);
   }

   public static boolean isVassal(ServerLevel level, UUID player, String faction) {
      return get(level).vassals.getOrDefault(player, Map.of()).containsKey(faction);
   }

   /** The faction the player swore fealty to, or null. */
   public static String overlord(ServerLevel level, UUID player) {
      Fealty fealty = get(level).overlords.get(player);
      return fealty == null ? null : fealty.faction;
   }

   static void addVassal(ServerLevel level, ServerPlayer player, String faction) {
      FactionVassals vassals = get(level);
      Vassal vassal = new Vassal();
      vassal.since = FactionWorldData.get(level).lastSimulatedDay();
      vassals.vassals.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>()).put(faction, vassal);
      vassals.setDirty();
      FactionStandingData.get(level).setTreaty(player.getUUID(), faction, RelationState.ALLIANCE);
      FactionCoalitions.peace(level, player.getUUID(), faction);
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.vassal_sworn", FactionNews.factionName(faction), player.getDisplayName()));
   }

   static void swear(ServerLevel level, ServerPlayer player, String faction) {
      FactionVassals vassals = get(level);
      Fealty fealty = new Fealty();
      fealty.faction = faction;
      fealty.due = FactionWorldData.get(level).lastSimulatedDay() + WEEK_DAYS;
      vassals.overlords.put(player.getUUID(), fealty);
      vassals.setDirty();
      FactionStandingData.get(level).setTreaty(player.getUUID(), faction, RelationState.ALLIANCE);
      FactionCoalitions.peace(level, player.getUUID(), faction);
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.fealty_sworn", player.getDisplayName(), FactionNews.factionName(faction)));
   }

   /** A gift to a vassal soothes it. */
   static void onGift(ServerLevel level, UUID player, String faction) {
      Vassal vassal = get(level).vassals.getOrDefault(player, Map.of()).get(faction);
      if (vassal != null) {
         vassal.discontent = Math.max(0, vassal.discontent - GIFT_DISCONTENT);
         get(level).setDirty();
      }
   }

   /** A line for the diplomacy window about the bond with this faction, or null. */
   static Component line(ServerLevel level, UUID player, String faction) {
      FactionVassals vassals = get(level);
      Vassal vassal = vassals.vassals.getOrDefault(player, Map.of()).get(faction);
      if (vassal != null) {
         return Component.translatable("vassal.ancientwarfare.line.vassal", vassal.discontent, MAX_DISCONTENT)
            .withStyle(vassal.discontent >= RESTLESS ? ChatFormatting.RED : ChatFormatting.GREEN);
      }

      Fealty fealty = vassals.overlords.get(player);
      if (fealty != null && fealty.faction.equals(faction)) {
         long days = Math.max(0L, fealty.due - FactionWorldData.get(level).lastSimulatedDay());
         return Component.translatable("vassal.ancientwarfare.line.overlord", FEALTY_TRIBUTE / 10, days).withStyle(ChatFormatting.GOLD);
      }

      return null;
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionVassals vassals = get(level);
      FactionStandingData standings = FactionStandingData.get(level);
      long day = data.lastSimulatedDay();
      for (Map.Entry<UUID, Map<String, Vassal>> entry : vassals.vassals.entrySet()) {
         UUID player = entry.getKey();
         ServerPlayer online = level.getServer().getPlayerList().getPlayer(player);
         int tribute = 0;
         for (Map.Entry<String, Vassal> bond : new java.util.ArrayList<>(entry.getValue().entrySet())) {
            String faction = bond.getKey();
            Vassal vassal = bond.getValue();
            if (standings.getTreaty(player, faction) != RelationState.ALLIANCE || data.settlements().stream().noneMatch(s -> s.faction().equals(faction))) {
               // The player broke with it, or it is gone.
               entry.getValue().remove(faction);
               FactionNews.tellPlayer(level, player, Component.translatable("vassal.ancientwarfare.lost", FactionNews.factionName(faction)).withStyle(ChatFormatting.GRAY));
               continue;
            }

            int before = vassal.discontent;
            vassal.discontent = Math.min(MAX_DISCONTENT, vassal.discontent + (standings.getStanding(player, faction) >= FOND_STANDING ? DAILY_DISCONTENT / 2 : DAILY_DISCONTENT));
            if (vassal.discontent >= MAX_DISCONTENT || vassal.discontent >= RESTLESS && level.getRandom().nextDouble() < RESTLESS_REVOLT) {
               entry.getValue().remove(faction);
               standings.setTreaty(player, faction, RelationState.WAR);
               standings.setStanding(player, faction, Math.min(standings.getStanding(player, faction), REVOLT_STANDING));
               Component who = online != null ? online.getDisplayName() : Component.literal("?");
               FactionNews.tell(level, Component.translatable("news.ancientwarfare.vassal_revolt", FactionNews.factionName(faction), who));
               CountryDiplomacy.warDeclaredOn(level, player, faction);
               continue;
            }

            if (vassal.discontent >= RESTLESS && before < RESTLESS) {
               FactionNews.tellPlayer(level, player, Component.translatable("vassal.ancientwarfare.restless", FactionNews.factionName(faction)).withStyle(ChatFormatting.RED));
            }

            if ((day - vassal.since) % WEEK_DAYS == 0L && day > vassal.since) {
               int pay = Math.max(0, Math.min(data.treasury(faction), Math.max(MIN_TRIBUTE, Math.min(MAX_TRIBUTE, data.treasury(faction) / TRIBUTE_SHARE))));
               data.setTreasury(faction, data.treasury(faction) - pay);
               tribute += pay;
            }
         }

         int due = tribute + vassals.owed.getOrDefault(player, 0);
         if (due > 0 && online != null) {
            Coins.give(online, due);
            vassals.owed.remove(player);
            online.sendSystemMessage(Component.translatable("vassal.ancientwarfare.tribute_in", due / 10, due % 10).withStyle(ChatFormatting.GOLD));
         } else if (due > 0) {
            vassals.owed.put(player, due);
         }
      }

      vassals.vassals.values().removeIf(Map::isEmpty);
      for (Map.Entry<UUID, Fealty> entry : new java.util.ArrayList<>(vassals.overlords.entrySet())) {
         UUID player = entry.getKey();
         Fealty fealty = entry.getValue();
         if (standings.getTreaty(player, fealty.faction) != RelationState.ALLIANCE) {
            vassals.overlords.remove(player);
            FactionNews.tellPlayer(level, player, Component.translatable("vassal.ancientwarfare.fealty_over", FactionNews.factionName(fealty.faction)).withStyle(ChatFormatting.GRAY));
            continue;
         }

         if (day < fealty.due) {
            continue;
         }

         ServerPlayer online = level.getServer().getPlayerList().getPlayer(player);
         fealty.due += WEEK_DAYS;
         if (online != null && Coins.take(online, FEALTY_TRIBUTE)) {
            fealty.missed = 0;
            data.setTreasury(fealty.faction, data.treasury(fealty.faction) + FEALTY_TRIBUTE);
            online.sendSystemMessage(Component.translatable("vassal.ancientwarfare.tribute_out", FEALTY_TRIBUTE / 10, FactionNews.factionName(fealty.faction)).withStyle(ChatFormatting.GRAY));
         } else if (++fealty.missed >= MAX_MISSED) {
            vassals.overlords.remove(player);
            standings.setTreaty(player, fealty.faction, RelationState.NEUTRAL);
            standings.adjustStanding(player, fealty.faction, MISSED_STANDING * 2);
            FactionNews.tellPlayer(level, player, Component.translatable("vassal.ancientwarfare.cast_off", FactionNews.factionName(fealty.faction)).withStyle(ChatFormatting.RED));
         } else {
            standings.adjustStanding(player, fealty.faction, MISSED_STANDING);
            FactionNews.tellPlayer(
               level, player, Component.translatable("vassal.ancientwarfare.missed", FactionNews.factionName(fealty.faction), FEALTY_TRIBUTE / 10).withStyle(ChatFormatting.RED)
            );
         }
      }

      vassals.setDirty();
      data.setDirty();
   }

   /** The player sets a vassal free: it stays an ally and thinks better of them. */
   public static void release(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      FactionVassals vassals = get(level);
      Map<String, Vassal> mine = vassals.vassals.get(player.getUUID());
      if (mine == null || mine.remove(faction) == null) {
         player.sendSystemMessage(Component.translatable("vassal.ancientwarfare.not_vassal").withStyle(ChatFormatting.GRAY));
         return;
      }

      vassals.setDirty();
      FactionStandingData.get(level).adjustStanding(player.getUUID(), faction, RELEASED_STANDING);
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.vassal_released", player.getDisplayName(), FactionNews.factionName(faction)));
   }

   /** The player renounces their fealty: the overlord takes it as betrayal and goes to war. */
   public static void renounce(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      FactionVassals vassals = get(level);
      Fealty fealty = vassals.overlords.remove(player.getUUID());
      if (fealty == null) {
         player.sendSystemMessage(Component.translatable("vassal.ancientwarfare.no_overlord").withStyle(ChatFormatting.GRAY));
         return;
      }

      vassals.setDirty();
      FactionStandingData standings = FactionStandingData.get(level);
      standings.setTreaty(player.getUUID(), fealty.faction, RelationState.WAR);
      standings.setStanding(player.getUUID(), fealty.faction, Math.min(standings.getStanding(player.getUUID(), fealty.faction), 0) + RENOUNCED_STANDING);
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.fealty_renounced", player.getDisplayName(), FactionNews.factionName(fealty.faction)));
      CountryDiplomacy.warDeclaredOn(level, player.getUUID(), fealty.faction);
   }

   /** Lists the player's vassals and overlord, with what each costs or brings. */
   public static void show(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      FactionVassals vassals = get(level);
      Map<String, Vassal> mine = vassals.vassals.getOrDefault(player.getUUID(), Map.of());
      Fealty fealty = vassals.overlords.get(player.getUUID());
      if (mine.isEmpty() && fealty == null) {
         player.sendSystemMessage(Component.translatable("vassal.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      }

      mine.forEach((faction, vassal) -> player.sendSystemMessage(
         FactionNews.factionName(faction).copy().append(": ").append(line(level, player.getUUID(), faction)).append("  ")
            .append(FactionEnvoys.button("vassal.ancientwarfare.release", "/awvassal release " + faction, ChatFormatting.AQUA))
      ));
      if (fealty != null) {
         player.sendSystemMessage(
            FactionNews.factionName(fealty.faction).copy().append(": ").append(line(level, player.getUUID(), fealty.faction)).append("  ")
               .append(FactionEnvoys.button("vassal.ancientwarfare.renounce", "/awvassal renounce", ChatFormatting.RED))
         );
      }
   }

   /** Test hook: sets a vassal's discontent. */
   public static boolean setDiscontent(ServerLevel level, UUID player, String faction, int discontent) {
      Vassal vassal = get(level).vassals.getOrDefault(player, Map.of()).get(faction);
      if (vassal == null) {
         return false;
      }

      vassal.discontent = discontent;
      get(level).setDirty();
      return true;
   }

   private static FactionVassals load(CompoundTag tag) {
      FactionVassals vassals = new FactionVassals();
      for (Tag element : tag.getList("Vassals", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Vassal vassal = new Vassal();
         vassal.discontent = entry.getInt("Discontent");
         vassal.since = entry.getLong("Since");
         vassals.vassals.computeIfAbsent(entry.getUUID("Player"), id -> new LinkedHashMap<>()).put(entry.getString("Faction"), vassal);
      }

      for (Tag element : tag.getList("Overlords", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Fealty fealty = new Fealty();
         fealty.faction = entry.getString("Faction");
         fealty.due = entry.getLong("Due");
         fealty.missed = entry.getInt("Missed");
         vassals.overlords.put(entry.getUUID("Player"), fealty);
      }

      for (Tag element : tag.getList("Owed", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         vassals.owed.put(entry.getUUID("Player"), entry.getInt("Silver"));
      }

      return vassals;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      this.vassals.forEach((player, mine) -> mine.forEach((faction, vassal) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Player", player);
         entry.putString("Faction", faction);
         entry.putInt("Discontent", vassal.discontent);
         entry.putLong("Since", vassal.since);
         list.add(entry);
      }));
      tag.put("Vassals", list);
      ListTag overlordList = new ListTag();
      this.overlords.forEach((player, fealty) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Player", player);
         entry.putString("Faction", fealty.faction);
         entry.putLong("Due", fealty.due);
         entry.putInt("Missed", fealty.missed);
         overlordList.add(entry);
      });
      tag.put("Overlords", overlordList);
      ListTag owedList = new ListTag();
      this.owed.forEach((player, silver) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Player", player);
         entry.putInt("Silver", silver);
         owedList.add(entry);
      });
      tag.put("Owed", owedList);
      return tag;
   }
}
