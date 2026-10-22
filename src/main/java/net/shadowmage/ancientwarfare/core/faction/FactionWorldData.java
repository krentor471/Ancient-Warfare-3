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
   /** Each faction's stores of wood, stone and iron; see FactionResources. */
   private final Map<String, int[]> stores = new HashMap<>();
   /** Buried treasure a rumour told of and not yet put in the ground, by packed x, z; see FactionRumours. */
   private final List<Long> treasures = new ArrayList<>();
   /** Countries players founded, by founder; see PlayerCountry. */
   private final Map<UUID, Country> countries = new HashMap<>();
   /** Quests players took from town boards, by player; see FactionQuests. */
   private final Map<UUID, List<FactionQuests.Quest>> quests = new HashMap<>();
   /** Until when each player knows the movements of each faction's armies, from a spy's report. */
   private final Map<UUID, Map<String, Long>> intel = new HashMap<>();
   /** Spies on their way; see FactionSpies. */
   private final List<Spy> spies = new ArrayList<>();
   /** Each faction's ruler: see FactionRulers. */
   private final Map<String, Ruler> rulers = new HashMap<>();
   private final List<PlannedSettlement> plannedSettlements = new ArrayList<>();
   private final Map<String, Long> lastFounding = new HashMap<>();
   private final List<Army> armies = new ArrayList<>();
   private final List<Campaign> campaigns = new ArrayList<>();
   /** Player villages by town hall position, with their owner. */
   private final Map<BlockPos, UUID> villages = new HashMap<>();
   private final Map<BlockPos, Long> lastRaid = new HashMap<>();
   /** Emeralds a village taken from a faction pays its new owner each day. */
   private final Map<BlockPos, Integer> taxes = new HashMap<>();
   /** Villages a player took from a faction: how the people there take their new lord. */
   private final Map<BlockPos, VillageState> villageStates = new HashMap<>();
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
      if (owner == null) {
         this.taxes.remove(pos);
         this.villageStates.remove(pos);
      }

      if (owner == null ? this.villages.remove(pos) != null : !owner.equals(this.villages.put(pos.immutable(), owner))) {
         this.setDirty();
      }
   }

   /** The state of a village taken from a faction, or null for any other village. */
   public VillageState villageState(BlockPos village) {
      return this.villageStates.get(village);
   }

   public void setVillageState(BlockPos village, VillageState state) {
      this.villageStates.put(village.immutable(), state);
      this.setDirty();
   }

   public int tax(BlockPos village) {
      return this.taxes.getOrDefault(village, 0);
   }

   public void setTax(BlockPos village, int emeralds) {
      if (emeralds <= 0) {
         this.taxes.remove(village);
      } else {
         this.taxes.put(village.immutable(), emeralds);
      }

      this.setDirty();
   }

   public long lastRaid(BlockPos village) {
      return this.lastRaid.getOrDefault(village, Long.MIN_VALUE / 2);
   }

   public void setLastRaid(BlockPos village, long day) {
      this.lastRaid.put(village.immutable(), day);
      this.setDirty();
   }

   public long intelUntil(UUID player, String faction) {
      return this.intel.getOrDefault(player, Map.of()).getOrDefault(faction, Long.MIN_VALUE);
   }

   public void setIntel(UUID player, String faction, long until) {
      this.intel.computeIfAbsent(player, ignored -> new HashMap<>()).put(faction, until);
      this.setDirty();
   }

   public List<Spy> spies() {
      return this.spies;
   }

   public Ruler ruler(String faction) {
      return this.rulers.get(faction);
   }

   public void setRuler(String faction, Ruler ruler) {
      this.rulers.put(faction, ruler);
      this.setDirty();
   }

   public int treasury(String faction) {
      return this.treasuries.getOrDefault(faction, 0);
   }

   public void setTreasury(String faction, int gold) {
      this.treasuries.put(faction, Math.max(0, gold));
      this.setDirty();
   }

   /** The faction's stores (wood, stone, iron), created with its starting stock; changes are written back in place. */
   public int[] stores(String faction) {
      return this.stores.computeIfAbsent(faction, ignored -> FactionResources.startingStores());
   }

   public List<Long> treasures() {
      return this.treasures;
   }

   /** The country the player founded, or null. */
   public Country country(UUID owner) {
      return this.countries.get(owner);
   }

   public Map<UUID, Country> countries() {
      return Collections.unmodifiableMap(this.countries);
   }

   /** The player's quests, taken and finished today; changes go through the list itself. */
   public List<FactionQuests.Quest> quests(UUID player) {
      return this.quests.computeIfAbsent(player, id -> new ArrayList<>());
   }

   public Map<UUID, List<FactionQuests.Quest>> allQuests() {
      return this.quests;
   }

   public Country addCountry(UUID owner, String name, BlockPos capital, long founded) {
      Country country = new Country(name, capital.immutable(), founded);
      this.countries.put(owner, country);
      this.setDirty();
      return country;
   }

   public void removeCountry(UUID owner) {
      if (this.countries.remove(owner) != null) {
         this.setDirty();
      }
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
         entry.putInt("Food", settlement.food);
         entry.putInt("Health", settlement.health);
         entry.putInt("Order", settlement.order);
         entry.putInt("Loyalty", settlement.loyalty);
         entry.putInt("Famine", settlement.famine);
         entry.putInt("Growth", settlement.growth);
         entry.putInt("Grown", settlement.grown);
         entry.putInt("Plague", settlement.plague);
         if (settlement.board != null) {
            entry.putLong("Board", settlement.board.asLong());
         }
         list.add(entry);
      }

      tag.put("Settlements", list);
      tag.putInt("NextSettlementId", this.nextSettlementId);
      CompoundTag gold = new CompoundTag();
      this.treasuries.forEach(gold::putInt);
      tag.put("Treasuries", gold);
      CompoundTag storeTag = new CompoundTag();
      this.stores.forEach(storeTag::putIntArray);
      tag.put("Stores", storeTag);
      ListTag countryList = new ListTag();
      this.countries.forEach((owner, country) -> {
         CompoundTag entry = country.save();
         entry.putUUID("Owner", owner);
         countryList.add(entry);
      });
      tag.put("Countries", countryList);
      tag.put("Treasures", new LongArrayTag(this.treasures));
      ListTag questList = new ListTag();
      this.quests.forEach((owner, taken) -> taken.forEach(quest -> {
         CompoundTag entry = quest.save();
         entry.putUUID("Owner", owner);
         questList.add(entry);
      }));
      tag.put("Quests", questList);
      CompoundTag rulerTag = new CompoundTag();
      this.rulers.forEach((faction, ruler) -> {
         CompoundTag entry = new CompoundTag();
         entry.putInt("Name", ruler.name());
         entry.putInt("Trait", ruler.trait());
         entry.putLong("Since", ruler.since());
         rulerTag.put(faction, entry);
      });
      tag.put("Rulers", rulerTag);
      CompoundTag intelTag = new CompoundTag();
      this.intel.forEach((player, known) -> {
         CompoundTag entry = new CompoundTag();
         known.forEach(entry::putLong);
         intelTag.put(player.toString(), entry);
      });
      tag.put("Intel", intelTag);
      ListTag spyList = new ListTag();
      for (Spy spy : this.spies) {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Player", spy.player());
         entry.putString("Faction", spy.faction());
         entry.putLong("Ready", spy.ready());
         spyList.add(entry);
      }

      tag.put("Spies", spyList);
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
         entry.putBoolean("Siege", army.siege);
         entry.putInt("Tribute", army.tribute);
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
         entry.putBoolean("Relief", campaign.relief);
         entry.putInt("Hunt", campaign.hunt);
         if (campaign.naval) {
            entry.putBoolean("Naval", true);
            entry.putDouble("LandX", campaign.landX);
            entry.putDouble("LandZ", campaign.landZ);
         }

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
         entry.putInt("Tax", this.tax(pos));
         VillageState state = this.villageStates.get(pos);
         if (state != null) {
            entry.put("State", state.save());
         }

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
         if (entry.contains("Health")) {
            settlement.food = entry.getInt("Food");
            settlement.health = entry.getInt("Health");
            settlement.order = entry.getInt("Order");
            settlement.loyalty = entry.getInt("Loyalty");
            settlement.famine = entry.getInt("Famine");
            settlement.plague = entry.getInt("Plague");
            settlement.growth = entry.getInt("Growth");
            settlement.grown = entry.getInt("Grown");
            settlement.board = entry.contains("Board") ? BlockPos.of(entry.getLong("Board")) : null;
         } else if (!settlement.capturedFrom.isEmpty()) {
            settlement.loyalty = 50;
         }

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
      CompoundTag storeTag = tag.getCompound("Stores");
      for (String faction : storeTag.getAllKeys()) {
         int[] saved = storeTag.getIntArray(faction);
         int[] stock = FactionResources.startingStores();
         System.arraycopy(saved, 0, stock, 0, Math.min(saved.length, stock.length));
         data.stores.put(faction, stock);
      }
      for (long treasure : tag.getLongArray("Treasures")) {
         data.treasures.add(treasure);
      }
      for (Tag element : tag.getList("Quests", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         if (entry.hasUUID("Owner")) {
            data.quests(entry.getUUID("Owner")).add(FactionQuests.Quest.load(entry));
         }
      }
      for (Tag element : tag.getList("Countries", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         if (entry.hasUUID("Owner")) {
            data.countries.put(entry.getUUID("Owner"), Country.load(entry));
         }
      }
      CompoundTag intelTag = tag.getCompound("Intel");
      for (String key : intelTag.getAllKeys()) {
         try {
            CompoundTag entry = intelTag.getCompound(key);
            Map<String, Long> known = new HashMap<>();
            entry.getAllKeys().forEach(faction -> known.put(faction, entry.getLong(faction)));
            data.intel.put(UUID.fromString(key), known);
         } catch (IllegalArgumentException ignored) {
         }
      }

      for (Tag element : tag.getList("Spies", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         if (entry.hasUUID("Player")) {
            data.spies.add(new Spy(entry.getUUID("Player"), entry.getString("Faction"), entry.getLong("Ready")));
         }
      }

      CompoundTag rulerTag = tag.getCompound("Rulers");
      for (String faction : rulerTag.getAllKeys()) {
         CompoundTag entry = rulerTag.getCompound(faction);
         data.rulers.put(faction, new Ruler(entry.getInt("Name"), entry.getInt("Trait"), entry.getLong("Since")));
      }
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
         army.siege = entry.getBoolean("Siege");
         army.tribute = entry.getInt("Tribute");
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
         campaign.relief = entry.getBoolean("Relief");
         campaign.hunt = entry.getInt("Hunt");
         campaign.naval = entry.getBoolean("Naval");
         campaign.landX = entry.getDouble("LandX");
         campaign.landZ = entry.getDouble("LandZ");
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
            if (entry.getInt("Tax") > 0) {
               data.taxes.put(pos, entry.getInt("Tax"));
            }

            if (entry.contains("State")) {
               data.villageStates.put(pos, VillageState.load(entry.getCompound("State")));
            }
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
      long ready;
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
      /** A full siege of a player's village with ranks, engines and ladders, not a raid. */
      boolean siege;
      /** Emeralds the attacker asks to call the siege off; 0 once the ultimatum is answered or expired. */
      int tribute;

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

      public boolean siege() {
         return this.siege;
      }

      public int tribute() {
         return this.tribute;
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
      boolean naval;
      double landX;
      double landZ;
      /** Marches to its own besieged settlement to join the garrison there. */
      boolean relief;
      /** The enemy army (campaign id) this one marches out to cut off; 0 for none. */
      int hunt;

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

      public boolean naval() {
         return this.naval;
      }

      public boolean relief() {
         return this.relief;
      }

      public int hunt() {
         return this.hunt;
      }

      public double landX() {
         return this.landX;
      }

      public double landZ() {
         return this.landZ;
      }

      public void sail(double landX, double landZ) {
         this.naval = true;
         this.landX = landX;
         this.landZ = landZ;
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

   /** A spy a player sent into a faction's lands; he reports back at ready. */
   public static record Spy(UUID player, String faction, long ready) {
   }

   /** A faction's ruler: a name (an index into the lang names) and a trait (a FactionRulers.Trait ordinal). */
   public static record Ruler(int name, int trait, long since) {
   }

   /** A village a player took from a faction; see FactionUnrest. */
   /** A player's country: its name, capital town hall and the tax waiting to reach the capital. */
   public static final class Country {
      public final String name;
      public BlockPos capital;
      public final long founded;
      /** Silver collected but not yet in the capital's town hall, which may be unloaded. */
      public int pending;
      /** Silver the whole country paid on the last tax day. */
      public int income;

      Country(String name, BlockPos capital, long founded) {
         this.name = name;
         this.capital = capital;
         this.founded = founded;
      }

      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putString("Name", this.name);
         tag.putLong("Capital", this.capital.asLong());
         tag.putLong("Founded", this.founded);
         tag.putInt("Pending", this.pending);
         tag.putInt("Income", this.income);
         return tag;
      }

      static Country load(CompoundTag tag) {
         Country country = new Country(tag.getString("Name"), BlockPos.of(tag.getLong("Capital")), tag.getLong("Founded"));
         country.pending = tag.getInt("Pending");
         country.income = tag.getInt("Income");
         return country;
      }
   }

   public static final class VillageState {
      public final String from;
      /** Fixed for a taken village; for one of the player's own, counted daily while it is loaded. */
      public int people;
      public int loyalty;
      public int order;
      /** 0 low, 1 usual, 2 high. */
      public int taxLevel = 1;
      /** The owner's soldiers last counted near the hall. */
      public int guards;

      public VillageState(String from, int people, int loyalty, int order) {
         this.from = from;
         this.people = people;
         this.loyalty = loyalty;
         this.order = order;
      }

      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putString("From", this.from);
         tag.putInt("People", this.people);
         tag.putInt("Loyalty", this.loyalty);
         tag.putInt("Order", this.order);
         tag.putInt("TaxLevel", this.taxLevel);
         tag.putInt("Guards", this.guards);
         return tag;
      }

      static VillageState load(CompoundTag tag) {
         VillageState state = new VillageState(tag.getString("From"), tag.getInt("People"), tag.getInt("Loyalty"), tag.getInt("Order"));
         state.taxLevel = tag.getInt("TaxLevel");
         state.guards = tag.getInt("Guards");
         return state;
      }
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
      /** Stored food in rations (one person eats one a day); below zero until first counted. */
      int food = -1;
      /** 0 to 100: falls with hunger and disease. */
      int health = 100;
      /** 0 to 100: kept up by the garrison and full stores, worn down by hunger, disease and unrest. */
      int order = 70;
      /** 0 to 100: how much the people hold to their faction; low in a settlement newly taken. */
      int loyalty = 100;
      /** Days of a bad harvest left. */
      int famine;
      /** Days of disease left. */
      int plague;
      /** Buildings the town has earned by growing and not yet put up; see FactionGrowth. */
      int growth;
      /** Buildings the town has put up by itself. */
      int grown;
      /** Where the town's quest board stands, once one was put up; see FactionQuests. */
      BlockPos board;

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

      /** The settlement now also covers the box from a to b. */
      public void extend(BlockPos a, BlockPos b) {
         this.min = new BlockPos(Math.min(this.min.getX(), Math.min(a.getX(), b.getX())), Math.min(this.min.getY(), Math.min(a.getY(), b.getY())), Math.min(this.min.getZ(), Math.min(a.getZ(), b.getZ())));
         this.max = new BlockPos(Math.max(this.max.getX(), Math.max(a.getX(), b.getX())), Math.max(this.max.getY(), Math.max(a.getY(), b.getY())), Math.max(this.max.getZ(), Math.max(a.getZ(), b.getZ())));
      }

      public int grown() {
         return this.grown;
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

      public int food() {
         return Math.max(0, this.food);
      }

      public int health() {
         return this.health;
      }

      public int order() {
         return this.order;
      }

      public int loyalty() {
         return this.loyalty;
      }

      public int famine() {
         return this.famine;
      }

      public int plague() {
         return this.plague;
      }

      public String capturedFrom() {
         return this.capturedFrom;
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
