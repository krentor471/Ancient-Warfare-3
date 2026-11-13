package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * The chests and beds in a faction's settlement belong to its people. A player seen taking from a
 * chest there, or lying down in someone's bed, loses standing and the witness cries out; a theft
 * nobody saw may still be found out. A room paid for at the tavern lets the player sleep there.
 */
public final class FactionCrime {
   private static final double SEE_RANGE = 16.0;
   private static final double SNEAK_SEE_RANGE = 7.0;
   private static final int THEFT_STANDING = -4;
   private static final int MOST_THEFT_STANDING = -15;
   /** Every this many items taken cost one more point of standing. */
   private static final int ITEMS_PER_STANDING = 16;
   private static final double FOUND_OUT_CHANCE = 0.3;
   private static final int FOUND_OUT_STANDING = -2;
   private static final int BED_STANDING = -2;
   private static final String ROOM_DAY_TAG = "aw_room_day";
   private static final String ROOM_TOWN_TAG = "aw_room_town";
   private static final long DAY_TICKS = 24000L;
   private static final Map<UUID, Looking> LOOKING = new HashMap<>();

   /** A chest in a settlement the player opened, and how much was in it. */
   private record Looking(BlockPos pos, int items, String faction) {
   }

   private FactionCrime() {
   }

   /** The built settlement of a faction (not bandits) that the position lies in, or null. */
   static FactionWorldData.Settlement settlementAt(ServerLevel level, BlockPos pos) {
      for (FactionWorldData.Settlement settlement : FactionWorldData.get(level).settlements()) {
         if (settlement.built() && !FactionDiplomacy.isOutlaw(settlement.faction()) && settlement.contains(pos.getX(), pos.getZ(), 0)) {
            return settlement;
         }
      }

      return null;
   }

   /** The player opened a container: remember what was in it if it stands in a settlement. */
   public static void onOpen(ServerPlayer player, BlockPos pos) {
      ServerLevel level = player.serverLevel();
      if (player.isCreative() || player.isSpectator()) {
         return;
      }

      Container container = container(level, pos);
      FactionWorldData.Settlement settlement = container == null ? null : settlementAt(level, pos);
      if (settlement == null) {
         LOOKING.remove(player.getUUID());
         return;
      }

      LOOKING.put(player.getUUID(), new Looking(pos.immutable(), count(container), settlement.faction()));
   }

   /** The player closed a container: if less is in it than when opened, something was taken. */
   public static void onClose(ServerPlayer player) {
      Looking looking = LOOKING.remove(player.getUUID());
      if (looking == null) {
         return;
      }

      ServerLevel level = player.serverLevel();
      Container container = container(level, looking.pos());
      int taken = container == null ? 0 : looking.items() - count(container);
      if (taken > 0) {
         theft(level, player, looking.faction(), taken);
      }
   }

   private static void theft(ServerLevel level, ServerPlayer player, String faction, int taken) {
      FactionStandingData standings = FactionStandingData.get(level);
      FactionNpcEntity witness = witness(level, player, faction);
      if (witness != null) {
         int loss = Math.max(MOST_THEFT_STANDING, THEFT_STANDING - taken / ITEMS_PER_STANDING);
         int standing = standings.adjustStanding(player.getUUID(), faction, loss);
         FactionChatter.cry(level, player, witness, "theft");
         TownWatch.wanted(player, faction, 20 + taken);
         player.sendSystemMessage(Component.translatable("crime.ancientwarfare.theft_seen", FactionNews.factionName(faction), loss, standing).withStyle(ChatFormatting.RED));
      } else if (level.getRandom().nextDouble() < FOUND_OUT_CHANCE) {
         int standing = standings.adjustStanding(player.getUUID(), faction, FOUND_OUT_STANDING);
         player.sendSystemMessage(Component.translatable("crime.ancientwarfare.theft_found", FactionNews.factionName(faction), FOUND_OUT_STANDING, standing).withStyle(ChatFormatting.YELLOW));
      }
   }

   /**
    * The player lies down in a bed: in a settlement it is someone's, unless the player paid for a room
    * there tonight. Returns true when the player may not sleep.
    */
   public static boolean refusesBed(ServerPlayer player, BlockPos pos) {
      ServerLevel level = player.serverLevel();
      if (player.isCreative() || player.isSpectator()) {
         return false;
      }

      FactionWorldData.Settlement settlement = settlementAt(level, pos);
      if (settlement == null || hasRoom(player, settlement)) {
         return false;
      }

      FactionNpcEntity witness = witness(level, player, settlement.faction());
      if (witness != null) {
         int standing = FactionStandingData.get(level).adjustStanding(player.getUUID(), settlement.faction(), BED_STANDING);
         FactionChatter.cry(level, player, witness, "bed");
         player.sendSystemMessage(Component.translatable("crime.ancientwarfare.bed_seen", FactionNews.factionName(settlement.faction()), BED_STANDING, standing).withStyle(ChatFormatting.RED));
      } else {
         player.sendSystemMessage(Component.translatable("crime.ancientwarfare.bed").withStyle(ChatFormatting.YELLOW));
      }

      return true;
   }

   /** A room paid for at the tavern: the player may sleep in the settlement's beds tonight. */
   static void rentRoom(ServerPlayer player, BlockPos at) {
      FactionWorldData.Settlement settlement = settlementAt(player.serverLevel(), at);
      if (settlement != null) {
         player.getPersistentData().putLong(ROOM_DAY_TAG, night(player.serverLevel()));
         player.getPersistentData().putInt(ROOM_TOWN_TAG, settlement.id());
      }
   }

   private static boolean hasRoom(ServerPlayer player, FactionWorldData.Settlement settlement) {
      return player.getPersistentData().contains(ROOM_DAY_TAG)
         && player.getPersistentData().getLong(ROOM_DAY_TAG) == night(player.serverLevel())
         && player.getPersistentData().getInt(ROOM_TOWN_TAG) == settlement.id();
   }

   /** Which night it is: the evening and the small hours after it count as one. */
   private static long night(ServerLevel level) {
      return Math.floorDiv(level.getDayTime() - DAY_TICKS / 2, DAY_TICKS);
   }

   /** A waking townsperson of the faction who can see the player, nearer when the player creeps. */
   private static FactionNpcEntity witness(ServerLevel level, Player player, String faction) {
      double range = player.isShiftKeyDown() ? SNEAK_SEE_RANGE : SEE_RANGE;
      for (FactionNpcEntity npc : level.getEntitiesOfClass(FactionNpcEntity.class, player.getBoundingBox().inflate(range))) {
         if (npc.isAlive()
            && !npc.isSleeping()
            && faction.equals(npc.getFactionName())
            && npc.distanceToSqr(player) <= range * range
            && npc.hasLineOfSight(player)) {
            return npc;
         }
      }

      return null;
   }

   private static Container container(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof ChestBlock chest) {
         // Both halves of a double chest.
         return ChestBlock.getContainer(chest, state, level, pos, true);
      }

      BlockEntity entity = level.getBlockEntity(pos);
      return entity instanceof Container container ? container : null;
   }

   private static int count(Container container) {
      int items = 0;
      for (int slot = 0; slot < container.getContainerSize(); slot++) {
         items += container.getItem(slot).getCount();
      }

      return items;
   }
}
