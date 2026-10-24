package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.resources.ResourceLocation;

public interface PlatformGameplayConfig {
   default double playerSoldierEquipmentDropChance() {
      return -1.0;
   }

   int effectiveLootRolls(ResourceLocation var1, int var2, long var3);

   double lootItemCountMultiplier();

   boolean preventDuplicateResearchNotes();

   boolean fillUnconfiguredStructureContainers();

   boolean awsEnabled();

   boolean largeAquaticAwsEnabled();

   default int largeIslandBlocksPerTick() {
      return 1024;
   }

   default int largeIslandTickBudgetMillis() {
      return 3;
   }

   default int largeIslandChunkWindowSize() {
      return 9;
   }

   int awsRegionChunks();

   int awsSpawnProtectionDistance();

   int awsMinimumDistance();

   int awsAquaticMinimumDistance();

   boolean awtEnabled();

   int awtRegionChunks();

   int awtSpawnProtectionDistance();

   int awtMinimumDistance();
}
