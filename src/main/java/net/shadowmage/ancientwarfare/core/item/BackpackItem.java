package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.backpack.BackpackData;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.BackpackMenu;

public class BackpackItem extends Item {
   public static final String LEVEL_TAG = "backpackLevel";
   public static final String ITEMS_TAG = "backpackItems";
   public static final int MAX_LEVEL = 3;

   public BackpackItem() {
      super(new Properties().stacksTo(1));
   }

   public static int getLevel(ItemStack stack) {
      return BackpackData.getLevel(stack);
   }

   public static void setLevel(ItemStack stack, int level) {
      BackpackData.setLevel(stack, level);
   }

   public static ItemStack create(int level) {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.BACKPACK);
      setLevel(stack, level);
      return stack;
   }

   public static int getSlotCount(ItemStack stack) {
      return BackpackData.getSlotCount(stack);
   }

   public Component getName(ItemStack stack) {
      return Component.translatable("item.ancientwarfare.backpack." + getLevel(stack));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new BackpackMenu(containerId, inventory, hand),
               Component.translatable("container.ancientwarfare.backpack")
            ),
            buffer -> buffer.writeEnum(hand)
         );
      }

      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.ancientwarfare.backpack.size", new Object[]{getSlotCount(stack)}).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.ancientwarfare.backpack.open").withStyle(ChatFormatting.DARK_GRAY));
   }
}
