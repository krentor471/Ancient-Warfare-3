package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide faction state: realm factions, province owners, relations and settlements. Stored with the overworld. */
public final class FactionWorldData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_world";
   private final Map<Long, String> realmFactions = new HashMap<>();
   private final Map<Long, String> provinceOwners = new HashMap<>();
   /** Relations changed from their defaults, stored under both factions. */
   private final Map<String, Map<String, FactionDiplomacy.Relation>> relations = new HashMap<>();
   private final List<Settlement> settlements = new ArrayList<>();
   private int nextSettlementId = 1;

   public static FactionWorldData get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionWorldData::load, FactionWorldData::new, DATA_NAME);
   }

   /** Faction a realm started with, or null when it was never decided. "" means a wild realm. */
   public String realmFaction(long realm) {
      return this.realmFactions.get(realm);
   }

   public void setRealmFaction(long realm, String faction) {
      this.realmFactions.put(realm, faction);
      this.setDirty();
   }

   /** Owner of a province, or null when it was never decided. "" means wild land. */
   public String provinceOwner(long province) {
      return this.provinceOwners.get(province);
   }

   public void setProvinceOwner(long province, String faction) {
      this.provinceOwners.put(province, faction);
      this.setDirty();
   }

   /** Stored relation between two factions, or null while it is still the default. */
   public FactionDiplomacy.Relation relation(String faction, String other) {
      Map<String, FactionDiplomacy.Relation> byOther = this.relations.get(faction);
      return byOther == null ? null : byOther.get(other);
   }

   public void setRelation(String faction, String other, FactionDiplomacy.Relation relation) {
      this.relations.computeIfAbsent(faction, ignored -> new HashMap<>()).put(other, relation);
      this.relations.computeIfAbsent(other, ignored -> new HashMap<>()).put(faction, relation);
      this.setDirty();
   }

   public List<Settlement> settlements() {
      return Collections.unmodifiableList(this.settlements);
   }

   public Settlement addSettlement(String faction, String kind, String template, BlockPos min, BlockPos max, long province, long gameTime) {
      Settlement settlement = new Settlement(this.nextSettlementId++, faction, kind, template, min, max, province, gameTime);
      this.settlements.add(settlement);
      this.setDirty();
      return settlement;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      tag.put("Realms", writeOwners(this.realmFactions));
      tag.put("Provinces", writeOwners(this.provinceOwners));
      ListTag relationList = new ListTag();
      this.relations.forEach((faction, byOther) -> byOther.forEach((other, relation) -> {
         if (faction.compareTo(other) < 0) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Faction", faction);
            entry.putString("Other", other);
            entry.putInt("Opinion", relation.opinion());
            entry.putString("State", relation.state().serializedName());
            entry.putLong("Since", relation.since());
            relationList.add(entry);
         }
      }));
      tag.put("Relations", relationList);
      ListTag list = new ListTag();

      for (Settlement settlement : this.settlements) {
         CompoundTag entry = new CompoundTag();
         entry.putInt("Id", settlement.id());
         entry.putString("Faction", settlement.faction());
         entry.putString("Kind", settlement.kind());
         entry.putString("Template", settlement.template());
         entry.put("Min", NbtUtils.writeBlockPos(settlement.min()));
         entry.put("Max", NbtUtils.writeBlockPos(settlement.max()));
         entry.putLong("Province", settlement.province());
         entry.putLong("Founded", settlement.founded());
         list.add(entry);
      }

      tag.put("Settlements", list);
      tag.putInt("NextSettlementId", this.nextSettlementId);
      return tag;
   }

   private static FactionWorldData load(CompoundTag tag) {
      FactionWorldData data = new FactionWorldData();

      readOwners(tag.getList("Realms", Tag.TAG_COMPOUND), data.realmFactions);
      readOwners(tag.getList("Provinces", Tag.TAG_COMPOUND), data.provinceOwners);

      for (Tag element : tag.getList("Relations", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         data.setRelation(
            entry.getString("Faction"),
            entry.getString("Other"),
            new FactionDiplomacy.Relation(entry.getInt("Opinion"), RelationState.byName(entry.getString("State")), entry.getLong("Since"))
         );
      }

      for (Tag element : tag.getList("Settlements", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         data.settlements.add(new Settlement(
            entry.getInt("Id"),
            entry.getString("Faction"),
            entry.getString("Kind"),
            entry.getString("Template"),
            NbtUtils.readBlockPos(entry.getCompound("Min")),
            NbtUtils.readBlockPos(entry.getCompound("Max")),
            entry.getLong("Province"),
            entry.getLong("Founded")
         ));
      }

      data.nextSettlementId = Math.max(tag.getInt("NextSettlementId"), data.settlements.size() + 1);
      return data;
   }

   private static ListTag writeOwners(Map<Long, String> owners) {
      ListTag list = new ListTag();
      owners.forEach((key, owner) -> {
         CompoundTag entry = new CompoundTag();
         entry.putLong("Key", key);
         entry.putString("Owner", owner);
         list.add(entry);
      });
      return list;
   }

   private static void readOwners(ListTag list, Map<Long, String> owners) {
      for (Tag element : list) {
         CompoundTag entry = (CompoundTag)element;
         owners.put(entry.getLong("Key"), entry.getString("Owner"));
      }
   }

   public static record Settlement(int id, String faction, String kind, String template, BlockPos min, BlockPos max, long province, long founded) {
      public BlockPos center() {
         return new BlockPos((this.min.getX() + this.max.getX()) / 2, this.min.getY(), (this.min.getZ() + this.max.getZ()) / 2);
      }

      public double horizontalDistanceSquared(double x, double z) {
         BlockPos center = this.center();
         double dx = center.getX() - x;
         double dz = center.getZ() - z;
         return dx * dx + dz * dz;
      }
   }
}
