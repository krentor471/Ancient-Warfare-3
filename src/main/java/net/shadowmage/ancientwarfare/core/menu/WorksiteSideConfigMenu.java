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
      this.level = playerInventory.f_35978_.m_9236_();
      this.worksite = this.level.m_7702_(pos) instanceof SideConfigurableWorksite configurable ? configurable : null;

      for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
         this.previewAccess[side.ordinal()] = this.getSideAccessFromWorksite(side);
      }
   }

   public boolean m_6366_(Player player, int id) {
      if (this.worksite != null && !this.level.f_46443_ && id >= 0 && id < WorksiteRelativeSide.values().length) {
         WorksiteRelativeSide side = WorksiteRelativeSide.values()[id];
         this.worksite.setSideAccess(side, this.worksite.getSideAccess(side).next());
         this.previewAccess[side.ordinal()] = this.worksite.getSideAccess(side);
         if (this.level.m_7702_(this.pos) != null) {
            this.level.m_7702_(this.pos).m_6596_();
         }

         this.m_38946_();
         return true;
      } else {
         return false;
      }
   }

   public WorksiteInventoryAccess getSideAccess(WorksiteRelativeSide side) {
      return this.level.f_46443_ ? this.previewAccess[side.ordinal()] : this.getSideAccessFromWorksite(side);
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

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, player.m_9236_().m_8055_(this.pos).m_60734_());
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }
}
