package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.TransportBoatEntity;

public class TransportBoatItem extends Item {
   public static final String CART_DATA_TAG = "TransportBoatData";
   private final int materialLevel;

   public TransportBoatItem(int materialLevel) {
      super(new Properties().m_41487_(1));
      this.materialLevel = materialLevel;
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Level level = context.m_43725_();
      BlockPos clickedPos = context.m_8083_();
      if (!level.m_6425_(clickedPos).m_205070_(FluidTags.f_13131_)) {
         return InteractionResult.FAIL;
      } else {
         if (!level.f_46443_) {
            this.spawnBoat(level, clickedPos, context.m_43722_(), context.m_43723_(), context.m_7074_());
         }

         return InteractionResult.m_19078_(level.f_46443_);
      }
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      BlockHitResult hit = m_41435_(level, player, Fluid.SOURCE_ONLY);
      if (hit.m_6662_() == Type.BLOCK && level.m_6425_(hit.m_82425_()).m_205070_(FluidTags.f_13131_)) {
         if (!level.f_46443_) {
            this.spawnBoat(level, hit.m_82425_(), stack, player, player.m_146908_());
         }

         return InteractionResultHolder.m_19092_(stack, level.f_46443_);
      } else {
         return InteractionResultHolder.m_19100_(stack);
      }
   }

   private void spawnBoat(Level level, BlockPos waterPos, ItemStack stack, Player player, float rotation) {
      TransportBoatEntity boat = (TransportBoatEntity)AWCoreRegistryAccess.entityType(AWCoreIds.TRANSPORT_BOAT).m_20615_(level);
      if (boat != null) {
         boat.setMaterialLevel(this.materialLevel);
         CompoundTag tag = stack.m_41783_();
         if (tag != null && tag.m_128441_("TransportBoatData")) {
            boat.loadCartData(tag.m_128469_("TransportBoatData"));
         } else {
            boat.setHealth(boat.getMaxHealth());
         }

         boat.m_7678_((double)waterPos.m_123341_() + 0.5, (double)waterPos.m_123342_() + 0.05, (double)waterPos.m_123343_() + 0.5, rotation, 0.0F);
         level.m_7967_(boat);
         if (player == null || !player.m_150110_().f_35937_) {
            stack.m_41774_(1);
         }
      }
   }

   public static Item itemForMaterialLevel(int level) {
      return switch (Math.max(0, Math.min(4, level))) {
         case 1 -> AWCoreRegistryAccess.item(AWCoreIds.id("transport_boat_iron_band_1"));
         case 2 -> AWCoreRegistryAccess.item(AWCoreIds.id("transport_boat_iron_band_2"));
         case 3 -> AWCoreRegistryAccess.item(AWCoreIds.id("transport_boat_iron"));
         case 4 -> AWCoreRegistryAccess.item(AWCoreIds.id("transport_boat_steel"));
         default -> AWCoreRegistryAccess.item(AWCoreIds.TRANSPORT_BOAT);
      };
   }
}
