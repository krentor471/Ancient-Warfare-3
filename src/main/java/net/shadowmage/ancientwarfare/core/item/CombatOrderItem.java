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
import net.shadowmage.ancientwarfare.core.common.npc.CombatOrderData;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.CombatOrderMenu;

public class CombatOrderItem extends Item {
   public CombatOrderItem() {
      super(new Properties().m_41487_(1));
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Player player = context.m_43723_();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         this.openCombatOrder(context.m_43725_(), player, context.m_43724_());
         return InteractionResult.m_19078_(context.m_43725_().f_46443_);
      }
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      this.openCombatOrder(level, player, hand);
      return InteractionResultHolder.m_19092_(player.m_21120_(hand), level.f_46443_);
   }

   public static boolean handleTargetedBlock(Player player, BlockPos pos) {
      ItemStack stack = findHeldCombatOrder(player);
      if (stack.m_41619_()) {
         return false;
      } else if (player.m_6144_()) {
         CombatOrderData.clear(stack);
         player.m_5661_(Component.m_237115_("guistrings.npc.combat_order.cleared"), true);
         return true;
      } else {
         if (CombatOrderData.addEntry(stack, player.m_9236_(), pos)) {
            player.m_5661_(Component.m_237110_("guistrings.npc.combat_order.added", new Object[]{WorkOrderItem.formatPos(pos)}), true);
         } else {
            player.m_5661_(Component.m_237115_("guistrings.npc.combat_order.add_failed"), true);
         }

         return true;
      }
   }

   private void openCombatOrder(Level level, Player player, InteractionHand hand) {
      if (!level.f_46443_) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new CombatOrderMenu(containerId, inventory, hand),
               Component.m_237115_("container.ancientwarfare.combat_order")
            ),
            buffer -> buffer.m_130068_(hand)
         );
      }
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      List<BlockPos> entries = CombatOrderData.getEntries(stack);
      tooltip.add(Component.m_237110_("tooltip.ancientwarfare.combat_order.entries", new Object[]{entries.size(), 32}).m_130940_(ChatFormatting.GRAY));
      entries.stream().limit(4L).forEach(pos -> tooltip.add(Component.m_237113_(WorkOrderItem.formatPos(pos)).m_130940_(ChatFormatting.DARK_GRAY)));
      if (entries.size() > 4) {
         tooltip.add(Component.m_237113_("...").m_130940_(ChatFormatting.DARK_GRAY));
      }

      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.combat_order.keybind").m_130940_(ChatFormatting.DARK_GRAY));
   }

   public static ItemStack findHeldCombatOrder(Player player) {
      ItemStack mainHand = player.m_21205_();
      if (mainHand.m_41720_() instanceof CombatOrderItem) {
         return mainHand;
      } else {
         ItemStack offHand = player.m_21206_();
         return offHand.m_41720_() instanceof CombatOrderItem ? offHand : ItemStack.f_41583_;
      }
   }
}
