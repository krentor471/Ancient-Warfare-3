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
import net.shadowmage.ancientwarfare.core.common.npc.UpkeepOrderData;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.UpkeepOrderMenu;

public class UpkeepOrderItem extends Item {
   public UpkeepOrderItem() {
      super(new Properties().stacksTo(1));
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         this.openUpkeepOrder(context.getLevel(), player, context.getHand());
         return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      this.openUpkeepOrder(level, player, hand);
      return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
   }

   public static boolean setTargetedBlock(Player player, BlockPos pos, Direction side) {
      ItemStack stack = findHeldUpkeepOrder(player);
      if (stack.isEmpty()) {
         return false;
      } else {
         if (UpkeepOrderData.setTarget(stack, player.level(), pos, side)) {
            player.displayClientMessage(Component.translatable("guistrings.npc.upkeep_order.point_set", new Object[]{WorkOrderItem.formatPos(pos), side.getName()}), true);
         } else {
            player.displayClientMessage(Component.translatable("guistrings.npc.upkeep_order.add_failed"), true);
         }

         return true;
      }
   }

   private void openUpkeepOrder(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new UpkeepOrderMenu(containerId, inventory, hand),
               Component.translatable("container.ancientwarfare.upkeep_order")
            ),
            buffer -> buffer.writeEnum(hand)
         );
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      UpkeepOrderData.UpkeepEntry entry = UpkeepOrderData.getEntry(stack).orElse(null);
      if (entry == null) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.upkeep_order.empty").withStyle(ChatFormatting.GRAY));
      } else {
         tooltip.add(Component.literal(WorkOrderItem.formatPos(entry.pos()) + " / " + entry.side().getName()).withStyle(ChatFormatting.GRAY));
         tooltip.add(Component.translatable("tooltip.ancientwarfare.upkeep_order.amount", new Object[]{entry.upkeepAmount()}).withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   public static ItemStack findHeldUpkeepOrder(Player player) {
      ItemStack mainHand = player.getMainHandItem();
      if (mainHand.getItem() instanceof UpkeepOrderItem) {
         return mainHand;
      } else {
         ItemStack offHand = player.getOffhandItem();
         return offHand.getItem() instanceof UpkeepOrderItem ? offHand : ItemStack.EMPTY;
      }
   }
}
