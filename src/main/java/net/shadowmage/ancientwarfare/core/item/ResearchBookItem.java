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
      super(new Properties().stacksTo(1));
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

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide) {
         if (hasResearcherName(stack) && !player.isShiftKeyDown()) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider(
                  (containerId, inventory, openedPlayer) -> new ResearchBookMenu(containerId, inventory),
                  Component.translatable("item.ancientwarfare.research_book")
               )
            );
         } else {
            ResearchBookData.setResearcherName(stack, player.getName().getString());
            player.displayClientMessage(Component.translatable("guistrings.research.researcher_set"), true);
         }
      }

      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   /** Used on a town hall: the book serves that town's country, so everyone in it shares what it teaches. */
   @Override
   public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
      Level level = context.getLevel();
      if (!(level.getBlockEntity(context.getClickedPos()) instanceof net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity hall) || context.getPlayer() == null) {
         return net.minecraft.world.InteractionResult.PASS;
      }

      if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel server) {
         Player player = context.getPlayer();
         java.util.Optional<java.util.UUID> owner = hall.getOwner();
         if (owner.isEmpty()) {
            return net.minecraft.world.InteractionResult.CONSUME;
         }

         java.util.UUID founder = net.shadowmage.ancientwarfare.core.faction.CountryMembers.countryOf(server, owner.get());
         java.util.UUID mine = net.shadowmage.ancientwarfare.core.faction.CountryMembers.countryOf(server, player.getUUID());
         if (!founder.equals(mine)) {
            player.displayClientMessage(Component.translatable("guistrings.research.not_your_country").withStyle(ChatFormatting.RED), true);
            return net.minecraft.world.InteractionResult.CONSUME;
         }

         net.minecraft.server.players.GameProfileCache cache = server.getServer().getProfileCache();
         String name = cache == null ? null : cache.get(founder).map(com.mojang.authlib.GameProfile::getName).orElse(null);
         if (name != null) {
            ResearchBookData.setResearcherName(context.getItemInHand(), name);
            player.displayClientMessage(Component.translatable("guistrings.research.country_set", name), true);
         }
      }

      return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
   }

   /** Used on another player: the book is theirs, for their own research. */
   @Override
   public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, Player player, net.minecraft.world.entity.LivingEntity target, InteractionHand hand) {
      if (!(target instanceof Player other)) {
         return net.minecraft.world.InteractionResult.PASS;
      }

      if (!player.level().isClientSide) {
         ItemStack held = player.getItemInHand(hand);
         ResearchBookData.setResearcherName(held, other.getName().getString());
         player.displayClientMessage(Component.translatable("guistrings.research.researcher_other", other.getName()), true);
      }

      return net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      String researcherName = getResearcherName(stack);
      if (researcherName == null) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.research_book.researcher_none").withStyle(ChatFormatting.GRAY));
         tooltip.add(Component.translatable("tooltip.ancientwarfare.research_book.assign").withStyle(ChatFormatting.DARK_GRAY));
      } else {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.research_book.researcher", new Object[]{researcherName}).withStyle(ChatFormatting.GRAY));
         String current = getCurrentResearch(stack);
         if (current != null && ResearchRegistry.get(current) != null) {
            ResearchGoal goal = ResearchRegistry.get(current);
            tooltip.add(
               Component.translatable("tooltip.ancientwarfare.research_book.current", new Object[]{goal.translatedName(), getCurrentProgress(stack), goal.time()})
                  .withStyle(ChatFormatting.GRAY)
            );
         }

         tooltip.add(
            Component.translatable("tooltip.ancientwarfare.research_book.queued", new Object[]{getQueuedResearch(stack).size()}).withStyle(ChatFormatting.GRAY)
         );
         tooltip.add(
            Component.translatable("guistrings.research.science", ResearchBookData.getScience(stack)).withStyle(ChatFormatting.AQUA)
         );
         tooltip.add(
            Component.translatable("tooltip.ancientwarfare.research_book.completed", new Object[]{getCompletedResearch(stack).size()})
               .withStyle(ChatFormatting.GRAY)
         );
         tooltip.add(Component.translatable("tooltip.ancientwarfare.research_book.view").withStyle(ChatFormatting.DARK_GRAY));
         tooltip.add(Component.translatable("tooltip.ancientwarfare.research_book.reassign").withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   @Nullable
   private static String sanitizeResearcherName(String rawName) {
      return ResearchBookData.sanitizeResearcherName(rawName);
   }
}
