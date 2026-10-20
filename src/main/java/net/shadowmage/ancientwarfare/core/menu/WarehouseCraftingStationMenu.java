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
   private final DataSlot matchingRecipeCount = DataSlot.m_39401_();
   private final DataSlot autoStore = DataSlot.m_39401_();
   private final DataSlot connectionStatus = DataSlot.m_39401_();
   private final DataSlot storedEnergy = DataSlot.m_39401_();
   private List<ItemStack> lastTemplate = List.of();
   private ResourceLocation lastRecipe;
   private WarehouseControlBlockEntity lastWarehouse;
   private long lastInventoryRevision = Long.MIN_VALUE;

   public WarehouseCraftingStationMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_CRAFTING), containerId);
      this.pos = pos;
      this.level = playerInventory.f_35978_.m_9236_();
      this.craftingStation = this.level.m_7702_(pos) instanceof WarehouseCraftingStationBlockEntity station ? station : null;
      this.container = (Container)(this.craftingStation == null ? new SimpleContainer(19) : this.craftingStation);
      this.container.m_5856_(playerInventory.f_35978_);
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
            this.m_38897_(new WarehouseCraftingStationMenu.CraftSlot(this.container, 1 + row * 3 + col, 62 + col * 18, 8 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(this.container, 10 + col, 8 + col * 18, 74));
      }

      this.addPlayerInventory(playerInventory);
      this.m_38895_(this.matchingRecipeCount);
      this.m_38895_(this.autoStore);
      this.m_38895_(this.connectionStatus);
      this.m_38895_(this.storedEnergy);
      this.m_6199_(this.container);
   }

   public void m_6199_(Container changedContainer) {
      if (!this.level.f_46443_ && !this.buildingCraftingInput) {
         this.lastTemplate = List.of();
         this.m_38946_();
      }
   }

   public void m_38946_() {
      if (!this.level.f_46443_ && !this.buildingCraftingInput) {
         this.refreshPreview();
      }

      super.m_38946_();
   }

   private void refreshPreview() {
      WarehouseControlBlockEntity warehouse = this.getWarehouse();
      long revision = warehouse == null ? Long.MIN_VALUE : warehouse.getInventoryRevision();
      ResourceLocation selectedId = this.craftingStation == null ? null : this.craftingStation.getSelectedRecipeId();
      List<ItemStack> template = new ArrayList<>();

      for (int slot = 0; slot < 10; slot++) {
         template.add(this.container.m_8020_(slot).m_41777_());
      }

      boolean templateChanged = template.size() != this.lastTemplate.size();
      if (!templateChanged) {
         for (int slot = 0; slot < template.size(); slot++) {
            if (!ItemStack.m_41728_(template.get(slot), this.lastTemplate.get(slot))) {
               templateChanged = true;
               break;
            }
         }
      }

      if (templateChanged || warehouse != this.lastWarehouse || revision != this.lastInventoryRevision || !Objects.equals(selectedId, this.lastRecipe)) {
         CraftingContainer crafting = this.buildCraftingInput();
         List<Recipe<CraftingContainer>> recipes = matchingRecipes(this.level, this.container, crafting);
         this.normalizeSelection(recipes);
         this.matchingRecipeCount.m_6422_(recipes.size());
         this.resultSlots.m_6836_(0, canCraftTemplate(this.level, this.container, false, null) ? this.getCraftingResult(crafting) : ItemStack.f_41583_);
         this.lastTemplate = template;
         this.lastWarehouse = warehouse;
         this.lastInventoryRevision = revision;
         this.lastRecipe = this.craftingStation == null ? null : this.craftingStation.getSelectedRecipeId();
      }

      this.autoStore.m_6422_(this.craftingStation != null && this.craftingStation.isAutoStoreOutput() ? 1 : 0);
      this.connectionStatus.m_6422_(this.craftingStation == null ? 0 : this.craftingStation.getWarehouseConnectionStatus());
      this.storedEnergy.m_6422_(this.craftingStation == null ? 0 : this.craftingStation.getStoredEnergy());
   }

   public boolean m_6366_(Player player, int id) {
      if (!this.m_6875_(player) || player.m_5833_() || player.f_36096_ != this) {
         return false;
      } else if (this.craftingStation != null && !this.level.f_46443_ && id == 3) {
         this.craftingStation.setAutoStoreOutput(!this.craftingStation.isAutoStoreOutput());
         this.m_38946_();
         return true;
      } else if (this.craftingStation != null && !this.level.f_46443_ && id == 4) {
         boolean changed = this.craftingStation.cycleWarehouse();
         this.m_6199_(this.container);
         return changed;
      } else if ((id == 1 || id == 2) && this.craftingStation != null && !this.level.f_46443_) {
         CraftingContainer crafting = this.buildCraftingInput();
         List<Recipe<CraftingContainer>> recipes = matchingRecipes(this.level, this.container, crafting);
         if (recipes.size() < 2) {
            return false;
         } else {
            this.craftingStation.setSelectedRecipeId(CraftingRecipeSelection.cycle(recipes, this.craftingStation.getSelectedRecipeId(), id == 1 ? -1 : 1));
            this.m_6199_(this.container);
            return true;
         }
      } else if (id == 0 && this.craftingStation != null && !this.level.f_46443_) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, p) -> new WorksiteSideConfigMenu(containerId, inventory, this.pos),
               Component.m_237115_("container.ancientwarfare.worksite_side_config")
            ),
            buffer -> buffer.m_130064_(this.pos)
         );
         return true;
      } else {
         return false;
      }
   }

   public boolean hasMultipleRecipes() {
      return this.matchingRecipeCount.m_6501_() > 1;
   }

   public boolean transferEngineeringRecipe(Player player, ResourceLocation recipeId) {
      if (!this.level.f_46443_ && recipeId != null && this.craftingStation != null && this.m_6875_(player)) {
         boolean transferred = this.level
            .m_7465_()
            .m_44043_(recipeId)
            .filter(recipe -> recipe.m_6671_() == AWCoreRegistryAccess.recipeType(AWCoreIds.id("engineering")))
            .map(recipe -> EngineeringRecipeTransfer.fill(this.container, 1, 0, player.m_150109_(), (EngineeringRecipe)recipe, new int[0], new int[0]))
            .orElse(false);
         if (transferred) {
            this.m_6199_(this.container);
         }

         return transferred;
      } else {
         return false;
      }
   }

   public boolean hasWarehouse() {
      return this.level.f_46443_ ? this.connectionStatus.m_6501_() == 1 : this.getWarehouse() != null;
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
            crafting.m_6836_(slot, this.container.m_8020_(1 + slot));
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
            grid[slot] = crafting.m_8020_(slot);
         }

         amounts = engineering.consumptionCounts(grid);
         if (amounts == null) {
            return List.of();
         }
      } else {
         for (int slot = 0; slot < 9; slot++) {
            amounts[slot] = crafting.m_8020_(slot).m_41619_() ? 0 : 1;
         }
      }

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = crafting.m_8020_(slot);
         if (!stack.m_41619_() && amounts[slot] > 0) {
            WarehouseCraftingStationMenu.StackKey key = new WarehouseCraftingStationMenu.StackKey(stack);
            WarehouseCraftingStationMenu.RequiredStack var10000 = required.computeIfAbsent(
               key, ignored -> new WarehouseCraftingStationMenu.RequiredStack(stack.m_255036_(1))
            );
            var10000.count = var10000.count + amounts[slot];
         }
      }

      return required.values().stream().map(value -> value.template.m_255036_(value.count)).toList();
   }

   private void addPlayerInventory(Inventory playerInventory) {
      int top = 106;

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
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.WAREHOUSE_CRAFTING));
   }

   public ItemStack m_7648_(Player player, int index) {
      if (player.m_5833_() || player.f_36096_ != this || !this.m_6875_(player)) {
         return ItemStack.f_41583_;
      } else if (index < 0 || index >= this.f_38839_.size()) {
         return ItemStack.f_41583_;
      } else if (index == 0) {
         return this.takeResultToInventory(player);
      } else {
         Slot slot = (Slot)this.f_38839_.get(index);
         if (slot != null && slot.m_6657_()) {
            ItemStack stack = slot.m_7993_();
            ItemStack original = stack.m_41777_();
            if (index >= 20) {
               if (stack.m_41720_() instanceof ResearchBookItem) {
                  if (!this.m_38903_(stack, 1, 2, false)) {
                     return ItemStack.f_41583_;
                  }
               } else if (!this.m_38903_(stack, 2, 11, false)) {
                  return ItemStack.f_41583_;
               }
            } else if (!this.m_38903_(stack, 20, this.f_38839_.size(), false)) {
               return ItemStack.f_41583_;
            }

            if (stack.m_41619_()) {
               slot.m_269060_(ItemStack.f_41583_);
            } else {
               slot.m_6654_();
            }

            if (stack.m_41613_() == original.m_41613_()) {
               return ItemStack.f_41583_;
            } else {
               slot.m_142406_(player, stack);
               return original;
            }
         } else {
            return ItemStack.f_41583_;
         }
      }
   }

   public void m_150399_(int slotId, int button, ClickType clickType, Player player) {
      if (!player.m_5833_() && player.f_36096_ == this && this.m_6875_(player)) {
         if (slotId != 0) {
            super.m_150399_(slotId, button, clickType, player);
         } else if (!this.level.f_46443_ && this.m_6875_(player)) {
            if (clickType == ClickType.CLONE && player.m_150110_().f_35937_ && this.m_142621_().m_41619_()) {
               ItemStack preview = this.resultSlots.m_8020_(0);
               if (!preview.m_41619_()) {
                  this.m_142503_(preview.m_255036_(preview.m_41741_()));
               }
            } else if (clickType == ClickType.QUICK_MOVE) {
               this.takeResultToInventory(player);
            } else {
               WarehouseCraftingStationMenu.CraftPlan plan = prepareCraft(this.level, this.container, false);
               if (plan != null) {
                  ItemStack result = plan.result;
                  ItemStack carried = this.m_142621_();
                  if (clickType != ClickType.PICKUP || button != 0 && button != 1) {
                     if (clickType != ClickType.SWAP || (button < 0 || button >= 9) && button != 40) {
                        if (clickType != ClickType.THROW || !this.m_142621_().m_41619_()) {
                           return;
                        }

                        if (!plan.commit()) {
                           return;
                        }

                        player.m_36176_(result.m_41777_(), false);
                     } else {
                        ItemStack target = player.m_150109_().m_8020_(button);
                        if (!target.m_41619_() && !ItemStack.m_150942_(target, result) || target.m_41613_() + result.m_41613_() > result.m_41741_()) {
                           return;
                        }

                        if (!plan.commit()) {
                           return;
                        }

                        player.m_150109_().m_6836_(button, target.m_41619_() ? result.m_41777_() : target.m_255036_(target.m_41613_() + result.m_41613_()));
                        player.m_150109_().m_6596_();
                     }
                  } else {
                     if (!carried.m_41619_() && !ItemStack.m_150942_(carried, result) || carried.m_41613_() + result.m_41613_() > result.m_41741_()) {
                        return;
                     }

                     if (!plan.commit()) {
                        return;
                     }

                     this.m_142503_(carried.m_41619_() ? result.m_41777_() : carried.m_255036_(carried.m_41613_() + result.m_41613_()));
                  }

                  result.m_41720_().m_7836_(result, this.level, player);
                  this.m_6199_(this.container);
               }
            }
         }
      }
   }

   public boolean m_5882_(ItemStack stack, Slot slot) {
      return slot.f_40218_ != this.resultSlots && super.m_5882_(stack, slot);
   }

   private ItemStack takeResultToInventory(Player player) {
      if (!this.level.f_46443_ && this.m_6875_(player)) {
         WarehouseCraftingStationMenu.CraftPlan plan = prepareCraft(this.level, this.container, false);
         if (plan == null) {
            return ItemStack.f_41583_;
         } else {
            ItemStack[] inventory = new ItemStack[36];

            for (int slot = 0; slot < inventory.length; slot++) {
               inventory[slot] = player.m_150109_().m_8020_(slot).m_41777_();
            }

            if (insertPlayerInventory(inventory, plan.result) && plan.commit()) {
               for (int slot = 0; slot < inventory.length; slot++) {
                  player.m_150109_().m_6836_(slot, inventory[slot]);
               }

               player.m_150109_().m_6596_();
               plan.result.m_41720_().m_7836_(plan.result, this.level, player);
               this.m_6199_(this.container);
               return plan.result.m_41777_();
            } else {
               return ItemStack.f_41583_;
            }
         }
      } else {
         return ItemStack.f_41583_;
      }
   }

   private static boolean insertPlayerInventory(ItemStack[] inventory, ItemStack stack) {
      ItemStack remaining = stack.m_41777_();

      for (int slot = 0; slot < inventory.length && !remaining.m_41619_(); slot++) {
         ItemStack current = inventory[slot];
         if (!current.m_41619_() && ItemStack.m_150942_(current, remaining)) {
            int moved = Math.min(remaining.m_41613_(), Math.max(0, current.m_41741_() - current.m_41613_()));
            current.m_41769_(moved);
            remaining.m_41774_(moved);
         }
      }

      for (int slotx = 0; slotx < inventory.length && !remaining.m_41619_(); slotx++) {
         if (inventory[slotx].m_41619_()) {
            int moved = Math.min(remaining.m_41613_(), remaining.m_41741_());
            inventory[slotx] = remaining.m_255036_(moved);
            remaining.m_41774_(moved);
         }
      }

      return remaining.m_41619_();
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      this.container.m_5785_(player);
   }

   public int getStoredEnergy() {
      return this.level.f_46443_ ? this.storedEnergy.m_6501_() : (this.craftingStation == null ? 0 : this.craftingStation.getStoredEnergy());
   }

   public int getMaxEnergy() {
      return this.craftingStation == null ? 0 : this.craftingStation.getMaxEnergy();
   }

   public WorksiteInventoryAccess getSideAccess(WorksiteRelativeSide side) {
      return this.craftingStation == null ? WorksiteInventoryAccess.NONE : this.craftingStation.getSideAccess(side);
   }

   public boolean isAutoStoreOutput() {
      return this.autoStore.m_6501_() != 0;
   }

   public int getConnectionStatus() {
      return this.connectionStatus.m_6501_();
   }

   public Component getConnectionDescription() {
      BlockPos controllerPos = this.craftingStation == null ? null : this.craftingStation.getWarehousePos();
      return controllerPos == null
         ? Component.m_237115_("guistrings.warehouse.connection_missing")
         : Component.m_237110_(
            this.getConnectionStatus() == 2 ? "guistrings.warehouse.connection_offline" : "guistrings.warehouse.connection",
            new Object[]{controllerPos.m_123341_(), controllerPos.m_123342_(), controllerPos.m_123343_()}
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
      if (level.f_46443_) {
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
               if (result.m_41619_()) {
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
                           for (ItemStack remainder : recipe.m_7457_(crafting)) {
                              if (!remainder.m_41619_()) {
                                 deposits.add(remainder.m_41777_());
                              }
                           }
                        }

                        if (autoStore) {
                           deposits.add(result.m_41777_());
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
         public ItemStack m_7648_(Player player, int index) {
            return ItemStack.f_41583_;
         }

         public boolean m_6875_(Player player) {
            return true;
         }
      }, 3, 3);

      for (int slot = 0; slot < 9; slot++) {
         crafting.m_6836_(slot, container.m_8020_(1 + slot));
      }

      return crafting;
   }

   private static ItemStack getCraftingResult(Level level, Container container, CraftingContainer crafting) {
      return CraftingRecipeSelection.result(selectedRecipe(level, container, crafting), crafting, level);
   }

   private static List<Recipe<CraftingContainer>> matchingRecipes(Level level, Container container, CraftingContainer crafting) {
      ItemStack book = container.m_8020_(0);
      Set<String> completed = book.m_41619_() ? Set.of() : ResearchBookItem.getCompletedResearch(book);
      return CraftingRecipeSelection.matching(level, crafting, completed);
   }

   private static Recipe<CraftingContainer> selectedRecipe(Level level, Container container, CraftingContainer crafting) {
      ResourceLocation selectedId = container instanceof WarehouseCraftingStationBlockEntity station ? station.getSelectedRecipeId() : null;
      List<Recipe<CraftingContainer>> recipes = matchingRecipes(level, container, crafting);
      Recipe<CraftingContainer> selected = CraftingRecipeSelection.select(recipes, selectedId);
      if (container instanceof WarehouseCraftingStationBlockEntity stationx) {
         ResourceLocation normalized = selected == null ? null : selected.m_6423_();
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
      if (this.craftingStation != null && !Objects.equals(this.craftingStation.getSelectedRecipeId(), selected == null ? null : selected.m_6423_())) {
         this.craftingStation.setSelectedRecipeId(selected == null ? null : selected.m_6423_());
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

               this.station.m_6596_();
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

      public void m_6654_() {
         super.m_6654_();
         WarehouseCraftingStationMenu.this.m_6199_(this.f_40218_);
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
         if (obj instanceof WarehouseCraftingStationMenu.StackKey other && ItemStack.m_150942_(this.stack, other.stack)) {
            return true;
         }

         return false;
      }

      @Override
      public int hashCode() {
         int result = this.stack.m_41720_().hashCode();
         return 31 * result + (this.stack.m_41783_() == null ? 0 : this.stack.m_41783_().hashCode());
      }
   }
}
