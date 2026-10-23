package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Rulers as people among people. Two rulers may grow to be friends or rivals, as their characters
 * meet; friends seldom war with each other and think better of each other by the day, rivals the
 * reverse. A rich ruler now and then holds a feast in his capital and bids the players he knows; a
 * guest who comes is liked the better, may win the ruler's friendship or, after a brawl, his
 * enmity, and may overhear a secret. A guest who said he would come and did not is remembered.
 * Friendships and rivalries end with the ruler who held them.
 */
public final class FactionFeasts extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_feasts";
   private static final double BOND_CHANCE = 0.01;
   private static final int MAX_BONDS = 2;
   private static final double FEAST_CHANCE = 0.04;
   private static final int FEAST_GOLD = 500;
   private static final int FEAST_COST = 100;
   private static final int FEAST_RANGE = 64;
   /** A ruler bids even those he thinks a little ill of: a feast mends things. */
   private static final int INVITE_STANDING = -20;
   private static final int GUEST_STANDING = 10;
   private static final int TOAST_STANDING = 5;
   private static final int BRAWL_STANDING = -8;
   private static final int SNUB_STANDING = -10;
   private static final double FRIEND_CHANCE = 0.3;
   private static final double BRAWL_CHANCE = 0.15;
   private static final double SECRET_CHANCE = 0.2;
   private static final int CHECK_TICKS = 100;
   /** How much a friendship or rivalry weighs on war between the two. */
   private static final double FRIEND_WAR = 0.3;
   private static final double RIVAL_WAR = 2.0;
   private static final double FRIEND_GIFT = 1.25;
   private static final double RIVAL_GIFT = 0.5;

   public static final int FRIEND = 1;
   public static final int RIVAL = -1;

   /** A bond between two rulers, each known by FactionRulers.key; only while both still rule. */
   private record Bond(String a, String rulerA, String b, String rulerB, int kind) {
      boolean holds(ServerLevel level) {
         FactionWorldData data = FactionWorldData.get(level);
         return FactionRulers.key(FactionRulers.ruler(data, this.a, level.getRandom())).equals(this.rulerA)
            && FactionRulers.key(FactionRulers.ruler(data, this.b, level.getRandom())).equals(this.rulerB);
      }

      String other(String faction) {
         return faction.equals(this.a) ? this.b : faction.equals(this.b) ? this.a : null;
      }
   }

   /** A player's friendship or rivalry with one ruler. */
   private record PlayerBond(String ruler, int kind) {
   }

   private static final class Feast {
      final String faction;
      /** The day of the feast itself, the day after it is proclaimed. */
      final long day;
      final Set<UUID> coming = new HashSet<>();
      final Set<UUID> came = new HashSet<>();

      Feast(String faction, long day) {
         this.faction = faction;
         this.day = day;
      }
   }

   private final List<Bond> bonds = new ArrayList<>();
   private final Map<UUID, Map<String, PlayerBond>> playerBonds = new HashMap<>();
   private final Map<String, Feast> feasts = new HashMap<>();

   public static FactionFeasts get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionFeasts::load, FactionFeasts::new, DATA_NAME);
   }

   // ---------------------------------------------------------------- rulers

   /** Whether two characters draw together, push apart, or neither. */
   static int affinity(int one, int two) {
      int score = 0;
      for (FactionPeople.Character mine : FactionPeople.traits(one)) {
         for (FactionPeople.Character theirs : FactionPeople.traits(two)) {
            if (mine == theirs) {
               // Two of a kind get on, save two who both want more than they have.
               score += mine == FactionPeople.Character.AMBITIOUS || mine == FactionPeople.Character.PARANOID ? -1 : 1;
            } else if (mine.opposite() == theirs) {
               score -= 1;
            }
         }
      }

      return score;
   }

   private static int bondsOf(List<Bond> bonds, String faction) {
      return (int)bonds.stream().filter(bond -> bond.other(faction) != null).count();
   }

   /** Between two factions: FRIEND, RIVAL, or 0. */
   public static int bond(ServerLevel level, String faction, String other) {
      for (Bond bond : get(level).bonds) {
         if (other.equals(bond.other(faction))) {
            return bond.kind;
         }
      }

      return 0;
   }

   /** Times the usual chance of war between the two factions, by their rulers' bond. */
   static double warFactor(ServerLevel level, String faction, String other) {
      int bond = bond(level, faction, other);
      return bond == FRIEND ? FRIEND_WAR : bond == RIVAL ? RIVAL_WAR : 1.0;
   }

   // ---------------------------------------------------------------- players

   /** The ruler's bond with the player: FRIEND, RIVAL, or 0. */
   public static int playerBond(ServerLevel level, UUID player, String faction) {
      PlayerBond bond = get(level).playerBonds.getOrDefault(player, Map.of()).get(faction);
      if (bond == null || !bond.ruler.equals(FactionRulers.key(FactionRulers.ruler(FactionWorldData.get(level), faction, level.getRandom())))) {
         return 0;
      }

      return bond.kind;
   }

   static double playerWarFactor(ServerLevel level, UUID player, String faction) {
      int bond = playerBond(level, player, faction);
      return bond == FRIEND ? FRIEND_WAR : bond == RIVAL ? RIVAL_WAR : 1.0;
   }

   static double giftFactor(ServerLevel level, UUID player, String faction) {
      int bond = playerBond(level, player, faction);
      return bond == FRIEND ? FRIEND_GIFT : bond == RIVAL ? RIVAL_GIFT : 1.0;
   }

   private void setPlayerBond(ServerLevel level, UUID player, String faction, int kind) {
      String ruler = FactionRulers.key(FactionRulers.ruler(FactionWorldData.get(level), faction, level.getRandom()));
      this.playerBonds.computeIfAbsent(player, ignored -> new HashMap<>()).put(faction, new PlayerBond(ruler, kind));
      this.setDirty();
   }

   /** Makes the player the ruler's friend or rival. */
   static void befriend(ServerLevel level, UUID player, String faction, int kind) {
      get(level).setPlayerBond(level, player, faction, kind);
   }

   /** The overview's lines: the ruler's friends and rivals among rulers, and his bond with the player. */
   static List<Component> lines(ServerLevel level, UUID player, String faction) {
      FactionFeasts feasts = get(level);
      List<Component> lines = new ArrayList<>();
      int mine = playerBond(level, player, faction);
      if (mine != 0) {
         lines.add(Component.translatable(mine == FRIEND ? "feast.ancientwarfare.line.your_friend" : "feast.ancientwarfare.line.your_rival").withStyle(mine == FRIEND ? ChatFormatting.GREEN : ChatFormatting.RED));
      }

      List<Component> friends = new ArrayList<>();
      List<Component> rivals = new ArrayList<>();
      for (Bond bond : feasts.bonds) {
         String other = bond.other(faction);
         if (other != null) {
            (bond.kind == FRIEND ? friends : rivals).add(FactionNews.factionName(other));
         }
      }

      if (!friends.isEmpty() || !rivals.isEmpty()) {
         MutableComponent line = Component.empty();
         if (!friends.isEmpty()) {
            line.append(Component.translatable("feast.ancientwarfare.line.friends", join(friends)).withStyle(ChatFormatting.GREEN));
         }

         if (!rivals.isEmpty()) {
            if (!friends.isEmpty()) {
               line.append(Component.literal("; ").withStyle(ChatFormatting.GRAY));
            }

            line.append(Component.translatable(friends.isEmpty() ? "feast.ancientwarfare.line.rivals_only" : "feast.ancientwarfare.line.rivals", join(rivals)).withStyle(ChatFormatting.RED));
         }

         lines.add(line);
      }

      Feast feast = feasts.feasts.get(faction);
      if (feast != null) {
         String key = feast.came.contains(player) ? "feast.ancientwarfare.line.came" : feast.coming.contains(player) ? "feast.ancientwarfare.line.coming" : "feast.ancientwarfare.line.feast";
         lines.add(Component.translatable(key, capitalName(level, faction)).withStyle(ChatFormatting.GOLD));
      }

      return lines;
   }

   private static Component join(List<Component> names) {
      MutableComponent joined = Component.empty();
      for (int i = 0; i < names.size(); i++) {
         if (i > 0) {
            joined.append(", ");
         }

         joined.append(names.get(i));
      }

      return joined;
   }

   static FactionWorldData.Settlement capital(FactionWorldData data, String faction) {
      return data.settlements().stream().filter(settlement -> settlement.faction().equals(faction) && FactionTowns.CAPITAL.equals(settlement.kind())).findFirst().orElse(null);
   }

   static Component capitalName(ServerLevel level, String faction) {
      FactionWorldData.Settlement capital = capital(FactionWorldData.get(level), faction);
      return capital == null || capital.name().isEmpty() ? FactionNews.factionName(faction) : capital.nameComponent();
   }

   // ---------------------------------------------------------------- days

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionFeasts feasts = get(level);
      long day = Math.max(0L, data.lastSimulatedDay());
      RandomSource random = level.getRandom();
      feasts.bonds.removeIf(bond -> !bond.holds(level));
      Set<String> factions = new TreeSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            factions.add(settlement.faction());
         }
      }

      // Rulers warm to each other or fall out, as their characters meet.
      List<String> list = new ArrayList<>(factions);
      for (int i = 0; i < list.size(); i++) {
         for (int j = i + 1; j < list.size(); j++) {
            String a = list.get(i);
            String b = list.get(j);
            int bond = bond(level, a, b);
            if (bond != 0) {
               FactionDiplomacy.adjustOpinion(level, a, b, bond);
               FactionDiplomacy.adjustOpinion(level, b, a, bond);
               continue;
            }

            if (bondsOf(feasts.bonds, a) >= MAX_BONDS || bondsOf(feasts.bonds, b) >= MAX_BONDS || random.nextDouble() >= BOND_CHANCE) {
               continue;
            }

            FactionWorldData.Ruler rulerA = FactionRulers.ruler(data, a, random);
            FactionWorldData.Ruler rulerB = FactionRulers.ruler(data, b, random);
            int affinity = affinity(rulerA.character(), rulerB.character());
            if (affinity == 0) {
               continue;
            }

            int kind = affinity > 0 ? FRIEND : RIVAL;
            feasts.bonds.add(new Bond(a, FactionRulers.key(rulerA), b, FactionRulers.key(rulerB), kind));
            FactionNews.tell(
               level,
               Component.translatable(
                  kind == FRIEND ? "news.ancientwarfare.rulers_friends" : "news.ancientwarfare.rulers_rivals",
                  FactionRulers.name(rulerA.name()), FactionNews.factionName(a), FactionRulers.name(rulerB.name()), FactionNews.factionName(b)
               )
            );
         }
      }

      // Last night's feasts are over: who said they would come and did not is remembered.
      feasts.feasts.values().removeIf(feast -> {
         if (day <= feast.day) {
            return false;
         }

         for (UUID guest : feast.coming) {
            if (!feast.came.contains(guest)) {
               FactionStandingData.get(level).adjustStanding(guest, feast.faction, SNUB_STANDING);
               FactionNews.tellPlayer(level, guest, Component.translatable("feast.ancientwarfare.snub", FactionNews.factionName(feast.faction)).withStyle(ChatFormatting.RED));
            }
         }

         return true;
      });
      // A rich ruler proclaims a feast for tomorrow.
      for (String faction : factions) {
         if (!feasts.feasts.containsKey(faction) && !FactionPastimes.away(level, faction) && data.treasury(faction) >= FEAST_GOLD && capital(data, faction) != null && random.nextDouble() < FEAST_CHANCE) {
            feasts.proclaim(level, data, faction, day + 1);
         }
      }

      feasts.setDirty();
   }

   private void proclaim(ServerLevel level, FactionWorldData data, String faction, long day) {
      Feast feast = new Feast(faction, day);
      this.feasts.put(faction, feast);
      data.setTreasury(faction, data.treasury(faction) - FEAST_COST);
      FactionWorldData.Ruler ruler = FactionRulers.ruler(data, faction, level.getRandom());
      FactionStandingData standings = FactionStandingData.get(level);
      for (ServerPlayer player : level.players()) {
         if (!standings.knownFactions(player.getUUID()).contains(faction)
            || standings.getTreaty(player.getUUID(), faction) == RelationState.WAR
            || standings.getStanding(player.getUUID(), faction) < INVITE_STANDING) {
            continue;
         }

         player.sendSystemMessage(
            Component.translatable("feast.ancientwarfare.invite", FactionRulers.name(ruler.name()), FactionNews.factionName(faction), capitalName(level, faction)).withStyle(ChatFormatting.GOLD)
         );
         player.sendSystemMessage(Component.literal("  ").append(FactionEnvoys.button("feast.ancientwarfare.accept", "/awfeast " + faction, ChatFormatting.GREEN)));
      }

      this.setDirty();
   }

   /** The player says he will come. */
   public static void accept(ServerPlayer player, String faction) {
      FactionFeasts feasts = get(player.serverLevel());
      Feast feast = feasts.feasts.get(faction);
      if (feast == null) {
         player.sendSystemMessage(Component.translatable("feast.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      }

      feast.coming.add(player.getUUID());
      feasts.setDirty();
      player.sendSystemMessage(Component.translatable("feast.ancientwarfare.accepted", capitalName(player.serverLevel(), faction)).withStyle(ChatFormatting.GOLD));
   }

   /** On the feast's day, a guest who stands in the capital sits at the ruler's table. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L) {
         return;
      }

      FactionFeasts feasts = get(level);
      long day = data.lastSimulatedDay();
      for (Feast feast : feasts.feasts.values()) {
         if (feast.day != day) {
            continue;
         }

         FactionWorldData.Settlement capital = capital(data, feast.faction);
         if (capital == null) {
            continue;
         }

         for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            if (!feast.came.contains(id) && capital.horizontalDistanceSquared(player.getX(), player.getZ()) < (double)FEAST_RANGE * FEAST_RANGE
               && FactionStandingData.get(level).getTreaty(id, feast.faction) != RelationState.WAR) {
               feast.came.add(id);
               feasts.attend(level, player, feast.faction);
            }
         }
      }
   }

   private void attend(ServerLevel level, ServerPlayer player, String faction) {
      RandomSource random = level.getRandom();
      FactionStandingData standings = FactionStandingData.get(level);
      FactionWorldData.Ruler ruler = FactionRulers.ruler(FactionWorldData.get(level), faction, random);
      Component name = FactionRulers.name(ruler.name());
      standings.adjustStanding(player.getUUID(), faction, GUEST_STANDING);
      player.sendSystemMessage(Component.translatable("feast.ancientwarfare.welcome", name, capitalName(level, faction)).withStyle(ChatFormatting.GOLD));
      // A drunkard's or a cruel ruler's table is the likelier to end in a brawl.
      double brawl = BRAWL_CHANCE * (FactionPeople.has(ruler.character(), FactionPeople.Character.DRUNKARD) ? 2.0 : 1.0)
         * (FactionPeople.has(ruler.character(), FactionPeople.Character.CRUEL) ? 1.5 : 1.0);
      if (random.nextDouble() < brawl) {
         standings.adjustStanding(player.getUUID(), faction, BRAWL_STANDING);
         boolean rival = playerBond(level, player.getUUID(), faction) != FRIEND && random.nextBoolean();
         if (rival) {
            this.setPlayerBond(level, player.getUUID(), faction, RIVAL);
         }

         player.sendSystemMessage(Component.translatable(rival ? "feast.ancientwarfare.brawl_rival" : "feast.ancientwarfare.brawl", name).withStyle(ChatFormatting.RED));
         return;
      }

      standings.adjustStanding(player.getUUID(), faction, TOAST_STANDING);
      player.sendSystemMessage(Component.translatable("feast.ancientwarfare.toast", name).withStyle(ChatFormatting.GREEN));
      if (playerBond(level, player.getUUID(), faction) != FRIEND
         && random.nextDouble() < FRIEND_CHANCE * (FactionPeople.has(ruler.character(), FactionPeople.Character.PARANOID) ? 0.5 : 1.0)) {
         this.setPlayerBond(level, player.getUUID(), faction, FRIEND);
         player.sendSystemMessage(Component.translatable("feast.ancientwarfare.friend", name).withStyle(ChatFormatting.GREEN));
      }

      if (random.nextDouble() < SECRET_CHANCE * FactionPeople.factor(ruler.character(), c -> c.secret)) {
         FactionPlots.overhear(level, player, faction);
      }
   }

   /** For testing: proclaim a feast in the faction's capital for today, or bind two rulers. */
   public static String debug(ServerLevel level, String faction, String what, String other) {
      FactionFeasts feasts = get(level);
      FactionWorldData data = FactionWorldData.get(level);
      switch (what) {
         case "feast" -> {
            if (capital(data, faction) == null) {
               return "No capital";
            }

            feasts.proclaim(level, data, faction, data.lastSimulatedDay());
            FactionWorldData.Settlement capital = capital(data, faction);
            return "Feast at " + capital.center().toShortString();
         }
         case "friend", "rival" -> {
            feasts.bonds.removeIf(bond -> bond.other(faction) != null && bond.other(faction).equals(other));
            feasts.bonds.add(new Bond(
               faction, FactionRulers.key(FactionRulers.ruler(data, faction, level.getRandom())), other, FactionRulers.key(FactionRulers.ruler(data, other, level.getRandom())),
               what.equals("friend") ? FRIEND : RIVAL
            ));
            feasts.setDirty();
            return "Bound";
         }
         default -> {
            return "feast|friend <other>|rival <other>";
         }
      }
   }

   // ---------------------------------------------------------------- saving

   private static FactionFeasts load(CompoundTag tag) {
      FactionFeasts data = new FactionFeasts();
      for (Tag element : tag.getList("Bonds", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         data.bonds.add(new Bond(entry.getString("A"), entry.getString("RulerA"), entry.getString("B"), entry.getString("RulerB"), entry.getInt("Kind")));
      }

      CompoundTag players = tag.getCompound("Players");
      for (String player : players.getAllKeys()) {
         try {
            Map<String, PlayerBond> map = new HashMap<>();
            CompoundTag entry = players.getCompound(player);
            for (String faction : entry.getAllKeys()) {
               CompoundTag bond = entry.getCompound(faction);
               map.put(faction, new PlayerBond(bond.getString("Ruler"), bond.getInt("Kind")));
            }

            data.playerBonds.put(UUID.fromString(player), map);
         } catch (IllegalArgumentException ignored) {
         }
      }

      for (Tag element : tag.getList("Feasts", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Feast feast = new Feast(entry.getString("Faction"), entry.getLong("Day"));
         readIds(entry.getList("Coming", Tag.TAG_STRING), feast.coming);
         readIds(entry.getList("Came", Tag.TAG_STRING), feast.came);
         data.feasts.put(feast.faction, feast);
      }

      return data;
   }

   private static void readIds(ListTag list, Set<UUID> into) {
      for (Tag element : list) {
         try {
            into.add(UUID.fromString(element.getAsString()));
         } catch (IllegalArgumentException ignored) {
         }
      }
   }

   private static ListTag writeIds(Set<UUID> ids) {
      ListTag list = new ListTag();
      ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
      return list;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag bonds = new ListTag();
      for (Bond bond : this.bonds) {
         CompoundTag entry = new CompoundTag();
         entry.putString("A", bond.a);
         entry.putString("RulerA", bond.rulerA);
         entry.putString("B", bond.b);
         entry.putString("RulerB", bond.rulerB);
         entry.putInt("Kind", bond.kind);
         bonds.add(entry);
      }

      tag.put("Bonds", bonds);
      CompoundTag players = new CompoundTag();
      this.playerBonds.forEach((player, map) -> {
         CompoundTag entry = new CompoundTag();
         map.forEach((faction, bond) -> {
            CompoundTag value = new CompoundTag();
            value.putString("Ruler", bond.ruler);
            value.putInt("Kind", bond.kind);
            entry.put(faction, value);
         });
         players.put(player.toString(), entry);
      });
      tag.put("Players", players);
      ListTag feasts = new ListTag();
      for (Feast feast : this.feasts.values()) {
         CompoundTag entry = new CompoundTag();
         entry.putString("Faction", feast.faction);
         entry.putLong("Day", feast.day);
         entry.put("Coming", writeIds(feast.coming));
         entry.put("Came", writeIds(feast.came));
         feasts.add(entry);
      }

      tag.put("Feasts", feasts);
      return tag;
   }
}
