package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/**
 * Settlements that do well grow by themselves. A day of a full, orderly, fed town adds to its
 * growth; while a player is somewhere around, but not watching the spot, the town builds one piece
 * a while on flat open ground just outside its edge: a house with a bed, a well, a market stall,
 * a lamp post or a garden, with a path back to the middle. Each piece widens the town's bounds and
 * makes room for a few more people.
 */
public final class FactionGrowth {
   /** People each built piece makes room for; see FactionSimulation.capacity. */
   static final int ROOM_PER_PIECE = 4;
   private static final int MAX_PENDING = 6;
   private static final int MAX_GROWN = 12;
   /** Each rank a town rises lets it build this many pieces more. */
   private static final int GROWN_PER_RANK = 6;
   private static final long TICK_INTERVAL = 200L;
   private static final double NEAR = 160.0;
   private static final double TOO_CLOSE = 24.0;
   private static final int SITE_TRIES = 24;
   private static final int GAP = 3;
   private static final int REACH = 8;
   private static final int MAX_SLOPE = 2;
   private static final int PATH_STEPS = 64;
   private static final Piece[] ORDER = {
      Piece.HOUSE, Piece.LAMP, Piece.GARDEN, Piece.HOUSE, Piece.WELL, Piece.HOUSE, Piece.STALL, Piece.LAMP, Piece.HOUSE, Piece.GARDEN, Piece.HOUSE, Piece.STALL
   };

   private FactionGrowth() {
   }

   private enum Piece {
      HOUSE(7, 6, 5),
      WELL(3, 3, 5),
      STALL(3, 3, 4),
      LAMP(1, 1, 5),
      GARDEN(5, 5, 2),
      /** A stone watchtower: only a town that has become a city builds them. */
      TOWER(3, 3, 9);

      final int width;
      final int depth;
      final int height;

      Piece(int width, int depth, int height) {
         this.width = width;
         this.depth = depth;
         this.height = height;
      }
   }

   private record Palette(BlockState wall, BlockState frame, BlockState floor, BlockState roof, BlockState fence, Block door) {
      static Palette of(ServerLevel level, BlockPos pos) {
         BlockState ground = level.getBlockState(pos);
         if (ground.is(BlockTags.SAND)) {
            return new Palette(
               Blocks.SANDSTONE.defaultBlockState(),
               Blocks.CUT_SANDSTONE.defaultBlockState(),
               Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
               Blocks.SANDSTONE_SLAB.defaultBlockState(),
               Blocks.JUNGLE_FENCE.defaultBlockState(),
               Blocks.JUNGLE_DOOR
            );
         }

         if (level.getBiome(pos).value().coldEnoughToSnow(pos)) {
            return new Palette(
               Blocks.SPRUCE_PLANKS.defaultBlockState(),
               Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
               Blocks.COBBLESTONE.defaultBlockState(),
               Blocks.SPRUCE_SLAB.defaultBlockState(),
               Blocks.SPRUCE_FENCE.defaultBlockState(),
               Blocks.SPRUCE_DOOR
            );
         }

         return new Palette(
            Blocks.OAK_PLANKS.defaultBlockState(),
            Blocks.OAK_LOG.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.SPRUCE_SLAB.defaultBlockState(),
            Blocks.OAK_FENCE.defaultBlockState(),
            Blocks.OAK_DOOR
         );
      }
   }

   /** Daily: a town that is nearly full, fed and orderly gets a piece more to build. */
   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (thriving(data, settlement) && settlement.growth < MAX_PENDING && settlement.grown + settlement.growth < MAX_GROWN + GROWN_PER_RANK * settlement.tier()) {
            settlement.growth++;
            data.setDirty();
         }
      }
   }

   private static boolean thriving(FactionWorldData data, FactionWorldData.Settlement settlement) {
      return FactionTownLife.isTown(settlement)
         && settlement.famine() == 0
         && settlement.plague() == 0
         && settlement.order() >= 50
         && settlement.population() * 4 >= FactionSimulation.capacity(settlement) * 3
         && !FactionTownLife.underAttack(data, settlement);
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % TICK_INTERVAL != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.growth > 0 && FactionTownLife.isTown(settlement) && nearestPlayer(level, settlement.center()) <= NEAR * NEAR && grow(level, data, settlement, false)) {
            return;
         }
      }
   }

   private static double nearestPlayer(ServerLevel level, BlockPos pos) {
      double best = Double.MAX_VALUE;
      for (ServerPlayer player : level.players()) {
         double dx = player.getX() - pos.getX() - 0.5;
         double dz = player.getZ() - pos.getZ() - 0.5;
         best = Math.min(best, dx * dx + dz * dz);
      }

      return best;
   }

   /** Builds the town's next piece, if a fitting spot is found. */
   private static boolean grow(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, boolean watched) {
      // A city puts up a watchtower every fourth piece.
      Piece piece = settlement.tier() >= FactionPorts.CITY && settlement.grown % 4 == 3 ? Piece.TOWER : ORDER[settlement.grown % ORDER.length];
      RandomSource random = level.getRandom();
      BlockPos site = site(level, data, settlement, piece, random, watched);
      if (site == null) {
         return false;
      }

      BlockPos center = settlement.center();
      Direction front = facing(site.getX() + piece.width / 2, site.getZ() + piece.depth / 2, center);
      Palette palette = Palette.of(level, site.offset(piece.width / 2, 0, piece.depth / 2));
      clear(level, site, piece);
      BlockPos door = switch (piece) {
         case HOUSE -> house(level, site, front, palette);
         case WELL -> well(level, site, palette);
         case STALL -> stall(level, site, palette, random);
         case LAMP -> lamp(level, site, palette);
         case GARDEN -> garden(level, site, palette, random);
         case TOWER -> tower(level, site, palette);
      };
      path(level, settlement, door.relative(front), center);
      settlement.extend(site, site.offset(piece.width - 1, piece.height, piece.depth - 1));
      settlement.grown++;
      settlement.growth = Math.max(0, settlement.growth - 1);
      data.setDirty();
      if (piece == Piece.HOUSE) {
         Component name = settlement.name().isEmpty() ? FactionNews.factionName(settlement.faction()) : settlement.nameComponent();
         for (ServerPlayer player : level.players()) {
            if (settlement.horizontalDistanceSquared(player.getX(), player.getZ()) <= NEAR * NEAR) {
               player.sendSystemMessage(Component.translatable("town.ancientwarfare.grew", name).withStyle(ChatFormatting.GRAY));
            }
         }
      }

      AncientWarfareCore.LOG.info("Settlement #{} of {} grows: a {} at {}", settlement.id(), settlement.faction(), piece, site);
      return true;
   }

   /** For the debug command: the nearest town grows by one piece now. */
   public static void force(ServerLevel level, ServerPlayer player) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement nearest = null;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (FactionTownLife.isTown(settlement) && (nearest == null || settlement.horizontalDistanceSquared(player.getX(), player.getZ()) < nearest.horizontalDistanceSquared(player.getX(), player.getZ()))) {
            nearest = settlement;
         }
      }

      if (nearest == null) {
         player.sendSystemMessage(Component.literal("No town").withStyle(ChatFormatting.RED));
         return;
      }

      boolean built = grow(level, data, nearest, true);
      player.sendSystemMessage(
         Component.literal("Settlement #" + nearest.id() + " " + (built ? "grew to " + nearest.grown + " pieces" : "found no spot to grow")).withStyle(built ? ChatFormatting.GREEN : ChatFormatting.RED)
      );
   }

   /** Flat open ground just outside the town's edge, clear of other settlements and of players' eyes. */
   private static BlockPos site(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement, Piece piece, RandomSource random, boolean watched) {
      BlockPos min = settlement.min();
      BlockPos max = settlement.max();
      for (int i = 0; i < SITE_TRIES; i++) {
         int out = GAP + random.nextInt(REACH);
         int alongX = min.getX() + random.nextInt(max.getX() - min.getX() + 1) - piece.width / 2;
         int alongZ = min.getZ() + random.nextInt(max.getZ() - min.getZ() + 1) - piece.depth / 2;
         int x;
         int z;
         switch (random.nextInt(4)) {
            case 0 -> {
               x = alongX;
               z = min.getZ() - out - piece.depth;
            }
            case 1 -> {
               x = alongX;
               z = max.getZ() + out + 1;
            }
            case 2 -> {
               x = min.getX() - out - piece.width;
               z = alongZ;
            }
            default -> {
               x = max.getX() + out + 1;
               z = alongZ;
            }
         }

         if (!loaded(level, x, z, piece) || crowded(data, x, z, piece)) {
            continue;
         }

         if (!watched && nearestPlayer(level, new BlockPos(x + piece.width / 2, 0, z + piece.depth / 2)) < TOO_CLOSE * TOO_CLOSE) {
            continue;
         }

         Integer y = flatGround(level, x, z, piece);
         if (y != null) {
            return new BlockPos(x, y, z);
         }
      }

      return null;
   }

   private static boolean loaded(ServerLevel level, int x, int z, Piece piece) {
      for (int dx = -1; dx <= piece.width; dx += piece.width + 1) {
         for (int dz = -1; dz <= piece.depth; dz += piece.depth + 1) {
            if (!level.isLoaded(new BlockPos(x + dx, 64, z + dz))) {
               return false;
            }
         }
      }

      return true;
   }

   /** Whether the spot runs into a settlement, its own included. */
   private static boolean crowded(FactionWorldData data, int x, int z, Piece piece) {
      for (FactionWorldData.Settlement other : data.settlements()) {
         if (x + piece.width + 1 >= other.min().getX() && x - 1 <= other.max().getX() && z + piece.depth + 1 >= other.min().getZ() && z - 1 <= other.max().getZ()) {
            return true;
         }
      }

      return false;
   }

   /** The ground level of the spot, or null where it is steep, wet, wooded or built on. */
   private static Integer flatGround(ServerLevel level, int x, int z, Piece piece) {
      int low = Integer.MAX_VALUE;
      int high = Integer.MIN_VALUE;
      long sum = 0;
      for (int dx = 0; dx < piece.width; dx++) {
         for (int dz = 0; dz < piece.depth; dz++) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
            if (!natural(level.getBlockState(new BlockPos(x + dx, y, z + dz)))) {
               return null;
            }

            low = Math.min(low, y);
            high = Math.max(high, y);
            sum += y;
         }
      }

      return high - low > MAX_SLOPE ? null : (int)Math.round((double)sum / (piece.width * piece.depth));
   }

   private static boolean natural(BlockState state) {
      return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.STONE) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.TERRACOTTA);
   }

   /** The side of a piece that looks toward the middle of the town. */
   private static Direction facing(int x, int z, BlockPos center) {
      int dx = center.getX() - x;
      int dz = center.getZ() - z;
      return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
   }

   private static void set(ServerLevel level, BlockPos pos, BlockState state) {
      level.setBlock(pos, state, Block.UPDATE_CLIENTS);
   }

   /** Levels the spot: open air above the ground, earth filled up below it. */
   private static void clear(ServerLevel level, BlockPos site, Piece piece) {
      BlockState top = level.getBlockState(site.offset(piece.width / 2, 0, piece.depth / 2));
      if (!natural(top)) {
         top = Blocks.GRASS_BLOCK.defaultBlockState();
      }

      for (int dx = 0; dx < piece.width; dx++) {
         for (int dz = 0; dz < piece.depth; dz++) {
            for (int dy = 1; dy <= piece.height + 1; dy++) {
               set(level, site.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }

            set(level, site.offset(dx, 0, dz), top);
            for (int dy = -1; dy >= -3 && !level.getBlockState(site.offset(dx, dy, dz)).isSolidRender(level, site.offset(dx, dy, dz)); dy--) {
               set(level, site.offset(dx, dy, dz), Blocks.DIRT.defaultBlockState());
            }
         }
      }
   }

   /** A one-room house with its door toward the middle of town, a bed, a lantern and a barrel. Returns the doorstep. */
   private static BlockPos house(ServerLevel level, BlockPos site, Direction front, Palette palette) {
      int width = Piece.HOUSE.width;
      int depth = Piece.HOUSE.depth;
      for (int dx = 0; dx < width; dx++) {
         for (int dz = 0; dz < depth; dz++) {
            boolean edgeX = dx == 0 || dx == width - 1;
            boolean edgeZ = dz == 0 || dz == depth - 1;
            set(level, site.offset(dx, 0, dz), palette.floor);
            for (int dy = 1; dy <= 3; dy++) {
               if (edgeX && edgeZ) {
                  set(level, site.offset(dx, dy, dz), palette.frame);
               } else if (edgeX || edgeZ) {
                  boolean window = dy == 2 && (edgeX ? dz == depth / 2 : dx == width / 2);
                  set(level, site.offset(dx, dy, dz), window ? Blocks.GLASS.defaultBlockState() : palette.wall);
               }
            }

            set(level, site.offset(dx, 4, dz), palette.roof.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
         }
      }

      BlockPos door = switch (front) {
         case NORTH -> site.offset(width / 2, 1, 0);
         case SOUTH -> site.offset(width / 2, 1, depth - 1);
         case WEST -> site.offset(0, 1, depth / 2);
         default -> site.offset(width - 1, 1, depth / 2);
      };
      BlockState lower = palette.door.defaultBlockState().setValue(DoorBlock.FACING, front.getOpposite());
      set(level, door, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
      set(level, door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

      // A bed in a back corner, head against the wall, away from the door.
      boolean left = front == Direction.EAST || front == Direction.SOUTH;
      int bedX = left ? 1 : width - 2;
      int bedZ = front == Direction.NORTH ? depth - 2 : 1;
      Direction toHead = front == Direction.NORTH ? Direction.SOUTH : Direction.NORTH;
      BlockPos head = site.offset(bedX, 1, bedZ);
      BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, toHead);
      set(level, head, bed.setValue(BedBlock.PART, BedPart.HEAD));
      set(level, head.relative(toHead.getOpposite()), bed.setValue(BedBlock.PART, BedPart.FOOT));
      set(level, site.offset(width - 1 - bedX, 1, bedZ), Blocks.BARREL.defaultBlockState());
      set(level, site.offset(width - 1 - bedX, 2, bedZ), Blocks.LANTERN.defaultBlockState());
      return door.below();
   }

   private static BlockPos well(ServerLevel level, BlockPos site, Palette palette) {
      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            BlockPos pos = site.offset(dx, 0, dz);
            if (dx == 1 && dz == 1) {
               set(level, pos.below(3), Blocks.COBBLESTONE.defaultBlockState());
               set(level, pos.below(2), Blocks.WATER.defaultBlockState());
               set(level, pos.below(), Blocks.WATER.defaultBlockState());
               set(level, pos, Blocks.WATER.defaultBlockState());
               continue;
            }

            set(level, pos, Blocks.COBBLESTONE.defaultBlockState());
            set(level, pos.above(), Blocks.COBBLESTONE.defaultBlockState());
            if (dx != 1 && dz != 1) {
               set(level, pos.above(2), palette.fence);
               set(level, pos.above(3), palette.fence);
            }
         }
      }

      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            set(level, site.offset(dx, 4, dz), palette.roof.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
         }
      }

      return site.offset(1, 0, 1);
   }

   private static BlockPos stall(ServerLevel level, BlockPos site, Palette palette, RandomSource random) {
      Block[] cloth = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.WHITE_WOOL};
      BlockState roof = cloth[random.nextInt(cloth.length)].defaultBlockState();
      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            if (dx != 1 && dz != 1) {
               set(level, site.offset(dx, 1, dz), palette.fence);
               set(level, site.offset(dx, 2, dz), palette.fence);
            }

            set(level, site.offset(dx, 3, dz), roof);
         }
      }

      set(level, site.offset(1, 1, 1), Blocks.BARREL.defaultBlockState());
      set(level, site.offset(1, 1, 0), Blocks.HAY_BLOCK.defaultBlockState());
      return site.offset(1, 0, 1);
   }

   private static BlockPos lamp(ServerLevel level, BlockPos site, Palette palette) {
      for (int dy = 1; dy <= 3; dy++) {
         set(level, site.above(dy), palette.fence);
      }

      set(level, site.above(4), Blocks.LANTERN.defaultBlockState());
      return site;
   }

   private static BlockPos garden(ServerLevel level, BlockPos site, Palette palette, RandomSource random) {
      BlockState crop = random.nextBoolean() ? Blocks.WHEAT.defaultBlockState() : random.nextBoolean() ? Blocks.CARROTS.defaultBlockState() : Blocks.POTATOES.defaultBlockState();
      for (int dx = 0; dx < 5; dx++) {
         for (int dz = 0; dz < 5; dz++) {
            BlockPos pos = site.offset(dx, 0, dz);
            if (dx == 0 || dz == 0 || dx == 4 || dz == 4) {
               set(level, pos, palette.frame);
            } else if (dx == 2 && dz == 2) {
               set(level, pos, Blocks.WATER.defaultBlockState());
            } else {
               set(level, pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
               set(level, pos.above(), crop.setValue(CropBlock.AGE, 2 + random.nextInt(6)));
            }
         }
      }

      return site.offset(2, 0, 2);
   }

   /** A stone watchtower with a ladder up the inside, a lookout with low walls and a lantern on top. */
   private static BlockPos tower(ServerLevel level, BlockPos site, Palette palette) {
      BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
      for (int dx = 0; dx < 3; dx++) {
         for (int dz = 0; dz < 3; dz++) {
            boolean middle = dx == 1 && dz == 1;
            set(level, site.offset(dx, 0, dz), stone);
            for (int dy = 1; dy <= 6; dy++) {
               if (!middle) {
                  boolean slit = dy == 4 && (dx == 1 || dz == 1);
                  set(level, site.offset(dx, dy, dz), slit ? Blocks.AIR.defaultBlockState() : stone);
               }
            }

            set(level, site.offset(dx, 7, dz), middle ? Blocks.AIR.defaultBlockState() : stone);
            if (!middle) {
               set(level, site.offset(dx, 8, dz), Blocks.STONE_BRICK_WALL.defaultBlockState());
            }
         }
      }

      // The way in faces south; a ladder goes up the middle to the lookout.
      set(level, site.offset(1, 1, 2), Blocks.AIR.defaultBlockState());
      set(level, site.offset(1, 2, 2), Blocks.AIR.defaultBlockState());
      BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH);
      for (int dy = 1; dy <= 7; dy++) {
         set(level, site.offset(1, dy, 1), ladder);
      }

      set(level, site.offset(0, 9, 0), Blocks.LANTERN.defaultBlockState());
      set(level, site.offset(2, 9, 2), Blocks.LANTERN.defaultBlockState());
      return site.offset(1, 0, 3);
   }

   /** A trodden path from the new piece toward the middle, until it meets the town. */
   private static void path(ServerLevel level, FactionWorldData.Settlement settlement, BlockPos from, BlockPos center) {
      int x = from.getX();
      int z = from.getZ();
      for (int i = 0; i < PATH_STEPS && !settlement.contains(x, z, 0); i++) {
         BlockPos column = new BlockPos(x, 64, z);
         if (!level.isLoaded(column)) {
            return;
         }

         BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
         BlockState state = level.getBlockState(ground);
         if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)) {
            set(level, ground, Blocks.DIRT_PATH.defaultBlockState());
            BlockState above = level.getBlockState(ground.above());
            if (above.canBeReplaced() && !above.liquid()) {
               set(level, ground.above(), Blocks.AIR.defaultBlockState());
            }
         }

         int dx = center.getX() - x;
         int dz = center.getZ() - z;
         if (Math.abs(dx) >= Math.abs(dz)) {
            x += Integer.signum(dx);
         } else {
            z += Integer.signum(dz);
         }
      }
   }
}
