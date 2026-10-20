package net.shadowmage.ancientwarfare.core.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
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
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteInventoryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteRelativeSide;
import net.shadowmage.ancientwarfare.core.item.ResearchBookItem;
import net.shadowmage.ancientwarfare.core.tile.WarehouseControlBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseCraftingStationBlockEntity;

public class WarehouseCraftingStationMenu extends AbstractContainerMenu {
   public static final int SIDES_BUTTON = 0;
   public static final int PREVIOUS_RECIPE_BUTTON = 1;
   public static final int NEXT_RECIPE_BUTTON = 2;
   public static final int AUTO_STORE_BUTTON = 3;
   public static final int CONNECTION_BUTTON = 4;
   private static final int BOOK_SLOT = 0;
   private static final int CRAFT_START = 1;
   private static final int CRAFT_SLOT_COUNT = 9;
   private static final int OUTPUT_START = 10;
   private static final int OUTPUT_SLOT_COUNT = 9;
   private static final int CONTAINER_SLOT_COUNT = 19;
   private static final int PLAYER_START = 20;
   private final Container container;
   private final ResultContainer resultSlots = new ResultContainer();
   private final BlockPos pos;
   private final Level level;
   private final WarehouseCraftingStationBlockEntity craftingStation;
   private boolean buildingCraftingInput;
   private final DataSlot matchingRecipeCount = DataSlot.standalone();
   private final DataSlot autoStore = DataSlot.standalone();
   private final DataSlot connectionStatus = DataSlot.standalone();
   private final DataSlot storedEnergy = DataSlot.standalone();
   private List<ItemStack> lastTemplate = List.of();
   private ResourceLocation lastRecipe;
   private WarehouseControlBlockEntity lastWarehouse;
   private long lastInventoryRevision = Long.MIN_VALUE;

   public WarehouseCraftingStationMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_CRAFTING), containerId);
      this.pos = pos;
      this.level = playerInventory.player.level();
      this.craftingStation = this.level.getBlockEntity(pos) instanceof WarehouseCraftingStationBlockEntity station ? station : null;
      this.container = (Container)(this.craftingStation == null ? new SimpleContainer(19) : this.craftingStation);
      this.container.startOpen(playerInventory.player);
      this.addSlot(new Slot(this.resultSlots, 0, 134, 26) {
         public boolean mayPlace(ItemStack stack) {
            return false;
         }

         public boolean mayPickup(Player player) {
            return false;
         }
      });
      this.addSlot(new Slot(this.container, 0, 8, 26) {
         public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ResearchBookItem;
         }
      });

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            this.addSlot(new WarehouseCraftingStationMenu.CraftSlot(this.container, 1 + row * 3 + col, 62 + col * 18, 8 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(this.container, 10 + col, 8 + col * 18, 74));
      }

      this.addPlayerInventory(playerInventory);
      this.addDataSlot(this.matchingRecipeCount);
      this.addDataSlot(this.autoStore);
      this.addDataSlot(this.connectionStatus);
      this.addDataSlot(this.storedEnergy);
      this.slotsChanged(this.container);
   }

   public void slotsChanged(Container changedContainer) {
      if (!this.level.isClientSide && !this.buildingCraftingInput) {
         this.lastTemplate = List.of();
         this.broadcastChanges();
      }
   }

   public void broadcastChanges() {
      if (!this.level.isClientSide && !this.buildingCraftingInput) {
         this.refreshPreview();
      }

      super.broadcastChanges();
   }

   private void refreshPreview() {
      WarehouseControlBlockEntity warehouse = this.getWarehouse();
      long revision = warehouse == null ? Long.MIN_VALUE : warehouse.getInventoryRevision();
      ResourceLocation selectedId = this.craftingStation == null ? null : this.craftingStation.getSelectedRecipeId();
      List<ItemStack> template = new ArrayList<>();

      for (int slot = 0; slot < 10; slot++) {
         template.add(this.container.getItem(slot).copy());
      }

      boolean templateChanged = template.size() != this.lastTemplate.size();
      if (!templateChanged) {
         for (int slot = 0; slot < template.size(); slot++) {
            if (!ItemStack.matches(template.get(slot), this.lastTemplate.get(slot))) {
               templateChanged = true;
               break;
            }
         }
      }

      if (templateChanged || warehouse != this.lastWarehouse || revision != this.lastInventoryRevision || !Objects.equals(selectedId, this.lastRecipe)) {
         CraftingContainer crafting = this.buildCraftingInput();
         List<Recipe<CraftingContainer>> recipes = matchingRecipes(this.level, this.container, crafting);
         this.normalizeSelection(recipes);
         this.matchingRecipeCount.set(recipes.size());
         this.resultSlots.setItem(0, canCraftTemplate(this.level, this.container, false, null) ? this.getCraftingResult(crafting) : ItemStack.EMPTY);
         this.lastTemplate = template;
         this.lastWarehouse = warehouse;
         this.lastInventoryRevision = revision;
         this.lastRecipe = this.craftingStation == null ? null : this.craftingStation.getSelectedRecipeId();
      }

      this.autoStore.set(this.craftingStation != null && this.craftingStation.isAutoStoreOutput() ? 1 : 0);
      this.connectionStatus.set(this.craftingStation == null ? 0 : this.craftingStation.getWarehouseConnectionStatus());
      this.storedEnergy.set(this.craftingStation == null ? 0 : this.craftingStation.getStoredEnergy());
   }

   public boolean clickMenuButton(Player player, int id) {
      if (!this.stillValid(player) || player.isSpectator() || player.containerMenu != this) {
         return false;
      } else if (this.craftingStation != null && !this.level.isClientSide && id == 3) {
         this.craftingStation.setAutoStoreOutput(!this.craftingStation.isAutoStoreOutput());
         this.broadcastChanges();
         return true;
      } else if (this.craftingStation != null && !this.level.isClientSide && id == 4) {
         boolean changed = this.craftingStation.cycleWarehouse();
         this.slotsChanged(this.container);
         return changed;
      } else if ((id == 1 || id == 2) && this.craftingStation != null && !this.level.isClientSide) {
         CraftingContainer crafting = this.buildCraftingInput();
         List<Recipe<CraftingContainer>> recipes = matchingRecipes(this.level, this.container, crafting);
         if (recipes.size() < 2) {
            return false;
         } else {
            this.craftingStation.setSelectedRecipeId(CraftingRecipeSelection.cycle(recipes, this.craftingStation.getSelectedRecipeId(), id == 1 ? -1 : 1));
            this.slotsChanged(this.container);
            return true;
         }
      } else if (id == 0 && this.craftingStation != null && !this.level.isClientSide) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, p) -> new WorksiteSideConfigMenu(containerId, inventory, this.pos),
               Component.translatable("container.ancientwarfare.worksite_side_config")
            ),
            buffer -> buffer.writeBlockPos(this.pos)
         );
         return true;
      } else {
         return false;
      }
   }

   public boolean hasMultipleRecipes() {
      return this.matchingRecipeCount.get() > 1;
   }

   public boolean transferEngineeringRecipe(Player player, ResourceLocation recipeId) {
      if (!this.level.isClientSide && recipeId != null && this.craftingStation != null && this.stillValid(player)) {
         boolean transferred = this.level
            .getRecipeManager()
            .byKey(recipeId)
            .filter(recipe -> recipe.getType() == AWCoreRegistryAccess.recipeType(AWCoreIds.id("engineering")))
            .map(recipe -> EngineeringRecipeTransfer.fill(this.container, 1, 0, player.getInventory(), (EngineeringRecipe)recipe, new int[0], new int[0]))
            .orElse(false);
         if (transferred) {
            this.slotsChanged(this.container);
         }

         return transferred;
      } else {
         return false;
      }
   }

   public boolean hasWarehouse() {
      return this.level.isClientSide ? this.connectionStatus.get() == 1 : this.getWarehouse() != null;
   }

   private WarehouseControlBlockEntity getWarehouse() {
      return this.craftingStation == null ? null : this.craftingStation.getWarehouse();
   }

   private ItemStack getCraftingResult(CraftingContainer crafting) {
      return getCraftingResult(this.level, this.container, crafting);
   }

   private CraftingContainer buildCraftingInput() {
      CraftingContainer crafting = new TransientCraftingContainer(this, 3, 3);
      this.buildingCraftingInput = true;

      try {
         for (int slot = 0; slot < 9; slot++) {
            crafting.setItem(slot, this.container.getItem(1 + slot));
         }
      } finally {
         this.buildingCraftingInput = false;
      }

      return crafting;
   }

   private static List<ItemStack> getRequiredStacks(CraftingContainer crafting, Recipe<CraftingContainer> recipe) {
      Map<WarehouseCraftingStationMenu.StackKey, WarehouseCraftingStationMenu.RequiredStack> required = new LinkedHashMap<>();
      int[] amounts = new int[9];
      if (recipe instanceof EngineeringRecipe engineering) {
         ItemStack[] grid = new ItemStack[9];

         for (int slot = 0; slot < grid.length; slot++) {
            grid[slot] = crafting.getItem(slot);
         }

         amounts = engineering.consumptionCounts(grid);
         if (amounts == null) {
            return List.of();
         }
      } else {
         for (int slot = 0; slot < 9; slot++) {
            amounts[slot] = crafting.getItem(slot).isEmpty() ? 0 : 1;
         }
      }

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = crafting.getItem(slot);
         if (!stack.isEmpty() && amounts[slot] > 0) {
            WarehouseCraftingStationMenu.StackKey key = new WarehouseCraftingStationMenu.StackKey(stack);
            WarehouseCraftingStationMenu.RequiredStack var10000 = required.computeIfAbsent(
               key, ignored -> new WarehouseCraftingStationMenu.RequiredStack(stack.copyWithCount(1))
            );
            var10000.count = var10000.count + amounts[slot];
         }
      }

      return required.values().stream().map(value -> value.template.copyWithCount(value.count)).toList();
   }

   private void addPlayerInventory(Inventory playerInventory) {
      int top = 106;

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
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_CRAFTING));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (player.isSpectator() || player.containerMenu != this || !this.stillValid(player)) {
         return ItemStack.EMPTY;
      } else if (index < 0 || index >= this.slots.size()) {
         return ItemStack.EMPTY;
      } else if (index == 0) {
         return this.takeResultToInventory(player);
      } else {
         Slot slot = (Slot)this.slots.get(index);
         if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index >= 20) {
               if (stack.getItem() instanceof ResearchBookItem) {
                  if (!this.moveItemStackTo(stack, 1, 2, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (!this.moveItemStackTo(stack, 2, 11, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(stack, 20, this.slots.size(), false)) {
               return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
               slot.setByPlayer(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }

            if (stack.getCount() == original.getCount()) {
               return ItemStack.EMPTY;
            } else {
               slot.onTake(player, stack);
               return original;
            }
         } else {
            return ItemStack.EMPTY;
         }
      }
   }

   public void clicked(int slotId, int button, ClickType clickType, Player player) {
      if (!player.isSpectator() && player.containerMenu == this && this.stillValid(player)) {
         if (slotId != 0) {
            super.clicked(slotId, button, clickType, player);
         } else if (!this.level.isClientSide && this.stillValid(player)) {
            if (clickType == ClickType.CLONE && player.getAbilities().instabuild && this.getCarried().isEmpty()) {
               ItemStack preview = this.resultSlots.getItem(0);
               if (!preview.isEmpty()) {
                  this.setCarried(preview.copyWithCount(preview.getMaxStackSize()));
               }
            } else if (clickType == ClickType.QUICK_MOVE) {
               this.takeResultToInventory(player);
            } else {
               WarehouseCraftingStationMenu.CraftPlan plan = prepareCraft(this.level, this.container, false);
               if (plan != null) {
                  ItemStack result = plan.result;
                  ItemStack carried = this.getCarried();
                  if (clickType != ClickType.PICKUP || button != 0 && button != 1) {
                     if (clickType != ClickType.SWAP || (button < 0 || button >= 9) && button != 40) {
                        if (clickType != ClickType.THROW || !this.getCarried().isEmpty()) {
                           return;
                        }

                        if (!plan.commit()) {
                           return;
                        }

                        player.drop(result.copy(), false);
                     } else {
                        ItemStack target = player.getInventory().getItem(button);
                        if (!target.isEmpty() && !ItemStack.isSameItemSameTags(target, result) || target.getCount() + result.getCount() > result.getMaxStackSize()) {
                           return;
                        }

                        if (!plan.commit()) {
                           return;
                        }

                        player.getInventory().setItem(button, target.isEmpty() ? result.copy() : target.copyWithCount(target.getCount() + result.getCount()));
                        player.getInventory().setChanged();
                     }
                  } else {
                     if (!carried.isEmpty() && !ItemStack.isSameItemSameTags(carried, result) || carried.getCount() + result.getCount() > result.getMaxStackSize()) {
                        return;
                     }

                     if (!plan.commit()) {
                        return;
                     }

                     this.setCarried(carried.isEmpty() ? result.copy() : carried.copyWithCount(carried.getCount() + result.getCount()));
                  }

                  result.getItem().onCraftedBy(result, this.level, player);
                  this.slotsChanged(this.container);
               }
            }
         }
      }
   }

   public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
      return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
   }

   private ItemStack takeResultToInventory(Player player) {
      if (!this.level.isClientSide && this.stillValid(player)) {
         WarehouseCraftingStationMenu.CraftPlan plan = prepareCraft(this.level, this.container, false);
         if (plan == null) {
            return ItemStack.EMPTY;
         } else {
            ItemStack[] inventory = new ItemStack[36];

            for (int slot = 0; slot < inventory.length; slot++) {
               inventory[slot] = player.getInventory().getItem(slot).copy();
            }

            if (insertPlayerInventory(inventory, plan.result) && plan.commit()) {
               for (int slot = 0; slot < inventory.length; slot++) {
                  player.getInventory().setItem(slot, inventory[slot]);
               }

               player.getInventory().setChanged();
               plan.result.getItem().onCraftedBy(plan.result, this.level, player);
               this.slotsChanged(this.container);
               return plan.result.copy();
            } else {
               return ItemStack.EMPTY;
            }
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static boolean insertPlayerInventory(ItemStack[] inventory, ItemStack stack) {
      ItemStack remaining = stack.copy();

      for (int slot = 0; slot < inventory.length && !remaining.isEmpty(); slot++) {
         ItemStack current = inventory[slot];
         if (!current.isEmpty() && ItemStack.isSameItemSameTags(current, remaining)) {
            int moved = Math.min(remaining.getCount(), Math.max(0, current.getMaxStackSize() - current.getCount()));
            current.grow(moved);
            remaining.shrink(moved);
         }
      }

      for (int slotx = 0; slotx < inventory.length && !remaining.isEmpty(); slotx++) {
         if (inventory[slotx].isEmpty()) {
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            inventory[slotx] = remaining.copyWithCount(moved);
            remaining.shrink(moved);
         }
      }

      return remaining.isEmpty();
   }

   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);
   }

   public int getStoredEnergy() {
      return this.level.isClientSide ? this.storedEnergy.get() : (this.craftingStation == null ? 0 : this.craftingStation.getStoredEnergy());
   }

   public int getMaxEnergy() {
      return this.craftingStation == null ? 0 : this.craftingStation.getMaxEnergy();
   }

   public WorksiteInventoryAccess getSideAccess(WorksiteRelativeSide side) {
      return this.craftingStation == null ? WorksiteInventoryAccess.NONE : this.craftingStation.getSideAccess(side);
   }

   public boolean isAutoStoreOutput() {
      return this.autoStore.get() != 0;
   }

   public int getConnectionStatus() {
      return this.connectionStatus.get();
   }

   public Component getConnectionDescription() {
      BlockPos controllerPos = this.craftingStation == null ? null : this.craftingStation.getWarehousePos();
      return controllerPos == null
         ? Component.translatable("guistrings.warehouse.connection_missing")
         : Component.translatable(
            this.getConnectionStatus() == 2 ? "guistrings.warehouse.connection_offline" : "guistrings.warehouse.connection",
            new Object[]{controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()}
         );
   }

   public static boolean canCraftTemplate(Level level, WarehouseCraftingStationBlockEntity station, boolean requireOutputSpace) {
      return canCraftTemplate(level, station, requireOutputSpace, null);
   }

   public static boolean canCraftTemplate(Level level, Container container, boolean requireOutputSpace, @Nullable ItemStack resultOverride) {
      return prepareCraft(level, container, requireOutputSpace) != null;
   }

   public static boolean craftTemplateToOutput(Level level, WarehouseCraftingStationBlockEntity station, @Nullable Player player) {
      return craftTemplate(level, station, player, true);
   }

   private static boolean craftTemplate(Level level, Container container, @Nullable Player player, boolean toOutput) {
      if (level.isClientSide) {
         return false;
      } else {
         WarehouseCraftingStationMenu.CraftPlan plan = prepareCraft(level, container, toOutput);
         return plan != null && plan.commit();
      }
   }

   @Nullable
   private static WarehouseCraftingStationMenu.CraftPlan prepareCraft(Level level, Container container, boolean toOutput) {
      if (container instanceof WarehouseCraftingStationBlockEntity station) {
         WarehouseControlBlockEntity warehouse = station.getWarehouse();
         if (warehouse == null) {
            return null;
         } else {
            CraftingContainer crafting = buildStaticCraftingInput(container);
            Recipe<CraftingContainer> recipe = selectedRecipe(level, container, crafting);
            if (recipe == null) {
               return null;
            } else {
               ItemStack result = CraftingRecipeSelection.result(recipe, crafting, level);
               if (result.isEmpty()) {
                  return null;
               } else {
                  boolean autoStore = toOutput && station.isAutoStoreOutput();
                  if (toOutput && !autoStore && !station.canAcceptOutput(result)) {
                     return null;
                  } else {
                     List<ItemStack> requirements = getRequiredStacks(crafting, recipe);
                     if (requirements.isEmpty()) {
                        return null;
                     } else {
                        List<ItemStack> deposits = new ArrayList<>();
                        if (!(recipe instanceof EngineeringRecipe)) {
                           for (ItemStack remainder : recipe.getRemainingItems(crafting)) {
                              if (!remainder.isEmpty()) {
                                 deposits.add(remainder.copy());
                              }
                           }
                        }

                        if (autoStore) {
                           deposits.add(result.copy());
                        }

                        return !warehouse.exchange(requirements, deposits, true)
                           ? null
                           : new WarehouseCraftingStationMenu.CraftPlan(station, warehouse, requirements, deposits, result, toOutput && !autoStore);
                     }
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   private static CraftingContainer buildStaticCraftingInput(Container container) {
      CraftingContainer crafting = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
         public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
         }

         public boolean stillValid(Player player) {
            return true;
         }
      }, 3, 3);

      for (int slot = 0; slot < 9; slot++) {
         crafting.setItem(slot, container.getItem(1 + slot));
      }

      return crafting;
   }

   private static ItemStack getCraftingResult(Level level, Container container, CraftingContainer crafting) {
      return CraftingRecipeSelection.result(selectedRecipe(level, container, crafting), crafting, level);
   }

   private static List<Recipe<CraftingContainer>> matchingRecipes(Level level, Container container, CraftingContainer crafting) {
      ItemStack book = container.getItem(0);
      Set<String> completed = book.isEmpty() ? Set.of() : ResearchBookItem.getCompletedResearch(book);
      return CraftingRecipeSelection.matching(level, crafting, completed);
   }

   private static Recipe<CraftingContainer> selectedRecipe(Level level, Container container, CraftingContainer crafting) {
      ResourceLocation selectedId = container instanceof WarehouseCraftingStationBlockEntity station ? station.getSelectedRecipeId() : null;
      List<Recipe<CraftingContainer>> recipes = matchingRecipes(level, container, crafting);
      Recipe<CraftingContainer> selected = CraftingRecipeSelection.select(recipes, selectedId);
      if (container instanceof WarehouseCraftingStationBlockEntity stationx) {
         ResourceLocation normalized = selected == null ? null : selected.getId();
         if (!Objects.equals(normalized, selectedId)) {
            stationx.setSelectedRecipeId(normalized);
         }
      }

      return selected;
   }

   private Recipe<CraftingContainer> normalizeSelection(List<Recipe<CraftingContainer>> recipes) {
      Recipe<CraftingContainer> selected = CraftingRecipeSelection.select(
         recipes, this.craftingStation == null ? null : this.craftingStation.getSelectedRecipeId()
      );
      if (this.craftingStation != null && !Objects.equals(this.craftingStation.getSelectedRecipeId(), selected == null ? null : selected.getId())) {
         this.craftingStation.setSelectedRecipeId(selected == null ? null : selected.getId());
      }

      return selected;
   }

   private static record CraftPlan(
      WarehouseCraftingStationBlockEntity station,
      WarehouseControlBlockEntity warehouse,
      List<ItemStack> requirements,
      List<ItemStack> deposits,
      ItemStack result,
      boolean localOutput
   ) {
      private boolean commit() {
         if (this.station.getWarehouse() == this.warehouse && (!this.localOutput || this.station.canAcceptOutput(this.result))) {
            if (!this.warehouse.exchange(this.requirements, this.deposits, false)) {
               return false;
            } else {
               if (this.localOutput) {
                  this.station.insertOutput(this.result);
               }

               this.station.setChanged();
               return true;
            }
         } else {
            return false;
         }
      }
   }

   private class CraftSlot extends Slot {
      private CraftSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      public void setChanged() {
         super.setChanged();
         WarehouseCraftingStationMenu.this.slotsChanged(this.container);
      }
   }

   private static class RequiredStack {
      private final ItemStack template;
      private int count;

      private RequiredStack(ItemStack template) {
         this.template = template;
      }
   }

   private static record StackKey(ItemStack stack) {
      @Override
      public boolean equals(Object obj) {
         if (obj instanceof WarehouseCraftingStationMenu.StackKey other && ItemStack.isSameItemSameTags(this.stack, other.stack)) {
            return true;
         }

         return false;
      }

      @Override
      public int hashCode() {
         int result = this.stack.getItem().hashCode();
         return 31 * result + (this.stack.getTag() == null ? 0 : this.stack.getTag().hashCode());
      }
   }
}
