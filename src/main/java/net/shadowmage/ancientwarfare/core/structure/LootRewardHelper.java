package net.shadowmage.ancientwarfare.core.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.item.ResearchNoteItem;

public final class LootRewardHelper {
   private static final int RESEARCH_REROLL_ATTEMPTS = 32;
   private static final long REROLL_SEED_STEP = -7046029254386353131L;

   private LootRewardHelper() {
   }

   public static List<ItemStack> getLootWithoutDuplicateResearch(LootTable table, LootParams params, long seed, Set<String> seenResearch) {
      if (!AWCorePlatform.gameplayConfig().preventDuplicateResearchNotes()) {
         return table.m_287214_(params, seed);
      } else {
         List<ItemStack> lastResult = List.of();

         for (int attempt = 0; attempt < 32; attempt++) {
            List<ItemStack> candidate = table.m_287214_(params, seed + -7046029254386353131L * (long)attempt);
            Set<String> candidateResearch = collectResearchIds(candidate);
            if (!containsDuplicateResearch(candidate, candidateResearch) && candidateResearch.stream().noneMatch(seenResearch::contains)) {
               seenResearch.addAll(candidateResearch);
               return candidate;
            }

            lastResult = candidate;
         }

         List<ItemStack> filtered = new ArrayList<>();

         for (ItemStack stack : lastResult) {
            String researchId = researchId(stack);
            if (researchId == null || seenResearch.add(researchId)) {
               filtered.add(stack);
            }
         }

         return filtered;
      }
   }

   public static List<ItemStack> scaleItemCounts(List<ItemStack> generated, double multiplier, long seed) {
      if (!generated.isEmpty() && multiplier != 1.0) {
         RandomSource random = RandomSource.m_216335_(seed);
         List<ItemStack> scaled = new ArrayList<>();

         for (ItemStack stack : generated) {
            double exact = (double)stack.m_41613_() * Math.max(0.0, multiplier);
            long count = (long)Math.floor(exact);
            if (random.m_188500_() < exact - (double)count && count < Long.MAX_VALUE) {
               count++;
            }

            long remaining = count;

            while (remaining > 0L) {
               ItemStack split = stack.m_41777_();
               split.m_41764_((int)Math.min(remaining, (long)split.m_41741_()));
               scaled.add(split);
               remaining -= (long)split.m_41613_();
            }
         }

         return scaled;
      } else {
         return generated;
      }
   }

   private static Set<String> collectResearchIds(List<ItemStack> stacks) {
      Set<String> ids = new HashSet<>();

      for (ItemStack stack : stacks) {
         String researchId = researchId(stack);
         if (researchId != null) {
            ids.add(researchId);
         }
      }

      return ids;
   }

   private static boolean containsDuplicateResearch(List<ItemStack> stacks, Set<String> uniqueIds) {
      int researchCount = 0;

      for (ItemStack stack : stacks) {
         if (researchId(stack) != null) {
            researchCount++;
         }
      }

      return researchCount != uniqueIds.size();
   }

   private static String researchId(ItemStack stack) {
      if (!(stack.m_41720_() instanceof ResearchNoteItem)) {
         return null;
      } else {
         String researchId = ResearchNoteItem.getResearchId(stack);
         if (researchId != null) {
            return researchId;
         } else {
            return stack.m_41782_() && stack.m_41783_().m_128441_("researchName") ? stack.m_41783_().m_128461_("researchName") : null;
         }
      }
   }
}
