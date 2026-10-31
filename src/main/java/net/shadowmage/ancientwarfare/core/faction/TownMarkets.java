package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.common.npc.FactionTradeListRegistry;

/**
 * Every trading town keeps its own stocks of the eight goods (see FactionResources). Each day it
 * makes what its land and people make and uses its share of what the whole world makes, so a town
 * rich in what it is known for has plenty of that and too little of the rest. Along the roads
 * between towns at peace goods flow from plenty to want, the more between bigger towns; a road
 * that is blocked by war or robbed that day carries nothing.
 *
 * What a good costs in a town follows how many days of it the town has in store: scarce goods are
 * dear, plentiful ones cheap. Traders there price their wares by it, and what the player buys or
 * sells there, or brings to its board, comes out of or goes into its stocks.
 */
public final class TownMarkets extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_town_markets";
   /** Days of a good a town likes to keep; at this a good sells at its usual price. */
   private static final double TARGET_DAYS = 10.0;
   /** No town stores more than this many days of a good. */
   private static final double MAX_DAYS = 40.0;
   private static final double MIN_PRICE = 0.5;
   private static final double MAX_PRICE = 2.5;
   /** A price this far off the usual is worth talking about. */
   public static final double DEAR = 1.25;
   public static final double CHEAP = 0.8;
   /** The share of the difference in stores between two towns a day's caravans even out. */
   private static final double CARRIED = 0.15;
   /** How far a trader or board looks for the town whose market it is part of. */
   private static final double MARKET_RANGE = 160.0;
   private final Map<Integer, double[]> stocks = new HashMap<>();
   /** Each town's daily use of each good, from the last day's reckoning. */
   private final Map<Integer, double[]> uses = new HashMap<>();

   public static TownMarkets get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(TownMarkets::load, TownMarkets::new, DATA_NAME);
   }

   /** One day of making, using and carrying goods between towns. Runs after the day's trade routes are reckoned. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      TownMarkets markets = get(level);
      List<FactionWorldData.Settlement> towns = new ArrayList<>();
      double[] made = new double[FactionResources.KINDS];
      double people = 0.0;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!settlement.built() || !FactionTrade.trades(settlement) || FactionTrade.partOfOlder(data, settlement)) {
            continue;
         }

         towns.add(settlement);
         int[] output = FactionResources.output(settlement);
         for (int kind = 0; kind < FactionResources.KINDS; kind++) {
            made[kind] += output[kind];
         }

         people += wants(settlement);
      }

      markets.stocks.keySet().removeIf(id -> data.settlement(id) == null);
      markets.uses.keySet().removeIf(id -> data.settlement(id) == null);
      if (towns.isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement town : towns) {
         // Each town uses its share of all the world makes: what one has over, another lacks.
         double share = wants(town) / people;
         double[] use = new double[FactionResources.KINDS];
         for (int kind = 0; kind < FactionResources.KINDS; kind++) {
            use[kind] = Math.max(0.2, made[kind] * share);
         }

         markets.uses.put(town.id(), use);
         double[] stock = markets.stocks.get(town.id());
         if (stock == null) {
            stock = new double[FactionResources.KINDS];
            for (int kind = 0; kind < FactionResources.KINDS; kind++) {
               stock[kind] = use[kind] * TARGET_DAYS;
            }

            markets.stocks.put(town.id(), stock);
         }

         int[] output = FactionResources.output(town);
         for (int kind = 0; kind < FactionResources.KINDS; kind++) {
            // A town with plenty of a good uses it freely and one short of it sparingly, so stocks
            // settle where a town's own work meets its wants instead of piling up for ever.
            double used = use[kind] * Mth.clamp(stock[kind] / use[kind] / TARGET_DAYS, 0.3, 3.0);
            stock[kind] = Mth.clamp(stock[kind] + output[kind] - used, 0.0, use[kind] * MAX_DAYS);
         }
      }

      for (FactionWorldData.Route route : data.routes()) {
         if (route.village() != null || route.blocked || route.income <= 0) {
            continue;
         }

         FactionWorldData.Settlement a = data.endpoint(route, true);
         FactionWorldData.Settlement b = data.endpoint(route, false);
         if (a == null || b == null) {
            continue;
         }

         markets.carry(a, b);
      }

      markets.setDirty();
   }

   /** How much a town wants against others: every household some, and more for more people, as a town's work grows. */
   private static double wants(FactionWorldData.Settlement town) {
      return 1.0 + town.population() / 15.0;
   }

   /** A day's caravans between two towns: each good moves toward the town with fewer days of it. */
   private void carry(FactionWorldData.Settlement a, FactionWorldData.Settlement b) {
      double[] stockA = this.stocks.get(a.id());
      double[] stockB = this.stocks.get(b.id());
      double[] useA = this.uses.get(a.id());
      double[] useB = this.uses.get(b.id());
      if (stockA == null || stockB == null || useA == null || useB == null) {
         return;
      }

      double load = 1.0 + (a.population() + b.population()) / 40.0;
      for (int kind = 0; kind < FactionResources.KINDS; kind++) {
         // The amount that would leave both with the same days of it in store; a day's caravans
         // carry only part of it, so a town far from plenty stays dearer than its neighbours.
         double even = (stockA[kind] * useB[kind] - stockB[kind] * useA[kind]) / (useA[kind] + useB[kind]);
         double moved = Mth.clamp(even * CARRIED, -load, load);
         stockA[kind] -= moved;
         stockB[kind] += moved;
      }
   }

   /** What a good costs in a town, against its usual price: above 1 dear, below cheap. */
   public double price(int town, int kind) {
      double[] stock = this.stocks.get(town);
      double[] use = this.uses.get(town);
      if (stock == null || use == null) {
         return 1.0;
      }

      double days = Math.max(0.4, stock[kind] / use[kind]);
      return Mth.clamp(Math.sqrt(TARGET_DAYS / days), MIN_PRICE, MAX_PRICE);
   }

   /** Goods come into a town's stocks (the player sold or brought them) or, taken away, go out of them. */
   public void change(int town, int kind, double amount) {
      double[] stock = this.stocks.get(town);
      double[] use = this.uses.get(town);
      if (stock == null || use == null) {
         return;
      }

      stock[kind] = Mth.clamp(stock[kind] + amount, 0.0, use[kind] * MAX_DAYS);
      this.setDirty();
   }

   /** The trading town whose market a spot is part of, if any. */
   public static FactionWorldData.Settlement townAt(ServerLevel level, double x, double z) {
      FactionWorldData data = FactionWorldData.get(level);
      TownMarkets markets = get(level);
      FactionWorldData.Settlement best = null;
      double bestDistance = MARKET_RANGE * MARKET_RANGE;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!markets.stocks.containsKey(settlement.id())) {
            continue;
         }

         double distance = settlement.horizontalDistanceSquared((int)x, (int)z);
         if (distance < bestDistance) {
            best = settlement;
            bestDistance = distance;
         }
      }

      return best;
   }

   /** The good an item counts as, or -1. */
   public static int kindOf(ItemStack stack) {
      if (stack.isEmpty()) {
         return -1;
      } else if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS) || stack.is(net.minecraft.world.item.Items.STICK)) {
         return FactionResources.WOOD;
      } else if (stack.is(ItemTags.WOOL) || stack.is(ItemTags.WOOL_CARPETS) || stack.is(net.minecraft.world.item.Items.STRING)) {
         return FactionResources.WOOL;
      } else if (stack.is(ItemTags.COALS) || stack.is(net.minecraft.world.item.Items.COAL_BLOCK)) {
         return FactionResources.COAL;
      }

      String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
      if (id.contains("copper")) {
         return FactionResources.COPPER;
      } else if (id.contains("iron")) {
         return FactionResources.IRON;
      } else if (id.contains("leather") || id.equals("rabbit_hide") || id.equals("saddle")) {
         return FactionResources.LEATHER;
      } else if (id.equals("wheat") || id.equals("bread") || id.equals("wheat_seeds") || id.equals("hay_block") || id.equals("cake") || id.equals("cookie")) {
         return FactionResources.GRAIN;
      } else if (id.equals("cobblestone") || id.equals("stone") || id.contains("stone_brick") || id.equals("smooth_stone") || id.startsWith("stone_")) {
         return FactionResources.STONE;
      }

      return -1;
   }

   /** The first good among some stacks, or -1. */
   private static int kindOf(List<ItemStack> stacks) {
      for (ItemStack stack : stacks) {
         int kind = kindOf(stack);
         if (kind >= 0) {
            return kind;
         }
      }

      return -1;
   }

   /**
    * A trader's wares at this town's prices: what he hands over costs more where it is scarce, and
    * what he takes in is wanted in fewer pieces where it is dear.
    */
   public static List<FactionTradeListRegistry.FactionTradeEntry> price(ServerLevel level, double x, double z, List<FactionTradeListRegistry.FactionTradeEntry> trades) {
      FactionWorldData.Settlement town = townAt(level, x, z);
      if (town == null) {
         return trades;
      }

      TownMarkets markets = get(level);
      List<FactionTradeListRegistry.FactionTradeEntry> priced = new ArrayList<>();
      for (FactionTradeListRegistry.FactionTradeEntry trade : trades) {
         int bought = kindOf(trade.outputs());
         int sold = kindOf(trade.inputs());
         double factor = (bought < 0 ? 1.0 : markets.price(town.id(), bought)) / (sold < 0 ? 1.0 : markets.price(town.id(), sold));
         if (Math.abs(factor - 1.0) < 0.05) {
            priced.add(trade);
            continue;
         }

         priced.add(new FactionTradeListRegistry.FactionTradeEntry(trade.inputs().stream().map(stack -> {
            ItemStack copy = stack.copy();
            if (copy.getMaxStackSize() > 1) {
               copy.setCount(Mth.clamp((int)Math.round(copy.getCount() * factor), 1, copy.getMaxStackSize()));
            }

            return copy;
         }).toList(), trade.outputs()));
      }

      return priced;
   }

   /** A trade done at a town's market: its goods leave or join the town's stocks. */
   public static void traded(ServerLevel level, double x, double z, FactionTradeListRegistry.FactionTradeEntry trade) {
      FactionWorldData.Settlement town = townAt(level, x, z);
      if (town == null) {
         return;
      }

      TownMarkets markets = get(level);
      for (ItemStack stack : trade.outputs()) {
         int kind = kindOf(stack);
         if (kind >= 0) {
            markets.change(town.id(), kind, -stack.getCount());
         }
      }

      for (ItemStack stack : trade.inputs()) {
         int kind = kindOf(stack);
         if (kind >= 0) {
            markets.change(town.id(), kind, stack.getCount());
         }
      }
   }

   /** The goods dear and cheap in a town just now, as a line for the player, or null when all sell at their usual price. */
   public static Component prices(ServerLevel level, FactionWorldData.Settlement town) {
      TownMarkets markets = get(level);
      if (!markets.stocks.containsKey(town.id())) {
         return null;
      }

      List<Integer> dear = new ArrayList<>();
      List<Integer> cheap = new ArrayList<>();
      for (int kind = 0; kind < FactionResources.KINDS; kind++) {
         double price = markets.price(town.id(), kind);
         if (price >= DEAR) {
            dear.add(kind);
         } else if (price <= CHEAP) {
            cheap.add(kind);
         }
      }

      if (dear.isEmpty() && cheap.isEmpty()) {
         return null;
      }

      MutableComponent line = Component.empty();
      if (!dear.isEmpty()) {
         line.append(Component.translatable("market.ancientwarfare.dear", goods(dear)));
      }

      if (!cheap.isEmpty()) {
         if (!dear.isEmpty()) {
            line.append(" ");
         }

         line.append(Component.translatable("market.ancientwarfare.cheap", goods(cheap)));
      }

      return line;
   }

   /** Each good's price in a town, in hundredths of its usual price, for the map; empty for a town with no market. */
   public static int[] priceList(ServerLevel level, int town) {
      TownMarkets markets = get(level);
      if (!markets.stocks.containsKey(town)) {
         return new int[0];
      }

      int[] list = new int[FactionResources.KINDS];
      for (int kind = 0; kind < FactionResources.KINDS; kind++) {
         list[kind] = (int)Math.round(markets.price(town, kind) * 100.0);
      }

      return list;
   }

   public static Component goodName(int kind) {
      return Component.translatable("trade.ancientwarfare.good." + FactionResources.NAMES[kind]);
   }

   private static Component goods(List<Integer> kinds) {
      MutableComponent list = Component.empty();
      for (int i = 0; i < kinds.size(); i++) {
         if (i > 0) {
            list.append(", ");
         }

         list.append(goodName(kinds.get(i)));
      }

      return list;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      for (Map.Entry<Integer, double[]> entry : this.stocks.entrySet()) {
         double[] use = this.uses.get(entry.getKey());
         if (use == null) {
            continue;
         }

         CompoundTag town = new CompoundTag();
         town.putInt("Town", entry.getKey());
         ListTag stock = new ListTag();
         ListTag uses = new ListTag();
         for (int kind = 0; kind < FactionResources.KINDS; kind++) {
            stock.add(net.minecraft.nbt.DoubleTag.valueOf(entry.getValue()[kind]));
            uses.add(net.minecraft.nbt.DoubleTag.valueOf(use[kind]));
         }

         town.put("Stock", stock);
         town.put("Use", uses);
         list.add(town);
      }

      tag.put("Towns", list);
      return tag;
   }

   private static TownMarkets load(CompoundTag tag) {
      TownMarkets markets = new TownMarkets();
      for (Tag raw : tag.getList("Towns", Tag.TAG_COMPOUND)) {
         CompoundTag town = (CompoundTag)raw;
         ListTag stock = town.getList("Stock", Tag.TAG_DOUBLE);
         ListTag use = town.getList("Use", Tag.TAG_DOUBLE);
         if (stock.size() != FactionResources.KINDS || use.size() != FactionResources.KINDS) {
            continue;
         }

         double[] stocks = new double[FactionResources.KINDS];
         double[] uses = new double[FactionResources.KINDS];
         for (int kind = 0; kind < FactionResources.KINDS; kind++) {
            stocks[kind] = stock.getDouble(kind);
            uses[kind] = use.getDouble(kind);
         }

         markets.stocks.put(town.getInt("Town"), stocks);
         markets.uses.put(town.getInt("Town"), uses);
      }

      return markets;
   }
}
