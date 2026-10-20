package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
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
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

public class TownHallMenu extends AbstractContainerMenu {
   private static final int TOWN_HALL_SLOTS = 27;
   private final Container container;
   private final BlockPos pos;

   public TownHallMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.TOWN_HALL_MENU), containerId);
      this.pos = pos;
      Level level = playerInventory.f_35978_.m_9236_();
      if (level.m_7702_(pos) instanceof TownHallBlockEntity townHall) {
         this.container = townHall;
      } else {
         this.container = new SimpleContainer(27);
      }

      this.container.m_5856_(playerInventory.f_35978_);
      this.addSlotGrid(this.container, 0, 8, 22, 9, 3);
      this.addPlayerInventory(playerInventory, 8, 92);
   }

   public List<TownHallBlockEntity.NpcDeathEntry> getDeathNotices() {
      return this.container instanceof TownHallBlockEntity townHall ? townHall.getDeathNotices() : List.of();
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
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.TOWN_HALL));
   }

   public ItemStack m_7648_(Player player, int index) {
      if (index >= 0 && index < this.f_38839_.size()) {
         Slot slot = (Slot)this.f_38839_.get(index);
         if (slot != null && slot.m_6657_()) {
            ItemStack stack = slot.m_7993_();
            ItemStack original = stack.m_41777_();
            if (index < 27) {
               if (!this.m_38903_(stack, 27, this.f_38839_.size(), true)) {
                  return ItemStack.f_41583_;
               }
            } else if (!this.m_38903_(stack, 0, 27, false)) {
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
