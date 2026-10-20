package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;

public class ResearchNoteItem extends Item {
   public static final String RESEARCH_NAME_TAG = "researchName";

   public ResearchNoteItem() {
      super(new Properties());
   }

   public static ItemStack create(String researchId) {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.RESEARCH_NOTE);
      stack.getOrCreateTag().putString("researchName", researchId);
      return stack;
   }

   @Nullable
   public static String getResearchId(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains("researchName")) {
         String id = tag.getString("researchName");
         return ResearchRegistry.get(id) == null ? null : id;
      } else {
         return null;
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide) {
         String researchId = getResearchId(stack);
         ResearchGoal goal = researchId == null ? null : ResearchRegistry.get(researchId);
         ItemStack book = findResearchBook(player.getInventory());
         if (goal == null) {
            player.displayClientMessage(Component.translatable("guistrings.research.note_invalid"), true);
         } else if (book.isEmpty()) {
            player.displayClientMessage(Component.translatable("guistrings.research.note_no_book"), true);
         } else if (!ResearchBookItem.getCompletedResearch(book).contains(researchId)) {
            ResearchBookItem.completeResearch(book, researchId);
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("guistrings.research.learned_from_item", new Object[]{goal.translatedName()}), true);
         } else if (ResearchBookItem.getCurrentResearch(book) != null) {
            ResearchBookItem.addProgress(book, Math.max(1, goal.time() / 4));
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("guistrings.research.added_progress", new Object[]{goal.translatedName()}), true);
         } else {
            player.displayClientMessage(Component.translatable("guistrings.research.note_no_active_research"), true);
         }
      }

      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      String researchId = getResearchId(stack);
      ResearchGoal goal = researchId == null ? null : ResearchRegistry.get(researchId);
      if (goal == null) {
         tooltip.add(Component.translatable("guistrings.research.note_invalid").withStyle(ChatFormatting.RED));
      } else {
         tooltip.add(goal.translatedName().copy().withStyle(ChatFormatting.GRAY));
         tooltip.add(Component.translatable("guistrings.research.click_to_learn").withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   private static ItemStack findResearchBook(Inventory inventory) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (AWCoreRegistryAccess.is(stack, AWCoreIds.RESEARCH_BOOK)) {
            return stack;
         }
      }

      return ItemStack.EMPTY;
   }
}
