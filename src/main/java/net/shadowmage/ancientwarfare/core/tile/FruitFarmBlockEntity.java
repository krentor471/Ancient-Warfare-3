package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.AutomationWorksiteBlock;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.BoundedWorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteBoundsGeometry;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgradeHelper;
import net.shadowmage.ancientwarfare.core.menu.FruitFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.FruitFarmMenu;
import org.jetbrains.annotations.Nullable;

public class FruitFarmBlockEntity extends BasicWorksiteBlockEntity implements Container, MenuProvider, BoundedWorkSite {
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
   private static final int WORK_COST = 100;
   private static final int WORKER_ENERGY_PER_VISIT = 1600;
   private static final int MAX_HEIGHT_SEARCH = 7;
   private final NonNullList<ItemStack> items = NonNullList.withSize(38, ItemStack.EMPTY);
   private BlockPos boundsMin;
   private BlockPos boundsMax;
   private byte[] targetMap = createFilledTargetMap();

   public FruitFarmBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.FRUIT_FARM), pos, state);
      this.resetDefaultBounds(pos, state);
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.FARMING;
   }

   @Override
   public String getDescriptionId() {
      return "block.ancientwarfare.fruit_farm";
   }

   @Override
   public boolean hasWork() {
      return this.level != null && !this.level.isClientSide
         ? this.findHarvestTarget().isPresent() || this.findPlantingTarget().isPresent() || this.findFertilizeTarget().isPresent()
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
      return this.tryHarvest() || this.tryPlant() || this.tryFertilize();
   }

   private boolean tryHarvest() {
      Optional<BlockPos> target = this.findHarvestTarget();
      if (!target.isEmpty() && this.level instanceof ServerLevel serverLevel) {
         BlockPos var7 = target.get();
         BlockState state = this.level.getBlockState(var7);
         if (this.isSweetBerryHarvestable(state)) {
            int age = (Integer)state.getValue(SweetBerryBushBlock.AGE);
            ItemStack berries = new ItemStack(Items.SWEET_BERRIES, 1 + this.level.random.nextInt(2) + (age == 3 ? 1 : 0));
            this.level.setBlock(var7, (BlockState)state.setValue(SweetBerryBushBlock.AGE, 1), 3);
            this.level.playSound(null, var7, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.insertOrDrop(var7, berries);
            return true;
         } else if (this.hasGlowBerries(state)) {
            this.level.setBlock(var7, (BlockState)state.setValue(CaveVines.BERRIES, false), 3);
            this.level.playSound(null, var7, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.insertOrDrop(var7, new ItemStack(Items.GLOW_BERRIES));
            return true;
         } else {
            List<ItemStack> drops = new ArrayList<>(
               Block.getDrops(state, serverLevel, var7, this.level.getBlockEntity(var7), null, WorksiteUpgradeHelper.fortuneTool(Items.DIAMOND_AXE, this, 33, 5))
            );
            if (drops.isEmpty()) {
               return false;
            } else {
               this.level.destroyBlock(var7, false);
               this.insertOrDrop(var7, drops);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean tryPlant() {
      Optional<FruitFarmBlockEntity.PlantingTarget> target = this.findPlantingTarget();
      if (target.isEmpty()) {
         return false;
      } else {
         FruitFarmBlockEntity.PlantingTarget plantingTarget = target.get();
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

   private Optional<BlockPos> findHarvestTarget() {
      return this.scanFruitArea().stream().filter(this::isHarvestableFruit).findFirst();
   }

   private Optional<FruitFarmBlockEntity.PlantingTarget> findPlantingTarget() {
      if (!this.hasPlantable()) {
         return Optional.empty();
      } else {
         for (BlockPos pos : this.scanFruitArea()) {
            if (this.level.getBlockState(pos).canBeReplaced()) {
               for (int slot = 27; slot < 30; slot++) {
                  BlockState fruit = this.fruitForPlantable((ItemStack)this.items.get(slot), pos);
                  if (fruit != null && fruit.canSurvive(this.level, pos)) {
                     return Optional.of(new FruitFarmBlockEntity.PlantingTarget(pos, slot, fruit));
                  }
               }
            }
         }

         return Optional.empty();
      }
   }

   private Optional<BlockPos> findFertilizeTarget() {
      return this.findBoneMealSlot().isEmpty() ? Optional.empty() : this.scanFruitArea().stream().filter(this::canFertilize).findFirst();
   }

   private List<BlockPos> scanFruitArea() {
      List<BlockPos> targets = new ArrayList<>();
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();

      for (int x = min.getX(); x <= max.getX(); x++) {
         for (int z = min.getZ(); z <= max.getZ(); z++) {
            BlockPos ground = new BlockPos(x, min.getY(), z);
            if (this.isTarget(ground)) {
               for (int y = 0; y < 7; y++) {
                  targets.add(ground.above(y));
               }
            }
         }
      }

      return targets;
   }

   private boolean isHarvestableFruit(BlockPos pos) {
      BlockState state = this.level.getBlockState(pos);
      return this.isMatureCocoa(state) || this.isSweetBerryHarvestable(state) || this.hasGlowBerries(state);
   }

   private boolean canFertilize(BlockPos pos) {
      BlockState state = this.level.getBlockState(pos);
      if (state.getBlock() instanceof BonemealableBlock bonemealable && bonemealable.isValidBonemealTarget(this.level, pos, state, false)) {
         return true;
      }

      return false;
   }

   private boolean isMatureCocoa(BlockState state) {
      return state.is(Blocks.COCOA) && (Integer)state.getValue(CocoaBlock.AGE) >= 2;
   }

   private boolean isSweetBerryHarvestable(BlockState state) {
      return state.is(Blocks.SWEET_BERRY_BUSH) && (Integer)state.getValue(SweetBerryBushBlock.AGE) >= 2;
   }

   private boolean hasGlowBerries(BlockState state) {
      return state.hasProperty(CaveVines.BERRIES) && (Boolean)state.getValue(CaveVines.BERRIES);
   }

   @Nullable
   private BlockState fruitForPlantable(ItemStack stack, BlockPos pos) {
      if (stack.is(Items.COCOA_BEANS)) {
         for (Direction direction : Plane.HORIZONTAL) {
            BlockState cocoa = (BlockState)Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, direction);
            if (cocoa.canSurvive(this.level, pos)) {
               return cocoa;
            }
         }
      }

      if (stack.is(Items.SWEET_BERRIES)) {
         return Blocks.SWEET_BERRY_BUSH.defaultBlockState();
      } else {
         return stack.is(Items.GLOW_BERRIES) ? (BlockState)Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, true) : null;
      }
   }

   public boolean isPlantable(ItemStack stack) {
      return stack.is(Items.COCOA_BEANS) || stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES);
   }

   public boolean isMiscItem(ItemStack stack) {
      return stack.is(Items.BONE_MEAL);
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

   private void insertOrDrop(BlockPos pos, List<ItemStack> drops) {
      for (ItemStack drop : drops) {
         this.insertOrDrop(pos, drop);
      }
   }

   private void insertOrDrop(BlockPos pos, ItemStack stack) {
      if (!stack.isEmpty()) {
         ItemStack remaining = this.insertStack(stack);
         if (!remaining.isEmpty()) {
            Block.popResource(this.level, pos, remaining);
         }
      }
   }

   private ItemStack insertStack(ItemStack stack) {
      ItemStack remaining = stack.copy();
      if (this.isPlantable(remaining)) {
         remaining = this.insertIntoRange(remaining, 27, 3);
      }

      return this.insertIntoRange(remaining, 0, 27);
   }

   private ItemStack insertIntoRange(ItemStack stack, int start, int count) {
      for (int slot = start; slot < start + count && !stack.isEmpty(); slot++) {
         ItemStack existing = (ItemStack)this.items.get(slot);
         if (existing.isEmpty()) {
            this.items.set(slot, stack.copy());
            stack.setCount(0);
         } else if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
            int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            stack.shrink(moved);
         }
      }

      this.setChanged();
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
         && pos.getY() < min.getY() + 7;
   }

   public byte[] getTargetMap() {
      return (byte[])this.targetMap.clone();
   }

   public void setTargetMap(byte[] targetMap) {
      if (targetMap.length <= 256) {
         this.normalizeTargetMap(targetMap);
         this.setChanged();
      }
   }

   private int targetIndex(BlockPos pos) {
      int x = pos.getX() - this.getBoundsMin().getX();
      int z = pos.getZ() - this.getBoundsMin().getZ();
      return z * 16 + x;
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
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
   }

   @Override
   public void load(CompoundTag tag) {
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
      return new FruitFarmMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new FruitFarmBoundsMenu(containerId, playerInventory, this.worldPosition);
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

   private static record PlantingTarget(BlockPos pos, int slot, BlockState state) {
   }
}
