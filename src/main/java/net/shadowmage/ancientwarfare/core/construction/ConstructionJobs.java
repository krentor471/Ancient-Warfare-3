package net.shadowmage.ancientwarfare.core.construction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplatePlacer;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

/**
 * Building work a player marks out on the ground: dig a box out, fill it in, raise a wall round it
 * or lay a floor. Each job keeps its box, the block to place and the chest its builders take blocks
 * from and bring what they dig to. The player's workers with a pickaxe, a shovel or no tool at all
 * take positions from nearby jobs one by one; see BuilderGoal.
 */
public final class ConstructionJobs extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_construction";
   public static final int MAX_JOBS = 8;
   public static final int MAX_VOLUME = 32 * 32 * 24;
   public static final int SUPPLY_RANGE = 32;
   /** How far ahead of the first unfinished position a builder looks for one to take. */
   private static final int SCAN_AHEAD = 4096;
   private final Map<Integer, Job> jobs = new HashMap<>();
   private int nextId = 1;
   /** Positions builders are working on now, so two never take the same one; not saved. */
   private final Map<Long, UUID> claims = new HashMap<>();
   /** Jobs whose owner was already told the chest ran out of blocks; not saved. */
   private final java.util.Set<Integer> warned = new java.util.HashSet<>();
   private final java.util.Set<String> warnedItems = new java.util.HashSet<>();

   public enum Kind {
      DIG,
      FILL,
      WALL,
      FLOOR,
      TEMPLATE;

      public boolean places() {
         return this != DIG;
      }

      /** Works with one block taken from the off hand; a building brings its own list. */
      public boolean singleBlock() {
         return this != DIG && this != TEMPLATE;
      }

      public String key() {
         return this.name().toLowerCase(Locale.ROOT);
      }

      static Kind byId(int id) {
         Kind[] kinds = values();
         return kinds[Math.floorMod(id, kinds.length)];
      }
   }

   public static final class Job {
      final int id;
      final UUID owner;
      final Kind kind;
      final BlockPos min;
      final BlockPos max;
      final BlockState state;
      BlockPos supply;
      /** For a building: its template, where it stands and which way it faces. */
      String template;
      BlockPos origin;
      Direction facing = Direction.SOUTH;
      /** A building's work order, built from its template: ground to clear top down, then blocks to set bottom up. */
      private List<BlockPos> plan;
      private Map<Long, BlockState> placing;
      /** The first position in work order that may still need work. */
      int cursor;
      int done;

      Job(int id, UUID owner, Kind kind, BlockPos min, BlockPos max, BlockState state, BlockPos supply) {
         this.id = id;
         this.owner = owner;
         this.kind = kind;
         this.min = min;
         this.max = max;
         this.state = state;
         this.supply = supply;
      }

      public int id() {
         return this.id;
      }

      public UUID owner() {
         return this.owner;
      }

      public Kind kind() {
         return this.kind;
      }

      public BlockPos min() {
         return this.min;
      }

      public BlockPos max() {
         return this.max;
      }

      public BlockState state() {
         return this.state;
      }

      public BlockPos supply() {
         return this.supply;
      }

      public int done() {
         return this.done;
      }

      public String template() {
         return this.template;
      }

      private void buildPlan() {
         AwsTemplatePlacer.PreviewData preview = AwsTemplatePlacer.preview(this.template, this.origin, this.facing);
         Map<Long, BlockState> placing = new HashMap<>();
         List<BlockPos> order = new ArrayList<>();
         for (AwsTemplatePlacer.PreviewBlock block : preview.blocks()) {
            placing.put(block.pos().asLong(), block.state());
         }

         // Earth inside the building above the ground line comes out first; the foundation below is left be.
         for (int y = this.max.getY(); y >= Math.max(this.min.getY(), this.origin.getY()); y--) {
            for (int z = this.min.getZ(); z <= this.max.getZ(); z++) {
               for (int x = this.min.getX(); x <= this.max.getX(); x++) {
                  BlockPos pos = new BlockPos(x, y, z);
                  if (!placing.containsKey(pos.asLong())) {
                     order.add(pos);
                  }
               }
            }
         }

         preview.blocks().forEach(block -> order.add(block.pos()));
         this.placing = placing;
         this.plan = order;
      }

      /** The block a building wants here, or null where it only wants the ground cleared. */
      public BlockState stateAt(BlockPos pos) {
         if (this.kind != Kind.TEMPLATE) {
            return this.state;
         }

         if (this.placing == null) {
            this.buildPlan();
         }

         return this.placing.get(pos.asLong());
      }

      /** The item it takes to set this block, or AIR when it comes free with another (a door's top, a bed's head). */
      public static Item itemFor(BlockState state) {
         if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
            || state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            return Items.AIR;
         }

         if (state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT_PATH)) {
            return Items.DIRT;
         }

         return state.getBlock().asItem();
      }

      /** What the job is called in chat and the list: the kind of work, or the building's name. */
      public Component title() {
         if (this.kind == Kind.TEMPLATE) {
            String path = this.template;
            return Component.literal(
               AwsTemplatePlacer.survivalTemplates().stream().filter(info -> info.path().equals(path)).map(AwsTemplatePlacer.TemplateInfo::name).findFirst().orElse(path)
            );
         }

         return Component.translatable("construction.ancientwarfare.kind." + this.kind.key());
      }

      int sizeX() {
         return this.max.getX() - this.min.getX() + 1;
      }

      int sizeZ() {
         return this.max.getZ() - this.min.getZ() + 1;
      }

      int layers() {
         return this.kind == Kind.FLOOR ? 1 : this.max.getY() - this.min.getY() + 1;
      }

      public int total() {
         if (this.kind == Kind.TEMPLATE) {
            if (this.plan == null) {
               this.buildPlan();
            }

            return this.plan.size();
         }

         return this.sizeX() * this.sizeZ() * this.layers();
      }

      /** The position at a step of the work order: digging goes top down, building bottom up. */
      /** Filling goes column by column from the edge inward, so builders stand on what they filled to reach further in. */
      private int[] fillOrder;

      public BlockPos at(int index) {
         if (this.kind == Kind.TEMPLATE) {
            if (this.plan == null) {
               this.buildPlan();
            }

            return this.plan.get(index);
         }

         if (this.kind == Kind.FILL) {
            if (this.fillOrder == null) {
               this.fillOrder = this.buildFillOrder();
            }

            index = this.fillOrder[index];
         }

         int perLayer = this.sizeX() * this.sizeZ();
         int layer = index / perLayer;
         int rest = index % perLayer;
         int y = this.kind == Kind.DIG ? this.max.getY() - layer : this.min.getY() + layer;
         return new BlockPos(this.min.getX() + rest % this.sizeX(), y, this.min.getZ() + rest / this.sizeX());
      }

      private int ring(int index) {
         int rest = index % (this.sizeX() * this.sizeZ());
         int x = rest % this.sizeX();
         int z = rest / this.sizeX();
         return Math.min(Math.min(x, this.sizeX() - 1 - x), Math.min(z, this.sizeZ() - 1 - z));
      }

      private int[] buildFillOrder() {
         int perLayer = this.sizeX() * this.sizeZ();
         return java.util.stream.IntStream.range(0, this.total())
            .boxed()
            .sorted(java.util.Comparator.<Integer>comparingInt(this::ring).thenComparingInt(index -> index / perLayer).thenComparingInt(index -> index % perLayer))
            .mapToInt(Integer::intValue)
            .toArray();
      }

      boolean inShape(BlockPos pos) {
         if (this.kind == Kind.DIG) {
            return !this.inRamp(pos);
         }

         if (this.kind != Kind.WALL) {
            return true;
         }

         return pos.getX() == this.min.getX() || pos.getX() == this.max.getX() || pos.getZ() == this.min.getZ() || pos.getZ() == this.max.getZ();
      }

      /**
       * A pit deeper than a block keeps a stair of earth along its longer side, one step down per
       * block, so the builders (and the player) can climb back out of it.
       */
      boolean inRamp(BlockPos pos) {
         int depth = this.max.getY() - pos.getY();
         if (depth < 1 || this.layers() < 2 || this.sizeX() < 2 || this.sizeZ() < 2) {
            return false;
         }

         boolean alongZ = this.sizeZ() >= this.sizeX();
         if ((alongZ ? pos.getX() != this.min.getX() : pos.getZ() != this.min.getZ())) {
            return false;
         }

         int step = alongZ ? pos.getZ() - this.min.getZ() : pos.getX() - this.min.getX();
         return depth > step;
      }

      public double distanceSq(double x, double y, double z) {
         double dx = Math.max(0.0, Math.max(this.min.getX() - x, x - this.max.getX() - 1));
         double dy = Math.max(0.0, Math.max(this.min.getY() - y, y - this.max.getY() - 1));
         double dz = Math.max(0.0, Math.max(this.min.getZ() - z, z - this.max.getZ() - 1));
         return dx * dx + dy * dy + dz * dz;
      }

      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putInt("Id", this.id);
         tag.putUUID("Owner", this.owner);
         tag.putInt("Kind", this.kind.ordinal());
         tag.putLong("Min", this.min.asLong());
         tag.putLong("Max", this.max.asLong());
         tag.put("State", NbtUtils.writeBlockState(this.state));
         if (this.supply != null) {
            tag.putLong("Supply", this.supply.asLong());
         }

         tag.putInt("Done", this.done);
         if (this.template != null) {
            tag.putString("Template", this.template);
            tag.putLong("Origin", this.origin.asLong());
            tag.putInt("Facing", this.facing.get2DDataValue());
         }

         return tag;
      }

      static Job load(Level level, CompoundTag tag) {
         Job job = new Job(
            tag.getInt("Id"),
            tag.getUUID("Owner"),
            Kind.byId(tag.getInt("Kind")),
            BlockPos.of(tag.getLong("Min")),
            BlockPos.of(tag.getLong("Max")),
            NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), tag.getCompound("State")),
            tag.contains("Supply") ? BlockPos.of(tag.getLong("Supply")) : null
         );
         // The scan starts over on load: cheap, and a block may have changed while the area was unloaded.
         job.cursor = 0;
         job.done = tag.getInt("Done");
         if (tag.contains("Template")) {
            job.template = tag.getString("Template");
            job.origin = BlockPos.of(tag.getLong("Origin"));
            job.facing = Direction.from2DDataValue(tag.getInt("Facing"));
         }

         return job;
      }
   }

   public static ConstructionJobs get(ServerLevel level) {
      return level.getDataStorage().computeIfAbsent(tag -> load(level, tag), ConstructionJobs::new, DATA_NAME);
   }

   private static ConstructionJobs load(ServerLevel level, CompoundTag tag) {
      ConstructionJobs data = new ConstructionJobs();
      for (Tag element : tag.getList("Jobs", Tag.TAG_COMPOUND)) {
         Job job = Job.load(level, (CompoundTag)element);
         if (job.kind == Kind.TEMPLATE && !isSurvivalTemplate(job.template)) {
            continue;
         }

         data.jobs.put(job.id, job);
      }

      data.nextId = Math.max(1, tag.getInt("NextId"));
      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      this.jobs.values().forEach(job -> list.add(job.save()));
      tag.put("Jobs", list);
      tag.putInt("NextId", this.nextId);
      return tag;
   }

   public Collection<Job> jobs() {
      return this.jobs.values();
   }

   public List<Job> jobsOf(UUID owner) {
      return this.jobs.values().stream().filter(job -> job.owner.equals(owner)).sorted((a, b) -> Integer.compare(a.id, b.id)).toList();
   }

   /** The player marked a box and chose the work: checks, then the job is posted. */
   public static void create(ServerPlayer player, BlockPos a, BlockPos b, Kind kind) {
      create(player, a, b, kind, null, Direction.SOUTH);
   }

   public static boolean isSurvivalTemplate(String path) {
      return path != null && AwsTemplatePlacer.survivalTemplates().stream().anyMatch(info -> info.path().equals(path));
   }

   /**
    * Where a building's template origin goes so that its footprint starts at the given corner and its
    * ground line lies on the given height; the same on both sides, so the preview matches the work.
    */
   public static BlockPos templateOrigin(String path, Direction facing, int minX, int groundY, int minZ) {
      BlockPos probe = new BlockPos(0, groundY, 0);
      BlockPos min = AwsTemplatePlacer.preview(path, probe, facing).min();
      return probe.offset(minX - min.getX(), 0, minZ - min.getZ());
   }

   public static void create(ServerPlayer player, BlockPos a, BlockPos b, Kind kind, String template, Direction facing) {
      ServerLevel level = player.serverLevel();
      ConstructionJobs data = get(level);
      BlockPos origin = null;
      if (kind == Kind.TEMPLATE) {
         if (!isSurvivalTemplate(template)) {
            return;
         }

         // The marked corner is the ground the building stands on; the template decides its size.
         origin = templateOrigin(template, facing, Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()) + 1, Math.min(a.getZ(), b.getZ()));
         AwsTemplatePlacer.PreviewData preview = AwsTemplatePlacer.preview(template, origin, facing);
         a = preview.min();
         b = preview.max();
      }

      BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
      BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
      long volume = (long)(max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
      if (data.jobsOf(player.getUUID()).size() >= MAX_JOBS) {
         refuse(player, "construction.ancientwarfare.too_many", MAX_JOBS);
         return;
      }

      if (volume > MAX_VOLUME && kind != Kind.TEMPLATE) {
         refuse(player, "construction.ancientwarfare.too_big", volume, MAX_VOLUME);
         return;
      }

      if (player.distanceToSqr(min.getX(), min.getY(), min.getZ()) > 256.0 * 256.0) {
         return;
      }

      BlockState state = Blocks.AIR.defaultBlockState();
      if (kind.singleBlock()) {
         ItemStack held = player.getOffhandItem();
         if (!(held.getItem() instanceof BlockItem blockItem) || blockItem.getBlock() instanceof EntityBlock) {
            refuse(player, "construction.ancientwarfare.no_block");
            return;
         }

         state = blockItem.getBlock().defaultBlockState();
      }

      BlockPos supply = findSupply(level, min, max);
      if (kind.places() && supply == null) {
         refuse(player, "construction.ancientwarfare.no_supply", SUPPLY_RANGE);
         return;
      }

      Job job = new Job(data.nextId++, player.getUUID(), kind, min, max, state, supply);
      if (kind == Kind.TEMPLATE) {
         job.template = template;
         job.origin = origin;
         job.facing = facing;
      }

      data.jobs.put(job.id, job);
      data.setDirty();
      player.sendSystemMessage(
         Component.translatable(
               "construction.ancientwarfare.created",
               job.id,
               job.title(),
               // A building counts the blocks it sets, not the ground it clears.
               kind == Kind.TEMPLATE ? job.total() - job.plan.size() + job.placing.size() : job.total(),
               supply == null ? Component.translatable("construction.ancientwarfare.no_chest") : Component.literal(supply.getX() + ", " + supply.getY() + ", " + supply.getZ())
            )
            .withStyle(ChatFormatting.GREEN)
      );
      sync(player);
      AncientWarfareCore.LOG.info("{} posts construction job #{} {} {}..{}", player.getScoreboardName(), job.id, kind, min, max);
   }

   public static void cancel(ServerPlayer player, int id) {
      ConstructionJobs data = get(player.serverLevel());
      Job job = data.jobs.get(id);
      if (job != null && job.owner.equals(player.getUUID())) {
         data.finish(job);
         player.sendSystemMessage(Component.translatable("construction.ancientwarfare.cancelled", id).withStyle(ChatFormatting.YELLOW));
         sync(player);
      }
   }

   private static void refuse(ServerPlayer player, String key, Object... args) {
      player.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED));
   }

   /** The nearest chest or town hall around the box: a town hall first, any container otherwise. */
   static BlockPos findSupply(ServerLevel level, BlockPos min, BlockPos max) {
      BlockPos center = new BlockPos((min.getX() + max.getX()) / 2, (min.getY() + max.getY()) / 2, (min.getZ() + max.getZ()) / 2);
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      boolean bestHall = false;
      for (int cx = (center.getX() - SUPPLY_RANGE) >> 4; cx <= (center.getX() + SUPPLY_RANGE) >> 4; cx++) {
         for (int cz = (center.getZ() - SUPPLY_RANGE) >> 4; cz <= (center.getZ() + SUPPLY_RANGE) >> 4; cz++) {
            var chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) {
               continue;
            }

            for (BlockEntity entity : chunk.getBlockEntities().values()) {
               if (!(entity instanceof Container container) || container.getContainerSize() < 9) {
                  continue;
               }

               BlockPos pos = entity.getBlockPos();
               double distance = pos.distSqr(center);
               boolean hall = entity instanceof TownHallBlockEntity;
               if (distance > (double)SUPPLY_RANGE * SUPPLY_RANGE || pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ() && pos.getY() >= min.getY() && pos.getY() <= max.getY()) {
                  continue;
               }

               if (best == null || hall && !bestHall || hall == bestHall && distance < bestDistance) {
                  best = pos;
                  bestDistance = distance;
                  bestHall = hall;
               }
            }
         }
      }

      return best;
   }

   /** Whether this position of the job still wants a builder. */
   public static boolean needsWork(Level level, Job job, BlockPos pos) {
      if (!level.isLoaded(pos) || !job.inShape(pos)) {
         return false;
      }

      BlockState current = level.getBlockState(pos);
      if (job.kind == Kind.TEMPLATE) {
         BlockState wanted = job.stateAt(pos);
         boolean clearable = current.getFluidState().isEmpty()
            && current.getDestroySpeed(level, pos) >= 0.0F
            && level.getBlockEntity(pos) == null
            && (current.canBeReplaced() || AwsTemplatePlacer.isBasicNaturalTerrain(current));
         if (wanted == null) {
            return !current.isAir() && clearable;
         }

         // Earth the template wants below ground is as good as the earth already there.
         if (AwsTemplatePlacer.isBasicNaturalTerrain(wanted) && AwsTemplatePlacer.isBasicNaturalTerrain(current) && !current.canBeReplaced()) {
            return false;
         }

         // Only open ground and plain earth give way; whatever the player built there is left standing.
         return !current.is(wanted.getBlock()) && (current.isAir() || current.canBeReplaced() || clearable);
      }

      if (job.kind == Kind.DIG) {
         return !current.isAir()
            && current.getFluidState().isEmpty()
            && current.getDestroySpeed(level, pos) >= 0.0F
            && level.getBlockEntity(pos) == null;
      }

      if (current.is(job.state.getBlock())) {
         return false;
      }

      // A floor is laid over whatever is there: the old ground comes up first.
      return current.canBeReplaced()
         || job.kind == Kind.FLOOR && current.getFluidState().isEmpty() && current.getDestroySpeed(level, pos) >= 0.0F && level.getBlockEntity(pos) == null;
   }

   /** The owner's jobs, sent to those holding the builder's wand so they see them and their progress. */
   public static void sync(net.minecraft.server.MinecraftServer server) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         if (player.getMainHandItem().getItem() instanceof net.shadowmage.ancientwarfare.core.item.ConstructionWandItem
            || player.getOffhandItem().getItem() instanceof net.shadowmage.ancientwarfare.core.item.ConstructionWandItem) {
            sync(player);
         }
      }
   }

   public static void sync(ServerPlayer player) {
      net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork.sendConstructionJobs(player, get(player.serverLevel()).jobsOf(player.getUUID()));
   }

   /** The next position a builder could take: the first that needs work and no one else holds. */
   public BlockPos take(Level level, Job job, UUID builder, java.util.Set<Long> skip) {
      int total = job.total();
      while (job.cursor < total && !needsWork(level, job, job.at(job.cursor)) && level.isLoaded(job.at(job.cursor))) {
         job.cursor++;
         this.setDirty();
      }

      if (job.cursor >= total) {
         this.complete(level, job);
         return null;
      }

      for (int index = job.cursor; index < Math.min(total, job.cursor + SCAN_AHEAD); index++) {
         BlockPos pos = job.at(index);
         long key = pos.asLong();
         UUID holder = this.claims.get(key);
         if (holder != null && level instanceof ServerLevel server && !isAlive(server.getEntity(holder))) {
            // The builder holding it is gone: dead or unloaded.
            this.claims.remove(key);
            holder = null;
         }

         if ((holder == null || holder.equals(builder)) && !skip.contains(key) && needsWork(level, job, pos)) {
            this.claims.put(key, builder);
            return pos;
         }
      }

      return null;
   }

   private static boolean isAlive(net.minecraft.world.entity.Entity entity) {
      return entity != null && entity.isAlive();
   }

   public void release(BlockPos pos, UUID builder) {
      if (pos != null) {
         this.claims.remove(pos.asLong(), builder);
      }
   }

   public void worked(Job job) {
      job.done++;
      this.setDirty();
   }

   /** The chest has none of the job's block left: the owner hears it once, until blocks come again. */
   public void outOfBlocks(Level level, Job job, boolean out) {
      if (!out) {
         this.warned.remove(job.id);
      } else if (this.warned.add(job.id) && owner(level, job) instanceof ServerPlayer owner) {
         owner.sendSystemMessage(
            Component.translatable("construction.ancientwarfare.out_of_blocks", job.id, job.state.getBlock().getName()).withStyle(ChatFormatting.YELLOW)
         );
      }
   }

   /** The chest has none of a block the building needs: told once per block, until the job is done. */
   public void missingItem(Level level, Job job, Item item) {
      if (this.warnedItems.add(job.id + ":" + BuiltInRegistries.ITEM.getKey(item)) && owner(level, job) instanceof ServerPlayer owner) {
         owner.sendSystemMessage(
            Component.translatable("construction.ancientwarfare.out_of_blocks", job.id, item.getDescription()).withStyle(ChatFormatting.YELLOW)
         );
      }
   }

   private static Object owner(Level level, Job job) {
      return level instanceof ServerLevel server ? server.getServer().getPlayerList().getPlayer(job.owner) : null;
   }

   /**
    * The blocks a building will need next, in the order it needs them, counted over the positions
    * after the first unfinished one that still want a block set.
    */
   public static Map<Item, Integer> upcomingItems(Level level, Job job, int scan) {
      Map<Item, Integer> needs = new java.util.LinkedHashMap<>();
      int total = job.total();
      for (int index = job.cursor; index < Math.min(total, job.cursor + scan); index++) {
         BlockPos pos = job.at(index);
         BlockState wanted = job.stateAt(pos);
         if (wanted != null && needsWork(level, job, pos)) {
            Item item = Job.itemFor(wanted);
            if (item != Items.AIR) {
               needs.merge(item, 1, Integer::sum);
            }
         }
      }

      return needs;
   }

   public Job job(int id) {
      return this.jobs.get(id);
   }

   private void complete(Level level, Job job) {
      this.finish(job);
      if (job.kind == Kind.TEMPLATE && level instanceof ServerLevel server) {
         // Doors, fences and chests get joined up, as when a building is set in one go. The finishing
         // step clears its own origin spot, so whatever stood there is put back unless the template wants it.
         BlockState atOrigin = server.getBlockState(job.origin);
         boolean ours = job.stateAt(job.origin) != null;
         AwsTemplatePlacer.finishPlayerBuiltIncremental(server, job.origin, job.template, job.facing);
         if (!ours && !atOrigin.isAir()) {
            server.setBlock(job.origin, atOrigin, 18);
         }
      }

      if (owner(level, job) instanceof ServerPlayer owner) {
         owner.sendSystemMessage(
            Component.translatable("construction.ancientwarfare.complete", job.id, job.title())
               .withStyle(ChatFormatting.GREEN)
         );
      }
   }

   private void finish(Job job) {
      this.jobs.remove(job.id);
      this.warned.remove(job.id);
      this.warnedItems.removeIf(key -> key.startsWith(job.id + ":"));
      this.setDirty();
   }

   /** Jobs of an owner near a spot, nearest first. */
   public List<Job> near(UUID owner, double x, double y, double z, double range) {
      List<Job> found = new ArrayList<>();
      for (Job job : this.jobs.values()) {
         if (job.owner.equals(owner) && job.distanceSq(x, y, z) <= range * range) {
            found.add(job);
         }
      }

      found.sort((a, b) -> Double.compare(a.distanceSq(x, y, z), b.distanceSq(x, y, z)));
      return found;
   }
}
