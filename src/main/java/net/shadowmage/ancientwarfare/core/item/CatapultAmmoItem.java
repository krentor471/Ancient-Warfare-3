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

public class CatapultAmmoItem extends Item {
   private final CatapultAmmoType ammoType;

   public CatapultAmmoItem(CatapultAmmoType ammoType) {
      super(new Properties().stacksTo(16));
      this.ammoType = ammoType;
   }

   public CatapultAmmoType ammoType() {
      return this.ammoType;
   }

   public static CatapultAmmoType ammoType(ItemStack stack) {
      return stack.getItem() instanceof CatapultAmmoItem ammo ? ammo.ammoType() : null;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.ancientwarfare.catapult_ammo.weight", new Object[]{this.ammoType.weight()}).withStyle(ChatFormatting.GRAY));
      if (this.ammoType.damage() > 0.0F) {
         tooltip.add(
            Component.translatable("tooltip.ancientwarfare.catapult_ammo.damage", new Object[]{formatNumber(this.ammoType.damage())})
               .withStyle(ChatFormatting.GRAY)
         );
      }

      if (this.ammoType.explosionPower() > 0.0F) {
         tooltip.add(
            Component.translatable("tooltip.ancientwarfare.catapult_ammo.explosion_power", new Object[]{formatNumber(this.ammoType.explosionPower())})
               .withStyle(ChatFormatting.GRAY)
         );
      }

      tooltip.add(
         Component.translatable(this.ammoType.effectTranslationKey(), new Object[]{this.ammoType.submunitionCount(), this.ammoType.areaTier()})
            .withStyle(ChatFormatting.DARK_GRAY)
      );
   }

   private static String formatNumber(float value) {
      return value == (float)((int)value) ? Integer.toString((int)value) : String.format("%.1f", value);
   }
}
