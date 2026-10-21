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
import net.minecraft.nbt.LongArrayTag;
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
   private final List<Campaign> campaigns = new ArrayList<>();
   /** Player villages by town hall position, with their owner. */
   private final Map<BlockPos, UUID> villages = new HashMap<>();
   private final Map<BlockPos, Long> lastRaid = new HashMap<>();
   private int nextSettlementId = 1;
   private int nextArmyId = 1;
   private final List<Route> routes = new ArrayList<>();
   /** The b end of a route to a player's village, and that village's id as an endpoint. */
   public static final int PLAYER_VILLAGE = -1;
   public static final String PLAYER_VILLAGE_KIND = "playervillage";
   private static final int VILLAGE_HALF_SIZE = 24;
   /** What a player's village counts as in trade income; its villagers are not counted. */
   private static final int PLAYER_VILLAGE_POPULATION = 20;
   private int nextRouteId = 1;
   private long lastSimulatedDay = -1L;
   /** Faction days that passed before the clock started: the history simulated when the world began. */
   private long dayOffset;
   /** Settlements of new worlds stand this much further apart than at first. */
   public static final int NEW_REGION_SCALE = 130;
   private boolean historyDone;
   /** The world's news, oldest first, as text components in JSON; see FactionNews. */
   private final List<ChronicleEntry> chronicle = new ArrayList<>();
   public static final int CHRONICLE_SIZE = 400;
   /** How far apart provinces, realms and natural structures are, in percent; worlds from before this was kept use 100. */
   private int regionScale = NEW_REGION_SCALE;
   /** Half the width of the area an unbuilt settlement is taken to cover until it is built. */
   private static final int UNBUILT_HALF_SIZE = 32;

   public static FactionWorldData get(ServerLevel level) {
      FactionWorldData data = level.getServer().overworld().getDataStorage().computeIfAbsent(FactionWorldData::load, FactionWorldData::new, DATA_NAME);
      FactionRegions.setScale(data.regionScale);
      return data;
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

   private String newName(String faction, int id) {
      return FactionNames.pick(faction, id, this.settlements.stream().map(Settlement::name).toList());
   }

   public List<ChronicleEntry> chronicle() {
      return Collections.unmodifiableList(this.chronicle);
   }

   public void addChronicle(long day, String json) {
      this.chronicle.add(new ChronicleEntry(day, json));
      if (this.chronicle.size() > CHRONICLE_SIZE) {
         this.chronicle.remove(0);
      }

      this.setDirty();
   }

   public List<Settlement> settlements() {
      return Collections.unmodifiableList(this.settlements);
   }

   public Settlement addSettlement(
      String faction, String kind, String template, BlockPos min, BlockPos max, long province, long gameTime, int garrison, int population
   ) {
      Settlement settlement = new Settlement(this.nextSettlementId++, faction, kind, template, min, max, province, gameTime);
      settlement.name = this.newName(faction, settlement.id());
      settlement.garrison = garrison;
      settlement.population = population;
      this.settlements.add(settlement);
      this.setDirty();
      return settlement;
   }

   /**
    * A settlement that exists on paper only: it lives, grows and fights in the daily simulation, and
    * is built once a player comes near enough for it to matter, at a site found then near x, z.
    */
   public Settlement addUnbuiltSettlement(String faction, String kind, int x, int y, int z, long province, long gameTime, int garrison, int population) {
      Settlement settlement = this.addSettlement(
         faction,
         kind,
         "",
         new BlockPos(x - UNBUILT_HALF_SIZE, y, z - UNBUILT_HALF_SIZE),
         new BlockPos(x + UNBUILT_HALF_SIZE, y, z + UNBUILT_HALF_SIZE),
         province,
         gameTime,
         garrison,
         population
      );
      settlement.built = false;
      return settlement;
   }

   public long dayOffset() {
      return this.dayOffset;
   }

   public void setDayOffset(long dayOffset) {
      this.dayOffset = dayOffset;
      this.setDirty();
   }

   public boolean historyDone() {
      return this.historyDone;
   }

   public void setHistoryDone() {
      this.historyDone = true;
      this.setDirty();
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

   public List<Campaign> campaigns() {
      return Collections.unmodifiableList(this.campaigns);
   }

   public Campaign addCampaign(String faction, String defender, int targetSettlement, double x, double z, int size, long since) {
      Campaign campaign = new Campaign(this.nextArmyId++, faction, defender, targetSettlement, x, z, size, since);
      this.campaigns.add(campaign);
      this.setDirty();
      return campaign;
   }

   public void removeCampaign(Campaign campaign) {
      this.campaigns.remove(campaign);
      this.setDirty();
   }

   public List<Route> routes() {
      return Collections.unmodifiableList(this.routes);
   }

   public Route route(int a, int b) {
      for (Route route : this.routes) {
         if (route.a == a && route.b == b || route.a == b && route.b == a) {
            return route;
         }
      }

      return null;
   }

   public Route addRoute(int a, int b) {
      Route route = new Route(this.nextRouteId++, Math.min(a, b), Math.max(a, b));
      this.routes.add(route);
      this.setDirty();
      return route;
   }

   /** A route from a faction settlement to a player's village, under the player's trade agreement. */
   public Route addPlayerRoute(int settlement, BlockPos village, UUID owner) {
      Route route = new Route(this.nextRouteId++, settlement, PLAYER_VILLAGE);
      route.village = village.immutable();
      route.owner = owner;
      this.routes.add(route);
      this.setDirty();
      return route;
   }

   public Route playerRoute(int settlement, BlockPos village) {
      for (Route route : this.routes) {
         if (route.a == settlement && village.equals(route.village)) {
            return route;
         }
      }

      return null;
   }

   /**
    * One end of a route as a settlement; a player's village stands in as a settlement of the faction
    * trading with it, so roads, posts and caravans treat both ends alike. Null when that end is gone.
    */
   public Settlement endpoint(Route route, boolean first) {
      if (first || route.village == null) {
         return this.settlement(first ? route.a : route.b);
      }

      Settlement partner = this.settlement(route.a);
      if (partner == null || !route.owner.equals(this.villages.get(route.village))) {
         return null;
      }

      BlockPos v = route.village;
      Settlement village = new Settlement(
         PLAYER_VILLAGE,
         partner.faction(),
         PLAYER_VILLAGE_KIND,
         "",
         new BlockPos(v.getX() - VILLAGE_HALF_SIZE, v.getY(), v.getZ() - VILLAGE_HALF_SIZE),
         new BlockPos(v.getX() + VILLAGE_HALF_SIZE, v.getY(), v.getZ() + VILLAGE_HALF_SIZE),
         0L,
         0L
      );
      village.population = PLAYER_VILLAGE_POPULATION;
      return village;
   }

   public void removeRoute(Route route) {
      this.routes.remove(route);
      this.setDirty();
   }

   public Settlement settlement(int id) {
      for (Settlement settlement : this.settlements) {
         if (settlement.id == id) {
            return settlement;
         }
      }

      return null;
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
         entry.putBoolean("Built", settlement.built);
         entry.putString("Name", settlement.name);
         list.add(entry);
      }

      tag.put("Settlements", list);
      tag.putInt("NextSettlementId", this.nextSettlementId);
      CompoundTag gold = new CompoundTag();
      this.treasuries.forEach(gold::putInt);
      tag.put("Treasuries", gold);
      tag.putLong("LastSimulatedDay", this.lastSimulatedDay);
      tag.putLong("DayOffset", this.dayOffset);
      tag.putBoolean("HistoryDone", this.historyDone);
      ListTag chronicleList = new ListTag();

      for (ChronicleEntry entry : this.chronicle) {
         CompoundTag line = new CompoundTag();
         line.putLong("Day", entry.day());
         line.putString("Text", entry.json());
         chronicleList.add(line);
      }

      tag.put("Chronicle", chronicleList);
      tag.putInt("RegionScale", this.regionScale);
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
         entry.putBoolean("Raid", army.raid);
         entry.putInt("Reserve", army.reserve);
         entry.putInt("CampX", army.campX);
         entry.putInt("CampZ", army.campZ);
         entry.putBoolean("Engines", army.engines);
         armyList.add(entry);
      }

      tag.put("Armies", armyList);
      ListTag campaignList = new ListTag();

      for (Campaign campaign : this.campaigns) {
         CompoundTag entry = new CompoundTag();
         entry.putInt("Id", campaign.id);
         entry.putString("Faction", campaign.faction);
         entry.putString("Defender", campaign.defender);
         entry.putInt("TargetSettlement", campaign.targetSettlement);
         entry.putDouble("X", campaign.x);
         entry.putDouble("Z", campaign.z);
         entry.putInt("Size", campaign.size);
         entry.putLong("Since", campaign.since);
         entry.putBoolean("Raid", campaign.raid);
         campaignList.add(entry);
      }

      tag.put("Campaigns", campaignList);
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
      ListTag routeList = new ListTag();

      for (Route route : this.routes) {
         CompoundTag entry = new CompoundTag();
         entry.putInt("Id", route.id);
         entry.putInt("A", route.a);
         entry.putInt("B", route.b);
         entry.putBoolean("Blocked", route.blocked);
         entry.putInt("Road", route.road.ordinal());
         entry.putInt("CursorX", route.cursorX);
         entry.putInt("CursorZ", route.cursorZ);
         entry.putInt("Steps", route.steps);
         entry.putInt("WaterRun", route.waterRun);
         entry.put("Points", new LongArrayTag(route.points));
         entry.putBoolean("Posts", route.posts);
         entry.putInt("PostsBuilt", route.postsBuilt);
         entry.putInt("Income", route.income);
         if (route.village != null) {
            entry.putLong("Village", route.village.asLong());
            entry.putUUID("Owner", route.owner);
         }

         routeList.add(entry);
      }

      tag.put("Routes", routeList);
      tag.putInt("NextRouteId", this.nextRouteId);
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
         settlement.built = !entry.contains("Built") || entry.getBoolean("Built");
         settlement.name = entry.getString("Name");
         data.settlements.add(settlement);
      }

      for (Settlement settlement : data.settlements) {
         // Worlds from before names: every settlement gets one in the style of its founders.
         if (settlement.name.isEmpty()) {
            settlement.name = data.newName(settlement.capturedFrom.isEmpty() ? settlement.faction() : settlement.capturedFrom, settlement.id());
         }
      }

      data.nextSettlementId = Math.max(tag.getInt("NextSettlementId"), data.settlements.size() + 1);
      CompoundTag gold = tag.getCompound("Treasuries");
      gold.getAllKeys().forEach(faction -> data.treasuries.put(faction, gold.getInt(faction)));
      data.lastSimulatedDay = tag.contains("LastSimulatedDay") ? tag.getLong("LastSimulatedDay") : -1L;
      data.dayOffset = tag.getLong("DayOffset");
      data.historyDone = tag.getBoolean("HistoryDone");

      for (Tag element : tag.getList("Chronicle", Tag.TAG_COMPOUND)) {
         CompoundTag line = (CompoundTag)element;
         data.chronicle.add(new ChronicleEntry(line.getLong("Day"), line.getString("Text")));
      }

      data.regionScale = tag.contains("RegionScale") ? tag.getInt("RegionScale") : 100;

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
         army.raid = entry.getBoolean("Raid");
         army.reserve = entry.getInt("Reserve");
         army.campX = entry.getInt("CampX");
         army.campZ = entry.getInt("CampZ");
         army.engines = entry.getBoolean("Engines");
         data.armies.add(army);
         data.nextArmyId = Math.max(data.nextArmyId, army.id + 1);
      }

      for (Tag element : tag.getList("Campaigns", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Campaign campaign = new Campaign(
            entry.getInt("Id"),
            entry.getString("Faction"),
            entry.getString("Defender"),
            entry.getInt("TargetSettlement"),
            entry.getDouble("X"),
            entry.getDouble("Z"),
            entry.getInt("Size"),
            entry.getLong("Since")
         );
         campaign.raid = entry.getBoolean("Raid");
         data.campaigns.add(campaign);
         data.nextArmyId = Math.max(data.nextArmyId, campaign.id + 1);
      }

      data.nextArmyId = Math.max(data.nextArmyId, tag.getInt("NextArmyId"));

      for (Tag element : tag.getList("Routes", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Route route = new Route(entry.getInt("Id"), entry.getInt("A"), entry.getInt("B"));
         route.blocked = entry.getBoolean("Blocked");
         route.road = RoadState.values()[Math.floorMod(entry.getInt("Road"), RoadState.values().length)];
         route.cursorX = entry.getInt("CursorX");
         route.cursorZ = entry.getInt("CursorZ");
         route.steps = entry.getInt("Steps");
         route.waterRun = entry.getInt("WaterRun");

         for (long point : entry.getLongArray("Points")) {
            route.points.add(point);
         }

         route.posts = entry.getBoolean("Posts");
         route.postsBuilt = entry.getInt("PostsBuilt");
         route.income = entry.getInt("Income");
         if (entry.contains("Village") && entry.hasUUID("Owner")) {
            route.village = BlockPos.of(entry.getLong("Village"));
            route.owner = entry.getUUID("Owner");
         } else if (route.b == PLAYER_VILLAGE) {
            continue;
         }

         data.routes.add(route);
         data.nextRouteId = Math.max(data.nextRouteId, route.id + 1);
      }

      data.nextRouteId = Math.max(data.nextRouteId, tag.getInt("NextRouteId"));

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

   public enum RoadState {
      NONE,
      BUILDING,
      DONE,
      FAILED
   }

   /**
    * A trade route between two settlements (by id, a below b). It pays both treasuries daily unless
    * blocked by war. Its road is laid in stages from a's side; points holds the road's course, one
    * packed x/z position every few blocks, for posts and caravans to follow.
    */
   public static final class Route {
      private final int id;
      private final int a;
      private final int b;
      boolean blocked;
      RoadState road = RoadState.NONE;
      int cursorX;
      int cursorZ;
      int steps;
      int waterRun;
      final List<Long> points = new ArrayList<>();
      boolean posts;
      int postsBuilt;
      /** Gold the route brought in on its last day. */
      int income;
      /** For a route to a player's village: its town hall and owner. */
      BlockPos village;
      UUID owner;

      Route(int id, int a, int b) {
         this.id = id;
         this.a = a;
         this.b = b;
      }

      public int id() {
         return this.id;
      }

      public int a() {
         return this.a;
      }

      public int b() {
         return this.b;
      }

      public boolean blocked() {
         return this.blocked;
      }

      public RoadState road() {
         return this.road;
      }

      public List<Long> points() {
         return Collections.unmodifiableList(this.points);
      }

      public boolean posts() {
         return this.posts;
      }

      public int income() {
         return this.income;
      }

      public BlockPos village() {
         return this.village;
      }

      public UUID owner() {
         return this.owner;
      }

      /** Starts the road over from its first end, after it failed; what was paved stays. */
      public void resetRoad() {
         if (this.road == RoadState.FAILED) {
            this.road = RoadState.NONE;
            this.steps = 0;
            this.waterRun = 0;
            this.points.clear();
         }
      }

      public int other(int settlement) {
         return settlement == this.a ? this.b : this.a;
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
      /** A raid plunders the target and goes home instead of taking it. */
      boolean raid;
      /** Soldiers waiting off the field to come on in the next wave. */
      int reserve;
      /** Where the army forms up before the storm; its engines stand behind it. */
      int campX;
      int campZ;
      /** Whether its siege engines have been brought up. */
      boolean engines;

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

      public boolean raid() {
         return this.raid;
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

      public int reserve() {
         return this.reserve;
      }

      public String tag() {
         return FactionArmies.TAG_PREFIX + this.id;
      }
   }

   /**
    * An army on the march toward an enemy settlement, a point moving across the map. Its soldiers
    * are a number; while a player is near, they walk as real NPCs tagged with its id.
    */
   public static final class Campaign {
      private final int id;
      private final String faction;
      private final String defender;
      private final int targetSettlement;
      double x;
      double z;
      int size;
      private final long since;
      /** Real soldiers out walking now; not saved, they are discarded with the world. */
      int walking;
      /** A raid plunders the target and goes home instead of taking it. */
      boolean raid;

      Campaign(int id, String faction, String defender, int targetSettlement, double x, double z, int size, long since) {
         this.id = id;
         this.faction = faction;
         this.defender = defender;
         this.targetSettlement = targetSettlement;
         this.x = x;
         this.z = z;
         this.size = size;
         this.since = since;
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

      public double x() {
         return this.x;
      }

      public double z() {
         return this.z;
      }

      public int size() {
         return this.size;
      }

      public boolean raid() {
         return this.raid;
      }

      public long since() {
         return this.since;
      }

      public String tag() {
         return FactionCampaigns.TAG_PREFIX + this.id;
      }
   }

   /** A village (or border fort) a faction founded on paper; it is built once no player is near enough to watch. */
   public static record PlannedSettlement(String faction, int x, int z, long province, long day, boolean fort) {
   }

   /**
    * A faction's settlement. Its people and garrison are abstract numbers the daily simulation grows;
    * the NPCs standing there are spawned to match the garrison while a player is near.
    */
   public static record ChronicleEntry(long day, String json) {
   }

   public static final class Settlement {
      private final int id;
      /** See FactionNames; empty for player villages. */
      String name = "";
      private String faction;
      private final String kind;
      private String template;
      private BlockPos min;
      private BlockPos max;
      private final long province;
      private final long founded;
      int population;
      int garrison;
      int tier;
      /** Set once the template's own spawners have fired, so respawns do not double them. */
      boolean manned;
      /** The faction this settlement was taken from; its NPCs found here when the land loads are gone. */
      String capturedFrom = "";
      /** False while the settlement exists on paper only; see addUnbuiltSettlement. */
      boolean built = true;
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

      public String name() {
         return this.name;
      }

      /** The settlement's name, or its number when it has none. */
      public net.minecraft.network.chat.MutableComponent nameComponent() {
         return this.name.isEmpty() ? net.minecraft.network.chat.Component.literal("#" + this.id) : FactionNames.component(this.name);
      }

      public String template() {
         return this.template;
      }

      public BlockPos min() {
         return this.min;
      }

      public boolean built() {
         return this.built;
      }

      /** The settlement now stands: its buildings and the area they cover. */
      public void place(String template, BlockPos min, BlockPos max) {
         this.template = template;
         this.min = min;
         this.max = max;
         this.built = true;
         this.manned = false;
         this.capturedFrom = "";
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
