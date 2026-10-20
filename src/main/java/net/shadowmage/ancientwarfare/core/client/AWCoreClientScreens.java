package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public final class AWCoreClientScreens {
   private AWCoreClientScreens() {
   }

   public static void registerScreens() {
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.MAILBOX), MailboxScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.BACKPACK), BackpackScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.CHEST_PUSHCART), ChestPushcartScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.MANUAL), ManualScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.ENGINEERING_STATION), EngineeringStationScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.RESEARCH_STATION), ResearchStationScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.CROP_FARM), CropFarmScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.CROP_FARM_BOUNDS), CropFarmBoundsScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.CROP_FARM_CONTROL), CropFarmControlScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.ANIMAL_FARM), AnimalFarmScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.ANIMAL_FARM_BOUNDS), AnimalFarmBoundsScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.ANIMAL_FARM_CONTROL), AnimalFarmControlScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.TREE_FARM), TreeFarmScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.TREE_FARM_BOUNDS), TreeFarmBoundsScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.FISH_FARM), FishFarmScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.FISH_FARM_BOUNDS), FishFarmBoundsScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.FISH_FARM_CONTROL), FishFarmControlScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.FRUIT_FARM), FruitFarmScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.FRUIT_FARM_BOUNDS), FruitFarmBoundsScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.QUARRY), QuarryScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.QUARRY_BOUNDS), QuarryBoundsScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.QUARRY_CONTROL), QuarryControlScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_CONTROL), WarehouseControlScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_STORAGE), WarehouseStorageScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_INTERFACE), WarehouseInterfaceScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_CRAFTING), WarehouseCraftingStationScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.AUTO_CRAFTING), AutoCraftingScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_BOUNDS), WarehouseBoundsScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_STOCK_VIEWER), WarehouseStockViewerScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.WAREHOUSE_STOCK_LINKER), WarehouseStockViewerScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.WORKSITE_SIDE_CONFIG), WorksiteSideConfigScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.RESEARCH_BOOK), ResearchBookScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.NPC_INVENTORY), NpcInventoryScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.BARD_MUSIC), BardMusicScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.WORK_ORDER_MENU), WorkOrderScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.ROUTING_ORDER_MENU), RoutingOrderScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.TRADE_ORDER_MENU), TradeOrderScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.FACTION_TRADE_MENU), FactionTradeScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.UPKEEP_ORDER_MENU), UpkeepOrderScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.COMBAT_ORDER_MENU), CombatOrderScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.TOWN_HALL_MENU), TownHallScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.STRUCTURE_CONTAINER), StructureContainerScreen::new);
      MenuScreens.m_96206_(AWCoreRegistryAccess.menu(AWCoreIds.ADVANCED_SPAWNER), AdvancedSpawnerScreen::new);
   }
}
