package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * Small happenings in a town while a player walks its streets: a thief caught with a loaf, a brawl
 * outside the tavern, a widow behind on her rent, a fire in a barn. A townsperson comes up and tells
 * of it, and the player picks what to do from the buttons in chat; each choice costs or earns silver,
 * goodwill with the town's country, or the town's order, health and purse, and some are a gamble.
 */
public final class TownEncounters {
   private static final long CHECK_TICKS = 200L;
   /** The least time between two happenings for one player. */
   private static final long REST_TICKS = 2400L;
   private static final double CHANCE = 0.3;
   private static final int RANGE = 64;
   /** How long the buttons stay good. */
   private static final long ANSWER_TICKS = 2400L;

   private enum When {
      ANY, NIGHT, PLAGUE, HUNGRY, RICH, POOR, WINTER, DISORDER
   }

   /** What a choice does: silver to the player (or taken, if negative), standing, the town's order, health and purse, and an item given. */
   private record Out(int coins, int standing, int order, int health, int wealth, String give, int count) {
      static Out of(int coins, int standing, int order, int health, int wealth) {
         return new Out(coins, standing, order, health, wealth, "", 0);
      }
   }

   /** A choice: its price in silver, an item it needs, how likely it goes well, and what follows either way. */
   private record Choice(String key, int price, String need, int needCount, double chance, Out ok, Out bad) {
      static Choice of(String key, Out ok) {
         return new Choice(key, 0, "", 0, 1.0, ok, ok);
      }

      static Choice paid(String key, int price, Out ok) {
         return new Choice(key, price, "", 0, 1.0, ok, ok);
      }

      static Choice risky(String key, int price, double chance, Out ok, Out bad) {
         return new Choice(key, price, "", 0, chance, ok, bad);
      }

      static Choice bring(String key, String need, int count, Out ok) {
         return new Choice(key, 0, need, count, 1.0, ok, ok);
      }
   }

   private record Encounter(String key, When when, Choice... choices) {
   }

   private static final Out NOTHING = Out.of(0, 0, 0, 0, 0);

   private static final Encounter[] ENCOUNTERS = {
      new Encounter("thief", When.ANY,
         Choice.of("pardon", Out.of(0, 1, -2, 0, 0)), Choice.paid("pay_for", 5, Out.of(0, 2, 1, 0, 0)), Choice.of("hand_over", Out.of(0, 1, 3, 0, 0))),
      new Encounter("lost_child", When.ANY,
         Choice.risky("search", 0, 0.7, Out.of(3, 3, 1, 0, 0), NOTHING), Choice.of("ignore", Out.of(0, -1, 0, 0, 0))),
      new Encounter("brawl", When.NIGHT,
         Choice.risky("break_up", 0, 0.6, Out.of(0, 2, 3, 0, 0), Out.of(0, 0, -1, -1, 0)), Choice.paid("buy_round", 8, Out.of(0, 2, 2, 0, 0)), Choice.of("walk_away", NOTHING)),
      new Encounter("widow", When.POOR,
         Choice.paid("pay_rent", 10, Out.of(0, 3, 1, 0, 0)), Choice.of("refuse", NOTHING)),
      new Encounter("merchant", When.ANY,
         Choice.risky("buy", 15, 0.6, new Out(0, 0, 0, 0, 2, "minecraft:emerald", 2), Out.of(0, 0, 0, 0, 2)), Choice.of("decline", NOTHING)),
      new Encounter("healer", When.ANY,
         Choice.bring("give_apples", "minecraft:apple", 4, Out.of(2, 2, 0, 5, 0)), Choice.of("decline", NOTHING)),
      new Encounter("bard", When.ANY,
         Choice.paid("pay_bard", 3, Out.of(0, 1, 2, 0, 0)), Choice.of("mock", Out.of(0, -1, -1, 0, 0))),
      new Encounter("neighbours", When.ANY,
         Choice.risky("judge_first", 0, 0.5, Out.of(0, 2, 2, 0, 0), Out.of(0, 0, -2, 0, 0)),
         Choice.risky("judge_second", 0, 0.5, Out.of(0, 2, 2, 0, 0), Out.of(0, 0, -2, 0, 0)), Choice.of("split", Out.of(0, 1, 1, 0, 0))),
      new Encounter("dice", When.NIGHT,
         Choice.risky("play", 10, 0.45, Out.of(22, 0, 0, 0, 0), NOTHING), Choice.of("refuse", NOTHING)),
      new Encounter("preacher", When.ANY,
         Choice.of("listen", Out.of(0, 1, -1, 0, 0)), Choice.of("report", Out.of(0, 1, 3, 0, 0)), Choice.of("ignore", NOTHING)),
      new Encounter("apprentice", When.RICH,
         Choice.paid("sponsor", 20, Out.of(0, 3, 0, 0, 15)), Choice.of("decline", NOTHING)),
      new Encounter("fire", When.ANY,
         Choice.risky("help", 0, 0.8, Out.of(0, 4, 2, 0, 5), Out.of(0, 2, 0, -3, -5)), Choice.of("flee", Out.of(0, -2, -1, 0, -10))),
      new Encounter("wolves", When.NIGHT,
         Choice.risky("hunt", 0, 0.7, new Out(4, 3, 1, 0, 0, "minecraft:leather", 3), Out.of(0, 0, 0, -1, 0)), Choice.of("ignore", Out.of(0, -1, 0, 0, -3))),
      new Encounter("bully", When.ANY,
         Choice.risky("intervene", 0, 0.5, Out.of(0, 3, 2, 0, 0), Out.of(0, -3, 0, 0, 0)), Choice.of("stay_out", NOTHING)),
      new Encounter("pilgrim", When.ANY,
         Choice.paid("alms", 5, Out.of(0, 2, 0, 0, 0)), Choice.risky("tale", 0, 0.4, new Out(0, 0, 0, 0, 0, "minecraft:map", 1), NOTHING), Choice.of("refuse", NOTHING)),
      new Encounter("sick_child", When.PLAGUE,
         Choice.bring("milk", "minecraft:milk_bucket", 1, Out.of(0, 3, 0, 5, 0)), Choice.paid("pay_healer", 12, Out.of(0, 2, 0, 3, 0)), Choice.of("decline", NOTHING)),
      new Encounter("horse", When.ANY,
         Choice.risky("catch", 0, 0.6, Out.of(8, 2, 0, 0, 0), NOTHING), Choice.of("let_go", NOTHING)),
      new Encounter("love_letter", When.ANY,
         Choice.of("deliver", Out.of(2, 2, 1, 0, 0)), Choice.of("read_aloud", Out.of(0, -1, -2, 0, 0))),
      new Encounter("baker", When.HUNGRY,
         Choice.bring("sell_wheat", "minecraft:wheat", 16, Out.of(10, 2, 1, 0, 0)), Choice.of("decline", NOTHING)),
      new Encounter("guard_bribe", When.DISORDER,
         Choice.of("report", Out.of(0, 2, 3, 0, 0)), Choice.of("take_cut", Out.of(10, -3, -3, 0, -5))),
      new Encounter("contest", When.ANY,
         Choice.risky("enter", 2, 0.4, Out.of(15, 2, 1, 0, 0), NOTHING), Choice.of("cheer", Out.of(0, 0, 1, 0, 0))),
      new Encounter("veteran", When.ANY,
         Choice.of("listen", Out.of(0, 1, 0, 0, 0)), Choice.paid("buy_sword", 12, new Out(0, 0, 0, 0, 3, "minecraft:iron_sword", 1))),
      new Encounter("noble_beggar", When.ANY,
         Choice.risky("help", 15, 0.3, Out.of(60, 3, 0, 0, 0), Out.of(0, 2, 0, 0, 0)), Choice.of("decline", NOTHING)),
      new Encounter("hungry_family", When.HUNGRY,
         Choice.bring("give_bread", "minecraft:bread", 4, Out.of(0, 3, 1, 1, 0)), Choice.paid("give_coin", 6, Out.of(0, 2, 0, 0, 0)), Choice.of("decline", NOTHING)),
      new Encounter("rich_feast", When.RICH,
         Choice.paid("attend", 5, Out.of(0, 3, 0, 0, 3)), Choice.of("decline", NOTHING)),
      new Encounter("cold_night", When.WINTER,
         Choice.bring("firewood", "#minecraft:logs", 12, Out.of(4, 3, 0, 3, 0)), Choice.of("decline", NOTHING)),
      new Encounter("well", When.ANY,
         Choice.risky("climb_down", 0, 0.65, Out.of(6, 2, 0, 2, 0), Out.of(0, 0, 0, -1, 0)), Choice.of("leave", NOTHING)),
      new Encounter("runaway_bride", When.ANY,
         Choice.of("hide_her", Out.of(0, -1, -2, 0, 0)), Choice.of("return_her", Out.of(5, 1, 1, 0, 0)), Choice.of("stay_out", NOTHING)),
      new Encounter("cartographer", When.ANY,
         Choice.paid("buy_map", 10, new Out(0, 0, 0, 0, 3, "minecraft:map", 1)), Choice.bring("sell_paper", "minecraft:paper", 12, Out.of(8, 1, 0, 0, 0)), Choice.of("decline", NOTHING)),
      new Encounter("alchemist", When.ANY,
         Choice.risky("taste", 4, 0.5, Out.of(0, 1, 0, 2, 2), Out.of(0, 0, 0, -2, 2)), Choice.bring("sell_eyes", "minecraft:spider_eye", 4, Out.of(8, 1, 0, 0, 0)), Choice.of("decline", NOTHING))
   };

   /** A happening waiting on the player's answer; teller is the townsperson bringing it, null when none was near. */
   private record Pending(int id, String encounter, int town, long until, UUID teller) {
   }

   /** Townsfolk on their way to tell a player something; the screen opens when they arrive. */
   private static final Map<UUID, Long> ARRIVE_BY = new HashMap<>();
   /** How long a teller may take to reach the player before the news comes anyway. */
   private static final long WALK_TICKS = 400L;

   private static final Map<UUID, Pending> PENDING = new HashMap<>();
   private static final Map<UUID, Long> LAST = new HashMap<>();
   private static int nextId;

   private TownEncounters() {
   }

   private static boolean fits(ServerLevel level, FactionWorldData.Settlement town, When when) {
      return switch (when) {
         case ANY -> true;
         case NIGHT -> level.isNight();
         case PLAGUE -> town.plague() > 0;
         case HUNGRY -> town.famine() > 0 || town.food() < (town.population() + town.garrison()) * 3;
         case RICH -> town.wealth() >= 300;
         case POOR -> town.wealth() < 100;
         case WINTER -> FactionSeasons.season(level) == FactionSeasons.Season.WINTER;
         case DISORDER -> town.order() < 60;
      };
   }

   private static Encounter byKey(String key) {
      for (Encounter encounter : ENCOUNTERS) {
         if (encounter.key.equals(key)) {
            return encounter;
         }
      }

      return null;
   }

   /** Now and then, to a player walking through a friendly town, something happens. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % 20L == 0L && !ARRIVE_BY.isEmpty()) {
         walkTellers(level, data, level.getGameTime());
      }

      if (level.getGameTime() % CHECK_TICKS != 0L || level.players().isEmpty()) {
         return;
      }

      long now = level.getGameTime();
      PENDING.values().removeIf(pending -> pending.until < now);
      RandomSource random = level.getRandom();
      FactionStandingData standings = FactionStandingData.get(level);
      for (ServerPlayer player : level.players()) {
         if (PENDING.containsKey(player.getUUID()) || now - LAST.getOrDefault(player.getUUID(), -REST_TICKS) < REST_TICKS || random.nextDouble() >= CHANCE) {
            continue;
         }

         FactionWorldData.Settlement town = null;
         for (FactionWorldData.Settlement settlement : data.settlements()) {
            if (FactionTownLife.isTown(settlement) && settlement.horizontalDistanceSquared(player.getX(), player.getZ()) < (double)RANGE * RANGE) {
               town = settlement;
               break;
            }
         }

         if (town == null || FactionDiplomacy.isOutlaw(town.faction()) || standings.isHostile(player.getUUID(), town.faction())) {
            continue;
         }

         List<Encounter> fitting = new ArrayList<>();
         for (Encounter encounter : ENCOUNTERS) {
            if (fits(level, town, encounter.when)) {
               fitting.add(encounter);
            }
         }

         Encounter encounter = fitting.get(random.nextInt(fitting.size()));
         start(player, town, encounter, now);
      }
   }

   private static void start(ServerPlayer player, FactionWorldData.Settlement town, Encounter encounter, long now) {
      // Someone from the town comes up to the player to tell of it, rather than a line in the chat.
      FactionNpcEntity teller = teller(player, town);
      Pending pending = new Pending(++nextId, encounter.key, town.id(), now + ANSWER_TICKS + WALK_TICKS, teller == null ? null : teller.getUUID());
      PENDING.put(player.getUUID(), pending);
      LAST.put(player.getUUID(), now);
      TownHappenings.begin(player, town, encounter.key);
      if (teller == null) {
         tell(player, town, pending, null);
         return;
      }

      teller.setGlowingTag(true);
      teller.getNavigation().moveTo(player, 0.7);
      ARRIVE_BY.put(player.getUUID(), now + WALK_TICKS);
   }

   /** A grown townsperson of the town near the player, not a soldier or a council member. */
   private static FactionNpcEntity teller(ServerPlayer player, FactionWorldData.Settlement town) {
      List<FactionNpcEntity> near = player.serverLevel().getEntitiesOfClass(FactionNpcEntity.class, player.getBoundingBox().inflate(28.0, 8.0, 28.0),
         npc -> npc.isAlive() && !npc.isBaby() && npc.getFactionName().equals(town.faction()) && FactionSimulation.isCivilian(npc)
            && npc.getPersistentData().getString(CouncilHalls.COUNCIL_TAG).isEmpty());
      FactionNpcEntity best = null;
      for (FactionNpcEntity npc : near) {
         if (best == null || npc.distanceToSqr(player) < best.distanceToSqr(player)) {
            best = npc;
         }
      }

      return best;
   }

   private static FactionNpcEntity tellerOf(ServerLevel level, Pending pending) {
      if (pending.teller == null) {
         return null;
      }

      net.minecraft.world.entity.Entity entity = level.getEntity(pending.teller);
      return entity instanceof FactionNpcEntity npc && npc.isAlive() ? npc : null;
   }

   /** Tellers walk on toward their player; once beside them, or out of time, they speak. */
   private static void walkTellers(ServerLevel level, FactionWorldData data, long now) {
      java.util.Iterator<Map.Entry<UUID, Long>> iterator = ARRIVE_BY.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<UUID, Long> entry = iterator.next();
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
         Pending pending = PENDING.get(entry.getKey());
         if (player == null || pending == null) {
            iterator.remove();
            continue;
         }

         FactionNpcEntity teller = tellerOf(level, pending);
         if (teller != null && teller.distanceToSqr(player) > 9.0 && now < entry.getValue()) {
            teller.getNavigation().moveTo(player, 0.7);
            continue;
         }

         iterator.remove();
         if (teller != null) {
            teller.getLookControl().setLookAt(player);
            teller.getNavigation().stop();
         }

         tell(player, data.settlement(pending.town), pending, teller);
      }
   }

   /** The player talks to the townsperson who brought news they have not answered: it is told again. */
   public static boolean tellAgain(ServerPlayer player, FactionNpcEntity npc) {
      Pending pending = PENDING.get(player.getUUID());
      if (pending == null || pending.teller == null || !pending.teller.equals(npc.getUUID())) {
         return false;
      }

      ARRIVE_BY.remove(player.getUUID());
      tell(player, FactionWorldData.get(player.serverLevel()).settlement(pending.town), pending, npc);
      return true;
   }

   /** Opens the happening on the player's screen, in the teller's words, with what may be done. */
   private static void tell(ServerPlayer player, FactionWorldData.Settlement town, Pending pending, FactionNpcEntity teller) {
      Encounter encounter = byKey(pending.encounter);
      if (encounter == null || town == null) {
         return;
      }

      List<String> keys = new ArrayList<>();
      List<Component> labels = new ArrayList<>();
      List<Component> costs = new ArrayList<>();
      for (Choice choice : encounter.choices) {
         keys.add(choice.key);
         labels.add(Component.translatable("encounter.ancientwarfare." + encounter.key + "." + choice.key));
         if (choice.price > 0) {
            costs.add(Component.translatable("encounter.ancientwarfare.price", choice.price));
         } else if (!choice.need.isEmpty()) {
            costs.add(Component.literal(choice.needCount + "× ").append(itemName(choice.need)));
         } else {
            costs.add(Component.empty());
         }
      }

      Component speaker = teller != null ? teller.getDisplayName() : FactionNews.named(town);
      net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork.sendEncounter(player, new net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork.EncounterPacket(
         pending.id, speaker, Component.translatable("encounter.ancientwarfare." + encounter.key, FactionNews.named(town)), keys, labels, costs));
   }

   /** For testing: start a happening at once, by key or at random. */
   public static void force(ServerPlayer player, String key) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement best = null;
      double distance = Double.MAX_VALUE;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         double d = settlement.horizontalDistanceSquared(player.getX(), player.getZ());
         if (FactionTownLife.isTown(settlement) && d < distance) {
            best = settlement;
            distance = d;
         }
      }

      Encounter encounter = byKey(key);
      if (best != null) {
         start(player, best, encounter != null ? encounter : ENCOUNTERS[level.getRandom().nextInt(ENCOUNTERS.length)], level.getGameTime());
      }
   }

   private static Component itemName(String item) {
      if (item.startsWith("#")) {
         return Component.translatable("quest.ancientwarfare.item." + item.substring(item.indexOf(':') + 1));
      }

      return BuiltInRegistries.ITEM.get(new ResourceLocation(item)).getDescription();
   }

   private static int count(ServerPlayer player, String item) {
      int found = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         if (TownsfolkFavours.matchesItem(player.getInventory().getItem(slot), item)) {
            found += player.getInventory().getItem(slot).getCount();
         }
      }

      return found;
   }

   private static void take(ServerPlayer player, String item, int wanted) {
      for (int slot = 0; slot < player.getInventory().getContainerSize() && wanted > 0; slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (TownsfolkFavours.matchesItem(stack, item)) {
            int used = Math.min(wanted, stack.getCount());
            stack.shrink(used);
            wanted -= used;
         }
      }
   }

   /** The player clicked one of the buttons. */
   public static void answer(ServerPlayer player, int id, String choiceKey) {
      Pending pending = PENDING.get(player.getUUID());
      if (pending == null || pending.id != id) {
         player.sendSystemMessage(Component.translatable("encounter.ancientwarfare.gone").withStyle(ChatFormatting.GRAY));
         return;
      }

      Encounter encounter = byKey(pending.encounter);
      Choice choice = null;
      for (Choice candidate : encounter == null ? new Choice[0] : encounter.choices) {
         if (candidate.key.equals(choiceKey)) {
            choice = candidate;
         }
      }

      if (choice == null) {
         return;
      }

      if (!choice.need.isEmpty() && count(player, choice.need) < choice.needCount) {
         player.sendSystemMessage(Component.translatable("encounter.ancientwarfare.lacking", choice.needCount, itemName(choice.need)).withStyle(ChatFormatting.RED));
         return;
      }

      if (choice.price > 0 && !Coins.take(player, choice.price)) {
         player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
         return;
      }

      if (!choice.need.isEmpty()) {
         take(player, choice.need, choice.needCount);
      }

      PENDING.remove(player.getUUID());
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement town = null;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.id() == pending.town) {
            town = settlement;
         }
      }

      boolean ok = level.getRandom().nextDouble() < choice.chance;
      Out out = ok ? choice.ok : choice.bad;
      if (out.coins > 0) {
         Coins.give(player, out.coins);
      }

      if (!out.give.isEmpty()) {
         Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(out.give));
         player.getInventory().placeItemBackInInventory(new ItemStack(item, out.count));
      }

      if (town != null) {
         if (out.standing != 0) {
            FactionStandingData.get(level).adjustStanding(player.getUUID(), town.faction(), out.standing);
         }

         town.order = Math.max(0, Math.min(100, town.order + out.order));
         town.health = Math.max(0, Math.min(100, town.health + out.health));
         town.wealth = Math.max(0, town.wealth + out.wealth);
         data.setDirty();
      }

      String outcome = "encounter.ancientwarfare." + encounter.key + "." + choice.key + (choice.chance < 1.0 ? (ok ? ".ok" : ".bad") : ".done");
      player.displayClientMessage(Component.translatable(outcome).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), true);
      FactionNpcEntity teller = tellerOf(level, pending);
      if (teller != null) {
         teller.setGlowingTag(false);
      }

      if (town != null) {
         TownHappenings.resolve(player, town, encounter.key, choice.key, ok);
      }
   }

   /** A new server starts from scratch. */
   public static void reset() {
      PENDING.clear();
      ARRIVE_BY.clear();
      LAST.clear();
   }
}
