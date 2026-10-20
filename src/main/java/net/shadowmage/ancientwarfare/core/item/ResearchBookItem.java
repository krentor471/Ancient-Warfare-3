package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import java.util.Set;
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
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.research.ResearchBookData;
import net.shadowmage.ancientwarfare.core.menu.ResearchBookMenu;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;

public class ResearchBookItem extends Item {
   public static final String RESEARCHER_NAME_TAG = "researcherName";
   public static final String CURRENT_RESEARCH_TAG = "currentResearch";
   public static final String CURRENT_PROGRESS_TAG = "currentProgress";
   public static final String COMPLETED_RESEARCH_TAG = "completedResearch";
   public static final String QUEUED_RESEARCH_TAG = "queuedResearch";
   private static final int MAX_RESEARCHER_NAME_LENGTH = 32;

   public ResearchBookItem() {
      super(new Properties().m_41487_(1));
   }

   @Nullable
   public static String getResearcherName(ItemStack stack) {
      return ResearchBookData.getResearcherName(stack);
   }

   public static List<String> getQueuedResearch(ItemStack stack) {
      return ResearchBookData.getQueuedResearch(stack);
   }

   public static boolean queueResearch(ItemStack stack, String id) {
      return ResearchBookData.queueResearch(stack, id);
   }

   public static boolean removeQueuedResearch(ItemStack stack, String id) {
      return ResearchBookData.removeQueuedResearch(stack, id);
   }

   @Nullable
   public static String peekNextQueuedResearch(ItemStack stack) {
      List<String> queued = getQueuedResearch(stack);
      return queued.isEmpty() ? null : queued.get(0);
   }

   public static boolean startNextQueuedResearch(ItemStack stack) {
      return ResearchBookData.startNextQueuedResearch(stack);
   }

   private static void setQueuedResearch(ItemStack stack, List<String> queued) {
      ResearchBookData.setQueuedResearch(stack, queued);
   }

   public static boolean hasResearcherName(ItemStack stack) {
      return ResearchBookData.hasResearcherName(stack);
   }

   @Nullable
   public static String getCurrentResearch(ItemStack stack) {
      return ResearchBookData.getCurrentResearch(stack);
   }

   public static int getCurrentProgress(ItemStack stack) {
      return ResearchBookData.getCurrentProgress(stack);
   }

   public static Set<String> getCompletedResearch(ItemStack stack) {
      return ResearchBookData.getCompletedResearch(stack);
   }

   public static void setCurrentResearch(ItemStack stack, String id) {
      ResearchBookData.setCurrentResearch(stack, id);
   }

   public static void addProgress(ItemStack stack, int amount) {
      ResearchBookData.addProgress(stack, amount);
   }

   public static void completeResearch(ItemStack stack, String id) {
      ResearchBookData.completeResearch(stack, id);
   }

   @Nullable
   public static ResearchGoal findNextResearch(ItemStack stack) {
      return ResearchBookData.findNextResearch(stack);
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      if (!level.f_46443_) {
         if (hasResearcherName(stack) && !player.m_6144_()) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider(
                  (containerId, inventory, openedPlayer) -> new ResearchBookMenu(containerId, inventory),
                  Component.m_237115_("item.ancientwarfare.research_book")
               )
            );
         } else {
            ResearchBookData.setResearcherName(stack, player.m_7755_().getString());
            player.m_5661_(Component.m_237115_("guistrings.research.researcher_set"), true);
         }
      }

      return InteractionResultHolder.m_19092_(stack, level.f_46443_);
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      String researcherName = getResearcherName(stack);
      if (researcherName == null) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.research_book.researcher_none").m_130940_(ChatFormatting.GRAY));
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.research_book.assign").m_130940_(ChatFormatting.DARK_GRAY));
      } else {
         tooltip.add(Component.m_237110_("tooltip.ancientwarfare.research_book.researcher", new Object[]{researcherName}).m_130940_(ChatFormatting.GRAY));
         String current = getCurrentResearch(stack);
         if (current != null && ResearchRegistry.get(current) != null) {
            ResearchGoal goal = ResearchRegistry.get(current);
            tooltip.add(
               Component.m_237110_("tooltip.ancientwarfare.research_book.current", new Object[]{goal.translatedName(), getCurrentProgress(stack), goal.time()})
                  .m_130940_(ChatFormatting.GRAY)
            );
         }

         tooltip.add(
            Component.m_237110_("tooltip.ancientwarfare.research_book.queued", new Object[]{getQueuedResearch(stack).size()}).m_130940_(ChatFormatting.GRAY)
         );
         tooltip.add(
            Component.m_237110_("tooltip.ancientwarfare.research_book.completed", new Object[]{getCompletedResearch(stack).size()})
               .m_130940_(ChatFormatting.GRAY)
         );
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.research_book.view").m_130940_(ChatFormatting.DARK_GRAY));
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.research_book.reassign").m_130940_(ChatFormatting.DARK_GRAY));
      }
   }

   @Nullable
   private static String sanitizeResearcherName(String rawName) {
      return ResearchBookData.sanitizeResearcherName(rawName);
   }
}
