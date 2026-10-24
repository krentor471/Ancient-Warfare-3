package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.engineering.CraftingRecipeMemory;
import net.shadowmage.ancientwarfare.core.common.engineering.CraftingRecipeSelection;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipeTransfer;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.ResearchBookItem;
import net.shadowmage.ancientwarfare.core.tile.EngineeringStationBlockEntity;

public class EngineeringStationMenu extends AbstractContainerMenu {
   public static final int PREVIOUS_RECIPE_BUTTON = 1000;
   public static final int NEXT_RECIPE_BUTTON = 1001;
   private static final int BOOK_SLOT = 0;
   private static final int CRAFT_START = 1;
   private static final int STORAGE_START = 10;
   private static final int CONTAINER_SLOT_COUNT = 28;
   private final Container container;
   private final ResultContainer resultSlots = new ResultContainer();
   private final BlockPos pos;
   private final Level level;
   private final DataSlot matchingRecipeCount = DataSlot.standalone();

   public EngineeringStationMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.ENGINEERING_STATION), containerId);
      this.pos = pos;
      this.level = playerInventory.player.level();
      if (this.level.getBlockEntity(pos) instanceof EngineeringStationBlockEntity engineeringStation) {
         this.container = engineeringStation;
      } else {
         this.container = new SimpleContainer(28);
      }

      this.container.startOpen(playerInventory.player);
      this.addSlot(new ResearchBookSlot(this.container, 0, 8, 26));

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            int index = 1 + row * 3 + col;
            this.addSlot(new EngineeringStationMenu.CraftInputSlot(this.container, index, 62 + col * 18, 8 + row * 18));
         }
      }

      this.addSlot(new Slot(this.resultSlots, 0, 134, 26) {
         public boolean mayPickup(Player player) {
            return super.mayPickup(player) && EngineeringStationMenu.this.isResearchUnlocked(EngineeringStationMenu.this.resultSlots.getItem(0));
         }

         public void onTake(Player player, ItemStack stack) {
            EngineeringStationMenu.this.consumeCraftingIngredients(player);
            super.onTake(player, stack);
            EngineeringStationMenu.this.slotsChanged(this.container);
         }
      });

      for (int row = 0; row < 2; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(this.container, 10 + row * 9 + col, 8 + col * 18, 66 + row * 18));
         }
      }

      this.addPlayerInventory(playerInventory);
      this.addDataSlot(this.matchingRecipeCount);
      this.slotsChanged(this.container);
   }

   public void slotsChanged(Container changedContainer) {
      if (!this.level.isClientSide) {
         CraftingContainer crafting = this.buildCraftingInput();
         List<Recipe<CraftingContainer>> recipes = this.matchingRecipes(crafting);
         Recipe<CraftingContainer> selected = this.normalizeSelection(recipes);
         this.matchingRecipeCount.set(recipes.size());
         this.resultSlots.setItem(0, CraftingRecipeSelection.result(selected, crafting, this.level));
         this.broadcastChanges();
      }
   }

   public boolean hasMultipleRecipes() {
      return this.matchingRecipeCount.get() > 1;
   }

   public boolean clickMenuButton(Player player, int id) {
      if (!this.level.isClientSide && (id == 1000 || id == 1001)) {
         CraftingContainer crafting = this.buildCraftingInput();
         List<Recipe<CraftingContainer>> recipes = this.matchingRecipes(crafting);
         if (recipes.size() < 2) {
            return false;
         } else {
            this.setSelectedRecipeId(CraftingRecipeSelection.cycle(recipes, this.getSelectedRecipeId(), id == 1000 ? -1 : 1));
            this.slotsChanged(this.container);
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean isResearchUnlocked(ItemStack result) {
      String required = this.getRequiredResearch(result);
      if (required == null) {
         return true;
      } else {
         ItemStack book = this.container.getItem(0);
         return !book.isEmpty() && this.hasCompletedResearch(book, required);
      }
   }

   public String getLockedResearchName() {
      ItemStack result = this.getEngineeringRecipeRawResult();
      String required = this.getRequiredResearch(result);
      return required != null && !this.isResearchUnlocked(result) ? required : "";
   }

   private boolean hasCompletedResearch(ItemStack book, String required) {
      Set<String> completed = ResearchBookItem.getCompletedResearch(book);
      if (!completed.contains(required) && !completed.contains("ancientwarfare:" + required)) {
         for (String id : completed) {
            if (id.equals(required) || id.endsWith(":" + required)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private String getRequiredResearch(ItemStack result) {
      return this.selectedRecipe(this.buildCraftingInput()) instanceof EngineeringRecipe engineering ? engineering.requiredResearch() : null;
   }

   private ItemStack getEngineeringRecipeRawResult() {
      CraftingContainer crafting = this.buildCraftingInput();
      return CraftingRecipeSelection.result(this.selectedRecipe(crafting), crafting, this.level);
   }

   private CraftingContainer buildCraftingInput() {
      ItemStack[] grid = new ItemStack[9];

      for (int i = 0; i < 9; i++) {
         grid[i] = this.container.getItem(1 + i);
      }

      CraftingContainer crafting = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
         public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
         }

         public boolean stillValid(Player player) {
            return true;
         }
      }, 3, 3);

      for (int i = 0; i < grid.length; i++) {
         crafting.setItem(i, grid[i]);
      }

      return crafting;
   }

   private void consumeCraftingIngredients(Player player) {
      CraftingContainer crafting = this.buildCraftingInput();
      Recipe<CraftingContainer> selected = this.selectedRecipe(crafting);
      ItemStack[] grid = new ItemStack[9];

      for (int index = 0; index < grid.length; index++) {
         grid[index] = crafting.getItem(index);
      }

      int[] engineeringConsumption = selected instanceof EngineeringRecipe engineering ? engineering.consumptionCounts(grid) : null;
      NonNullList<ItemStack> remaining = selected instanceof EngineeringRecipe
         ? NonNullList.withSize(9, ItemStack.EMPTY)
         : (selected == null ? NonNullList.withSize(9, ItemStack.EMPTY) : selected.getRemainingItems(crafting));

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.container.getItem(1 + i);
         if (!stack.isEmpty()) {
            stack.shrink(engineeringConsumption == null ? 1 : engineeringConsumption[i]);
            this.container.setItem(1 + i, stack);
         }

         ItemStack remainder = (ItemStack)remaining.get(i);
         if (!remainder.isEmpty()) {
            if (this.container.getItem(1 + i).isEmpty()) {
               this.container.setItem(1 + i, remainder);
            } else if (!player.getInventory().add(remainder)) {
               player.drop(remainder, false);
            }
         }
      }

      this.container.setChanged();
   }

   private List<Recipe<CraftingContainer>> matchingRecipes(CraftingContainer crafting) {
      ItemStack book = this.container.getItem(0);
      return CraftingRecipeSelection.matching(this.level, crafting, book.isEmpty() ? Set.of() : ResearchBookItem.getCompletedResearch(book));
   }

   private Recipe<CraftingContainer> selectedRecipe(CraftingContainer crafting) {
      return CraftingRecipeSelection.select(this.matchingRecipes(crafting), this.getSelectedRecipeId());
   }

   private Recipe<CraftingContainer> normalizeSelection(List<Recipe<CraftingContainer>> recipes) {
      Recipe<CraftingContainer> selected = CraftingRecipeSelection.select(recipes, this.getSelectedRecipeId());
      this.setSelectedRecipeId(selected == null ? null : selected.getId());
      return selected;
   }

   private ResourceLocation getSelectedRecipeId() {
      return this.container instanceof CraftingRecipeMemory memory ? memory.getSelectedRecipeId() : null;
   }

   private void setSelectedRecipeId(ResourceLocation recipeId) {
      if (this.container instanceof CraftingRecipeMemory memory && !Objects.equals(memory.getSelectedRecipeId(), recipeId)) {
         memory.setSelectedRecipeId(recipeId);
      }
   }

   private void addPlayerInventory(Inventory playerInventory) {
      int top = 110;

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, top + 58));
      }
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.ENGINEERING_STATION));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      int resultSlot = 10;
      int storageMenuStart = 11;
      int playerMenuStart = 29;
      if (index >= 0 && index < this.slots.size()) {
         Slot slot = (Slot)this.slots.get(index);
         if (!slot.hasItem()) {
            return ItemStack.EMPTY;
         } else {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index == 10) {
               if (!this.moveItemStackTo(stack, 29, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }

               slot.onQuickCraft(stack, original);
            } else if (index < 29) {
               if (!this.moveItemStackTo(stack, 29, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (AWCoreRegistryAccess.is(stack, AWCoreIds.RESEARCH_BOOK)) {
               if (!this.moveItemStackTo(stack, 0, 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(stack, 11, 29, false) && !this.moveItemStackTo(stack, 1, 10, false)) {
               return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
               slot.set(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }

            if (stack.getCount() == original.getCount()) {
               return ItemStack.EMPTY;
            } else {
               slot.onTake(player, stack);
               return original;
            }
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public boolean transferEngineeringRecipe(Player player, ResourceLocation recipeId) {
      if (!this.level.isClientSide && recipeId != null) {
         boolean transferred = this.level
            .getRecipeManager()
            .byKey(recipeId)
            .filter(recipe -> recipe.getType() == AWCoreRegistryAccess.recipeType(AWCoreIds.id("engineering")))
            .map(recipe -> EngineeringRecipeTransfer.fill(this.container, 1, 0, player.getInventory(), (EngineeringRecipe)recipe, new int[]{10}, new int[]{18}))
            .orElse(false);
         if (transferred) {
            this.slotsChanged(this.container);
         }

         return transferred;
      } else {
         return false;
      }
   }

   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);
   }

   private class CraftInputSlot extends Slot {
      private CraftInputSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public void setChanged() {
         super.setChanged();
         EngineeringStationMenu.this.slotsChanged(this.container);
      }
   }
}
