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

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237115_("item.ancientwarfare.vehicle_upgrade_tooltip").m_130940_(ChatFormatting.GRAY));
      tooltip.add(Component.m_237115_("item.ancientwarfare." + this.id + ".tooltip").m_130940_(ChatFormatting.DARK_GRAY));
   }
}
