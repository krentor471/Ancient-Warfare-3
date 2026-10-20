package net.shadowmage.ancientwarfare.core.common.backpack;

import net.minecraft.world.item.ItemStack;

public final class BackpackData {
   public static final String LEVEL_TAG = "backpackLevel";
   public static final String ITEMS_TAG = "backpackItems";
   public static final int MAX_LEVEL = 3;
   public static final int SLOTS_PER_LEVEL = 9;

   private BackpackData() {
   }

   public static int clampLevel(int level) {
      return Math.max(0, Math.min(3, level));
   }

   public static int getLevel(ItemStack stack) {
      return clampLevel(stack.m_41784_().m_128451_("backpackLevel"));
   }

   public static void setLevel(ItemStack stack, int level) {
      stack.m_41784_().m_128405_("backpackLevel", clampLevel(level));
   }

   public static int getSlotCount(ItemStack stack) {
      return getSlotCountForLevel(getLevel(stack));
   }

   public static int getSlotCountForLevel(int level) {
      return (clampLevel(level) + 1) * 9;
   }
}
