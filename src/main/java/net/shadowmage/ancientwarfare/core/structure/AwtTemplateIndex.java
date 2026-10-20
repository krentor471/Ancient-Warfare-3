package net.shadowmage.ancientwarfare.core.structure;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public final class AwtTemplateIndex {
   private static final String RESOURCE_ROOT = "assets/ancientwarfare/template/";
   private static final String INDEX_RESOURCE = "/assets/ancientwarfare/town_template_index.txt";
   private static List<String> templates;

   private AwtTemplateIndex() {
   }

   public static List<String> allTemplates() {
      if (templates == null) {
         templates = Collections.unmodifiableList(scanTemplates());
      }

      return templates;
   }

   public static String defaultTemplate() {
      List<String> all = allTemplates();
      return all.isEmpty() ? "" : all.get(0);
   }

   private static List<String> scanTemplates() {
      List<String> indexed = readIndex();
      if (!indexed.isEmpty()) {
         return indexed;
      } else {
         List<String> result = new ArrayList<>();

         try {
            Enumeration<URL> roots = AwtTemplateIndex.class.getClassLoader().getResources("assets/ancientwarfare/template/");

            while (roots.hasMoreElements()) {
               URL root = roots.nextElement();
               if ("file".equals(root.getProtocol())) {
                  scanDirectory(result, root);
               } else if ("jar".equals(root.getProtocol())) {
                  scanJar(result, root);
               }
            }
         } catch (IOException var4) {
            AncientWarfareCore.LOG.warn("Unable to scan AWT templates", var4);
         }

         result.sort(String.CASE_INSENSITIVE_ORDER);
         return result;
      }
   }

   private static List<String> readIndex() {
      try {
         List var2;
         try (InputStream stream = AwtTemplateIndex.class.getResourceAsStream("/assets/ancientwarfare/town_template_index.txt")) {
            if (stream == null) {
               return List.of();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
               var2 = reader.lines()
                  .map(String::trim)
                  .filter(line -> !line.isBlank())
                  .filter(line -> line.toLowerCase(Locale.ROOT).endsWith(".awt"))
                  .sorted(String.CASE_INSENSITIVE_ORDER)
                  .toList();
            }
         }

         return var2;
      } catch (IOException var8) {
         AncientWarfareCore.LOG.warn("Unable to read AWT template index", var8);
         return List.of();
      }
   }

   private static void scanDirectory(List<String> result, URL root) {
      try {
         File directory = new File(root.toURI());
         scanDirectory(result, directory, directory);
      } catch (URISyntaxException var3) {
         AncientWarfareCore.LOG.warn("Unable to scan AWT template directory {}", root, var3);
      }
   }

   private static void scanDirectory(List<String> result, File root, File current) {
      File[] files = current.listFiles();
      if (files != null) {
         for (File file : files) {
            if (file.isDirectory()) {
               scanDirectory(result, root, file);
            } else if (file.getName().toLowerCase(Locale.ROOT).endsWith(".awt")) {
               result.add(root.toPath().relativize(file.toPath()).toString().replace('\\', '/'));
            }
         }
      }
   }

   private static void scanJar(List<String> result, URL root) {
      try {
         JarURLConnection connection = (JarURLConnection)root.openConnection();

         try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
               JarEntry entry = entries.nextElement();
               String name = entry.getName();
               if (!entry.isDirectory() && name.startsWith("assets/ancientwarfare/template/") && name.endsWith(".awt")) {
                  result.add(name.substring("assets/ancientwarfare/template/".length()));
               }
            }
         }
      } catch (IOException var9) {
         AncientWarfareCore.LOG.warn("Unable to scan AWT templates from jar", var9);
      }
   }
}
