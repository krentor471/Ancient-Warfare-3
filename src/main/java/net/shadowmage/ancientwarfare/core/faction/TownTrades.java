package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * The trades of townsfolk, and the village head. Each grown townsperson follows one of forty
 * medieval trades, picked by what the town has built: the smith sells tools and buys iron, the
 * alchemist sells potions' makings, the cartographer maps. Clicked, they offer a small deal each way.
 *
 * One townsperson in each town is its head. The head thinks well or ill of the player: a gift
 * warms him, persuasion may win him (and the town) over, his purse can be lifted at a risk, and he
 * hands out errands of his own, paid better than the townsfolk's.
 */
public final class TownTrades {
   private static final String TRADE = "AWTrade";
   private static final String HEAD = "AWHead";
   private static final String OPINION = "AWHeadOpinion";
   private static final String ERRAND = "AWHeadErrand";
   private static final String STOLEN = "AWHeadStolen";
   private static final int GIFT_PRICE = 10;
   private static final int TOWN_RANGE = 64;

   /** A trade: the building it goes with (a word of the building's key, or none), and what is sold and bought. */
   private record Trade(String key, String building, String sells, int sellCount, int sellPrice, String buys, int buyCount, int buyPrice) {
   }

   private static final Trade[] TRADES = {
      new Trade("smith", "smith", "minecraft:iron_pickaxe", 1, 18, "minecraft:iron_ingot", 4, 6),
      new Trade("armourer", "smith", "minecraft:iron_chestplate", 1, 40, "minecraft:iron_ingot", 6, 9),
      new Trade("weaponsmith", "smith", "minecraft:iron_sword", 1, 14, "minecraft:coal", 16, 5),
      new Trade("fletcher", "archery", "minecraft:arrow", 24, 6, "minecraft:feather", 16, 4),
      new Trade("bowyer", "archery", "minecraft:bow", 1, 8, "minecraft:string", 8, 4),
      new Trade("baker", "bak", "minecraft:bread", 8, 5, "minecraft:wheat", 24, 5),
      new Trade("butcher", "butcher", "minecraft:cooked_beef", 6, 6, "minecraft:beef", 8, 4),
      new Trade("brewer", "brew", "minecraft:honey_bottle", 3, 6, "minecraft:sugar", 16, 4),
      new Trade("miller", "mill", "minecraft:bread", 6, 4, "minecraft:wheat", 32, 6),
      new Trade("farmer", "farm", "minecraft:carrot", 16, 3, "minecraft:bone_meal", 16, 3),
      new Trade("shepherd", "pen", "minecraft:white_wool", 8, 5, "minecraft:shears", 1, 4),
      new Trade("tanner", "tann", "minecraft:leather", 6, 6, "minecraft:rabbit_hide", 8, 4),
      new Trade("weaver", "weav", "minecraft:white_carpet", 8, 5, "minecraft:string", 16, 5),
      new Trade("tailor", "weav", "minecraft:leather_chestplate", 1, 8, "minecraft:white_wool", 8, 4),
      new Trade("cobbler", "tann", "minecraft:leather_boots", 1, 6, "minecraft:leather", 4, 4),
      new Trade("carpenter", "carpent", "minecraft:chest", 2, 4, "minecraft:oak_log", 16, 4),
      new Trade("mason", "mason", "minecraft:stone_bricks", 32, 6, "minecraft:cobblestone", 48, 4),
      new Trade("potter", "", "minecraft:flower_pot", 6, 4, "minecraft:clay_ball", 16, 4),
      new Trade("glassblower", "", "minecraft:glass_bottle", 8, 4, "minecraft:sand", 32, 4),
      new Trade("alchemist", "alchem", "minecraft:glowstone_dust", 8, 10, "minecraft:spider_eye", 4, 6),
      new Trade("potion_maker", "alchem", "minecraft:fermented_spider_eye", 3, 10, "minecraft:nether_wart", 4, 6),
      new Trade("herbalist", "herb", "minecraft:sweet_berries", 16, 4, "minecraft:dandelion", 8, 2),
      new Trade("physician", "", "minecraft:golden_apple", 1, 30, "minecraft:honey_bottle", 3, 5),
      new Trade("cartographer", "library", "minecraft:map", 1, 8, "minecraft:paper", 16, 5),
      new Trade("scribe", "library", "minecraft:writable_book", 1, 8, "minecraft:ink_sac", 6, 4),
      new Trade("bookbinder", "library", "minecraft:book", 2, 8, "minecraft:leather", 4, 4),
      new Trade("candlemaker", "chapel", "minecraft:candle", 6, 5, "minecraft:honeycomb", 4, 5),
      new Trade("fisherman", "dock", "minecraft:cooked_cod", 8, 5, "minecraft:string", 8, 3),
      new Trade("hunter", "", "minecraft:cooked_rabbit", 6, 5, "minecraft:arrow", 16, 3),
      new Trade("woodcutter", "", "minecraft:oak_log", 16, 4, "minecraft:iron_axe", 1, 6),
      new Trade("charcoal_burner", "", "minecraft:charcoal", 16, 5, "minecraft:oak_log", 16, 3),
      new Trade("miner", "mine", "minecraft:raw_iron", 6, 6, "minecraft:torch", 32, 3),
      new Trade("jeweller", "", "minecraft:emerald", 1, 12, "minecraft:gold_ingot", 2, 8),
      new Trade("goldsmith", "", "minecraft:golden_carrot", 4, 8, "minecraft:gold_nugget", 18, 4),
      new Trade("stablemaster", "stable", "minecraft:saddle", 1, 20, "minecraft:hay_block", 6, 5),
      new Trade("beekeeper", "", "minecraft:honeycomb", 4, 5, "minecraft:glass_bottle", 6, 2),
      new Trade("innkeeper", "tavern", "minecraft:mushroom_stew", 3, 5, "minecraft:brown_mushroom", 8, 3),
      new Trade("dyer", "", "minecraft:blue_dye", 8, 5, "minecraft:lapis_lazuli", 8, 5),
      new Trade("ropemaker", "dock", "minecraft:lead", 2, 6, "minecraft:string", 12, 4),
      new Trade("bellfounder", "chapel", "minecraft:bell", 1, 30, "minecraft:copper_ingot", 12, 6)
   };

   /** The errands a head hands out: so many of an item, for so much silver. */
   private record Errand(String key, String item, int count, int reward) {
   }

   private static final Errand[] ERRANDS = {
      new Errand("walls", "minecraft:stone_bricks", 48, 30),
      new Errand("granary", "minecraft:wheat", 48, 24),
      new Errand("watch", "minecraft:iron_sword", 2, 26),
      new Errand("feast", "minecraft:cooked_beef", 16, 24),
      new Errand("records", "minecraft:book", 4, 22),
      new Errand("lamps", "minecraft:lantern", 8, 22)
   };

   private TownTrades() {
   }

   private static boolean grown(FactionNpcEntity npc) {
      return !npc.isBaby() && !TownsfolkFavours.isBeggar(npc);
   }

   private static FactionWorldData.Settlement town(ServerLevel level, FactionNpcEntity npc) {
      return FactionSimulation.settlementAt(FactionWorldData.get(level), npc.getFactionName(), npc.getX(), npc.getZ(), TOWN_RANGE);
   }

   /** The townsperson's trade, picked the first time anyone asks; empty for children and beggars. */
   public static String tradeOf(ServerLevel level, FactionNpcEntity npc) {
      return grown(npc) && !npc.getPersistentData().getBoolean(HEAD) ? trade(level, npc).key : "";
   }

   private static Trade trade(ServerLevel level, FactionNpcEntity npc) {
      CompoundTag data = npc.getPersistentData();
      String key = data.getString(TRADE);
      for (Trade trade : TRADES) {
         if (trade.key.equals(key)) {
            return trade;
         }
      }

      // Picked once: trades whose building the town has are three times as likely.
      FactionWorldData.Settlement town = town(level, npc);
      List<Trade> pool = new ArrayList<>();
      for (Trade trade : TRADES) {
         int weight = 1;
         if (town != null && !trade.building.isEmpty() && town.buildings().stream().anyMatch(b -> b.contains(trade.building))) {
            weight = 4;
         }

         for (int i = 0; i < weight; i++) {
            pool.add(trade);
         }
      }

      Trade picked = pool.get(npc.getRandom().nextInt(pool.size()));
      data.putString(TRADE, picked.key);
      return picked;
   }

   /** The head of the town this townsperson lives in is chosen the first time anyone there is spoken to. */
   private static boolean isHead(ServerLevel level, FactionNpcEntity npc) {
      if (npc.getPersistentData().getBoolean(HEAD)) {
         return true;
      }

      FactionWorldData.Settlement town = town(level, npc);
      if (town == null || !grown(npc)) {
         return false;
      }

      List<FactionNpcEntity> heads = level.getEntitiesOfClass(
         FactionNpcEntity.class, npc.getBoundingBox().inflate(TOWN_RANGE), other -> other.getPersistentData().getBoolean(HEAD) && other.getFactionName().equals(npc.getFactionName())
      );
      if (!heads.isEmpty()) {
         return false;
      }

      npc.getPersistentData().putBoolean(HEAD, true);
      npc.setPersistenceRequired();
      npc.setCustomName(Component.translatable("head.ancientwarfare.title"));
      npc.setCustomNameVisible(true);
      return true;
   }

   private static int opinion(FactionNpcEntity head, ServerPlayer player) {
      return head.getPersistentData().getInt(OPINION + player.getStringUUID());
   }

   private static void changeOpinion(FactionNpcEntity head, ServerPlayer player, int delta) {
      int opinion = Math.max(-100, Math.min(100, opinion(head, player) + delta));
      head.getPersistentData().putInt(OPINION + player.getStringUUID(), opinion);
   }

   private static MutableComponent button(Component label, String command, ChatFormatting color, Component hint) {
      return Component.literal("[")
         .append(label)
         .append("]")
         .withStyle(style -> style.withColor(color)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hint)));
   }

   private static Component item(String id) {
      return BuiltInRegistries.ITEM.get(new ResourceLocation(id)).getDescription();
   }

   /** After the usual talk: the townsperson's trade and deals, or the head's business. */
   /** The village head: everything the town offers, on a page of its own. False for anyone else. */
   public static boolean headPanel(ServerPlayer player, FactionNpcEntity npc) {
      ServerLevel level = player.serverLevel();
      if (!grown(npc) || !isHead(level, npc)) {
         return false;
      }

      // The first line tells the client to open the head's page with these lines instead of the chat.
      player.sendSystemMessage(Component.translatable("head.ancientwarfare.panel", npc.getDisplayName()));
      offerHead(player, npc);
      offerTrade(player, npc);
      return true;
   }

   private static void offerTrade(ServerPlayer player, FactionNpcEntity npc) {
      Trade trade = trade(player.serverLevel(), npc);
      String id = "/awtownsfolk " + npc.getId() + " ";
      player.sendSystemMessage(
         Component.literal("  ")
            .append(Component.translatable("trade.ancientwarfare.who", Component.translatable("trade.ancientwarfare." + trade.key)).withStyle(ChatFormatting.GRAY))
            .append("  ")
            .append(button(Component.translatable("trade.ancientwarfare.buy", trade.sellCount, item(trade.sells), trade.sellPrice), id + "buy", ChatFormatting.GREEN, Component.translatable("trade.ancientwarfare.buy.hint")))
            .append("  ")
            .append(button(Component.translatable("trade.ancientwarfare.sell", trade.buyCount, item(trade.buys), trade.buyPrice), id + "sell", ChatFormatting.YELLOW, Component.translatable("trade.ancientwarfare.sell.hint")))
      );
   }

   public static void offer(ServerPlayer player, FactionNpcEntity npc) {
      ServerLevel level = player.serverLevel();
      if (!grown(npc)) {
         return;
      }

      if (isHead(level, npc)) {
         offerHead(player, npc);
         return;
      }

      Trade trade = trade(level, npc);
      String id = "/awtownsfolk " + npc.getId() + " ";
      player.sendSystemMessage(
         Component.literal("  ")
            .append(Component.translatable("trade.ancientwarfare.who", Component.translatable("trade.ancientwarfare." + trade.key)).withStyle(ChatFormatting.GRAY))
            .append("  ")
            .append(button(Component.translatable("trade.ancientwarfare.buy", trade.sellCount, item(trade.sells), trade.sellPrice), id + "buy", ChatFormatting.GREEN, Component.translatable("trade.ancientwarfare.buy.hint")))
            .append("  ")
            .append(button(Component.translatable("trade.ancientwarfare.sell", trade.buyCount, item(trade.buys), trade.buyPrice), id + "sell", ChatFormatting.YELLOW, Component.translatable("trade.ancientwarfare.sell.hint")))
      );
   }

   private static void offerHead(ServerPlayer player, FactionNpcEntity head) {
      int opinion = opinion(head, player);
      String id = "/awtownsfolk " + head.getId() + " ";
      ChatFormatting mood = opinion >= 30 ? ChatFormatting.GREEN : opinion <= -30 ? ChatFormatting.RED : ChatFormatting.GRAY;
      player.sendSystemMessage(Component.literal("  ").append(Component.translatable("head.ancientwarfare.who", opinion).withStyle(mood)));
      FactionWorldData.Settlement home = town(player.serverLevel(), head);
      if (home != null) {
         String homeId = "/awtownsfolk " + head.getId() + " ";
         int[] prices = {TownHomes.HOUSE_PRICE, TownHomes.FURNISH_PRICE, TownHomes.SERVANT_PRICE};
         TownHomes.offer(player, head, home, (act, key) -> button(
            Component.translatable(key, act.equals("home_buy") ? prices[0] : act.equals("home_furnish") ? prices[1] : prices[2]), homeId + act, ChatFormatting.GREEN, Component.translatable(key + ".hint")
         ));
         player.sendSystemMessage(Component.literal("  ").append(Component.translatable("head.ancientwarfare.faith", FactionFaith.name(FaithSpread.faith(player.serverLevel(), home))).withStyle(ChatFormatting.LIGHT_PURPLE)));
      }
      Errand errand = errand(head);
      if (errand != null) {
         player.sendSystemMessage(Component.literal("  ").append(Component.translatable("head.ancientwarfare.errand." + errand.key, errand.count, item(errand.item), errand.reward).withStyle(ChatFormatting.GOLD)));
      }

      player.sendSystemMessage(
         Component.literal("  ")
            .append(button(Component.translatable("head.ancientwarfare.gift", GIFT_PRICE), id + "gift", ChatFormatting.GREEN, Component.translatable("head.ancientwarfare.gift.hint")))
            .append("  ")
            .append(button(Component.translatable("head.ancientwarfare.persuade"), id + "persuade", ChatFormatting.AQUA, Component.translatable("head.ancientwarfare.persuade.hint")))
            .append("  ")
            .append(button(Component.translatable("head.ancientwarfare.errand"), id + (errand == null ? "errand" : "deliver"), ChatFormatting.GOLD, Component.translatable("head.ancientwarfare.errand.hint")))
            .append("  ")
            .append(button(Component.translatable("head.ancientwarfare.preach", FaithSpread.PREACHER_PRICE), id + "preach", ChatFormatting.LIGHT_PURPLE, Component.translatable("head.ancientwarfare.preach.hint")))
            .append("  ")
            .append(button(Component.translatable("head.ancientwarfare.steal"), id + "steal", ChatFormatting.RED, Component.translatable("head.ancientwarfare.steal.hint")))
      );
   }

   private static Errand errand(FactionNpcEntity head) {
      String key = head.getPersistentData().getString(ERRAND);
      for (Errand errand : ERRANDS) {
         if (errand.key.equals(key)) {
            return errand;
         }
      }

      return null;
   }

   private static int count(ServerPlayer player, String item) {
      int found = 0;
      for (ItemStack stack : player.getInventory().items) {
         if (TownsfolkFavours.matchesItem(stack, item)) {
            found += stack.getCount();
         }
      }

      return found;
   }

   private static void take(ServerPlayer player, String item, int wanted) {
      for (ItemStack stack : player.getInventory().items) {
         if (wanted > 0 && TownsfolkFavours.matchesItem(stack, item)) {
            int used = Math.min(wanted, stack.getCount());
            stack.shrink(used);
            wanted -= used;
         }
      }
   }

   /** A click on one of the deal buttons: "buy", "sell", or the head's "gift", "persuade", "errand", "deliver", "steal". */
   public static void act(ServerPlayer player, int npcId, String action) {
      ServerLevel level = player.serverLevel();
      if (action.startsWith("court_")) {
         TownWatch.act(player, action);
         return;
      }

      if (!(level.getEntity(npcId) instanceof FactionNpcEntity npc) || !npc.isAlive() || npc.distanceToSqr(player) > 64.0 || !grown(npc)) {
         return;
      }

      FactionStandingData standings = FactionStandingData.get(level);
      if (standings.isHostile(player.getUUID(), npc.getFactionName())) {
         return;
      }

      if (action.equals("chronicle")) {
         FactionWorldData.Settlement home = town(level, npc);
         if (home != null) {
            TownChronicle.read(player, home);
         }

         return;
      }

      if (action.startsWith("home_")) {
         FactionWorldData.Settlement home = town(level, npc);
         if (home != null && npc.getPersistentData().getBoolean(HEAD)) {
            TownHomes.act(player, npc, home, action);
         }

         return;
      }

      if (CouncilHalls.act(player, npc, action) || TownCalendar.act(player, npc, action)) {
         return;
      }

      Component speaker = npc.getDisplayName();
      RandomSource random = npc.getRandom();
      boolean giver = npc.getPersistentData().getBoolean(HEAD) || !npc.getPersistentData().getString(CouncilHalls.COUNCIL_TAG).isEmpty();
      switch (action) {
         case "buy" -> {
            Trade trade = trade(level, npc);
            FactionWorldData.Settlement home = town(level, npc);
            // Guild members buy at the brothers' price.
            int price = home != null && CouncilHalls.member(player, home) ? Math.max(1, trade.sellPrice * 85 / 100) : trade.sellPrice;
            if (!Coins.take(player, price)) {
               player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
               return;
            }

            player.getInventory().placeItemBackInInventory(new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(trade.sells)), trade.sellCount));
            FactionRumours.say(player, speaker, Component.translatable("trade.ancientwarfare.bought"));
         }
         case "sell" -> {
            Trade trade = trade(level, npc);
            if (count(player, trade.buys) < trade.buyCount) {
               player.sendSystemMessage(Component.translatable("encounter.ancientwarfare.lacking", trade.buyCount, item(trade.buys)).withStyle(ChatFormatting.RED));
               return;
            }

            take(player, trade.buys, trade.buyCount);
            Coins.give(player, trade.buyPrice);
            FactionRumours.say(player, speaker, Component.translatable("trade.ancientwarfare.sold"));
         }
         case "gift" -> {
            if (!npc.getPersistentData().getBoolean(HEAD) || !Coins.take(player, GIFT_PRICE)) {
               return;
            }

            changeOpinion(npc, player, 10);
            standings.adjustStanding(player.getUUID(), npc.getFactionName(), 1);
            FactionRumours.say(player, speaker, Component.translatable("head.ancientwarfare.gift.done"));
         }
         case "persuade" -> {
            if (!npc.getPersistentData().getBoolean(HEAD)) {
               return;
            }

            // The better he thinks of the player, and the better the country does, the likelier he listens.
            double chance = 0.25 + opinion(npc, player) / 200.0 + standings.getStanding(player.getUUID(), npc.getFactionName()) / 400.0;
            if (random.nextDouble() < chance) {
               changeOpinion(npc, player, 15);
               standings.adjustStanding(player.getUUID(), npc.getFactionName(), 5);
               FactionWorldData.Settlement town = town(level, npc);
               if (town != null) {
                  town.order = Math.min(100, town.order + 3);
                  FactionWorldData.get(level).setDirty();
               }

               FactionRumours.say(player, speaker, Component.translatable("head.ancientwarfare.persuade.ok"));
            } else {
               changeOpinion(npc, player, -5);
               FactionRumours.say(player, speaker, Component.translatable("head.ancientwarfare.persuade.bad"));
            }
         }
         case "errand" -> {
            if (!giver) {
               return;
            }

            if (opinion(npc, player) < -20) {
               FactionRumours.say(player, speaker, Component.translatable("head.ancientwarfare.errand.refused"));
               return;
            }

            Errand errand = ERRANDS[random.nextInt(ERRANDS.length)];
            npc.getPersistentData().putString(ERRAND, errand.key);
            FactionRumours.say(player, speaker, Component.translatable("head.ancientwarfare.errand." + errand.key, errand.count, item(errand.item), errand.reward));
         }
         case "deliver" -> {
            Errand errand = errand(npc);
            if (errand == null) {
               return;
            }

            if (count(player, errand.item) < errand.count) {
               player.sendSystemMessage(Component.translatable("encounter.ancientwarfare.lacking", errand.count, item(errand.item)).withStyle(ChatFormatting.RED));
               return;
            }

            take(player, errand.item, errand.count);
            npc.getPersistentData().remove(ERRAND);
            Coins.give(player, errand.reward);
            changeOpinion(npc, player, 12);
            standings.adjustStanding(player.getUUID(), npc.getFactionName(), 4);
            FactionWorldData.Settlement town = town(level, npc);
            if (town != null) {
               town.wealth += 10;
               FactionWorldData.get(level).setDirty();
            }

            FactionRumours.say(player, speaker, Component.translatable("head.ancientwarfare.errand.done", errand.reward));
         }
         case "steal" -> {
            if (!npc.getPersistentData().getBoolean(HEAD)) {
               return;
            }

            long day = level.getDayTime() / FactionSimulation.DAY_TICKS;
            if (npc.getPersistentData().getLong(STOLEN) == day + 1) {
               FactionRumours.say(player, speaker, Component.translatable("head.ancientwarfare.steal.watchful"));
               return;
            }

            npc.getPersistentData().putLong(STOLEN, day + 1);
            FactionWorldData.Settlement town = town(level, npc);
            if (random.nextDouble() < 0.45) {
               int purse = 15 + random.nextInt(30);
               if (town != null) {
                  purse = Math.min(purse + town.wealth() / 50, Math.max(10, town.wealth() / 5));
                  town.wealth = Math.max(0, town.wealth - purse);
                  FactionWorldData.get(level).setDirty();
               }

               Coins.give(player, purse);
               player.sendSystemMessage(Component.translatable("head.ancientwarfare.steal.ok", purse).withStyle(ChatFormatting.DARK_GREEN));
            } else {
               changeOpinion(npc, player, -40);
               standings.adjustStanding(player.getUUID(), npc.getFactionName(), -15);
               player.sendSystemMessage(Component.translatable("head.ancientwarfare.steal.bad").withStyle(ChatFormatting.RED));
            }
         }
         case "preach" -> {
            MutableComponent choices = Component.literal("  ");
            for (int faith = 0; faith < FactionFaith.KEYS.length; faith++) {
               choices.append(button(FactionFaith.name(faith), "/awtownsfolk " + npc.getId() + " preach" + faith, ChatFormatting.LIGHT_PURPLE, Component.translatable("head.ancientwarfare.preach.hint"))).append("  ");
            }

            player.sendSystemMessage(choices);
         }
         default -> {
            if (action.startsWith("preach") && action.length() == 7 && npc.getPersistentData().getBoolean(HEAD)) {
               int faith = Math.floorMod(action.charAt(6) - '0', FactionFaith.KEYS.length);
               FactionWorldData.Settlement town = town(level, npc);
               if (town == null || FaithSpread.faith(level, town) == faith) {
                  FactionRumours.say(player, speaker, Component.translatable("head.ancientwarfare.preach.same"));
                  return;
               }

               if (!Coins.take(player, FaithSpread.PREACHER_PRICE)) {
                  player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
                  return;
               }

               if (!FaithSpread.sendPreacher(level, town, faith)) {
                  Coins.give(player, FaithSpread.PREACHER_PRICE);
                  FactionRumours.say(player, speaker, Component.translatable("head.ancientwarfare.preach.busy"));
                  return;
               }

               // The realm's own priests take it ill.
               changeOpinion(npc, player, -10);
               standings.adjustStanding(player.getUUID(), npc.getFactionName(), -3);
               player.sendSystemMessage(Component.translatable("head.ancientwarfare.preach.sent", FactionFaith.name(faith)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
         }
      }
   }
}
