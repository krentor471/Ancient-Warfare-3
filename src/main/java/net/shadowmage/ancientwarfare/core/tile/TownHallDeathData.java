package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class TownHallDeathData extends SavedData {
   private static final String DATA_ID = "ancientwarfare_pending_npc_deaths";
   private final Map<BlockPos, List<TownHallBlockEntity.NpcDeathEntry>> pending = new HashMap<>();

   public static TownHallDeathData get(ServerLevel level) {
      return (TownHallDeathData)level.getDataStorage().computeIfAbsent(TownHallDeathData::load, TownHallDeathData::new, "ancientwarfare_pending_npc_deaths");
   }

   public void enqueue(BlockPos pos, TownHallBlockEntity.NpcDeathEntry entry) {
      List<TownHallBlockEntity.NpcDeathEntry> entries = this.pending.computeIfAbsent(pos.immutable(), ignored -> new ArrayList<>());
      if (!entries.stream().anyMatch(existing -> existing.recordId().equals(entry.recordId()))) {
         entries.add(entry);

         while (entries.size() > 64) {
            entries.remove(0);
         }

         this.setDirty();
      }
   }

   public List<TownHallBlockEntity.NpcDeathEntry> take(BlockPos pos, UUID hallOwner) {
      List<TownHallBlockEntity.NpcDeathEntry> entries = this.pending.get(pos);
      if (entries == null) {
         return List.of();
      } else {
         List<TownHallBlockEntity.NpcDeathEntry> accepted = new ArrayList<>();
         entries.removeIf(entry -> {
            CompoundTag details = entry.details();
            if (hallOwner == null || details.hasUUID("Owner") && hallOwner.equals(details.getUUID("Owner"))) {
               accepted.add(entry);
               return true;
            } else {
               return false;
            }
         });
         if (entries.isEmpty()) {
            this.pending.remove(pos);
         }

         if (!accepted.isEmpty()) {
            this.setDirty();
         }

         return accepted;
      }
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag halls = new ListTag();
      this.pending.forEach((pos, entries) -> {
         CompoundTag hall = new CompoundTag();
         hall.putLong("Pos", pos.asLong());
         ListTag records = new ListTag();

         for (TownHallBlockEntity.NpcDeathEntry entry : entries) {
            records.add(entry.save());
         }

         hall.put("Records", records);
         halls.add(hall);
      });
      tag.put("TownHalls", halls);
      return tag;
   }

   public static TownHallDeathData load(CompoundTag tag) {
      TownHallDeathData data = new TownHallDeathData();
      ListTag halls = tag.getList("TownHalls", 10);

      for (int i = 0; i < halls.size(); i++) {
         CompoundTag hall = halls.getCompound(i);
         BlockPos pos = BlockPos.of(hall.getLong("Pos"));
         ListTag entries = hall.getList("Records", 10);

         for (int j = 0; j < entries.size(); j++) {
            data.enqueue(pos, TownHallBlockEntity.NpcDeathEntry.load(entries.getCompound(j)));
         }
      }

      data.setDirty(false);
      return data;
   }
}
