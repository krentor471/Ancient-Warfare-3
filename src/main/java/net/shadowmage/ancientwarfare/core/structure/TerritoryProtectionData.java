package net.shadowmage.ancientwarfare.core.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class TerritoryProtectionData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_territories";
   private final List<TerritoryProtectionData.Territory> territories = new ArrayList<>();

   public static TerritoryProtectionData get(ServerLevel level) {
      return (TerritoryProtectionData)level.getDataStorage().computeIfAbsent(TerritoryProtectionData::load, TerritoryProtectionData::new, "ancientwarfare_territories");
   }

   public void register(BlockPos flagPos, BlockPos min, BlockPos max) {
      TerritoryProtectionData.Territory replacement = new TerritoryProtectionData.Territory(flagPos.immutable(), min.immutable(), max.immutable(), false, null, "");

      for (int i = 0; i < this.territories.size(); i++) {
         if (this.territories.get(i).flagPos().equals(flagPos)) {
            TerritoryProtectionData.Territory old = this.territories.get(i);
            this.territories
               .set(
                  i,
                  new TerritoryProtectionData.Territory(
                     replacement.flagPos(), replacement.min(), replacement.max(), old.claimed(), old.owner(), old.ownerName()
                  )
               );
            this.setDirty();
            return;
         }
      }

      this.territories.add(replacement);
      this.setDirty();
   }

   public boolean isProtected(BlockPos pos) {
      return this.territories.stream().anyMatch(territory -> !territory.claimed() && territory.contains(pos));
   }

   public boolean shareUnclaimedTerritory(BlockPos first, BlockPos second) {
      return this.territories.stream().anyMatch(territory -> !territory.claimed() && territory.contains(first) && territory.contains(second));
   }

   public UUID claimedOwner(BlockPos first, BlockPos second) {
      UUID owner = null;
      boolean covers = false;

      for (TerritoryProtectionData.Territory territory : this.territories) {
         if (territory.claimed() && territory.intersects(first, second)) {
            if (territory.owner() == null || owner != null && !owner.equals(territory.owner())) {
               return null;
            }

            owner = territory.owner();
            covers |= territory.contains(first) && territory.contains(second);
         }
      }

      return covers ? owner : null;
   }

   public boolean hasClaimedTerritory(BlockPos first, BlockPos second) {
      return this.territories.stream().anyMatch(territory -> territory.claimed() && territory.intersects(first, second));
   }

   public void claim(BlockPos flagPos, UUID owner, String ownerName) {
      TerritoryProtectionData.Territory selected = this.territories.stream().filter(value -> value.flagPos().equals(flagPos)).findFirst().orElse(null);
      if (selected != null) {
         for (int i = 0; i < this.territories.size(); i++) {
            TerritoryProtectionData.Territory territory = this.territories.get(i);
            if (territory.sameBounds(selected)) {
               this.territories.set(i, new TerritoryProtectionData.Territory(territory.flagPos(), territory.min(), territory.max(), true, owner, ownerName));
            }
         }

         this.setDirty();
      }
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();

      for (TerritoryProtectionData.Territory territory : this.territories) {
         CompoundTag value = new CompoundTag();
         value.putLong("Flag", territory.flagPos().asLong());
         value.putLong("Min", territory.min().asLong());
         value.putLong("Max", territory.max().asLong());
         value.putBoolean("Claimed", territory.claimed());
         if (territory.owner() != null) {
            value.putUUID("Owner", territory.owner());
            value.putString("OwnerName", territory.ownerName());
         }

         list.add(value);
      }

      tag.put("Territories", list);
      return tag;
   }

   private static TerritoryProtectionData load(CompoundTag tag) {
      TerritoryProtectionData data = new TerritoryProtectionData();

      for (Tag raw : tag.getList("Territories", 10)) {
         CompoundTag value = (CompoundTag)raw;
         data.territories
            .add(
               new TerritoryProtectionData.Territory(
                  BlockPos.of(value.getLong("Flag")),
                  BlockPos.of(value.getLong("Min")),
                  BlockPos.of(value.getLong("Max")),
                  value.getBoolean("Claimed"),
                  value.hasUUID("Owner") ? value.getUUID("Owner") : null,
                  value.getString("OwnerName")
               )
            );
      }

      return data;
   }

   public static record Territory(BlockPos flagPos, BlockPos min, BlockPos max, boolean claimed, UUID owner, String ownerName) {
      public boolean contains(BlockPos pos) {
         return pos.getX() >= this.min.getX()
            && pos.getX() <= this.max.getX()
            && pos.getY() >= this.min.getY()
            && pos.getY() <= this.max.getY()
            && pos.getZ() >= this.min.getZ()
            && pos.getZ() <= this.max.getZ();
      }

      public boolean sameBounds(TerritoryProtectionData.Territory other) {
         return this.min.equals(other.min) && this.max.equals(other.max);
      }

      public boolean intersects(BlockPos first, BlockPos second) {
         return this.min.getX() <= Math.max(first.getX(), second.getX())
            && this.max.getX() >= Math.min(first.getX(), second.getX())
            && this.min.getY() <= Math.max(first.getY(), second.getY())
            && this.max.getY() >= Math.min(first.getY(), second.getY())
            && this.min.getZ() <= Math.max(first.getZ(), second.getZ())
            && this.max.getZ() >= Math.min(first.getZ(), second.getZ());
      }
   }
}
