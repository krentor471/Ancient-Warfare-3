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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(50, ItemStack.f_41583_);
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
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
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
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && worker.canWorkAt(this.getWorkType()) && this.hasWork()) {
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
      return WorksiteUpgradeHelper.adjustedWorkCost(100, this, 45, 5);
   }

   private boolean processNextAction() {
      Optional<BlockPos> target = this.findNextMineTarget();
      if (!target.isEmpty() && this.f_58857_ instanceof ServerLevel serverLevel) {
         BlockPos var6 = target.get();
         BlockState state = this.f_58857_.m_8055_(var6);
         ArrayList drops = new ArrayList(
            Block.m_49874_(state, serverLevel, var6, this.f_58857_.m_7702_(var6), null, WorksiteUpgradeHelper.fortuneTool(Items.f_42390_, this, 45, 5))
         );
         if (!this.canInsertAll(drops)) {
            this.finished = false;
            return false;
         } else {
            this.f_58857_.m_7731_(var6, Blocks.f_50016_.m_49966_(), 3);
            this.insertOrDrop(var6, drops);
            this.current = this.nextPosition(var6);
            this.m_6596_();
            return true;
         }
      } else {
         this.finished = target.isEmpty();
         return false;
      }
   }

   private Optional<BlockPos> findNextMineTarget() {
      if (this.f_58857_ != null && !this.finished) {
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
      BlockState state = this.f_58857_.m_8055_(pos);
      return !state.m_60795_() && !state.m_60713_(Blocks.f_50752_) && state.m_60819_().m_76178_() && (!this.onlyMineOres || AWCorePlatform.isOre(state));
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
      if (pos.m_123341_() < max.m_123341_()) {
         return pos.m_122029_();
      } else if (pos.m_123343_() < max.m_123343_()) {
         return new BlockPos(min.m_123341_(), pos.m_123342_(), pos.m_123343_() + 1);
      } else {
         return pos.m_123342_() > this.f_58857_.m_141937_() ? new BlockPos(min.m_123341_(), pos.m_123342_() - 1, min.m_123343_()) : null;
      }
   }

   private BlockPos normalizeCurrent() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      if (this.current == null
         || this.current.m_123341_() < min.m_123341_()
         || this.current.m_123341_() > max.m_123341_()
         || this.current.m_123343_() < min.m_123343_()
         || this.current.m_123343_() > max.m_123343_()
         || this.current.m_123342_() > max.m_123342_()
         || this.current.m_123342_() < this.f_58857_.m_141937_()) {
         this.resetCurrent();
      }

      return this.current;
   }

   private void resetCurrent() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      this.current = new BlockPos(min.m_123341_(), max.m_123342_(), min.m_123343_());
      this.finished = false;
   }

   private boolean canInsertAll(List<ItemStack> drops) {
      NonNullList<ItemStack> simulated = NonNullList.m_122780_(45, ItemStack.f_41583_);

      for (int slot = 0; slot < 45; slot++) {
         simulated.set(slot, ((ItemStack)this.items.get(0 + slot)).m_41777_());
      }

      for (ItemStack drop : drops) {
         ItemStack remaining = this.insertIntoList(drop.m_41777_(), simulated);
         if (!remaining.m_41619_()) {
            return false;
         }
      }

      return true;
   }

   private ItemStack insertIntoList(ItemStack stack, NonNullList<ItemStack> target) {
      for (int slot = 0; slot < target.size() && !stack.m_41619_(); slot++) {
         ItemStack existing = (ItemStack)target.get(slot);
         if (existing.m_41619_()) {
            target.set(slot, stack.m_41777_());
            stack.m_41764_(0);
         } else if (ItemStack.m_150942_(existing, stack) && existing.m_41613_() < existing.m_41741_()) {
            int moved = Math.min(stack.m_41613_(), existing.m_41741_() - existing.m_41613_());
            existing.m_41769_(moved);
            stack.m_41774_(moved);
         }
      }

      return stack;
   }

   private void insertOrDrop(BlockPos pos, List<ItemStack> drops) {
      for (ItemStack drop : drops) {
         if (!drop.m_41619_()) {
            ItemStack remaining = this.insertStack(drop);
            if (!remaining.m_41619_()) {
               Block.m_49840_(this.f_58857_, pos, remaining);
            }
         }
      }
   }

   private ItemStack insertStack(ItemStack stack) {
      return this.insertIntoRange(stack.m_41777_(), 0, 45);
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
      return WorksiteUpgradeHelper.getQuarryBoundsMaxWidth(this, 45, 5);
   }

   public void adjustBounds(int minXDelta, int maxXDelta, int minZDelta, int maxZDelta) {
      WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjust(
         this.f_58858_, this.getBoundsMin(), this.getBoundsMax(), this.getMaxBoundsWidth(), minXDelta, maxXDelta, minZDelta, maxZDelta
      );
      if (adjusted != null) {
         BlockPos min = adjusted.min();
         BlockPos max = adjusted.max();
         this.boundsMin = new BlockPos(min.m_123341_(), this.f_58857_ == null ? min.m_123342_() : this.f_58857_.m_141937_(), min.m_123343_());
         this.boundsMax = new BlockPos(max.m_123341_(), this.f_58858_.m_123342_() - 1, max.m_123343_());
         this.resetCurrent();
         this.saveAndSyncWorksiteState();
      }
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
         && pos.m_123342_() <= max.m_123342_();
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
      Direction facing = state.m_61138_(AutomationWorksiteBlock.FACING)
         ? ((Direction)state.m_61143_(AutomationWorksiteBlock.FACING)).m_122424_()
         : Direction.SOUTH;
      BlockPos near = pos.m_121945_(facing);
      BlockPos far = pos.m_5484_(facing, 5);
      int minY = this.f_58857_ == null ? pos.m_123342_() - 1 : this.f_58857_.m_141937_();
      int maxY = pos.m_123342_() - 1;
      if (facing.m_122434_() == Axis.Z) {
         this.boundsMin = new BlockPos(pos.m_123341_() - 2, minY, Math.min(near.m_123343_(), far.m_123343_()));
         this.boundsMax = new BlockPos(pos.m_123341_() + 2, maxY, Math.max(near.m_123343_(), far.m_123343_()));
      } else {
         this.boundsMin = new BlockPos(Math.min(near.m_123341_(), far.m_123341_()), minY, pos.m_123343_() - 2);
         this.boundsMax = new BlockPos(Math.max(near.m_123341_(), far.m_123341_()), maxY, pos.m_123343_() + 2);
      }
   }

   @Override
   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
      tag.m_128356_("BoundsMin", this.getBoundsMin().m_121878_());
      tag.m_128356_("BoundsMax", this.getBoundsMax().m_121878_());
      tag.m_128356_("Current", this.normalizeCurrent().m_121878_());
      tag.m_128379_("Finished", this.finished);
      tag.m_128379_("OnlyMineOres", this.onlyMineOres);
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

      if (tag.m_128441_("Current")) {
         this.current = BlockPos.m_122022_(tag.m_128454_("Current"));
      }

      this.finished = tag.m_128471_("Finished");
      this.onlyMineOres = tag.m_128471_("OnlyMineOres");
      this.normalizeLoadedBounds();
   }

   private void normalizeLoadedBounds() {
      if (this.f_58857_ == null || !this.f_58857_.f_46443_) {
         if (this.boundsMin != null && this.boundsMax != null && !WorksiteBoundsGeometry.hasValidSize(this.boundsMin, this.boundsMax, this.getMaxBoundsWidth())
            )
          {
            this.resetDefaultBounds(this.f_58858_, this.m_58900_());
            this.resetCurrent();
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
      tag.m_128379_("OnlyMineOres", this.onlyMineOres);
      return tag;
   }

   public int m_6643_() {
      return 50;
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
         if (slot >= 45 && slot < 50) {
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
      return new QuarryMenu(containerId, playerInventory, this.f_58858_);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new QuarryBoundsMenu(containerId, playerInventory, this.f_58858_);
   }

   public AbstractContainerMenu createControlMenu(int containerId, Inventory playerInventory) {
      return new QuarryControlMenu(containerId, playerInventory, this.f_58858_);
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
