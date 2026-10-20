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
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteBoundsGeometry;
import net.shadowmage.ancientwarfare.core.tile.FishFarmBlockEntity;

public class FishFarmBoundsMenu extends AbstractContainerMenu {
   public static final int NORTH = 0;
   public static final int SOUTH = 1;
   public static final int WEST = 2;
   public static final int EAST = 3;
   public static final int X_SIZE_MINUS = 4;
   public static final int X_SIZE_PLUS = 5;
   public static final int Z_SIZE_MINUS = 6;
   public static final int Z_SIZE_PLUS = 7;
   private final BlockPos pos;
   private final Level level;
   private final Player owner;
   private final FishFarmBlockEntity fishFarm;
   private BlockPos previewMin;
   private BlockPos previewMax;

   public FishFarmBoundsMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.FISH_FARM_BOUNDS), containerId);
      this.pos = pos;
      this.level = playerInventory.f_35978_.m_9236_();
      this.owner = playerInventory.f_35978_;
      this.fishFarm = this.level.m_7702_(pos) instanceof FishFarmBlockEntity farm ? farm : null;
      this.previewMin = this.fishFarm == null ? pos : this.fishFarm.getBoundsMin();
      this.previewMax = this.fishFarm == null ? pos : this.fishFarm.getBoundsMax();
   }

   public boolean m_6366_(Player player, int id) {
      if (this.fishFarm != null
         && !this.level.f_46443_
         && player == this.owner
         && player.m_9236_() == this.level
         && !player.m_5833_()
         && player.f_36096_ == this
         && this.m_6875_(player)
         && this.level.m_7702_(this.pos) == this.fishFarm) {
         BlockPos min = this.fishFarm.getBoundsMin();
         BlockPos max = this.fishFarm.getBoundsMax();
         WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjustButton(this.pos, min, max, this.fishFarm.getMaxBoundsWidth(), id);
         if (adjusted == null) {
            return false;
         } else {
            long minXDelta = (long)adjusted.min().m_123341_() - (long)min.m_123341_();
            long maxXDelta = (long)adjusted.max().m_123341_() - (long)max.m_123341_();
            long minZDelta = (long)adjusted.min().m_123343_() - (long)min.m_123343_();
            long maxZDelta = (long)adjusted.max().m_123343_() - (long)max.m_123343_();
            if (minXDelta >= -2147483648L
               && minXDelta <= 2147483647L
               && maxXDelta >= -2147483648L
               && maxXDelta <= 2147483647L
               && minZDelta >= -2147483648L
               && minZDelta <= 2147483647L
               && maxZDelta >= -2147483648L
               && maxZDelta <= 2147483647L) {
               this.fishFarm.adjustBounds((int)minXDelta, (int)maxXDelta, (int)minZDelta, (int)maxZDelta);
               this.previewMin = this.fishFarm.getBoundsMin();
               this.previewMax = this.fishFarm.getBoundsMax();
               this.m_38946_();
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void previewButton(int id) {
      if (this.fishFarm != null) {
         WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjustButton(
            this.pos, this.previewMin, this.previewMax, this.fishFarm.getMaxBoundsWidth(), id
         );
         if (adjusted != null) {
            long minXDelta = (long)adjusted.min().m_123341_() - (long)this.previewMin.m_123341_();
            long maxXDelta = (long)adjusted.max().m_123341_() - (long)this.previewMax.m_123341_();
            long minZDelta = (long)adjusted.min().m_123343_() - (long)this.previewMin.m_123343_();
            long maxZDelta = (long)adjusted.max().m_123343_() - (long)this.previewMax.m_123343_();
            if (minXDelta < -2147483648L
               || minXDelta > 2147483647L
               || maxXDelta < -2147483648L
               || maxXDelta > 2147483647L
               || minZDelta < -2147483648L
               || minZDelta > 2147483647L
               || maxZDelta < -2147483648L
               || maxZDelta > 2147483647L) {
               return;
            }

            this.previewMin = adjusted.min();
            this.previewMax = adjusted.max();
         }
      }
   }

   public BlockPos getBoundsMin() {
      return this.fishFarm == null ? this.pos : this.previewMin;
   }

   public BlockPos getBoundsMax() {
      return this.fishFarm == null ? this.pos : this.previewMax;
   }

   public BlockPos getWorksitePos() {
      return this.pos;
   }

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.FISH_FARM));
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }
}
