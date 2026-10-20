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
      return (TerritoryProtectionData)level.m_8895_().m_164861_(TerritoryProtectionData::load, TerritoryProtectionData::new, "ancientwarfare_territories");
   }

   public void register(BlockPos flagPos, BlockPos min, BlockPos max) {
      TerritoryProtectionData.Territory replacement = new TerritoryProtectionData.Territory(flagPos.m_7949_(), min.m_7949_(), max.m_7949_(), false, null, "");

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
            this.m_77762_();
            return;
         }
      }

      this.territories.add(replacement);
      this.m_77762_();
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

         this.m_77762_();
      }
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag list = new ListTag();

      for (TerritoryProtectionData.Territory territory : this.territories) {
         CompoundTag value = new CompoundTag();
         value.m_128356_("Flag", territory.flagPos().m_121878_());
         value.m_128356_("Min", territory.min().m_121878_());
         value.m_128356_("Max", territory.max().m_121878_());
         value.m_128379_("Claimed", territory.claimed());
         if (territory.owner() != null) {
            value.m_128362_("Owner", territory.owner());
            value.m_128359_("OwnerName", territory.ownerName());
         }

         list.add(value);
      }

      tag.m_128365_("Territories", list);
      return tag;
   }

   private static TerritoryProtectionData load(CompoundTag tag) {
      TerritoryProtectionData data = new TerritoryProtectionData();

      for (Tag raw : tag.m_128437_("Territories", 10)) {
         CompoundTag value = (CompoundTag)raw;
         data.territories
            .add(
               new TerritoryProtectionData.Territory(
                  BlockPos.m_122022_(value.m_128454_("Flag")),
                  BlockPos.m_122022_(value.m_128454_("Min")),
                  BlockPos.m_122022_(value.m_128454_("Max")),
                  value.m_128471_("Claimed"),
                  value.m_128403_("Owner") ? value.m_128342_("Owner") : null,
                  value.m_128461_("OwnerName")
               )
            );
      }

      return data;
   }

   public static record Territory(BlockPos flagPos, BlockPos min, BlockPos max, boolean claimed, UUID owner, String ownerName) {
      public boolean contains(BlockPos pos) {
         return pos.m_123341_() >= this.min.m_123341_()
            && pos.m_123341_() <= this.max.m_123341_()
            && pos.m_123342_() >= this.min.m_123342_()
            && pos.m_123342_() <= this.max.m_123342_()
            && pos.m_123343_() >= this.min.m_123343_()
            && pos.m_123343_() <= this.max.m_123343_();
      }

      public boolean sameBounds(TerritoryProtectionData.Territory other) {
         return this.min.equals(other.min) && this.max.equals(other.max);
      }

      public boolean intersects(BlockPos first, BlockPos second) {
         return this.min.m_123341_() <= Math.max(first.m_123341_(), second.m_123341_())
            && this.max.m_123341_() >= Math.min(first.m_123341_(), second.m_123341_())
            && this.min.m_123342_() <= Math.max(first.m_123342_(), second.m_123342_())
            && this.max.m_123342_() >= Math.min(first.m_123342_(), second.m_123342_())
            && this.min.m_123343_() <= Math.max(first.m_123343_(), second.m_123343_())
            && this.max.m_123343_() >= Math.min(first.m_123343_(), second.m_123343_());
      }
   }
}
