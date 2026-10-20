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
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.AutomationWorksiteBlock;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.BoundedWorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteBoundsGeometry;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteInventoryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteRelativeSide;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgradeHelper;
import net.shadowmage.ancientwarfare.core.menu.QuarryBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.QuarryControlMenu;
import net.shadowmage.ancientwarfare.core.menu.QuarryMenu;
import org.jetbrains.annotations.Nullable;

public class QuarryBlockEntity extends BasicWorksiteBlockEntity implements Container, MenuProvider, BoundedWorkSite {
   public static final int MAIN_SLOT_START = 0;
   public static final int MAIN_SLOT_COUNT = 45;
   public static final int UPGRADE_SLOT_START = 45;
   public static final int UPGRADE_SLOT_COUNT = 5;
   public static final int SLOT_COUNT = 50;
   public static final int DEFAULT_SIZE = 5;
   public static final int MAX_BOUNDS_WIDTH = 64;
   private static final String MIN_TAG = "BoundsMin";
   private static final String MAX_TAG = "BoundsMax";
   private static final String CURRENT_TAG = "Current";
   private static final String FINISHED_TAG = "Finished";
   private static final String ONLY_MINE_ORES_TAG = "OnlyMineOres";
   private static final int WORK_COST = 100;
   private static final int WORKER_ENERGY_PER_VISIT = 1600;
   private final NonNullList<ItemStack> items = NonNullList.withSize(50, ItemStack.EMPTY);
   private BlockPos boundsMin;
   private BlockPos boundsMax;
   private BlockPos current;
   private boolean finished;
   private boolean onlyMineOres;

   public QuarryBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.QUARRY), pos, state);
      this.resetDefaultBounds(pos, state);
      this.resetCurrent();
   }

   @Override
   protected void resetDefaultSideAccess() {
      super.resetDefaultSideAccess();
      this.setSideAccess(WorksiteRelativeSide.TOP, WorksiteInventoryAccess.NONE);
      this.setSideAccess(WorksiteRelativeSide.FRONT, WorksiteInventoryAccess.NONE);
      this.setSideAccess(WorksiteRelativeSide.BACK, WorksiteInventoryAccess.NONE);
      this.setSideAccess(WorksiteRelativeSide.LEFT, WorksiteInventoryAccess.NONE);
      this.setSideAccess(WorksiteRelativeSide.RIGHT, WorksiteInventoryAccess.NONE);
      this.setSideAccess(WorksiteRelativeSide.BOTTOM, WorksiteInventoryAccess.OUTPUT);
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.MINING;
   }

   @Override
   public String getDescriptionId() {
      return "block.ancientwarfare.quarry";
   }

   @Override
   public boolean hasWork() {
      if (this.level != null && !this.level.isClientSide) {
         if (this.finished) {
            this.resetCurrent();
         }

         return this.findNextMineTarget().isPresent();
      } else {
         return false;
      }
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.level != null && !this.level.isClientSide && worker.canWorkAt(this.getWorkType()) && this.hasWork()) {
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
      return WorksiteUpgradeHelper.adjustedWorkCost(100, this, 45, 5);
   }

   private boolean processNextAction() {
      Optional<BlockPos> target = this.findNextMineTarget();
      if (!target.isEmpty() && this.level instanceof ServerLevel serverLevel) {
         BlockPos var6 = target.get();
         BlockState state = this.level.getBlockState(var6);
         ArrayList drops = new ArrayList(
            Block.getDrops(state, serverLevel, var6, this.level.getBlockEntity(var6), null, WorksiteUpgradeHelper.fortuneTool(Items.DIAMOND_PICKAXE, this, 45, 5))
         );
         if (!this.canInsertAll(drops)) {
            this.finished = false;
            return false;
         } else {
            this.level.setBlock(var6, Blocks.AIR.defaultBlockState(), 3);
            this.insertOrDrop(var6, drops);
            this.current = this.nextPosition(var6);
            this.setChanged();
            return true;
         }
      } else {
         this.finished = target.isEmpty();
         return false;
      }
   }

   private Optional<BlockPos> findNextMineTarget() {
      if (this.level != null && !this.finished) {
         for (BlockPos cursor = this.normalizeCurrent(); cursor != null; cursor = this.nextPosition(cursor)) {
            if (this.isMineable(cursor)) {
               return Optional.of(cursor);
            }
         }

         return Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   private boolean isMineable(BlockPos pos) {
      BlockState state = this.level.getBlockState(pos);
      return !state.isAir() && !state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty() && (!this.onlyMineOres || AWCorePlatform.isOre(state));
   }

   public boolean onlyMineOres() {
      return this.onlyMineOres;
   }

   public void toggleOnlyMineOres() {
      this.onlyMineOres = !this.onlyMineOres;
      this.resetCurrent();
      this.saveAndSyncWorksiteState();
   }

   @Nullable
   private BlockPos nextPosition(BlockPos pos) {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      if (pos.getX() < max.getX()) {
         return pos.east();
      } else if (pos.getZ() < max.getZ()) {
         return new BlockPos(min.getX(), pos.getY(), pos.getZ() + 1);
      } else {
         return pos.getY() > this.level.getMinBuildHeight() ? new BlockPos(min.getX(), pos.getY() - 1, min.getZ()) : null;
      }
   }

   private BlockPos normalizeCurrent() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      if (this.current == null
         || this.current.getX() < min.getX()
         || this.current.getX() > max.getX()
         || this.current.getZ() < min.getZ()
         || this.current.getZ() > max.getZ()
         || this.current.getY() > max.getY()
         || this.current.getY() < this.level.getMinBuildHeight()) {
         this.resetCurrent();
      }

      return this.current;
   }

   private void resetCurrent() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      this.current = new BlockPos(min.getX(), max.getY(), min.getZ());
      this.finished = false;
   }

   private boolean canInsertAll(List<ItemStack> drops) {
      NonNullList<ItemStack> simulated = NonNullList.withSize(45, ItemStack.EMPTY);

      for (int slot = 0; slot < 45; slot++) {
         simulated.set(slot, ((ItemStack)this.items.get(0 + slot)).copy());
      }

      for (ItemStack drop : drops) {
         ItemStack remaining = this.insertIntoList(drop.copy(), simulated);
         if (!remaining.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private ItemStack insertIntoList(ItemStack stack, NonNullList<ItemStack> target) {
      for (int slot = 0; slot < target.size() && !stack.isEmpty(); slot++) {
         ItemStack existing = (ItemStack)target.get(slot);
         if (existing.isEmpty()) {
            target.set(slot, stack.copy());
            stack.setCount(0);
         } else if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
            int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            stack.shrink(moved);
         }
      }

      return stack;
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
      return this.insertIntoRange(stack.copy(), 0, 45);
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
      return WorksiteUpgradeHelper.getQuarryBoundsMaxWidth(this, 45, 5);
   }

   public void adjustBounds(int minXDelta, int maxXDelta, int minZDelta, int maxZDelta) {
      WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjust(
         this.worldPosition, this.getBoundsMin(), this.getBoundsMax(), this.getMaxBoundsWidth(), minXDelta, maxXDelta, minZDelta, maxZDelta
      );
      if (adjusted != null) {
         BlockPos min = adjusted.min();
         BlockPos max = adjusted.max();
         this.boundsMin = new BlockPos(min.getX(), this.level == null ? min.getY() : this.level.getMinBuildHeight(), min.getZ());
         this.boundsMax = new BlockPos(max.getX(), this.worldPosition.getY() - 1, max.getZ());
         this.resetCurrent();
         this.saveAndSyncWorksiteState();
      }
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
         && pos.getY() <= max.getY();
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
      Direction facing = state.hasProperty(AutomationWorksiteBlock.FACING)
         ? ((Direction)state.getValue(AutomationWorksiteBlock.FACING)).getOpposite()
         : Direction.SOUTH;
      BlockPos near = pos.relative(facing);
      BlockPos far = pos.relative(facing, 5);
      int minY = this.level == null ? pos.getY() - 1 : this.level.getMinBuildHeight();
      int maxY = pos.getY() - 1;
      if (facing.getAxis() == Axis.Z) {
         this.boundsMin = new BlockPos(pos.getX() - 2, minY, Math.min(near.getZ(), far.getZ()));
         this.boundsMax = new BlockPos(pos.getX() + 2, maxY, Math.max(near.getZ(), far.getZ()));
      } else {
         this.boundsMin = new BlockPos(Math.min(near.getX(), far.getX()), minY, pos.getZ() - 2);
         this.boundsMax = new BlockPos(Math.max(near.getX(), far.getX()), maxY, pos.getZ() + 2);
      }
   }

   @Override
   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.items);
      tag.putLong("BoundsMin", this.getBoundsMin().asLong());
      tag.putLong("BoundsMax", this.getBoundsMax().asLong());
      tag.putLong("Current", this.normalizeCurrent().asLong());
      tag.putBoolean("Finished", this.finished);
      tag.putBoolean("OnlyMineOres", this.onlyMineOres);
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

      if (tag.contains("Current")) {
         this.current = BlockPos.of(tag.getLong("Current"));
      }

      this.finished = tag.getBoolean("Finished");
      this.onlyMineOres = tag.getBoolean("OnlyMineOres");
      this.normalizeLoadedBounds();
   }

   private void normalizeLoadedBounds() {
      if (this.level == null || !this.level.isClientSide) {
         if (this.boundsMin != null && this.boundsMax != null && !WorksiteBoundsGeometry.hasValidSize(this.boundsMin, this.boundsMax, this.getMaxBoundsWidth())
            )
          {
            this.resetDefaultBounds(this.worldPosition, this.getBlockState());
            this.resetCurrent();
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
      tag.putBoolean("OnlyMineOres", this.onlyMineOres);
      return tag;
   }

   public int getContainerSize() {
      return 50;
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
         if (slot >= 45 && slot < 50) {
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
      return new QuarryMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new QuarryBoundsMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createControlMenu(int containerId, Inventory playerInventory) {
      return new QuarryControlMenu(containerId, playerInventory, this.worldPosition);
   }

   @Override
   protected BasicWorksiteBlockEntity.UpgradeSlots getUpgradeSlots() {
      return new BasicWorksiteBlockEntity.UpgradeSlots(45, 5);
   }

   @Override
   protected boolean isOutputSlot(int slot) {
      return slot >= 0 && slot < 45;
   }

   @Override
   protected boolean isInputSlot(int slot) {
      return false;
   }

   @Override
   protected boolean isQuarryWorksite() {
      return true;
   }

   private boolean isValidSlot(int slot) {
      return slot >= 0 && slot < 50;
   }
}
