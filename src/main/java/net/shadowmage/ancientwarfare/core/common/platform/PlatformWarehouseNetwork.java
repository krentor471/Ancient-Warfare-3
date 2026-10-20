package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public interface PlatformWarehouseNetwork {
   void setInterfaceFilter(BlockPos var1, int var2, ItemStack var3, int var4);

   void addInterfaceFilter(BlockPos var1);

   void removeInterfaceFilter(BlockPos var1, int var2);

   void setSearch(String var1);

   default void setScrollRow(int row) {
   }

   default void clickEntry(int containerId, int slot, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
   }

   default void setStorageScrollPixels(int containerId, int pixels) {
   }

   default void clickStorageEntry(int containerId, int entry, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
   }

   default void changeStorageFilter(int containerId, int action, int index, int expectedCount, ItemStack expectedFilter, ItemStack expectedCarried) {
   }

   void setStockFilter(BlockPos var1, int var2, ItemStack var3);
}
