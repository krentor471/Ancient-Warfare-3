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
import net.shadowmage.ancientwarfare.core.tile.CropFarmBlockEntity;

public class CropFarmBoundsMenu extends AbstractContainerMenu {
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
   private final CropFarmBlockEntity cropFarm;
   private BlockPos previewMin;
   private BlockPos previewMax;

   public CropFarmBoundsMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.CROP_FARM_BOUNDS), containerId);
      this.pos = pos;
      this.level = playerInventory.player.level();
      this.owner = playerInventory.player;
      this.cropFarm = this.level.getBlockEntity(pos) instanceof CropFarmBlockEntity farm ? farm : null;
      this.previewMin = this.cropFarm == null ? pos : this.cropFarm.getBoundsMin();
      this.previewMax = this.cropFarm == null ? pos : this.cropFarm.getBoundsMax();
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.cropFarm != null
         && !this.level.isClientSide
         && player == this.owner
         && player.level() == this.level
         && !player.isSpectator()
         && player.containerMenu == this
         && this.stillValid(player)
         && this.level.getBlockEntity(this.pos) == this.cropFarm) {
         BlockPos min = this.cropFarm.getBoundsMin();
         BlockPos max = this.cropFarm.getBoundsMax();
         WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjustButton(this.pos, min, max, this.cropFarm.getMaxBoundsWidth(), id);
         if (adjusted == null) {
            return false;
         } else {
            long minXDelta = (long)adjusted.min().getX() - (long)min.getX();
            long maxXDelta = (long)adjusted.max().getX() - (long)max.getX();
            long minZDelta = (long)adjusted.min().getZ() - (long)min.getZ();
            long maxZDelta = (long)adjusted.max().getZ() - (long)max.getZ();
            if (minXDelta >= -2147483648L
               && minXDelta <= 2147483647L
               && maxXDelta >= -2147483648L
               && maxXDelta <= 2147483647L
               && minZDelta >= -2147483648L
               && minZDelta <= 2147483647L
               && maxZDelta >= -2147483648L
               && maxZDelta <= 2147483647L) {
               this.cropFarm.adjustBounds((int)minXDelta, (int)maxXDelta, (int)minZDelta, (int)maxZDelta);
               this.previewMin = this.cropFarm.getBoundsMin();
               this.previewMax = this.cropFarm.getBoundsMax();
               this.broadcastChanges();
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
      if (this.cropFarm != null) {
         WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjustButton(
            this.pos, this.previewMin, this.previewMax, this.cropFarm.getMaxBoundsWidth(), id
         );
         if (adjusted != null) {
            long minXDelta = (long)adjusted.min().getX() - (long)this.previewMin.getX();
            long maxXDelta = (long)adjusted.max().getX() - (long)this.previewMax.getX();
            long minZDelta = (long)adjusted.min().getZ() - (long)this.previewMin.getZ();
            long maxZDelta = (long)adjusted.max().getZ() - (long)this.previewMax.getZ();
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
      return this.cropFarm == null ? this.pos : this.previewMin;
   }

   public BlockPos getBoundsMax() {
      return this.cropFarm == null ? this.pos : this.previewMax;
   }

   public BlockPos getWorksitePos() {
      return this.pos;
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.CROP_FARM));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }
}
