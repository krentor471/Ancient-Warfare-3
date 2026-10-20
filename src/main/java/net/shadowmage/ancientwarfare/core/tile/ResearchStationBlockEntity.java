package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.ResearchStationBlock;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.research.ResearchResource;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.item.ResearchBookItem;
import net.shadowmage.ancientwarfare.core.menu.ResearchStationMenu;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;
import org.jetbrains.annotations.Nullable;

public class ResearchStationBlockEntity extends BlockEntity implements Container, MenuProvider, WorkSite {
   public static final int BOOK_SLOT = 0;
   public static final int RESOURCE_SLOT_START = 1;
   public static final int SLOT_COUNT = 10;
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(10, ItemStack.f_41583_);

   public ResearchStationBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.RESEARCH_STATION), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchStationBlockEntity station) {
      station.tickResearch();
   }

   private void tickResearch() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         ItemStack book = (ItemStack)this.items.get(0);
         if (!book.m_41619_() && ResearchBookItem.getResearcherName(book) != null) {
            if (ResearchBookItem.getCurrentResearch(book) == null) {
               this.startNextQueuedResearch(book);
            }

            if (ResearchBookItem.getCurrentResearch(book) != null) {
               ResearchBookItem.addProgress(book, 1);
               this.m_6596_();
            }
         }
      }
   }

   private void startNextQueuedResearch(ItemStack book) {
      String nextId = ResearchBookItem.peekNextQueuedResearch(book);
      ResearchGoal next = nextId == null ? null : ResearchRegistry.get(nextId);
      if (next != null && this.consumeResearchResources(next)) {
         ResearchBookItem.startNextQueuedResearch(book);
         this.m_6596_();
      }
   }

   private boolean consumeResearchResources(ResearchGoal goal) {
      for (ResearchResource requirement : goal.resourceRequirements()) {
         int available = 0;

         for (int slot = 1; slot < 10; slot++) {
            ItemStack stack = (ItemStack)this.items.get(slot);
            if (requirement.matches(stack)) {
               available += stack.m_41613_();
            }
         }

         if (available < requirement.count()) {
            return false;
         }
      }

      for (ResearchResource requirement : goal.resourceRequirements()) {
         int remaining = requirement.count();

         for (int slotx = 1; slotx < 10 && remaining > 0; slotx++) {
            ItemStack stack = (ItemStack)this.items.get(slotx);
            if (requirement.matches(stack)) {
               int removed = Math.min(remaining, stack.m_41613_());
               stack.m_41774_(removed);
               remaining -= removed;
               if (stack.m_41619_()) {
                  this.items.set(slotx, ItemStack.f_41583_);
               }
            }
         }
      }

      this.m_6596_();
      return true;
   }

   @Override
   public boolean hasWork() {
      ItemStack book = (ItemStack)this.items.get(0);
      return !book.m_41619_() && ResearchBookItem.getResearcherName(book) != null
         ? ResearchBookItem.getCurrentResearch(book) != null || ResearchBookItem.peekNextQueuedResearch(book) != null
         : false;
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && worker.canWorkAt(this.getWorkType())) {
         ItemStack book = (ItemStack)this.items.get(0);
         if (!book.m_41619_() && ResearchBookItem.getResearcherName(book) != null) {
            if (ResearchBookItem.getCurrentResearch(book) == null) {
               this.startNextQueuedResearch(book);
            }

            if (ResearchBookItem.getCurrentResearch(book) != null) {
               ResearchBookItem.addProgress(book, Math.max(1, Math.round(worker.getWorkEffectiveness(this.getWorkType()))));
               this.m_6596_();
            }
         }
      }
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.RESEARCH;
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      ContainerHelper.m_18980_(tag, this.items);
      this.updateBookState();
   }

   public int m_6643_() {
      return 10;
   }

   public boolean m_7983_() {
      return this.items.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int slot) {
      return this.isValidSlot(slot) ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      if (this.isValidSlot(slot) && amount > 0) {
         ItemStack removed = ContainerHelper.m_18969_(this.items, slot, amount);
         if (!removed.m_41619_()) {
            this.m_6596_();
            this.updateBookState();
         }

         return removed;
      } else {
         return ItemStack.f_41583_;
      }
   }

   public ItemStack m_8016_(int slot) {
      if (!this.isValidSlot(slot)) {
         return ItemStack.f_41583_;
      } else {
         ItemStack stack = ContainerHelper.m_18966_(this.items, slot);
         this.updateBookState();
         return stack;
      }
   }

   public void m_6836_(int slot, ItemStack stack) {
      if (this.isValidSlot(slot)) {
         this.items.set(slot, stack);
         if (!stack.m_41619_() && stack.m_41613_() > this.m_6893_()) {
            stack.m_41764_(this.m_6893_());
         }

         if (slot == 0 && !stack.m_41619_() && stack.m_41613_() > 1) {
            stack.m_41764_(1);
         }

         this.m_6596_();
         this.updateBookState();
      }
   }

   public boolean m_6542_(Player player) {
      return this.f_58857_ != null
         && this.f_58857_.m_7702_(this.f_58858_) == this
         && player.m_20275_((double)this.f_58858_.m_123341_() + 0.5, (double)this.f_58858_.m_123342_() + 0.5, (double)this.f_58858_.m_123343_() + 0.5) <= 64.0;
   }

   public void m_6211_() {
      this.items.clear();
      this.m_6596_();
      this.updateBookState();
   }

   public Component m_5446_() {
      return Component.m_237115_("container.ancientwarfare.research_station");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new ResearchStationMenu(containerId, playerInventory, this.f_58858_);
   }

   public NonNullList<ItemStack> getDrops() {
      NonNullList<ItemStack> drops = NonNullList.m_122779_();

      for (ItemStack stack : this.items) {
         if (!stack.m_41619_()) {
            drops.add(stack.m_41777_());
         }
      }

      return drops;
   }

   private boolean isValidSlot(int slot) {
      return slot >= 0 && slot < 10;
   }

   private void updateBookState() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         BlockState state = this.m_58900_();
         boolean hasBook = !((ItemStack)this.items.get(0)).m_41619_();
         if (state.m_61138_(ResearchStationBlock.HAS_BOOK) && (Boolean)state.m_61143_(ResearchStationBlock.HAS_BOOK) != hasBook) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(ResearchStationBlock.HAS_BOOK, hasBook), 3);
         }
      }
   }
}
