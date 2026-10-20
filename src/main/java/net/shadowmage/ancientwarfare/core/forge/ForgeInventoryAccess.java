package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.core.common.inventory.ItemHandlerAccess;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformInventoryAccess;

public final class ForgeInventoryAccess implements PlatformInventoryAccess {
   @Override
   public ItemHandlerAccess find(BlockEntity blockEntity, Direction side) {
      IItemHandler handler = (IItemHandler)blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
      if (handler == null) {
         return null;
      } else {
         return (ItemHandlerAccess)(handler instanceof ItemHandlerAccess ? (ItemHandlerAccess)handler : new ForgeInventoryAccess.Adapter(handler));
      }
   }

   private static record Adapter(IItemHandler delegate) implements ItemHandlerAccess {
      @Override
      public int getSlots() {
         return this.delegate.getSlots();
      }

      @Override
      public ItemStack getStackInSlot(int slot) {
         return this.delegate.getStackInSlot(slot);
      }

      @Override
      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return this.delegate.insertItem(slot, stack, simulate);
      }

      @Override
      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return this.delegate.extractItem(slot, amount, simulate);
      }

      @Override
      public int getSlotLimit(int slot) {
         return this.delegate.getSlotLimit(slot);
      }

      @Override
      public boolean isItemValid(int slot, ItemStack stack) {
         return this.delegate.isItemValid(slot, stack);
      }
   }
}
