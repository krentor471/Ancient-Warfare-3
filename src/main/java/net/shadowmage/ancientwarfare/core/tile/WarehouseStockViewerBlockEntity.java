package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseStockFilter;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStockViewerMenu;
import org.jetbrains.annotations.Nullable;

public class WarehouseStockViewerBlockEntity extends BlockEntity implements MenuProvider {
   public static final int FILTER_COUNT = 4;
   private static final String FILTERS_TAG = "Filters";
   protected final WarehouseStockFilter[] filters = new WarehouseStockFilter[4];
   private final WarehouseMembership membership = new WarehouseMembership();
   private int tickCounter;
   private WarehouseControlBlockEntity lastController;
   private long lastRevision = Long.MIN_VALUE;
   private boolean filtersDirty = true;
   private boolean requireAll;
   private long configurationRevision;

   public WarehouseStockViewerBlockEntity(BlockPos pos, BlockState state) {
      this(AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_STOCK_VIEWER), pos, state);
   }

   protected WarehouseStockViewerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);

      for (int i = 0; i < this.filters.length; i++) {
         this.filters[i] = new WarehouseStockFilter();
      }
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, WarehouseStockViewerBlockEntity viewer) {
      if (++viewer.tickCounter >= 20) {
         viewer.tickCounter = 0;
         viewer.recount();
      }
   }

   public WarehouseStockFilter getFilter(int index) {
      return index >= 0 && index < this.filters.length ? this.filters[index] : new WarehouseStockFilter();
   }

   public BlockPos getControllerPos() {
      return this.membership.getControllerPos();
   }

   public int getConnectionStatus() {
      return this.membership.getConnectionStatus(this);
   }

   public boolean cycleController() {
      boolean changed = this.membership.cycleController(this);
      if (changed) {
         this.filtersDirty = true;
         this.recount();
         this.syncChanged();
      }

      return changed;
   }

   public boolean requiresAll() {
      return this.requireAll;
   }

   public long getConfigurationRevision() {
      return this.configurationRevision;
   }

   public void toggleRequireAll() {
      this.requireAll = !this.requireAll;
      this.filtersDirty = true;
      this.recount();
      this.syncChanged();
   }

   public void setFilterItem(int index, ItemStack stack) {
      if (index >= 0 && index < this.filters.length) {
         this.filters[index].setItem(stack);
         this.filtersDirty = true;
         this.recount();
         this.syncChanged();
      }
   }

   public void adjustTarget(int index, int delta) {
      if (index >= 0 && index < this.filters.length) {
         this.filters[index].adjustTarget(delta);
         this.filtersDirty = true;
         this.recount();
         this.syncChanged();
      }
   }

   public void cycleEquality(int index) {
      if (index >= 0 && index < this.filters.length) {
         this.filters[index].cycleEquality();
         this.filtersDirty = true;
         this.recount();
         this.syncChanged();
      }
   }

   protected void recount() {
      WarehouseControlBlockEntity control = this.findController();
      long revision = control == null ? -1L : control.getInventoryRevision();
      if (this.filtersDirty || control != this.lastController || revision != this.lastRevision) {
         for (WarehouseStockFilter filter : this.filters) {
            long quantity = control != null && !filter.item().m_41619_() ? control.getCountOfLong(filter.item()) : 0L;
            filter.setQuantity(quantity);
         }

         this.filtersDirty = false;
         this.lastController = control;
         this.lastRevision = revision;
      }
   }

   protected WarehouseControlBlockEntity findController() {
      return this.membership.findController(this);
   }

   protected void syncChanged() {
      this.m_6596_();
      if (this.f_58857_ != null) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      this.saveFilters(tag);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.loadFilters(tag);
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

   protected void saveFilters(CompoundTag tag) {
      this.membership.save(tag);
      tag.m_128379_("RequireAll", this.requireAll);
      ListTag list = new ListTag();

      for (WarehouseStockFilter filter : this.filters) {
         list.add(filter.save());
      }

      tag.m_128365_("Filters", list);
   }

   protected void loadFilters(CompoundTag tag) {
      this.configurationRevision++;
      this.membership.load(tag);
      this.requireAll = tag.m_128471_("RequireAll");
      this.filtersDirty = true;
      if (tag.m_128441_("Filters")) {
         ListTag list = tag.m_128437_("Filters", 10);

         for (int i = 0; i < this.filters.length && i < list.size(); i++) {
            this.filters[i].load(list.m_128728_(i));
         }
      }
   }

   public Component m_5446_() {
      return Component.m_237115_("container.ancientwarfare.warehouse_stock_viewer");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new WarehouseStockViewerMenu(containerId, playerInventory, this.f_58858_, false);
   }
}
