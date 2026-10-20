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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.engineering.CraftingRecipeMemory;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.EngineeringStationMenu;
import org.jetbrains.annotations.Nullable;

public class EngineeringStationBlockEntity extends BlockEntity implements Container, MenuProvider, CraftingRecipeMemory {
   public static final int BOOK_SLOT = 0;
   public static final int CRAFT_SLOT_START = 1;
   public static final int STORAGE_SLOT_START = 10;
   public static final int SLOT_COUNT = 28;
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(28, ItemStack.f_41583_);
   private ResourceLocation selectedRecipeId;

   public EngineeringStationBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.ENGINEERING_STATION), pos, state);
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
      if (this.selectedRecipeId != null) {
         tag.m_128359_("SelectedRecipe", this.selectedRecipeId.toString());
      }
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      ContainerHelper.m_18980_(tag, this.items);
      this.selectedRecipeId = tag.m_128441_("SelectedRecipe") ? ResourceLocation.m_135820_(tag.m_128461_("SelectedRecipe")) : null;
   }

   public int m_6643_() {
      return 28;
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
         }

         return removed;
      } else {
         return ItemStack.f_41583_;
      }
   }

   public ItemStack m_8016_(int slot) {
      return this.isValidSlot(slot) ? ContainerHelper.m_18966_(this.items, slot) : ItemStack.f_41583_;
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
      return Component.m_237115_("container.ancientwarfare.engineering_station");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new EngineeringStationMenu(containerId, playerInventory, this.f_58858_);
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
      return slot >= 0 && slot < 28;
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
