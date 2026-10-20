package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.StructureContainerBlockEntity;

public class StructureContainerMenu extends AbstractContainerMenu {
   private final Container container;
   private final BlockPos pos;
   private final int containerRows;
   private final int containerSlotCount;
   private final int playerStart;

   public StructureContainerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.STRUCTURE_CONTAINER), containerId);
      this.pos = pos;
      Level level = playerInventory.f_35978_.m_9236_();
      BlockEntity blockEntity = level.m_7702_(pos);
      int expectedSize = StructureContainerBlockEntity.containerSize(level.m_8055_(pos));
      if (blockEntity instanceof StructureContainerBlockEntity structureContainer) {
         this.container = structureContainer;
      } else {
         this.container = new SimpleContainer(expectedSize);
      }

      this.containerSlotCount = expectedSize;
      this.playerStart = this.containerSlotCount;
      this.containerRows = Math.max(1, (int)Math.ceil((double)this.containerSlotCount / 9.0));
      this.container.m_5856_(playerInventory.f_35978_);
      this.addSlotGrid(this.container, 0, 8, 18, 9, this.containerRows);
      this.addPlayerInventory(playerInventory, 8, 32 + this.containerRows * 18);
   }

   public int getContainerRows() {
      return this.containerRows;
   }

   private void addSlotGrid(Container container, int firstSlot, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            int index = firstSlot + row * columns + col;
            if (index < container.m_6643_()) {
               this.m_38897_(new Slot(container, index, x + col * 18, y + row * 18));
            }
         }
      }
   }

   private void addPlayerInventory(Inventory playerInventory, int x, int y) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(playerInventory, col + row * 9 + 9, x + col * 18, y + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(playerInventory, col, x + col * 18, y + 58));
      }
   }

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.LOOT_BASKET))
         || m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.URN))
         || m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN))
         || m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN))
         || m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.ADVANCED_LOOT_CHEST));
   }

   public ItemStack m_7648_(Player player, int index) {
      if (index >= 0 && index < this.f_38839_.size()) {
         Slot slot = (Slot)this.f_38839_.get(index);
         if (slot != null && slot.m_6657_()) {
            ItemStack stack = slot.m_7993_();
            ItemStack original = stack.m_41777_();
            if (index < this.containerSlotCount) {
               if (!this.m_38903_(stack, this.playerStart, this.f_38839_.size(), true)) {
                  return ItemStack.f_41583_;
               }
            } else if (!this.m_38903_(stack, 0, this.containerSlotCount, false)) {
               return ItemStack.f_41583_;
            }

            if (stack.m_41619_()) {
               slot.m_269060_(ItemStack.f_41583_);
            } else {
               slot.m_6654_();
            }

            if (stack.m_41613_() == original.m_41613_()) {
               return ItemStack.f_41583_;
            } else {
               slot.m_142406_(player, stack);
               this.m_38946_();
               return original;
            }
         } else {
            return ItemStack.f_41583_;
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      this.container.m_5785_(player);
   }
}
