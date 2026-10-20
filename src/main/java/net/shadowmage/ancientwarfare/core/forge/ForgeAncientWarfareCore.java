package net.shadowmage.ancientwarfare.core.forge;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.BardTrack;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformClientNetwork;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformMailboxNetwork;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformNpcNetwork;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformStructureNetwork;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformWarehouseNetwork;
import net.shadowmage.ancientwarfare.core.common.structure.AdvancedSpawnerConfig;
import net.shadowmage.ancientwarfare.core.compat.CreateHandCrankCompat;
import net.shadowmage.ancientwarfare.core.forge.config.AWLootConfig;
import net.shadowmage.ancientwarfare.core.forge.config.AWNpcConfig;
import net.shadowmage.ancientwarfare.core.forge.config.AWStructureConfig;
import net.shadowmage.ancientwarfare.core.forge.config.AWTargetingConfig;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreSounds;
import net.shadowmage.ancientwarfare.core.structure.AWStructureTemplateOverrides;

@Mod("ancientwarfare")
public final class ForgeAncientWarfareCore {
   public ForgeAncientWarfareCore() {
      AWCorePlatform.setMenuOpener(new ForgeMenuOpener());
      AWCorePlatform.setChunkLoader(new ForgeChunkLoader());
      AWCorePlatform.setAttributeAccess(new ForgeAttributeAccess());
      AWCorePlatform.setEntitySpawnPacket(new ForgeEntitySpawnPacket());
      AWCorePlatform.setModLoader(ForgePlatformAdapters.modLoader());
      AWCorePlatform.setConfigDirectory(ForgePlatformAdapters.configDirectory());
      AWCorePlatform.setFuel(ForgePlatformAdapters.fuel());
      AWCorePlatform.setWorkSiteAccess(CreateHandCrankCompat::getWorkSite);
      AWCorePlatform.setInventoryAccess(new ForgeInventoryAccess());
      AWCorePlatform.setTagAccess(new ForgeTagAccess());
      AWCorePlatform.setGameplayConfig(new ForgeGameplayConfig());
      AWCorePlatform.setBlockEntityData(BlockEntity::getPersistentData);
      AWCorePlatform.setSoundAccess(AWCoreSounds::npcVoice);
      AWCorePlatform.setMailboxNetwork(new PlatformMailboxNetwork() {
         @Override
         public void sendConfig(BlockPos pos, int action, String value, boolean flag) {
            AWCoreNetwork.sendMailboxConfig(pos, action, value, flag);
         }

         @Override
         public void requestList(BlockPos pos) {
            AWCoreNetwork.requestMailboxList(pos);
         }
      });
      AWCorePlatform.setWarehouseNetwork(new PlatformWarehouseNetwork() {
         @Override
         public void setInterfaceFilter(BlockPos pos, int index, ItemStack stack, int quantity) {
            AWCoreNetwork.sendSetWarehouseInterfaceFilter(pos, index, stack, quantity);
         }

         @Override
         public void addInterfaceFilter(BlockPos pos) {
            AWCoreNetwork.sendAddWarehouseInterfaceFilter(pos);
         }

         @Override
         public void removeInterfaceFilter(BlockPos pos, int index) {
            AWCoreNetwork.sendRemoveWarehouseInterfaceFilter(pos, index);
         }

         @Override
         public void setSearch(String search) {
            AWCoreNetwork.sendSetWarehouseSearch(search);
         }

         @Override
         public void setScrollRow(int row) {
            AWCoreNetwork.sendSetWarehouseScrollRow(row);
         }

         @Override
         public void clickEntry(int containerId, int slot, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
            AWCoreNetwork.sendClickWarehouseEntry(containerId, slot, button, clickType, expectedItem, expectedCarried);
         }

         @Override
         public void setStorageScrollPixels(int containerId, int pixels) {
            AWCoreNetwork.sendSetWarehouseStorageScroll(containerId, pixels);
         }

         @Override
         public void clickStorageEntry(int containerId, int entry, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
            AWCoreNetwork.sendClickWarehouseStorageEntry(containerId, entry, button, clickType, expectedItem, expectedCarried);
         }

         @Override
         public void changeStorageFilter(int containerId, int action, int index, int expectedCount, ItemStack expectedFilter, ItemStack expectedCarried) {
            AWCoreNetwork.sendChangeWarehouseStorageFilter(containerId, action, index, expectedCount, expectedFilter, expectedCarried);
         }

         @Override
         public void setStockFilter(BlockPos pos, int index, ItemStack stack) {
            AWCoreNetwork.sendSetWarehouseStockFilter(pos, index, stack);
         }
      });
      AWCorePlatform.setStructureNetwork(new PlatformStructureNetwork() {
         @Override
         public void saveScan(BlockPos pos, String name) {
            AWCoreNetwork.sendSaveStructureScan(pos, name);
         }

         @Override
         public void manageTemplate(BlockPos pos, String name, boolean delete) {
            AWCoreNetwork.sendManageStructureTemplate(pos, name, delete);
         }

         @Override
         public void requestDraftingPreview(int containerId, int templateIndex, int requestId) {
            AWCoreNetwork.requestDraftingPreview(containerId, templateIndex, requestId);
         }

         @Override
         public void setAdvancedSpawnerConfig(BlockPos pos, AdvancedSpawnerConfig config) {
            AWCoreNetwork.sendSetAdvancedSpawnerConfig(pos, config);
         }
      });
      AWCorePlatform.setNpcNetwork(new PlatformNpcNetwork() {
         @Override
         public void setSkin(int entityId, String skinName, boolean alexModel) {
            AWCoreNetwork.sendSetNpcSkin(entityId, skinName, alexModel);
         }

         @Override
         public void setBardTracks(int entityId, List<BardTrack> tracks) {
            AWCoreNetwork.sendSetBardTracks(entityId, tracks);
         }
      });
      AWCorePlatform.setClientNetwork(new PlatformClientNetwork() {
         @Override
         public void addWorkOrderPoint(BlockPos pos, Direction side) {
            AWCoreNetwork.sendAddWorkOrderPoint(pos, side);
         }

         @Override
         public void clearCommandBatonSelections() {
            AWCoreNetwork.sendCommandBatonHotkey();
         }

         @Override
         public void setVehicleInput(float forward, float turn) {
            AWCoreNetwork.sendVehicleInput(forward, turn);
         }

         @Override
         public void adjustVehicleWeapon(float pitchDelta, float yawDelta) {
            AWCoreNetwork.sendAdjustBallistaTurret(pitchDelta, yawDelta);
         }

         @Override
         public void fireVehicleWeapon() {
            AWCoreNetwork.sendFireBallista();
         }

         @Override
         public void sendTraderToRoute(int traderEntityId, BlockPos target) {
            AWCoreNetwork.sendTraderToRoute(traderEntityId, target);
         }
      });
      AWCoreNetwork.register();
      FMLJavaModLoadingContext.get().getModEventBus().addListener(AWTargetingConfig::onConfigChanged);
      FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeConfigReloadEvents::onTomlReload);
      ModLoadingContext.get().registerConfig(Type.COMMON, AWLootConfig.SPEC, "ancientwarfare-loot.toml");
      ModLoadingContext.get().registerConfig(Type.COMMON, AWNpcConfig.SPEC, "ancientwarfare-npc.toml");
      ModLoadingContext.get().registerConfig(Type.COMMON, AWStructureConfig.SPEC, "ancientwarfare-structures.toml");
      ModLoadingContext.get().registerConfig(Type.COMMON, AWTargetingConfig.SPEC, "ancientwarfare-targeting.toml");
      ForgeLootTableOverridePack.initialize();
      AWStructureTemplateOverrides.initialize();
      ForgeCoreRegistries.register(FMLJavaModLoadingContext.get().getModEventBus());
      AncientWarfareCore.LOG.info("Ancient Warfare 3 loaded");
   }
}
