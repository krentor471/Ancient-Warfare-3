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
      super(new Properties().m_41487_(1));
   }

   public InteractionResult m_6225_(UseOnContext context) {
      if (context.m_43725_().f_46443_) {
         return InteractionResult.SUCCESS;
      } else {
         ItemStack stack = context.m_43722_();
         CompoundTag tag = stack.m_41784_();
         BlockPos pos = context.m_8083_();
         if (context.m_43723_() != null && context.m_43723_().m_6144_()) {
            tag.m_128356_("BuildOrigin", pos.m_121878_());
            context.m_43723_()
               .m_5661_(
                  Component.m_237110_("message.ancientwarfare.structure_scan.origin", new Object[]{pos.m_123341_(), pos.m_123342_(), pos.m_123343_()}), true
               );
         } else if (!tag.m_128441_("FirstPos")) {
            tag.m_128356_("FirstPos", pos.m_121878_());
            context.m_43723_()
               .m_5661_(
                  Component.m_237110_("message.ancientwarfare.structure_scan.first", new Object[]{pos.m_123341_(), pos.m_123342_(), pos.m_123343_()}), true
               );
         } else {
            tag.m_128356_("SecondPos", pos.m_121878_());
            context.m_43723_()
               .m_5661_(
                  Component.m_237110_("message.ancientwarfare.structure_scan.second", new Object[]{pos.m_123341_(), pos.m_123342_(), pos.m_123343_()}), true
               );
         }

         return InteractionResult.CONSUME;
      }
   }
}
