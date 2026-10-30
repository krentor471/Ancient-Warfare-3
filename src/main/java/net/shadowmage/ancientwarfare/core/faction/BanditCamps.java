package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * Bandit camps by the roads: tents around a fire, a stolen chest and the men who live there. A
 * camp stands until every one of its men is dead; then the fire is put out, the tents fall, the
 * chest is the player's and the realm whose roads it preyed on pays a bounty. New camps spring up
 * now and then along the roads, away from towns and from where any player stands.
 */
public final class BanditCamps extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_bandit_camps";
   public static final String CAMP_TAG = "AWCamp";
   private static final String OUTLAW = "brigand";
   private static final int MAX_CAMPS = 24;
   private static final double NEW_CAMP_CHANCE = 0.35;
   private static final int TOWN_CLEARANCE = 160;
   private static final int ROAD_OFFSET = 24;
   private static final int SHOW_RANGE = 112;
   private static final int CAMP_RADIUS = 9;
   private static final int BOUNTY = 40;
   private static final int BOUNTY_STANDING = 4;
   private static final long CHECK_TICKS = 40L;

   private final List<Camp> camps = new ArrayList<>();
   private int nextId = 1;

   static final class Camp {
      final int id;
      final int x;
      final int z;
      final int men;
      int y;
      boolean built;

      Camp(int id, int x, int z, int men) {
         this.id = id;
         this.x = x;
         this.z = z;
         this.men = men;
      }

      BlockPos fire() {
         return new BlockPos(this.x, this.y, this.z);
      }
   }

   public static BanditCamps get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(BanditCamps::load, BanditCamps::new, DATA_NAME);
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      BanditCamps camps = get(level);
      RandomSource random = level.getRandom();
      if (camps.camps.size() >= MAX_CAMPS || random.nextDouble() >= NEW_CAMP_CHANCE) {
         return;
      }

      List<FactionWorldData.Route> roads = data.routes().stream().filter(route -> route.road() == FactionWorldData.RoadState.DONE && route.points().size() > 10).toList();
      if (roads.isEmpty()) {
         return;
      }

      FactionWorldData.Route road = roads.get(random.nextInt(roads.size()));
      long point = road.points().get(3 + random.nextInt(road.points().size() - 6));
      double angle = random.nextDouble() * Math.PI * 2.0;
      int x = FactionRoads.unpackX(point) + (int)(Math.cos(angle) * ROAD_OFFSET);
      int z = FactionRoads.unpackZ(point) + (int)(Math.sin(angle) * ROAD_OFFSET);
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.contains(x, z, TOWN_CLEARANCE)) {
            return;
         }
      }

      for (Camp other : camps.camps) {
         if (Math.abs(other.x - x) < 300 && Math.abs(other.z - z) < 300) {
            return;
         }
      }

      if (level.hasNearbyAlivePlayer(x, level.getSeaLevel(), z, SHOW_RANGE + 32)) {
         return;
      }

      camps.camps.add(new Camp(camps.nextId++, x, z, 4 + random.nextInt(4)));
      camps.setDirty();
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L || level.players().isEmpty()) {
         return;
      }

      BanditCamps camps = get(level);
      for (Camp camp : new ArrayList<>(camps.camps)) {
         if (!level.hasNearbyAlivePlayer(camp.x, level.getSeaLevel(), camp.z, SHOW_RANGE) || level.getChunkSource().getChunkNow(camp.x >> 4, camp.z >> 4) == null) {
            continue;
         }

         if (!camp.built) {
            if (camps.build(level, camp)) {
               camp.built = true;
               camps.setDirty();
            }

            continue;
         }

         String tag = CAMP_TAG + camp.id;
         AABB area = new AABB(camp.fire()).inflate(48.0);
         if (level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getTags().contains(tag)).isEmpty()) {
            camps.destroy(level, data, camp);
            camps.camps.remove(camp);
            camps.setDirty();
         }
      }
   }

   /** Tents around a fire, logs to sit on, a chest of plunder, and the men. False while the land is not right. */
   private boolean build(ServerLevel level, Camp camp) {
      camp.y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, camp.x, camp.z);
      BlockPos fire = camp.fire();
      if (!level.getFluidState(fire.below()).isEmpty()) {
         // Water here: the camp moves a little way along.
         return false;
      }

      RandomSource random = level.getRandom();
      for (int dx = -CAMP_RADIUS; dx <= CAMP_RADIUS; dx++) {
         for (int dz = -CAMP_RADIUS; dz <= CAMP_RADIUS; dz++) {
            if (dx * dx + dz * dz <= CAMP_RADIUS * CAMP_RADIUS) {
               int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, camp.x + dx, camp.z + dz);
               BlockPos ground = new BlockPos(camp.x + dx, y - 1, camp.z + dz);
               if (Math.abs(y - camp.y) <= 2 && random.nextInt(3) == 0 && level.getBlockState(ground).is(Blocks.GRASS_BLOCK)) {
                  level.setBlockAndUpdate(ground, Blocks.COARSE_DIRT.defaultBlockState());
               }
            }
         }
      }

      level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
      for (Direction side : Direction.Plane.HORIZONTAL) {
         BlockPos seat = fire.relative(side, 2);
         level.setBlockAndUpdate(seat, Blocks.SPRUCE_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, side.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X));
      }

      BlockState[] cloth = {Blocks.BROWN_WOOL.defaultBlockState(), Blocks.GRAY_WOOL.defaultBlockState(), Blocks.RED_WOOL.defaultBlockState()};
      for (int i = 0; i < 3; i++) {
         double angle = Math.PI * 2.0 * i / 3.0 + random.nextDouble() * 0.5;
         int tx = camp.x + (int)Math.round(Math.cos(angle) * 6);
         int tz = camp.z + (int)Math.round(Math.sin(angle) * 6);
         tent(level, new BlockPos(tx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz), tz), cloth[i]);
      }

      BlockPos chestPos = fire.offset(1, 0, 3);
      level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
      if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
         chest.setItem(0, new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(5)));
         chest.setItem(1, new ItemStack(Items.BREAD, 3 + random.nextInt(4)));
         chest.setItem(2, new ItemStack(Items.LEATHER, 2 + random.nextInt(4)));
         if (random.nextBoolean()) {
            chest.setItem(3, new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(3)));
         }
      }

      for (int i = 0; i < camp.men; i++) {
         EntityType<? extends FactionNpcEntity> type = i % 3 == 2 ? AWCoreEntityTypes.FACTION_ARCHER_NPC.get() : AWCoreEntityTypes.FACTION_SOLDIER_NPC.get();
         FactionNpcEntity man = type.create(level);
         if (man == null) {
            continue;
         }

         double angle = Math.PI * 2.0 * i / camp.men;
         int mx = camp.x + (int)Math.round(Math.cos(angle) * 4);
         int mz = camp.z + (int)Math.round(Math.sin(angle) * 4);
         BlockPos at = new BlockPos(mx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mx, mz), mz);
         man.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
         man.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
         man.setFactionName(OUTLAW);
         man.addTag(CAMP_TAG + camp.id);
         man.setPersistenceRequired();
         man.restrictTo(fire, 14);
         level.addFreshEntity(man);
      }

      return true;
   }

   /** A small A-frame tent of cloth, open at the front. */
   private static void tent(ServerLevel level, BlockPos base, BlockState cloth) {
      for (int dz = 0; dz < 3; dz++) {
         level.setBlockAndUpdate(base.offset(-1, 0, dz), cloth);
         level.setBlockAndUpdate(base.offset(1, 0, dz), cloth);
         level.setBlockAndUpdate(base.offset(0, 1, dz), cloth);
         level.setBlockAndUpdate(base.offset(0, 0, dz), Blocks.AIR.defaultBlockState());
      }

      level.setBlockAndUpdate(base.offset(0, 0, 2), Blocks.WHITE_CARPET.defaultBlockState());
   }

   /** Every man is dead: the fire goes out, the tents come down, and the bounty is paid. */
   private void destroy(ServerLevel level, FactionWorldData data, Camp camp) {
      BlockPos fire = camp.fire();
      if (level.getBlockState(fire).is(Blocks.CAMPFIRE)) {
         level.setBlockAndUpdate(fire, level.getBlockState(fire).setValue(CampfireBlock.LIT, false));
      }

      for (int dx = -8; dx <= 8; dx++) {
         for (int dz = -8; dz <= 8; dz++) {
            for (int dy = -1; dy <= 2; dy++) {
               BlockPos pos = fire.offset(dx, dy, dz);
               BlockState state = level.getBlockState(pos);
               if (state.is(Blocks.BROWN_WOOL) || state.is(Blocks.GRAY_WOOL) || state.is(Blocks.RED_WOOL) || state.is(Blocks.WHITE_CARPET)) {
                  level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
               }
            }
         }
      }

      FactionWorldData.Settlement nearest = null;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction()) && (nearest == null || settlement.horizontalDistanceSquared(camp.x, camp.z) < nearest.horizontalDistanceSquared(camp.x, camp.z))) {
            nearest = settlement;
         }
      }

      for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(fire).inflate(64.0))) {
         Coins.give(player, BOUNTY);
         if (nearest != null) {
            FactionStandingData.get(level).adjustStanding(player.getUUID(), nearest.faction(), BOUNTY_STANDING);
            TownChronicle.write(level, nearest.id(), "camp", player.getName().getString());
         }

         player.sendSystemMessage(Component.translatable("camp.ancientwarfare.destroyed", BOUNTY,
            nearest == null ? Component.literal("-") : FactionNews.factionName(nearest.faction()), BOUNTY_STANDING).withStyle(ChatFormatting.GREEN));
      }
   }

   /** The camp at this spot, for the map: {x, z} of each camp a player could find. */
   public static List<int[]> standing(ServerLevel level) {
      List<int[]> out = new ArrayList<>();
      get(level).camps.forEach(camp -> out.add(new int[]{camp.x, camp.z}));
      return out;
   }

   private static BanditCamps load(CompoundTag tag) {
      BanditCamps camps = new BanditCamps();
      camps.nextId = Math.max(1, tag.getInt("NextId"));
      for (Tag raw : tag.getList("Camps", Tag.TAG_COMPOUND)) {
         CompoundTag value = (CompoundTag)raw;
         Camp camp = new Camp(value.getInt("Id"), value.getInt("X"), value.getInt("Z"), value.getInt("Men"));
         camp.y = value.getInt("Y");
         camp.built = value.getBoolean("Built");
         camps.camps.add(camp);
      }

      return camps;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      tag.putInt("NextId", this.nextId);
      ListTag list = new ListTag();
      for (Camp camp : this.camps) {
         CompoundTag value = new CompoundTag();
         value.putInt("Id", camp.id);
         value.putInt("X", camp.x);
         value.putInt("Y", camp.y);
         value.putInt("Z", camp.z);
         value.putInt("Men", camp.men);
         value.putBoolean("Built", camp.built);
         list.add(value);
      }

      tag.put("Camps", list);
      return tag;
   }
}
