package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * The player's own trade ventures, fitted out at a merchants' guild. For every road out of the
 * town the guild shows what would pay best to carry: the good, what a load costs here, what it
 * should fetch there, how many days the trip takes and how likely brigands are to take it. The
 * player pays for the load; when the caravan arrives the takings are paid out, or the load is lost.
 */
public final class TradeVentures {
   private static final String TAG = "AWVentures";
   public static final int LOAD = 64;
   private static final double TOLLS = 0.9;
   private static final int MAX_VENTURES = 3;

   record Offer(FactionWorldData.Settlement to, int kind, int cost, int takings, int days, int risk) {
      int profit() {
         return this.takings - this.cost;
      }
   }

   private TradeVentures() {
   }

   /** The best good to carry on each road out of the town. */
   static List<Offer> offers(ServerLevel level, FactionWorldData.Settlement from) {
      FactionWorldData data = FactionWorldData.get(level);
      TownMarkets markets = TownMarkets.get(level);
      List<Offer> offers = new ArrayList<>();
      List<int[]> camps = BanditCamps.standing(level);
      for (FactionWorldData.Route route : data.routes()) {
         int other = route.a() == from.id() ? route.b() : route.b() == from.id() ? route.a() : -1;
         FactionWorldData.Settlement to = other < 0 ? null : data.settlement(other);
         if (to == null || route.blocked()) {
            continue;
         }

         Offer best = null;
         double distance = Math.sqrt(to.horizontalDistanceSquared(from.center().getX(), from.center().getZ()));
         int days = 1 + (int)(distance / 1200.0);
         int midX = (from.center().getX() + to.center().getX()) / 2;
         int midZ = (from.center().getZ() + to.center().getZ()) / 2;
         long near = camps.stream().filter(c -> Math.abs(c[0] - midX) + Math.abs(c[1] - midZ) < distance / 2 + 300).count();
         int risk = (int)Math.min(50, 5 + near * 8);
         for (int kind = 0; kind < FactionResources.KINDS; kind++) {
            int cost = (int)Math.ceil(LOAD * FactionResources.PRICE[kind] * markets.price(from.id(), kind));
            int takings = (int)(LOAD * FactionResources.PRICE[kind] * markets.price(to.id(), kind) * TOLLS);
            if (best == null || takings - cost > best.profit()) {
               best = new Offer(to, kind, cost, takings, days, risk);
            }
         }

         if (best != null) {
            offers.add(best);
         }
      }

      return offers;
   }

   /** The guild's page of ventures, opened as the head's window. */
   static void show(ServerPlayer player, FactionNpcEntity master, FactionWorldData.Settlement town) {
      ServerLevel level = player.serverLevel();
      List<Offer> offers = offers(level, town);
      player.sendSystemMessage(Component.translatable("head.ancientwarfare.panel", Component.translatable("venture.ancientwarfare.title")));
      player.sendSystemMessage(Component.translatable("venture.ancientwarfare.intro", LOAD).withStyle(ChatFormatting.GRAY));
      ListTag running = player.getPersistentData().getList(TAG, Tag.TAG_COMPOUND);
      for (int i = 0; i < running.size(); i++) {
         CompoundTag v = running.getCompound(i);
         long left = Math.max(0, v.getLong("Due") - FactionWorldData.get(level).lastSimulatedDay());
         player.sendSystemMessage(Component.translatable("venture.ancientwarfare.running", v.getString("To"), TownMarkets.goodName(v.getInt("Kind")), left).withStyle(ChatFormatting.AQUA));
      }

      if (offers.isEmpty()) {
         player.sendSystemMessage(Component.translatable("venture.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      }

      for (int i = 0; i < offers.size(); i++) {
         Offer offer = offers.get(i);
         final int index = i;
         int expected = (int)Math.round(offer.takings() * (100 - offer.risk()) / 100.0) - offer.cost();
         ChatFormatting color = offer.profit() <= 0 ? ChatFormatting.DARK_GRAY : expected > 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
         Component hint = Component.translatable("venture.ancientwarfare.hint", offer.cost(), offer.takings(), offer.profit(), offer.days(), offer.risk(), expected);
         MutableComponent label = Component.translatable("venture.ancientwarfare.offer", offer.to().nameComponent(), TownMarkets.goodName(offer.kind()), offer.cost(), offer.profit() >= 0 ? "+" + offer.profit() : String.valueOf(offer.profit()), offer.days(), offer.risk());
         player.sendSystemMessage(Component.literal("  ").append(Component.literal("[").append(label).append("]").withStyle(style -> style.withColor(color)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/awtownsfolk " + master.getId() + " venture" + index))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hint)))));
      }
   }

   static void start(ServerPlayer player, FactionWorldData.Settlement town, int index) {
      ServerLevel level = player.serverLevel();
      List<Offer> offers = offers(level, town);
      if (index < 0 || index >= offers.size()) {
         return;
      }

      Offer offer = offers.get(index);
      ListTag running = player.getPersistentData().getList(TAG, Tag.TAG_COMPOUND);
      if (running.size() >= MAX_VENTURES) {
         player.sendSystemMessage(Component.translatable("venture.ancientwarfare.too_many", MAX_VENTURES).withStyle(ChatFormatting.RED));
         return;
      }

      if (offer.profit() <= 0) {
         player.sendSystemMessage(Component.translatable("venture.ancientwarfare.no_profit").withStyle(ChatFormatting.RED));
         return;
      }

      if (!Coins.take(player, offer.cost())) {
         player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
         return;
      }

      CompoundTag venture = new CompoundTag();
      venture.putString("To", offer.to().nameComponent().getString());
      venture.putInt("Kind", offer.kind());
      venture.putInt("Takings", offer.takings());
      venture.putInt("Risk", offer.risk());
      venture.putLong("Due", FactionWorldData.get(level).lastSimulatedDay() + offer.days());
      running.add(venture);
      player.getPersistentData().put(TAG, running);
      player.sendSystemMessage(Component.translatable("venture.ancientwarfare.started", TownMarkets.goodName(offer.kind()), offer.to().nameComponent(), offer.days()).withStyle(ChatFormatting.GREEN));
   }

   /** Arrived caravans pay out to their owners. */
   static void tick(ServerLevel level) {
      if (level.getGameTime() % 200L != 0L) {
         return;
      }

      long today = FactionWorldData.get(level).lastSimulatedDay();
      for (ServerPlayer player : level.players()) {
         ListTag running = player.getPersistentData().getList(TAG, Tag.TAG_COMPOUND);
         boolean changed = false;
         for (int i = running.size() - 1; i >= 0; i--) {
            CompoundTag v = running.getCompound(i);
            if (v.getLong("Due") > today) {
               continue;
            }

            running.remove(i);
            changed = true;
            if (level.getRandom().nextInt(100) < v.getInt("Risk")) {
               player.sendSystemMessage(Component.translatable("venture.ancientwarfare.robbed", TownMarkets.goodName(v.getInt("Kind")), v.getString("To")).withStyle(ChatFormatting.RED));
            } else {
               Coins.give(player, v.getInt("Takings"));
               player.sendSystemMessage(Component.translatable("venture.ancientwarfare.arrived", TownMarkets.goodName(v.getInt("Kind")), v.getString("To"), v.getInt("Takings")).withStyle(ChatFormatting.GOLD));
            }
         }

         if (changed) {
            player.getPersistentData().put(TAG, running);
         }
      }
   }
}
