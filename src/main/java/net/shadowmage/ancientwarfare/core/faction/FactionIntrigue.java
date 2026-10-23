package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.common.research.ResearchBookData;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.research.PlayerResearch;

/**
 * Secret work a player's agents do in a faction's lands: stealing its scholars' learning, bribing a
 * courtier, setting its stores on fire, spreading rumours among its towns, or forging a letter that
 * sets it against its closest friend. Each takes a while and may fail; a failed agent may be caught,
 * and then the faction and the world hear who sent them.
 */
public final class FactionIntrigue extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_intrigue";
   private static final long TRAVEL_TICKS = 3000L;
   /** Of the agents who fail, this share is caught. */
   private static final double CAUGHT_SHARE = 0.6;
   private static final int CAUGHT_STANDING = -25;
   private static final int STOLEN_SCIENCE = 60;
   private static final int BRIBED_FAVOR = 20;
   private static final double BURNED_SHARE = 0.2;
   private static final int RUMOUR_ORDER = 15;
   private static final int FORGED_OPINION = -35;

   public enum Op {
      STEAL(40, 0.5),
      BRIBE(30, 0.7),
      ARSON(25, 0.6),
      RUMOUR(15, 0.75),
      FORGERY(50, 0.45);

      /** Price in silver. */
      public final int cost;
      final double chance;

      Op(int cost, double chance) {
         this.cost = cost;
         this.chance = chance;
      }

      public String key() {
         return this.name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   private record Pending(UUID player, String faction, Op op, long ready) {
   }

   private final List<Pending> pending = new ArrayList<>();

   public static FactionIntrigue get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionIntrigue::load, FactionIntrigue::new, DATA_NAME);
   }

   /** The chance an operation succeeds, against a ruler of this trait, with the player's secret service and ciphers. */
   public static double chance(Op op, int trait, boolean secretService, boolean ciphers) {
      double chance = op.chance * (trait == FactionRulers.Trait.CAUTIOUS.ordinal() ? 0.8 : 1.0) + (secretService ? 0.15 : 0.0) + (ciphers ? 0.05 : 0.0);
      return Mth.clamp(chance, 0.05, 0.95);
   }

   /** Operations under way against the faction, one bit for each. */
   static int pendingMask(ServerLevel level, UUID player, String faction) {
      int mask = 0;
      for (Pending entry : get(level).pending) {
         if (entry.player.equals(player) && entry.faction.equals(faction)) {
            mask |= 1 << entry.op.ordinal();
         }
      }

      return mask;
   }

   static Component start(ServerPlayer player, String faction, Op op) {
      ServerLevel level = player.serverLevel();
      FactionIntrigue intrigue = get(level);
      Component name = FactionNews.factionName(faction);
      if ((pendingMask(level, player.getUUID(), faction) & 1 << op.ordinal()) != 0) {
         return Component.translatable("intrigue.ancientwarfare.already", name);
      } else if (!PlayerDiplomacy.takeEmeralds(player, op.cost)) {
         return Component.translatable("intrigue.ancientwarfare.missing", op.cost);
      }

      intrigue.pending.add(new Pending(player.getUUID(), faction, op, level.getGameTime() + TRAVEL_TICKS));
      intrigue.setDirty();
      return Component.translatable("intrigue.ancientwarfare.sent", Component.translatable("intrigue.ancientwarfare.op." + op.key()), name);
   }

   static void tick(ServerLevel level, FactionWorldData data) {
      FactionIntrigue intrigue = get(level);
      if (level.getGameTime() % 20L != 0L || intrigue.pending.isEmpty()) {
         return;
      }

      long now = level.getGameTime();
      for (Pending entry : new ArrayList<>(intrigue.pending)) {
         if (now < entry.ready) {
            continue;
         }

         intrigue.pending.remove(entry);
         intrigue.setDirty();
         FactionWorldData.Ruler ruler = FactionRulers.ruler(data, entry.faction, level.getRandom());
         double chance = chance(
            entry.op, ruler.trait(), PlayerResearch.has(level, entry.player, PlayerResearch.SECRET_SERVICE), PlayerResearch.has(level, entry.player, PlayerResearch.CIPHERS)
         );
         Component name = FactionNews.factionName(entry.faction);
         Component opName = Component.translatable("intrigue.ancientwarfare.op." + entry.op.key());
         if (level.getRandom().nextDouble() < chance) {
            Component result = carryOut(level, data, entry);
            FactionNews.tellPlayer(level, entry.player, result.copy().withStyle(ChatFormatting.AQUA));
         } else if (level.getRandom().nextDouble() < CAUGHT_SHARE * (PlayerResearch.has(level, entry.player, PlayerResearch.SECRET_SERVICE) ? 0.5 : 1.0)) {
            FactionStandingData.get(level).adjustStanding(entry.player, entry.faction, CAUGHT_STANDING);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.player);
            Component who = player != null ? player.getDisplayName() : Component.literal("?");
            FactionNews.tell(level, Component.translatable("news.ancientwarfare.agent_caught", name, opName, who));
         } else {
            FactionNews.tellPlayer(level, entry.player, Component.translatable("intrigue.ancientwarfare.failed", opName, name).withStyle(ChatFormatting.GRAY));
         }
      }
   }

   private static Component carryOut(ServerLevel level, FactionWorldData data, Pending entry) {
      String faction = entry.faction;
      Component name = FactionNews.factionName(faction);
      switch (entry.op) {
         case STEAL -> {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.player);
            ItemStack book = player == null ? ItemStack.EMPTY : findBook(player);
            if (book.isEmpty()) {
               return Component.translatable("intrigue.ancientwarfare.steal.no_book", name);
            }

            ResearchBookData.addScience(book, STOLEN_SCIENCE);
            return Component.translatable("intrigue.ancientwarfare.steal.done", name, STOLEN_SCIENCE);
         }
         case BRIBE -> {
            FactionCourt.Role role = FactionCourt.bribe(level, faction, entry.player, BRIBED_FAVOR);
            return Component.translatable("intrigue.ancientwarfare.bribe.done", FactionCourt.roleName(role), name);
         }
         case ARSON -> {
            int[] stores = data.stores(faction);
            int burned = 0;
            for (int kind = 0; kind < stores.length; kind++) {
               int loss = (int)(stores[kind] * BURNED_SHARE);
               stores[kind] -= loss;
               burned += loss;
            }

            data.setDirty();
            return Component.translatable("intrigue.ancientwarfare.arson.done", name, burned);
         }
         case RUMOUR -> {
            int towns = 0;
            for (FactionWorldData.Settlement settlement : data.settlements()) {
               if (settlement.faction().equals(faction)) {
                  settlement.order = Math.max(0, settlement.order - RUMOUR_ORDER);
                  towns++;
               }
            }

            data.setDirty();
            return Component.translatable("intrigue.ancientwarfare.rumour.done", name, towns);
         }
         default -> {
            String friend = closestFriend(level, faction);
            if (friend == null) {
               return Component.translatable("intrigue.ancientwarfare.forgery.no_friend", name);
            }

            FactionDiplomacy.adjustOpinion(level, faction, friend, FORGED_OPINION);
            FactionDiplomacy.adjustOpinion(level, friend, faction, FORGED_OPINION);
            FactionNews.tension(level, faction, friend);
            return Component.translatable("intrigue.ancientwarfare.forgery.done", name, FactionNews.factionName(friend));
         }
      }
   }

   private static ItemStack findBook(ServerPlayer player) {
      for (ItemStack stack : player.getInventory().items) {
         if (stack.is(AWCoreItems.RESEARCH_BOOK.get())) {
            return stack;
         }
      }

      return player.getOffhandItem().is(AWCoreItems.RESEARCH_BOOK.get()) ? player.getOffhandItem() : ItemStack.EMPTY;
   }

   /** The faction it thinks best of, short of an outlaw. */
   private static String closestFriend(ServerLevel level, String faction) {
      String best = null;
      int bestOpinion = Integer.MIN_VALUE;
      for (String other : FactionRegistry.factionNames()) {
         if (other.equals(faction) || FactionDiplomacy.isOutlaw(other) || !FactionDiplomacy.canNegotiate(faction, other)) {
            continue;
         }

         FactionDiplomacy.Relation relation = FactionDiplomacy.relation(level, faction, other);
         if (relation.state() != RelationState.WAR && relation.opinion() > bestOpinion) {
            best = other;
            bestOpinion = relation.opinion();
         }
      }

      return best;
   }

   private static FactionIntrigue load(CompoundTag tag) {
      FactionIntrigue intrigue = new FactionIntrigue();
      for (Tag element : tag.getList("Pending", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         try {
            intrigue.pending.add(new Pending(entry.getUUID("Player"), entry.getString("Faction"), Op.valueOf(entry.getString("Op")), entry.getLong("Ready")));
         } catch (IllegalArgumentException e) {
            AncientWarfareCore.LOG.warn("Dropping unknown intrigue {}", entry);
         }
      }

      return intrigue;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      for (Pending entry : this.pending) {
         CompoundTag compound = new CompoundTag();
         compound.putUUID("Player", entry.player);
         compound.putString("Faction", entry.faction);
         compound.putString("Op", entry.op.name());
         compound.putLong("Ready", entry.ready);
         list.add(compound);
      }

      tag.put("Pending", list);
      return tag;
   }
}
