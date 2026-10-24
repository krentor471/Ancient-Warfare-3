package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.engineering.CraftingRecipeMemory;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.item.ResearchBookItem;
import net.shadowmage.ancientwarfare.core.menu.WarehouseCraftingStationMenu;
import org.jetbrains.annotations.Nullable;

public class WarehouseCraftingStationBlockEntity extends BasicWorksiteBlockEntity implements Container, MenuProvider, CraftingRecipeMemory {
   public static final int BOOK_SLOT = 0;
   public static final int CRAFT_SLOT_START = 1;
   public static final int CRAFT_SLOT_COUNT = 9;
   public static final int OUTPUT_SLOT_START = 10;
   public static final int OUTPUT_SLOT_COUNT = 9;
   public static final int SLOT_COUNT = 19;
   private static final int WORK_COST = 100;
   private static final int WORKER_ENERGY_PER_VISIT = 1600;
   private final NonNullList<ItemStack> items = NonNullList.withSize(19, ItemStack.EMPTY);
   private final WarehouseMembership membership = new WarehouseMembership();
   private ResourceLocation selectedRecipeId;
   private boolean autoStoreOutput;
   private long contentsRevision;
   private long failedWarehouseRevision = Long.MIN_VALUE;
   private long failedContentsRevision = Long.MIN_VALUE;
   private long nextCraftRetry;
   private WarehouseControlBlockEntity checkedWarehouse;
   private long checkedWarehouseRevision = Long.MIN_VALUE;
   private long checkedContentsRevision = Long.MIN_VALUE;
   private long nextAvailabilityCheck;
   private boolean craftingAvailable;

   public WarehouseCraftingStationBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_CRAFTING), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, WarehouseCraftingStationBlockEntity station) {
      if (!level.isClientSide && station.storedEnergy >= 100) {
         int processed;
         for (processed = 0; station.storedEnergy >= 100 && processed < 1 && station.craftToOutput(); processed++) {
            station.storedEnergy -= 100;
         }

         if (processed > 0) {
            station.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
         }
      }
   }

   @Nullable
   public WarehouseControlBlockEntity getWarehouse() {
      return this.membership.findController(this);
   }

   @Nullable
   public BlockPos getWarehousePos() {
      return this.membership.getControllerPos();
   }

   public int getWarehouseConnectionStatus() {
      return this.membership.getConnectionStatus(this);
   }

   public boolean cycleWarehouse() {
      return this.membership.cycleController(this);
   }

   public boolean isAutoStoreOutput() {
      return this.autoStoreOutput;
   }

   public void setAutoStoreOutput(boolean autoStoreOutput) {
      if (this.autoStoreOutput != autoStoreOutput) {
         this.autoStoreOutput = autoStoreOutput;
         this.setChanged();
         if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
         }
      }
   }

   public boolean canAcceptOutput(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         ItemStack remaining = stack.copy();

         for (int slot = 10; slot < 19; slot++) {
            ItemStack existing = (ItemStack)this.items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, remaining)) {
               int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
               remaining.shrink(Math.max(0, moved));
            }
         }

         for (int slotx = 10; slotx < 19 && !remaining.isEmpty(); slotx++) {
            if (((ItemStack)this.items.get(slotx)).isEmpty()) {
               remaining.shrink(Math.min(remaining.getCount(), remaining.getMaxStackSize()));
            }
         }

         return remaining.isEmpty();
      }
   }

   public boolean insertOutput(ItemStack stack) {
      if (!this.canAcceptOutput(stack)) {
         return false;
      } else {
         ItemStack remaining = stack.copy();

         for (int slot = 10; slot < 19 && !remaining.isEmpty(); slot++) {
            ItemStack existing = (ItemStack)this.items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, remaining)) {
               int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
               if (moved > 0) {
                  existing.grow(moved);
                  remaining.shrink(moved);
               }
            }
         }

         for (int slotx = 10; slotx < 19 && !remaining.isEmpty(); slotx++) {
            if (((ItemStack)this.items.get(slotx)).isEmpty()) {
               int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
               this.items.set(slotx, remaining.copyWithCount(moved));
               remaining.shrink(moved);
            }
         }

         if (remaining.isEmpty()) {
            this.setChanged();
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean craftToOutput() {
      if (this.level != null && !this.level.isClientSide) {
         WarehouseControlBlockEntity warehouse = this.getWarehouse();
         long revision = warehouse == null ? Long.MIN_VALUE : warehouse.getInventoryRevision();
         if (this.level.getGameTime() < this.nextCraftRetry && revision == this.failedWarehouseRevision && this.contentsRevision == this.failedContentsRevision
            )
          {
            return false;
         } else {
            boolean crafted = WarehouseCraftingStationMenu.craftTemplateToOutput(this.level, this, null);
            if (!crafted) {
               this.failedWarehouseRevision = revision;
               this.failedContentsRevision = this.contentsRevision;
               this.nextCraftRetry = this.level.getGameTime() + 20L;
            }

            return crafted;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean hasWork() {
      if (this.level != null && !this.level.isClientSide && this.storedEnergy < this.getMaxEnergy()) {
         WarehouseControlBlockEntity warehouse = this.getWarehouse();
         long revision = warehouse == null ? Long.MIN_VALUE : warehouse.getInventoryRevision();
         if (warehouse != this.checkedWarehouse
            || revision != this.checkedWarehouseRevision
            || this.contentsRevision != this.checkedContentsRevision
            || this.level.getGameTime() >= this.nextAvailabilityCheck) {
            this.craftingAvailable = WarehouseCraftingStationMenu.canCraftTemplate(this.level, this, true);
            this.checkedWarehouse = warehouse;
            this.checkedWarehouseRevision = revision;
            this.checkedContentsRevision = this.contentsRevision;
            this.nextAvailabilityCheck = this.level.getGameTime() + 20L;
         }

         return this.craftingAvailable;
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

         for (int processed = 0; this.storedEnergy >= 100 && processed < 1 && this.craftToOutput(); processed++) {
            this.storedEnergy -= 100;
         }

         this.setChanged();
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.CRAFTING;
   }

   @Override
   public String getDescriptionId() {
      return "block.ancientwarfare.warehouse_crafting";
   }

   @Override
   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.items);
      this.membership.save(tag);
      tag.putBoolean("AutoStoreOutput", this.autoStoreOutput);
      if (this.selectedRecipeId != null) {
         tag.putString("SelectedRecipe", this.selectedRecipeId.toString());
      }
   }

   @Override
   public void load(CompoundTag tag) {
      super.load(tag);
      ContainerHelper.loadAllItems(tag, this.items);
      this.membership.load(tag);
      this.autoStoreOutput = tag.getBoolean("AutoStoreOutput");
      this.selectedRecipeId = tag.contains("SelectedRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public int getContainerSize() {
      return this.items.size();
   }

   public boolean isEmpty() {
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int slot) {
      return slot >= 0 && slot < this.items.size() ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.removeItem(this.items, slot, amount) : ItemStack.EMPTY;
      if (!removed.isEmpty()) {
         this.setChanged();
      }

      return removed;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
      if (!removed.isEmpty()) {
         this.setChanged();
      }

      return removed;
   }

   public void setItem(int slot, ItemStack stack) {
      if (slot >= 0 && slot < this.items.size()) {
         this.items.set(slot, stack);
         if (!stack.isEmpty() && stack.getCount() > Math.min(this.getMaxStackSize(), stack.getMaxStackSize())) {
            stack.setCount(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
         }

         this.setChanged();
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
      return Component.translatable("container.ancientwarfare.warehouse_crafting");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new WarehouseCraftingStationMenu(containerId, playerInventory, this.worldPosition);
   }

   @Override
   public ResourceLocation getSelectedRecipeId() {
      return this.selectedRecipeId;
   }

   @Override
   public void setSelectedRecipeId(ResourceLocation recipeId) {
      this.selectedRecipeId = recipeId;
      this.setChanged();
   }

   public void setChanged() {
      this.contentsRevision++;
      super.setChanged();
   }

   @Override
   protected boolean isOutputSlot(int slot) {
      return slot >= 10 && slot < 19;
   }

   @Override
   protected boolean isInputSlot(int slot) {
      return slot >= 1 && slot < 10;
   }

   @Override
   protected boolean isSpecialSlot(int slot) {
      return slot == 0;
   }

   public boolean canPlaceItem(int slot, ItemStack stack) {
      return slot != 0 || stack.getItem() instanceof ResearchBookItem;
   }
}
