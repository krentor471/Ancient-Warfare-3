package net.shadowmage.ancientwarfare.core.structure;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class AwsWorldGenerationData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_aws_worldgen";
   private final List<AwsWorldGenerationData.Entry> entries = new ArrayList<>();

   public static AwsWorldGenerationData get(ServerLevel level) {
      return (AwsWorldGenerationData)level.m_8895_().m_164861_(AwsWorldGenerationData::load, AwsWorldGenerationData::new, "ancientwarfare_aws_worldgen");
   }

   public List<AwsWorldGenerationData.Entry> entries() {
      return this.entries;
   }

   public void add(AwsWorldGenerationData.Entry entry) {
      this.entries.add(entry);
      this.m_77762_();
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag list = new ListTag();

      for (AwsWorldGenerationData.Entry entry : this.entries) {
         CompoundTag value = new CompoundTag();
         value.m_128359_("Template", entry.template());
         value.m_128405_("MinX", entry.minX());
         value.m_128405_("MinY", entry.minY());
         value.m_128405_("MinZ", entry.minZ());
         value.m_128405_("MaxX", entry.maxX());
         value.m_128405_("MaxY", entry.maxY());
         value.m_128405_("MaxZ", entry.maxZ());
         value.m_128405_("Facing", entry.facing());
         list.add(value);
      }

      tag.m_128365_("Structures", list);
      return tag;
   }

   private static AwsWorldGenerationData load(CompoundTag tag) {
      AwsWorldGenerationData data = new AwsWorldGenerationData();

      for (Tag raw : tag.m_128437_("Structures", 10)) {
         CompoundTag value = (CompoundTag)raw;
         data.entries
            .add(
               new AwsWorldGenerationData.Entry(
                  value.m_128461_("Template"),
                  value.m_128451_("MinX"),
                  value.m_128451_("MinY"),
                  value.m_128451_("MinZ"),
                  value.m_128451_("MaxX"),
                  value.m_128451_("MaxY"),
                  value.m_128451_("MaxZ"),
                  value.m_128441_("Facing") ? value.m_128451_("Facing") : 2
               )
            );
      }

      return data;
   }

   public static record Entry(String template, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int facing) {
      public boolean intersects(int otherMinX, int otherMinZ, int otherMaxX, int otherMaxZ, int border) {
         return this.minX - border <= otherMaxX && this.maxX + border >= otherMinX && this.minZ - border <= otherMaxZ && this.maxZ + border >= otherMinZ;
      }

      public double horizontalDistanceSquared(int x, int z) {
         double centerX = (double)(this.minX + this.maxX) * 0.5;
         double centerZ = (double)(this.minZ + this.maxZ) * 0.5;
         double dx = centerX - (double)x;
         double dz = centerZ - (double)z;
         return dx * dx + dz * dz;
      }
   }
}
