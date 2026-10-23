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
 * better.
 */
public final class FactionArmament {
   public static final int NONE = 0;
   public static final int LEATHER = 1;
   public static final int MAIL = 2;
   private static final int LEATHER_NEEDED = 30;
   private static final int COAL_NEEDED = 60;
   private static final int COPPER_NEEDED = 30;
   private static final int IRON_NEEDED = 150;
   /** Set on a soldier once armed, so loading him again leaves him as he was. */
   private static final String ARMED_TAG = "aw_armed";

   private FactionArmament() {
   }

   /** The arms the stores allow: NONE, LEATHER or MAIL. */
   public static int tier(int[] stores) {
      if (stores.length <= FactionResources.COPPER || stores[FactionResources.LEATHER] < LEATHER_NEEDED) {
         return NONE;
      }

      return stores[FactionResources.COAL] >= COAL_NEEDED && stores[FactionResources.COPPER] >= COPPER_NEEDED && stores[FactionResources.IRON] >= IRON_NEEDED ? MAIL : LEATHER;
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
      if (npc.getNpcSubtype().endsWith(".elite")) {
         tier = Math.min(MAIL, tier + 1);
      }

      if (tier == NONE) {
         return;
      }

      int color = FactionColors.color(npc.getFactionName());
      if (tier == MAIL) {
         setIfEmpty(npc, EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
         setIfEmpty(npc, EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
         setIfEmpty(npc, EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS, color));
      } else {
         setIfEmpty(npc, EquipmentSlot.HEAD, dyed(Items.LEATHER_HELMET, color));
         setIfEmpty(npc, EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, color));
      }
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
