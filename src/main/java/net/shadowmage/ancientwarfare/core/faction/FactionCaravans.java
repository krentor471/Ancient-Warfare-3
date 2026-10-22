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
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.CoinMetal;
import net.shadowmage.ancientwarfare.core.item.CoinItem;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionTraderNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * Caravans a player can meet: while a player is near a finished road, now and then a trader and
 * guards of one end's faction walk along it toward the other end, and can be traded with. On roads
 * outlaws prowl near and no guard post watches, bandits may lie in wait ahead. Killing bandits who
 * threaten a caravan earns its faction's thanks; killing its trader is robbery and means war.
 * A wealthy faction's caravan leads pack animals: donkeys with chests, then a mule, then horses;
 * a rich one adds carts of goods, each drawn by a horse in the string. Breaking a cart open is
 * robbery, the same as killing the trader.
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
   private static final long COOLDOWN_TICKS = 3600L;
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
   /** Treasury a faction needs for each further pack animal in its caravans. */
   private static final int[] PACK_TREASURY = {300, 800, 1600, 3000};
   /** Treasury a faction needs for each cart of goods in its caravans. */
   private static final int[] CART_TREASURY = {5000, 12000};
   /** How far behind its horse a cart rolls, and how far it may lag before it is set right behind. */
   private static final double CART_GAP = 2.5;
   private static final double CART_LAG = 10.0;
   private static final double CART_SPEED = 0.35;
   /** Lead lengths in the string of animals: past slack an animal is led on, past wait the trader stops, past lag it is set behind. */
   private static final double STRING_SLACK = 6.0;
   private static final double STRING_WAIT = 8.0;
   private static final double STRING_LAG = 14.0;
   /** Advance checks the trader waits for a straggler before it is set behind in plain sight. */
   private static final int STRING_PATIENCE = 30;
   /** A player this near when a cart is broken open is the robber. */
   private static final double ROBBER_RANGE = 16.0;
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

   /** A caravan NPC or pack animal loaded from an old save: caravans are not kept, so it goes. */
   public static boolean isStale(Entity entity) {
      int id = caravanId(entity);
      return id != 0 && !ACTIVE.containsKey(id);
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      long now = level.getGameTime();
      // Carts roll every tick, or they would jerk along behind their horses.
      ACTIVE.values().forEach(caravan -> caravan.pullCarts(level));
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
      } else if (npc instanceof FactionTraderNpcEntity) {
         robbed(level, caravan, killer);
      }
   }

   /** Killing the trader or breaking open a cart: the faction turns on the robber. */
   private static void robbed(ServerLevel level, Caravan caravan, Player robber) {
      if (FactionDiplomacy.isOutlaw(caravan.faction)) {
         return;
      }

      AncientWarfareCore.LOG.info("{} robbed a caravan of {}", robber.getName().getString(), caravan.faction);
      FactionStandingData standings = FactionStandingData.get(level);
      standings.adjustStanding(robber.getUUID(), caravan.faction, ROBBERY_STANDING);
      if (standings.getTreaty(robber.getUUID(), caravan.faction) != RelationState.WAR) {
         standings.setTreaty(robber.getUUID(), caravan.faction, RelationState.WAR);
         robber.sendSystemMessage(Component.translatable("caravan.ancientwarfare.robbed", FactionNews.factionName(caravan.faction)));
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
      /** Advance checks the trader has waited for the string of animals. */
      private int waits;
      private UUID trader;
      private final List<UUID> guards = new ArrayList<>();
      private final List<UUID> bandits = new ArrayList<>();
      private final List<UUID> animals = new ArrayList<>();
      /** Carts of goods, each drawn by the animal at the same place in pullers. */
      private final List<ChestPushcartEntity> carts = new ArrayList<>();
      private final List<UUID> pullers = new ArrayList<>();

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

         Entity holder = traderNpc;
         int treasury = data.treasury(this.faction);

         for (int i = 0; i < PACK_TREASURY.length && treasury >= PACK_TREASURY[i]; i++) {
            AbstractHorse animal = this.createAnimal(level, i, x + this.direction * (2 + i * 2), z);
            if (animal == null) {
               break;
            }

            // Led in a string: the first by the trader, each of the others by the one in front.
            animal.setLeashedTo(holder, true);
            holder = animal;
            this.animals.add(animal.getUUID());
         }

         for (int i = 0; i < CART_TREASURY.length && treasury >= CART_TREASURY[i]; i++) {
            int offset = this.direction * (2 + (this.animals.size() + 1) * 3);
            AbstractHorse horse = this.createAnimal(level, PACK_TREASURY.length - 1, x + offset, z);
            if (horse == null) {
               break;
            }

            horse.setLeashedTo(holder, true);
            holder = horse;
            this.animals.add(horse.getUUID());
            ChestPushcartEntity cart = this.createCart(level, x + offset + this.direction * 3, z);
            if (cart != null) {
               this.carts.add(cart);
               this.pullers.add(horse.getUUID());
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

      /** Donkeys with chests first, then a mule, then horses. */
      private AbstractHorse createAnimal(ServerLevel level, int index, int x, int z) {
         EntityType<? extends AbstractHorse> type = index < 2 ? EntityType.DONKEY : index == 2 ? EntityType.MULE : EntityType.HORSE;
         AbstractHorse animal = type.create(level);
         if (animal == null) {
            return null;
         }

         level.getChunk(x >> 4, z >> 4);
         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         animal.moveTo(x + 0.5, y, z + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
         animal.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null, null);
         animal.setTamed(true);
         if (animal instanceof AbstractChestedHorse chested) {
            chested.setChest(true);
         }

         animal.addTag(this.tag());
         level.addFreshEntity(animal);
         return animal;
      }

      /** A cart loaded with the goods the caravan trades. */
      private ChestPushcartEntity createCart(ServerLevel level, int x, int z) {
         ChestPushcartEntity cart = AWCoreEntityTypes.CHEST_PUSHCART.get().create(level);
         if (cart == null) {
            return null;
         }

         level.getChunk(x >> 4, z >> 4);
         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         cart.moveTo(x + 0.5, y, z + 0.5, this.direction > 0 ? -90.0F : 90.0F, 0.0F);
         RandomSource random = level.getRandom();
         ItemStack[] goods = {
            new ItemStack(Items.WHEAT, 16 + random.nextInt(32)),
            new ItemStack(Items.BREAD, 8 + random.nextInt(16)),
            new ItemStack(Items.WHITE_WOOL, 8 + random.nextInt(16)),
            new ItemStack(Items.LEATHER, 4 + random.nextInt(12)),
            new ItemStack(Items.IRON_INGOT, 4 + random.nextInt(8)),
            CoinItem.create(CoinMetal.SILVER, 2 + random.nextInt(5))
         };

         for (int slot = 0; slot < goods.length; slot++) {
            cart.setItem(slot, goods[slot]);
         }

         cart.addTag(this.tag());
         level.addFreshEntity(cart);
         return cart;
      }

      /**
       * Each cart rolls after its horse, a little behind, facing it, along the ground: it is moved
       * rather than pushed, so road steps and slopes do not hold it back. One left far behind out of
       * sight is set right behind.
       */
      void pullCarts(ServerLevel level) {
         for (int i = 0; i < this.carts.size(); i++) {
            ChestPushcartEntity cart = this.carts.get(i);
            Entity puller = level.getEntity(this.pullers.get(i));
            if (!cart.isAlive() || cart.isVehicle() || puller == null || !puller.isAlive()) {
               continue;
            }

            double dx = puller.getX() - cart.getX();
            double dz = puller.getZ() - cart.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < 0.01) {
               continue;
            }

            cart.setNoGravity(true);
            cart.setDeltaMovement(0.0, 0.0, 0.0);
            cart.setYRot((float)(Math.atan2(-dx, dz) * (180.0 / Math.PI)));
            double step = distance > CART_LAG && level.getNearestPlayer(cart, HIDDEN_RANGE) == null
               ? distance - CART_GAP
               : Math.max(0.0, Math.min(distance - CART_GAP, CART_SPEED));
            double x = cart.getX() + dx / distance * step;
            double z = cart.getZ() + dz / distance * step;
            cart.setPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z)), z);
         }
      }

      /**
       * Keeps the string of animals on its leads: each follows the one in front (the first, the
       * trader), a straggler is led back, out of sight set right behind, and a snapped lead is tied
       * again once it is close. Returns whether the trader should wait for it.
       */
      private boolean keepStringTogether(ServerLevel level, Entity trader, boolean impatient) {
         Entity holder = trader;
         boolean waiting = false;

         for (UUID id : this.animals) {
            if (!(level.getEntity(id) instanceof AbstractHorse animal) || !animal.isAlive()) {
               continue;
            }

            double distance = animal.distanceTo(holder);
            // Stuck in plain sight for long, it is set behind all the same.
            if (distance > STRING_LAG && (impatient || level.getNearestPlayer(animal, HIDDEN_RANGE) == null)
               || impatient && distance > STRING_WAIT) {
               double dx = animal.getX() - holder.getX();
               double dz = animal.getZ() - holder.getZ();
               double length = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
               double x = holder.getX() + dx / length * 2.0;
               double z = holder.getZ() + dz / length * 2.0;
               animal.getNavigation().stop();
               animal.moveTo(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z)), z);
               distance = 2.0;
            } else if (distance > STRING_SLACK) {
               animal.getNavigation().moveTo(holder, 1.3);
            }

            if (!animal.isLeashed() && distance < STRING_SLACK) {
               animal.setLeashedTo(holder, true);
            }

            waiting |= distance > STRING_WAIT;
            holder = animal;
         }

         return waiting;
      }

      /** A cart broken open (not merely left behind) by a player near it is robbery. */
      private void checkCarts(ServerLevel level) {
         for (int i = this.carts.size() - 1; i >= 0; i--) {
            // Held, not looked up: a cart broken apart is gone from the world but still says how it went.
            ChestPushcartEntity cart = this.carts.get(i);
            if (cart.isAlive()) {
               continue;
            }

            if (cart.getRemovalReason() == Entity.RemovalReason.KILLED) {
               Player robber = level.getNearestPlayer(cart, ROBBER_RANGE);
               if (robber != null && !robber.isCreative()) {
                  robbed(level, this, robber);
               }
            }

            this.carts.remove(i);
            this.pullers.remove(i);
         }
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

         this.checkCarts(level);
         boolean waiting = this.keepStringTogether(level, traderNpc, this.waits > STRING_PATIENCE);
         this.waits = waiting ? this.waits + 1 : 0;

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

         if (waiting) {
            // The string of animals has fallen behind: the trader waits for it.
            traderNpc.getNavigation().stop();
            this.stuck = 0;
         } else if (traderNpc.getTarget() == null && (next || traderNpc.getNavigation().isDone())) {
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
         double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         npc.moveTo(x + 0.5, y, z + 0.5);
         this.stuck = 0;

         // The string of pack animals comes along, or its leads would snap.
         for (UUID id : this.animals) {
            if (level.getEntity(id) instanceof AbstractHorse animal && animal.isAlive()) {
               animal.getNavigation().stop();
               animal.moveTo(x + 0.5, y, z + 0.5);
            }
         }

         for (ChestPushcartEntity cart : this.carts) {
            if (cart.isAlive()) {
               cart.moveTo(x + 0.5, y, z + 0.5);
            }
         }
      }

      void discard(ServerLevel level) {
         List<UUID> all = new ArrayList<>(this.guards);
         all.addAll(this.bandits);
         all.addAll(this.animals);
         this.carts.forEach(Entity::discard);
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

            if (FactionDiplomacy.isOutlaw(settlement.faction()) && FactionBandits.level(settlement) == 1) {
               // Only small lairs prey on caravans; bigger ones raid posts and villages.
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
