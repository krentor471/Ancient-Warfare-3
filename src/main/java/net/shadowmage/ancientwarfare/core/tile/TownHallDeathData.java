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
      return (TownHallDeathData)level.m_8895_().m_164861_(TownHallDeathData::load, TownHallDeathData::new, "ancientwarfare_pending_npc_deaths");
   }

   public void enqueue(BlockPos pos, TownHallBlockEntity.NpcDeathEntry entry) {
      List<TownHallBlockEntity.NpcDeathEntry> entries = this.pending.computeIfAbsent(pos.m_7949_(), ignored -> new ArrayList<>());
      if (!entries.stream().anyMatch(existing -> existing.recordId().equals(entry.recordId()))) {
         entries.add(entry);

         while (entries.size() > 64) {
            entries.remove(0);
         }

         this.m_77762_();
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
            if (hallOwner == null || details.m_128403_("Owner") && hallOwner.equals(details.m_128342_("Owner"))) {
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
            this.m_77762_();
         }

         return accepted;
      }
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag halls = new ListTag();
      this.pending.forEach((pos, entries) -> {
         CompoundTag hall = new CompoundTag();
         hall.m_128356_("Pos", pos.m_121878_());
         ListTag records = new ListTag();

         for (TownHallBlockEntity.NpcDeathEntry entry : entries) {
            records.add(entry.save());
         }

         hall.m_128365_("Records", records);
         halls.add(hall);
      });
      tag.m_128365_("TownHalls", halls);
      return tag;
   }

   public static TownHallDeathData load(CompoundTag tag) {
      TownHallDeathData data = new TownHallDeathData();
      ListTag halls = tag.m_128437_("TownHalls", 10);

      for (int i = 0; i < halls.size(); i++) {
         CompoundTag hall = halls.m_128728_(i);
         BlockPos pos = BlockPos.m_122022_(hall.m_128454_("Pos"));
         ListTag entries = hall.m_128437_("Records", 10);

         for (int j = 0; j < entries.size(); j++) {
            data.enqueue(pos, TownHallBlockEntity.NpcDeathEntry.load(entries.m_128728_(j)));
         }
      }

      data.m_77760_(false);
      return data;
   }
}
