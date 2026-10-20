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
import net.shadowmage.ancientwarfare.core.common.structure.AdvancedSpawnerConfig;
import net.shadowmage.ancientwarfare.core.tile.AdvancedSpawnerBlockEntity;

public class AdvancedSpawnerMenu extends AbstractContainerMenu {
   private final BlockPos pos;
   private final AdvancedSpawnerBlockEntity spawner;

   public AdvancedSpawnerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.ADVANCED_SPAWNER), containerId);
      this.pos = pos;
      Level level = playerInventory.player.level();
      this.spawner = level.getBlockEntity(pos) instanceof AdvancedSpawnerBlockEntity advancedSpawner ? advancedSpawner : null;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public AdvancedSpawnerConfig getConfig() {
      return this.spawner == null
         ? new AdvancedSpawnerConfig("", "", "", 1, 1, 200, 16, 4, 4, 8, 0, false, false, true, false, false)
         : this.spawner.getConfig();
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.ADVANCED_SPAWNER));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }
}
