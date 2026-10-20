package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseEntry;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseItemKey;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStorageMenu;
import org.jetbrains.annotations.Nullable;

public class WarehouseStorageBlockEntity extends BlockEntity implements Container, MenuProvider {
   public static final long SMALL_CAPACITY = 576L;
   private static final String QUANTITIES_TAG = "ItemQuantities";
   private static final String FILTERS_TAG = "StorageFilters";
   private final Map<WarehouseItemKey, Long> quantities = new LinkedHashMap<>();
   private final List<ItemStack> filters = new ArrayList<>();
   private final WarehouseMembership membership = new WarehouseMembership();
   private long storedCount;
   private long contentsRevision;

   public WarehouseStorageBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_STORAGE), pos, state);
   }

   public WarehouseStorageBlockEntity(BlockPos pos, BlockState state, int oldSlotCount) {
      this(pos, state);
   }

   public WarehouseMembership getMembership() {
      return this.membership;
   }

   public void m_142339_(Level level) {
      super.m_142339_(level);
      WarehouseNetwork.register(this);
   }

   public void m_7651_() {
      WarehouseNetwork.changed(this.f_58857_);
      super.m_7651_();
   }

   public void m_6596_() {
      super.m_6596_();
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         WarehouseControlBlockEntity control = WarehouseNetwork.loadedController(this.f_58857_, this.membership.getControllerPos());
         if (control != null) {
            control.storageChanged(this);
         }
      }
   }

   public static int storageSize(BlockState state) {
      if (state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_LARGE))) {
         return 54;
      } else {
         return state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_MEDIUM)) ? 45 : 27;
      }
   }

   public long getCapacity() {
      if (this.m_58900_().m_60713_(AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_LARGE))) {
         return 1728L;
      } else {
         return this.m_58900_().m_60713_(AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_MEDIUM)) ? 1152L : 576L;
      }
   }

   public long getItemCapacity() {
      return this.getCapacity();
   }

   public long getStoredCount() {
      return this.storedCount;
   }

   public long getStoredItemCount() {
      return this.storedCount;
   }

   public long getInventoryRevision() {
      return this.contentsRevision;
   }

   public long getContentsRevision() {
      return this.contentsRevision;
   }

   public long getCountOf(ItemStack stack) {
      return stack.m_41619_() ? 0L : this.quantities.getOrDefault(new WarehouseItemKey(stack), 0L);
   }

   public long getQuantity(ItemStack stack) {
      return this.getCountOf(stack);
   }

   public List<WarehouseEntry> getEntries() {
      return this.copyEntries();
   }

   public List<WarehouseEntry> copyEntries() {
      List<WarehouseEntry> result = new ArrayList<>(this.quantities.size());
      this.quantities.forEach((key, count) -> result.add(new WarehouseEntry(key.stack(), count)));
      return List.copyOf(result);
   }

   public void replaceEntries(List<WarehouseEntry> replacement) {
      Map<WarehouseItemKey, Long> next = new LinkedHashMap<>();
      long total = 0L;

      for (WarehouseEntry entry : replacement) {
         if (!entry.stack().m_41619_() && entry.count() > 0L) {
            total = Math.addExact(total, entry.count());
            next.merge(new WarehouseItemKey(entry.stack()), entry.count(), Math::addExact);
         }
      }

      if (total > Math.max(this.getCapacity(), this.storedCount)) {
         throw new IllegalArgumentException("Warehouse quantity capacity exceeded");
      } else if (!next.equals(this.quantities)) {
         this.quantities.clear();
         this.quantities.putAll(next);
         this.storedCount = total;
         this.syncChanged(false);
      }
   }

   public List<WarehouseEntry> takeAllEntries() {
      List<WarehouseEntry> result = this.copyEntries();
      this.m_6211_();
      return result;
   }

   public ItemStack insertStack(ItemStack stack) {
      return this.insertStack(stack, true);
   }

   public ItemStack insertStack(ItemStack stack, boolean allowNewKinds) {
      if (stack.m_41619_()) {
         return ItemStack.f_41583_;
      } else {
         WarehouseItemKey key = new WarehouseItemKey(stack);
         long old = this.quantities.getOrDefault(key, 0L);
         if (!allowNewKinds && old == 0L) {
            return stack.m_41777_();
         } else {
            long available = Math.max(0L, this.getCapacity() - this.storedCount);
            int moved = (int)Math.min((long)stack.m_41613_(), available);
            if (moved <= 0) {
               return stack.m_41777_();
            } else {
               this.quantities.put(key, old + (long)moved);
               this.storedCount += (long)moved;
               this.syncChanged(false);
               return moved == stack.m_41613_() ? ItemStack.f_41583_ : stack.m_255036_(stack.m_41613_() - moved);
            }
         }
      }
   }

   public ItemStack extractMatching(ItemStack filter, int amount) {
      if (!filter.m_41619_() && amount > 0) {
         WarehouseItemKey key = new WarehouseItemKey(filter);
         long old = this.quantities.getOrDefault(key, 0L);
         int moved = (int)Math.min(old, (long)Math.min(amount, filter.m_41741_()));
         if (moved <= 0) {
            return ItemStack.f_41583_;
         } else {
            if (old == (long)moved) {
               this.quantities.remove(key);
            } else {
               this.quantities.put(key, old - (long)moved);
            }

            this.storedCount -= (long)moved;
            this.syncChanged(false);
            return filter.m_255036_(moved);
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   public List<ItemStack> getFilters() {
      return this.filters.stream().<ItemStack>map(ItemStack::m_41777_).toList();
   }

   public int getFilterCount() {
      return this.filters.size();
   }

   public ItemStack getFilter(int index) {
      return index >= 0 && index < this.filters.size() ? this.filters.get(index).m_41777_() : ItemStack.f_41583_;
   }

   public boolean matchesFilter(ItemStack stack) {
      return !stack.m_41619_() && this.filters.stream().anyMatch(filter -> ItemStack.m_150942_(filter, stack));
   }

   public boolean addFilter(ItemStack stack) {
      if (this.filters.size() >= 10) {
         return false;
      } else {
         this.filters.add(stack.m_41619_() ? ItemStack.f_41583_ : stack.m_255036_(1));
         this.syncChanged(true);
         return true;
      }
   }

   public boolean removeFilter(int index) {
      if (index >= 0 && index < this.filters.size()) {
         this.filters.remove(index);
         this.syncChanged(true);
         return true;
      } else {
         return false;
      }
   }

   public void setFilter(int index, ItemStack stack) {
      if (index >= 0 && index < this.filters.size()) {
         this.filters.set(index, stack.m_41619_() ? ItemStack.f_41583_ : stack.m_255036_(1));
         this.syncChanged(true);
      }
   }

   public void setFilters(List<ItemStack> replacement) {
      this.filters.clear();

      for (ItemStack stack : replacement) {
         if (this.filters.size() >= 10) {
            break;
         }

         this.filters.add(stack.m_41619_() ? ItemStack.f_41583_ : stack.m_255036_(1));
      }

      this.syncChanged(true);
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128405_("WarehouseStorageVersion", 2);
      ListTag entries = new ListTag();
      this.quantities.forEach((key, count) -> {
         CompoundTag entry = new CompoundTag();
         entry.m_128365_("Item", key.stack().m_41739_(new CompoundTag()));
         entry.m_128356_("Quantity", count);
         entries.add(entry);
      });
      tag.m_128365_("ItemQuantities", entries);
      this.saveFilters(tag);
      this.membership.save(tag);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.quantities.clear();
      this.storedCount = 0L;
      if (tag.m_128425_("ItemQuantities", 9)) {
         ListTag entries = tag.m_128437_("ItemQuantities", 10);

         for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.m_128728_(i);
            this.loadQuantity(ItemStack.m_41712_(entry.m_128469_("Item")), entry.m_128454_("Quantity"));
         }
      } else {
         ListTag oldItems = tag.m_128437_("Items", 10);

         for (int i = 0; i < oldItems.size(); i++) {
            CompoundTag entry = oldItems.m_128728_(i);
            ItemStack stack = ItemStack.m_41712_(entry);
            int count = entry.m_128425_("Count", 1) ? Byte.toUnsignedInt(entry.m_128445_("Count")) : stack.m_41613_();
            if (stack.m_41619_() && count > 0) {
               CompoundTag prototype = entry.m_6426_();
               prototype.m_128344_("Count", (byte)1);
               stack = ItemStack.m_41712_(prototype);
            }

            this.loadQuantity(stack, (long)count);
         }
      }

      this.loadFilters(tag);
      this.membership.load(tag);
      this.contentsRevision++;
      WarehouseNetwork.changed(this.f_58857_);
      WarehouseControlBlockEntity control = WarehouseNetwork.loadedController(this.f_58857_, this.membership.getControllerPos());
      if (control != null) {
         control.storageChanged(this);
      }
   }

   private void loadQuantity(ItemStack stack, long count) {
      if (!stack.m_41619_() && count > 0L) {
         if (count > Long.MAX_VALUE - this.storedCount) {
            throw new IllegalArgumentException("Invalid warehouse quantity total");
         } else {
            this.quantities.merge(new WarehouseItemKey(stack), count, Math::addExact);
            this.storedCount += count;
         }
      }
   }

   private void saveFilters(CompoundTag tag) {
      ListTag saved = new ListTag();

      for (ItemStack filter : this.filters) {
         saved.add(filter.m_41739_(new CompoundTag()));
      }

      tag.m_128365_("StorageFilters", saved);
   }

   private void loadFilters(CompoundTag tag) {
      this.filters.clear();
      ListTag saved = tag.m_128437_("StorageFilters", 10);

      for (int i = 0; i < saved.size() && i < 10; i++) {
         ItemStack filter = ItemStack.m_41712_(saved.m_128728_(i));
         this.filters.add(filter.m_41619_() ? ItemStack.f_41583_ : filter.m_255036_(1));
      }
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = new CompoundTag();
      this.saveFilters(tag);
      this.membership.save(tag);
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.loadFilters(tag);
      this.membership.load(tag);
   }

   public int m_6643_() {
      return 0;
   }

   public boolean m_7983_() {
      return this.quantities.isEmpty();
   }

   public ItemStack m_8020_(int slot) {
      return ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      return ItemStack.f_41583_;
   }

   public ItemStack m_8016_(int slot) {
      return ItemStack.f_41583_;
   }

   public void m_6836_(int slot, ItemStack stack) {
   }

   public boolean m_7013_(int slot, ItemStack stack) {
      return false;
   }

   public boolean m_6542_(Player player) {
      return this.f_58857_ != null
         && this.f_58857_.m_7702_(this.f_58858_) == this
         && player.m_20275_((double)this.f_58858_.m_123341_() + 0.5, (double)this.f_58858_.m_123342_() + 0.5, (double)this.f_58858_.m_123343_() + 0.5) <= 64.0;
   }

   public void m_6211_() {
      if (!this.quantities.isEmpty()) {
         this.quantities.clear();
         this.storedCount = 0L;
         this.syncChanged(false);
      }
   }

   public Component m_5446_() {
      return Component.m_237115_("container.ancientwarfare.warehouse_storage");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory inventory, Player player) {
      return new WarehouseStorageMenu(containerId, inventory, this.f_58858_);
   }

   private void syncChanged(boolean syncFilters) {
      this.contentsRevision++;
      this.m_6596_();
      if (syncFilters && this.f_58857_ != null && !this.f_58857_.f_46443_) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 2);
      }
   }
}
