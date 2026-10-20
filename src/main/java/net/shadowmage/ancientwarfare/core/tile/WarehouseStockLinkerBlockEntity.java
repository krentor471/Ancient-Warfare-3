package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.WarehouseStockLinkerBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseStockFilter;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStockViewerMenu;
import org.jetbrains.annotations.Nullable;

public class WarehouseStockLinkerBlockEntity extends WarehouseStockViewerBlockEntity {
   public WarehouseStockLinkerBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_STOCK_LINKER), pos, state);
   }

   public boolean isActive() {
      if (this.getConnectionStatus() != 1) {
         return false;
      } else {
         boolean configured = false;

         for (WarehouseStockFilter filter : this.filters) {
            if (!filter.item().m_41619_()) {
               configured = true;
               if (this.requiresAll() && !filter.passes()) {
                  return false;
               }

               if (!this.requiresAll() && filter.passes()) {
                  return true;
               }
            }
         }

         return this.requiresAll() && configured;
      }
   }

   @Override
   protected void recount() {
      super.recount();
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         BlockState state = this.m_58900_();
         if (state.m_61138_(WarehouseStockLinkerBlock.LIT) && (Boolean)state.m_61143_(WarehouseStockLinkerBlock.LIT) != this.isActive()) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(WarehouseStockLinkerBlock.LIT, this.isActive()), 3);
            this.f_58857_.m_46672_(this.f_58858_, state.m_60734_());
         }
      }
   }

   @Nullable
   @Override
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new WarehouseStockViewerMenu(containerId, playerInventory, this.f_58858_, true);
   }
}
