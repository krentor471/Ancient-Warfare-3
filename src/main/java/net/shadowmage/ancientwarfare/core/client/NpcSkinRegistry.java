package net.shadowmage.ancientwarfare.core.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.shadowmage.ancientwarfare.core.entity.NpcBaseEntity;

public final class NpcSkinRegistry {
   private static final ResourceLocation STEVE = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
   private static final ResourceLocation SKIN_META = new ResourceLocation("ancientwarfare", "skin_pack/skin_pack.meta");
   private static final String SKIN_TEXTURE_PATH = "textures/entity/npc/skin_pack/";
   private static final Map<String, ResourceLocation> SKINS = new LinkedHashMap<>();
   private static boolean loaded;

   private NpcSkinRegistry() {
   }

   public static ResourceLocation textureFor(NpcBaseEntity entity) {
      load();
      ResourceLocation selected = textureForKey(entity.getSkinName());
      if (selected != null) {
         return selected;
      } else {
         ResourceLocation typed = textureForKey(defaultKey(entity));
         if (typed != null) {
            return typed;
         } else {
            ResourceLocation base = textureForKey(entity.getNpcType());
            return base == null ? STEVE : base;
         }
      }
   }

   public static boolean hasSkin(String skinName) {
      load();
      return textureForKey(skinName) != null;
   }

   public static ResourceLocation textureForName(String skinName) {
      load();
      return textureForKey(skinName);
   }

   public static List<String> skinNames() {
      load();
      return List.copyOf(SKINS.keySet());
   }

   private static ResourceLocation textureForKey(String skinName) {
      return skinName != null && !skinName.isBlank() ? SKINS.get(skinName.trim()) : null;
   }

   private static String defaultKey(NpcBaseEntity entity) {
      String subtype = entity.getNpcSubtype();
      return subtype != null && !subtype.isBlank() ? entity.getNpcType() + "." + subtype : entity.getNpcType();
   }

   private static void load() {
      if (!loaded) {
         loaded = true;
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft != null && minecraft.getResourceManager() != null) {
            try {
               Resource resource = (Resource)minecraft.getResourceManager().getResource(SKIN_META).orElse(null);
               if (resource == null) {
                  return;
               }

               String line;
               try (BufferedReader reader = resource.openAsReader()) {
                  while ((line = reader.readLine()) != null) {
                     addSkin(line);
                  }
               }

               addLooseSkinFiles(minecraft);
            } catch (IOException var7) {
               SKINS.clear();
            }
         }
      }
   }

   private static void addSkin(String line) {
      String trimmed = line.trim();
      if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
         int separator = trimmed.indexOf(61);
         if (separator > 0 && separator != trimmed.length() - 1) {
            String key = trimmed.substring(0, separator).trim();
            String fileName = trimmed.substring(separator + 1).trim();
            if (!key.isEmpty() && !fileName.isEmpty()) {
               String texturePath = "textures/entity/npc/skin_pack/" + fileName;
               ResourceLocation texture = ResourceLocation.tryParse("ancientwarfare:" + texturePath);
               if (texture != null) {
                  Minecraft minecraft = Minecraft.getInstance();
                  if (minecraft != null && minecraft.getResourceManager().getResource(texture).isPresent()) {
                     SKINS.putIfAbsent(key, texture);
                  }
               }
            }
         }
      }
   }

   private static void addLooseSkinFiles(Minecraft minecraft) {
      minecraft.getResourceManager()
         .listResources(
            "textures/entity/npc/skin_pack/".substring(0, "textures/entity/npc/skin_pack/".length() - 1), resource -> resource.getPath().endsWith(".png")
         )
         .keySet()
         .stream()
         .sorted(Comparator.comparing(ResourceLocation::getPath))
         .forEach(texture -> {
            String path = texture.getPath();
            int fileNameStart = path.lastIndexOf(47) + 1;
            String fileName = path.substring(fileNameStart);
            String skinName = fileName.substring(0, fileName.length() - ".png".length());
            SKINS.putIfAbsent(skinName, texture);
         });
   }
}
