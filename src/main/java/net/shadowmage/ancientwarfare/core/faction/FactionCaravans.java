package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionTraderNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * Caravans a player can meet: while a player is near a finished road, now and then a trader and
 * guards of one end's faction walk along it toward the other end, and can be traded with. On roads
 * outlaws prowl near and no guard post watches, bandits may lie in wait ahead. Killing bandits who
 * threaten a caravan earns its faction's thanks; killing its trader is robbery and means war.
 * Caravans live only while a player is near; they are not saved.
 */
public final class FactionCaravans {
   public static final String TAG_PREFIX = "aw_caravan_";
   private static final int TICK_INTERVAL = 10;
   private static final int SPAWN_CHECK_TICKS = 200;
   private static final double NOTICE_RANGE = 96.0;
   private static final double MIN_SPAWN_DISTANCE = 48.0;
   private static final double MAX_SPAWN_DISTANCE = 96.0;
   private static final double DESPAWN_RANGE = 160.0;
   private static final long COOLDOWN_TICKS = 6000L;
   private static final int GUARDS = 2;
   private static final int BANDITS = 3;
   private static final double AMBUSH_CHANCE = 0.5;
   private static final int AMBUSH_RANGE = 400;
   /** Points ahead of the caravan where bandits wait. */
   private static final int AMBUSH_AHEAD = 8;
   private static final double CARAVAN_SPEED = 0.25;
   /** Roads laid over rough ground keep a few drops; a fall this high does no harm. */
   private static final int CARAVAN_DROP = 3;
   /** Advance checks without reaching the next road point before the caravan counts as stuck. */
   private static final int STUCK_CHECKS = 20;
   /** How far back along the road guards look for their way to the trader. */
   private static final int STRAGGLER_POINTS = 6;
   private static final double HIDDEN_RANGE = 24.0;
   private static final double LINGER_RANGE = 64.0;
   private static final int RESCUE_STANDING = 3;
   private static final int ROBBERY_STANDING = -40;
   private static final Map<Integer, Caravan> ACTIVE = new HashMap<>();
   private static final Map<Integer, Long> LAST_CARAVAN = new HashMap<>();
   /** Set by the test command: the next caravan that can be ambushed is. */
   private static boolean forceAmbush;

   private FactionCaravans() {
   }

   public static int caravanId(Entity entity) {
      for (String tag : entity.getTags()) {
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

   /** A caravan NPC loaded from an old save: caravans are not kept, so it goes. */
   public static boolean isStale(FactionNpcEntity npc) {
      int id = caravanId(npc);
      return id != 0 && !ACTIVE.containsKey(id);
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      long now = level.getGameTime();
      if (now % TICK_INTERVAL != 0L) {
         return;
      }

      Iterator<Caravan> iterator = ACTIVE.values().iterator();

      while (iterator.hasNext()) {
         Caravan caravan = iterator.next();
         if (!caravan.advance(level, data)) {
            caravan.discard(level);
            iterator.remove();
         }
      }

      if (now % SPAWN_CHECK_TICKS == 0L) {
         for (ServerPlayer player : level.players()) {
            trySpawn(level, data, player, now);
         }
      }
   }

   private static void trySpawn(ServerLevel level, FactionWorldData data, ServerPlayer player, long now) {
      for (FactionWorldData.Route route : data.routes()) {
         if (route.road() != FactionWorldData.RoadState.DONE
            || route.blocked()
            || ACTIVE.containsKey(route.id())
            || now - LAST_CARAVAN.getOrDefault(route.id(), Long.MIN_VALUE / 2) < COOLDOWN_TICKS) {
            continue;
         }

         List<Long> points = route.points();
         int start = -1;
         boolean near = false;

         for (int i = 0; i < points.size(); i++) {
            double distance = distance(player, points.get(i));
            near |= distance < NOTICE_RANGE;
            if (start < 0 && distance >= MIN_SPAWN_DISTANCE && distance <= MAX_SPAWN_DISTANCE && i > 2 && i < points.size() - 3) {
               start = i;
            }
         }

         if (!near || start < 0) {
            continue;
         }

         // Head for whichever end is farther, so the caravan passes the player.
         int direction = start < points.size() / 2 ? 1 : -1;
         FactionWorldData.Settlement home = data.endpoint(route, direction < 0);
         if (home == null) {
            continue;
         }

         LAST_CARAVAN.put(route.id(), now);
         Caravan caravan = new Caravan(route.id(), home.faction(), start, direction);
         // Registered first: its NPCs are checked against the active caravans as they join the world.
         ACTIVE.put(route.id(), caravan);
         if (caravan.spawn(level, data, route)) {
            return;
         }

         ACTIVE.remove(route.id());
         caravan.discard(level);
      }
   }

   /** A faction NPC died: thanks for killing bandits near a caravan, war for robbing one. */
   public static void onNpcKilled(ServerLevel level, FactionNpcEntity npc, Player killer) {
      int id = caravanId(npc);
      Caravan caravan = ACTIVE.get(id);
      if (caravan == null) {
         return;
      }

      FactionStandingData standings = FactionStandingData.get(level);
      Component name = FactionNews.factionName(caravan.faction);
      if (caravan.bandits.contains(npc.getUUID())) {
         standings.adjustStanding(killer.getUUID(), caravan.faction, RESCUE_STANDING);
         if (killer instanceof ServerPlayer player) {
            PlayerDiplomacy.discover(player, caravan.faction);
            player.displayClientMessage(Component.translatable("caravan.ancientwarfare.rescue", name), true);
         }
      } else if (npc instanceof FactionTraderNpcEntity && !FactionDiplomacy.isOutlaw(caravan.faction)) {
         standings.adjustStanding(killer.getUUID(), caravan.faction, ROBBERY_STANDING);
         if (standings.getTreaty(killer.getUUID(), caravan.faction) != RelationState.WAR) {
            standings.setTreaty(killer.getUUID(), caravan.faction, RelationState.WAR);
            killer.sendSystemMessage(Component.translatable("caravan.ancientwarfare.robbed", name));
         }
      }
   }

   private static double distance(Entity entity, long point) {
      double dx = FactionRoads.unpackX(point) + 0.5 - entity.getX();
      double dz = FactionRoads.unpackZ(point) + 0.5 - entity.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static final class Caravan {
      private final int routeId;
      private final String faction;
      private final int direction;
      private int index;
      private int stuck;
      private UUID trader;
      private final List<UUID> guards = new ArrayList<>();
      private final List<UUID> bandits = new ArrayList<>();

      Caravan(int routeId, String faction, int index, int direction) {
         this.routeId = routeId;
         this.faction = faction;
         this.index = index;
         this.direction = direction;
      }

      String tag() {
         return TAG_PREFIX + this.routeId;
      }

      boolean spawn(ServerLevel level, FactionWorldData data, FactionWorldData.Route route) {
         long point = route.points().get(this.index);
         int x = FactionRoads.unpackX(point);
         int z = FactionRoads.unpackZ(point);
         FactionNpcEntity traderNpc = this.create(level, AWCoreEntityTypes.FACTION_TRADER_NPC.get(), this.faction, x, z);
         if (traderNpc == null) {
            return false;
         }

         this.trader = traderNpc.getUUID();

         for (int i = 0; i < GUARDS; i++) {
            FactionNpcEntity guard = this.create(level, AWCoreEntityTypes.FACTION_SOLDIER_NPC.get(), this.faction, x - this.direction * (2 + i * 2), z);
            if (guard != null) {
               this.guards.add(guard.getUUID());
            }
         }

         String outlaw = nearbyOutlaws(data, route);
         int ahead = this.index + this.direction * AMBUSH_AHEAD;
         if (outlaw != null && ahead > 0 && ahead < route.points().size() && (forceAmbush || level.getRandom().nextDouble() < AMBUSH_CHANCE)) {
            forceAmbush = false;
            long spot = route.points().get(ahead);

            for (int i = 0; i < BANDITS; i++) {
               int bx = FactionRoads.unpackX(spot) + level.getRandom().nextInt(13) - 6;
               int bz = FactionRoads.unpackZ(spot) + level.getRandom().nextInt(13) - 6;
               FactionNpcEntity bandit = this.create(level, AWCoreEntityTypes.FACTION_SOLDIER_NPC.get(), outlaw, bx, bz);
               if (bandit != null) {
                  this.bandits.add(bandit.getUUID());
               }
            }
         }

         return true;
      }

      private FactionNpcEntity create(ServerLevel level, EntityType<? extends FactionNpcEntity> type, String faction, int x, int z) {
         level.getChunk(x >> 4, z >> 4);
         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         FactionNpcEntity npc = type.create(level);
         if (npc == null) {
            return null;
         }

         npc.moveTo(x + 0.5, y, z + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
         npc.addTag(this.tag());
         npc.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null, null);
         npc.setFactionName(faction);
         AttributeInstance speed = npc.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speed != null && speed.getBaseValue() < CARAVAN_SPEED) {
            // Faction defaults keep settlement traders at their stalls; a caravan trader walks.
            speed.setBaseValue(CARAVAN_SPEED);
         }

         npc.setMaxDrop(CARAVAN_DROP);
         level.addFreshEntity(npc);
         return npc;
      }

      /** Walks the caravan on along its road. Returns false when it is done: arrived, lost or no player near. */
      boolean advance(ServerLevel level, FactionWorldData data) {
         FactionWorldData.Route route = null;

         for (FactionWorldData.Route candidate : data.routes()) {
            if (candidate.id() == this.routeId) {
               route = candidate;
            }
         }

         Entity leader = this.trader == null ? null : level.getEntity(this.trader);
         if (route == null || !(leader instanceof FactionNpcEntity traderNpc) || !traderNpc.isAlive()) {
            // Robbed or lost: whoever is left stays until no player is near enough to see them go.
            return route != null && this.anyoneWatched(level);
         }

         if (level.getNearestPlayer(traderNpc, DESPAWN_RANGE) == null) {
            return false;
         }

         List<Long> points = route.points();
         boolean next = false;
         if (distance(traderNpc, points.get(this.index)) < 3.0) {
            this.index += this.direction;
            this.stuck = 0;
            next = true;
            if (this.index < 0 || this.index >= points.size()) {
               return false;
            }
         } else if (++this.stuck >= STUCK_CHECKS && level.getNearestPlayer(traderNpc, HIDDEN_RANGE) == null) {
            // A step too high on the road: out of sight, the caravan hops on to the next point.
            this.hop(level, traderNpc, points.get(this.index));
         }

         if (traderNpc.getTarget() == null && (next || traderNpc.getNavigation().isDone())) {
            long point = points.get(this.index);
            int x = FactionRoads.unpackX(point);
            int z = FactionRoads.unpackZ(point);
            traderNpc.getNavigation().moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, 1.0);
         }

         for (UUID id : this.guards) {
            if (level.getEntity(id) instanceof FactionNpcEntity guard && guard.isAlive() && guard.getTarget() == null && guard.distanceToSqr(traderNpc) > 16.0) {
               if (guard.distanceToSqr(traderNpc) > 400.0 && level.getNearestPlayer(guard, HIDDEN_RANGE) == null) {
                  guard.moveTo(traderNpc.getX(), traderNpc.getY(), traderNpc.getZ());
               } else if (guard.distanceToSqr(traderNpc) <= 144.0) {
                  guard.getNavigation().moveTo(traderNpc, 1.2);
               } else if (guard.getNavigation().isDone()) {
                  // Along the road, not straight at the trader: a long path across rough ground fails.
                  long point = points.get(this.nextPointFor(guard, points));
                  int x = FactionRoads.unpackX(point);
                  int z = FactionRoads.unpackZ(point);
                  guard.getNavigation().moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, 1.2);
               }
            }
         }

         return true;
      }

      private boolean anyoneWatched(ServerLevel level) {
         List<UUID> all = new ArrayList<>(this.guards);
         all.addAll(this.bandits);

         for (UUID id : all) {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive() && level.getNearestPlayer(entity, LINGER_RANGE) != null) {
               return true;
            }
         }

         return false;
      }

      /** The road point after the one nearest a straggler, among those the trader passed lately. */
      private int nextPointFor(Entity follower, List<Long> points) {
         int nearest = this.index;
         double best = Double.MAX_VALUE;

         for (int back = 0; back <= STRAGGLER_POINTS; back++) {
            int i = this.index - this.direction * back;
            if (i < 0 || i >= points.size()) {
               break;
            }

            double d = distance(follower, points.get(i));
            if (d < best) {
               best = d;
               nearest = i;
            }
         }

         return nearest == this.index || best >= 3.0 ? nearest : nearest + this.direction;
      }

      private void hop(ServerLevel level, FactionNpcEntity npc, long point) {
         int x = FactionRoads.unpackX(point);
         int z = FactionRoads.unpackZ(point);
         npc.getNavigation().stop();
         npc.moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
         this.stuck = 0;
      }

      void discard(ServerLevel level) {
         List<UUID> all = new ArrayList<>(this.guards);
         all.addAll(this.bandits);
         if (this.trader != null) {
            all.add(this.trader);
         }

         for (UUID id : all) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
               entity.discard();
            }
         }
      }
   }

   /** An outlaw faction camped near the road's middle with no guard post watching it, or null. */
   private static String nearbyOutlaws(FactionWorldData data, FactionWorldData.Route route) {
      List<Long> points = route.points();
      long middle = points.get(points.size() / 2);
      int x = FactionRoads.unpackX(middle);
      int z = FactionRoads.unpackZ(middle);
      String outlaw = null;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.horizontalDistanceSquared(x, z) <= (double)AMBUSH_RANGE * AMBUSH_RANGE) {
            if (FactionTrade.GUARD_POST.equals(settlement.kind()) && settlement.garrison() > 0) {
               return null;
            }

            if (FactionDiplomacy.isOutlaw(settlement.faction())) {
               outlaw = settlement.faction();
            }
         }
      }

      return outlaw;
   }

   /** For the debug command: makes the next caravan on every road due at once. */
   public static void hurry(boolean ambush) {
      LAST_CARAVAN.clear();
      forceAmbush = ambush;
   }

   public static int active() {
      return ACTIVE.size();
   }
}
