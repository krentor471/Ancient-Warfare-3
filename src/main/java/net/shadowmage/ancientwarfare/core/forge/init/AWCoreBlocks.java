package net.shadowmage.ancientwarfare.core.forge.init;

import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.block.AdvancedLootChestBlock;
import net.shadowmage.ancientwarfare.core.block.AdvancedSpawnerBlock;
import net.shadowmage.ancientwarfare.core.block.AutomationWorksiteBlock;
import net.shadowmage.ancientwarfare.core.block.AwtTownMarkerBlock;
import net.shadowmage.ancientwarfare.core.block.ChunkLoaderBlock;
import net.shadowmage.ancientwarfare.core.block.DraftingStationBlock;
import net.shadowmage.ancientwarfare.core.block.EngineeringStationBlock;
import net.shadowmage.ancientwarfare.core.block.FlywheelControllerBlock;
import net.shadowmage.ancientwarfare.core.block.FlywheelStorageBlock;
import net.shadowmage.ancientwarfare.core.block.GateProxyBlock;
import net.shadowmage.ancientwarfare.core.block.HandCrankedGeneratorBlock;
import net.shadowmage.ancientwarfare.core.block.MailboxBlock;
import net.shadowmage.ancientwarfare.core.block.ProtectionFlagBlock;
import net.shadowmage.ancientwarfare.core.block.ResearchStationBlock;
import net.shadowmage.ancientwarfare.core.block.StaticGateBlock;
import net.shadowmage.ancientwarfare.core.block.StructureContainerBlock;
import net.shadowmage.ancientwarfare.core.block.StructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.StructureScannerBlock;
import net.shadowmage.ancientwarfare.core.block.SurvivalStructureBuilderBlock;
import net.shadowmage.ancientwarfare.core.block.TorqueGeneratorBlock;
import net.shadowmage.ancientwarfare.core.block.TorqueTransportBlock;
import net.shadowmage.ancientwarfare.core.block.TownHallBlock;
import net.shadowmage.ancientwarfare.core.block.UndeadCemeterySkullMarkerBlock;
import net.shadowmage.ancientwarfare.core.block.WarehouseBlock;
import net.shadowmage.ancientwarfare.core.block.WarehouseStockLinkerBlock;
import net.shadowmage.ancientwarfare.core.block.WindmillBladeBlock;
import net.shadowmage.ancientwarfare.core.block.WindmillBladeRunningBlock;
import net.shadowmage.ancientwarfare.core.common.registry.StructureDecorBlockFactory;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueGeneratorKind;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTransportKind;
import net.shadowmage.ancientwarfare.core.forge.item.ForgeAdvancedLootChestBlockItem;
import net.shadowmage.ancientwarfare.core.forge.item.ForgeProtectionFlagBlockItem;
import net.shadowmage.ancientwarfare.core.forge.item.ForgeRestoredAutomationBlockItem;
import net.shadowmage.ancientwarfare.core.forge.item.ForgeRestoredCoffinBlockItem;
import net.shadowmage.ancientwarfare.core.item.CoinStackBlockItem;
import net.shadowmage.ancientwarfare.core.item.SurvivalStructureBuilderItem;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplateBlockItem;
import net.shadowmage.ancientwarfare.core.structure.AwtTownBlockItem;
import net.shadowmage.ancientwarfare.core.tile.AnimalFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.AutoCraftingBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.CropFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.FishFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.FruitFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.QuarryBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.StructureContainerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.TreeFarmBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseControlBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseCraftingStationBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseInterfaceBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStockLinkerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStockViewerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStorageBlockEntity;

public final class AWCoreBlocks {
   public static final DeferredRegister<Block> REGISTER = DeferredRegister.create(ForgeRegistries.BLOCKS, "ancientwarfare");
   public static final RegistryObject<Block> MAILBOX = REGISTER.register(
      "mailbox", () -> new MailboxBlock(Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).requiresCorrectToolForDrops())
   );
   public static final RegistryObject<Block> ENGINEERING_STATION = REGISTER.register(
      "engineering_station", () -> new EngineeringStationBlock(stationProperties())
   );
   public static final RegistryObject<Block> RESEARCH_STATION = REGISTER.register("research_station", () -> new ResearchStationBlock(stationProperties()));
   public static final RegistryObject<Block> CROP_FARM = REGISTER.register(
      "crop_farm", () -> new AutomationWorksiteBlock(worksiteProperties(), CropFarmBlockEntity::new)
   );
   public static final RegistryObject<Block> ANIMAL_FARM = REGISTER.register(
      "animal_farm", () -> new AutomationWorksiteBlock(worksiteProperties(), AnimalFarmBlockEntity::new)
   );
   public static final RegistryObject<Block> TREE_FARM = REGISTER.register(
      "tree_farm", () -> new AutomationWorksiteBlock(worksiteProperties(), TreeFarmBlockEntity::new)
   );
   public static final RegistryObject<Block> FISH_FARM = REGISTER.register(
      "fish_farm", () -> new AutomationWorksiteBlock(worksiteProperties(), FishFarmBlockEntity::new)
   );
   public static final RegistryObject<Block> FRUIT_FARM = REGISTER.register(
      "fruit_farm", () -> new AutomationWorksiteBlock(worksiteProperties(), FruitFarmBlockEntity::new)
   );
   public static final RegistryObject<Block> QUARRY = REGISTER.register(
      "quarry", () -> new AutomationWorksiteBlock(worksiteProperties(), QuarryBlockEntity::new)
   );
   public static final RegistryObject<Block> WAREHOUSE_CONTROL = REGISTER.register(
      "warehouse_control", () -> new WarehouseBlock(worksiteProperties(), WarehouseControlBlockEntity::new)
   );
   public static final RegistryObject<Block> WAREHOUSE_STORAGE_SMALL = REGISTER.register(
      "warehouse_storage_small", () -> new WarehouseBlock(worksiteProperties(), (pos, state) -> new WarehouseStorageBlockEntity(pos, state, 27))
   );
   public static final RegistryObject<Block> WAREHOUSE_STORAGE_MEDIUM = REGISTER.register(
      "warehouse_storage_medium", () -> new WarehouseBlock(worksiteProperties(), (pos, state) -> new WarehouseStorageBlockEntity(pos, state, 45))
   );
   public static final RegistryObject<Block> WAREHOUSE_STORAGE_LARGE = REGISTER.register(
      "warehouse_storage_large", () -> new WarehouseBlock(worksiteProperties(), (pos, state) -> new WarehouseStorageBlockEntity(pos, state, 54))
   );
   public static final RegistryObject<Block> WAREHOUSE_INTERFACE = REGISTER.register(
      "warehouse_interface", () -> new WarehouseBlock(worksiteProperties(), WarehouseInterfaceBlockEntity::new)
   );
   public static final RegistryObject<Block> WAREHOUSE_CRAFTING = REGISTER.register(
      "warehouse_crafting", () -> new WarehouseBlock(worksiteProperties(), WarehouseCraftingStationBlockEntity::new)
   );
   public static final RegistryObject<Block> AUTO_CRAFTING = REGISTER.register(
      "auto_crafting", () -> new WarehouseBlock(worksiteProperties(), AutoCraftingBlockEntity::new)
   );
   public static final RegistryObject<Block> WAREHOUSE_STOCK_VIEWER = REGISTER.register(
      "warehouse_stock_viewer", () -> new WarehouseBlock(worksiteProperties(), WarehouseStockViewerBlockEntity::new)
   );
   public static final RegistryObject<Block> WAREHOUSE_STOCK_LINKER = REGISTER.register(
      "warehouse_stock_linker", () -> new WarehouseStockLinkerBlock(worksiteProperties(), WarehouseStockLinkerBlockEntity::new)
   );
   public static final RegistryObject<Block> TORQUE_SHAFT_LIGHT = REGISTER.register(
      "torque_shaft_light", () -> new TorqueTransportBlock(torqueProperties(), TorqueTransportKind.SHAFT, TorqueTier.LIGHT)
   );
   public static final RegistryObject<Block> TORQUE_SHAFT_MEDIUM = REGISTER.register(
      "torque_shaft_medium", () -> new TorqueTransportBlock(torqueProperties(), TorqueTransportKind.SHAFT, TorqueTier.MEDIUM)
   );
   public static final RegistryObject<Block> TORQUE_SHAFT_HEAVY = REGISTER.register(
      "torque_shaft_heavy", () -> new TorqueTransportBlock(torqueProperties(), TorqueTransportKind.SHAFT, TorqueTier.HEAVY)
   );
   public static final RegistryObject<Block> TORQUE_JUNCTION_LIGHT = REGISTER.register(
      "torque_junction_light", () -> new TorqueTransportBlock(torqueProperties(), TorqueTransportKind.JUNCTION, TorqueTier.LIGHT)
   );
   public static final RegistryObject<Block> TORQUE_JUNCTION_MEDIUM = REGISTER.register(
      "torque_junction_medium", () -> new TorqueTransportBlock(torqueProperties(), TorqueTransportKind.JUNCTION, TorqueTier.MEDIUM)
   );
   public static final RegistryObject<Block> TORQUE_JUNCTION_HEAVY = REGISTER.register(
      "torque_junction_heavy", () -> new TorqueTransportBlock(torqueProperties(), TorqueTransportKind.JUNCTION, TorqueTier.HEAVY)
   );
   public static final RegistryObject<Block> TORQUE_DISTRIBUTOR_LIGHT = REGISTER.register(
      "torque_distributor_light", () -> new TorqueTransportBlock(torqueProperties(), TorqueTransportKind.DISTRIBUTOR, TorqueTier.LIGHT)
   );
   public static final RegistryObject<Block> TORQUE_DISTRIBUTOR_MEDIUM = REGISTER.register(
      "torque_distributor_medium", () -> new TorqueTransportBlock(torqueProperties(), TorqueTransportKind.DISTRIBUTOR, TorqueTier.MEDIUM)
   );
   public static final RegistryObject<Block> TORQUE_DISTRIBUTOR_HEAVY = REGISTER.register(
      "torque_distributor_heavy", () -> new TorqueTransportBlock(torqueProperties(), TorqueTransportKind.DISTRIBUTOR, TorqueTier.HEAVY)
   );
   public static final RegistryObject<Block> HAND_CRANKED_GENERATOR = REGISTER.register(
      "hand_cranked_generator", () -> new HandCrankedGeneratorBlock(torqueProperties())
   );
   public static final RegistryObject<Block> STIRLING_GENERATOR = REGISTER.register(
      "stirling_generator", () -> new TorqueGeneratorBlock(torqueProperties(), TorqueGeneratorKind.STIRLING)
   );
   public static final RegistryObject<Block> WATERWHEEL_GENERATOR = REGISTER.register(
      "waterwheel_generator", () -> new TorqueGeneratorBlock(torqueProperties(), TorqueGeneratorKind.WATERWHEEL)
   );
   public static final RegistryObject<Block> WINDMILL_GENERATOR = REGISTER.register(
      "windmill_generator", () -> new TorqueGeneratorBlock(torqueProperties(), TorqueGeneratorKind.WINDMILL)
   );
   public static final RegistryObject<Block> WINDMILL_BLADE = REGISTER.register("windmill_blade", () -> new WindmillBladeBlock(torqueProperties()));
   public static final RegistryObject<Block> WINDMILL_BLADE_RUNNING = REGISTER.register(
      "windmill_blade_running", () -> new WindmillBladeRunningBlock(torqueProperties().noLootTable())
   );
   public static final RegistryObject<Block> CHUNK_LOADER_SIMPLE = REGISTER.register("chunk_loader_simple", () -> new ChunkLoaderBlock(worksiteProperties()));
   public static final RegistryObject<Block> TOWN_HALL = REGISTER.register("town_hall", () -> new TownHallBlock(stationProperties()));
   public static final RegistryObject<Block> FLYWHEEL_CONTROLLER_LIGHT = REGISTER.register(
      "flywheel_controller_light", () -> new FlywheelControllerBlock(torqueProperties(), TorqueTier.LIGHT)
   );
   public static final RegistryObject<Block> FLYWHEEL_CONTROLLER_MEDIUM = REGISTER.register(
      "flywheel_controller_medium", () -> new FlywheelControllerBlock(torqueProperties(), TorqueTier.MEDIUM)
   );
   public static final RegistryObject<Block> FLYWHEEL_CONTROLLER_HEAVY = REGISTER.register(
      "flywheel_controller_heavy", () -> new FlywheelControllerBlock(torqueProperties(), TorqueTier.HEAVY)
   );
   public static final RegistryObject<Block> FLYWHEEL_STORAGE_LIGHT = REGISTER.register(
      "flywheel_storage_light", () -> new FlywheelStorageBlock(torqueProperties(), TorqueTier.LIGHT)
   );
   public static final RegistryObject<Block> FLYWHEEL_STORAGE_MEDIUM = REGISTER.register(
      "flywheel_storage_medium", () -> new FlywheelStorageBlock(torqueProperties(), TorqueTier.MEDIUM)
   );
   public static final RegistryObject<Block> FLYWHEEL_STORAGE_HEAVY = REGISTER.register(
      "flywheel_storage_heavy", () -> new FlywheelStorageBlock(torqueProperties(), TorqueTier.HEAVY)
   );
   public static final RegistryObject<Block> SCISSOR_SEAT = decor("scissor_seat");
   public static final RegistryObject<Block> TRIBAL_STOOL = decor("tribal_stool");
   public static final RegistryObject<Block> WOODEN_THRONE = decor("wooden_throne");
   public static final RegistryObject<Block> GOLDEN_IDOL = decor("golden_idol");
   public static final RegistryObject<Block> GOLDEN_THRONE = decor("golden_throne");
   public static final RegistryObject<Block> ORC_TOTEM_1 = decor("orc_totem_1");
   public static final RegistryObject<Block> ORC_TOTEM_2 = decor("orc_totem_2");
   public static final RegistryObject<Block> ORC_TOTEM_2_LIT = decor("orc_totem_2_lit");
   public static final RegistryObject<Block> GOBLIN_TOTEM_1 = decor("goblin_totem_1");
   public static final RegistryObject<Block> GOBLIN_TOTEM_2 = decor("goblin_totem_2");
   public static final RegistryObject<Block> GOBLIN_TOTEM_2_LIT = decor("goblin_totem_2_lit");
   public static final RegistryObject<Block> IRON_CAGE = decor("iron_cage");
   public static final RegistryObject<Block> GIBBET = decor("gibbet");
   public static final RegistryObject<Block> WOODEN_POST = decor("wooden_post");
   public static final RegistryObject<Block> STOOL = decor("stool");
   public static final RegistryObject<Block> TABLE = decor("table");
   public static final RegistryObject<Block> CHAIR = decor("chair");
   public static final RegistryObject<Block> BENCH = decor("bench");
   public static final RegistryObject<Block> TRIBAL_CHAIR = decor("tribal_chair");
   public static final RegistryObject<Block> ALTAR_LECTERN = decor("altar_lectern");
   public static final RegistryObject<Block> ALTAR_SUN = decor("altar_sun");
   public static final RegistryObject<Block> FIRE_PIT = decor("fire_pit");
   public static final RegistryObject<Block> FIRE_PIT_LOG = decor("fire_pit_log");
   public static final RegistryObject<Block> FIRE_PIT_SQUARE = decor("fire_pit_square");
   public static final RegistryObject<Block> BRAZIER_FLAME = decor("brazier_flame");
   public static final RegistryObject<Block> BRAZIER_EMBER = decor("brazier_ember");
   public static final RegistryObject<Block> COIN_STACK_COPPER = decor("coin_stack_copper");
   public static final RegistryObject<Block> COIN_STACK_SILVER = decor("coin_stack_silver");
   public static final RegistryObject<Block> COIN_STACK_GOLD = decor("coin_stack_gold");
   public static final RegistryObject<Block> COIN_STACK_ANCIENT = decor("coin_stack_ancient");
   public static final RegistryObject<Block> STRETCHING_RACK = decor("stretching_rack");
   public static final RegistryObject<Block> GRAVESTONE_1 = decor("gravestone_1");
   public static final RegistryObject<Block> GRAVESTONE_2 = decor("gravestone_2");
   public static final RegistryObject<Block> GRAVESTONE_3 = decor("gravestone_3");
   public static final RegistryObject<Block> GRAVESTONE_4 = decor("gravestone_4");
   public static final RegistryObject<Block> GRAVESTONE_5 = decor("gravestone_5");
   public static final RegistryObject<Block> GRAVESTONE_6 = decor("gravestone_6");
   public static final RegistryObject<Block> GRAVESTONE_7 = decor("gravestone_7");
   public static final RegistryObject<Block> GRAVESTONE_8 = decor("gravestone_8");
   public static final RegistryObject<Block> ALTAR_SHORT_CLOTH = decor("altar_short_cloth");
   public static final RegistryObject<Block> ALTAR_LONG_CLOTH = decor("altar_long_cloth");
   public static final RegistryObject<Block> ALTAR_CANDLE = decor("altar_candle");
   public static final RegistryObject<Block> STAKE = decor("stake");
   public static final RegistryObject<Block> TOTEM_PART_BASE = decor("totem_part_base");
   public static final RegistryObject<Block> TOTEM_PART_MID = decor("totem_part_mid");
   public static final RegistryObject<Block> TOTEM_PART_MID_ALT = decor("totem_part_mid_alt");
   public static final RegistryObject<Block> TOTEM_PART_TOP = decor("totem_part_top");
   public static final RegistryObject<Block> TOTEM_PART_WINGS = decor("totem_part_wings");
   public static final RegistryObject<Block> TOTEM_PART_IRMINSUL_BASE = decor("totem_part_irminsul_base");
   public static final RegistryObject<Block> TOTEM_PART_IRMINSUL_MID = decor("totem_part_irminsul_mid");
   public static final RegistryObject<Block> TOTEM_PART_IRMINSUL_TOP = decor("totem_part_irminsul_top");
   public static final RegistryObject<Block> TOTEM_PART_IRMINSUL_SIDE = decor("totem_part_irminsul_side");
   public static final RegistryObject<Block> TOTEM_PART_ELEPHANT_BODY = decor("totem_part_elephant_body");
   public static final RegistryObject<Block> TOTEM_PART_ELEPHANT_LEFT = decor("totem_part_elephant_left");
   public static final RegistryObject<Block> TOTEM_PART_ELEPHANT_RIGHT = decor("totem_part_elephant_right");
   public static final RegistryObject<Block> XOLTEC_IDOL_BODY_LEFT = decor("xoltec_idol_body_left");
   public static final RegistryObject<Block> XOLTEC_IDOL_BODY_RIGHT = decor("xoltec_idol_body_right");
   public static final RegistryObject<Block> XOLTEC_IDOL_HEAD_LEFT = decor("xoltec_idol_head_left");
   public static final RegistryObject<Block> XOLTEC_IDOL_HEAD_RIGHT = decor("xoltec_idol_head_right");
   public static final RegistryObject<Block> LOOT_BASKET = REGISTER.register(
      "loot_basket",
      () -> new StructureContainerBlock(
            structureWoodProperties(), Block.box(1.0, 0.0, 1.0, 15.0, 10.0, 15.0), (pos, state) -> new StructureContainerBlockEntity(pos, state, 27)
         )
   );
   public static final RegistryObject<Block> URN = REGISTER.register(
      "urn",
      () -> new StructureContainerBlock(
            Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(0.4F).sound(SoundType.DECORATED_POT).noOcclusion(),
            Block.box(2.0, 0.0, 2.0, 14.0, 15.0, 14.0),
            (pos, state) -> new StructureContainerBlockEntity(pos, state, 27)
         )
   );
   public static final RegistryObject<Block> WOODEN_COFFIN = REGISTER.register(
      "wooden_coffin",
      () -> new StructureContainerBlock(
            structureWoodProperties(), Block.box(1.0, 0.0, 2.0, 15.0, 8.0, 14.0), (pos, state) -> new StructureContainerBlockEntity(pos, state, 27)
         )
   );
   public static final RegistryObject<Block> STONE_COFFIN = REGISTER.register(
      "stone_coffin",
      () -> new StructureContainerBlock(
            structureStoneProperties().noOcclusion(),
            Block.box(1.0, 0.0, 2.0, 15.0, 8.0, 14.0),
            (pos, state) -> new StructureContainerBlockEntity(pos, state, 27)
         )
   );
   public static final RegistryObject<Block> ADVANCED_LOOT_CHEST = REGISTER.register(
      "advanced_loot_chest",
      () -> new AdvancedLootChestBlock(
            structureWoodProperties(), () -> (BlockEntityType<? extends ChestBlockEntity>)AWCoreBlockEntities.ADVANCED_LOOT_CHEST.get()
         )
   );
   public static final RegistryObject<Block> ADVANCED_SPAWNER = REGISTER.register(
      "advanced_spawner", () -> new AdvancedSpawnerBlock(structureStoneProperties().noOcclusion())
   );
   public static final RegistryObject<Block> PROTECTION_FLAG = REGISTER.register(
      "protection_flag", () -> new ProtectionFlagBlock(structureWoodProperties().lightLevel(state -> 13).strength(2.0F, 3600000.0F))
   );
   public static final RegistryObject<Block> GATE_VERTICAL_WOODEN = REGISTER.register(
      "gate_vertical_wooden", () -> new StaticGateBlock(structureWoodProperties().strength(3.0F), false)
   );
   public static final RegistryObject<Block> GATE_VERTICAL_IRON = REGISTER.register(
      "gate_vertical_iron", () -> new StaticGateBlock(structureMetalProperties().strength(5.0F), true)
   );
   public static final RegistryObject<Block> GATE_SINGLE_WOOD = REGISTER.register(
      "gate_single_wood", () -> new StaticGateBlock(structureWoodProperties().strength(3.0F), false)
   );
   public static final RegistryObject<Block> GATE_SINGLE_IRON = REGISTER.register(
      "gate_single_iron", () -> new StaticGateBlock(structureMetalProperties().strength(5.0F), true)
   );
   public static final RegistryObject<Block> GATE_DOUBLE_WOOD = REGISTER.register(
      "gate_double_wood", () -> new StaticGateBlock(structureWoodProperties().strength(3.0F), false)
   );
   public static final RegistryObject<Block> GATE_DOUBLE_IRON = REGISTER.register(
      "gate_double_iron", () -> new StaticGateBlock(structureMetalProperties().strength(5.0F), true)
   );
   public static final RegistryObject<Block> GATE_DRAWBRIDGE = REGISTER.register(
      "gate_drawbridge", () -> new StaticGateBlock(structureWoodProperties().strength(3.0F), false)
   );
   public static final RegistryObject<Block> GATE_PROXY = REGISTER.register(
      "gate_proxy", () -> new GateProxyBlock(Properties.of().mapColor(MapColor.NONE).strength(-1.0F, 3600000.0F).noOcclusion().noLootTable())
   );
   public static final RegistryObject<Block> UNDEAD_CEMETERY_FILL_SKULL_1 = REGISTER.register(
      "undead_cemetery_fill_skull_1", () -> new UndeadCemeterySkullMarkerBlock(structureStoneProperties().noOcclusion())
   );
   public static final RegistryObject<Block> AWT_TOWN_MARKER = REGISTER.register(
      "awt_town_marker", () -> new AwtTownMarkerBlock(structureStoneProperties().noOcclusion())
   );
   public static final RegistryObject<Block> STRUCTURE_SCANNER_BLOCK = REGISTER.register(
      "structure_scanner_block", () -> new StructureScannerBlock(structureWoodProperties())
   );
   public static final RegistryObject<Block> DRAFTING_STATION = REGISTER.register("drafting_station", () -> new DraftingStationBlock(stationProperties()));
   public static final RegistryObject<Block> STRUCTURE_BUILDER_TICKED = REGISTER.register(
      "structure_builder_ticked", () -> new SurvivalStructureBuilderBlock(structureStoneProperties())
   );

   private AWCoreBlocks() {
   }

   private static Properties stationProperties() {
      return Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).requiresCorrectToolForDrops().noOcclusion();
   }

   private static Properties worksiteProperties() {
      return Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).requiresCorrectToolForDrops();
   }

   private static Properties torqueProperties() {
      return Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
   }

   private static Properties structureWoodProperties() {
      return Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion();
   }

   private static Properties structureMetalProperties() {
      return Properties.of().mapColor(MapColor.GOLD).strength(2.0F).sound(SoundType.METAL).noOcclusion();
   }

   private static Properties structureStoneProperties() {
      return Properties.of().mapColor(MapColor.STONE).strength(2.0F).sound(SoundType.STONE);
   }

   private static Map<Direction, VoxelShape> fullBlockShapes() {
      VoxelShape shape = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
      return Map.of(Direction.NORTH, shape, Direction.SOUTH, shape, Direction.EAST, shape, Direction.WEST, shape);
   }

   private static VoxelShape fullBlockShape() {
      return Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   }

   private static Map<Direction, VoxelShape> totemShapes() {
      VoxelShape shape = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
      return Map.of(Direction.NORTH, shape, Direction.SOUTH, shape, Direction.EAST, shape, Direction.WEST, shape);
   }

   private static Map<Direction, VoxelShape> tribalStoolShapes() {
      VoxelShape zAxis = Block.box(0.0, 0.0, 1.0, 16.0, 10.0, 15.0);
      VoxelShape xAxis = Block.box(1.0, 0.0, 0.0, 15.0, 10.0, 16.0);
      return Map.of(Direction.NORTH, zAxis, Direction.SOUTH, zAxis, Direction.EAST, xAxis, Direction.WEST, xAxis);
   }

   private static Map<Direction, VoxelShape> scissorSeatShapes() {
      return Map.of(
         Direction.NORTH,
         StructureDecorBlock.shape(
            Block.box(3.0, 0.0, 3.0, 13.0, 8.0, 12.0),
            Block.box(1.5, 11.0, 12.5, 14.5, 15.5, 13.5),
            Block.box(2.0, 8.0, 2.0, 3.0, 11.0, 13.0),
            Block.box(13.0, 8.0, 2.0, 14.0, 11.0, 13.0)
         ),
         Direction.SOUTH,
         StructureDecorBlock.shape(
            Block.box(3.0, 0.0, 4.0, 13.0, 8.0, 13.0),
            Block.box(1.5, 11.0, 2.5, 14.5, 15.5, 3.5),
            Block.box(2.0, 8.0, 3.0, 3.0, 11.0, 14.0),
            Block.box(13.0, 8.0, 3.0, 14.0, 11.0, 14.0)
         ),
         Direction.EAST,
         StructureDecorBlock.shape(
            Block.box(4.0, 0.0, 3.0, 13.0, 8.0, 13.0),
            Block.box(2.5, 11.0, 1.5, 3.5, 15.5, 14.5),
            Block.box(3.0, 8.0, 2.0, 14.0, 11.0, 3.0),
            Block.box(3.0, 8.0, 13.0, 14.0, 11.0, 14.0)
         ),
         Direction.WEST,
         StructureDecorBlock.shape(
            Block.box(3.0, 0.0, 3.0, 12.0, 8.0, 13.0),
            Block.box(12.5, 11.0, 1.5, 13.5, 15.5, 14.5),
            Block.box(2.0, 8.0, 2.0, 13.0, 11.0, 3.0),
            Block.box(2.0, 8.0, 13.0, 13.0, 11.0, 14.0)
         )
      );
   }

   private static Map<Direction, VoxelShape> stoolShapes() {
      VoxelShape shape = Block.box(3.0, 0.0, 3.0, 13.0, 9.0, 13.0);
      return Map.of(Direction.NORTH, shape, Direction.SOUTH, shape, Direction.EAST, shape, Direction.WEST, shape);
   }

   private static VoxelShape firePitShape() {
      return Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
   }

   private static VoxelShape brazierFlameShape() {
      return Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);
   }

   private static VoxelShape brazierEmberShape() {
      return Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);
   }

   private static VoxelShape coinStackShape() {
      return Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   }

   private static Map<Direction, VoxelShape> chairShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 15.0),
         Direction.SOUTH,
         Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 16.0),
         Direction.EAST,
         Block.box(1.0, 0.0, 1.0, 16.0, 16.0, 15.0),
         Direction.WEST,
         Block.box(0.0, 0.0, 1.0, 15.0, 16.0, 15.0)
      );
   }

   private static Map<Direction, VoxelShape> gravestoneShapes() {
      VoxelShape zAxis = Block.box(0.0, 0.0, 6.0, 16.0, 19.0, 10.0);
      VoxelShape xAxis = Block.box(6.0, 0.0, 0.0, 10.0, 19.0, 16.0);
      return Map.of(Direction.NORTH, zAxis, Direction.SOUTH, zAxis, Direction.EAST, xAxis, Direction.WEST, xAxis);
   }

   private static Map<Direction, VoxelShape> woodenThroneLowerShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 16.0),
         Direction.SOUTH,
         Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 15.0),
         Direction.EAST,
         Block.box(0.0, 0.0, 0.0, 15.0, 16.0, 16.0),
         Direction.WEST,
         Block.box(1.0, 0.0, 0.0, 16.0, 16.0, 16.0)
      );
   }

   private static Map<Direction, VoxelShape> woodenThroneUpperShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0),
         Direction.SOUTH,
         Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0),
         Direction.EAST,
         Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0),
         Direction.WEST,
         Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0)
      );
   }

   private static Map<Direction, VoxelShape> goldenThroneLowerShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 15.0),
         Direction.SOUTH,
         Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 15.0),
         Direction.EAST,
         Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 16.0),
         Direction.WEST,
         Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 16.0)
      );
   }

   private static Map<Direction, VoxelShape> goldenThroneUpperShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 15.0),
         Direction.SOUTH,
         Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 3.0),
         Direction.EAST,
         Block.box(1.0, 0.0, 0.0, 3.0, 16.0, 16.0),
         Direction.WEST,
         Block.box(13.0, 0.0, 0.0, 15.0, 16.0, 16.0)
      );
   }

   private static Map<Direction, VoxelShape> tribalChairLowerShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 15.0),
         Direction.SOUTH,
         Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 16.0),
         Direction.EAST,
         Block.box(1.0, 0.0, 1.0, 16.0, 16.0, 15.0),
         Direction.WEST,
         Block.box(0.0, 0.0, 1.0, 15.0, 16.0, 15.0)
      );
   }

   private static Map<Direction, VoxelShape> tribalChairUpperShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(1.0, 0.0, 12.0, 15.0, 14.0, 16.0),
         Direction.SOUTH,
         Block.box(1.0, 0.0, 0.0, 15.0, 14.0, 4.0),
         Direction.EAST,
         Block.box(0.0, 0.0, 1.0, 4.0, 14.0, 15.0),
         Direction.WEST,
         Block.box(12.0, 0.0, 1.0, 16.0, 14.0, 15.0)
      );
   }

   private static RegistryObject<Block> decor(String name) {
      return REGISTER.register(name, () -> StructureDecorBlockFactory.create(name));
   }

   private static void blockItem(String name, RegistryObject<Block> block) {
      AWCoreItems.REGISTER.register(name, () -> new BlockItem((Block)block.get(), new net.minecraft.world.item.Item.Properties()));
   }

   private static void coinStackItem(String name, RegistryObject<Block> block) {
      AWCoreItems.REGISTER.register(name, () -> new CoinStackBlockItem((Block)block.get(), new net.minecraft.world.item.Item.Properties()));
   }

   private static void restoredAutomationBlockItem(String name, RegistryObject<Block> block) {
      AWCoreItems.REGISTER.register(name, () -> new ForgeRestoredAutomationBlockItem((Block)block.get(), new net.minecraft.world.item.Item.Properties()));
   }

   static {
      blockItem("mailbox", MAILBOX);
      blockItem("engineering_station", ENGINEERING_STATION);
      blockItem("research_station", RESEARCH_STATION);
      blockItem("crop_farm", CROP_FARM);
      blockItem("animal_farm", ANIMAL_FARM);
      blockItem("tree_farm", TREE_FARM);
      blockItem("fish_farm", FISH_FARM);
      blockItem("fruit_farm", FRUIT_FARM);
      blockItem("quarry", QUARRY);
      blockItem("warehouse_control", WAREHOUSE_CONTROL);
      blockItem("warehouse_storage_small", WAREHOUSE_STORAGE_SMALL);
      blockItem("warehouse_storage_medium", WAREHOUSE_STORAGE_MEDIUM);
      blockItem("warehouse_storage_large", WAREHOUSE_STORAGE_LARGE);
      blockItem("warehouse_interface", WAREHOUSE_INTERFACE);
      blockItem("warehouse_crafting", WAREHOUSE_CRAFTING);
      blockItem("auto_crafting", AUTO_CRAFTING);
      blockItem("warehouse_stock_viewer", WAREHOUSE_STOCK_VIEWER);
      blockItem("warehouse_stock_linker", WAREHOUSE_STOCK_LINKER);
      blockItem("torque_shaft_light", TORQUE_SHAFT_LIGHT);
      blockItem("torque_shaft_medium", TORQUE_SHAFT_MEDIUM);
      blockItem("torque_shaft_heavy", TORQUE_SHAFT_HEAVY);
      blockItem("torque_junction_light", TORQUE_JUNCTION_LIGHT);
      blockItem("torque_junction_medium", TORQUE_JUNCTION_MEDIUM);
      blockItem("torque_junction_heavy", TORQUE_JUNCTION_HEAVY);
      blockItem("torque_distributor_light", TORQUE_DISTRIBUTOR_LIGHT);
      blockItem("torque_distributor_medium", TORQUE_DISTRIBUTOR_MEDIUM);
      blockItem("torque_distributor_heavy", TORQUE_DISTRIBUTOR_HEAVY);
      blockItem("hand_cranked_generator", HAND_CRANKED_GENERATOR);
      blockItem("stirling_generator", STIRLING_GENERATOR);
      restoredAutomationBlockItem("waterwheel_generator", WATERWHEEL_GENERATOR);
      blockItem("windmill_generator", WINDMILL_GENERATOR);
      blockItem("windmill_blade", WINDMILL_BLADE);
      blockItem("chunk_loader_simple", CHUNK_LOADER_SIMPLE);
      blockItem("town_hall", TOWN_HALL);
      restoredAutomationBlockItem("flywheel_controller_light", FLYWHEEL_CONTROLLER_LIGHT);
      restoredAutomationBlockItem("flywheel_controller_medium", FLYWHEEL_CONTROLLER_MEDIUM);
      restoredAutomationBlockItem("flywheel_controller_heavy", FLYWHEEL_CONTROLLER_HEAVY);
      restoredAutomationBlockItem("flywheel_storage_light", FLYWHEEL_STORAGE_LIGHT);
      restoredAutomationBlockItem("flywheel_storage_medium", FLYWHEEL_STORAGE_MEDIUM);
      restoredAutomationBlockItem("flywheel_storage_heavy", FLYWHEEL_STORAGE_HEAVY);
      blockItem("scissor_seat", SCISSOR_SEAT);
      blockItem("tribal_stool", TRIBAL_STOOL);
      blockItem("wooden_throne", WOODEN_THRONE);
      blockItem("golden_idol", GOLDEN_IDOL);
      blockItem("golden_throne", GOLDEN_THRONE);
      blockItem("orc_totem_1", ORC_TOTEM_1);
      blockItem("orc_totem_2", ORC_TOTEM_2);
      blockItem("orc_totem_2_lit", ORC_TOTEM_2_LIT);
      blockItem("goblin_totem_1", GOBLIN_TOTEM_1);
      blockItem("goblin_totem_2", GOBLIN_TOTEM_2);
      blockItem("goblin_totem_2_lit", GOBLIN_TOTEM_2_LIT);
      blockItem("iron_cage", IRON_CAGE);
      blockItem("gibbet", GIBBET);
      blockItem("wooden_post", WOODEN_POST);
      blockItem("stool", STOOL);
      blockItem("table", TABLE);
      blockItem("chair", CHAIR);
      blockItem("bench", BENCH);
      blockItem("tribal_chair", TRIBAL_CHAIR);
      blockItem("altar_lectern", ALTAR_LECTERN);
      blockItem("altar_sun", ALTAR_SUN);
      blockItem("fire_pit", FIRE_PIT);
      blockItem("fire_pit_log", FIRE_PIT_LOG);
      blockItem("fire_pit_square", FIRE_PIT_SQUARE);
      blockItem("brazier_flame", BRAZIER_FLAME);
      blockItem("brazier_ember", BRAZIER_EMBER);
      coinStackItem("coin_stack_copper", COIN_STACK_COPPER);
      coinStackItem("coin_stack_silver", COIN_STACK_SILVER);
      coinStackItem("coin_stack_gold", COIN_STACK_GOLD);
      coinStackItem("coin_stack_ancient", COIN_STACK_ANCIENT);
      blockItem("stretching_rack", STRETCHING_RACK);
      blockItem("gravestone_1", GRAVESTONE_1);
      blockItem("gravestone_2", GRAVESTONE_2);
      blockItem("gravestone_3", GRAVESTONE_3);
      blockItem("gravestone_4", GRAVESTONE_4);
      blockItem("gravestone_5", GRAVESTONE_5);
      blockItem("gravestone_6", GRAVESTONE_6);
      blockItem("gravestone_7", GRAVESTONE_7);
      blockItem("gravestone_8", GRAVESTONE_8);
      blockItem("altar_short_cloth", ALTAR_SHORT_CLOTH);
      blockItem("altar_long_cloth", ALTAR_LONG_CLOTH);
      blockItem("altar_candle", ALTAR_CANDLE);
      blockItem("stake", STAKE);
      blockItem("totem_part_base", TOTEM_PART_BASE);
      blockItem("totem_part_mid", TOTEM_PART_MID);
      blockItem("totem_part_mid_alt", TOTEM_PART_MID_ALT);
      blockItem("totem_part_top", TOTEM_PART_TOP);
      blockItem("totem_part_wings", TOTEM_PART_WINGS);
      blockItem("totem_part_irminsul_base", TOTEM_PART_IRMINSUL_BASE);
      blockItem("totem_part_irminsul_mid", TOTEM_PART_IRMINSUL_MID);
      blockItem("totem_part_irminsul_top", TOTEM_PART_IRMINSUL_TOP);
      blockItem("totem_part_irminsul_side", TOTEM_PART_IRMINSUL_SIDE);
      blockItem("totem_part_elephant_body", TOTEM_PART_ELEPHANT_BODY);
      blockItem("totem_part_elephant_left", TOTEM_PART_ELEPHANT_LEFT);
      blockItem("totem_part_elephant_right", TOTEM_PART_ELEPHANT_RIGHT);
      blockItem("xoltec_idol_body_left", XOLTEC_IDOL_BODY_LEFT);
      blockItem("xoltec_idol_body_right", XOLTEC_IDOL_BODY_RIGHT);
      blockItem("xoltec_idol_head_left", XOLTEC_IDOL_HEAD_LEFT);
      blockItem("xoltec_idol_head_right", XOLTEC_IDOL_HEAD_RIGHT);
      blockItem("loot_basket", LOOT_BASKET);
      blockItem("urn", URN);
      AWCoreItems.REGISTER
         .register("wooden_coffin", () -> new ForgeRestoredCoffinBlockItem((Block)WOODEN_COFFIN.get(), new net.minecraft.world.item.Item.Properties()));
      AWCoreItems.REGISTER
         .register("stone_coffin", () -> new ForgeRestoredCoffinBlockItem((Block)STONE_COFFIN.get(), new net.minecraft.world.item.Item.Properties()));
      AWCoreItems.REGISTER
         .register(
            "advanced_loot_chest", () -> new ForgeAdvancedLootChestBlockItem((Block)ADVANCED_LOOT_CHEST.get(), new net.minecraft.world.item.Item.Properties())
         );
      blockItem("advanced_spawner", ADVANCED_SPAWNER);
      AWCoreItems.REGISTER
         .register("protection_flag", () -> new ForgeProtectionFlagBlockItem((Block)PROTECTION_FLAG.get(), new net.minecraft.world.item.Item.Properties()));
      blockItem("gate_vertical_wooden", GATE_VERTICAL_WOODEN);
      blockItem("gate_vertical_iron", GATE_VERTICAL_IRON);
      blockItem("gate_single_wood", GATE_SINGLE_WOOD);
      blockItem("gate_single_iron", GATE_SINGLE_IRON);
      blockItem("gate_double_wood", GATE_DOUBLE_WOOD);
      blockItem("gate_double_iron", GATE_DOUBLE_IRON);
      blockItem("gate_drawbridge", GATE_DRAWBRIDGE);
      AWCoreItems.REGISTER
         .register(
            "undead_cemetery_fill_skull_1",
            () -> new AwsTemplateBlockItem((Block)UNDEAD_CEMETERY_FILL_SKULL_1.get(), new net.minecraft.world.item.Item.Properties())
         );
      AWCoreItems.REGISTER
         .register("awt_town_marker", () -> new AwtTownBlockItem((Block)AWT_TOWN_MARKER.get(), new net.minecraft.world.item.Item.Properties()));
      blockItem("structure_scanner_block", STRUCTURE_SCANNER_BLOCK);
      blockItem("drafting_station", DRAFTING_STATION);
      AWCoreItems.REGISTER
         .register(
            "structure_builder_ticked",
            () -> new SurvivalStructureBuilderItem((Block)STRUCTURE_BUILDER_TICKED.get(), new net.minecraft.world.item.Item.Properties())
         );
   }
}
