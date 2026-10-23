package net.shadowmage.ancientwarfare.core.common.research;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;

public final class ResearchBookData {
   public static final String RESEARCHER_NAME_TAG = "researcherName";
   public static final String CURRENT_RESEARCH_TAG = "currentResearch";
   public static final String CURRENT_PROGRESS_TAG = "currentProgress";
   public static final String COMPLETED_RESEARCH_TAG = "completedResearch";
   public static final String QUEUED_RESEARCH_TAG = "queuedResearch";
   /** Science the scribes have gathered for this book and not yet spent. */
   public static final String SCIENCE_TAG = "science";
   public static final int MAX_RESEARCHER_NAME_LENGTH = 32;

   private ResearchBookData() {
   }

   @Nullable
   public static String getResearcherName(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("researcherName") ? sanitizeResearcherName(tag.getString("researcherName")) : null;
   }

   public static void setResearcherName(ItemStack stack, String name) {
      stack.getOrCreateTag().putString("researcherName", name);
   }

   public static boolean hasResearcherName(ItemStack stack) {
      return getResearcherName(stack) != null;
   }

   public static List<String> getQueuedResearch(ItemStack stack) {
      ArrayList<String> queued = new ArrayList<>();
      CompoundTag tag = stack.getTag();
      if (tag == null) {
         return queued;
      } else {
         ListTag list = tag.getList("queuedResearch", 8);

         for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (ResearchRegistry.get(id) != null) {
               queued.add(id);
            }
         }

         return queued;
      }
   }

   public static boolean queueResearch(ItemStack stack, String id) {
      ResearchGoal goal = ResearchRegistry.get(id);
      if (goal == null || getCurrentResearch(stack) != null || getCompletedResearch(stack).contains(id)) {
         return false;
      } else if (!goal.canResearch(getCompletedResearch(stack))) {
         return false;
      } else {
         List<String> queued = getQueuedResearch(stack);
         if (queued.contains(id)) {
            return true;
         } else {
            queued.add(id);
            setQueuedResearch(stack, queued);
            return true;
         }
      }
   }

   public static boolean removeQueuedResearch(ItemStack stack, String id) {
      List<String> queued = getQueuedResearch(stack);
      boolean removed = queued.remove(id);
      if (removed) {
         setQueuedResearch(stack, queued);
      }

      return removed;
   }

   @Nullable
   public static String peekNextQueuedResearch(ItemStack stack) {
      List<String> queued = getQueuedResearch(stack);
      return queued.isEmpty() ? null : queued.get(0);
   }

   public static boolean startNextQueuedResearch(ItemStack stack) {
      if (getCurrentResearch(stack) != null) {
         return false;
      } else {
         List<String> queued = getQueuedResearch(stack);

         while (!queued.isEmpty()) {
            String id = queued.remove(0);
            ResearchGoal goal = ResearchRegistry.get(id);
            if (goal != null && goal.canResearch(getCompletedResearch(stack))) {
               setQueuedResearch(stack, queued);
               setCurrentResearch(stack, id);
               return true;
            }
         }

         setQueuedResearch(stack, queued);
         return false;
      }
   }

   public static void setQueuedResearch(ItemStack stack, List<String> queued) {
      ListTag list = new ListTag();

      for (String id : queued) {
         if (ResearchRegistry.get(id) != null) {
            list.add(StringTag.valueOf(id));
         }
      }

      stack.getOrCreateTag().put("queuedResearch", list);
   }

   @Nullable
   public static String getCurrentResearch(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains("currentResearch")) {
         String id = tag.getString("currentResearch");
         return ResearchRegistry.get(id) == null ? null : id;
      } else {
         return null;
      }
   }

   public static int getScience(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag == null ? 0 : Math.max(0, tag.getInt(SCIENCE_TAG));
   }

   public static void addScience(ItemStack stack, int amount) {
      stack.getOrCreateTag().putInt(SCIENCE_TAG, getScience(stack) + Math.max(0, amount));
   }

   public static boolean takeScience(ItemStack stack, int amount) {
      int science = getScience(stack);
      if (science < amount) {
         return false;
      }

      stack.getOrCreateTag().putInt(SCIENCE_TAG, science - amount);
      return true;
   }

   public static int getCurrentProgress(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag == null ? 0 : Math.max(0, tag.getInt("currentProgress"));
   }

   public static Set<String> getCompletedResearch(ItemStack stack) {
      Set<String> completed = new HashSet<>();
      CompoundTag tag = stack.getTag();
      if (tag == null) {
         return completed;
      } else {
         ListTag list = tag.getList("completedResearch", 8);

         for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (ResearchRegistry.get(id) != null) {
               completed.add(id);
            }
         }

         return completed;
      }
   }

   public static void setCurrentResearch(ItemStack stack, String id) {
      CompoundTag tag = stack.getOrCreateTag();
      tag.putString("currentResearch", id);
      tag.putInt("currentProgress", 0);
   }

   public static void addProgress(ItemStack stack, int amount) {
      String current = getCurrentResearch(stack);
      if (current != null) {
         ResearchGoal goal = ResearchRegistry.get(current);
         if (goal != null) {
            CompoundTag tag = stack.getOrCreateTag();
            int progress = Math.min(goal.time(), getCurrentProgress(stack) + amount);
            if (progress >= goal.time()) {
               completeResearch(stack, current);
            } else {
               tag.putInt("currentProgress", progress);
            }
         }
      }
   }

   public static void completeResearch(ItemStack stack, String id) {
      CompoundTag tag = stack.getOrCreateTag();
      Set<String> completed = getCompletedResearch(stack);
      completed.add(id);
      ListTag list = new ListTag();

      for (String research : completed) {
         list.add(StringTag.valueOf(research));
      }

      tag.put("completedResearch", list);
      tag.remove("currentResearch");
      tag.remove("currentProgress");
   }

   @Nullable
   public static ResearchGoal findNextResearch(ItemStack stack) {
      Set<String> completed = getCompletedResearch(stack);

      for (ResearchGoal goal : ResearchRegistry.all()) {
         if (goal.canResearch(completed)) {
            return goal;
         }
      }

      return null;
   }

   @Nullable
   public static String sanitizeResearcherName(String rawName) {
      String name = rawName == null ? "" : rawName.trim();
      return !name.isEmpty() && name.length() <= 32 && !looksLikeSerializedProfile(name) ? name : null;
   }

   private static boolean looksLikeSerializedProfile(String name) {
      return name.indexOf(123) >= 0 || name.indexOf(125) >= 0 || name.contains("GameProfile") || name.contains("uuid") || name.contains("id=");
   }
}
