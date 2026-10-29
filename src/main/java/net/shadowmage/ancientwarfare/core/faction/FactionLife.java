package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

/**
 * The daily life of settlements: harvests and food stores, bad harvests, disease, order and the
 * loyalty of people under a new master, and the unrest and revolts that follow when those run low.
 * Villages a player took from a faction live by the same rules, with the player's soldiers for a
 * garrison and the tax the player sets.
 */
public final class FactionLife {
   /** Rations a working person brings in a day, before the kind of place counts. */
   private static final double YIELD = 1.6;
   private static final int STORE_DAYS = 8;
   /** The share of a farmer's yield a soldier in garrison brings in. */
   private static final double GARRISON_WORK = 0.4;
   private static final double FAMINE_YIELD = 0.35;
   private static final double PLAGUE_YIELD = 0.7;
   private static final double FAMINE_CHANCE = 0.02;
   private static final int FAMINE_RANGE = 900;
   private static final double PLAGUE_CHANCE = 0.003;
   private static final double SICKLY_PLAGUE_CHANCE = 0.01;
   private static final double PLAGUE_SPREAD = 0.06;
   private static final int PLAGUE_RANGE = 700;
   private static final int SMALLEST_FOR_PLAGUE = 12;
   private static final int SPARED_DAYS = 40;
   /** Days left that a settlement which has come through a plague is spared another; not saved. */
   private static final Map<Integer, Integer> SPARED = new HashMap<>();
   /** A garrison of one soldier to this many people keeps full order. */
   private static final int PEOPLE_PER_GUARD = 6;
   private static final double REVOLT_CHANCE = 0.35;
   private static final int REVOLT_LOYALTY = 25;
   private static final int REVOLT_ORDER = 35;
   private static final double RIOT_CHANCE = 0.25;
   private static final int RIOT_ORDER = 15;
   private static final int SETTLED_LOYALTY = 90;
   private static final double PLAYER_NEAR = 160.0;
   private static final double GUARD_RANGE = 48.0;
   private static final int UNEASY_LOYALTY = 40;
   /** A village of the player's rises only when this far gone, so its lord has days of warning. */
   private static final int VILLAGE_REVOLT_LOYALTY = 20;
   /** Emeralds a village pays at a low, usual and high tax, as a share of the usual tax. */
   private static final double[] TAX_SHARE = {0.5, 1.0, 1.75};
   private static final int[] TAX_ORDER = {10, 0, -20};
   private static final int[] TAX_LOYALTY = {3, 1, -3};
   private static final int GRANARY_ORDER = 8;
   private static final int FESTIVAL_ORDER = 5;
   private static final int LAW_ORDER = 6;
   /** Settlements that went hungry on the last day; not saved, so a restart may tell of it once more. */
   private static final Set<Integer> HUNGRY = new HashSet<>();
   /** Rations a load of grain from the faction's granary makes. */
   private static final int GRAIN_RATIONS = 2;
   /** In winter one bale of wool a day keeps this many people warm. */
   private static final int PEOPLE_PER_WOOL = 15;
   private static final int COLD_ORDER = 10;
   /** Settlements without wool this winter day, and factions told of it this winter; not saved. */
   private static final Set<Integer> COLD = new HashSet<>();
   private static final Set<String> COLD_FACTIONS = new HashSet<>();

   private FactionLife() {
   }

   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      RandomSource random = level.getRandom();
      List<FactionWorldData.Settlement> settlements = new ArrayList<>(data.settlements());
      if (random.nextDouble() < FAMINE_CHANCE * FactionSeasons.badHarvests(level) && !settlements.isEmpty()) {
         badHarvest(level, settlements, settlements.get(random.nextInt(settlements.size())), random);
      }

      Map<String, List<FactionWorldData.Settlement>> byFaction = new HashMap<>();
      for (FactionWorldData.Settlement settlement : settlements) {
         harvest(settlement, FactionSeasons.yield(level));
         byFaction.computeIfAbsent(settlement.faction(), faction -> new ArrayList<>()).add(settlement);
      }

      byFaction.forEach((faction, list) -> share(data, faction, list));
      boolean winter = FactionSeasons.season(level) == FactionSeasons.Season.WINTER;
      if (!winter) {
         COLD.clear();
         COLD_FACTIONS.clear();
      }

      for (FactionWorldData.Settlement settlement : settlements) {
         hunger(level, settlement);
         if (winter) {
            cold(level, data, settlement);
         }

         disease(level, settlements, settlement, random);
         order(settlement, FactionFaith.order(level, data, settlement));
         loyalty(settlement);
         if (settlement.famine > 0) {
            settlement.famine--;
         }

         unrest(level, data, settlement, random);
      }

      for (BlockPos hall : new ArrayList<>(data.villages().keySet())) {
         FactionWorldData.VillageState state = data.villageState(hall);
         if (state != null) {
            village(level, data, hall, state, random);
         }
      }

      data.setDirty();
   }

   /** Rations the settlement's own people bring in, less what everyone there eats. */
   private static void harvest(FactionWorldData.Settlement settlement, double season) {
      int mouths = settlement.population + settlement.garrison;
      if (settlement.food < 0) {
         settlement.food = mouths * STORE_DAYS / 2;
      }

      double yield = YIELD * kindYield(settlement) * season;
      if (settlement.famine > 0) {
         yield *= FAMINE_YIELD;
      }

      if (settlement.plague > 0) {
         yield *= PLAGUE_YIELD;
      }

      if (settlement.order < RIOT_ORDER * 2) {
         yield *= 0.8;
      }

      // Soldiers in garrison work the fields too in quiet times, if not as hard as the people.
      settlement.food += (int)Math.round((settlement.population + settlement.garrison * GARRISON_WORK) * yield) - mouths;
   }

   private static double kindYield(FactionWorldData.Settlement settlement) {
      if (FactionAI.isFort(settlement)) {
         return 0.3;
      }

      return switch (settlement.kind()) {
         case FactionTowns.VILLAGE -> 1.3;
         case FactionTowns.CAPITAL -> 0.9;
         default -> 1.0;
      };
   }

   /**
    * A faction carts food from settlements with plenty to those going short; where none has any to
    * spare, it opens its granary.
    */
   private static void share(FactionWorldData data, String faction, List<FactionWorldData.Settlement> settlements) {
      for (FactionWorldData.Settlement needy : settlements) {
         for (FactionWorldData.Settlement rich : settlements) {
            if (needy.food >= 0) {
               break;
            }

            int spare = rich.food - 2 * (rich.population + rich.garrison);
            if (rich != needy && spare > 0) {
               int moved = Math.min(spare, -needy.food);
               rich.food -= moved;
               needy.food += moved;
            }
         }
      }

      int[] stores = data.stores(faction);
      for (FactionWorldData.Settlement needy : settlements) {
         if (needy.food < 0 && stores[FactionResources.GRAIN] > 0) {
            int grain = Math.min(stores[FactionResources.GRAIN], Mth.positiveCeilDiv(-needy.food, GRAIN_RATIONS));
            stores[FactionResources.GRAIN] -= grain;
            needy.food = Math.min(0, needy.food + grain * GRAIN_RATIONS);
         }
      }

      for (FactionWorldData.Settlement settlement : settlements) {
         settlement.food = Math.min(settlement.food, (settlement.population + settlement.garrison) * STORE_DAYS);
      }
   }

   /** In winter the people burn through wool; without it they freeze, sicken and grumble. */
   private static void cold(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement) {
      // A fort's soldiers have their cloaks; a hamlet of a few souls gets by.
      int need = (settlement.population + PEOPLE_PER_WOOL / 2) / PEOPLE_PER_WOOL;
      if (FactionDiplomacy.isOutlaw(settlement.faction()) || FactionAI.isFort(settlement) || need == 0) {
         COLD.remove(settlement.id());
         return;
      }

      int[] stores = data.stores(settlement.faction());
      if (stores[FactionResources.WOOL] >= need) {
         stores[FactionResources.WOOL] -= need;
         COLD.remove(settlement.id());
         return;
      }

      stores[FactionResources.WOOL] = 0;
      COLD.add(settlement.id());
      settlement.health = Math.max(0, settlement.health - 2);
      if (COLD_FACTIONS.add(settlement.faction())) {
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.cold", FactionNews.factionName(settlement.faction())));
      }
   }

   /** Whether the faction's people are freezing for want of wool. */
   public static boolean cold(String faction) {
      return COLD_FACTIONS.contains(faction);
   }

   /** What could not be eaten is missed: people starve or leave, soldiers desert. */
   private static void hunger(ServerLevel level, FactionWorldData.Settlement settlement) {
      if (settlement.food >= 0) {
         HUNGRY.remove(settlement.id());
         settlement.health = Math.min(100, settlement.health + (settlement.plague > 0 ? 0 : 4));
         return;
      }

      int shortfall = -settlement.food;
      settlement.food = 0;
      // Where the soldiers outnumber the people, it is the soldiers who desert first.
      boolean overmanned = settlement.garrison > settlement.population;
      settlement.population = Math.max(1, settlement.population - Math.max(overmanned ? 0 : 1, shortfall / (overmanned ? 8 : 4)));
      settlement.garrison = Math.max(0, settlement.garrison - Math.max(overmanned ? 1 : 0, shortfall / (overmanned ? 3 : 8)));
      settlement.health = Math.max(0, settlement.health - 8);
      if (HUNGRY.add(settlement.id()) && !FactionDiplomacy.isOutlaw(settlement.faction())) {
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.hunger", FactionNews.factionName(settlement.faction()), FactionNews.named(settlement)));
      }
   }

   /** A drought or blight: the harvest fails in every settlement around a place for some days. */
   public static void badHarvest(ServerLevel level, List<FactionWorldData.Settlement> settlements, FactionWorldData.Settlement heart, RandomSource random) {
      BlockPos center = heart.center();
      int days = 6 + random.nextInt(7);
      int struck = 0;

      for (FactionWorldData.Settlement settlement : settlements) {
         if (settlement.horizontalDistanceSquared(center.getX(), center.getZ()) < (double)FAMINE_RANGE * FAMINE_RANGE) {
            settlement.famine = Math.max(settlement.famine, days);
            struck++;
         }
      }

      if (struck > 0) {
         FactionNews.tell(
            level, Component.translatable("news.ancientwarfare.famine", FactionNews.factionName(heart.faction()), FactionNews.named(heart), center.getX(), center.getZ(), struck)
         );
      }
   }

   /** Disease breaks out where people are many or weak, kills, and spreads to the neighbours. */
   private static void disease(ServerLevel level, List<FactionWorldData.Settlement> settlements, FactionWorldData.Settlement settlement, RandomSource random) {
      if (settlement.plague <= 0) {
         // Survivors of a plague are spared the next for a while; a hamlet of a few souls has none to catch.
         Integer spared = SPARED.get(settlement.id());
         if (spared != null && spared > 0) {
            SPARED.put(settlement.id(), spared - 1);
            return;
         }

         if (settlement.population < SMALLEST_FOR_PLAGUE) {
            return;
         }

         double chance = PLAGUE_CHANCE + (settlement.health < 50 ? SICKLY_PLAGUE_CHANCE : 0.0) + (settlement.population > 60 ? PLAGUE_CHANCE : 0.0);
         if (random.nextDouble() < chance) {
            startPlague(level, settlement, random, true);
         }

         return;
      }

      settlement.population = Math.max(1, settlement.population - Math.max(1, settlement.population * 6 / 100));
      settlement.garrison = Math.max(0, settlement.garrison - settlement.garrison * 5 / 100);
      settlement.health = Math.max(0, settlement.health - 10);
      BlockPos center = settlement.center();

      for (FactionWorldData.Settlement other : settlements) {
         if (other != settlement
            && other.plague <= 0
            && SPARED.getOrDefault(other.id(), 0) <= 0
            && other.population >= SMALLEST_FOR_PLAGUE
            && other.horizontalDistanceSquared(center.getX(), center.getZ()) < (double)PLAGUE_RANGE * PLAGUE_RANGE
            && random.nextDouble() < PLAGUE_SPREAD) {
            // The neighbours sicken quietly; the outbreak was news enough.
            startPlague(level, other, random, false);
         }
      }

      settlement.plague--;
      if (settlement.plague <= 0) {
         SPARED.put(settlement.id(), SPARED_DAYS);
      }
   }

   public static void startPlague(ServerLevel level, FactionWorldData.Settlement settlement, RandomSource random, boolean news) {
      settlement.plague = 4 + random.nextInt(5);
      if (!news) {
         return;
      }

      BlockPos center = settlement.center();
      FactionNews.tell(
         level,
         Component.translatable("news.ancientwarfare.plague", FactionNews.factionName(settlement.faction()), FactionNews.named(settlement), center.getX(), center.getZ())
      );
   }

   /** Order drifts toward what the garrison, the stores and the people's mood allow. */
   private static void order(FactionWorldData.Settlement settlement, int faith) {
      int target = 50 + guardOrder(settlement.garrison, settlement.population) + faith;
      if (HUNGRY.contains(settlement.id())) {
         target -= 30;
      }

      if (COLD.contains(settlement.id())) {
         target -= COLD_ORDER;
      }

      if (settlement.famine > 0) {
         target -= 5;
      }

      if (settlement.plague > 0) {
         target -= 15;
      }

      if (settlement.loyalty < 50) {
         target -= (50 - settlement.loyalty) / 2;
      }

      if (FactionTowns.CAPITAL.equals(settlement.kind())) {
         target += 10;
      }

      settlement.order = drift(settlement.order, Mth.clamp(target, 0, 100));
   }

   /** Up to 35 points of order from soldiers enough for the people. */
   private static int guardOrder(int guards, int people) {
      return (int)Math.round(35.0 * Math.min(1.0, guards * PEOPLE_PER_GUARD / (people + 1.0)));
   }

   private static int drift(int value, int target) {
      int step = (target - value) / 3;
      return value + (step != 0 ? step : Integer.signum(target - value));
   }

   /** People under a new master come round in good times and turn against him in bad. */
   private static void loyalty(FactionWorldData.Settlement settlement) {
      if (settlement.capturedFrom.isEmpty()) {
         settlement.loyalty = Math.min(100, settlement.loyalty + 2);
         return;
      }

      int change;
      if (settlement.order >= 50 && !HUNGRY.contains(settlement.id())) {
         change = 3;
      } else if (settlement.order < 30) {
         change = -3;
      } else {
         change = 1;
      }

      if (settlement.plague > 0 || HUNGRY.contains(settlement.id())) {
         change -= 2;
      }

      settlement.loyalty = Mth.clamp(settlement.loyalty + change, 0, 100);
      if (settlement.loyalty >= SETTLED_LOYALTY) {
         // They have come to think of themselves as the new master's people.
         settlement.capturedFrom = "";
      }
   }

   /** Disorder breeds riots; in a taken settlement, a revolt that hands it back to its old faction. */
   private static void unrest(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, RandomSource random) {
      if (playerNear(level, settlement.center())) {
         return;
      }

      String from = settlement.capturedFrom;
      if (!from.isEmpty()
         && settlement.loyalty < REVOLT_LOYALTY
         && settlement.order < REVOLT_ORDER
         && !FactionDiplomacy.isOutlaw(from)
         && random.nextDouble() < REVOLT_CHANCE) {
         String master = settlement.faction();
         int rebels = Math.max(2, settlement.population / 4);
         settlement.setFaction(from);
         settlement.capturedFrom = "";
         settlement.garrison = rebels;
         settlement.population = Math.max(1, settlement.population - rebels);
         settlement.loyalty = 80;
         settlement.order = 50;
         data.setProvinceOwner(settlement.province(), from);
         FactionNews.tell(
            level,
            Component.translatable("news.ancientwarfare.revolt", FactionNews.named(settlement), FactionNews.factionName(master), FactionNews.factionName(from))
               .withStyle(ChatFormatting.GOLD)
         );
      } else if (settlement.order < RIOT_ORDER && random.nextDouble() < RIOT_CHANCE) {
         settlement.garrison -= settlement.garrison / 4;
         settlement.population = Math.max(1, settlement.population - settlement.population / 10);
         data.setTreasury(settlement.faction(), Math.max(0, data.treasury(settlement.faction()) - 10));
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            FactionNews.tell(level, Component.translatable("news.ancientwarfare.riot", FactionNews.factionName(settlement.faction()), FactionNews.named(settlement)));
         }
      }
   }

   private static boolean playerNear(ServerLevel level, BlockPos pos) {
      return level.players().stream().anyMatch(player -> player.distanceToSqr(pos.getX(), player.getY(), pos.getZ()) < PLAYER_NEAR * PLAYER_NEAR);
   }

   /** The emeralds a taken village pays a day at its present tax. */
   public static int villageTax(FactionWorldData data, BlockPos hall) {
      int tax = data.tax(hall);
      FactionWorldData.VillageState state = data.villageState(hall);
      return state == null || tax <= 0 ? tax : Math.max(1, (int)Math.round(tax * TAX_SHARE[Mth.clamp(state.taxLevel, 0, 2)]));
   }

   /** A village the player took: his soldiers near the hall keep order, the tax decides the mood. */
   private static void village(ServerLevel level, FactionWorldData data, BlockPos hall, FactionWorldData.VillageState state, RandomSource random) {
      UUID owner = data.villages().get(hall);
      if (level.isLoaded(hall)) {
         state.guards = level.getEntitiesOfClass(
               CombatNpcEntity.class, new AABB(hall).inflate(GUARD_RANGE), npc -> npc.isAlive() && npc.getOwner().map(owner::equals).orElse(false)
            )
            .size();
      }

      int tax = Mth.clamp(state.taxLevel, 0, 2);
      // What the owner has learned: garrison drill makes every guard count twice, granaries and festivals keep the peace, temples win hearts.
      int guards = net.shadowmage.ancientwarfare.core.research.PlayerResearch.has(level, owner, net.shadowmage.ancientwarfare.core.research.PlayerResearch.GARRISONS) ? state.guards * 2 : state.guards;
      int calm = (net.shadowmage.ancientwarfare.core.research.PlayerResearch.has(level, owner, net.shadowmage.ancientwarfare.core.research.PlayerResearch.GRANARIES) ? GRANARY_ORDER : 0)
         + (net.shadowmage.ancientwarfare.core.research.PlayerResearch.has(level, owner, net.shadowmage.ancientwarfare.core.research.PlayerResearch.FESTIVALS) ? (FactionSeasons.season(level) == FactionSeasons.Season.AUTUMN ? FESTIVAL_ORDER * 2 : FESTIVAL_ORDER) : 0)
         + (net.shadowmage.ancientwarfare.core.research.PlayerResearch.has(level, owner, net.shadowmage.ancientwarfare.core.research.PlayerResearch.WRITTEN_LAW) ? LAW_ORDER : 0);
      state.order = drift(state.order, Mth.clamp(40 + guardOrder(guards, state.people) + TAX_ORDER[tax] + calm, 0, 100));
      int change = TAX_LOYALTY[tax] + (state.order >= 50 ? 1 : state.order < 30 ? -2 : 0) + (net.shadowmage.ancientwarfare.core.research.PlayerResearch.has(level, owner, net.shadowmage.ancientwarfare.core.research.PlayerResearch.TEMPLES) ? 1 : 0);
      state.loyalty = Mth.clamp(state.loyalty + change, 0, 100);

      if (state.loyalty < VILLAGE_REVOLT_LOYALTY && state.order < REVOLT_ORDER && !playerNear(level, hall) && random.nextDouble() < REVOLT_CHANCE) {
         revolt(level, data, hall, state, owner);
      } else if (state.loyalty < UNEASY_LOYALTY) {
         FactionNews.tellPlayer(
            level, owner, Component.translatable("village.ancientwarfare.uneasy", hall.getX(), hall.getZ(), state.loyalty, state.order).withStyle(ChatFormatting.YELLOW)
         );
      }
   }

   /** The village throws the player out and goes back to the faction it was taken from. */
   private static void revolt(ServerLevel level, FactionWorldData data, BlockPos hall, FactionWorldData.VillageState state, UUID owner) {
      String from = FactionRegions.OUTLAWS.contains(state.from) || state.from.isEmpty() ? "brigand" : state.from;
      FactionWorldData.Settlement settlement = handOver(level, data, hall, state, from);
      FactionNews.tellPlayer(
         level, owner, Component.translatable("village.ancientwarfare.revolt", hall.getX(), hall.getZ(), FactionNews.factionName(from)).withStyle(ChatFormatting.RED)
      );
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.village_revolt", FactionNews.factionName(from), FactionNews.named(settlement)));
   }

   /** The village leaves the player and becomes the faction's settlement again; returns the settlement. */
   static FactionWorldData.Settlement handOver(ServerLevel level, FactionWorldData data, BlockPos hall, FactionWorldData.VillageState state, String from) {
      data.setVillage(hall, null);
      if (level.getBlockEntity(hall) instanceof TownHallBlockEntity townHall) {
         // The new masters empty the treasury before anything else.
         townHall.clearContent();
      }

      level.setBlockAndUpdate(hall, Blocks.AIR.defaultBlockState());
      FactionRegions.Province province = FactionRegions.provinceAt(level, hall.getX(), hall.getZ());
      int rebels = Math.max(2, state.people / 4);
      FactionWorldData.Settlement settlement = data.addSettlement(
         from, "captured", "", hall.offset(-16, -4, -16), hall.offset(16, 8, 16), province.key(), level.getGameTime(), rebels, Math.max(1, state.people - rebels)
      );
      settlement.manned = true;
      settlement.loyalty = 80;
      return settlement;
   }

   /** A player right-clicks a faction's person with food: if their settlement is going short, it is aid. */
   public static boolean onFeed(net.minecraft.server.level.ServerPlayer player, net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity npc, net.minecraft.world.item.ItemStack stack) {
      net.minecraft.world.food.FoodProperties food = stack.getItem().getFoodProperties(stack, player);
      String faction = npc.getFactionName();
      ServerLevel level = player.serverLevel();
      if (food == null || FactionDiplomacy.isOutlaw(faction)) {
         return false;
      }

      // Even an enemy's hungry people take bread, and it softens the war.
      net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData standings = net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData.get(level);

      FactionWorldData.Settlement settlement = FactionSimulation.settlementAt(FactionWorldData.get(level), faction, npc.getX(), npc.getZ(), 16);
      if (settlement == null) {
         return false;
      }

      int rations = stack.getCount() * Math.max(1, food.getNutrition() / 2);
      int standing = aid(level, player, settlement, rations);
      if (standing < 0) {
         player.sendSystemMessage(Component.translatable("aid.ancientwarfare.not_needed", FactionNews.named(settlement)));
         return true;
      }

      stack.setCount(0);
      standings.adjustStanding(player.getUUID(), faction, standing);
      player.sendSystemMessage(
         Component.translatable("aid.ancientwarfare.given", rations, FactionNews.factionName(faction), FactionNews.named(settlement), standing).withStyle(ChatFormatting.GREEN)
      );
      return true;
   }

   /**
    * The player hands food to a faction's people in a settlement going short: it goes into their
    * stores, and they remember it. Returns the standing gained, or -1 when they need nothing.
    */
   public static int aid(ServerLevel level, Player player, FactionWorldData.Settlement settlement, int rations) {
      int mouths = settlement.population + settlement.garrison;
      if (settlement.famine <= 0 && settlement.plague <= 0 && settlement.food >= mouths * 2) {
         return -1;
      }

      settlement.food = Math.min(Math.max(0, settlement.food) + rations, mouths * STORE_DAYS);
      int standing = Mth.clamp(rations / 6, 1, 15);
      FactionWorldData.get(level).setDirty();
      return standing;
   }
}
