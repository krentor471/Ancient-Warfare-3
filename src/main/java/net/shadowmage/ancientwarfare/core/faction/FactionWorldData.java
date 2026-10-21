package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
   private final Map<String, Integer> treasuries = new HashMap<>();
   private final List<PlannedSettlement> plannedSettlements = new ArrayList<>();
   private final Map<String, Long> lastFounding = new HashMap<>();
   private final List<Army> armies = new ArrayList<>();
   /** Player villages by town hall position, with their owner. */
   private final Map<BlockPos, UUID> villages = new HashMap<>();
   private final Map<BlockPos, Long> lastRaid = new HashMap<>();
   private int nextSettlementId = 1;
   private int nextArmyId = 1;
   private long lastSimulatedDay = -1L;

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

   public Map<Long, String> provinceOwners() {
      return Collections.unmodifiableMap(this.provinceOwners);
   }

   /** Provinces decided so far that faction holds. */
   public List<Long> provincesOf(String faction) {
      List<Long> provinces = new ArrayList<>();
      this.provinceOwners.forEach((province, owner) -> {
         if (owner.equals(faction)) {
            provinces.add(province);
         }
      });
      return provinces;
   }

   public List<PlannedSettlement> plannedSettlements() {
      return this.plannedSettlements;
   }

   public void addPlannedSettlement(PlannedSettlement plan) {
      this.plannedSettlements.add(plan);
      this.setDirty();
   }

   public long lastFounding(String faction) {
      return this.lastFounding.getOrDefault(faction, Long.MIN_VALUE / 2);
   }

   public void setLastFounding(String faction, long day) {
      this.lastFounding.put(faction, day);
      this.setDirty();
   }

   public List<Settlement> settlements() {
      return Collections.unmodifiableList(this.settlements);
   }

   public Settlement addSettlement(
      String faction, String kind, String template, BlockPos min, BlockPos max, long province, long gameTime, int garrison, int population
   ) {
      Settlement settlement = new Settlement(this.nextSettlementId++, faction, kind, template, min, max, province, gameTime);
      settlement.garrison = garrison;
      settlement.population = population;
      this.settlements.add(settlement);
      this.setDirty();
      return settlement;
   }

   public void removeSettlement(Settlement settlement) {
      this.settlements.remove(settlement);
      this.setDirty();
   }

   public List<Army> armies() {
      return Collections.unmodifiableList(this.armies);
   }

   public Army addArmy(String faction, String defender, int targetSettlement, UUID villageOwner, BlockPos target, int size, long ready) {
      Army army = new Army(this.nextArmyId++, faction, defender, targetSettlement, villageOwner, target, size, ready);
      this.armies.add(army);
      this.setDirty();
      return army;
   }

   public Army army(int id) {
      for (Army army : this.armies) {
         if (army.id == id) {
            return army;
         }
      }

      return null;
   }

   public void removeArmy(Army army) {
      this.armies.remove(army);
      this.setDirty();
   }

   public Map<BlockPos, UUID> villages() {
      return Collections.unmodifiableMap(this.villages);
   }

   public void setVillage(BlockPos pos, UUID owner) {
      if (owner == null ? this.villages.remove(pos) != null : !owner.equals(this.villages.put(pos.immutable(), owner))) {
         this.setDirty();
      }
   }

   public long lastRaid(BlockPos village) {
      return this.lastRaid.getOrDefault(village, Long.MIN_VALUE / 2);
   }

   public void setLastRaid(BlockPos village, long day) {
      this.lastRaid.put(village.immutable(), day);
      this.setDirty();
   }

   public int treasury(String faction) {
      return this.treasuries.getOrDefault(faction, 0);
   }

   public void setTreasury(String faction, int gold) {
      this.treasuries.put(faction, Math.max(0, gold));
      this.setDirty();
   }

   public long lastSimulatedDay() {
      return this.lastSimulatedDay;
   }

   public void setLastSimulatedDay(long day) {
      this.lastSimulatedDay = day;
      this.setDirty();
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
         entry.putInt("Population", settlement.population);
         entry.putInt("Garrison", settlement.garrison);
         entry.putInt("Tier", settlement.tier);
         entry.putBoolean("Manned", settlement.manned);
         entry.putString("CapturedFrom", settlement.capturedFrom);
         list.add(entry);
      }

      tag.put("Settlements", list);
      tag.putInt("NextSettlementId", this.nextSettlementId);
      CompoundTag gold = new CompoundTag();
      this.treasuries.forEach(gold::putInt);
      tag.put("Treasuries", gold);
      tag.putLong("LastSimulatedDay", this.lastSimulatedDay);
      ListTag plans = new ListTag();

      for (PlannedSettlement plan : this.plannedSettlements) {
         CompoundTag entry = new CompoundTag();
         entry.putString("Faction", plan.faction());
         entry.putInt("X", plan.x());
         entry.putInt("Z", plan.z());
         entry.putLong("Province", plan.province());
         entry.putLong("Day", plan.day());
         entry.putBoolean("Fort", plan.fort());
         plans.add(entry);
      }

      tag.put("PlannedSettlements", plans);
      CompoundTag foundings = new CompoundTag();
      this.lastFounding.forEach(foundings::putLong);
      tag.put("LastFounding", foundings);
      ListTag armyList = new ListTag();

      for (Army army : this.armies) {
         CompoundTag entry = new CompoundTag();
         entry.putInt("Id", army.id);
         entry.putString("Faction", army.faction);
         entry.putString("Defender", army.defender);
         entry.putInt("TargetSettlement", army.targetSettlement);
         if (army.villageOwner != null) {
            entry.putUUID("VillageOwner", army.villageOwner);
         }

         entry.put("Target", NbtUtils.writeBlockPos(army.target));
         entry.putInt("Size", army.size);
         entry.putLong("Ready", army.ready);
         entry.putBoolean("Spawned", army.spawned);
         entry.putLong("Started", army.started);
         entry.putInt("Alive", army.alive);
         armyList.add(entry);
      }

      tag.put("Armies", armyList);
      tag.putInt("NextArmyId", this.nextArmyId);
      ListTag villageList = new ListTag();
      this.villages.forEach((pos, owner) -> {
         CompoundTag entry = new CompoundTag();
         entry.put("Pos", NbtUtils.writeBlockPos(pos));
         entry.putUUID("Owner", owner);
         entry.putLong("LastRaid", this.lastRaid(pos));
         villageList.add(entry);
      });
      tag.put("Villages", villageList);
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
         Settlement settlement = new Settlement(
            entry.getInt("Id"),
            entry.getString("Faction"),
            entry.getString("Kind"),
            entry.getString("Template"),
            NbtUtils.readBlockPos(entry.getCompound("Min")),
            NbtUtils.readBlockPos(entry.getCompound("Max")),
            entry.getLong("Province"),
            entry.getLong("Founded")
         );
         settlement.population = entry.getInt("Population");
         settlement.garrison = entry.getInt("Garrison");
         settlement.tier = entry.getInt("Tier");
         settlement.manned = entry.getBoolean("Manned");
         settlement.capturedFrom = entry.getString("CapturedFrom");
         data.settlements.add(settlement);
      }

      data.nextSettlementId = Math.max(tag.getInt("NextSettlementId"), data.settlements.size() + 1);
      CompoundTag gold = tag.getCompound("Treasuries");
      gold.getAllKeys().forEach(faction -> data.treasuries.put(faction, gold.getInt(faction)));
      data.lastSimulatedDay = tag.contains("LastSimulatedDay") ? tag.getLong("LastSimulatedDay") : -1L;

      for (Tag element : tag.getList("PlannedSettlements", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         data.plannedSettlements
            .add(new PlannedSettlement(entry.getString("Faction"), entry.getInt("X"), entry.getInt("Z"), entry.getLong("Province"), entry.getLong("Day"), entry.getBoolean("Fort")));
      }

      CompoundTag foundings = tag.getCompound("LastFounding");
      foundings.getAllKeys().forEach(faction -> data.lastFounding.put(faction, foundings.getLong(faction)));

      for (Tag element : tag.getList("Armies", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Army army = new Army(
            entry.getInt("Id"),
            entry.getString("Faction"),
            entry.getString("Defender"),
            entry.getInt("TargetSettlement"),
            entry.hasUUID("VillageOwner") ? entry.getUUID("VillageOwner") : null,
            NbtUtils.readBlockPos(entry.getCompound("Target")),
            entry.getInt("Size"),
            entry.getLong("Ready")
         );
         army.spawned = entry.getBoolean("Spawned");
         army.started = entry.getLong("Started");
         army.alive = entry.getInt("Alive");
         data.armies.add(army);
         data.nextArmyId = Math.max(data.nextArmyId, army.id + 1);
      }

      data.nextArmyId = Math.max(data.nextArmyId, tag.getInt("NextArmyId"));

      for (Tag element : tag.getList("Villages", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         if (entry.hasUUID("Owner")) {
            BlockPos pos = NbtUtils.readBlockPos(entry.getCompound("Pos"));
            data.villages.put(pos, entry.getUUID("Owner"));
            data.lastRaid.put(pos, entry.getLong("LastRaid"));
         }
      }

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

   /**
    * Soldiers a faction sent against a settlement or a player's village near a player. They are
    * real NPCs while their target is loaded, tagged with the army's id.
    */
   public static final class Army {
      private final int id;
      private final String faction;
      /** The faction defending the target settlement; "" for a player's village. */
      private final String defender;
      /** Target settlement id, or 0 for a player's village. */
      private final int targetSettlement;
      private final UUID villageOwner;
      private final BlockPos target;
      private final int size;
      /** Game time from which the army may set out. */
      private final long ready;
      boolean spawned;
      long started;
      int alive;

      Army(int id, String faction, String defender, int targetSettlement, UUID villageOwner, BlockPos target, int size, long ready) {
         this.id = id;
         this.faction = faction;
         this.defender = defender;
         this.targetSettlement = targetSettlement;
         this.villageOwner = villageOwner;
         this.target = target;
         this.size = size;
         this.ready = ready;
         this.alive = size;
      }

      public int id() {
         return this.id;
      }

      public String faction() {
         return this.faction;
      }

      public String defender() {
         return this.defender;
      }

      public int targetSettlement() {
         return this.targetSettlement;
      }

      public UUID villageOwner() {
         return this.villageOwner;
      }

      public boolean isRaid() {
         return this.villageOwner != null;
      }

      public BlockPos target() {
         return this.target;
      }

      public int size() {
         return this.size;
      }

      public long ready() {
         return this.ready;
      }

      public boolean spawned() {
         return this.spawned;
      }

      public int alive() {
         return this.alive;
      }

      public String tag() {
         return FactionArmies.TAG_PREFIX + this.id;
      }
   }

   /** A village (or border fort) a faction founded on paper; it is built once no player is near enough to watch. */
   public static record PlannedSettlement(String faction, int x, int z, long province, long day, boolean fort) {
   }

   /**
    * A faction's settlement. Its people and garrison are abstract numbers the daily simulation grows;
    * the NPCs standing there are spawned to match the garrison while a player is near.
    */
   public static final class Settlement {
      private final int id;
      private String faction;
      private final String kind;
      private final String template;
      private final BlockPos min;
      private final BlockPos max;
      private final long province;
      private final long founded;
      int population;
      int garrison;
      int tier;
      /** Set once the template's own spawners have fired, so respawns do not double them. */
      boolean manned;
      /** The faction this settlement was taken from; its NPCs found here when the land loads are gone. */
      String capturedFrom = "";
      /** Game time of the last garrison respawn; not saved. */
      long lastRespawn;

      Settlement(int id, String faction, String kind, String template, BlockPos min, BlockPos max, long province, long founded) {
         this.id = id;
         this.faction = faction;
         this.kind = kind;
         this.template = template;
         this.min = min;
         this.max = max;
         this.province = province;
         this.founded = founded;
      }

      public int id() {
         return this.id;
      }

      public String faction() {
         return this.faction;
      }

      void setFaction(String faction) {
         this.faction = faction;
      }

      public String kind() {
         return this.kind;
      }

      public String template() {
         return this.template;
      }

      public BlockPos min() {
         return this.min;
      }

      public BlockPos max() {
         return this.max;
      }

      public long province() {
         return this.province;
      }

      public long founded() {
         return this.founded;
      }

      public int population() {
         return this.population;
      }

      public int garrison() {
         return this.garrison;
      }

      public int tier() {
         return this.tier;
      }

      public boolean manned() {
         return this.manned;
      }

      public BlockPos center() {
         return new BlockPos((this.min.getX() + this.max.getX()) / 2, this.min.getY(), (this.min.getZ() + this.max.getZ()) / 2);
      }

      public double horizontalDistanceSquared(double x, double z) {
         BlockPos center = this.center();
         double dx = center.getX() - x;
         double dz = center.getZ() - z;
         return dx * dx + dz * dz;
      }

      public boolean contains(double x, double z, int margin) {
         return x >= this.min.getX() - margin && x <= this.max.getX() + 1 + margin && z >= this.min.getZ() - margin && z <= this.max.getZ() + 1 + margin;
      }
   }
}
