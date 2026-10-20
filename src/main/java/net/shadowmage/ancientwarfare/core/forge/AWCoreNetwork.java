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
   private static final String PROTOCOL_VERSION = "5";
   private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation("ancientwarfare", "main"), () -> "5", "5"::equals, "5"::equals
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
      CHANNEL.sendToServer(new AWCoreNetwork.SetWarehouseInterfaceFilterPacket(pos, 1, 0, ItemStack.f_41583_, 0));
   }

   public static void sendRemoveWarehouseInterfaceFilter(BlockPos pos, int index) {
      CHANNEL.sendToServer(new AWCoreNetwork.SetWarehouseInterfaceFilterPacket(pos, 2, index, ItemStack.f_41583_, 0));
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
         buffer.m_130064_(packet.pos);
         buffer.m_130068_(packet.side);
      }

      private static AWCoreNetwork.AddWorkOrderPointPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.AddWorkOrderPointPacket(buffer.m_130135_(), (Direction)buffer.m_130066_(Direction.class));
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
               if (context.getSender().m_20202_() instanceof CannonMobileEntity cannonMobile) {
                  cannonMobile.applyStandInput(packet.pitchDelta, 0.0F);
               } else if (context.getSender().m_20202_() instanceof BallistaMobileEntity ballista) {
                  ballista.adjustTurret(packet.pitchDelta, packet.yawDelta);
               } else if (context.getSender().m_20202_() instanceof CatapultMobileEntity catapult) {
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
         buffer.m_130130_(packet.containerId);
         buffer.m_130130_(packet.action);
         buffer.m_130130_(packet.index);
         buffer.m_130130_(packet.expectedCount);
         buffer.m_130055_(packet.expectedFilter.m_41619_() ? ItemStack.f_41583_ : packet.expectedFilter.m_255036_(1));
         buffer.m_130055_(packet.expectedCarried);
      }

      public static AWCoreNetwork.ChangeWarehouseStorageFilterPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ChangeWarehouseStorageFilterPacket(
            buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130267_(), buffer.m_130267_()
         );
      }

      private static void handle(AWCoreNetwork.ChangeWarehouseStorageFilterPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().m_5833_()
                  && context.getSender().f_36096_ instanceof WarehouseStorageMenu menu
                  && menu.f_38840_ == packet.containerId
                  && menu.m_6875_(context.getSender())) {
                  menu.handleFilterAction(context.getSender(), packet.action, packet.index, packet.expectedCount, packet.expectedFilter, packet.expectedCarried);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record ClickWarehouseEntryPacket(int containerId, int slot, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
      public static void encode(AWCoreNetwork.ClickWarehouseEntryPacket packet, FriendlyByteBuf buffer) {
         buffer.m_130130_(packet.containerId);
         buffer.m_130130_(packet.slot);
         buffer.m_130130_(packet.button);
         buffer.m_130068_(packet.clickType);
         buffer.m_130055_(packet.expectedItem.m_41619_() ? ItemStack.f_41583_ : packet.expectedItem.m_255036_(1));
         buffer.m_130055_(packet.expectedCarried);
      }

      public static AWCoreNetwork.ClickWarehouseEntryPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ClickWarehouseEntryPacket(
            buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), (ClickType)buffer.m_130066_(ClickType.class), buffer.m_130267_(), buffer.m_130267_()
         );
      }

      private static void handle(AWCoreNetwork.ClickWarehouseEntryPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().m_5833_()
                  && context.getSender().f_36096_ instanceof WarehouseControlMenu menu
                  && menu.f_38840_ == packet.containerId
                  && menu.m_6875_(context.getSender())) {
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
         buffer.m_130130_(packet.containerId);
         buffer.m_130130_(packet.entry);
         buffer.m_130130_(packet.button);
         buffer.m_130068_(packet.clickType);
         buffer.m_130055_(packet.expectedItem.m_41619_() ? ItemStack.f_41583_ : packet.expectedItem.m_255036_(1));
         buffer.m_130055_(packet.expectedCarried);
      }

      public static AWCoreNetwork.ClickWarehouseStorageEntryPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ClickWarehouseStorageEntryPacket(
            buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), (ClickType)buffer.m_130066_(ClickType.class), buffer.m_130267_(), buffer.m_130267_()
         );
      }

      private static void handle(AWCoreNetwork.ClickWarehouseStorageEntryPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().m_5833_()
                  && context.getSender().f_36096_ instanceof WarehouseStorageMenu menu
                  && menu.f_38840_ == packet.containerId
                  && menu.m_6875_(context.getSender())) {
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
               ItemStack stack = player.m_21205_();
               if (!(stack.m_41720_() instanceof CommandBatonItem)) {
                  stack = player.m_21206_();
               }

               CommandBatonItem.clearSelections(stack, player);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record DraftingPreviewPacket(int containerId, int templateIndex, int requestId, int chunkIndex, int chunkCount, DraftingPreviewData data) {
      private static void encode(AWCoreNetwork.DraftingPreviewPacket packet, FriendlyByteBuf buffer) {
         DraftingPreviewData.requireChunkMetadata(packet.chunkIndex, packet.chunkCount);
         buffer.m_130130_(packet.containerId);
         buffer.m_130130_(packet.templateIndex);
         buffer.m_130130_(packet.requestId);
         buffer.m_130130_(packet.chunkIndex);
         buffer.m_130130_(packet.chunkCount);
         DraftingPreviewData.writeChunk(buffer, packet.data);
      }

      private static AWCoreNetwork.DraftingPreviewPacket decode(FriendlyByteBuf buffer) {
         if (buffer.readableBytes() >= 30000) {
            throw new IllegalArgumentException("Preview packet too large");
         } else {
            int containerId = buffer.m_130242_();
            int templateIndex = buffer.m_130242_();
            int requestId = buffer.m_130242_();
            int chunkIndex = buffer.m_130242_();
            int chunkCount = buffer.m_130242_();
            DraftingPreviewData.requireChunkMetadata(chunkIndex, chunkCount);
            return new AWCoreNetwork.DraftingPreviewPacket(containerId, templateIndex, requestId, chunkIndex, chunkCount, DraftingPreviewData.readChunk(buffer));
         }
      }

      private static void handle(AWCoreNetwork.DraftingPreviewPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            if (context.getSender() == null) {
               LocalPlayer player = Minecraft.m_91087_().f_91074_;
               if (player != null && player.f_36096_ instanceof DraftingStationMenu menu) {
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
            if (context.getSender() != null && context.getSender().m_20202_() instanceof BallistaMobileEntity ballista) {
               ballista.tryFire();
            } else if (context.getSender() != null && context.getSender().m_20202_() instanceof CatapultMobileEntity catapult) {
               catapult.tryFire();
            } else if (context.getSender() != null && context.getSender().m_20202_() instanceof CannonTurretStandEntity cannon) {
               cannon.tryFire();
            } else if (context.getSender() != null && context.getSender().m_20202_() instanceof HwachaEntity hwacha) {
               hwacha.tryFire();
            } else if (context.getSender() != null && context.getSender().m_20202_() instanceof BatteringRamEntity ram) {
               ram.tryAttack();
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record MailboxConfigPacket(BlockPos pos, int action, String value, boolean flag) {
      private static void encode(AWCoreNetwork.MailboxConfigPacket p, FriendlyByteBuf b) {
         b.m_130064_(p.pos);
         b.m_130130_(p.action);
         b.m_130072_(p.value, 128);
         b.writeBoolean(p.flag);
      }

      private static AWCoreNetwork.MailboxConfigPacket decode(FriendlyByteBuf b) {
         return new AWCoreNetwork.MailboxConfigPacket(b.m_130135_(), b.m_130242_(), b.m_130136_(128), b.readBoolean());
      }

      private static void handle(AWCoreNetwork.MailboxConfigPacket p, Supplier<Context> sup) {
         Context c = sup.get();
         c.enqueueWork(() -> {
            if (c.getSender() != null && c.getSender().m_9236_().m_7702_(p.pos) instanceof MailboxBlockEntity box && box.stillValid(c.getSender())) {
               String owner = c.getSender().m_36316_().getName();
               MailboxData data = MailboxData.get((ServerLevel)c.getSender().m_9236_());
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
               box.m_6596_();
               box.m_6596_();
            }
         });
         c.setPacketHandled(true);
      }
   }

   public static record MailboxListPacket(BlockPos pos, List<String> entries) {
      private static void encode(AWCoreNetwork.MailboxListPacket p, FriendlyByteBuf b) {
         b.m_130064_(p.pos);
         b.m_130130_(Math.min(256, p.entries.size()));

         for (String s : p.entries.stream().limit(256L).toList()) {
            b.m_130072_(s, 256);
         }
      }

      private static AWCoreNetwork.MailboxListPacket decode(FriendlyByteBuf b) {
         BlockPos pos = b.m_130135_();
         int n = b.m_130242_();
         List<String> e = new ArrayList<>();

         for (int i = 0; i < n; i++) {
            e.add(b.m_130136_(256));
         }

         return new AWCoreNetwork.MailboxListPacket(pos, e);
      }

      private static void handle(AWCoreNetwork.MailboxListPacket p, Supplier<Context> sup) {
         Context c = sup.get();
         c.enqueueWork(() -> {
            if (c.getSender() != null && p.entries.isEmpty() && c.getSender().m_9236_() instanceof ServerLevel sl) {
               List<String> list = MailboxData.get(sl).knownMailboxes().stream().map(i -> i.name() + " @ " + i.pos().m_123344_()).toList();
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
         buffer.m_130064_(packet.pos);
         buffer.m_130072_(packet.name, 48);
         buffer.writeBoolean(packet.delete);
      }

      private static AWCoreNetwork.ManageStructureTemplatePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.ManageStructureTemplatePacket(buffer.m_130135_(), buffer.m_130136_(48), buffer.readBoolean());
      }

      private static void handle(AWCoreNetwork.ManageStructureTemplatePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               ServerPlayer player = context.getSender();
               if (player != null && player.f_36096_ instanceof StructureScannerMenu menu && menu.pos().equals(packet.pos)) {
                  ItemStack scanner = player.m_21205_().m_150930_((Item)AWCoreItems.STRUCTURE_SCANNER.get()) ? player.m_21205_() : player.m_21206_();
                  if (scanner.m_150930_((Item)AWCoreItems.STRUCTURE_SCANNER.get())) {
                     if (packet.delete) {
                        boolean removed = PlayerStructureTemplateData.get(player.m_284548_()).remove(packet.name);
                        player.m_5661_(
                           Component.m_237110_(
                              removed ? "message.ancientwarfare.structure_scan.deleted" : "message.ancientwarfare.structure_scan.not_found",
                              new Object[]{packet.name}
                           ),
                           false
                        );
                     } else {
                        scanner.m_41784_().m_128473_("FirstPos");
                        scanner.m_41783_().m_128473_("SecondPos");
                        scanner.m_41783_().m_128473_("BuildOrigin");
                        player.m_5661_(Component.m_237115_("message.ancientwarfare.structure_scan.reset"), false);
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
         buffer.m_130130_(packet.containerId);
         buffer.m_130130_(packet.templateIndex);
         buffer.m_130130_(packet.requestId);
      }

      private static AWCoreNetwork.RequestDraftingPreviewPacket decode(FriendlyByteBuf buffer) {
         if (buffer.readableBytes() > 15) {
            throw new IllegalArgumentException("Preview request too large");
         } else {
            AWCoreNetwork.RequestDraftingPreviewPacket packet = new AWCoreNetwork.RequestDraftingPreviewPacket(
               buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_()
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
               if (player != null && player.f_36096_ instanceof DraftingStationMenu menu) {
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
         buffer.m_130064_(packet.pos);
         buffer.m_130072_(packet.name, 48);
      }

      private static AWCoreNetwork.SaveStructureScanPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SaveStructureScanPacket(buffer.m_130135_(), buffer.m_130136_(48));
      }

      private static void handle(AWCoreNetwork.SaveStructureScanPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               ServerPlayer player = context.getSender();
               if (player != null && player.f_36096_ instanceof StructureScannerMenu menu && menu.pos().equals(packet.pos)) {
                  ItemStack scanner = player.m_21205_().m_150930_((Item)AWCoreItems.STRUCTURE_SCANNER.get()) ? player.m_21205_() : player.m_21206_();
                  if (scanner.m_150930_((Item)AWCoreItems.STRUCTURE_SCANNER.get())
                     && scanner.m_41782_()
                     && scanner.m_41783_().m_128441_("FirstPos")
                     && scanner.m_41783_().m_128441_("SecondPos")) {
                     BlockPos first = BlockPos.m_122022_(scanner.m_41783_().m_128454_("FirstPos"));
                     BlockPos second = BlockPos.m_122022_(scanner.m_41783_().m_128454_("SecondPos"));
                     BlockPos origin = scanner.m_41783_().m_128441_("BuildOrigin")
                        ? BlockPos.m_122022_(scanner.m_41783_().m_128454_("BuildOrigin"))
                        : packet.pos.m_7494_();
                     PlayerStructureTemplateData.CaptureResult result = PlayerStructureTemplateData.get(player.m_284548_())
                        .capture(player.m_284548_(), packet.name, first, second, origin);
                     if (result.success()) {
                        scanner.m_41783_().m_128473_("FirstPos");
                        scanner.m_41783_().m_128473_("SecondPos");
                        scanner.m_41783_().m_128473_("BuildOrigin");
                     }

                     player.m_5661_(Component.m_237110_(result.messageKey(), new Object[]{result.blockCount()}), false);
                  } else {
                     player.m_5661_(Component.m_237115_("message.ancientwarfare.structure_scan.incomplete"), false);
                  }
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SelectDraftingTemplatePacket(BlockPos pos, String name, boolean start) {
      private static void encode(AWCoreNetwork.SelectDraftingTemplatePacket packet, FriendlyByteBuf buffer) {
         buffer.m_130064_(packet.pos);
         buffer.m_130072_(packet.name, 48);
         buffer.writeBoolean(packet.start);
      }

      private static AWCoreNetwork.SelectDraftingTemplatePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SelectDraftingTemplatePacket(buffer.m_130135_(), buffer.m_130136_(48), buffer.readBoolean());
      }

      private static void handle(AWCoreNetwork.SelectDraftingTemplatePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.m_9236_().m_7702_(packet.pos) instanceof DraftingStationBlockEntity station) {
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
         buffer.m_130064_(packet.pos);
         buffer.m_130072_(packet.config.entityId(), 128);
         buffer.m_130072_(packet.config.factionName(), 64);
         buffer.m_130072_(packet.config.entityNbt(), 2048);
         buffer.m_130130_(packet.config.minToSpawn());
         buffer.m_130130_(packet.config.maxToSpawn());
         buffer.m_130130_(packet.config.spawnDelay());
         buffer.m_130130_(packet.config.playerRange());
         buffer.m_130130_(packet.config.mobRange());
         buffer.m_130130_(packet.config.spawnRange());
         buffer.m_130130_(packet.config.maxNearby());
         buffer.writeInt(packet.config.spawnYOffset());
         buffer.writeBoolean(packet.config.lightSensitive());
         buffer.writeBoolean(packet.config.redstoneSensitive());
         buffer.writeBoolean(packet.config.redstoneMode());
         buffer.writeBoolean(packet.config.transparent());
         buffer.writeBoolean(packet.config.debugMode());
      }

      private static AWCoreNetwork.SetAdvancedSpawnerConfigPacket decode(FriendlyByteBuf buffer) {
         BlockPos pos = buffer.m_130135_();
         AdvancedSpawnerConfig config = new AdvancedSpawnerConfig(
            buffer.m_130136_(128),
            buffer.m_130136_(64),
            buffer.m_130136_(2048),
            buffer.m_130242_(),
            buffer.m_130242_(),
            buffer.m_130242_(),
            buffer.m_130242_(),
            buffer.m_130242_(),
            buffer.m_130242_(),
            buffer.m_130242_(),
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
            if (context.getSender() != null && context.getSender().m_9236_().m_7702_(packet.pos) instanceof AdvancedSpawnerBlockEntity spawner) {
               spawner.applyConfig(packet.config);
               context.getSender().m_5661_(spawner.statusMessage(), true);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record SetBardTracksPacket(int entityId, List<BardTrack> tracks) {
      private static void encode(AWCoreNetwork.SetBardTracksPacket packet, FriendlyByteBuf buffer) {
         buffer.m_130130_(packet.entityId);
         buffer.m_130130_(Math.min(32, packet.tracks.size()));

         for (BardTrack track : packet.tracks.stream().limit(32L).toList()) {
            track.toNetwork(buffer);
         }
      }

      private static AWCoreNetwork.SetBardTracksPacket decode(FriendlyByteBuf buffer) {
         int entityId = buffer.m_130242_();
         int count = Math.min(32, buffer.m_130242_());
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
               if (context.getSender().m_9236_().m_6815_(packet.entityId) instanceof BardNpcEntity bard && bard.hasCommandPermission(context.getSender())) {
                  bard.setBardTracks(packet.tracks);
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static record SetNpcSkinPacket(int entityId, String skinName, boolean alexModel) {
      private static void encode(AWCoreNetwork.SetNpcSkinPacket packet, FriendlyByteBuf buffer) {
         buffer.m_130130_(packet.entityId);
         buffer.m_130072_(packet.skinName, 128);
         buffer.writeBoolean(packet.alexModel);
      }

      private static AWCoreNetwork.SetNpcSkinPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetNpcSkinPacket(buffer.m_130242_(), buffer.m_130136_(128), buffer.readBoolean());
      }

      private static void handle(AWCoreNetwork.SetNpcSkinPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null) {
                  if (context.getSender().m_9236_().m_6815_(packet.entityId) instanceof PlayerOwnedNpcEntity npc
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
         buffer.m_130064_(packet.pos);
         buffer.m_130130_(packet.action);
         buffer.m_130130_(packet.index);
         buffer.m_130055_(packet.stack);
         buffer.m_130130_(packet.quantity);
      }

      private static AWCoreNetwork.SetWarehouseInterfaceFilterPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseInterfaceFilterPacket(
            buffer.m_130135_(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130267_(), buffer.m_130242_()
         );
      }

      private static void handle(AWCoreNetwork.SetWarehouseInterfaceFilterPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().m_5833_()
                  && context.getSender().f_36096_ instanceof WarehouseInterfaceMenu menu
                  && menu.getPos().equals(packet.pos)
                  && menu.m_6875_(context.getSender())
                  && context.getSender().m_9236_().m_46805_(packet.pos)
                  && context.getSender().m_9236_().m_7702_(packet.pos) instanceof WarehouseInterfaceBlockEntity warehouseInterface
                  && packet.action >= 0
                  && packet.action <= 2
                  && (packet.action == 1 || packet.index >= 0 && packet.index < warehouseInterface.getFilterCount())
                  && packet.quantity >= 0) {
                  if (packet.action == 1) {
                     warehouseInterface.addFilter();
                  } else if (packet.action == 2) {
                     warehouseInterface.removeFilter(packet.index);
                  } else {
                     warehouseInterface.setFilter(packet.index, packet.stack.m_41619_() ? ItemStack.f_41583_ : packet.stack.m_255036_(1), packet.quantity);
                  }
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SetWarehouseScrollPacket(int row) {
      private static void encode(AWCoreNetwork.SetWarehouseScrollPacket packet, FriendlyByteBuf buffer) {
         buffer.m_130130_(Math.max(0, packet.row));
      }

      private static AWCoreNetwork.SetWarehouseScrollPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseScrollPacket(Math.max(0, buffer.m_130242_()));
      }

      private static void handle(AWCoreNetwork.SetWarehouseScrollPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().m_5833_()
                  && context.getSender().f_36096_ instanceof WarehouseControlMenu menu
                  && menu.m_6875_(context.getSender())) {
                  menu.scrollToRow(packet.row);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SetWarehouseSearchPacket(String search) {
      private static void encode(AWCoreNetwork.SetWarehouseSearchPacket packet, FriendlyByteBuf buffer) {
         buffer.m_130072_(packet.search, 64);
      }

      private static AWCoreNetwork.SetWarehouseSearchPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseSearchPacket(buffer.m_130136_(64));
      }

      private static void handle(AWCoreNetwork.SetWarehouseSearchPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().m_5833_()
                  && context.getSender().f_36096_ instanceof WarehouseControlMenu menu
                  && menu.m_6875_(context.getSender())) {
                  menu.setSearch(packet.search);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SetWarehouseStockFilterPacket(BlockPos pos, int index, ItemStack stack) {
      private static void encode(AWCoreNetwork.SetWarehouseStockFilterPacket packet, FriendlyByteBuf buffer) {
         buffer.m_130064_(packet.pos);
         buffer.m_130130_(packet.index);
         buffer.m_130055_(packet.stack);
      }

      private static AWCoreNetwork.SetWarehouseStockFilterPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseStockFilterPacket(buffer.m_130135_(), buffer.m_130242_(), buffer.m_130267_());
      }

      private static void handle(AWCoreNetwork.SetWarehouseStockFilterPacket packet, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().m_5833_()
                  && context.getSender().f_36096_ instanceof WarehouseStockViewerMenu menu
                  && menu.getPos().equals(packet.pos)
                  && menu.m_6875_(context.getSender())
                  && context.getSender().m_9236_().m_46805_(packet.pos)
                  && packet.index >= 0
                  && packet.index < 4
                  && context.getSender().m_9236_().m_7702_(packet.pos) instanceof WarehouseStockViewerBlockEntity viewer) {
                  viewer.setFilterItem(packet.index, packet.stack.m_41619_() ? ItemStack.f_41583_ : packet.stack.m_255036_(1));
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record SetWarehouseStorageScrollPacket(int containerId, int pixels) {
      public static void encode(AWCoreNetwork.SetWarehouseStorageScrollPacket packet, FriendlyByteBuf buffer) {
         buffer.m_130130_(packet.containerId);
         buffer.m_130130_(Math.max(0, packet.pixels));
      }

      public static AWCoreNetwork.SetWarehouseStorageScrollPacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.SetWarehouseStorageScrollPacket(buffer.m_130242_(), Math.max(0, buffer.m_130242_()));
      }

      private static void handle(AWCoreNetwork.SetWarehouseStorageScrollPacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               if (context.getSender() != null
                  && !context.getSender().m_5833_()
                  && context.getSender().f_36096_ instanceof WarehouseStorageMenu menu
                  && menu.f_38840_ == packet.containerId
                  && menu.m_6875_(context.getSender())) {
                  menu.scrollToPixel(packet.pixels);
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static record TraderRoutePacket(int traderEntityId, BlockPos target) {
      private static void encode(AWCoreNetwork.TraderRoutePacket packet, FriendlyByteBuf buffer) {
         buffer.m_130130_(packet.traderEntityId);
         buffer.m_130064_(packet.target);
      }

      private static AWCoreNetwork.TraderRoutePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.TraderRoutePacket(buffer.m_130242_(), buffer.m_130135_());
      }

      private static void handle(AWCoreNetwork.TraderRoutePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(
            () -> {
               ServerPlayer player = context.getSender();
               if (player != null
                  && player.f_36096_ instanceof TradeOrderMenu menu
                  && menu.isTraderBound()
                  && menu.getTraderEntityId() == packet.traderEntityId) {
                  if (player.m_9236_().m_6815_(packet.traderEntityId) instanceof TraderNpcEntity trader
                     && trader.m_6084_()
                     && trader.hasCommandPermission(player)
                     && AWCoreRegistryAccess.is(trader.getOrdersStack(), AWCoreIds.TRADE_ORDER)) {
                     BlockPos target = packet.target;
                     ServerLevel level = player.m_284548_();
                     if (target.m_123342_() >= level.m_141937_() && target.m_123342_() < level.m_151558_() && level.m_6857_().m_61937_(target)) {
                        ItemStack order = trader.getOrdersStack().m_41777_();
                        TradeOrderData.setRouteTarget(order, target);
                        trader.setOrdersStack(order);
                        trader.clearCommand();
                        trader.m_8127_();
                        trader.m_21573_().m_26573_();
                        trader.m_20256_(Vec3.f_82478_);
                        level.m_46865_(target);
                        trader.m_6021_((double)target.m_123341_() + 0.5, (double)target.m_123342_(), (double)target.m_123343_() + 0.5);
                        player.m_5661_(
                           Component.m_237110_("guistrings.npc.trade_order.departed", new Object[]{target.m_123341_(), target.m_123342_(), target.m_123343_()}),
                           true
                        );
                        return;
                     }

                     player.m_5661_(Component.m_237115_("guistrings.npc.trade_order.destination_invalid"), true);
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
         buffer.m_130085_(packet.recipeId);
      }

      private static AWCoreNetwork.TransferEngineeringRecipePacket decode(FriendlyByteBuf buffer) {
         return new AWCoreNetwork.TransferEngineeringRecipePacket(buffer.m_130281_());
      }

      private static void handle(AWCoreNetwork.TransferEngineeringRecipePacket packet, Supplier<Context> supplier) {
         Context context = supplier.get();
         context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
               boolean transferred = false;
               if (player.f_36096_ instanceof EngineeringStationMenu menu) {
                  transferred = menu.transferEngineeringRecipe(player, packet.recipeId);
               } else if (player.f_36096_ instanceof AutoCraftingMenu menu) {
                  transferred = menu.transferEngineeringRecipe(player, packet.recipeId);
               } else if (player.f_36096_ instanceof WarehouseCraftingStationMenu menu) {
                  transferred = menu.transferEngineeringRecipe(player, packet.recipeId);
               }

               if (!transferred) {
                  player.m_5661_(Component.m_237115_("jei.ancientwarfare.transfer_failed"), true);
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
            if (context.getSender() != null && context.getSender().m_20202_() instanceof CannonMobileEntity cannonMobile) {
               cannonMobile.setVehicleInput(packet.forward, packet.turn);
            } else if (context.getSender() != null && context.getSender().m_20202_() instanceof CatapultTurretStandEntity catapult) {
               catapult.applyStandInput(packet.forward, packet.turn);
            } else if (context.getSender() != null && context.getSender().m_20202_() instanceof CannonTurretStandEntity cannon) {
               cannon.applyStandInput(packet.forward, packet.turn);
            } else if (context.getSender() != null && context.getSender().m_20202_() instanceof ChestPushcartEntity cart) {
               cart.setVehicleInput(packet.forward, packet.turn);
            }
         });
         context.setPacketHandled(true);
      }
   }
}
