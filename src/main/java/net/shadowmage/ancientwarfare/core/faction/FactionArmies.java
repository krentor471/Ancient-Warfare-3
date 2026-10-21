package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
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

      int taken = FactionAI.takeLosses(data, attacker, Math.min(MAX_ARMY, force));
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
         goHome(data, army, army.size());
         return;
      }

      if (!level.hasChunkAt(target) || !level.hasNearbyAlivePlayer(target.getX(), target.getY(), target.getZ(), ACTIVE_RANGE)) {
         if (!army.isRaid() && now - army.ready() > WAIT_TICKS) {
            goHome(data, army, army.size());
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
      FactionAI.returnSoldiers(data, army.faction(), army.size() - spawned.size());
      data.setDirty();
      Component message = army.isRaid()
         ? Component.translatable("news.ancientwarfare.raid_arrived", FactionNews.factionName(army.faction()), target.getX(), target.getZ())
         : Component.translatable(
            "news.ancientwarfare.assault", FactionNews.factionName(army.faction()), FactionNews.factionName(army.defender()), target.getX(), target.getZ()
         );
      if (army.isRaid()) {
         FactionNews.tellPlayer(level, army.villageOwner(), message);
      } else {
         tellNear(level, target, message);
      }
   }

   private static void fight(ServerLevel level, FactionWorldData data, FactionWorldData.Army army) {
      long now = level.getGameTime();
      BlockPos target = army.target();
      if (!level.hasChunkAt(target)) {
         if (now - army.started > FIGHT_TICKS) {
            goHome(data, army, army.alive());
         }

         return;
      }

      List<FactionNpcEntity> members = members(level, army);
      if (members.isEmpty()) {
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
         goHome(data, army, members.size());
         if (army.isRaid()) {
            FactionNews.tellPlayer(level, army.villageOwner(), Component.translatable("news.ancientwarfare.raid_retreat", FactionNews.factionName(army.faction())));
         }

         return;
      }

      march(level, members, target);
      if (army.isRaid()) {
         checkVillageTaken(level, data, army, members);
      } else {
         checkSettlementTaken(level, data, army, members);
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
      if (inside > 0 && FactionDiplomacy.isOutlaw(army.faction())) {
         // Bandits plunder and make off with the loot.
         members.forEach(FactionNpcEntity::discard);
         goHome(data, army, members.size());
         FactionBandits.plunder(level, data, army.faction(), settlement, 3);
      } else if (inside > 0) {
         disband(members, army);
         data.removeArmy(army);
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

         for (int i = 0; i < army.size() * 3 && spawned.size() < army.size(); i++) {
            int x = cx + random.nextInt(9) - 4;
            int z = cz + random.nextInt(9) - 4;
            if (!level.hasChunkAt(new BlockPos(x, target.getY(), z))) {
               continue;
            }

            FactionNpcEntity npc = FactionSimulation.spawnSoldier(level, army.faction(), x, z);
            if (npc != null) {
               npc.addTag(army.tag());
               npc.setPersistenceRequired();
               spawned.add(npc);
            }
         }
      }

      return spawned;
   }

   private static List<FactionNpcEntity> members(ServerLevel level, FactionWorldData.Army army) {
      AABB area = new AABB(army.target()).inflate(SPAWN_DISTANCE * 4, 64.0, SPAWN_DISTANCE * 4);
      return level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getTags().contains(army.tag()));
   }

   /** The army's soldiers stay where they are as the new garrison. */
   private static void disband(List<FactionNpcEntity> members, FactionWorldData.Army army) {
      members.forEach(npc -> npc.removeTag(army.tag()));
   }

   private static void goHome(FactionWorldData data, FactionWorldData.Army army, int soldiers) {
      data.removeArmy(army);
      FactionAI.returnSoldiers(data, army.faction(), soldiers);
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
