package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * The people about a ruler, and what a faction wants. Each faction's court holds an heir, a marshal,
 * a treasurer and a priest, each of a mind of their own and each thinking well or ill of every
 * player. The marshal pulls the ruler toward war or away from it, the treasurer sets the prices, the
 * priest speaks for peace, and when the ruler dies the heir takes the throne and remembers who
 * courted them. A player wins a courtier over with gifts; their good word makes treaties easier.
 *
 * Every faction also wants one or two things: revenge on a rival, gold for its treasury, or a town
 * that belongs to someone else. A player who helps rises in its esteem; its goals are learned from
 * spies, or told to a player bound to it by a pact or an alliance.
 */
public final class FactionCourt extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_court";

   public enum Role {
      HEIR,
      MARSHAL,
      TREASURER,
      PRIEST;

      public String key() {
         return this.name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   public enum GoalKind {
      /** Wants a rival faction humbled: its soldiers killed, its towns taken. */
      REVENGE,
      /** Wants gold: gifts count for more until enough have come. */
      GOLD,
      /** Wants a town that another faction holds. */
      CONQUEST;

      public String key() {
         return this.name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   /** Silver a gift to a courtier costs: five gold. */
   public static final int COURTIER_GIFT = 50;
   /**
    * How much a gift is worth after so many recent ones of the same kind: a court soon tires of
    * presents. A gift's weight wears off one step a day.
    */
   private static final int[] WEARINESS = {100, 50, 25, 0};
   /** After this many recent gifts of a kind, another is refused before any coin changes hands. */
   static final int TIRED = WEARINESS.length - 1;
   /** Standing for each soldier of the faction's enemy a player kills, and at most so much a day. */
   private static final int AID_KILL_STANDING = 1;
   private static final int MAX_AID_A_DAY = 8;
   /** A supply of what the faction lacks most. */
   public static final int SUPPLY_AMOUNT = 32;
   private static final int SUPPLY_STANDING = 6;
   /** Peace brought about between two factions: what the player needs with each, and what it brings. */
   public static final int MEDIATE_STANDING = 20;
   private static final int MEDIATED_STANDING = 10;
   private static final int MEDIATE_FAILED_STANDING = -3;
   private static final int GIFT_FAVOR = 10;
   private static final int MAX_FAVOR = 50;
   private static final int MIN_FAVOR = -50;
   /** Favor every courtier loses when the player declares war on their faction. */
   private static final int WAR_FAVOR = -15;
   /** The favor at which a courtier's word counts: a marshal keeps the peace, the court eases treaties. */
   private static final int FRIEND_FAVOR = 20;
   /** At most this much off the standing a treaty needs, for a court that thinks well of the player. */
   private static final int MAX_TREATY_BONUS = 15;
   private static final int GOALS = 2;
   private static final int GOAL_DAYS = 20;
   private static final int GOLD_GIFTS = 3;
   private static final int GOAL_DONE_STANDING = 10;
   private static final int REVENGE_KILL_STANDING = 2;
   private static final int CONQUEST_KILL_STANDING = 1;
   /** A town within this distance of the faction's own is one it may covet. */
   private static final double CONQUEST_RANGE = 1600.0;
   private static final FactionRulers.Trait[] TRAITS = FactionRulers.Trait.values();
   private final Map<String, Courtier[]> courts = new HashMap<>();
   private final Map<String, List<Goal>> goals = new HashMap<>();
   /** Recent favors of each kind, by player, faction and kind: their weight, falling by one each day. */
   private final Map<String, Integer> recent = new HashMap<>();
   /** Days the recent weights were last worn down; not saved, the next day simply wears them again. */
   private long woreDay = -1L;

   static final int ADULT_AGE = 16;

   static final class Courtier {
      int name;
      int trait;
      /** What they look like; see FactionPeople. */
      int look;
      /** The day they were born; Long.MIN_VALUE until a court read from an older world is first asked for. */
      long born;
      /** Their traits of character; see FactionPeople.Character. */
      int character;
      final Map<UUID, Integer> favor = new HashMap<>();

      Courtier(int name, int trait, int look, long born, int character) {
         this.name = name;
         this.trait = trait;
         this.look = look;
         this.born = born;
         this.character = character;
      }

      FactionRulers.Trait trait() {
         return TRAITS[Mth.clamp(this.trait, 0, TRAITS.length - 1)];
      }

      int favor(UUID player) {
         return this.favor.getOrDefault(player, 0);
      }
   }

   static final class Goal {
      final GoalKind kind;
      /** The rival faction, or the owner of the coveted town. */
      final String target;
      final int settlement;
      final long since;
      int progress;
      final Set<UUID> known = new HashSet<>();

      Goal(GoalKind kind, String target, int settlement, long since) {
         this.kind = kind;
         this.target = target;
         this.settlement = settlement;
         this.since = since;
      }
   }

   public static FactionCourt get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionCourt::load, FactionCourt::new, DATA_NAME);
   }

   /** The faction's court, gathered the first time anyone asks. */
   static Courtier[] court(ServerLevel level, String faction) {
      FactionCourt data = get(level);
      Courtier[] court = data.courts.get(faction);
      if (court == null) {
         RandomSource random = level.getRandom();
         int ruler = FactionRulers.ruler(FactionWorldData.get(level), faction, random).name();
         long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
         court = new Courtier[Role.values().length];
         for (Role role : Role.values()) {
            court[role.ordinal()] = newcomer(court, ruler, random, day, role);
         }

         data.courts.put(faction, court);
         data.setDirty();
      } else {
         for (Courtier courtier : court) {
            if (courtier.born == Long.MIN_VALUE) {
               courtier.born = FactionPeople.oldBorn(courtier.look, Math.max(0L, FactionWorldData.get(level).lastSimulatedDay()));
               data.setDirty();
            }
         }
      }

      return court;
   }

   /** Someone new at court: an heir in youth, the others grown. */
   private static Courtier newcomer(Courtier[] court, int ruler, RandomSource random, long day, Role role) {
      int age = role == Role.HEIR ? 14 + random.nextInt(14) : 25 + random.nextInt(30);
      int trait = random.nextInt(TRAITS.length);
      return new Courtier(freshName(court, ruler, random), trait, FactionPeople.newLook(random), FactionPeople.bornAt(day, age, random), FactionPeople.newCharacter(random, TRAITS[trait]));
   }

   private static int freshName(Courtier[] court, int ruler, RandomSource random) {
      for (int tries = 0; tries < 16; tries++) {
         int name = random.nextInt(FactionRulers.NAMES);
         boolean taken = name == ruler;
         for (Courtier courtier : court) {
            taken |= courtier != null && courtier.name == name;
         }

         if (!taken) {
            return name;
         }
      }

      return random.nextInt(FactionRulers.NAMES);
   }

   private static Courtier courtier(ServerLevel level, String faction, Role role) {
      return court(level, faction)[role.ordinal()];
   }

   /** Times the usual chance this faction goes to war with another: a fierce marshal urges it on. */
   static double warFactor(ServerLevel level, String faction) {
      return switch (courtier(level, faction, Role.MARSHAL).trait()) {
         case WARLIKE -> 1.3;
         case CAUTIOUS -> 0.7;
         default -> 1.0;
      };
   }

   /** Times the usual chance this faction makes peace: the priest speaks for it, or against. */
   static double peaceFactor(ServerLevel level, String faction) {
      return switch (courtier(level, faction, Role.PRIEST).trait()) {
         case GENEROUS, CAUTIOUS -> 1.3;
         case WARLIKE -> 0.8;
         default -> 1.0;
      };
   }

   /** Times the usual chance this faction goes to war with the player: a marshal who likes them holds it back. */
   static double playerWarFactor(ServerLevel level, String faction, UUID player) {
      int favor = courtier(level, faction, Role.MARSHAL).favor(player);
      return warFactor(level, faction) * (favor >= FRIEND_FAVOR ? 0.5 : favor <= -FRIEND_FAVOR ? 1.5 : 1.0);
   }

   /** Times the usual prices: the treasurer's hand. */
   static double priceFactor(ServerLevel level, String faction) {
      return switch (courtier(level, faction, Role.TREASURER).trait()) {
         case GREEDY -> 1.1;
         case GENEROUS -> 0.9;
         default -> 1.0;
      };
   }

   /** Taken off the standing a treaty needs (or added, for a court that dislikes the player). */
   static int treatyBonus(ServerLevel level, String faction, UUID player) {
      int sum = 0;
      for (Courtier courtier : court(level, faction)) {
         sum += courtier.favor(player);
      }

      return Mth.clamp(sum / 4, -MAX_TREATY_BONUS, MAX_TREATY_BONUS);
   }

   /** A gift to one of the court: a greedy courtier is swayed more. */
   static Component gift(ServerPlayer player, String faction, int index) {
      Courtier[] court = court(player.serverLevel(), faction);
      if (index < 0 || index >= court.length) {
         return Component.empty();
      }

      if (recent(player.serverLevel(), player.getUUID(), faction, "courtier" + index) >= TIRED) {
         return Component.translatable("court.ancientwarfare.tired", roleName(Role.values()[index]), FactionRulers.name(court[index].name));
      } else if (!PlayerDiplomacy.takeEmeralds(player, COURTIER_GIFT)) {
         return Component.translatable("diplomacy.ancientwarfare.gift_missing", COURTIER_GIFT);
      }

      Courtier courtier = court[index];
      int gift = weigh(player.serverLevel(), player.getUUID(), faction, "courtier" + index, courtier.trait() == FactionRulers.Trait.GREEDY ? GIFT_FAVOR * 3 / 2 : GIFT_FAVOR);
      int favor = Math.min(MAX_FAVOR, courtier.favor(player.getUUID()) + gift);
      courtier.favor.put(player.getUUID(), favor);
      get(player.serverLevel()).setDirty();
      return Component.translatable(
         "court.ancientwarfare.gift", roleName(Role.values()[index]), FactionRulers.name(courtier.name), FactionNews.factionName(faction), favor
      );
   }

   /** What a favor of this kind is worth now, given how many came lately; counts this one. */
   static int weigh(ServerLevel level, UUID player, String faction, String kind, int full) {
      FactionCourt data = get(level);
      String key = player + "|" + faction + "|" + kind;
      int recent = data.recent.getOrDefault(key, 0);
      data.recent.put(key, recent + 1);
      data.setDirty();
      return full * WEARINESS[Math.min(recent, WEARINESS.length - 1)] / 100;
   }

   /** How many favors of this kind came lately, without counting a new one. */
   static int recent(ServerLevel level, UUID player, String faction, String kind) {
      return get(level).recent.getOrDefault(player + "|" + faction + "|" + kind, 0);
   }

   private void wear(long day) {
      if (day == this.woreDay) {
         return;
      }

      this.woreDay = day;
      this.recent.replaceAll((key, value) -> value - 1);
      this.recent.values().removeIf(value -> value <= 0);
      this.setDirty();
   }

   /**
    * A player brings the faction what its stores lack most: iron, stone or timber. Returns the
    * message; nothing is taken when the player has too little.
    */
   static Component supply(ServerPlayer player, String faction) {
      FactionWorldData data = FactionWorldData.get(player.serverLevel());
      int[] stores = data.stores(faction);
      int need = wanted(stores);
      net.minecraft.tags.TagKey<net.minecraft.world.item.Item> wanted = FactionDeals.GOODS_TAGS[need];
      Component goods = FactionDeals.goodsName(need);
      if (recent(player.serverLevel(), player.getUUID(), faction, "supply") >= TIRED) {
         return Component.translatable("supply.ancientwarfare.tired", FactionNews.factionName(faction));
      } else if (!player.isCreative() && !takeItems(player, wanted, SUPPLY_AMOUNT)) {
         return Component.translatable("supply.ancientwarfare.missing", SUPPLY_AMOUNT, goods);
      }

      stores[need] += SUPPLY_AMOUNT;
      data.setDirty();
      int standing = weigh(player.serverLevel(), player.getUUID(), faction, "supply", SUPPLY_STANDING);
      int now = FactionStandingData.get(player.serverLevel()).adjustStanding(player.getUUID(), faction, standing);
      return Component.translatable(standing > 0 ? "supply.ancientwarfare.given" : "supply.ancientwarfare.given_tired", FactionNews.factionName(faction), SUPPLY_AMOUNT, goods, now);
   }

   /** The kind the faction lacks most, measured against what it needs of each. */
   public static int wanted(int[] stores) {
      int need = FactionResources.IRON;
      for (int kind = 0; kind < Math.min(stores.length, FactionResources.KINDS); kind++) {
         if (stores[kind] / (double)FactionResources.NEED[kind] < stores[need] / (double)FactionResources.NEED[need]) {
            need = kind;
         }
      }

      return need;
   }

   /** How many of the tagged items the player carries. */
   static int heldItems(ServerPlayer player, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
      net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
      int held = 0;
      for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
         if (inventory.getItem(slot).is(tag)) {
            held += inventory.getItem(slot).getCount();
         }
      }

      return held;
   }

   static boolean takeItems(ServerPlayer player, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag, int count) {
      net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
      if (heldItems(player, tag) < count) {
         return false;
      }

      int owed = count;
      for (int slot = 0; slot < inventory.getContainerSize() && owed > 0; slot++) {
         net.minecraft.world.item.ItemStack stack = inventory.getItem(slot);
         if (stack.is(tag)) {
            int taken = Math.min(owed, stack.getCount());
            stack.shrink(taken);
            owed -= taken;
         }
      }

      return true;
   }

   /**
    * The player offers to make peace between the faction and the enemy it is at war with that thinks
    * best of the player. Both must think well enough of them; the longer the war, the readier they are.
    */
   static Component mediate(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      FactionStandingData standings = FactionStandingData.get(level);
      FactionWorldData data = FactionWorldData.get(level);
      String best = null;
      for (String other : FactionRegistry.factionNames()) {
         if (!other.equals(faction) && !FactionDiplomacy.isOutlaw(other) && FactionDiplomacy.relation(level, faction, other).state() == RelationState.WAR
            && standings.knownFactions(player.getUUID()).contains(other)
            && (best == null || standings.getStanding(player.getUUID(), other) > standings.getStanding(player.getUUID(), best))) {
            best = other;
         }
      }

      Component name = FactionNews.factionName(faction);
      if (best == null) {
         return Component.translatable("mediate.ancientwarfare.no_war", name);
      }

      Component otherName = FactionNews.factionName(best);
      int mine = standings.getStanding(player.getUUID(), faction);
      int theirs = standings.getStanding(player.getUUID(), best);
      if (standings.getTreaty(player.getUUID(), faction) == RelationState.WAR || standings.getTreaty(player.getUUID(), best) == RelationState.WAR
         || Math.min(mine, theirs) < MEDIATE_STANDING) {
         return Component.translatable("mediate.ancientwarfare.not_trusted", name, otherName, MEDIATE_STANDING);
      }

      if (recent(level, player.getUUID(), faction, "mediate") > 0) {
         return Component.translatable("mediate.ancientwarfare.too_soon", name);
      }

      weigh(level, player.getUUID(), faction, "mediate", 0);
      weigh(level, player.getUUID(), faction, "mediate", 0);
      weigh(level, player.getUUID(), faction, "mediate", 0);
      long days = Math.max(0L, data.lastSimulatedDay() - FactionDiplomacy.relation(level, faction, best).since());
      double chance = Mth.clamp(0.2 + Math.min(mine, theirs) / 200.0 + days * 0.03, 0.0, 0.9) * Math.min(peaceFactor(level, faction), peaceFactor(level, best));
      if (level.getRandom().nextDouble() >= chance) {
         standings.adjustStanding(player.getUUID(), faction, MEDIATE_FAILED_STANDING);
         standings.adjustStanding(player.getUUID(), best, MEDIATE_FAILED_STANDING);
         return Component.translatable("mediate.ancientwarfare.failed", name, otherName);
      }

      if (FactionDiplomacy.setState(level, faction, best, RelationState.NEUTRAL)) {
         FactionBorders.settlePeace(level, data, faction, best);
         FactionNews.relationChanged(level, faction, best, RelationState.NEUTRAL, "mediated", player.getDisplayName());
      }

      standings.adjustStanding(player.getUUID(), faction, MEDIATED_STANDING);
      standings.adjustStanding(player.getUUID(), best, MEDIATED_STANDING);
      return Component.translatable("mediate.ancientwarfare.done", name, otherName, MEDIATED_STANDING);
   }

   /** The player declared war: the whole court takes it ill. */
   static void warDeclared(ServerLevel level, UUID player, String faction) {
      for (Courtier courtier : court(level, faction)) {
         courtier.favor.put(player, Math.max(MIN_FAVOR, courtier.favor(player) + WAR_FAVOR));
      }

      get(level).setDirty();
   }

   /**
    * The ruler died: the heir takes the throne with their own mind, and each player stands with the
    * new ruler as well as they stood with the heir. A new heir is named.
    */
   static FactionWorldData.Ruler succession(ServerLevel level, String faction, long day) {
      FactionCourt data = get(level);
      Courtier[] court = court(level, faction);
      Courtier heir = court[Role.HEIR.ordinal()];
      FactionStandingData standings = FactionStandingData.get(level);
      heir.favor.forEach((player, favor) -> standings.adjustStanding(player, faction, favor / 2));
      court[Role.HEIR.ordinal()] = newcomer(court, heir.name, level.getRandom(), day, Role.HEIR);
      data.setDirty();
      return new FactionWorldData.Ruler(heir.name, heir.trait, day, heir.look, heir.born, false, heir.character);
   }

   /** The courtier most fond of the player, if fond enough to plot with them; null if none is. */
   static Role plotter(ServerLevel level, String faction, UUID player, int minFavor) {
      Courtier[] court = court(level, faction);
      Role best = null;
      for (Role role : Role.values()) {
         int favor = court[role.ordinal()].favor(player);
         if (favor >= minFavor && (best == null || favor > court[best.ordinal()].favor(player))) {
            best = role;
         }
      }

      return best;
   }

   /** A courtier's traits of character; see FactionPeople.Character. */
   static int character(ServerLevel level, String faction, Role role) {
      return courtier(level, faction, role).character;
   }

   /** The marshal's traits of character; see FactionPeople.Character. */
   static int marshalCharacter(ServerLevel level, String faction) {
      return courtier(level, faction, Role.MARSHAL).character;
   }

   static FactionRulers.Trait priestTrait(ServerLevel level, String faction) {
      return courtier(level, faction, Role.PRIEST).trait();
   }

   static FactionRulers.Trait marshalTrait(ServerLevel level, String faction) {
      return courtier(level, faction, Role.MARSHAL).trait();
   }

   static Component courtierName(ServerLevel level, String faction, Role role) {
      return FactionRulers.name(courtier(level, faction, role).name);
   }

   static void adjustFavor(ServerLevel level, String faction, Role role, UUID player, int change) {
      Courtier courtier = courtier(level, faction, role);
      courtier.favor.put(player, Mth.clamp(courtier.favor(player) + change, -MAX_FAVOR, MAX_FAVOR));
      get(level).setDirty();
   }

   /** A courtier sent from court; a newcomer takes their place. */
   static void replace(ServerLevel level, String faction, Role role, long day) {
      Courtier[] court = court(level, faction);
      int ruler = FactionRulers.ruler(FactionWorldData.get(level), faction, level.getRandom()).name();
      court[role.ordinal()] = newcomer(court, ruler, level.getRandom(), day, role);
      get(level).setDirty();
   }

   /** Whether this person, by name and day of birth, is the faction's named heir. */
   static boolean isHeir(ServerLevel level, String faction, int name, long born) {
      Courtier heir = courtier(level, faction, Role.HEIR);
      return heir.name == name && heir.born == born;
   }

   /** Whether the courtier is old enough to lead men; a child heir raises no banner. */
   static boolean adult(ServerLevel level, String faction, Role role) {
      return FactionPeople.age(courtier(level, faction, role).born, Math.max(0L, FactionWorldData.get(level).lastSimulatedDay())) >= ADULT_AGE;
   }

   /** A son or brother of the ruler is named heir in place of whoever was. */
   static void nameHeir(ServerLevel level, String faction, int name, int trait, int look, long born, int character) {
      court(level, faction)[Role.HEIR.ordinal()] = new Courtier(name, trait, look, born, character);
      get(level).setDirty();
   }

   /** A courtier takes the throne; a newcomer fills their place at court. */
   static FactionWorldData.Ruler crown(ServerLevel level, String faction, Role role, long day) {
      Courtier[] court = court(level, faction);
      Courtier usurper = court[role.ordinal()];
      court[role.ordinal()] = newcomer(court, usurper.name, level.getRandom(), day, role);
      get(level).setDirty();
      return new FactionWorldData.Ruler(usurper.name, usurper.trait, day, usurper.look, usurper.born, false, usurper.character);
   }

   /** The old at court die in time and a newcomer takes their place; the realm hears when it was the heir. */
   private void age(ServerLevel level, String faction, long day) {
      Courtier[] court = court(level, faction);
      RandomSource random = level.getRandom();
      for (Role role : Role.values()) {
         Courtier courtier = court[role.ordinal()];
         int age = FactionPeople.age(courtier.born, day);
         if (random.nextDouble() < FactionPeople.oldAgeDeath(age) * FactionPeople.factor(courtier.character, c -> c.oldAge)) {
            int ruler = FactionRulers.ruler(FactionWorldData.get(level), faction, random).name();
            court[role.ordinal()] = newcomer(court, ruler, random, day, role);
            FactionNews.tell(
               level,
               Component.translatable("news.ancientwarfare.courtier_died", FactionNews.factionName(faction), roleName(role), FactionRulers.name(courtier.name), age, FactionRulers.name(court[role.ordinal()].name))
            );
            this.setDirty();
         }
      }
   }

   /** Daily: goals are set, fulfilled, or given up after a while. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionCourt court = get(level);
      long day = Math.max(0L, data.lastSimulatedDay());
      court.wear(day);
      Set<String> factions = new HashSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            factions.add(settlement.faction());
         }
      }

      for (String faction : factions) {
         court.age(level, faction, day);
         List<Goal> goals = court.goals.computeIfAbsent(faction, ignored -> new ArrayList<>());
         goals.removeIf(goal -> {
            if (fulfilled(data, faction, goal)) {
               tellKnown(level, goal, Component.translatable("goal.ancientwarfare.done", FactionNews.factionName(faction), goalText(data, goal)));
               return true;
            }

            return day - goal.since >= GOAL_DAYS || goal.kind != GoalKind.GOLD && !factions.contains(goal.target);
         });
         while (goals.size() < GOALS) {
            Goal goal = newGoal(level, data, faction, goals, day);
            if (goal == null) {
               break;
            }

            goals.add(goal);
         }
      }

      court.goals.keySet().retainAll(factions);
      court.setDirty();
   }

   private static boolean fulfilled(FactionWorldData data, String faction, Goal goal) {
      if (goal.kind == GoalKind.GOLD) {
         return goal.progress >= GOLD_GIFTS;
      } else if (goal.kind == GoalKind.CONQUEST) {
         FactionWorldData.Settlement town = settlement(data, goal.settlement);
         return town == null || town.faction().equals(faction);
      }

      return false;
   }

   private static Goal newGoal(ServerLevel level, FactionWorldData data, String faction, List<Goal> goals, long day) {
      RandomSource random = level.getRandom();
      List<GoalKind> kinds = new ArrayList<>(List.of(GoalKind.values()));
      goals.forEach(goal -> kinds.remove(goal.kind));
      java.util.Collections.shuffle(kinds, new java.util.Random(random.nextLong()));
      for (GoalKind kind : kinds) {
         switch (kind) {
            case GOLD -> {
               return new Goal(kind, "", 0, day);
            }
            case REVENGE -> {
               String rival = rival(level, data, faction);
               if (rival != null) {
                  return new Goal(kind, rival, 0, day);
               }
            }
            case CONQUEST -> {
               FactionWorldData.Settlement town = coveted(level, data, faction);
               if (town != null) {
                  return new Goal(kind, town.faction(), town.id(), day);
               }
            }
         }
      }

      return null;
   }

   /** The faction it is at war with, or else the one it thinks worst of. */
   private static String rival(ServerLevel level, FactionWorldData data, String faction) {
      String worst = null;
      int worstOpinion = 0;
      for (String other : FactionRegistry.factionNames()) {
         if (other.equals(faction) || FactionDiplomacy.isOutlaw(other) || data.settlements().stream().noneMatch(settlement -> settlement.faction().equals(other))) {
            continue;
         }

         FactionDiplomacy.Relation relation = FactionDiplomacy.relation(level, faction, other);
         int score = relation.state() == RelationState.WAR ? -1000 + relation.opinion() : relation.opinion();
         if (score < worstOpinion) {
            worst = other;
            worstOpinion = score;
         }
      }

      return worst;
   }

   /** A town of another faction near its own, a capital never; the nearest of its rival's first. */
   private static FactionWorldData.Settlement coveted(ServerLevel level, FactionWorldData data, String faction) {
      String rival = rival(level, data, faction);
      FactionWorldData.Settlement best = null;
      double bestScore = Double.MAX_VALUE;
      for (FactionWorldData.Settlement town : data.settlements()) {
         if (town.faction().equals(faction) || FactionDiplomacy.isOutlaw(town.faction()) || FactionTowns.CAPITAL.equals(town.kind()) || !town.built()) {
            continue;
         }

         for (FactionWorldData.Settlement own : data.settlements()) {
            if (own.faction().equals(faction)) {
               double distance = own.horizontalDistanceSquared(town.center().getX(), town.center().getZ());
               double score = town.faction().equals(rival) ? distance / 4.0 : distance;
               if (distance <= CONQUEST_RANGE * CONQUEST_RANGE && score < bestScore) {
                  best = town;
                  bestScore = score;
               }
            }
         }
      }

      return best;
   }

   private static FactionWorldData.Settlement settlement(FactionWorldData data, int id) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.id() == id) {
            return settlement;
         }
      }

      return null;
   }

   /**
    * A player killed one of the victim faction's people: whoever wants it humbled thinks better of
    * them, and so, a little and only so often a day, does every faction at war with it.
    */
   public static void onKill(ServerLevel level, ServerPlayer player, String victim) {
      FactionCourt court = get(level);
      FactionStandingData standings = FactionStandingData.get(level);
      if (!FactionDiplomacy.isOutlaw(victim)) {
         for (String ally : standings.knownFactions(player.getUUID())) {
            if (!ally.equals(victim) && FactionDiplomacy.relation(level, ally, victim).state() == RelationState.WAR
               && recent(level, player.getUUID(), ally, "aid") < MAX_AID_A_DAY) {
               weigh(level, player.getUUID(), ally, "aid", 0);
               standings.adjustStanding(player.getUUID(), ally, AID_KILL_STANDING);
            }
         }
      }

      court.goals.forEach((faction, goals) -> {
         for (Goal goal : goals) {
            if (!faction.equals(victim) && goal.target.equals(victim) && goal.kind != GoalKind.GOLD) {
               standings.adjustStanding(player.getUUID(), faction, goal.kind == GoalKind.REVENGE ? REVENGE_KILL_STANDING : CONQUEST_KILL_STANDING);
            }
         }
      });
   }

   /** An agent's bribe wins over the courtier who thinks least of the player; returns their role. */
   static Role bribe(ServerLevel level, String faction, UUID player, int favor) {
      Courtier[] court = court(level, faction);
      int worst = 0;
      for (int i = 1; i < court.length; i++) {
         if (court[i].favor(player) < court[worst].favor(player)) {
            worst = i;
         }
      }

      court[worst].favor.put(player, Math.min(MAX_FAVOR, court[worst].favor(player) + favor));
      get(level).setDirty();
      return Role.values()[worst];
   }

   /** Whether the faction has set its heart on beating the other, for revenge or for a town. */
   static boolean wantsWar(ServerLevel level, String faction, String target) {
      return get(level).goals.getOrDefault(faction, List.of()).stream().anyMatch(goal -> goal.kind != GoalKind.GOLD && goal.target.equals(target));
   }

   /** Whether the faction is out to fill its treasury. */
   static boolean wantsGold(ServerLevel level, String faction) {
      return get(level).goals.getOrDefault(faction, List.of()).stream().anyMatch(goal -> goal.kind == GoalKind.GOLD);
   }

   /** A gift to the faction: a treasury in want takes it gladly. Returns the standing it is worth beyond the usual. */
   static int onGift(ServerPlayer player, String faction, int usual) {
      FactionCourt court = get(player.serverLevel());
      for (Goal goal : court.goals.getOrDefault(faction, List.of())) {
         if (goal.kind == GoalKind.GOLD) {
            goal.progress++;
            court.setDirty();
            if (goal.progress >= GOLD_GIFTS) {
               player.sendSystemMessage(Component.translatable("goal.ancientwarfare.gold_met", FactionNews.factionName(faction)).withStyle(ChatFormatting.GREEN));
               return usual + GOAL_DONE_STANDING;
            }

            return usual;
         }
      }

      return 0;
   }

   /** What the faction wants, as far as the player knows it. */
   static List<Component> goalLines(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionCourt court = get(level);
      List<Goal> goals = court.goals.getOrDefault(faction, List.of());
      RelationState treaty = FactionStandingData.get(level).getTreaty(player.getUUID(), faction);
      boolean told = FactionSpies.knows(level, data, player, faction) || treaty == RelationState.PACT || treaty == RelationState.ALLIANCE;
      List<Component> lines = new ArrayList<>();
      for (Goal goal : goals) {
         if (told && goal.known.add(player.getUUID())) {
            court.setDirty();
         }

         if (goal.known.contains(player.getUUID())) {
            lines.add(goalText(data, goal));
         }
      }

      if (lines.isEmpty() && !goals.isEmpty()) {
         lines.add(Component.translatable("goal.ancientwarfare.unknown"));
      }

      return lines;
   }

   private static Component goalText(FactionWorldData data, Goal goal) {
      return switch (goal.kind) {
         case REVENGE -> Component.translatable("goal.ancientwarfare.revenge", FactionNews.factionName(goal.target));
         case GOLD -> Component.translatable("goal.ancientwarfare.gold", goal.progress, GOLD_GIFTS);
         case CONQUEST -> {
            FactionWorldData.Settlement town = settlement(data, goal.settlement);
            yield Component.translatable(
               "goal.ancientwarfare.conquest", town == null ? Component.literal("?") : town.name().isEmpty() ? Component.literal("#" + town.id()) : town.nameComponent(), FactionNews.factionName(goal.target)
            );
         }
      };
   }

   private static void tellKnown(ServerLevel level, Goal goal, Component message) {
      for (UUID id : goal.known) {
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
         if (player != null) {
            player.sendSystemMessage(message.copy().withStyle(ChatFormatting.GRAY));
         }
      }
   }

   /** How many numbers courtView gives for each courtier. */
   public static final int COURT_VIEW = 6;

   /** The court for the diplomacy screen: name, trait, this player's favor, looks and age of each courtier. */
   static int[] courtView(ServerLevel level, String faction, UUID player) {
      Courtier[] court = court(level, faction);
      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      int[] view = new int[court.length * COURT_VIEW];
      for (int i = 0; i < court.length; i++) {
         view[i * COURT_VIEW] = court[i].name;
         view[i * COURT_VIEW + 1] = court[i].trait;
         view[i * COURT_VIEW + 2] = court[i].favor(player);
         view[i * COURT_VIEW + 3] = court[i].look;
         view[i * COURT_VIEW + 4] = FactionPeople.age(court[i].born, day);
         view[i * COURT_VIEW + 5] = court[i].character;
      }

      return view;
   }

   public static Component roleName(Role role) {
      return Component.translatable("court.ancientwarfare.role." + role.key());
   }

   private static FactionCourt load(CompoundTag tag) {
      FactionCourt data = new FactionCourt();
      CompoundTag courts = tag.getCompound("Courts");
      for (String faction : courts.getAllKeys()) {
         ListTag list = courts.getList(faction, Tag.TAG_COMPOUND);
         Courtier[] court = new Courtier[Role.values().length];
         for (int i = 0; i < court.length; i++) {
            CompoundTag entry = i < list.size() ? list.getCompound(i) : new CompoundTag();
            int look = entry.contains("Look") ? entry.getInt("Look") : FactionPeople.oldLook(faction + i, entry.getInt("Name"));
            court[i] = new Courtier(
               entry.getInt("Name"), entry.getInt("Trait"), look, entry.contains("Born") ? entry.getLong("Born") : Long.MIN_VALUE,
               FactionPeople.consistent(
                  entry.contains("Character") ? entry.getInt("Character") : FactionPeople.oldCharacter(look),
                  TRAITS[Mth.clamp(entry.getInt("Trait"), 0, TRAITS.length - 1)]
               )
            );
            CompoundTag favor = entry.getCompound("Favor");
            for (String player : favor.getAllKeys()) {
               try {
                  court[i].favor.put(UUID.fromString(player), favor.getInt(player));
               } catch (IllegalArgumentException ignored) {
               }
            }
         }

         data.courts.put(faction, court);
      }

      CompoundTag goals = tag.getCompound("Goals");
      for (String faction : goals.getAllKeys()) {
         List<Goal> list = new ArrayList<>();
         for (Tag element : goals.getList(faction, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag)element;
            try {
               Goal goal = new Goal(GoalKind.valueOf(entry.getString("Kind")), entry.getString("Target"), entry.getInt("Settlement"), entry.getLong("Since"));
               goal.progress = entry.getInt("Progress");
               for (Tag known : entry.getList("Known", Tag.TAG_INT_ARRAY)) {
                  goal.known.add(net.minecraft.nbt.NbtUtils.loadUUID(known));
               }

               list.add(goal);
            } catch (IllegalArgumentException e) {
               AncientWarfareCore.LOG.warn("Dropping unknown faction goal {}", entry);
            }
         }

         data.goals.put(faction, list);
      }

      CompoundTag recent = tag.getCompound("Recent");
      for (String key : recent.getAllKeys()) {
         data.recent.put(key, recent.getInt(key));
      }

      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag courts = new CompoundTag();
      this.courts.forEach((faction, court) -> {
         ListTag list = new ListTag();
         for (Courtier courtier : court) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Name", courtier.name);
            entry.putInt("Trait", courtier.trait);
            entry.putInt("Look", courtier.look);
            entry.putLong("Born", courtier.born);
            entry.putInt("Character", courtier.character);
            CompoundTag favor = new CompoundTag();
            courtier.favor.forEach((player, value) -> favor.putInt(player.toString(), value));
            entry.put("Favor", favor);
            list.add(entry);
         }

         courts.put(faction, list);
      });
      tag.put("Courts", courts);
      CompoundTag goals = new CompoundTag();
      this.goals.forEach((faction, list) -> {
         ListTag entries = new ListTag();
         for (Goal goal : list) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Kind", goal.kind.name());
            entry.putString("Target", goal.target);
            entry.putInt("Settlement", goal.settlement);
            entry.putLong("Since", goal.since);
            entry.putInt("Progress", goal.progress);
            ListTag known = new ListTag();
            goal.known.forEach(id -> known.add(net.minecraft.nbt.NbtUtils.createUUID(id)));
            entry.put("Known", known);
            entries.add(entry);
         }

         goals.put(faction, entries);
      });
      tag.put("Goals", goals);
      CompoundTag recent = new CompoundTag();
      this.recent.forEach(recent::putInt);
      tag.put("Recent", recent);
      return tag;
   }
}
