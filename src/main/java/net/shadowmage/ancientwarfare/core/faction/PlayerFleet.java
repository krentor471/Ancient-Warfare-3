package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.shadowmage.ancientwarfare.core.entity.BallistaBoatEntity;
import net.shadowmage.ancientwarfare.core.entity.CatapultBoatEntity;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.TransportBoatEntity;
import net.shadowmage.ancientwarfare.core.research.PlayerResearch;

/**
 * The player's own fleet, opened by the landing research. A player who takes the helm of a boat
 * calls their soldiers standing near aboard the empty boats lying about, one to a boat; those boats
 * then keep station behind the player's, the way a faction's fleet sails. When the player steps
 * ashore, every soldier of the fleet leaves their boat and the landing party follows them in.
 */
public final class PlayerFleet {
   /** How far from the player soldiers are called aboard and empty boats are manned. */
   private static final double MUSTER_RANGE = 16.0;
   /** Beyond this a boat has lost the fleet and its soldier stays where they are. */
   private static final double LOST_RANGE = 96.0;
   private static final int MAX_BOATS = 9;
   private static final int MUSTER_TICKS = 20;
   private static final Map<UUID, List<UUID>> FLEETS = new HashMap<>();

   private PlayerFleet() {
   }

   public static boolean isBoat(Entity entity) {
      return entity instanceof TransportBoatEntity || entity instanceof BallistaBoatEntity || entity instanceof CatapultBoatEntity;
   }

   public static void tick(MinecraftServer server) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         List<UUID> fleet = FLEETS.get(player.getUUID());
         Entity flagship = player.getVehicle();
         boolean afloat = isBoat(flagship) && flagship.isInWater();
         if (!isBoat(flagship)) {
            if (fleet != null) {
               land(player, fleet);
            }

            continue;
         }

         if (!PlayerResearch.has(player, PlayerResearch.LANDING)) {
            continue;
         }

         if (fleet == null) {
            fleet = new ArrayList<>();
            FLEETS.put(player.getUUID(), fleet);
         }

         if (server.getTickCount() % MUSTER_TICKS == 0 && fleet.size() < MAX_BOATS) {
            muster(player, (ChestPushcartEntity)flagship, fleet);
         }

         if (afloat) {
            steer(player.serverLevel(), (ChestPushcartEntity)flagship, fleet);
         }
      }

      FLEETS.keySet().removeIf(owner -> server.getPlayerList().getPlayer(owner) == null);
   }

   /** Soldiers near the player board the empty boats near the player, nearest soldier first. */
   private static void muster(ServerPlayer player, ChestPushcartEntity flagship, List<UUID> fleet) {
      ServerLevel level = player.serverLevel();
      List<CombatNpcEntity> soldiers = level.getEntitiesOfClass(
         CombatNpcEntity.class,
         player.getBoundingBox().inflate(MUSTER_RANGE, 6.0, MUSTER_RANGE),
         npc -> npc.isAlive() && !npc.isPassenger() && npc.getTarget() == null && npc.getOwner().map(player.getUUID()::equals).orElse(false)
      );
      if (soldiers.isEmpty()) {
         return;
      }

      List<ChestPushcartEntity> boats = level.getEntitiesOfClass(
         ChestPushcartEntity.class,
         player.getBoundingBox().inflate(MUSTER_RANGE, 6.0, MUSTER_RANGE),
         boat -> boat != flagship && boat.isAlive() && isBoat(boat) && !boat.isVehicle()
      );
      soldiers.sort(Comparator.comparingDouble(player::distanceToSqr));
      int boarded = 0;
      for (CombatNpcEntity soldier : soldiers) {
         if (boats.isEmpty() || fleet.size() >= MAX_BOATS) {
            break;
         }

         boats.sort(Comparator.comparingDouble(soldier::distanceToSqr));
         ChestPushcartEntity boat = boats.remove(0);
         soldier.getNavigation().stop();
         if (soldier.startRiding(boat, true)) {
            fleet.add(boat.getUUID());
            boarded++;
         }
      }

      if (boarded > 0) {
         player.displayClientMessage(Component.translatable("fleet.ancientwarfare.boarded", boarded, fleet.size()).withStyle(ChatFormatting.AQUA), true);
      }
   }

   /** The fleet's boats make for their stations behind the flagship: three abreast, six blocks a row. */
   private static void steer(ServerLevel level, ChestPushcartEntity flagship, List<UUID> fleet) {
      double yaw = Math.toRadians(flagship.getYRot());
      double dirX = -Math.sin(yaw);
      double dirZ = Math.cos(yaw);
      fleet.removeIf(id -> {
         Entity entity = level.getEntity(id);
         return !(entity instanceof ChestPushcartEntity boat) || !boat.isAlive() || !(boat.getControllingPassenger() instanceof CombatNpcEntity)
            || boat.distanceToSqr(flagship) > LOST_RANGE * LOST_RANGE;
      });
      for (int slot = 0; slot < fleet.size(); slot++) {
         ChestPushcartEntity boat = (ChestPushcartEntity)level.getEntity(fleet.get(slot));
         int row = slot / 3 + 1;
         int side = slot % 3 - 1;
         double atX = flagship.getX() - dirX * row * 6 - dirZ * side * 5;
         double atZ = flagship.getZ() - dirZ * row * 6 + dirX * side * 5;
         // A little ahead of the station, so the boats keep moving with the flagship.
         double moving = flagship.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3 ? 3.0 : 0.0;
         double toX = atX + dirX * moving - boat.getX();
         double toZ = atZ + dirZ * moving - boat.getZ();
         double away = Math.sqrt(toX * toX + toZ * toZ);
         float wanted = (float)(Mth.atan2(-toX, toZ) * Mth.RAD_TO_DEG);
         float turn = Mth.wrapDegrees(wanted - boat.getYRot());
         float forward = away < 1.5 ? 0.0F : Math.abs(turn) > 70.0F ? 0.2F : away > 10.0 ? 1.0F : 0.7F;
         boat.setVehicleInput(forward, Mth.clamp(-turn / 6.0F, -1.0F, 1.0F));
      }
   }

   /** The player is ashore: the soldiers leave their boats and come after them. */
   private static void land(ServerPlayer player, List<UUID> fleet) {
      FLEETS.remove(player.getUUID());
      ServerLevel level = player.serverLevel();
      int landed = 0;
      for (UUID id : fleet) {
         if (level.getEntity(id) instanceof ChestPushcartEntity boat && boat.getControllingPassenger() instanceof CombatNpcEntity soldier) {
            boat.setVehicleInput(0.0F, 0.0F);
            soldier.stopRiding();
            soldier.getNavigation().moveTo(player, 1.2);
            landed++;
         }
      }

      if (landed > 0) {
         player.displayClientMessage(Component.translatable("fleet.ancientwarfare.landed", landed).withStyle(ChatFormatting.AQUA), true);
      }
   }
}
