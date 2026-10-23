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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * People a player meets on the roads besides caravans: pilgrims behind a priest, a lone peddler
 * with goods to sell, a messenger in a hurry, and refugees fleeing a town struck by war, hunger or
 * plague. They walk the road from one end toward the other and, stopped and spoken to, say who
 * they are and pass on the news of the land (see FactionRumours). Like caravans, they live only
 * while a player is near and are not saved.
 */
public final class FactionTravellers {
   public static final String TAG_PREFIX = "aw_traveller_";
   private static final int TICK_INTERVAL = 10;
   private static final int SPAWN_CHECK_TICKS = 300;
   private static final double SPAWN_CHANCE = 0.5;
   private static final int MAX_GROUPS = 3;
   private static final double NOTICE_RANGE = 96.0;
   private static final double MIN_SPAWN_DISTANCE = 40.0;
   private static final double MAX_SPAWN_DISTANCE = 90.0;
   private static final double DESPAWN_RANGE = 150.0;
   private static final double HIDDEN_RANGE = 24.0;
   private static final long ROUTE_COOLDOWN = 2400L;
   private static final double REFUGEE_CHANCE = 0.5;
   private static final double WALK_SPEED = 0.25;
   private static final double MESSENGER_SPEED = 0.36;
   private static final int STUCK_CHECKS = 20;
   private static final int MAX_DROP = 3;

   enum Kind {
      PILGRIMS,
      PEDDLER,
      MESSENGER,
      REFUGEES;
   }

   private static final Map<Integer, Group> ACTIVE = new HashMap<>();
   private static final Map<Integer, Long> LAST_ON_ROUTE = new HashMap<>();
   private static int nextId = 1;

   private FactionTravellers() {
   }

   public static int travellerId(Entity entity) {
      for (String tag : entity.getTags()) {
         if (tag.startsWith(TAG_PREFIX)) {
            try {
               return Integer.parseInt(tag.substring(TAG_PREFIX.length()));
            } catch (NumberFormatException ignored) {
               return -1;
            }
         }
      }

      return 0;
   }

   /** Whether a traveller joining the world belongs to a group still on the road; old ones from a save go. */
   public static boolean stillTravelling(Entity entity) {
      return ACTIVE.containsKey(travellerId(entity));
   }

   public static boolean isTraveller(Entity entity) {
      return travellerId(entity) != 0;
   }

   /** The player stopped a traveller: who they are, where they go, and the news. */
   public static void talk(ServerPlayer player, FactionNpcEntity npc) {
      Group group = ACTIVE.get(travellerId(npc));
      if (group == null) {
         return;
      }

      ServerLevel level = player.serverLevel();
      if (net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData.get(level).isHostile(player.getUUID(), group.faction)) {
         FactionRumours.chat(player, npc);
         return;
      }

      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement goal = group.goal(data);
      Component where = goal == null || goal.name().isEmpty() ? Component.translatable("traveller.ancientwarfare.next_town") : FactionNews.named(goal);
      Component intro = Component.translatable("traveller.ancientwarfare." + group.kind.name().toLowerCase(java.util.Locale.ROOT), where, FactionNews.factionName(group.faction));
      FactionRumours.say(player, npc.getDisplayName(), intro);
      FactionRumours.chat(player, npc);
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      long now = level.getGameTime();
      if (now % TICK_INTERVAL != 0L) {
         return;
      }

      Iterator<Group> iterator = ACTIVE.values().iterator();
      while (iterator.hasNext()) {
         Group group = iterator.next();
         if (!group.advance(level, data)) {
            group.discard(level);
            iterator.remove();
         }
      }

      if (now % SPAWN_CHECK_TICKS == 0L) {
         for (ServerPlayer player : level.players()) {
            if (ACTIVE.size() < MAX_GROUPS && level.getRandom().nextDouble() < SPAWN_CHANCE * FactionSeasons.travel(level)) {
               trySpawn(level, data, player, now);
            }
         }
      }
   }

   private static void trySpawn(ServerLevel level, FactionWorldData data, ServerPlayer player, long now) {
      List<FactionWorldData.Route> routes = new ArrayList<>(data.routes());
      java.util.Collections.shuffle(routes, new java.util.Random(level.getRandom().nextLong()));
      for (FactionWorldData.Route route : routes) {
         if (route.road() != FactionWorldData.RoadState.DONE || now - LAST_ON_ROUTE.getOrDefault(route.id(), Long.MIN_VALUE / 2) < ROUTE_COOLDOWN) {
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

         // Toward whichever end is farther, so they pass the player.
         int direction = start < points.size() / 2 ? 1 : -1;
         FactionWorldData.Settlement from = data.endpoint(route, direction < 0);
         if (from == null) {
            continue;
         }

         RandomSource random = level.getRandom();
         Kind kind;
         String faction = from.faction();
         boolean struck = from.famine() > 0 || from.plague() > 0 || !from.capturedFrom().isEmpty();
         if (struck && random.nextDouble() < REFUGEE_CHANCE) {
            kind = Kind.REFUGEES;
            // Those who fled a captured town are of the people who lost it.
            faction = from.capturedFrom().isEmpty() ? faction : from.capturedFrom();
         } else if (FactionDiplomacy.isOutlaw(faction)) {
            continue;
         } else {
            double roll = random.nextDouble();
            kind = roll < 0.35 ? Kind.PILGRIMS : roll < 0.7 ? Kind.PEDDLER : Kind.MESSENGER;
         }

         LAST_ON_ROUTE.put(route.id(), now);
         Group group = new Group(nextId++, kind, faction, route.id(), start, direction);
         // Registered first: its people are checked against the active groups as they join the world.
         ACTIVE.put(group.id, group);
         if (group.spawn(level, route)) {
            AncientWarfareCore.LOG.debug("Travellers #{} ({}) of {} on road #{}", group.id, kind, faction, route.id());
            return;
         }

         ACTIVE.remove(group.id);
         group.discard(level);
      }
   }

   private static double distance(Entity entity, long point) {
      double dx = FactionRoads.unpackX(point) + 0.5 - entity.getX();
      double dz = FactionRoads.unpackZ(point) + 0.5 - entity.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   public static int active() {
      return ACTIVE.size();
   }

   private static final class Group {
      private final int id;
      private final Kind kind;
      private final String faction;
      private final int routeId;
      private final int direction;
      private int index;
      private int stuck;
      private UUID leader;
      private final List<UUID> followers = new ArrayList<>();

      Group(int id, Kind kind, String faction, int routeId, int index, int direction) {
         this.id = id;
         this.kind = kind;
         this.faction = faction;
         this.routeId = routeId;
         this.index = index;
         this.direction = direction;
      }

      FactionWorldData.Settlement goal(FactionWorldData data) {
         FactionWorldData.Route route = route(data);
         return route == null ? null : data.endpoint(route, this.direction > 0);
      }

      private FactionWorldData.Route route(FactionWorldData data) {
         for (FactionWorldData.Route route : data.routes()) {
            if (route.id() == this.routeId) {
               return route;
            }
         }

         return null;
      }

      boolean spawn(ServerLevel level, FactionWorldData.Route route) {
         long point = route.points().get(this.index);
         int x = FactionRoads.unpackX(point);
         int z = FactionRoads.unpackZ(point);
         RandomSource random = level.getRandom();
         EntityType<? extends FactionNpcEntity> leaderType = switch (this.kind) {
            case PILGRIMS -> AWCoreEntityTypes.FACTION_PRIEST_NPC.get();
            case PEDDLER -> AWCoreEntityTypes.FACTION_TRADER_NPC.get();
            case MESSENGER -> AWCoreEntityTypes.FACTION_SOLDIER_NPC.get();
            case REFUGEES -> civilian(random);
         };
         FactionNpcEntity first = this.create(level, leaderType, x, z, this.kind == Kind.MESSENGER ? MESSENGER_SPEED : WALK_SPEED);
         if (first == null) {
            return false;
         }

         this.leader = first.getUUID();
         int followers = switch (this.kind) {
            case PILGRIMS -> 2 + random.nextInt(3);
            case REFUGEES -> 2 + random.nextInt(4);
            default -> 0;
         };
         for (int i = 0; i < followers; i++) {
            FactionNpcEntity follower = this.create(level, civilian(random), x - this.direction * (2 + i), z + (i % 2 == 0 ? 1 : -1), WALK_SPEED);
            if (follower != null) {
               if (this.kind == Kind.REFUGEES) {
                  // Worn out from the road.
                  follower.setHealth(follower.getMaxHealth() * 0.6F);
               }

               this.followers.add(follower.getUUID());
            }
         }

         return true;
      }

      private static EntityType<? extends FactionNpcEntity> civilian(RandomSource random) {
         return random.nextBoolean() ? AWCoreEntityTypes.FACTION_CIVILIAN_MALE_NPC.get() : AWCoreEntityTypes.FACTION_CIVILIAN_FEMALE_NPC.get();
      }

      private FactionNpcEntity create(ServerLevel level, EntityType<? extends FactionNpcEntity> type, int x, int z, double speed) {
         if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
         }

         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         FactionNpcEntity npc = type.create(level);
         if (npc == null) {
            return null;
         }

         npc.moveTo(x + 0.5, y, z + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
         npc.addTag(TAG_PREFIX + this.id);
         npc.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null, null);
         npc.setFactionName(this.faction);
         AttributeInstance movement = npc.getAttribute(Attributes.MOVEMENT_SPEED);
         if (movement != null) {
            movement.setBaseValue(speed);
         }

         npc.setMaxDrop(MAX_DROP);
         level.addFreshEntity(npc);
         return npc;
      }

      /** Walks on along the road; false once the group has gone out of sight, reached its end or lost its leader. */
      boolean advance(ServerLevel level, FactionWorldData data) {
         FactionWorldData.Route route = this.route(data);
         Entity entity = this.leader == null ? null : level.getEntity(this.leader);
         if (route == null || !(entity instanceof FactionNpcEntity lead) || !lead.isAlive()) {
            return this.anyoneWatched(level);
         }

         if (level.getNearestPlayer(lead, DESPAWN_RANGE) == null) {
            return false;
         }

         List<Long> points = route.points();
         if (this.index < 0 || this.index >= points.size()) {
            // Arrived while a player watched: they wait at the road's end until out of sight.
            return level.getNearestPlayer(lead, HIDDEN_RANGE) != null;
         }

         boolean next = false;
         if (distance(lead, points.get(this.index)) < 3.0) {
            this.index += this.direction;
            this.stuck = 0;
            next = true;
            if (this.index < 0 || this.index >= points.size()) {
               // Arrived: they go into the town, out of the player's way.
               return level.getNearestPlayer(lead, HIDDEN_RANGE) != null;
            }
         } else if (++this.stuck >= STUCK_CHECKS && level.getNearestPlayer(lead, HIDDEN_RANGE) == null) {
            long point = points.get(this.index);
            int x = FactionRoads.unpackX(point);
            int z = FactionRoads.unpackZ(point);
            lead.getNavigation().stop();
            lead.moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
            this.stuck = 0;
         }

         boolean talking = level.getNearestPlayer(lead, 3.0) != null;
         if (talking) {
            // A player stopped them: they stand and talk.
            lead.getNavigation().stop();
            this.stuck = 0;
         } else if (lead.getTarget() == null && (next || lead.getNavigation().isDone())) {
            long point = points.get(this.index);
            int x = FactionRoads.unpackX(point);
            int z = FactionRoads.unpackZ(point);
            lead.getNavigation().moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, 1.0);
         }

         for (UUID id : this.followers) {
            if (level.getEntity(id) instanceof FactionNpcEntity follower && follower.isAlive() && follower.getTarget() == null) {
               double gap = follower.distanceToSqr(lead);
               if (gap > 400.0 && level.getNearestPlayer(follower, HIDDEN_RANGE) == null) {
                  follower.moveTo(lead.getX(), lead.getY(), lead.getZ());
               } else if (gap > 9.0) {
                  follower.getNavigation().moveTo(lead, 1.1);
               } else if (talking) {
                  follower.getNavigation().stop();
               }
            }
         }

         return true;
      }

      private boolean anyoneWatched(ServerLevel level) {
         for (UUID id : this.followers) {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive() && level.getNearestPlayer(entity, NOTICE_RANGE) != null) {
               return true;
            }
         }

         return false;
      }

      void discard(ServerLevel level) {
         List<UUID> all = new ArrayList<>(this.followers);
         if (this.leader != null) {
            all.add(this.leader);
         }

         for (UUID id : all) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
               entity.discard();
            }
         }
      }
   }
}
