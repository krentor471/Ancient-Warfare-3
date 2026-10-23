package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;
import net.shadowmage.ancientwarfare.core.research.PlayerResearch;

/**
 * Deals struck with a faction's ruler. Each side puts its part on the scales: the player gold, a
 * weekly tribute or a promise to go to war against one of the faction's enemies; the faction gold
 * from its treasury, the next treaty, a trade agreement or a war on one of the player's enemies.
 * The ruler weighs the deal by the faction's wants and their own temper and agrees, asks for more
 * or refuses. Promises are remembered: one kept makes the player's word worth more to every
 * faction, one broken makes it worth less, and every faction hears of it.
 */
public final class FactionDeals extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_deals";
   /** Steps of the gold buttons, in gold coins. */
   public static final int GOLD_STEP = 5;
   /** Goods change hands in loads of this many items. */
   public static final int GOODS_STEP = 16;
   /** Wood, stone and iron. */
   public static final int KINDS = 3;
   public static final int GOODS = KINDS * 2;
   /** What an item of each kind is worth in silver to a faction with middling stores. */
   private static final double[] GOODS_PRICE = {0.5, 0.5, 2.0};
   public static final int TRIBUTE_STEP = 2;
   public static final int TRIBUTE_WEEKS = 4;
   private static final int WEEK_DAYS = 7;
   public static final int WAR_PROMISE_DAYS = 10;
   /** Days a tribute may run late while its payer is away before it counts as broken. */
   private static final int TRIBUTE_GRACE = 3;
   /** What a new enemy for its foe is worth to a faction, in silver. */
   private static final int WAR_WORTH = 400;
   /** Silver a point of standing the faction lacks for a treaty costs. */
   private static final int STANDING_PRICE = 8;
   private static final int TREATY_PRICE = 20;
   /** Beyond this many gold short, the ruler does not haggle. */
   private static final int MAX_MISSING = 50;
   private static final int DEAL_STANDING = 3;
   private static final int KEPT_STANDING = 5;
   private static final int BROKEN_STANDING = -30;
   private static final int BROKEN_ELSEWHERE = -5;
   public static final int MIN_RELIABILITY = -5;
   public static final int MAX_RELIABILITY = 5;
   /** At this reliability or below, no faction takes the player's word. */
   public static final int NO_TRUST = -3;
   /** The ruler's answers. */
   public static final int NONE = -1;
   public static final int ACCEPT = 0;
   public static final int MORE = 1;
   public static final int REFUSE = 2;
   public static final int DONE = 3;
   /** Bonds a deal can make: none, the faction becomes the player's vassal, or the player swears fealty to it. */
   public static final int NO_BOND = 0;
   public static final int VASSAL = 1;
   public static final int FEALTY = 2;
   /** The player asks for the hand of the ruler's daughter. */
   public static final int MARRIAGE = 3;
   /** The player offers his own daughter to the ruler. */
   public static final int GIVE_DAUGHTER = 4;
   /** What a vassal's submission costs it, in silver: per settlement it holds, and per soldier. */
   private static final int VASSAL_SETTLEMENT_PRICE = 150;
   private static final int VASSAL_SOLDIER_PRICE = 3;
   /** What a sworn player is worth to an overlord, tribute and prestige together. */
   private static final int FEALTY_WORTH = 400;

   private final List<Promise> promises = new ArrayList<>();
   private final Map<UUID, Integer> reliability = new HashMap<>();

   /**
    * What each side puts in: gold in gold coins, tribute in gold a week; goods are wood, stone and iron
    * the player gives, then the same the faction gives, each counted in loads of GOODS_STEP.
    */
   public record Offer(int playerGold, int tribute, String playerWar, int factionGold, boolean treaty, boolean trade, String factionWar, int[] goods, int bond) {
      public static final Offer EMPTY = new Offer(0, 0, "", 0, false, false, "", new int[GOODS], NO_BOND);

      public Offer {
         goods = java.util.Arrays.copyOf(goods, GOODS);
      }

      boolean empty() {
         return this.playerGold <= 0 && this.tribute <= 0 && this.playerWar.isEmpty() && this.factionGold <= 0 && !this.treaty && !this.trade && this.factionWar.isEmpty() && this.bond == NO_BOND
            && java.util.Arrays.stream(this.goods).allMatch(load -> load <= 0);
      }

      /** Items of a kind the player gives (mine) or the faction gives. */
      public int items(int kind, boolean mine) {
         return Math.max(0, this.goods[(mine ? 0 : KINDS) + kind]) * GOODS_STEP;
      }

      boolean promises() {
         return this.tribute > 0 || !this.playerWar.isEmpty();
      }
   }

   /** What the deal window shows: the sides' possible wars, the player's word and promises, and the ruler's answer. */
   public record View(
      String faction, RelationState treaty, boolean trade, int reliability, List<String> playerTargets, List<String> factionTargets, List<Component> promises, int verdict,
      Component answer, Offer offer
   ) {
   }

   enum Kind {
      WAR,
      TRIBUTE
   }

   static final class Promise {
      final UUID player;
      final String faction;
      final Kind kind;
      final String target;
      final int amount;
      long due;
      int left;

      Promise(UUID player, String faction, Kind kind, String target, int amount, long due, int left) {
         this.player = player;
         this.faction = faction;
         this.kind = kind;
         this.target = target;
         this.amount = amount;
         this.due = due;
         this.left = left;
      }
   }

   public static FactionDeals get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionDeals::load, FactionDeals::new, DATA_NAME);
   }

   public static int reliability(ServerLevel level, UUID player) {
      return get(level).reliability.getOrDefault(player, 0);
   }

   /** From the deal window: 0 opens it, 1 asks the ruler's answer, 2 strikes the deal. */
   public static void handle(ServerPlayer player, String faction, int mode, Offer offer) {
      FactionStandingData standings = FactionStandingData.get(player.serverLevel());
      if (!standings.knownFactions(player.getUUID()).contains(faction)) {
         return;
      }

      int verdict = NONE;
      Component answer = Component.empty();
      if (mode > 0 && !PlayerDiplomacy.leaderNear(player, faction)) {
         verdict = REFUSE;
         answer = Component.translatable("diplomacy.ancientwarfare.need_leader", FactionNews.factionName(faction));
      } else if (mode > 0) {
         Weighed weighed = weigh(player, faction, offer);
         verdict = weighed.verdict;
         answer = weighed.answer;
         if (mode == 2 && verdict == ACCEPT) {
            Component struck = strike(player, faction, offer);
            verdict = struck == null ? DONE : REFUSE;
            answer = struck == null ? Component.translatable("deal.ancientwarfare.struck", FactionNews.factionName(faction)) : struck;
            if (struck == null) {
               player.sendSystemMessage(answer.copy().withStyle(ChatFormatting.GREEN));
            }
         }
      }

      AWCoreNetwork.sendDealData(player, mode == 0, view(player, faction, verdict, answer, verdict == DONE ? Offer.EMPTY : offer));
   }

   private static View view(ServerPlayer player, String faction, int verdict, Component answer, Offer offer) {
      ServerLevel level = player.serverLevel();
      FactionStandingData standings = FactionStandingData.get(level);
      List<Component> promises = new ArrayList<>();
      long today = FactionWorldData.get(level).lastSimulatedDay();
      for (Promise promise : get(level).promises) {
         if (promise.player.equals(player.getUUID())) {
            promises.add(describe(promise, today));
         }
      }

      return new View(
         faction,
         standings.getTreaty(player.getUUID(), faction),
         standings.hasTrade(player.getUUID(), faction),
         reliability(level, player.getUUID()),
         playerTargets(player, faction),
         factionTargets(player, faction),
         promises,
         verdict,
         answer,
         offer
      );
   }

   private static Component describe(Promise promise, long today) {
      Component faction = FactionNews.factionName(promise.faction);
      long days = Math.max(0L, promise.due - today);
      return promise.kind == Kind.WAR
         ? Component.translatable("deal.ancientwarfare.promise.war", faction, FactionNews.factionName(promise.target), days)
         : Component.translatable("deal.ancientwarfare.promise.tribute", faction, promise.amount, promise.left, days);
   }

   /** The faction's enemies the player could promise to fight: known, not outlaws, not already at war with the player. */
   private static List<String> playerTargets(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      FactionStandingData standings = FactionStandingData.get(level);
      List<String> targets = new ArrayList<>();
      for (String other : new TreeSet<>(standings.knownFactions(player.getUUID()))) {
         if (!other.equals(faction) && !FactionDiplomacy.isOutlaw(other) && FactionDiplomacy.relation(level, faction, other).state() == RelationState.WAR
            && standings.getTreaty(player.getUUID(), other) != RelationState.WAR) {
            targets.add(other);
         }
      }

      return targets;
   }

   /** The player's enemies the faction could go to war with: not its friends, not at war with it already. */
   private static List<String> factionTargets(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      FactionStandingData standings = FactionStandingData.get(level);
      List<String> targets = new ArrayList<>();
      for (String other : new TreeSet<>(FactionRegistry.factionNames())) {
         if (!other.equals(faction) && !FactionDiplomacy.isOutlaw(other) && standings.getTreaty(player.getUUID(), other) == RelationState.WAR
            && FactionDiplomacy.relation(level, faction, other).state() == RelationState.NEUTRAL) {
            targets.add(other);
         }
      }

      return targets;
   }

   private record Weighed(int verdict, Component answer) {
   }

   /** The ruler weighs what they get against what they give, both in silver. */
   private static Weighed weigh(ServerPlayer player, String faction, Offer offer) {
      ServerLevel level = player.serverLevel();
      FactionStandingData standings = FactionStandingData.get(level);
      FactionWorldData data = FactionWorldData.get(level);
      Component name = FactionNews.factionName(faction);
      RelationState treaty = standings.getTreaty(player.getUUID(), faction);
      int standing = standings.getStanding(player.getUUID(), faction);
      int word = reliability(level, player.getUUID());
      FactionRulers.Trait trait = FactionRulers.trait(level, faction);
      if (offer.empty()) {
         return refuse("deal.ancientwarfare.empty", name);
      } else if (offer.promises() && word <= NO_TRUST) {
         return refuse("deal.ancientwarfare.no_trust", name);
      } else if (treaty == RelationState.WAR && !offer.treaty() && offer.bond() == NO_BOND) {
         return refuse("deal.ancientwarfare.at_war", name);
      } else if (!offer.playerWar().isEmpty() && !playerTargets(player, faction).contains(offer.playerWar())
         || !offer.factionWar().isEmpty() && !factionTargets(player, faction).contains(offer.factionWar())) {
         return refuse("deal.ancientwarfare.bad_war", name);
      } else if (!offer.factionWar().isEmpty() && standing < 0) {
         return refuse("deal.ancientwarfare.no_war_for_you", name);
      } else if (offer.factionGold() * 10 > data.treasury(faction) / 2) {
         return refuse("deal.ancientwarfare.too_poor", name);
      } else if (offer.treaty() && treaty == RelationState.ALLIANCE) {
         return refuse("deal.ancientwarfare.no_treaty", name);
      } else if (offer.treaty() && treaty == RelationState.PACT && !PlayerResearch.has(player, PlayerResearch.DIPLOMACY)) {
         return new Weighed(REFUSE, PlayerResearch.needs(PlayerResearch.DIPLOMACY));
      } else if (shortOf(player, offer) >= 0) {
         return new Weighed(REFUSE, Component.translatable("deal.ancientwarfare.no_goods", offer.items(shortOf(player, offer), true), goodsName(shortOf(player, offer))));
      } else if (stingy(data, faction, offer)) {
         return refuse("deal.ancientwarfare.no_spare_goods", name);
      } else if (offer.bond() != NO_BOND && offer.treaty()) {
         return refuse("deal.ancientwarfare.bond_and_treaty", name);
      } else if (offer.bond() == VASSAL && (FactionVassals.isVassal(level, player.getUUID(), faction) || faction.equals(FactionVassals.overlord(level, player.getUUID())))) {
         return refuse("deal.ancientwarfare.already_bound", name);
      } else if (offer.bond() == VASSAL && settlements(data, faction) >= FactionVassals.PROUD_SETTLEMENTS) {
         return refuse("deal.ancientwarfare.too_proud", name);
      } else if (offer.bond() == VASSAL && data.villages().values().stream().filter(player.getUUID()::equals).count() < FactionVassals.LORD_VILLAGES) {
         return new Weighed(REFUSE, Component.translatable("deal.ancientwarfare.too_small_lord", name, FactionVassals.LORD_VILLAGES));
      } else if (offer.bond() == FEALTY && (FactionVassals.overlord(level, player.getUUID()) != null || FactionVassals.isVassal(level, player.getUUID(), faction))) {
         return refuse("deal.ancientwarfare.already_bound", name);
      } else if (offer.bond() == FEALTY && settlements(data, faction) < FactionVassals.PROTECTOR_SETTLEMENTS) {
         return new Weighed(REFUSE, Component.translatable("deal.ancientwarfare.too_weak_protector", name, FactionVassals.PROTECTOR_SETTLEMENTS));
      } else if (offer.bond() == MARRIAGE && FactionFamilies.cannotMarry(level, player.getUUID(), faction) != null) {
         return refuse(FactionFamilies.cannotMarry(level, player.getUUID(), faction), name);
      } else if (offer.bond() == GIVE_DAUGHTER && FactionFamilies.cannotGiveDaughter(level, player.getUUID(), faction) != null) {
         return refuse(FactionFamilies.cannotGiveDaughter(level, player.getUUID(), faction), name);
      } else if ((offer.bond() == MARRIAGE || offer.bond() == GIVE_DAUGHTER) && treaty == RelationState.WAR) {
         return refuse("deal.ancientwarfare.at_war", name);
      } else if (offer.trade() && (standings.hasTrade(player.getUUID(), faction) || !PlayerResearch.has(player, "trade"))) {
         return refuse("deal.ancientwarfare.no_trade", name);
      }

      // An honest ruler weighs a player's word, kept or broken, twice as heavily.
      boolean honest = FactionPeople.has(FactionRulers.ruler(data, faction, level.getRandom()).character(), FactionPeople.Character.HONEST);
      double trust = Mth.clamp(1.0 + 0.15 * word * (honest ? 2 : 1), 0.25, 1.5);
      double goldTaste = (trait == FactionRulers.Trait.GREEDY ? 1.5 : 1.0) * (FactionCourt.wantsGold(level, faction) ? 1.3 : 1.0) * (data.treasury(faction) < 200 ? 1.3 : 1.0);
      double costs = 0.0;
      double gains = offer.playerGold() * 10 * goldTaste + offer.tribute() * 10 * TRIBUTE_WEEKS * goldTaste * trust;
      int[] stores = data.stores(faction);
      for (int kind = 0; kind < KINDS; kind++) {
         // A faction short of a good pays more for it and parts with it less gladly.
         double worth = GOODS_PRICE[kind] * Mth.clamp(300.0 / (stores[kind] + 100.0), 0.5, 2.0);
         gains += offer.items(kind, true) * worth;
         costs += offer.items(kind, false) * worth * 1.2;
      }

      if (!offer.playerWar().isEmpty()) {
         gains += WAR_WORTH * trust * (FactionCourt.wantsWar(level, faction, offer.playerWar()) ? 2.0 : 1.0);
      }

      if (FactionPlots.leverage(level, player.getUUID(), faction)) {
         // The ruler's secret weighs on the scales.
         gains += FactionPlots.LEVERAGE;
      }

      // A friend's offer counts for more, a foe's for less.
      gains *= Mth.clamp(1.0 + standing / 200.0, 0.5, 1.5);
      int temper = trait.treatyStanding - FactionCourt.treatyBonus(level, faction, player.getUUID());
      costs += offer.factionGold() * 10 * (trait == FactionRulers.Trait.GREEDY ? 1.5 : 1.0);
      if (offer.treaty()) {
         int need = switch (treaty) {
            case WAR -> PlayerDiplomacy.PEACE_STANDING;
            case NEUTRAL -> PlayerDiplomacy.PACT_STANDING;
            default -> PlayerDiplomacy.ALLIANCE_STANDING;
         } + temper;
         costs += Math.max(0, need - standing) * STANDING_PRICE + (treaty == RelationState.WAR ? 0 : TREATY_PRICE);
      }

      if (offer.bond() == VASSAL) {
         int soldiers = data.settlements().stream().filter(settlement -> settlement.faction().equals(faction)).mapToInt(settlement -> settlement.garrison).sum();
         double price = settlements(data, faction) * VASSAL_SETTLEMENT_PRICE + soldiers * VASSAL_SOLDIER_PRICE;
         // A beaten foe bows sooner; a proud ruler later.
         costs += price * (treaty == RelationState.WAR ? 0.6 : 1.0) * (trait == FactionRulers.Trait.WARLIKE ? 1.3 : 1.0) + Math.max(0, -standing) * STANDING_PRICE / 2.0;
      } else if (offer.bond() == GIVE_DAUGHTER) {
         gains += FactionFamilies.daughterWorth();
      } else if (offer.bond() == MARRIAGE) {
         costs += Math.max(0.0, FactionFamilies.price(data, faction, standing)) + Math.max(0, -standing) * STANDING_PRICE;
      } else if (offer.bond() == FEALTY) {
         gains += FEALTY_WORTH;
         costs += Math.max(0, -standing) * STANDING_PRICE;
      }

      if (offer.trade()) {
         costs += Math.max(0, PlayerDiplomacy.TRADE_STANDING - standing) * STANDING_PRICE + TREATY_PRICE;
      }

      if (!offer.factionWar().isEmpty()) {
         double eager = FactionCourt.wantsWar(level, faction, offer.factionWar()) ? 0.25 : 1.0;
         costs += WAR_WORTH * eager / Math.max(0.25, FactionCourt.warFactor(level, faction));
      }

      double balance = gains - costs;
      if (balance >= 0.0) {
         return new Weighed(ACCEPT, Component.translatable("deal.ancientwarfare.accept", name));
      }

      int missing = Mth.ceil(-balance / 10.0 / goldTaste);
      if (missing <= MAX_MISSING) {
         return new Weighed(MORE, Component.translatable("deal.ancientwarfare.more", name, missing));
      }

      return refuse("deal.ancientwarfare.refuse", name);
   }

   private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item>[] GOODS_TAGS = goodsTags();
   private static final net.minecraft.world.item.Item[] GOODS_ITEMS = {
      net.minecraft.world.item.Items.OAK_LOG, net.minecraft.world.item.Items.COBBLESTONE, net.minecraft.world.item.Items.IRON_INGOT
   };

   @SuppressWarnings("unchecked")
   private static net.minecraft.tags.TagKey<net.minecraft.world.item.Item>[] goodsTags() {
      return new net.minecraft.tags.TagKey[]{
         net.minecraft.tags.ItemTags.LOGS, net.minecraftforge.common.Tags.Items.COBBLESTONE, net.minecraftforge.common.Tags.Items.INGOTS_IRON
      };
   }

   public static Component goodsName(int kind) {
      return Component.translatable("supply.ancientwarfare.goods." + (kind == 0 ? "wood" : kind == 1 ? "stone" : "iron"));
   }

   /** The first kind of goods the player offers more of than they carry, or -1. */
   private static int shortOf(ServerPlayer player, Offer offer) {
      for (int kind = 0; kind < KINDS; kind++) {
         if (!player.isCreative() && offer.items(kind, true) > FactionCourt.heldItems(player, GOODS_TAGS[kind])) {
            return kind;
         }
      }

      return -1;
   }

   /** Whether the faction is asked for more of a good than half its stores. */
   private static boolean stingy(FactionWorldData data, String faction, Offer offer) {
      int[] stores = data.stores(faction);
      for (int kind = 0; kind < KINDS; kind++) {
         if (offer.items(kind, false) > stores[kind] / 2) {
            return true;
         }
      }

      return false;
   }

   private static int settlements(FactionWorldData data, String faction) {
      return (int)data.settlements().stream().filter(settlement -> settlement.faction().equals(faction)).count();
   }

   private static Weighed refuse(String key, Component name) {
      return new Weighed(REFUSE, Component.translatable(key, name));
   }

   /** Carries the deal out; returns why it fell through, or null. */
   private static Component strike(ServerPlayer player, String faction, Offer offer) {
      ServerLevel level = player.serverLevel();
      FactionStandingData standings = FactionStandingData.get(level);
      FactionWorldData data = FactionWorldData.get(level);
      if (offer.playerGold() > 0 && !Coins.take(player, offer.playerGold() * 10)) {
         return Component.translatable("deal.ancientwarfare.no_gold", offer.playerGold());
      }

      if (shortOf(player, offer) >= 0) {
         return Component.translatable("deal.ancientwarfare.no_goods", offer.items(shortOf(player, offer), true), goodsName(shortOf(player, offer)));
      }

      int[] stores = data.stores(faction);
      for (int kind = 0; kind < KINDS; kind++) {
         if (!player.isCreative()) {
            FactionCourt.takeItems(player, GOODS_TAGS[kind], offer.items(kind, true));
         }

         stores[kind] += offer.items(kind, true) - offer.items(kind, false);
         int given = offer.items(kind, false);
         while (given > 0) {
            int count = Math.min(64, given);
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(GOODS_ITEMS[kind], count);
            if (!player.getInventory().add(stack)) {
               player.drop(stack, false);
            }

            given -= count;
         }
      }

      data.setTreasury(faction, data.treasury(faction) + offer.playerGold() * 10 - offer.factionGold() * 10);
      if (offer.factionGold() > 0) {
         Coins.give(player, offer.factionGold() * 10);
      }

      if (offer.treaty()) {
         RelationState treaty = standings.getTreaty(player.getUUID(), faction);
         RelationState next = treaty == RelationState.WAR ? RelationState.NEUTRAL : treaty == RelationState.NEUTRAL ? RelationState.PACT : RelationState.ALLIANCE;
         standings.setTreaty(player.getUUID(), faction, next);
         if (next == RelationState.NEUTRAL) {
            standings.setStanding(player.getUUID(), faction, Math.max(standings.getStanding(player.getUUID(), faction), PlayerDiplomacy.PEACE_RESTORED_STANDING));
         }
      }

      if (offer.trade()) {
         standings.setTrade(player.getUUID(), faction, true);
      }

      if (offer.bond() == VASSAL) {
         FactionVassals.addVassal(level, player, faction);
      } else if (offer.bond() == FEALTY) {
         FactionVassals.swear(level, player, faction);
      } else if (offer.bond() == MARRIAGE) {
         FactionFamilies.marry(level, player, faction);
      } else if (offer.bond() == GIVE_DAUGHTER) {
         FactionFamilies.giveDaughter(level, player, faction);
      }

      if (!offer.factionWar().isEmpty() && FactionDiplomacy.setState(level, faction, offer.factionWar(), RelationState.WAR)) {
         FactionNews.relationChanged(level, faction, offer.factionWar(), RelationState.WAR, "deal", player.getDisplayName());
      }

      FactionDeals deals = get(level);
      long today = data.lastSimulatedDay();
      if (!offer.playerWar().isEmpty()) {
         deals.promises.add(new Promise(player.getUUID(), faction, Kind.WAR, offer.playerWar(), 0, today + WAR_PROMISE_DAYS, 1));
      }

      if (offer.tribute() > 0) {
         deals.promises.add(new Promise(player.getUUID(), faction, Kind.TRIBUTE, "", offer.tribute(), today + WEEK_DAYS, TRIBUTE_WEEKS));
      }

      deals.setDirty();
      data.setDirty();
      FactionPlots.spendLeverage(level, player.getUUID(), faction);
      standings.adjustStanding(player.getUUID(), faction, FactionCourt.weigh(level, player.getUUID(), faction, "deal", DEAL_STANDING));
      AncientWarfareCore.LOG.info("{} strikes a deal with {}: {}", player.getName().getString(), faction, offer);
      return null;
   }

   /** A promise of war given outside a deal, as to an envoy. */
   static void promiseWar(ServerLevel level, UUID player, String faction, String target) {
      FactionDeals deals = get(level);
      deals.promises.add(new Promise(player, faction, Kind.WAR, target, 0, FactionWorldData.get(level).lastSimulatedDay() + WAR_PROMISE_DAYS, 1));
      deals.setDirty();
   }

   /** Daily: promises fall due, and are kept or broken. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionDeals deals = get(level);
      FactionStandingData standings = FactionStandingData.get(level);
      long today = data.lastSimulatedDay();
      List<Promise> done = new ArrayList<>();
      for (Promise promise : deals.promises) {
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(promise.player);
         if (standings.getTreaty(promise.player, promise.faction) == RelationState.WAR) {
            // At war with the faction the promise was made to: it is void.
            done.add(promise);
         } else if (promise.kind == Kind.WAR) {
            if (standings.getTreaty(promise.player, promise.target) == RelationState.WAR) {
               deals.kept(level, promise, player);
               done.add(promise);
            } else if (today > promise.due) {
               deals.broken(level, promise, player);
               done.add(promise);
            }
         } else if (today >= promise.due) {
            if (player != null && Coins.take(player, promise.amount * 10)) {
               data.setTreasury(promise.faction, data.treasury(promise.faction) + promise.amount * 10);
               data.setDirty();
               promise.left--;
               promise.due += WEEK_DAYS;
               deals.setDirty();
               player.sendSystemMessage(Component.translatable("deal.ancientwarfare.tribute_paid", promise.amount, FactionNews.factionName(promise.faction), promise.left).withStyle(ChatFormatting.GRAY));
               if (promise.left <= 0) {
                  deals.kept(level, promise, player);
                  done.add(promise);
               }
            } else if (player != null || today > promise.due + TRIBUTE_GRACE) {
               deals.broken(level, promise, player);
               done.add(promise);
            }
         }
      }

      if (!done.isEmpty()) {
         deals.promises.removeAll(done);
         deals.setDirty();
      }
   }

   private void kept(ServerLevel level, Promise promise, ServerPlayer player) {
      this.reliability.merge(promise.player, 1, (a, b) -> Math.min(MAX_RELIABILITY, a + b));
      FactionStandingData.get(level).adjustStanding(promise.player, promise.faction, KEPT_STANDING);
      if (player != null) {
         player.sendSystemMessage(Component.translatable("deal.ancientwarfare.kept", FactionNews.factionName(promise.faction)).withStyle(ChatFormatting.GREEN));
      }
   }

   private void broken(ServerLevel level, Promise promise, ServerPlayer player) {
      this.reliability.merge(promise.player, -2, (a, b) -> Math.max(MIN_RELIABILITY, a + b));
      FactionStandingData standings = FactionStandingData.get(level);
      standings.adjustStanding(promise.player, promise.faction, BROKEN_STANDING);
      for (String other : standings.knownFactions(promise.player)) {
         if (!other.equals(promise.faction)) {
            standings.adjustStanding(promise.player, other, BROKEN_ELSEWHERE);
         }
      }

      Component who = player != null ? player.getDisplayName() : Component.literal(promise.player.toString().substring(0, 8));
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.promise_broken", who, FactionNews.factionName(promise.faction)));
   }

   private static FactionDeals load(CompoundTag tag) {
      FactionDeals deals = new FactionDeals();
      for (Tag element : tag.getList("Promises", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         try {
            deals.promises.add(
               new Promise(
                  entry.getUUID("Player"), entry.getString("Faction"), Kind.valueOf(entry.getString("Kind")), entry.getString("Target"), entry.getInt("Amount"),
                  entry.getLong("Due"), entry.getInt("Left")
               )
            );
         } catch (IllegalArgumentException e) {
            AncientWarfareCore.LOG.warn("Dropping unknown promise {}", entry);
         }
      }

      CompoundTag reliability = tag.getCompound("Reliability");
      for (String player : reliability.getAllKeys()) {
         try {
            deals.reliability.put(UUID.fromString(player), reliability.getInt(player));
         } catch (IllegalArgumentException ignored) {
         }
      }

      return deals;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag promises = new ListTag();
      for (Promise promise : this.promises) {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Player", promise.player);
         entry.putString("Faction", promise.faction);
         entry.putString("Kind", promise.kind.name());
         entry.putString("Target", promise.target);
         entry.putInt("Amount", promise.amount);
         entry.putLong("Due", promise.due);
         entry.putInt("Left", promise.left);
         promises.add(entry);
      }

      tag.put("Promises", promises);
      CompoundTag reliability = new CompoundTag();
      this.reliability.forEach((player, value) -> reliability.putInt(player.toString(), value));
      tag.put("Reliability", reliability);
      return tag;
   }
}
