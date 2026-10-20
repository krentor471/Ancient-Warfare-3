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

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      CompoundTag ancientWarfareData = AWCorePlatform.blockEntityData(this);
      if (this.f_58857_ instanceof ServerLevel) {
         AncientWarfareCore.LOG
            .debug(
               "Advanced loot chest block entity loaded: pos={}, block={}, type={}, items={}, configuredTable={}, rolls={}",
               new Object[]{
                  this.f_58858_,
                  this.m_58900_().m_60734_(),
                  this.getClass().getSimpleName(),
                  this.countNonEmpty(),
                  ancientWarfareData.m_128461_("AWStructureLootTable"),
                  ancientWarfareData.m_128451_("AWStructureLootRolls")
               }
            );
         AncientWarfareCore.LOG
            .debug(
               "Advanced loot chest legacy settings loaded: pos={}, present={}, keys={}",
               new Object[]{
                  this.f_58858_,
                  ancientWarfareData.m_128441_("AWLegacyLootSettings"),
                  ancientWarfareData.m_128441_("AWLegacyLootSettings") ? ancientWarfareData.m_128469_("AWLegacyLootSettings").m_128431_().size() : 0
               }
            );
      }
   }

   private int countNonEmpty() {
      int count = 0;

      for (int slot = 0; slot < this.m_6643_(); slot++) {
         if (!this.m_8020_(slot).m_41619_()) {
            count++;
         }
      }

      return count;
   }
}
