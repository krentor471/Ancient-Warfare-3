package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.entity.TransportBoatEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * How far a town has come, and the ports of the cities on the coast. A settlement's tier is its
 * rank: a village, then a large village, a city and a great city. The world hears when a town
 * becomes a city, and a city by the sea builds a port: a pier out into the water with lanterns and
 * a boat tied up. A port brings its faction trade every day, and the faction's fleets put out from it
 * with more men aboard.
 */
public final class FactionPorts {
   /** The rank from which a town is a city: it builds watchtowers and, on the coast, a port. */
   public static final int CITY = 2;
   /** Silver a port brings its faction's treasury every day. */
   static final int PORT_TRADE = 6;
   /** Soldiers more a fleet carries when it puts out from a port. */
   static final int PORT_CREW = 8;
   private static final long TICK_INTERVAL = 400L;
   /** A port goes up only while a player is about, but not right where it stands. */
   private static final double NEAR = 192.0;
   private static final double TOO_CLOSE = 40.0;
   private static final int PIER_LENGTH = 12;
   private static final int MAX_SHORE = 160;
   private static final int MAX_DEPTH = 16;

   private FactionPorts() {
   }

   /** The settlement's rank, 0 to 3; -1 for forts and outlaw lairs, which have none. */
   public static int rank(FactionWorldData.Settlement settlement) {
      return FactionDiplomacy.isOutlaw(settlement.faction()) || FactionAI.isFort(settlement) ? -1 : settlement.tier();
   }

   public static Component rankName(int rank) {
      return Component.translatable("town.ancientwarfare.rank." + rank);
   }

   /** A town rose a rank: a city is news across the land, a smaller step only near it. */
   static void rankRose(ServerLevel level, FactionWorldData.Settlement settlement) {
      int rank = rank(settlement);
      if (rank < 1) {
         return;
      }

      Component name = settlement.name().isEmpty() ? FactionNews.factionName(settlement.faction()) : settlement.nameComponent();
      Component message = Component.translatable("news.ancientwarfare.town_rank", name, rankName(rank), FactionNews.factionName(settlement.faction()));
      if (rank >= CITY) {
         FactionNews.tell(level, message);
      } else {
         FactionCampaigns.tellNear(level, settlement.center().getX(), settlement.center().getZ(), message.copy().withStyle(ChatFormatting.GRAY));
      }
   }

   /** For the debug commands: the nearest town rises a rank, or builds its port now. */
   public static void force(ServerLevel level, ServerPlayer player, boolean port) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement nearest = null;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (rank(settlement) >= 0 && (nearest == null || settlement.horizontalDistanceSquared(player.getX(), player.getZ()) < nearest.horizontalDistanceSquared(player.getX(), player.getZ()))) {
            nearest = settlement;
         }
      }

      if (nearest == null) {
         player.sendSystemMessage(Component.literal("No town").withStyle(ChatFormatting.RED));
      } else if (port) {
         boolean built = build(level, data, nearest, true);
         player.sendSystemMessage(Component.literal("Settlement #" + nearest.id() + (built ? " built a port at " + nearest.port().toShortString() : " found no shore")).withStyle(built ? ChatFormatting.GREEN : ChatFormatting.RED));
      } else {
         nearest.tier = Math.min(3, nearest.tier + 1);
         data.setDirty();
         rankRose(level, nearest);
         player.sendSystemMessage(Component.literal("Settlement #" + nearest.id() + " is now rank " + nearest.tier).withStyle(ChatFormatting.GREEN));
      }
   }

   /** Daily: each port brings its faction trade. */
   static void simulateDay(FactionWorldData data) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.port() != null && rank(settlement) >= CITY) {
            data.setTreasury(settlement.faction(), data.treasury(settlement.faction()) + PORT_TRADE);
         }
      }
   }

   /** Every so often: a coastal city near a player without a port builds one. */
   static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % TICK_INTERVAL != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.port() == null && settlement.built() && rank(settlement) >= CITY && playerAbout(level, settlement.center()) && build(level, data, settlement, false)) {
            return;
         }
      }
   }

   private static boolean playerAbout(ServerLevel level, BlockPos pos) {
      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(pos.getX(), player.getY(), pos.getZ()) <= NEAR * NEAR) {
            return true;
         }
      }

      return false;
   }

   /** Builds the port where the way from the town to the sea first meets the water. */
   static boolean build(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, boolean forced) {
      BlockPos coast = FactionFleets.coast(level, settlement);
      if (coast == null && forced) {
         // The operator's port goes to the nearest open water, sea or not.
         coast = nearestWater(level, settlement.center());
      }

      if (coast == null) {
         return false;
      }

      BlockPos center = settlement.center();
      double dx = coast.getX() - center.getX();
      double dz = coast.getZ() - center.getZ();
      double distance = Math.sqrt(dx * dx + dz * dz);
      if (distance < 1.0) {
         return false;
      }

      Direction out = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
      BlockPos shore = null;
      int waterTop = 0;
      for (int step = 0; step <= Math.min(MAX_SHORE, (int)distance); step++) {
         int x = center.getX() + (int)Math.round(dx / distance * step);
         int z = center.getZ() + (int)Math.round(dz / distance * step);
         if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return false;
         }

         int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
         if (level.getFluidState(new BlockPos(x, top, z)).is(FluidTags.WATER)) {
            shore = new BlockPos(x, top, z);
            waterTop = top;
            break;
         }
      }

      if (shore == null) {
         return false;
      }

      for (ServerPlayer player : level.players()) {
         if (!forced && player.distanceToSqr(shore.getX(), player.getY(), shore.getZ()) < TOO_CLOSE * TOO_CLOSE) {
            return false;
         }
      }

      for (int i = 0; i < PIER_LENGTH; i++) {
         if (!level.hasChunkAt(shore.relative(out, i))) {
            return false;
         }
      }

      BlockPos end = pier(level, shore, out, waterTop + 1);
      settlement.port = end;
      data.setDirty();
      TransportBoatEntity boat = AWCoreEntityTypes.TRANSPORT_BOAT.get().create(level);
      BlockPos mooring = end.relative(out.getClockWise(), 2);
      if (boat != null && level.getFluidState(mooring.below()).is(FluidTags.WATER)) {
         boat.moveTo(mooring.getX() + 0.5, waterTop + 0.6, mooring.getZ() + 0.5, out.toYRot(), 0.0F);
         level.addFreshEntity(boat);
      }

      Component name = settlement.name().isEmpty() ? FactionNews.factionName(settlement.faction()) : settlement.nameComponent();
      FactionCampaigns.tellNear(level, end.getX(), end.getZ(), Component.translatable("town.ancientwarfare.port_built", name).withStyle(ChatFormatting.GRAY));
      AncientWarfareCore.LOG.info("Settlement #{} of {} builds a port at {}", settlement.id(), settlement.faction(), end);
      return true;
   }

   private static BlockPos nearestWater(ServerLevel level, BlockPos center) {
      for (int radius = 8; radius <= 112; radius += 4) {
         for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            int x = center.getX() + (int)Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int)Math.round(Math.sin(angle) * radius);
            if (level.hasChunkAt(new BlockPos(x, 0, z))) {
               int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
               if (level.getFluidState(new BlockPos(x, top, z)).is(FluidTags.WATER)) {
                  return new BlockPos(x, top, z);
               }
            }
         }
      }

      return null;
   }

   /** A pier three wide on posts going out from the shore; returns the middle of its far end. */
   private static BlockPos pier(ServerLevel level, BlockPos shore, Direction out, int deck) {
      Direction side = out.getClockWise();
      BlockState plank = Blocks.SPRUCE_PLANKS.defaultBlockState();
      BlockState post = Blocks.SPRUCE_LOG.defaultBlockState();
      BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
      BlockPos end = shore;
      for (int i = 0; i < PIER_LENGTH; i++) {
         BlockPos middle = new BlockPos(shore.getX(), deck, shore.getZ()).relative(out, i);
         end = middle;
         for (int across = -1; across <= 1; across++) {
            BlockPos pos = middle.relative(side, across);
            set(level, pos, plank);
            clearAbove(level, pos);
            if (across != 0 && i % 4 == 3) {
               for (int y = pos.getY() - 1; y >= pos.getY() - MAX_DEPTH && replaceable(level, new BlockPos(pos.getX(), y, pos.getZ())); y--) {
                  set(level, new BlockPos(pos.getX(), y, pos.getZ()), post);
               }

               set(level, pos.above(), fence);
               if (i == PIER_LENGTH - 1 || i == 3) {
                  set(level, pos.above(2), Blocks.LANTERN.defaultBlockState());
               }
            }
         }
      }

      // At the far end: a rail across, a barrel of goods and a bell to call the boats in.
      set(level, end.relative(out).relative(side, -1).above(), fence);
      set(level, end.relative(out).relative(side, 1).above(), fence);
      set(level, end.relative(out), Blocks.SPRUCE_PLANKS.defaultBlockState());
      set(level, end.relative(out).relative(side, -1), Blocks.SPRUCE_PLANKS.defaultBlockState());
      set(level, end.relative(out).relative(side, 1), Blocks.SPRUCE_PLANKS.defaultBlockState());
      set(level, end.relative(out).above(), Blocks.BELL.defaultBlockState());
      set(level, end.relative(side, -1).above(), Blocks.BARREL.defaultBlockState());
      return end;
   }

   private static boolean replaceable(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.isAir() || state.getFluidState().is(FluidTags.WATER) && state.canBeReplaced() || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
   }

   private static void clearAbove(ServerLevel level, BlockPos pos) {
      for (int dy = 1; dy <= 3; dy++) {
         BlockState above = level.getBlockState(pos.above(dy));
         if (!above.isAir() && above.canBeReplaced()) {
            set(level, pos.above(dy), Blocks.AIR.defaultBlockState());
         }
      }
   }

   private static void set(ServerLevel level, BlockPos pos, BlockState state) {
      level.setBlock(pos, state, Block.UPDATE_CLIENTS);
   }
}
