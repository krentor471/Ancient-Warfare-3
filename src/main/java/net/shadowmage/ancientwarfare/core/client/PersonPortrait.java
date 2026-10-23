package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.faction.FactionColors;

/**
 * A ruler's or courtier's portrait: the head and shoulders of one of their faction's skins, picked by
 * their looks, greying with age, pale when ill, and crowned when they rule.
 */
final class PersonPortrait {
   private static final String SKINS = "textures/entity/npc/skin_pack";
   private static final Pattern NOBLE_SKIN = Pattern.compile("_(chief|chieftain|lord|warlord|noble|king|priest)(_\\d+)?\\.png$");
   private static final Pattern FEMALE = Pattern.compile("_(woman|princess|queen|druidess|girl|priestess|childbearer|huntress)");
   private static final Pattern NOT_COURT = Pattern.compile("(woman|princess|queen|spirit|totem|animal|creature|childbearer|druidess|child|slave)");
   /** The bust is 16 by 16 skin pixels. */
   static final int SIZE = 16;
   private static List<ResourceLocation> all;
   private static final Map<String, List<ResourceLocation>> POOLS = new HashMap<>();

   private PersonPortrait() {
   }

   /** Which of a faction's skins a portrait is drawn from. */
   static final int COURT = 0;
   static final int NOBLE = 1;
   static final int WOMAN = 2;

   private static List<ResourceLocation> pool(String faction, int kind) {
      return POOLS.computeIfAbsent(faction + "#" + kind, key -> {
         if (all == null) {
            all = new ArrayList<>(Minecraft.getInstance().getResourceManager().listResources(SKINS, id -> id.getNamespace().equals("ancientwarfare") && id.getPath().endsWith(".png")).keySet());
            all.sort(null);
         }

         String prefix = SKINS + "/" + faction + "_";
         List<ResourceLocation> mine = all.stream().filter(id -> id.getPath().startsWith(prefix)).toList();
         List<ResourceLocation> pool = mine.stream()
            .filter(id -> switch (kind) {
               case NOBLE -> NOBLE_SKIN.matcher(id.getPath()).find();
               case WOMAN -> FEMALE.matcher(id.getPath()).find();
               default -> !NOT_COURT.matcher(id.getPath().substring(prefix.length())).find();
            })
            .toList();
         if (pool.isEmpty() && kind == WOMAN) {
            // A faction with no women of its own among the skins borrows from the others.
            pool = all.stream().filter(id -> FEMALE.matcher(id.getPath()).find()).toList();
         }

         return !pool.isEmpty() ? pool : !mine.isEmpty() ? mine : all;
      });
   }

   /**
    * Draws the portrait with its top left at x, y, size pixels across. The frame is the faction's
    * colour; a crown for the ruler, a circlet for the heir.
    */
   static void draw(GuiGraphics graphics, String faction, int look, int age, boolean ill, int kind, int crown, int x, int y, int size) {
      List<ResourceLocation> pool = pool(faction, kind);
      if (pool.isEmpty()) {
         return;
      }

      ResourceLocation skin = pool.get(Math.floorMod(look, pool.size()));
      int color = 0xFF000000 | FactionColors.color(faction);
      graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF0B0907);
      graphics.fill(x, y, x + size, y + size, color);
      graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, shade(color, 0.35F));
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0.0F);
      float scale = size / (float)SIZE;
      graphics.pose().scale(scale, scale, 1.0F);
      RenderSystem.enableBlend();
      // Arms, body and head, each with its outer layer.
      graphics.blit(skin, 0, 11, 4, 5, 44, 20, 4, 5, 64, 64);
      graphics.blit(skin, 12, 11, 4, 5, 36, 52, 4, 5, 64, 64);
      graphics.blit(skin, 4, 11, 8, 5, 20, 20, 8, 5, 64, 64);
      graphics.blit(skin, 0, 11, 4, 5, 44, 36, 4, 5, 64, 64);
      graphics.blit(skin, 12, 11, 4, 5, 52, 52, 4, 5, 64, 64);
      graphics.blit(skin, 4, 11, 8, 5, 20, 36, 8, 5, 64, 64);
      graphics.blit(skin, 4, 3, 8, 8, 8, 8, 8, 8, 64, 64);
      graphics.blit(skin, 4, 3, 8, 8, 40, 8, 8, 8, 64, 64);
      // Grey creeps into the hair, and the old who wear a beard grow it white.
      if (age >= 50) {
         int grey = age >= 70 ? 0xA0D8D8D8 : age >= 60 ? 0x70C8C8C8 : 0x40C0C0C0;
         graphics.fill(4, 3, 12, 5, grey);
         graphics.fill(4, 5, 5, 7, grey);
         graphics.fill(11, 5, 12, 7, grey);
         if ((look >> 8 & 1) == 1 && age >= 60) {
            for (int i = 0; i < 8; i++) {
               if ((look >> (10 + i) & 1) == 1 || i == 3 || i == 4) {
                  graphics.fill(4 + i, 9, 5 + i, i == 0 || i == 7 ? 10 : 11, 0xFFE4E4E0);
               }
            }
         }
      }

      if (ill) {
         graphics.fill(4, 3, 12, 11, 0x5070A050);
      }

      if (crown == 2) {
         int gold = 0xFFE0B030;
         int gem = 0xFF000000 | FactionColors.color(faction);
         graphics.fill(4, 2, 12, 4, gold);
         graphics.fill(4, 1, 5, 2, gold);
         graphics.fill(7, 0, 9, 2, gold);
         graphics.fill(11, 1, 12, 2, gold);
         graphics.fill(7, 2, 9, 3, gem);
         graphics.fill(5, 3, 6, 4, 0xFFB08020);
         graphics.fill(10, 3, 11, 4, 0xFFB08020);
      } else if (crown == 1) {
         graphics.fill(4, 3, 12, 4, 0xFFC8C8D0);
         graphics.fill(7, 3, 9, 4, 0xFF000000 | FactionColors.color(faction));
      }

      graphics.pose().popPose();
   }

   private static int shade(int color, float factor) {
      int r = (int)((color >> 16 & 0xFF) * factor);
      int g = (int)((color >> 8 & 0xFF) * factor);
      int b = (int)((color & 0xFF) * factor);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }
}
