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
            if (!filter.item().isEmpty()) {
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
      if (this.level != null && !this.level.isClientSide) {
         BlockState state = this.getBlockState();
         if (state.hasProperty(WarehouseStockLinkerBlock.LIT) && (Boolean)state.getValue(WarehouseStockLinkerBlock.LIT) != this.isActive()) {
            this.level.setBlock(this.worldPosition, (BlockState)state.setValue(WarehouseStockLinkerBlock.LIT, this.isActive()), 3);
            this.level.updateNeighborsAt(this.worldPosition, state.getBlock());
         }
      }
   }

   @Nullable
   @Override
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new WarehouseStockViewerMenu(containerId, playerInventory, this.worldPosition, true);
   }
}
