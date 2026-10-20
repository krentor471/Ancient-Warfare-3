package net.shadowmage.ancientwarfare.core.common.hammer;

import net.minecraft.world.item.ItemStack;

public final class HammerModeData {
   public static final String WORK_MODE_TAG = "workMode";

   private HammerModeData() {
   }

   public static boolean isWorkMode(ItemStack stack) {
      return stack.m_41782_() && stack.m_41784_().m_128471_("workMode");
   }

   public static boolean toggleWorkMode(ItemStack stack) {
      boolean workMode = !isWorkMode(stack);
      stack.m_41784_().m_128379_("workMode", workMode);
      return workMode;
   }

   public static void setWorkMode(ItemStack stack, boolean workMode) {
      stack.m_41784_().m_128379_("workMode", workMode);
   }
}
