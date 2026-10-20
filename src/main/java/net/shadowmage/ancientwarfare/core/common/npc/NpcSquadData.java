package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Squads are saved NPC groups per player (slots 1-9), recalled onto the command baton like control groups in an RTS. */
public final class NpcSquadData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_npc_squads";
   public static final int SLOTS = 9;
   private final Map<UUID, Map<Integer, List<UUID>>> squads = new HashMap<>();

   public static NpcSquadData get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(NpcSquadData::load, NpcSquadData::new, DATA_NAME);
   }

   public List<UUID> getSquad(UUID playerId, int slot) {
      return new ArrayList<>(this.squads.getOrDefault(playerId, Map.of()).getOrDefault(slot, List.of()));
   }

   public void setSquad(UUID playerId, int slot, List<UUID> members) {
      Map<Integer, List<UUID>> playerSquads = this.squads.computeIfAbsent(playerId, ignored -> new HashMap<>());
      if (members.isEmpty()) {
         playerSquads.remove(slot);
      } else {
         // An NPC belongs to one squad at a time.
         playerSquads.forEach((otherSlot, otherMembers) -> otherMembers.removeAll(members));
         playerSquads.put(slot, new ArrayList<>(members));
      }

      this.setDirty();
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag players = new ListTag();
      this.squads.forEach((playerId, playerSquads) -> {
         CompoundTag playerTag = new CompoundTag();
         playerTag.putUUID("Player", playerId);
         CompoundTag slots = new CompoundTag();
         playerSquads.forEach((slot, members) -> {
            ListTag list = new ListTag();
            members.forEach(uuid -> list.add(NbtUtils.createUUID(uuid)));
            slots.put(Integer.toString(slot), list);
         });
         playerTag.put("Squads", slots);
         players.add(playerTag);
      });
      tag.put("Players", players);
      return tag;
   }

   private static NpcSquadData load(CompoundTag tag) {
      NpcSquadData data = new NpcSquadData();
      ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);

      for (int i = 0; i < players.size(); i++) {
         CompoundTag playerTag = players.getCompound(i);
         if (playerTag.hasUUID("Player")) {
            Map<Integer, List<UUID>> playerSquads = new HashMap<>();
            CompoundTag slots = playerTag.getCompound("Squads");

            for (String key : slots.getAllKeys()) {
               try {
                  int slot = Integer.parseInt(key);
                  List<UUID> members = new ArrayList<>();
                  slots.getList(key, Tag.TAG_INT_ARRAY).forEach(entry -> members.add(NbtUtils.loadUUID(entry)));
                  playerSquads.put(slot, members);
               } catch (NumberFormatException ignored) {
               }
            }

            data.squads.put(playerTag.getUUID("Player"), playerSquads);
         }
      }

      return data;
   }
}
