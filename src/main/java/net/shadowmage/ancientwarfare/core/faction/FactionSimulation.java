package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;
import net.shadowmage.ancientwarfare.core.tile.AdvancedSpawnerBlockEntity;

/**
 * Faction life far from the player, one step per game day: settlements grow their people, people
 * pay taxes into the faction's treasury, the treasury recruits soldiers from the people, and full
 * settlements grow a tier. Near a player, a settlement's garrison stands as real NPCs: losses come
 * off the garrison, and missing soldiers come back one at a time, out of sight, while the
 * simulated garrison still has them.
 */
public final class FactionSimulation {
   public static final long DAY_TICKS = 24000L;
   private static final int GARRISON_CHECK_TICKS = 100;
   private static final int RESPAWN_TICKS = 600;
   private static final double ACTIVE_RANGE = 128.0;
   private static final double HIDDEN_RANGE = 24.0;
   private static final int GARRISON_MARGIN = 24;
   /** Townsfolk out in a town's streets: one for so many of its people, within these bounds. */
   private static final int PEOPLE_PER_TOWNSFOLK = 4;
   private static final int MIN_TOWNSFOLK = 4;
   private static final int MAX_TOWNSFOLK = 20;
   private static final int LOSS_MARGIN = 32;
   private static final int RECRUIT_COST = 5;
   private static final int MAX_RECRUITS_PER_DAY = 2;
   private static final int TIER_COST = 60;
   private static final int MAX_TIER = 3;
   private static final int SMALL_FACTION_SETTLEMENTS = 10;
   private static final int SMALL_FACTION_TAX_BONUS = 100;
   private static final long LEADER_RESPAWN_TICKS = DAY_TICKS;
   private static final int LEADER_SPREAD = 6;
   /** When each capital's leader was found missing; not saved. */
   private static final Map<Integer, Long> LEADER_LOST = new HashMap<>();

   private FactionSimulation() {
   }

   public static int startingPopulation(int soldiers, int civilians) {
      return Math.max(4, soldiers + civilians * 3);
   }

   /** People a settlement can hold: grows with its starting size and tier. */
   public static int capacity(FactionWorldData.Settlement settlement) {
      int base = switch (settlement.kind()) {
         case FactionTowns.CAPITAL -> 90;
         case "town" -> 60;
         case FactionTowns.VILLAGE -> 30;
         default -> 12;
      };
      return base + settlement.tier() * base / 2 + settlement.grown() * FactionGrowth.ROOM_PER_PIECE + settlement.garrison();
   }

   public static void tick(ServerLevel level) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionHistory.runIfNeeded(level, data);
      long clockDay = level.getGameTime() / DAY_TICKS;
      if (data.lastSimulatedDay() > clockDay + data.dayOffset()) {
         // Days simulated ahead of the clock (the history, the test command) count as already past.
         data.setDayOffset(data.lastSimulatedDay() - clockDay);
      }

      long day = clockDay + data.dayOffset();
      if (data.lastSimulatedDay() < 0L) {
         data.setLastSimulatedDay(day);
      } else if (day > data.lastSimulatedDay()) {
         data.setLastSimulatedDay(day);
         simulateDay(level, data);
      }

      FactionExpansion.tick(level, data);
      FactionArmies.tick(level, data);
      FactionConquest.tick(level, data);
      PlayerCountry.tick(level, data);
      FactionSpies.tick(level, data);
      FactionIntrigue.tick(level, data);
      FactionIncidents.tick(level, data);
      FactionFeasts.tick(level, data);
      FactionPastimes.tick(level, data);
      FactionTournaments.tick(level);
      FactionCampaigns.tick(level, data);
      FactionRoads.tick(level, data);
      FactionCaravans.tick(level, data);
      FactionTravellers.tick(level, data);
      FactionRumours.tick(level, data);
      FactionTownLife.tick(level, data);
      FactionGrowth.tick(level, data);
      FactionRepairs.tick(level, data);
      FreeVillages.tick(level, data);
      FactionPorts.tick(level, data);
      FactionQuests.tick(level, data);
      FactionChatter.tick(level, data);
      FactionMercenaries.tick(level, data);
      FactionEnvoys.tick(level);

      if (level.getGameTime() % GARRISON_CHECK_TICKS == 0L && !level.players().isEmpty()) {
         maintainGarrisons(level, data);
      }
   }

   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionSeasons.simulateDay(level);
      FactionLife.simulateDay(level, data);
      FactionResources.simulateDay(data);
      FactionMarkets.simulateDay(level, data);
      FactionMusters.simulateDay(level, data);
      FactionRebellions.simulateDay(level, data);
      FactionFaith.simulateDay(level, data);
      Map<String, Integer> recruited = new HashMap<>();
      Map<String, Integer> settlements = new HashMap<>();
      data.settlements().forEach(settlement -> settlements.merge(settlement.faction(), 1, Integer::sum));

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         int capacity = capacity(settlement);
         // People multiply only with food in store, in health and in order.
         boolean thriving = settlement.food() >= settlement.population && settlement.health() > 40 && settlement.order() >= 20;
         if (thriving && settlement.population < capacity) {
            settlement.population = Math.min(capacity, settlement.population + Math.max(1, settlement.population / 20));
         }

         String faction = settlement.faction();
         int gold = data.treasury(faction) + settlement.population * taxPercent(settlements.getOrDefault(faction, 1)) / 400;

         // A fort is a garrison with a few hands to keep it; anywhere else a third of the people may serve.
         int maxGarrison = FactionAI.isFort(settlement) ? FactionExpansion.FORT_GARRISON + settlement.tier * 4 : settlement.population / 3;

         for (int i = 0;
            i < MAX_RECRUITS_PER_DAY && settlement.garrison < maxGarrison && settlement.population > 1 && gold >= RECRUIT_COST && FactionResources.hasIron(data, faction);
            i++
         ) {
            FactionResources.spendIron(data, faction);
            FactionArmament.recruit(data, faction);
            settlement.garrison++;
            settlement.population--;
            gold -= RECRUIT_COST;
            recruited.merge(faction, 1, Integer::sum);
         }

         int tierCost = TIER_COST * (settlement.tier + 1);
         if (settlement.population >= capacity && settlement.tier < MAX_TIER && gold >= tierCost
            && FactionResources.has(data, faction, FactionResources.tierCost(settlement.tier))) {
            FactionResources.spend(data, faction, FactionResources.tierCost(settlement.tier));
            settlement.tier++;
            gold -= tierCost;
            FactionPorts.rankRose(level, settlement);
         }

         data.setTreasury(faction, gold);
      }

      FactionExpansion.simulateDay(level, data, data.lastSimulatedDay());
      FactionAI.simulateDay(level, data, data.lastSimulatedDay());
      FactionBorders.simulateDay(level, data);
      CountryDiplomacy.simulateDay(level, data);
      FactionBandits.simulateDay(level, data);
      FactionFleets.simulateDay(level, data);
      FactionArmies.simulateDay(level, data, data.lastSimulatedDay());
      PlayerCountry.startTaxDay(data);
      FactionTownLife.simulateDay(level, data);
      FactionGrowth.simulateDay(level, data);
      FactionPorts.simulateDay(data);
      FactionRulers.simulateDay(level, data);
      FactionFamilies.simulateDay(level, data);
      FactionFeasts.simulateDay(level, data);
      FactionPastimes.simulateDay(level, data);
      FactionCourt.simulateDay(level, data);
      FactionDeals.simulateDay(level, data);
      FactionCounterSpies.simulateDay(level, data);
      FactionIncidents.simulateDay(level, data);
      FactionCoalitions.simulateDay(level, data);
      FactionVassals.simulateDay(level, data);
      FactionCouncil.simulateDay(level, data);
      FactionPlots.simulateDay(level, data);
      FactionQuests.simulateDay(level, data);
      FactionConquest.simulateDay(level, data);
      FactionTrade.simulateDay(level, data);
      TownMarkets.simulateDay(level, data);
      FreeVillages.simulateDay(level, data);
      data.setDirty();
      AncientWarfareCore.LOG.debug("Simulated faction day {}: recruits {}", data.lastSimulatedDay(), recruited);
   }

   /**
    * Taxes in percent of the usual quarter of the people: a small faction (the guild, a tribe with
    * a village or two) collects up to twice as much, so it can grow at all.
    */
   static int taxPercent(int settlements) {
      return 100 + SMALL_FACTION_TAX_BONUS * Math.max(0, SMALL_FACTION_SETTLEMENTS - settlements) / SMALL_FACTION_SETTLEMENTS;
   }

   /** A faction NPC died: soldiers come off their settlement's garrison, civilians off its people. */
   public static void onNpcDeath(ServerLevel level, FactionNpcEntity npc) {
      if (PlayerDiplomacy.isLeader(npc)) {
         // The successor comes a day later, not the moment the throne stands empty.
         for (FactionWorldData.Settlement settlement : FactionWorldData.get(level).settlements()) {
            if (settlement.faction().equals(npc.getFactionName()) && FactionTowns.CAPITAL.equals(settlement.kind())) {
               LEADER_LOST.put(settlement.id(), level.getGameTime());
            }
         }

         return;
      }

      if (FactionCampaigns.campaignId(npc) != 0) {
         FactionCampaigns.onNpcDeath(FactionWorldData.get(level), npc);
         return;
      }

      if (FactionArmies.armyId(npc) != 0 || FactionCaravans.caravanId(npc) != 0 || FactionTravellers.isTraveller(npc) || FactionTownLife.isFairgoer(npc)) {
         return;
      }

      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement settlement = settlementAt(data, npc.getFactionName(), npc.getX(), npc.getZ(), LOSS_MARGIN);
      if (settlement != null) {
         if (isCivilian(npc)) {
            settlement.population = Math.max(0, settlement.population - 1);
         } else {
            settlement.garrison = Math.max(0, settlement.garrison - 1);
         }

         data.setDirty();
      }
   }

   /**
    * A faction NPC entered the world: if it belongs to the faction its settlement was taken from,
    * it fell in that battle (soldiers) or now serves the new owner (civilians). Returns false when
    * the NPC should not be added.
    */
   public static boolean onNpcJoin(ServerLevel level, FactionNpcEntity npc) {
      FactionWorldData data = FactionWorldData.get(level);
      if (FactionCaravans.isStale(npc)) {
         return false;
      }

      if (FactionTravellers.isTraveller(npc)) {
         return FactionTravellers.stillTravelling(npc);
      }

      if (FactionTownLife.isFairgoer(npc)) {
         return !FactionTownLife.isStale(npc);
      }

      if (FactionTownLife.isThief(npc)) {
         return !FactionTownLife.isStaleThief(npc);
      }

      int campaign = FactionCampaigns.campaignId(npc);
      if (campaign != 0) {
         // A marching column is spawned afresh from its numbers; old soldiers of it go.
         return FactionCampaigns.stillMarching(npc);
      }

      int army = FactionArmies.armyId(npc);
      if (army != 0) {
         // Soldiers of an army that has since gone home or fallen.
         return data.army(army) != null;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.capturedFrom.equals(npc.getFactionName()) && settlement.contains(npc.getX(), npc.getZ(), GARRISON_MARGIN)) {
            if (isCivilian(npc)) {
               npc.setFactionName(settlement.faction());
               return true;
            }

            return false;
         }
      }

      return true;
   }

   /** The faction's settlement at a place; of two near each other, the one whose middle is closer. */
   public static FactionWorldData.Settlement settlementAt(FactionWorldData data, String faction, double x, double z, int margin) {
      FactionWorldData.Settlement best = null;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction)
            && settlement.contains(x, z, margin)
            && (best == null || settlement.horizontalDistanceSquared(x, z) < best.horizontalDistanceSquared(x, z))) {
            best = settlement;
         }
      }

      return best;
   }

   private static void maintainGarrisons(ServerLevel level, FactionWorldData data) {
      List<ServerPlayer> players = level.players();

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         BlockPos center = settlement.center();
         boolean near = false;

         for (ServerPlayer player : players) {
            double distance = player.distanceToSqr(center.getX(), player.getY(), center.getZ());
            if (distance < PlayerDiplomacy.DISCOVERY_RANGE * PlayerDiplomacy.DISCOVERY_RANGE) {
               PlayerDiplomacy.discover(player, settlement.faction());
            }

            if (distance < ACTIVE_RANGE * ACTIVE_RANGE) {
               near = true;
            }
         }

         if (near && settlement.built() && level.isLoaded(settlement.min()) && level.isLoaded(settlement.max())) {
            if (FactionTowns.CAPITAL.equals(settlement.kind())) {
               keepLeader(level, settlement);
            }

            // The town's own people are about whether or not its soldiers have taken up their posts yet.
            keepTownsfolk(level, data, settlement);
            if (!settlement.manned) {
               if (spawnersRemain(level, settlement)) {
                  continue;
               }

               settlement.manned = true;
               data.setDirty();
            }

            if (level.getGameTime() - settlement.lastRespawn >= RESPAWN_TICKS && liveSoldiers(level, settlement) < settlement.garrison) {
               settlement.lastRespawn = level.getGameTime();
               respawnSoldier(level, settlement);
            }
         }
      }
   }

   /** A capital always has its leader: one stands near the town's centre, back a day after he falls. */
   private static void keepLeader(ServerLevel level, FactionWorldData.Settlement settlement) {
      AABB area = new AABB(settlement.min(), settlement.max().offset(1, 1, 1)).inflate(GARRISON_MARGIN, 32.0, GARRISON_MARGIN);
      List<FactionNpcEntity> leaders = level.getEntitiesOfClass(
         FactionNpcEntity.class, area, npc -> npc.isAlive() && PlayerDiplomacy.isLeader(npc) && npc.getFactionName().equals(settlement.faction())
      );
      if (!leaders.isEmpty()) {
         FactionRulers.settleThrone(level, leaders);
         return;
      }

      Long fell = LEADER_LOST.get(settlement.id());
      if (fell == null) {
         LEADER_LOST.put(settlement.id(), level.getGameTime() - LEADER_RESPAWN_TICKS);
         return;
      } else if (level.getGameTime() - fell < LEADER_RESPAWN_TICKS) {
         return;
      }

      BlockPos center = settlement.center();
      RandomSource random = level.getRandom();

      for (int attempt = 0; attempt < 8; attempt++) {
         int x = center.getX() + random.nextInt(2 * LEADER_SPREAD + 1) - LEADER_SPREAD;
         int z = center.getZ() + random.nextInt(2 * LEADER_SPREAD + 1) - LEADER_SPREAD;
         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         BlockPos pos = new BlockPos(x, y, z);
         if (!level.getBlockState(pos.below()).getFluidState().isEmpty()
            || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
            || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            continue;
         }

         FactionNpcEntity leader = AWCoreEntityTypes.FACTION_LEADER_NPC.get().create(level);
         if (leader == null) {
            return;
         }

         leader.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
         leader.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
         leader.setFactionName(settlement.faction());
         leader.setPersistenceRequired();
         leader.restrictTo(center, LEADER_SPREAD * 2);
         level.addFreshEntity(leader);
         LEADER_LOST.remove(settlement.id());
         AncientWarfareCore.LOG.info("Leader of {} stands in capital #{} at {}, {}, {}", settlement.faction(), settlement.id(), x, y, z);
         return;
      }
   }

   /**
    * A town's streets are not empty: while a player is near, as many of its people as its size
    * calls for are out and about, one more coming out of a door at a time while nobody watches the
    * spot. They keep the day of townsfolk (see CivilianRoutineGoal) and sleep in its beds at night.
    */
   private static void keepTownsfolk(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement) {
      if (!FactionTownLife.isTown(settlement) || FactionTownLife.underAttack(data, settlement)) {
         return;
      }

      int wanted = Math.max(MIN_TOWNSFOLK, Math.min(MAX_TOWNSFOLK, settlement.population / PEOPLE_PER_TOWNSFOLK));
      AABB area = new AABB(settlement.min(), settlement.max().offset(1, 1, 1)).inflate(8.0, 16.0, 8.0);
      int out = level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getFactionName().equals(settlement.faction()) && isCivilian(npc)).size();
      if (out >= wanted) {
         return;
      }

      RandomSource random = level.getRandom();
      for (int attempt = 0; attempt < 12; attempt++) {
         int x = settlement.min().getX() + random.nextInt(settlement.max().getX() - settlement.min().getX() + 1);
         int z = settlement.min().getZ() + random.nextInt(settlement.max().getZ() - settlement.min().getZ() + 1);
         if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            continue;
         }

         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         BlockPos pos = new BlockPos(x, y, z);
         if (level.getNearestPlayer(x + 0.5, y, z + 0.5, HIDDEN_RANGE, false) != null
            || !level.getBlockState(pos.below()).getFluidState().isEmpty()
            || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
            || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
            || !onTheGround(level, pos)) {
            continue;
         }

         EntityType<? extends FactionNpcEntity> type = random.nextBoolean()
            ? AWCoreEntityTypes.FACTION_CIVILIAN_MALE_NPC.get()
            : AWCoreEntityTypes.FACTION_CIVILIAN_FEMALE_NPC.get();
         FactionNpcEntity npc = type.create(level);
         if (npc == null) {
            return;
         }

         npc.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
         npc.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
         npc.setFactionName(settlement.faction());
         level.addFreshEntity(npc);
         return;
      }
   }

   private static boolean spawnersRemain(ServerLevel level, FactionWorldData.Settlement settlement) {
      for (int cx = settlement.min().getX() >> 4; cx <= settlement.max().getX() >> 4; cx++) {
         for (int cz = settlement.min().getZ() >> 4; cz <= settlement.max().getZ() >> 4; cz++) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) {
               return true;
            }

            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
               if (blockEntity instanceof AdvancedSpawnerBlockEntity && settlement.contains(blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getZ(), 0)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   static int liveSoldiers(ServerLevel level, FactionWorldData.Settlement settlement) {
      return liveSoldierList(level, settlement).size();
   }

   static List<FactionNpcEntity> liveSoldierList(ServerLevel level, FactionWorldData.Settlement settlement) {
      AABB area = new AABB(settlement.min(), settlement.max().offset(1, 1, 1)).inflate(GARRISON_MARGIN, 32.0, GARRISON_MARGIN);
      return level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getFactionName().equals(settlement.faction()) && !isCivilian(npc));
   }

   private static void respawnSoldier(ServerLevel level, FactionWorldData.Settlement settlement) {
      RandomSource random = level.getRandom();

      for (int attempt = 0; attempt < 8; attempt++) {
         int x = settlement.min().getX() + random.nextInt(settlement.max().getX() - settlement.min().getX() + 1);
         int z = settlement.min().getZ() + random.nextInt(settlement.max().getZ() - settlement.min().getZ() + 1);
         if (level.getNearestPlayer(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, HIDDEN_RANGE, false) == null
            && spawnDefender(level, settlement.faction(), x, z) != null) {
            return;
         }
      }
   }

   /** Spawns one of the faction's soldiers (a third of them archers) standing on the ground at x, z; null when there is no room. */
   static FactionNpcEntity spawnSoldier(ServerLevel level, String faction, int x, int z) {
      return spawnSoldier(level, faction, x, z, null);
   }

   /** As spawnSoldier, tagged before it joins the world (so an army's soldier is known as one when it does). */
   static FactionNpcEntity spawnSoldier(ServerLevel level, String faction, int x, int z, String tag) {
      return spawnSoldier(level, faction, x, z, tag, 1.0 / 3.0);
   }

   /** As spawnSoldier, an archer with the given chance. */
   static FactionNpcEntity spawnSoldier(ServerLevel level, String faction, int x, int z, String tag, double archers) {
      return spawnSoldier(level, faction, x, z, tag, archers, false);
   }

   /** A town's own defender: one may come out on its walls (never on a roof), and climbs down to fight. */
   static FactionNpcEntity spawnDefender(ServerLevel level, String faction, int x, int z) {
      return spawnSoldier(level, faction, x, z, null, 1.0 / 3.0, true);
   }

   private static FactionNpcEntity spawnSoldier(ServerLevel level, String faction, int x, int z, String tag, double archers, boolean walls) {
      RandomSource random = level.getRandom();
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos pos = new BlockPos(x, y, z);
      if (!level.getBlockState(pos.below()).getFluidState().isEmpty()
         || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
         || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
         || onRoof(level, pos)
         || !walls && onWall(level, pos)) {
         return null;
      }

      EntityType<? extends FactionNpcEntity> type = random.nextDouble() < archers ? AWCoreEntityTypes.FACTION_ARCHER_NPC.get() : AWCoreEntityTypes.FACTION_SOLDIER_NPC.get();
      FactionNpcEntity npc = type.create(level);
      if (npc == null) {
         return null;
      }

      npc.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
      npc.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
      npc.setFactionName(faction);
      if (tag != null) {
         npc.addTag(tag);
      }

      return level.addFreshEntity(npc) ? npc : null;
   }

   /**
    * Whether a spot is on the ground rather than up on a roof or a wall walk, where a soldier put
    * down would stand stuck out of the fight: a roof has rooms under it, and a wall walk stands
    * well above the land on either side of it.
    */
   static boolean onTheGround(ServerLevel level, BlockPos feet) {
      return !onRoof(level, feet) && !onWall(level, feet);
   }

   /** A roof is built, not grown, and has rooms under it; land over a cave or the world's floor is not one. */
   static boolean onRoof(ServerLevel level, BlockPos feet) {
      net.minecraft.world.level.block.state.BlockState under = level.getBlockState(feet.below());
      if (under.is(net.minecraft.tags.BlockTags.DIRT)
         || under.is(net.minecraft.tags.BlockTags.SAND)
         || under.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
         || under.is(net.minecraft.world.level.block.Blocks.GRAVEL)
         || under.is(net.minecraft.world.level.block.Blocks.DIRT_PATH)
         || under.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)) {
         return false;
      }

      for (int dy = 2; dy <= 5 && feet.getY() - dy >= level.getMinBuildHeight(); dy++) {
         if (level.getBlockState(feet.below(dy)).isAir()) {
            return true;
         }
      }

      return false;
   }

   /** A wall walk stands well above the land on either side of it. */
   static boolean onWall(ServerLevel level, BlockPos feet) {
      int below = 0;
      for (int i = 0; i < 8; i++) {
         double angle = i * Math.PI / 4.0;
         int x = feet.getX() + (int)Math.round(Math.cos(angle) * 4.0);
         int z = feet.getZ() + (int)Math.round(Math.sin(angle) * 4.0);
         if (level.hasChunkAt(new BlockPos(x, feet.getY(), z)) && level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) <= feet.getY() - 3) {
            below++;
         }
      }

      return below >= 4;
   }

   static boolean isCivilian(FactionNpcEntity npc) {
      return FactionNpcRole.byId(npc.getNpcSubtype()).map(FactionNpcRole::isCivilian).orElse(false);
   }
}
