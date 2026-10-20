package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.AnimalFarmBlockEntity;

public class AnimalFarmControlMenu extends AbstractContainerMenu {
   public static final int ROWS_PER_PAGE = 8;
   public static final int PREV_PAGE = 1000;
   public static final int NEXT_PAGE = 1001;
   private final BlockPos pos;
   private final Level level;
   private final AnimalFarmBlockEntity animalFarm;
   private final int[] previewLimits = new int[AnimalFarmBlockEntity.SUPPORTED_ANIMAL_TYPES.length];
   private int page;

   public AnimalFarmControlMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.ANIMAL_FARM_CONTROL), containerId);
      this.pos = pos;
      this.level = playerInventory.f_35978_.m_9236_();
      this.animalFarm = this.level.m_7702_(pos) instanceof AnimalFarmBlockEntity farm ? farm : null;

      for (int i = 0; i < this.previewLimits.length; i++) {
         this.previewLimits[i] = this.animalFarm == null ? 0 : this.animalFarm.getAnimalLimit(AnimalFarmBlockEntity.SUPPORTED_ANIMAL_TYPES[i]);
      }
   }

   public boolean m_6366_(Player player, int id) {
      if (id == 1000) {
         this.page = Math.max(0, this.page - 1);
         return true;
      } else if (id == 1001) {
         this.page = Math.min(this.getPageCount() - 1, this.page + 1);
         return true;
      } else {
         int visibleRow = id / 2;
         int action = id % 2;
         int index = this.page * 8 + visibleRow;
         if (visibleRow >= 0 && visibleRow < 8 && index >= 0 && index < this.previewLimits.length) {
            int delta = action == 0 ? -1 : 1;
            if (this.level.f_46443_) {
               this.previewLimits[index] = Math.max(0, this.previewLimits[index] + delta);
               return true;
            } else if (this.animalFarm != null) {
               this.animalFarm.adjustAnimalLimit(AnimalFarmBlockEntity.SUPPORTED_ANIMAL_TYPES[index], delta);
               this.previewLimits[index] = this.animalFarm.getAnimalLimit(AnimalFarmBlockEntity.SUPPORTED_ANIMAL_TYPES[index]);
               this.m_38946_();
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public void previewButton(int id) {
      this.m_6366_(null, id);
   }

   public int getPage() {
      return this.page;
   }

   public int getPageCount() {
      return Math.max(1, (AnimalFarmBlockEntity.SUPPORTED_ANIMAL_TYPES.length + 8 - 1) / 8);
   }

   public ResourceLocation getAnimalType(int visibleRow) {
      int index = this.page * 8 + visibleRow;
      return index >= 0 && index < AnimalFarmBlockEntity.SUPPORTED_ANIMAL_TYPES.length ? AnimalFarmBlockEntity.SUPPORTED_ANIMAL_TYPES[index] : null;
   }

   public int getAnimalLimit(int visibleRow) {
      int index = this.page * 8 + visibleRow;
      return index >= 0 && index < this.previewLimits.length ? this.previewLimits[index] : 0;
   }

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.ANIMAL_FARM));
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }
}
