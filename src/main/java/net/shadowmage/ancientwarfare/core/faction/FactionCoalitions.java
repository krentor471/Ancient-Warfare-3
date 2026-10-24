package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * Coalitions. A player who grows too fast, holding many villages or taking settlement after
 * settlement, frightens their neighbours: those who do not count themselves the player's friends
 * band together, and after a few days of warning go to war with the player all at once. The player
 * can break the coalition apart before or during the war: pay a member to stand aside, sow discord
 * with a forged letter, make peace with its members one by one, or simply stop growing.
 */
public final class FactionCoalitions extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_coalitions";
   /** Threat at which the neighbours band together: two per village, three per recent conquest. */
   public static final int THREAT_LIMIT = 10;
   private static final int VILLAGE_THREAT = 2;
   private static final int CONQUEST_THREAT = 3;
   /** Conquests older than this are forgotten. */
   private static final long CONQUEST_MEMORY_DAYS = 20L;
   private static final long WARNING_DAYS = 3L;
   /** A coalition at war longer than this tires and falls apart. */
   private static final long WAR_DAYS = 20L;
   private static final long COOLDOWN_DAYS = 15L;
   private static final int FRIEND_STANDING = 30;
   private static final double JOIN_CHANCE = 0.7;
   private static final int MIN_MEMBERS = 2;
   /** Price, in silver, of a member's leaving before the war; doubled once it has begun. */
   public static final int BRIBE = 300;
   private static final int BRIBE_STANDING = 5;
   private static final int WAR_STANDING = -40;

   private static final class Coalition {
      final Set<String> members = new LinkedHashSet<>();
      long warDay;
      boolean atWar;
      /** Called by the council of factions: it does not calm down when the player stops growing. */
      boolean proclaimed;
   }

   private final Map<UUID, Coalition> coalitions = new HashMap<>();
   private final Map<UUID, List<Long>> conquests = new HashMap<>();
   private final Map<UUID, Long> cooldown = new HashMap<>();

   public static FactionCoalitions get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionCoalitions::load, FactionCoalitions::new, DATA_NAME);
   }

   /** The player took a settlement: annexed, ransomed, plundered or razed it. */
   public static void onConquest(ServerLevel level, UUID player) {
      FactionCoalitions coalitions = get(level);
      coalitions.conquests.computeIfAbsent(player, id -> new ArrayList<>()).add(FactionWorldData.get(level).lastSimulatedDay());
      coalitions.setDirty();
   }

   public static int threat(ServerLevel level, FactionWorldData data, UUID player) {
      long day = data.lastSimulatedDay();
      int villages = (int)data.villages().values().stream().filter(player::equals).count();
      long recent = get(level).conquests.getOrDefault(player, List.of()).stream().filter(when -> day - when <= CONQUEST_MEMORY_DAYS).count();
      return villages * VILLAGE_THREAT + (int)recent * CONQUEST_THREAT;
   }

   /** A line for the diplomacy window when the faction is in a coalition against the player, else null. */
   static Component line(ServerLevel level, UUID player, String faction) {
      Coalition coalition = get(level).coalitions.get(player);
      if (coalition == null || !coalition.members.contains(faction)) {
         return null;
      }

      if (coalition.atWar) {
         return Component.translatable("coalition.ancientwarfare.line.war", coalition.members.size());
      }

      long days = Math.max(0L, coalition.warDay - FactionWorldData.get(level).lastSimulatedDay());
      return Component.translatable("coalition.ancientwarfare.line.warning", coalition.members.size(), days);
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionCoalitions coalitions = get(level);
      long day = data.lastSimulatedDay();
      coalitions.conquests.values().forEach(list -> list.removeIf(when -> day - when > CONQUEST_MEMORY_DAYS));
      coalitions.conquests.values().removeIf(List::isEmpty);
      Set<UUID> players = new LinkedHashSet<>(data.villages().values());
      players.addAll(coalitions.conquests.keySet());
      players.addAll(coalitions.coalitions.keySet());
      for (UUID player : players) {
         Coalition coalition = coalitions.coalitions.get(player);
         int threat = threat(level, data, player);
         if (coalition == null) {
            if (threat >= THREAT_LIMIT && day >= coalitions.cooldown.getOrDefault(player, 0L)) {
               coalitions.form(level, player, day, null);
            }
         } else if (!coalition.atWar) {
            if (threat < THREAT_LIMIT * 7 / 10 && !coalition.proclaimed) {
               coalitions.dissolve(level, player, "calmed");
            } else if (day >= coalition.warDay) {
               coalitions.declare(level, player, coalition);
            }
         } else {
            FactionStandingData standings = FactionStandingData.get(level);
            coalition.members.removeIf(member -> standings.getTreaty(player, member) != RelationState.WAR);
            if (coalition.members.size() < MIN_MEMBERS) {
               coalitions.dissolve(level, player, "broken");
            } else if (day - coalition.warDay > WAR_DAYS) {
               coalitions.dissolve(level, player, "tired");
            }
         }
      }

      coalitions.setDirty();
   }

   /** The council of factions named the player the common enemy: those who voted for it band together. */
   static void proclaim(ServerLevel level, UUID player, Set<String> members) {
      FactionCoalitions coalitions = get(level);
      Coalition coalition = coalitions.coalitions.get(player);
      if (coalition != null) {
         coalition.members.addAll(members);
         coalition.proclaimed = true;
         coalitions.setDirty();
         if (coalition.atWar) {
            // The newcomers go to war at once.
            coalitions.declare(level, player, coalition);
         } else {
            ServerPlayer online = level.getServer().getPlayerList().getPlayer(player);
            FactionNews.tell(level, Component.translatable("news.ancientwarfare.coalition_formed", names(coalition.members), online != null ? online.getDisplayName() : Component.literal("?")));
         }
      } else {
         coalitions.form(level, player, FactionWorldData.get(level).lastSimulatedDay(), members);
      }
   }

   private void form(ServerLevel level, UUID player, long day, Set<String> preset) {
      FactionStandingData standings = FactionStandingData.get(level);
      Coalition coalition = new Coalition();
      coalition.proclaimed = preset != null;
      if (preset != null) {
         coalition.members.addAll(preset);
      }

      for (String faction : preset != null ? List.<String>of() : standings.knownFactions(player)) {
         RelationState treaty = standings.getTreaty(player, faction);
         if (FactionDiplomacy.isOutlaw(faction) || treaty == RelationState.ALLIANCE || treaty == RelationState.PACT || FactionFamilies.isKin(level, player, faction)) {
            continue;
         }

         if (treaty == RelationState.WAR || standings.getStanding(player, faction) < FRIEND_STANDING && level.getRandom().nextDouble() < JOIN_CHANCE) {
            coalition.members.add(faction);
         }
      }

      if (coalition.members.size() < MIN_MEMBERS) {
         return;
      }

      coalition.warDay = day + WARNING_DAYS;
      this.coalitions.put(player, coalition);
      this.setDirty();
      ServerPlayer online = level.getServer().getPlayerList().getPlayer(player);
      Component who = online != null ? online.getDisplayName() : Component.literal("?");
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.coalition_formed", names(coalition.members), who));
      if (online != null) {
         online.sendSystemMessage(Component.translatable("coalition.ancientwarfare.formed", names(coalition.members), WARNING_DAYS).withStyle(ChatFormatting.GOLD));
         show(online);
      }
   }

   private void declare(ServerLevel level, UUID player, Coalition coalition) {
      FactionStandingData standings = FactionStandingData.get(level);
      coalition.atWar = true;
      // Those who made friends with the player meanwhile stand aside.
      coalition.members.removeIf(member -> standings.getTreaty(player, member) == RelationState.PACT || standings.getTreaty(player, member) == RelationState.ALLIANCE);
      if (coalition.members.size() < MIN_MEMBERS) {
         this.dissolve(level, player, "broken");
         return;
      }

      for (String member : coalition.members) {
         if (standings.getTreaty(player, member) != RelationState.WAR) {
            standings.setTreaty(player, member, RelationState.WAR);
            standings.discover(player, member);
            CountryDiplomacy.warDeclaredOn(level, player, member);
         }

         standings.setStanding(player, member, Math.min(standings.getStanding(player, member), WAR_STANDING));
      }

      ServerPlayer online = level.getServer().getPlayerList().getPlayer(player);
      Component who = online != null ? online.getDisplayName() : Component.literal("?");
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.coalition_war", names(coalition.members), who));
   }

   private void dissolve(ServerLevel level, UUID player, String why) {
      if (this.coalitions.remove(player) == null) {
         return;
      }

      this.cooldown.put(player, FactionWorldData.get(level).lastSimulatedDay() + COOLDOWN_DAYS);
      this.setDirty();
      FactionNews.tellPlayer(level, player, Component.translatable("coalition.ancientwarfare.dissolved." + why).withStyle(ChatFormatting.GREEN));
   }

   /** A member leaves; with too few left, the coalition falls apart. */
   private void leave(ServerLevel level, UUID player, String faction) {
      Coalition coalition = this.coalitions.get(player);
      if (coalition != null && coalition.members.remove(faction)) {
         this.setDirty();
         if (coalition.members.size() < MIN_MEMBERS) {
            this.dissolve(level, player, "broken");
         }
      }
   }

   /** The player's forged letter turned a member against its allies: it quits the coalition. */
   static void discord(ServerLevel level, UUID player, String faction) {
      FactionCoalitions coalitions = get(level);
      Coalition coalition = coalitions.coalitions.get(player);
      if (coalition != null && coalition.members.contains(faction)) {
         FactionNews.tellPlayer(level, player, Component.translatable("coalition.ancientwarfare.discord", FactionNews.factionName(faction)).withStyle(ChatFormatting.AQUA));
         coalitions.leave(level, player, faction);
      }
   }

   /** The player made peace with a member on their own. */
   static void peace(ServerLevel level, UUID player, String faction) {
      get(level).leave(level, player, faction);
   }

   /** Pays a member to stand aside; once at war, it also makes peace. */
   public static void bribe(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      FactionCoalitions coalitions = get(level);
      Coalition coalition = coalitions.coalitions.get(player.getUUID());
      if (coalition == null || !coalition.members.contains(faction)) {
         player.sendSystemMessage(Component.translatable("coalition.ancientwarfare.not_member").withStyle(ChatFormatting.GRAY));
         return;
      }

      int price = price(coalition);
      if (!Coins.take(player, price)) {
         player.sendSystemMessage(Component.translatable("coalition.ancientwarfare.no_money", price / 10).withStyle(ChatFormatting.RED));
         return;
      }

      FactionWorldData data = FactionWorldData.get(level);
      data.setTreasury(faction, data.treasury(faction) + price);
      FactionStandingData standings = FactionStandingData.get(level);
      if (coalition.atWar) {
         standings.setTreaty(player.getUUID(), faction, RelationState.NEUTRAL);
      }

      standings.adjustStanding(player.getUUID(), faction, BRIBE_STANDING);
      player.sendSystemMessage(Component.translatable("coalition.ancientwarfare.bribed", FactionNews.factionName(faction)).withStyle(ChatFormatting.GREEN));
      coalitions.leave(level, player.getUUID(), faction);
   }

   private static int price(Coalition coalition) {
      return coalition.atWar ? BRIBE * 2 : BRIBE;
   }

   /** Tells the player about the coalition against them, with a button to buy off each member. */
   public static void show(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Coalition coalition = get(level).coalitions.get(player.getUUID());
      if (coalition == null) {
         int threat = threat(level, FactionWorldData.get(level), player.getUUID());
         player.sendSystemMessage(Component.translatable("coalition.ancientwarfare.none", threat, THREAT_LIMIT).withStyle(ChatFormatting.GRAY));
         return;
      }

      player.sendSystemMessage(line(level, player.getUUID(), coalition.members.iterator().next()).copy().withStyle(ChatFormatting.GOLD));
      int price = price(coalition);
      for (String member : coalition.members) {
         Component label = Component.translatable("coalition.ancientwarfare.bribe", price / 10);
         MutableComponent button = Component.literal("[")
            .append(label)
            .append("]")
            .withStyle(
               style -> style.withColor(ChatFormatting.YELLOW)
                  .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/awcoalition bribe " + member))
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("coalition.ancientwarfare.bribe.hint")))
            );
         player.sendSystemMessage(Component.literal("  ").append(FactionNews.factionName(member)).append("  ").append(button));
      }

      player.sendSystemMessage(Component.translatable("coalition.ancientwarfare.hint").withStyle(ChatFormatting.GRAY));
   }

   private static Component names(Set<String> members) {
      MutableComponent list = Component.empty();
      boolean first = true;
      for (String member : members) {
         if (!first) {
            list.append(", ");
         }

         list.append(FactionNews.factionName(member));
         first = false;
      }

      return list;
   }

   /** Test hook: raises the player's threat by pretending they took this many settlements today. */
   public static void forceConquests(ServerLevel level, UUID player, int count) {
      for (int i = 0; i < count; i++) {
         onConquest(level, player);
      }

      get(level).cooldown.remove(player);
   }

   private static FactionCoalitions load(CompoundTag tag) {
      FactionCoalitions coalitions = new FactionCoalitions();
      for (Tag element : tag.getList("Coalitions", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Coalition coalition = new Coalition();
         entry.getList("Members", Tag.TAG_STRING).forEach(member -> coalition.members.add(member.getAsString()));
         coalition.warDay = entry.getLong("WarDay");
         coalition.atWar = entry.getBoolean("AtWar");
         coalition.proclaimed = entry.getBoolean("Proclaimed");
         coalitions.coalitions.put(entry.getUUID("Player"), coalition);
      }

      for (Tag element : tag.getList("Conquests", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         List<Long> days = new ArrayList<>();
         for (long day : entry.getLongArray("Days")) {
            days.add(day);
         }

         coalitions.conquests.put(entry.getUUID("Player"), days);
      }

      for (Tag element : tag.getList("Cooldown", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         coalitions.cooldown.put(entry.getUUID("Player"), entry.getLong("Until"));
      }

      return coalitions;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      this.coalitions.forEach((player, coalition) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Player", player);
         ListTag members = new ListTag();
         coalition.members.forEach(member -> members.add(StringTag.valueOf(member)));
         entry.put("Members", members);
         entry.putLong("WarDay", coalition.warDay);
         entry.putBoolean("AtWar", coalition.atWar);
         entry.putBoolean("Proclaimed", coalition.proclaimed);
         list.add(entry);
      });
      tag.put("Coalitions", list);
      ListTag conquestList = new ListTag();
      this.conquests.forEach((player, days) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Player", player);
         entry.putLongArray("Days", days.stream().mapToLong(Long::longValue).toArray());
         conquestList.add(entry);
      });
      tag.put("Conquests", conquestList);
      ListTag cooldownList = new ListTag();
      this.cooldown.forEach((player, until) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Player", player);
         entry.putLong("Until", until);
         cooldownList.add(entry);
      });
      tag.put("Cooldown", cooldownList);
      return tag;
   }
}
