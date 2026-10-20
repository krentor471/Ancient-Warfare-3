package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import org.jetbrains.annotations.Nullable;

public class SurvivalStructureBuilderItem extends BlockItem {
   private static final String TEMPLATE_TAG = "TemplateName";
   private static final String PHASE_TAG = "BuildPhase";
   private static final String CURSOR_TAG = "BuildCursor";
   private static final String ENERGY_TAG = "StoredWorkEnergy";

   public SurvivalStructureBuilderItem(Block block, Properties properties) {
      super(block, properties.m_41487_(1));
   }

   public static String templateName(ItemStack stack) {
      return stack.m_41782_() ? stack.m_41783_().m_128461_("TemplateName") : "";
   }

   public static int phase(ItemStack stack) {
      return stack.m_41782_() ? stack.m_41783_().m_128451_("BuildPhase") : 0;
   }

   public static int cursor(ItemStack stack) {
      return stack.m_41782_() ? stack.m_41783_().m_128451_("BuildCursor") : 0;
   }

   public static int storedWorkEnergy(ItemStack stack) {
      return stack.m_41782_() ? stack.m_41783_().m_128451_("StoredWorkEnergy") : 0;
   }

   public static void writeProgress(ItemStack stack, String template, int phase, int cursor, int energy) {
      CompoundTag tag = stack.m_41784_();
      tag.m_128359_("TemplateName", template == null ? "" : template);
      tag.m_128405_("BuildPhase", Math.max(0, phase));
      tag.m_128405_("BuildCursor", Math.max(0, cursor));
      tag.m_128405_("StoredWorkEnergy", Math.max(0, energy));
   }

   public static ItemStack forAwsTemplate(String templatePath, String displayName) {
      ItemStack stack = new ItemStack(AWCoreRegistryAccess.block(AWCoreIds.STRUCTURE_BUILDER_TICKED));
      writeProgress(stack, "aws:" + templatePath, 0, 0, 0);
      if (displayName != null && !displayName.isBlank()) {
         stack.m_41714_(Component.m_237115_("block.ancientwarfare.structure_builder_ticked").m_130946_(": ").m_7220_(Component.m_237113_(displayName)));
      }

      return stack;
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      String name = templateName(stack);
      if (!name.isBlank()) {
         tooltip.add(Component.m_237110_("tooltip.ancientwarfare.structure_builder.template", new Object[]{name}));
      }

      if (phase(stack) > 0 || cursor(stack) > 0) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.structure_builder.progress_saved"));
      }
   }
}
