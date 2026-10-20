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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(32, ItemStack.f_41583_);
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
      return this.f_58857_ != null && !this.f_58857_.f_46443_ && this.countWater() > 0 && (this.harvestFish || this.harvestInk) && this.hasOutputSpace();
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

      for (int workCost = this.getEffectiveWorkCost(); this.storedEnergy >= workCost && processed < 1 && this.processNextAction(); processed++) {
         this.storedEnergy -= workCost;
      }
   }

   private int getEffectiveWorkCost() {
      return WorksiteUpgradeHelper.adjustedWorkCost(100, this, 27, 5);
   }

   private boolean processNextAction() {
      int water = this.countWater();
      if (water > 0 && this.f_58857_ instanceof ServerLevel serverLevel) {
         boolean var6 = this.harvestFish;
         boolean ink = this.harvestInk;
         if (var6 && ink) {
            var6 = this.f_58857_.f_46441_.m_188499_();
            ink = !var6;
         }

         if (var6) {
            return this.produceFishingLoot(serverLevel, this.findWaterPos().orElse(this.f_58858_));
         } else if (ink) {
            ItemStack inkStack = new ItemStack(Items.f_42532_);
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
         .m_287286_(LootContextParams.f_81460_, Vec3.m_82512_(origin))
         .m_287286_(LootContextParams.f_81463_, new ItemStack(Items.f_42523_))
         .m_287235_(LootContextParamSets.f_81414_);
      int roll = this.f_58857_.f_46441_.m_188503_(100);
      ResourceLocation tableId = roll < 10 ? FISHING_JUNK : (roll < 15 ? FISHING_TREASURE : FISHING_FISH);
      LootTable table = serverLevel.m_7654_().m_278653_().m_278676_(tableId);
      List<ItemStack> drops = new ArrayList<>(table.m_287195_(params));
      return drops.isEmpty() ? false : this.insertAll(drops);
   }

   private int countWater() {
      int count = 0;
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();

      for (int x = min.m_123341_(); x <= max.m_123341_(); x++) {
         for (int z = min.m_123343_(); z <= max.m_123343_(); z++) {
            for (int y = max.m_123342_(); y >= min.m_123342_() && this.f_58857_.m_6425_(new BlockPos(x, y, z)).m_205070_(FluidTags.f_13131_); y--) {
               count++;
            }
         }
      }

      return count;
   }

   private Optional<BlockPos> findWaterPos() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();

      for (int x = min.m_123341_(); x <= max.m_123341_(); x++) {
         for (int z = min.m_123343_(); z <= max.m_123343_(); z++) {
            for (int y = max.m_123342_(); y >= min.m_123342_(); y--) {
               BlockPos pos = new BlockPos(x, y, z);
               if (this.f_58857_.m_6425_(pos).m_205070_(FluidTags.f_13131_)) {
                  return Optional.of(pos);
               }
            }
         }
      }

      return Optional.empty();
   }

   private boolean insertAll(List<ItemStack> drops) {
      NonNullList<ItemStack> simulated = NonNullList.m_122780_(27, ItemStack.f_41583_);

      for (int slot = 0; slot < 27; slot++) {
         simulated.set(slot, ((ItemStack)this.items.get(0 + slot)).m_41777_());
      }

      for (ItemStack drop : drops) {
         if (!this.insertIntoRange(simulated, drop.m_41777_(), 0, 27).m_41619_()) {
            return false;
         }
      }

      for (int slot = 0; slot < 27; slot++) {
         this.items.set(0 + slot, (ItemStack)simulated.get(slot));
      }

      this.m_6596_();
      return true;
   }

   private ItemStack insertIntoRange(NonNullList<ItemStack> target, ItemStack stack, int start, int count) {
      for (int slot = start; slot < start + count && !stack.m_41619_(); slot++) {
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

   private boolean hasOutputSpace() {
      for (int slot = 0; slot < 27; slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (stack.m_41619_() || stack.m_41613_() < Math.min(this.m_6893_(), stack.m_41741_())) {
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
      this.m_6596_();
   }

   public void toggleHarvestInk() {
      this.harvestInk = !this.harvestInk;
      this.m_6596_();
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
      return WorksiteUpgradeHelper.getBoundsMaxWidth(this, 27, 5);
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
      int minY = pos.m_123342_() - 5;
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
      tag.m_128379_("HarvestFish", this.harvestFish);
      tag.m_128379_("HarvestInk", this.harvestInk);
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

      this.harvestFish = !tag.m_128441_("HarvestFish") || tag.m_128471_("HarvestFish");
      this.harvestInk = !tag.m_128441_("HarvestInk") || tag.m_128471_("HarvestInk");
      this.normalizeLoadedBounds();
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
      tag.m_128379_("HarvestFish", this.harvestFish);
      tag.m_128379_("HarvestInk", this.harvestInk);
      return tag;
   }

   public int m_6643_() {
      return 32;
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
         if (slot >= 27 && slot < 32) {
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
      return new FishFarmMenu(containerId, playerInventory, this.f_58858_);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new FishFarmBoundsMenu(containerId, playerInventory, this.f_58858_);
   }

   public AbstractContainerMenu createControlMenu(int containerId, Inventory playerInventory) {
      return new FishFarmControlMenu(containerId, playerInventory, this.f_58858_);
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
