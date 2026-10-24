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
import net.shadowmage.ancientwarfare.core.tile.FishFarmBlockEntity;

public class FishFarmControlMenu extends AbstractContainerMenu {
   public static final int TOGGLE_FISH = 0;
   public static final int TOGGLE_INK = 1;
   private final BlockPos pos;
   private final Level level;
   private final FishFarmBlockEntity fishFarm;
   private boolean previewFish;
   private boolean previewInk;

   public FishFarmControlMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.FISH_FARM_CONTROL), containerId);
      this.pos = pos;
      this.level = playerInventory.player.level();
      this.fishFarm = this.level.getBlockEntity(pos) instanceof FishFarmBlockEntity farm ? farm : null;
      this.previewFish = this.fishFarm == null || this.fishFarm.harvestFish();
      this.previewInk = this.fishFarm == null || this.fishFarm.harvestInk();
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.fishFarm == null || this.level.isClientSide) {
         return false;
      } else if (id == 0) {
         this.fishFarm.toggleHarvestFish();
         this.previewFish = this.fishFarm.harvestFish();
         return true;
      } else if (id == 1) {
         this.fishFarm.toggleHarvestInk();
         this.previewInk = this.fishFarm.harvestInk();
         return true;
      } else {
         return false;
      }
   }

   public void previewButton(int id) {
      if (id == 0) {
         this.previewFish = !this.previewFish;
      } else if (id == 1) {
         this.previewInk = !this.previewInk;
      }
   }

   public boolean harvestFish() {
      return this.level.isClientSide ? this.previewFish : this.fishFarm == null || this.fishFarm.harvestFish();
   }

   public boolean harvestInk() {
      return this.level.isClientSide ? this.previewInk : this.fishFarm == null || this.fishFarm.harvestInk();
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.FISH_FARM));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }
}
