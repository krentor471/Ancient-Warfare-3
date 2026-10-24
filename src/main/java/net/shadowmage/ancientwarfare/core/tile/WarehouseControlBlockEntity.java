package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.WarehouseBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseEntry;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseItemKey;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseSortType;
import net.shadowmage.ancientwarfare.core.common.worksite.BoundedArea;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteBoundsGeometry;
import net.shadowmage.ancientwarfare.core.menu.WarehouseControlMenu;
import org.jetbrains.annotations.Nullable;

public class WarehouseControlBlockEntity extends BlockEntity implements Container, MenuProvider, BoundedArea {
   public static final int INPUT_SLOT_COUNT = 9;
   public static final int DEFAULT_RADIUS = 2;
   public static final int DEFAULT_DEPTH = 5;
   public static final int DEFAULT_HEIGHT = 4;
   public static final int MAX_BOUNDS_WIDTH = 16;
   private static final String MIN_TAG = "BoundsMin";
   private static final String MAX_TAG = "BoundsMax";
   private final NonNullList<ItemStack> inputItems = NonNullList.withSize(9, ItemStack.EMPTY);
   private BlockPos boundsMin;
   private BlockPos boundsMax;
   private int tickCounter;
   private final Map<BlockPos, WarehouseStorageBlockEntity> members = new LinkedHashMap<>();
   private final Map<BlockPos, List<WarehouseEntry>> snapshots = new HashMap<>();
   private final Map<BlockPos, Long> capacities = new HashMap<>();
   private final Map<BlockPos, Long> contentRevisions = new HashMap<>();
   private final Map<WarehouseItemKey, Long> totals = new LinkedHashMap<>();
   private final Map<WarehouseItemKey, Set<BlockPos>> locations = new HashMap<>();
   private final Set<BlockPos> dirtyStorages = new LinkedHashSet<>();
   private final Set<BlockPos> knownStorages = new LinkedHashSet<>();
   private long topologyRevision = Long.MIN_VALUE;
   private long inventoryRevision;
   private long nextMemberCheck;
   private long nextAudit;
   private boolean updatingIndex;
   private List<WarehouseEntry> filteredCache = List.of();
   private String cachedSearch;
   private WarehouseSortType cachedSort;
   private boolean cachedDescending;
   private long cachedInventoryRevision = -1L;
   private int occupiedSlots;
   private int totalSlots;
   private int offlineStorages;
   private long totalCapacity;

   public void setLevel(Level level) {
      super.setLevel(level);
      WarehouseNetwork.register(this);
   }

   public void setRemoved() {
      WarehouseNetwork.changed(this.level);
      super.setRemoved();
   }

   public void storageChanged(WarehouseStorageBlockEntity storage) {
      this.dirtyStorages.add(storage.getBlockPos());
   }

   public WarehouseControlBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_CONTROL), pos, state);
      this.resetDefaultBounds(pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, WarehouseControlBlockEntity warehouse) {
      warehouse.ensureIndex();
      if (++warehouse.tickCounter >= 10) {
         warehouse.tickCounter = 0;
         warehouse.depositInputs();
      }
   }

   public void depositInputs() {
      boolean changed = false;

      for (int slot = 0; slot < this.inputItems.size(); slot++) {
         ItemStack stack = (ItemStack)this.inputItems.get(slot);
         if (!stack.isEmpty()) {
            ItemStack remainder = this.insertIntoStorage(stack);
            if (remainder.getCount() != stack.getCount()) {
               this.inputItems.set(slot, remainder);
               changed = true;
            }
         }
      }

      if (changed) {
         this.setChanged();
      }
   }

   public ItemStack insertIntoStorage(ItemStack stack) {
      ItemStack remaining = stack.copy();

      for (WarehouseStorageBlockEntity storage : this.destinations(stack)) {
         remaining = storage.insertStack(remaining);
         if (remaining.isEmpty()) {
            return ItemStack.EMPTY;
         }
      }

      return remaining;
   }

   private List<WarehouseStorageBlockEntity> destinations(ItemStack stack) {
      return this.destinations(stack, this.findStorageBlocks(), storage -> storage.getQuantity(stack) > 0L);
   }

   private List<WarehouseStorageBlockEntity> destinations(
      ItemStack stack, List<WarehouseStorageBlockEntity> storages, Predicate<WarehouseStorageBlockEntity> hasItem
   ) {
      List<WarehouseStorageBlockEntity> eligible = new ArrayList<>();
      List<WarehouseStorageBlockEntity> unfiltered = new ArrayList<>();

      for (WarehouseStorageBlockEntity storage : storages) {
         List<ItemStack> filters = storage.getFilters();
         if (filters.isEmpty()) {
            unfiltered.add(storage);
         } else if (filters.stream().anyMatch(filter -> !filter.isEmpty() && ItemStack.isSameItemSameTags(filter, stack))) {
            eligible.add(storage);
         }
      }

      Comparator<WarehouseStorageBlockEntity> existingFirst = Comparator.comparingInt(storagex -> hasItem.test(storagex) ? 0 : 1);
      eligible.sort(existingFirst);
      unfiltered.sort(existingFirst);
      eligible.addAll(unfiltered);
      return eligible;
   }

   public ItemStack extractFromStorage(ItemStack filter, int amount) {
      if (!filter.isEmpty() && amount > 0) {
         amount = Math.min(amount, Math.min(this.getMaxStackSize(), filter.getMaxStackSize()));
         this.ensureIndex();
         ItemStack result = filter.copy();
         result.setCount(0);
         Set<BlockPos> candidates = this.locations.getOrDefault(new WarehouseItemKey(filter), Set.of());

         for (BlockPos position : new ArrayList<>(candidates)) {
            WarehouseStorageBlockEntity storage = this.members.get(position);
            if (storage != null && this.isAvailable(storage)) {
               ItemStack extracted = storage.extractMatching(filter, amount - result.getCount());
               if (!extracted.isEmpty()) {
                  result.grow(extracted.getCount());
               }

               if (result.getCount() >= amount) {
                  break;
               }
            }
         }

         return result.isEmpty() ? ItemStack.EMPTY : result;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public NonNullList<ItemStack> getDisplayItems(int maxSlots, String search, WarehouseSortType sortType, boolean descending, int offset) {
      List<WarehouseEntry> entries = this.getFilteredEntries(search, sortType, descending);
      NonNullList<ItemStack> display = NonNullList.withSize(maxSlots, ItemStack.EMPTY);
      int start = Math.max(0, Math.min(offset, entries.size()));

      for (int index = 0; index < maxSlots && start + index < entries.size(); index++) {
         display.set(index, entries.get(start + index).stack().copy());
      }

      return display;
   }

   public int getDisplayItemCount(String search) {
      return this.getFilteredEntries(search, WarehouseSortType.NAME, false).size();
   }

   public int getCurrentStoredCount() {
      return (int)Math.min(2147483647L, this.getTotalItemCount());
   }

   public int getCountOf(ItemStack filter) {
      return (int)Math.min(2147483647L, this.getCountOfLong(filter));
   }

   public long getCountOfLong(ItemStack filter) {
      this.ensureIndex();
      return filter.isEmpty() ? 0L : this.totals.getOrDefault(new WarehouseItemKey(filter), 0L);
   }

   public long getInventoryRevision() {
      this.ensureIndex();
      return this.inventoryRevision;
   }

   public long getTotalItemCount() {
      this.ensureIndex();
      return this.totals.values().stream().mapToLong(Long::longValue).sum();
   }

   public int getOccupiedSlotCount() {
      this.ensureIndex();
      return this.occupiedSlots;
   }

   public int getTotalSlotCount() {
      this.ensureIndex();
      return this.totalSlots;
   }

   public int getOfflineStorageCount() {
      this.ensureIndex();
      return this.offlineStorages;
   }

   public long getTotalCapacity() {
      this.ensureIndex();
      return this.totalCapacity;
   }

   public int getMaxStorageCount() {
      return (int)Math.min(2147483647L, this.getTotalCapacity());
   }

   public List<WarehouseEntry> getFilteredEntries(String search, WarehouseSortType sortType, boolean descending) {
      this.ensureIndex();
      String searchTerm = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();
      if (this.cachedInventoryRevision == this.inventoryRevision
         && searchTerm.equals(this.cachedSearch)
         && sortType == this.cachedSort
         && descending == this.cachedDescending) {
         return this.filteredCache;
      } else {
         List<WarehouseEntry> items = new ArrayList<>();

         for (Entry<WarehouseItemKey, Long> entry : this.totals.entrySet()) {
            ItemStack prototype = entry.getKey().stack();
            if (this.matchesSearch(prototype, searchTerm)) {
               items.add(new WarehouseEntry(prototype, entry.getValue()));
            }
         }

         Comparator<WarehouseEntry> comparator = sortType == WarehouseSortType.COUNT
            ? Comparator.comparingLong(WarehouseEntry::count).thenComparing(entryx -> entryx.stack().getHoverName().getString())
            : Comparator.comparing(entryx -> entryx.stack().getHoverName().getString().toLowerCase(Locale.ROOT));
         if (descending) {
            comparator = comparator.reversed();
         }

         items.sort(comparator);
         this.cachedSearch = searchTerm;
         this.cachedSort = sortType;
         this.cachedDescending = descending;
         this.cachedInventoryRevision = this.inventoryRevision;
         this.filteredCache = List.copyOf(items);
         return this.filteredCache;
      }
   }

   private boolean matchesSearch(ItemStack stack, String searchTerm) {
      if (searchTerm.isEmpty()) {
         return true;
      } else {
         String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
         String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
         return searchTerm.startsWith("@") ? itemId.contains(searchTerm.substring(1)) : name.contains(searchTerm) || itemId.contains(searchTerm);
      }
   }

   public List<WarehouseStorageBlockEntity> findStorageBlocks() {
      this.ensureIndex();
      return this.members.values().stream().filter(this::isAvailable).toList();
   }

   private boolean isAvailable(WarehouseStorageBlockEntity storage) {
      return this.level != null
         && !storage.isRemoved()
         && this.level.hasChunkAt(storage.getBlockPos())
         && this.level.getBlockEntity(storage.getBlockPos()) == storage
         && this.worldPosition.equals(storage.getMembership().getControllerPos());
   }

   private void ensureIndex() {
      if (this.level != null && !this.level.isClientSide && !this.updatingIndex) {
         this.updatingIndex = true;

         try {
            long now = this.level.getGameTime();
            long topology = WarehouseNetwork.revision(this.level);
            if (topology != this.topologyRevision || now >= this.nextMemberCheck) {
               Set<BlockPos> previousKnown = new LinkedHashSet<>(this.knownStorages);
               Map<BlockPos, WarehouseStorageBlockEntity> found = new LinkedHashMap<>();
               List<BlockEntity> loaded = WarehouseNetwork.loadedEntities(this.level, this.getBoundsMin(), this.getBoundsMax());
               loaded.sort(Comparator.comparingLong(entity -> entity.getBlockPos().asLong()));

               for (BlockEntity entity : loaded) {
                  if (entity instanceof WarehouseStorageBlockEntity) {
                     WarehouseStorageBlockEntity storage = (WarehouseStorageBlockEntity)entity;
                     if (storage.getMembership().findController(storage) == this) {
                        found.put(storage.getBlockPos(), storage);
                        this.knownStorages.add(storage.getBlockPos());
                     }
                  }
               }

               for (BlockPos old : new ArrayList<>(this.snapshots.keySet())) {
                  if (!found.containsKey(old)) {
                     this.removeSnapshot(old);
                  }
               }

               for (Entry<BlockPos, WarehouseStorageBlockEntity> entry : found.entrySet()) {
                  if (this.members.get(entry.getKey()) != entry.getValue()) {
                     this.dirtyStorages.add(entry.getKey());
                  }
               }

               this.members.clear();
               this.members.putAll(found);
               int offline = 0;

               for (BlockPos known : new ArrayList<>(this.knownStorages)) {
                  if (!this.containsWorkTarget(known)) {
                     this.knownStorages.remove(known);
                  } else if (!this.level.hasChunkAt(known)) {
                     offline++;
                  } else if (!found.containsKey(known)) {
                     this.knownStorages.remove(known);
                  }
               }

               if (offline != this.offlineStorages) {
                  this.offlineStorages = offline;
                  this.inventoryRevision++;
               }

               this.topologyRevision = WarehouseNetwork.revision(this.level);
               this.nextMemberCheck = now + 20L;
               if (!previousKnown.equals(this.knownStorages)) {
                  this.setChanged();
               }
            }

            if (now >= this.nextAudit) {
               this.dirtyStorages.addAll(this.members.keySet());
               this.nextAudit = now + 200L;
            }

            for (BlockPos position : new ArrayList<>(this.dirtyStorages)) {
               WarehouseStorageBlockEntity storage = this.members.get(position);
               if (storage != null && this.isAvailable(storage)) {
                  List<WarehouseEntry> current = storage.copyEntries();
                  List<WarehouseEntry> previous = this.snapshots.get(position);
                  long capacity = storage.getItemCapacity();
                  long contentRevision = storage.getContentsRevision();
                  if (previous != null && sameEntries(previous, current) && this.capacities.getOrDefault(position, -1L) == capacity) {
                     if (this.contentRevisions.getOrDefault(position, Long.MIN_VALUE) != contentRevision) {
                        this.contentRevisions.put(position, contentRevision);
                        this.inventoryRevision++;
                     }
                  } else {
                     this.removeSnapshot(position);
                     this.snapshots.put(position, current);
                     this.capacities.put(position, capacity);
                     this.contentRevisions.put(position, contentRevision);
                     this.totalCapacity += capacity;
                     this.totalSlots += (int)(capacity / 64L);

                     for (WarehouseEntry entryx : current) {
                        if (!entryx.stack().isEmpty() && entryx.count() > 0L) {
                           this.occupiedSlots++;
                           WarehouseItemKey key = new WarehouseItemKey(entryx.stack());
                           this.totals.merge(key, entryx.count(), Long::sum);
                           this.locations.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(position);
                        }
                     }

                     this.inventoryRevision++;
                  }
               }
            }

            this.dirtyStorages.clear();
         } finally {
            this.updatingIndex = false;
         }
      }
   }

   private void removeSnapshot(BlockPos position) {
      List<WarehouseEntry> old = this.snapshots.remove(position);
      if (old != null) {
         long capacity = this.capacities.getOrDefault(position, 0L);
         this.capacities.remove(position);
         this.contentRevisions.remove(position);
         this.totalCapacity -= capacity;
         this.totalSlots -= (int)(capacity / 64L);

         for (WarehouseEntry entry : old) {
            if (!entry.stack().isEmpty() && entry.count() > 0L) {
               this.occupiedSlots--;
               WarehouseItemKey key = new WarehouseItemKey(entry.stack());
               long remaining = this.totals.getOrDefault(key, 0L) - entry.count();
               if (remaining <= 0L) {
                  this.totals.remove(key);
               } else {
                  this.totals.put(key, remaining);
               }

               Set<BlockPos> slots = this.locations.get(key);
               if (slots != null) {
                  slots.remove(position);
                  if (slots.isEmpty()) {
                     this.locations.remove(key);
                  }
               }
            }
         }

         this.inventoryRevision++;
      }
   }

   private static boolean sameEntries(List<WarehouseEntry> a, List<WarehouseEntry> b) {
      if (a.size() != b.size()) {
         return false;
      } else {
         for (int i = 0; i < a.size(); i++) {
            if (a.get(i).count() != b.get(i).count() || !ItemStack.isSameItemSameTags(a.get(i).stack(), b.get(i).stack())) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean exchange(List<ItemStack> requirements, List<ItemStack> deposits, boolean simulate) {
      if (this.level != null && !this.level.isClientSide) {
         List<WarehouseStorageBlockEntity> storages = this.findStorageBlocks();
         Map<WarehouseItemKey, Long> requiredTotals = new HashMap<>();

         for (ItemStack required : requirements) {
            if (!required.isEmpty()) {
               requiredTotals.merge(new WarehouseItemKey(required), (long)required.getCount(), Long::sum);
            }
         }

         for (Entry<WarehouseItemKey, Long> requiredx : requiredTotals.entrySet()) {
            if (this.totals.getOrDefault(requiredx.getKey(), 0L) < requiredx.getValue()) {
               return false;
            }
         }

         List<Map<WarehouseItemKey, Long>> copies = new ArrayList<>();

         for (WarehouseStorageBlockEntity storage : storages) {
            Map<WarehouseItemKey, Long> copy = new LinkedHashMap<>();

            for (WarehouseEntry entry : storage.copyEntries()) {
               copy.put(new WarehouseItemKey(entry.stack()), entry.count());
            }

            copies.add(copy);
         }

         for (ItemStack requiredxx : requirements) {
            long missing = (long)requiredxx.getCount();
            if (missing < 0L) {
               return false;
            }

            WarehouseItemKey key = new WarehouseItemKey(requiredxx);

            for (Map<WarehouseItemKey, Long> inventory : copies) {
               long available = inventory.getOrDefault(key, 0L);
               long moved = Math.min(missing, available);
               if (available == moved) {
                  inventory.remove(key);
               } else {
                  inventory.put(key, available - moved);
               }

               missing -= moved;
            }

            if (missing != 0L) {
               return false;
            }
         }

         for (ItemStack deposit : deposits) {
            if (!deposit.isEmpty()) {
               long remaining = (long)deposit.getCount();
               WarehouseItemKey key = new WarehouseItemKey(deposit);

               for (WarehouseStorageBlockEntity destination : this.destinations(
                  deposit, storages, storage -> copies.get(storages.indexOf(storage)).getOrDefault(key, 0L) > 0L
               )) {
                  Map<WarehouseItemKey, Long> inventory = copies.get(storages.indexOf(destination));
                  long occupied = inventory.values().stream().mapToLong(Long::longValue).sum();
                  long moved = Math.min(remaining, Math.max(0L, destination.getItemCapacity() - occupied));
                  if (moved > 0L) {
                     inventory.merge(key, moved, Long::sum);
                  }

                  remaining -= moved;
                  if (remaining == 0L) {
                     break;
                  }
               }

               if (remaining != 0L) {
                  return false;
               }
            }
         }

         if (!simulate) {
            for (int i = 0; i < storages.size(); i++) {
               List<WarehouseEntry> entries = new ArrayList<>();

               for (Entry<WarehouseItemKey, Long> entry : copies.get(i).entrySet()) {
                  entries.add(new WarehouseEntry(entry.getKey().stack(), entry.getValue()));
               }

               if (!sameEntries(storages.get(i).copyEntries(), entries)) {
                  storages.get(i).replaceEntries(entries);
               }
            }
         }

         return true;
      } else {
         return false;
      }
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

   public void adjustBounds(int minXDelta, int maxXDelta, int minZDelta, int maxZDelta) {
      WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjust(
         this.worldPosition, this.getBoundsMin(), this.getBoundsMax(), 16, minXDelta, maxXDelta, minZDelta, maxZDelta
      );
      if (adjusted != null) {
         this.boundsMin = adjusted.min();
         this.boundsMax = adjusted.max();
         this.topologyRevision = Long.MIN_VALUE;
         WarehouseNetwork.changed(this.level);
         this.setChanged();
         if (this.level != null) {
            if (this.level instanceof ServerLevel serverLevel) {
               serverLevel.getChunkAt(this.worldPosition).setUnsaved(true);
            }

            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
         }
      }
   }

   @Override
   public boolean containsWorkTarget(BlockPos pos) {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return pos.getX() >= min.getX()
         && pos.getX() <= max.getX()
         && pos.getY() >= min.getY()
         && pos.getY() <= max.getY()
         && pos.getZ() >= min.getZ()
         && pos.getZ() <= max.getZ();
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
      Direction facing = state.hasProperty(WarehouseBlock.FACING) ? ((Direction)state.getValue(WarehouseBlock.FACING)).getOpposite() : Direction.SOUTH;
      BlockPos near = pos.relative(facing);
      BlockPos far = pos.relative(facing, 5);
      int minY = pos.getY();
      int maxY = pos.getY() + 4 - 1;
      if (facing.getAxis() == Axis.Z) {
         this.boundsMin = new BlockPos(pos.getX() - 2, minY, Math.min(near.getZ(), far.getZ()));
         this.boundsMax = new BlockPos(pos.getX() + 2, maxY, Math.max(near.getZ(), far.getZ()));
      } else {
         this.boundsMin = new BlockPos(Math.min(near.getX(), far.getX()), minY, pos.getZ() - 2);
         this.boundsMax = new BlockPos(Math.max(near.getX(), far.getX()), maxY, pos.getZ() + 2);
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.inputItems);
      tag.putLong("BoundsMin", this.getBoundsMin().asLong());
      tag.putLong("BoundsMax", this.getBoundsMax().asLong());
      tag.putLongArray("KnownWarehouseStorages", this.knownStorages.stream().mapToLong(BlockPos::asLong).toArray());
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      ContainerHelper.loadAllItems(tag, this.inputItems);
      boolean hasMin = tag.contains("BoundsMin");
      boolean hasMax = tag.contains("BoundsMax");
      if (hasMin && hasMax) {
         this.boundsMin = BlockPos.of(tag.getLong("BoundsMin"));
         this.boundsMax = BlockPos.of(tag.getLong("BoundsMax"));
         if ((this.level == null || !this.level.isClientSide) && !WorksiteBoundsGeometry.hasValidSize(this.boundsMin, this.boundsMax, 16)) {
            this.resetDefaultBounds(this.worldPosition, this.getBlockState());
         }
      }

      this.knownStorages.clear();

      for (long position : tag.getLongArray("KnownWarehouseStorages")) {
         this.knownStorages.add(BlockPos.of(position));
      }

      this.members.clear();
      this.snapshots.clear();
      this.capacities.clear();
      this.contentRevisions.clear();
      this.totals.clear();
      this.locations.clear();
      this.dirtyStorages.clear();
      this.occupiedSlots = this.totalSlots = this.offlineStorages = 0;
      this.totalCapacity = 0L;
      this.topologyRevision = Long.MIN_VALUE;
      this.inventoryRevision++;
      WarehouseNetwork.changed(this.level);
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      tag.putLong("BoundsMin", this.getBoundsMin().asLong());
      tag.putLong("BoundsMax", this.getBoundsMax().asLong());
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.load(tag);
   }

   public int getContainerSize() {
      return this.inputItems.size();
   }

   public boolean isEmpty() {
      return this.inputItems.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int slot) {
      return slot >= 0 && slot < this.inputItems.size() ? (ItemStack)this.inputItems.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      ItemStack removed = slot >= 0 && slot < this.inputItems.size() ? ContainerHelper.removeItem(this.inputItems, slot, amount) : ItemStack.EMPTY;
      if (!removed.isEmpty()) {
         this.setChanged();
      }

      return removed;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      return slot >= 0 && slot < this.inputItems.size() ? ContainerHelper.takeItem(this.inputItems, slot) : ItemStack.EMPTY;
   }

   public void setItem(int slot, ItemStack stack) {
      if (slot >= 0 && slot < this.inputItems.size()) {
         this.inputItems.set(slot, stack);
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
      this.inputItems.clear();
      this.setChanged();
   }

   public Component getDisplayName() {
      return Component.translatable("container.ancientwarfare.warehouse_control");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new WarehouseControlMenu(containerId, playerInventory, this.worldPosition);
   }
}
