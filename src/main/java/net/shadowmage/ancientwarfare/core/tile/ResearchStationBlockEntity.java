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
   private final NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);

   public ResearchStationBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.RESEARCH_STATION), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchStationBlockEntity station) {
      station.tickResearch();
   }

   private void tickResearch() {
      if (this.level != null && !this.level.isClientSide) {
         ItemStack book = (ItemStack)this.items.get(0);
         if (!book.isEmpty() && ResearchBookItem.getResearcherName(book) != null) {
            if (ResearchBookItem.getCurrentResearch(book) == null) {
               this.startNextQueuedResearch(book);
            }

            if (ResearchBookItem.getCurrentResearch(book) != null) {
               ResearchBookItem.addProgress(book, 1);
               this.setChanged();
            }
         }
      }
   }

   private void startNextQueuedResearch(ItemStack book) {
      String nextId = ResearchBookItem.peekNextQueuedResearch(book);
      ResearchGoal next = nextId == null ? null : ResearchRegistry.get(nextId);
      if (next != null && this.consumeResearchResources(next)) {
         ResearchBookItem.startNextQueuedResearch(book);
         this.setChanged();
      }
   }

   private boolean consumeResearchResources(ResearchGoal goal) {
      for (ResearchResource requirement : goal.resourceRequirements()) {
         int available = 0;

         for (int slot = 1; slot < 10; slot++) {
            ItemStack stack = (ItemStack)this.items.get(slot);
            if (requirement.matches(stack)) {
               available += stack.getCount();
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
               int removed = Math.min(remaining, stack.getCount());
               stack.shrink(removed);
               remaining -= removed;
               if (stack.isEmpty()) {
                  this.items.set(slotx, ItemStack.EMPTY);
               }
            }
         }
      }

      this.setChanged();
      return true;
   }

   @Override
   public boolean hasWork() {
      ItemStack book = (ItemStack)this.items.get(0);
      return !book.isEmpty() && ResearchBookItem.getResearcherName(book) != null
         ? ResearchBookItem.getCurrentResearch(book) != null || ResearchBookItem.peekNextQueuedResearch(book) != null
         : false;
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.level != null && !this.level.isClientSide && worker.canWorkAt(this.getWorkType())) {
         ItemStack book = (ItemStack)this.items.get(0);
         if (!book.isEmpty() && ResearchBookItem.getResearcherName(book) != null) {
            if (ResearchBookItem.getCurrentResearch(book) == null) {
               this.startNextQueuedResearch(book);
            }

            if (ResearchBookItem.getCurrentResearch(book) != null) {
               ResearchBookItem.addProgress(book, Math.max(1, Math.round(worker.getWorkEffectiveness(this.getWorkType()))));
               this.setChanged();
            }
         }
      }
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.RESEARCH;
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.items);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      ContainerHelper.loadAllItems(tag, this.items);
      this.updateBookState();
   }

   public int getContainerSize() {
      return 10;
   }

   public boolean isEmpty() {
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int slot) {
      return this.isValidSlot(slot) ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      if (this.isValidSlot(slot) && amount > 0) {
         ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
         if (!removed.isEmpty()) {
            this.setChanged();
            this.updateBookState();
         }

         return removed;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public ItemStack removeItemNoUpdate(int slot) {
      if (!this.isValidSlot(slot)) {
         return ItemStack.EMPTY;
      } else {
         ItemStack stack = ContainerHelper.takeItem(this.items, slot);
         this.updateBookState();
         return stack;
      }
   }

   public void setItem(int slot, ItemStack stack) {
      if (this.isValidSlot(slot)) {
         this.items.set(slot, stack);
         if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
         }

         if (slot == 0 && !stack.isEmpty() && stack.getCount() > 1) {
            stack.setCount(1);
         }

         this.setChanged();
         this.updateBookState();
      }
   }

   public boolean stillValid(Player player) {
      return this.level != null
         && this.level.getBlockEntity(this.worldPosition) == this
         && player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) <= 64.0;
   }

   public void clearContent() {
      this.items.clear();
      this.setChanged();
      this.updateBookState();
   }

   public Component getDisplayName() {
      return Component.translatable("container.ancientwarfare.research_station");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new ResearchStationMenu(containerId, playerInventory, this.worldPosition);
   }

   public NonNullList<ItemStack> getDrops() {
      NonNullList<ItemStack> drops = NonNullList.create();

      for (ItemStack stack : this.items) {
         if (!stack.isEmpty()) {
            drops.add(stack.copy());
         }
      }

      return drops;
   }

   private boolean isValidSlot(int slot) {
      return slot >= 0 && slot < 10;
   }

   private void updateBookState() {
      if (this.level != null && !this.level.isClientSide) {
         BlockState state = this.getBlockState();
         boolean hasBook = !((ItemStack)this.items.get(0)).isEmpty();
         if (state.hasProperty(ResearchStationBlock.HAS_BOOK) && (Boolean)state.getValue(ResearchStationBlock.HAS_BOOK) != hasBook) {
            this.level.setBlock(this.worldPosition, (BlockState)state.setValue(ResearchStationBlock.HAS_BOOK, hasBook), 3);
         }
      }
   }
}
