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
      return (AwtWorldGenerationData)level.m_8895_().m_164861_(AwtWorldGenerationData::load, AwtWorldGenerationData::new, "ancientwarfare_awt_worldgen");
   }

   public List<AwtWorldGenerationData.TownTask> towns() {
      return this.towns;
   }

   public void add(AwtWorldGenerationData.TownTask task) {
      this.towns.add(task);
      this.m_77762_();
   }

   public boolean markCandidateAttempted(int chunkX, int chunkZ) {
      boolean added = this.attemptedCandidates.add(candidateKey(chunkX, chunkZ));
      if (added) {
         this.m_77762_();
      }

      return added;
   }

   public boolean wasCandidateAttempted(int chunkX, int chunkZ) {
      return this.attemptedCandidates.contains(candidateKey(chunkX, chunkZ));
   }

   public boolean removeFailed() {
      boolean removed = this.towns.removeIf(task -> task.phase == AwtWorldGenerationData.Phase.FAILED);
      if (removed) {
         this.m_77762_();
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

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag list = new ListTag();

      for (AwtWorldGenerationData.TownTask town : this.towns) {
         if (town.phase != AwtWorldGenerationData.Phase.FAILED) {
            CompoundTag value = new CompoundTag();
            value.m_128359_("Path", town.path);
            value.m_128359_("Name", town.name);
            value.m_128405_("X", town.x);
            value.m_128405_("Y", town.y);
            value.m_128405_("Z", town.z);
            value.m_128359_("Phase", town.phase.name());
            value.m_128405_("Progress", town.progress);
            value.m_128405_("MinSurface", town.minSurface);
            value.m_128405_("MaxSurface", town.maxSurface);
            value.m_128408_("SurfaceSamples", town.surfaceSamples);
            list.add(value);
         }
      }

      tag.m_128365_("Towns", list);
      tag.m_128405_("GenerationRuleVersion", 7);
      tag.m_128388_("AttemptedCandidates", this.attemptedCandidates.stream().mapToLong(Long::longValue).toArray());
      return tag;
   }

   private static AwtWorldGenerationData load(CompoundTag tag) {
      AwtWorldGenerationData data = new AwtWorldGenerationData();

      for (Tag raw : tag.m_128437_("Towns", 10)) {
         CompoundTag value = (CompoundTag)raw;
         AwtWorldGenerationData.TownTask town = new AwtWorldGenerationData.TownTask(
            value.m_128461_("Path"), value.m_128461_("Name"), value.m_128451_("X"), value.m_128451_("Y"), value.m_128451_("Z")
         );

         try {
            town.phase = AwtWorldGenerationData.Phase.valueOf(value.m_128461_("Phase"));
         } catch (IllegalArgumentException var10) {
            town.phase = AwtWorldGenerationData.Phase.SURVEY;
         }

         town.progress = value.m_128451_("Progress");
         town.minSurface = value.m_128451_("MinSurface");
         town.maxSurface = value.m_128451_("MaxSurface");

         for (int sample : value.m_128465_("SurfaceSamples")) {
            town.surfaceSamples.add(sample);
         }

         if (town.phase == AwtWorldGenerationData.Phase.SURVEY && town.progress > town.surfaceSamples.size()) {
            town.progress = 0;
            town.minSurface = Integer.MAX_VALUE;
            town.maxSurface = Integer.MIN_VALUE;
         }

         if (town.phase != AwtWorldGenerationData.Phase.FAILED) {
            data.towns.add(town);
         }
      }

      if (tag.m_128451_("GenerationRuleVersion") == 7) {
         for (long candidate : tag.m_128467_("AttemptedCandidates")) {
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
