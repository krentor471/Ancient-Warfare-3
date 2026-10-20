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
      stack.m_41784_().m_128359_("aw_town_template", template);
      return stack;
   }

   public static String template(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128441_("aw_town_template") ? tag.m_128461_("aw_town_template") : AwtTemplateIndex.defaultTemplate();
   }

   public Component m_7626_(ItemStack stack) {
      String template = template(stack);
      if (template.isBlank()) {
         return Component.m_237115_(this.m_5671_(stack));
      } else {
         int slash = template.lastIndexOf(47);
         String fileName = slash >= 0 ? template.substring(slash + 1) : template;
         return Component.m_237113_(fileName.endsWith(".awt") ? fileName.substring(0, fileName.length() - 4) : fileName);
      }
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      String template = template(stack);
      if (!template.isBlank()) {
         tooltip.add(Component.m_237113_(template).m_130940_(ChatFormatting.DARK_GRAY));
      }
   }
}
