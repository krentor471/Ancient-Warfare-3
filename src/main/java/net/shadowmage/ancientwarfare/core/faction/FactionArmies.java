package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionArcherEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionArcherNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionMountedArcherNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSiegeEngineerNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

/**
 * Real armies near players. A battle the AI fights where a player can see it is fought by soldiers
 * who march from the attacker's side and take the settlement if they clear it. Factions at war with
 * a player raid the player's villages (a town hall) with a warning first; raiders who hold the town
 * hall with no defenders left take the village. A player who clears a faction settlement's garrison
 * can claim it by placing a town hall there.
 */
public final class FactionArmies {
   public static final String TAG_PREFIX = "aw_army_";
   private static final int MAX_ARMY = 12;
   /** The largest assault sent straight at a settlement; one that marched there brings all its soldiers. */
   private static final int MAX_SIEGE = 100;
   /** Soldiers of a siege on the field at once; the rest wait in reserve. */
   private static final int MAX_ON_FIELD = 50;
   /** When this few are left standing, the next wave comes on. */
   private static final int WAVE_AT = 30;
   /** How long the army stands in ranks before the storm, while its engines shoot. */
   private static final long FORM_TICKS = 600L;
   private static final int RANK_WIDTH = 8;
   private static final double RANK_SPACING = 2.0;
   /** How often soldiers at the walls put up ladders: one man falls, a defender up top or a climber. */
   private static final long ESCALADE_TICKS = 300L;
   private static final int WALL_MARGIN = 6;
   private static final int MIN_CLIMBERS = 3;
   /** When each siege last put up ladders; not saved. */
   private static final Map<Integer, Long> LAST_ESCALADE = new HashMap<>();
   /** An army this large brings a catapult even against a place without walls. */
   private static final int ENGINE_ARMY = 30;
   private static final int MIN_RAID = 3;
   private static final double RAID_SHARE = 0.4;
   private static final int SPAWN_DISTANCE = 40;
   private static final int MARCH_STEP = 20;
   private static final double ACTIVE_RANGE = 128.0;
   private static final int TICK_INTERVAL = 20;
   /** An assault no player is near for this long goes home. */
   private static final long WAIT_TICKS = FactionSimulation.DAY_TICKS;
   /** An army that has not won by then retreats. */
   private static final long FIGHT_TICKS = FactionSimulation.DAY_TICKS / 2;
   /** Time between the raid warning and the raiders setting out. */
   public static final long RAID_WARNING_TICKS = 6000L;
   private static final int RAID_RANGE = 1024;
   private static final long RAID_COOLDOWN_DAYS = 3L;
   private static final double RAID_CHANCE = 0.35;
   private static final long FIRST_WAR_DAY = 4L;
   private static final int PLAYER_WAR_STANDING = -50;
   private static final double PLAYER_WAR_CHANCE = 0.2;
   private static final double VILLAGE_RADIUS = 32.0;
   private static final double HOLD_RADIUS = 8.0;
   private static final int CLAIM_STANDING = -20;

   private FactionArmies() {
   }

   /** The army an NPC marches with, or 0. */
   public static int armyId(FactionNpcEntity npc) {
      for (String tag : npc.getTags()) {
         if (tag.startsWith(TAG_PREFIX)) {
            try {
               return Integer.parseInt(tag.substring(TAG_PREFIX.length()));
            } catch (NumberFormatException ignored) {
               return 0;
            }
         }
      }

      return 0;
   }

   /** The attacker sends real soldiers against a settlement a player is near. */
   public static FactionWorldData.Army assault(ServerLevel level, FactionWorldData data, String attacker, FactionWorldData.Settlement target, int force) {
      for (FactionWorldData.Army army : data.armies()) {
         if (army.targetSettlement() == target.id()) {
            return null;
         }
      }

      int taken = FactionAI.takeLosses(data, attacker, Math.min(MAX_SIEGE, force));
      if (taken <= 0) {
         return null;
      }

      return data.addArmy(attacker, target.faction(), target.id(), null, target.center(), taken, level.getGameTime());
   }

   /** Plans a raid on a player's village; the owner is warned and the raiders set out after the delay. */
   public static FactionWorldData.Army raid(ServerLevel level, FactionWorldData data, String faction, BlockPos village, UUID owner, int size, long delay) {
      int taken = FactionAI.takeLosses(data, faction, Math.min(MAX_ARMY, size));
      if (taken <= 0) {
         return null;
      }

      data.setLastRaid(village, data.lastSimulatedDay());
      FactionNews.tellPlayer(
         level, owner, Component.translatable("news.ancientwarfare.raid_warning", FactionNews.factionName(faction), taken, village.getX(), village.getZ())
      );
      return data.addArmy(faction, "", 0, owner, village, taken, level.getGameTime() + delay);
   }

   /** Daily: factions that hate a nearby player declare war on them, and factions at war plan raids. */
   public static void simulateDay(ServerLevel level, FactionWorldData data, long day) {
      RandomSource random = level.getRandom();
      FactionStandingData standings = FactionStandingData.get(level);
      Map<String, Integer> strength = FactionAI.strengths(data);

      for (Map.Entry<BlockPos, UUID> village : new ArrayList<>(data.villages().entrySet())) {
         BlockPos pos = village.getKey();
         UUID owner = village.getValue();
         boolean raided = isTargeted(data, pos);

         for (String faction : new TreeSet<>(FactionRegistry.factionNames())) {
            if (nearestSettlement(data, faction, pos, RAID_RANGE, -1) == null) {
               continue;
            }

            RelationState treaty = standings.getTreaty(owner, faction);
            if (treaty == RelationState.NEUTRAL
               && day >= FIRST_WAR_DAY
               && standings.getStanding(owner, faction) <= PLAYER_WAR_STANDING
               && random.nextDouble() < PLAYER_WAR_CHANCE) {
               standings.setTreaty(owner, faction, RelationState.WAR);
               standings.discover(owner, faction);
               FactionNews.tellPlayer(level, owner, Component.translatable("news.ancientwarfare.war_on_you", FactionNews.factionName(faction)));
               treaty = RelationState.WAR;
            }

            int size = (int)Math.ceil(strength.getOrDefault(faction, 0) * RAID_SHARE);
            if (treaty == RelationState.WAR
               && !raided
               && size >= MIN_RAID
               && day - data.lastRaid(pos) >= RAID_COOLDOWN_DAYS
               && random.nextDouble() < RAID_CHANCE
               && raid(level, data, faction, pos, owner, size, RAID_WARNING_TICKS) != null) {
               raided = true;
            }
         }
      }
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % TICK_INTERVAL != 0L || data.armies().isEmpty()) {
         return;
      }

      for (FactionWorldData.Army army : new ArrayList<>(data.armies())) {
         if (army.spawned()) {
            fight(level, data, army);
         } else {
            setOut(level, data, army);
         }
      }
   }

   private static void setOut(ServerLevel level, FactionWorldData data, FactionWorldData.Army army) {
      long now = level.getGameTime();
      if (now < army.ready()) {
         return;
      }

      BlockPos target = army.target();
      if (!stillStands(level, data, army)) {
         goHome(level, data, army, army.size());
         return;
      }

      if (!level.hasChunkAt(target) || !level.hasNearbyAlivePlayer(target.getX(), target.getY(), target.getZ(), ACTIVE_RANGE)) {
         if (!army.isRaid() && now - army.ready() > WAIT_TICKS) {
            goHome(level, data, army, army.size());
         }

         return;
      }

      List<FactionNpcEntity> spawned = spawnArmy(level, data, army);
      if (spawned.isEmpty()) {
         return;
      }

      army.spawned = true;
      army.started = now;
      army.alive = spawned.size();
      // The rest wait in reserve and come on in waves as the first fall.
      army.reserve = army.size() - spawned.size();
      data.setDirty();
      Component message = army.isRaid()
         ? Component.translatable("news.ancientwarfare.raid_arrived", FactionNews.factionName(army.faction()), target.getX(), target.getZ())
         : Component.translatable(
            "news.ancientwarfare.assault",
            FactionNews.factionName(army.faction()),
            FactionNews.factionName(army.defender()),
            target.getX(),
            target.getZ(),
            FactionNews.named(settlementById(data, army.targetSettlement()))
         );
      if (army.isRaid()) {
         FactionNews.tellPlayer(level, army.villageOwner(), message);
      } else {
         tellNear(level, target, message);
         FactionWorldData.Settlement settlement = settlementById(data, army.targetSettlement());
         if (settlement != null) {
            siegeBegins(level, data, army, settlement);
         }
      }
   }

   private static void fight(ServerLevel level, FactionWorldData data, FactionWorldData.Army army) {
      long now = level.getGameTime();
      BlockPos target = army.target();
      if (!level.hasChunkAt(target)) {
         if (now - army.started > FIGHT_TICKS) {
            goHome(level, data, army, army.alive());
         }

         return;
      }

      List<FactionNpcEntity> members = members(level, army);
      List<FactionNpcEntity> soldiers = members.stream().filter(npc -> !(npc instanceof FactionSiegeEngineerNpcEntity)).toList();
      if (soldiers.size() <= WAVE_AT && army.reserve > 0) {
         // The next wave comes up from the camp.
         int wave = Math.min(army.reserve, MAX_ON_FIELD - soldiers.size());
         List<FactionNpcEntity> fresh = spawnAt(level, army, army.campX, army.campZ, wave);
         army.reserve -= fresh.size();
         members = new ArrayList<>(members);
         members.addAll(fresh);
         soldiers = new ArrayList<>(soldiers);
         soldiers.addAll(fresh);
         if (!fresh.isEmpty()) {
            tellNear(level, target, Component.translatable("news.ancientwarfare.siege_wave", FactionNews.factionName(army.faction()), fresh.size(), army.reserve));
         }
      }

      if (soldiers.isEmpty()) {
         members.forEach(FactionNpcEntity::discard);
         dismissEngines(level, army);
         data.removeArmy(army);
         if (army.isRaid()) {
            FactionNews.tellPlayer(level, army.villageOwner(), Component.translatable("news.ancientwarfare.raid_repelled", FactionNews.factionName(army.faction())));
         } else {
            tellNear(level, target, Component.translatable("news.ancientwarfare.assault_repelled", FactionNews.factionName(army.defender()), FactionNews.factionName(army.faction())));
         }

         return;
      }

      army.alive = members.size();
      data.setDirty();
      if (now - army.started > FIGHT_TICKS || !stillStands(level, data, army)) {
         members.forEach(FactionNpcEntity::discard);
         goHome(level, data, army, soldiers.size());
         if (army.isRaid()) {
            FactionNews.tellPlayer(level, army.villageOwner(), Component.translatable("news.ancientwarfare.raid_retreat", FactionNews.factionName(army.faction())));
         }

         return;
      }

      if (!army.isRaid() && now - army.started < FORM_TICKS) {
         formUp(level, army, soldiers);
      } else {
         march(level, soldiers, target);
      }

      if (army.isRaid()) {
         checkVillageTaken(level, data, army, members);
      } else {
         if (now - army.started >= FORM_TICKS) {
            escalade(level, data, army, soldiers);
         }

         checkSettlementTaken(level, data, army, soldiers);
      }
   }

   /**
    * Defenders on a tower top or a wall walk are out of a swordsman's reach, so soldiers who have
    * reached the walls climb: now and then the highest defender falls to them, or a climber falls,
    * by how many there are on each side.
    */
   private static void escalade(ServerLevel level, FactionWorldData data, FactionWorldData.Army army, List<FactionNpcEntity> soldiers) {
      FactionWorldData.Settlement settlement = settlementById(data, army.targetSettlement());
      long now = level.getGameTime();
      Long last = LAST_ESCALADE.putIfAbsent(army.id(), now);
      if (settlement == null || last == null || now - last < ESCALADE_TICKS) {
         return;
      }

      LAST_ESCALADE.put(army.id(), now);
      List<FactionNpcEntity> climbers = soldiers.stream().filter(npc -> settlement.contains(npc.getX(), npc.getZ(), WALL_MARGIN)).toList();
      List<FactionNpcEntity> defenders = FactionSimulation.liveSoldierList(level, settlement);
      if (climbers.size() < MIN_CLIMBERS || defenders.isEmpty()) {
         return;
      }

      RandomSource random = level.getRandom();
      if (random.nextInt(climbers.size() + defenders.size()) < climbers.size()) {
         defenders.stream().max(Comparator.comparingDouble(FactionNpcEntity::getY)).ifPresent(FactionNpcEntity::kill);
      } else {
         climbers.get(random.nextInt(climbers.size())).kill();
      }
   }

   private static void march(ServerLevel level, List<FactionNpcEntity> members, BlockPos target) {
      for (FactionNpcEntity npc : members) {
         if (npc.getTarget() != null || !npc.getNavigation().isDone()) {
            continue;
         }

         double dx = target.getX() + 0.5 - npc.getX();
         double dz = target.getZ() + 0.5 - npc.getZ();
         double distance = Math.sqrt(dx * dx + dz * dz);
         if (distance < 3.0) {
            continue;
         }

         double step = Math.min(distance, MARCH_STEP) / distance;
         int x = Mth.floor(npc.getX() + dx * step);
         int z = Mth.floor(npc.getZ() + dz * step);
         npc.getNavigation().moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, 1.0);
      }
   }

   private static void checkSettlementTaken(ServerLevel level, FactionWorldData data, FactionWorldData.Army army, List<FactionNpcEntity> members) {
      FactionWorldData.Settlement settlement = settlementById(data, army.targetSettlement());
      if (settlement == null || FactionSimulation.liveSoldiers(level, settlement) > 0) {
         return;
      }

      long inside = members.stream().filter(npc -> settlement.contains(npc.getX(), npc.getZ(), 4)).count();
      if (inside > 0 && army.raid) {
         // Raiders loot the place and make for home.
         members.forEach(FactionNpcEntity::discard);
         goHome(level, data, army, members.size());
         FactionRaids.plunder(level, data, army.faction(), settlement);
      } else if (inside > 0 && FactionDiplomacy.isOutlaw(army.faction())) {
         // Bandits plunder and make off with the loot.
         members.forEach(FactionNpcEntity::discard);
         goHome(level, data, army, members.size());
         FactionBandits.plunder(level, data, army.faction(), settlement, 3);
      } else if (inside > 0 && FactionDiplomacy.isOutlaw(settlement.faction())) {
         // A bandit lair is scattered, not kept.
         members.forEach(FactionNpcEntity::discard);
         goHome(level, data, army, members.size());
         FactionBandits.scatter(level, data, army.faction(), settlement);
      } else if (inside > 0) {
         disband(members, army);
         dismissEngines(level, army);
         data.removeArmy(army);
         FactionAI.returnSoldiers(data, army.faction(), army.reserve);
         FactionAI.capture(level, data, settlement, army.faction(), members.size());
      }
   }

   private static void checkVillageTaken(ServerLevel level, FactionWorldData data, FactionWorldData.Army army, List<FactionNpcEntity> members) {
      BlockPos hall = army.target();
      UUID owner = army.villageOwner();
      AABB village = new AABB(hall).inflate(VILLAGE_RADIUS, 16.0, VILLAGE_RADIUS);
      boolean defended = !level.getEntitiesOfClass(CombatNpcEntity.class, village, npc -> npc.isAlive() && npc.getOwner().map(owner::equals).orElse(false))
            .isEmpty()
         || level.getPlayers(player -> player.getUUID().equals(owner) && player.isAlive() && !player.isSpectator() && village.contains(player.position()))
            .stream()
            .anyMatch(player -> !player.isCreative());
      boolean holding = members.stream().anyMatch(npc -> npc.distanceToSqr(hall.getX() + 0.5, npc.getY(), hall.getZ() + 0.5) < HOLD_RADIUS * HOLD_RADIUS);
      if (defended || !holding) {
         return;
      }

      disband(members, army);
      data.removeArmy(army);
      data.setVillage(hall, null);
      level.destroyBlock(hall, true);
      FactionRegions.Province province = FactionRegions.provinceAt(level, hall.getX(), hall.getZ());
      FactionWorldData.Settlement settlement = data.addSettlement(
         army.faction(), "captured", "", hall.offset(-16, -4, -16), hall.offset(16, 8, 16), province.key(), level.getGameTime(), members.size(), 4
      );
      settlement.manned = true;
      FactionNews.tellPlayer(
         level, owner, Component.translatable("news.ancientwarfare.village_captured", FactionNews.factionName(army.faction()), hall.getX(), hall.getZ())
      );
   }

   /** A town hall was placed: it becomes a village, and a cleared faction settlement around it becomes the player's. */
   public static void onTownHallPlaced(ServerLevel level, BlockPos pos, UUID owner) {
      FactionWorldData data = FactionWorldData.get(level);
      data.setVillage(pos, owner);

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!settlement.contains(pos.getX(), pos.getZ(), 0)) {
            continue;
         }

         Component name = FactionNews.factionName(settlement.faction());
         if (settlement.manned() && settlement.garrison() == 0 && FactionSimulation.liveSoldiers(level, settlement) == 0) {
            data.removeSettlement(settlement);
            FactionStandingData.get(level).adjustStanding(owner, settlement.faction(), CLAIM_STANDING);
            FactionNews.tellPlayer(level, owner, Component.translatable("news.ancientwarfare.village_claimed", name));
         } else {
            FactionNews.tellPlayer(level, owner, Component.translatable("news.ancientwarfare.village_held", name, settlement.garrison()));
         }

         return;
      }
   }

   public static void onTownHallRemoved(ServerLevel level, BlockPos pos) {
      FactionWorldData.get(level).setVillage(pos, null);
   }

   private static boolean stillStands(ServerLevel level, FactionWorldData data, FactionWorldData.Army army) {
      if (army.isRaid()) {
         return data.villages().containsKey(army.target())
            && (!level.hasChunkAt(army.target()) || level.getBlockEntity(army.target()) instanceof TownHallBlockEntity);
      }

      FactionWorldData.Settlement settlement = settlementById(data, army.targetSettlement());
      return settlement != null && settlement.faction().equals(army.defender());
   }

   private static List<FactionNpcEntity> spawnArmy(ServerLevel level, FactionWorldData data, FactionWorldData.Army army) {
      BlockPos target = army.target();
      RandomSource random = level.getRandom();
      FactionWorldData.Settlement home = nearestSettlement(data, army.faction(), target, Integer.MAX_VALUE, army.targetSettlement());
      double angle = home == null
         ? random.nextDouble() * Math.PI * 2.0
         : Math.atan2(home.center().getZ() - target.getZ(), home.center().getX() - target.getX());
      List<FactionNpcEntity> spawned = new ArrayList<>();

      for (int attempt = 0; attempt < 7 && spawned.isEmpty(); attempt++) {
         double turn = (attempt + 1) / 2 * (attempt % 2 == 0 ? 1 : -1) * Math.PI / 6.0;
         int cx = target.getX() + (int)Math.round(Math.cos(angle + turn) * SPAWN_DISTANCE);
         int cz = target.getZ() + (int)Math.round(Math.sin(angle + turn) * SPAWN_DISTANCE);
         if (!level.hasChunkAt(new BlockPos(cx, target.getY(), cz))) {
            continue;
         }

         spawned = spawnAt(level, army, cx, cz, Math.min(army.size(), MAX_ON_FIELD));
         army.campX = cx;
         army.campZ = cz;
      }

      return spawned;
   }

   /** Up to count soldiers of the army, around a point. */
   private static List<FactionNpcEntity> spawnAt(ServerLevel level, FactionWorldData.Army army, int cx, int cz, int count) {
      RandomSource random = level.getRandom();
      List<FactionNpcEntity> spawned = new ArrayList<>();

      // A bigger band needs more ground to stand on.
      int spread = 4 + count / 6;
      for (int i = 0; i < count * 5 && spawned.size() < count; i++) {
         int x = cx + random.nextInt(2 * spread + 1) - spread;
         int z = cz + random.nextInt(2 * spread + 1) - spread;
         if (!level.hasChunkAt(new BlockPos(x, army.target().getY(), z))) {
            continue;
         }

         FactionNpcEntity npc = FactionSimulation.spawnSoldier(level, army.faction(), x, z);
         if (npc != null) {
            npc.addTag(army.tag());
            npc.setPersistenceRequired();
            spawned.add(npc);
         }
      }

      return spawned;
   }

   /**
    * Before the storm the soldiers draw up in ranks at the camp, facing the settlement: men with
    * blades in front, archers behind. Anyone already fighting keeps fighting.
    */
   private static void formUp(ServerLevel level, FactionWorldData.Army army, List<FactionNpcEntity> soldiers) {
      double[] facing = facing(army);
      double fx = facing[0];
      double fz = facing[1];
      List<FactionNpcEntity> ranks = new ArrayList<>(soldiers);
      ranks.sort(Comparator.comparingInt(npc -> isArcher(npc) ? 1 : 0));

      for (int i = 0; i < ranks.size(); i++) {
         FactionNpcEntity npc = ranks.get(i);
         if (npc.getTarget() != null) {
            continue;
         }

         int row = i / RANK_WIDTH;
         double column = i % RANK_WIDTH - (RANK_WIDTH - 1) / 2.0;
         double x = army.campX + 0.5 - fz * column * RANK_SPACING - fx * row * RANK_SPACING;
         double z = army.campZ + 0.5 + fx * column * RANK_SPACING - fz * row * RANK_SPACING;
         if (npc.distanceToSqr(x, npc.getY(), z) > 2.0) {
            npc.getNavigation().moveTo(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z)), z, 1.0);
         } else {
            npc.getNavigation().stop();
            npc.getLookControl().setLookAt(army.target().getX() + 0.5, npc.getEyeY(), army.target().getZ() + 0.5);
         }
      }
   }

   private static boolean isArcher(FactionNpcEntity npc) {
      return npc instanceof FactionArcherNpcEntity || npc instanceof FactionArcherEliteNpcEntity || npc instanceof FactionMountedArcherNpcEntity;
   }

   /** The unit direction from the camp toward the target. */
   private static double[] facing(FactionWorldData.Army army) {
      double dx = army.target().getX() - army.campX;
      double dz = army.target().getZ() - army.campZ;
      double length = Math.sqrt(dx * dx + dz * dz);
      return length < 1.0 ? new double[]{1.0, 0.0} : new double[]{dx / length, dz / length};
   }

   /**
    * A siege of walls brings engines: a fort a catapult and a ram for its gate, a capital a
    * trebuchet and a ram, and any other place a catapult when the army is large. Each comes with an
    * engineer, and they stand behind the ranks.
    */
   static void siegeBegins(ServerLevel level, FactionWorldData data, FactionWorldData.Army army, FactionWorldData.Settlement target) {
      if (army.engines) {
         return;
      }

      List<EntityType<? extends Entity>> engines = new ArrayList<>();
      if (FactionTowns.CAPITAL.equals(target.kind())) {
         engines.add(AWCoreEntityTypes.TREBUCHET_MOBILE.get());
         engines.add(AWCoreEntityTypes.BATTERING_RAM.get());
      } else if (FactionAI.isFort(target)) {
         engines.add(AWCoreEntityTypes.CATAPULT_MOBILE.get());
         engines.add(AWCoreEntityTypes.BATTERING_RAM.get());
      } else if (army.size() >= ENGINE_ARMY) {
         engines.add(AWCoreEntityTypes.CATAPULT_MOBILE.get());
      }

      army.engines = true;
      data.setDirty();
      if (engines.isEmpty()) {
         return;
      }

      double[] facing = facing(army);
      int depth = (Math.min(army.size(), MAX_ON_FIELD) + RANK_WIDTH - 1) / RANK_WIDTH * (int)RANK_SPACING + 6;
      int placed = 0;

      for (int i = 0; i < engines.size(); i++) {
         double side = (i - (engines.size() - 1) / 2.0) * 6.0;
         int x = Mth.floor(army.campX - facing[0] * depth - facing[1] * side);
         int z = Mth.floor(army.campZ - facing[1] * depth + facing[0] * side);
         if (placeEngine(level, army, engines.get(i), x, z)) {
            placed++;
         }
      }

      if (placed > 0) {
         tellNear(
            level,
            army.target(),
            Component.translatable(
               "news.ancientwarfare.siege_camp", FactionNews.factionName(army.faction()), FactionNews.factionName(army.defender()), placed, FactionNews.named(target)
            )
         );
      }
   }

   private static boolean placeEngine(ServerLevel level, FactionWorldData.Army army, EntityType<? extends Entity> type, int x, int z) {
      BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
      if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below()).getFluidState().isEmpty()) {
         return false;
      }

      Entity engine = type.create(level);
      FactionSiegeEngineerNpcEntity engineer = AWCoreEntityTypes.FACTION_SIEGE_ENGINEER_NPC.get().create(level);
      if (engine == null || engineer == null) {
         return false;
      }

      double[] facing = facing(army);
      float yaw = (float)(Mth.atan2(facing[1], facing[0]) * Mth.RAD_TO_DEG) - 90.0F;
      engine.moveTo(x + 0.5, pos.getY(), z + 0.5, yaw, 0.0F);
      engine.addTag(army.tag());
      level.addFreshEntity(engine);
      engineer.moveTo(x + 0.5 - facing[0] * 2, pos.getY(), z + 0.5 - facing[1] * 2, yaw, 0.0F);
      engineer.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
      engineer.setFactionName(army.faction());
      engineer.addTag(army.tag());
      engineer.setPersistenceRequired();
      level.addFreshEntity(engineer);
      return true;
   }

   private static List<FactionNpcEntity> members(ServerLevel level, FactionWorldData.Army army) {
      AABB area = new AABB(army.target()).inflate(SPAWN_DISTANCE * 4, 64.0, SPAWN_DISTANCE * 4);
      return level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getTags().contains(army.tag()));
   }

   /** The army's soldiers stay where they are as the new garrison. */
   private static void disband(List<FactionNpcEntity> members, FactionWorldData.Army army) {
      members.forEach(npc -> npc.removeTag(army.tag()));
   }

   /** The army is done: its engines are left behind and the soldiers on the field and in reserve go home. */
   private static void goHome(ServerLevel level, FactionWorldData data, FactionWorldData.Army army, int soldiers) {
      dismissEngines(level, army);
      data.removeArmy(army);
      FactionAI.returnSoldiers(data, army.faction(), soldiers + army.reserve);
   }

   private static void dismissEngines(ServerLevel level, FactionWorldData.Army army) {
      if (!army.engines) {
         return;
      }

      AABB area = new AABB(army.target()).inflate(SPAWN_DISTANCE * 4, 64.0, SPAWN_DISTANCE * 4);
      for (Entity entity : level.getEntities((Entity)null, area, entity -> entity.getTags().contains(army.tag()) && (!(entity instanceof FactionNpcEntity) || entity instanceof FactionSiegeEngineerNpcEntity))) {
         entity.ejectPassengers();
         entity.discard();
      }

      army.engines = false;
   }

   private static boolean isTargeted(FactionWorldData data, BlockPos village) {
      for (FactionWorldData.Army army : data.armies()) {
         if (army.isRaid() && army.target().equals(village)) {
            return true;
         }
      }

      return false;
   }

   private static FactionWorldData.Settlement settlementById(FactionWorldData data, int id) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.id() == id) {
            return settlement;
         }
      }

      return null;
   }

   private static FactionWorldData.Settlement nearestSettlement(FactionWorldData data, String faction, BlockPos pos, int range, int excluded) {
      FactionWorldData.Settlement best = null;
      double bestDistance = (double)range * range;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         double distance = settlement.horizontalDistanceSquared(pos.getX(), pos.getZ());
         if (settlement.faction().equals(faction) && settlement.id() != excluded && distance <= bestDistance) {
            best = settlement;
            bestDistance = distance;
         }
      }

      return best;
   }

   private static void tellNear(ServerLevel level, BlockPos pos, Component message) {
      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(pos.getX(), player.getY(), pos.getZ()) < ACTIVE_RANGE * ACTIVE_RANGE * 4) {
            player.sendSystemMessage(message);
         }
      }
   }
}
