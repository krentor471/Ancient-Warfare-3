package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction.Axis;
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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.shadowmage.ancientwarfare.core.block.AutomationWorksiteBlock;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.BoundedWorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteBoundsGeometry;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgradeHelper;
import net.shadowmage.ancientwarfare.core.menu.CropFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.CropFarmControlMenu;
import net.shadowmage.ancientwarfare.core.menu.CropFarmMenu;
import org.jetbrains.annotations.Nullable;

public class CropFarmBlockEntity extends BasicWorksiteBlockEntity implements Container, MenuProvider, BoundedWorkSite {
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
   private static final String AUTO_TILL_TAG = "AutoTill";
   private static final int WORK_COST = 100;
   private static final int WORKER_ENERGY_PER_VISIT = 1600;
   private final NonNullList<ItemStack> items = NonNullList.withSize(38, ItemStack.EMPTY);
   private BlockPos boundsMin;
   private BlockPos boundsMax;
   private byte[] targetMap = createFilledTargetMap();
   private boolean autoTill;

   public CropFarmBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.CROP_FARM), pos, state);
      this.resetDefaultBounds(pos, state);
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.FARMING;
   }

   @Override
   public String getDescriptionId() {
      return "block.ancientwarfare.crop_farm";
   }

   @Override
   public boolean hasWork() {
      return this.level != null && !this.level.isClientSide
         ? this.findHarvestTarget().isPresent()
            || this.findFertilizeTarget().isPresent()
            || this.findPlantTarget().isPresent()
            || this.autoTill && this.findTillTarget().isPresent()
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
      return this.tryHarvest() || this.tryFertilize() || this.tryPlant() || this.autoTill && this.tryTill();
   }

   private boolean tryHarvest() {
      Optional<BlockPos> target = this.findHarvestTarget();
      if (!target.isEmpty() && this.level instanceof ServerLevel serverLevel) {
         BlockPos var6 = this.harvestPos(target.get()).orElse(target.get());
         BlockState state = this.level.getBlockState(var6);
         if (this.tryHarvestPreservingPlant(var6, state)) {
            return true;
         } else {
            List<ItemStack> drops = new ArrayList<>(
               Block.getDrops(state, serverLevel, var6, this.level.getBlockEntity(var6), null, WorksiteUpgradeHelper.fortuneTool(Items.DIAMOND_HOE, this, 33, 5))
            );
            if (drops.isEmpty()) {
               return false;
            } else {
               this.level.destroyBlock(var6, false);
               if (this.shouldReplantAfterHarvest(state)) {
                  this.replantFromDrops(var6, drops);
               }

               this.insertOrDrop(var6, drops);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean tryHarvestPreservingPlant(BlockPos pos, BlockState state) {
      if (!this.isSweetBerryHarvestable(state)) {
         return false;
      } else {
         int age = (Integer)state.getValue(SweetBerryBushBlock.AGE);
         ItemStack berries = new ItemStack(Items.SWEET_BERRIES, 1 + this.level.random.nextInt(2) + (age == 3 ? 1 : 0));
         this.level.setBlock(pos, (BlockState)state.setValue(SweetBerryBushBlock.AGE, 1), 3);
         this.level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + this.level.random.nextFloat() * 0.4F);
         this.insertOrDrop(pos, List.of(berries));
         return true;
      }
   }

   private void replantFromDrops(BlockPos pos, List<ItemStack> drops) {
      for (ItemStack stack : drops) {
         BlockState crop = this.cropForPlantable(stack);
         if (!stack.isEmpty() && crop != null && this.canPlant(pos, crop)) {
            this.level.setBlock(pos, crop, 3);
            stack.shrink(1);
            return;
         }
      }
   }

   private boolean tryFertilize() {
      Optional<BlockPos> target = this.findFertilizeTarget();
      Optional<Integer> boneMealSlot = this.findBoneMealSlot();
      if (!target.isEmpty() && !boneMealSlot.isEmpty() && this.level instanceof ServerLevel serverLevel) {
         BlockPos var8 = this.fertilizablePos(target.get()).orElse(target.get());
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

   private boolean tryPlant() {
      for (BlockPos workTarget : this.scanTargets()) {
         Optional<BlockPos> plantingPosition = this.plantPos(workTarget);
         if (!plantingPosition.isEmpty()) {
            BlockPos pos = plantingPosition.get();

            for (int slot = 27; slot < 30; slot++) {
               ItemStack stack = (ItemStack)this.items.get(slot);
               BlockState crop = this.cropForPlantable(stack);
               if (!stack.isEmpty() && crop != null && this.canPlant(pos, crop)) {
                  this.level.setBlock(pos, crop, 3);
                  stack.shrink(1);
                  this.setChanged();
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean tryTill() {
      Optional<BlockPos> target = this.findTillTarget();
      if (target.isEmpty()) {
         return false;
      } else {
         BlockPos pos = target.get();
         this.level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
         this.level.levelEvent(1501, pos, 0);
         return true;
      }
   }

   private Optional<BlockPos> findHarvestTarget() {
      return this.scanTargets().stream().filter(this::isMatureCrop).findFirst();
   }

   private Optional<BlockPos> findFertilizeTarget() {
      return this.findBoneMealSlot().isEmpty() ? Optional.empty() : this.scanTargets().stream().filter(this::canFertilize).findFirst();
   }

   private Optional<BlockPos> findPlantTarget() {
      if (!this.hasPlantable()) {
         return Optional.empty();
      } else {
         for (BlockPos workTarget : this.scanTargets()) {
            Optional<BlockPos> plantingPosition = this.plantPos(workTarget);
            if (!plantingPosition.isEmpty()) {
               for (int slot = 27; slot < 30; slot++) {
                  BlockState crop = this.cropForPlantable((ItemStack)this.items.get(slot));
                  if (crop != null && this.canPlant(plantingPosition.get(), crop)) {
                     return plantingPosition;
                  }
               }
            }
         }

         return Optional.empty();
      }
   }

   private Optional<BlockPos> findTillTarget() {
      for (BlockPos workTarget : this.scanTargets()) {
         Optional<BlockPos> tillPosition = this.tillPos(workTarget);
         if (tillPosition.isPresent()) {
            return tillPosition;
         }
      }

      return Optional.empty();
   }

   private List<BlockPos> scanTargets() {
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

      return targets;
   }

   private boolean isMatureCrop(BlockPos pos) {
      Optional<BlockPos> harvestPos = this.harvestPos(pos);
      if (harvestPos.isEmpty()) {
         return false;
      } else {
         BlockState state = this.level.getBlockState(harvestPos.get());
         if (this.isFruitBlock(state)
            || this.isHighCropBlock(state)
            || this.isSweetBerryHarvestable(state)
            || this.isMatureNetherWart(state)
            || this.isMaturePitcherCrop(state)) {
            return true;
         } else {
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
               return true;
            }

            return false;
         }
      }
   }

   private boolean canFertilize(BlockPos pos) {
      Optional<BlockPos> fertilizablePos = this.fertilizablePos(pos);
      if (fertilizablePos.isEmpty()) {
         return false;
      } else {
         BlockState state = this.level.getBlockState(fertilizablePos.get());
         if (state.getBlock() instanceof BonemealableBlock bonemealable && bonemealable.isValidBonemealTarget(this.level, fertilizablePos.get(), state, false)) {
            return true;
         }

         return false;
      }
   }

   private Optional<BlockPos> cropPos(BlockPos workPos) {
      if (this.level.getBlockState(workPos).getBlock() instanceof CropBlock) {
         return Optional.of(workPos);
      } else {
         return this.level.getBlockState(workPos.above()).getBlock() instanceof CropBlock ? Optional.of(workPos.above()) : Optional.empty();
      }
   }

   private Optional<BlockPos> fertilizablePos(BlockPos workPos) {
      if (this.level.getBlockState(workPos).getBlock() instanceof BonemealableBlock) {
         return Optional.of(workPos);
      } else {
         return this.level.getBlockState(workPos.above()).getBlock() instanceof BonemealableBlock ? Optional.of(workPos.above()) : Optional.empty();
      }
   }

   private Optional<BlockPos> harvestPos(BlockPos workPos) {
      Optional<BlockPos> highCropPos = this.highCropHarvestPos(workPos);
      if (highCropPos.isPresent()) {
         return highCropPos;
      } else if (this.isFruitBlock(this.level.getBlockState(workPos))) {
         return Optional.of(workPos);
      } else if (this.isFruitBlock(this.level.getBlockState(workPos.above()))) {
         return Optional.of(workPos.above());
      } else if (!this.isSweetBerryHarvestable(this.level.getBlockState(workPos)) && !this.isMatureNetherWart(this.level.getBlockState(workPos))) {
         if (!this.isSweetBerryHarvestable(this.level.getBlockState(workPos.above())) && !this.isMatureNetherWart(this.level.getBlockState(workPos.above()))) {
            Optional<BlockPos> pitcherPos = this.maturePitcherCropPos(workPos);
            return pitcherPos.isPresent() ? pitcherPos : this.cropPos(workPos);
         } else {
            return Optional.of(workPos.above());
         }
      } else {
         return Optional.of(workPos);
      }
   }

   private Optional<BlockPos> maturePitcherCropPos(BlockPos workPos) {
      if (this.isMaturePitcherCrop(this.level.getBlockState(workPos))) {
         return Optional.of(this.lowerHalfPos(workPos));
      } else if (this.isMaturePitcherCrop(this.level.getBlockState(workPos.above()))) {
         return Optional.of(this.lowerHalfPos(workPos.above()));
      } else {
         return this.isMaturePitcherCrop(this.level.getBlockState(workPos.above(2))) ? Optional.of(this.lowerHalfPos(workPos.above(2))) : Optional.empty();
      }
   }

   private BlockPos lowerHalfPos(BlockPos pos) {
      BlockState state = this.level.getBlockState(pos);
      return state.hasProperty(DoublePlantBlock.HALF) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
   }

   private Optional<BlockPos> highCropHarvestPos(BlockPos workPos) {
      BlockPos base = null;
      if (this.isHighCropBlock(this.level.getBlockState(workPos))) {
         base = workPos;
      } else if (this.isHighCropBlock(this.level.getBlockState(workPos.above()))) {
         base = workPos.above();
      }

      if (base == null) {
         return Optional.empty();
      } else {
         while (this.isSameHighCrop(this.level.getBlockState(base), this.level.getBlockState(base.below()))) {
            base = base.below();
         }

         BlockState baseState = this.level.getBlockState(base);
         if (!baseState.is(Blocks.SUGAR_CANE) && !baseState.is(Blocks.BAMBOO)) {
            return baseState.is(Blocks.CACTUS) && this.level.getBlockState(base.above()).is(Blocks.CACTUS)
               ? Optional.of(base.above())
               : Optional.empty();
         } else {
            return this.isSameHighCrop(baseState, this.level.getBlockState(base.above())) ? Optional.of(base.above()) : Optional.empty();
         }
      }
   }

   private boolean isSameHighCrop(BlockState first, BlockState second) {
      return first.is(Blocks.SUGAR_CANE) && second.is(Blocks.SUGAR_CANE)
         || first.is(Blocks.BAMBOO) && second.is(Blocks.BAMBOO)
         || first.is(Blocks.CACTUS) && second.is(Blocks.CACTUS);
   }

   private boolean isHighCropBlock(BlockState state) {
      return state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO) || state.is(Blocks.CACTUS);
   }

   private boolean isFruitBlock(BlockState state) {
      return state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON);
   }

   private boolean isSweetBerryHarvestable(BlockState state) {
      return state.is(Blocks.SWEET_BERRY_BUSH) && (Integer)state.getValue(SweetBerryBushBlock.AGE) >= 2;
   }

   private boolean isMatureNetherWart(BlockState state) {
      return state.is(Blocks.NETHER_WART) && (Integer)state.getValue(NetherWartBlock.AGE) >= 3;
   }

   private boolean isMaturePitcherCrop(BlockState state) {
      return state.is(Blocks.PITCHER_CROP) && (Integer)state.getValue(PitcherCropBlock.AGE) >= 4;
   }

   private boolean shouldReplantAfterHarvest(BlockState harvestedState) {
      return !this.isFruitBlock(harvestedState) && !this.isHighCropBlock(harvestedState);
   }

   private Optional<BlockPos> plantPos(BlockPos workPos) {
      if (this.level.getBlockState(workPos).canBeReplaced() && this.level.getBlockState(workPos.below()).is(Blocks.FARMLAND)) {
         return Optional.of(workPos);
      } else {
         return !this.level.getBlockState(workPos).canBeReplaced() && this.level.getBlockState(workPos.above()).canBeReplaced()
            ? Optional.of(workPos.above())
            : Optional.empty();
      }
   }

   private Optional<BlockPos> tillPos(BlockPos workPos) {
      if (this.isTillable(this.level.getBlockState(workPos)) && this.level.getBlockState(workPos.above()).canBeReplaced()) {
         return Optional.of(workPos);
      } else {
         return this.level.getBlockState(workPos).canBeReplaced() && this.isTillable(this.level.getBlockState(workPos.below()))
            ? Optional.of(workPos.below())
            : Optional.empty();
      }
   }

   private boolean isTillable(BlockState state) {
      return state.is(Blocks.DIRT)
         || state.is(Blocks.GRASS_BLOCK)
         || state.is(Blocks.DIRT_PATH)
         || state.is(Blocks.COARSE_DIRT)
         || state.is(Blocks.ROOTED_DIRT);
   }

   public boolean autoTill() {
      return this.autoTill;
   }

   public void toggleAutoTill() {
      this.autoTill = !this.autoTill;
      this.saveAndSyncWorksiteState();
   }

   private boolean hasPlantable() {
      for (int slot = 27; slot < 30; slot++) {
         if (this.cropForPlantable((ItemStack)this.items.get(slot)) != null) {
            return true;
         }
      }

      return false;
   }

   private Optional<Integer> findBoneMealSlot() {
      for (int slot = 30; slot < 33; slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (stack.is(Items.BONE_MEAL)) {
            return Optional.of(slot);
         }
      }

      return Optional.empty();
   }

   @Nullable
   private BlockState cropForPlantable(ItemStack stack) {
      if (stack.is(Items.WHEAT_SEEDS)) {
         return Blocks.WHEAT.defaultBlockState();
      } else if (stack.is(Items.CARROT)) {
         return Blocks.CARROTS.defaultBlockState();
      } else if (stack.is(Items.POTATO)) {
         return Blocks.POTATOES.defaultBlockState();
      } else if (stack.is(Items.BEETROOT_SEEDS)) {
         return Blocks.BEETROOTS.defaultBlockState();
      } else if (stack.is(Items.PUMPKIN_SEEDS)) {
         return Blocks.PUMPKIN_STEM.defaultBlockState();
      } else if (stack.is(Items.MELON_SEEDS)) {
         return Blocks.MELON_STEM.defaultBlockState();
      } else if (stack.is(Items.SUGAR_CANE)) {
         return Blocks.SUGAR_CANE.defaultBlockState();
      } else if (stack.is(Items.NETHER_WART)) {
         return Blocks.NETHER_WART.defaultBlockState();
      } else if (stack.is(Items.SWEET_BERRIES)) {
         return Blocks.SWEET_BERRY_BUSH.defaultBlockState();
      } else if (stack.is(Items.BAMBOO)) {
         return Blocks.BAMBOO_SAPLING.defaultBlockState();
      } else if (stack.is(Items.CACTUS)) {
         return Blocks.CACTUS.defaultBlockState();
      } else if (stack.is(Items.TORCHFLOWER_SEEDS)) {
         return Blocks.TORCHFLOWER_CROP.defaultBlockState();
      } else {
         return stack.is(Items.PITCHER_POD) ? Blocks.PITCHER_CROP.defaultBlockState() : null;
      }
   }

   private boolean canPlant(BlockPos pos, BlockState crop) {
      return this.level.getBlockState(pos).canBeReplaced() && !this.wouldExtendColumnCrop(pos, crop) && crop.canSurvive(this.level, pos);
   }

   private boolean wouldExtendColumnCrop(BlockPos pos, BlockState crop) {
      BlockState below = this.level.getBlockState(pos.below());
      if (crop.is(Blocks.SUGAR_CANE)) {
         return below.is(Blocks.SUGAR_CANE);
      } else {
         return !crop.is(Blocks.BAMBOO_SAPLING) && !crop.is(Blocks.BAMBOO) ? false : below.is(Blocks.BAMBOO_SAPLING) || below.is(Blocks.BAMBOO);
      }
   }

   public boolean isPlantable(ItemStack stack) {
      return this.cropForPlantable(stack) != null;
   }

   public boolean isMiscItem(ItemStack stack) {
      return stack.is(Items.BONE_MEAL);
   }

   private void insertOrDrop(BlockPos pos, List<ItemStack> drops) {
      for (ItemStack drop : drops) {
         if (!drop.isEmpty()) {
            ItemStack remaining = this.insertStack(drop);
            if (!remaining.isEmpty()) {
               Block.popResource(this.level, pos, remaining);
            }
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
         && Math.abs(pos.getY() - min.getY()) <= 1;
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
      tag.putBoolean("AutoTill", this.autoTill);
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

      this.autoTill = tag.getBoolean("AutoTill");
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
         if (this.boundsMin != null
            && this.boundsMax != null
            && this.boundsMin.getY() == this.worldPosition.getY() + 1
            && this.boundsMax.getY() == this.worldPosition.getY() + 1) {
            this.boundsMin = this.boundsMin.below();
            this.boundsMax = this.boundsMax.below();
         }

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
      tag.putBoolean("AutoTill", this.autoTill);
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
      return new CropFarmMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new CropFarmBoundsMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createControlMenu(int containerId, Inventory playerInventory) {
      return new CropFarmControlMenu(containerId, playerInventory, this.worldPosition);
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
}
