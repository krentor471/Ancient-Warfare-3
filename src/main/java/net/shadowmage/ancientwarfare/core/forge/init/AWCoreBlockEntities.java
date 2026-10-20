package net.shadowmage.ancientwarfare.core.forge.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.tile.AdvancedLootChestBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.AdvancedSpawnerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.AnimalFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.AutoCraftingBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.ChunkLoaderBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.CropFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.DraftingStationBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.EngineeringStationBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.FishFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.FlywheelControllerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.FlywheelStorageBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.FruitFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.GateProxyBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.HandCrankedGeneratorBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.MailboxBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.ProtectionFlagBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.QuarryBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.ResearchStationBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.StructureContainerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.SurvivalStructureBuilderBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.TorqueGeneratorBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.TorqueTransportBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.TreeFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseControlBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseCraftingStationBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseInterfaceBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStockLinkerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStockViewerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStorageBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WindmillBladeBlockEntity;

public final class AWCoreBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ancientwarfare");
   public static final RegistryObject<BlockEntityType<MailboxBlockEntity>> MAILBOX = REGISTER.register(
      "mailbox", () -> Builder.m_155273_(MailboxBlockEntity::new, new Block[]{(Block)AWCoreBlocks.MAILBOX.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<EngineeringStationBlockEntity>> ENGINEERING_STATION = REGISTER.register(
      "engineering_station",
      () -> Builder.m_155273_(EngineeringStationBlockEntity::new, new Block[]{(Block)AWCoreBlocks.ENGINEERING_STATION.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<ResearchStationBlockEntity>> RESEARCH_STATION = REGISTER.register(
      "research_station", () -> Builder.m_155273_(ResearchStationBlockEntity::new, new Block[]{(Block)AWCoreBlocks.RESEARCH_STATION.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<CropFarmBlockEntity>> CROP_FARM = REGISTER.register(
      "crop_farm", () -> Builder.m_155273_(CropFarmBlockEntity::new, new Block[]{(Block)AWCoreBlocks.CROP_FARM.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<AnimalFarmBlockEntity>> ANIMAL_FARM = REGISTER.register(
      "animal_farm", () -> Builder.m_155273_(AnimalFarmBlockEntity::new, new Block[]{(Block)AWCoreBlocks.ANIMAL_FARM.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TreeFarmBlockEntity>> TREE_FARM = REGISTER.register(
      "tree_farm", () -> Builder.m_155273_(TreeFarmBlockEntity::new, new Block[]{(Block)AWCoreBlocks.TREE_FARM.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<FishFarmBlockEntity>> FISH_FARM = REGISTER.register(
      "fish_farm", () -> Builder.m_155273_(FishFarmBlockEntity::new, new Block[]{(Block)AWCoreBlocks.FISH_FARM.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<FruitFarmBlockEntity>> FRUIT_FARM = REGISTER.register(
      "fruit_farm", () -> Builder.m_155273_(FruitFarmBlockEntity::new, new Block[]{(Block)AWCoreBlocks.FRUIT_FARM.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<QuarryBlockEntity>> QUARRY = REGISTER.register(
      "quarry", () -> Builder.m_155273_(QuarryBlockEntity::new, new Block[]{(Block)AWCoreBlocks.QUARRY.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<WarehouseControlBlockEntity>> WAREHOUSE_CONTROL = REGISTER.register(
      "warehouse_control", () -> Builder.m_155273_(WarehouseControlBlockEntity::new, new Block[]{(Block)AWCoreBlocks.WAREHOUSE_CONTROL.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<WarehouseStorageBlockEntity>> WAREHOUSE_STORAGE = REGISTER.register(
      "warehouse_storage",
      () -> Builder.m_155273_(
               WarehouseStorageBlockEntity::new,
               new Block[]{
                  (Block)AWCoreBlocks.WAREHOUSE_STORAGE_SMALL.get(),
                  (Block)AWCoreBlocks.WAREHOUSE_STORAGE_MEDIUM.get(),
                  (Block)AWCoreBlocks.WAREHOUSE_STORAGE_LARGE.get()
               }
            )
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<WarehouseInterfaceBlockEntity>> WAREHOUSE_INTERFACE = REGISTER.register(
      "warehouse_interface",
      () -> Builder.m_155273_(WarehouseInterfaceBlockEntity::new, new Block[]{(Block)AWCoreBlocks.WAREHOUSE_INTERFACE.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<WarehouseCraftingStationBlockEntity>> WAREHOUSE_CRAFTING = REGISTER.register(
      "warehouse_crafting",
      () -> Builder.m_155273_(WarehouseCraftingStationBlockEntity::new, new Block[]{(Block)AWCoreBlocks.WAREHOUSE_CRAFTING.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<AutoCraftingBlockEntity>> AUTO_CRAFTING = REGISTER.register(
      "auto_crafting", () -> Builder.m_155273_(AutoCraftingBlockEntity::new, new Block[]{(Block)AWCoreBlocks.AUTO_CRAFTING.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<WarehouseStockViewerBlockEntity>> WAREHOUSE_STOCK_VIEWER = REGISTER.register(
      "warehouse_stock_viewer",
      () -> Builder.m_155273_(WarehouseStockViewerBlockEntity::new, new Block[]{(Block)AWCoreBlocks.WAREHOUSE_STOCK_VIEWER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<WarehouseStockLinkerBlockEntity>> WAREHOUSE_STOCK_LINKER = REGISTER.register(
      "warehouse_stock_linker",
      () -> Builder.m_155273_(WarehouseStockLinkerBlockEntity::new, new Block[]{(Block)AWCoreBlocks.WAREHOUSE_STOCK_LINKER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TorqueTransportBlockEntity>> TORQUE_TRANSPORT = REGISTER.register(
      "torque_transport",
      () -> Builder.m_155273_(
               TorqueTransportBlockEntity::new,
               new Block[]{
                  (Block)AWCoreBlocks.TORQUE_SHAFT_LIGHT.get(),
                  (Block)AWCoreBlocks.TORQUE_SHAFT_MEDIUM.get(),
                  (Block)AWCoreBlocks.TORQUE_SHAFT_HEAVY.get(),
                  (Block)AWCoreBlocks.TORQUE_JUNCTION_LIGHT.get(),
                  (Block)AWCoreBlocks.TORQUE_JUNCTION_MEDIUM.get(),
                  (Block)AWCoreBlocks.TORQUE_JUNCTION_HEAVY.get(),
                  (Block)AWCoreBlocks.TORQUE_DISTRIBUTOR_LIGHT.get(),
                  (Block)AWCoreBlocks.TORQUE_DISTRIBUTOR_MEDIUM.get(),
                  (Block)AWCoreBlocks.TORQUE_DISTRIBUTOR_HEAVY.get()
               }
            )
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<HandCrankedGeneratorBlockEntity>> HAND_CRANKED_GENERATOR = REGISTER.register(
      "hand_cranked_generator",
      () -> Builder.m_155273_(HandCrankedGeneratorBlockEntity::new, new Block[]{(Block)AWCoreBlocks.HAND_CRANKED_GENERATOR.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TorqueGeneratorBlockEntity>> TORQUE_GENERATOR = REGISTER.register(
      "torque_generator",
      () -> Builder.m_155273_(
               TorqueGeneratorBlockEntity::new,
               new Block[]{
                  (Block)AWCoreBlocks.STIRLING_GENERATOR.get(), (Block)AWCoreBlocks.WATERWHEEL_GENERATOR.get(), (Block)AWCoreBlocks.WINDMILL_GENERATOR.get()
               }
            )
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<WindmillBladeBlockEntity>> WINDMILL_BLADE = REGISTER.register(
      "windmill_blade", () -> Builder.m_155273_(WindmillBladeBlockEntity::new, new Block[]{(Block)AWCoreBlocks.WINDMILL_BLADE_RUNNING.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<ChunkLoaderBlockEntity>> CHUNK_LOADER_SIMPLE = REGISTER.register(
      "chunk_loader_simple", () -> Builder.m_155273_(ChunkLoaderBlockEntity::new, new Block[]{(Block)AWCoreBlocks.CHUNK_LOADER_SIMPLE.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<TownHallBlockEntity>> TOWN_HALL = REGISTER.register(
      "town_hall", () -> Builder.m_155273_(TownHallBlockEntity::new, new Block[]{(Block)AWCoreBlocks.TOWN_HALL.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<FlywheelControllerBlockEntity>> FLYWHEEL_CONTROLLER = REGISTER.register(
      "flywheel_controller",
      () -> Builder.m_155273_(
               FlywheelControllerBlockEntity::new,
               new Block[]{
                  (Block)AWCoreBlocks.FLYWHEEL_CONTROLLER_LIGHT.get(),
                  (Block)AWCoreBlocks.FLYWHEEL_CONTROLLER_MEDIUM.get(),
                  (Block)AWCoreBlocks.FLYWHEEL_CONTROLLER_HEAVY.get()
               }
            )
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<FlywheelStorageBlockEntity>> FLYWHEEL_STORAGE = REGISTER.register(
      "flywheel_storage",
      () -> Builder.m_155273_(
               FlywheelStorageBlockEntity::new,
               new Block[]{
                  (Block)AWCoreBlocks.FLYWHEEL_STORAGE_LIGHT.get(),
                  (Block)AWCoreBlocks.FLYWHEEL_STORAGE_MEDIUM.get(),
                  (Block)AWCoreBlocks.FLYWHEEL_STORAGE_HEAVY.get()
               }
            )
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<StructureContainerBlockEntity>> STRUCTURE_CONTAINER = REGISTER.register(
      "structure_container",
      () -> Builder.m_155273_(
               StructureContainerBlockEntity::new,
               new Block[]{
                  (Block)AWCoreBlocks.LOOT_BASKET.get(),
                  (Block)AWCoreBlocks.URN.get(),
                  (Block)AWCoreBlocks.WOODEN_COFFIN.get(),
                  (Block)AWCoreBlocks.STONE_COFFIN.get()
               }
            )
            .m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<AdvancedLootChestBlockEntity>> ADVANCED_LOOT_CHEST = REGISTER.register(
      "advanced_loot_chest",
      () -> Builder.m_155273_(AdvancedLootChestBlockEntity::new, new Block[]{(Block)AWCoreBlocks.ADVANCED_LOOT_CHEST.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<AdvancedSpawnerBlockEntity>> ADVANCED_SPAWNER = REGISTER.register(
      "advanced_spawner", () -> Builder.m_155273_(AdvancedSpawnerBlockEntity::new, new Block[]{(Block)AWCoreBlocks.ADVANCED_SPAWNER.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<ProtectionFlagBlockEntity>> PROTECTION_FLAG = REGISTER.register(
      "protection_flag", () -> Builder.m_155273_(ProtectionFlagBlockEntity::new, new Block[]{(Block)AWCoreBlocks.PROTECTION_FLAG.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<GateProxyBlockEntity>> GATE_PROXY = REGISTER.register(
      "gate_proxy", () -> Builder.m_155273_(GateProxyBlockEntity::new, new Block[]{(Block)AWCoreBlocks.GATE_PROXY.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<DraftingStationBlockEntity>> DRAFTING_STATION = REGISTER.register(
      "drafting_station", () -> Builder.m_155273_(DraftingStationBlockEntity::new, new Block[]{(Block)AWCoreBlocks.DRAFTING_STATION.get()}).m_58966_(null)
   );
   public static final RegistryObject<BlockEntityType<SurvivalStructureBuilderBlockEntity>> STRUCTURE_BUILDER_TICKED = REGISTER.register(
      "structure_builder_ticked",
      () -> Builder.m_155273_(SurvivalStructureBuilderBlockEntity::new, new Block[]{(Block)AWCoreBlocks.STRUCTURE_BUILDER_TICKED.get()}).m_58966_(null)
   );

   private AWCoreBlockEntities() {
   }
}
