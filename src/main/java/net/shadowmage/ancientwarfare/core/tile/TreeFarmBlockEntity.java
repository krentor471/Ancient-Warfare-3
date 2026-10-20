package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.block.AutomationWorksiteBlock;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.BoundedWorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteBoundsGeometry;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgradeHelper;
import net.shadowmage.ancientwarfare.core.menu.TreeFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.TreeFarmMenu;
import org.jetbrains.annotations.Nullable;

public class TreeFarmBlockEntity extends BasicWorksiteBlockEntity implements Container, MenuProvider, BoundedWorkSite {
   public static final int MAIN_SLOT_START = 0;
   public static final int MAIN_SLOT_COUNT = 27;
   public static final int PLANTABLE_SLOT_START = 27;
   public static final int PLANTABLE_SLOT_COUNT = 3;
   public static final int MISC_SLOT_START = 30;
   public static final int MISC_SLOT_COUNT = 3;
   public static final int UPGRADE_SLOT_START = 33;
   public static final int UPGRADE_SLOT_COUNT = 5;
   public static final int SLOT_COUNT = 38;
   public static final int DEFAULT_RADIUS = 2;
   public static final int DEFAULT_DEPTH = 5;
   public static final int MAX_BOUNDS_WIDTH = 16;
   private static final String MIN_TAG = "BoundsMin";
   private static final String MAX_TAG = "BoundsMax";
   private static final String TARGETS_TAG = "TargetMap";
   private static final String HARVEST_PLAN_TAG = "HarvestPlan";
   private static final int WORK_COST = 100;
   private static final int WORKER_ENERGY_PER_VISIT = 1600;
   private static final int TREE_SCAN_HEIGHT = 16;
   private static final int MAX_TREE_BLOCKS = 384;
   private static final int LEAF_SCAN_RADIUS = 5;
   private static final int TREE_EXTENSION_RADIUS = 8;
   private static final int TREE_HARVEST_HEIGHT = 32;
   private static final int MAX_HARVEST_PLAN_BLOCKS = 16384;
   private static final long TREE_PLAN_REFRESH_INTERVAL = 40L;
   private final NonNullList<ItemStack> items = NonNullList.withSize(38, ItemStack.EMPTY);
   private BlockPos boundsMin;
   private BlockPos boundsMax;
   private byte[] targetMap = createFilledTargetMap();
   private int targetScanCursor;
   private List<BlockPos> groundTargetsCache;
   private TreeFarmBlockEntity.TreeTargets treeTargets = new TreeFarmBlockEntity.TreeTargets(new ArrayDeque<>(), new ArrayDeque<>());
   private final Map<BlockPos, Block> plannedBlocks = new HashMap<>();
   private final Map<BlockPos, TreeFarmBlockEntity.HarvestDrops> pendingHarvestDrops = new HashMap<>();
   private boolean treePlanDirty = true;
   private boolean refreshWhenExhausted;
   private long treePlanRefreshAt;

   public TreeFarmBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.TREE_FARM), pos, state);
      this.resetDefaultBounds(pos, state);
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.FORESTRY;
   }

   @Override
   public String getDescriptionId() {
      return "block.ancientwarfare.tree_farm";
   }

   @Override
   public boolean hasWork() {
      return this.level != null && !this.level.isClientSide
         ? this.findPickup().isPresent()
            || this.findShearTarget().isPresent()
            || this.findLeafTarget().isPresent()
            || this.findLogTarget().isPresent()
            || this.findPlantTarget().isPresent()
            || this.findFertilizeTarget().isPresent()
         : false;
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.level != null && !this.level.isClientSide && worker.canWorkAt(this.getWorkType())) {
         this.storedEnergy = Math.min(
            this.getMaxEnergy(), this.storedEnergy + Math.max(1, Math.round(1600.0F * worker.getWorkEffectiveness(this.getWorkType())))
         );
         this.processStoredEnergy();
         this.setChanged();
      }
   }

   @Override
   protected void onWorkEnergyAdded() {
      this.processStoredEnergy();
   }

   @Override
   protected void onStoredEnergyTick() {
      this.processStoredEnergy();
      this.setChanged();
   }

   private void processStoredEnergy() {
      int processed = 0;

      for (int workCost = this.getEffectiveWorkCost(); this.storedEnergy >= workCost && processed < 8 && this.processNextAction(); processed++) {
         this.storedEnergy -= workCost;
      }
   }

   private int getEffectiveWorkCost() {
      return WorksiteUpgradeHelper.adjustedWorkCost(100, this, 33, 5);
   }

   private boolean processNextAction() {
      if (this.tryPickup()) {
         return true;
      } else if (this.findLeafTarget().isPresent()) {
         return this.findShearsSlot().isPresent() ? this.tryShearLeaf() : this.tryBreakLeaf();
      } else {
         return this.tryBreakLog() || this.tryPlant() || this.tryFertilize();
      }
   }

   private boolean tryPickup() {
      Optional<ItemEntity> target = this.findPickup();
      if (target.isEmpty()) {
         return false;
      } else {
         ItemEntity item = target.get();
         int originalCount = item.getItem().getCount();
         ItemStack remaining = this.insertStack(item.getItem());
         if (remaining.getCount() == originalCount) {
            return false;
         } else {
            if (remaining.isEmpty()) {
               item.discard();
            } else {
               item.setItem(remaining);
            }

            return true;
         }
      }
   }

   private boolean tryShearLeaf() {
      Optional<BlockPos> target = this.findShearTarget();
      Optional<Integer> shearsSlot = this.findShearsSlot();
      if (!target.isEmpty() && !shearsSlot.isEmpty() && this.level instanceof ServerLevel serverLevel) {
         BlockPos var8 = target.get();
         BlockState state = this.level.getBlockState(var8);
         List<ItemStack> inventory = this.inventoryAfterDrops(List.of(new ItemStack(state.getBlock().asItem())));
         if (inventory != null && this.level.setBlock(var8, Blocks.AIR.defaultBlockState(), 18)) {
            this.commitHarvestInventory(inventory);
            this.forgetHarvestTarget(var8);
            ItemStack shears = (ItemStack)this.items.get(shearsSlot.get());
            if (shears.hurt(1, serverLevel.random, null)) {
               shears.shrink(1);
            }

            this.setChanged();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean tryBreakLeaf() {
      if (this.findShearsSlot().isPresent()) {
         return false;
      } else {
         Optional<BlockPos> target = this.findLeafTarget();
         return target.isEmpty() ? false : this.harvestBlock(target.get());
      }
   }

   private boolean tryBreakLog() {
      Optional<BlockPos> target = this.findLogTarget();
      return target.isEmpty() ? false : this.harvestBlock(target.get());
   }

   private boolean harvestBlock(BlockPos pos) {
      if (!(this.level instanceof ServerLevel serverLevel)) {
         return false;
      } else {
         BlockState var6 = this.level.getBlockState(pos);
         TreeFarmBlockEntity.HarvestDrops cached = this.pendingHarvestDrops.get(pos);
         if (cached == null || cached.block != var6.getBlock()) {
            cached = new TreeFarmBlockEntity.HarvestDrops(
               var6.getBlock(),
               Block.getDrops(var6, serverLevel, pos, this.level.getBlockEntity(pos), null, WorksiteUpgradeHelper.fortuneTool(Items.DIAMOND_AXE, this, 33, 5))
            );
            this.pendingHarvestDrops.put(pos, cached);
         }

         List<ItemStack> inventory = this.inventoryAfterDrops(cached.drops);
         if (inventory != null && this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18)) {
            this.level.levelEvent(2001, pos, Block.getId(var6));
            this.commitHarvestInventory(inventory);
            this.forgetHarvestTarget(pos);
            return true;
         } else {
            return false;
         }
      }
   }

   private boolean tryPlant() {
      Optional<TreeFarmBlockEntity.PlantingTarget> target = this.findPlantingTarget();
      if (target.isEmpty()) {
         return false;
      } else {
         TreeFarmBlockEntity.PlantingTarget plantingTarget = target.get();
         this.level.setBlock(plantingTarget.pos(), plantingTarget.state(), 3);
         ((ItemStack)this.items.get(plantingTarget.slot())).shrink(1);
         this.setChanged();
         return true;
      }
   }

   private boolean tryFertilize() {
      Optional<BlockPos> target = this.findFertilizeTarget();
      Optional<Integer> boneMealSlot = this.findBoneMealSlot();
      if (!target.isEmpty() && !boneMealSlot.isEmpty() && this.level instanceof ServerLevel serverLevel) {
         BlockPos var8 = target.get();
         BlockState state = this.level.getBlockState(var8);
         if (state.getBlock() instanceof BonemealableBlock bonemealable
            && bonemealable.isValidBonemealTarget(this.level, var8, state, false)
            && bonemealable.isBonemealSuccess(this.level, this.level.random, var8, state)) {
            bonemealable.performBonemeal(serverLevel, this.level.random, var8, state);
            this.treePlanDirty = true;
            ((ItemStack)this.items.get(boneMealSlot.get())).shrink(1);
            this.level.levelEvent(2005, var8, 0);
            this.setChanged();
            return true;
         }

         return false;
      } else {
         return false;
      }
   }

   private Optional<ItemEntity> findPickup() {
      return this.level
         .getEntitiesOfClass(ItemEntity.class, this.workAabb(), item -> item.isAlive() && this.isTreeDrop(item.getItem()) && this.canInsertAny(item.getItem()))
         .stream()
         .findFirst();
   }

   private Optional<BlockPos> findShearTarget() {
      return this.findShearsSlot().isEmpty() ? Optional.empty() : this.firstValidTarget(this.findTreeTargets().leaves, this::isHarvestLeafLike);
   }

   private Optional<BlockPos> findLeafTarget() {
      return this.firstValidTarget(this.findTreeTargets().leaves, this::isHarvestLeafLike);
   }

   private Optional<BlockPos> findLogTarget() {
      return this.firstValidTarget(this.findTreeTargets().logs, this::isHarvestLogLike);
   }

   private Optional<BlockPos> findPlantTarget() {
      return !this.hasPlantable()
         ? Optional.empty()
         : this.scanGroundTargetsFromCursor().stream().map(this::plantPos).filter(Optional::isPresent).map(Optional::get).findFirst();
   }

   private Optional<TreeFarmBlockEntity.PlantingTarget> findPlantingTarget() {
      if (!this.hasPlantable()) {
         return Optional.empty();
      } else {
         for (BlockPos workPos : this.scanGroundTargetsFromCursor()) {
            Optional<BlockPos> target = this.plantPos(workPos);
            if (!target.isEmpty()) {
               for (int slot = 27; slot < 30; slot++) {
                  ItemStack stack = (ItemStack)this.items.get(slot);
                  BlockState sapling = this.blockStateForPlantable(stack);
                  if (!stack.isEmpty() && sapling != null && this.canPlant(target.get(), sapling)) {
                     return Optional.of(new TreeFarmBlockEntity.PlantingTarget(target.get(), slot, sapling));
                  }
               }
            }
         }

         return Optional.empty();
      }
   }

   private Optional<BlockPos> findFertilizeTarget() {
      return this.findBoneMealSlot().isEmpty()
         ? Optional.empty()
         : this.scanGroundTargetsFromCursor()
            .stream()
            .flatMap(pos -> Stream.of(pos, pos.above()))
            .filter(this::canFertilize)
            .findFirst();
   }

   private boolean canFertilize(BlockPos pos) {
      BlockState state = this.level.getBlockState(pos);
      if (state.getBlock() instanceof BonemealableBlock bonemealable && bonemealable.isValidBonemealTarget(this.level, pos, state, false)) {
         return true;
      }

      return false;
   }

   private TreeFarmBlockEntity.TreeTargets findTreeTargets() {
      if (this.level != null && !this.level.isClientSide) {
         long gameTime = this.level.getGameTime();
         if (this.treePlanDirty || gameTime >= this.treePlanRefreshAt || this.refreshWhenExhausted && !this.hasPendingTreeTargets()) {
            this.rebuildTreeTargets(gameTime);
         }

         return this.treeTargets;
      } else {
         return this.treeTargets;
      }
   }

   private List<BlockPos> scanGroundTargets() {
      if (this.groundTargetsCache != null) {
         return this.groundTargetsCache;
      } else {
         List<BlockPos> targets = new ArrayList<>();
         BlockPos min = this.getBoundsMin();
         BlockPos max = this.getBoundsMax();

         for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
               BlockPos pos = new BlockPos(x, min.getY(), z);
               if (this.isTarget(pos)) {
                  targets.add(pos);
               }
            }
         }

         this.groundTargetsCache = List.copyOf(targets);
         return this.groundTargetsCache;
      }
   }

   private List<BlockPos> scanGroundTargetsFromCursor() {
      List<BlockPos> targets = this.scanGroundTargets();
      if (targets.size() <= 1) {
         return targets;
      } else {
         int start = Math.floorMod(this.targetScanCursor, targets.size());
         List<BlockPos> rotated = new ArrayList<>(targets.size());
         rotated.addAll(targets.subList(start, targets.size()));
         rotated.addAll(targets.subList(0, start));
         this.targetScanCursor = (start + 1) % targets.size();
         return rotated;
      }
   }

   private List<BlockPos> scanTreeArea() {
      List<BlockPos> targets = new ArrayList<>();

      for (BlockPos ground : this.scanGroundTargetsFromCursor()) {
         for (int y = 0; y <= 16; y++) {
            targets.add(ground.above(y));
         }
      }

      return targets;
   }

   private void rebuildTreeTargets(long gameTime) {
      List<BlockPos> scan = this.scanTreeArea();
      Set<BlockPos> logPositions = new HashSet<>();
      Set<BlockPos> specialLeaves = new HashSet<>();
      List<BlockPos> specialLogs = new ArrayList<>();

      for (BlockPos pos : scan) {
         if (this.level.hasChunkAt(pos)) {
            BlockState state = this.level.getBlockState(pos);
            if (state.is(Blocks.MUSHROOM_STEM)) {
               specialLogs.add(pos);
            } else if (this.isLogLike(state)) {
               logPositions.add(pos);
            } else if (this.isSpecialLeaf(state)) {
               specialLeaves.add(pos);
            } else if (this.isChorusLike(state)) {
               specialLogs.add(pos);
            }
         }
      }

      Set<BlockPos> validLogs = new HashSet<>();
      Set<BlockPos> visited = new HashSet<>();
      List<BlockPos> seeds = new ArrayList<>();

      for (BlockPos posx : scan) {
         if (logPositions.contains(posx) && (posx.getY() <= this.getBoundsMin().getY() + 1 || this.isRememberedBlock(posx))) {
            seeds.add(posx);
         }
      }

      for (BlockPos posxx : this.treeTargets.logs) {
         if (this.isRememberedBlock(posxx) && this.isLogLike(this.level.getBlockState(posxx))) {
            seeds.add(posxx);
         }
      }

      for (BlockPos start : seeds) {
         if (visited.add(start)) {
            Set<BlockPos> component = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);

            while (!queue.isEmpty()) {
               BlockPos current = queue.remove();
               component.add(current);

               for (BlockPos neighbour : this.neighbours(current)) {
                  if (this.isTargetPosition(neighbour)
                     && this.level.hasChunkAt(neighbour)
                     && this.isLogLike(this.level.getBlockState(neighbour))
                     && !this.isOutsideTreeTrunk(neighbour)
                     && visited.add(neighbour)) {
                     queue.add(neighbour);
                  }
               }
            }

            boolean hasNearbyLeaves = component.stream()
               .anyMatch(
                  log -> this.neighbours(log)
                        .stream()
                        .anyMatch(
                           posxxx -> this.isTargetPosition(posxxx) && this.level.hasChunkAt(posxxx) && this.isNaturalLeaf(this.level.getBlockState(posxxx))
                        )
               );
            boolean fungus = component.stream()
               .anyMatch(
                  log -> this.neighbours(log)
                        .stream()
                        .anyMatch(
                           posxxx -> this.isTargetPosition(posxxx) && this.level.hasChunkAt(posxxx) && this.isSpecialLeaf(this.level.getBlockState(posxxx))
                        )
               );
            boolean rememberedTree = component.stream().anyMatch(this::isRememberedBlock);
            if (component.size() <= 384 && (hasNearbyLeaves || fungus || rememberedTree)) {
               validLogs.addAll(component);
            }
         }
      }

      List<BlockPos> oldLeaves = new ArrayList<>(this.treeTargets.leaves);
      List<BlockPos> oldLogs = new ArrayList<>(this.treeTargets.logs);
      ArrayDeque<BlockPos> logs = new ArrayDeque<>();
      ArrayDeque<BlockPos> leaves = new ArrayDeque<>();
      Set<BlockPos> queuedLogs = new HashSet<>();
      Set<BlockPos> queuedLeaves = new HashSet<>();

      for (BlockPos posxxx : oldLogs) {
         if (this.isRememberedBlock(posxxx) && this.isHarvestLogLike(this.level.getBlockState(posxxx)) && queuedLogs.add(posxxx)) {
            logs.add(posxxx);
         }
      }

      for (BlockPos posxxxx : oldLeaves) {
         if (this.isRememberedBlock(posxxxx) && this.isHarvestLeafLike(this.level.getBlockState(posxxxx)) && queuedLeaves.add(posxxxx)) {
            leaves.add(posxxxx);
         }
      }

      Set<BlockPos> crown = this.connectedCrown(validLogs);

      for (BlockPos posxxxxx : scan) {
         if (specialLeaves.contains(posxxxxx) && queuedLeaves.add(posxxxxx)) {
            leaves.add(posxxxxx);
         }
      }

      for (BlockPos posxxxxxx : validLogs) {
         if (logs.size() + leaves.size() < 16384 && queuedLogs.add(posxxxxxx)) {
            logs.add(posxxxxxx);
         }
      }

      for (BlockPos posxxxxxxx : crown) {
         if (logs.size() + leaves.size() < 16384 && queuedLeaves.add(posxxxxxxx)) {
            leaves.add(posxxxxxxx);
         }
      }

      for (BlockPos posxxxxxxxx : specialLogs) {
         if (logs.size() + leaves.size() < 16384 && queuedLogs.add(posxxxxxxxx)) {
            logs.add(posxxxxxxxx);
         }
      }

      this.plannedBlocks.clear();

      for (BlockPos posxxxxxxxxx : logs) {
         this.plannedBlocks.put(posxxxxxxxxx, this.level.getBlockState(posxxxxxxxxx).getBlock());
      }

      for (BlockPos posxxxxxxxxx : leaves) {
         this.plannedBlocks.put(posxxxxxxxxx, this.level.getBlockState(posxxxxxxxxx).getBlock());
      }

      this.pendingHarvestDrops.entrySet().removeIf(entry -> this.plannedBlocks.get(entry.getKey()) != entry.getValue().block);
      this.treeTargets = new TreeFarmBlockEntity.TreeTargets(logs, leaves);
      this.refreshWhenExhausted = !logs.isEmpty() || !leaves.isEmpty();
      this.treePlanDirty = false;
      this.treePlanRefreshAt = gameTime + 40L;
      this.setChanged();
   }

   private boolean hasPendingTreeTargets() {
      return this.firstValidTarget(this.treeTargets.leaves, this::isHarvestLeafLike).isPresent()
         || this.firstValidTarget(this.treeTargets.logs, this::isHarvestLogLike).isPresent();
   }

   private Optional<BlockPos> firstValidTarget(Queue<BlockPos> targets, Predicate<BlockState> predicate) {
      while (!targets.isEmpty()) {
         BlockPos pos = targets.peek();
         if (this.isRememberedBlock(pos) && predicate.test(this.level.getBlockState(pos))) {
            return Optional.of(pos);
         }

         targets.remove();
         this.plannedBlocks.remove(pos);
         this.pendingHarvestDrops.remove(pos);
      }

      return Optional.empty();
   }

   private boolean isTargetPosition(BlockPos pos) {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return pos.getX() >= min.getX() - 8
         && pos.getX() <= max.getX() + 8
         && pos.getZ() >= min.getZ() - 8
         && pos.getZ() <= max.getZ() + 8
         && pos.getY() >= min.getY()
         && pos.getY() <= min.getY() + 32;
   }

   private boolean isInsideSelectedColumn(BlockPos pos) {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return pos.getX() >= min.getX()
         && pos.getX() <= max.getX()
         && pos.getZ() >= min.getZ()
         && pos.getZ() <= max.getZ()
         && this.isTarget(new BlockPos(pos.getX(), min.getY(), pos.getZ()));
   }

   private boolean isOutsideTreeTrunk(BlockPos pos) {
      if (this.isInsideSelectedColumn(pos)) {
         return false;
      } else {
         for (int y = pos.getY(); y >= this.getBoundsMin().getY(); y--) {
            BlockPos below = new BlockPos(pos.getX(), y, pos.getZ());
            if (!this.level.hasChunkAt(below)) {
               return false;
            }

            BlockState state = this.level.getBlockState(below);
            if (!this.isLogLike(state)) {
               return y == this.getBoundsMin().getY()
                  && (state.is(BlockTags.DIRT) || state.is(Blocks.CRIMSON_NYLIUM) || state.is(Blocks.WARPED_NYLIUM));
            }
         }

         return true;
      }
   }

   private boolean isRememberedBlock(BlockPos pos) {
      return this.isTargetPosition(pos) && this.level.hasChunkAt(pos) && this.plannedBlocks.get(pos) == this.level.getBlockState(pos).getBlock();
   }

   private List<BlockPos> neighbours(BlockPos pos) {
      List<BlockPos> result = new ArrayList<>(26);

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               if (x != 0 || y != 0 || z != 0) {
                  result.add(pos.offset(x, y, z));
               }
            }
         }
      }

      return result;
   }

   private Set<BlockPos> connectedCrown(Set<BlockPos> logs) {
      Set<BlockPos> crown = new HashSet<>();
      Queue<BlockPos> frontier = new ArrayDeque<>();

      for (BlockPos log : logs) {
         for (BlockPos neighbour : this.neighbours(log)) {
            if (crown.size() < 16384 && this.isCrownTarget(neighbour, logs) && crown.add(neighbour)) {
               frontier.add(neighbour);
            }
         }
      }

      for (int distance = 1; distance < 5 && !frontier.isEmpty(); distance++) {
         int layerSize = frontier.size();

         for (int i = 0; i < layerSize; i++) {
            for (BlockPos neighbourx : this.neighbours(frontier.remove())) {
               if (crown.size() < 16384 && this.isCrownTarget(neighbourx, logs) && crown.add(neighbourx)) {
                  frontier.add(neighbourx);
               }
            }
         }
      }

      return crown;
   }

   private boolean isCrownTarget(BlockPos pos, Set<BlockPos> logs) {
      return this.isTargetPosition(pos) && this.level.hasChunkAt(pos) && this.isHarvestLeafLike(this.level.getBlockState(pos))
         ? this.neighbours(pos)
            .stream()
            .noneMatch(neighbour -> !logs.contains(neighbour) && this.level.hasChunkAt(neighbour) && this.isLogLike(this.level.getBlockState(neighbour)))
         : false;
   }

   private void forgetHarvestTarget(BlockPos pos) {
      this.plannedBlocks.remove(pos);
      this.pendingHarvestDrops.remove(pos);
      this.treeTargets.logs.remove(pos);
      this.treeTargets.leaves.remove(pos);
   }

   private void invalidateTreePlan() {
      this.groundTargetsCache = null;
      this.treeTargets = new TreeFarmBlockEntity.TreeTargets(new ArrayDeque<>(), new ArrayDeque<>());
      this.plannedBlocks.clear();
      this.pendingHarvestDrops.clear();
      this.treePlanDirty = true;
      this.refreshWhenExhausted = false;
   }

   private boolean isNaturalLeaf(BlockState state) {
      return state.is(BlockTags.LEAVES) && (!state.hasProperty(LeavesBlock.PERSISTENT) || !(Boolean)state.getValue(LeavesBlock.PERSISTENT));
   }

   private boolean isSpecialLeaf(BlockState state) {
      return state.is(Blocks.NETHER_WART_BLOCK)
         || state.is(Blocks.WARPED_WART_BLOCK)
         || state.is(Blocks.SHROOMLIGHT)
         || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
         || state.is(Blocks.RED_MUSHROOM_BLOCK);
   }

   private boolean isHarvestLeafLike(BlockState state) {
      return this.isNaturalLeaf(state) || this.isSpecialLeaf(state);
   }

   private boolean isHarvestLogLike(BlockState state) {
      return this.isLogLike(state) || this.isChorusLike(state);
   }

   private Optional<BlockPos> plantPos(BlockPos workPos) {
      if (this.level.getBlockState(workPos).canBeReplaced()) {
         return Optional.of(workPos);
      } else {
         return this.level.getBlockState(workPos.above()).canBeReplaced() ? Optional.of(workPos.above()) : Optional.empty();
      }
   }

   private boolean canPlant(BlockPos pos, BlockState sapling) {
      return this.level.getBlockState(pos).canBeReplaced() && sapling.canSurvive(this.level, pos);
   }

   @Nullable
   private BlockState blockStateForPlantable(ItemStack stack) {
      if (stack.getItem() instanceof BlockItem blockItem) {
         BlockState var4 = blockItem.getBlock().defaultBlockState();
         return this.isPlantableBlock(var4) ? var4 : null;
      } else {
         return null;
      }
   }

   public boolean isPlantable(ItemStack stack) {
      return this.blockStateForPlantable(stack) != null;
   }

   public boolean isMiscItem(ItemStack stack) {
      return stack.is(Items.BONE_MEAL) || stack.is(Items.SHEARS);
   }

   private boolean hasPlantable() {
      for (int slot = 27; slot < 30; slot++) {
         if (this.isPlantable((ItemStack)this.items.get(slot))) {
            return true;
         }
      }

      return false;
   }

   private Optional<Integer> findBoneMealSlot() {
      for (int slot = 30; slot < 33; slot++) {
         if (((ItemStack)this.items.get(slot)).is(Items.BONE_MEAL)) {
            return Optional.of(slot);
         }
      }

      return Optional.empty();
   }

   private Optional<Integer> findShearsSlot() {
      for (int slot = 30; slot < 33; slot++) {
         if (((ItemStack)this.items.get(slot)).is(Items.SHEARS)) {
            return Optional.of(slot);
         }
      }

      return Optional.empty();
   }

   private boolean isTreeDrop(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (!stack.is(Items.APPLE) && !stack.is(Items.STICK)) {
         BlockState state = this.blockStateForItem(stack);
         return state != null && (this.isPlantableBlock(state) || this.isLogLike(state) || this.isLeafLike(state) || this.isChorusLike(state));
      } else {
         return true;
      }
   }

   @Nullable
   private BlockState blockStateForItem(ItemStack stack) {
      return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : null;
   }

   private boolean isPlantableBlock(BlockState state) {
      return state.is(BlockTags.SAPLINGS)
         || state.is(Blocks.MANGROVE_PROPAGULE)
         || state.is(Blocks.BROWN_MUSHROOM)
         || state.is(Blocks.RED_MUSHROOM)
         || state.is(Blocks.CRIMSON_FUNGUS)
         || state.is(Blocks.WARPED_FUNGUS)
         || state.is(Blocks.AZALEA)
         || state.is(Blocks.FLOWERING_AZALEA)
         || state.is(Blocks.CHORUS_FLOWER);
   }

   private boolean isLogLike(BlockState state) {
      return state.is(BlockTags.LOGS) || state.is(Blocks.MUSHROOM_STEM);
   }

   private boolean isLeafLike(BlockState state) {
      return state.is(BlockTags.LEAVES)
         || state.is(Blocks.NETHER_WART_BLOCK)
         || state.is(Blocks.WARPED_WART_BLOCK)
         || state.is(Blocks.SHROOMLIGHT)
         || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
         || state.is(Blocks.RED_MUSHROOM_BLOCK);
   }

   private boolean isChorusLike(BlockState state) {
      return state.is(Blocks.CHORUS_PLANT) || state.is(Blocks.CHORUS_FLOWER);
   }

   private AABB workAabb() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return new AABB(
         (double)(min.getX() - 8),
         (double)min.getY(),
         (double)(min.getZ() - 8),
         (double)(max.getX() + 8) + 1.0,
         (double)(min.getY() + 32) + 1.0,
         (double)(max.getZ() + 8) + 1.0
      );
   }

   @Nullable
   private List<ItemStack> inventoryAfterDrops(List<ItemStack> drops) {
      List<ItemStack> inventory = new ArrayList<>(38);

      for (ItemStack item : this.items) {
         inventory.add(item.copy());
      }

      for (ItemStack drop : drops) {
         ItemStack remaining = this.insertStackInto(inventory, drop);
         if (!remaining.isEmpty()) {
            return null;
         }
      }

      return inventory;
   }

   private void commitHarvestInventory(List<ItemStack> inventory) {
      for (int slot = 0; slot < 38; slot++) {
         this.items.set(slot, inventory.get(slot));
      }

      this.setChanged();
   }

   private boolean canInsertAny(ItemStack stack) {
      List<ItemStack> inventory = new ArrayList<>(38);

      for (ItemStack item : this.items) {
         inventory.add(item.copy());
      }

      return this.insertStackInto(inventory, stack).getCount() < stack.getCount();
   }

   private ItemStack insertStack(ItemStack stack) {
      ItemStack remaining = this.insertStackInto(this.items, stack);
      if (remaining.getCount() < stack.getCount()) {
         this.setChanged();
      }

      return remaining;
   }

   private ItemStack insertStackInto(List<ItemStack> inventory, ItemStack stack) {
      ItemStack remaining = stack.copy();
      if (this.isPlantable(remaining)) {
         remaining = this.insertIntoRange(inventory, remaining, 27, 3);
      }

      return this.insertIntoRange(inventory, remaining, 0, 27);
   }

   private ItemStack insertIntoRange(List<ItemStack> inventory, ItemStack stack, int start, int count) {
      for (int pass = 0; pass < 2; pass++) {
         for (int slot = start; slot < start + count && !stack.isEmpty(); slot++) {
            ItemStack existing = inventory.get(slot);
            int limit = Math.min(stack.getMaxStackSize(), this.getMaxStackSize());
            if (pass == 0 && !existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)) {
               int moved = Math.min(stack.getCount(), Math.max(0, limit - existing.getCount()));
               existing.grow(moved);
               stack.shrink(moved);
            } else if (pass == 1 && existing.isEmpty()) {
               int moved = Math.min(stack.getCount(), limit);
               ItemStack placed = stack.copy();
               placed.setCount(moved);
               inventory.set(slot, placed);
               stack.shrink(moved);
            }
         }
      }

      return stack;
   }

   @Override
   public BlockPos getBoundsMin() {
      if (this.boundsMin == null) {
         this.resetDefaultBounds(this.worldPosition, this.getBlockState());
      }

      return this.boundsMin;
   }

   @Override
   public BlockPos getBoundsMax() {
      if (this.boundsMax == null) {
         this.resetDefaultBounds(this.worldPosition, this.getBlockState());
      }

      return this.boundsMax;
   }

   public int getMaxBoundsWidth() {
      return WorksiteUpgradeHelper.getBoundsMaxWidth(this, 33, 5);
   }

   public void adjustBounds(int minXDelta, int maxXDelta, int minZDelta, int maxZDelta) {
      WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjust(
         this.worldPosition, this.getBoundsMin(), this.getBoundsMax(), this.getMaxBoundsWidth(), minXDelta, maxXDelta, minZDelta, maxZDelta
      );
      if (adjusted != null) {
         this.boundsMin = adjusted.min();
         this.boundsMax = adjusted.max();
         this.invalidateTreePlan();
         this.saveAndSyncWorksiteState();
      }
   }

   public boolean isTarget(BlockPos pos) {
      int index = this.targetIndex(pos);
      return index >= 0 && index < this.targetMap.length && this.targetMap[index] == 1;
   }

   @Override
   public boolean containsWorkTarget(BlockPos pos) {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return pos.getX() >= min.getX()
         && pos.getX() <= max.getX()
         && pos.getZ() >= min.getZ()
         && pos.getZ() <= max.getZ()
         && pos.getY() >= min.getY()
         && pos.getY() <= min.getY() + 16;
   }

   public byte[] getTargetMap() {
      return (byte[])this.targetMap.clone();
   }

   public void setTargetMap(byte[] targetMap) {
      if (targetMap.length <= 256) {
         this.normalizeTargetMap(targetMap);
         this.invalidateTreePlan();
         this.setChanged();
      }
   }

   private int targetIndex(BlockPos pos) {
      int x = pos.getX() - this.getBoundsMin().getX();
      int z = pos.getZ() - this.getBoundsMin().getZ();
      return z * 16 + x;
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
      this.invalidateTreePlan();
      Direction facing = state.hasProperty(AutomationWorksiteBlock.FACING)
         ? ((Direction)state.getValue(AutomationWorksiteBlock.FACING)).getOpposite()
         : Direction.SOUTH;
      BlockPos near = pos.relative(facing);
      BlockPos far = pos.relative(facing, 5);
      if (facing.getAxis() == Axis.Z) {
         this.boundsMin = new BlockPos(pos.getX() - 2, pos.getY(), Math.min(near.getZ(), far.getZ()));
         this.boundsMax = new BlockPos(pos.getX() + 2, pos.getY(), Math.max(near.getZ(), far.getZ()));
      } else {
         this.boundsMin = new BlockPos(Math.min(near.getX(), far.getX()), pos.getY(), pos.getZ() - 2);
         this.boundsMax = new BlockPos(Math.max(near.getX(), far.getX()), pos.getY(), pos.getZ() + 2);
      }
   }

   private static byte[] createFilledTargetMap() {
      byte[] map = new byte[256];

      for (int i = 0; i < map.length; i++) {
         map[i] = 1;
      }

      return map;
   }

   @Override
   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.items);
      tag.putLong("BoundsMin", this.getBoundsMin().asLong());
      tag.putLong("BoundsMax", this.getBoundsMax().asLong());
      tag.putByteArray("TargetMap", this.targetMap);
      ListTag plan = new ListTag();

      for (Entry<BlockPos, Block> entry : this.plannedBlocks.entrySet()) {
         CompoundTag block = new CompoundTag();
         block.putLong("Pos", entry.getKey().asLong());
         block.putString("Block", BuiltInRegistries.BLOCK.getKey(entry.getValue()).toString());
         plan.add(block);
      }

      tag.put("HarvestPlan", plan);
   }

   @Override
   public void load(CompoundTag tag) {
      this.invalidateTreePlan();
      super.load(tag);
      ContainerHelper.loadAllItems(tag, this.items);
      if (tag.contains("BoundsMin")) {
         this.boundsMin = BlockPos.of(tag.getLong("BoundsMin"));
      }

      if (tag.contains("BoundsMax")) {
         this.boundsMax = BlockPos.of(tag.getLong("BoundsMax"));
      }

      if (tag.contains("TargetMap")) {
         byte[] loadedTargets = tag.getByteArray("TargetMap");
         if (loadedTargets.length <= 256) {
            this.normalizeTargetMap(loadedTargets);
         }
      }

      this.normalizeLoadedBounds();
      ListTag plan = tag.getList("HarvestPlan", 10);

      for (int i = 0; i < Math.min(plan.size(), 16384); i++) {
         CompoundTag entry = plan.getCompound(i);
         ResourceLocation blockId = ResourceLocation.tryParse(entry.getString("Block"));
         BlockPos pos = BlockPos.of(entry.getLong("Pos"));
         if (blockId != null && BuiltInRegistries.BLOCK.containsKey(blockId) && this.isTargetPosition(pos)) {
            Block block = (Block)BuiltInRegistries.BLOCK.get(blockId);
            BlockState state = block.defaultBlockState();
            if (this.isHarvestLogLike(state)) {
               this.treeTargets.logs.add(pos);
            } else {
               if (!this.isHarvestLeafLike(state)) {
                  continue;
               }

               this.treeTargets.leaves.add(pos);
            }

            this.plannedBlocks.put(pos, block);
         }
      }
   }

   private void normalizeTargetMap(byte[] loadedTargets) {
      byte[] map = createFilledTargetMap();
      int oldWidth = (int)Math.round(Math.sqrt((double)loadedTargets.length));
      if (oldWidth > 0 && oldWidth * oldWidth == loadedTargets.length && oldWidth <= 16) {
         for (int z = 0; z < oldWidth; z++) {
            for (int x = 0; x < oldWidth; x++) {
               map[z * 16 + x] = loadedTargets[z * oldWidth + x];
            }
         }
      }

      this.targetMap = map;
   }

   private void normalizeLoadedBounds() {
      if (this.level == null || !this.level.isClientSide) {
         if (this.boundsMin != null && this.boundsMax != null && !WorksiteBoundsGeometry.hasValidSize(this.boundsMin, this.boundsMax, this.getMaxBoundsWidth())
            )
          {
            this.resetDefaultBounds(this.worldPosition, this.getBlockState());
         }
      }
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      tag.putLong("BoundsMin", this.getBoundsMin().asLong());
      tag.putLong("BoundsMax", this.getBoundsMax().asLong());
      tag.putByteArray("TargetMap", this.targetMap);
      return tag;
   }

   public int getContainerSize() {
      return 38;
   }

   public boolean isEmpty() {
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int slot) {
      return this.isValidSlot(slot) ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      ItemStack removed = this.isValidSlot(slot) ? ContainerHelper.removeItem(this.items, slot, amount) : ItemStack.EMPTY;
      if (!removed.isEmpty()) {
         this.setChanged();
      }

      return removed;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      return this.isValidSlot(slot) ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
   }

   public void setItem(int slot, ItemStack stack) {
      if (this.isValidSlot(slot)) {
         this.items.set(slot, stack);
         if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
         }

         this.setChanged();
         if (slot >= 33 && slot < 38) {
            this.refreshChunkLoading();
         }
      }
   }

   public boolean stillValid(Player player) {
      return this.level != null
         && this.level.getBlockEntity(this.worldPosition) == this
         && player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) <= 64.0;
   }

   public void clearContent() {
      this.items.clear();
      this.setChanged();
   }

   public Component getDisplayName() {
      return Component.translatable(this.getDescriptionId());
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new TreeFarmMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new TreeFarmBoundsMenu(containerId, playerInventory, this.worldPosition);
   }

   @Override
   protected BasicWorksiteBlockEntity.UpgradeSlots getUpgradeSlots() {
      return new BasicWorksiteBlockEntity.UpgradeSlots(33, 5);
   }

   @Override
   protected boolean isOutputSlot(int slot) {
      return slot >= 0 && slot < 27;
   }

   @Override
   protected boolean isInputSlot(int slot) {
      return slot >= 27 && slot < 30;
   }

   @Override
   protected boolean isSpecialSlot(int slot) {
      return slot >= 30 && slot < 33;
   }

   private boolean isValidSlot(int slot) {
      return slot >= 0 && slot < 38;
   }

   private static record HarvestDrops(Block block, List<ItemStack> drops) {
   }

   private static record PlantingTarget(BlockPos pos, int slot, BlockState state) {
   }

   private static record TreeTargets(Queue<BlockPos> logs, Queue<BlockPos> leaves) {
   }
}
