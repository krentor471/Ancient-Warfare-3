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
import net.shadowmage.ancientwarfare.core.entity.CatapultTurretStandEntity;

public class CatapultTurretStandItem extends Item {
   public static final String CART_DATA_TAG = "CatapultTurretStandData";
   private final int materialLevel;
   private final boolean fixedTurret;

   public CatapultTurretStandItem(int materialLevel) {
      this(materialLevel, false);
   }

   public CatapultTurretStandItem(int materialLevel, boolean fixedTurret) {
      super(new Properties().stacksTo(1));
      this.materialLevel = materialLevel;
      this.fixedTurret = fixedTurret;
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      ItemStack stack = context.getItemInHand();
      if (!level.isClientSide) {
         BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
         CatapultTurretStandEntity catapult = (CatapultTurretStandEntity)AWCoreRegistryAccess.entityType(AWCoreIds.CATAPULT_TURRET_STAND).create(level);
         if (catapult != null) {
            catapult.setMaterialLevel(this.materialLevel);
            catapult.setFixedTurret(this.fixedTurret);
            if (stack.hasTag() && stack.getTag().contains("CatapultTurretStandData")) {
               catapult.loadCartData(stack.getTag().getCompound("CatapultTurretStandData"));
            } else {
               catapult.setHealth(catapult.getMaxHealth());
            }

            catapult.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, context.getRotation(), 0.0F);
            level.addFreshEntity(catapult);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
               stack.shrink(1);
            }
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public static Item itemForMaterialLevel(int level, boolean fixedTurret) {
      if (fixedTurret) {
         return switch (Math.max(0, Math.min(4, level))) {
            case 1 -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_stand_fixed_iron_band_1"));
            case 2 -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_stand_fixed_iron_band_2"));
            case 3 -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_stand_fixed_iron"));
            case 4 -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_stand_fixed_steel"));
            default -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_stand_fixed"));
         };
      } else {
         return switch (Math.max(0, Math.min(4, level))) {
            case 1 -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_turret_stand_iron_band_1"));
            case 2 -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_turret_stand_iron_band_2"));
            case 3 -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_turret_stand_iron"));
            case 4 -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_turret_stand_steel"));
            default -> AWCoreRegistryAccess.item(AWCoreIds.id("catapult_turret_stand"));
         };
      }
   }
}
