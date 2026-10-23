package net.shadowmage.ancientwarfare.core.research;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/**
 * What each researcher knows, kept with the world rather than only in their book, so the systems a
 * technology opens (the construction view, the command view, alliances, spies) can ask whether a
 * player has learned it while the book sits on a desk. A book's research is copied here whenever
 * a research station holds it; the player's client is told what they know when they join and
 * whenever they learn something new. Players in creative mode know everything.
 */
public final class PlayerResearch extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_player_research";
   /** Plans and marks building work for the player's workers: the builder's wand and its top-down view. */
   public static final String SURVEYING = "surveying";
   /** Builds whole buildings from templates with the builder's wand. */
   public static final String ARCHITECTURE = "architecture";
   /** Paves roads and squares: the floor work of the builder's wand. */
   public static final String ROAD_BUILDING = "road_building";
   /** Commands soldiers from the top-down command view. */
   public static final String FIELD_COMMAND = "field_command";
   /** Offers alliances to factions. */
   public static final String DIPLOMACY = "diplomacy";
   /** Sends spies to factions. */
   public static final String ESPIONAGE = "espionage";
   private static final float SCIENCE_PER_VILLAGE = 0.1F;
   private static final float MAX_SCIENCE_SCALE = 2.0F;
   private final Map<String, Set<String>> known = new HashMap<>();

   public static PlayerResearch get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(PlayerResearch::load, PlayerResearch::new, DATA_NAME);
   }

   private static String key(String researcher) {
      return researcher.toLowerCase(Locale.ROOT);
   }

   public static Set<String> known(ServerPlayer player) {
      return get(player.serverLevel()).known.getOrDefault(key(player.getName().getString()), Set.of());
   }

   public static boolean has(ServerPlayer player, String research) {
      return player.isCreative() || known(player).contains(research);
   }

   /** Whether the player knows the research; if not, tells them which one they need. */
   public static boolean require(ServerPlayer player, String research) {
      if (has(player, research)) {
         return true;
      }

      player.displayClientMessage(needs(research), true);
      return false;
   }

   public static Component needs(String research) {
      return Component.translatable("research.ancientwarfare.needed", Component.translatable("research.ancientwarfare." + research)).withStyle(ChatFormatting.RED);
   }

   /** A research station holds the researcher's book: what the book knows, the world now knows too. */
   public static void learn(ServerLevel level, String researcher, Set<String> completed) {
      PlayerResearch data = get(level);
      Set<String> known = data.known.computeIfAbsent(key(researcher), ignored -> new TreeSet<>());
      if (known.containsAll(completed)) {
         return;
      }

      Set<String> fresh = new TreeSet<>(completed);
      fresh.removeAll(known);
      known.addAll(completed);
      data.setDirty();
      ServerPlayer player = level.getServer().getPlayerList().getPlayerByName(researcher);
      java.util.UUID owner = owner(level, researcher);
      for (String research : fresh) {
         if (owner != null && research.startsWith(ResearchRegistry.ERA_PREFIX)) {
            net.shadowmage.ancientwarfare.core.faction.CountryEras.reached(level, owner, research);
         }
      }

      if (player != null) {
         for (String research : fresh) {
            player.sendSystemMessage(
               Component.translatable("research.ancientwarfare.learned", Component.translatable("research.ancientwarfare." + research)).withStyle(ChatFormatting.GOLD)
            );
         }

         sync(player);
      }
   }

   /** The player a researcher's name belongs to, whether they are online or not. */
   @org.jetbrains.annotations.Nullable
   public static java.util.UUID owner(ServerLevel level, String researcher) {
      ServerPlayer player = level.getServer().getPlayerList().getPlayerByName(researcher);
      if (player != null) {
         return player.getUUID();
      }

      net.minecraft.server.players.GameProfileCache cache = level.getServer().getProfileCache();
      return cache == null ? null : cache.get(researcher).map(com.mojang.authlib.GameProfile::getId).orElse(null);
   }

   /**
    * Research grows dearer as a country grows, as in Civilization: every village past the first adds
    * a tenth to the science each technology costs, up to twice the price.
    */
   public static float scienceScale(ServerLevel level, String researcher) {
      java.util.UUID owner = owner(level, researcher);
      if (owner == null) {
         return 1.0F;
      }

      int villages = 0;
      for (java.util.UUID village : net.shadowmage.ancientwarfare.core.faction.FactionWorldData.get(level).villages().values()) {
         if (owner.equals(village)) {
            villages++;
         }
      }

      return Math.min(MAX_SCIENCE_SCALE, 1.0F + SCIENCE_PER_VILLAGE * Math.max(0, villages - 1));
   }

   public static void sync(ServerPlayer player) {
      AWCoreNetwork.sendResearchKnown(player, known(player));
   }

   private static PlayerResearch load(CompoundTag tag) {
      PlayerResearch data = new PlayerResearch();
      CompoundTag researchers = tag.getCompound("Researchers");
      for (String researcher : researchers.getAllKeys()) {
         Set<String> known = new TreeSet<>();
         ListTag list = researchers.getList(researcher, Tag.TAG_STRING);
         for (int index = 0; index < list.size(); index++) {
            known.add(list.getString(index));
         }

         data.known.put(researcher, known);
      }

      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag researchers = new CompoundTag();
      this.known.forEach((researcher, known) -> {
         ListTag list = new ListTag();
         known.forEach(research -> list.add(StringTag.valueOf(research)));
         researchers.put(researcher, list);
      });
      tag.put("Researchers", researchers);
      return tag;
   }
}
