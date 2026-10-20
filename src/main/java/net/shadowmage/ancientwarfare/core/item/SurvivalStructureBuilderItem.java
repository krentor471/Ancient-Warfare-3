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
      super(block, properties.stacksTo(1));
   }

   public static String templateName(ItemStack stack) {
      return stack.hasTag() ? stack.getTag().getString("TemplateName") : "";
   }

   public static int phase(ItemStack stack) {
      return stack.hasTag() ? stack.getTag().getInt("BuildPhase") : 0;
   }

   public static int cursor(ItemStack stack) {
      return stack.hasTag() ? stack.getTag().getInt("BuildCursor") : 0;
   }

   public static int storedWorkEnergy(ItemStack stack) {
      return stack.hasTag() ? stack.getTag().getInt("StoredWorkEnergy") : 0;
   }

   public static void writeProgress(ItemStack stack, String template, int phase, int cursor, int energy) {
      CompoundTag tag = stack.getOrCreateTag();
      tag.putString("TemplateName", template == null ? "" : template);
      tag.putInt("BuildPhase", Math.max(0, phase));
      tag.putInt("BuildCursor", Math.max(0, cursor));
      tag.putInt("StoredWorkEnergy", Math.max(0, energy));
   }

   public static ItemStack forAwsTemplate(String templatePath, String displayName) {
      ItemStack stack = new ItemStack(AWCoreRegistryAccess.block(AWCoreIds.STRUCTURE_BUILDER_TICKED));
      writeProgress(stack, "aws:" + templatePath, 0, 0, 0);
      if (displayName != null && !displayName.isBlank()) {
         stack.setHoverName(Component.translatable("block.ancientwarfare.structure_builder_ticked").append(": ").append(Component.literal(displayName)));
      }

      return stack;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      String name = templateName(stack);
      if (!name.isBlank()) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.structure_builder.template", new Object[]{name}));
      }

      if (phase(stack) > 0 || cursor(stack) > 0) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.structure_builder.progress_saved"));
      }
   }
}
