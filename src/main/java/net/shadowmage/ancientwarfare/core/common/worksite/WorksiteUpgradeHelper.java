package net.shadowmage.ancientwarfare.core.common.worksite;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

public final class WorksiteUpgradeHelper {
   private WorksiteUpgradeHelper() {
   }

   public static boolean mayPlace(Container container, int slot, ItemStack stack, int upgradeSlotStart, int upgradeSlotCount, boolean quarry) {
      WorksiteUpgrade upgrade = WorksiteUpgrade.fromStack(stack);
      if (upgrade != null && isAllowed(upgrade, quarry)) {
         for (int i = upgradeSlotStart; i < upgradeSlotStart + upgradeSlotCount; i++) {
            if (i != slot) {
               WorksiteUpgrade existing = WorksiteUpgrade.fromStack(container.getItem(i));
               if (existing != null && upgrade.conflictsWith(existing)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static int adjustedWorkCost(int baseCost, Container container, int upgradeSlotStart, int upgradeSlotCount) {
      int quality = getToolQualityLevel(container, upgradeSlotStart, upgradeSlotCount);
      int divisor = 1 + quality;
      return Math.max(1, baseCost / divisor);
   }

   public static ItemStack fortuneTool(Item item, Container container, int upgradeSlotStart, int upgradeSlotCount) {
      ItemStack tool = new ItemStack(item);
      int level = getEnchantedToolsLevel(container, upgradeSlotStart, upgradeSlotCount);
      if (level > 0) {
         tool.enchant(Enchantments.BLOCK_FORTUNE, level);
      }

      return tool;
   }

   public static int getToolQualityLevel(Container container, int upgradeSlotStart, int upgradeSlotCount) {
      if (hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, WorksiteUpgrade.TOOL_QUALITY_3)) {
         return 3;
      } else if (hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, WorksiteUpgrade.TOOL_QUALITY_2)) {
         return 2;
      } else {
         return hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, WorksiteUpgrade.TOOL_QUALITY_1) ? 1 : 0;
      }
   }

   public static int getEnchantedToolsLevel(Container container, int upgradeSlotStart, int upgradeSlotCount) {
      if (hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, WorksiteUpgrade.ENCHANTED_TOOLS_2)) {
         return 2;
      } else {
         return hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, WorksiteUpgrade.ENCHANTED_TOOLS_1) ? 1 : 0;
      }
   }

   public static int getBoundsMaxWidth(Container container, int upgradeSlotStart, int upgradeSlotCount) {
      if (hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, WorksiteUpgrade.SIZE_LARGE)) {
         return 16;
      } else {
         return hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, WorksiteUpgrade.SIZE_MEDIUM) ? 9 : 5;
      }
   }

   public static int getQuarryBoundsMaxWidth(Container container, int upgradeSlotStart, int upgradeSlotCount) {
      if (hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, WorksiteUpgrade.QUARRY_LARGE)) {
         return 64;
      } else {
         return hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, WorksiteUpgrade.QUARRY_MEDIUM) ? 32 : 16;
      }
   }

   public static boolean hasUpgrade(Container container, int upgradeSlotStart, int upgradeSlotCount, WorksiteUpgrade upgrade) {
      for (int i = upgradeSlotStart; i < upgradeSlotStart + upgradeSlotCount; i++) {
         if (WorksiteUpgrade.fromStack(container.getItem(i)) == upgrade) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasChunkLoader(Container container, int upgradeSlotStart, int upgradeSlotCount, boolean quarry) {
      return hasUpgrade(container, upgradeSlotStart, upgradeSlotCount, quarry ? WorksiteUpgrade.QUARRY_CHUNK_LOADER : WorksiteUpgrade.BASIC_CHUNK_LOADER);
   }

   private static boolean isAllowed(WorksiteUpgrade upgrade, boolean quarry) {
      return switch (upgrade) {
         case SIZE_MEDIUM, SIZE_LARGE -> !quarry;
         case QUARRY_MEDIUM, QUARRY_LARGE -> quarry;
         case BASIC_CHUNK_LOADER -> !quarry;
         case QUARRY_CHUNK_LOADER -> quarry;
         case ENCHANTED_TOOLS_1, ENCHANTED_TOOLS_2, TOOL_QUALITY_1, TOOL_QUALITY_2, TOOL_QUALITY_3 -> true;
      };
   }
}
