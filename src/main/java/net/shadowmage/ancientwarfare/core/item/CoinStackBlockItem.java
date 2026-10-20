package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.block.CoinStackBlock;
import org.jetbrains.annotations.Nullable;

public class CoinStackBlockItem extends BlockItem {
   public CoinStackBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237110_("tooltip.ancientwarfare.coin_stack.count", new Object[]{CoinStackBlock.stackSize(stack)}));
   }
}
