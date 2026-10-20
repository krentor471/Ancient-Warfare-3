package net.shadowmage.ancientwarfare.core.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.ResearchBookItem;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;
import net.shadowmage.ancientwarfare.core.tile.ResearchStationBlockEntity;

public class ResearchStationMenu extends AbstractContainerMenu {
   private static final int STATION_SLOT_COUNT = 10;
   private final Container container;
   private final BlockPos pos;

   public ResearchStationMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.RESEARCH_STATION), containerId);
      this.pos = pos;
      if (playerInventory.f_35978_.m_9236_().m_7702_(pos) instanceof ResearchStationBlockEntity researchStation) {
         this.container = researchStation;
      } else {
         this.container = new SimpleContainer(10);
      }

      this.container.m_5856_(playerInventory.f_35978_);
      this.m_38897_(new ResearchBookSlot(this.container, 0, 8, 22));
      int yBase = 98;
      int slotNum = 1;

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            this.m_38897_(new Slot(this.container, slotNum++, 26 + col * 18, yBase + row * 18));
         }
      }

      this.addPlayerInventory(playerInventory);
   }

   public String getResearcherName() {
      ItemStack book = this.getResearchBook();
      String name = ResearchBookItem.getResearcherName(book);
      return name == null ? "" : name;
   }

   public String getCurrentResearchName() {
      ResearchGoal goal = this.getDisplayGoal();
      return goal == null ? "" : goal.translatedName().getString();
   }

   public String getActiveResearchName() {
      ResearchGoal goal = this.getCurrentGoal();
      return goal == null ? "" : goal.translatedName().getString();
   }

   public int getCurrentProgress() {
      return ResearchBookItem.getCurrentProgress(this.getResearchBook());
   }

   public int getCurrentResearchTime() {
      ResearchGoal goal = this.getCurrentGoal();
      return goal == null ? 0 : goal.time();
   }

   public float getProgressRatio() {
      int total = this.getCurrentResearchTime();
      return total <= 0 ? 0.0F : Math.min(1.0F, (float)this.getCurrentProgress() / (float)total);
   }

   public List<ItemStack> getCurrentResearchResources() {
      ResearchGoal goal = this.getDisplayGoal();
      return goal == null ? List.of() : goal.resources();
   }

   private ResearchGoal getDisplayGoal() {
      ResearchGoal current = this.getCurrentGoal();
      if (current != null) {
         return current;
      } else {
         String queued = ResearchBookItem.peekNextQueuedResearch(this.getResearchBook());
         return queued == null ? null : ResearchRegistry.get(queued);
      }
   }

   private ResearchGoal getCurrentGoal() {
      String current = ResearchBookItem.getCurrentResearch(this.getResearchBook());
      return current == null ? null : ResearchRegistry.get(current);
   }

   private ItemStack getResearchBook() {
      return this.container.m_8020_(0);
   }

   private void addPlayerInventory(Inventory playerInventory) {
      int top = 156;

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(playerInventory, col, 8 + col * 18, top + 4 + 54));
      }

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
         }
      }
   }

   public boolean m_6366_(Player player, int id) {
      ItemStack book = this.getResearchBook();
      if (book.m_41619_() || ResearchBookItem.getResearcherName(book) == null) {
         return false;
      } else if (id >= 100) {
         int queueIndex = id - 100;
         List<String> queued = ResearchBookItem.getQueuedResearch(book);
         if (queueIndex >= 0 && queueIndex < queued.size()) {
            ResearchBookItem.removeQueuedResearch(book, queued.get(queueIndex));
            this.container.m_6596_();
            return true;
         } else {
            return false;
         }
      } else {
         ResearchGoal goal = ResearchRegistry.getByIndex(id);
         if (goal != null) {
            boolean changed = ResearchBookItem.queueResearch(book, goal.id());
            if (changed) {
               this.container.m_6596_();
            }

            return changed;
         } else {
            return false;
         }
      }
   }

   public List<String> getQueuedResearch() {
      return ResearchBookItem.getQueuedResearch(this.getResearchBook());
   }

   public List<ResearchGoal> getLearnableResearch() {
      ItemStack book = this.getResearchBook();
      if (book.m_41619_()) {
         return List.of();
      } else {
         List<String> queued = ResearchBookItem.getQueuedResearch(book);
         List<ResearchGoal> learnable = new ArrayList<>();

         for (ResearchGoal goal : ResearchRegistry.all()) {
            if (!queued.contains(goal.id())
               && !ResearchBookItem.getCompletedResearch(book).contains(goal.id())
               && !goal.id().equals(ResearchBookItem.getCurrentResearch(book))
               && goal.canResearch(ResearchBookItem.getCompletedResearch(book))) {
               learnable.add(goal);
            }
         }

         return learnable;
      }
   }

   public ResearchGoal getQueuedGoal(int index) {
      List<String> queued = ResearchBookItem.getQueuedResearch(this.getResearchBook());
      return index >= 0 && index < queued.size() ? ResearchRegistry.get(queued.get(index)) : null;
   }

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.RESEARCH_STATION));
   }

   public ItemStack m_7648_(Player player, int index) {
      ItemStack result = ItemStack.f_41583_;
      Slot slot = (Slot)this.f_38839_.get(index);
      if (slot.m_6657_()) {
         ItemStack stack = slot.m_7993_();
         result = stack.m_41777_();
         if (index < 10) {
            if (!this.m_38903_(stack, 10, this.f_38839_.size(), true)) {
               return ItemStack.f_41583_;
            }
         } else if (AWCoreRegistryAccess.is(stack, AWCoreIds.RESEARCH_BOOK)) {
            if (!this.m_38903_(stack, 0, 1, false)) {
               return ItemStack.f_41583_;
            }
         } else if (!this.m_38903_(stack, 1, 10, false)) {
            return ItemStack.f_41583_;
         }

         if (stack.m_41619_()) {
            slot.m_5852_(ItemStack.f_41583_);
         } else {
            slot.m_6654_();
         }
      }

      return result;
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      this.container.m_5785_(player);
   }
}
