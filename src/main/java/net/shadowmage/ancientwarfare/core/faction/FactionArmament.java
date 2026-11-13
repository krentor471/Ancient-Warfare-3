package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * How a faction's soldiers are armed, by what its stores allow. With leather to spare its
 * recruits get leather caps and jerkins in the faction's colours; with coal and copper as well as
 * iron its smiths make mail and helms. Each recruit uses up a little of each; a faction that runs
 * short sends its new soldiers out with what they stand in. Elite soldiers are armed one step
 * better. No two men of a host look alike: each takes what the armoury has to hand, a helm or
 * none, mail or leather, greaves when there are greaves; a rich faction puts its best men in plate.
 */
public final class FactionArmament {
   public static final int NONE = 0;
   public static final int LEATHER = 1;
   public static final int MAIL = 2;
   public static final int PLATE = 3;
   private static final int LEATHER_NEEDED = 30;
   private static final int COAL_NEEDED = 60;
   private static final int COPPER_NEEDED = 30;
   private static final int IRON_NEEDED = 150;
   private static final int PLATE_IRON_NEEDED = 400;
   private static final int PLATE_COAL_NEEDED = 150;
   /** Set on a soldier once armed, so loading him again leaves him as he was. */
   private static final String ARMED_TAG = "aw_armed";

   private FactionArmament() {
   }

   /** The arms the stores allow: NONE, LEATHER, MAIL or PLATE. */
   public static int tier(int[] stores) {
      if (stores.length <= FactionResources.COPPER || stores[FactionResources.LEATHER] < LEATHER_NEEDED) {
         return NONE;
      }

      if (stores[FactionResources.COAL] < COAL_NEEDED || stores[FactionResources.COPPER] < COPPER_NEEDED || stores[FactionResources.IRON] < IRON_NEEDED) {
         return LEATHER;
      }

      return stores[FactionResources.IRON] >= PLATE_IRON_NEEDED && stores[FactionResources.COAL] >= PLATE_COAL_NEEDED ? PLATE : MAIL;
   }

   /** A recruit takes his share of the stores for his arms. */
   static void recruit(FactionWorldData data, String faction) {
      int[] stores = data.stores(faction);
      int tier = tier(stores);
      if (tier >= LEATHER) {
         stores[FactionResources.LEATHER]--;
      }

      if (tier >= MAIL) {
         stores[FactionResources.COAL]--;
         stores[FactionResources.COPPER]--;
      }
   }

   private static boolean soldier(String role) {
      return FactionNpcRole.byId(role)
         .map(
            value -> switch (value) {
               case SOLDIER, SOLDIER_ELITE, ARCHER, ARCHER_ELITE, CAVALRY, MOUNTED_ARCHER -> true;
               default -> false;
            }
         )
         .orElse(false);
   }

   /** A soldier of the faction comes into the world: he gets the armour his faction can give. */
   public static void equip(ServerLevel level, FactionNpcEntity npc) {
      if (npc.getTags().contains(ARMED_TAG) || !soldier(npc.getNpcSubtype()) || FactionDiplomacy.isOutlaw(npc.getFactionName())) {
         return;
      }

      npc.addTag(ARMED_TAG);
      int tier = tier(FactionWorldData.get(level).stores(npc.getFactionName()));
      boolean elite = npc.getNpcSubtype().endsWith(".elite");
      if (elite) {
         tier = Math.min(PLATE, tier + 1);
      }

      if (tier == NONE) {
         return;
      }

      // Archers and riders go lighter than the foot: no heavier than mail.
      String role = npc.getNpcSubtype();
      boolean light = role.contains("archer") || role.contains("cavalry");
      if (light && tier == PLATE) {
         tier = MAIL;
      }

      net.minecraft.util.RandomSource random = npc.getRandom();
      int color = FactionColors.color(npc.getFactionName());
      double full = elite ? 1.0 : 0.0;
      if (random.nextDouble() < Math.max(full, 0.8)) {
         setIfEmpty(npc, EquipmentSlot.HEAD, piece(random, EquipmentSlot.HEAD, tier, color));
      }

      if (random.nextDouble() < Math.max(full, 0.95)) {
         setIfEmpty(npc, EquipmentSlot.CHEST, piece(random, EquipmentSlot.CHEST, tier, color));
      }

      if (random.nextDouble() < Math.max(full, 0.25 + 0.2 * tier)) {
         setIfEmpty(npc, EquipmentSlot.LEGS, piece(random, EquipmentSlot.LEGS, tier, color));
      }

      if (random.nextDouble() < Math.max(full, 0.7)) {
         setIfEmpty(npc, EquipmentSlot.FEET, piece(random, EquipmentSlot.FEET, tier, color));
      }
   }

   /** One piece for a slot: mostly of the faction's best, now and then a step worse. */
   private static ItemStack piece(net.minecraft.util.RandomSource random, EquipmentSlot slot, int tier, int color) {
      int made = tier > LEATHER && random.nextInt(3) == 0 ? tier - 1 : tier;
      net.minecraft.world.item.Item item = switch (made) {
         case PLATE -> switch (slot) {
            case HEAD -> Items.IRON_HELMET;
            case CHEST -> Items.IRON_CHESTPLATE;
            case LEGS -> Items.IRON_LEGGINGS;
            default -> Items.IRON_BOOTS;
         };
         case MAIL -> switch (slot) {
            case HEAD -> random.nextBoolean() ? Items.CHAINMAIL_HELMET : Items.IRON_HELMET;
            case CHEST -> Items.CHAINMAIL_CHESTPLATE;
            case LEGS -> Items.CHAINMAIL_LEGGINGS;
            default -> random.nextBoolean() ? Items.CHAINMAIL_BOOTS : Items.LEATHER_BOOTS;
         };
         default -> switch (slot) {
            case HEAD -> Items.LEATHER_HELMET;
            case CHEST -> Items.LEATHER_CHESTPLATE;
            case LEGS -> Items.LEATHER_LEGGINGS;
            default -> Items.LEATHER_BOOTS;
         };
      };
      return dyed(item, color);
   }

   private static ItemStack dyed(net.minecraft.world.item.Item item, int color) {
      ItemStack stack = new ItemStack(item);
      if (item instanceof DyeableLeatherItem dyeable) {
         dyeable.setColor(stack, color & 0xFFFFFF);
      }

      return stack;
   }

   private static void setIfEmpty(FactionNpcEntity npc, EquipmentSlot slot, ItemStack stack) {
      if (npc.getItemBySlot(slot).isEmpty()) {
         npc.setItemSlot(slot, stack);
         // What a soldier was given is his, not loot for whoever kills him.
         npc.setDropChance(slot, 0.0F);
      }
   }
}
