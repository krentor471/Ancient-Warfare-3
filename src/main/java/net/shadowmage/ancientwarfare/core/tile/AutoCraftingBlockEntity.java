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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(37, ItemStack.f_41583_);
   private ResourceLocation selectedRecipeId;

   public AutoCraftingBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.AUTO_CRAFTING), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, AutoCraftingBlockEntity station) {
      if (!level.f_46443_ && station.storedEnergy >= 100 && station.craftOnce()) {
         station.storedEnergy -= 100;
         station.m_6596_();
         level.m_7260_(pos, state, state, 3);
      }
   }

   public boolean craftOnce() {
      return this.f_58857_ != null && !this.f_58857_.f_46443_ && AutoCraftingMenu.craftOnce(this.f_58857_, this);
   }

   @Override
   public boolean hasWork() {
      return this.storedEnergy < this.getMaxEnergy() && AutoCraftingMenu.canCraft(this.f_58857_, this);
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && worker.canWorkAt(this.getWorkType()) && this.hasWork()) {
         this.storedEnergy = Math.min(
            this.getMaxEnergy(), this.storedEnergy + Math.max(1, Math.round(1600.0F * worker.getWorkEffectiveness(this.getWorkType())))
         );
         if (this.storedEnergy >= 100 && this.craftOnce()) {
            this.storedEnergy -= 100;
         }

         this.m_6596_();
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
   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
      if (this.selectedRecipeId != null) {
         tag.m_128359_("SelectedRecipe", this.selectedRecipeId.toString());
      }
   }

   @Override
   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      ContainerHelper.m_18980_(tag, this.items);
      this.selectedRecipeId = tag.m_128441_("SelectedRecipe") ? ResourceLocation.m_135820_(tag.m_128461_("SelectedRecipe")) : null;
   }

   public int m_6643_() {
      return 37;
   }

   public boolean m_7983_() {
      return this.items.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int slot) {
      return slot >= 0 && slot < 37 ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      return slot >= 0 && slot < 37 ? ContainerHelper.m_18969_(this.items, slot, amount) : ItemStack.f_41583_;
   }

   public ItemStack m_8016_(int slot) {
      return slot >= 0 && slot < 37 ? ContainerHelper.m_18966_(this.items, slot) : ItemStack.f_41583_;
   }

   public void m_6836_(int slot, ItemStack stack) {
      if (slot >= 0 && slot < 37) {
         this.items.set(slot, stack);
         this.m_6596_();
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
   }

   public Component m_5446_() {
      return Component.m_237115_("container.ancientwarfare.auto_crafting");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int id, Inventory inv, Player player) {
      return new AutoCraftingMenu(id, inv, this.f_58858_);
   }

   @Override
   public ResourceLocation getSelectedRecipeId() {
      return this.selectedRecipeId;
   }

   @Override
   public void setSelectedRecipeId(ResourceLocation recipeId) {
      this.selectedRecipeId = recipeId;
      this.m_6596_();
   }
}
