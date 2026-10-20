package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.CropFarmBlockEntity;

public class CropFarmControlMenu extends AbstractContainerMenu {
   public static final int TOGGLE_AUTO_TILL = 0;
   private final BlockPos pos;
   private final Level level;
   private final CropFarmBlockEntity cropFarm;
   private boolean previewAutoTill;

   public CropFarmControlMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.CROP_FARM_CONTROL), containerId);
      this.pos = pos;
      this.level = playerInventory.f_35978_.m_9236_();
      this.cropFarm = this.level.m_7702_(pos) instanceof CropFarmBlockEntity farm ? farm : null;
      this.previewAutoTill = this.cropFarm != null && this.cropFarm.autoTill();
   }

   public boolean m_6366_(Player player, int id) {
      if (id == 0 && this.cropFarm != null && !this.level.f_46443_) {
         this.cropFarm.toggleAutoTill();
         this.previewAutoTill = this.cropFarm.autoTill();
         return true;
      } else {
         return false;
      }
   }

   public void previewButton(int id) {
      if (id == 0) {
         this.previewAutoTill = !this.previewAutoTill;
      }
   }

   public boolean autoTill() {
      return this.level.f_46443_ ? this.previewAutoTill : this.cropFarm != null && this.cropFarm.autoTill();
   }

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.CROP_FARM));
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }
}
