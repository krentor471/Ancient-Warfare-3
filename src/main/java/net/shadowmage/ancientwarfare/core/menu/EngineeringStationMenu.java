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
   private final DataSlot matchingRecipeCount = DataSlot.m_39401_();

   public EngineeringStationMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.ENGINEERING_STATION), containerId);
      this.pos = pos;
      this.level = playerInventory.f_35978_.m_9236_();
      if (this.level.m_7702_(pos) instanceof EngineeringStationBlockEntity engineeringStation) {
         this.container = engineeringStation;
      } else {
         this.container = new SimpleContainer(28);
      }

      this.container.m_5856_(playerInventory.f_35978_);
      this.m_38897_(new ResearchBookSlot(this.container, 0, 8, 26));

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            int index = 1 + row * 3 + col;
            this.m_38897_(new EngineeringStationMenu.CraftInputSlot(this.container, index, 62 + col * 18, 8 + row * 18));
         }
      }

      this.m_38897_(new Slot(this.resultSlots, 0, 134, 26) {
         public boolean m_8010_(Player player) {
            return super.m_8010_(player) && EngineeringStationMenu.this.isResearchUnlocked(EngineeringStationMenu.this.resultSlots.m_8020_(0));
         }

         public void m_142406_(Player player, ItemStack stack) {
            EngineeringStationMenu.this.consumeCraftingIngredients(player);
            super.m_142406_(player, stack);
            EngineeringStationMenu.this.m_6199_(this.f_40218_);
         }
      });

      for (int row = 0; row < 2; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(this.container, 10 + row * 9 + col, 8 + col * 18, 66 + row * 18));
         }
      }

      this.addPlayerInventory(playerInventory);
      this.m_38895_(this.matchingRecipeCount);
      this.m_6199_(this.container);
   }

   public void m_6199_(Container changedContainer) {
      if (!this.level.f_46443_) {
         CraftingContainer crafting = this.buildCraftingInput();
         List<Recipe<CraftingContainer>> recipes = this.matchingRecipes(crafting);
         Recipe<CraftingContainer> selected = this.normalizeSelection(recipes);
         this.matchingRecipeCount.m_6422_(recipes.size());
         this.resultSlots.m_6836_(0, CraftingRecipeSelection.result(selected, crafting, this.level));
         this.m_38946_();
      }
   }

   public boolean hasMultipleRecipes() {
      return this.matchingRecipeCount.m_6501_() > 1;
   }

   public boolean m_6366_(Player player, int id) {
      if (!this.level.f_46443_ && (id == 1000 || id == 1001)) {
         CraftingContainer crafting = this.buildCraftingInput();
         List<Recipe<CraftingContainer>> recipes = this.matchingRecipes(crafting);
         if (recipes.size() < 2) {
            return false;
         } else {
            this.setSelectedRecipeId(CraftingRecipeSelection.cycle(recipes, this.getSelectedRecipeId(), id == 1000 ? -1 : 1));
            this.m_6199_(this.container);
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
         ItemStack book = this.container.m_8020_(0);
         return !book.m_41619_() && this.hasCompletedResearch(book, required);
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
         grid[i] = this.container.m_8020_(1 + i);
      }

      CraftingContainer crafting = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
         public ItemStack m_7648_(Player player, int index) {
            return ItemStack.f_41583_;
         }

         public boolean m_6875_(Player player) {
            return true;
         }
      }, 3, 3);

      for (int i = 0; i < grid.length; i++) {
         crafting.m_6836_(i, grid[i]);
      }

      return crafting;
   }

   private void consumeCraftingIngredients(Player player) {
      CraftingContainer crafting = this.buildCraftingInput();
      Recipe<CraftingContainer> selected = this.selectedRecipe(crafting);
      ItemStack[] grid = new ItemStack[9];

      for (int index = 0; index < grid.length; index++) {
         grid[index] = crafting.m_8020_(index);
      }

      int[] engineeringConsumption = selected instanceof EngineeringRecipe engineering ? engineering.consumptionCounts(grid) : null;
      NonNullList<ItemStack> remaining = selected instanceof EngineeringRecipe
         ? NonNullList.m_122780_(9, ItemStack.f_41583_)
         : (selected == null ? NonNullList.m_122780_(9, ItemStack.f_41583_) : selected.m_7457_(crafting));

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.container.m_8020_(1 + i);
         if (!stack.m_41619_()) {
            stack.m_41774_(engineeringConsumption == null ? 1 : engineeringConsumption[i]);
            this.container.m_6836_(1 + i, stack);
         }

         ItemStack remainder = (ItemStack)remaining.get(i);
         if (!remainder.m_41619_()) {
            if (this.container.m_8020_(1 + i).m_41619_()) {
               this.container.m_6836_(1 + i, remainder);
            } else if (!player.m_150109_().m_36054_(remainder)) {
               player.m_36176_(remainder, false);
            }
         }
      }

      this.container.m_6596_();
   }

   private List<Recipe<CraftingContainer>> matchingRecipes(CraftingContainer crafting) {
      ItemStack book = this.container.m_8020_(0);
      return CraftingRecipeSelection.matching(this.level, crafting, book.m_41619_() ? Set.of() : ResearchBookItem.getCompletedResearch(book));
   }

   private Recipe<CraftingContainer> selectedRecipe(CraftingContainer crafting) {
      return CraftingRecipeSelection.select(this.matchingRecipes(crafting), this.getSelectedRecipeId());
   }

   private Recipe<CraftingContainer> normalizeSelection(List<Recipe<CraftingContainer>> recipes) {
      Recipe<CraftingContainer> selected = CraftingRecipeSelection.select(recipes, this.getSelectedRecipeId());
      this.setSelectedRecipeId(selected == null ? null : selected.m_6423_());
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
            this.m_38897_(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(playerInventory, col, 8 + col * 18, top + 58));
      }
   }

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.ENGINEERING_STATION));
   }

   public ItemStack m_7648_(Player player, int index) {
      int resultSlot = 10;
      int storageMenuStart = 11;
      int playerMenuStart = 29;
      if (index >= 0 && index < this.f_38839_.size()) {
         Slot slot = (Slot)this.f_38839_.get(index);
         if (!slot.m_6657_()) {
            return ItemStack.f_41583_;
         } else {
            ItemStack stack = slot.m_7993_();
            ItemStack original = stack.m_41777_();
            if (index == 10) {
               if (!this.m_38903_(stack, 29, this.f_38839_.size(), true)) {
                  return ItemStack.f_41583_;
               }

               slot.m_40234_(stack, original);
            } else if (index < 29) {
               if (!this.m_38903_(stack, 29, this.f_38839_.size(), true)) {
                  return ItemStack.f_41583_;
               }
            } else if (AWCoreRegistryAccess.is(stack, AWCoreIds.RESEARCH_BOOK)) {
               if (!this.m_38903_(stack, 0, 1, false)) {
                  return ItemStack.f_41583_;
               }
            } else if (!this.m_38903_(stack, 11, 29, false) && !this.m_38903_(stack, 1, 10, false)) {
               return ItemStack.f_41583_;
            }

            if (stack.m_41619_()) {
               slot.m_5852_(ItemStack.f_41583_);
            } else {
               slot.m_6654_();
            }

            if (stack.m_41613_() == original.m_41613_()) {
               return ItemStack.f_41583_;
            } else {
               slot.m_142406_(player, stack);
               return original;
            }
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   public boolean transferEngineeringRecipe(Player player, ResourceLocation recipeId) {
      if (!this.level.f_46443_ && recipeId != null) {
         boolean transferred = this.level
            .m_7465_()
            .m_44043_(recipeId)
            .filter(recipe -> recipe.m_6671_() == AWCoreRegistryAccess.recipeType(AWCoreIds.id("engineering")))
            .map(recipe -> EngineeringRecipeTransfer.fill(this.container, 1, 0, player.m_150109_(), (EngineeringRecipe)recipe, new int[]{10}, new int[]{18}))
            .orElse(false);
         if (transferred) {
            this.m_6199_(this.container);
         }

         return transferred;
      } else {
         return false;
      }
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      this.container.m_5785_(player);
   }

   private class CraftInputSlot extends Slot {
      private CraftInputSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public void m_6654_() {
         super.m_6654_();
         EngineeringStationMenu.this.m_6199_(this.f_40218_);
      }
   }
}
