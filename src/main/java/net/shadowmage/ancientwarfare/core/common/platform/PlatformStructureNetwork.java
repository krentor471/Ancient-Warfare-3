package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.core.common.structure.AdvancedSpawnerConfig;

public interface PlatformStructureNetwork {
   void saveScan(BlockPos var1, String var2);

   void manageTemplate(BlockPos var1, String var2, boolean var3);

   void requestDraftingPreview(int var1, int var2, int var3);

   void setAdvancedSpawnerConfig(BlockPos var1, AdvancedSpawnerConfig var2);
}
