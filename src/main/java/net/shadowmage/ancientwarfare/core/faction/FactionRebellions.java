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
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Risings and civil wars.
 *
 * Discontent builds in a settlement kept in disorder, hungry, sick, or under a cruel ruler, and
 * ebbs in good order. When it runs high the people rise: their leader drives out the garrison and
 * the ruler answers by his character. A cruel, suspicious or warlike ruler sends soldiers to crush
 * them; a kind or generous one buys peace with concessions. A rising that is not put down
 * breaks away and goes over to a neighbour, or to the brigands. A player may help put one down.
 *
 * A realm in trouble, under a sick or cruel ruler, with risings in its towns, may fall into civil
 * war: an ambitious heir or marshal raises his banner and some of the towns go over to him. The two
 * sides fight it out day by day and win towns from each other; the pretender takes the throne or
 * goes to the block. While it lasts the realm sends no hosts abroad and its neighbours smell
 * weakness. A player may back either side, and the winner remembers.
 */
public final class FactionRebellions extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_rebellions";
   private static final int RISING_DISCONTENT = 60;
   private static final double RISING_CHANCE = 0.15;
   /** Days the ruler takes to answer a rising, and how long one can last before the town breaks away. */
   private static final long ANSWER_DAYS = 1L;
   private static final long BREAK_DAYS = 3L;
   /** Silver a player pays toward putting a rising down, and backing a side in a civil war. */
   public static final int HELP_COST = 40;
   public static final int BACK_COST = 80;
   private static final int HELP_STANDING = 6;
   private static final int BACKED_WIN_STANDING = 20;
   private static final int BACKED_LOSS_STANDING = -15;
   /** Soldiers a player's silver hires for the side they back. */
   private static final int BACKED_SOLDIERS = 8;
   private static final int MIN_TOWNS = 5;
   private static final double CIVIL_WAR_CHANCE = 0.003;
   private static final long STALEMATE_DAYS = 20L;

   /** Discontent by settlement id. */
   private final Map<Integer, Integer> discontent = new HashMap<>();
   private final Map<Integer, Rising> risings = new HashMap<>();
   private final Map<String, CivilWar> wars = new HashMap<>();

   static final class Rising {
      final int settlement;
      final int leader;
      int rebels;
      final long day;
      final Set<UUID> helpers = new HashSet<>();

      Rising(int settlement, int leader, int rebels, long day) {
         this.settlement = settlement;
         this.leader = leader;
         this.rebels = rebels;
         this.day = day;
      }
   }

   static final class CivilWar {
      final String faction;
      final FactionCourt.Role role;
      final long day;
      /** The pretender's army in the field, and the ruler's. */
      int rebels;
      int loyal;
      final Set<Integer> towns = new HashSet<>();
      final Map<UUID, Boolean> backers = new HashMap<>();
      /** For a war of faith, the creed the pretender holds to; -1 for none. */
      int faith = -1;

      CivilWar(String faction, FactionCourt.Role role, long day) {
         this.faction = faction;
         this.role = role;
         this.day = day;
      }
   }

   public static FactionRebellions get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionRebellions::load, FactionRebellions::new, DATA_NAME);
   }

   /** Whether the faction is at war with itself. */
   public static boolean inCivilWar(ServerLevel level, String faction) {
      return get(level).wars.containsKey(faction);
   }

   /** Times the usual chance of a war between the two: a realm at war with itself tempts its neighbours. */
   static double warFactor(ServerLevel level, String faction, String other) {
      return inCivilWar(level, faction) || inCivilWar(level, other) ? 1.5 : 1.0;
   }

   private static boolean rulerHas(FactionWorldData data, String faction, FactionPeople.Character character, RandomSource random) {
      return FactionPeople.has(FactionRulers.ruler(data, faction, random).character(), character);
   }

   // ---------------------------------------------------------------- days

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionRebellions rebellions = get(level);
      RandomSource random = level.getRandom();
      long day = Math.max(0L, data.lastSimulatedDay());
      Map<String, Integer> wars = new HashMap<>();
      for (String faction : factions(data)) {
         for (String other : factions(data)) {
            if (!other.equals(faction) && FactionDiplomacy.relation(level, faction, other).state() == RelationState.WAR) {
               wars.merge(faction, 1, Integer::sum);
            }
         }
      }

      // At most one town in a realm rises on any day.
      Set<String> risen = new HashSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         String faction = settlement.faction();
         if (FactionDiplomacy.isOutlaw(faction) || FactionAI.isFort(settlement)) {
            continue;
         }

         int order = settlement.order();
         int change = order >= 60 ? -2 : order >= 45 ? -1 : 1 + (45 - order) / 8;
         change += settlement.famine() > 0 ? 3 : 0;
         change += settlement.plague() > 0 ? 2 : 0;
         change += settlement.capturedFrom().isEmpty() ? 0 : 1;
         // Endless wars on every side wear the people out.
         change += wars.getOrDefault(faction, 0) >= 3 ? 1 : 0;
         change += rulerHas(data, faction, FactionPeople.Character.CRUEL, random) ? 1 : 0;
         change -= rulerHas(data, faction, FactionPeople.Character.KIND, random) ? 1 : 0;
         change += rebellions.wars.containsKey(faction) ? 1 : 0;
         if (FactionTowns.CAPITAL.equals(settlement.kind()) && change > 0) {
            change = (change + 1) / 2;
         }

         int value = Mth.clamp(rebellions.discontent.getOrDefault(settlement.id(), 0) + change, 0, 100);
         if (value == 0) {
            rebellions.discontent.remove(settlement.id());
         } else {
            rebellions.discontent.put(settlement.id(), value);
         }

         if (value >= RISING_DISCONTENT && !rebellions.risings.containsKey(settlement.id()) && !risen.contains(faction) && random.nextDouble() < RISING_CHANCE) {
            risen.add(faction);
            rebellions.rise(level, data, settlement, day);
         }
      }

      for (Rising rising : new ArrayList<>(rebellions.risings.values())) {
         rebellions.tend(level, data, rising, day);
      }

      for (String faction : factions(data)) {
         if (!rebellions.wars.containsKey(faction)) {
            rebellions.maybeCivilWar(level, data, faction, day);
         }
      }

      for (CivilWar war : new ArrayList<>(rebellions.wars.values())) {
         rebellions.fight(level, data, war, day);
      }

      rebellions.setDirty();
   }

   private static Set<String> factions(FactionWorldData data) {
      Set<String> factions = new TreeSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            factions.add(settlement.faction());
         }
      }

      return factions;
   }

   // ---------------------------------------------------------------- risings

   /** The people rise: the garrison is driven out, and those who stay join the rebels. */
   private void rise(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, long day) {
      RandomSource random = level.getRandom();
      int rebels = Math.max(3, settlement.population / 5);
      int deserters = Math.min(settlement.garrison, settlement.garrison / 3);
      FactionAI.returnSoldiers(data, settlement.faction(), settlement.garrison - deserters);
      settlement.garrison = 0;
      rebels += deserters;
      Rising rising = new Rising(settlement.id(), random.nextInt(FactionRulers.NAMES), rebels, day);
      this.risings.put(settlement.id(), rising);
      data.setDirty();
      AncientWarfareCore.LOG.info("Rising at #{} of {}: {} rebels", settlement.id(), settlement.faction(), rebels);
      FactionNews.tell(
         level,
         Component.translatable("news.ancientwarfare.rising", FactionNews.factionName(settlement.faction()), FactionNews.named(settlement), rebels, FactionRulers.name(rising.leader))
            .withStyle(ChatFormatting.GOLD)
      );
      tellKnown(
         level,
         settlement.faction(),
         Component.translatable("rising.ancientwarfare.offer", HELP_COST)
            .withStyle(ChatFormatting.GRAY)
            .append(" ")
            .append(button(Component.translatable("rising.ancientwarfare.help"), "/awrising " + settlement.id(), ChatFormatting.GREEN))
      );
   }

   /** The ruler answers the rising after a day: with soldiers, or with concessions; left alone it breaks away. */
   private void tend(ServerLevel level, FactionWorldData data, Rising rising, long day) {
      FactionWorldData.Settlement town = data.settlement(rising.settlement);
      if (town == null || FactionDiplomacy.isOutlaw(town.faction())) {
         this.risings.remove(rising.settlement);
         return;
      }

      if (day < rising.day + ANSWER_DAYS) {
         return;
      }

      String faction = town.faction();
      RandomSource random = level.getRandom();
      FactionRulers.Trait trait = FactionRulers.trait(data, faction, random);
      int cost = Math.min(300, 40 + town.population * 2);
      boolean harsh = rulerHas(data, faction, FactionPeople.Character.CRUEL, random)
         || rulerHas(data, faction, FactionPeople.Character.PARANOID, random)
         || rulerHas(data, faction, FactionPeople.Character.AMBITIOUS, random)
         || trait == FactionRulers.Trait.WARLIKE;
      boolean mild = rulerHas(data, faction, FactionPeople.Character.KIND, random) || trait == FactionRulers.Trait.GENEROUS;
      // With players paying for soldiers, there is no need to give way.
      boolean concede = !harsh && rising.helpers.isEmpty() && data.treasury(faction) >= cost && (mild || data.treasury(faction) >= cost * 3);
      Component ruler = FactionRulers.name(FactionRulers.ruler(data, faction, random).name());
      if (concede) {
         data.setTreasury(faction, data.treasury(faction) - cost);
         this.end(level, data, rising, town, rising.rebels / 2, 20, 50);
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.rising_conceded", ruler, FactionNews.named(town), cost / 10));
         return;
      }

      int sent = FactionAI.takeLosses(data, faction, Math.max(4, rising.rebels * 3 / 2));
      if (sent > 0) {
         // The rebels fight behind their own walls; players who helped paid for more men and better.
         double edge = 1.0 + 0.25 * rising.helpers.size() + (rulerHas(data, faction, FactionPeople.Character.CRUEL, random) ? 0.2 : 0.0);
         double chance = sent * edge / (sent * edge + rising.rebels * 1.2);
         if (random.nextDouble() < chance) {
            int lost = Math.min(sent - 1, (int)Math.round(rising.rebels * (0.3 + random.nextDouble() * 0.3)));
            town.population = Math.max(1, town.population - rising.rebels / 2);
            this.end(level, data, rising, town, sent - lost, 30, 40);
            FactionNews.tell(level, Component.translatable("news.ancientwarfare.rising_crushed", FactionNews.factionName(faction), FactionNews.named(town)));
            this.thankHelpers(level, rising, faction);
            return;
         }

         rising.rebels = Math.max(2, rising.rebels - sent / 3);
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.rising_held", FactionNews.factionName(faction), FactionNews.named(town)));
      }

      if (day >= rising.day + BREAK_DAYS) {
         this.breakAway(level, data, rising, town);
      }
   }

   /** The rising is over and the town the ruler's again. */
   private void end(ServerLevel level, FactionWorldData data, Rising rising, FactionWorldData.Settlement town, int garrison, int discontent, int order) {
      this.risings.remove(rising.settlement);
      this.discontent.put(town.id(), discontent);
      town.garrison += Math.max(1, garrison);
      town.order = Math.max(town.order, order);
      data.setDirty();
   }

   private void thankHelpers(ServerLevel level, Rising rising, String faction) {
      FactionStandingData standings = FactionStandingData.get(level);
      for (UUID helper : rising.helpers) {
         standings.adjustStanding(helper, faction, HELP_STANDING);
         FactionNews.tellPlayer(level, helper, Component.translatable("rising.ancientwarfare.thanks", FactionNews.factionName(faction), HELP_STANDING).withStyle(ChatFormatting.GREEN));
      }
   }

   /** The town throws off its master and goes over to a neighbour, one at war with him first; with none, to the brigands. */
   private void breakAway(ServerLevel level, FactionWorldData data, Rising rising, FactionWorldData.Settlement town) {
      String master = town.faction();
      String best = null;
      double bestScore = Double.MAX_VALUE;
      for (FactionWorldData.Settlement other : data.settlements()) {
         String faction = other.faction();
         if (faction.equals(master) || FactionDiplomacy.isOutlaw(faction)) {
            continue;
         }

         RelationState state = FactionDiplomacy.relation(level, master, faction).state();
         if (state == RelationState.ALLIANCE || state == RelationState.PACT) {
            continue;
         }

         // An enemy of the old master is welcome from twice as far.
         double score = other.horizontalDistanceSquared(town.center().getX(), town.center().getZ()) / (state == RelationState.WAR ? 4.0 : 1.0);
         if (score < bestScore) {
            bestScore = score;
            best = faction;
         }
      }

      String to = best != null && bestScore < 1500.0 * 1500.0 ? best : "brigand";
      this.risings.remove(rising.settlement);
      this.discontent.remove(town.id());
      town.setFaction(to);
      town.capturedFrom = FactionDiplomacy.isOutlaw(to) ? "" : master;
      town.garrison = rising.rebels;
      town.loyalty = 60;
      town.order = 40;
      data.setProvinceOwner(town.province(), to);
      if (!FactionDiplomacy.isOutlaw(to)) {
         FactionDiplomacy.adjustOpinion(level, master, to, -10);
      }

      data.setDirty();
      FactionNews.tell(
         level,
         Component.translatable("news.ancientwarfare.broke_away", FactionNews.named(town), FactionNews.factionName(master), FactionNews.factionName(to)).withStyle(ChatFormatting.GOLD)
      );
   }

   /** A player pays toward putting a rising down. */
   public static void help(ServerPlayer player, int settlement) {
      ServerLevel level = player.serverLevel();
      FactionRebellions rebellions = get(level);
      Rising rising = rebellions.risings.get(settlement);
      if (rising == null) {
         player.sendSystemMessage(Component.translatable("rising.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      } else if (rising.helpers.contains(player.getUUID())) {
         player.sendSystemMessage(Component.translatable("rising.ancientwarfare.already").withStyle(ChatFormatting.GRAY));
         return;
      } else if (!PlayerDiplomacy.takeEmeralds(player, HELP_COST)) {
         player.sendSystemMessage(Component.translatable("diplomacy.ancientwarfare.gift_missing", HELP_COST).withStyle(ChatFormatting.RED));
         return;
      }

      FactionWorldData.Settlement town = FactionWorldData.get(level).settlement(settlement);
      rising.helpers.add(player.getUUID());
      rebellions.setDirty();
      player.sendSystemMessage(
         Component.translatable("rising.ancientwarfare.helped", town == null ? Component.literal("?") : FactionNews.named(town)).withStyle(ChatFormatting.GREEN)
      );
   }

   // ---------------------------------------------------------------- civil wars

   /** A realm in trouble may split behind a pretender: an ambitious heir or marshal. */
   private void maybeCivilWar(ServerLevel level, FactionWorldData data, String faction, long day) {
      List<FactionWorldData.Settlement> towns = towns(data, faction);
      if (towns.size() < MIN_TOWNS) {
         return;
      }

      RandomSource random = level.getRandom();
      FactionWorldData.Ruler ruler = FactionRulers.ruler(data, faction, random);
      int risen = (int)this.risings.values().stream().filter(rising -> {
         FactionWorldData.Settlement town = data.settlement(rising.settlement);
         return town != null && town.faction().equals(faction);
      }).count();
      double chance = CIVIL_WAR_CHANCE * (1 + risen * 2);
      chance *= ruler.ill() ? 2.0 : 1.0;
      chance *= FactionPeople.has(ruler.character(), FactionPeople.Character.CRUEL) || FactionPeople.has(ruler.character(), FactionPeople.Character.PARANOID) ? 1.5 : 1.0;
      chance *= data.treasury(faction) < 200 ? 1.5 : 1.0;
      FactionCourt.Role role = pretender(level, faction);
      if (role == null || random.nextDouble() >= chance) {
         return;
      }

      this.startCivilWar(level, data, faction, role, day);
   }

   /** Who at court would raise his banner: an ambitious heir or marshal, the heir first. */
   private static FactionCourt.Role pretender(ServerLevel level, String faction) {
      if (FactionPeople.has(FactionCourt.character(level, faction, FactionCourt.Role.HEIR), FactionPeople.Character.AMBITIOUS)) {
         return FactionCourt.Role.HEIR;
      }

      if (FactionPeople.has(FactionCourt.character(level, faction, FactionCourt.Role.MARSHAL), FactionPeople.Character.AMBITIOUS)
         || FactionCourt.marshalTrait(level, faction) == FactionRulers.Trait.WARLIKE) {
         return FactionCourt.Role.MARSHAL;
      }

      return null;
   }

   private static List<FactionWorldData.Settlement> towns(FactionWorldData data, String faction) {
      List<FactionWorldData.Settlement> towns = new ArrayList<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction) && !FactionAI.isFort(settlement)) {
            towns.add(settlement);
         }
      }

      return towns;
   }

   private boolean startCivilWar(ServerLevel level, FactionWorldData data, String faction, FactionCourt.Role role, long day) {
      return this.startCivilWar(level, data, faction, role, day, null, -1);
   }

   /**
    * A war of faith: the towns given, turned to another creed, rise for a pretender who holds to
    * it. Returns false when the realm cannot split that way.
    */
   static boolean religiousWar(ServerLevel level, FactionWorldData data, String faction, FactionCourt.Role role, Set<Integer> towns, int faith) {
      FactionRebellions rebellions = get(level);
      return !rebellions.wars.containsKey(faction) && rebellions.startCivilWar(level, data, faction, role, Math.max(0L, data.lastSimulatedDay()), towns, faith);
   }

   /** Discontent rises or falls in a town: festivals calm it, persecution stirs it. */
   static void stir(ServerLevel level, int settlement, int change) {
      FactionRebellions rebellions = get(level);
      int value = Mth.clamp(rebellions.discontent.getOrDefault(settlement, 0) + change, 0, 100);
      if (value == 0) {
         rebellions.discontent.remove(settlement);
      } else {
         rebellions.discontent.put(settlement, value);
      }

      rebellions.setDirty();
   }

   static int discontent(ServerLevel level, int settlement) {
      return get(level).discontent.getOrDefault(settlement, 0);
   }

   private boolean startCivilWar(ServerLevel level, FactionWorldData data, String faction, FactionCourt.Role role, long day, Set<Integer> chosen, int faith) {
      RandomSource random = level.getRandom();
      CivilWar war = new CivilWar(faction, role, day);
      war.faith = faith;
      List<FactionWorldData.Settlement> towns = towns(data, faction);
      for (FactionWorldData.Settlement town : towns) {
         if (chosen != null) {
            if (chosen.contains(town.id()) && !FactionTowns.CAPITAL.equals(town.kind())) {
               war.towns.add(town.id());
            }

            continue;
         }

         if (FactionTowns.CAPITAL.equals(town.kind())) {
            continue;
         }

         // The discontented, the newly taken and the risen go over to him readily.
         double side = 0.15 + this.discontent.getOrDefault(town.id(), 0) / 150.0 + (town.capturedFrom().isEmpty() ? 0.0 : 0.2) + (this.risings.containsKey(town.id()) ? 0.5 : 0.0);
         if (random.nextDouble() < side) {
            war.towns.add(town.id());
         }
      }

      if (war.towns.isEmpty() || war.towns.size() >= towns.size()) {
         return false;
      }

      // Each side's field army is half its towns' garrisons.
      for (FactionWorldData.Settlement town : towns) {
         int half = town.garrison / 2;
         town.garrison -= half;
         if (war.towns.contains(town.id())) {
            war.rebels += half;
         } else {
            war.loyal += half;
         }

         Rising rising = this.risings.remove(town.id());
         if (rising != null) {
            war.rebels += rising.rebels;
         }
      }

      this.wars.put(faction, war);
      this.setDirty();
      data.setDirty();
      Component pretender = FactionCourt.courtierName(level, faction, role);
      Component roleName = Component.translatable("quarrel.ancientwarfare.role." + role.key());
      AncientWarfareCore.LOG.info("Civil war in {}: the {} raises his banner with {} of {} towns", faction, role.key(), war.towns.size(), towns.size());
      FactionNews.tell(
         level,
         (faith < 0
               ? Component.translatable("news.ancientwarfare.civil_war", FactionNews.factionName(faction), roleName, pretender, war.towns.size(), towns.size())
               : Component.translatable("news.ancientwarfare.civil_war_faith", FactionNews.factionName(faction), roleName, pretender, war.towns.size(), towns.size(), FactionFaith.name(faith)))
            .withStyle(ChatFormatting.RED)
      );
      tellKnown(
         level,
         faction,
         Component.translatable("civil.ancientwarfare.offer", BACK_COST)
            .withStyle(ChatFormatting.GRAY)
            .append(" ")
            .append(button(Component.translatable("civil.ancientwarfare.back_ruler"), "/awcivil " + faction + " ruler", ChatFormatting.GREEN))
            .append(" ")
            .append(button(Component.translatable("civil.ancientwarfare.back_pretender"), "/awcivil " + faction + " pretender", ChatFormatting.GOLD))
      );
      return true;
   }

   /** A day of civil war: the two armies clash and the stronger wins a town over. */
   private void fight(ServerLevel level, FactionWorldData data, CivilWar war, long day) {
      RandomSource random = level.getRandom();
      List<FactionWorldData.Settlement> towns = towns(data, war.faction);
      war.towns.removeIf(id -> towns.stream().noneMatch(town -> town.id() == id));
      if (towns.isEmpty()) {
         this.wars.remove(war.faction);
         return;
      }

      // A skirmish a day: each side wears the other down.
      int rebels = war.rebels;
      int loyal = war.loyal;
      war.rebels = Math.max(0, war.rebels - (int)Math.round(loyal * (0.05 + random.nextDouble() * 0.15)));
      war.loyal = Math.max(0, war.loyal - (int)Math.round(rebels * (0.05 + random.nextDouble() * 0.15)));
      // Both sides call up more from their towns.
      for (FactionWorldData.Settlement town : towns) {
         int levy = town.garrison / 10;
         town.garrison -= levy;
         if (war.towns.contains(town.id())) {
            war.rebels += levy;
         } else {
            war.loyal += levy;
         }
      }

      // The stronger side wins over a town of the other.
      boolean rebelsAhead = war.rebels > war.loyal;
      double edge = Math.abs(war.rebels - war.loyal) / (double)Math.max(1, war.rebels + war.loyal);
      if (random.nextDouble() < 0.2 + edge) {
         List<FactionWorldData.Settlement> other = new ArrayList<>();
         for (FactionWorldData.Settlement town : towns) {
            if (war.towns.contains(town.id()) != rebelsAhead) {
               other.add(town);
            }
         }

         if (!other.isEmpty()) {
            FactionWorldData.Settlement won = other.get(random.nextInt(other.size()));
            if (rebelsAhead) {
               war.towns.add(won.id());
            } else {
               war.towns.remove(won.id());
            }

            won.order = Math.min(won.order, 30);
            data.setDirty();
         }
      }

      FactionWorldData.Settlement capital = towns.stream().filter(town -> FactionTowns.CAPITAL.equals(town.kind())).findFirst().orElse(null);
      boolean capitalFell = capital != null && war.towns.contains(capital.id());
      if (war.rebels <= 2 || war.towns.isEmpty()) {
         this.finish(level, data, war, false);
      } else if (war.loyal <= 2 || capitalFell || war.towns.size() * 4 >= towns.size() * 3) {
         this.finish(level, data, war, true);
      } else if (day >= war.day + STALEMATE_DAYS) {
         // Neither side can win: the stronger has its way.
         this.finish(level, data, war, war.rebels > war.loyal);
      }
   }

   private void finish(ServerLevel level, FactionWorldData data, CivilWar war, boolean pretenderWins) {
      this.wars.remove(war.faction);
      RandomSource random = level.getRandom();
      Component faction = FactionNews.factionName(war.faction);
      Component pretender = FactionCourt.courtierName(level, war.faction, war.role);
      FactionAI.returnSoldiers(data, war.faction, war.rebels + war.loyal);
      long day = Math.max(0L, data.lastSimulatedDay());
      if (pretenderWins) {
         FactionWorldData.Ruler old = FactionRulers.ruler(data, war.faction, random);
         FactionWorldData.Ruler crowned = FactionCourt.crown(level, war.faction, war.role, day);
         FactionRulers.enthrone(level, war.faction, crowned);
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.civil_war_won", faction, pretender, FactionRulers.name(old.name())).withStyle(ChatFormatting.RED));
         if (war.faith >= 0) {
            FactionFaith.convert(level, war.faction, war.faith);
         }
      } else {
         FactionCourt.replace(level, war.faction, war.role, day);
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.civil_war_lost", faction, pretender));
      }

      // The towns of the losing side are punished, or sulk.
      for (FactionWorldData.Settlement town : towns(data, war.faction)) {
         if (war.towns.contains(town.id()) != pretenderWins) {
            town.order = Math.min(town.order, 30);
            this.discontent.merge(town.id(), 20, Integer::sum);
         }
      }

      FactionStandingData standings = FactionStandingData.get(level);
      for (Map.Entry<UUID, Boolean> backer : war.backers.entrySet()) {
         boolean won = backer.getValue() == pretenderWins;
         int change = won ? BACKED_WIN_STANDING : BACKED_LOSS_STANDING;
         standings.adjustStanding(backer.getKey(), war.faction, change);
         FactionNews.tellPlayer(
            level, backer.getKey(), Component.translatable(won ? "civil.ancientwarfare.backed_won" : "civil.ancientwarfare.backed_lost", faction, change)
               .withStyle(won ? ChatFormatting.GREEN : ChatFormatting.RED)
         );
      }

      data.setDirty();
      this.setDirty();
   }

   /** A player hires soldiers for one side of a civil war. */
   public static void back(ServerPlayer player, String faction, boolean pretender) {
      ServerLevel level = player.serverLevel();
      FactionRebellions rebellions = get(level);
      CivilWar war = rebellions.wars.get(faction);
      if (war == null) {
         player.sendSystemMessage(Component.translatable("civil.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      } else if (war.backers.containsKey(player.getUUID())) {
         player.sendSystemMessage(Component.translatable("civil.ancientwarfare.already").withStyle(ChatFormatting.GRAY));
         return;
      } else if (!PlayerDiplomacy.takeEmeralds(player, BACK_COST)) {
         player.sendSystemMessage(Component.translatable("diplomacy.ancientwarfare.gift_missing", BACK_COST).withStyle(ChatFormatting.RED));
         return;
      }

      war.backers.put(player.getUUID(), pretender);
      if (pretender) {
         war.rebels += BACKED_SOLDIERS;
      } else {
         war.loyal += BACKED_SOLDIERS;
      }

      rebellions.setDirty();
      player.sendSystemMessage(
         Component.translatable(
               pretender ? "civil.ancientwarfare.backed_pretender" : "civil.ancientwarfare.backed_ruler",
               FactionCourt.courtierName(level, faction, war.role),
               FactionNews.factionName(faction),
               BACKED_SOLDIERS
            )
            .withStyle(ChatFormatting.GREEN)
      );
   }

   // ---------------------------------------------------------------- view

   /** The overview's lines: towns risen against the ruler, a civil war. */
   static List<Component> lines(ServerLevel level, String faction) {
      FactionRebellions rebellions = get(level);
      FactionWorldData data = FactionWorldData.get(level);
      List<Component> lines = new ArrayList<>();
      CivilWar war = rebellions.wars.get(faction);
      if (war != null) {
         lines.add(
            Component.translatable(
                  "civil.ancientwarfare.line",
                  FactionCourt.courtierName(level, faction, war.role),
                  war.towns.size(),
                  towns(data, faction).size(),
                  war.rebels,
                  war.loyal
               )
               .withStyle(ChatFormatting.RED)
         );
      }

      List<Component> risen = new ArrayList<>();
      for (Rising rising : rebellions.risings.values()) {
         FactionWorldData.Settlement town = data.settlement(rising.settlement);
         if (town != null && town.faction().equals(faction)) {
            risen.add(FactionNews.named(town));
         }
      }

      if (risen.size() == 1) {
         lines.add(Component.translatable("rising.ancientwarfare.line", risen.get(0)).withStyle(ChatFormatting.GOLD));
      } else if (!risen.isEmpty()) {
         lines.add(Component.translatable("rising.ancientwarfare.line_many", risen.size()).withStyle(ChatFormatting.GOLD));
      }

      return lines;
   }

   private static void tellKnown(ServerLevel level, String faction, Component message) {
      FactionStandingData standings = FactionStandingData.get(level);
      for (ServerPlayer player : level.players()) {
         if (standings.knownFactions(player.getUUID()).contains(faction)) {
            player.sendSystemMessage(message);
         }
      }
   }

   private static MutableComponent button(Component label, String command, ChatFormatting color) {
      return Component.literal("[")
         .append(label)
         .append("]")
         .withStyle(
            style -> style.withColor(color)
               .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, label))
         );
   }

   /** Admin: "rise" raises the faction's most discontented town, "civil" starts a civil war, "end" settles everything, "list". */
   public static String debug(ServerLevel level, String faction, String what) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionRebellions rebellions = get(level);
      long day = Math.max(0L, data.lastSimulatedDay());
      switch (what) {
         case "rise" -> {
            FactionWorldData.Settlement worst = null;
            for (FactionWorldData.Settlement town : towns(data, faction)) {
               if (!FactionTowns.CAPITAL.equals(town.kind()) && !rebellions.risings.containsKey(town.id())
                  && (worst == null || rebellions.discontent.getOrDefault(town.id(), 0) > rebellions.discontent.getOrDefault(worst.id(), 0))) {
                  worst = town;
               }
            }

            if (worst == null) {
               return "No town";
            }

            rebellions.rise(level, data, worst, day);
            rebellions.setDirty();
            return "Rising at #" + worst.id();
         }
         case "civil" -> {
            FactionCourt.Role role = pretender(level, faction);
            boolean started = rebellions.startCivilWar(level, data, faction, role == null ? FactionCourt.Role.MARSHAL : role, day);
            return started ? "Civil war" : "No sides";
         }
         case "tend" -> {
            for (Rising rising : new ArrayList<>(rebellions.risings.values())) {
               rebellions.tend(level, data, rising, rising.day + BREAK_DAYS);
            }

            CivilWar war = rebellions.wars.get(faction);
            if (war != null) {
               rebellions.fight(level, data, war, day);
            }

            rebellions.setDirty();
            return "Tended";
         }
         case "end" -> {
            CivilWar war = rebellions.wars.get(faction);
            if (war != null) {
               rebellions.finish(level, data, war, war.rebels > war.loyal);
            }

            return "Ended";
         }
         default -> {
            StringBuilder out = new StringBuilder("Risings:");
            for (Rising rising : rebellions.risings.values()) {
               FactionWorldData.Settlement town = data.settlement(rising.settlement);
               out.append(" #").append(rising.settlement).append(' ').append(town == null ? "?" : town.faction()).append(' ').append(rising.rebels).append(" since ").append(rising.day);
            }

            out.append(" Wars:");
            for (CivilWar war : rebellions.wars.values()) {
               out.append(' ').append(war.faction).append(' ').append(war.role.key()).append(" towns ").append(war.towns.size()).append(" rebels ").append(war.rebels).append(" loyal ").append(war.loyal);
            }

            out.append(" Discontent:");
            for (FactionWorldData.Settlement town : towns(data, faction)) {
               out.append(' ').append(town.id()).append('=').append(rebellions.discontent.getOrDefault(town.id(), 0));
            }

            return out.toString();
         }
      }
   }

   // ---------------------------------------------------------------- saving

   private static FactionRebellions load(CompoundTag tag) {
      FactionRebellions data = new FactionRebellions();
      CompoundTag discontent = tag.getCompound("Discontent");
      for (String key : discontent.getAllKeys()) {
         try {
            data.discontent.put(Integer.parseInt(key), discontent.getInt(key));
         } catch (NumberFormatException ignored) {
         }
      }

      for (Tag element : tag.getList("Risings", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Rising rising = new Rising(entry.getInt("Settlement"), entry.getInt("Leader"), entry.getInt("Rebels"), entry.getLong("Day"));
         readIds(entry.getList("Helpers", Tag.TAG_STRING), rising.helpers);
         data.risings.put(rising.settlement, rising);
      }

      FactionCourt.Role[] roles = FactionCourt.Role.values();
      for (Tag element : tag.getList("Wars", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         CivilWar war = new CivilWar(entry.getString("Faction"), roles[Mth.clamp(entry.getInt("Role"), 0, roles.length - 1)], entry.getLong("Day"));
         war.rebels = entry.getInt("Rebels");
         war.loyal = entry.getInt("Loyal");
         war.faith = entry.contains("Faith") ? entry.getInt("Faith") : -1;
         for (int id : entry.getIntArray("Towns")) {
            war.towns.add(id);
         }

         CompoundTag backers = entry.getCompound("Backers");
         for (String key : backers.getAllKeys()) {
            try {
               war.backers.put(UUID.fromString(key), backers.getBoolean(key));
            } catch (IllegalArgumentException ignored) {
            }
         }

         data.wars.put(war.faction, war);
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

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag discontent = new CompoundTag();
      this.discontent.forEach((id, value) -> discontent.putInt(Integer.toString(id), value));
      tag.put("Discontent", discontent);
      ListTag risings = new ListTag();
      for (Rising rising : this.risings.values()) {
         CompoundTag entry = new CompoundTag();
         entry.putInt("Settlement", rising.settlement);
         entry.putInt("Leader", rising.leader);
         entry.putInt("Rebels", rising.rebels);
         entry.putLong("Day", rising.day);
         ListTag helpers = new ListTag();
         rising.helpers.forEach(id -> helpers.add(StringTag.valueOf(id.toString())));
         entry.put("Helpers", helpers);
         risings.add(entry);
      }

      tag.put("Risings", risings);
      ListTag wars = new ListTag();
      for (CivilWar war : this.wars.values()) {
         CompoundTag entry = new CompoundTag();
         entry.putString("Faction", war.faction);
         entry.putInt("Role", war.role.ordinal());
         entry.putLong("Day", war.day);
         entry.putInt("Rebels", war.rebels);
         entry.putInt("Loyal", war.loyal);
         entry.putInt("Faith", war.faith);
         entry.put("Towns", new IntArrayTag(war.towns.stream().mapToInt(Integer::intValue).toArray()));
         CompoundTag backers = new CompoundTag();
         war.backers.forEach((id, side) -> backers.putBoolean(id.toString(), side));
         entry.put("Backers", backers);
         wars.add(entry);
      }

      tag.put("Wars", wars);
      return tag;
   }
}
