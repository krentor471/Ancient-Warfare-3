package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;
import net.shadowmage.ancientwarfare.core.entity.BallistaBoatEntity;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * War at sea. A country with a port at war with a country whose places lie on the coast may
 * fit out boats instead of marching: a ballista boat leads, transports carry the soldiers, and
 * the fleet sails straight across the water to a beach near its target, lands and plunders.
 * Pirates sail the same way. Near a player the fleet sails as real boats, which can be fought
 * on the water; out of sight it is a point on the map like an army.
 */
public final class FactionFleets {
   private static final double FLEET_CHANCE = 0.12;
   /** Farther than this from any of its ports a country does not sail; nearer than the second it marches. */
   private static final int RANGE = 2400;
   private static final int MIN_SEA_ROUTE = 256;
   private static final int PIRATE_MIN_SEA_ROUTE = 96;
   private static final int MIN_CREW = 8;
   private static final int MAX_CREW = 24;
   /** Gold for fitting out the boats. */
   private static final int SHIP_COST = 20;
   /** Blocks per second under sail, a little slower than the boats themselves so they keep up. */
   private static final double SPEED = 2.4;
   private static final double LAND_RANGE = 10.0;
   private static final double SHOW_RANGE = 128.0;
   private static final double HIDE_RANGE = 192.0;
   private static final int MAX_BOATS = 9;
   /** Ring radii and spokes along which a settlement's shore is looked for. */
   private static final int[] RINGS = {24, 48, 80, 112};
   private static final int SPOKES = 16;
   /** Beyond the first sea found, this much farther out, so the boats have water under them. */
   private static final int OFFSHORE = 12;
   private static final Map<Long, Optional<BlockPos>> COASTS = new HashMap<>();
   /** Each boat's place in the formation, by campaign. */
   private static final Map<Integer, List<UUID>> FORMATION = new HashMap<>();

   private FactionFleets() {
   }

   /** Countries at war with someone across the water perhaps put to sea. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      RandomSource random = level.getRandom();
      Set<String> factions = new LinkedHashSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            factions.add(settlement.faction());
         }
      }

      for (String faction : factions) {
         if (random.nextDouble() >= FLEET_CHANCE || data.treasury(faction) < SHIP_COST || sailing(data, faction)) {
            continue;
         }

         FactionWorldData.Settlement bestHome = null;
         FactionWorldData.Settlement bestTarget = null;
         double best = (double)RANGE * RANGE;
         for (FactionWorldData.Settlement home : data.settlements()) {
            if (!home.faction().equals(faction) || home.garrison() < MIN_CREW * 2 || coast(level, home) == null) {
               continue;
            }

            for (FactionWorldData.Settlement target : data.settlements()) {
               if (target.faction().equals(faction)
                  || FactionDiplomacy.isOutlaw(target.faction())
                  || FactionAI.isFort(target)
                  || FactionTowns.CAPITAL.equals(target.kind())
                  || FactionDiplomacy.relation(level, faction, target.faction()).state() != RelationState.WAR
                  || underAttack(data, target)) {
                  continue;
               }

               double distance = home.horizontalDistanceSquared(target.center().getX(), target.center().getZ());
               if (distance < best && distance > (double)MIN_SEA_ROUTE * MIN_SEA_ROUTE && coast(level, target) != null) {
                  best = distance;
                  bestHome = home;
                  bestTarget = target;
               }
            }
         }

         if (bestHome == null) {
            continue;
         }

         int crew = Math.min(MAX_CREW, bestHome.garrison() / 2);
         data.setTreasury(faction, data.treasury(faction) - SHIP_COST);
         FactionCampaigns.sail(level, data, bestHome, bestTarget, crew, coast(level, bestHome), coast(level, bestTarget), true);
      }
   }

   /** A pirate lair raiding a place on the coast goes by sea; returns the fleet, or null to march. */
   static FactionWorldData.Campaign pirateRaid(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement lair, FactionWorldData.Settlement target, int force) {
      if (!"pirate".equals(lair.faction()) || force <= 0 || force > lair.garrison()) {
         return null;
      }

      BlockPos port = coast(level, lair);
      BlockPos landing = port == null ? null : coast(level, target);
      if (landing == null || lair.horizontalDistanceSquared(target.center().getX(), target.center().getZ()) < (double)PIRATE_MIN_SEA_ROUTE * PIRATE_MIN_SEA_ROUTE) {
         return null;
      }

      return FactionCampaigns.sail(level, data, lair, target, force, port, landing, false);
   }

   /**
    * For the operator: a fleet of the faction puts out from the given point of the sea against the
    * target, landing on its shore, or short of it where it has none.
    */
   public static FactionWorldData.Campaign sendFrom(ServerLevel level, FactionWorldData data, String faction, FactionWorldData.Settlement target, int size, BlockPos port) {
      FactionWorldData.Settlement home = null;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction) && (home == null || settlement.garrison() > home.garrison())) {
            home = settlement;
         }
      }

      if (home == null) {
         return null;
      }

      BlockPos landing = coast(level, target);
      if (landing == null) {
         double dx = target.center().getX() - port.getX();
         double dz = target.center().getZ() - port.getZ();
         double distance = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
         double back = Math.min(40.0, distance);
         landing = BlockPos.containing(target.center().getX() - dx / distance * back, port.getY(), target.center().getZ() - dz / distance * back);
      }

      home.garrison += size;
      return FactionCampaigns.sail(level, data, home, target, size, port, landing, true);
   }

   private static boolean sailing(FactionWorldData data, String faction) {
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.naval() && campaign.faction().equals(faction)) {
            return true;
         }
      }

      return false;
   }

   private static boolean underAttack(FactionWorldData data, FactionWorldData.Settlement settlement) {
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.targetSettlement() == settlement.id()) {
            return true;
         }
      }

      return false;
   }

   /** A point offshore near the settlement, or null when it lies inland. */
   public static BlockPos coast(ServerLevel level, FactionWorldData.Settlement settlement) {
      BlockPos center = settlement.center();
      return COASTS.computeIfAbsent(center.asLong(), key -> findCoast(level, center)).orElse(null);
   }

   private static Optional<BlockPos> findCoast(ServerLevel level, BlockPos center) {
      for (int radius : RINGS) {
         for (int i = 0; i < SPOKES; i++) {
            double angle = Math.PI * 2 * i / SPOKES;
            int x = center.getX() + (int)Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int)Math.round(Math.sin(angle) * radius);
            if (level.getUncachedNoiseBiome(x >> 2, 64 >> 2, z >> 2).is(BiomeTags.IS_OCEAN)) {
               int out = radius + OFFSHORE;
               return Optional.of(
                  new BlockPos(center.getX() + (int)Math.round(Math.cos(angle) * out), level.getSeaLevel(), center.getZ() + (int)Math.round(Math.sin(angle) * out))
               );
            }
         }
      }

      return Optional.empty();
   }

   /** One step of a fleet at sea; the crew are the campaign's NPCs aboard. */
   static void sail(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign, List<FactionNpcEntity> crew, double seconds) {
      double dx = campaign.landX() - campaign.x;
      double dz = campaign.landZ() - campaign.z;
      double distance = Math.sqrt(dx * dx + dz * dz);
      List<ChestPushcartEntity> ships = ships(level, campaign);
      if (distance <= LAND_RANGE) {
         land(level, data, campaign, ships);
         return;
      }

      double step = Math.min(distance, SPEED * seconds);
      campaign.x += dx / distance * step;
      campaign.z += dz / distance * step;
      data.setDirty();
      Player near = level.getNearestPlayer(campaign.x, level.getSeaLevel(), campaign.z, HIDE_RANGE, false);
      boolean show = near != null && near.distanceToSqr(campaign.x, near.getY(), campaign.z) <= SHOW_RANGE * SHOW_RANGE;
      if (ships.isEmpty() && crew.isEmpty() && show) {
         launchShips(level, campaign, dx / distance, dz / distance);
      } else if (near == null && (!ships.isEmpty() || !crew.isEmpty())) {
         crew.forEach(FactionNpcEntity::discard);
         scuttle(level, campaign);
         campaign.walking = 0;
      }
   }

   /** The fleet reaches its beach: the soldiers go ashore and march the rest of the way. */
   private static void land(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign, List<ChestPushcartEntity> ships) {
      for (ChestPushcartEntity ship : ships) {
         ship.ejectPassengers();
         ship.discard();
      }

      FORMATION.remove(campaign.id());
      campaign.naval = false;
      data.setDirty();
      AncientWarfareCore.LOG.info("Fleet #{} of {} lands {} soldiers", campaign.id(), campaign.faction(), campaign.size());
      FactionCampaigns.tellNear(
         level, campaign.x, campaign.z, Component.translatable("news.ancientwarfare.fleet_landed", FactionNews.factionName(campaign.faction()), campaign.size())
      );
   }

   private static void launchShips(ServerLevel level, FactionWorldData.Campaign campaign, double dirX, double dirZ) {
      Set<UUID> column = FactionCampaigns.newColumn(campaign.id());
      List<UUID> formation = new ArrayList<>();
      FORMATION.put(campaign.id(), formation);
      RandomSource random = level.getRandom();
      float yaw = (float)(Mth.atan2(-dirX, dirZ) * Mth.RAD_TO_DEG);
      int boats = Math.min(MAX_BOATS, campaign.size);

      for (int slot = 0; slot < boats; slot++) {
         double[] at = slot(campaign, slot, dirX, dirZ);
         int x = Mth.floor(at[0]);
         int z = Mth.floor(at[1]);
         if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            continue;
         }

         int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
         if (!level.getFluidState(new BlockPos(x, surface - 1, z)).is(FluidTags.WATER)) {
            continue;
         }

         EntityType<? extends ChestPushcartEntity> boatType = slot == 0 ? AWCoreEntityTypes.BALLISTA_BOAT.get() : AWCoreEntityTypes.TRANSPORT_BOAT.get();
         EntityType<? extends FactionNpcEntity> crewType = slot == 0
            ? AWCoreEntityTypes.FACTION_SIEGE_ENGINEER_NPC.get()
            : random.nextInt(3) == 0 ? AWCoreEntityTypes.FACTION_ARCHER_NPC.get() : AWCoreEntityTypes.FACTION_SOLDIER_NPC.get();
         ChestPushcartEntity boat = boatType.create(level);
         FactionNpcEntity npc = crewType.create(level);
         if (boat == null || npc == null) {
            continue;
         }

         boat.moveTo(x + 0.5, surface - 0.4, z + 0.5, yaw, 0.0F);
         boat.addTag(campaign.tag());
         column.add(boat.getUUID());
         formation.add(boat.getUUID());
         level.addFreshEntity(boat);
         npc.moveTo(x + 0.5, surface, z + 0.5, yaw, 0.0F);
         npc.finalizeSpawn(level, level.getCurrentDifficultyAt(boat.blockPosition()), MobSpawnType.EVENT, null, null);
         npc.setFactionName(campaign.faction());
         npc.addTag(campaign.tag());
         column.add(npc.getUUID());
         campaign.walking++;
         level.addFreshEntity(npc);
         npc.startRiding(boat, true);
      }
   }

   /** Where a boat keeps station: the ballista boat leads, the transports follow three abreast. */
   private static double[] slot(FactionWorldData.Campaign campaign, int slot, double dirX, double dirZ) {
      int row = slot == 0 ? 0 : (slot + 2) / 3;
      int side = slot == 0 ? 0 : (slot + 2) % 3 - 1;
      return new double[]{campaign.x - dirX * row * 6 - dirZ * side * 5, campaign.z - dirZ * row * 6 + dirX * side * 5};
   }

   /** Every tick: the boats of a fleet in sight steer for their stations. */
   static void steer(ServerLevel level, FactionWorldData data) {
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         List<UUID> formation = FORMATION.get(campaign.id());
         if (!campaign.naval() || formation == null) {
            continue;
         }

         double dx = campaign.landX() - campaign.x;
         double dz = campaign.landZ() - campaign.z;
         double distance = Math.max(1.0E-3, Math.sqrt(dx * dx + dz * dz));
         for (ChestPushcartEntity ship : ships(level, campaign)) {
            int slot = formation.indexOf(ship.getUUID());
            if (slot < 0 || ship.getControllingPassenger() == null) {
               continue;
            }

            if (ship instanceof BallistaBoatEntity && ship.getControllingPassenger() instanceof FactionNpcEntity engineer && engineer.getTarget() != null) {
               // Its engineer has an enemy in sight and works the ballista himself.
               continue;
            }

            double[] at = slot(campaign, slot, dx / distance, dz / distance);
            // A little ahead of the station, so the boats keep moving with the fleet.
            double toX = at[0] + dx / distance * 4 - ship.getX();
            double toZ = at[1] + dz / distance * 4 - ship.getZ();
            double away = Math.sqrt(toX * toX + toZ * toZ);
            float wanted = (float)(Mth.atan2(-toX, toZ) * Mth.RAD_TO_DEG);
            float turn = Mth.wrapDegrees(wanted - ship.getYRot());
            float forward = away < 2.0 ? 0.0F : Math.abs(turn) > 70.0F ? 0.2F : away > 10.0 ? 1.0F : 0.7F;
            ship.setVehicleInput(forward, Mth.clamp(-turn / 6.0F, -1.0F, 1.0F));
         }
      }
   }

   /** Removes the fleet's boats from the world. */
   static void scuttle(ServerLevel level, FactionWorldData.Campaign campaign) {
      for (ChestPushcartEntity ship : ships(level, campaign)) {
         ship.ejectPassengers();
         ship.discard();
      }

      FORMATION.remove(campaign.id());
   }

   private static List<ChestPushcartEntity> ships(ServerLevel level, FactionWorldData.Campaign campaign) {
      if (!FORMATION.containsKey(campaign.id())) {
         return new ArrayList<>();
      }

      AABB area = new AABB(campaign.x - HIDE_RANGE, level.getMinBuildHeight(), campaign.z - HIDE_RANGE, campaign.x + HIDE_RANGE, level.getMaxBuildHeight(), campaign.z + HIDE_RANGE);
      return new ArrayList<>(level.getEntitiesOfClass(ChestPushcartEntity.class, area, ship -> ship.isAlive() && ship.getTags().contains(campaign.tag())));
   }

   /** A fleet's boat saved with the world from an earlier voyage: it does not come back. */
   public static boolean isStaleShip(Entity entity) {
      for (String tag : entity.getTags()) {
         if (tag.startsWith(FactionCampaigns.TAG_PREFIX)) {
            try {
               return !FactionCampaigns.inColumn(Integer.parseInt(tag.substring(FactionCampaigns.TAG_PREFIX.length())), entity.getUUID());
            } catch (NumberFormatException ignored) {
               return true;
            }
         }
      }

      return false;
   }
}
