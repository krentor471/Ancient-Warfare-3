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
import net.shadowmage.ancientwarfare.core.tile.QuarryBlockEntity;

public class QuarryControlMenu extends AbstractContainerMenu {
   public static final int TOGGLE_ONLY_ORES = 0;
   private final BlockPos pos;
   private final Level level;
   private final QuarryBlockEntity quarry;
   private boolean previewOnlyOres;

   public QuarryControlMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.QUARRY_CONTROL), containerId);
      this.pos = pos;
      this.level = playerInventory.f_35978_.m_9236_();
      this.quarry = this.level.m_7702_(pos) instanceof QuarryBlockEntity quarryBlockEntity ? quarryBlockEntity : null;
      this.previewOnlyOres = this.quarry != null && this.quarry.onlyMineOres();
   }

   public boolean m_6366_(Player player, int id) {
      if (id == 0 && this.quarry != null && !this.level.f_46443_) {
         this.quarry.toggleOnlyMineOres();
         this.previewOnlyOres = this.quarry.onlyMineOres();
         return true;
      } else {
         return false;
      }
   }

   public void previewButton(int id) {
      if (id == 0) {
         this.previewOnlyOres = !this.previewOnlyOres;
      }
   }

   public boolean onlyMineOres() {
      return this.level.f_46443_ ? this.previewOnlyOres : this.quarry != null && this.quarry.onlyMineOres();
   }

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.QUARRY));
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }
}
