package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.inventory.ItemHandlerAccess;
import net.shadowmage.ancientwarfare.core.common.inventory.PlatformItemHandlerProvider;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseInterfaceFilter;
import net.shadowmage.ancientwarfare.core.menu.WarehouseInterfaceMenu;
import org.jetbrains.annotations.Nullable;

public class WarehouseInterfaceBlockEntity extends BlockEntity implements Container, MenuProvider, PlatformItemHandlerProvider {
   public static final int SLOT_COUNT = 9;
   public static final int FILTER_COUNT = 9;
   private static final String FILTERS_TAG = "Filters";
   private static final int TRANSFER_BUDGET = 128;
   private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
   private final WarehouseInterfaceFilter[] filters = new WarehouseInterfaceFilter[9];
   private final WarehouseMembership membership = new WarehouseMembership();
   private final List<ItemStack> pendingReturns = new ArrayList<>();
   private int filterCount;
   private final ItemHandlerAccess itemHandler = new WarehouseInterfaceBlockEntity.ExternalHandler();
   private int tickCounter;
   private WarehouseInterfaceBlockEntity.Mode mode = WarehouseInterfaceBlockEntity.Mode.STOCK;
   private long localRevision;
   private long processedLocalRevision = -1L;
   private long lastWarehouseRevision = -1L;
   private WarehouseControlBlockEntity lastController;
   private long nextRetryTick;
   private int workStatus;
   private long configurationRevision;

   public WarehouseInterfaceBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_INTERFACE), pos, state);

      for (int i = 0; i < this.filters.length; i++) {
         this.filters[i] = new WarehouseInterfaceFilter();
      }
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, WarehouseInterfaceBlockEntity blockEntity) {
      if (++blockEntity.tickCounter >= 10) {
         blockEntity.tickCounter = 0;
         blockEntity.processWarehouseExchange();
      }
   }

   public WarehouseInterfaceFilter getFilter(int index) {
      return index >= 0 && index < this.filters.length ? this.filters[index] : new WarehouseInterfaceFilter();
   }

   public int getFilterCount() {
      return this.filterCount;
   }

   public WarehouseInterfaceBlockEntity.Mode getMode() {
      return this.mode;
   }

   public void cycleMode() {
      this.mode = this.mode.next();
      this.syncChanged();
   }

   public boolean cycleController() {
      boolean changed = this.membership.cycleController(this);
      if (changed) {
         this.syncChanged();
      }

      return changed;
   }

   public BlockPos getControllerPos() {
      return this.membership.getControllerPos();
   }

   public int getConnectionStatus() {
      return this.membership.getConnectionStatus(this);
   }

   public int getWorkStatus() {
      return this.workStatus;
   }

   public long getConfigurationRevision() {
      return this.configurationRevision;
   }

   public List<ItemStack> takePendingReturns() {
      List<ItemStack> result = new ArrayList<>(this.pendingReturns);
      this.pendingReturns.clear();
      return result;
   }

   public void setFilter(int index, ItemStack stack, int quantity) {
      if (index >= 0 && index < this.filterCount) {
         this.filters[index].set(stack, quantity);
         this.syncChanged();
      }
   }

   public void addFilter() {
      if (this.filterCount < 9) {
         this.filters[this.filterCount].set(ItemStack.EMPTY, 64);
         this.filterCount++;
         this.syncChanged();
      }
   }

   public void removeFilter(int index) {
      if (index >= 0 && index < this.filterCount) {
         for (int i = index; i < this.filterCount - 1; i++) {
            this.filters[i].set(this.filters[i + 1].item(), this.filters[i + 1].quantity());
         }

         this.filterCount--;
         this.filters[this.filterCount].set(ItemStack.EMPTY, 0);
         this.syncChanged();
      }
   }

   public void setFilterItem(int index, ItemStack stack) {
      if (index >= 0 && index < this.filters.length) {
         int quantity = this.filters[index].quantity();
         this.filters[index].set(stack, stack.isEmpty() ? 0 : Math.max(1, quantity == 0 ? 64 : quantity));
         this.syncChanged();
      }
   }

   public void adjustFilterQuantity(int index, int delta) {
      if (index >= 0 && index < this.filters.length) {
         this.filters[index].adjustQuantity(delta);
         this.syncChanged();
      }
   }

   private void processWarehouseExchange() {
      if (this.level != null && !this.level.isClientSide) {
         WarehouseControlBlockEntity controller = this.membership.findController(this);
         if (controller == null) {
            this.workStatus = 0;
         } else {
            long revision = controller.getInventoryRevision();
            long now = this.level.getGameTime();
            if (controller != this.lastController
               || revision != this.lastWarehouseRevision
               || this.localRevision != this.processedLocalRevision
               || now >= this.nextRetryTick) {
               int budget = 128;
               boolean changed = false;

               for (int i = this.pendingReturns.size() - 1; i >= 0 && budget > 0; i--) {
                  ItemStack pending = this.pendingReturns.get(i);
                  int attempt = Math.min(budget, pending.getCount());
                  ItemStack remainder = controller.insertIntoStorage(pending.copyWithCount(attempt));
                  int moved = attempt - remainder.getCount();
                  pending.shrink(moved);
                  budget -= moved;
                  changed |= moved > 0;
                  if (pending.isEmpty()) {
                     this.pendingReturns.remove(i);
                  }
               }

               List<WarehouseInterfaceBlockEntity.Request> requests = this.collectRequests();
               if (this.mode != WarehouseInterfaceBlockEntity.Mode.OUTPUT) {
                  int remaining = this.emptyUnwantedOrExcess(controller, requests, budget);
                  changed |= remaining != budget;
                  budget = remaining;
               }

               if (this.mode != WarehouseInterfaceBlockEntity.Mode.INPUT && this.pendingReturns.isEmpty()) {
                  int remaining = this.fillRequested(controller, requests, budget);
                  changed |= remaining != budget;
               }

               this.workStatus = this.hasCapacityWarning(requests) ? 3 : (changed ? 1 : (this.isSatisfied(requests) ? 0 : 2));
               if (changed) {
                  this.setChanged();
               }

               this.lastController = controller;
               this.lastWarehouseRevision = controller.getInventoryRevision();
               this.processedLocalRevision = this.localRevision;
               this.nextRetryTick = now + (long)(changed ? 10 : 100);
            }
         }
      }
   }

   private int emptyUnwantedOrExcess(WarehouseControlBlockEntity controller, List<WarehouseInterfaceBlockEntity.Request> requests, int budget) {
      for (int slot = 0; slot < this.items.size() && budget > 0; slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (!stack.isEmpty()) {
            long allowed = this.mode == WarehouseInterfaceBlockEntity.Mode.INPUT ? 0L : this.getRequestedQuantity(stack, requests);
            int total = this.countMatching(stack);
            if (allowed <= 0L) {
               budget -= this.moveToWarehouse(controller, slot, Math.min(budget, stack.getCount()));
            } else if ((long)total > allowed) {
               budget -= this.moveToWarehouse(controller, slot, (int)Math.min((long)budget, Math.min((long)stack.getCount(), (long)total - allowed)));
            }
         }
      }

      return budget;
   }

   private int fillRequested(WarehouseControlBlockEntity controller, List<WarehouseInterfaceBlockEntity.Request> requests, int budget) {
      for (WarehouseInterfaceBlockEntity.Request request : requests) {
         long missing = request.quantity - (long)this.countMatching(request.item);

         while (missing > 0L && budget > 0) {
            int capacity = this.insertionCapacity(request.item);
            int amount = (int)Math.min(missing, (long)Math.min(budget, Math.min(capacity, request.item.getMaxStackSize())));
            if (amount <= 0) {
               break;
            }

            ItemStack extracted = controller.extractFromStorage(request.item, amount);
            if (extracted.isEmpty()) {
               break;
            }

            int extractedCount = extracted.getCount();
            ItemStack remaining = this.insertIntoInterface(extracted);
            int moved = extractedCount - remaining.getCount();
            missing -= (long)moved;
            budget -= moved;
            if (!remaining.isEmpty()) {
               ItemStack unreturned = controller.insertIntoStorage(remaining);
               if (!unreturned.isEmpty()) {
                  this.pendingReturns.add(unreturned);
                  this.setChanged();
               }
               break;
            }
         }
      }

      return budget;
   }

   private int moveToWarehouse(WarehouseControlBlockEntity controller, int slot, int amount) {
      ItemStack existing = (ItemStack)this.items.get(slot);
      if (!existing.isEmpty() && amount > 0) {
         ItemStack moving = existing.copyWithCount(Math.min(amount, existing.getCount()));
         ItemStack remaining = controller.insertIntoStorage(moving);
         int moved = moving.getCount() - remaining.getCount();
         if (moved <= 0) {
            return 0;
         } else {
            existing.shrink(moved);
            if (existing.isEmpty()) {
               this.items.set(slot, ItemStack.EMPTY);
            }

            return moved;
         }
      } else {
         return 0;
      }
   }

   private ItemStack insertIntoInterface(ItemStack stack) {
      ItemStack remaining = stack.copy();

      for (int slot = 0; slot < this.items.size() && !remaining.isEmpty(); slot++) {
         ItemStack existing = (ItemStack)this.items.get(slot);
         if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, remaining) && existing.getCount() < existing.getMaxStackSize()) {
            int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
         }
      }

      for (int slotx = 0; slotx < this.items.size() && !remaining.isEmpty(); slotx++) {
         if (((ItemStack)this.items.get(slotx)).isEmpty()) {
            int moved = Math.min(remaining.getCount(), Math.min(this.getMaxStackSize(), remaining.getMaxStackSize()));
            this.items.set(slotx, remaining.copyWithCount(moved));
            remaining.shrink(moved);
         }
      }

      return remaining;
   }

   private int countMatching(ItemStack filter) {
      int count = 0;

      for (ItemStack stack : this.items) {
         if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, filter)) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private List<WarehouseInterfaceBlockEntity.Request> collectRequests() {
      List<WarehouseInterfaceBlockEntity.Request> requests = new ArrayList<>();

      for (int i = 0; i < this.filterCount; i++) {
         WarehouseInterfaceFilter filter = this.filters[i];
         if (!filter.item().isEmpty() && filter.quantity() > 0) {
            WarehouseInterfaceBlockEntity.Request existing = null;

            for (WarehouseInterfaceBlockEntity.Request request : requests) {
               if (ItemStack.isSameItemSameTags(request.item, filter.item())) {
                  existing = request;
                  break;
               }
            }

            if (existing == null) {
               requests.add(new WarehouseInterfaceBlockEntity.Request(filter.item(), (long)filter.quantity()));
            } else {
               existing.quantity = existing.quantity + (long)filter.quantity();
            }
         }
      }

      return requests;
   }

   private long getRequestedQuantity(ItemStack stack, List<WarehouseInterfaceBlockEntity.Request> requests) {
      for (WarehouseInterfaceBlockEntity.Request request : requests) {
         if (ItemStack.isSameItemSameTags(request.item, stack)) {
            return request.quantity;
         }
      }

      return 0L;
   }

   private int insertionCapacity(ItemStack stack) {
      int capacity = 0;
      int limit = Math.min(this.getMaxStackSize(), stack.getMaxStackSize());

      for (ItemStack existing : this.items) {
         if (existing.isEmpty()) {
            capacity += limit;
         } else if (ItemStack.isSameItemSameTags(existing, stack)) {
            capacity += Math.max(0, limit - existing.getCount());
         }
      }

      return capacity;
   }

   private boolean hasCapacityWarning(List<WarehouseInterfaceBlockEntity.Request> requests) {
      if (this.mode == WarehouseInterfaceBlockEntity.Mode.INPUT) {
         return false;
      } else {
         long requiredSlots = 0L;

         for (WarehouseInterfaceBlockEntity.Request request : requests) {
            int limit = Math.min(this.getMaxStackSize(), request.item.getMaxStackSize());
            requiredSlots += (request.quantity + (long)limit - 1L) / (long)limit;
         }

         return requiredSlots > 9L;
      }
   }

   private boolean isSatisfied(List<WarehouseInterfaceBlockEntity.Request> requests) {
      if (!this.pendingReturns.isEmpty()) {
         return false;
      } else {
         if (this.mode != WarehouseInterfaceBlockEntity.Mode.OUTPUT) {
            for (ItemStack stack : this.items) {
               if (!stack.isEmpty()
                  && (this.mode == WarehouseInterfaceBlockEntity.Mode.INPUT || (long)this.countMatching(stack) > this.getRequestedQuantity(stack, requests))) {
                  return false;
               }
            }
         }

         if (this.mode != WarehouseInterfaceBlockEntity.Mode.INPUT) {
            for (WarehouseInterfaceBlockEntity.Request request : requests) {
               if ((long)this.countMatching(request.item) < request.quantity) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public void setChanged() {
      super.setChanged();
      this.localRevision++;
   }

   private void syncChanged() {
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.items);
      this.saveFilters(tag);
      ListTag pending = new ListTag();

      for (ItemStack stack : this.pendingReturns) {
         pending.add(stack.save(new CompoundTag()));
      }

      tag.put("PendingReturns", pending);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      if (tag.contains("Items")) {
         ContainerHelper.loadAllItems(tag, this.items);
      }

      this.loadFilters(tag);
      if (tag.contains("PendingReturns")) {
         this.pendingReturns.clear();

         for (Tag entry : tag.getList("PendingReturns", 10)) {
            ItemStack pending = ItemStack.of((CompoundTag)entry);
            if (!pending.isEmpty()) {
               this.pendingReturns.add(pending);
            }
         }
      }

      this.processedLocalRevision = -1L;
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      this.saveFilters(tag);
      return tag;
   }

   private void saveFilters(CompoundTag tag) {
      this.membership.save(tag);
      tag.putInt("InterfaceMode", this.mode.ordinal());
      tag.putInt("FilterCount", this.filterCount);
      ListTag list = new ListTag();

      for (WarehouseInterfaceFilter filter : this.filters) {
         list.add(filter.save());
      }

      tag.put("Filters", list);
   }

   private void loadFilters(CompoundTag tag) {
      this.configurationRevision++;
      this.membership.load(tag);
      int modeIndex = tag.getInt("InterfaceMode");
      this.mode = modeIndex >= 0 && modeIndex < WarehouseInterfaceBlockEntity.Mode.values().length
         ? WarehouseInterfaceBlockEntity.Mode.values()[modeIndex]
         : WarehouseInterfaceBlockEntity.Mode.STOCK;
      if (tag.contains("Filters")) {
         ListTag list = tag.getList("Filters", 10);

         for (int i = 0; i < this.filters.length && i < list.size(); i++) {
            this.filters[i].load(list.getCompound(i));
         }

         if (tag.contains("FilterCount")) {
            this.filterCount = Math.max(0, Math.min(9, tag.getInt("FilterCount")));
         } else {
            this.filterCount = 0;

            for (int i = 0; i < this.filters.length; i++) {
               if (!this.filters[i].item().isEmpty() || this.filters[i].quantity() > 0) {
                  this.filterCount = i + 1;
               }
            }
         }
      }
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
      return Component.translatable("container.ancientwarfare.warehouse_interface");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new WarehouseInterfaceMenu(containerId, playerInventory, this.worldPosition);
   }

   @Override
   public ItemHandlerAccess getItemHandler(@Nullable Direction side) {
      return this.itemHandler;
   }

   private class ExternalHandler implements ItemHandlerAccess {
      @Override
      public int getSlots() {
         return 9;
      }

      @Override
      public ItemStack getStackInSlot(int slot) {
         return WarehouseInterfaceBlockEntity.this.getItem(slot);
      }

      @Override
      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         if (slot >= 0 && slot < 9 && !stack.isEmpty()) {
            ItemStack existing = (ItemStack)WarehouseInterfaceBlockEntity.this.items.get(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, stack)) {
               return stack;
            } else {
               int limit = Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
               int moved = existing.isEmpty() ? Math.min(stack.getCount(), limit) : Math.min(stack.getCount(), limit - existing.getCount());
               if (moved <= 0) {
                  return stack;
               } else {
                  if (!simulate) {
                     if (existing.isEmpty()) {
                        WarehouseInterfaceBlockEntity.this.items.set(slot, stack.copyWithCount(moved));
                     } else {
                        existing.grow(moved);
                     }

                     WarehouseInterfaceBlockEntity.this.setChanged();
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
         if (slot >= 0 && slot < 9 && amount > 0 && !((ItemStack)WarehouseInterfaceBlockEntity.this.items.get(slot)).isEmpty()) {
            ItemStack existing = (ItemStack)WarehouseInterfaceBlockEntity.this.items.get(slot);
            ItemStack extracted = existing.copyWithCount(Math.min(amount, Math.min(existing.getCount(), existing.getMaxStackSize())));
            if (!simulate) {
               existing.shrink(extracted.getCount());
               if (existing.isEmpty()) {
                  WarehouseInterfaceBlockEntity.this.items.set(slot, ItemStack.EMPTY);
               }

               WarehouseInterfaceBlockEntity.this.setChanged();
            }

            return extracted;
         } else {
            return ItemStack.EMPTY;
         }
      }

      @Override
      public int getSlotLimit(int slot) {
         return WarehouseInterfaceBlockEntity.this.getMaxStackSize();
      }

      @Override
      public boolean isItemValid(int slot, ItemStack stack) {
         return true;
      }
   }

   public static enum Mode {
      STOCK,
      INPUT,
      OUTPUT;

      public WarehouseInterfaceBlockEntity.Mode next() {
         return values()[(this.ordinal() + 1) % values().length];
      }

      public String translationKey() {
         return "guistrings.warehouse.interface_mode." + this.name().toLowerCase(Locale.ROOT);
      }
   }

   private static final class Request {
      private final ItemStack item;
      private long quantity;

      private Request(ItemStack item, long quantity) {
         this.item = item;
         this.quantity = quantity;
      }
   }
}
