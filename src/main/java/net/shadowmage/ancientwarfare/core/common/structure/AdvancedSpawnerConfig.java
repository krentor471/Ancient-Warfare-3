package net.shadowmage.ancientwarfare.core.common.structure;

public record AdvancedSpawnerConfig(
   String entityId,
   String factionName,
   String entityNbt,
   int minToSpawn,
   int maxToSpawn,
   int spawnDelay,
   int playerRange,
   int mobRange,
   int spawnRange,
   int maxNearby,
   int spawnYOffset,
   boolean lightSensitive,
   boolean redstoneSensitive,
   boolean redstoneMode,
   boolean transparent,
   boolean debugMode
) {
}
