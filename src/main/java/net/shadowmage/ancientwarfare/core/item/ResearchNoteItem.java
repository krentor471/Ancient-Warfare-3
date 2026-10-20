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
      stack.m_41784_().m_128359_("researchName", researchId);
      return stack;
   }

   @Nullable
   public static String getResearchId(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      if (tag != null && tag.m_128441_("researchName")) {
         String id = tag.m_128461_("researchName");
         return ResearchRegistry.get(id) == null ? null : id;
      } else {
         return null;
      }
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      if (!level.f_46443_) {
         String researchId = getResearchId(stack);
         ResearchGoal goal = researchId == null ? null : ResearchRegistry.get(researchId);
         ItemStack book = findResearchBook(player.m_150109_());
         if (goal == null) {
            player.m_5661_(Component.m_237115_("guistrings.research.note_invalid"), true);
         } else if (book.m_41619_()) {
            player.m_5661_(Component.m_237115_("guistrings.research.note_no_book"), true);
         } else if (!ResearchBookItem.getCompletedResearch(book).contains(researchId)) {
            ResearchBookItem.completeResearch(book, researchId);
            stack.m_41774_(1);
            player.m_5661_(Component.m_237110_("guistrings.research.learned_from_item", new Object[]{goal.translatedName()}), true);
         } else if (ResearchBookItem.getCurrentResearch(book) != null) {
            ResearchBookItem.addProgress(book, Math.max(1, goal.time() / 4));
            stack.m_41774_(1);
            player.m_5661_(Component.m_237110_("guistrings.research.added_progress", new Object[]{goal.translatedName()}), true);
         } else {
            player.m_5661_(Component.m_237115_("guistrings.research.note_no_active_research"), true);
         }
      }

      return InteractionResultHolder.m_19092_(stack, level.f_46443_);
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      String researchId = getResearchId(stack);
      ResearchGoal goal = researchId == null ? null : ResearchRegistry.get(researchId);
      if (goal == null) {
         tooltip.add(Component.m_237115_("guistrings.research.note_invalid").m_130940_(ChatFormatting.RED));
      } else {
         tooltip.add(goal.translatedName().m_6881_().m_130940_(ChatFormatting.GRAY));
         tooltip.add(Component.m_237115_("guistrings.research.click_to_learn").m_130940_(ChatFormatting.DARK_GRAY));
      }
   }

   private static ItemStack findResearchBook(Inventory inventory) {
      for (int i = 0; i < inventory.m_6643_(); i++) {
         ItemStack stack = inventory.m_8020_(i);
         if (AWCoreRegistryAccess.is(stack, AWCoreIds.RESEARCH_BOOK)) {
            return stack;
         }
      }

      return ItemStack.f_41583_;
   }
}
