package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.BatteringRamEntity;

public class BatteringRamItem extends Item {
   public static final String CART_DATA_TAG = "BatteringRamData";
   private final int materialLevel;

   public BatteringRamItem(int materialLevel) {
      super(new Properties().stacksTo(1));
      this.materialLevel = materialLevel;
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      ItemStack stack = context.getItemInHand();
      if (!level.isClientSide) {
         BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
         BatteringRamEntity ram = (BatteringRamEntity)AWCoreRegistryAccess.entityType(AWCoreIds.BATTERING_RAM).create(level);
         if (ram != null) {
            ram.setMaterialLevel(this.materialLevel);
            if (stack.hasTag() && stack.getTag().contains("BatteringRamData")) {
               ram.loadCartData(stack.getTag().getCompound("BatteringRamData"));
            } else {
               ram.setHealth(ram.getMaxHealth());
            }

            ram.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, context.getRotation(), 0.0F);
            level.addFreshEntity(ram);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
               stack.shrink(1);
            }
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public static Item itemForMaterialLevel(int level) {
      return switch (Math.max(0, Math.min(4, level))) {
         case 1 -> AWCoreRegistryAccess.item(AWCoreIds.id("battering_ram_iron_band_1"));
         case 2 -> AWCoreRegistryAccess.item(AWCoreIds.id("battering_ram_iron_band_2"));
         case 3 -> AWCoreRegistryAccess.item(AWCoreIds.id("battering_ram_iron"));
         case 4 -> AWCoreRegistryAccess.item(AWCoreIds.id("battering_ram_steel"));
         default -> AWCoreRegistryAccess.item(AWCoreIds.id("battering_ram"));
      };
   }
}
