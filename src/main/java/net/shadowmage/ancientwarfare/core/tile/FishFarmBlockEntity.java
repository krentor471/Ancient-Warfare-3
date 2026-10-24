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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.block.AutomationWorksiteBlock;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.BoundedWorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteBoundsGeometry;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgradeHelper;
import net.shadowmage.ancientwarfare.core.menu.FishFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.FishFarmControlMenu;
import net.shadowmage.ancientwarfare.core.menu.FishFarmMenu;
import org.jetbrains.annotations.Nullable;

public class FishFarmBlockEntity extends BasicWorksiteBlockEntity implements Container, MenuProvider, BoundedWorkSite {
   public static final int MAIN_SLOT_START = 0;
   public static final int MAIN_SLOT_COUNT = 27;
   public static final int UPGRADE_SLOT_START = 27;
   public static final int UPGRADE_SLOT_COUNT = 5;
   public static final int SLOT_COUNT = 32;
   public static final int DEFAULT_RADIUS = 2;
   public static final int DEFAULT_DEPTH = 5;
   public static final int MAX_BOUNDS_WIDTH = 16;
   public static final int WATER_SCAN_DEPTH = 5;
   private static final String MIN_TAG = "BoundsMin";
   private static final String MAX_TAG = "BoundsMax";
   private static final String HARVEST_FISH_TAG = "HarvestFish";
   private static final String HARVEST_INK_TAG = "HarvestInk";
   private static final ResourceLocation FISHING_FISH = new ResourceLocation("minecraft", "gameplay/fishing/fish");
   private static final ResourceLocation FISHING_JUNK = new ResourceLocation("minecraft", "gameplay/fishing/junk");
   private static final ResourceLocation FISHING_TREASURE = new ResourceLocation("minecraft", "gameplay/fishing/treasure");
   private static final int WORK_COST = 100;
   private static final int WORKER_ENERGY_PER_VISIT = 1600;
   private final NonNullList<ItemStack> items = NonNullList.withSize(32, ItemStack.EMPTY);
   private BlockPos boundsMin;
   private BlockPos boundsMax;
   private boolean harvestFish = true;
   private boolean harvestInk = true;

   public FishFarmBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.FISH_FARM), pos, state);
      this.resetDefaultBounds(pos, state);
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.FARMING;
   }

   @Override
   public String getDescriptionId() {
      return "block.ancientwarfare.fish_farm";
   }

   @Override
   public boolean hasWork() {
      return this.level != null && !this.level.isClientSide && this.countWater() > 0 && (this.harvestFish || this.harvestInk) && this.hasOutputSpace();
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

      for (int workCost = this.getEffectiveWorkCost(); this.storedEnergy >= workCost && processed < 1 && this.processNextAction(); processed++) {
         this.storedEnergy -= workCost;
      }
   }

   private int getEffectiveWorkCost() {
      return WorksiteUpgradeHelper.adjustedWorkCost(100, this, 27, 5);
   }

   private boolean processNextAction() {
      int water = this.countWater();
      if (water > 0 && this.level instanceof ServerLevel serverLevel) {
         boolean var6 = this.harvestFish;
         boolean ink = this.harvestInk;
         if (var6 && ink) {
            var6 = this.level.random.nextBoolean();
            ink = !var6;
         }

         if (var6) {
            return this.produceFishingLoot(serverLevel, this.findWaterPos().orElse(this.worldPosition));
         } else if (ink) {
            ItemStack inkStack = new ItemStack(Items.INK_SAC);
            return this.insertAll(List.of(inkStack));
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean produceFishingLoot(ServerLevel serverLevel, BlockPos origin) {
      LootParams params = new Builder(serverLevel)
         .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
         .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
         .create(LootContextParamSets.FISHING);
      int roll = this.level.random.nextInt(100);
      ResourceLocation tableId = roll < 10 ? FISHING_JUNK : (roll < 15 ? FISHING_TREASURE : FISHING_FISH);
      LootTable table = serverLevel.getServer().getLootData().getLootTable(tableId);
      List<ItemStack> drops = new ArrayList<>(table.getRandomItems(params));
      return drops.isEmpty() ? false : this.insertAll(drops);
   }

   private int countWater() {
      int count = 0;
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();

      for (int x = min.getX(); x <= max.getX(); x++) {
         for (int z = min.getZ(); z <= max.getZ(); z++) {
            for (int y = max.getY(); y >= min.getY() && this.level.getFluidState(new BlockPos(x, y, z)).is(FluidTags.WATER); y--) {
               count++;
            }
         }
      }

      return count;
   }

   private Optional<BlockPos> findWaterPos() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();

      for (int x = min.getX(); x <= max.getX(); x++) {
         for (int z = min.getZ(); z <= max.getZ(); z++) {
            for (int y = max.getY(); y >= min.getY(); y--) {
               BlockPos pos = new BlockPos(x, y, z);
               if (this.level.getFluidState(pos).is(FluidTags.WATER)) {
                  return Optional.of(pos);
               }
            }
         }
      }

      return Optional.empty();
   }

   private boolean insertAll(List<ItemStack> drops) {
      NonNullList<ItemStack> simulated = NonNullList.withSize(27, ItemStack.EMPTY);

      for (int slot = 0; slot < 27; slot++) {
         simulated.set(slot, ((ItemStack)this.items.get(0 + slot)).copy());
      }

      for (ItemStack drop : drops) {
         if (!this.insertIntoRange(simulated, drop.copy(), 0, 27).isEmpty()) {
            return false;
         }
      }

      for (int slot = 0; slot < 27; slot++) {
         this.items.set(0 + slot, (ItemStack)simulated.get(slot));
      }

      this.setChanged();
      return true;
   }

   private ItemStack insertIntoRange(NonNullList<ItemStack> target, ItemStack stack, int start, int count) {
      for (int slot = start; slot < start + count && !stack.isEmpty(); slot++) {
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

   private boolean hasOutputSpace() {
      for (int slot = 0; slot < 27; slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (stack.isEmpty() || stack.getCount() < Math.min(this.getMaxStackSize(), stack.getMaxStackSize())) {
            return true;
         }
      }

      return false;
   }

   public boolean harvestFish() {
      return this.harvestFish;
   }

   public boolean harvestInk() {
      return this.harvestInk;
   }

   public void toggleHarvestFish() {
      this.harvestFish = !this.harvestFish;
      this.setChanged();
   }

   public void toggleHarvestInk() {
      this.harvestInk = !this.harvestInk;
      this.setChanged();
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
      return WorksiteUpgradeHelper.getBoundsMaxWidth(this, 27, 5);
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
      int minY = pos.getY() - 5;
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
      tag.putBoolean("HarvestFish", this.harvestFish);
      tag.putBoolean("HarvestInk", this.harvestInk);
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

      this.harvestFish = !tag.contains("HarvestFish") || tag.getBoolean("HarvestFish");
      this.harvestInk = !tag.contains("HarvestInk") || tag.getBoolean("HarvestInk");
      this.normalizeLoadedBounds();
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
      tag.putBoolean("HarvestFish", this.harvestFish);
      tag.putBoolean("HarvestInk", this.harvestInk);
      return tag;
   }

   public int getContainerSize() {
      return 32;
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
         if (slot >= 27 && slot < 32) {
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
      return new FishFarmMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new FishFarmBoundsMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createControlMenu(int containerId, Inventory playerInventory) {
      return new FishFarmControlMenu(containerId, playerInventory, this.worldPosition);
   }

   @Override
   protected BasicWorksiteBlockEntity.UpgradeSlots getUpgradeSlots() {
      return new BasicWorksiteBlockEntity.UpgradeSlots(27, 5);
   }

   @Override
   protected boolean isOutputSlot(int slot) {
      return slot >= 0 && slot < 27;
   }

   @Override
   protected boolean isInputSlot(int slot) {
      return false;
   }

   private boolean isValidSlot(int slot) {
      return slot >= 0 && slot < 32;
   }
}
