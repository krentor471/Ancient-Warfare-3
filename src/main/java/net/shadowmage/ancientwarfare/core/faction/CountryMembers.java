package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Players band together in one player's country: its founder invites, the invited join, and any
 * member may leave. A member shares the country's research (what the founder's books have taught).
 */
public final class CountryMembers extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_country_members";
   /** Member to the founder of the country they joined. */
   private final Map<UUID, UUID> memberOf = new HashMap<>();
   /** Open invitations: invited player to the founders who asked them; not saved. */
   private static final Map<UUID, Set<UUID>> INVITED = new HashMap<>();

   public static CountryMembers get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(CountryMembers::load, CountryMembers::new, DATA_NAME);
   }

   /** The founder of the country the player belongs to: the player themself unless they joined another. */
   public static UUID countryOf(ServerLevel level, UUID player) {
      return get(level).memberOf.getOrDefault(player, player);
   }

   public static Optional<UUID> joined(ServerLevel level, UUID player) {
      return Optional.ofNullable(get(level).memberOf.get(player));
   }

   public static Set<UUID> members(ServerLevel level, UUID founder) {
      Set<UUID> members = new HashSet<>();
      get(level).memberOf.forEach((member, country) -> {
         if (country.equals(founder)) {
            members.add(member);
         }
      });
      return members;
   }

   public static void invite(ServerPlayer founder, ServerPlayer invited) {
      ServerLevel level = founder.serverLevel();
      if (founder == invited) {
         founder.sendSystemMessage(Component.translatable("country.ancientwarfare.invite_self").withStyle(ChatFormatting.RED));
         return;
      }

      if (get(level).memberOf.containsKey(founder.getUUID())) {
         founder.sendSystemMessage(Component.translatable("country.ancientwarfare.not_founder").withStyle(ChatFormatting.RED));
         return;
      }

      INVITED.computeIfAbsent(invited.getUUID(), id -> new HashSet<>()).add(founder.getUUID());
      founder.sendSystemMessage(Component.translatable("country.ancientwarfare.invited", invited.getDisplayName()).withStyle(ChatFormatting.GREEN));
      invited.sendSystemMessage(Component.translatable("country.ancientwarfare.invitation", founder.getDisplayName())
         .append(" ")
         .append(FactionEnvoys.button("country.ancientwarfare.accept", "/awcountry join " + founder.getScoreboardName(), ChatFormatting.GREEN)));
   }

   public static void join(ServerPlayer player, ServerPlayer founder) {
      ServerLevel level = player.serverLevel();
      Set<UUID> invites = INVITED.getOrDefault(player.getUUID(), Set.of());
      if (!invites.contains(founder.getUUID())) {
         player.sendSystemMessage(Component.translatable("country.ancientwarfare.no_invitation", founder.getDisplayName()).withStyle(ChatFormatting.RED));
         return;
      }

      CountryMembers data = get(level);
      if (!members(level, player.getUUID()).isEmpty()) {
         player.sendSystemMessage(Component.translatable("country.ancientwarfare.has_members").withStyle(ChatFormatting.RED));
         return;
      }

      INVITED.remove(player.getUUID());
      data.memberOf.put(player.getUUID(), founder.getUUID());
      data.setDirty();
      player.sendSystemMessage(Component.translatable("country.ancientwarfare.joined", founder.getDisplayName()).withStyle(ChatFormatting.GREEN));
      founder.sendSystemMessage(Component.translatable("country.ancientwarfare.member_joined", player.getDisplayName()).withStyle(ChatFormatting.GREEN));
   }

   public static void leave(ServerPlayer player) {
      CountryMembers data = get(player.serverLevel());
      if (data.memberOf.remove(player.getUUID()) == null) {
         player.sendSystemMessage(Component.translatable("country.ancientwarfare.not_member").withStyle(ChatFormatting.RED));
         return;
      }

      data.setDirty();
      player.sendSystemMessage(Component.translatable("country.ancientwarfare.left").withStyle(ChatFormatting.YELLOW));
   }

   public static void list(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      UUID founder = countryOf(level, player.getUUID());
      net.minecraft.server.players.GameProfileCache cache = level.getServer().getProfileCache();
      StringBuilder names = new StringBuilder();
      Set<UUID> all = members(level, founder);
      all.add(founder);
      for (UUID id : all) {
         String name = cache == null ? id.toString() : cache.get(id).map(com.mojang.authlib.GameProfile::getName).orElse(id.toString());
         if (names.length() > 0) {
            names.append(", ");
         }
         names.append(name).append(id.equals(founder) ? " *" : "");
      }

      player.sendSystemMessage(Component.translatable("country.ancientwarfare.members", names.toString()));
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      this.memberOf.forEach((member, founder) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Member", member);
         entry.putUUID("Founder", founder);
         list.add(entry);
      });
      tag.put("Members", list);
      return tag;
   }

   private static CountryMembers load(CompoundTag tag) {
      CountryMembers data = new CountryMembers();
      for (Tag element : tag.getList("Members", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         data.memberOf.put(entry.getUUID("Member"), entry.getUUID("Founder"));
      }
      return data;
   }
}
