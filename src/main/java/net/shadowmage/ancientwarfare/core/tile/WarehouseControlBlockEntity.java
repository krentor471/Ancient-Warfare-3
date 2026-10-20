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
   private final NonNullList<ItemStack> inputItems = NonNullList.m_122780_(9, ItemStack.f_41583_);
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

   public void m_142339_(Level level) {
      super.m_142339_(level);
      WarehouseNetwork.register(this);
   }

   public void m_7651_() {
      WarehouseNetwork.changed(this.f_58857_);
      super.m_7651_();
   }

   public void storageChanged(WarehouseStorageBlockEntity storage) {
      this.dirtyStorages.add(storage.m_58899_());
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
         if (!stack.m_41619_()) {
            ItemStack remainder = this.insertIntoStorage(stack);
            if (remainder.m_41613_() != stack.m_41613_()) {
               this.inputItems.set(slot, remainder);
               changed = true;
            }
         }
      }

      if (changed) {
         this.m_6596_();
      }
   }

   public ItemStack insertIntoStorage(ItemStack stack) {
      ItemStack remaining = stack.m_41777_();

      for (WarehouseStorageBlockEntity storage : this.destinations(stack)) {
         remaining = storage.insertStack(remaining);
         if (remaining.m_41619_()) {
            return ItemStack.f_41583_;
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
         } else if (filters.stream().anyMatch(filter -> !filter.m_41619_() && ItemStack.m_150942_(filter, stack))) {
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
      if (!filter.m_41619_() && amount > 0) {
         amount = Math.min(amount, Math.min(this.m_6893_(), filter.m_41741_()));
         this.ensureIndex();
         ItemStack result = filter.m_41777_();
         result.m_41764_(0);
         Set<BlockPos> candidates = this.locations.getOrDefault(new WarehouseItemKey(filter), Set.of());

         for (BlockPos position : new ArrayList<>(candidates)) {
            WarehouseStorageBlockEntity storage = this.members.get(position);
            if (storage != null && this.isAvailable(storage)) {
               ItemStack extracted = storage.extractMatching(filter, amount - result.m_41613_());
               if (!extracted.m_41619_()) {
                  result.m_41769_(extracted.m_41613_());
               }

               if (result.m_41613_() >= amount) {
                  break;
               }
            }
         }

         return result.m_41619_() ? ItemStack.f_41583_ : result;
      } else {
         return ItemStack.f_41583_;
      }
   }

   public NonNullList<ItemStack> getDisplayItems(int maxSlots, String search, WarehouseSortType sortType, boolean descending, int offset) {
      List<WarehouseEntry> entries = this.getFilteredEntries(search, sortType, descending);
      NonNullList<ItemStack> display = NonNullList.m_122780_(maxSlots, ItemStack.f_41583_);
      int start = Math.max(0, Math.min(offset, entries.size()));

      for (int index = 0; index < maxSlots && start + index < entries.size(); index++) {
         display.set(index, entries.get(start + index).stack().m_41777_());
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
      return filter.m_41619_() ? 0L : this.totals.getOrDefault(new WarehouseItemKey(filter), 0L);
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
            ? Comparator.comparingLong(WarehouseEntry::count).thenComparing(entryx -> entryx.stack().m_41786_().getString())
            : Comparator.comparing(entryx -> entryx.stack().m_41786_().getString().toLowerCase(Locale.ROOT));
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
         String name = stack.m_41786_().getString().toLowerCase(Locale.ROOT);
         String itemId = BuiltInRegistries.f_257033_.m_7981_(stack.m_41720_()).toString().toLowerCase(Locale.ROOT);
         return searchTerm.startsWith("@") ? itemId.contains(searchTerm.substring(1)) : name.contains(searchTerm) || itemId.contains(searchTerm);
      }
   }

   public List<WarehouseStorageBlockEntity> findStorageBlocks() {
      this.ensureIndex();
      return this.members.values().stream().filter(this::isAvailable).toList();
   }

   private boolean isAvailable(WarehouseStorageBlockEntity storage) {
      return this.f_58857_ != null
         && !storage.m_58901_()
         && this.f_58857_.m_46805_(storage.m_58899_())
         && this.f_58857_.m_7702_(storage.m_58899_()) == storage
         && this.f_58858_.equals(storage.getMembership().getControllerPos());
   }

   private void ensureIndex() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && !this.updatingIndex) {
         this.updatingIndex = true;

         try {
            long now = this.f_58857_.m_46467_();
            long topology = WarehouseNetwork.revision(this.f_58857_);
            if (topology != this.topologyRevision || now >= this.nextMemberCheck) {
               Set<BlockPos> previousKnown = new LinkedHashSet<>(this.knownStorages);
               Map<BlockPos, WarehouseStorageBlockEntity> found = new LinkedHashMap<>();
               List<BlockEntity> loaded = WarehouseNetwork.loadedEntities(this.f_58857_, this.getBoundsMin(), this.getBoundsMax());
               loaded.sort(Comparator.comparingLong(entity -> entity.m_58899_().m_121878_()));

               for (BlockEntity entity : loaded) {
                  if (entity instanceof WarehouseStorageBlockEntity) {
                     WarehouseStorageBlockEntity storage = (WarehouseStorageBlockEntity)entity;
                     if (storage.getMembership().findController(storage) == this) {
                        found.put(storage.m_58899_(), storage);
                        this.knownStorages.add(storage.m_58899_());
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
                  } else if (!this.f_58857_.m_46805_(known)) {
                     offline++;
                  } else if (!found.containsKey(known)) {
                     this.knownStorages.remove(known);
                  }
               }

               if (offline != this.offlineStorages) {
                  this.offlineStorages = offline;
                  this.inventoryRevision++;
               }

               this.topologyRevision = WarehouseNetwork.revision(this.f_58857_);
               this.nextMemberCheck = now + 20L;
               if (!previousKnown.equals(this.knownStorages)) {
                  this.m_6596_();
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
                        if (!entryx.stack().m_41619_() && entryx.count() > 0L) {
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
            if (!entry.stack().m_41619_() && entry.count() > 0L) {
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
            if (a.get(i).count() != b.get(i).count() || !ItemStack.m_150942_(a.get(i).stack(), b.get(i).stack())) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean exchange(List<ItemStack> requirements, List<ItemStack> deposits, boolean simulate) {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         List<WarehouseStorageBlockEntity> storages = this.findStorageBlocks();
         Map<WarehouseItemKey, Long> requiredTotals = new HashMap<>();

         for (ItemStack required : requirements) {
            if (!required.m_41619_()) {
               requiredTotals.merge(new WarehouseItemKey(required), (long)required.m_41613_(), Long::sum);
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
            long missing = (long)requiredxx.m_41613_();
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
            if (!deposit.m_41619_()) {
               long remaining = (long)deposit.m_41613_();
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

   public void adjustBounds(int minXDelta, int maxXDelta, int minZDelta, int maxZDelta) {
      WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjust(
         this.f_58858_, this.getBoundsMin(), this.getBoundsMax(), 16, minXDelta, maxXDelta, minZDelta, maxZDelta
      );
      if (adjusted != null) {
         this.boundsMin = adjusted.min();
         this.boundsMax = adjusted.max();
         this.topologyRevision = Long.MIN_VALUE;
         WarehouseNetwork.changed(this.f_58857_);
         this.m_6596_();
         if (this.f_58857_ != null) {
            if (this.f_58857_ instanceof ServerLevel serverLevel) {
               serverLevel.m_46745_(this.f_58858_).m_8092_(true);
            }

            this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
         }
      }
   }

   @Override
   public boolean containsWorkTarget(BlockPos pos) {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return pos.m_123341_() >= min.m_123341_()
         && pos.m_123341_() <= max.m_123341_()
         && pos.m_123342_() >= min.m_123342_()
         && pos.m_123342_() <= max.m_123342_()
         && pos.m_123343_() >= min.m_123343_()
         && pos.m_123343_() <= max.m_123343_();
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
      Direction facing = state.m_61138_(WarehouseBlock.FACING) ? ((Direction)state.m_61143_(WarehouseBlock.FACING)).m_122424_() : Direction.SOUTH;
      BlockPos near = pos.m_121945_(facing);
      BlockPos far = pos.m_5484_(facing, 5);
      int minY = pos.m_123342_();
      int maxY = pos.m_123342_() + 4 - 1;
      if (facing.m_122434_() == Axis.Z) {
         this.boundsMin = new BlockPos(pos.m_123341_() - 2, minY, Math.min(near.m_123343_(), far.m_123343_()));
         this.boundsMax = new BlockPos(pos.m_123341_() + 2, maxY, Math.max(near.m_123343_(), far.m_123343_()));
      } else {
         this.boundsMin = new BlockPos(Math.min(near.m_123341_(), far.m_123341_()), minY, pos.m_123343_() - 2);
         this.boundsMax = new BlockPos(Math.max(near.m_123341_(), far.m_123341_()), maxY, pos.m_123343_() + 2);
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.inputItems);
      tag.m_128356_("BoundsMin", this.getBoundsMin().m_121878_());
      tag.m_128356_("BoundsMax", this.getBoundsMax().m_121878_());
      tag.m_128388_("KnownWarehouseStorages", this.knownStorages.stream().mapToLong(BlockPos::m_121878_).toArray());
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      ContainerHelper.m_18980_(tag, this.inputItems);
      boolean hasMin = tag.m_128441_("BoundsMin");
      boolean hasMax = tag.m_128441_("BoundsMax");
      if (hasMin && hasMax) {
         this.boundsMin = BlockPos.m_122022_(tag.m_128454_("BoundsMin"));
         this.boundsMax = BlockPos.m_122022_(tag.m_128454_("BoundsMax"));
         if ((this.f_58857_ == null || !this.f_58857_.f_46443_) && !WorksiteBoundsGeometry.hasValidSize(this.boundsMin, this.boundsMax, 16)) {
            this.resetDefaultBounds(this.f_58858_, this.m_58900_());
         }
      }

      this.knownStorages.clear();

      for (long position : tag.m_128467_("KnownWarehouseStorages")) {
         this.knownStorages.add(BlockPos.m_122022_(position));
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
      WarehouseNetwork.changed(this.f_58857_);
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = super.m_5995_();
      tag.m_128356_("BoundsMin", this.getBoundsMin().m_121878_());
      tag.m_128356_("BoundsMax", this.getBoundsMax().m_121878_());
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.m_142466_(tag);
   }

   public int m_6643_() {
      return this.inputItems.size();
   }

   public boolean m_7983_() {
      return this.inputItems.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int slot) {
      return slot >= 0 && slot < this.inputItems.size() ? (ItemStack)this.inputItems.get(slot) : ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      ItemStack removed = slot >= 0 && slot < this.inputItems.size() ? ContainerHelper.m_18969_(this.inputItems, slot, amount) : ItemStack.f_41583_;
      if (!removed.m_41619_()) {
         this.m_6596_();
      }

      return removed;
   }

   public ItemStack m_8016_(int slot) {
      return slot >= 0 && slot < this.inputItems.size() ? ContainerHelper.m_18966_(this.inputItems, slot) : ItemStack.f_41583_;
   }

   public void m_6836_(int slot, ItemStack stack) {
      if (slot >= 0 && slot < this.inputItems.size()) {
         this.inputItems.set(slot, stack);
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
      this.inputItems.clear();
      this.m_6596_();
   }

   public Component m_5446_() {
      return Component.m_237115_("container.ancientwarfare.warehouse_control");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new WarehouseControlMenu(containerId, playerInventory, this.f_58858_);
   }
}
