package net.shadowmage.ancientwarfare.core.structure;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class LargeIslandWorldGenerationData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_large_island_worldgen";
   private LargeIslandWorldGenerationData.Task active;

   public static LargeIslandWorldGenerationData get(ServerLevel level) {
      return (LargeIslandWorldGenerationData)level.getDataStorage()
         .computeIfAbsent(LargeIslandWorldGenerationData::load, LargeIslandWorldGenerationData::new, "ancientwarfare_large_island_worldgen");
   }

   public LargeIslandWorldGenerationData.Task active() {
      return this.active;
   }

   public void setActive(LargeIslandWorldGenerationData.Task active) {
      this.active = active;
      this.setDirty();
   }

   public CompoundTag save(CompoundTag tag) {
      if (this.active != null) {
         CompoundTag value = new CompoundTag();
         value.putString("Path", this.active.path);
         value.putString("Name", this.active.name);
         value.putInt("MinX", this.active.minX);
         value.putInt("MinY", this.active.minY);
         value.putInt("MinZ", this.active.minZ);
         value.putInt("Facing", this.active.facing);
         value.putString("Phase", this.active.phase.name());
         value.putInt("Progress", this.active.progress);
         value.putInt("Layer", this.active.layer);
         value.putInt("CellCursor", this.active.cellCursor);
         value.putInt("EntityCursor", this.active.entityCursor);
         value.putInt("LegacyConfiguredChunks", this.active.legacyConfiguredChunks);
         value.putInt("PlanSizeX", this.active.planSizeX);
         value.putInt("PlanSizeY", this.active.planSizeY);
         value.putInt("PlanSizeZ", this.active.planSizeZ);
         value.putString("LastError", this.active.lastError);
         value.putBoolean("LayeredBuild", this.active.layeredBuild);
         value.putBoolean("ModernSurvey", this.active.modernSurvey);
         value.putLong("SurveySamples", this.active.surveySamples);
         value.putLong("SurveyWaterSamples", this.active.surveyWaterSamples);
         value.putLong("SurveyDepthSamples", this.active.surveyDepthSamples);
         value.putInt("RetryAttempt", this.active.retryAttempt);
         value.putInt("AnchorX", this.active.anchorX);
         value.putInt("AnchorY", this.active.anchorY);
         value.putInt("AnchorZ", this.active.anchorZ);
         value.putLong("RetrySeed", this.active.retrySeed);
         CompoundTag rejected = new CompoundTag();
         int rejectionCount = Math.min(3, this.active.rejectedTemplates.size());
         rejected.putInt("Count", rejectionCount);

         for (int index = 0; index < rejectionCount; index++) {
            rejected.putString("Template" + index, this.active.rejectedTemplates.get(index));
         }

         value.put("RejectedTemplates", rejected);
         tag.put("Active", value);
      }

      return tag;
   }

   private static LargeIslandWorldGenerationData load(CompoundTag tag) {
      LargeIslandWorldGenerationData data = new LargeIslandWorldGenerationData();
      if (tag.contains("Active")) {
         CompoundTag value = tag.getCompound("Active");
         data.active = new LargeIslandWorldGenerationData.Task(
            value.getString("Path"),
            value.getString("Name"),
            value.getInt("MinX"),
            value.getInt("MinY"),
            value.getInt("MinZ"),
            value.getInt("Facing")
         );
         boolean invalidPhase = false;

         try {
            data.active.phase = LargeIslandWorldGenerationData.Phase.valueOf(value.getString("Phase"));
         } catch (IllegalArgumentException var8) {
            data.active.phase = LargeIslandWorldGenerationData.Phase.SURVEY;
            invalidPhase = true;
         }

         data.active.progress = Math.max(0, value.getInt("Progress"));
         data.active.layer = Math.max(0, value.getInt("Layer"));
         data.active.cellCursor = Math.max(0, value.getInt("CellCursor"));
         data.active.entityCursor = Math.max(0, value.getInt("EntityCursor"));
         data.active.legacyConfiguredChunks = Math.max(0, value.getInt("LegacyConfiguredChunks"));
         data.active.planSizeX = Math.max(0, value.getInt("PlanSizeX"));
         data.active.planSizeY = Math.max(0, value.getInt("PlanSizeY"));
         data.active.planSizeZ = Math.max(0, value.getInt("PlanSizeZ"));
         data.active.lastError = value.getString("LastError");
         data.active.layeredBuild = value.contains("LayeredBuild") && value.getBoolean("LayeredBuild");
         data.active.modernSurvey = value.contains("ModernSurvey") && value.getBoolean("ModernSurvey");
         if (invalidPhase) {
            data.active.modernSurvey = false;
            data.active.progress = 0;
            data.active.layer = 0;
            data.active.cellCursor = 0;
            data.active.entityCursor = 0;
         }

         data.active.surveySamples = Math.max(0L, value.getLong("SurveySamples"));
         data.active.surveyWaterSamples = Math.max(0L, value.getLong("SurveyWaterSamples"));
         data.active.surveyDepthSamples = Math.max(0L, value.getLong("SurveyDepthSamples"));
         data.active.retryAttempt = Math.max(0, Math.min(3, value.getInt("RetryAttempt")));
         data.active.anchorX = value.contains("AnchorX") ? value.getInt("AnchorX") : data.active.minX;
         data.active.anchorY = value.contains("AnchorY") ? value.getInt("AnchorY") : data.active.minY;
         data.active.anchorZ = value.contains("AnchorZ") ? value.getInt("AnchorZ") : data.active.minZ;
         data.active.retrySeed = value.getLong("RetrySeed");
         CompoundTag rejected = value.getCompound("RejectedTemplates");
         int rejectionCount = Math.max(0, Math.min(3, rejected.getInt("Count")));

         for (int index = 0; index < rejectionCount; index++) {
            String path = rejected.getString("Template" + index);
            if (!path.isEmpty() && !data.active.rejectedTemplates.contains(path)) {
               data.active.rejectedTemplates.add(path);
            }
         }
      }

      return data;
   }

   public static enum Phase {
      SURVEY,
      PREPARE,
      VALIDATE,
      CLEANUP,
      BUILD,
      CONNECT,
      CONFIGURE,
      FINAL_CONNECT,
      ENTITIES;
   }

   public static final class Task {
      public final String path;
      public final String name;
      public final int minX;
      public final int minY;
      public final int minZ;
      public final int facing;
      public LargeIslandWorldGenerationData.Phase phase = LargeIslandWorldGenerationData.Phase.SURVEY;
      public int progress;
      public int layer;
      public int cellCursor;
      public int entityCursor;
      public int legacyConfiguredChunks;
      public int planSizeX;
      public int planSizeY;
      public int planSizeZ;
      public String lastError = "";
      public boolean layeredBuild = true;
      public boolean modernSurvey = true;
      public long surveySamples;
      public long surveyWaterSamples;
      public long surveyDepthSamples;
      public int retryAttempt;
      public int anchorX;
      public int anchorY;
      public int anchorZ;
      public long retrySeed;
      public final List<String> rejectedTemplates = new ArrayList<>();

      public Task(String path, String name, int minX, int minY, int minZ, int facing) {
         this.path = path;
         this.name = name;
         this.minX = minX;
         this.minY = minY;
         this.minZ = minZ;
         this.facing = facing;
         this.anchorX = minX;
         this.anchorY = minY;
         this.anchorZ = minZ;
      }
   }
}
