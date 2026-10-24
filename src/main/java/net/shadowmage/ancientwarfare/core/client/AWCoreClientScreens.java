package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public final class AWCoreClientScreens {
   private AWCoreClientScreens() {
   }

   public static void registerScreens() {
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.MAILBOX), MailboxScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.BACKPACK), BackpackScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.CHEST_PUSHCART), ChestPushcartScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.MANUAL), ManualScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.ENGINEERING_STATION), EngineeringStationScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.RESEARCH_STATION), ResearchStationScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.CROP_FARM), CropFarmScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.CROP_FARM_BOUNDS), CropFarmBoundsScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.CROP_FARM_CONTROL), CropFarmControlScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.ANIMAL_FARM), AnimalFarmScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.ANIMAL_FARM_BOUNDS), AnimalFarmBoundsScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.ANIMAL_FARM_CONTROL), AnimalFarmControlScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.TREE_FARM), TreeFarmScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.TREE_FARM_BOUNDS), TreeFarmBoundsScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.FISH_FARM), FishFarmScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.FISH_FARM_BOUNDS), FishFarmBoundsScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.FISH_FARM_CONTROL), FishFarmControlScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.FRUIT_FARM), FruitFarmScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.FRUIT_FARM_BOUNDS), FruitFarmBoundsScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.QUARRY), QuarryScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.QUARRY_BOUNDS), QuarryBoundsScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.QUARRY_CONTROL), QuarryControlScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_CONTROL), WarehouseControlScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_STORAGE), WarehouseStorageScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_INTERFACE), WarehouseInterfaceScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_CRAFTING), WarehouseCraftingStationScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.AUTO_CRAFTING), AutoCraftingScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_BOUNDS), WarehouseBoundsScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_STOCK_VIEWER), WarehouseStockViewerScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_STOCK_LINKER), WarehouseStockViewerScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.WORKSITE_SIDE_CONFIG), WorksiteSideConfigScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.RESEARCH_BOOK), ResearchBookScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.NPC_INVENTORY), NpcInventoryScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.BARD_MUSIC), BardMusicScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.WORK_ORDER_MENU), WorkOrderScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.ROUTING_ORDER_MENU), RoutingOrderScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.TRADE_ORDER_MENU), TradeOrderScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.FACTION_TRADE_MENU), FactionTradeScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.UPKEEP_ORDER_MENU), UpkeepOrderScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.COMBAT_ORDER_MENU), CombatOrderScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.TOWN_HALL_MENU), TownHallScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.STRUCTURE_CONTAINER), StructureContainerScreen::new);
      MenuScreens.register(AWCoreRegistryAccess.menu(AWCoreIds.ADVANCED_SPAWNER), AdvancedSpawnerScreen::new);
   }
}
