package net.shadowmage.ancientwarfare.core.faction;

import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BannerBlock;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSoldierNpcEntity;

/**
 * Coats of arms: every faction has its own banner, the field in the faction's colour with a
 * division and a charge drawn from its name, the same in every world. Armies and marching
 * columns carry it: one soldier is the standard-bearer, who steadies the men around him; when he
 * falls, the whole band is shaken. His banner drops as a trophy.
 */
public final class FactionHeraldry {
   public static final String BEARER_TAG = "AWStandardBearer";
   /** Extra blow to the morale of every comrade near a fallen standard-bearer. */
   public static final float BEARER_DEATH_MORALE = 60.0F;
   /** Divisions of the field and charges on it, as banner pattern codes. */
   private static final String[] DIVISIONS = {"hh", "vh", "ts", "bs", "cs", "ms", "ld", "rd", "bts", "tts", "sc", "cr"};
   private static final String[] CHARGES = {"mc", "mr", "flo", "glb", "bo", "cbo", "tt", "bt", "sku"};
   /** Some peoples' arms are fixed by hand. */
   private static final Map<String, String[]> FIXED = Map.of(
      "empire", new String[]{"red", "ts:yellow", "mc:yellow", "bo:yellow"},
      "brigand", new String[]{"black", "sku:white"},
      "pirate", new String[]{"black", "sku:white", "bs:red"},
      "norska", new String[]{"light_blue", "sc:white", "bo:blue"},
      "barbarian", new String[]{"green", "bts:brown", "sku:white"}
   );

   private FactionHeraldry() {
   }

   /** The faction's banner, ready to carry or hang. */
   public static ItemStack banner(String faction) {
      String[] arms = FIXED.get(faction);
      DyeColor field;
      ListTag patterns = new ListTag();
      if (arms != null) {
         field = DyeColor.byName(arms[0], DyeColor.WHITE);
         for (int i = 1; i < arms.length; i++) {
            String[] layer = arms[i].split(":");
            patterns.add(layer(layer[0], DyeColor.byName(layer[1], DyeColor.WHITE)));
         }
      } else {
         field = nearestDye(FactionColors.color(faction));
         int hash = faction.hashCode() * 0x9E3779B1;
         DyeColor accent = contrast(field, hash >>> 4);
         patterns.add(layer(DIVISIONS[Math.floorMod(hash, DIVISIONS.length)], accent));
         patterns.add(layer(CHARGES[Math.floorMod(hash >>> 12, CHARGES.length - 1)], (hash & 0x100) == 0 ? accent : field == DyeColor.BLACK ? DyeColor.WHITE : DyeColor.BLACK));
      }

      ItemStack stack = new ItemStack(BannerBlock.byColor(field).asItem());
      CompoundTag entity = new CompoundTag();
      entity.put("Patterns", patterns);
      stack.getOrCreateTag().put("BlockEntityTag", entity);
      stack.setHoverName(Component.translatable("item.ancientwarfare.faction_banner", FactionNews.factionName(faction)));
      return stack;
   }

   private static CompoundTag layer(String pattern, DyeColor color) {
      CompoundTag tag = new CompoundTag();
      tag.putString("Pattern", pattern);
      tag.putInt("Color", color.getId());
      return tag;
   }

   private static DyeColor nearestDye(int argb) {
      DyeColor best = DyeColor.WHITE;
      long bestDistance = Long.MAX_VALUE;

      for (DyeColor dye : DyeColor.values()) {
         int rgb = dye.getFireworkColor();
         long dr = ((argb >> 16) & 0xFF) - ((rgb >> 16) & 0xFF);
         long dg = ((argb >> 8) & 0xFF) - ((rgb >> 8) & 0xFF);
         long db = (argb & 0xFF) - (rgb & 0xFF);
         long distance = dr * dr + dg * dg + db * db;
         if (distance < bestDistance) {
            bestDistance = distance;
            best = dye;
         }
      }

      return best;
   }

   /** A colour that stands out on the field: light on dark, dark on light. */
   private static DyeColor contrast(DyeColor field, int pick) {
      int rgb = field.getFireworkColor();
      int light = ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
      DyeColor[] choices = light > 380
         ? new DyeColor[]{DyeColor.BLACK, DyeColor.BLUE, DyeColor.RED, DyeColor.BROWN}
         : new DyeColor[]{DyeColor.WHITE, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.LIGHT_GRAY};
      return choices[Math.floorMod(pick, choices.length)];
   }

   public static boolean isBearer(FactionNpcEntity npc) {
      return npc.getPersistentData().getBoolean(BEARER_TAG);
   }

   /** Makes sure a band has a standard-bearer while it has a swordsman left to carry the banner. */
   public static void ensureBearer(List<FactionNpcEntity> band) {
      FactionNpcEntity candidate = null;

      for (FactionNpcEntity npc : band) {
         if (!npc.isAlive()) {
            continue;
         }

         if (isBearer(npc)) {
            return;
         }

         if (candidate == null && npc instanceof FactionSoldierNpcEntity) {
            candidate = npc;
         }
      }

      if (candidate != null) {
         candidate.getPersistentData().putBoolean(BEARER_TAG, true);
         candidate.setItemSlot(EquipmentSlot.HEAD, banner(candidate.getFactionName()));
         candidate.setDropChance(EquipmentSlot.HEAD, 1.0F);
      }
   }
}
