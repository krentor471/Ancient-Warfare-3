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
      if (playerInventory.player.level().getBlockEntity(pos) instanceof ResearchStationBlockEntity researchStation) {
         this.container = researchStation;
      } else {
         this.container = new SimpleContainer(10);
      }

      this.container.startOpen(playerInventory.player);
      this.addSlot(new ResearchBookSlot(this.container, 0, 8, 22));
      int yBase = 98;
      int slotNum = 1;

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            this.addSlot(new Slot(this.container, slotNum++, 26 + col * 18, yBase + row * 18));
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

   /** Science gathered in the book, and what the technology on the desk costs (0 when it is being studied already). */
   public int getScience() {
      return net.shadowmage.ancientwarfare.core.common.research.ResearchBookData.getScience(this.getResearchBook());
   }

   public BlockPos pos() {
      return this.pos;
   }

   public int getNeededScience() {
      ResearchGoal goal = this.getDisplayGoal();
      return goal == null || goal == this.getCurrentGoal() ? 0 : goal.science();
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

   public ItemStack book() {
      return this.getResearchBook();
   }

   private ItemStack getResearchBook() {
      return this.container.getItem(0);
   }

   private void addPlayerInventory(Inventory playerInventory) {
      int top = 156;

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, top + 4 + 54));
      }

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
         }
      }
   }

   public boolean clickMenuButton(Player player, int id) {
      ItemStack book = this.getResearchBook();
      if (book.isEmpty() || ResearchBookItem.getResearcherName(book) == null) {
         return false;
      } else if (id >= 100) {
         int queueIndex = id - 100;
         List<String> queued = ResearchBookItem.getQueuedResearch(book);
         if (queueIndex >= 0 && queueIndex < queued.size()) {
            ResearchBookItem.removeQueuedResearch(book, queued.get(queueIndex));
            this.container.setChanged();
            return true;
         } else {
            return false;
         }
      } else {
         ResearchGoal goal = ResearchRegistry.getByIndex(id);
         if (goal != null) {
            boolean changed = ResearchBookItem.queueResearch(book, goal.id());
            if (changed) {
               this.container.setChanged();
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
      if (book.isEmpty()) {
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

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.RESEARCH_STATION));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack result = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot.hasItem()) {
         ItemStack stack = slot.getItem();
         result = stack.copy();
         if (index < 10) {
            if (!this.moveItemStackTo(stack, 10, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (AWCoreRegistryAccess.is(stack, AWCoreIds.RESEARCH_BOOK)) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 1, 10, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return result;
   }

   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);
   }
}
