package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.CoinMetal;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import org.jetbrains.annotations.Nullable;

public class CoinItem extends Item {
   private static final String METAL_TAG = "metal";

   public CoinItem() {
      super(new Properties());
   }

   public static ItemStack create(CoinMetal metal) {
      ItemStack stack = new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.COIN));
      setMetal(stack, metal);
      return stack;
   }

   public static ItemStack create(CoinMetal metal, int count) {
      ItemStack stack = create(metal);
      stack.setCount(Math.max(1, count));
      return stack;
   }

   public static CoinMetal getMetal(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag == null ? CoinMetal.COPPER : CoinMetal.byId(tag.getString("metal"));
   }

   public static void setMetal(ItemStack stack, CoinMetal metal) {
      stack.getOrCreateTag().putString("metal", metal.id());
   }

   public Component getName(ItemStack stack) {
      return Component.translatable("item.ancientwarfare.coin." + getMetal(stack).id());
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      if (getMetal(stack) == CoinMetal.ANCIENT) {
         tooltip.add(
            Component.translatable("item.ancientwarfare.coin.ancient.tooltip").withStyle(new ChatFormatting[]{ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC})
         );
      }
   }
}
