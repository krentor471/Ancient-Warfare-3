package net.shadowmage.ancientwarfare.core.client;

import java.io.File;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.BardTrack;
import net.shadowmage.ancientwarfare.core.entity.BardNpcEntity;

public final class BardMusicPlaybackManager {
   private static final double ACTIVE_PLAYBACK_RANGE = 96.0;
   private static BardNpcEntity activeBard;
   private static Clip activeClip;
   private static int trackIndex;
   private static int loopIndex;
   private static long nextStartMillis;

   private BardMusicPlaybackManager() {
   }

   public static void clientTick() {
      Minecraft minecraft = Minecraft.m_91087_();
      LocalPlayer player = minecraft.f_91074_;
      if (minecraft.f_91073_ == null || player == null) {
         stop();
      } else if (activeBard != null) {
         tickActiveBard();
      } else {
         BardNpcEntity bard = nearestPlayableBard(player);
         if (bard != null) {
            activeBard = bard;
            trackIndex = 0;
            loopIndex = 0;
            nextStartMillis = System.currentTimeMillis();
            tickActiveBard();
         }
      }
   }

   private static BardNpcEntity nearestPlayableBard(LocalPlayer player) {
      double range = 26.0;
      AABB search = player.m_20191_().m_82400_(range);
      BardNpcEntity best = null;
      double bestDistance = Double.MAX_VALUE;

      for (Entity entity : player.m_9236_().m_45933_(player, search)) {
         if (entity instanceof BardNpcEntity) {
            BardNpcEntity bard = (BardNpcEntity)entity;
            if (!bard.getBardTracks().isEmpty()) {
               double playRange = (double)bard.getBardPlayRange();
               double distance = player.m_20280_(bard);
               if (distance <= playRange * playRange && distance < bestDistance) {
                  best = bard;
                  bestDistance = distance;
               }
            }
         }
      }

      return best;
   }

   private static void tickActiveBard() {
      if (activeBard != null && activeBard.m_6084_() && withinStopRange(activeBard)) {
         List<BardTrack> tracks = activeBard.getBardTracks();
         if (tracks.isEmpty()) {
            stop();
         } else {
            long now = System.currentTimeMillis();
            if (activeClip == null || !activeClip.isRunning()) {
               if (activeClip != null) {
                  activeClip.close();
                  activeClip = null;
                  advanceTrack(tracks.get(trackIndex));
               } else if (now >= nextStartMillis) {
                  BardTrack track = tracks.get(trackIndex % tracks.size());
                  play(track);
               }
            }
         }
      } else {
         stop();
      }
   }

   private static boolean withinStopRange(BardNpcEntity bard) {
      Minecraft minecraft = Minecraft.m_91087_();
      return minecraft.f_91074_ == null ? false : minecraft.f_91074_.m_20280_(bard) <= 9216.0;
   }

   private static void play(BardTrack track) {
      File file = BardMusicLibrary.file(track.fileName());
      if (!file.isFile()) {
         advanceTrack(track);
      } else {
         try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            activeClip = clip;
            clip.start();
            nextStartMillis = System.currentTimeMillis() + (long)track.durationSeconds() * 1000L;
         } catch (Exception var4) {
            activeClip = null;
            advanceTrack(track);
         }
      }
   }

   private static void advanceTrack(BardTrack track) {
      loopIndex++;
      if (loopIndex >= track.loops()) {
         loopIndex = 0;
         trackIndex++;
      }

      if (activeBard != null && !activeBard.getBardTracks().isEmpty()) {
         trackIndex = trackIndex % activeBard.getBardTracks().size();
      }

      nextStartMillis = System.currentTimeMillis() + (long)(track.outroSilenceSeconds() + nextIntroSilence()) * 1000L;
   }

   private static int nextIntroSilence() {
      return activeBard != null && !activeBard.getBardTracks().isEmpty()
         ? activeBard.getBardTracks().get(trackIndex % activeBard.getBardTracks().size()).introSilenceSeconds()
         : 0;
   }

   private static void stop() {
      if (activeClip != null) {
         activeClip.stop();
         activeClip.close();
      }

      activeClip = null;
      activeBard = null;
      trackIndex = 0;
      loopIndex = 0;
      nextStartMillis = 0L;
   }
}
