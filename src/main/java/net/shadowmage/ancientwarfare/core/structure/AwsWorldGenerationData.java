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
      return (AwsWorldGenerationData)level.getDataStorage().computeIfAbsent(AwsWorldGenerationData::load, AwsWorldGenerationData::new, "ancientwarfare_aws_worldgen");
   }

   public List<AwsWorldGenerationData.Entry> entries() {
      return this.entries;
   }

   public void add(AwsWorldGenerationData.Entry entry) {
      this.entries.add(entry);
      this.setDirty();
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();

      for (AwsWorldGenerationData.Entry entry : this.entries) {
         CompoundTag value = new CompoundTag();
         value.putString("Template", entry.template());
         value.putInt("MinX", entry.minX());
         value.putInt("MinY", entry.minY());
         value.putInt("MinZ", entry.minZ());
         value.putInt("MaxX", entry.maxX());
         value.putInt("MaxY", entry.maxY());
         value.putInt("MaxZ", entry.maxZ());
         value.putInt("Facing", entry.facing());
         list.add(value);
      }

      tag.put("Structures", list);
      return tag;
   }

   private static AwsWorldGenerationData load(CompoundTag tag) {
      AwsWorldGenerationData data = new AwsWorldGenerationData();

      for (Tag raw : tag.getList("Structures", 10)) {
         CompoundTag value = (CompoundTag)raw;
         data.entries
            .add(
               new AwsWorldGenerationData.Entry(
                  value.getString("Template"),
                  value.getInt("MinX"),
                  value.getInt("MinY"),
                  value.getInt("MinZ"),
                  value.getInt("MaxX"),
                  value.getInt("MaxY"),
                  value.getInt("MaxZ"),
                  value.contains("Facing") ? value.getInt("Facing") : 2
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
