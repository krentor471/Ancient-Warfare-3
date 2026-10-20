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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(38, ItemStack.f_41583_);
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
      return this.f_58857_ != null && !this.f_58857_.f_46443_
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
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && worker.canWorkAt(this.getWorkType())) {
         this.storedEnergy = Math.min(
            this.getMaxEnergy(), this.storedEnergy + Math.max(1, Math.round(1600.0F * worker.getWorkEffectiveness(this.getWorkType())))
         );
         this.processStoredEnergy();
         this.m_6596_();
      }
   }

   @Override
   protected void onWorkEnergyAdded() {
      this.processStoredEnergy();
   }

   @Override
   protected void onStoredEnergyTick() {
      this.processStoredEnergy();
      this.m_6596_();
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
         int originalCount = item.m_32055_().m_41613_();
         ItemStack remaining = this.insertStack(item.m_32055_());
         if (remaining.m_41613_() == originalCount) {
            return false;
         } else {
            if (remaining.m_41619_()) {
               item.m_146870_();
            } else {
               item.m_32045_(remaining);
            }

            return true;
         }
      }
   }

   private boolean tryShearLeaf() {
      Optional<BlockPos> target = this.findShearTarget();
      Optional<Integer> shearsSlot = this.findShearsSlot();
      if (!target.isEmpty() && !shearsSlot.isEmpty() && this.f_58857_ instanceof ServerLevel serverLevel) {
         BlockPos var8 = target.get();
         BlockState state = this.f_58857_.m_8055_(var8);
         List<ItemStack> inventory = this.inventoryAfterDrops(List.of(new ItemStack(state.m_60734_().m_5456_())));
         if (inventory != null && this.f_58857_.m_7731_(var8, Blocks.f_50016_.m_49966_(), 18)) {
            this.commitHarvestInventory(inventory);
            this.forgetHarvestTarget(var8);
            ItemStack shears = (ItemStack)this.items.get(shearsSlot.get());
            if (shears.m_220157_(1, serverLevel.f_46441_, null)) {
               shears.m_41774_(1);
            }

            this.m_6596_();
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
      if (!(this.f_58857_ instanceof ServerLevel serverLevel)) {
         return false;
      } else {
         BlockState var6 = this.f_58857_.m_8055_(pos);
         TreeFarmBlockEntity.HarvestDrops cached = this.pendingHarvestDrops.get(pos);
         if (cached == null || cached.block != var6.m_60734_()) {
            cached = new TreeFarmBlockEntity.HarvestDrops(
               var6.m_60734_(),
               Block.m_49874_(var6, serverLevel, pos, this.f_58857_.m_7702_(pos), null, WorksiteUpgradeHelper.fortuneTool(Items.f_42391_, this, 33, 5))
            );
            this.pendingHarvestDrops.put(pos, cached);
         }

         List<ItemStack> inventory = this.inventoryAfterDrops(cached.drops);
         if (inventory != null && this.f_58857_.m_7731_(pos, Blocks.f_50016_.m_49966_(), 18)) {
            this.f_58857_.m_46796_(2001, pos, Block.m_49956_(var6));
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
         this.f_58857_.m_7731_(plantingTarget.pos(), plantingTarget.state(), 3);
         ((ItemStack)this.items.get(plantingTarget.slot())).m_41774_(1);
         this.m_6596_();
         return true;
      }
   }

   private boolean tryFertilize() {
      Optional<BlockPos> target = this.findFertilizeTarget();
      Optional<Integer> boneMealSlot = this.findBoneMealSlot();
      if (!target.isEmpty() && !boneMealSlot.isEmpty() && this.f_58857_ instanceof ServerLevel serverLevel) {
         BlockPos var8 = target.get();
         BlockState state = this.f_58857_.m_8055_(var8);
         if (state.m_60734_() instanceof BonemealableBlock bonemealable
            && bonemealable.m_7370_(this.f_58857_, var8, state, false)
            && bonemealable.m_214167_(this.f_58857_, this.f_58857_.f_46441_, var8, state)) {
            bonemealable.m_214148_(serverLevel, this.f_58857_.f_46441_, var8, state);
            this.treePlanDirty = true;
            ((ItemStack)this.items.get(boneMealSlot.get())).m_41774_(1);
            this.f_58857_.m_46796_(2005, var8, 0);
            this.m_6596_();
            return true;
         }

         return false;
      } else {
         return false;
      }
   }

   private Optional<ItemEntity> findPickup() {
      return this.f_58857_
         .m_6443_(ItemEntity.class, this.workAabb(), item -> item.m_6084_() && this.isTreeDrop(item.m_32055_()) && this.canInsertAny(item.m_32055_()))
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
                  if (!stack.m_41619_() && sapling != null && this.canPlant(target.get(), sapling)) {
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
            .flatMap(pos -> (Stream<? extends BlockPos>)List.of(pos, pos.m_7494_()).stream())
            .filter(this::canFertilize)
            .findFirst();
   }

   private boolean canFertilize(BlockPos pos) {
      BlockState state = this.f_58857_.m_8055_(pos);
      if (state.m_60734_() instanceof BonemealableBlock bonemealable && bonemealable.m_7370_(this.f_58857_, pos, state, false)) {
         return true;
      }

      return false;
   }

   private TreeFarmBlockEntity.TreeTargets findTreeTargets() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         long gameTime = this.f_58857_.m_46467_();
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

         for (int x = min.m_123341_(); x <= max.m_123341_(); x++) {
            for (int z = min.m_123343_(); z <= max.m_123343_(); z++) {
               BlockPos pos = new BlockPos(x, min.m_123342_(), z);
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
            targets.add(ground.m_6630_(y));
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
         if (this.f_58857_.m_46805_(pos)) {
            BlockState state = this.f_58857_.m_8055_(pos);
            if (state.m_60713_(Blocks.f_50182_)) {
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
         if (logPositions.contains(posx) && (posx.m_123342_() <= this.getBoundsMin().m_123342_() + 1 || this.isRememberedBlock(posx))) {
            seeds.add(posx);
         }
      }

      for (BlockPos posxx : this.treeTargets.logs) {
         if (this.isRememberedBlock(posxx) && this.isLogLike(this.f_58857_.m_8055_(posxx))) {
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
                     && this.f_58857_.m_46805_(neighbour)
                     && this.isLogLike(this.f_58857_.m_8055_(neighbour))
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
                           posxxx -> this.isTargetPosition(posxxx) && this.f_58857_.m_46805_(posxxx) && this.isNaturalLeaf(this.f_58857_.m_8055_(posxxx))
                        )
               );
            boolean fungus = component.stream()
               .anyMatch(
                  log -> this.neighbours(log)
                        .stream()
                        .anyMatch(
                           posxxx -> this.isTargetPosition(posxxx) && this.f_58857_.m_46805_(posxxx) && this.isSpecialLeaf(this.f_58857_.m_8055_(posxxx))
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
         if (this.isRememberedBlock(posxxx) && this.isHarvestLogLike(this.f_58857_.m_8055_(posxxx)) && queuedLogs.add(posxxx)) {
            logs.add(posxxx);
         }
      }

      for (BlockPos posxxxx : oldLeaves) {
         if (this.isRememberedBlock(posxxxx) && this.isHarvestLeafLike(this.f_58857_.m_8055_(posxxxx)) && queuedLeaves.add(posxxxx)) {
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
         this.plannedBlocks.put(posxxxxxxxxx, this.f_58857_.m_8055_(posxxxxxxxxx).m_60734_());
      }

      for (BlockPos posxxxxxxxxx : leaves) {
         this.plannedBlocks.put(posxxxxxxxxx, this.f_58857_.m_8055_(posxxxxxxxxx).m_60734_());
      }

      this.pendingHarvestDrops.entrySet().removeIf(entry -> this.plannedBlocks.get(entry.getKey()) != entry.getValue().block);
      this.treeTargets = new TreeFarmBlockEntity.TreeTargets(logs, leaves);
      this.refreshWhenExhausted = !logs.isEmpty() || !leaves.isEmpty();
      this.treePlanDirty = false;
      this.treePlanRefreshAt = gameTime + 40L;
      this.m_6596_();
   }

   private boolean hasPendingTreeTargets() {
      return this.firstValidTarget(this.treeTargets.leaves, this::isHarvestLeafLike).isPresent()
         || this.firstValidTarget(this.treeTargets.logs, this::isHarvestLogLike).isPresent();
   }

   private Optional<BlockPos> firstValidTarget(Queue<BlockPos> targets, Predicate<BlockState> predicate) {
      while (!targets.isEmpty()) {
         BlockPos pos = targets.peek();
         if (this.isRememberedBlock(pos) && predicate.test(this.f_58857_.m_8055_(pos))) {
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
      return pos.m_123341_() >= min.m_123341_() - 8
         && pos.m_123341_() <= max.m_123341_() + 8
         && pos.m_123343_() >= min.m_123343_() - 8
         && pos.m_123343_() <= max.m_123343_() + 8
         && pos.m_123342_() >= min.m_123342_()
         && pos.m_123342_() <= min.m_123342_() + 32;
   }

   private boolean isInsideSelectedColumn(BlockPos pos) {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return pos.m_123341_() >= min.m_123341_()
         && pos.m_123341_() <= max.m_123341_()
         && pos.m_123343_() >= min.m_123343_()
         && pos.m_123343_() <= max.m_123343_()
         && this.isTarget(new BlockPos(pos.m_123341_(), min.m_123342_(), pos.m_123343_()));
   }

   private boolean isOutsideTreeTrunk(BlockPos pos) {
      if (this.isInsideSelectedColumn(pos)) {
         return false;
      } else {
         for (int y = pos.m_123342_(); y >= this.getBoundsMin().m_123342_(); y--) {
            BlockPos below = new BlockPos(pos.m_123341_(), y, pos.m_123343_());
            if (!this.f_58857_.m_46805_(below)) {
               return false;
            }

            BlockState state = this.f_58857_.m_8055_(below);
            if (!this.isLogLike(state)) {
               return y == this.getBoundsMin().m_123342_()
                  && (state.m_204336_(BlockTags.f_144274_) || state.m_60713_(Blocks.f_50699_) || state.m_60713_(Blocks.f_50690_));
            }
         }

         return true;
      }
   }

   private boolean isRememberedBlock(BlockPos pos) {
      return this.isTargetPosition(pos) && this.f_58857_.m_46805_(pos) && this.plannedBlocks.get(pos) == this.f_58857_.m_8055_(pos).m_60734_();
   }

   private List<BlockPos> neighbours(BlockPos pos) {
      List<BlockPos> result = new ArrayList<>(26);

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               if (x != 0 || y != 0 || z != 0) {
                  result.add(pos.m_7918_(x, y, z));
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
      return this.isTargetPosition(pos) && this.f_58857_.m_46805_(pos) && this.isHarvestLeafLike(this.f_58857_.m_8055_(pos))
         ? this.neighbours(pos)
            .stream()
            .noneMatch(neighbour -> !logs.contains(neighbour) && this.f_58857_.m_46805_(neighbour) && this.isLogLike(this.f_58857_.m_8055_(neighbour)))
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
      return state.m_204336_(BlockTags.f_13035_) && (!state.m_61138_(LeavesBlock.f_54419_) || !(Boolean)state.m_61143_(LeavesBlock.f_54419_));
   }

   private boolean isSpecialLeaf(BlockState state) {
      return state.m_60713_(Blocks.f_50451_)
         || state.m_60713_(Blocks.f_50692_)
         || state.m_60713_(Blocks.f_50701_)
         || state.m_60713_(Blocks.f_50180_)
         || state.m_60713_(Blocks.f_50181_);
   }

   private boolean isHarvestLeafLike(BlockState state) {
      return this.isNaturalLeaf(state) || this.isSpecialLeaf(state);
   }

   private boolean isHarvestLogLike(BlockState state) {
      return this.isLogLike(state) || this.isChorusLike(state);
   }

   private Optional<BlockPos> plantPos(BlockPos workPos) {
      if (this.f_58857_.m_8055_(workPos).m_247087_()) {
         return Optional.of(workPos);
      } else {
         return this.f_58857_.m_8055_(workPos.m_7494_()).m_247087_() ? Optional.of(workPos.m_7494_()) : Optional.empty();
      }
   }

   private boolean canPlant(BlockPos pos, BlockState sapling) {
      return this.f_58857_.m_8055_(pos).m_247087_() && sapling.m_60710_(this.f_58857_, pos);
   }

   @Nullable
   private BlockState blockStateForPlantable(ItemStack stack) {
      if (stack.m_41720_() instanceof BlockItem blockItem) {
         BlockState var4 = blockItem.m_40614_().m_49966_();
         return this.isPlantableBlock(var4) ? var4 : null;
      } else {
         return null;
      }
   }

   public boolean isPlantable(ItemStack stack) {
      return this.blockStateForPlantable(stack) != null;
   }

   public boolean isMiscItem(ItemStack stack) {
      return stack.m_150930_(Items.f_42499_) || stack.m_150930_(Items.f_42574_);
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
         if (((ItemStack)this.items.get(slot)).m_150930_(Items.f_42499_)) {
            return Optional.of(slot);
         }
      }

      return Optional.empty();
   }

   private Optional<Integer> findShearsSlot() {
      for (int slot = 30; slot < 33; slot++) {
         if (((ItemStack)this.items.get(slot)).m_150930_(Items.f_42574_)) {
            return Optional.of(slot);
         }
      }

      return Optional.empty();
   }

   private boolean isTreeDrop(ItemStack stack) {
      if (stack.m_41619_()) {
         return false;
      } else if (!stack.m_150930_(Items.f_42410_) && !stack.m_150930_(Items.f_42398_)) {
         BlockState state = this.blockStateForItem(stack);
         return state != null && (this.isPlantableBlock(state) || this.isLogLike(state) || this.isLeafLike(state) || this.isChorusLike(state));
      } else {
         return true;
      }
   }

   @Nullable
   private BlockState blockStateForItem(ItemStack stack) {
      return stack.m_41720_() instanceof BlockItem blockItem ? blockItem.m_40614_().m_49966_() : null;
   }

   private boolean isPlantableBlock(BlockState state) {
      return state.m_204336_(BlockTags.f_13104_)
         || state.m_60713_(Blocks.f_220831_)
         || state.m_60713_(Blocks.f_50072_)
         || state.m_60713_(Blocks.f_50073_)
         || state.m_60713_(Blocks.f_50700_)
         || state.m_60713_(Blocks.f_50691_)
         || state.m_60713_(Blocks.f_152541_)
         || state.m_60713_(Blocks.f_152542_)
         || state.m_60713_(Blocks.f_50491_);
   }

   private boolean isLogLike(BlockState state) {
      return state.m_204336_(BlockTags.f_13106_) || state.m_60713_(Blocks.f_50182_);
   }

   private boolean isLeafLike(BlockState state) {
      return state.m_204336_(BlockTags.f_13035_)
         || state.m_60713_(Blocks.f_50451_)
         || state.m_60713_(Blocks.f_50692_)
         || state.m_60713_(Blocks.f_50701_)
         || state.m_60713_(Blocks.f_50180_)
         || state.m_60713_(Blocks.f_50181_);
   }

   private boolean isChorusLike(BlockState state) {
      return state.m_60713_(Blocks.f_50490_) || state.m_60713_(Blocks.f_50491_);
   }

   private AABB workAabb() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return new AABB(
         (double)(min.m_123341_() - 8),
         (double)min.m_123342_(),
         (double)(min.m_123343_() - 8),
         (double)(max.m_123341_() + 8) + 1.0,
         (double)(min.m_123342_() + 32) + 1.0,
         (double)(max.m_123343_() + 8) + 1.0
      );
   }

   @Nullable
   private List<ItemStack> inventoryAfterDrops(List<ItemStack> drops) {
      List<ItemStack> inventory = new ArrayList<>(38);

      for (ItemStack item : this.items) {
         inventory.add(item.m_41777_());
      }

      for (ItemStack drop : drops) {
         ItemStack remaining = this.insertStackInto(inventory, drop);
         if (!remaining.m_41619_()) {
            return null;
         }
      }

      return inventory;
   }

   private void commitHarvestInventory(List<ItemStack> inventory) {
      for (int slot = 0; slot < 38; slot++) {
         this.items.set(slot, inventory.get(slot));
      }

      this.m_6596_();
   }

   private boolean canInsertAny(ItemStack stack) {
      List<ItemStack> inventory = new ArrayList<>(38);

      for (ItemStack item : this.items) {
         inventory.add(item.m_41777_());
      }

      return this.insertStackInto(inventory, stack).m_41613_() < stack.m_41613_();
   }

   private ItemStack insertStack(ItemStack stack) {
      ItemStack remaining = this.insertStackInto(this.items, stack);
      if (remaining.m_41613_() < stack.m_41613_()) {
         this.m_6596_();
      }

      return remaining;
   }

   private ItemStack insertStackInto(List<ItemStack> inventory, ItemStack stack) {
      ItemStack remaining = stack.m_41777_();
      if (this.isPlantable(remaining)) {
         remaining = this.insertIntoRange(inventory, remaining, 27, 3);
      }

      return this.insertIntoRange(inventory, remaining, 0, 27);
   }

   private ItemStack insertIntoRange(List<ItemStack> inventory, ItemStack stack, int start, int count) {
      for (int pass = 0; pass < 2; pass++) {
         for (int slot = start; slot < start + count && !stack.m_41619_(); slot++) {
            ItemStack existing = inventory.get(slot);
            int limit = Math.min(stack.m_41741_(), this.m_6893_());
            if (pass == 0 && !existing.m_41619_() && ItemStack.m_150942_(existing, stack)) {
               int moved = Math.min(stack.m_41613_(), Math.max(0, limit - existing.m_41613_()));
               existing.m_41769_(moved);
               stack.m_41774_(moved);
            } else if (pass == 1 && existing.m_41619_()) {
               int moved = Math.min(stack.m_41613_(), limit);
               ItemStack placed = stack.m_41777_();
               placed.m_41764_(moved);
               inventory.set(slot, placed);
               stack.m_41774_(moved);
            }
         }
      }

      return stack;
   }

   @Override
   public BlockPos getBoundsMin() {
      if (this.boundsMin == null) {
         this.resetDefaultBounds(this.f_58858_, this.m_58900_());
      }

      return this.boundsMin;
   }

   @Override
   public BlockPos getBoundsMax() {
      if (this.boundsMax == null) {
         this.resetDefaultBounds(this.f_58858_, this.m_58900_());
      }

      return this.boundsMax;
   }

   public int getMaxBoundsWidth() {
      return WorksiteUpgradeHelper.getBoundsMaxWidth(this, 33, 5);
   }

   public void adjustBounds(int minXDelta, int maxXDelta, int minZDelta, int maxZDelta) {
      WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjust(
         this.f_58858_, this.getBoundsMin(), this.getBoundsMax(), this.getMaxBoundsWidth(), minXDelta, maxXDelta, minZDelta, maxZDelta
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
      return pos.m_123341_() >= min.m_123341_()
         && pos.m_123341_() <= max.m_123341_()
         && pos.m_123343_() >= min.m_123343_()
         && pos.m_123343_() <= max.m_123343_()
         && pos.m_123342_() >= min.m_123342_()
         && pos.m_123342_() <= min.m_123342_() + 16;
   }

   public byte[] getTargetMap() {
      return (byte[])this.targetMap.clone();
   }

   public void setTargetMap(byte[] targetMap) {
      if (targetMap.length <= 256) {
         this.normalizeTargetMap(targetMap);
         this.invalidateTreePlan();
         this.m_6596_();
      }
   }

   private int targetIndex(BlockPos pos) {
      int x = pos.m_123341_() - this.getBoundsMin().m_123341_();
      int z = pos.m_123343_() - this.getBoundsMin().m_123343_();
      return z * 16 + x;
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
      this.invalidateTreePlan();
      Direction facing = state.m_61138_(AutomationWorksiteBlock.FACING)
         ? ((Direction)state.m_61143_(AutomationWorksiteBlock.FACING)).m_122424_()
         : Direction.SOUTH;
      BlockPos near = pos.m_121945_(facing);
      BlockPos far = pos.m_5484_(facing, 5);
      if (facing.m_122434_() == Axis.Z) {
         this.boundsMin = new BlockPos(pos.m_123341_() - 2, pos.m_123342_(), Math.min(near.m_123343_(), far.m_123343_()));
         this.boundsMax = new BlockPos(pos.m_123341_() + 2, pos.m_123342_(), Math.max(near.m_123343_(), far.m_123343_()));
      } else {
         this.boundsMin = new BlockPos(Math.min(near.m_123341_(), far.m_123341_()), pos.m_123342_(), pos.m_123343_() - 2);
         this.boundsMax = new BlockPos(Math.max(near.m_123341_(), far.m_123341_()), pos.m_123342_(), pos.m_123343_() + 2);
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
   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
      tag.m_128356_("BoundsMin", this.getBoundsMin().m_121878_());
      tag.m_128356_("BoundsMax", this.getBoundsMax().m_121878_());
      tag.m_128382_("TargetMap", this.targetMap);
      ListTag plan = new ListTag();

      for (Entry<BlockPos, Block> entry : this.plannedBlocks.entrySet()) {
         CompoundTag block = new CompoundTag();
         block.m_128356_("Pos", entry.getKey().m_121878_());
         block.m_128359_("Block", BuiltInRegistries.f_256975_.m_7981_(entry.getValue()).toString());
         plan.add(block);
      }

      tag.m_128365_("HarvestPlan", plan);
   }

   @Override
   public void m_142466_(CompoundTag tag) {
      this.invalidateTreePlan();
      super.m_142466_(tag);
      ContainerHelper.m_18980_(tag, this.items);
      if (tag.m_128441_("BoundsMin")) {
         this.boundsMin = BlockPos.m_122022_(tag.m_128454_("BoundsMin"));
      }

      if (tag.m_128441_("BoundsMax")) {
         this.boundsMax = BlockPos.m_122022_(tag.m_128454_("BoundsMax"));
      }

      if (tag.m_128441_("TargetMap")) {
         byte[] loadedTargets = tag.m_128463_("TargetMap");
         if (loadedTargets.length <= 256) {
            this.normalizeTargetMap(loadedTargets);
         }
      }

      this.normalizeLoadedBounds();
      ListTag plan = tag.m_128437_("HarvestPlan", 10);

      for (int i = 0; i < Math.min(plan.size(), 16384); i++) {
         CompoundTag entry = plan.m_128728_(i);
         ResourceLocation blockId = ResourceLocation.m_135820_(entry.m_128461_("Block"));
         BlockPos pos = BlockPos.m_122022_(entry.m_128454_("Pos"));
         if (blockId != null && BuiltInRegistries.f_256975_.m_7804_(blockId) && this.isTargetPosition(pos)) {
            Block block = (Block)BuiltInRegistries.f_256975_.m_7745_(blockId);
            BlockState state = block.m_49966_();
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
      if (this.f_58857_ == null || !this.f_58857_.f_46443_) {
         if (this.boundsMin != null && this.boundsMax != null && !WorksiteBoundsGeometry.hasValidSize(this.boundsMin, this.boundsMax, this.getMaxBoundsWidth())
            )
          {
            this.resetDefaultBounds(this.f_58858_, this.m_58900_());
         }
      }
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = super.m_5995_();
      tag.m_128356_("BoundsMin", this.getBoundsMin().m_121878_());
      tag.m_128356_("BoundsMax", this.getBoundsMax().m_121878_());
      tag.m_128382_("TargetMap", this.targetMap);
      return tag;
   }

   public int m_6643_() {
      return 38;
   }

   public boolean m_7983_() {
      return this.items.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int slot) {
      return this.isValidSlot(slot) ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      ItemStack removed = this.isValidSlot(slot) ? ContainerHelper.m_18969_(this.items, slot, amount) : ItemStack.f_41583_;
      if (!removed.m_41619_()) {
         this.m_6596_();
      }

      return removed;
   }

   public ItemStack m_8016_(int slot) {
      return this.isValidSlot(slot) ? ContainerHelper.m_18966_(this.items, slot) : ItemStack.f_41583_;
   }

   public void m_6836_(int slot, ItemStack stack) {
      if (this.isValidSlot(slot)) {
         this.items.set(slot, stack);
         if (!stack.m_41619_() && stack.m_41613_() > this.m_6893_()) {
            stack.m_41764_(this.m_6893_());
         }

         this.m_6596_();
         if (slot >= 33 && slot < 38) {
            this.refreshChunkLoading();
         }
      }
   }

   public boolean m_6542_(Player player) {
      return this.f_58857_ != null
         && this.f_58857_.m_7702_(this.f_58858_) == this
         && player.m_20275_((double)this.f_58858_.m_123341_() + 0.5, (double)this.f_58858_.m_123342_() + 0.5, (double)this.f_58858_.m_123343_() + 0.5) <= 64.0;
   }

   public void m_6211_() {
      this.items.clear();
      this.m_6596_();
   }

   public Component m_5446_() {
      return Component.m_237115_(this.getDescriptionId());
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new TreeFarmMenu(containerId, playerInventory, this.f_58858_);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new TreeFarmBoundsMenu(containerId, playerInventory, this.f_58858_);
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
