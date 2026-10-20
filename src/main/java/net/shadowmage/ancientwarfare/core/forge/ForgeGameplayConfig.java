package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformGameplayConfig;
import net.shadowmage.ancientwarfare.core.forge.config.AWLootConfig;
import net.shadowmage.ancientwarfare.core.forge.config.AWNpcConfig;
import net.shadowmage.ancientwarfare.core.forge.config.AWStructureConfig;

public final class ForgeGameplayConfig implements PlatformGameplayConfig {
   @Override
   public double playerSoldierEquipmentDropChance() {
      return AWNpcConfig.playerSoldierEquipmentDropChance();
   }

   @Override
   public int effectiveLootRolls(ResourceLocation table, int baseRolls, long seed) {
      return AWLootConfig.effectiveRolls(table, baseRolls, seed);
   }

   @Override
   public double lootItemCountMultiplier() {
      return AWLootConfig.itemCountMultiplier();
   }

   @Override
   public boolean preventDuplicateResearchNotes() {
      return AWLootConfig.preventDuplicateResearchNotes();
   }

   @Override
   public boolean fillUnconfiguredStructureContainers() {
      return AWLootConfig.fillUnconfiguredStructureContainers();
   }

   @Override
   public boolean awsEnabled() {
      return AWStructureConfig.awsEnabled();
   }

   @Override
   public boolean largeAquaticAwsEnabled() {
      return AWStructureConfig.largeAquaticAwsEnabled();
   }

   @Override
   public int largeIslandBlocksPerTick() {
      return AWStructureConfig.largeIslandBlocksPerTick();
   }

   @Override
   public int largeIslandTickBudgetMillis() {
      return AWStructureConfig.largeIslandTickBudgetMillis();
   }

   @Override
   public int largeIslandChunkWindowSize() {
      return AWStructureConfig.largeIslandChunkWindowSize();
   }

   @Override
   public int awsRegionChunks() {
      return AWStructureConfig.awsRegionChunks();
   }

   @Override
   public int awsSpawnProtectionDistance() {
      return AWStructureConfig.awsSpawnProtectionDistance();
   }

   @Override
   public int awsMinimumDistance() {
      return AWStructureConfig.awsMinimumDistance();
   }

   @Override
   public int awsAquaticMinimumDistance() {
      return AWStructureConfig.awsAquaticMinimumDistance();
   }

   @Override
   public boolean awtEnabled() {
      return AWStructureConfig.awtEnabled();
   }

   @Override
   public int awtRegionChunks() {
      return AWStructureConfig.awtRegionChunks();
   }

   @Override
   public int awtSpawnProtectionDistance() {
      return AWStructureConfig.awtSpawnProtectionDistance();
   }

   @Override
   public int awtMinimumDistance() {
      return AWStructureConfig.awtMinimumDistance();
   }
}
