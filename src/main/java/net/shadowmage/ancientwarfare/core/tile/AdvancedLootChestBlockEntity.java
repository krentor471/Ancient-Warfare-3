package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class AdvancedLootChestBlockEntity extends ChestBlockEntity {
   public AdvancedLootChestBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.ADVANCED_LOOT_CHEST), pos, state);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      CompoundTag ancientWarfareData = AWCorePlatform.blockEntityData(this);
      if (this.level instanceof ServerLevel) {
         AncientWarfareCore.LOG
            .debug(
               "Advanced loot chest block entity loaded: pos={}, block={}, type={}, items={}, configuredTable={}, rolls={}",
               new Object[]{
                  this.worldPosition,
                  this.getBlockState().getBlock(),
                  this.getClass().getSimpleName(),
                  this.countNonEmpty(),
                  ancientWarfareData.getString("AWStructureLootTable"),
                  ancientWarfareData.getInt("AWStructureLootRolls")
               }
            );
         AncientWarfareCore.LOG
            .debug(
               "Advanced loot chest legacy settings loaded: pos={}, present={}, keys={}",
               new Object[]{
                  this.worldPosition,
                  ancientWarfareData.contains("AWLegacyLootSettings"),
                  ancientWarfareData.contains("AWLegacyLootSettings") ? ancientWarfareData.getCompound("AWLegacyLootSettings").getAllKeys().size() : 0
               }
            );
      }
   }

   private int countNonEmpty() {
      int count = 0;

      for (int slot = 0; slot < this.getContainerSize(); slot++) {
         if (!this.getItem(slot).isEmpty()) {
            count++;
         }
      }

      return count;
   }
}
