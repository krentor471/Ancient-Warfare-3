package net.shadowmage.ancientwarfare.core.forge.init;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.menu.AdvancedSpawnerMenu;
import net.shadowmage.ancientwarfare.core.menu.AnimalFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.AnimalFarmControlMenu;
import net.shadowmage.ancientwarfare.core.menu.AnimalFarmMenu;
import net.shadowmage.ancientwarfare.core.menu.AutoCraftingMenu;
import net.shadowmage.ancientwarfare.core.menu.BackpackMenu;
import net.shadowmage.ancientwarfare.core.menu.BardMusicMenu;
import net.shadowmage.ancientwarfare.core.menu.ChestPushcartMenu;
import net.shadowmage.ancientwarfare.core.menu.CombatOrderMenu;
import net.shadowmage.ancientwarfare.core.menu.CropFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.CropFarmControlMenu;
import net.shadowmage.ancientwarfare.core.menu.CropFarmMenu;
import net.shadowmage.ancientwarfare.core.menu.DraftingStationMenu;
import net.shadowmage.ancientwarfare.core.menu.EngineeringStationMenu;
import net.shadowmage.ancientwarfare.core.menu.FactionTradeMenu;
import net.shadowmage.ancientwarfare.core.menu.FishFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.FishFarmControlMenu;
import net.shadowmage.ancientwarfare.core.menu.FishFarmMenu;
import net.shadowmage.ancientwarfare.core.menu.FruitFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.FruitFarmMenu;
import net.shadowmage.ancientwarfare.core.menu.MailboxMenu;
import net.shadowmage.ancientwarfare.core.menu.ManualMenu;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;
import net.shadowmage.ancientwarfare.core.menu.QuarryBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.QuarryControlMenu;
import net.shadowmage.ancientwarfare.core.menu.QuarryMenu;
import net.shadowmage.ancientwarfare.core.menu.ResearchBookMenu;
import net.shadowmage.ancientwarfare.core.menu.ResearchStationMenu;
import net.shadowmage.ancientwarfare.core.menu.RoutingOrderMenu;
import net.shadowmage.ancientwarfare.core.menu.StructureContainerMenu;
import net.shadowmage.ancientwarfare.core.menu.StructureScannerMenu;
import net.shadowmage.ancientwarfare.core.menu.TownHallMenu;
import net.shadowmage.ancientwarfare.core.menu.TradeOrderMenu;
import net.shadowmage.ancientwarfare.core.menu.TreeFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.TreeFarmMenu;
import net.shadowmage.ancientwarfare.core.menu.UpkeepOrderMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseControlMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseCraftingStationMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseInterfaceMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStockViewerMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStorageMenu;
import net.shadowmage.ancientwarfare.core.menu.WorkOrderMenu;
import net.shadowmage.ancientwarfare.core.menu.WorksiteSideConfigMenu;
import net.shadowmage.ancientwarfare.core.tile.MailboxBlockEntity;

public final class AWCoreMenuTypes {
   public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "ancientwarfare");
   public static final RegistryObject<MenuType<MailboxMenu>> MAILBOX = REGISTER.register(
      "mailbox",
      () -> IForgeMenuType.create((windowId, inv, data) -> new MailboxMenu(windowId, inv, (MailboxBlockEntity)inv.player.level().getBlockEntity(data.readBlockPos())))
   );
   public static final RegistryObject<MenuType<BackpackMenu>> BACKPACK = REGISTER.register(
      "backpack", () -> IForgeMenuType.create((windowId, inv, data) -> new BackpackMenu(windowId, inv, (InteractionHand)data.readEnum(InteractionHand.class)))
   );
   public static final RegistryObject<MenuType<ChestPushcartMenu>> CHEST_PUSHCART = REGISTER.register(
      "chest_pushcart", () -> IForgeMenuType.create((windowId, inv, data) -> new ChestPushcartMenu(windowId, inv, data.readInt()))
   );
   public static final RegistryObject<MenuType<EngineeringStationMenu>> ENGINEERING_STATION = REGISTER.register(
      "engineering_station", () -> IForgeMenuType.create((windowId, inv, data) -> new EngineeringStationMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<ResearchStationMenu>> RESEARCH_STATION = REGISTER.register(
      "research_station", () -> IForgeMenuType.create((windowId, inv, data) -> new ResearchStationMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<CropFarmMenu>> CROP_FARM = REGISTER.register(
      "crop_farm", () -> IForgeMenuType.create((windowId, inv, data) -> new CropFarmMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<CropFarmBoundsMenu>> CROP_FARM_BOUNDS = REGISTER.register(
      "crop_farm_bounds", () -> IForgeMenuType.create((windowId, inv, data) -> new CropFarmBoundsMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<CropFarmControlMenu>> CROP_FARM_CONTROL = REGISTER.register(
      "crop_farm_control", () -> IForgeMenuType.create((windowId, inv, data) -> new CropFarmControlMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<AnimalFarmMenu>> ANIMAL_FARM = REGISTER.register(
      "animal_farm", () -> IForgeMenuType.create((windowId, inv, data) -> new AnimalFarmMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<AnimalFarmBoundsMenu>> ANIMAL_FARM_BOUNDS = REGISTER.register(
      "animal_farm_bounds", () -> IForgeMenuType.create((windowId, inv, data) -> new AnimalFarmBoundsMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<AnimalFarmControlMenu>> ANIMAL_FARM_CONTROL = REGISTER.register(
      "animal_farm_control", () -> IForgeMenuType.create((windowId, inv, data) -> new AnimalFarmControlMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<TreeFarmMenu>> TREE_FARM = REGISTER.register(
      "tree_farm", () -> IForgeMenuType.create((windowId, inv, data) -> new TreeFarmMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<TreeFarmBoundsMenu>> TREE_FARM_BOUNDS = REGISTER.register(
      "tree_farm_bounds", () -> IForgeMenuType.create((windowId, inv, data) -> new TreeFarmBoundsMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<FishFarmMenu>> FISH_FARM = REGISTER.register(
      "fish_farm", () -> IForgeMenuType.create((windowId, inv, data) -> new FishFarmMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<FishFarmBoundsMenu>> FISH_FARM_BOUNDS = REGISTER.register(
      "fish_farm_bounds", () -> IForgeMenuType.create((windowId, inv, data) -> new FishFarmBoundsMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<FishFarmControlMenu>> FISH_FARM_CONTROL = REGISTER.register(
      "fish_farm_control", () -> IForgeMenuType.create((windowId, inv, data) -> new FishFarmControlMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<FruitFarmMenu>> FRUIT_FARM = REGISTER.register(
      "fruit_farm", () -> IForgeMenuType.create((windowId, inv, data) -> new FruitFarmMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<FruitFarmBoundsMenu>> FRUIT_FARM_BOUNDS = REGISTER.register(
      "fruit_farm_bounds", () -> IForgeMenuType.create((windowId, inv, data) -> new FruitFarmBoundsMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<QuarryMenu>> QUARRY = REGISTER.register(
      "quarry", () -> IForgeMenuType.create((windowId, inv, data) -> new QuarryMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<QuarryBoundsMenu>> QUARRY_BOUNDS = REGISTER.register(
      "quarry_bounds", () -> IForgeMenuType.create((windowId, inv, data) -> new QuarryBoundsMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<QuarryControlMenu>> QUARRY_CONTROL = REGISTER.register(
      "quarry_control", () -> IForgeMenuType.create((windowId, inv, data) -> new QuarryControlMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<WorksiteSideConfigMenu>> WORKSITE_SIDE_CONFIG = REGISTER.register(
      "worksite_side_config", () -> IForgeMenuType.create((windowId, inv, data) -> new WorksiteSideConfigMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<WarehouseControlMenu>> WAREHOUSE_CONTROL = REGISTER.register(
      "warehouse_control", () -> IForgeMenuType.create((windowId, inv, data) -> new WarehouseControlMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<WarehouseStorageMenu>> WAREHOUSE_STORAGE = REGISTER.register(
      "warehouse_storage", () -> IForgeMenuType.create((windowId, inv, data) -> new WarehouseStorageMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<WarehouseInterfaceMenu>> WAREHOUSE_INTERFACE = REGISTER.register(
      "warehouse_interface", () -> IForgeMenuType.create((windowId, inv, data) -> new WarehouseInterfaceMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<WarehouseCraftingStationMenu>> WAREHOUSE_CRAFTING = REGISTER.register(
      "warehouse_crafting", () -> IForgeMenuType.create((windowId, inv, data) -> new WarehouseCraftingStationMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<AutoCraftingMenu>> AUTO_CRAFTING = REGISTER.register(
      "auto_crafting", () -> IForgeMenuType.create((windowId, inv, data) -> new AutoCraftingMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<WarehouseBoundsMenu>> WAREHOUSE_BOUNDS = REGISTER.register(
      "warehouse_bounds", () -> IForgeMenuType.create((windowId, inv, data) -> new WarehouseBoundsMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<WarehouseStockViewerMenu>> WAREHOUSE_STOCK_VIEWER = REGISTER.register(
      "warehouse_stock_viewer", () -> IForgeMenuType.create((windowId, inv, data) -> new WarehouseStockViewerMenu(windowId, inv, data.readBlockPos(), false))
   );
   public static final RegistryObject<MenuType<WarehouseStockViewerMenu>> WAREHOUSE_STOCK_LINKER = REGISTER.register(
      "warehouse_stock_linker", () -> IForgeMenuType.create((windowId, inv, data) -> new WarehouseStockViewerMenu(windowId, inv, data.readBlockPos(), true))
   );
   public static final RegistryObject<MenuType<ManualMenu>> MANUAL = REGISTER.register(
      "manual", () -> IForgeMenuType.create((windowId, inv, data) -> new ManualMenu(windowId))
   );
   public static final RegistryObject<MenuType<ResearchBookMenu>> RESEARCH_BOOK = REGISTER.register(
      "research_book", () -> IForgeMenuType.create((windowId, inv, data) -> new ResearchBookMenu(windowId, inv))
   );
   public static final RegistryObject<MenuType<NpcInventoryMenu>> NPC_INVENTORY = REGISTER.register(
      "npc_inventory", () -> IForgeMenuType.create((windowId, inv, data) -> new NpcInventoryMenu(windowId, inv, data.readInt()))
   );
   public static final RegistryObject<MenuType<BardMusicMenu>> BARD_MUSIC = REGISTER.register("bard_music", () -> IForgeMenuType.create(BardMusicMenu::new));
   public static final RegistryObject<MenuType<WorkOrderMenu>> WORK_ORDER = REGISTER.register(
      "work_order",
      () -> IForgeMenuType.create((windowId, inv, data) -> new WorkOrderMenu(windowId, inv, (InteractionHand)data.readEnum(InteractionHand.class)))
   );
   public static final RegistryObject<MenuType<RoutingOrderMenu>> ROUTING_ORDER = REGISTER.register(
      "routing_order",
      () -> IForgeMenuType.create((windowId, inv, data) -> new RoutingOrderMenu(windowId, inv, (InteractionHand)data.readEnum(InteractionHand.class)))
   );
   public static final RegistryObject<MenuType<TradeOrderMenu>> TRADE_ORDER = REGISTER.register("trade_order", () -> IForgeMenuType.create(TradeOrderMenu::new));
   public static final RegistryObject<MenuType<FactionTradeMenu>> FACTION_TRADE = REGISTER.register(
      "faction_trade", () -> IForgeMenuType.create(FactionTradeMenu::new)
   );
   public static final RegistryObject<MenuType<UpkeepOrderMenu>> UPKEEP_ORDER = REGISTER.register(
      "upkeep_order",
      () -> IForgeMenuType.create((windowId, inv, data) -> new UpkeepOrderMenu(windowId, inv, (InteractionHand)data.readEnum(InteractionHand.class)))
   );
   public static final RegistryObject<MenuType<CombatOrderMenu>> COMBAT_ORDER = REGISTER.register(
      "combat_order",
      () -> IForgeMenuType.create((windowId, inv, data) -> new CombatOrderMenu(windowId, inv, (InteractionHand)data.readEnum(InteractionHand.class)))
   );
   public static final RegistryObject<MenuType<TownHallMenu>> TOWN_HALL = REGISTER.register(
      "town_hall", () -> IForgeMenuType.create((windowId, inv, data) -> new TownHallMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<StructureContainerMenu>> STRUCTURE_CONTAINER = REGISTER.register(
      "structure_container", () -> IForgeMenuType.create((windowId, inv, data) -> new StructureContainerMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<AdvancedSpawnerMenu>> ADVANCED_SPAWNER = REGISTER.register(
      "advanced_spawner", () -> IForgeMenuType.create((windowId, inv, data) -> new AdvancedSpawnerMenu(windowId, inv, data.readBlockPos()))
   );
   public static final RegistryObject<MenuType<StructureScannerMenu>> STRUCTURE_SCANNER = REGISTER.register(
      "structure_scanner", () -> IForgeMenuType.create(StructureScannerMenu::new)
   );
   public static final RegistryObject<MenuType<DraftingStationMenu>> DRAFTING_STATION = REGISTER.register(
      "drafting_station", () -> IForgeMenuType.create(DraftingStationMenu::new)
   );

   private AWCoreMenuTypes() {
   }
}
