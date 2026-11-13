package net.shadowmage.ancientwarfare.core.faction;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * The player's own faith, and what the four creeds think of those outside them.
 *
 * A player takes up a creed at a temple or shrine of it. The faithful of every realm warm a little
 * each day to one who shares their creed and cool to one who does not, the more the more zealous
 * the creed: the Flame burns unbelievers, the Old Gods despise the weak of faith, the Sun judges by
 * law, and the Ancestors care little what strangers believe. Prices in a town follow suit, and the
 * people of a village ruled by a lord of another faith are slow to love him.
 *
 * Each creed has its tale: its history, its myths, and what it asks of the faithful, written in a
 * book that its priests hand out.
 */
public final class PlayerFaith {
   private static final String KEY = "AWFaith";
   /** How hard each creed is on unbelievers, by faith: sun, old gods, ancestors, flame. */
   private static final int[] ZEAL = {1, 2, 0, 3};
   /** Standing the daily warmth of shared faith builds up to, and the coldness of another faith down to. */
   private static final int WARM_CAP = 25;
   private static final int COLD_FLOOR = -20;
   /** Standing lost with the faithful of the creed one turns away from. */
   private static final int APOSTASY = 15;
   private static final int MYTHS = 3;

   private PlayerFaith() {
   }

   private static CompoundTag kept(Player player) {
      CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
      player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
      return persisted;
   }

   /** The player's creed, or -1 for none. */
   public static int faith(Player player) {
      CompoundTag kept = kept(player);
      if (!kept.contains(KEY) && player.getPersistentData().contains(KEY)) {
         // Taken up by prayer before the faith was kept across death.
         kept.putInt(KEY, player.getPersistentData().getInt(KEY));
      }

      return kept.contains(KEY) ? kept.getInt(KEY) : -1;
   }

   /** Prayer at a shrine makes the player one of its faithful, as joining does. */
   static void set(Player player, int faith) {
      kept(player).putInt(KEY, faith);
   }

   public static int zeal(int faith) {
      return faith < 0 || faith >= ZEAL.length ? 0 : ZEAL[faith];
   }

   /** -1 when the player is of another creed than these people, 1 of theirs, 0 of none. */
   public static int kinship(ServerLevel level, Player player, String faction) {
      int mine = faith(player);
      if (mine < 0 || FactionDiplomacy.isOutlaw(faction)) {
         return 0;
      }

      return mine == FactionFaith.faith(level, faction) ? 1 : -1;
   }

   /** A price factor for this player in a town of this faction: kin pay less, unbelievers more, by the creed's zeal. */
   public static double priceFactor(ServerLevel level, Player player, String faction) {
      int kin = kinship(level, player, faction);
      if (kin > 0) {
         return 0.95;
      } else if (kin < 0) {
         return 1.0 + 0.05 * zeal(FactionFaith.faith(level, faction));
      }

      return 1.0;
   }

   /** The player takes up a creed, at a temple or shrine of it. */
   public static void join(ServerPlayer player, int faith) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement town = FactionCrime.settlementAt(level, player.blockPosition());
      if (town == null || FaithSpread.faith(level, town) != faith || !hasHolyPlace(town)) {
         player.sendSystemMessage(Component.translatable("faith.ancientwarfare.join_where", FactionFaith.name(faith)).withStyle(ChatFormatting.RED));
         return;
      }

      int old = faith(player);
      if (old == faith) {
         return;
      }

      if (old >= 0) {
         // The faithful of the creed left behind do not forget it.
         FactionStandingData standings = FactionStandingData.get(level);
         for (String faction : factions(data)) {
            if (FactionFaith.faith(level, faction) == old) {
               standings.adjustStanding(player.getUUID(), faction, -APOSTASY);
            }
         }
      }

      kept(player).putInt(KEY, faith);
      player.sendSystemMessage(Component.translatable("faith.ancientwarfare.joined", FactionFaith.name(faith), trait(faith)).withStyle(ChatFormatting.LIGHT_PURPLE));
      giveBook(player, faith);
   }

   private static boolean hasHolyPlace(FactionWorldData.Settlement town) {
      return town.buildings().stream().anyMatch(b -> {
         String lower = b.toLowerCase(java.util.Locale.ROOT);
         return lower.contains("temple") || lower.contains("church") || lower.contains("chapel") || lower.contains("shrine") || lower.contains("monast");
      }) || town.halls().stream().anyMatch(h -> h.kind().equals(TownBuildings.Kind.TEMPLE.key()) || h.kind().equals(TownBuildings.Kind.SHRINE.key())
         || h.kind().equals(TownBuildings.Kind.MONASTERY.key())) || WorldMap.isTemple(town);
   }

   private static Set<String> factions(FactionWorldData data) {
      Set<String> factions = new LinkedHashSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction()) && !PlayerCountry.isLand(settlement.faction())) {
            factions.add(settlement.faction());
         }
      }

      return factions;
   }

   /** Once a day: the faithful warm to their own and cool to unbelievers. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionStandingData standings = FactionStandingData.get(level);
      Set<String> factions = factions(data);
      for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
         int mine = faith(player);
         if (mine < 0) {
            continue;
         }

         for (String faction : factions) {
            int theirs = FactionFaith.faith(level, faction);
            int standing = standings.getStanding(player.getUUID(), faction);
            if (theirs == mine && standing < WARM_CAP) {
               standings.adjustStanding(player.getUUID(), faction, 1);
            } else if (theirs != mine && standing > COLD_FLOOR && zeal(theirs) > 0) {
               standings.adjustStanding(player.getUUID(), faction, -zeal(theirs));
            }
         }
      }
   }

   /** A village the player rules: its people take to a lord of their faith, and are slow to love one of another. */
   static void village(ServerLevel level, FactionWorldData.VillageState state, java.util.UUID owner) {
      ServerPlayer lord = level.getServer().getPlayerList().getPlayer(owner);
      if (lord == null || state.from == null || state.from.isEmpty()) {
         return;
      }

      int mine = faith(lord);
      int theirs = FactionFaith.faith(level, state.from);
      if (mine == theirs) {
         state.loyalty = Math.min(100, state.loyalty + 1);
      } else {
         // A lord of no faith is merely a stranger; one of another faith is an unbeliever.
         int cold = mine < 0 ? 1 : 1 + zeal(theirs);
         state.loyalty = Math.max(10, state.loyalty - cold);
         state.order = Math.max(10, state.order - (mine < 0 ? 0 : 1));
      }
   }

   static Component trait(int faith) {
      return Component.translatable("faith.ancientwarfare." + FactionFaith.KEYS[faith] + ".trait");
   }

   /** The book of a creed: its history, myths and what it asks, as a priest would hand it out. */
   public static void giveBook(ServerPlayer player, int faith) {
      String key = "faith.ancientwarfare." + FactionFaith.KEYS[faith];
      ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
      CompoundTag tag = book.getOrCreateTag();
      tag.putString("title", Component.translatable(key + ".book").getString());
      tag.putString("author", Component.translatable("faith.ancientwarfare.priests").getString());
      ListTag pages = new ListTag();
      pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(key + ".history"))));
      for (int i = 0; i < MYTHS; i++) {
         pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(key + ".myth" + i))));
      }

      pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(key + ".trait"))));
      tag.put("pages", pages);
      // The title shows in the reader's own language.
      book.setHoverName(Component.translatable(key + ".book").withStyle(style -> style.withItalic(false)));
      tag.putBoolean("resolved", false);
      player.getInventory().placeItemBackInInventory(book);
   }

   /** /awfaith: the player's creed and, in a town with a temple or shrine, what may be done there. */
   public static void show(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      int mine = faith(player);
      player.sendSystemMessage(mine < 0
         ? Component.translatable("faith.ancientwarfare.none").withStyle(ChatFormatting.GRAY)
         : Component.translatable("faith.ancientwarfare.yours", FactionFaith.name(mine), trait(mine)).withStyle(ChatFormatting.LIGHT_PURPLE));
      FactionWorldData.Settlement town = FactionCrime.settlementAt(level, player.blockPosition());
      if (town != null && hasHolyPlace(town)) {
         int here = FaithSpread.faith(level, town);
         MutableComponent line = Component.translatable("faith.ancientwarfare.here", FactionFaith.name(here)).withStyle(ChatFormatting.GRAY).append("  ");
         line.append(button(Component.translatable("faith.ancientwarfare.read"), "/awfaith book " + FactionFaith.KEYS[here], ChatFormatting.AQUA));
         if (here != mine) {
            line.append("  ").append(button(Component.translatable("faith.ancientwarfare.convert"), "/awfaith join " + FactionFaith.KEYS[here], ChatFormatting.GOLD));
         }

         player.sendSystemMessage(line);
      }
   }

   /** The book of the creed of the town the player is in. */
   public static void book(ServerPlayer player, int faith) {
      FactionWorldData.Settlement town = FactionCrime.settlementAt(player.serverLevel(), player.blockPosition());
      if (town == null || FaithSpread.faith(player.serverLevel(), town) != faith || !hasHolyPlace(town)) {
         player.sendSystemMessage(Component.translatable("faith.ancientwarfare.join_where", FactionFaith.name(faith)).withStyle(ChatFormatting.RED));
         return;
      }

      giveBook(player, faith);
   }

   public static int byKey(String key) {
      for (int i = 0; i < FactionFaith.KEYS.length; i++) {
         if (FactionFaith.KEYS[i].equals(key)) {
            return i;
         }
      }

      return -1;
   }

   private static MutableComponent button(Component label, String command, ChatFormatting color) {
      return Component.literal("[").append(label).append("]").withStyle(style -> style.withColor(color)
         .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
         .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
   }
}
