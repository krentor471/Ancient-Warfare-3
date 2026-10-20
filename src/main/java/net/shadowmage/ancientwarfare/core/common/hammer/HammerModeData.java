package net.shadowmage.ancientwarfare.core.common.hammer;

import net.minecraft.world.item.ItemStack;

public final class HammerModeData {
   public static final String WORK_MODE_TAG = "workMode";

   private HammerModeData() {
   }

   public static boolean isWorkMode(ItemStack stack) {
      return stack.hasTag() && stack.getOrCreateTag().getBoolean("workMode");
   }

   public static boolean toggleWorkMode(ItemStack stack) {
      boolean workMode = !isWorkMode(stack);
      stack.getOrCreateTag().putBoolean("workMode", workMode);
      return workMode;
   }

   public static void setWorkMode(ItemStack stack, boolean workMode) {
      stack.getOrCreateTag().putBoolean("workMode", workMode);
   }
}
