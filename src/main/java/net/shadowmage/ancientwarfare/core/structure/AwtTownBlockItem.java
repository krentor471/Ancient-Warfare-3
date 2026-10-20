package net.shadowmage.ancientwarfare.core.structure;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class AwtTownBlockItem extends BlockItem {
   public static final String TEMPLATE_TAG = "aw_town_template";

   public AwtTownBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   public static ItemStack create(Block block, String template) {
      ItemStack stack = new ItemStack(block);
      stack.getOrCreateTag().putString("aw_town_template", template);
      return stack;
   }

   public static String template(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("aw_town_template") ? tag.getString("aw_town_template") : AwtTemplateIndex.defaultTemplate();
   }

   public Component getName(ItemStack stack) {
      String template = template(stack);
      if (template.isBlank()) {
         return Component.translatable(this.getDescriptionId(stack));
      } else {
         int slash = template.lastIndexOf(47);
         String fileName = slash >= 0 ? template.substring(slash + 1) : template;
         return Component.literal(fileName.endsWith(".awt") ? fileName.substring(0, fileName.length() - 4) : fileName);
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      String template = template(stack);
      if (!template.isBlank()) {
         tooltip.add(Component.literal(template).withStyle(ChatFormatting.DARK_GRAY));
      }
   }
}
