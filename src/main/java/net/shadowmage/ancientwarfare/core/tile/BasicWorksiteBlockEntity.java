package net.shadowmage.ancientwarfare.core.tile;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.AutomationWorksiteBlock;
import net.shadowmage.ancientwarfare.core.common.inventory.ItemHandlerAccess;
import net.shadowmage.ancientwarfare.core.common.inventory.PlatformItemHandlerProvider;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.BoundedArea;
import net.shadowmage.ancientwarfare.core.common.worksite.SideConfigurableWorksite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteInventoryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteRelativeSide;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgradeHelper;

public abstract class BasicWorksiteBlockEntity extends BlockEntity implements WorkSite, SideConfigurableWorksite, TorqueAccess, PlatformItemHandlerProvider {
   private static final String ENERGY_TAG = "StoredEnergy";
   private static final String FRACTIONAL_ENERGY_TAG = "FractionalStoredEnergy";
   private static final String SIDE_ACCESS_TAG = "SideAccess";
   private static final int DEFAULT_MAX_ENERGY = 1600;
   private static final int BASE_WORKER_ENERGY = 1600;
   private static final double MAX_TORQUE_INPUT_PER_TICK = 1000.0;
   private static final double TORQUE_TO_WORK_ENERGY = 1.0;
   private static final int STORED_ENERGY_WORK_INTERVAL = 10;
   private static final int CHUNK_LOAD_REFRESH_INTERVAL = 40;
   protected int storedEnergy;
   private double fractionalStoredEnergy;
   private int storedEnergyWorkCooldown;
   private int chunkLoadRefreshCooldown;
   private boolean chunkLoadingActive;
   private final Set<Long> forcedChunks = new HashSet<>();
   private final EnumMap<WorksiteRelativeSide, WorksiteInventoryAccess> sideAccess = new EnumMap<>(WorksiteRelativeSide.class);

   protected BasicWorksiteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.resetDefaultSideAccess();
   }

   public static void tickStoredEnergy(Level level, BlockPos pos, BlockState state, BasicWorksiteBlockEntity worksite) {
      if (!level.isClientSide && worksite.storedEnergy > 0) {
         if (worksite.storedEnergyWorkCooldown > 0) {
            worksite.storedEnergyWorkCooldown--;
         } else {
            worksite.storedEnergyWorkCooldown = 10;
            worksite.onStoredEnergyTick();
         }
      }
   }

   @Override
   public boolean hasWork() {
      return this.storedEnergy < this.getMaxEnergy();
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.level != null && !this.level.isClientSide && worker.canWorkAt(this.getWorkType()) && this.hasWork()) {
         int amount = Math.max(1, Math.round(1600.0F * worker.getWorkEffectiveness(this.getWorkType())));
         this.storedEnergy = Math.min(this.getMaxEnergy(), this.storedEnergy + amount);
         this.setChanged();
      }
   }

   @Override
   public abstract NpcWorkType getWorkType();

   public int getStoredEnergy() {
      return this.storedEnergy;
   }

   public int getMaxEnergy() {
      return 1600;
   }

   @Override
   public double getMaxTorque(@Nullable Direction side) {
      return (double)this.getMaxEnergy();
   }

   @Override
   public double getTorqueStored(@Nullable Direction side) {
      return (double)this.storedEnergy + this.fractionalStoredEnergy;
   }

   @Override
   public double addTorque(@Nullable Direction side, double amount) {
      if (this.level != null && !this.level.isClientSide && this.canInputTorque(side)) {
         double accepted = Math.min(amount, this.getMaxTorqueInput(side));
         if (accepted <= 0.0) {
            return 0.0;
         } else {
            this.addStoredEnergy(accepted * 1.0);
            this.onWorkEnergyAdded();
            this.setChanged();
            return accepted;
         }
      } else {
         return 0.0;
      }
   }

   @Override
   public double drainTorque(@Nullable Direction side, double amount) {
      return 0.0;
   }

   @Override
   public double getMaxTorqueOutput(Direction side) {
      return 0.0;
   }

   @Override
   public double getMaxTorqueInput(@Nullable Direction side) {
      return !this.canInputTorque(side) ? 0.0 : Math.min(1000.0, Math.max(0.0, (double)this.getMaxEnergy() - this.getTorqueStored(side)));
   }

   @Override
   public boolean canOutputTorque(Direction side) {
      return false;
   }

   @Override
   public boolean canInputTorque(@Nullable Direction side) {
      return side != null && this.getTorqueStored(side) < (double)this.getMaxEnergy() && this.hasWork();
   }

   protected void onWorkEnergyAdded() {
   }

   protected void onStoredEnergyTick() {
   }

   private void addStoredEnergy(double amount) {
      double total = Math.min((double)this.getMaxEnergy(), (double)this.storedEnergy + this.fractionalStoredEnergy + amount);
      this.storedEnergy = (int)Math.floor(total);
      this.fractionalStoredEnergy = total - (double)this.storedEnergy;
   }

   private void tickChunkLoading() {
      if (this.level instanceof ServerLevel serverLevel) {
         if (this.chunkLoadRefreshCooldown > 0) {
            this.chunkLoadRefreshCooldown--;
         } else {
            this.chunkLoadRefreshCooldown = 40;
            this.refreshChunkLoading(serverLevel);
         }
      }
   }

   protected void refreshChunkLoading() {
      if (this.level instanceof ServerLevel serverLevel) {
         this.refreshChunkLoading(serverLevel);
      }
   }

   public void releaseChunkLoading() {
      if (this.level instanceof ServerLevel serverLevel) {
         this.setForcedChunks(serverLevel, Set.of(), false);
      }
   }

   private void refreshChunkLoading(ServerLevel serverLevel) {
      boolean shouldLoad = this.hasChunkLoaderUpgrade();
      Set<Long> desired = shouldLoad ? this.desiredForcedChunks() : Set.of();
      this.setForcedChunks(serverLevel, desired, shouldLoad);
   }

   private void setForcedChunks(ServerLevel serverLevel, Set<Long> desired, boolean ticking) {
      for (long chunk : Set.copyOf(this.forcedChunks)) {
         if (!desired.contains(chunk) || this.chunkLoadingActive != ticking) {
            ChunkPos chunkPos = new ChunkPos(chunk);
            AWCorePlatform.setChunkForced(serverLevel, this.worldPosition, chunkPos, false, this.chunkLoadingActive);
            this.forcedChunks.remove(chunk);
         }
      }

      for (long chunkx : desired) {
         if (!this.forcedChunks.contains(chunkx) || !this.chunkLoadingActive) {
            ChunkPos chunkPos = new ChunkPos(chunkx);
            if (AWCorePlatform.setChunkForced(serverLevel, this.worldPosition, chunkPos, true, ticking)) {
               this.forcedChunks.add(chunkx);
            }
         }
      }

      this.chunkLoadingActive = ticking && !this.forcedChunks.isEmpty();
   }

   private Set<Long> desiredForcedChunks() {
      Set<Long> chunks = new HashSet<>();
      chunks.add(new ChunkPos(this.worldPosition).toLong());
      if (this instanceof BoundedArea boundedArea) {
         BlockPos min = boundedArea.getBoundsMin();
         BlockPos max = boundedArea.getBoundsMax();
         int minChunkX = Math.floorDiv(min.getX(), 16);
         int maxChunkX = Math.floorDiv(max.getX(), 16);
         int minChunkZ = Math.floorDiv(min.getZ(), 16);
         int maxChunkZ = Math.floorDiv(max.getZ(), 16);

         for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
               chunks.add(ChunkPos.asLong(chunkX, chunkZ));
            }
         }
      }

      return chunks;
   }

   private boolean hasChunkLoaderUpgrade() {
      if (this instanceof Container container) {
         BasicWorksiteBlockEntity.UpgradeSlots slots = this.getUpgradeSlots();
         return slots == null ? false : WorksiteUpgradeHelper.hasChunkLoader(container, slots.start(), slots.count(), this.isQuarryWorksite());
      } else {
         return false;
      }
   }

   protected boolean isQuarryWorksite() {
      return false;
   }

   @Nullable
   protected BasicWorksiteBlockEntity.UpgradeSlots getUpgradeSlots() {
      return null;
   }

   protected boolean isOutputSlot(int slot) {
      BasicWorksiteBlockEntity.UpgradeSlots upgrades = this.getUpgradeSlots();
      return upgrades == null || slot < upgrades.start() || slot >= upgrades.start() + upgrades.count();
   }

   protected boolean isInputSlot(int slot) {
      return this.isOutputSlot(slot);
   }

   protected boolean isSpecialSlot(int slot) {
      return false;
   }

   protected boolean isUpgradeSlot(int slot) {
      BasicWorksiteBlockEntity.UpgradeSlots upgrades = this.getUpgradeSlots();
      return upgrades != null && slot >= upgrades.start() && slot < upgrades.start() + upgrades.count();
   }

   @Nullable
   @Override
   public ItemHandlerAccess getItemHandler(@Nullable Direction side) {
      return side != null && this instanceof Container ? new BasicWorksiteBlockEntity.WorksiteItemHandler(side) : null;
   }

   public WorksiteRelativeSide getRelativeSide(Direction side) {
      if (side == Direction.UP) {
         return WorksiteRelativeSide.TOP;
      } else if (side == Direction.DOWN) {
         return WorksiteRelativeSide.BOTTOM;
      } else {
         Direction front = this.getBlockState().hasProperty(AutomationWorksiteBlock.FACING)
            ? (Direction)this.getBlockState().getValue(AutomationWorksiteBlock.FACING)
            : Direction.NORTH;
         if (side == front) {
            return WorksiteRelativeSide.FRONT;
         } else if (side == front.getOpposite()) {
            return WorksiteRelativeSide.BACK;
         } else {
            return side == front.getCounterClockWise() ? WorksiteRelativeSide.RIGHT : WorksiteRelativeSide.LEFT;
         }
      }
   }

   private boolean slotMatchesAccess(int slot, WorksiteInventoryAccess access) {
      return switch (access) {
         case OUTPUT -> this.isOutputSlot(slot);
         case INPUT -> this.isInputSlot(slot);
         case SPECIAL -> this.isSpecialSlot(slot);
         case UPGRADE -> this.isUpgradeSlot(slot);
         case NONE -> false;
      };
   }

   public abstract String getDescriptionId();

   @Override
   public WorksiteInventoryAccess getSideAccess(WorksiteRelativeSide side) {
      return this.sideAccess.getOrDefault(side, WorksiteInventoryAccess.NONE);
   }

   @Override
   public void setSideAccess(WorksiteRelativeSide side, WorksiteInventoryAccess access) {
      this.sideAccess.put(side, access == null ? WorksiteInventoryAccess.NONE : access);
      this.setChanged();
   }

   @Override
   public Map<WorksiteRelativeSide, WorksiteInventoryAccess> getSideAccessMap() {
      return Map.copyOf(this.sideAccess);
   }

   protected void resetDefaultSideAccess() {
      for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
         this.sideAccess.put(side, WorksiteInventoryAccess.NONE);
      }

      this.sideAccess.put(WorksiteRelativeSide.TOP, WorksiteInventoryAccess.OUTPUT);
      this.sideAccess.put(WorksiteRelativeSide.FRONT, WorksiteInventoryAccess.INPUT);
      this.sideAccess.put(WorksiteRelativeSide.BOTTOM, WorksiteInventoryAccess.SPECIAL);
   }

   protected final void saveAndSyncWorksiteState() {
      this.setChanged();
      if (this.level != null) {
         if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkAt(this.worldPosition).setUnsaved(true);
         }

         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("StoredEnergy", this.storedEnergy);
      tag.putDouble("FractionalStoredEnergy", this.fractionalStoredEnergy);
      CompoundTag sideTag = new CompoundTag();

      for (Entry<WorksiteRelativeSide, WorksiteInventoryAccess> entry : this.sideAccess.entrySet()) {
         sideTag.putString(entry.getKey().name(), entry.getValue().name());
      }

      tag.put("SideAccess", sideTag);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.storedEnergy = Math.max(0, Math.min(this.getMaxEnergy(), tag.getInt("StoredEnergy")));
      this.fractionalStoredEnergy = Math.max(0.0, Math.min(1.0, tag.getDouble("FractionalStoredEnergy")));
      if (this.storedEnergy >= this.getMaxEnergy()) {
         this.fractionalStoredEnergy = 0.0;
      }

      this.resetDefaultSideAccess();
      if (tag.contains("SideAccess")) {
         CompoundTag sideTag = tag.getCompound("SideAccess");

         for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
            if (sideTag.contains(side.name())) {
               try {
                  this.sideAccess.put(side, WorksiteInventoryAccess.valueOf(sideTag.getString(side.name())));
               } catch (IllegalArgumentException var8) {
                  this.sideAccess.put(side, WorksiteInventoryAccess.NONE);
               }
            }
         }
      }
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.load(tag);
   }

   protected static record UpgradeSlots(int start, int count) {
   }

   private final class WorksiteItemHandler implements ItemHandlerAccess {
      private final Direction side;

      private WorksiteItemHandler(Direction side) {
         this.side = side;
      }

      private Container container() {
         return (Container)BasicWorksiteBlockEntity.this;
      }

      private WorksiteInventoryAccess access() {
         return BasicWorksiteBlockEntity.this.getSideAccess(BasicWorksiteBlockEntity.this.getRelativeSide(this.side));
      }

      @Override
      public int getSlots() {
         return this.container().getContainerSize();
      }

      @Override
      public ItemStack getStackInSlot(int slot) {
         return this.valid(slot) && BasicWorksiteBlockEntity.this.slotMatchesAccess(slot, this.access()) ? this.container().getItem(slot) : ItemStack.EMPTY;
      }

      @Override
      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         WorksiteInventoryAccess access = this.access();
         if (this.valid(slot)
            && !stack.isEmpty()
            && access != WorksiteInventoryAccess.OUTPUT
            && BasicWorksiteBlockEntity.this.slotMatchesAccess(slot, access)
            && this.container().canPlaceItem(slot, stack)) {
            ItemStack existing = this.container().getItem(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, stack)) {
               return stack;
            } else {
               int limit = Math.min(this.container().getMaxStackSize(), stack.getMaxStackSize());
               int moved = Math.min(existing.isEmpty() ? limit : limit - existing.getCount(), stack.getCount());
               if (moved <= 0) {
                  return stack;
               } else {
                  if (!simulate) {
                     if (existing.isEmpty()) {
                        this.container().setItem(slot, stack.copyWithCount(moved));
                     } else {
                        existing.grow(moved);
                        this.container().setItem(slot, existing);
                     }

                     this.container().setChanged();
                  }

                  ItemStack remaining = stack.copy();
                  remaining.shrink(moved);
                  return remaining;
               }
            }
         } else {
            return stack;
         }
      }

      @Override
      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         WorksiteInventoryAccess access = this.access();
         if (this.valid(slot) && amount > 0 && access != WorksiteInventoryAccess.INPUT && BasicWorksiteBlockEntity.this.slotMatchesAccess(slot, access)) {
            ItemStack existing = this.container().getItem(slot);
            if (existing.isEmpty()) {
               return ItemStack.EMPTY;
            } else {
               int moved = Math.min(amount, existing.getCount());
               ItemStack result = existing.copyWithCount(moved);
               if (!simulate) {
                  this.container().removeItem(slot, moved);
                  this.container().setChanged();
               }

               return result;
            }
         } else {
            return ItemStack.EMPTY;
         }
      }

      @Override
      public int getSlotLimit(int slot) {
         return this.valid(slot) ? this.container().getMaxStackSize() : 0;
      }

      @Override
      public boolean isItemValid(int slot, ItemStack stack) {
         WorksiteInventoryAccess access = this.access();
         return this.valid(slot)
            && access != WorksiteInventoryAccess.OUTPUT
            && BasicWorksiteBlockEntity.this.slotMatchesAccess(slot, access)
            && this.container().canPlaceItem(slot, stack);
      }

      private boolean valid(int slot) {
         return slot >= 0 && slot < this.container().getContainerSize();
      }
   }
}
