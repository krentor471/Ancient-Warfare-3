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
      return (LargeIslandWorldGenerationData)level.m_8895_()
         .m_164861_(LargeIslandWorldGenerationData::load, LargeIslandWorldGenerationData::new, "ancientwarfare_large_island_worldgen");
   }

   public LargeIslandWorldGenerationData.Task active() {
      return this.active;
   }

   public void setActive(LargeIslandWorldGenerationData.Task active) {
      this.active = active;
      this.m_77762_();
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      if (this.active != null) {
         CompoundTag value = new CompoundTag();
         value.m_128359_("Path", this.active.path);
         value.m_128359_("Name", this.active.name);
         value.m_128405_("MinX", this.active.minX);
         value.m_128405_("MinY", this.active.minY);
         value.m_128405_("MinZ", this.active.minZ);
         value.m_128405_("Facing", this.active.facing);
         value.m_128359_("Phase", this.active.phase.name());
         value.m_128405_("Progress", this.active.progress);
         value.m_128405_("Layer", this.active.layer);
         value.m_128405_("CellCursor", this.active.cellCursor);
         value.m_128405_("EntityCursor", this.active.entityCursor);
         value.m_128405_("LegacyConfiguredChunks", this.active.legacyConfiguredChunks);
         value.m_128405_("PlanSizeX", this.active.planSizeX);
         value.m_128405_("PlanSizeY", this.active.planSizeY);
         value.m_128405_("PlanSizeZ", this.active.planSizeZ);
         value.m_128359_("LastError", this.active.lastError);
         value.m_128379_("LayeredBuild", this.active.layeredBuild);
         value.m_128379_("ModernSurvey", this.active.modernSurvey);
         value.m_128356_("SurveySamples", this.active.surveySamples);
         value.m_128356_("SurveyWaterSamples", this.active.surveyWaterSamples);
         value.m_128356_("SurveyDepthSamples", this.active.surveyDepthSamples);
         value.m_128405_("RetryAttempt", this.active.retryAttempt);
         value.m_128405_("AnchorX", this.active.anchorX);
         value.m_128405_("AnchorY", this.active.anchorY);
         value.m_128405_("AnchorZ", this.active.anchorZ);
         value.m_128356_("RetrySeed", this.active.retrySeed);
         CompoundTag rejected = new CompoundTag();
         int rejectionCount = Math.min(3, this.active.rejectedTemplates.size());
         rejected.m_128405_("Count", rejectionCount);

         for (int index = 0; index < rejectionCount; index++) {
            rejected.m_128359_("Template" + index, this.active.rejectedTemplates.get(index));
         }

         value.m_128365_("RejectedTemplates", rejected);
         tag.m_128365_("Active", value);
      }

      return tag;
   }

   private static LargeIslandWorldGenerationData load(CompoundTag tag) {
      LargeIslandWorldGenerationData data = new LargeIslandWorldGenerationData();
      if (tag.m_128441_("Active")) {
         CompoundTag value = tag.m_128469_("Active");
         data.active = new LargeIslandWorldGenerationData.Task(
            value.m_128461_("Path"),
            value.m_128461_("Name"),
            value.m_128451_("MinX"),
            value.m_128451_("MinY"),
            value.m_128451_("MinZ"),
            value.m_128451_("Facing")
         );
         boolean invalidPhase = false;

         try {
            data.active.phase = LargeIslandWorldGenerationData.Phase.valueOf(value.m_128461_("Phase"));
         } catch (IllegalArgumentException var8) {
            data.active.phase = LargeIslandWorldGenerationData.Phase.SURVEY;
            invalidPhase = true;
         }

         data.active.progress = Math.max(0, value.m_128451_("Progress"));
         data.active.layer = Math.max(0, value.m_128451_("Layer"));
         data.active.cellCursor = Math.max(0, value.m_128451_("CellCursor"));
         data.active.entityCursor = Math.max(0, value.m_128451_("EntityCursor"));
         data.active.legacyConfiguredChunks = Math.max(0, value.m_128451_("LegacyConfiguredChunks"));
         data.active.planSizeX = Math.max(0, value.m_128451_("PlanSizeX"));
         data.active.planSizeY = Math.max(0, value.m_128451_("PlanSizeY"));
         data.active.planSizeZ = Math.max(0, value.m_128451_("PlanSizeZ"));
         data.active.lastError = value.m_128461_("LastError");
         data.active.layeredBuild = value.m_128441_("LayeredBuild") && value.m_128471_("LayeredBuild");
         data.active.modernSurvey = value.m_128441_("ModernSurvey") && value.m_128471_("ModernSurvey");
         if (invalidPhase) {
            data.active.modernSurvey = false;
            data.active.progress = 0;
            data.active.layer = 0;
            data.active.cellCursor = 0;
            data.active.entityCursor = 0;
         }

         data.active.surveySamples = Math.max(0L, value.m_128454_("SurveySamples"));
         data.active.surveyWaterSamples = Math.max(0L, value.m_128454_("SurveyWaterSamples"));
         data.active.surveyDepthSamples = Math.max(0L, value.m_128454_("SurveyDepthSamples"));
         data.active.retryAttempt = Math.max(0, Math.min(3, value.m_128451_("RetryAttempt")));
         data.active.anchorX = value.m_128441_("AnchorX") ? value.m_128451_("AnchorX") : data.active.minX;
         data.active.anchorY = value.m_128441_("AnchorY") ? value.m_128451_("AnchorY") : data.active.minY;
         data.active.anchorZ = value.m_128441_("AnchorZ") ? value.m_128451_("AnchorZ") : data.active.minZ;
         data.active.retrySeed = value.m_128454_("RetrySeed");
         CompoundTag rejected = value.m_128469_("RejectedTemplates");
         int rejectionCount = Math.max(0, Math.min(3, rejected.m_128451_("Count")));

         for (int index = 0; index < rejectionCount; index++) {
            String path = rejected.m_128461_("Template" + index);
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
