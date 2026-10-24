package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
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
import net.shadowmage.ancientwarfare.core.common.npc.TradeOrderData;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.TradeOrderMenu;

public class TradeOrderItem extends Item {
   public TradeOrderItem() {
      super(new Properties().stacksTo(1));
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         this.openTradeOrder(context.getLevel(), player, context.getHand());
         return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      this.openTradeOrder(level, player, hand);
      return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
   }

   private void openTradeOrder(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new TradeOrderMenu(containerId, inventory, hand),
               Component.translatable("container.ancientwarfare.trade_order")
            ),
            buffer -> {
               buffer.writeEnum(hand);
               buffer.writeInt(-1);
            }
         );
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      List<TradeOrderData.TradeEntry> trades = TradeOrderData.getTrades(stack);
      tooltip.add(Component.translatable("tooltip.ancientwarfare.trade_order.entries", new Object[]{trades.size(), 8}).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.ancientwarfare.trade_order.porting_note").withStyle(ChatFormatting.DARK_GRAY));
   }
}
