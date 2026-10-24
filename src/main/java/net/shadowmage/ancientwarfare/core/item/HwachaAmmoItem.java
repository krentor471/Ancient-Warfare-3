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
      super(new Properties().stacksTo(64));
      this.ammoType = ammoType;
   }

   public HwachaAmmoType ammoType() {
      return this.ammoType;
   }

   public static HwachaAmmoType ammoType(ItemStack stack) {
      return stack.getItem() instanceof HwachaAmmoItem ammo ? ammo.ammoType() : null;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable(this.ammoType.effectTranslationKey()).withStyle(ChatFormatting.GRAY));
   }
}
