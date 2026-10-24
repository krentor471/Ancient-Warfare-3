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
import net.shadowmage.ancientwarfare.core.entity.TrebuchetFixedEntity;

public class TrebuchetFixedItem extends Item {
   public static final String CART_DATA_TAG = "TrebuchetFixedData";
   private final int materialLevel;

   public TrebuchetFixedItem(int materialLevel) {
      super(new Properties().stacksTo(1));
      this.materialLevel = materialLevel;
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      ItemStack stack = context.getItemInHand();
      if (!level.isClientSide) {
         BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
         TrebuchetFixedEntity trebuchet = (TrebuchetFixedEntity)AWCoreRegistryAccess.entityType(AWCoreIds.TREBUCHET_FIXED).create(level);
         if (trebuchet != null) {
            trebuchet.setMaterialLevel(this.materialLevel);
            trebuchet.setFixedTurret(true);
            if (stack.hasTag() && stack.getTag().contains("TrebuchetFixedData")) {
               trebuchet.loadCartData(stack.getTag().getCompound("TrebuchetFixedData"));
            } else {
               trebuchet.setHealth(trebuchet.getMaxHealth());
            }

            trebuchet.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, context.getRotation(), 0.0F);
            level.addFreshEntity(trebuchet);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
               stack.shrink(1);
            }
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public static Item itemForMaterialLevel(int level) {
      return switch (Math.max(0, Math.min(4, level))) {
         case 1 -> AWCoreRegistryAccess.item(AWCoreIds.id("trebuchet_fixed_iron_band_1"));
         case 2 -> AWCoreRegistryAccess.item(AWCoreIds.id("trebuchet_fixed_iron_band_2"));
         case 3 -> AWCoreRegistryAccess.item(AWCoreIds.id("trebuchet_fixed_iron"));
         case 4 -> AWCoreRegistryAccess.item(AWCoreIds.id("trebuchet_fixed_steel"));
         default -> AWCoreRegistryAccess.item(AWCoreIds.TREBUCHET_FIXED);
      };
   }
}
