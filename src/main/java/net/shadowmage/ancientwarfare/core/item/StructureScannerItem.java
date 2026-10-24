package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;

public class StructureScannerItem extends Item {
   public StructureScannerItem() {
      super(new Properties().stacksTo(1));
   }

   public InteractionResult useOn(UseOnContext context) {
      if (context.getLevel().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         ItemStack stack = context.getItemInHand();
         CompoundTag tag = stack.getOrCreateTag();
         BlockPos pos = context.getClickedPos();
         if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            tag.putLong("BuildOrigin", pos.asLong());
            context.getPlayer()
               .displayClientMessage(
                  Component.translatable("message.ancientwarfare.structure_scan.origin", new Object[]{pos.getX(), pos.getY(), pos.getZ()}), true
               );
         } else if (!tag.contains("FirstPos")) {
            tag.putLong("FirstPos", pos.asLong());
            context.getPlayer()
               .displayClientMessage(
                  Component.translatable("message.ancientwarfare.structure_scan.first", new Object[]{pos.getX(), pos.getY(), pos.getZ()}), true
               );
         } else {
            tag.putLong("SecondPos", pos.asLong());
            context.getPlayer()
               .displayClientMessage(
                  Component.translatable("message.ancientwarfare.structure_scan.second", new Object[]{pos.getX(), pos.getY(), pos.getZ()}), true
               );
         }

         return InteractionResult.CONSUME;
      }
   }
}
