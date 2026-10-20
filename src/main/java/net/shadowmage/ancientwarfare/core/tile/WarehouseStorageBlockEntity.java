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

   public void setLevel(Level level) {
      super.setLevel(level);
      WarehouseNetwork.register(this);
   }

   public void setRemoved() {
      WarehouseNetwork.changed(this.level);
      super.setRemoved();
   }

   public void setChanged() {
      super.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         WarehouseControlBlockEntity control = WarehouseNetwork.loadedController(this.level, this.membership.getControllerPos());
         if (control != null) {
            control.storageChanged(this);
         }
      }
   }

   public static int storageSize(BlockState state) {
      if (state.is(AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_LARGE))) {
         return 54;
      } else {
         return state.is(AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_MEDIUM)) ? 45 : 27;
      }
   }

   public long getCapacity() {
      if (this.getBlockState().is(AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_LARGE))) {
         return 1728L;
      } else {
         return this.getBlockState().is(AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_STORAGE_MEDIUM)) ? 1152L : 576L;
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
      return stack.isEmpty() ? 0L : this.quantities.getOrDefault(new WarehouseItemKey(stack), 0L);
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
         if (!entry.stack().isEmpty() && entry.count() > 0L) {
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
      this.clearContent();
      return result;
   }

   public ItemStack insertStack(ItemStack stack) {
      return this.insertStack(stack, true);
   }

   public ItemStack insertStack(ItemStack stack, boolean allowNewKinds) {
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         WarehouseItemKey key = new WarehouseItemKey(stack);
         long old = this.quantities.getOrDefault(key, 0L);
         if (!allowNewKinds && old == 0L) {
            return stack.copy();
         } else {
            long available = Math.max(0L, this.getCapacity() - this.storedCount);
            int moved = (int)Math.min((long)stack.getCount(), available);
            if (moved <= 0) {
               return stack.copy();
            } else {
               this.quantities.put(key, old + (long)moved);
               this.storedCount += (long)moved;
               this.syncChanged(false);
               return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
            }
         }
      }
   }

   public ItemStack extractMatching(ItemStack filter, int amount) {
      if (!filter.isEmpty() && amount > 0) {
         WarehouseItemKey key = new WarehouseItemKey(filter);
         long old = this.quantities.getOrDefault(key, 0L);
         int moved = (int)Math.min(old, (long)Math.min(amount, filter.getMaxStackSize()));
         if (moved <= 0) {
            return ItemStack.EMPTY;
         } else {
            if (old == (long)moved) {
               this.quantities.remove(key);
            } else {
               this.quantities.put(key, old - (long)moved);
            }

            this.storedCount -= (long)moved;
            this.syncChanged(false);
            return filter.copyWithCount(moved);
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public List<ItemStack> getFilters() {
      return this.filters.stream().<ItemStack>map(ItemStack::copy).toList();
   }

   public int getFilterCount() {
      return this.filters.size();
   }

   public ItemStack getFilter(int index) {
      return index >= 0 && index < this.filters.size() ? this.filters.get(index).copy() : ItemStack.EMPTY;
   }

   public boolean matchesFilter(ItemStack stack) {
      return !stack.isEmpty() && this.filters.stream().anyMatch(filter -> ItemStack.isSameItemSameTags(filter, stack));
   }

   public boolean addFilter(ItemStack stack) {
      if (this.filters.size() >= 10) {
         return false;
      } else {
         this.filters.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
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
         this.filters.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
         this.syncChanged(true);
      }
   }

   public void setFilters(List<ItemStack> replacement) {
      this.filters.clear();

      for (ItemStack stack : replacement) {
         if (this.filters.size() >= 10) {
            break;
         }

         this.filters.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
      }

      this.syncChanged(true);
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("WarehouseStorageVersion", 2);
      ListTag entries = new ListTag();
      this.quantities.forEach((key, count) -> {
         CompoundTag entry = new CompoundTag();
         entry.put("Item", key.stack().save(new CompoundTag()));
         entry.putLong("Quantity", count);
         entries.add(entry);
      });
      tag.put("ItemQuantities", entries);
      this.saveFilters(tag);
      this.membership.save(tag);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.quantities.clear();
      this.storedCount = 0L;
      if (tag.contains("ItemQuantities", 9)) {
         ListTag entries = tag.getList("ItemQuantities", 10);

         for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            this.loadQuantity(ItemStack.of(entry.getCompound("Item")), entry.getLong("Quantity"));
         }
      } else {
         ListTag oldItems = tag.getList("Items", 10);

         for (int i = 0; i < oldItems.size(); i++) {
            CompoundTag entry = oldItems.getCompound(i);
            ItemStack stack = ItemStack.of(entry);
            int count = entry.contains("Count", 1) ? Byte.toUnsignedInt(entry.getByte("Count")) : stack.getCount();
            if (stack.isEmpty() && count > 0) {
               CompoundTag prototype = entry.copy();
               prototype.putByte("Count", (byte)1);
               stack = ItemStack.of(prototype);
            }

            this.loadQuantity(stack, (long)count);
         }
      }

      this.loadFilters(tag);
      this.membership.load(tag);
      this.contentsRevision++;
      WarehouseNetwork.changed(this.level);
      WarehouseControlBlockEntity control = WarehouseNetwork.loadedController(this.level, this.membership.getControllerPos());
      if (control != null) {
         control.storageChanged(this);
      }
   }

   private void loadQuantity(ItemStack stack, long count) {
      if (!stack.isEmpty() && count > 0L) {
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
         saved.add(filter.save(new CompoundTag()));
      }

      tag.put("StorageFilters", saved);
   }

   private void loadFilters(CompoundTag tag) {
      this.filters.clear();
      ListTag saved = tag.getList("StorageFilters", 10);

      for (int i = 0; i < saved.size() && i < 10; i++) {
         ItemStack filter = ItemStack.of(saved.getCompound(i));
         this.filters.add(filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1));
      }
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = new CompoundTag();
      this.saveFilters(tag);
      this.membership.save(tag);
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.loadFilters(tag);
      this.membership.load(tag);
   }

   public int getContainerSize() {
      return 0;
   }

   public boolean isEmpty() {
      return this.quantities.isEmpty();
   }

   public ItemStack getItem(int slot) {
      return ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      return ItemStack.EMPTY;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      return ItemStack.EMPTY;
   }

   public void setItem(int slot, ItemStack stack) {
   }

   public boolean canPlaceItem(int slot, ItemStack stack) {
      return false;
   }

   public boolean stillValid(Player player) {
      return this.level != null
         && this.level.getBlockEntity(this.worldPosition) == this
         && player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) <= 64.0;
   }

   public void clearContent() {
      if (!this.quantities.isEmpty()) {
         this.quantities.clear();
         this.storedCount = 0L;
         this.syncChanged(false);
      }
   }

   public Component getDisplayName() {
      return Component.translatable("container.ancientwarfare.warehouse_storage");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
      return new WarehouseStorageMenu(containerId, inventory, this.worldPosition);
   }

   private void syncChanged(boolean syncFilters) {
      this.contentsRevision++;
      this.setChanged();
      if (syncFilters && this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
      }
   }
}
