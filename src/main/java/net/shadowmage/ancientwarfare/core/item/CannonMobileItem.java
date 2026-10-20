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
import net.shadowmage.ancientwarfare.core.entity.CannonMobileEntity;

public class CannonMobileItem extends Item {
   public static final String CART_DATA_TAG = "CannonMobileData";
   private final int materialLevel;

   public CannonMobileItem(int materialLevel) {
      super(new Properties().m_41487_(1));
      this.materialLevel = materialLevel;
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Level level = context.m_43725_();
      ItemStack stack = context.m_43722_();
      if (!level.f_46443_) {
         BlockPos pos = context.m_8083_().m_121945_(context.m_43719_());
         CannonMobileEntity cannon = (CannonMobileEntity)AWCoreRegistryAccess.entityType(AWCoreIds.CANNON_MOBILE).m_20615_(level);
         if (cannon != null) {
            cannon.setMaterialLevel(this.materialLevel);
            if (stack.m_41782_() && stack.m_41783_().m_128441_("CannonMobileData")) {
               cannon.loadCartData(stack.m_41783_().m_128469_("CannonMobileData"));
            } else {
               cannon.setHealth(cannon.getMaxHealth());
            }

            cannon.m_7678_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5, context.m_7074_(), 0.0F);
            level.m_7967_(cannon);
            if (context.m_43723_() == null || !context.m_43723_().m_150110_().f_35937_) {
               stack.m_41774_(1);
            }
         }
      }

      return InteractionResult.m_19078_(level.f_46443_);
   }

   public static Item itemForMaterialLevel(int level) {
      return switch (Math.max(0, Math.min(4, level))) {
         case 1 -> AWCoreRegistryAccess.item(AWCoreIds.id("cannon_mobile_iron_band_1"));
         case 2 -> AWCoreRegistryAccess.item(AWCoreIds.id("cannon_mobile_iron_band_2"));
         case 3 -> AWCoreRegistryAccess.item(AWCoreIds.id("cannon_mobile_iron"));
         case 4 -> AWCoreRegistryAccess.item(AWCoreIds.id("cannon_mobile_steel"));
         default -> AWCoreRegistryAccess.item(AWCoreIds.CANNON_MOBILE);
      };
   }
}
