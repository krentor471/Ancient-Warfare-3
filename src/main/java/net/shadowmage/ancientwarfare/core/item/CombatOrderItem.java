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
      super(new Properties().stacksTo(1));
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      } else {
         this.openCombatOrder(context.getLevel(), player, context.getHand());
         return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      this.openCombatOrder(level, player, hand);
      return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
   }

   public static boolean handleTargetedBlock(Player player, BlockPos pos) {
      ItemStack stack = findHeldCombatOrder(player);
      if (stack.isEmpty()) {
         return false;
      } else if (player.isShiftKeyDown()) {
         CombatOrderData.clear(stack);
         player.displayClientMessage(Component.translatable("guistrings.npc.combat_order.cleared"), true);
         return true;
      } else {
         if (CombatOrderData.addEntry(stack, player.level(), pos)) {
            player.displayClientMessage(Component.translatable("guistrings.npc.combat_order.added", new Object[]{WorkOrderItem.formatPos(pos)}), true);
         } else {
            player.displayClientMessage(Component.translatable("guistrings.npc.combat_order.add_failed"), true);
         }

         return true;
      }
   }

   private void openCombatOrder(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new CombatOrderMenu(containerId, inventory, hand),
               Component.translatable("container.ancientwarfare.combat_order")
            ),
            buffer -> buffer.writeEnum(hand)
         );
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      List<BlockPos> entries = CombatOrderData.getEntries(stack);
      tooltip.add(Component.translatable("tooltip.ancientwarfare.combat_order.entries", new Object[]{entries.size(), 32}).withStyle(ChatFormatting.GRAY));
      entries.stream().limit(4L).forEach(pos -> tooltip.add(Component.literal(WorkOrderItem.formatPos(pos)).withStyle(ChatFormatting.DARK_GRAY)));
      if (entries.size() > 4) {
         tooltip.add(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
      }

      tooltip.add(Component.translatable("tooltip.ancientwarfare.combat_order.keybind").withStyle(ChatFormatting.DARK_GRAY));
   }

   public static ItemStack findHeldCombatOrder(Player player) {
      ItemStack mainHand = player.getMainHandItem();
      if (mainHand.getItem() instanceof CombatOrderItem) {
         return mainHand;
      } else {
         ItemStack offHand = player.getOffhandItem();
         return offHand.getItem() instanceof CombatOrderItem ? offHand : ItemStack.EMPTY;
      }
   }
}
