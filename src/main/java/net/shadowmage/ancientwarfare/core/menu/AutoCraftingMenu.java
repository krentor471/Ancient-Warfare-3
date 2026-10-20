package net.shadowmage.ancientwarfare.core.menu;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
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
import net.shadowmage.ancientwarfare.core.common.engineering.CraftingRecipeSelection;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipeTransfer;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.ResearchBookItem;
import net.shadowmage.ancientwarfare.core.tile.AutoCraftingBlockEntity;

public class AutoCraftingMenu extends AbstractContainerMenu {
   public static final int CRAFT_BUTTON = 1;
   public static final int PREVIOUS_RECIPE_BUTTON = 1000;
   public static final int NEXT_RECIPE_BUTTON = 1001;
   private static final int BOOK_SLOT = 0;
   private static final int CRAFT_START = 1;
   private static final int RESOURCE_START = 10;
   private static final int RESOURCE_COUNT = 18;
   private static final int OUTPUT_START = 28;
   private static final int OUTPUT_COUNT = 9;
   private final BlockPos pos;
   private final Level level;
   private final AutoCraftingBlockEntity station;
   private final Container container;
   private final ResultContainer resultSlots = new ResultContainer();
   private final DataSlot matchingRecipeCount = DataSlot.m_39401_();

   public AutoCraftingMenu(int id, Inventory inventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.AUTO_CRAFTING), id);
      this.pos = pos;
      this.level = inventory.f_35978_.m_9236_();
      this.station = this.level.m_7702_(pos) instanceof AutoCraftingBlockEntity auto ? auto : null;
      this.container = (Container)(this.station == null ? new SimpleContainer(37) : this.station);
      this.m_38897_(new Slot(this.resultSlots, 0, 134, 26) {
         public boolean m_5857_(ItemStack stack) {
            return false;
         }

         public boolean m_8010_(Player player) {
            return false;
         }
      });
      this.m_38897_(new Slot(this.container, 0, 8, 26) {
         public boolean m_5857_(ItemStack stack) {
            return stack.m_41720_() instanceof ResearchBookItem;
         }
      });

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            this.m_38897_(new Slot(this.container, 1 + row * 3 + col, 62 + col * 18, 8 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(this.container, 28 + col, 8 + col * 18, 66));
      }

      for (int row = 0; row < 2; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(this.container, 10 + row * 9 + col, 8 + col * 18, 84 + row * 18));
         }
      }

      this.addPlayerInventory(inventory);
      this.m_38895_(this.matchingRecipeCount);
      this.m_6199_(this.container);
   }

   public void m_6199_(Container changedContainer) {
      if (!this.level.f_46443_) {
         CraftingContainer crafting = template(this.container);
         List<Recipe<CraftingContainer>> recipes = matchingRecipes(this.level, this.container, crafting);
         Recipe<CraftingContainer> selected = this.normalizeSelection(recipes);
         this.matchingRecipeCount.m_6422_(recipes.size());
         this.resultSlots.m_6836_(0, CraftingRecipeSelection.result(selected, crafting, this.level));
         this.m_38946_();
      }
   }

   private void addPlayerInventory(Inventory inventory) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 132 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(inventory, col, 8 + col * 18, 190));
      }
   }

   public int getStoredEnergy() {
      return this.station == null ? 0 : this.station.getStoredEnergy();
   }

   public int getMaxEnergy() {
      return this.station == null ? 0 : this.station.getMaxEnergy();
   }

   public boolean hasStation() {
      return this.station != null;
   }

   public boolean hasMultipleRecipes() {
      return this.matchingRecipeCount.m_6501_() > 1;
   }

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.AUTO_CRAFTING));
   }

   public ItemStack m_7648_(Player player, int index) {
      if (index >= 0 && index < this.f_38839_.size()) {
         Slot source = (Slot)this.f_38839_.get(index);
         if (!source.m_6657_()) {
            return ItemStack.f_41583_;
         } else {
            ItemStack original = source.m_7993_().m_41777_();
            ItemStack moving = source.m_7993_();
            int storageStart = 11;
            int outputStart = storageStart + 18;
            int playerStart = outputStart + 9;
            int playerEnd = playerStart + 36;
            boolean moved;
            if (index < 2) {
               moved = this.m_38903_(moving, playerStart, playerEnd, false);
            } else if (index < storageStart) {
               moved = this.m_38903_(moving, storageStart, outputStart, false);
            } else if (index < outputStart) {
               moved = this.m_38903_(moving, playerStart, playerEnd, false);
            } else if (index < playerStart) {
               moved = this.m_38903_(moving, playerStart, playerEnd, false);
            } else {
               moved = moving.m_41720_() instanceof ResearchBookItem
                  ? this.m_38903_(moving, 1, 2, false)
                  : this.m_38903_(moving, storageStart, outputStart, false);
            }

            if (!moved) {
               return ItemStack.f_41583_;
            } else {
               if (moving.m_41619_()) {
                  source.m_5852_(ItemStack.f_41583_);
               } else {
                  source.m_6654_();
               }

               source.m_142406_(player, moving);
               return original;
            }
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   public boolean m_6366_(Player player, int id) {
      if (this.level.f_46443_ || this.station == null) {
         return false;
      } else if (id != 1000 && id != 1001) {
         return id == 1 && this.station.craftOnce();
      } else {
         CraftingContainer crafting = template(this.container);
         List<Recipe<CraftingContainer>> recipes = matchingRecipes(this.level, this.container, crafting);
         if (recipes.size() < 2) {
            return false;
         } else {
            this.station.setSelectedRecipeId(CraftingRecipeSelection.cycle(recipes, this.station.getSelectedRecipeId(), id == 1000 ? -1 : 1));
            this.m_6199_(this.container);
            return true;
         }
      }
   }

   public boolean transferEngineeringRecipe(Player player, ResourceLocation recipeId) {
      if (!this.level.f_46443_ && recipeId != null && this.station != null) {
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

   public static boolean canCraft(Level level, AutoCraftingBlockEntity station) {
      if (level != null && station != null) {
         CraftingContainer template = template(station);
         ItemStack result = result(level, station, template);
         return !result.m_41619_() && hasResources(station, template) && hasOutputSpace(station, result);
      } else {
         return false;
      }
   }

   public static boolean craftOnce(Level level, AutoCraftingBlockEntity station) {
      if (!canCraft(level, station)) {
         return false;
      } else {
         CraftingContainer template = template(station);
         Map<AutoCraftingMenu.StackKey, Integer> required = required(template);

         for (Entry<AutoCraftingMenu.StackKey, Integer> entry : required.entrySet()) {
            removeResources(station, entry.getKey().stack, entry.getValue());
         }

         ItemStack result = result(level, station, template);
         insertOutput(station, result);
         station.m_6596_();
         return true;
      }
   }

   private static CraftingContainer template(Container container) {
      CraftingContainer crafting = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
         public ItemStack m_7648_(Player player, int index) {
            return ItemStack.f_41583_;
         }

         public boolean m_6875_(Player player) {
            return true;
         }
      }, 3, 3);

      for (int i = 0; i < 9; i++) {
         crafting.m_6836_(i, container.m_8020_(1 + i));
      }

      return crafting;
   }

   private static ItemStack result(Level level, Container container, CraftingContainer crafting) {
      return CraftingRecipeSelection.result(selectedRecipe(level, container, crafting), crafting, level);
   }

   private static List<Recipe<CraftingContainer>> matchingRecipes(Level level, Container container, CraftingContainer crafting) {
      ItemStack book = container.m_8020_(0);
      Set<String> completed = book.m_41619_() ? Set.of() : ResearchBookItem.getCompletedResearch(book);
      return CraftingRecipeSelection.matching(level, crafting, completed);
   }

   private static Recipe<CraftingContainer> selectedRecipe(Level level, Container container, CraftingContainer crafting) {
      ResourceLocation selectedId = container instanceof AutoCraftingBlockEntity station ? station.getSelectedRecipeId() : null;
      List<Recipe<CraftingContainer>> recipes = matchingRecipes(level, container, crafting);
      Recipe<CraftingContainer> selected = CraftingRecipeSelection.select(recipes, selectedId);
      if (container instanceof AutoCraftingBlockEntity stationx) {
         ResourceLocation normalized = selected == null ? null : selected.m_6423_();
         if (!Objects.equals(normalized, selectedId)) {
            stationx.setSelectedRecipeId(normalized);
         }
      }

      return selected;
   }

   private Recipe<CraftingContainer> normalizeSelection(List<Recipe<CraftingContainer>> recipes) {
      Recipe<CraftingContainer> selected = CraftingRecipeSelection.select(recipes, this.station == null ? null : this.station.getSelectedRecipeId());
      if (this.station != null && !Objects.equals(this.station.getSelectedRecipeId(), selected == null ? null : selected.m_6423_())) {
         this.station.setSelectedRecipeId(selected == null ? null : selected.m_6423_());
      }

      return selected;
   }

   private static boolean hasResources(Container container, CraftingContainer template) {
      for (Entry<AutoCraftingMenu.StackKey, Integer> entry : required(template).entrySet()) {
         int count = 0;

         for (int i = 0; i < 18; i++) {
            ItemStack stack = container.m_8020_(10 + i);
            if (!stack.m_41619_() && ItemStack.m_150942_(stack, entry.getKey().stack)) {
               count += stack.m_41613_();
            }
         }

         if (count < entry.getValue()) {
            return false;
         }
      }

      return true;
   }

   private static boolean hasOutputSpace(Container container, ItemStack result) {
      int remaining = result.m_41613_();

      for (int i = 0; i < 9; i++) {
         ItemStack stack = container.m_8020_(28 + i);
         if (!stack.m_41619_() && ItemStack.m_150942_(stack, result)) {
            remaining -= Math.max(0, stack.m_41741_() - stack.m_41613_());
         }
      }

      for (int ix = 0; ix < 9 && remaining > 0; ix++) {
         if (container.m_8020_(28 + ix).m_41619_()) {
            remaining -= result.m_41741_();
         }
      }

      return remaining <= 0;
   }

   private static void removeResources(Container container, ItemStack template, int count) {
      for (int i = 0; i < 18 && count > 0; i++) {
         ItemStack stack = container.m_8020_(10 + i);
         if (!stack.m_41619_() && ItemStack.m_150942_(stack, template)) {
            int moved = Math.min(count, stack.m_41613_());
            stack.m_41774_(moved);
            count -= moved;
         }
      }
   }

   private static void insertOutput(Container container, ItemStack result) {
      for (int i = 0; i < 9 && !result.m_41619_(); i++) {
         ItemStack stack = container.m_8020_(28 + i);
         if (!stack.m_41619_() && ItemStack.m_150942_(stack, result)) {
            int moved = Math.min(result.m_41613_(), stack.m_41741_() - stack.m_41613_());
            stack.m_41769_(moved);
            result.m_41774_(moved);
         }
      }

      for (int ix = 0; ix < 9 && !result.m_41619_(); ix++) {
         if (container.m_8020_(28 + ix).m_41619_()) {
            int moved = Math.min(result.m_41613_(), result.m_41741_());
            container.m_6836_(28 + ix, result.m_255036_(moved));
            result.m_41774_(moved);
         }
      }
   }

   private static Map<AutoCraftingMenu.StackKey, Integer> required(CraftingContainer template) {
      Map<AutoCraftingMenu.StackKey, Integer> values = new LinkedHashMap<>();

      for (int i = 0; i < 9; i++) {
         ItemStack stack = template.m_8020_(i);
         if (!stack.m_41619_()) {
            values.merge(new AutoCraftingMenu.StackKey(stack.m_255036_(1)), stack.m_41613_(), Integer::sum);
         }
      }

      return values;
   }

   private static ItemStack[] items(CraftingContainer crafting) {
      ItemStack[] values = new ItemStack[9];

      for (int i = 0; i < 9; i++) {
         values[i] = crafting.m_8020_(i);
      }

      return values;
   }

   private static record StackKey(ItemStack stack) {
      @Override
      public boolean equals(Object o) {
         if (o instanceof AutoCraftingMenu.StackKey other && ItemStack.m_150942_(this.stack, other.stack)) {
            return true;
         }

         return false;
      }

      @Override
      public int hashCode() {
         return this.stack.m_41720_().hashCode() * 31 + (this.stack.m_41783_() == null ? 0 : this.stack.m_41783_().hashCode());
      }
   }
}
