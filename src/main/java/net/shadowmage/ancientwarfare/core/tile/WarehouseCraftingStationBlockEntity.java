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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(19, ItemStack.f_41583_);
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
      if (!level.f_46443_ && station.storedEnergy >= 100) {
         int processed;
         for (processed = 0; station.storedEnergy >= 100 && processed < 1 && station.craftToOutput(); processed++) {
            station.storedEnergy -= 100;
         }

         if (processed > 0) {
            station.m_6596_();
            level.m_7260_(pos, state, state, 3);
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
         this.m_6596_();
         if (this.f_58857_ != null) {
            this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 2);
         }
      }
   }

   public boolean canAcceptOutput(ItemStack stack) {
      if (stack.m_41619_()) {
         return false;
      } else {
         ItemStack remaining = stack.m_41777_();

         for (int slot = 10; slot < 19; slot++) {
            ItemStack existing = (ItemStack)this.items.get(slot);
            if (!existing.m_41619_() && ItemStack.m_150942_(existing, remaining)) {
               int moved = Math.min(remaining.m_41613_(), existing.m_41741_() - existing.m_41613_());
               remaining.m_41774_(Math.max(0, moved));
            }
         }

         for (int slotx = 10; slotx < 19 && !remaining.m_41619_(); slotx++) {
            if (((ItemStack)this.items.get(slotx)).m_41619_()) {
               remaining.m_41774_(Math.min(remaining.m_41613_(), remaining.m_41741_()));
            }
         }

         return remaining.m_41619_();
      }
   }

   public boolean insertOutput(ItemStack stack) {
      if (!this.canAcceptOutput(stack)) {
         return false;
      } else {
         ItemStack remaining = stack.m_41777_();

         for (int slot = 10; slot < 19 && !remaining.m_41619_(); slot++) {
            ItemStack existing = (ItemStack)this.items.get(slot);
            if (!existing.m_41619_() && ItemStack.m_150942_(existing, remaining)) {
               int moved = Math.min(remaining.m_41613_(), existing.m_41741_() - existing.m_41613_());
               if (moved > 0) {
                  existing.m_41769_(moved);
                  remaining.m_41774_(moved);
               }
            }
         }

         for (int slotx = 10; slotx < 19 && !remaining.m_41619_(); slotx++) {
            if (((ItemStack)this.items.get(slotx)).m_41619_()) {
               int moved = Math.min(remaining.m_41613_(), remaining.m_41741_());
               this.items.set(slotx, remaining.m_255036_(moved));
               remaining.m_41774_(moved);
            }
         }

         if (remaining.m_41619_()) {
            this.m_6596_();
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean craftToOutput() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         WarehouseControlBlockEntity warehouse = this.getWarehouse();
         long revision = warehouse == null ? Long.MIN_VALUE : warehouse.getInventoryRevision();
         if (this.f_58857_.m_46467_() < this.nextCraftRetry && revision == this.failedWarehouseRevision && this.contentsRevision == this.failedContentsRevision
            )
          {
            return false;
         } else {
            boolean crafted = WarehouseCraftingStationMenu.craftTemplateToOutput(this.f_58857_, this, null);
            if (!crafted) {
               this.failedWarehouseRevision = revision;
               this.failedContentsRevision = this.contentsRevision;
               this.nextCraftRetry = this.f_58857_.m_46467_() + 20L;
            }

            return crafted;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean hasWork() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && this.storedEnergy < this.getMaxEnergy()) {
         WarehouseControlBlockEntity warehouse = this.getWarehouse();
         long revision = warehouse == null ? Long.MIN_VALUE : warehouse.getInventoryRevision();
         if (warehouse != this.checkedWarehouse
            || revision != this.checkedWarehouseRevision
            || this.contentsRevision != this.checkedContentsRevision
            || this.f_58857_.m_46467_() >= this.nextAvailabilityCheck) {
            this.craftingAvailable = WarehouseCraftingStationMenu.canCraftTemplate(this.f_58857_, this, true);
            this.checkedWarehouse = warehouse;
            this.checkedWarehouseRevision = revision;
            this.checkedContentsRevision = this.contentsRevision;
            this.nextAvailabilityCheck = this.f_58857_.m_46467_() + 20L;
         }

         return this.craftingAvailable;
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

         for (int processed = 0; this.storedEnergy >= 100 && processed < 1 && this.craftToOutput(); processed++) {
            this.storedEnergy -= 100;
         }

         this.m_6596_();
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
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
   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
      this.membership.save(tag);
      tag.m_128379_("AutoStoreOutput", this.autoStoreOutput);
      if (this.selectedRecipeId != null) {
         tag.m_128359_("SelectedRecipe", this.selectedRecipeId.toString());
      }
   }

   @Override
   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      ContainerHelper.m_18980_(tag, this.items);
      this.membership.load(tag);
      this.autoStoreOutput = tag.m_128471_("AutoStoreOutput");
      this.selectedRecipeId = tag.m_128441_("SelectedRecipe") ? ResourceLocation.m_135820_(tag.m_128461_("SelectedRecipe")) : null;
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public CompoundTag m_5995_() {
      return this.m_187482_();
   }

   public int m_6643_() {
      return this.items.size();
   }

   public boolean m_7983_() {
      return this.items.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int slot) {
      return slot >= 0 && slot < this.items.size() ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.m_18969_(this.items, slot, amount) : ItemStack.f_41583_;
      if (!removed.m_41619_()) {
         this.m_6596_();
      }

      return removed;
   }

   public ItemStack m_8016_(int slot) {
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.m_18966_(this.items, slot) : ItemStack.f_41583_;
      if (!removed.m_41619_()) {
         this.m_6596_();
      }

      return removed;
   }

   public void m_6836_(int slot, ItemStack stack) {
      if (slot >= 0 && slot < this.items.size()) {
         this.items.set(slot, stack);
         if (!stack.m_41619_() && stack.m_41613_() > Math.min(this.m_6893_(), stack.m_41741_())) {
            stack.m_41764_(Math.min(this.m_6893_(), stack.m_41741_()));
         }

         this.m_6596_();
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
      return Component.m_237115_("container.ancientwarfare.warehouse_crafting");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new WarehouseCraftingStationMenu(containerId, playerInventory, this.f_58858_);
   }

   @Override
   public ResourceLocation getSelectedRecipeId() {
      return this.selectedRecipeId;
   }

   @Override
   public void setSelectedRecipeId(ResourceLocation recipeId) {
      this.selectedRecipeId = recipeId;
      this.m_6596_();
   }

   public void m_6596_() {
      this.contentsRevision++;
      super.m_6596_();
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

   public boolean m_7013_(int slot, ItemStack stack) {
      return slot != 0 || stack.m_41720_() instanceof ResearchBookItem;
   }
}
