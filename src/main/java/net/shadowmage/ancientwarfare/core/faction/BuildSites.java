package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * A building a town puts up while a player is near does not appear at once. Its plot is levelled
 * and marked out with posts, then builders raise it a few blocks at a time, from the ground up,
 * until it stands as it would have stood. The finished building is worked out first and taken down
 * to its foundation, so what the builders lay is block for block what it will be.
 */
public final class BuildSites extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_build_sites";
   private static final String BUILDER = "AWBuilder";
   private static final long STEP_TICKS = 10L;
   /** Steps a building takes at least, so even a big hall is done in a few minutes. */
   private static final int STEPS = 360;
   private static final int BUILDERS = 2;

   private final List<Site> sites = new ArrayList<>();

   static final class Site {
      final BlockPos min;
      final BlockPos max;
      final int base;
      final List<Long> positions = new ArrayList<>();
      final List<BlockState> states = new ArrayList<>();
      final List<CompoundTag> entities = new ArrayList<>();
      int next;

      Site(BlockPos min, BlockPos max, int base) {
         this.min = min;
         this.max = max;
         this.base = base;
      }

      BlockPos middle() {
         return new BlockPos((this.min.getX() + this.max.getX()) / 2, this.base + 1, (this.min.getZ() + this.max.getZ()) / 2);
      }
   }

   public static BuildSites get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(BuildSites::load, BuildSites::new, DATA_NAME);
   }

   /** Takes the just-finished building between these corners down to its foundation, to be raised again by builders. */
   static void stage(ServerLevel level, BlockPos min, BlockPos max) {
      int base = ground(level, min, max);
      BlockPos low = new BlockPos(min.getX(), base + 1, min.getZ());
      BlockPos high = new BlockPos(max.getX(), Math.max(max.getY(), base + 1) + 2, max.getZ());
      Site site = new Site(min, max, base);
      List<BlockPos> solid = new ArrayList<>();
      List<BlockPos> fixtures = new ArrayList<>();
      for (BlockPos pos : BlockPos.betweenClosed(low, high)) {
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && level.getFluidState(pos).isEmpty()) {
            (state.isCollisionShapeFullBlock(level, pos) ? solid : fixtures).add(pos.immutable());
         }
      }

      if (solid.size() + fixtures.size() < 12) {
         net.shadowmage.ancientwarfare.core.AncientWarfareCore.LOG.info("Build site at {}..{} left as built: {} blocks above y {}", min, max, solid.size() + fixtures.size(), base);
         return;
      }

      // Walls course by course, then what hangs on them: torches, doors, beds.
      Comparator<BlockPos> order = Comparator.comparingInt(BlockPos::getY);
      solid.sort(order);
      fixtures.sort(order);
      solid.addAll(fixtures);
      for (BlockPos pos : solid) {
         BlockEntity entity = level.getBlockEntity(pos);
         site.positions.add(pos.asLong());
         site.states.add(level.getBlockState(pos));
         site.entities.add(entity == null ? null : entity.saveWithFullMetadata());
      }

      // Fixtures first so nothing drops as its support goes.
      for (int i = solid.size() - 1; i >= 0; i--) {
         BlockPos pos = solid.get(i);
         level.removeBlockEntity(pos);
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
      }

      for (BlockPos corner : List.of(min, new BlockPos(max.getX(), 0, min.getZ()), new BlockPos(min.getX(), 0, max.getZ()), max)) {
         BlockPos post = new BlockPos(corner.getX(), base + 1, corner.getZ());
         if (level.getBlockState(post).isAir()) {
            level.setBlock(post, Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(post.above(), Blocks.WHITE_BANNER.defaultBlockState(), Block.UPDATE_CLIENTS);
         }
      }

      net.shadowmage.ancientwarfare.core.AncientWarfareCore.LOG.info("Build site at {}..{}: {} blocks to raise from y {}", min, max, site.positions.size(), base);
      BuildSites sites = get(level);
      sites.sites.add(site);
      sites.setDirty();
   }

   /** The ground the building stands on: the commonest height just outside its box. */
   private static int ground(ServerLevel level, BlockPos min, BlockPos max) {
      java.util.Map<Integer, Integer> count = new java.util.HashMap<>();
      for (int x = min.getX() - 1; x <= max.getX() + 1; x++) {
         count.merge(level.getHeight(Heightmap.Types.WORLD_SURFACE, x, min.getZ() - 1) - 1, 1, Integer::sum);
         count.merge(level.getHeight(Heightmap.Types.WORLD_SURFACE, x, max.getZ() + 1) - 1, 1, Integer::sum);
      }

      int best = min.getY();
      int seen = -1;
      for (java.util.Map.Entry<Integer, Integer> entry : count.entrySet()) {
         if (entry.getValue() > seen && entry.getKey() >= min.getY() - 1 && entry.getKey() <= max.getY()) {
            best = entry.getKey();
            seen = entry.getValue();
         }
      }

      return best;
   }

   /** Whether a building is being raised at this spot, so the town does not count it yet. */
   public static boolean building(ServerLevel level, BlockPos pos) {
      for (Site site : get(level).sites) {
         if (pos.getX() >= site.min.getX() && pos.getX() <= site.max.getX() && pos.getZ() >= site.min.getZ() && pos.getZ() <= site.max.getZ()) {
            return true;
         }
      }

      return false;
   }

   public static void tick(ServerLevel level) {
      if (level.getGameTime() % STEP_TICKS != 0L) {
         return;
      }

      BuildSites sites = get(level);
      for (Site site : new ArrayList<>(sites.sites)) {
         BlockPos middle = site.middle();
         if (level.getChunkSource().getChunkNow(middle.getX() >> 4, middle.getZ() >> 4) == null) {
            continue;
         }

         // Far from every player the work goes on all the same, the rest laid at once.
         boolean watched = level.hasNearbyAlivePlayer(middle.getX(), middle.getY(), middle.getZ(), 160);
         int step = watched ? Math.max(1, site.positions.size() / STEPS) : site.positions.size();
         List<FactionNpcEntity> builders = watched ? builders(level, site) : List.of();
         for (int i = 0; i < step && site.next < site.positions.size(); i++, site.next++) {
            BlockPos pos = BlockPos.of(site.positions.get(site.next));
            if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).canBeReplaced()) {
               continue;
            }

            level.setBlock(pos, site.states.get(site.next), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            CompoundTag tag = site.entities.get(site.next);
            if (tag != null) {
               BlockEntity entity = level.getBlockEntity(pos);
               if (entity != null) {
                  entity.load(tag);
                  entity.setChanged();
               }
            }

            if (!builders.isEmpty()) {
               FactionNpcEntity builder = builders.get(site.next % builders.size());
               builder.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
               builder.swing(InteractionHand.MAIN_HAND);
               if (builder.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 36.0) {
                  builder.getNavigation().moveTo(pos.getX() + 0.5, site.base + 1, pos.getZ() + 0.5, 0.7);
               }

               if (site.next % 6 == 0) {
                  level.playSound(null, pos, site.states.get(site.next).getSoundType().getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.0F);
               }
            }
         }

         if (site.next >= site.positions.size()) {
            finish(level, site);
            sites.sites.remove(site);
         }

         sites.setDirty();
      }
   }

   private static List<FactionNpcEntity> builders(ServerLevel level, Site site) {
      AABB area = new AABB(site.min, site.max).inflate(12.0, 8.0, 12.0);
      List<FactionNpcEntity> found = new ArrayList<>(level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getTags().contains(BUILDER)));
      FactionWorldData.Settlement town = null;
      for (FactionWorldData.Settlement settlement : FactionWorldData.get(level).settlements()) {
         if (town == null || settlement.horizontalDistanceSquared(site.middle().getX(), site.middle().getZ()) < town.horizontalDistanceSquared(site.middle().getX(), site.middle().getZ())) {
            town = settlement;
         }
      }

      while (found.size() < BUILDERS && town != null) {
         FactionNpcEntity builder = AWCoreEntityTypes.FACTION_CIVILIAN_MALE_NPC.get().create(level);
         if (builder == null) {
            break;
         }

         BlockPos at = site.min.offset(-1 - found.size(), 0, -1);
         at = new BlockPos(at.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()), at.getZ());
         builder.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
         builder.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
         builder.setFactionName(town.faction());
         builder.addTag(BUILDER);
         builder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(found.isEmpty() ? Items.IRON_AXE : Items.STONE_SHOVEL));
         builder.setCustomName(net.minecraft.network.chat.Component.translatable("town.ancientwarfare.builder"));
         builder.restrictTo(site.middle(), 16);
         level.addFreshEntity(builder);
         found.add(builder);
      }

      return found;
   }

   /** The building stands: the posts come down and the builders go home. */
   private static void finish(ServerLevel level, Site site) {
      for (BlockPos corner : List.of(site.min, new BlockPos(site.max.getX(), 0, site.min.getZ()), new BlockPos(site.min.getX(), 0, site.max.getZ()), site.max)) {
         BlockPos post = new BlockPos(corner.getX(), site.base + 1, corner.getZ());
         if (level.getBlockState(post).is(Blocks.OAK_FENCE) && level.getBlockState(post.above()).is(Blocks.WHITE_BANNER)) {
            level.setBlock(post.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(post, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
         }
      }

      AABB area = new AABB(site.min, site.max).inflate(16.0, 8.0, 16.0);
      level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.getTags().contains(BUILDER)).forEach(npc -> npc.discard());
   }

   private static BuildSites load(CompoundTag tag) {
      BuildSites sites = new BuildSites();
      for (Tag raw : tag.getList("Sites", Tag.TAG_COMPOUND)) {
         CompoundTag value = (CompoundTag)raw;
         Site site = new Site(BlockPos.of(value.getLong("Min")), BlockPos.of(value.getLong("Max")), value.getInt("Base"));
         site.next = value.getInt("Next");
         for (Tag rawBlock : value.getList("Blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag)rawBlock;
            site.positions.add(block.getLong("P"));
            site.states.add(NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), block.getCompound("S")));
            site.entities.add(block.contains("E") ? block.getCompound("E") : null);
         }

         sites.sites.add(site);
      }

      return sites;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      for (Site site : this.sites) {
         CompoundTag value = new CompoundTag();
         value.putLong("Min", site.min.asLong());
         value.putLong("Max", site.max.asLong());
         value.putInt("Base", site.base);
         ListTag blocks = new ListTag();
         for (int i = site.next; i < site.positions.size(); i++) {
            CompoundTag block = new CompoundTag();
            block.putLong("P", site.positions.get(i));
            block.put("S", NbtUtils.writeBlockState(site.states.get(i)));
            if (site.entities.get(i) != null) {
               block.put("E", site.entities.get(i));
            }

            blocks.add(block);
         }

         // Only what is left to lay is kept, so the count starts again at the first of it.
         value.putInt("Next", 0);
         value.put("Blocks", blocks);
         list.add(value);
      }

      tag.put("Sites", list);
      return tag;
   }
}
