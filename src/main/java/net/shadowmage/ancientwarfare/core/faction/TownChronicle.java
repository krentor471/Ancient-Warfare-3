package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Each town keeps a chronicle: its fairs and feasts, fires and floods, riots, new faiths, the
 * houses it sold to strangers. The village head reads out the latest entries.
 */
public final class TownChronicle extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_town_chronicle";
   private static final int KEPT = 40;
   private static final int READ = 10;
   private static final String SEP = "\u0001";

   private final Map<Integer, List<String>> entries = new HashMap<>();

   public static TownChronicle get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(TownChronicle::load, TownChronicle::new, DATA_NAME);
   }

   /** Writes an entry: a lang key under chronicle.ancientwarfare. and its plain arguments. */
   public static void write(ServerLevel level, int town, String key, Object... args) {
      TownChronicle chronicle = get(level);
      StringBuilder entry = new StringBuilder().append(level.getDayTime() / FactionSimulation.DAY_TICKS + 1).append(SEP).append(key);
      for (Object arg : args) {
         entry.append(SEP).append(arg);
      }

      List<String> list = chronicle.entries.computeIfAbsent(town, id -> new ArrayList<>());
      list.add(entry.toString());
      while (list.size() > KEPT) {
         list.remove(0);
      }

      chronicle.setDirty();
   }

   public static void read(ServerPlayer player, FactionWorldData.Settlement town) {
      List<String> list = get(player.serverLevel()).entries.getOrDefault(town.id(), List.of());
      player.sendSystemMessage(Component.translatable("chronicle.ancientwarfare.title", FactionNews.named(town)).withStyle(ChatFormatting.GOLD));
      if (list.isEmpty()) {
         player.sendSystemMessage(Component.translatable("chronicle.ancientwarfare.empty").withStyle(ChatFormatting.GRAY));
         return;
      }

      for (String entry : list.subList(Math.max(0, list.size() - READ), list.size())) {
         String[] parts = entry.split(SEP);
         Object[] args = new Object[parts.length - 2];
         for (int i = 0; i < args.length; i++) {
            String arg = parts[i + 2];
            args[i] = arg.startsWith("#") ? Component.translatable(arg.substring(1)) : arg;
         }
         player.sendSystemMessage(Component.translatable("chronicle.ancientwarfare.day", parts[0]).withStyle(ChatFormatting.GRAY)
            .append(" ").append(Component.translatable("chronicle.ancientwarfare." + parts[1], args).withStyle(ChatFormatting.WHITE)));
      }
   }

   private static TownChronicle load(CompoundTag tag) {
      TownChronicle chronicle = new TownChronicle();
      CompoundTag towns = tag.getCompound("Towns");
      for (String key : towns.getAllKeys()) {
         List<String> list = new ArrayList<>();
         for (Tag entry : towns.getList(key, Tag.TAG_STRING)) {
            list.add(entry.getAsString());
         }

         chronicle.entries.put(Integer.parseInt(key), list);
      }

      return chronicle;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag towns = new CompoundTag();
      this.entries.forEach((id, list) -> {
         ListTag entries = new ListTag();
         list.forEach(entry -> entries.add(StringTag.valueOf(entry)));
         towns.put(String.valueOf(id), entries);
      });
      tag.put("Towns", towns);
      return tag;
   }
}
