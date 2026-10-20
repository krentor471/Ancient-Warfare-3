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

public class VehicleCraftingMaterialItem extends Item {
   private final VehicleCraftingMaterialItem.MaterialType materialType;

   public VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType materialType) {
      super(new Properties());
      this.materialType = materialType;
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237115_(this.materialType.tooltipKey).m_130940_(ChatFormatting.GRAY));
   }

   public static enum MaterialType {
      AMMO_MATERIAL("item.ancientwarfare.ammo_material.tooltip"),
      VEHICLE_COMPONENT("item.ancientwarfare.vehicle_component.tooltip");

      private final String tooltipKey;

      private MaterialType(String tooltipKey) {
         this.tooltipKey = tooltipKey;
      }
   }
}
