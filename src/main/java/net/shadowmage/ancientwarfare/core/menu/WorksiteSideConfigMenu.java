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
import net.shadowmage.ancientwarfare.core.common.worksite.SideConfigurableWorksite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteInventoryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteRelativeSide;

public class WorksiteSideConfigMenu extends AbstractContainerMenu {
   private final BlockPos pos;
   private final Level level;
   private final SideConfigurableWorksite worksite;
   private final WorksiteInventoryAccess[] previewAccess = new WorksiteInventoryAccess[WorksiteRelativeSide.values().length];

   public WorksiteSideConfigMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.WORKSITE_SIDE_CONFIG), containerId);
      this.pos = pos;
      this.level = playerInventory.player.level();
      this.worksite = this.level.getBlockEntity(pos) instanceof SideConfigurableWorksite configurable ? configurable : null;

      for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
         this.previewAccess[side.ordinal()] = this.getSideAccessFromWorksite(side);
      }
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.worksite != null && !this.level.isClientSide && id >= 0 && id < WorksiteRelativeSide.values().length) {
         WorksiteRelativeSide side = WorksiteRelativeSide.values()[id];
         this.worksite.setSideAccess(side, this.worksite.getSideAccess(side).next());
         this.previewAccess[side.ordinal()] = this.worksite.getSideAccess(side);
         if (this.level.getBlockEntity(this.pos) != null) {
            this.level.getBlockEntity(this.pos).setChanged();
         }

         this.broadcastChanges();
         return true;
      } else {
         return false;
      }
   }

   public WorksiteInventoryAccess getSideAccess(WorksiteRelativeSide side) {
      return this.level.isClientSide ? this.previewAccess[side.ordinal()] : this.getSideAccessFromWorksite(side);
   }

   public void previewButton(int id) {
      if (id >= 0 && id < this.previewAccess.length) {
         this.previewAccess[id] = this.previewAccess[id].next();
      }
   }

   private WorksiteInventoryAccess getSideAccessFromWorksite(WorksiteRelativeSide side) {
      return this.worksite == null ? WorksiteInventoryAccess.NONE : this.worksite.getSideAccess(side);
   }

   public BlockPos getWorksitePos() {
      return this.pos;
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, player.level().getBlockState(this.pos).getBlock());
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }
}
