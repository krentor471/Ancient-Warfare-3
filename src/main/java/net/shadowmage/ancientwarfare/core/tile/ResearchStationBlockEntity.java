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
import net.shadowmage.ancientwarfare.core.common.research.ResearchBookData;
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
   /** Science a scribe gathers with each stretch of work at the desk, with a wooden quill and no shelves about. */
   public static final float SCIENCE_PER_EFFORT = 1.0F;
   /** Science one sheet of paper and one ink sac from a chest beside the desk are good for. */
   public static final int SCIENCE_PER_SUPPLY = 20;
   /** Shelves counted within this many blocks of the desk; this many or more make the room a scriptorium. */
   private static final int SHELF_RANGE = 4;
   private static final int SCRIPTORIUM_SHELVES = 6;
   private static final int MAX_SHELVES = 20;
   private static final float SCRIPTORIUM_BONUS = 0.25F;
   private static final float SHELF_BONUS = 0.03F;
   private static final int SUPPLY_RANGE = 2;
   private final NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);
   /** Science paid for with paper and ink but not yet gathered. */
   private int supplied;
   private float scienceFraction;
   private float roomBonus = -1.0F;
   private long roomCheckedAt;

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

            if (this.level.getGameTime() % 20L == 0L && this.level instanceof net.minecraft.server.level.ServerLevel server) {
               net.shadowmage.ancientwarfare.core.research.PlayerResearch.learn(server, ResearchBookItem.getResearcherName(book), ResearchBookItem.getCompletedResearch(book));
            }
         }
      }
   }

   private void startNextQueuedResearch(ItemStack book) {
      String nextId = ResearchBookItem.peekNextQueuedResearch(book);
      ResearchGoal next = nextId == null ? null : ResearchRegistry.get(nextId);
      if (next != null && ResearchBookData.getScience(book) >= next.science() && this.consumeResearchResources(next)) {
         ResearchBookData.takeScience(book, next.science());
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

   /** A scribe always has work at a desk with a named book: when there is nothing to study, he gathers science. */
   @Override
   public boolean hasWork() {
      ItemStack book = (ItemStack)this.items.get(0);
      return !book.isEmpty() && ResearchBookItem.getResearcherName(book) != null;
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.level != null && !this.level.isClientSide && worker.canWorkAt(this.getWorkType())) {
         ItemStack book = (ItemStack)this.items.get(0);
         if (!book.isEmpty() && ResearchBookItem.getResearcherName(book) != null) {
            if (ResearchBookItem.getCurrentResearch(book) == null) {
               this.startNextQueuedResearch(book);
            }

            int effort = Math.max(1, Math.round(worker.getWorkEffectiveness(this.getWorkType())));
            if (ResearchBookItem.getCurrentResearch(book) != null) {
               ResearchBookItem.addProgress(book, effort);
            }

            this.gatherScience(book, worker, effort);
            this.setChanged();
         }
      }
   }

   /**
    * The scribe writes: the science he gathers grows with his quill and with the shelves about the
    * desk, and he writes on paper with ink from a chest beside it. Without them he gathers nothing,
    * and the book says so.
    */
   private void gatherScience(ItemStack book, WorkerAccess worker, int effort) {
      float quill = worker instanceof net.minecraft.world.entity.LivingEntity scribe
         ? net.shadowmage.ancientwarfare.core.item.QuillItem.scienceMultiplier(scribe.getMainHandItem())
         : 1.0F;
      if (this.roomBonus < 0.0F || this.level.getGameTime() - this.roomCheckedAt > 200L) {
         this.roomBonus = roomBonus(this.level, this.worldPosition);
         this.roomCheckedAt = this.level.getGameTime();
      }

      this.scienceFraction += effort * SCIENCE_PER_EFFORT * quill * (1.0F + this.roomBonus);
      int whole = (int)this.scienceFraction;
      if (whole <= 0) {
         return;
      }

      if (this.supplied < whole && !this.takeSupplies()) {
         ResearchBookData.setStarved(book, true);
         this.scienceFraction = Math.min(this.scienceFraction, 1.0F);
         return;
      }

      this.scienceFraction -= whole;
      this.supplied -= whole;
      ResearchBookData.setStarved(book, false);
      ResearchBookData.addScience(book, whole);
   }

   private boolean takeSupplies() {
      Container paper = null;
      int paperSlot = -1;
      Container ink = null;
      int inkSlot = -1;
      for (BlockPos pos : BlockPos.betweenClosed(this.worldPosition.offset(-SUPPLY_RANGE, -1, -SUPPLY_RANGE), this.worldPosition.offset(SUPPLY_RANGE, 1, SUPPLY_RANGE))) {
         if (pos.equals(this.worldPosition) || !(this.level.getBlockEntity(pos) instanceof Container container)) {
            continue;
         }

         for (int slot = 0; slot < container.getContainerSize() && (paper == null || ink == null); slot++) {
            ItemStack stack = container.getItem(slot);
            if (paper == null && stack.is(net.minecraft.world.item.Items.PAPER)) {
               paper = container;
               paperSlot = slot;
            } else if (ink == null && (stack.is(net.minecraft.world.item.Items.INK_SAC) || stack.is(net.minecraft.world.item.Items.BLACK_DYE))) {
               ink = container;
               inkSlot = slot;
            }
         }

         if (paper != null && ink != null) {
            paper.removeItem(paperSlot, 1);
            ink.removeItem(inkSlot, 1);
            paper.setChanged();
            ink.setChanged();
            this.supplied += SCIENCE_PER_SUPPLY;
            return true;
         }
      }

      return false;
   }

   /** The bonus the room gives the scribes: a little for every bookshelf near the desk, and more when there are enough to call it a scriptorium. */
   public static float roomBonus(Level level, BlockPos desk) {
      int shelves = shelves(level, desk);
      return (shelves >= SCRIPTORIUM_SHELVES ? SCRIPTORIUM_BONUS : 0.0F) + SHELF_BONUS * Math.min(shelves, MAX_SHELVES);
   }

   public static boolean isScriptorium(Level level, BlockPos desk) {
      return shelves(level, desk) >= SCRIPTORIUM_SHELVES;
   }

   private static int shelves(Level level, BlockPos desk) {
      int shelves = 0;
      for (BlockPos pos : BlockPos.betweenClosed(desk.offset(-SHELF_RANGE, -1, -SHELF_RANGE), desk.offset(SHELF_RANGE, 3, SHELF_RANGE))) {
         BlockState state = level.getBlockState(pos);
         if (state.is(net.minecraft.world.level.block.Blocks.BOOKSHELF) || state.is(net.minecraft.world.level.block.Blocks.CHISELED_BOOKSHELF)) {
            shelves++;
         }
      }

      return shelves;
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.RESEARCH;
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.items);
      tag.putInt("ScienceSupplied", this.supplied);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      ContainerHelper.loadAllItems(tag, this.items);
      this.supplied = tag.getInt("ScienceSupplied");
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
