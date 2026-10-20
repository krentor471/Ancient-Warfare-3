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
import net.shadowmage.ancientwarfare.core.entity.BallistaBoatEntity;

public class BallistaBoatItem extends Item {
   public static final String CART_DATA_TAG = "BallistaBoatData";
   private final int materialLevel;

   public BallistaBoatItem(int materialLevel) {
      super(new Properties().stacksTo(1));
      this.materialLevel = materialLevel;
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos clickedPos = context.getClickedPos();
      if (!level.getFluidState(clickedPos).is(FluidTags.WATER)) {
         return InteractionResult.FAIL;
      } else {
         if (!level.isClientSide) {
            this.spawnBoat(level, clickedPos, context.getItemInHand(), context.getPlayer(), context.getRotation());
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      BlockHitResult hit = getPlayerPOVHitResult(level, player, Fluid.SOURCE_ONLY);
      if (hit.getType() == Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
         if (!level.isClientSide) {
            this.spawnBoat(level, hit.getBlockPos(), stack, player, player.getYRot());
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
      } else {
         return InteractionResultHolder.fail(stack);
      }
   }

   private void spawnBoat(Level level, BlockPos waterPos, ItemStack stack, Player player, float rotation) {
      BallistaBoatEntity boat = (BallistaBoatEntity)AWCoreRegistryAccess.entityType(AWCoreIds.BALLISTA_BOAT).create(level);
      if (boat != null) {
         boat.setMaterialLevel(this.materialLevel);
         CompoundTag tag = stack.getTag();
         if (tag != null && tag.contains("BallistaBoatData")) {
            boat.loadCartData(tag.getCompound("BallistaBoatData"));
         } else {
            boat.setHealth(boat.getMaxHealth());
         }

         boat.moveTo((double)waterPos.getX() + 0.5, (double)waterPos.getY() + 0.05, (double)waterPos.getZ() + 0.5, rotation, 0.0F);
         level.addFreshEntity(boat);
         if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
         }
      }
   }

   public static Item itemForMaterialLevel(int level) {
      return switch (Math.max(0, Math.min(4, level))) {
         case 1 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_boat_iron_band_1"));
         case 2 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_boat_iron_band_2"));
         case 3 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_boat_iron"));
         case 4 -> AWCoreRegistryAccess.item(AWCoreIds.id("ballista_boat_steel"));
         default -> AWCoreRegistryAccess.item(AWCoreIds.BALLISTA_BOAT);
      };
   }
}
