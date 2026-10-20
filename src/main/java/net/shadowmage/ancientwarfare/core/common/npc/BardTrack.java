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
      return new BardTrack(buffer.m_130136_(128), buffer.m_130136_(128), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_());
   }

   public void toNetwork(FriendlyByteBuf buffer) {
      buffer.m_130072_(this.fileName, 128);
      buffer.m_130072_(this.displayName, 128);
      buffer.m_130130_(this.durationSeconds);
      buffer.m_130130_(this.introSilenceSeconds);
      buffer.m_130130_(this.outroSilenceSeconds);
      buffer.m_130130_(this.loops);
   }

   public static BardTrack load(CompoundTag tag) {
      return new BardTrack(
         tag.m_128461_("File"),
         tag.m_128461_("Name"),
         tag.m_128441_("Duration") ? tag.m_128451_("Duration") : 120,
         tag.m_128441_("Intro") ? tag.m_128451_("Intro") : 2,
         tag.m_128441_("Outro") ? tag.m_128451_("Outro") : 2,
         tag.m_128441_("Loops") ? tag.m_128451_("Loops") : 1
      );
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.m_128359_("File", this.fileName);
      tag.m_128359_("Name", this.displayName);
      tag.m_128405_("Duration", this.durationSeconds);
      tag.m_128405_("Intro", this.introSilenceSeconds);
      tag.m_128405_("Outro", this.outroSilenceSeconds);
      tag.m_128405_("Loops", this.loops);
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
