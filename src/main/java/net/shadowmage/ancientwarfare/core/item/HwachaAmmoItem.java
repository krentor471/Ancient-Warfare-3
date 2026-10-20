package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class HwachaAmmoItem extends Item {
   private final HwachaAmmoType ammoType;

   public HwachaAmmoItem(HwachaAmmoType ammoType) {
      super(new Properties().m_41487_(64));
      this.ammoType = ammoType;
   }

   public HwachaAmmoType ammoType() {
      return this.ammoType;
   }

   public static HwachaAmmoType ammoType(ItemStack stack) {
      return stack.m_41720_() instanceof HwachaAmmoItem ammo ? ammo.ammoType() : null;
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237115_(this.ammoType.effectTranslationKey()).m_130940_(ChatFormatting.GRAY));
   }
}
