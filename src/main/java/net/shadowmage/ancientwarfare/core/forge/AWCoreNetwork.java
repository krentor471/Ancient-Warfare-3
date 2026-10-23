package net.shadowmage.ancientwarfare.core.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.simple.SimpleChannel;
import net.shadowmage.ancientwarfare.core.client.DiplomacyScreen;
import net.shadowmage.ancientwarfare.core.client.ChronicleScreen;
import net.shadowmage.ancientwarfare.core.client.WorldMapScreen;
import net.shadowmage.ancientwarfare.core.faction.PlayerDiplomacy;
import net.shadowmage.ancientwarfare.core.faction.FactionWorldData;
import net.shadowmage.ancientwarfare.core.faction.WorldMap;
import net.shadowmage.ancientwarfare.core.faction.RelationState;
import net.shadowmage.ancientwarfare.core.client.MailboxTargetScreen;
import net.shadowmage.ancientwarfare.core.common.npc.BardTrack;
import net.shadowmage.ancientwarfare.core.common.npc.TradeOrderData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.structure.AdvancedSpawnerConfig;
import net.shadowmage.ancientwarfare.core.common.structure.DraftingPreviewData;
import net.shadowmage.ancientwarfare.core.entity.BallistaMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.BardNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.BatteringRamEntity;
import net.shadowmage.ancientwarfare.core.entity.CannonMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.CannonTurretStandEntity;
import net.shadowmage.ancientwarfare.core.entity.CatapultMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.CatapultTurretStandEntity;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;
import net.shadowmage.ancientwarfare.core.entity.HwachaEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.TraderNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.item.CombatOrderItem;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.item.RoutingOrderItem;
import net.shadowmage.ancientwarfare.core.item.UpkeepOrderItem;
import net.shadowmage.ancientwarfare.core.item.WorkOrderItem;
import net.shadowmage.ancientwarfare.core.menu.AutoCraftingMenu;
import net.shadowmage.ancientwarfare.core.menu.DraftingStationMenu;
import net.shadowmage.ancientwarfare.core.menu.EngineeringStationMenu;
import net.shadowmage.ancientwarfare.core.menu.StructureScannerMenu;
import net.shadowmage.ancientwarfare.core.menu.TradeOrderMenu;
import net.shadowmage.ancientwarfare.core.client.ConquestScreen;
import net.shadowmage.ancientwarfare.core.faction.FactionConquest;
import net.shadowmage.ancientwarfare.core.faction.PlayerCountry;
import net.shadowmage.ancientwarfare.core.faction.FactionQuests;
import net.shadowmage.ancientwarfare.core.menu.WarehouseControlMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseCraftingStationMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseInterfaceMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStockViewerMenu;
import net.shadowmage.ancientwarfare.core.menu.WarehouseStorageMenu;
import net.shadowmage.ancientwarfare.core.structure.PlayerStructureTemplateData;
import net.shadowmage.ancientwarfare.core.tile.AdvancedSpawnerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.DraftingStationBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.MailboxBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.MailboxData;
import net.shadowmage.ancientwarfare.core.tile.WarehouseInterfaceBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStockViewerBlockEntity;

public final class AWCoreNetwork {
   private static final String PROTOCOL_VERSION = "15";
   private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation("ancientwarfare", "main"), () -> "9", "9"::equals, "9"::equals
   );

   private AWCoreNetwork() {
   }

   public static void register() {
      CHANNEL.messageBuilder(AWCoreNetwork.AddWorkOrderPointPacket.class, 0)
         .encoder(AWCoreNetwork.AddWorkOrderPointPacket::encode)
         .decoder(AWCoreNetwork.AddWorkOrderPointPacket::decode)
         .consumerMainThread(AWCoreNetwork.AddWorkOrderPointPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SetWarehouseInterfaceFilterPacket.class, 1)
         .encoder(AWCoreNetwork.SetWarehouseInterfaceFilterPacket::encode)
         .decoder(AWCoreNetwork.SetWarehouseInterfaceFilterPacket::decode)
         .consumerMainThread(AWCoreNetwork.SetWarehouseInterfaceFilterPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SetWarehouseSearchPacket.class, 2)
         .encoder(AWCoreNetwork.SetWarehouseSearchPacket::encode)
         .decoder(AWCoreNetwork.SetWarehouseSearchPacket::decode)
         .consumerMainThread(AWCoreNetwork.SetWarehouseSearchPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SetWarehouseScrollPacket.class, 18)
         .encoder(AWCoreNetwork.SetWarehouseScrollPacket::encode)
         .decoder(AWCoreNetwork.SetWarehouseScrollPacket::decode)
         .consumerMainThread(AWCoreNetwork.SetWarehouseScrollPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ClickWarehouseEntryPacket.class, 19)
         .encoder(AWCoreNetwork.ClickWarehouseEntryPacket::encode)
         .decoder(AWCoreNetwork.ClickWarehouseEntryPacket::decode)
         .consumerMainThread(AWCoreNetwork.ClickWarehouseEntryPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SetWarehouseStorageScrollPacket.class, 20)
         .encoder(AWCoreNetwork.SetWarehouseStorageScrollPacket::encode)
         .decoder(AWCoreNetwork.SetWarehouseStorageScrollPacket::decode)
         .consumerMainThread(AWCoreNetwork.SetWarehouseStorageScrollPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ClickWarehouseStorageEntryPacket.class, 21)
         .encoder(AWCoreNetwork.ClickWarehouseStorageEntryPacket::encode)
         .decoder(AWCoreNetwork.ClickWarehouseStorageEntryPacket::decode)
         .consumerMainThread(AWCoreNetwork.ClickWarehouseStorageEntryPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ChangeWarehouseStorageFilterPacket.class, 22)
         .encoder(AWCoreNetwork.ChangeWarehouseStorageFilterPacket::encode)
         .decoder(AWCoreNetwork.ChangeWarehouseStorageFilterPacket::decode)
         .consumerMainThread(AWCoreNetwork.ChangeWarehouseStorageFilterPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SetWarehouseStockFilterPacket.class, 3)
         .encoder(AWCoreNetwork.SetWarehouseStockFilterPacket::encode)
         .decoder(AWCoreNetwork.SetWarehouseStockFilterPacket::decode)
         .consumerMainThread(AWCoreNetwork.SetWarehouseStockFilterPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SetNpcSkinPacket.class, 4)
         .encoder(AWCoreNetwork.SetNpcSkinPacket::encode)
         .decoder(AWCoreNetwork.SetNpcSkinPacket::decode)
         .consumerMainThread(AWCoreNetwork.SetNpcSkinPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SetBardTracksPacket.class, 5)
         .encoder(AWCoreNetwork.SetBardTracksPacket::encode)
         .decoder(AWCoreNetwork.SetBardTracksPacket::decode)
         .consumerMainThread(AWCoreNetwork.SetBardTracksPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SetAdvancedSpawnerConfigPacket.class, 6)
         .encoder(AWCoreNetwork.SetAdvancedSpawnerConfigPacket::encode)
         .decoder(AWCoreNetwork.SetAdvancedSpawnerConfigPacket::decode)
         .consumerMainThread(AWCoreNetwork.SetAdvancedSpawnerConfigPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.VehicleInputPacket.class, 7)
         .encoder(AWCoreNetwork.VehicleInputPacket::encode)
         .decoder(AWCoreNetwork.VehicleInputPacket::decode)
         .consumerMainThread(AWCoreNetwork.VehicleInputPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.AdjustBallistaTurretPacket.class, 8)
         .encoder(AWCoreNetwork.AdjustBallistaTurretPacket::encode)
         .decoder(AWCoreNetwork.AdjustBallistaTurretPacket::decode)
         .consumerMainThread(AWCoreNetwork.AdjustBallistaTurretPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.FireBallistaPacket.class, 9)
         .encoder(AWCoreNetwork.FireBallistaPacket::encode)
         .decoder(AWCoreNetwork.FireBallistaPacket::decode)
         .consumerMainThread(AWCoreNetwork.FireBallistaPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SaveStructureScanPacket.class, 10)
         .encoder(AWCoreNetwork.SaveStructureScanPacket::encode)
         .decoder(AWCoreNetwork.SaveStructureScanPacket::decode)
         .consumerMainThread(AWCoreNetwork.SaveStructureScanPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.SelectDraftingTemplatePacket.class, 11)
         .encoder(AWCoreNetwork.SelectDraftingTemplatePacket::encode)
         .decoder(AWCoreNetwork.SelectDraftingTemplatePacket::decode)
         .consumerMainThread(AWCoreNetwork.SelectDraftingTemplatePacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ManageStructureTemplatePacket.class, 12)
         .encoder(AWCoreNetwork.ManageStructureTemplatePacket::encode)
         .decoder(AWCoreNetwork.ManageStructureTemplatePacket::decode)
         .consumerMainThread(AWCoreNetwork.ManageStructureTemplatePacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.RequestDraftingPreviewPacket.class, 23, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.RequestDraftingPreviewPacket::encode)
         .decoder(AWCoreNetwork.RequestDraftingPreviewPacket::decode)
         .consumerMainThread(AWCoreNetwork.RequestDraftingPreviewPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.DraftingPreviewPacket.class, 24, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.DraftingPreviewPacket::encode)
         .decoder(AWCoreNetwork.DraftingPreviewPacket::decode)
         .consumerMainThread(AWCoreNetwork.DraftingPreviewPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.BatonSquadPacket.class, 25, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.BatonSquadPacket::encode)
         .decoder(AWCoreNetwork.BatonSquadPacket::decode)
         .consumerMainThread(AWCoreNetwork.BatonSquadPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.BatonFormationPacket.class, 26, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.BatonFormationPacket::encode)
         .decoder(AWCoreNetwork.BatonFormationPacket::decode)
         .consumerMainThread(AWCoreNetwork.BatonFormationPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.BatonBarrageRadiusPacket.class, 38, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.BatonBarrageRadiusPacket::encode)
         .decoder(AWCoreNetwork.BatonBarrageRadiusPacket::decode)
         .consumerMainThread(AWCoreNetwork.BatonBarrageRadiusPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.CommandViewShapePacket.class, 39, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.CommandViewShapePacket::encode)
         .decoder(AWCoreNetwork.CommandViewShapePacket::decode)
         .consumerMainThread(AWCoreNetwork.CommandViewShapePacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.CommandViewFacePacket.class, 40, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.CommandViewFacePacket::encode)
         .decoder(AWCoreNetwork.CommandViewFacePacket::decode)
         .consumerMainThread(AWCoreNetwork.CommandViewFacePacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ConquestOfferPacket.class, 41, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.ConquestOfferPacket::encode)
         .decoder(AWCoreNetwork.ConquestOfferPacket::decode)
         .consumerMainThread(AWCoreNetwork.ConquestOfferPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ConquestChoicePacket.class, 42, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.ConquestChoicePacket::encode)
         .decoder(AWCoreNetwork.ConquestChoicePacket::decode)
         .consumerMainThread(AWCoreNetwork.ConquestChoicePacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.QuestBoardPacket.class, 44, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.QuestBoardPacket::encode)
         .decoder(AWCoreNetwork.QuestBoardPacket::decode)
         .consumerMainThread(AWCoreNetwork.QuestBoardPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.QuestActionPacket.class, 45, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.QuestActionPacket::encode)
         .decoder(AWCoreNetwork.QuestActionPacket::decode)
         .consumerMainThread(AWCoreNetwork.QuestActionPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ConstructionActionPacket.class, 46, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.ConstructionActionPacket::encode)
         .decoder(AWCoreNetwork.ConstructionActionPacket::decode)
         .consumerMainThread(AWCoreNetwork.ConstructionActionPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.CountryOpenPacket.class, 48, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.CountryOpenPacket::encode)
         .decoder(AWCoreNetwork.CountryOpenPacket::decode)
         .consumerMainThread(AWCoreNetwork.CountryOpenPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ResearchKnownPacket.class, 50, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.ResearchKnownPacket::encode)
         .decoder(AWCoreNetwork.ResearchKnownPacket::decode)
         .consumerMainThread(AWCoreNetwork.ResearchKnownPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.CountryDataPacket.class, 49, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.CountryDataPacket::encode)
         .decoder(AWCoreNetwork.CountryDataPacket::decode)
         .consumerMainThread(AWCoreNetwork.CountryDataPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ConstructionJobsPacket.class, 47, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.ConstructionJobsPacket::encode)
         .decoder(AWCoreNetwork.ConstructionJobsPacket::decode)
         .consumerMainThread(AWCoreNetwork.ConstructionJobsPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.FoundCountryPacket.class, 43, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.FoundCountryPacket::encode)
         .decoder(AWCoreNetwork.FoundCountryPacket::decode)
         .consumerMainThread(AWCoreNetwork.FoundCountryPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.BatonStancePacket.class, 29, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.BatonStancePacket::encode)
         .decoder(AWCoreNetwork.BatonStancePacket::decode)
         .consumerMainThread(AWCoreNetwork.BatonStancePacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.DiplomacyOpenPacket.class, 30, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.DiplomacyOpenPacket::encode)
         .decoder(AWCoreNetwork.DiplomacyOpenPacket::decode)
         .consumerMainThread(AWCoreNetwork.DiplomacyOpenPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.DiplomacyActionPacket.class, 31, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.DiplomacyActionPacket::encode)
         .decoder(AWCoreNetwork.DiplomacyActionPacket::decode)
         .consumerMainThread(AWCoreNetwork.DiplomacyActionPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.DiplomacyDataPacket.class, 32, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.DiplomacyDataPacket::encode)
         .decoder(AWCoreNetwork.DiplomacyDataPacket::decode)
         .consumerMainThread(AWCoreNetwork.DiplomacyDataPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.DealPacket.class, 51, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.DealPacket::encode)
         .decoder(AWCoreNetwork.DealPacket::decode)
         .consumerMainThread(AWCoreNetwork.DealPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.DealDataPacket.class, 52, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.DealDataPacket::encode)
         .decoder(AWCoreNetwork.DealDataPacket::decode)
         .consumerMainThread(AWCoreNetwork.DealDataPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.WorldMapOpenPacket.class, 33, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.WorldMapOpenPacket::encode)
         .decoder(AWCoreNetwork.WorldMapOpenPacket::decode)
         .consumerMainThread(AWCoreNetwork.WorldMapOpenPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.WorldMapDataPacket.class, 34, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.WorldMapDataPacket::encode)
         .decoder(AWCoreNetwork.WorldMapDataPacket::decode)
         .consumerMainThread(AWCoreNetwork.WorldMapDataPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.WorldMapTeleportPacket.class, 35, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.WorldMapTeleportPacket::encode)
         .decoder(AWCoreNetwork.WorldMapTeleportPacket::decode)
         .consumerMainThread(AWCoreNetwork.WorldMapTeleportPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ChronicleOpenPacket.class, 36, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.ChronicleOpenPacket::encode)
         .decoder(AWCoreNetwork.ChronicleOpenPacket::decode)
         .consumerMainThread(AWCoreNetwork.ChronicleOpenPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.ChronicleDataPacket.class, 37, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AWCoreNetwork.ChronicleDataPacket::encode)
         .decoder(AWCoreNetwork.ChronicleDataPacket::decode)
         .consumerMainThread(AWCoreNetwork.ChronicleDataPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.CommandViewSelectPacket.class, 27, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.CommandViewSelectPacket::encode)
         .decoder(AWCoreNetwork.CommandViewSelectPacket::decode)
         .consumerMainThread(AWCoreNetwork.CommandViewSelectPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.CommandViewOrderPacket.class, 28, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AWCoreNetwork.CommandViewOrderPacket::encode)
         .decoder(AWCoreNetwork.CommandViewOrderPacket::decode)
         .consumerMainThread(AWCoreNetwork.CommandViewOrderPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.MailboxConfigPacket.class, 13)
         .encoder(AWCoreNetwork.MailboxConfigPacket::encode)
         .decoder(AWCoreNetwork.MailboxConfigPacket::decode)
         .consumerMainThread(AWCoreNetwork.MailboxConfigPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.MailboxListPacket.class, 14)
         .encoder(AWCoreNetwork.MailboxListPacket::encode)
         .decoder(AWCoreNetwork.MailboxListPacket::decode)
         .consumerMainThread(AWCoreNetwork.MailboxListPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.TransferEngineeringRecipePacket.class, 15)
         .encoder(AWCoreNetwork.TransferEngineeringRecipePacket::encode)
         .decoder(AWCoreNetwork.TransferEngineeringRecipePacket::decode)
         .consumerMainThread(AWCoreNetwork.TransferEngineeringRecipePacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.CommandBatonHotkeyPacket.class, 16)
         .encoder(AWCoreNetwork.CommandBatonHotkeyPacket::encode)
         .decoder(AWCoreNetwork.CommandBatonHotkeyPacket::decode)
         .consumerMainThread(AWCoreNetwork.CommandBatonHotkeyPacket::handle)
         .add();
      CHANNEL.messageBuilder(AWCoreNetwork.TraderRoutePacket.class, 17)
         .encoder(AWCoreNetwork.TraderRoutePacket::encode)
         .decoder(AWCoreNetwork.TraderRoutePacket::decode)
         .consumerMainThread(AWCoreNetwork.TraderRoutePacket::handle)
         .add();
   }

   public static void sendAddWorkOrderPoint(BlockPos pos, Direction side) {
      CHANNEL.sendToServer(new AWCoreNetwork.AddWorkOrderPointPacket(pos, side));
   }

   public static void sendSetWarehouseInterfaceFilter(BlockPos pos, int index, ItemStack stack, int quantity) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetWarehouseInterfaceFilterPacket(pos, 0, index, stack, quantity));
   }

   public static void sendAddWarehouseInterfaceFilter(BlockPos pos) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetWarehouseInterfaceFilterPacket(pos, 1, 0, ItemStack.EMPTY, 0));
   }

   public static void sendRemoveWarehouseInterfaceFilter(BlockPos pos, int index) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetWarehouseInterfaceFilterPacket(pos, 2, index, ItemStack.EMPTY, 0));
   }

   public static void sendSetWarehouseSearch(String search) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetWarehouseSearchPacket(search));
   }

   public static void sendSetWarehouseScrollRow(int row) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetWarehouseScrollPacket(Math.max(0, row)));
   }

   public static void sendClickWarehouseEntry(int containerId, int slot, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
      CHANNEL.sendToServer(new AWCoreNetwork.ClickWarehouseEntryPacket(containerId, slot, button, clickType, expectedItem, expectedCarried));
   }

   public static void sendSetWarehouseStorageScroll(int containerId, int pixels) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetWarehouseStorageScrollPacket(containerId, Math.max(0, pixels)));
   }

   public static void sendClickWarehouseStorageEntry(
      int containerId, int entry, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried
   ) {
      CHANNEL.sendToServer(new AWCoreNetwork.ClickWarehouseStorageEntryPacket(containerId, entry, button, clickType, expectedItem, expectedCarried));
   }

   public static void sendChangeWarehouseStorageFilter(
      int containerId, int action, int index, int expectedCount, ItemStack expectedFilter, ItemStack expectedCarried
   ) {
      CHANNEL.sendToServer(new AWCoreNetwork.ChangeWarehouseStorageFilterPacket(containerId, action, index, expectedCount, expectedFilter, expectedCarried));
   }

   public static void sendSetWarehouseStockFilter(BlockPos pos, int index, ItemStack stack) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetWarehouseStockFilterPacket(pos, index, stack));
   }

   public static void sendSetNpcSkin(int entityId, String skinName) {
      sendSetNpcSkin(entityId, skinName, false);
   }

   public static void sendSetNpcSkin(int entityId, String skinName, boolean alexModel) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetNpcSkinPacket(entityId, skinName, alexModel));
   }

   public static void sendSetBardTracks(int entityId, List<BardTrack> tracks) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetBardTracksPacket(entityId, tracks));
   }

   public static void sendSetAdvancedSpawnerConfig(BlockPos pos, AdvancedSpawnerConfig config) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetAdvancedSpawnerConfigPacket(pos, config));
   }

   public static void sendVehicleInput(float forward, float turn) {
      CHANNEL.sendToServer(new AWCoreNetwork.VehicleInputPacket(forward, turn));
   }

   public static void sendAdjustBallistaTurret(float pitchDelta, float yawDelta) {
      CHANNEL.sendToServer(new AWCoreNetwork.AdjustBallistaTurretPacket(pitchDelta, yawDelta));
   }

   public static void sendFireBallista() {
      CHANNEL.sendToServer(new AWCoreNetwork.FireBallistaPacket());
   }

   public static void sendTransferEngineeringRecipe(ResourceLocation recipeId) {
      CHANNEL.sendToServer(new AWCoreNetwork.TransferEngineeringRecipePacket(recipeId));
   }

   public static void sendCommandBatonHotkey() {
      CHANNEL.sendToServer(new AWCoreNetwork.CommandBatonHotkeyPacket());
   }

   public static void sendCommandViewSelect(int[] entityIds, boolean add) {
      CHANNEL.sendToServer(new AWCoreNetwork.CommandViewSelectPacket(entityIds, add));
   }

   public static void sendCommandViewOrder(CommandBatonItem.ViewOrder order, BlockPos pos, int entityId, boolean queued) {
      CHANNEL.sendToServer(new AWCoreNetwork.CommandViewOrderPacket(order, pos, pos, entityId, queued));
   }

   /** Area fire on {@code center}; when {@code edge} differs, the circle reaches out to it. */
   public static void sendCommandViewBarrage(BlockPos center, BlockPos edge) {
      CHANNEL.sendToServer(new AWCoreNetwork.CommandViewOrderPacket(CommandBatonItem.ViewOrder.BARRAGE, center, edge, -1, false));
   }

   /** Move order with the front of the formation drawn from {@code start} to {@code end}. */
   public static void sendCommandViewFront(BlockPos start, BlockPos end, boolean queued) {
      CHANNEL.sendToServer(new AWCoreNetwork.CommandViewOrderPacket(CommandBatonItem.ViewOrder.FRONT, start, end, -1, queued));
   }

   public static void sendBatonFormation() {
      CHANNEL.sendToServer(new AWCoreNetwork.BatonFormationPacket());
   }

   public static void sendDiplomacyOpen() {
      CHANNEL.sendToServer(new AWCoreNetwork.DiplomacyOpenPacket());
   }

   public static void sendDiplomacyAction(String faction, PlayerDiplomacy.Action action) {
      CHANNEL.sendToServer(new AWCoreNetwork.DiplomacyActionPacket(faction, action.ordinal()));
   }

   public static void sendWorldMapOpen() {
      CHANNEL.sendToServer(new AWCoreNetwork.WorldMapOpenPacket());
   }

   public static void sendChronicleOpen() {
      CHANNEL.sendToServer(new AWCoreNetwork.ChronicleOpenPacket());
   }

   public static void sendConquestOffer(ServerPlayer player, FactionConquest.Offer offer) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AWCoreNetwork.ConquestOfferPacket(offer));
   }

   public static void sendQuestBoard(
      ServerPlayer player, BlockPos board, Component title, Component note, List<FactionQuests.Line> offers, List<FactionQuests.Line> active
   ) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AWCoreNetwork.QuestBoardPacket(board, title, note, offers, active));
   }

   public static void sendConstructionCreate(BlockPos min, BlockPos max, int kind, String template, int facing) {
      CHANNEL.sendToServer(new AWCoreNetwork.ConstructionActionPacket(ConstructionActionPacket.CREATE, min, max, kind, template == null ? "" : template, facing));
   }

   public static void sendCountryOpen() {
      CHANNEL.sendToServer(new AWCoreNetwork.CountryOpenPacket());
   }

   public static void sendResearchKnown(ServerPlayer player, java.util.Set<String> known) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AWCoreNetwork.ResearchKnownPacket(List.copyOf(known)));
   }

   public static void sendCountryData(ServerPlayer player, net.shadowmage.ancientwarfare.core.faction.PlayerCountry.CountryView view) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AWCoreNetwork.CountryDataPacket(view));
   }

   public static void sendConstructionCancel(int id) {
      CHANNEL.sendToServer(new AWCoreNetwork.ConstructionActionPacket(ConstructionActionPacket.CANCEL, BlockPos.ZERO, BlockPos.ZERO, id, "", 0));
   }

   public static void sendConstructionJobs(ServerPlayer player, List<net.shadowmage.ancientwarfare.core.construction.ConstructionJobs.Job> jobs) {
      List<ConstructionJobsPacket.Entry> entries = new ArrayList<>();
      for (var job : jobs) {
         entries.add(new ConstructionJobsPacket.Entry(job.id(), job.kind().ordinal(), job.min(), job.max(), job.done(), job.total(), job.kind() == net.shadowmage.ancientwarfare.core.construction.ConstructionJobs.Kind.TEMPLATE ? job.title() : job.state().getBlock().getName()));
      }

      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AWCoreNetwork.ConstructionJobsPacket(entries));
   }

   public static void sendQuestAction(BlockPos board, int action, long argument) {
      CHANNEL.sendToServer(new AWCoreNetwork.QuestActionPacket(board, action, argument));
   }

   public static void sendFoundCountry(BlockPos hall, String name) {
      CHANNEL.sendToServer(new AWCoreNetwork.FoundCountryPacket(hall, name));
   }

   public static void sendConquestChoice(int settlement, FactionConquest.Choice choice) {
      CHANNEL.sendToServer(new AWCoreNetwork.ConquestChoicePacket(settlement, choice));
   }

   public static void sendChronicle(ServerPlayer player, List<FactionWorldData.ChronicleEntry> entries) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AWCoreNetwork.ChronicleDataPacket(entries));
   }

   public static void sendWorldMapTeleport(int x, int z) {
      CHANNEL.sendToServer(new AWCoreNetwork.WorldMapTeleportPacket(x, z));
   }

   public static void sendWorldMap(ServerPlayer player, WorldMap.Data data) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AWCoreNetwork.WorldMapDataPacket(data));
   }

   public static void sendDeal(String faction, int mode, net.shadowmage.ancientwarfare.core.faction.FactionDeals.Offer offer) {
      CHANNEL.sendToServer(new AWCoreNetwork.DealPacket(faction, mode, offer));
   }

   public static void sendDealData(ServerPlayer player, boolean open, net.shadowmage.ancientwarfare.core.faction.FactionDeals.View view) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AWCoreNetwork.DealDataPacket(open, view));
   }

   public static void sendDiplomacyData(ServerPlayer player, String focus, boolean open, List<PlayerDiplomacy.Entry> entries) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AWCoreNetwork.DiplomacyDataPacket(focus, open, entries));
   }

   public static void sendBatonBarrageRadius() {
      CHANNEL.sendToServer(new AWCoreNetwork.BatonBarrageRadiusPacket());
   }

   /** Move along a line or loop drawn in the command view. */
   public static void sendCommandViewShape(List<BlockPos> points, boolean queued) {
      CHANNEL.sendToServer(new AWCoreNetwork.CommandViewShapePacket(points, queued));
   }

   public static void sendCommandViewFace(BlockPos point) {
      CHANNEL.sendToServer(new AWCoreNetwork.CommandViewFacePacket(point));
   }

   public static void sendBatonStance() {
      CHANNEL.sendToServer(new AWCoreNetwork.BatonStancePacket());
   }

   public static void sendBatonSquad(int slot, boolean assign) {
      CHANNEL.sendToServer(new AWCoreNetwork.BatonSquadPacket(slot, assign));
   }

   public static void sendTraderToRoute(int traderEntityId, BlockPos target) {
      CHANNEL.sendToServer(new AWCoreNetwork.TraderRoutePacket(traderEntityId, target));
   }

   public static void sendSaveStructureScan(BlockPos pos, String name) {
      CHANNEL.sendToServer(new AWCoreNetwork.SaveStructureScanPacket(pos, name));
   }

   public static void sendSelectDraftingTemplate(BlockPos pos, String name, boolean start) {
      CHANNEL.sendToServer(new AWCoreNetwork.SelectDraftingTemplatePacket(pos, name, start));
   }

   public static void sendManageStructureTemplate(BlockPos pos, String name, boolean delete) {
      CHANNEL.sendToServer(new AWCoreNetwork.ManageStructureTemplatePacket(pos, name, delete));
   }

   public static void requestDraftingPreview(int containerId, int templateIndex, int requestId) {
      CHANNEL.sendToServer(new AWCoreNetwork.RequestDraftingPreviewPacket(containerId, templateIndex, requestId));
   }

   public static void sendMailboxConfig(BlockPos pos, int action, String value, boolean flag) {
      CHANNEL.sendToServer(new AWCoreNetwork.MailboxConfigPacket(pos, action, value == null ? "" : value, flag));
   }

   public static void requestMailboxList(BlockPos pos) {
      CHANNEL.sendToServer(new AWCoreNetwork.MailboxListPacket(pos, List.of()));
   }

   public static record AddWorkOrderPointPacket(BlockPos pos, Direction side) {
      private static void encode(AWCoreNetwork.AddWorkOrderPointPacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.pos);
         buffer.writeEnum(packet.side);
      }

      private static AWCoreNetwork.AddWorkOrderPointPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.AddWorkOrderPointPacket(buffer.readBlockPos(), (Direction)buffer.readEnum(Direction.class));
      }

      private static void handle(AWCoreNetwork.AddWorkOrderPointPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !CombatOrderItem.handleTargetedBlock(context.getSender(), packet.pos)
                  && !UpkeepOrderItem.setTargetedBlock(context.getSender(), packet.pos, packet.side)
                  && !RoutingOrderItem.addTargetedBlock(context.getSender(), packet.pos, packet.side)) {
                  WorkOrderItem.addTargetedBlock(context.getSender(), packet.pos);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record AdjustBallistaTurretPacket(float pitchDelta, float yawDelta) {
      private static void encode(AWCoreNetwork.AdjustBallistaTurretPacket packet, FriendlyByteBuf buffer) {
         buffer.writeFloat(packet.pitchDelta);
         buffer.writeFloat(packet.yawDelta);
      }

      private static AWCoreNetwork.AdjustBallistaTurretPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.AdjustBallistaTurretPacket(buffer.readFloat(), buffer.readFloat());
      }

      private static void handle(AWCoreNetwork.AdjustBallistaTurretPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() != null) {
               if (context.getSender().getVehicle() instanceof CannonMobileEntity cannonMobile) {
                  cannonMobile.applyStandInput(packet.pitchDelta, 0.0F);
               } else if (context.getSender().getVehicle() instanceof BallistaMobileEntity ballista) {
                  ballista.adjustTurret(packet.pitchDelta, packet.yawDelta);
               } else if (context.getSender().getVehicle() instanceof CatapultMobileEntity catapult) {
                  catapult.adjustTurret(packet.yawDelta);
                  catapult.adjustLaunchVelocity(packet.pitchDelta * 0.01F);
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record ChangeWarehouseStorageFilterPacket(
      int containerId, int action, int index, int expectedCount, ItemStack expectedFilter, ItemStack expectedCarried
   ) {
      public static void encode(AWCoreNetwork.ChangeWarehouseStorageFilterPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.containerId);
         buffer.writeVarInt(packet.action);
         buffer.writeVarInt(packet.index);
         buffer.writeVarInt(packet.expectedCount);
         buffer.writeItem(packet.expectedFilter.isEmpty() ? ItemStack.EMPTY : packet.expectedFilter.copyWithCount(1));
         buffer.writeItem(packet.expectedCarried);
      }

      public static AWCoreNetwork.ChangeWarehouseStorageFilterPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ChangeWarehouseStorageFilterPacket(
            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readItem(), buffer.readItem()
         );
      }

      private static void handle(AWCoreNetwork.ChangeWarehouseStorageFilterPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().isSpectator()
                  && context.getSender().containerMenu instanceof WarehouseStorageMenu menu
                  && menu.containerId == packet.containerId
                  && menu.stillValid(context.getSender())) {
                  menu.handleFilterAction(context.getSender(), packet.action, packet.index, packet.expectedCount, packet.expectedFilter, packet.expectedCarried);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record ClickWarehouseEntryPacket(int containerId, int slot, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
      public static void encode(AWCoreNetwork.ClickWarehouseEntryPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.containerId);
         buffer.writeVarInt(packet.slot);
         buffer.writeVarInt(packet.button);
         buffer.writeEnum(packet.clickType);
         buffer.writeItem(packet.expectedItem.isEmpty() ? ItemStack.EMPTY : packet.expectedItem.copyWithCount(1));
         buffer.writeItem(packet.expectedCarried);
      }

      public static AWCoreNetwork.ClickWarehouseEntryPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ClickWarehouseEntryPacket(
            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), (ClickType)buffer.readEnum(ClickType.class), buffer.readItem(), buffer.readItem()
         );
      }

      private static void handle(AWCoreNetwork.ClickWarehouseEntryPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().isSpectator()
                  && context.getSender().containerMenu instanceof WarehouseControlMenu menu
                  && menu.containerId == packet.containerId
                  && menu.stillValid(context.getSender())) {
                  menu.handleVirtualClick(context.getSender(), packet.slot, packet.button, packet.clickType, packet.expectedItem, packet.expectedCarried);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record ClickWarehouseStorageEntryPacket(
      int containerId, int entry, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried
   ) {
      public static void encode(AWCoreNetwork.ClickWarehouseStorageEntryPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.containerId);
         buffer.writeVarInt(packet.entry);
         buffer.writeVarInt(packet.button);
         buffer.writeEnum(packet.clickType);
         buffer.writeItem(packet.expectedItem.isEmpty() ? ItemStack.EMPTY : packet.expectedItem.copyWithCount(1));
         buffer.writeItem(packet.expectedCarried);
      }

      public static AWCoreNetwork.ClickWarehouseStorageEntryPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ClickWarehouseStorageEntryPacket(
            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), (ClickType)buffer.readEnum(ClickType.class), buffer.readItem(), buffer.readItem()
         );
      }

      private static void handle(AWCoreNetwork.ClickWarehouseStorageEntryPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().isSpectator()
                  && context.getSender().containerMenu instanceof WarehouseStorageMenu menu
                  && menu.containerId == packet.containerId
                  && menu.stillValid(context.getSender())) {
                  menu.handleVirtualClick(context.getSender(), packet.entry, packet.button, packet.clickType, packet.expectedItem, packet.expectedCarried);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record CommandBatonHotkeyPacket() {
      private static void encode(AWCoreNetwork.CommandBatonHotkeyPacket packet, FriendlyByteBuf buffer) {
      }

      private static AWCoreNetwork.CommandBatonHotkeyPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.CommandBatonHotkeyPacket();
      }

      private static void handle(AWCoreNetwork.CommandBatonHotkeyPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               ItemStack stack = player.getMainHandItem();
               if (!(stack.getItem() instanceof CommandBatonItem)) {
                  stack = player.getOffhandItem();
               }

               CommandBatonItem.clearSelections(stack, player);
            }
         });
         context.setPacketHandled(true);
      }
   }

   private static ItemStack heldBaton(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      return stack.getItem() instanceof CommandBatonItem ? stack : player.getOffhandItem();
   }

   public static record CommandViewSelectPacket(int[] entityIds, boolean add) {
      private static final int MAX_IDS = 512;

      private static void encode(AWCoreNetwork.CommandViewSelectPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarIntArray(packet.entityIds);
         buffer.writeBoolean(packet.add);
      }

      private static AWCoreNetwork.CommandViewSelectPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.CommandViewSelectPacket(buffer.readVarIntArray(MAX_IDS), buffer.readBoolean());
      }

      private static void handle(AWCoreNetwork.CommandViewSelectPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               CommandBatonItem.commandViewSelect(heldBaton(player), player, packet.entityIds, packet.add);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record CommandViewOrderPacket(CommandBatonItem.ViewOrder order, BlockPos pos, BlockPos end, int entityId, boolean queued) {
      private static void encode(AWCoreNetwork.CommandViewOrderPacket packet, FriendlyByteBuf buffer) {
         buffer.writeEnum(packet.order);
         buffer.writeBlockPos(packet.pos);
         buffer.writeBlockPos(packet.end);
         buffer.writeVarInt(packet.entityId);
         buffer.writeBoolean(packet.queued);
      }

      private static AWCoreNetwork.CommandViewOrderPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.CommandViewOrderPacket(buffer.readEnum(CommandBatonItem.ViewOrder.class), buffer.readBlockPos(), buffer.readBlockPos(), buffer.readVarInt(), buffer.readBoolean()
         );
      }

      private static void handle(AWCoreNetwork.CommandViewOrderPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               CommandBatonItem.commandViewOrder(heldBaton(player), player, packet.order, packet.pos, packet.end, packet.entityId, packet.queued);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record DiplomacyOpenPacket() {
      private static void encode(AWCoreNetwork.DiplomacyOpenPacket packet, FriendlyByteBuf buffer) {
      }

      private static AWCoreNetwork.DiplomacyOpenPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.DiplomacyOpenPacket();
      }

      private static void handle(AWCoreNetwork.DiplomacyOpenPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               PlayerDiplomacy.open(player, "");
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record DiplomacyActionPacket(String faction, int action) {
      private static void encode(AWCoreNetwork.DiplomacyActionPacket packet, FriendlyByteBuf buffer) {
         buffer.writeUtf(packet.faction, 64);
         buffer.writeVarInt(packet.action);
      }

      private static AWCoreNetwork.DiplomacyActionPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.DiplomacyActionPacket(buffer.readUtf(64), buffer.readVarInt());
      }

      private static void handle(AWCoreNetwork.DiplomacyActionPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               PlayerDiplomacy.act(player, packet.faction, PlayerDiplomacy.Action.byId(packet.action));
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record DealPacket(String faction, int mode, net.shadowmage.ancientwarfare.core.faction.FactionDeals.Offer offer) {
      private static void encode(AWCoreNetwork.DealPacket packet, FriendlyByteBuf buffer) {
         buffer.writeUtf(packet.faction, 64);
         buffer.writeVarInt(packet.mode);
         writeOffer(buffer, packet.offer);
      }

      private static AWCoreNetwork.DealPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.DealPacket(buffer.readUtf(64), buffer.readVarInt(), readOffer(buffer));
      }

      private static void handle(AWCoreNetwork.DealPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               net.shadowmage.ancientwarfare.core.faction.FactionDeals.handle(player, packet.faction, packet.mode, packet.offer);
            }
         });
         context.setPacketHandled(true);
      }
   }

   private static void writeOffer(FriendlyByteBuf buffer, net.shadowmage.ancientwarfare.core.faction.FactionDeals.Offer offer) {
      buffer.writeVarInt(offer.playerGold());
      buffer.writeVarInt(offer.tribute());
      buffer.writeUtf(offer.playerWar(), 64);
      buffer.writeVarInt(offer.factionGold());
      buffer.writeBoolean(offer.treaty());
      buffer.writeBoolean(offer.trade());
      buffer.writeUtf(offer.factionWar(), 64);
      buffer.writeVarIntArray(offer.goods());
      buffer.writeVarInt(offer.bond());
   }

   private static net.shadowmage.ancientwarfare.core.faction.FactionDeals.Offer readOffer(FriendlyByteBuf buffer) {
      return new net.shadowmage.ancientwarfare.core.faction.FactionDeals.Offer(
         Math.max(0, Math.min(10000, buffer.readVarInt())), Math.max(0, Math.min(1000, buffer.readVarInt())), buffer.readUtf(64),
         Math.max(0, Math.min(10000, buffer.readVarInt())), buffer.readBoolean(), buffer.readBoolean(), buffer.readUtf(64),
         java.util.Arrays.stream(buffer.readVarIntArray(8)).map(load -> Math.max(0, Math.min(1000, load))).toArray(), Math.max(0, Math.min(net.shadowmage.ancientwarfare.core.faction.FactionDeals.GIVE_DAUGHTER, buffer.readVarInt()))
      );
   }

   private static void writeNames(FriendlyByteBuf buffer, List<String> names) {
      buffer.writeVarInt(Math.min(32, names.size()));
      names.stream().limit(32).forEach(name -> buffer.writeUtf(name, 64));
   }

   private static List<String> readNames(FriendlyByteBuf buffer) {
      int count = Math.min(32, buffer.readVarInt());
      List<String> names = new ArrayList<>();
      for (int i = 0; i < count; i++) {
         names.add(buffer.readUtf(64));
      }

      return names;
   }

   public static record DealDataPacket(boolean open, net.shadowmage.ancientwarfare.core.faction.FactionDeals.View view) {
      private static void encode(AWCoreNetwork.DealDataPacket packet, FriendlyByteBuf buffer) {
         net.shadowmage.ancientwarfare.core.faction.FactionDeals.View view = packet.view;
         buffer.writeBoolean(packet.open);
         buffer.writeUtf(view.faction(), 64);
         buffer.writeVarInt(view.treaty().ordinal());
         buffer.writeBoolean(view.trade());
         buffer.writeVarInt(view.reliability());
         writeNames(buffer, view.playerTargets());
         writeNames(buffer, view.factionTargets());
         buffer.writeVarInt(Math.min(8, view.promises().size()));
         view.promises().stream().limit(8).forEach(buffer::writeComponent);
         buffer.writeVarInt(view.verdict());
         buffer.writeComponent(view.answer());
         writeOffer(buffer, view.offer());
      }

      private static AWCoreNetwork.DealDataPacket decode(FriendlyByteBuf buffer) {
         boolean open = buffer.readBoolean();
         String faction = buffer.readUtf(64);
         RelationState treaty = RelationState.values()[Math.floorMod(buffer.readVarInt(), RelationState.values().length)];
         boolean trade = buffer.readBoolean();
         int reliability = buffer.readVarInt();
         List<String> playerTargets = readNames(buffer);
         List<String> factionTargets = readNames(buffer);
         int count = Math.min(8, buffer.readVarInt());
         List<net.minecraft.network.chat.Component> promises = new ArrayList<>();
         for (int i = 0; i < count; i++) {
            promises.add(buffer.readComponent());
         }

         int verdict = buffer.readVarInt();
         net.minecraft.network.chat.Component answer = buffer.readComponent();
         return new AWCoreNetwork.DealDataPacket(
            open,
            new net.shadowmage.ancientwarfare.core.faction.FactionDeals.View(
               faction, treaty, trade, reliability, playerTargets, factionTargets, promises, verdict, answer, readOffer(buffer)
            )
         );
      }

      private static void handle(AWCoreNetwork.DealDataPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               net.shadowmage.ancientwarfare.core.client.DiplomacyScreen.acceptDeal(packet.view);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record WorldMapOpenPacket() {
      private static void encode(AWCoreNetwork.WorldMapOpenPacket packet, FriendlyByteBuf buffer) {
      }

      private static AWCoreNetwork.WorldMapOpenPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.WorldMapOpenPacket();
      }

      private static void handle(AWCoreNetwork.WorldMapOpenPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               WorldMap.open(player);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record ChronicleOpenPacket() {
      private static void encode(AWCoreNetwork.ChronicleOpenPacket packet, FriendlyByteBuf buffer) {
      }

      private static AWCoreNetwork.ChronicleOpenPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ChronicleOpenPacket();
      }

      private static void handle(AWCoreNetwork.ChronicleOpenPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               sendChronicle(player, FactionWorldData.get(player.serverLevel()).chronicle());
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record ChronicleDataPacket(List<FactionWorldData.ChronicleEntry> entries) {
      private static final int MAX_TEXT = 8192;

      private static void encode(AWCoreNetwork.ChronicleDataPacket packet, FriendlyByteBuf buffer) {
         List<FactionWorldData.ChronicleEntry> entries = packet.entries.stream().filter(entry -> entry.json().length() <= MAX_TEXT).toList();
         buffer.writeVarInt(entries.size());

         for (FactionWorldData.ChronicleEntry entry : entries) {
            buffer.writeVarLong(entry.day());
            buffer.writeUtf(entry.json(), MAX_TEXT);
         }
      }

      private static AWCoreNetwork.ChronicleDataPacket decode(FriendlyByteBuf buffer) {
         int count = Math.min(FactionWorldData.CHRONICLE_SIZE, buffer.readVarInt());
         List<FactionWorldData.ChronicleEntry> entries = new ArrayList<>();

         for (int i = 0; i < count; i++) {
            entries.add(new FactionWorldData.ChronicleEntry(buffer.readVarLong(), buffer.readUtf(MAX_TEXT)));
         }

         return new AWCoreNetwork.ChronicleDataPacket(entries);
      }

      private static void handle(AWCoreNetwork.ChronicleDataPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               List<ChronicleScreen.Entry> entries = new ArrayList<>();

               for (FactionWorldData.ChronicleEntry entry : packet.entries) {
                  Component text = Component.Serializer.fromJson(entry.json());
                  if (text != null) {
                     entries.add(new ChronicleScreen.Entry(entry.day(), text));
                  }
               }

               ChronicleScreen.open(entries);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record WorldMapTeleportPacket(int x, int z) {
      private static void encode(AWCoreNetwork.WorldMapTeleportPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.x);
         buffer.writeVarInt(packet.z);
      }

      private static AWCoreNetwork.WorldMapTeleportPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.WorldMapTeleportPacket(buffer.readVarInt(), buffer.readVarInt());
      }

      private static void handle(AWCoreNetwork.WorldMapTeleportPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               WorldMap.teleport(player, packet.x, packet.z);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record WorldMapDataPacket(WorldMap.Data data) {
      private static final int MAX_ITEMS = 4096;

      private static void encode(AWCoreNetwork.WorldMapDataPacket packet, FriendlyByteBuf buffer) {
         WorldMap.Data data = packet.data;
         buffer.writeInt(data.originX());
         buffer.writeInt(data.originZ());
         buffer.writeByteArray(data.owners());
         buffer.writeByteArray(data.terrain());
         buffer.writeByteArray(data.rivers());
         buffer.writeVarInt(data.factions().size());
         data.factions().forEach(faction -> buffer.writeUtf(faction, 64));
         List<WorldMap.Marker> markers = data.markers().subList(0, Math.min(MAX_ITEMS, data.markers().size()));
         buffer.writeVarInt(markers.size());

         for (WorldMap.Marker marker : markers) {
            buffer.writeInt(marker.x());
            buffer.writeInt(marker.z());
            buffer.writeEnum(marker.kind());
            buffer.writeVarInt(marker.faction());
            buffer.writeUtf(marker.label(), 128);
            buffer.writeUtf(marker.name(), 64);
            buffer.writeVarIntArray(marker.life());
         }

         List<int[]> roads = data.roads().subList(0, Math.min(MAX_ITEMS, data.roads().size()));
         buffer.writeVarInt(roads.size());
         roads.forEach(buffer::writeVarIntArray);
         List<int[]> marches = data.marches().subList(0, Math.min(MAX_ITEMS, data.marches().size()));
         buffer.writeVarInt(marches.size());
         marches.forEach(buffer::writeVarIntArray);
         buffer.writeBoolean(data.canTeleport());
      }

      private static AWCoreNetwork.WorldMapDataPacket decode(FriendlyByteBuf buffer) {
         int originX = buffer.readInt();
         int originZ = buffer.readInt();
         byte[] owners = buffer.readByteArray(WorldMap.SIZE * WorldMap.SIZE);
         byte[] terrain = buffer.readByteArray(WorldMap.SIZE * WorldMap.SIZE);
         byte[] rivers = buffer.readByteArray(WorldMap.SIZE * WorldMap.SIZE * WorldMap.RIVER_DETAIL * WorldMap.RIVER_DETAIL / 8 + 1024);
         int factionCount = Math.min(256, buffer.readVarInt());
         List<String> factions = new ArrayList<>();

         for (int i = 0; i < factionCount; i++) {
            factions.add(buffer.readUtf(64));
         }

         int markerCount = Math.min(MAX_ITEMS, buffer.readVarInt());
         List<WorldMap.Marker> markers = new ArrayList<>();

         for (int i = 0; i < markerCount; i++) {
            markers.add(
               new WorldMap.Marker(
                  buffer.readInt(), buffer.readInt(), buffer.readEnum(WorldMap.Kind.class), buffer.readVarInt(), buffer.readUtf(128), buffer.readUtf(64), buffer.readVarIntArray(8)
               )
            );
         }

         int roadCount = Math.min(MAX_ITEMS, buffer.readVarInt());
         List<int[]> roads = new ArrayList<>();

         for (int i = 0; i < roadCount; i++) {
            roads.add(buffer.readVarIntArray(8192));
         }

         int marchCount = Math.min(MAX_ITEMS, buffer.readVarInt());
         List<int[]> marches = new ArrayList<>();

         for (int i = 0; i < marchCount; i++) {
            marches.add(buffer.readVarIntArray(4));
         }

         return new AWCoreNetwork.WorldMapDataPacket(new WorldMap.Data(originX, originZ, owners, terrain, rivers, factions, markers, roads, marches, buffer.readBoolean()));
      }

      private static void handle(AWCoreNetwork.WorldMapDataPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               WorldMapScreen.open(packet.data);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record DiplomacyDataPacket(String focus, boolean open, List<PlayerDiplomacy.Entry> entries) {
      private static final int MAX_ENTRIES = 64;

      private static void encode(AWCoreNetwork.DiplomacyDataPacket packet, FriendlyByteBuf buffer) {
         buffer.writeUtf(packet.focus, 64);
         buffer.writeBoolean(packet.open);
         buffer.writeVarInt(Math.min(MAX_ENTRIES, packet.entries.size()));

         for (PlayerDiplomacy.Entry entry : packet.entries.subList(0, Math.min(MAX_ENTRIES, packet.entries.size()))) {
            buffer.writeUtf(entry.faction(), 64);
            buffer.writeVarInt(entry.standing());
            buffer.writeVarInt(entry.treaty().ordinal());
            buffer.writeVarInt(Math.min(MAX_ENTRIES, entry.wars().size()));
            entry.wars().stream().limit(MAX_ENTRIES).forEach(war -> buffer.writeUtf(war, 64));
            buffer.writeVarInt(entry.settlements());
            buffer.writeBoolean(entry.leaderNear());
            buffer.writeBoolean(entry.trade());
            buffer.writeVarInt(entry.routes());
            buffer.writeVarInt(entry.ruler());
            buffer.writeVarInt(entry.trait());
            buffer.writeVarIntArray(entry.stores());
            buffer.writeVarIntArray(entry.court());
            buffer.writeVarInt(Math.min(4, entry.goals().size()));
            entry.goals().stream().limit(4).forEach(buffer::writeComponent);
            buffer.writeInt(entry.pending());
            writeNames(buffer, entry.ties());
            buffer.writeVarIntArray(entry.tieData());
            buffer.writeVarInt(Math.min(6, entry.bonds().size()));
            entry.bonds().stream().limit(6).forEach(buffer::writeComponent);
            buffer.writeVarIntArray(entry.person());
            buffer.writeVarIntArray(entry.family());
            buffer.writeVarInt(Math.min(8, entry.familyTips().size()));
            entry.familyTips().stream().limit(8).forEach(buffer::writeComponent);
         }
      }

      private static AWCoreNetwork.DiplomacyDataPacket decode(FriendlyByteBuf buffer) {
         String focus = buffer.readUtf(64);
         boolean open = buffer.readBoolean();
         int count = Math.min(MAX_ENTRIES, buffer.readVarInt());
         List<PlayerDiplomacy.Entry> entries = new ArrayList<>();

         for (int i = 0; i < count; i++) {
            String faction = buffer.readUtf(64);
            int standing = buffer.readVarInt();
            RelationState treaty = RelationState.values()[Math.floorMod(buffer.readVarInt(), RelationState.values().length)];
            int warCount = Math.min(MAX_ENTRIES, buffer.readVarInt());
            List<String> wars = new ArrayList<>();

            for (int j = 0; j < warCount; j++) {
               wars.add(buffer.readUtf(64));
            }

            entries.add(
               new PlayerDiplomacy.Entry(
                  faction, standing, treaty, wars, buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                  buffer.readVarIntArray(8), buffer.readVarIntArray(32), readGoals(buffer), buffer.readInt(), readNames(buffer), buffer.readVarIntArray(64), readLines(buffer, 6), buffer.readVarIntArray(8),
                  buffer.readVarIntArray(32), readLines(buffer, 8)
               )
            );
         }

         return new AWCoreNetwork.DiplomacyDataPacket(focus, open, entries);
      }

      private static List<net.minecraft.network.chat.Component> readGoals(FriendlyByteBuf buffer) {
         return readLines(buffer, 4);
      }

      private static List<net.minecraft.network.chat.Component> readLines(FriendlyByteBuf buffer, int max) {
         int count = Math.min(max, buffer.readVarInt());
         List<net.minecraft.network.chat.Component> goals = new ArrayList<>();
         for (int i = 0; i < count; i++) {
            goals.add(buffer.readComponent());
         }

         return goals;
      }

      private static void handle(AWCoreNetwork.DiplomacyDataPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               DiplomacyScreen.accept(packet.focus, packet.open, packet.entries);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record BatonBarrageRadiusPacket() {
      private static void encode(AWCoreNetwork.BatonBarrageRadiusPacket packet, FriendlyByteBuf buffer) {
      }

      private static AWCoreNetwork.BatonBarrageRadiusPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.BatonBarrageRadiusPacket();
      }

      private static void handle(AWCoreNetwork.BatonBarrageRadiusPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               CommandBatonItem.cycleBarrageRadius(heldBaton(player), player);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record CommandViewShapePacket(List<BlockPos> points, boolean queued) {
      private static final int MAX_POINTS = 96;

      private static void encode(AWCoreNetwork.CommandViewShapePacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.points.size());
         packet.points.forEach(buffer::writeBlockPos);
         buffer.writeBoolean(packet.queued);
      }

      private static AWCoreNetwork.CommandViewShapePacket decode(FriendlyByteBuf buffer) {
         int size = Math.min(MAX_POINTS, buffer.readVarInt());
         List<BlockPos> points = new ArrayList<>(size);
         for (int i = 0; i < size; i++) {
            points.add(buffer.readBlockPos());
         }

         return new AWCoreNetwork.CommandViewShapePacket(points, buffer.readBoolean());
      }

      private static void handle(AWCoreNetwork.CommandViewShapePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               CommandBatonItem.commandViewShape(heldBaton(player), player, packet.points, packet.queued);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record ConquestOfferPacket(FactionConquest.Offer offer) {
      private static void encode(AWCoreNetwork.ConquestOfferPacket packet, FriendlyByteBuf buffer) {
         FactionConquest.Offer offer = packet.offer;
         buffer.writeVarInt(offer.settlement());
         buffer.writeUtf(offer.faction(), 64);
         buffer.writeComponent(offer.name());
         buffer.writeVarInt(offer.population());
         buffer.writeVarInt(offer.tax());
         buffer.writeVarInt(offer.ransom());
         buffer.writeVarInt(offer.loot());
         buffer.writeVarInt(offer.razeLoot());
      }

      private static AWCoreNetwork.ConquestOfferPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ConquestOfferPacket(
            new FactionConquest.Offer(
               buffer.readVarInt(),
               buffer.readUtf(64),
               buffer.readComponent(),
               buffer.readVarInt(),
               buffer.readVarInt(),
               buffer.readVarInt(),
               buffer.readVarInt(),
               buffer.readVarInt()
            )
         );
      }

      private static void handle(AWCoreNetwork.ConquestOfferPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               ConquestScreen.open(packet.offer);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record ConquestChoicePacket(int settlement, FactionConquest.Choice choice) {
      private static void encode(AWCoreNetwork.ConquestChoicePacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.settlement);
         buffer.writeEnum(packet.choice);
      }

      private static AWCoreNetwork.ConquestChoicePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ConquestChoicePacket(buffer.readVarInt(), buffer.readEnum(FactionConquest.Choice.class));
      }

      private static void handle(AWCoreNetwork.ConquestChoicePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               FactionConquest.choose(player, packet.settlement, packet.choice);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record QuestBoardPacket(BlockPos board, Component title, Component note, List<FactionQuests.Line> offers, List<FactionQuests.Line> active) {
      private static final int MAX_LINES = 16;

      private static void writeLines(FriendlyByteBuf buffer, List<FactionQuests.Line> lines) {
         buffer.writeVarInt(Math.min(MAX_LINES, lines.size()));
         for (FactionQuests.Line line : lines.subList(0, Math.min(MAX_LINES, lines.size()))) {
            buffer.writeLong(line.id());
            buffer.writeComponent(line.text());
            buffer.writeComponent(line.detail());
            buffer.writeVarInt(line.state());
         }
      }

      private static List<FactionQuests.Line> readLines(FriendlyByteBuf buffer) {
         int count = Math.min(MAX_LINES, buffer.readVarInt());
         List<FactionQuests.Line> lines = new ArrayList<>();
         for (int i = 0; i < count; i++) {
            lines.add(new FactionQuests.Line(buffer.readLong(), buffer.readComponent(), buffer.readComponent(), buffer.readVarInt()));
         }

         return lines;
      }

      private static void encode(AWCoreNetwork.QuestBoardPacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.board);
         buffer.writeComponent(packet.title);
         buffer.writeComponent(packet.note);
         writeLines(buffer, packet.offers);
         writeLines(buffer, packet.active);
      }

      private static AWCoreNetwork.QuestBoardPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.QuestBoardPacket(buffer.readBlockPos(), buffer.readComponent(), buffer.readComponent(), readLines(buffer), readLines(buffer));
      }

      private static void handle(AWCoreNetwork.QuestBoardPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               net.shadowmage.ancientwarfare.core.client.QuestBoardScreen.open(packet.board, packet.title, packet.note, packet.offers, packet.active);
            }
         });
         context.setPacketHandled(true);
      }
   }

   /** take: the offer's index; turn in and give up: the quest's id. */
   public static record QuestActionPacket(BlockPos board, int action, long argument) {
      public static final int TAKE = 0;
      public static final int TURN_IN = 1;
      public static final int ABANDON = 2;

      private static void encode(AWCoreNetwork.QuestActionPacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.board);
         buffer.writeVarInt(packet.action);
         buffer.writeLong(packet.argument);
      }

      private static AWCoreNetwork.QuestActionPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.QuestActionPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readLong());
      }

      private static void handle(AWCoreNetwork.QuestActionPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               switch (packet.action) {
                  case TAKE -> FactionQuests.take(player, packet.board, (int)packet.argument);
                  case TURN_IN -> FactionQuests.turnIn(player, packet.board, packet.argument);
                  case ABANDON -> FactionQuests.abandon(player, packet.board, packet.argument);
                  default -> {
                  }
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   /** create: the box, the kind of work and for a building its template and facing; cancel: the job's id in argument. */
   public static record ConstructionActionPacket(int action, BlockPos min, BlockPos max, int argument, String template, int facing) {
      public static final int CREATE = 0;
      public static final int CANCEL = 1;

      private static void encode(AWCoreNetwork.ConstructionActionPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.action);
         buffer.writeBlockPos(packet.min);
         buffer.writeBlockPos(packet.max);
         buffer.writeVarInt(packet.argument);
         buffer.writeUtf(packet.template, 256);
         buffer.writeVarInt(packet.facing);
      }

      private static AWCoreNetwork.ConstructionActionPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ConstructionActionPacket(buffer.readVarInt(), buffer.readBlockPos(), buffer.readBlockPos(), buffer.readVarInt(), buffer.readUtf(256), buffer.readVarInt());
      }

      private static void handle(AWCoreNetwork.ConstructionActionPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               if (packet.action == CREATE) {
                  net.shadowmage.ancientwarfare.core.construction.ConstructionJobs.create(
                     player,
                     packet.min,
                     packet.max,
                     net.shadowmage.ancientwarfare.core.construction.ConstructionJobs.Kind.values()[
                        Math.floorMod(packet.argument, net.shadowmage.ancientwarfare.core.construction.ConstructionJobs.Kind.values().length)
                     ],
                     packet.template.isEmpty() ? null : packet.template,
                     net.minecraft.core.Direction.from2DDataValue(packet.facing)
                  );
               } else if (packet.action == CANCEL) {
                  net.shadowmage.ancientwarfare.core.construction.ConstructionJobs.cancel(player, packet.argument);
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record ResearchKnownPacket(List<String> known) {
      private static void encode(AWCoreNetwork.ResearchKnownPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.known.size());
         packet.known.forEach(research -> buffer.writeUtf(research, 64));
      }

      private static AWCoreNetwork.ResearchKnownPacket decode(FriendlyByteBuf buffer) {
         int count = Math.min(512, buffer.readVarInt());
         List<String> known = new ArrayList<>();
         for (int index = 0; index < count; index++) {
            known.add(buffer.readUtf(64));
         }

         return new AWCoreNetwork.ResearchKnownPacket(known);
      }

      private static void handle(AWCoreNetwork.ResearchKnownPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               net.shadowmage.ancientwarfare.core.client.ClientResearch.set(java.util.Set.copyOf(packet.known));
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record CountryOpenPacket() {
      private static void encode(AWCoreNetwork.CountryOpenPacket packet, FriendlyByteBuf buffer) {
      }

      private static AWCoreNetwork.CountryOpenPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.CountryOpenPacket();
      }

      private static void handle(AWCoreNetwork.CountryOpenPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               net.shadowmage.ancientwarfare.core.faction.PlayerCountry.open(player);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record CountryDataPacket(net.shadowmage.ancientwarfare.core.faction.PlayerCountry.CountryView view) {
      private static final int MAX_ROWS = 64;

      private static void encode(AWCoreNetwork.CountryDataPacket packet, FriendlyByteBuf buffer) {
         var view = packet.view;
         buffer.writeUtf(view.name(), 64);
         buffer.writeVarLong(view.days());
         buffer.writeBlockPos(view.capital());
         buffer.writeInt(view.treasury());
         buffer.writeVarInt(view.income());
         buffer.writeVarInt(view.taxLevel());
         buffer.writeVarInt(view.provinces());
         var villages = view.villages().subList(0, Math.min(MAX_ROWS, view.villages().size()));
         buffer.writeVarInt(villages.size());
         for (var village : villages) {
            buffer.writeBlockPos(village.pos());
            buffer.writeBoolean(village.capital());
            buffer.writeVarInt(village.people());
            buffer.writeVarInt(village.loyalty());
            buffer.writeVarInt(village.order());
         }

         var neighbours = view.neighbours().subList(0, Math.min(MAX_ROWS, view.neighbours().size()));
         buffer.writeVarInt(neighbours.size());
         for (var neighbour : neighbours) {
            buffer.writeUtf(neighbour.faction(), 64);
            buffer.writeVarInt(neighbour.borders());
            buffer.writeVarInt(neighbour.treaty());
            buffer.writeInt(neighbour.standing());
            buffer.writeBoolean(neighbour.trade());
         }
      }

      private static AWCoreNetwork.CountryDataPacket decode(FriendlyByteBuf buffer) {
         String name = buffer.readUtf(64);
         long days = buffer.readVarLong();
         BlockPos capital = buffer.readBlockPos();
         int treasury = buffer.readInt();
         int income = buffer.readVarInt();
         int tax = buffer.readVarInt();
         int provinces = buffer.readVarInt();
         List<net.shadowmage.ancientwarfare.core.faction.PlayerCountry.CountryView.Village> villages = new ArrayList<>();
         int count = Math.min(MAX_ROWS, buffer.readVarInt());
         for (int index = 0; index < count; index++) {
            villages.add(
               new net.shadowmage.ancientwarfare.core.faction.PlayerCountry.CountryView.Village(
                  buffer.readBlockPos(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()
               )
            );
         }

         List<net.shadowmage.ancientwarfare.core.faction.PlayerCountry.CountryView.Neighbour> neighbours = new ArrayList<>();
         count = Math.min(MAX_ROWS, buffer.readVarInt());
         for (int index = 0; index < count; index++) {
            neighbours.add(
               new net.shadowmage.ancientwarfare.core.faction.PlayerCountry.CountryView.Neighbour(
                  buffer.readUtf(64), buffer.readVarInt(), buffer.readVarInt(), buffer.readInt(), buffer.readBoolean()
               )
            );
         }

         return new AWCoreNetwork.CountryDataPacket(
            new net.shadowmage.ancientwarfare.core.faction.PlayerCountry.CountryView(name, days, capital, treasury, income, tax, provinces, villages, neighbours)
         );
      }

      private static void handle(AWCoreNetwork.CountryDataPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               net.shadowmage.ancientwarfare.core.client.CountryScreen.show(packet.view);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record ConstructionJobsPacket(List<ConstructionJobsPacket.Entry> jobs) {
      private static final int MAX_JOBS = 16;

      public static record Entry(int id, int kind, BlockPos min, BlockPos max, int done, int total, Component block) {
      }

      private static void encode(AWCoreNetwork.ConstructionJobsPacket packet, FriendlyByteBuf buffer) {
         List<Entry> jobs = packet.jobs.subList(0, Math.min(MAX_JOBS, packet.jobs.size()));
         buffer.writeVarInt(jobs.size());
         for (Entry job : jobs) {
            buffer.writeVarInt(job.id);
            buffer.writeVarInt(job.kind);
            buffer.writeBlockPos(job.min);
            buffer.writeBlockPos(job.max);
            buffer.writeVarInt(job.done);
            buffer.writeVarInt(job.total);
            buffer.writeComponent(job.block);
         }
      }

      private static AWCoreNetwork.ConstructionJobsPacket decode(FriendlyByteBuf buffer) {
         int count = Math.min(MAX_JOBS, buffer.readVarInt());
         List<Entry> jobs = new ArrayList<>();
         for (int index = 0; index < count; index++) {
            jobs.add(new Entry(buffer.readVarInt(), buffer.readVarInt(), buffer.readBlockPos(), buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(), buffer.readComponent()));
         }

         return new AWCoreNetwork.ConstructionJobsPacket(jobs);
      }

      private static void handle(AWCoreNetwork.ConstructionJobsPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               List<net.shadowmage.ancientwarfare.core.client.ConstructionClient.JobView> views = new ArrayList<>();
               for (Entry job : packet.jobs) {
                  views.add(new net.shadowmage.ancientwarfare.core.client.ConstructionClient.JobView(job.id, job.kind, job.min, job.max, job.done, job.total, job.block));
               }

               net.shadowmage.ancientwarfare.core.client.ConstructionClient.setJobs(views);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record FoundCountryPacket(BlockPos hall, String name) {
      private static void encode(AWCoreNetwork.FoundCountryPacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.hall);
         buffer.writeUtf(packet.name, PlayerCountry.MAX_NAME * 4);
      }

      private static AWCoreNetwork.FoundCountryPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.FoundCountryPacket(buffer.readBlockPos(), buffer.readUtf(PlayerCountry.MAX_NAME * 4));
      }

      private static void handle(AWCoreNetwork.FoundCountryPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               PlayerCountry.found(player, packet.hall, packet.name);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record CommandViewFacePacket(BlockPos point) {
      private static void encode(AWCoreNetwork.CommandViewFacePacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.point);
      }

      private static AWCoreNetwork.CommandViewFacePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.CommandViewFacePacket(buffer.readBlockPos());
      }

      private static void handle(AWCoreNetwork.CommandViewFacePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               CommandBatonItem.commandViewFace(heldBaton(player), player, packet.point);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record BatonStancePacket() {
      private static void encode(AWCoreNetwork.BatonStancePacket packet, FriendlyByteBuf buffer) {
      }

      private static AWCoreNetwork.BatonStancePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.BatonStancePacket();
      }

      private static void handle(AWCoreNetwork.BatonStancePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               CommandBatonItem.cycleStance(heldBaton(player), player);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record BatonFormationPacket() {
      private static void encode(AWCoreNetwork.BatonFormationPacket packet, FriendlyByteBuf buffer) {
      }

      private static AWCoreNetwork.BatonFormationPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.BatonFormationPacket();
      }

      private static void handle(AWCoreNetwork.BatonFormationPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               ItemStack stack = player.getMainHandItem();
               if (!(stack.getItem() instanceof CommandBatonItem)) {
                  stack = player.getOffhandItem();
               }

               CommandBatonItem.cycleFormation(stack, player);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record BatonSquadPacket(int slot, boolean assign) {
      private static void encode(AWCoreNetwork.BatonSquadPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.slot);
         buffer.writeBoolean(packet.assign);
      }

      private static AWCoreNetwork.BatonSquadPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.BatonSquadPacket(buffer.readVarInt(), buffer.readBoolean());
      }

      private static void handle(AWCoreNetwork.BatonSquadPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               ItemStack stack = player.getMainHandItem();
               if (!(stack.getItem() instanceof CommandBatonItem)) {
                  stack = player.getOffhandItem();
               }

               CommandBatonItem.squadHotkey(stack, player, packet.slot, packet.assign);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record DraftingPreviewPacket(int containerId, int templateIndex, int requestId, int chunkIndex, int chunkCount, DraftingPreviewData data) {
      private static void encode(AWCoreNetwork.DraftingPreviewPacket packet, FriendlyByteBuf buffer) {
         DraftingPreviewData.requireChunkMetadata(packet.chunkIndex, packet.chunkCount);
         buffer.writeVarInt(packet.containerId);
         buffer.writeVarInt(packet.templateIndex);
         buffer.writeVarInt(packet.requestId);
         buffer.writeVarInt(packet.chunkIndex);
         buffer.writeVarInt(packet.chunkCount);
         DraftingPreviewData.writeChunk(buffer, packet.data);
      }

      private static AWCoreNetwork.DraftingPreviewPacket decode(FriendlyByteBuf buffer) {
         if (buffer.readableBytes() >= 30000) {
            throw new IllegalArgumentException("Preview packet too large");
         } else {
            int containerId = buffer.readVarInt();
            int templateIndex = buffer.readVarInt();
            int requestId = buffer.readVarInt();
            int chunkIndex = buffer.readVarInt();
            int chunkCount = buffer.readVarInt();
            DraftingPreviewData.requireChunkMetadata(chunkIndex, chunkCount);
            return new AWCoreNetwork.DraftingPreviewPacket(containerId, templateIndex, requestId, chunkIndex, chunkCount, DraftingPreviewData.readChunk(buffer));
         }
      }

      private static void handle(AWCoreNetwork.DraftingPreviewPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               LocalPlayer player = Minecraft.getInstance().player;
               if (player != null && player.containerMenu instanceof DraftingStationMenu menu) {
                  menu.acceptPreview(packet.containerId, packet.templateIndex, packet.requestId, packet.chunkIndex, packet.chunkCount, packet.data);
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record FireBallistaPacket() {
      private static void encode(AWCoreNetwork.FireBallistaPacket packet, FriendlyByteBuf buffer) {
      }

      private static AWCoreNetwork.FireBallistaPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.FireBallistaPacket();
      }

      private static void handle(AWCoreNetwork.FireBallistaPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() != null && context.getSender().getVehicle() instanceof BallistaMobileEntity ballista) {
               ballista.tryFire();
            } else if (context.getSender() != null && context.getSender().getVehicle() instanceof CatapultMobileEntity catapult) {
               catapult.tryFire();
            } else if (context.getSender() != null && context.getSender().getVehicle() instanceof CannonTurretStandEntity cannon) {
               cannon.tryFire();
            } else if (context.getSender() != null && context.getSender().getVehicle() instanceof HwachaEntity hwacha) {
               hwacha.tryFire();
            } else if (context.getSender() != null && context.getSender().getVehicle() instanceof BatteringRamEntity ram) {
               ram.tryAttack();
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record MailboxConfigPacket(BlockPos pos, int action, String value, boolean flag) {
      private static void encode(AWCoreNetwork.MailboxConfigPacket p, FriendlyByteBuf b) {
         b.writeBlockPos(p.pos);
         b.writeVarInt(p.action);
         b.writeUtf(p.value, 128);
         b.writeBoolean(p.flag);
      }

      private static AWCoreNetwork.MailboxConfigPacket decode(FriendlyByteBuf b) {
         return new AWCoreNetwork.MailboxConfigPacket(b.readBlockPos(), b.readVarInt(), b.readUtf(128), b.readBoolean());
      }

      private static void handle(AWCoreNetwork.MailboxConfigPacket p, Supplier<Context> sup) {
         Context c = sup.get();
         c.enqueueWork(() -> {
            if (c.getSender() != null && c.getSender().level().getBlockEntity(p.pos) instanceof MailboxBlockEntity box && box.stillValid(c.getSender())) {
               String owner = c.getSender().getGameProfile().getName();
               MailboxData data = MailboxData.get((ServerLevel)c.getSender().level());
               switch (p.action) {
                  case 0:
                     box.setMailboxName(p.value);
                     break;
                  case 1:
                     box.setTargetName(p.value);
                     break;
                  case 2:
                     box.setPrivateBox(p.flag);
                     break;
                  case 3:
                     box.setAutoExport(p.flag);
                     break;
                  case 4:
                     data.addName(owner, p.flag, p.value);
                     break;
                  case 5:
                     data.removeName(owner, p.flag, p.value);
               }

               data.register(box);
               box.setChanged();
               box.setChanged();
            }
         });
         c.setPacketHandled(true);
      }
   }

   public static record MailboxListPacket(BlockPos pos, List<String> entries) {
      private static void encode(AWCoreNetwork.MailboxListPacket p, FriendlyByteBuf b) {
         b.writeBlockPos(p.pos);
         b.writeVarInt(Math.min(256, p.entries.size()));

         for (String s : p.entries.stream().limit(256L).toList()) {
            b.writeUtf(s, 256);
         }
      }

      private static AWCoreNetwork.MailboxListPacket decode(FriendlyByteBuf b) {
         BlockPos pos = b.readBlockPos();
         int n = b.readVarInt();
         List<String> e = new ArrayList<>();

         for (int i = 0; i < n; i++) {
            e.add(b.readUtf(256));
         }

         return new AWCoreNetwork.MailboxListPacket(pos, e);
      }

      private static void handle(AWCoreNetwork.MailboxListPacket p, Supplier<Context> sup) {
         Context c = sup.get();
         c.enqueueWork(() -> {
            if (c.getSender() != null && p.entries.isEmpty() && c.getSender().level() instanceof ServerLevel sl) {
               List<String> list = MailboxData.get(sl).knownMailboxes().stream().map(i -> i.name() + " @ " + i.pos().toShortString()).toList();
               AWCoreNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(c::getSender), new AWCoreNetwork.MailboxListPacket(p.pos, list));
            } else if (c.getSender() == null) {
               MailboxTargetScreen.receive(p.entries);
            }
         });
         c.setPacketHandled(true);
      }
   }

   public static record ManageStructureTemplatePacket(BlockPos pos, String name, boolean delete) {
      private static void encode(AWCoreNetwork.ManageStructureTemplatePacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.pos);
         buffer.writeUtf(packet.name, 48);
         buffer.writeBoolean(packet.delete);
      }

      private static AWCoreNetwork.ManageStructureTemplatePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ManageStructureTemplatePacket(buffer.readBlockPos(), buffer.readUtf(48), buffer.readBoolean());
      }

      private static void handle(AWCoreNetwork.ManageStructureTemplatePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               ServerPlayer player = context.getSender();
               if (player != null && player.containerMenu instanceof StructureScannerMenu menu && menu.pos().equals(packet.pos)) {
                  ItemStack scanner = player.getMainHandItem().is((Item)AWCoreItems.STRUCTURE_SCANNER.get()) ? player.getMainHandItem() : player.getOffhandItem();
                  if (scanner.is((Item)AWCoreItems.STRUCTURE_SCANNER.get())) {
                     if (packet.delete) {
                        boolean removed = PlayerStructureTemplateData.get(player.serverLevel()).remove(packet.name);
                        player.displayClientMessage(
                           Component.translatable(
                              removed ? "message.ancientwarfare.structure_scan.deleted" : "message.ancientwarfare.structure_scan.not_found",
                              new Object[]{packet.name}
                           ),
                           false
                        );
                     } else {
                        scanner.getOrCreateTag().remove("FirstPos");
                        scanner.getTag().remove("SecondPos");
                        scanner.getTag().remove("BuildOrigin");
                        player.displayClientMessage(Component.translatable("message.ancientwarfare.structure_scan.reset"), false);
                     }
                  }
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record RequestDraftingPreviewPacket(int containerId, int templateIndex, int requestId) {
      private static void encode(AWCoreNetwork.RequestDraftingPreviewPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.containerId);
         buffer.writeVarInt(packet.templateIndex);
         buffer.writeVarInt(packet.requestId);
      }

      private static AWCoreNetwork.RequestDraftingPreviewPacket decode(FriendlyByteBuf buffer) {
         if (buffer.readableBytes() > 15) {
            throw new IllegalArgumentException("Preview request too large");
         } else {
            AWCoreNetwork.RequestDraftingPreviewPacket packet = new AWCoreNetwork.RequestDraftingPreviewPacket(
               buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()
            );
            if (!buffer.isReadable() && packet.containerId >= 0 && packet.templateIndex >= 0 && packet.requestId >= 1) {
               return packet;
            } else {
               throw new IllegalArgumentException("Invalid preview request");
            }
         }
      }

      private static void handle(AWCoreNetwork.RequestDraftingPreviewPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               ServerPlayer player = context.getSender();
               if (player != null && player.containerMenu instanceof DraftingStationMenu menu) {
                  menu.requestServerPreview(
                     player,
                     packet.containerId,
                     packet.templateIndex,
                     packet.requestId,
                     preview -> {
                        int chunks = preview.chunkCount();

                        for (int index = 0; index < chunks; index++) {
                           AWCoreNetwork.CHANNEL
                              .send(
                                 PacketDistributor.PLAYER.with(() -> player),
                                 new AWCoreNetwork.DraftingPreviewPacket(
                                    packet.containerId, packet.templateIndex, packet.requestId, index, chunks, preview.chunk(index)
                                 )
                              );
                        }
                     }
                  );
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SaveStructureScanPacket(BlockPos pos, String name) {
      private static void encode(AWCoreNetwork.SaveStructureScanPacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.pos);
         buffer.writeUtf(packet.name, 48);
      }

      private static AWCoreNetwork.SaveStructureScanPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SaveStructureScanPacket(buffer.readBlockPos(), buffer.readUtf(48));
      }

      private static void handle(AWCoreNetwork.SaveStructureScanPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               ServerPlayer player = context.getSender();
               if (player != null && player.containerMenu instanceof StructureScannerMenu menu && menu.pos().equals(packet.pos)) {
                  ItemStack scanner = player.getMainHandItem().is((Item)AWCoreItems.STRUCTURE_SCANNER.get()) ? player.getMainHandItem() : player.getOffhandItem();
                  if (scanner.is((Item)AWCoreItems.STRUCTURE_SCANNER.get())
                     && scanner.hasTag()
                     && scanner.getTag().contains("FirstPos")
                     && scanner.getTag().contains("SecondPos")) {
                     BlockPos first = BlockPos.of(scanner.getTag().getLong("FirstPos"));
                     BlockPos second = BlockPos.of(scanner.getTag().getLong("SecondPos"));
                     BlockPos origin = scanner.getTag().contains("BuildOrigin")
                        ? BlockPos.of(scanner.getTag().getLong("BuildOrigin"))
                        : packet.pos.above();
                     PlayerStructureTemplateData.CaptureResult result = PlayerStructureTemplateData.get(player.serverLevel())
                        .capture(player.serverLevel(), packet.name, first, second, origin);
                     if (result.success()) {
                        scanner.getTag().remove("FirstPos");
                        scanner.getTag().remove("SecondPos");
                        scanner.getTag().remove("BuildOrigin");
                     }

                     player.displayClientMessage(Component.translatable(result.messageKey(), new Object[]{result.blockCount()}), false);
                  } else {
                     player.displayClientMessage(Component.translatable("message.ancientwarfare.structure_scan.incomplete"), false);
                  }
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SelectDraftingTemplatePacket(BlockPos pos, String name, boolean start) {
      private static void encode(AWCoreNetwork.SelectDraftingTemplatePacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.pos);
         buffer.writeUtf(packet.name, 48);
         buffer.writeBoolean(packet.start);
      }

      private static AWCoreNetwork.SelectDraftingTemplatePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SelectDraftingTemplatePacket(buffer.readBlockPos(), buffer.readUtf(48), buffer.readBoolean());
      }

      private static void handle(AWCoreNetwork.SelectDraftingTemplatePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.level().getBlockEntity(packet.pos) instanceof DraftingStationBlockEntity station) {
               station.selectTemplate(packet.name);
               if (packet.start) {
                  station.start();
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record SetAdvancedSpawnerConfigPacket(BlockPos pos, AdvancedSpawnerConfig config) {
      private static void encode(AWCoreNetwork.SetAdvancedSpawnerConfigPacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.pos);
         buffer.writeUtf(packet.config.entityId(), 128);
         buffer.writeUtf(packet.config.factionName(), 64);
         buffer.writeUtf(packet.config.entityNbt(), 2048);
         buffer.writeVarInt(packet.config.minToSpawn());
         buffer.writeVarInt(packet.config.maxToSpawn());
         buffer.writeVarInt(packet.config.spawnDelay());
         buffer.writeVarInt(packet.config.playerRange());
         buffer.writeVarInt(packet.config.mobRange());
         buffer.writeVarInt(packet.config.spawnRange());
         buffer.writeVarInt(packet.config.maxNearby());
         buffer.writeInt(packet.config.spawnYOffset());
         buffer.writeBoolean(packet.config.lightSensitive());
         buffer.writeBoolean(packet.config.redstoneSensitive());
         buffer.writeBoolean(packet.config.redstoneMode());
         buffer.writeBoolean(packet.config.transparent());
         buffer.writeBoolean(packet.config.debugMode());
      }

      private static AWCoreNetwork.SetAdvancedSpawnerConfigPacket decode(FriendlyByteBuf buffer) {
         BlockPos pos = buffer.readBlockPos();
         AdvancedSpawnerConfig config = new AdvancedSpawnerConfig(
            buffer.readUtf(128),
            buffer.readUtf(64),
            buffer.readUtf(2048),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readInt(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean()
         );
         return new AWCoreNetwork.SetAdvancedSpawnerConfigPacket(pos, config);
      }

      private static void handle(AWCoreNetwork.SetAdvancedSpawnerConfigPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() != null && context.getSender().level().getBlockEntity(packet.pos) instanceof AdvancedSpawnerBlockEntity spawner) {
               spawner.applyConfig(packet.config);
               context.getSender().displayClientMessage(spawner.statusMessage(), true);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record SetBardTracksPacket(int entityId, List<BardTrack> tracks) {
      private static void encode(AWCoreNetwork.SetBardTracksPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.entityId);
         buffer.writeVarInt(Math.min(32, packet.tracks.size()));

         for (BardTrack track : packet.tracks.stream().limit(32L).toList()) {
            track.toNetwork(buffer);
         }
      }

      private static AWCoreNetwork.SetBardTracksPacket decode(FriendlyByteBuf buffer) {
         int entityId = buffer.readVarInt();
         int count = Math.min(32, buffer.readVarInt());
         List<BardTrack> tracks = new ArrayList<>(count);

         for (int i = 0; i < count; i++) {
            tracks.add(BardTrack.fromNetwork(buffer));
         }

         return new AWCoreNetwork.SetBardTracksPacket(entityId, tracks);
      }

      private static void handle(AWCoreNetwork.SetBardTracksPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() != null) {
               if (context.getSender().level().getEntity(packet.entityId) instanceof BardNpcEntity bard && bard.hasCommandPermission(context.getSender())) {
                  bard.setBardTracks(packet.tracks);
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record SetNpcSkinPacket(int entityId, String skinName, boolean alexModel) {
      private static void encode(AWCoreNetwork.SetNpcSkinPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.entityId);
         buffer.writeUtf(packet.skinName, 128);
         buffer.writeBoolean(packet.alexModel);
      }

      private static AWCoreNetwork.SetNpcSkinPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetNpcSkinPacket(buffer.readVarInt(), buffer.readUtf(128), buffer.readBoolean());
      }

      private static void handle(AWCoreNetwork.SetNpcSkinPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null) {
                  if (context.getSender().level().getEntity(packet.entityId) instanceof PlayerOwnedNpcEntity npc
                     && npc.hasCommandPermission(context.getSender())) {
                     npc.setSkinName(packet.skinName);
                     npc.setAlexModel(packet.alexModel);
                  }
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SetWarehouseInterfaceFilterPacket(BlockPos pos, int action, int index, ItemStack stack, int quantity) {
      private static void encode(AWCoreNetwork.SetWarehouseInterfaceFilterPacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.pos);
         buffer.writeVarInt(packet.action);
         buffer.writeVarInt(packet.index);
         buffer.writeItem(packet.stack);
         buffer.writeVarInt(packet.quantity);
      }

      private static AWCoreNetwork.SetWarehouseInterfaceFilterPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseInterfaceFilterPacket(
            buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(), buffer.readItem(), buffer.readVarInt()
         );
      }

      private static void handle(AWCoreNetwork.SetWarehouseInterfaceFilterPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().isSpectator()
                  && context.getSender().containerMenu instanceof WarehouseInterfaceMenu menu
                  && menu.getPos().equals(packet.pos)
                  && menu.stillValid(context.getSender())
                  && context.getSender().level().hasChunkAt(packet.pos)
                  && context.getSender().level().getBlockEntity(packet.pos) instanceof WarehouseInterfaceBlockEntity warehouseInterface
                  && packet.action >= 0
                  && packet.action <= 2
                  && (packet.action == 1 || packet.index >= 0 && packet.index < warehouseInterface.getFilterCount())
                  && packet.quantity >= 0) {
                  if (packet.action == 1) {
                     warehouseInterface.addFilter();
                  } else if (packet.action == 2) {
                     warehouseInterface.removeFilter(packet.index);
                  } else {
                     warehouseInterface.setFilter(packet.index, packet.stack.isEmpty() ? ItemStack.EMPTY : packet.stack.copyWithCount(1), packet.quantity);
                  }
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SetWarehouseScrollPacket(int row) {
      private static void encode(AWCoreNetwork.SetWarehouseScrollPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(Math.max(0, packet.row));
      }

      private static AWCoreNetwork.SetWarehouseScrollPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseScrollPacket(Math.max(0, buffer.readVarInt()));
      }

      private static void handle(AWCoreNetwork.SetWarehouseScrollPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().isSpectator()
                  && context.getSender().containerMenu instanceof WarehouseControlMenu menu
                  && menu.stillValid(context.getSender())) {
                  menu.scrollToRow(packet.row);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SetWarehouseSearchPacket(String search) {
      private static void encode(AWCoreNetwork.SetWarehouseSearchPacket packet, FriendlyByteBuf buffer) {
         buffer.writeUtf(packet.search, 64);
      }

      private static AWCoreNetwork.SetWarehouseSearchPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseSearchPacket(buffer.readUtf(64));
      }

      private static void handle(AWCoreNetwork.SetWarehouseSearchPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().isSpectator()
                  && context.getSender().containerMenu instanceof WarehouseControlMenu menu
                  && menu.stillValid(context.getSender())) {
                  menu.setSearch(packet.search);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SetWarehouseStockFilterPacket(BlockPos pos, int index, ItemStack stack) {
      private static void encode(AWCoreNetwork.SetWarehouseStockFilterPacket packet, FriendlyByteBuf buffer) {
         buffer.writeBlockPos(packet.pos);
         buffer.writeVarInt(packet.index);
         buffer.writeItem(packet.stack);
      }

      private static AWCoreNetwork.SetWarehouseStockFilterPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseStockFilterPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readItem());
      }

      private static void handle(AWCoreNetwork.SetWarehouseStockFilterPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().isSpectator()
                  && context.getSender().containerMenu instanceof WarehouseStockViewerMenu menu
                  && menu.getPos().equals(packet.pos)
                  && menu.stillValid(context.getSender())
                  && context.getSender().level().hasChunkAt(packet.pos)
                  && packet.index >= 0
                  && packet.index < 4
                  && context.getSender().level().getBlockEntity(packet.pos) instanceof WarehouseStockViewerBlockEntity viewer) {
                  viewer.setFilterItem(packet.index, packet.stack.isEmpty() ? ItemStack.EMPTY : packet.stack.copyWithCount(1));
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SetWarehouseStorageScrollPacket(int containerId, int pixels) {
      public static void encode(AWCoreNetwork.SetWarehouseStorageScrollPacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.containerId);
         buffer.writeVarInt(Math.max(0, packet.pixels));
      }

      public static AWCoreNetwork.SetWarehouseStorageScrollPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseStorageScrollPacket(buffer.readVarInt(), Math.max(0, buffer.readVarInt()));
      }

      private static void handle(AWCoreNetwork.SetWarehouseStorageScrollPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().isSpectator()
                  && context.getSender().containerMenu instanceof WarehouseStorageMenu menu
                  && menu.containerId == packet.containerId
                  && menu.stillValid(context.getSender())) {
                  menu.scrollToPixel(packet.pixels);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record TraderRoutePacket(int traderEntityId, BlockPos target) {
      private static void encode(AWCoreNetwork.TraderRoutePacket packet, FriendlyByteBuf buffer) {
         buffer.writeVarInt(packet.traderEntityId);
         buffer.writeBlockPos(packet.target);
      }

      private static AWCoreNetwork.TraderRoutePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.TraderRoutePacket(buffer.readVarInt(), buffer.readBlockPos());
      }

      private static void handle(AWCoreNetwork.TraderRoutePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               ServerPlayer player = context.getSender();
               if (player != null
                  && player.containerMenu instanceof TradeOrderMenu menu
                  && menu.isTraderBound()
                  && menu.getTraderEntityId() == packet.traderEntityId) {
                  if (player.level().getEntity(packet.traderEntityId) instanceof TraderNpcEntity trader
                     && trader.isAlive()
                     && trader.hasCommandPermission(player)
                     && AWCoreRegistryAccess.is(trader.getOrdersStack(), AWCoreIds.TRADE_ORDER)) {
                     BlockPos target = packet.target;
                     ServerLevel level = player.serverLevel();
                     if (target.getY() >= level.getMinBuildHeight() && target.getY() < level.getMaxBuildHeight() && level.getWorldBorder().isWithinBounds(target)) {
                        ItemStack order = trader.getOrdersStack().copy();
                        TradeOrderData.setRouteTarget(order, target);
                        trader.setOrdersStack(order);
                        trader.clearCommand();
                        trader.stopRiding();
                        trader.getNavigation().stop();
                        trader.setDeltaMovement(Vec3.ZERO);
                        level.getChunk(target);
                        trader.teleportTo((double)target.getX() + 0.5, (double)target.getY(), (double)target.getZ() + 0.5);
                        player.displayClientMessage(
                           Component.translatable("guistrings.npc.trade_order.departed", new Object[]{target.getX(), target.getY(), target.getZ()}),
                           true
                        );
                        return;
                     }

                     player.displayClientMessage(Component.translatable("guistrings.npc.trade_order.destination_invalid"), true);
                     return;
                  }
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record TransferEngineeringRecipePacket(ResourceLocation recipeId) {
      private static void encode(AWCoreNetwork.TransferEngineeringRecipePacket packet, FriendlyByteBuf buffer) {
         buffer.writeResourceLocation(packet.recipeId);
      }

      private static AWCoreNetwork.TransferEngineeringRecipePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.TransferEngineeringRecipePacket(buffer.readResourceLocation());
      }

      private static void handle(AWCoreNetwork.TransferEngineeringRecipePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               boolean transferred = false;
               if (player.containerMenu instanceof EngineeringStationMenu menu) {
                  transferred = menu.transferEngineeringRecipe(player, packet.recipeId);
               } else if (player.containerMenu instanceof AutoCraftingMenu menu) {
                  transferred = menu.transferEngineeringRecipe(player, packet.recipeId);
               } else if (player.containerMenu instanceof WarehouseCraftingStationMenu menu) {
                  transferred = menu.transferEngineeringRecipe(player, packet.recipeId);
               }

               if (!transferred) {
                  player.displayClientMessage(Component.translatable("jei.ancientwarfare.transfer_failed"), true);
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record VehicleInputPacket(float forward, float turn) {
      private static void encode(AWCoreNetwork.VehicleInputPacket packet, FriendlyByteBuf buffer) {
         buffer.writeFloat(packet.forward);
         buffer.writeFloat(packet.turn);
      }

      private static AWCoreNetwork.VehicleInputPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.VehicleInputPacket(buffer.readFloat(), buffer.readFloat());
      }

      private static void handle(AWCoreNetwork.VehicleInputPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() != null && context.getSender().getVehicle() instanceof CannonMobileEntity cannonMobile) {
               cannonMobile.setVehicleInput(packet.forward, packet.turn);
            } else if (context.getSender() != null && context.getSender().getVehicle() instanceof CatapultTurretStandEntity catapult) {
               catapult.applyStandInput(packet.forward, packet.turn);
            } else if (context.getSender() != null && context.getSender().getVehicle() instanceof CannonTurretStandEntity cannon) {
               cannon.applyStandInput(packet.forward, packet.turn);
            } else if (context.getSender() != null && context.getSender().getVehicle() instanceof ChestPushcartEntity cart) {
               cart.setVehicleInput(packet.forward, packet.turn);
            }
         });
         context.setPacketHandled(true);
      }
   }
}
