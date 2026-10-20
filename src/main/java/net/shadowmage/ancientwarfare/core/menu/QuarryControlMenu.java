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
      this.level = playerInventory.player.level();
      this.quarry = this.level.getBlockEntity(pos) instanceof QuarryBlockEntity quarryBlockEntity ? quarryBlockEntity : null;
      this.previewOnlyOres = this.quarry != null && this.quarry.onlyMineOres();
   }

   public boolean clickMenuButton(Player player, int id) {
      if (id == 0 && this.quarry != null && !this.level.isClientSide) {
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
      return this.level.isClientSide ? this.previewOnlyOres : this.quarry != null && this.quarry.onlyMineOres();
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.QUARRY));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }
}
