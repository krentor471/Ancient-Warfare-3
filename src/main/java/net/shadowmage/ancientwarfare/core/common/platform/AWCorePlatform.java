package net.shadowmage.ancientwarfare.core.common.platform;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.inventory.ItemHandlerAccess;
import net.shadowmage.ancientwarfare.core.common.npc.BardTrack;
import net.shadowmage.ancientwarfare.core.common.structure.AdvancedSpawnerConfig;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkSite;

public final class AWCorePlatform {
   private static PlatformMenuOpener menuOpener = new AWCorePlatform.NoopMenuOpener();
   private static PlatformChunkLoader chunkLoader = new AWCorePlatform.NoopChunkLoader();
   private static PlatformAttributeAccess attributeAccess = new AWCorePlatform.NoopAttributeAccess();
   private static PlatformEntitySpawnPacket entitySpawnPacket = entity -> null;
   private static PlatformModLoader modLoader = modId -> false;
   private static PlatformConfigDirectory configDirectory = () -> Path.of("config");
   private static PlatformFuel fuel = stack -> 0;
   private static PlatformWorkSiteAccess workSiteAccess = (level, pos) -> Optional.empty();
   private static PlatformInventoryAccess inventoryAccess = (blockEntity, side) -> null;
   private static PlatformTagAccess tagAccess = new AWCorePlatform.NoopTagAccess();
   private static PlatformGameplayConfig gameplayConfig = new AWCorePlatform.NoopGameplayConfig();
   private static PlatformSoundAccess soundAccess = name -> null;
   private static PlatformMailboxNetwork mailboxNetwork = new AWCorePlatform.NoopMailboxNetwork();
   private static PlatformWarehouseNetwork warehouseNetwork = new AWCorePlatform.NoopWarehouseNetwork();
   private static PlatformStructureNetwork structureNetwork = new AWCorePlatform.NoopStructureNetwork();
   private static PlatformNpcNetwork npcNetwork = new AWCorePlatform.NoopNpcNetwork();
   private static PlatformClientNetwork clientNetwork = new AWCorePlatform.NoopClientNetwork();
   private static PlatformBlockEntityData blockEntityData = blockEntity -> blockEntity instanceof CommonBlockEntityDataHolder holder
         ? holder.ancientWarfareData()
         : new CompoundTag();

   private AWCorePlatform() {
   }

   public static void setMenuOpener(PlatformMenuOpener opener) {
      menuOpener = (PlatformMenuOpener)(opener == null ? new AWCorePlatform.NoopMenuOpener() : opener);
   }

   public static void setChunkLoader(PlatformChunkLoader loader) {
      chunkLoader = (PlatformChunkLoader)(loader == null ? new AWCorePlatform.NoopChunkLoader() : loader);
   }

   public static void setAttributeAccess(PlatformAttributeAccess access) {
      attributeAccess = (PlatformAttributeAccess)(access == null ? new AWCorePlatform.NoopAttributeAccess() : access);
   }

   public static Attribute entityReachAttribute() {
      return attributeAccess.entityReachAttribute();
   }

   public static void setEntitySpawnPacket(PlatformEntitySpawnPacket adapter) {
      entitySpawnPacket = adapter == null ? entity -> null : adapter;
   }

   public static Packet<ClientGamePacketListener> createEntitySpawnPacket(Entity entity) {
      return entitySpawnPacket.create(entity);
   }

   public static void setModLoader(PlatformModLoader adapter) {
      modLoader = adapter == null ? modId -> false : adapter;
   }

   public static boolean isModLoaded(String modId) {
      return modLoader.isLoaded(modId);
   }

   public static void setConfigDirectory(PlatformConfigDirectory adapter) {
      configDirectory = adapter == null ? () -> Path.of("config") : adapter;
   }

   public static Path configDirectory() {
      return configDirectory.configDirectory();
   }

   public static void setFuel(PlatformFuel adapter) {
      fuel = adapter == null ? stack -> 0 : adapter;
   }

   public static int fuelBurnTime(ItemStack stack) {
      return fuel.burnTime(stack);
   }

   public static void setWorkSiteAccess(PlatformWorkSiteAccess adapter) {
      workSiteAccess = adapter == null ? (level, pos) -> Optional.empty() : adapter;
   }

   public static Optional<WorkSite> findCompatibleWorkSite(Level level, BlockPos pos) {
      return level != null && pos != null ? workSiteAccess.findCompatibleWorkSite(level, pos) : Optional.empty();
   }

   public static void setInventoryAccess(PlatformInventoryAccess adapter) {
      inventoryAccess = adapter == null ? (blockEntity, side) -> null : adapter;
   }

   public static ItemHandlerAccess findItemHandler(BlockEntity blockEntity, Direction side) {
      return blockEntity == null ? null : inventoryAccess.find(blockEntity, side);
   }

   public static void setTagAccess(PlatformTagAccess adapter) {
      tagAccess = (PlatformTagAccess)(adapter == null ? new AWCorePlatform.NoopTagAccess() : adapter);
   }

   public static boolean isOre(BlockState state) {
      return state != null && tagAccess.isOre(state);
   }

   public static boolean isBiomeTagged(Holder<Biome> biome, PlatformTagAccess.BiomeTag tag) {
      return biome != null && tag != null && tagAccess.isBiome(biome, tag);
   }

   public static void setGameplayConfig(PlatformGameplayConfig config) {
      gameplayConfig = (PlatformGameplayConfig)(config == null ? new AWCorePlatform.NoopGameplayConfig() : config);
   }

   public static PlatformGameplayConfig gameplayConfig() {
      return gameplayConfig;
   }

   public static void setSoundAccess(PlatformSoundAccess access) {
      soundAccess = access == null ? name -> null : access;
   }

   public static SoundEvent npcVoice(String name) {
      return soundAccess.npcVoice(name);
   }

   public static void setMailboxNetwork(PlatformMailboxNetwork network) {
      mailboxNetwork = (PlatformMailboxNetwork)(network == null ? new AWCorePlatform.NoopMailboxNetwork() : network);
   }

   public static void sendMailboxConfig(BlockPos pos, int action, String value, boolean flag) {
      mailboxNetwork.sendConfig(pos, action, value, flag);
   }

   public static void requestMailboxList(BlockPos pos) {
      mailboxNetwork.requestList(pos);
   }

   public static void setWarehouseNetwork(PlatformWarehouseNetwork network) {
      warehouseNetwork = (PlatformWarehouseNetwork)(network == null ? new AWCorePlatform.NoopWarehouseNetwork() : network);
   }

   public static void setWarehouseInterfaceFilter(BlockPos pos, int index, ItemStack stack, int quantity) {
      warehouseNetwork.setInterfaceFilter(pos, index, stack, quantity);
   }

   public static void addWarehouseInterfaceFilter(BlockPos pos) {
      warehouseNetwork.addInterfaceFilter(pos);
   }

   public static void removeWarehouseInterfaceFilter(BlockPos pos, int index) {
      warehouseNetwork.removeInterfaceFilter(pos, index);
   }

   public static void setWarehouseSearch(String search) {
      warehouseNetwork.setSearch(search);
   }

   public static void setWarehouseScrollRow(int row) {
      warehouseNetwork.setScrollRow(row);
   }

   public static void clickWarehouseEntry(int containerId, int slot, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
      warehouseNetwork.clickEntry(containerId, slot, button, clickType, expectedItem, expectedCarried);
   }

   public static void setWarehouseStorageScrollPixels(int containerId, int pixels) {
      warehouseNetwork.setStorageScrollPixels(containerId, pixels);
   }

   public static void clickWarehouseStorageEntry(int containerId, int entry, int button, ClickType clickType, ItemStack expectedItem, ItemStack expectedCarried) {
      warehouseNetwork.clickStorageEntry(containerId, entry, button, clickType, expectedItem, expectedCarried);
   }

   public static void changeWarehouseStorageFilter(
      int containerId, int action, int index, int expectedCount, ItemStack expectedFilter, ItemStack expectedCarried
   ) {
      warehouseNetwork.changeStorageFilter(containerId, action, index, expectedCount, expectedFilter, expectedCarried);
   }

   public static void setWarehouseStockFilter(BlockPos pos, int index, ItemStack stack) {
      warehouseNetwork.setStockFilter(pos, index, stack);
   }

   public static void setStructureNetwork(PlatformStructureNetwork network) {
      structureNetwork = (PlatformStructureNetwork)(network == null ? new AWCorePlatform.NoopStructureNetwork() : network);
   }

   public static void saveStructureScan(BlockPos pos, String name) {
      structureNetwork.saveScan(pos, name);
   }

   public static void manageStructureTemplate(BlockPos pos, String name, boolean delete) {
      structureNetwork.manageTemplate(pos, name, delete);
   }

   public static void requestDraftingPreview(int containerId, int templateIndex, int requestId) {
      structureNetwork.requestDraftingPreview(containerId, templateIndex, requestId);
   }

   public static void setAdvancedSpawnerConfig(BlockPos pos, AdvancedSpawnerConfig config) {
      structureNetwork.setAdvancedSpawnerConfig(pos, config);
   }

   public static void setNpcNetwork(PlatformNpcNetwork network) {
      npcNetwork = (PlatformNpcNetwork)(network == null ? new AWCorePlatform.NoopNpcNetwork() : network);
   }

   public static void setNpcSkin(int entityId, String skinName, boolean alexModel) {
      npcNetwork.setSkin(entityId, skinName, alexModel);
   }

   public static void setBardTracks(int entityId, List<BardTrack> tracks) {
      npcNetwork.setBardTracks(entityId, tracks);
   }

   public static void setClientNetwork(PlatformClientNetwork network) {
      clientNetwork = (PlatformClientNetwork)(network == null ? new AWCorePlatform.NoopClientNetwork() : network);
   }

   public static void setBlockEntityData(PlatformBlockEntityData access) {
      blockEntityData = access == null
         ? blockEntity -> blockEntity instanceof CommonBlockEntityDataHolder holder ? holder.ancientWarfareData() : new CompoundTag()
         : access;
   }

   public static CompoundTag blockEntityData(BlockEntity blockEntity) {
      return blockEntity == null ? new CompoundTag() : blockEntityData.data(blockEntity);
   }

   public static void addWorkOrderPoint(BlockPos pos, Direction side) {
      clientNetwork.addWorkOrderPoint(pos, side);
   }

   public static void clearCommandBatonSelections() {
      clientNetwork.clearCommandBatonSelections();
   }

   public static void setVehicleInput(float forward, float turn) {
      clientNetwork.setVehicleInput(forward, turn);
   }

   public static void adjustVehicleWeapon(float pitchDelta, float yawDelta) {
      clientNetwork.adjustVehicleWeapon(pitchDelta, yawDelta);
   }

   public static void fireVehicleWeapon() {
      clientNetwork.fireVehicleWeapon();
   }

   public static void sendTraderToRoute(int traderEntityId, BlockPos target) {
      clientNetwork.sendTraderToRoute(traderEntityId, target);
   }

   public static void openMenu(Player player, MenuProvider provider) {
      openMenu(player, provider, buffer -> {
      });
   }

   public static void openMenu(Player player, MenuProvider provider, Consumer<FriendlyByteBuf> extraDataWriter) {
      menuOpener.openMenu(player, provider, extraDataWriter);
   }

   public static void openBlockMenu(Player player, MenuProvider provider, BlockPos pos) {
      menuOpener.openBlockMenu(player, provider, pos);
   }

   public static boolean setChunkForced(ServerLevel level, BlockPos owner, ChunkPos chunk, boolean forced, boolean ticking) {
      return chunkLoader.setForced(level, owner, chunk, forced, ticking);
   }

   private static final class NoopAttributeAccess implements PlatformAttributeAccess {
      @Override
      public Attribute entityReachAttribute() {
         return null;
      }
   }

   private static final class NoopChunkLoader implements PlatformChunkLoader {
      @Override
      public boolean setForced(ServerLevel level, BlockPos owner, ChunkPos chunk, boolean forced, boolean ticking) {
         return false;
      }
   }

   private static final class NoopClientNetwork implements PlatformClientNetwork {
      @Override
      public void addWorkOrderPoint(BlockPos pos, Direction side) {
      }

      @Override
      public void clearCommandBatonSelections() {
      }

      @Override
      public void setVehicleInput(float forward, float turn) {
      }

      @Override
      public void adjustVehicleWeapon(float pitchDelta, float yawDelta) {
      }

      @Override
      public void fireVehicleWeapon() {
      }

      @Override
      public void sendTraderToRoute(int traderEntityId, BlockPos target) {
      }
   }

   private static final class NoopGameplayConfig implements PlatformGameplayConfig {
      @Override
      public int effectiveLootRolls(ResourceLocation table, int baseRolls, long seed) {
         return baseRolls;
      }

      @Override
      public double lootItemCountMultiplier() {
         return 1.0;
      }

      @Override
      public boolean preventDuplicateResearchNotes() {
         return true;
      }

      @Override
      public boolean fillUnconfiguredStructureContainers() {
         return true;
      }

      @Override
      public boolean awsEnabled() {
         return true;
      }

      @Override
      public boolean largeAquaticAwsEnabled() {
         return true;
      }

      @Override
      public int awsRegionChunks() {
         return 12;
      }

      @Override
      public int awsSpawnProtectionDistance() {
         return 400;
      }

      @Override
      public int awsMinimumDistance() {
         return 192;
      }

      @Override
      public int awsAquaticMinimumDistance() {
         return 320;
      }

      @Override
      public boolean awtEnabled() {
         return true;
      }

      @Override
      public int awtRegionChunks() {
         return 96;
      }

      @Override
      public int awtSpawnProtectionDistance() {
         return 1200;
      }

      @Override
      public int awtMinimumDistance() {
         return 1800;
      }
   }

   private static final class NoopMailboxNetwork implements PlatformMailboxNetwork {
      @Override
      public void sendConfig(BlockPos pos, int action, String value, boolean flag) {
      }

      @Override
      public void requestList(BlockPos pos) {
      }
   }

   private static final class NoopMenuOpener implements PlatformMenuOpener {
      @Override
      public void openMenu(Player player, MenuProvider provider, Consumer<FriendlyByteBuf> extraDataWriter) {
      }

      @Override
      public void openBlockMenu(Player player, MenuProvider provider, BlockPos pos) {
      }
   }

   private static final class NoopNpcNetwork implements PlatformNpcNetwork {
      @Override
      public void setSkin(int entityId, String skinName, boolean alexModel) {
      }

      @Override
      public void setBardTracks(int entityId, List<BardTrack> tracks) {
      }
   }

   private static final class NoopStructureNetwork implements PlatformStructureNetwork {
      @Override
      public void saveScan(BlockPos pos, String name) {
      }

      @Override
      public void manageTemplate(BlockPos pos, String name, boolean delete) {
      }

      @Override
      public void requestDraftingPreview(int containerId, int templateIndex, int requestId) {
      }

      @Override
      public void setAdvancedSpawnerConfig(BlockPos pos, AdvancedSpawnerConfig config) {
      }
   }

   private static final class NoopTagAccess implements PlatformTagAccess {
      @Override
      public boolean isOre(BlockState state) {
         return false;
      }

      @Override
      public boolean isBiome(Holder<Biome> biome, PlatformTagAccess.BiomeTag tag) {
         return false;
      }
   }

   private static final class NoopWarehouseNetwork implements PlatformWarehouseNetwork {
      @Override
      public void setInterfaceFilter(BlockPos pos, int index, ItemStack stack, int quantity) {
      }

      @Override
      public void addInterfaceFilter(BlockPos pos) {
      }

      @Override
      public void removeInterfaceFilter(BlockPos pos, int index) {
      }

      @Override
      public void setSearch(String search) {
      }

      @Override
      public void setScrollRow(int row) {
      }

      @Override
      public void setStockFilter(BlockPos pos, int index, ItemStack stack) {
      }
   }
}
