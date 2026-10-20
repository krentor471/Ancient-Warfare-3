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

public class VehicleArmorItem extends Item {
   private final VehicleArmorItem.Type type;

   public VehicleArmorItem(VehicleArmorItem.Type type) {
      super(new Properties());
      this.type = type;
   }

   public VehicleArmorItem.Type type() {
      return this.type;
   }

   @Nullable
   public static VehicleArmorItem.Type fromStack(ItemStack stack) {
      return stack.getItem() instanceof VehicleArmorItem armor ? armor.type() : null;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.ancientwarfare.vehicle_armor_tooltip").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.ancientwarfare." + this.type.id() + ".tooltip").withStyle(ChatFormatting.DARK_GRAY));
   }

   public static enum Type {
      STONE("armor_stone", 2.5F, 2.5F, 7.0F),
      IRON("armor_iron", 7.0F, 2.5F, 2.5F),
      OBSIDIAN("armor_obsidian", 2.5F, 7.0F, 2.5F);

      private final String id;
      private final float generalReduction;
      private final float explosiveReduction;
      private final float fireReduction;

      private Type(String id, float generalReduction, float explosiveReduction, float fireReduction) {
         this.id = id;
         this.generalReduction = generalReduction;
         this.explosiveReduction = explosiveReduction;
         this.fireReduction = fireReduction;
      }

      public String id() {
         return this.id;
      }

      public float generalReduction() {
         return this.generalReduction;
      }

      public float explosiveReduction() {
         return this.explosiveReduction;
      }

      public float fireReduction() {
         return this.fireReduction;
      }
   }
}
