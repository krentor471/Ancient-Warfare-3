package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.shadowmage.ancientwarfare.core.common.npc.RoutingOrderData;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.RoutingOrderMenu;

public class RoutingOrderItem extends Item {
   public RoutingOrderItem() {
      super(new Properties().stacksTo(1));
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         this.openRoutingOrder(context.getLevel(), player, context.getHand());
         return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      this.openRoutingOrder(level, player, hand);
      return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
   }

   public static boolean addTargetedBlock(Player player, BlockPos pos, Direction side) {
      ItemStack stack = findHeldRoutingOrder(player);
      if (stack.isEmpty()) {
         return false;
      } else {
         if (RoutingOrderData.addEntry(stack, player.level(), pos, side)) {
            player.displayClientMessage(Component.translatable("guistrings.npc.routing_order.added", new Object[]{WorkOrderItem.formatPos(pos), side.getName()}), true);
         } else {
            player.displayClientMessage(Component.translatable("guistrings.npc.routing_order.add_failed"), true);
         }

         return true;
      }
   }

   private void openRoutingOrder(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new RoutingOrderMenu(containerId, inventory, hand),
               Component.translatable("container.ancientwarfare.routing_order")
            ),
            buffer -> buffer.writeEnum(hand)
         );
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      List<RoutingOrderData.RouteEntry> entries = RoutingOrderData.getEntries(stack);
      tooltip.add(Component.translatable("tooltip.ancientwarfare.routing_order.entries", new Object[]{entries.size(), 16}).withStyle(ChatFormatting.GRAY));
      entries.stream()
         .limit(4L)
         .forEach(
            entry -> tooltip.add(
                  Component.literal(WorkOrderItem.formatPos(entry.pos()) + " / " + entry.side().getName()).withStyle(ChatFormatting.DARK_GRAY)
               )
         );
      if (entries.size() > 4) {
         tooltip.add(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   public static ItemStack findHeldRoutingOrder(Player player) {
      ItemStack mainHand = player.getMainHandItem();
      if (mainHand.getItem() instanceof RoutingOrderItem) {
         return mainHand;
      } else {
         ItemStack offHand = player.getOffhandItem();
         return offHand.getItem() instanceof RoutingOrderItem ? offHand : ItemStack.EMPTY;
      }
   }
}
