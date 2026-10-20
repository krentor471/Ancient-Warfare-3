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

public class VehicleUpgradeItem extends Item {
   private final String id;

   public VehicleUpgradeItem(String id) {
      super(new Properties());
      this.id = id;
   }

   public String id() {
      return this.id;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.ancientwarfare.vehicle_upgrade_tooltip").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.ancientwarfare." + this.id + ".tooltip").withStyle(ChatFormatting.DARK_GRAY));
   }
}
