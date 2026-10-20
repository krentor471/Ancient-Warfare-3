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
      super(new Properties().m_41487_(1));
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

   public Component m_7626_(ItemStack stack) {
      return Component.m_237115_("item.ancientwarfare.backpack." + getLevel(stack));
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      if (!level.f_46443_) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new BackpackMenu(containerId, inventory, hand),
               Component.m_237115_("container.ancientwarfare.backpack")
            ),
            buffer -> buffer.m_130068_(hand)
         );
      }

      return InteractionResultHolder.m_19092_(stack, level.f_46443_);
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237110_("tooltip.ancientwarfare.backpack.size", new Object[]{getSlotCount(stack)}).m_130940_(ChatFormatting.GRAY));
      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.backpack.open").m_130940_(ChatFormatting.DARK_GRAY));
   }
}
