package net.shadowmage.ancientwarfare.core.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class AwtWorldGenerationData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_awt_worldgen";
   private static final int GENERATION_RULE_VERSION = 7;
   private final List<AwtWorldGenerationData.TownTask> towns = new ArrayList<>();
   private final Set<Long> attemptedCandidates = new HashSet<>();

   public static AwtWorldGenerationData get(ServerLevel level) {
      return (AwtWorldGenerationData)level.getDataStorage().computeIfAbsent(AwtWorldGenerationData::load, AwtWorldGenerationData::new, "ancientwarfare_awt_worldgen");
   }

   public List<AwtWorldGenerationData.TownTask> towns() {
      return this.towns;
   }

   public void add(AwtWorldGenerationData.TownTask task) {
      this.towns.add(task);
      this.setDirty();
   }

   public boolean markCandidateAttempted(int chunkX, int chunkZ) {
      boolean added = this.attemptedCandidates.add(candidateKey(chunkX, chunkZ));
      if (added) {
         this.setDirty();
      }

      return added;
   }

   public boolean wasCandidateAttempted(int chunkX, int chunkZ) {
      return this.attemptedCandidates.contains(candidateKey(chunkX, chunkZ));
   }

   public boolean removeFailed() {
      boolean removed = this.towns.removeIf(task -> task.phase == AwtWorldGenerationData.Phase.FAILED);
      if (removed) {
         this.setDirty();
      }

      return removed;
   }

   public AwtWorldGenerationData.TownTask active() {
      return this.towns
         .stream()
         .filter(task -> task.phase != AwtWorldGenerationData.Phase.COMPLETE && task.phase != AwtWorldGenerationData.Phase.FAILED)
         .findFirst()
         .orElse(null);
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();

      for (AwtWorldGenerationData.TownTask town : this.towns) {
         if (town.phase != AwtWorldGenerationData.Phase.FAILED) {
            CompoundTag value = new CompoundTag();
            value.putString("Path", town.path);
            value.putString("Name", town.name);
            value.putInt("X", town.x);
            value.putInt("Y", town.y);
            value.putInt("Z", town.z);
            value.putString("Phase", town.phase.name());
            value.putInt("Progress", town.progress);
            value.putInt("MinSurface", town.minSurface);
            value.putInt("MaxSurface", town.maxSurface);
            value.putIntArray("SurfaceSamples", town.surfaceSamples);
            value.putInt("Settlement", town.settlement);
            value.putInt("Water", town.water);
            if (town.heights != null) {
               value.putIntArray("Heights", town.heights);
            }
            list.add(value);
         }
      }

      tag.put("Towns", list);
      tag.putInt("GenerationRuleVersion", 7);
      tag.putLongArray("AttemptedCandidates", this.attemptedCandidates.stream().mapToLong(Long::longValue).toArray());
      return tag;
   }

   private static AwtWorldGenerationData load(CompoundTag tag) {
      AwtWorldGenerationData data = new AwtWorldGenerationData();

      for (Tag raw : tag.getList("Towns", 10)) {
         CompoundTag value = (CompoundTag)raw;
         AwtWorldGenerationData.TownTask town = new AwtWorldGenerationData.TownTask(
            value.getString("Path"), value.getString("Name"), value.getInt("X"), value.getInt("Y"), value.getInt("Z")
         );

         try {
            town.phase = AwtWorldGenerationData.Phase.valueOf(value.getString("Phase"));
         } catch (IllegalArgumentException var10) {
            town.phase = AwtWorldGenerationData.Phase.SURVEY;
         }

         town.progress = value.getInt("Progress");
         town.settlement = value.getInt("Settlement");
         town.water = value.getInt("Water");
         town.minSurface = value.getInt("MinSurface");
         town.maxSurface = value.getInt("MaxSurface");
         town.heights = value.contains("Heights") ? value.getIntArray("Heights") : null;

         for (int sample : value.getIntArray("SurfaceSamples")) {
            town.surfaceSamples.add(sample);
         }

         if (town.phase == AwtWorldGenerationData.Phase.SURVEY && town.progress > town.surfaceSamples.size()) {
            town.progress = 0;
            town.water = 0;
            town.minSurface = Integer.MAX_VALUE;
            town.maxSurface = Integer.MIN_VALUE;
         }

         if (town.phase != AwtWorldGenerationData.Phase.FAILED) {
            data.towns.add(town);
         }
      }

      if (tag.getInt("GenerationRuleVersion") == 7) {
         for (long candidate : tag.getLongArray("AttemptedCandidates")) {
            data.attemptedCandidates.add(candidate);
         }
      }

      return data;
   }

   private static long candidateKey(int x, int z) {
      return (long)x << 32 ^ (long)z & 4294967295L;
   }

   public static enum Phase {
      SURVEY,
      CLEAR,
      ROADS,
      WALLS,
      BUILDINGS,
      COMPLETE,
      FAILED;
   }

   public static final class TownTask {
      public final String path;
      public final String name;
      public final int x;
      public int y;
      public final int z;
      public AwtWorldGenerationData.Phase phase = AwtWorldGenerationData.Phase.SURVEY;
      public int progress;
      public int minSurface = Integer.MAX_VALUE;
      public int maxSurface = Integer.MIN_VALUE;
      public final List<Integer> surfaceSamples = new ArrayList<>();
      /** The faction settlement this town is built for, still on paper until it completes; 0 for none. */
      public int settlement;
      /**
       * The land's height on the survey grid, row by row; the town follows it instead of standing on
       * one levelled square. Null for a town begun before towns followed the land.
       */
      public int[] heights;
      /** Survey samples so far that found water on top. */
      public int water;

      public TownTask(String path, String name, int x, int y, int z) {
         this.path = path;
         this.name = name;
         this.x = x;
         this.y = y;
         this.z = z;
      }

      public double distanceSquared(int px, int pz) {
         double dx = (double)(this.x - px);
         double dz = (double)(this.z - pz);
         return dx * dx + dz * dz;
      }
   }
}
