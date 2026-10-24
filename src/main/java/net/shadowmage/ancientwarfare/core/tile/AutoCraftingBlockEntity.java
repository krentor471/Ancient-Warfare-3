package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.engineering.CraftingRecipeMemory;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.menu.AutoCraftingMenu;
import org.jetbrains.annotations.Nullable;

public class AutoCraftingBlockEntity extends BasicWorksiteBlockEntity implements Container, MenuProvider, CraftingRecipeMemory {
   public static final int BOOK_SLOT = 0;
   public static final int CRAFT_SLOT_START = 1;
   public static final int CRAFT_SLOT_COUNT = 9;
   public static final int RESOURCE_SLOT_START = 10;
   public static final int RESOURCE_SLOT_COUNT = 18;
   public static final int OUTPUT_SLOT_START = 28;
   public static final int OUTPUT_SLOT_COUNT = 9;
   public static final int SLOT_COUNT = 37;
   private static final int WORK_COST = 100;
   private final NonNullList<ItemStack> items = NonNullList.withSize(37, ItemStack.EMPTY);
   private ResourceLocation selectedRecipeId;

   public AutoCraftingBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.AUTO_CRAFTING), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, AutoCraftingBlockEntity station) {
      if (!level.isClientSide && station.storedEnergy >= 100 && station.craftOnce()) {
         station.storedEnergy -= 100;
         station.setChanged();
         level.sendBlockUpdated(pos, state, state, 3);
      }
   }

   public boolean craftOnce() {
      return this.level != null && !this.level.isClientSide && AutoCraftingMenu.craftOnce(this.level, this);
   }

   @Override
   public boolean hasWork() {
      return this.storedEnergy < this.getMaxEnergy() && AutoCraftingMenu.canCraft(this.level, this);
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.level != null && !this.level.isClientSide && worker.canWorkAt(this.getWorkType()) && this.hasWork()) {
         this.storedEnergy = Math.min(
            this.getMaxEnergy(), this.storedEnergy + Math.max(1, Math.round(1600.0F * worker.getWorkEffectiveness(this.getWorkType())))
         );
         if (this.storedEnergy >= 100 && this.craftOnce()) {
            this.storedEnergy -= 100;
         }

         this.setChanged();
      }
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.CRAFTING;
   }

   @Override
   public String getDescriptionId() {
      return "block.ancientwarfare.auto_crafting";
   }

   @Override
   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.items);
      if (this.selectedRecipeId != null) {
         tag.putString("SelectedRecipe", this.selectedRecipeId.toString());
      }
   }

   @Override
   public void load(CompoundTag tag) {
      super.load(tag);
      ContainerHelper.loadAllItems(tag, this.items);
      this.selectedRecipeId = tag.contains("SelectedRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
   }

   public int getContainerSize() {
      return 37;
   }

   public boolean isEmpty() {
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int slot) {
      return slot >= 0 && slot < 37 ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      return slot >= 0 && slot < 37 ? ContainerHelper.removeItem(this.items, slot, amount) : ItemStack.EMPTY;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      return slot >= 0 && slot < 37 ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
   }

   public void setItem(int slot, ItemStack stack) {
      if (slot >= 0 && slot < 37) {
         this.items.set(slot, stack);
         this.setChanged();
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
   }

   public Component getDisplayName() {
      return Component.translatable("container.ancientwarfare.auto_crafting");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new AutoCraftingMenu(id, inv, this.worldPosition);
   }

   @Override
   public ResourceLocation getSelectedRecipeId() {
      return this.selectedRecipeId;
   }

   @Override
   public void setSelectedRecipeId(ResourceLocation recipeId) {
      this.selectedRecipeId = recipeId;
      this.setChanged();
   }
}
