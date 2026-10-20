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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(38, ItemStack.f_41583_);
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
      return this.f_58857_ != null && !this.f_58857_.f_46443_
         ? this.findHarvestTarget().isPresent() || this.findPlantingTarget().isPresent() || this.findFertilizeTarget().isPresent()
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
      if (!target.isEmpty() && this.f_58857_ instanceof ServerLevel serverLevel) {
         BlockPos var7 = target.get();
         BlockState state = this.f_58857_.m_8055_(var7);
         if (this.isSweetBerryHarvestable(state)) {
            int age = (Integer)state.m_61143_(SweetBerryBushBlock.f_57244_);
            ItemStack berries = new ItemStack(Items.f_42780_, 1 + this.f_58857_.f_46441_.m_188503_(2) + (age == 3 ? 1 : 0));
            this.f_58857_.m_7731_(var7, (BlockState)state.m_61124_(SweetBerryBushBlock.f_57244_, 1), 3);
            this.f_58857_.m_5594_(null, var7, SoundEvents.f_12457_, SoundSource.BLOCKS, 1.0F, 0.8F + this.f_58857_.f_46441_.m_188501_() * 0.4F);
            this.insertOrDrop(var7, berries);
            return true;
         } else if (this.hasGlowBerries(state)) {
            this.f_58857_.m_7731_(var7, (BlockState)state.m_61124_(CaveVines.f_152949_, false), 3);
            this.f_58857_.m_5594_(null, var7, SoundEvents.f_144088_, SoundSource.BLOCKS, 1.0F, 0.8F + this.f_58857_.f_46441_.m_188501_() * 0.4F);
            this.insertOrDrop(var7, new ItemStack(Items.f_151079_));
            return true;
         } else {
            List<ItemStack> drops = new ArrayList<>(
               Block.m_49874_(state, serverLevel, var7, this.f_58857_.m_7702_(var7), null, WorksiteUpgradeHelper.fortuneTool(Items.f_42391_, this, 33, 5))
            );
            if (drops.isEmpty()) {
               return false;
            } else {
               this.f_58857_.m_46961_(var7, false);
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

   private Optional<BlockPos> findHarvestTarget() {
      return this.scanFruitArea().stream().filter(this::isHarvestableFruit).findFirst();
   }

   private Optional<FruitFarmBlockEntity.PlantingTarget> findPlantingTarget() {
      if (!this.hasPlantable()) {
         return Optional.empty();
      } else {
         for (BlockPos pos : this.scanFruitArea()) {
            if (this.f_58857_.m_8055_(pos).m_247087_()) {
               for (int slot = 27; slot < 30; slot++) {
                  BlockState fruit = this.fruitForPlantable((ItemStack)this.items.get(slot), pos);
                  if (fruit != null && fruit.m_60710_(this.f_58857_, pos)) {
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

      for (int x = min.m_123341_(); x <= max.m_123341_(); x++) {
         for (int z = min.m_123343_(); z <= max.m_123343_(); z++) {
            BlockPos ground = new BlockPos(x, min.m_123342_(), z);
            if (this.isTarget(ground)) {
               for (int y = 0; y < 7; y++) {
                  targets.add(ground.m_6630_(y));
               }
            }
         }
      }

      return targets;
   }

   private boolean isHarvestableFruit(BlockPos pos) {
      BlockState state = this.f_58857_.m_8055_(pos);
      return this.isMatureCocoa(state) || this.isSweetBerryHarvestable(state) || this.hasGlowBerries(state);
   }

   private boolean canFertilize(BlockPos pos) {
      BlockState state = this.f_58857_.m_8055_(pos);
      if (state.m_60734_() instanceof BonemealableBlock bonemealable && bonemealable.m_7370_(this.f_58857_, pos, state, false)) {
         return true;
      }

      return false;
   }

   private boolean isMatureCocoa(BlockState state) {
      return state.m_60713_(Blocks.f_50262_) && (Integer)state.m_61143_(CocoaBlock.f_51736_) >= 2;
   }

   private boolean isSweetBerryHarvestable(BlockState state) {
      return state.m_60713_(Blocks.f_50685_) && (Integer)state.m_61143_(SweetBerryBushBlock.f_57244_) >= 2;
   }

   private boolean hasGlowBerries(BlockState state) {
      return state.m_61138_(CaveVines.f_152949_) && (Boolean)state.m_61143_(CaveVines.f_152949_);
   }

   @Nullable
   private BlockState fruitForPlantable(ItemStack stack, BlockPos pos) {
      if (stack.m_150930_(Items.f_42533_)) {
         for (Direction direction : Plane.HORIZONTAL) {
            BlockState cocoa = (BlockState)Blocks.f_50262_.m_49966_().m_61124_(CocoaBlock.f_54117_, direction);
            if (cocoa.m_60710_(this.f_58857_, pos)) {
               return cocoa;
            }
         }
      }

      if (stack.m_150930_(Items.f_42780_)) {
         return Blocks.f_50685_.m_49966_();
      } else {
         return stack.m_150930_(Items.f_151079_) ? (BlockState)Blocks.f_152538_.m_49966_().m_61124_(CaveVines.f_152949_, true) : null;
      }
   }

   public boolean isPlantable(ItemStack stack) {
      return stack.m_150930_(Items.f_42533_) || stack.m_150930_(Items.f_42780_) || stack.m_150930_(Items.f_151079_);
   }

   public boolean isMiscItem(ItemStack stack) {
      return stack.m_150930_(Items.f_42499_);
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

   private void insertOrDrop(BlockPos pos, List<ItemStack> drops) {
      for (ItemStack drop : drops) {
         this.insertOrDrop(pos, drop);
      }
   }

   private void insertOrDrop(BlockPos pos, ItemStack stack) {
      if (!stack.m_41619_()) {
         ItemStack remaining = this.insertStack(stack);
         if (!remaining.m_41619_()) {
            Block.m_49840_(this.f_58857_, pos, remaining);
         }
      }
   }

   private ItemStack insertStack(ItemStack stack) {
      ItemStack remaining = stack.m_41777_();
      if (this.isPlantable(remaining)) {
         remaining = this.insertIntoRange(remaining, 27, 3);
      }

      return this.insertIntoRange(remaining, 0, 27);
   }

   private ItemStack insertIntoRange(ItemStack stack, int start, int count) {
      for (int slot = start; slot < start + count && !stack.m_41619_(); slot++) {
         ItemStack existing = (ItemStack)this.items.get(slot);
         if (existing.m_41619_()) {
            this.items.set(slot, stack.m_41777_());
            stack.m_41764_(0);
         } else if (ItemStack.m_150942_(existing, stack) && existing.m_41613_() < existing.m_41741_()) {
            int moved = Math.min(stack.m_41613_(), existing.m_41741_() - existing.m_41613_());
            existing.m_41769_(moved);
            stack.m_41774_(moved);
         }
      }

      this.m_6596_();
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
         && pos.m_123342_() < min.m_123342_() + 7;
   }

   public byte[] getTargetMap() {
      return (byte[])this.targetMap.clone();
   }

   public void setTargetMap(byte[] targetMap) {
      if (targetMap.length <= 256) {
         this.normalizeTargetMap(targetMap);
         this.m_6596_();
      }
   }

   private int targetIndex(BlockPos pos) {
      int x = pos.m_123341_() - this.getBoundsMin().m_123341_();
      int z = pos.m_123343_() - this.getBoundsMin().m_123343_();
      return z * 16 + x;
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
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
   }

   @Override
   public void m_142466_(CompoundTag tag) {
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
      return new FruitFarmMenu(containerId, playerInventory, this.f_58858_);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new FruitFarmBoundsMenu(containerId, playerInventory, this.f_58858_);
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
