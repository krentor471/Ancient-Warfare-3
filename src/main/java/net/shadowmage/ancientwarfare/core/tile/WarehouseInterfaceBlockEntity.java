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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(9, ItemStack.f_41583_);
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
         this.filters[this.filterCount].set(ItemStack.f_41583_, 64);
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
         this.filters[this.filterCount].set(ItemStack.f_41583_, 0);
         this.syncChanged();
      }
   }

   public void setFilterItem(int index, ItemStack stack) {
      if (index >= 0 && index < this.filters.length) {
         int quantity = this.filters[index].quantity();
         this.filters[index].set(stack, stack.m_41619_() ? 0 : Math.max(1, quantity == 0 ? 64 : quantity));
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
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         WarehouseControlBlockEntity controller = this.membership.findController(this);
         if (controller == null) {
            this.workStatus = 0;
         } else {
            long revision = controller.getInventoryRevision();
            long now = this.f_58857_.m_46467_();
            if (controller != this.lastController
               || revision != this.lastWarehouseRevision
               || this.localRevision != this.processedLocalRevision
               || now >= this.nextRetryTick) {
               int budget = 128;
               boolean changed = false;

               for (int i = this.pendingReturns.size() - 1; i >= 0 && budget > 0; i--) {
                  ItemStack pending = this.pendingReturns.get(i);
                  int attempt = Math.min(budget, pending.m_41613_());
                  ItemStack remainder = controller.insertIntoStorage(pending.m_255036_(attempt));
                  int moved = attempt - remainder.m_41613_();
                  pending.m_41774_(moved);
                  budget -= moved;
                  changed |= moved > 0;
                  if (pending.m_41619_()) {
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
                  this.m_6596_();
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
         if (!stack.m_41619_()) {
            long allowed = this.mode == WarehouseInterfaceBlockEntity.Mode.INPUT ? 0L : this.getRequestedQuantity(stack, requests);
            int total = this.countMatching(stack);
            if (allowed <= 0L) {
               budget -= this.moveToWarehouse(controller, slot, Math.min(budget, stack.m_41613_()));
            } else if ((long)total > allowed) {
               budget -= this.moveToWarehouse(controller, slot, (int)Math.min((long)budget, Math.min((long)stack.m_41613_(), (long)total - allowed)));
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
            int amount = (int)Math.min(missing, (long)Math.min(budget, Math.min(capacity, request.item.m_41741_())));
            if (amount <= 0) {
               break;
            }

            ItemStack extracted = controller.extractFromStorage(request.item, amount);
            if (extracted.m_41619_()) {
               break;
            }

            int extractedCount = extracted.m_41613_();
            ItemStack remaining = this.insertIntoInterface(extracted);
            int moved = extractedCount - remaining.m_41613_();
            missing -= (long)moved;
            budget -= moved;
            if (!remaining.m_41619_()) {
               ItemStack unreturned = controller.insertIntoStorage(remaining);
               if (!unreturned.m_41619_()) {
                  this.pendingReturns.add(unreturned);
                  this.m_6596_();
               }
               break;
            }
         }
      }

      return budget;
   }

   private int moveToWarehouse(WarehouseControlBlockEntity controller, int slot, int amount) {
      ItemStack existing = (ItemStack)this.items.get(slot);
      if (!existing.m_41619_() && amount > 0) {
         ItemStack moving = existing.m_255036_(Math.min(amount, existing.m_41613_()));
         ItemStack remaining = controller.insertIntoStorage(moving);
         int moved = moving.m_41613_() - remaining.m_41613_();
         if (moved <= 0) {
            return 0;
         } else {
            existing.m_41774_(moved);
            if (existing.m_41619_()) {
               this.items.set(slot, ItemStack.f_41583_);
            }

            return moved;
         }
      } else {
         return 0;
      }
   }

   private ItemStack insertIntoInterface(ItemStack stack) {
      ItemStack remaining = stack.m_41777_();

      for (int slot = 0; slot < this.items.size() && !remaining.m_41619_(); slot++) {
         ItemStack existing = (ItemStack)this.items.get(slot);
         if (!existing.m_41619_() && ItemStack.m_150942_(existing, remaining) && existing.m_41613_() < existing.m_41741_()) {
            int moved = Math.min(remaining.m_41613_(), existing.m_41741_() - existing.m_41613_());
            existing.m_41769_(moved);
            remaining.m_41774_(moved);
         }
      }

      for (int slotx = 0; slotx < this.items.size() && !remaining.m_41619_(); slotx++) {
         if (((ItemStack)this.items.get(slotx)).m_41619_()) {
            int moved = Math.min(remaining.m_41613_(), Math.min(this.m_6893_(), remaining.m_41741_()));
            this.items.set(slotx, remaining.m_255036_(moved));
            remaining.m_41774_(moved);
         }
      }

      return remaining;
   }

   private int countMatching(ItemStack filter) {
      int count = 0;

      for (ItemStack stack : this.items) {
         if (!stack.m_41619_() && ItemStack.m_150942_(stack, filter)) {
            count += stack.m_41613_();
         }
      }

      return count;
   }

   private List<WarehouseInterfaceBlockEntity.Request> collectRequests() {
      List<WarehouseInterfaceBlockEntity.Request> requests = new ArrayList<>();

      for (int i = 0; i < this.filterCount; i++) {
         WarehouseInterfaceFilter filter = this.filters[i];
         if (!filter.item().m_41619_() && filter.quantity() > 0) {
            WarehouseInterfaceBlockEntity.Request existing = null;

            for (WarehouseInterfaceBlockEntity.Request request : requests) {
               if (ItemStack.m_150942_(request.item, filter.item())) {
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
         if (ItemStack.m_150942_(request.item, stack)) {
            return request.quantity;
         }
      }

      return 0L;
   }

   private int insertionCapacity(ItemStack stack) {
      int capacity = 0;
      int limit = Math.min(this.m_6893_(), stack.m_41741_());

      for (ItemStack existing : this.items) {
         if (existing.m_41619_()) {
            capacity += limit;
         } else if (ItemStack.m_150942_(existing, stack)) {
            capacity += Math.max(0, limit - existing.m_41613_());
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
            int limit = Math.min(this.m_6893_(), request.item.m_41741_());
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
               if (!stack.m_41619_()
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

   public void m_6596_() {
      super.m_6596_();
      this.localRevision++;
   }

   private void syncChanged() {
      this.m_6596_();
      if (this.f_58857_ != null) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
      this.saveFilters(tag);
      ListTag pending = new ListTag();

      for (ItemStack stack : this.pendingReturns) {
         pending.add(stack.m_41739_(new CompoundTag()));
      }

      tag.m_128365_("PendingReturns", pending);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      if (tag.m_128441_("Items")) {
         ContainerHelper.m_18980_(tag, this.items);
      }

      this.loadFilters(tag);
      if (tag.m_128441_("PendingReturns")) {
         this.pendingReturns.clear();

         for (Tag entry : tag.m_128437_("PendingReturns", 10)) {
            ItemStack pending = ItemStack.m_41712_((CompoundTag)entry);
            if (!pending.m_41619_()) {
               this.pendingReturns.add(pending);
            }
         }
      }

      this.processedLocalRevision = -1L;
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = super.m_5995_();
      this.saveFilters(tag);
      return tag;
   }

   private void saveFilters(CompoundTag tag) {
      this.membership.save(tag);
      tag.m_128405_("InterfaceMode", this.mode.ordinal());
      tag.m_128405_("FilterCount", this.filterCount);
      ListTag list = new ListTag();

      for (WarehouseInterfaceFilter filter : this.filters) {
         list.add(filter.save());
      }

      tag.m_128365_("Filters", list);
   }

   private void loadFilters(CompoundTag tag) {
      this.configurationRevision++;
      this.membership.load(tag);
      int modeIndex = tag.m_128451_("InterfaceMode");
      this.mode = modeIndex >= 0 && modeIndex < WarehouseInterfaceBlockEntity.Mode.values().length
         ? WarehouseInterfaceBlockEntity.Mode.values()[modeIndex]
         : WarehouseInterfaceBlockEntity.Mode.STOCK;
      if (tag.m_128441_("Filters")) {
         ListTag list = tag.m_128437_("Filters", 10);

         for (int i = 0; i < this.filters.length && i < list.size(); i++) {
            this.filters[i].load(list.m_128728_(i));
         }

         if (tag.m_128441_("FilterCount")) {
            this.filterCount = Math.max(0, Math.min(9, tag.m_128451_("FilterCount")));
         } else {
            this.filterCount = 0;

            for (int i = 0; i < this.filters.length; i++) {
               if (!this.filters[i].item().m_41619_() || this.filters[i].quantity() > 0) {
                  this.filterCount = i + 1;
               }
            }
         }
      }
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
      return Component.m_237115_("container.ancientwarfare.warehouse_interface");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new WarehouseInterfaceMenu(containerId, playerInventory, this.f_58858_);
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
         return WarehouseInterfaceBlockEntity.this.m_8020_(slot);
      }

      @Override
      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         if (slot >= 0 && slot < 9 && !stack.m_41619_()) {
            ItemStack existing = (ItemStack)WarehouseInterfaceBlockEntity.this.items.get(slot);
            if (!existing.m_41619_() && !ItemStack.m_150942_(existing, stack)) {
               return stack;
            } else {
               int limit = Math.min(this.getSlotLimit(slot), stack.m_41741_());
               int moved = existing.m_41619_() ? Math.min(stack.m_41613_(), limit) : Math.min(stack.m_41613_(), limit - existing.m_41613_());
               if (moved <= 0) {
                  return stack;
               } else {
                  if (!simulate) {
                     if (existing.m_41619_()) {
                        WarehouseInterfaceBlockEntity.this.items.set(slot, stack.m_255036_(moved));
                     } else {
                        existing.m_41769_(moved);
                     }

                     WarehouseInterfaceBlockEntity.this.m_6596_();
                  }

                  ItemStack remaining = stack.m_41777_();
                  remaining.m_41774_(moved);
                  return remaining;
               }
            }
         } else {
            return stack;
         }
      }

      @Override
      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         if (slot >= 0 && slot < 9 && amount > 0 && !((ItemStack)WarehouseInterfaceBlockEntity.this.items.get(slot)).m_41619_()) {
            ItemStack existing = (ItemStack)WarehouseInterfaceBlockEntity.this.items.get(slot);
            ItemStack extracted = existing.m_255036_(Math.min(amount, Math.min(existing.m_41613_(), existing.m_41741_())));
            if (!simulate) {
               existing.m_41774_(extracted.m_41613_());
               if (existing.m_41619_()) {
                  WarehouseInterfaceBlockEntity.this.items.set(slot, ItemStack.f_41583_);
               }

               WarehouseInterfaceBlockEntity.this.m_6596_();
            }

            return extracted;
         } else {
            return ItemStack.f_41583_;
         }
      }

      @Override
      public int getSlotLimit(int slot) {
         return WarehouseInterfaceBlockEntity.this.m_6893_();
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
