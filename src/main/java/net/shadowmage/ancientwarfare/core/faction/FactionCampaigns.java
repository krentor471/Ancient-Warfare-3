package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * Armies on the march. A faction at war gathers soldiers from its garrisons at the settlement
 * nearest its target and sends them across the land, a point on the map moving at walking pace.
 * Two hostile armies that meet fight it out; an army that reaches its target besieges it. Near a
 * player the column walks as real soldiers, who can be fought along the way, and at a target a
 * player is near the siege is fought by them too.
 */
public final class FactionCampaigns {
   public static final String TAG_PREFIX = "aw_campaign_";
   private static final int TICK_INTERVAL = 20;
   /** Blocks per second on the march, baggage and all. */
   private static final double SPEED = 1.5;
   private static final double ARRIVE_RANGE = 48.0;
   private static final double MEET_RANGE = 64.0;
   /** A player this close sees the column as real soldiers, and they go once the player is this far. */
   private static final double SHOW_RANGE = 112.0;
   private static final double HIDE_RANGE = 176.0;
   /** At most this many of the army walk as real NPCs; the rest are counted as following behind. */
   private static final int MAX_WALKING = 20;
   private static final double SIEGE_PLAYER_RANGE = 192.0;
   private static final int LEAD = 12;
   /** A soldier this far behind the army, with no player close enough to see, catches up at once. */
   private static final double STRAGGLE = 40.0;
   private static final double HIDDEN_RANGE = 24.0;
   private static final double WAIT_DISTANCE = 48.0;
   /** Marching soldiers spawned since the server started. */
   private static final Map<Integer, Set<UUID>> SPAWNED = new HashMap<>();

   private FactionCampaigns() {
   }

   public static int campaignId(FactionNpcEntity npc) {
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

   /**
    * Whether a marching soldier entering the world is one of its column's current soldiers; older
    * ones (saved with the world, or replaced while their land was unloaded) go.
    */
   static boolean stillMarching(FactionNpcEntity npc) {
      return SPAWNED.getOrDefault(campaignId(npc), Set.of()).contains(npc.getUUID());
   }

   static boolean marchingAgainst(FactionWorldData data, String attacker, String defender) {
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.faction().equals(attacker) && campaign.defender().equals(defender)) {
            return true;
         }
      }

      return false;
   }

   /** Gathers force soldiers at the attacker's settlement nearest the target and sets out. Returns false when nobody could go. */
   public static boolean launch(ServerLevel level, FactionWorldData data, String attacker, FactionWorldData.Settlement target, int force) {
      FactionWorldData.Settlement home = null;
      double best = Double.MAX_VALUE;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         double distance = settlement.horizontalDistanceSquared(target.center().getX(), target.center().getZ());
         if (settlement.faction().equals(attacker) && distance < best) {
            best = distance;
            home = settlement;
         }
      }

      if (home == null) {
         return false;
      }

      int taken = FactionAI.takeLosses(data, attacker, force);
      if (taken <= 0) {
         return false;
      }

      start(level, data, home, target, taken);
      return true;
   }

   /** Sends force soldiers of one settlement's own garrison out against the target; null when it has too few. */
   static FactionWorldData.Campaign launchFrom(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement home, FactionWorldData.Settlement target, int force) {
      int taken = Math.min(force, home.garrison);
      if (taken <= 0) {
         return null;
      }

      home.garrison -= taken;
      return start(level, data, home, target, taken);
   }

   private static FactionWorldData.Campaign start(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement home, FactionWorldData.Settlement target, int taken) {
      String attacker = home.faction();
      FactionWorldData.Campaign campaign = data.addCampaign(
         attacker, target.faction(), target.id(), home.center().getX() + 0.5, home.center().getZ() + 0.5, taken, level.getGameTime()
      );
      AncientWarfareCore.LOG
         .info("Faction {} marches {} soldiers from #{} against {} #{} (army #{})", attacker, taken, home.id(), target.faction(), target.id(), campaign.id());
      tellNear(
         level,
         home.center().getX(),
         home.center().getZ(),
         Component.translatable("news.ancientwarfare.army_set_out", FactionNews.factionName(attacker), taken, FactionNews.factionName(target.faction()))
      );
      return campaign;
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % TICK_INTERVAL != 0L || data.campaigns().isEmpty()) {
         return;
      }

      for (FactionWorldData.Campaign campaign : new ArrayList<>(data.campaigns())) {
         if (data.campaigns().contains(campaign)) {
            advance(level, data, campaign);
         }
      }
   }

   private static void advance(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign) {
      FactionWorldData.Settlement target = settlement(data, campaign.targetSettlement());
      List<FactionNpcEntity> walkers = walkers(level, campaign);
      countFallen(level, data, campaign, walkers);
      if (campaign.size <= 0) {
         end(level, data, campaign, walkers, 0);
         tellNear(level, campaign.x, campaign.z, Component.translatable("news.ancientwarfare.army_destroyed", FactionNews.factionName(campaign.faction())));
         return;
      }

      if (target == null
         || !target.faction().equals(campaign.defender())
         || FactionDiplomacy.relation(level, campaign.faction(), campaign.defender()).state() != RelationState.WAR) {
         // Taken by someone else, or peace was made: the army goes home.
         end(level, data, campaign, walkers, campaign.size);
         return;
      }

      double dx = target.center().getX() + 0.5 - campaign.x;
      double dz = target.center().getZ() + 0.5 - campaign.z;
      double distance = Math.sqrt(dx * dx + dz * dz);
      if (distance <= ARRIVE_RANGE) {
         arrive(level, data, campaign, target, walkers);
         return;
      }

      // The army keeps its pace; real soldiers walk after it, and stragglers out of sight catch up.
      // If the whole column is held up where a player can see it, the army waits for it.
      boolean waiting = !walkers.isEmpty()
         && walkers.stream().allMatch(npc -> npc.distanceToSqr(campaign.x, npc.getY(), campaign.z) > WAIT_DISTANCE * WAIT_DISTANCE);
      if (!waiting) {
         double step = Math.min(distance, SPEED * TICK_INTERVAL / 20.0);
         campaign.x += dx / distance * step;
         campaign.z += dz / distance * step;
      }
      data.setDirty();
      if (meetEnemy(level, data, campaign)) {
         return;
      }

      showOrHide(level, data, campaign, walkers, dx / distance, dz / distance);
   }

   /** A soldier of a marching column fell: the army is one fewer. */
   static void onNpcDeath(FactionWorldData data, FactionNpcEntity npc) {
      int id = campaignId(npc);
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.id() == id) {
            campaign.size--;
            campaign.walking--;
            data.setDirty();
            return;
         }
      }
   }

   /** Soldiers of the column who left the world unseen (their land unloaded) are no longer out walking. */
   private static void countFallen(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign, List<FactionNpcEntity> walkers) {
      campaign.walking = walkers.size();
   }

   /** Shows the column as real soldiers near a player, hides it again once nobody is near. */
   private static void showOrHide(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign, List<FactionNpcEntity> walkers, double dirX, double dirZ) {
      Player near = level.getNearestPlayer(campaign.x, level.getSeaLevel(), campaign.z, HIDE_RANGE, false);
      boolean show = near != null && near.distanceToSqr(campaign.x, near.getY(), campaign.z) <= SHOW_RANGE * SHOW_RANGE;
      if (walkers.isEmpty() && show && level.hasChunkAt(BlockPos.containing(campaign.x, 0, campaign.z))) {
         int wanted = Math.min(MAX_WALKING, campaign.size);
         Set<UUID> column = new HashSet<>();
         SPAWNED.put(campaign.id(), column);
         RandomSource random = level.getRandom();

         for (int i = 0; i < wanted * 3 && walkers.size() < wanted; i++) {
            // A column: in pairs, stretched back along the way it came.
            int row = walkers.size() / 2;
            int side = walkers.size() % 2 == 0 ? -1 : 1;
            int x = Mth.floor(campaign.x - dirX * row * 2 - dirZ * side * 1.5 + random.nextInt(3) - 1);
            int z = Mth.floor(campaign.z - dirZ * row * 2 + dirX * side * 1.5 + random.nextInt(3) - 1);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
               continue;
            }

            campaign.walking++;
            FactionNpcEntity npc = FactionSimulation.spawnSoldier(level, campaign.faction(), x, z);
            if (npc == null) {
               campaign.walking--;
            } else {
               npc.addTag(campaign.tag());
               column.add(npc.getUUID());
               walkers.add(npc);
            }
         }

         return;
      }

      if (!walkers.isEmpty() && near == null) {
         walkers.forEach(FactionNpcEntity::discard);
         campaign.walking = 0;
         return;
      }

      for (FactionNpcEntity npc : walkers) {
         if (npc.getTarget() != null) {
            continue;
         }

         double behind = Math.sqrt(npc.distanceToSqr(campaign.x, npc.getY(), campaign.z));
         if (behind > STRAGGLE && level.getNearestPlayer(npc, HIDDEN_RANGE) == null) {
            int x = Mth.floor(campaign.x - dirX * 4 + level.getRandom().nextInt(5) - 2);
            int z = Mth.floor(campaign.z - dirZ * 4 + level.getRandom().nextInt(5) - 2);
            npc.getNavigation().stop();
            npc.moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
         } else if (npc.getNavigation().isDone() || npc.tickCount % 3 == 0) {
            // Toward a point a little ahead of the army, so the column keeps moving.
            int x = Mth.floor(campaign.x + dirX * LEAD);
            int z = Mth.floor(campaign.z + dirZ * LEAD);
            npc.getNavigation().moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, behind > 12.0 ? 1.2 : 0.9);
         }
      }
   }

   /** Two hostile armies within reach of each other fight; the smaller side is worn down more. */
   private static boolean meetEnemy(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign) {
      for (FactionWorldData.Campaign other : data.campaigns()) {
         if (other == campaign
            || other.faction().equals(campaign.faction())
            || FactionDiplomacy.relation(level, campaign.faction(), other.faction()).state() != RelationState.WAR) {
            continue;
         }

         double dx = other.x - campaign.x;
         double dz = other.z - campaign.z;
         if (dx * dx + dz * dz > MEET_RANGE * MEET_RANGE) {
            continue;
         }

         if (!walkers(level, campaign).isEmpty() || !walkers(level, other).isEmpty()) {
            // Real soldiers on the field fight it out themselves.
            return false;
         }

         RandomSource random = level.getRandom();
         int mine = campaign.size;
         int theirs = other.size;
         campaign.size -= Math.min(mine, (int)Math.round(theirs * (0.4 + random.nextDouble() * 0.4)));
         other.size -= Math.min(theirs, (int)Math.round(mine * (0.4 + random.nextDouble() * 0.4)));
         FactionWorldData.Campaign loser = campaign.size * theirs < other.size * mine ? campaign : other;
         FactionWorldData.Campaign winner = loser == campaign ? other : campaign;
         FactionNews.tell(
            level,
            Component.translatable(
               "news.ancientwarfare.field_battle",
               FactionNews.factionName(winner.faction()),
               FactionNews.factionName(loser.faction()),
               Mth.floor(campaign.x),
               Mth.floor(campaign.z)
            )
         );
         // The beaten army falls back home with what is left of it.
         end(level, data, loser, List.of(), Math.max(0, loser.size));
         if (winner.size <= 0) {
            end(level, data, winner, List.of(), 0);
         }

         data.setDirty();
         return loser == campaign || campaign.size <= 0;
      }

      return false;
   }

   /** At the target: with a player near, real soldiers storm it; otherwise the siege is reckoned. */
   private static void arrive(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign, FactionWorldData.Settlement target, List<FactionNpcEntity> walkers) {
      BlockPos center = target.center();
      boolean watched = target.built() && level.hasNearbyAlivePlayer(center.getX(), center.getY(), center.getZ(), SIEGE_PLAYER_RANGE);
      if (watched) {
         int storming = walkers.isEmpty() ? Math.min(MAX_WALKING, campaign.size) : walkers.size();
         FactionWorldData.Army army = data.addArmy(campaign.faction(), campaign.defender(), target.id(), null, center, storming, level.getGameTime());
         if (!walkers.isEmpty()) {
            // The column marches straight into the assault.
            for (FactionNpcEntity npc : walkers) {
               npc.removeTag(campaign.tag());
               npc.addTag(army.tag());
               npc.setPersistenceRequired();
            }

            army.spawned = true;
            army.started = level.getGameTime();
            army.alive = walkers.size();
         }

         campaign.walking = 0;
         end(level, data, campaign, List.of(), campaign.size - storming);
         return;
      }

      walkers.forEach(FactionNpcEntity::discard);
      campaign.walking = 0;
      boolean bandits = FactionDiplomacy.isOutlaw(campaign.faction());
      // Bandits do not hold what they raid: they plunder it and go.
      int survivors = FactionAI.siege(level, data, level.getRandom(), campaign.faction(), target, campaign.size, !bandits);
      if (bandits && target.garrison <= 0 && survivors > 0) {
         FactionBandits.plunder(level, data, campaign.faction(), target, FactionBandits.raidLevel(campaign.id(), data));
      }

      end(level, data, campaign, List.of(), survivors);
   }

   /** The campaign is over: its soldiers on the road leave the world and homecomers rejoin the garrisons. */
   private static void end(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign, List<FactionNpcEntity> walkers, int homecomers) {
      walkers.forEach(FactionNpcEntity::discard);
      for (FactionNpcEntity npc : walkers(level, campaign)) {
         npc.discard();
      }

      campaign.walking = 0;
      SPAWNED.remove(campaign.id());
      data.removeCampaign(campaign);
      FactionAI.returnSoldiers(data, campaign.faction(), homecomers);
   }

   private static List<FactionNpcEntity> walkers(ServerLevel level, FactionWorldData.Campaign campaign) {
      if (!SPAWNED.containsKey(campaign.id())) {
         return new ArrayList<>();
      }

      AABB area = new AABB(campaign.x - HIDE_RANGE, level.getMinBuildHeight(), campaign.z - HIDE_RANGE, campaign.x + HIDE_RANGE, level.getMaxBuildHeight(), campaign.z + HIDE_RANGE);
      return new ArrayList<>(level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getTags().contains(campaign.tag())));
   }

   private static FactionWorldData.Settlement settlement(FactionWorldData data, int id) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.id() == id) {
            return settlement;
         }
      }

      return null;
   }

   private static void tellNear(ServerLevel level, double x, double z, Component message) {
      if (FactionHistory.running()) {
         return;
      }

      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(x, player.getY(), z) < 512.0 * 512.0) {
            player.sendSystemMessage(message);
         }
      }
   }
}
