package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.WorkOrderData;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.WorkOrderMenu;

public class WorkOrderItem extends Item {
   public WorkOrderItem() {
      super(new Properties().m_41487_(1));
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Player player = context.m_43723_();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         this.openWorkOrder(context.m_43725_(), player, context.m_43724_());
         return InteractionResult.m_19078_(context.m_43725_().f_46443_);
      }
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      this.openWorkOrder(level, player, hand);
      return InteractionResultHolder.m_19092_(player.m_21120_(hand), level.f_46443_);
   }

   public static boolean addTargetedBlock(Player player, BlockPos pos) {
      ItemStack stack = findHeldWorkOrder(player);
      if (stack.m_41619_()) {
         return false;
      } else {
         if (WorkOrderData.addEntry(stack, player.m_9236_(), pos)) {
            player.m_5661_(Component.m_237110_("guistrings.npc.work_order.added", new Object[]{formatPos(pos)}), true);
         } else {
            player.m_5661_(Component.m_237115_("guistrings.npc.work_order.add_failed"), true);
         }

         return true;
      }
   }

   private void openWorkOrder(Level level, Player player, InteractionHand hand) {
      if (!level.f_46443_) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new WorkOrderMenu(containerId, inventory, hand),
               Component.m_237115_("container.ancientwarfare.work_order")
            ),
            buffer -> buffer.m_130068_(hand)
         );
      }
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      List<BlockPos> entries = WorkOrderData.getEntries(stack);
      tooltip.add(Component.m_237110_("tooltip.ancientwarfare.work_order.entries", new Object[]{entries.size(), 8}).m_130940_(ChatFormatting.GRAY));
      entries.stream().limit(4L).forEach(pos -> tooltip.add(Component.m_237113_(formatPos(pos)).m_130940_(ChatFormatting.DARK_GRAY)));
      if (entries.size() > 4) {
         tooltip.add(Component.m_237113_("...").m_130940_(ChatFormatting.DARK_GRAY));
      }
   }

   private static ItemStack findHeldWorkOrder(Player player) {
      ItemStack mainHand = player.m_21205_();
      if (mainHand.m_41720_() instanceof WorkOrderItem) {
         return mainHand;
      } else {
         ItemStack offHand = player.m_21206_();
         return offHand.m_41720_() instanceof WorkOrderItem ? offHand : ItemStack.f_41583_;
      }
   }

   public static String formatPos(BlockPos pos) {
      return pos.m_123341_() + ", " + pos.m_123342_() + ", " + pos.m_123343_();
   }
}
