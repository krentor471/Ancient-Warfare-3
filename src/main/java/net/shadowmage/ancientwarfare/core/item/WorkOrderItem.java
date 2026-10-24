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
      super(new Properties().stacksTo(1));
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         this.openWorkOrder(context.getLevel(), player, context.getHand());
         return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      this.openWorkOrder(level, player, hand);
      return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
   }

   public static boolean addTargetedBlock(Player player, BlockPos pos) {
      ItemStack stack = findHeldWorkOrder(player);
      if (stack.isEmpty()) {
         return false;
      } else {
         if (WorkOrderData.addEntry(stack, player.level(), pos)) {
            player.displayClientMessage(Component.translatable("guistrings.npc.work_order.added", new Object[]{formatPos(pos)}), true);
         } else {
            player.displayClientMessage(Component.translatable("guistrings.npc.work_order.add_failed"), true);
         }

         return true;
      }
   }

   private void openWorkOrder(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new WorkOrderMenu(containerId, inventory, hand),
               Component.translatable("container.ancientwarfare.work_order")
            ),
            buffer -> buffer.writeEnum(hand)
         );
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      List<BlockPos> entries = WorkOrderData.getEntries(stack);
      tooltip.add(Component.translatable("tooltip.ancientwarfare.work_order.entries", new Object[]{entries.size(), 8}).withStyle(ChatFormatting.GRAY));
      entries.stream().limit(4L).forEach(pos -> tooltip.add(Component.literal(formatPos(pos)).withStyle(ChatFormatting.DARK_GRAY)));
      if (entries.size() > 4) {
         tooltip.add(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   private static ItemStack findHeldWorkOrder(Player player) {
      ItemStack mainHand = player.getMainHandItem();
      if (mainHand.getItem() instanceof WorkOrderItem) {
         return mainHand;
      } else {
         ItemStack offHand = player.getOffhandItem();
         return offHand.getItem() instanceof WorkOrderItem ? offHand : ItemStack.EMPTY;
      }
   }

   public static String formatPos(BlockPos pos) {
      return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
   }
}
