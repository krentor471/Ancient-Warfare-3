package net.shadowmage.ancientwarfare.core.forge;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraftforge.common.ForgeHooks;

public final class ForgeLootOverrideSnapshot {
   private static final Gson LOOT_GSON = Deserializers.createLootTableSerializer().create();
   private final Map<String, byte[]> files;

   private ForgeLootOverrideSnapshot(Map<String, byte[]> files) {
      this.files = Collections.unmodifiableMap(new LinkedHashMap<>(files));
   }

   public static ForgeLootOverrideSnapshot empty() {
      return new ForgeLootOverrideSnapshot(
         Map.of(
            "pack.mcmeta",
            "{\"pack\":{\"pack_format\":15,\"description\":\"Ancient Warfare editable structure loot overrides\"}}".getBytes(StandardCharsets.UTF_8)
         )
      );
   }

   public static ForgeLootOverrideSnapshot read(Path root) throws IOException {
      Map<String, byte[]> files = new LinkedHashMap<>();

      try (Stream<Path> paths = Files.walk(root)) {
         for (Path file : paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).sorted().toList()) {
            String relative = root.relativize(file).toString().replace('\\', '/');
            byte[] bytes = Files.readAllBytes(file);
            if (relative.endsWith(".json") || relative.equals("pack.mcmeta")) {
               validate(relative, bytes);
            }

            files.put(relative, bytes);
         }
      }

      if (!files.containsKey("pack.mcmeta")) {
         throw new IOException("Missing loot override pack metadata: " + root.resolve("pack.mcmeta"));
      } else {
         return new ForgeLootOverrideSnapshot(files);
      }
   }

   private static void validate(String path, byte[] bytes) throws IOException {
      try {
         JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
         if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("JSON root must be an object");
         } else {
            JsonObject json = parsed.getAsJsonObject();
            if (path.equals("pack.mcmeta")) {
               if (!json.has("pack") || !json.get("pack").isJsonObject()) {
                  throw new IllegalArgumentException("pack.mcmeta requires a pack object");
               }

               JsonObject pack = json.getAsJsonObject("pack");
               if (!pack.has("pack_format")
                  || !pack.get("pack_format").isJsonPrimitive()
                  || !pack.getAsJsonPrimitive("pack_format").isNumber()
                  || pack.getAsJsonPrimitive("pack_format").getAsBigDecimal().intValueExact() < 1) {
                  throw new IllegalArgumentException("pack_format must be a positive integer");
               }

               PackMetadataSection.TYPE.fromJson(pack);
            } else {
               String[] parts = path.split("/", 4);
               if (parts.length == 4 && parts[0].equals(PackType.SERVER_DATA.getDirectory()) && parts[2].equals("loot_tables")) {
                  ResourceLocation location = ResourceLocation.tryBuild(parts[1], parts[3].substring(0, parts[3].length() - ".json".length()));
                  if (location == null) {
                     throw new IllegalArgumentException("Invalid loot-table resource path");
                  }

                  if (ForgeHooks.loadLootTable(LOOT_GSON, location, json, true) == null) {
                     throw new IllegalArgumentException("Loot table must not be null");
                  }
               }
            }
         }
      } catch (RuntimeException var6) {
         throw new IOException("Invalid loot override resource " + path + ": " + var6.getMessage(), var6);
      }
   }

   public PackResources packResources(String id) {
      return new ForgeLootOverrideSnapshot.SnapshotResources(id);
   }

   private final class SnapshotResources extends AbstractPackResources {
      private SnapshotResources(String id) {
         super(id, true);
      }

      public IoSupplier<InputStream> getRootResource(String... elements) {
         for (String element : elements) {
            if (element.isEmpty() || element.equals(".") || element.equals("..") || element.indexOf(47) >= 0 || element.indexOf(92) >= 0) {
               return null;
            }
         }

         return this.resource(String.join("/", elements));
      }

      public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
         return this.resource(type.getDirectory() + "/" + location.getNamespace() + "/" + location.getPath());
      }

      public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
         String namespaceRoot = type.getDirectory() + "/" + namespace + "/";
         String prefix = namespaceRoot + (path.isEmpty() ? "" : path + "/");

         for (String name : ForgeLootOverrideSnapshot.this.files.keySet()) {
            if (name.startsWith(prefix)) {
               ResourceLocation location = ResourceLocation.tryBuild(namespace, name.substring(namespaceRoot.length()));
               if (location != null) {
                  output.accept(location, this.resource(name));
               }
            }
         }
      }

      public Set<String> getNamespaces(PackType type) {
         Set<String> namespaces = new LinkedHashSet<>();
         String prefix = type.getDirectory() + "/";

         for (String name : ForgeLootOverrideSnapshot.this.files.keySet()) {
            if (name.startsWith(prefix)) {
               int end = name.indexOf(47, prefix.length());
               if (end > prefix.length()) {
                  String namespace = name.substring(prefix.length(), end);
                  if (ResourceLocation.isValidNamespace(namespace)) {
                     namespaces.add(namespace);
                  }
               }
            }
         }

         return Collections.unmodifiableSet(namespaces);
      }

      private IoSupplier<InputStream> resource(String path) {
         byte[] bytes = ForgeLootOverrideSnapshot.this.files.get(path);
         return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
      }

      public void close() {
      }
   }
}
