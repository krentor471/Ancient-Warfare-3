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
 * plague; a patrol of soldiers keeping the road, a shepherd driving his flock, a hunter, a knight
 * errant looking for a fair fight, a tax collector under guard and a wedding procession. They walk
 * the road from one end toward the other and, stopped and spoken to, say who they are and pass on
 * the news of the land (see FactionRumours). Some offer more: the knight a duel, the hunter his
 * hides and meat, refugees a new home in a player's village. Some are worth robbing: a messenger's
 * letter tells the plans of his country, a tax collector carries silver, a flock can be driven
 * off; the country does not forget it. Like caravans, they live only while a player is near and
 * are not saved.
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
      REFUGEES,
      PATROL,
      SHEPHERD,
      HUNTER,
      KNIGHT,
      TAX_COLLECTOR,
      PROCESSION;
   }

   /** How often each peaceful kind takes the road, in the order of Kind from PILGRIMS; refugees come by their own chance. */
   private static final double[] KIND_WEIGHTS = {3, 3, 2.5, 0, 3, 2.5, 1.5, 1.2, 1.5, 1.2};
   private static final int DUEL_REWARD = 20;
   private static final int DUEL_LOSS = 10;
   private static final int DUEL_STANDING = 4;
   /** A duel ends when either side is down to this share of its health; nobody dies of honour. */
   private static final float DUEL_YIELD = 0.35F;
   private static final double DUEL_RANGE = 24.0;
   private static final int TAX_SILVER = 30;
   private static final int ROB_STANDING = -15;
   private static final int LETTER_STANDING = -6;
   private static final int HERD_STANDING = -4;
   private static final int REFUGE_STANDING = 4;
   private static final int HIDES_PRICE = 3;
   private static final int MEAT_PRICE = 2;
   private static final double OFFER_RANGE = 6.0;

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
      switch (group.kind) {
         case KNIGHT -> offer(player, group, "duel");
         case HUNTER -> {
            offer(player, group, "hides");
            offer(player, group, "meat");
         }
         case REFUGEES -> {
            if (ownVillage(data, player) != null) {
               offer(player, group, "refuge");
            }
         }
         default -> FactionRumours.chat(player, npc);
      }
   }

   private static void offer(ServerPlayer player, Group group, String action) {
      player.sendSystemMessage(
         Component.literal("  [")
            .append(Component.translatable("traveller.ancientwarfare.offer." + action, HIDES_PRICE, MEAT_PRICE))
            .append("]")
            .withStyle(
               style -> style.withColor(net.minecraft.ChatFormatting.GREEN)
                  .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/awroad " + group.id + " " + action))
            )
      );
   }

   /** The hall of the player's village nearest to him, if he has one. */
   private static BlockPos ownVillage(FactionWorldData data, ServerPlayer player) {
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      for (Map.Entry<BlockPos, UUID> village : data.villages().entrySet()) {
         double distance = village.getKey().distSqr(player.blockPosition());
         if (player.getUUID().equals(village.getValue()) && distance < bestDistance) {
            best = village.getKey();
            bestDistance = distance;
         }
      }

      return best;
   }

   /** The player clicked one of a traveller's offers in chat. */
   public static void act(ServerPlayer player, int groupId, String action) {
      Group group = ACTIVE.get(groupId);
      ServerLevel level = player.serverLevel();
      Entity entity = group == null || group.leader == null ? null : level.getEntity(group.leader);
      if (!(entity instanceof FactionNpcEntity lead) || !lead.isAlive() || lead.distanceToSqr(player) > OFFER_RANGE * OFFER_RANGE * 4) {
         player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.gone"));
         return;
      }

      Component speaker = lead.getDisplayName();
      switch (action) {
         case "duel" -> {
            if (group.kind == Kind.KNIGHT && group.duelist == null) {
               group.duelist = player.getUUID();
               FactionRumours.say(player, speaker, Component.translatable("traveller.ancientwarfare.duel.begin"));
            }
         }
         case "hides", "meat" -> {
            if (group.kind != Kind.HUNTER) {
               return;
            }

            boolean hides = action.equals("hides");
            if (!Coins.take(player, hides ? HIDES_PRICE : MEAT_PRICE)) {
               player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver"));
               return;
            }

            net.minecraft.world.item.ItemStack goods = hides
               ? new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER, 4)
               : new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_BEEF, 6);
            if (!player.getInventory().add(goods)) {
               player.drop(goods, false);
            }

            FactionRumours.say(player, speaker, Component.translatable("traveller.ancientwarfare.sold"));
         }
         case "refuge" -> {
            FactionWorldData data = FactionWorldData.get(level);
            BlockPos hall = ownVillage(data, player);
            if (group.kind != Kind.REFUGEES || hall == null) {
               return;
            }

            // They take the player's word and go to his village: from now on they work for him.
            List<UUID> people = new ArrayList<>(group.followers);
            people.add(group.leader);
            int settled = 0;
            for (UUID id : people) {
               if (level.getEntity(id) instanceof FactionNpcEntity person && person.isAlive()) {
                  net.shadowmage.ancientwarfare.core.entity.WorkerNpcEntity worker = AWCoreEntityTypes.WORKER_NPC.get().create(level);
                  if (worker != null) {
                     worker.moveTo(person.getX(), person.getY(), person.getZ(), person.getYRot(), 0.0F);
                     worker.finalizeSpawn(level, level.getCurrentDifficultyAt(person.blockPosition()), MobSpawnType.EVENT, null, null);
                     worker.setOwner(player.getUUID());
                     worker.setPersistenceRequired();
                     level.addFreshEntity(worker);
                     settled++;
                  }

                  person.discard();
               }
            }

            ACTIVE.remove(group.id);
            net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData.get(level).adjustStanding(player.getUUID(), group.faction, REFUGE_STANDING);
            player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.refuge.taken", settled, hall.getX(), hall.getZ()).withStyle(net.minecraft.ChatFormatting.GREEN));
         }
         default -> {
         }
      }
   }

   /**
    * A blow in a knight's duel. The blow that would bring either side below its yielding point ends
    * the duel instead of landing; returns true when the blow is to be cancelled.
    */
   public static boolean onDamage(net.minecraft.world.entity.LivingEntity victim, Entity attacker, float amount) {
      for (Group group : ACTIVE.values()) {
         if (group.duelist == null || !(victim.level() instanceof ServerLevel level)) {
            continue;
         }

         Entity knight = level.getEntity(group.leader);
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(group.duelist);
         if (!(knight instanceof FactionNpcEntity lead) || player == null) {
            continue;
         }

         boolean knightHit = victim == lead && attacker == player;
         boolean playerHit = victim == player && attacker == lead;
         if ((knightHit || playerHit) && victim.getHealth() - amount <= victim.getMaxHealth() * DUEL_YIELD) {
            group.endDuel(level, lead, player, knightHit);
            return true;
         }
      }

      return false;
   }

   /** A player killed one of a group on the road: robbing the tax, reading the letter, taking from the flock. */
   public static void onKilled(ServerLevel level, Entity dead, ServerPlayer player) {
      Group group = ACTIVE.get(travellerId(dead));
      if (group == null) {
         return;
      }

      var standings = net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData.get(level);
      boolean leader = dead.getUUID().equals(group.leader);
      if (group.kind == Kind.TAX_COLLECTOR && leader) {
         for (net.minecraft.world.item.ItemStack coins : Coins.silver(TAX_SILVER)) {
            dead.spawnAtLocation(coins);
         }

         standings.adjustStanding(player.getUUID(), group.faction, ROB_STANDING);
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.tax_robbed", FactionNews.factionName(group.faction)));
      } else if (group.kind == Kind.MESSENGER && leader) {
         standings.adjustStanding(player.getUUID(), group.faction, LETTER_STANDING);
         player.sendSystemMessage(letter(level, group.faction).withStyle(net.minecraft.ChatFormatting.YELLOW));
      } else if (group.kind == Kind.SHEPHERD && !leader) {
         standings.adjustStanding(player.getUUID(), group.faction, HERD_STANDING);
      }
   }

   /** What a messenger's letter tells of his country's plans: its armies and marches, or nothing of note. */
   private static net.minecraft.network.chat.MutableComponent letter(ServerLevel level, String faction) {
      FactionWorldData data = FactionWorldData.get(level);
      for (FactionWorldData.Army army : data.armies()) {
         FactionWorldData.Settlement target = data.settlement(army.targetSettlement());
         if (army.faction().equals(faction) && target != null) {
            return Component.translatable("traveller.ancientwarfare.letter.army", army.size(), FactionNews.named(target));
         }
      }

      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         FactionWorldData.Settlement target = data.settlement(campaign.targetSettlement());
         if (campaign.faction().equals(faction) && target != null) {
            return Component.translatable("traveller.ancientwarfare.letter.army", campaign.size(), FactionNews.named(target));
         }
      }

      return Component.translatable("traveller.ancientwarfare.letter.nothing", FactionNews.factionName(faction));
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

   /** For operators: sends travellers of a kind down a road near the player; false when no road will do. */
   public static boolean spawnNear(ServerLevel level, ServerPlayer player, String kindName) {
      Kind kind;
      try {
         kind = Kind.valueOf(kindName.toUpperCase(java.util.Locale.ROOT));
      } catch (IllegalArgumentException e) {
         return false;
      }

      int before = ACTIVE.size();
      trySpawn(level, FactionWorldData.get(level), player, level.getGameTime(), kind);
      return ACTIVE.size() > before;
   }

   private static void trySpawn(ServerLevel level, FactionWorldData data, ServerPlayer player, long now) {
      trySpawn(level, data, player, now, null);
   }

   private static void trySpawn(ServerLevel level, FactionWorldData data, ServerPlayer player, long now, Kind forced) {
      List<FactionWorldData.Route> routes = new ArrayList<>(data.routes());
      java.util.Collections.shuffle(routes, new java.util.Random(level.getRandom().nextLong()));
      for (FactionWorldData.Route route : routes) {
         if (route.road() != FactionWorldData.RoadState.DONE || forced == null && now - LAST_ON_ROUTE.getOrDefault(route.id(), Long.MIN_VALUE / 2) < ROUTE_COOLDOWN) {
            continue;
         }

         List<Long> points = route.points();
         int start = -1;
         boolean near = false;
         for (int i = 0; i < points.size(); i++) {
            double distance = distance(player, points.get(i));
            near |= distance < NOTICE_RANGE;
            if (start < 0 && distance >= MIN_SPAWN_DISTANCE && distance <= MAX_SPAWN_DISTANCE && i > 2 && i < points.size() - 3 && !FactionCaravans.inTown(data, points.get(i))) {
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
         if (forced != null) {
            kind = forced;
         } else if (struck && random.nextDouble() < REFUGEE_CHANCE) {
            kind = Kind.REFUGEES;
            // Those who fled a captured town are of the people who lost it.
            faction = from.capturedFrom().isEmpty() ? faction : from.capturedFrom();
         } else if (FactionDiplomacy.isOutlaw(faction)) {
            continue;
         } else {
            kind = pick(random);
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

   private static Kind pick(RandomSource random) {
      double total = 0.0;
      for (double weight : KIND_WEIGHTS) {
         total += weight;
      }

      double roll = random.nextDouble() * total;
      for (Kind kind : Kind.values()) {
         roll -= KIND_WEIGHTS[kind.ordinal()];
         if (roll < 0.0) {
            return kind;
         }
      }

      return Kind.PILGRIMS;
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
      /** The player a knight is fighting, while the duel lasts. */
      private UUID duelist;

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
            case PILGRIMS, PROCESSION -> AWCoreEntityTypes.FACTION_PRIEST_NPC.get();
            case PEDDLER -> AWCoreEntityTypes.FACTION_TRADER_NPC.get();
            case MESSENGER, PATROL -> AWCoreEntityTypes.FACTION_SOLDIER_NPC.get();
            case HUNTER -> AWCoreEntityTypes.FACTION_ARCHER_NPC.get();
            case KNIGHT -> AWCoreEntityTypes.FACTION_SOLDIER_ELITE_NPC.get();
            case TAX_COLLECTOR -> AWCoreEntityTypes.FACTION_CIVILIAN_MALE_NPC.get();
            case REFUGEES, SHEPHERD -> civilian(random);
         };
         FactionNpcEntity first = this.create(level, leaderType, x, z, this.kind == Kind.MESSENGER ? MESSENGER_SPEED : WALK_SPEED);
         if (first == null) {
            return false;
         }

         this.leader = first.getUUID();
         int followers = switch (this.kind) {
            case PILGRIMS -> 2 + random.nextInt(3);
            case REFUGEES -> 2 + random.nextInt(4);
            case PATROL -> 2 + random.nextInt(2);
            case SHEPHERD -> 3 + random.nextInt(4);
            case TAX_COLLECTOR -> 2;
            case PROCESSION -> 4 + random.nextInt(3);
            default -> 0;
         };
         for (int i = 0; i < followers; i++) {
            int fx = x - this.direction * (2 + i);
            int fz = z + (i % 2 == 0 ? 1 : -1);
            if (this.kind == Kind.SHEPHERD) {
               net.minecraft.world.entity.Mob beast = this.createBeast(level, random.nextInt(3) == 0 ? EntityType.COW : EntityType.SHEEP, fx, fz);
               if (beast != null) {
                  this.followers.add(beast.getUUID());
               }

               continue;
            }

            EntityType<? extends FactionNpcEntity> type = switch (this.kind) {
               case PATROL -> random.nextInt(3) == 0 ? AWCoreEntityTypes.FACTION_ARCHER_NPC.get() : AWCoreEntityTypes.FACTION_SOLDIER_NPC.get();
               case TAX_COLLECTOR -> AWCoreEntityTypes.FACTION_SOLDIER_NPC.get();
               default -> civilian(random);
            };
            FactionNpcEntity follower = this.create(level, type, fx, fz, WALK_SPEED);
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

      /** A beast of a shepherd's flock: it walks behind him and is gone with him. */
      private net.minecraft.world.entity.Mob createBeast(ServerLevel level, EntityType<? extends net.minecraft.world.entity.Mob> type, int x, int z) {
         if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
         }

         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         net.minecraft.world.entity.Mob beast = type.create(level);
         if (beast == null) {
            return null;
         }

         beast.moveTo(x + 0.5, y, z + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
         beast.addTag(TAG_PREFIX + this.id);
         beast.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null, null);
         level.addFreshEntity(beast);
         return beast;
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

         if (this.duelist != null) {
            this.duel(level, lead);
            return true;
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
            if (level.getEntity(id) instanceof net.minecraft.world.entity.Mob follower && follower.isAlive() && follower.getTarget() == null && !follower.isLeashed()) {
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

      /** One look at a knight's duel: it ends when either side has had enough, or the player walks off. */
      private void duel(ServerLevel level, FactionNpcEntity knight) {
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.duelist);
         if (player == null || !player.isAlive() || player.distanceToSqr(knight) > DUEL_RANGE * DUEL_RANGE) {
            knight.setTarget(null);
            this.duelist = null;
            return;
         }

         if (knight.getTarget() != player) {
            knight.setTarget(player);
         }
      }

      /** The duel is over: the knight yields and pays, or the player does. */
      private void endDuel(ServerLevel level, FactionNpcEntity knight, ServerPlayer player, boolean playerWon) {
         knight.setTarget(null);
         this.duelist = null;
         if (playerWon) {
            knight.setHealth(knight.getMaxHealth());
            Coins.give(player, DUEL_REWARD);
            net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData.get(level).adjustStanding(player.getUUID(), this.faction, DUEL_STANDING);
            FactionRumours.say(player, knight.getDisplayName(), Component.translatable("traveller.ancientwarfare.duel.won", DUEL_REWARD));
         } else {
            Coins.take(player, Math.min(DUEL_LOSS, Coins.purse(player)));
            FactionRumours.say(player, knight.getDisplayName(), Component.translatable("traveller.ancientwarfare.duel.lost", DUEL_LOSS));
         }
      }

      void discard(ServerLevel level) {
         List<UUID> all = new ArrayList<>(this.followers);
         if (this.leader != null) {
            all.add(this.leader);
         }

         for (UUID id : all) {
            Entity entity = level.getEntity(id);
            if (entity instanceof net.minecraft.world.entity.Mob beast && beast.isLeashed() && beast.getLeashHolder() instanceof ServerPlayer thief) {
               // A beast a player led away is his now, and the shepherd's country knows who took it.
               beast.removeTag(TAG_PREFIX + this.id);
               net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData.get(level).adjustStanding(thief.getUUID(), this.faction, HERD_STANDING);
            } else if (entity != null) {
               entity.discard();
            }
         }
      }
   }
}
