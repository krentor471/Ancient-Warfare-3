package net.shadowmage.ancientwarfare.core.forge.init;

import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWeaponEffect;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgrade;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;
import net.shadowmage.ancientwarfare.core.item.BackpackItem;
import net.shadowmage.ancientwarfare.core.item.BallistaAmmoItem;
import net.shadowmage.ancientwarfare.core.item.BallistaAmmoType;
import net.shadowmage.ancientwarfare.core.item.BallistaBoatItem;
import net.shadowmage.ancientwarfare.core.item.BallistaMobileItem;
import net.shadowmage.ancientwarfare.core.item.BallistaTurretStandItem;
import net.shadowmage.ancientwarfare.core.item.BardInstrumentItem;
import net.shadowmage.ancientwarfare.core.item.BatteringRamItem;
import net.shadowmage.ancientwarfare.core.item.CannonAmmoItem;
import net.shadowmage.ancientwarfare.core.item.CannonAmmoType;
import net.shadowmage.ancientwarfare.core.item.CannonMobileItem;
import net.shadowmage.ancientwarfare.core.item.CannonTurretStandItem;
import net.shadowmage.ancientwarfare.core.item.CatapultAmmoItem;
import net.shadowmage.ancientwarfare.core.item.CatapultAmmoType;
import net.shadowmage.ancientwarfare.core.item.CatapultBoatItem;
import net.shadowmage.ancientwarfare.core.item.CatapultMobileItem;
import net.shadowmage.ancientwarfare.core.item.CatapultTurretStandItem;
import net.shadowmage.ancientwarfare.core.item.ChestPushcartItem;
import net.shadowmage.ancientwarfare.core.item.CoinItem;
import net.shadowmage.ancientwarfare.core.item.CombatOrderItem;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.item.GateSpawnerItem;
import net.shadowmage.ancientwarfare.core.item.GiantTrebuchetItem;
import net.shadowmage.ancientwarfare.core.item.HammerItem;
import net.shadowmage.ancientwarfare.core.item.HwachaAmmoItem;
import net.shadowmage.ancientwarfare.core.item.HwachaAmmoType;
import net.shadowmage.ancientwarfare.core.item.HwachaItem;
import net.shadowmage.ancientwarfare.core.item.ManualItem;
import net.shadowmage.ancientwarfare.core.item.NpcShieldItem;
import net.shadowmage.ancientwarfare.core.item.NpcSpawnerItem;
import net.shadowmage.ancientwarfare.core.item.NpcWeaponItem;
import net.shadowmage.ancientwarfare.core.item.QuillItem;
import net.shadowmage.ancientwarfare.core.item.ResearchBookItem;
import net.shadowmage.ancientwarfare.core.item.ResearchNoteItem;
import net.shadowmage.ancientwarfare.core.item.RoutingOrderItem;
import net.shadowmage.ancientwarfare.core.item.StructureScannerItem;
import net.shadowmage.ancientwarfare.core.item.TradeOrderItem;
import net.shadowmage.ancientwarfare.core.item.TransportBoatItem;
import net.shadowmage.ancientwarfare.core.item.TrebuchetFixedItem;
import net.shadowmage.ancientwarfare.core.item.TrebuchetMobileItem;
import net.shadowmage.ancientwarfare.core.item.TrebuchetTurretItem;
import net.shadowmage.ancientwarfare.core.item.UpkeepOrderItem;
import net.shadowmage.ancientwarfare.core.item.VehicleArmorItem;
import net.shadowmage.ancientwarfare.core.item.VehicleCraftingMaterialItem;
import net.shadowmage.ancientwarfare.core.item.VehicleUpgradeItem;
import net.shadowmage.ancientwarfare.core.item.WorkOrderItem;
import net.shadowmage.ancientwarfare.core.item.WorksiteUpgradeItem;

public final class AWCoreItems {
   public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, "ancientwarfare");
   public static final RegistryObject<Item> INFO_TOOL = simple("info_tool");
   public static final RegistryObject<Item> MANUAL = REGISTER.register("manual", ManualItem::new);
   public static final RegistryObject<Item> RESEARCH_BOOK = REGISTER.register("research_book", ResearchBookItem::new);
   public static final RegistryObject<Item> RESEARCH_NOTE = REGISTER.register("research_note", ResearchNoteItem::new);
   public static final RegistryObject<Item> BACKPACK = REGISTER.register("backpack", BackpackItem::new);
   public static final RegistryObject<Item> STRUCTURE_SCANNER = REGISTER.register("structure_scanner", StructureScannerItem::new);
   public static final RegistryObject<Item> STEEL_INGOT = simple("steel_ingot");
   public static final RegistryObject<Item> WOODEN_GEAR = simple("wooden_gear");
   public static final RegistryObject<Item> IRON_GEAR = simple("iron_gear");
   public static final RegistryObject<Item> STEEL_GEAR = simple("steel_gear");
   public static final RegistryObject<Item> WOODEN_SHAFT = simple("wooden_shaft");
   public static final RegistryObject<Item> IRON_SHAFT = simple("iron_shaft");
   public static final RegistryObject<Item> STEEL_SHAFT = simple("steel_shaft");
   public static final RegistryObject<Item> WOODEN_BEARINGS = simple("wooden_bearings");
   public static final RegistryObject<Item> IRON_BEARINGS = simple("iron_bearings");
   public static final RegistryObject<Item> STEEL_BEARINGS = simple("steel_bearings");
   public static final RegistryObject<Item> WOODEN_QUILL = REGISTER.register("wooden_quill", () -> new QuillItem(Tiers.WOOD));
   public static final RegistryObject<Item> STONE_QUILL = REGISTER.register("stone_quill", () -> new QuillItem(Tiers.STONE));
   public static final RegistryObject<Item> IRON_QUILL = REGISTER.register("iron_quill", () -> new QuillItem(Tiers.IRON));
   public static final RegistryObject<Item> GOLD_QUILL = REGISTER.register("gold_quill", () -> new QuillItem(Tiers.GOLD));
   public static final RegistryObject<Item> DIAMOND_QUILL = REGISTER.register("diamond_quill", () -> new QuillItem(Tiers.DIAMOND));
   public static final RegistryObject<Item> WOODEN_HAMMER = REGISTER.register("wooden_hammer", () -> new HammerItem(Tiers.WOOD));
   public static final RegistryObject<Item> STONE_HAMMER = REGISTER.register("stone_hammer", () -> new HammerItem(Tiers.STONE));
   public static final RegistryObject<Item> IRON_HAMMER = REGISTER.register("iron_hammer", () -> new HammerItem(Tiers.IRON));
   public static final RegistryObject<Item> GOLD_HAMMER = REGISTER.register("gold_hammer", () -> new HammerItem(Tiers.GOLD));
   public static final RegistryObject<Item> DIAMOND_HAMMER = REGISTER.register("diamond_hammer", () -> new HammerItem(Tiers.DIAMOND));
   public static final RegistryObject<Item> FLAME_CHARGE = REGISTER.register(
      "flame_charge", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.AMMO_MATERIAL)
   );
   public static final RegistryObject<Item> EXPLOSIVE_CHARGE = REGISTER.register(
      "explosive_charge", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.AMMO_MATERIAL)
   );
   public static final RegistryObject<Item> ROCKET_CHARGE = REGISTER.register(
      "rocket_charge", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.AMMO_MATERIAL)
   );
   public static final RegistryObject<Item> CLUSTER_CHARGE = REGISTER.register(
      "cluster_charge", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.AMMO_MATERIAL)
   );
   public static final RegistryObject<Item> NAPALM_CHARGE = REGISTER.register(
      "napalm_charge", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.AMMO_MATERIAL)
   );
   public static final RegistryObject<Item> CLAY_CASING = REGISTER.register(
      "clay_casing", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.AMMO_MATERIAL)
   );
   public static final RegistryObject<Item> IRON_CASING = REGISTER.register(
      "iron_casing", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.AMMO_MATERIAL)
   );
   public static final RegistryObject<Item> MOBILITY_UNIT = REGISTER.register(
      "mobility_unit", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> TURRET_COMPONENTS = REGISTER.register(
      "turret_components", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> TORSION_UNIT = REGISTER.register(
      "torsion_unit", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> COUNTER_WEIGHT_UNIT = REGISTER.register(
      "counter_weight_unit", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> POWDER_CASE = REGISTER.register(
      "powder_case", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> EQUIPMENT_BAY = REGISTER.register(
      "equipment_bay", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> ROUGH_WOOD = REGISTER.register(
      "rough_wood", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> TREATED_WOOD = REGISTER.register(
      "treated_wood", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> IRONSHOD_WOOD = REGISTER.register(
      "ironshod_wood", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> IRON_CORE_WOOD = REGISTER.register(
      "iron_core_wood", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> ROUGH_IRON = REGISTER.register(
      "rough_iron", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> FINE_IRON = REGISTER.register(
      "fine_iron", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> TEMPERED_IRON = REGISTER.register(
      "tempered_iron", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> MINOR_ALLOY = REGISTER.register(
      "minor_alloy", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> MAJOR_ALLOY = REGISTER.register(
      "major_alloy", () -> new VehicleCraftingMaterialItem(VehicleCraftingMaterialItem.MaterialType.VEHICLE_COMPONENT)
   );
   public static final RegistryObject<Item> ARMOR_STONE = REGISTER.register("armor_stone", () -> new VehicleArmorItem(VehicleArmorItem.Type.STONE));
   public static final RegistryObject<Item> ARMOR_IRON = REGISTER.register("armor_iron", () -> new VehicleArmorItem(VehicleArmorItem.Type.IRON));
   public static final RegistryObject<Item> ARMOR_OBSIDIAN = REGISTER.register("armor_obsidian", () -> new VehicleArmorItem(VehicleArmorItem.Type.OBSIDIAN));
   public static final RegistryObject<Item> VEHICLE_UPGRADE_SPEED = REGISTER.register(
      "vehicle_upgrade_speed", () -> new VehicleUpgradeItem("vehicle_upgrade_speed")
   );
   public static final RegistryObject<Item> VEHICLE_UPGRADE_AIM = REGISTER.register("vehicle_upgrade_aim", () -> new VehicleUpgradeItem("vehicle_upgrade_aim"));
   public static final RegistryObject<Item> VEHICLE_UPGRADE_RELOAD = REGISTER.register(
      "vehicle_upgrade_reload", () -> new VehicleUpgradeItem("vehicle_upgrade_reload")
   );
   public static final RegistryObject<Item> VEHICLE_UPGRADE_POWER = REGISTER.register(
      "vehicle_upgrade_power", () -> new VehicleUpgradeItem("vehicle_upgrade_power")
   );
   public static final RegistryObject<Item> VEHICLE_UPGRADE_TURRET_PITCH = REGISTER.register(
      "vehicle_upgrade_turret_pitch", () -> new VehicleUpgradeItem("vehicle_upgrade_turret_pitch")
   );
   public static final RegistryObject<Item> VEHICLE_UPGRADE_PITCH_UP = REGISTER.register(
      "vehicle_upgrade_pitch_up", () -> new VehicleUpgradeItem("vehicle_upgrade_pitch_up")
   );
   public static final RegistryObject<Item> VEHICLE_UPGRADE_PITCH_DOWN = REGISTER.register(
      "vehicle_upgrade_pitch_down", () -> new VehicleUpgradeItem("vehicle_upgrade_pitch_down")
   );
   public static final RegistryObject<Item> CHEST_PUSHCART = REGISTER.register("chest_pushcart", () -> new ChestPushcartItem(0));
   public static final RegistryObject<Item> CHEST_PUSHCART_IRON_BAND_1 = REGISTER.register("chest_pushcart_iron_band_1", () -> new ChestPushcartItem(1));
   public static final RegistryObject<Item> CHEST_PUSHCART_IRON_BAND_2 = REGISTER.register("chest_pushcart_iron_band_2", () -> new ChestPushcartItem(2));
   public static final RegistryObject<Item> CHEST_PUSHCART_IRON = REGISTER.register("chest_pushcart_iron", () -> new ChestPushcartItem(3));
   public static final RegistryObject<Item> CHEST_PUSHCART_STEEL = REGISTER.register("chest_pushcart_steel", () -> new ChestPushcartItem(4));
   public static final RegistryObject<Item> BALLISTA_MOBILE = REGISTER.register("ballista_mobile", () -> new BallistaMobileItem(0));
   public static final RegistryObject<Item> BALLISTA_MOBILE_IRON_BAND_1 = REGISTER.register("ballista_mobile_iron_band_1", () -> new BallistaMobileItem(1));
   public static final RegistryObject<Item> BALLISTA_MOBILE_IRON_BAND_2 = REGISTER.register("ballista_mobile_iron_band_2", () -> new BallistaMobileItem(2));
   public static final RegistryObject<Item> BALLISTA_MOBILE_IRON = REGISTER.register("ballista_mobile_iron", () -> new BallistaMobileItem(3));
   public static final RegistryObject<Item> BALLISTA_MOBILE_STEEL = REGISTER.register("ballista_mobile_steel", () -> new BallistaMobileItem(4));
   public static final RegistryObject<Item> BALLISTA_MOBILE_FIXED = REGISTER.register("ballista_mobile_fixed", () -> new BallistaMobileItem(0, true));
   public static final RegistryObject<Item> BALLISTA_MOBILE_FIXED_IRON_BAND_1 = REGISTER.register(
      "ballista_mobile_fixed_iron_band_1", () -> new BallistaMobileItem(1, true)
   );
   public static final RegistryObject<Item> BALLISTA_MOBILE_FIXED_IRON_BAND_2 = REGISTER.register(
      "ballista_mobile_fixed_iron_band_2", () -> new BallistaMobileItem(2, true)
   );
   public static final RegistryObject<Item> BALLISTA_MOBILE_FIXED_IRON = REGISTER.register("ballista_mobile_fixed_iron", () -> new BallistaMobileItem(3, true));
   public static final RegistryObject<Item> BALLISTA_MOBILE_FIXED_STEEL = REGISTER.register(
      "ballista_mobile_fixed_steel", () -> new BallistaMobileItem(4, true)
   );
   public static final RegistryObject<Item> BALLISTA_BOAT = REGISTER.register("ballista_boat", () -> new BallistaBoatItem(0));
   public static final RegistryObject<Item> BALLISTA_BOAT_IRON_BAND_1 = REGISTER.register("ballista_boat_iron_band_1", () -> new BallistaBoatItem(1));
   public static final RegistryObject<Item> BALLISTA_BOAT_IRON_BAND_2 = REGISTER.register("ballista_boat_iron_band_2", () -> new BallistaBoatItem(2));
   public static final RegistryObject<Item> BALLISTA_BOAT_IRON = REGISTER.register("ballista_boat_iron", () -> new BallistaBoatItem(3));
   public static final RegistryObject<Item> BALLISTA_BOAT_STEEL = REGISTER.register("ballista_boat_steel", () -> new BallistaBoatItem(4));
   public static final RegistryObject<Item> BALLISTA_TURRET_STAND = REGISTER.register("ballista_turret_stand", () -> new BallistaTurretStandItem(0));
   public static final RegistryObject<Item> BALLISTA_TURRET_STAND_IRON_BAND_1 = REGISTER.register(
      "ballista_turret_stand_iron_band_1", () -> new BallistaTurretStandItem(1)
   );
   public static final RegistryObject<Item> BALLISTA_TURRET_STAND_IRON_BAND_2 = REGISTER.register(
      "ballista_turret_stand_iron_band_2", () -> new BallistaTurretStandItem(2)
   );
   public static final RegistryObject<Item> BALLISTA_TURRET_STAND_IRON = REGISTER.register("ballista_turret_stand_iron", () -> new BallistaTurretStandItem(3));
   public static final RegistryObject<Item> BALLISTA_TURRET_STAND_STEEL = REGISTER.register("ballista_turret_stand_steel", () -> new BallistaTurretStandItem(4));
   public static final RegistryObject<Item> BALLISTA_STAND_FIXED = REGISTER.register("ballista_stand_fixed", () -> new BallistaTurretStandItem(0, true));
   public static final RegistryObject<Item> BALLISTA_STAND_FIXED_IRON_BAND_1 = REGISTER.register(
      "ballista_stand_fixed_iron_band_1", () -> new BallistaTurretStandItem(1, true)
   );
   public static final RegistryObject<Item> BALLISTA_STAND_FIXED_IRON_BAND_2 = REGISTER.register(
      "ballista_stand_fixed_iron_band_2", () -> new BallistaTurretStandItem(2, true)
   );
   public static final RegistryObject<Item> BALLISTA_STAND_FIXED_IRON = REGISTER.register(
      "ballista_stand_fixed_iron", () -> new BallistaTurretStandItem(3, true)
   );
   public static final RegistryObject<Item> BALLISTA_STAND_FIXED_STEEL = REGISTER.register(
      "ballista_stand_fixed_steel", () -> new BallistaTurretStandItem(4, true)
   );
   public static final RegistryObject<Item> CATAPULT_MOBILE = REGISTER.register("catapult_mobile", () -> new CatapultMobileItem(0));
   public static final RegistryObject<Item> CATAPULT_MOBILE_IRON_BAND_1 = REGISTER.register("catapult_mobile_iron_band_1", () -> new CatapultMobileItem(1));
   public static final RegistryObject<Item> CATAPULT_MOBILE_IRON_BAND_2 = REGISTER.register("catapult_mobile_iron_band_2", () -> new CatapultMobileItem(2));
   public static final RegistryObject<Item> CATAPULT_MOBILE_IRON = REGISTER.register("catapult_mobile_iron", () -> new CatapultMobileItem(3));
   public static final RegistryObject<Item> CATAPULT_MOBILE_STEEL = REGISTER.register("catapult_mobile_steel", () -> new CatapultMobileItem(4));
   public static final RegistryObject<Item> CATAPULT_MOBILE_FIXED = REGISTER.register("catapult_mobile_fixed", () -> new CatapultMobileItem(0, true));
   public static final RegistryObject<Item> CATAPULT_MOBILE_FIXED_IRON_BAND_1 = REGISTER.register(
      "catapult_mobile_fixed_iron_band_1", () -> new CatapultMobileItem(1, true)
   );
   public static final RegistryObject<Item> CATAPULT_MOBILE_FIXED_IRON_BAND_2 = REGISTER.register(
      "catapult_mobile_fixed_iron_band_2", () -> new CatapultMobileItem(2, true)
   );
   public static final RegistryObject<Item> CATAPULT_MOBILE_FIXED_IRON = REGISTER.register("catapult_mobile_fixed_iron", () -> new CatapultMobileItem(3, true));
   public static final RegistryObject<Item> CATAPULT_MOBILE_FIXED_STEEL = REGISTER.register(
      "catapult_mobile_fixed_steel", () -> new CatapultMobileItem(4, true)
   );
   public static final RegistryObject<Item> TREBUCHET_MOBILE = REGISTER.register("trebuchet_mobile", () -> new TrebuchetMobileItem(0));
   public static final RegistryObject<Item> TREBUCHET_MOBILE_IRON_BAND_1 = REGISTER.register("trebuchet_mobile_iron_band_1", () -> new TrebuchetMobileItem(1));
   public static final RegistryObject<Item> TREBUCHET_MOBILE_IRON_BAND_2 = REGISTER.register("trebuchet_mobile_iron_band_2", () -> new TrebuchetMobileItem(2));
   public static final RegistryObject<Item> TREBUCHET_MOBILE_IRON = REGISTER.register("trebuchet_mobile_iron", () -> new TrebuchetMobileItem(3));
   public static final RegistryObject<Item> TREBUCHET_MOBILE_STEEL = REGISTER.register("trebuchet_mobile_steel", () -> new TrebuchetMobileItem(4));
   public static final RegistryObject<Item> TREBUCHET_FIXED = REGISTER.register("trebuchet_fixed", () -> new TrebuchetFixedItem(0));
   public static final RegistryObject<Item> TREBUCHET_FIXED_IRON_BAND_1 = REGISTER.register("trebuchet_fixed_iron_band_1", () -> new TrebuchetFixedItem(1));
   public static final RegistryObject<Item> TREBUCHET_FIXED_IRON_BAND_2 = REGISTER.register("trebuchet_fixed_iron_band_2", () -> new TrebuchetFixedItem(2));
   public static final RegistryObject<Item> TREBUCHET_FIXED_IRON = REGISTER.register("trebuchet_fixed_iron", () -> new TrebuchetFixedItem(3));
   public static final RegistryObject<Item> TREBUCHET_FIXED_STEEL = REGISTER.register("trebuchet_fixed_steel", () -> new TrebuchetFixedItem(4));
   public static final RegistryObject<Item> TREBUCHET_TURRET = REGISTER.register("trebuchet_turret", () -> new TrebuchetTurretItem(0));
   public static final RegistryObject<Item> TREBUCHET_TURRET_IRON_BAND_1 = REGISTER.register("trebuchet_turret_iron_band_1", () -> new TrebuchetTurretItem(1));
   public static final RegistryObject<Item> TREBUCHET_TURRET_IRON_BAND_2 = REGISTER.register("trebuchet_turret_iron_band_2", () -> new TrebuchetTurretItem(2));
   public static final RegistryObject<Item> TREBUCHET_TURRET_IRON = REGISTER.register("trebuchet_turret_iron", () -> new TrebuchetTurretItem(3));
   public static final RegistryObject<Item> TREBUCHET_TURRET_STEEL = REGISTER.register("trebuchet_turret_steel", () -> new TrebuchetTurretItem(4));
   public static final RegistryObject<Item> GIANT_TREBUCHET = REGISTER.register("giant_trebuchet", () -> new GiantTrebuchetItem(0));
   public static final RegistryObject<Item> GIANT_TREBUCHET_IRON_BAND_1 = REGISTER.register("giant_trebuchet_iron_band_1", () -> new GiantTrebuchetItem(1));
   public static final RegistryObject<Item> GIANT_TREBUCHET_IRON_BAND_2 = REGISTER.register("giant_trebuchet_iron_band_2", () -> new GiantTrebuchetItem(2));
   public static final RegistryObject<Item> GIANT_TREBUCHET_IRON = REGISTER.register("giant_trebuchet_iron", () -> new GiantTrebuchetItem(3));
   public static final RegistryObject<Item> GIANT_TREBUCHET_STEEL = REGISTER.register("giant_trebuchet_steel", () -> new GiantTrebuchetItem(4));
   public static final RegistryObject<Item> CATAPULT_TURRET_STAND = REGISTER.register("catapult_turret_stand", () -> new CatapultTurretStandItem(0));
   public static final RegistryObject<Item> CATAPULT_TURRET_STAND_IRON_BAND_1 = REGISTER.register(
      "catapult_turret_stand_iron_band_1", () -> new CatapultTurretStandItem(1)
   );
   public static final RegistryObject<Item> CATAPULT_TURRET_STAND_IRON_BAND_2 = REGISTER.register(
      "catapult_turret_stand_iron_band_2", () -> new CatapultTurretStandItem(2)
   );
   public static final RegistryObject<Item> CATAPULT_TURRET_STAND_IRON = REGISTER.register("catapult_turret_stand_iron", () -> new CatapultTurretStandItem(3));
   public static final RegistryObject<Item> CATAPULT_TURRET_STAND_STEEL = REGISTER.register("catapult_turret_stand_steel", () -> new CatapultTurretStandItem(4));
   public static final RegistryObject<Item> CATAPULT_STAND_FIXED = REGISTER.register("catapult_stand_fixed", () -> new CatapultTurretStandItem(0, true));
   public static final RegistryObject<Item> CATAPULT_STAND_FIXED_IRON_BAND_1 = REGISTER.register(
      "catapult_stand_fixed_iron_band_1", () -> new CatapultTurretStandItem(1, true)
   );
   public static final RegistryObject<Item> CATAPULT_STAND_FIXED_IRON_BAND_2 = REGISTER.register(
      "catapult_stand_fixed_iron_band_2", () -> new CatapultTurretStandItem(2, true)
   );
   public static final RegistryObject<Item> CATAPULT_STAND_FIXED_IRON = REGISTER.register(
      "catapult_stand_fixed_iron", () -> new CatapultTurretStandItem(3, true)
   );
   public static final RegistryObject<Item> CATAPULT_STAND_FIXED_STEEL = REGISTER.register(
      "catapult_stand_fixed_steel", () -> new CatapultTurretStandItem(4, true)
   );
   public static final RegistryObject<Item> CATAPULT_BOAT = REGISTER.register("catapult_boat", () -> new CatapultBoatItem(0));
   public static final RegistryObject<Item> CATAPULT_BOAT_IRON_BAND_1 = REGISTER.register("catapult_boat_iron_band_1", () -> new CatapultBoatItem(1));
   public static final RegistryObject<Item> CATAPULT_BOAT_IRON_BAND_2 = REGISTER.register("catapult_boat_iron_band_2", () -> new CatapultBoatItem(2));
   public static final RegistryObject<Item> CATAPULT_BOAT_IRON = REGISTER.register("catapult_boat_iron", () -> new CatapultBoatItem(3));
   public static final RegistryObject<Item> CATAPULT_BOAT_STEEL = REGISTER.register("catapult_boat_steel", () -> new CatapultBoatItem(4));
   public static final RegistryObject<Item> TRANSPORT_BOAT = REGISTER.register("transport_boat", () -> new TransportBoatItem(0));
   public static final RegistryObject<Item> TRANSPORT_BOAT_IRON_BAND_1 = REGISTER.register("transport_boat_iron_band_1", () -> new TransportBoatItem(1));
   public static final RegistryObject<Item> TRANSPORT_BOAT_IRON_BAND_2 = REGISTER.register("transport_boat_iron_band_2", () -> new TransportBoatItem(2));
   public static final RegistryObject<Item> TRANSPORT_BOAT_IRON = REGISTER.register("transport_boat_iron", () -> new TransportBoatItem(3));
   public static final RegistryObject<Item> TRANSPORT_BOAT_STEEL = REGISTER.register("transport_boat_steel", () -> new TransportBoatItem(4));
   public static final RegistryObject<Item> CANNON_TURRET_STAND = REGISTER.register("cannon_turret_stand", () -> new CannonTurretStandItem(0));
   public static final RegistryObject<Item> CANNON_TURRET_STAND_IRON_BAND_1 = REGISTER.register(
      "cannon_turret_stand_iron_band_1", () -> new CannonTurretStandItem(1)
   );
   public static final RegistryObject<Item> CANNON_TURRET_STAND_IRON_BAND_2 = REGISTER.register(
      "cannon_turret_stand_iron_band_2", () -> new CannonTurretStandItem(2)
   );
   public static final RegistryObject<Item> CANNON_TURRET_STAND_IRON = REGISTER.register("cannon_turret_stand_iron", () -> new CannonTurretStandItem(3));
   public static final RegistryObject<Item> CANNON_TURRET_STAND_STEEL = REGISTER.register("cannon_turret_stand_steel", () -> new CannonTurretStandItem(4));
   public static final RegistryObject<Item> CANNON_STAND_FIXED = REGISTER.register("cannon_stand_fixed", () -> new CannonTurretStandItem(0, true));
   public static final RegistryObject<Item> CANNON_STAND_FIXED_IRON_BAND_1 = REGISTER.register(
      "cannon_stand_fixed_iron_band_1", () -> new CannonTurretStandItem(1, true)
   );
   public static final RegistryObject<Item> CANNON_STAND_FIXED_IRON_BAND_2 = REGISTER.register(
      "cannon_stand_fixed_iron_band_2", () -> new CannonTurretStandItem(2, true)
   );
   public static final RegistryObject<Item> CANNON_STAND_FIXED_IRON = REGISTER.register("cannon_stand_fixed_iron", () -> new CannonTurretStandItem(3, true));
   public static final RegistryObject<Item> CANNON_STAND_FIXED_STEEL = REGISTER.register("cannon_stand_fixed_steel", () -> new CannonTurretStandItem(4, true));
   public static final RegistryObject<Item> CANNON_MOBILE = REGISTER.register("cannon_mobile", () -> new CannonMobileItem(0));
   public static final RegistryObject<Item> CANNON_MOBILE_IRON_BAND_1 = REGISTER.register("cannon_mobile_iron_band_1", () -> new CannonMobileItem(1));
   public static final RegistryObject<Item> CANNON_MOBILE_IRON_BAND_2 = REGISTER.register("cannon_mobile_iron_band_2", () -> new CannonMobileItem(2));
   public static final RegistryObject<Item> CANNON_MOBILE_IRON = REGISTER.register("cannon_mobile_iron", () -> new CannonMobileItem(3));
   public static final RegistryObject<Item> CANNON_MOBILE_STEEL = REGISTER.register("cannon_mobile_steel", () -> new CannonMobileItem(4));
   public static final RegistryObject<Item> BATTERING_RAM = REGISTER.register("battering_ram", () -> new BatteringRamItem(0));
   public static final RegistryObject<Item> BATTERING_RAM_IRON_BAND_1 = REGISTER.register("battering_ram_iron_band_1", () -> new BatteringRamItem(1));
   public static final RegistryObject<Item> BATTERING_RAM_IRON_BAND_2 = REGISTER.register("battering_ram_iron_band_2", () -> new BatteringRamItem(2));
   public static final RegistryObject<Item> BATTERING_RAM_IRON = REGISTER.register("battering_ram_iron", () -> new BatteringRamItem(3));
   public static final RegistryObject<Item> BATTERING_RAM_STEEL = REGISTER.register("battering_ram_steel", () -> new BatteringRamItem(4));
   public static final RegistryObject<Item> HWACHA = REGISTER.register("hwacha", () -> new HwachaItem(0));
   public static final RegistryObject<Item> HWACHA_IRON_BAND_1 = REGISTER.register("hwacha_iron_band_1", () -> new HwachaItem(1));
   public static final RegistryObject<Item> HWACHA_IRON_BAND_2 = REGISTER.register("hwacha_iron_band_2", () -> new HwachaItem(2));
   public static final RegistryObject<Item> HWACHA_IRON = REGISTER.register("hwacha_iron", () -> new HwachaItem(3));
   public static final RegistryObject<Item> HWACHA_STEEL = REGISTER.register("hwacha_steel", () -> new HwachaItem(4));
   public static final RegistryObject<Item> AMMO_BALLISTA_BOLT = REGISTER.register("ammo_ballista_bolt", () -> new BallistaAmmoItem(BallistaAmmoType.WOOD));
   public static final RegistryObject<Item> AMMO_BALLISTA_BOLT_IRON = REGISTER.register(
      "ammo_ballista_bolt_iron", () -> new BallistaAmmoItem(BallistaAmmoType.IRON)
   );
   public static final RegistryObject<Item> AMMO_BALLISTA_BOLT_FLAME = REGISTER.register(
      "ammo_ballista_bolt_flame", () -> new BallistaAmmoItem(BallistaAmmoType.FLAME)
   );
   public static final RegistryObject<Item> AMMO_BALLISTA_BOLT_EXPLOSIVE = REGISTER.register(
      "ammo_ballista_bolt_explosive", () -> new BallistaAmmoItem(BallistaAmmoType.EXPLOSIVE)
   );
   public static final RegistryObject<Item> AMMO_HWACHA_ROCKET = REGISTER.register("ammo_hwacha_rocket", () -> new HwachaAmmoItem(HwachaAmmoType.STANDARD));
   public static final RegistryObject<Item> AMMO_HWACHA_ROCKET_FLAME = REGISTER.register(
      "ammo_hwacha_rocket_flame", () -> new HwachaAmmoItem(HwachaAmmoType.FLAME)
   );
   public static final RegistryObject<Item> AMMO_HWACHA_ROCKET_EXPLOSIVE = REGISTER.register(
      "ammo_hwacha_rocket_explosive", () -> new HwachaAmmoItem(HwachaAmmoType.EXPLOSIVE)
   );
   public static final RegistryObject<Item> AMMO_HWACHA_ROCKET_AIRBURST = REGISTER.register(
      "ammo_hwacha_rocket_airburst", () -> new HwachaAmmoItem(HwachaAmmoType.AIRBURST)
   );
   public static final RegistryObject<Item> AMMO_IRON_SHOT_5 = REGISTER.register("ammo_iron_shot_5", () -> new CannonAmmoItem(CannonAmmoType.IRON_5));
   public static final RegistryObject<Item> AMMO_IRON_SHOT_10 = REGISTER.register("ammo_iron_shot_10", () -> new CannonAmmoItem(CannonAmmoType.IRON_10));
   public static final RegistryObject<Item> AMMO_IRON_SHOT_15 = REGISTER.register("ammo_iron_shot_15", () -> new CannonAmmoItem(CannonAmmoType.IRON_15));
   public static final RegistryObject<Item> AMMO_IRON_SHOT_25 = REGISTER.register("ammo_iron_shot_25", () -> new CannonAmmoItem(CannonAmmoType.IRON_25));
   public static final RegistryObject<Item> AMMO_GRAPE_SHOT_5 = REGISTER.register("ammo_grape_shot_5", () -> new CannonAmmoItem(CannonAmmoType.GRAPE_5));
   public static final RegistryObject<Item> AMMO_GRAPE_SHOT_10 = REGISTER.register("ammo_grape_shot_10", () -> new CannonAmmoItem(CannonAmmoType.GRAPE_10));
   public static final RegistryObject<Item> AMMO_GRAPE_SHOT_15 = REGISTER.register("ammo_grape_shot_15", () -> new CannonAmmoItem(CannonAmmoType.GRAPE_15));
   public static final RegistryObject<Item> AMMO_GRAPE_SHOT_25 = REGISTER.register("ammo_grape_shot_25", () -> new CannonAmmoItem(CannonAmmoType.GRAPE_25));
   public static final RegistryObject<Item> AMMO_CANISTER_SHOT_5 = REGISTER.register(
      "ammo_canister_shot_5", () -> new CannonAmmoItem(CannonAmmoType.CANISTER_5)
   );
   public static final RegistryObject<Item> AMMO_CANISTER_SHOT_10 = REGISTER.register(
      "ammo_canister_shot_10", () -> new CannonAmmoItem(CannonAmmoType.CANISTER_10)
   );
   public static final RegistryObject<Item> AMMO_CANISTER_SHOT_15 = REGISTER.register(
      "ammo_canister_shot_15", () -> new CannonAmmoItem(CannonAmmoType.CANISTER_15)
   );
   public static final RegistryObject<Item> AMMO_CANISTER_SHOT_25 = REGISTER.register(
      "ammo_canister_shot_25", () -> new CannonAmmoItem(CannonAmmoType.CANISTER_25)
   );
   public static final RegistryObject<Item> AMMO_STONE_SHOT_10 = REGISTER.register("ammo_stone_shot_10", () -> new CatapultAmmoItem(CatapultAmmoType.STONE_10));
   public static final RegistryObject<Item> AMMO_STONE_SHOT_15 = REGISTER.register("ammo_stone_shot_15", () -> new CatapultAmmoItem(CatapultAmmoType.STONE_15));
   public static final RegistryObject<Item> AMMO_STONE_SHOT_30 = REGISTER.register("ammo_stone_shot_30", () -> new CatapultAmmoItem(CatapultAmmoType.STONE_30));
   public static final RegistryObject<Item> AMMO_STONE_SHOT_45 = REGISTER.register("ammo_stone_shot_45", () -> new CatapultAmmoItem(CatapultAmmoType.STONE_45));
   public static final RegistryObject<Item> AMMO_FLAME_SHOT_10 = REGISTER.register("ammo_flame_shot_10", () -> new CatapultAmmoItem(CatapultAmmoType.FLAME_10));
   public static final RegistryObject<Item> AMMO_FLAME_SHOT_15 = REGISTER.register("ammo_flame_shot_15", () -> new CatapultAmmoItem(CatapultAmmoType.FLAME_15));
   public static final RegistryObject<Item> AMMO_FLAME_SHOT_30 = REGISTER.register("ammo_flame_shot_30", () -> new CatapultAmmoItem(CatapultAmmoType.FLAME_30));
   public static final RegistryObject<Item> AMMO_FLAME_SHOT_45 = REGISTER.register("ammo_flame_shot_45", () -> new CatapultAmmoItem(CatapultAmmoType.FLAME_45));
   public static final RegistryObject<Item> AMMO_PEBBLE_SHOT_10 = REGISTER.register(
      "ammo_pebble_shot_10", () -> new CatapultAmmoItem(CatapultAmmoType.PEBBLE_10)
   );
   public static final RegistryObject<Item> AMMO_PEBBLE_SHOT_15 = REGISTER.register(
      "ammo_pebble_shot_15", () -> new CatapultAmmoItem(CatapultAmmoType.PEBBLE_15)
   );
   public static final RegistryObject<Item> AMMO_PEBBLE_SHOT_30 = REGISTER.register(
      "ammo_pebble_shot_30", () -> new CatapultAmmoItem(CatapultAmmoType.PEBBLE_30)
   );
   public static final RegistryObject<Item> AMMO_PEBBLE_SHOT_45 = REGISTER.register(
      "ammo_pebble_shot_45", () -> new CatapultAmmoItem(CatapultAmmoType.PEBBLE_45)
   );
   public static final RegistryObject<Item> AMMO_CLUSTER_SHOT_10 = REGISTER.register(
      "ammo_cluster_shot_10", () -> new CatapultAmmoItem(CatapultAmmoType.CLUSTER_10)
   );
   public static final RegistryObject<Item> AMMO_CLUSTER_SHOT_15 = REGISTER.register(
      "ammo_cluster_shot_15", () -> new CatapultAmmoItem(CatapultAmmoType.CLUSTER_15)
   );
   public static final RegistryObject<Item> AMMO_CLUSTER_SHOT_30 = REGISTER.register(
      "ammo_cluster_shot_30", () -> new CatapultAmmoItem(CatapultAmmoType.CLUSTER_30)
   );
   public static final RegistryObject<Item> AMMO_CLUSTER_SHOT_45 = REGISTER.register(
      "ammo_cluster_shot_45", () -> new CatapultAmmoItem(CatapultAmmoType.CLUSTER_45)
   );
   public static final RegistryObject<Item> AMMO_EXPLOSIVE_SHOT_10 = REGISTER.register(
      "ammo_explosive_shot_10", () -> new CatapultAmmoItem(CatapultAmmoType.EXPLOSIVE_10)
   );
   public static final RegistryObject<Item> AMMO_EXPLOSIVE_SHOT_15 = REGISTER.register(
      "ammo_explosive_shot_15", () -> new CatapultAmmoItem(CatapultAmmoType.EXPLOSIVE_15)
   );
   public static final RegistryObject<Item> AMMO_EXPLOSIVE_SHOT_30 = REGISTER.register(
      "ammo_explosive_shot_30", () -> new CatapultAmmoItem(CatapultAmmoType.EXPLOSIVE_30)
   );
   public static final RegistryObject<Item> AMMO_EXPLOSIVE_SHOT_45 = REGISTER.register(
      "ammo_explosive_shot_45", () -> new CatapultAmmoItem(CatapultAmmoType.EXPLOSIVE_45)
   );
   public static final RegistryObject<Item> AMMO_HIGH_EXPLOSIVE_SHOT_10 = REGISTER.register(
      "ammo_explosive_shot_10_big", () -> new CatapultAmmoItem(CatapultAmmoType.HIGH_EXPLOSIVE_10)
   );
   public static final RegistryObject<Item> AMMO_HIGH_EXPLOSIVE_SHOT_15 = REGISTER.register(
      "ammo_explosive_shot_15_big", () -> new CatapultAmmoItem(CatapultAmmoType.HIGH_EXPLOSIVE_15)
   );
   public static final RegistryObject<Item> AMMO_HIGH_EXPLOSIVE_SHOT_30 = REGISTER.register(
      "ammo_explosive_shot_30_big", () -> new CatapultAmmoItem(CatapultAmmoType.HIGH_EXPLOSIVE_30)
   );
   public static final RegistryObject<Item> AMMO_HIGH_EXPLOSIVE_SHOT_45 = REGISTER.register(
      "ammo_explosive_shot_45_big", () -> new CatapultAmmoItem(CatapultAmmoType.HIGH_EXPLOSIVE_45)
   );
   public static final RegistryObject<Item> AMMO_NAPALM_SHOT_10 = REGISTER.register(
      "ammo_napalm_shot_10", () -> new CatapultAmmoItem(CatapultAmmoType.NAPALM_10)
   );
   public static final RegistryObject<Item> AMMO_NAPALM_SHOT_15 = REGISTER.register(
      "ammo_napalm_shot_15", () -> new CatapultAmmoItem(CatapultAmmoType.NAPALM_15)
   );
   public static final RegistryObject<Item> AMMO_NAPALM_SHOT_30 = REGISTER.register(
      "ammo_napalm_shot_30", () -> new CatapultAmmoItem(CatapultAmmoType.NAPALM_30)
   );
   public static final RegistryObject<Item> AMMO_NAPALM_SHOT_45 = REGISTER.register(
      "ammo_napalm_shot_45", () -> new CatapultAmmoItem(CatapultAmmoType.NAPALM_45)
   );
   public static final RegistryObject<Item> WORKER_NPC_SPAWN_EGG = REGISTER.register(
      "worker_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.WORKER_NPC, 8016432, 12165226, new Properties())
   );
   public static final RegistryObject<Item> COMBAT_NPC_SPAWN_EGG = REGISTER.register(
      "combat_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.COMBAT_NPC, 6241069, 12071466, new Properties())
   );
   public static final RegistryObject<Item> COURIER_NPC_SPAWN_EGG = REGISTER.register(
      "courier_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.COURIER_NPC, 5007941, 12884566, new Properties())
   );
   public static final RegistryObject<Item> TRADER_NPC_SPAWN_EGG = REGISTER.register(
      "trader_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.TRADER_NPC, 6966838, 14004589, new Properties())
   );
   public static final RegistryObject<Item> BARD_NPC_SPAWN_EGG = REGISTER.register(
      "bard_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.BARD_NPC, 7359626, 14073950, new Properties())
   );
   public static final RegistryObject<Item> PRIEST_NPC_SPAWN_EGG = REGISTER.register(
      "priest_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.PRIEST_NPC, 13157024, 7232059, new Properties())
   );
   public static final RegistryObject<Item> SIEGE_ENGINEER_NPC_SPAWN_EGG = REGISTER.register(
      "siege_engineer_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.SIEGE_ENGINEER_NPC, 4868674, 13144378, new Properties())
   );
   public static final RegistryObject<Item> FACTION_SOLDIER_NPC_SPAWN_EGG = REGISTER.register(
      "faction_soldier_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.FACTION_SOLDIER_NPC, 3877412, 11547947, new Properties())
   );
   public static final RegistryObject<Item> FACTION_SOLDIER_ELITE_NPC_SPAWN_EGG = REGISTER.register(
      "faction_soldier_elite_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.FACTION_SOLDIER_ELITE_NPC, 2760475, 13666866, new Properties())
   );
   public static final RegistryObject<Item> FACTION_ARCHER_NPC_SPAWN_EGG = REGISTER.register(
      "faction_archer_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.FACTION_ARCHER_NPC, 2962213, 9067058, new Properties())
   );
   public static final RegistryObject<Item> FACTION_ARCHER_ELITE_NPC_SPAWN_EGG = REGISTER.register(
      "faction_archer_elite_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.FACTION_ARCHER_ELITE_NPC, 1975323, 13666866, new Properties())
   );
   public static final RegistryObject<Item> FACTION_LEADER_NPC_SPAWN_EGG = REGISTER.register(
      "faction_leader_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.FACTION_LEADER_NPC, 3810869, 14073950, new Properties())
   );
   public static final RegistryObject<Item> FACTION_LEADER_ELITE_NPC_SPAWN_EGG = REGISTER.register(
      "faction_leader_elite_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.FACTION_LEADER_ELITE_NPC, 2431275, 15779678, new Properties())
   );
   public static final RegistryObject<Item> FACTION_CIVILIAN_MALE_NPC_SPAWN_EGG = REGISTER.register(
      "faction_civilian_male_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.FACTION_CIVILIAN_MALE_NPC, 6965812, 11897698, new Properties())
   );
   public static final RegistryObject<Item> FACTION_CIVILIAN_FEMALE_NPC_SPAWN_EGG = REGISTER.register(
      "faction_civilian_female_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.FACTION_CIVILIAN_FEMALE_NPC, 6176050, 12021112, new Properties())
   );
   public static final RegistryObject<Item> FACTION_PRIEST_NPC_SPAWN_EGG = REGISTER.register(
      "faction_priest_npc_spawn_egg", () -> new ForgeSpawnEggItem(AWCoreEntityTypes.FACTION_PRIEST_NPC, 13157024, 7232059, new Properties())
   );
   public static final RegistryObject<Item> NPC_SPAWNER = REGISTER.register("npc_spawner", NpcSpawnerItem::new);
   public static final RegistryObject<Item> WOODEN_COMMAND_BATON = REGISTER.register("wooden_command_baton", () -> new CommandBatonItem(Tiers.WOOD));
   public static final RegistryObject<Item> STONE_COMMAND_BATON = REGISTER.register("stone_command_baton", () -> new CommandBatonItem(Tiers.STONE));
   public static final RegistryObject<Item> IRON_COMMAND_BATON = REGISTER.register("iron_command_baton", () -> new CommandBatonItem(Tiers.IRON));
   public static final RegistryObject<Item> GOLD_COMMAND_BATON = REGISTER.register("gold_command_baton", () -> new CommandBatonItem(Tiers.GOLD));
   public static final RegistryObject<Item> DIAMOND_COMMAND_BATON = REGISTER.register("diamond_command_baton", () -> new CommandBatonItem(Tiers.DIAMOND));
   public static final RegistryObject<Item> WORK_ORDER = REGISTER.register("work_order", WorkOrderItem::new);
   public static final RegistryObject<Item> ROUTING_ORDER = REGISTER.register("routing_order", RoutingOrderItem::new);
   public static final RegistryObject<Item> TRADE_ORDER = REGISTER.register("trade_order", TradeOrderItem::new);
   public static final RegistryObject<Item> UPKEEP_ORDER = REGISTER.register("upkeep_order", UpkeepOrderItem::new);
   public static final RegistryObject<Item> COMBAT_ORDER = REGISTER.register("combat_order", CombatOrderItem::new);
   public static final RegistryObject<Item> WOODEN_SPEAR = npcWeapon("wooden_spear", Tiers.WOOD, 2.0, -3.0, 4.2F);
   public static final RegistryObject<Item> STONE_SPEAR = npcWeapon("stone_spear", Tiers.STONE, 2.0, -3.0, 4.2F);
   public static final RegistryObject<Item> IRON_SPEAR = npcWeapon("iron_spear", Tiers.IRON, 2.0, -3.0, 4.2F);
   public static final RegistryObject<Item> GOLDEN_SPEAR = npcWeapon("golden_spear", Tiers.GOLD, 2.0, -3.0, 4.2F);
   public static final RegistryObject<Item> DIAMOND_SPEAR = npcWeapon("diamond_spear", Tiers.DIAMOND, 2.0, -3.0, 4.2F);
   public static final RegistryObject<Item> OBSIDIAN_SPEAR = npcWeapon("obsidian_spear", Tiers.DIAMOND, 2.0, -3.0, 4.2F);
   public static final RegistryObject<Item> ICE_SPEAR = npcWeapon("ice_spear", Tiers.DIAMOND, 2.0, -3.0, 4.2F, NpcWeaponEffect.ICE);
   public static final RegistryObject<Item> WOODEN_HALBERD = npcWeapon("wooden_halberd", Tiers.WOOD, 3.0, -3.2, 4.5F);
   public static final RegistryObject<Item> STONE_HALBERD = npcWeapon("stone_halberd", Tiers.STONE, 3.0, -3.2, 4.5F);
   public static final RegistryObject<Item> IRON_HALBERD = npcWeapon("iron_halberd", Tiers.IRON, 3.0, -3.2, 4.5F);
   public static final RegistryObject<Item> GOLDEN_HALBERD = npcWeapon("golden_halberd", Tiers.GOLD, 3.0, -3.2, 4.5F);
   public static final RegistryObject<Item> DIAMOND_HALBERD = npcWeapon("diamond_halberd", Tiers.DIAMOND, 3.0, -3.2, 4.5F);
   public static final RegistryObject<Item> WOODEN_LANCE = npcWeapon("wooden_lance", Tiers.WOOD, 2.5, -3.2, 5.5F);
   public static final RegistryObject<Item> STONE_LANCE = npcWeapon("stone_lance", Tiers.STONE, 2.5, -3.2, 5.5F);
   public static final RegistryObject<Item> IRON_LANCE = npcWeapon("iron_lance", Tiers.IRON, 2.5, -3.2, 5.5F);
   public static final RegistryObject<Item> GOLDEN_LANCE = npcWeapon("golden_lance", Tiers.GOLD, 2.5, -3.2, 5.5F);
   public static final RegistryObject<Item> DIAMOND_LANCE = npcWeapon("diamond_lance", Tiers.DIAMOND, 2.5, -3.2, 5.5F);
   public static final RegistryObject<Item> WOODEN_CLEAVER = npcWeapon("wooden_cleaver", Tiers.WOOD, 3.5, -2.8, 3.0F);
   public static final RegistryObject<Item> STONE_CLEAVER = npcWeapon("stone_cleaver", Tiers.STONE, 3.5, -2.8, 3.0F);
   public static final RegistryObject<Item> IRON_CLEAVER = npcWeapon("iron_cleaver", Tiers.IRON, 3.5, -2.8, 3.0F);
   public static final RegistryObject<Item> GOLDEN_CLEAVER = npcWeapon("golden_cleaver", Tiers.GOLD, 3.5, -2.8, 3.0F);
   public static final RegistryObject<Item> DIAMOND_CLEAVER = npcWeapon("diamond_cleaver", Tiers.DIAMOND, 3.5, -2.8, 3.0F);
   public static final RegistryObject<Item> SICKLE = npcWeapon("sickle", Tiers.IRON, 0.0, -2.3, 3.0F);
   public static final RegistryObject<Item> PITCHFORK = npcWeapon("pitchfork", Tiers.IRON, 0.0, -2.3, 3.0F);
   public static final RegistryObject<Item> SCYTHE = npcWeapon("scythe", Tiers.IRON, 0.0, -2.3, 3.0F);
   public static final RegistryObject<Item> DEATH_SCYTHE = npcWeapon("death_scythe", Tiers.DIAMOND, 0.0, -2.3, 3.0F, NpcWeaponEffect.WITHER);
   public static final RegistryObject<Item> GIANT_CLUB = npcWeapon("giant_club", Tiers.DIAMOND, 3.5, -3.6, 4.2F, NpcWeaponEffect.CLUB_KNOCKBACK);
   public static final RegistryObject<Item> MACUAHUITL = npcWeapon("macuahuitl", Tiers.IRON, 3.0, -2.4, 3.0F);
   public static final RegistryObject<Item> WOODEN_SHIELD = npcShield("wooden_shield", Tiers.WOOD, 336);
   public static final RegistryObject<Item> STONE_SHIELD = npcShield("stone_shield", Tiers.STONE, 506);
   public static final RegistryObject<Item> IRON_SHIELD = npcShield("iron_shield", Tiers.IRON, 759);
   public static final RegistryObject<Item> GOLD_SHIELD = npcShield("gold_shield", Tiers.GOLD, 64);
   public static final RegistryObject<Item> DIAMOND_SHIELD = npcShield("diamond_shield", Tiers.DIAMOND, 1138);
   public static final RegistryObject<Item> SHIELD_TRIBAL_1 = npcShield("shield_tribal_1", Tiers.WOOD, 336);
   public static final RegistryObject<Item> SHIELD_TRIBAL_2 = npcShield("shield_tribal_2", Tiers.WOOD, 336);
   public static final RegistryObject<Item> SHIELD_ROUND_1 = npcShield("shield_round_1", Tiers.WOOD, 336);
   public static final RegistryObject<Item> SHIELD_ROUND_2 = npcShield("shield_round_2", Tiers.WOOD, 336);
   public static final RegistryObject<Item> SHIELD_ROUND_3 = npcShield("shield_round_3", Tiers.WOOD, 336);
   public static final RegistryObject<Item> SHIELD_ROUND_4 = npcShield("shield_round_4", Tiers.WOOD, 336);
   public static final RegistryObject<Item> SHIELD_ROUND_5 = npcShield("shield_round_5", Tiers.WOOD, 336);
   public static final RegistryObject<Item> SHIELD_ROUND_6 = npcShield("shield_round_6", Tiers.WOOD, 336);
   public static final RegistryObject<Item> SHIELD_WITCHBANE_1 = npcShield("shield_witchbane_1", Tiers.IRON, 759);
   public static final RegistryObject<Item> SHIELD_WITCHBANE_2 = npcShield("shield_witchbane_2", Tiers.IRON, 759);
   public static final RegistryObject<Item> SHIELD_BUFFLOKA = npcShield("shield_buffloka", Tiers.WOOD, 336);
   public static final RegistryObject<Item> COIN = REGISTER.register("coin", CoinItem::new);
   public static final RegistryObject<Item> FOOD_BUNDLE = REGISTER.register(
      "food_bundle", () -> new Item(new Properties().food(new Builder().nutrition(15).saturationMod(1.0F).meat().build()))
   );
   public static final RegistryObject<Item> BARD_INSTRUMENT = REGISTER.register("bard_instrument", BardInstrumentItem::new);
   public static final RegistryObject<Item> GATE_SPAWNER = REGISTER.register("gate_spawner", GateSpawnerItem::new);
   public static final RegistryObject<Item> GATE_SPAWNER_IRON = REGISTER.register("gate_spawner_iron", () -> new GateSpawnerItem(GateEntity.Material.IRON));
   public static final RegistryObject<Item> GATE_SPAWNER_SINGLE_WOOD = REGISTER.register(
      "gate_spawner_single_wood", () -> new GateSpawnerItem(GateEntity.Material.WOOD, GateEntity.OpeningStyle.SINGLE_SLIDING)
   );
   public static final RegistryObject<Item> GATE_SPAWNER_SINGLE_IRON = REGISTER.register(
      "gate_spawner_single_iron", () -> new GateSpawnerItem(GateEntity.Material.IRON, GateEntity.OpeningStyle.SINGLE_SLIDING)
   );
   public static final RegistryObject<Item> GATE_SPAWNER_DOUBLE_WOOD = REGISTER.register(
      "gate_spawner_double_wood", () -> new GateSpawnerItem(GateEntity.Material.WOOD, GateEntity.OpeningStyle.DOUBLE_SLIDING)
   );
   public static final RegistryObject<Item> GATE_SPAWNER_DOUBLE_IRON = REGISTER.register(
      "gate_spawner_double_iron", () -> new GateSpawnerItem(GateEntity.Material.IRON, GateEntity.OpeningStyle.DOUBLE_SLIDING)
   );
   public static final RegistryObject<Item> GATE_SPAWNER_DRAWBRIDGE = REGISTER.register(
      "gate_spawner_drawbridge", () -> new GateSpawnerItem(GateEntity.Material.WOOD, GateEntity.OpeningStyle.DRAWBRIDGE)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_SIZE_MEDIUM = REGISTER.register(
      "worksite_upgrade_size_medium", () -> new WorksiteUpgradeItem(WorksiteUpgrade.SIZE_MEDIUM)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_SIZE_LARGE = REGISTER.register(
      "worksite_upgrade_size_large", () -> new WorksiteUpgradeItem(WorksiteUpgrade.SIZE_LARGE)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_QUARRY_MEDIUM = REGISTER.register(
      "worksite_upgrade_quarry_medium", () -> new WorksiteUpgradeItem(WorksiteUpgrade.QUARRY_MEDIUM)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_QUARRY_LARGE = REGISTER.register(
      "worksite_upgrade_quarry_large", () -> new WorksiteUpgradeItem(WorksiteUpgrade.QUARRY_LARGE)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_ENCHANTED_TOOLS_1 = REGISTER.register(
      "worksite_upgrade_enchanted_tools_1", () -> new WorksiteUpgradeItem(WorksiteUpgrade.ENCHANTED_TOOLS_1)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_ENCHANTED_TOOLS_2 = REGISTER.register(
      "worksite_upgrade_enchanted_tools_2", () -> new WorksiteUpgradeItem(WorksiteUpgrade.ENCHANTED_TOOLS_2)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_TOOL_QUALITY_1 = REGISTER.register(
      "worksite_upgrade_tool_quality_1", () -> new WorksiteUpgradeItem(WorksiteUpgrade.TOOL_QUALITY_1)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_TOOL_QUALITY_2 = REGISTER.register(
      "worksite_upgrade_tool_quality_2", () -> new WorksiteUpgradeItem(WorksiteUpgrade.TOOL_QUALITY_2)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_TOOL_QUALITY_3 = REGISTER.register(
      "worksite_upgrade_tool_quality_3", () -> new WorksiteUpgradeItem(WorksiteUpgrade.TOOL_QUALITY_3)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_BASIC_CHUNK_LOADER = REGISTER.register(
      "worksite_upgrade_basic_chunk_loader", () -> new WorksiteUpgradeItem(WorksiteUpgrade.BASIC_CHUNK_LOADER)
   );
   public static final RegistryObject<Item> WORKSITE_UPGRADE_QUARRY_CHUNK_LOADER = REGISTER.register(
      "worksite_upgrade_quarry_chunk_loader", () -> new WorksiteUpgradeItem(WorksiteUpgrade.QUARRY_CHUNK_LOADER)
   );

   private AWCoreItems() {
   }

   private static RegistryObject<Item> simple(String name) {
      return REGISTER.register(name, () -> new Item(new Properties()));
   }

   private static RegistryObject<Item> npcWeapon(String name, Tier tier, double attackOffset, double attackSpeed, float reach) {
      return npcWeapon(name, tier, attackOffset, attackSpeed, reach, NpcWeaponEffect.NONE);
   }

   private static RegistryObject<Item> npcWeapon(String name, Tier tier, double attackOffset, double attackSpeed, float reach, NpcWeaponEffect effect) {
      return REGISTER.register(name, () -> new NpcWeaponItem(tier, attackOffset, attackSpeed, reach, effect, new Properties()));
   }

   private static RegistryObject<Item> npcShield(String name, Tier tier, int durability) {
      return REGISTER.register(name, () -> new NpcShieldItem(tier, durability));
   }
}
