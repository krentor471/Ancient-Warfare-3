package net.shadowmage.ancientwarfare.core.client;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class BardMusicLibrary {
   private static final String MUSIC_FOLDER = "ancientwarfare_bard_music";
   private static final List<String> EXTENSIONS = List.of(".mp3", ".ogg", ".wav");

   private BardMusicLibrary() {
   }

   public static File musicFolder() {
      File folder = new File(Minecraft.getInstance().gameDirectory, "ancientwarfare_bard_music");
      if (!folder.exists()) {
         folder.mkdirs();
      }

      return folder;
   }

   public static List<String> availableTracks() {
      File[] files = musicFolder().listFiles(file -> file.isFile() && isSupported(file.getName()));
      return files == null ? List.of() : Arrays.stream(files).map(File::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
   }

   public static File file(String fileName) {
      String safeName = fileName == null ? "" : fileName.replace("\\", "").replace("/", "");
      return new File(musicFolder(), safeName);
   }

   private static boolean isSupported(String name) {
      String lower = name.toLowerCase(Locale.ROOT);
      return EXTENSIONS.stream().anyMatch(lower::endsWith);
   }
}
