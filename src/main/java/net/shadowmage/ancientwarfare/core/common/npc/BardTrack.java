package net.shadowmage.ancientwarfare.core.common.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record BardTrack(String fileName, String displayName, int durationSeconds, int introSilenceSeconds, int outroSilenceSeconds, int loops) {
   public static final int DEFAULT_DURATION_SECONDS = 120;
   public static final int DEFAULT_SILENCE_SECONDS = 2;

   public BardTrack(String fileName, String displayName, int durationSeconds, int introSilenceSeconds, int outroSilenceSeconds, int loops) {
      fileName = clean(fileName);
      displayName = clean(displayName);
      durationSeconds = clamp(durationSeconds, 1, 3600);
      introSilenceSeconds = clamp(introSilenceSeconds, 0, 300);
      outroSilenceSeconds = clamp(outroSilenceSeconds, 0, 300);
      loops = clamp(loops, 1, 99);
      this.fileName = fileName;
      this.displayName = displayName;
      this.durationSeconds = durationSeconds;
      this.introSilenceSeconds = introSilenceSeconds;
      this.outroSilenceSeconds = outroSilenceSeconds;
      this.loops = loops;
   }

   public static BardTrack blank() {
      return new BardTrack("", "", 120, 2, 2, 1);
   }

   public static BardTrack fromNetwork(FriendlyByteBuf buffer) {
      return new BardTrack(buffer.readUtf(128), buffer.readUtf(128), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
   }

   public void toNetwork(FriendlyByteBuf buffer) {
      buffer.writeUtf(this.fileName, 128);
      buffer.writeUtf(this.displayName, 128);
      buffer.writeVarInt(this.durationSeconds);
      buffer.writeVarInt(this.introSilenceSeconds);
      buffer.writeVarInt(this.outroSilenceSeconds);
      buffer.writeVarInt(this.loops);
   }

   public static BardTrack load(CompoundTag tag) {
      return new BardTrack(
         tag.getString("File"),
         tag.getString("Name"),
         tag.contains("Duration") ? tag.getInt("Duration") : 120,
         tag.contains("Intro") ? tag.getInt("Intro") : 2,
         tag.contains("Outro") ? tag.getInt("Outro") : 2,
         tag.contains("Loops") ? tag.getInt("Loops") : 1
      );
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putString("File", this.fileName);
      tag.putString("Name", this.displayName);
      tag.putInt("Duration", this.durationSeconds);
      tag.putInt("Intro", this.introSilenceSeconds);
      tag.putInt("Outro", this.outroSilenceSeconds);
      tag.putInt("Loops", this.loops);
      return tag;
   }

   public String label() {
      return this.displayName.isBlank() ? this.fileName : this.displayName;
   }

   private static String clean(String value) {
      return value == null ? "" : value.trim();
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}
