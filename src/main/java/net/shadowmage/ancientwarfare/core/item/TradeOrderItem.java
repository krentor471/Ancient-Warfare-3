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
      super(new Properties().m_41487_(1));
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Player player = context.m_43723_();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         this.openTradeOrder(context.m_43725_(), player, context.m_43724_());
         return InteractionResult.m_19078_(context.m_43725_().f_46443_);
      }
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      this.openTradeOrder(level, player, hand);
      return InteractionResultHolder.m_19092_(player.m_21120_(hand), level.f_46443_);
   }

   private void openTradeOrder(Level level, Player player, InteractionHand hand) {
      if (!level.f_46443_) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new TradeOrderMenu(containerId, inventory, hand),
               Component.m_237115_("container.ancientwarfare.trade_order")
            ),
            buffer -> {
               buffer.m_130068_(hand);
               buffer.writeInt(-1);
            }
         );
      }
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      List<TradeOrderData.TradeEntry> trades = TradeOrderData.getTrades(stack);
      tooltip.add(Component.m_237110_("tooltip.ancientwarfare.trade_order.entries", new Object[]{trades.size(), 8}).m_130940_(ChatFormatting.GRAY));
      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.trade_order.porting_note").m_130940_(ChatFormatting.DARK_GRAY));
   }
}
