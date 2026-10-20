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

public class CannonAmmoItem extends Item {
   private final CannonAmmoType ammoType;

   public CannonAmmoItem(CannonAmmoType ammoType) {
      super(new Properties().m_41487_(16));
      this.ammoType = ammoType;
   }

   public CannonAmmoType ammoType() {
      return this.ammoType;
   }

   public static CannonAmmoType ammoType(ItemStack stack) {
      return stack.m_41720_() instanceof CannonAmmoItem ammo ? ammo.ammoType() : null;
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237110_("tooltip.ancientwarfare.cannon_ammo.weight", new Object[]{this.ammoType.weight()}).m_130940_(ChatFormatting.GRAY));
      if (this.ammoType.damage() > 0.0F) {
         tooltip.add(
            Component.m_237110_("tooltip.ancientwarfare.cannon_ammo.damage", new Object[]{formatNumber(this.ammoType.damage())}).m_130940_(ChatFormatting.GRAY)
         );
      }

      tooltip.add(Component.m_237110_(this.ammoType.effectTranslationKey(), new Object[]{this.ammoType.pelletCount()}).m_130940_(ChatFormatting.DARK_GRAY));
   }

   private static String formatNumber(float value) {
      return value == (float)((int)value) ? Integer.toString((int)value) : String.format("%.1f", value);
   }
}
