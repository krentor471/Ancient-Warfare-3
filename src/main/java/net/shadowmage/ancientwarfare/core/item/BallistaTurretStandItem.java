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
import net.shadowmage.ancientwarfare.core.entity.BallistaTurretStandEntity;

public class BallistaTurretStandItem extends Item {
   public static final String CART_DATA_TAG = "BallistaTurretStandData";
   private final int materialLevel;
   private final boolean fixed;

   public BallistaTurretStandItem(int materialLevel) {
      this(materialLevel, false);
   }

   public BallistaTurretStandItem(int materialLevel, boolean fixed) {
      super(new Properties().stacksTo(1));
      this.materialLevel = materialLevel;
      this.fixed = fixed;
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      ItemStack stack = context.getItemInHand();
      if (!level.isClientSide) {
         BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
         BallistaTurretStandEntity stand = this.fixed
            ? (BallistaTurretStandEntity)AWCoreRegistryAccess.entityType(AWCoreIds.BALLISTA_STAND_FIXED).create(level)
            : (BallistaTurretStandEntity)AWCoreRegistryAccess.entityType(AWCoreIds.BALLISTA_TURRET_STAND).create(level);
         if (stand != null) {
            stand.setMaterialLevel(this.materialLevel);
            if (stack.hasTag() && stack.getTag().contains("BallistaTurretStandData")) {
               stand.loadCartData(stack.getTag().getCompound("BallistaTurretStandData"));
            } else {
               stand.setHealth(stand.getMaxHealth());
            }

            stand.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, context.getRotation(), 0.0F);
            level.addFreshEntity(stand);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
               stack.shrink(1);
            }
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public static Item itemForMaterialLevel(int level) {
      return itemForMaterialLevel(level, false);
   }

   public static Item itemForMaterialLevel(int level, boolean fixed) {
      if (fixed) {
         return switch (Math.max(0, Math.min(4, level))) {
            case 1 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_stand_fixed_iron_band_1"));
            case 2 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_stand_fixed_iron_band_2"));
            case 3 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_stand_fixed_iron"));
            case 4 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_stand_fixed_steel"));
            default -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_stand_fixed"));
         };
      } else {
         return switch (Math.max(0, Math.min(4, level))) {
            case 1 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_turret_stand_iron_band_1"));
            case 2 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_turret_stand_iron_band_2"));
            case 3 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_turret_stand_iron"));
            case 4 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_turret_stand_steel"));
            default -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_turret_stand"));
         };
      }
   }
}
