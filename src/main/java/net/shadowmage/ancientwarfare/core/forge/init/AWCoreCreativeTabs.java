package net.shadowmage.ancientwarfare.core.forge.init;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.block.CoinStackBlock;
import net.shadowmage.ancientwarfare.core.common.npc.BardInstrumentType;
import net.shadowmage.ancientwarfare.core.common.npc.CoinMetal;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.item.BackpackItem;
import net.shadowmage.ancientwarfare.core.item.BardInstrumentItem;
import net.shadowmage.ancientwarfare.core.item.CoinItem;
import net.shadowmage.ancientwarfare.core.item.NpcSpawnerItem;
import net.shadowmage.ancientwarfare.core.item.ProtectionFlagBlockItem;
import net.shadowmage.ancientwarfare.core.item.ResearchNoteItem;
import net.shadowmage.ancientwarfare.core.item.SurvivalStructureBuilderItem;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplateBlockItem;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplateIndex;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplatePlacer;
import net.shadowmage.ancientwarfare.core.structure.AwtTemplateIndex;
import net.shadowmage.ancientwarfare.core.structure.AwtTownBlockItem;

public final class AWCoreCreativeTabs {
   public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.f_279569_, "ancientwarfare");
   public static final RegistryObject<CreativeModeTab> CORE = REGISTER.register(
      "core",
      () -> CreativeModeTab.builder()
            .m_257941_(Component.m_237115_("itemGroup.ancientwarfare.core"))
            .m_257737_(() -> ((Item)AWCoreItems.MANUAL.get()).m_7968_())
            .m_257501_((parameters, output) -> {
               output.m_246326_((ItemLike)AWCoreItems.MANUAL.get());
               output.m_246326_((ItemLike)AWCoreItems.INFO_TOOL.get());
               output.m_246326_((ItemLike)AWCoreItems.RESEARCH_BOOK.get());

               for (ResearchGoal goal : ResearchRegistry.all()) {
                  output.m_246342_(ResearchNoteItem.create(goal.id()));
               }

               for (int level = 0; level <= 3; level++) {
                  output.m_246342_(BackpackItem.create(level));
               }

               output.m_246326_((ItemLike)AWCoreItems.STEEL_INGOT.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_QUILL.get());
               output.m_246326_((ItemLike)AWCoreItems.STONE_QUILL.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_QUILL.get());
               output.m_246326_((ItemLike)AWCoreItems.GOLD_QUILL.get());
               output.m_246326_((ItemLike)AWCoreItems.DIAMOND_QUILL.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_HAMMER.get());
               output.m_246326_((ItemLike)AWCoreItems.STONE_HAMMER.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_HAMMER.get());
               output.m_246326_((ItemLike)AWCoreItems.GOLD_HAMMER.get());
               output.m_246326_((ItemLike)AWCoreItems.DIAMOND_HAMMER.get());
               output.m_246326_((ItemLike)AWCoreBlocks.ENGINEERING_STATION.get());
               output.m_246326_((ItemLike)AWCoreBlocks.MAILBOX.get());
               output.m_246326_((ItemLike)AWCoreBlocks.RESEARCH_STATION.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_GEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_GEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.STEEL_GEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_SHAFT.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_SHAFT.get());
               output.m_246326_((ItemLike)AWCoreItems.STEEL_SHAFT.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_BEARINGS.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_BEARINGS.get());
               output.m_246326_((ItemLike)AWCoreItems.STEEL_BEARINGS.get());
            })
            .m_257652_()
   );
   public static final RegistryObject<CreativeModeTab> VEHICLES = REGISTER.register(
      "vehicles",
      () -> CreativeModeTab.builder()
            .m_257941_(Component.m_237115_("itemGroup.ancientwarfare.vehicles"))
            .m_257737_(() -> ((Item)AWCoreItems.CHEST_PUSHCART.get()).m_7968_())
            .m_257501_((parameters, output) -> {
               output.m_246326_((ItemLike)AWCoreItems.CHEST_PUSHCART.get());
               output.m_246326_((ItemLike)AWCoreItems.CHEST_PUSHCART_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.CHEST_PUSHCART_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.CHEST_PUSHCART_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.CHEST_PUSHCART_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_BOAT.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_BOAT_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_BOAT_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_BOAT_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_BOAT_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_MOBILE.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_MOBILE_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_MOBILE_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_MOBILE_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_MOBILE_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_FIXED.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_FIXED_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_FIXED_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_FIXED_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_FIXED_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_TURRET.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_TURRET_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_TURRET_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_TURRET_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.TREBUCHET_TURRET_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.GIANT_TREBUCHET.get());
               output.m_246326_((ItemLike)AWCoreItems.GIANT_TREBUCHET_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.GIANT_TREBUCHET_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.GIANT_TREBUCHET_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.GIANT_TREBUCHET_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_BOAT.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_BOAT_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_BOAT_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_BOAT_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.CATAPULT_BOAT_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.TRANSPORT_BOAT.get());
               output.m_246326_((ItemLike)AWCoreItems.TRANSPORT_BOAT_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.TRANSPORT_BOAT_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.TRANSPORT_BOAT_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.TRANSPORT_BOAT_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_TURRET_STAND.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_TURRET_STAND_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_TURRET_STAND_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_TURRET_STAND_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_TURRET_STAND_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_STAND_FIXED.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_STAND_FIXED_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_STAND_FIXED_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_STAND_FIXED_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_STAND_FIXED_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_MOBILE.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_MOBILE_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_MOBILE_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_MOBILE_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.CANNON_MOBILE_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.BATTERING_RAM.get());
               output.m_246326_((ItemLike)AWCoreItems.BATTERING_RAM_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.BATTERING_RAM_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.BATTERING_RAM_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.BATTERING_RAM_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.HWACHA.get());
               output.m_246326_((ItemLike)AWCoreItems.HWACHA_IRON_BAND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.HWACHA_IRON_BAND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.HWACHA_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.HWACHA_STEEL.get());
               output.m_246326_((ItemLike)AWCoreItems.FLAME_CHARGE.get());
               output.m_246326_((ItemLike)AWCoreItems.EXPLOSIVE_CHARGE.get());
               output.m_246326_((ItemLike)AWCoreItems.ROCKET_CHARGE.get());
               output.m_246326_((ItemLike)AWCoreItems.CLUSTER_CHARGE.get());
               output.m_246326_((ItemLike)AWCoreItems.NAPALM_CHARGE.get());
               output.m_246326_((ItemLike)AWCoreItems.CLAY_CASING.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_CASING.get());
               output.m_246326_((ItemLike)AWCoreItems.MOBILITY_UNIT.get());
               output.m_246326_((ItemLike)AWCoreItems.TURRET_COMPONENTS.get());
               output.m_246326_((ItemLike)AWCoreItems.TORSION_UNIT.get());
               output.m_246326_((ItemLike)AWCoreItems.COUNTER_WEIGHT_UNIT.get());
               output.m_246326_((ItemLike)AWCoreItems.POWDER_CASE.get());
               output.m_246326_((ItemLike)AWCoreItems.EQUIPMENT_BAY.get());
               output.m_246326_((ItemLike)AWCoreItems.ROUGH_WOOD.get());
               output.m_246326_((ItemLike)AWCoreItems.TREATED_WOOD.get());
               output.m_246326_((ItemLike)AWCoreItems.IRONSHOD_WOOD.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_CORE_WOOD.get());
               output.m_246326_((ItemLike)AWCoreItems.ROUGH_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.FINE_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.TEMPERED_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.MINOR_ALLOY.get());
               output.m_246326_((ItemLike)AWCoreItems.MAJOR_ALLOY.get());
               output.m_246326_((ItemLike)AWCoreItems.ARMOR_STONE.get());
               output.m_246326_((ItemLike)AWCoreItems.ARMOR_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.ARMOR_OBSIDIAN.get());
               output.m_246326_((ItemLike)AWCoreItems.VEHICLE_UPGRADE_SPEED.get());
               output.m_246326_((ItemLike)AWCoreItems.VEHICLE_UPGRADE_AIM.get());
               output.m_246326_((ItemLike)AWCoreItems.VEHICLE_UPGRADE_RELOAD.get());
               output.m_246326_((ItemLike)AWCoreItems.VEHICLE_UPGRADE_POWER.get());
               output.m_246326_((ItemLike)AWCoreItems.VEHICLE_UPGRADE_TURRET_PITCH.get());
               output.m_246326_((ItemLike)AWCoreItems.VEHICLE_UPGRADE_PITCH_UP.get());
               output.m_246326_((ItemLike)AWCoreItems.VEHICLE_UPGRADE_PITCH_DOWN.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_STONE_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_STONE_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_STONE_SHOT_30.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_STONE_SHOT_45.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_FLAME_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_FLAME_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_FLAME_SHOT_30.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_FLAME_SHOT_45.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_PEBBLE_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_PEBBLE_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_PEBBLE_SHOT_30.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_PEBBLE_SHOT_45.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_CLUSTER_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_CLUSTER_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_CLUSTER_SHOT_30.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_CLUSTER_SHOT_45.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_EXPLOSIVE_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_EXPLOSIVE_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_EXPLOSIVE_SHOT_30.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_EXPLOSIVE_SHOT_45.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_HIGH_EXPLOSIVE_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_HIGH_EXPLOSIVE_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_HIGH_EXPLOSIVE_SHOT_30.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_HIGH_EXPLOSIVE_SHOT_45.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_NAPALM_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_NAPALM_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_NAPALM_SHOT_30.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_NAPALM_SHOT_45.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_BALLISTA_BOLT.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_BALLISTA_BOLT_IRON.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_BALLISTA_BOLT_FLAME.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_BALLISTA_BOLT_EXPLOSIVE.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_HWACHA_ROCKET.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_HWACHA_ROCKET_FLAME.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_HWACHA_ROCKET_EXPLOSIVE.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_HWACHA_ROCKET_AIRBURST.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_IRON_SHOT_5.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_IRON_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_IRON_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_IRON_SHOT_25.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_GRAPE_SHOT_5.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_GRAPE_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_GRAPE_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_GRAPE_SHOT_25.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_CANISTER_SHOT_5.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_CANISTER_SHOT_10.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_CANISTER_SHOT_15.get());
               output.m_246326_((ItemLike)AWCoreItems.AMMO_CANISTER_SHOT_25.get());
            })
            .m_257652_()
   );
   public static final RegistryObject<CreativeModeTab> NPC = REGISTER.register(
      "npc",
      () -> CreativeModeTab.builder()
            .m_257941_(Component.m_237115_("itemGroup.ancientwarfare.npc"))
            .m_257737_(NpcSpawnerItem::createWorker)
            .m_257501_((parameters, output) -> {
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_COMMAND_BATON.get());
               output.m_246326_((ItemLike)AWCoreItems.STONE_COMMAND_BATON.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_COMMAND_BATON.get());
               output.m_246326_((ItemLike)AWCoreItems.GOLD_COMMAND_BATON.get());
               output.m_246326_((ItemLike)AWCoreItems.DIAMOND_COMMAND_BATON.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TOWN_HALL.get());
               output.m_246326_((ItemLike)AWCoreItems.WORK_ORDER.get());
               output.m_246326_((ItemLike)AWCoreItems.ROUTING_ORDER.get());
               output.m_246326_((ItemLike)AWCoreItems.TRADE_ORDER.get());
               output.m_246326_((ItemLike)AWCoreItems.UPKEEP_ORDER.get());
               output.m_246326_((ItemLike)AWCoreItems.COMBAT_ORDER.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_SPEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.STONE_SPEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_SPEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.GOLDEN_SPEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.DIAMOND_SPEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.OBSIDIAN_SPEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.ICE_SPEAR.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_HALBERD.get());
               output.m_246326_((ItemLike)AWCoreItems.STONE_HALBERD.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_HALBERD.get());
               output.m_246326_((ItemLike)AWCoreItems.GOLDEN_HALBERD.get());
               output.m_246326_((ItemLike)AWCoreItems.DIAMOND_HALBERD.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_LANCE.get());
               output.m_246326_((ItemLike)AWCoreItems.STONE_LANCE.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_LANCE.get());
               output.m_246326_((ItemLike)AWCoreItems.GOLDEN_LANCE.get());
               output.m_246326_((ItemLike)AWCoreItems.DIAMOND_LANCE.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_CLEAVER.get());
               output.m_246326_((ItemLike)AWCoreItems.STONE_CLEAVER.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_CLEAVER.get());
               output.m_246326_((ItemLike)AWCoreItems.GOLDEN_CLEAVER.get());
               output.m_246326_((ItemLike)AWCoreItems.DIAMOND_CLEAVER.get());
               output.m_246326_((ItemLike)AWCoreItems.SICKLE.get());
               output.m_246326_((ItemLike)AWCoreItems.PITCHFORK.get());
               output.m_246326_((ItemLike)AWCoreItems.SCYTHE.get());
               output.m_246326_((ItemLike)AWCoreItems.DEATH_SCYTHE.get());
               output.m_246326_((ItemLike)AWCoreItems.GIANT_CLUB.get());
               output.m_246326_((ItemLike)AWCoreItems.MACUAHUITL.get());
               output.m_246326_((ItemLike)AWCoreItems.WOODEN_SHIELD.get());
               output.m_246326_((ItemLike)AWCoreItems.STONE_SHIELD.get());
               output.m_246326_((ItemLike)AWCoreItems.IRON_SHIELD.get());
               output.m_246326_((ItemLike)AWCoreItems.GOLD_SHIELD.get());
               output.m_246326_((ItemLike)AWCoreItems.DIAMOND_SHIELD.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_TRIBAL_1.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_TRIBAL_2.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_ROUND_1.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_ROUND_2.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_ROUND_3.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_ROUND_4.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_ROUND_5.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_ROUND_6.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_WITCHBANE_1.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_WITCHBANE_2.get());
               output.m_246326_((ItemLike)AWCoreItems.SHIELD_BUFFLOKA.get());

               for (CoinMetal metal : CoinMetal.values()) {
                  output.m_246342_(CoinItem.create(metal));
               }

               output.m_246326_((ItemLike)AWCoreItems.FOOD_BUNDLE.get());

               for (BardInstrumentType type : BardInstrumentType.values()) {
                  output.m_246342_(BardInstrumentItem.create(type));
               }

               output.m_246342_(NpcSpawnerItem.createWorker());
               output.m_246342_(NpcSpawnerItem.createCombat());
               output.m_246342_(NpcSpawnerItem.createCourier());
               output.m_246342_(NpcSpawnerItem.createTrader());
               output.m_246342_(NpcSpawnerItem.createBard());
               output.m_246342_(NpcSpawnerItem.createPriest());
               output.m_246342_(NpcSpawnerItem.createSiegeEngineer());

               for (ItemStack stack : NpcSpawnerItem.createFactionCreativeStacks()) {
                  output.m_246342_(stack);
               }
            })
            .m_257652_()
   );
   public static final RegistryObject<CreativeModeTab> AUTOMATION = REGISTER.register(
      "automation",
      () -> CreativeModeTab.builder()
            .m_257941_(Component.m_237115_("itemGroup.ancientwarfare.automation"))
            .m_257737_(() -> ((Block)AWCoreBlocks.CROP_FARM.get()).m_5456_().m_7968_())
            .m_257501_((parameters, output) -> {
               output.m_246326_((ItemLike)AWCoreBlocks.CROP_FARM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.ANIMAL_FARM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TREE_FARM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.FISH_FARM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.FRUIT_FARM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.QUARRY.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WAREHOUSE_CONTROL.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WAREHOUSE_STORAGE_SMALL.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WAREHOUSE_STORAGE_MEDIUM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WAREHOUSE_STORAGE_LARGE.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WAREHOUSE_INTERFACE.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WAREHOUSE_CRAFTING.get());
               output.m_246326_((ItemLike)AWCoreBlocks.AUTO_CRAFTING.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WAREHOUSE_STOCK_VIEWER.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WAREHOUSE_STOCK_LINKER.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TORQUE_SHAFT_LIGHT.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TORQUE_SHAFT_MEDIUM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TORQUE_SHAFT_HEAVY.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TORQUE_JUNCTION_LIGHT.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TORQUE_JUNCTION_MEDIUM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TORQUE_JUNCTION_HEAVY.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TORQUE_DISTRIBUTOR_LIGHT.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TORQUE_DISTRIBUTOR_MEDIUM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.TORQUE_DISTRIBUTOR_HEAVY.get());
               output.m_246326_((ItemLike)AWCoreBlocks.HAND_CRANKED_GENERATOR.get());
               output.m_246326_((ItemLike)AWCoreBlocks.STIRLING_GENERATOR.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WATERWHEEL_GENERATOR.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WINDMILL_GENERATOR.get());
               output.m_246326_((ItemLike)AWCoreBlocks.WINDMILL_BLADE.get());
               output.m_246326_((ItemLike)AWCoreBlocks.CHUNK_LOADER_SIMPLE.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_SIZE_MEDIUM.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_SIZE_LARGE.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_QUARRY_MEDIUM.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_QUARRY_LARGE.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_ENCHANTED_TOOLS_1.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_ENCHANTED_TOOLS_2.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_TOOL_QUALITY_1.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_TOOL_QUALITY_2.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_TOOL_QUALITY_3.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_BASIC_CHUNK_LOADER.get());
               output.m_246326_((ItemLike)AWCoreItems.WORKSITE_UPGRADE_QUARRY_CHUNK_LOADER.get());
               output.m_246326_((ItemLike)AWCoreBlocks.FLYWHEEL_CONTROLLER_LIGHT.get());
               output.m_246326_((ItemLike)AWCoreBlocks.FLYWHEEL_CONTROLLER_MEDIUM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.FLYWHEEL_CONTROLLER_HEAVY.get());
               output.m_246326_((ItemLike)AWCoreBlocks.FLYWHEEL_STORAGE_LIGHT.get());
               output.m_246326_((ItemLike)AWCoreBlocks.FLYWHEEL_STORAGE_MEDIUM.get());
               output.m_246326_((ItemLike)AWCoreBlocks.FLYWHEEL_STORAGE_HEAVY.get());
            })
            .m_257652_()
   );
   public static final RegistryObject<CreativeModeTab> STRUCTURE = REGISTER.register(
      "structure",
      () -> CreativeModeTab.builder()
            .m_257941_(Component.m_237115_("itemGroup.ancientwarfare.structure"))
            .m_257737_(() -> ((Block)AWCoreBlocks.SCISSOR_SEAT.get()).m_5456_().m_7968_())
            .m_257501_(
               (parameters, output) -> {
                  output.m_246326_((ItemLike)AWCoreItems.STRUCTURE_SCANNER.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.STRUCTURE_SCANNER_BLOCK.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.DRAFTING_STATION.get());

                  for (AwsTemplatePlacer.TemplateInfo template : AwsTemplatePlacer.survivalTemplates()) {
                     output.m_246342_(SurvivalStructureBuilderItem.forAwsTemplate(template.path(), template.name()));
                  }

                  output.m_246326_((ItemLike)AWCoreBlocks.SCISSOR_SEAT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TRIBAL_STOOL.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.WOODEN_THRONE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GOLDEN_IDOL.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GOLDEN_THRONE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ORC_TOTEM_1.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ORC_TOTEM_2.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ORC_TOTEM_2_LIT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GOBLIN_TOTEM_1.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GOBLIN_TOTEM_2.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GOBLIN_TOTEM_2_LIT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.IRON_CAGE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GIBBET.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.WOODEN_POST.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.STOOL.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TABLE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.CHAIR.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.BENCH.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TRIBAL_CHAIR.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ALTAR_LECTERN.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ALTAR_SUN.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.FIRE_PIT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.FIRE_PIT_LOG.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.FIRE_PIT_SQUARE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.BRAZIER_FLAME.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.BRAZIER_EMBER.get());

                  for (int size = 8; size <= 64; size += 8) {
                     output.m_246342_(((CoinStackBlock)AWCoreBlocks.COIN_STACK_COPPER.get()).stackItem(size));
                     output.m_246342_(((CoinStackBlock)AWCoreBlocks.COIN_STACK_SILVER.get()).stackItem(size));
                     output.m_246342_(((CoinStackBlock)AWCoreBlocks.COIN_STACK_GOLD.get()).stackItem(size));
                     output.m_246342_(((CoinStackBlock)AWCoreBlocks.COIN_STACK_ANCIENT.get()).stackItem(size));
                  }

                  output.m_246326_((ItemLike)AWCoreBlocks.STRETCHING_RACK.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GRAVESTONE_1.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GRAVESTONE_2.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GRAVESTONE_3.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GRAVESTONE_4.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GRAVESTONE_5.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GRAVESTONE_6.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GRAVESTONE_7.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.GRAVESTONE_8.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ALTAR_SHORT_CLOTH.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ALTAR_LONG_CLOTH.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ALTAR_CANDLE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.STAKE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_BASE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_MID.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_MID_ALT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_TOP.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_WINGS.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_IRMINSUL_BASE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_IRMINSUL_MID.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_IRMINSUL_TOP.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_IRMINSUL_SIDE.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_ELEPHANT_BODY.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_ELEPHANT_LEFT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.TOTEM_PART_ELEPHANT_RIGHT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.XOLTEC_IDOL_BODY_LEFT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.XOLTEC_IDOL_BODY_RIGHT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.XOLTEC_IDOL_HEAD_LEFT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.XOLTEC_IDOL_HEAD_RIGHT.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.LOOT_BASKET.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.URN.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.WOODEN_COFFIN.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.STONE_COFFIN.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ADVANCED_LOOT_CHEST.get());
                  output.m_246326_((ItemLike)AWCoreBlocks.ADVANCED_SPAWNER.get());
                  FactionRegistry.factionNames()
                     .stream()
                     .sorted()
                     .forEach(faction -> output.m_246342_(ProtectionFlagBlockItem.create((Block)AWCoreBlocks.PROTECTION_FLAG.get(), faction)));
                  output.m_246326_((ItemLike)AWCoreItems.GATE_SPAWNER.get());
                  output.m_246326_((ItemLike)AWCoreItems.GATE_SPAWNER_IRON.get());
                  output.m_246326_((ItemLike)AWCoreItems.GATE_SPAWNER_SINGLE_WOOD.get());
                  output.m_246326_((ItemLike)AWCoreItems.GATE_SPAWNER_SINGLE_IRON.get());
                  output.m_246326_((ItemLike)AWCoreItems.GATE_SPAWNER_DOUBLE_WOOD.get());
                  output.m_246326_((ItemLike)AWCoreItems.GATE_SPAWNER_DOUBLE_IRON.get());
                  output.m_246326_((ItemLike)AWCoreItems.GATE_SPAWNER_DRAWBRIDGE.get());
               }
            )
            .m_257652_()
   );
   public static final RegistryObject<CreativeModeTab> AWS_TEMPLATES = REGISTER.register(
      "aws_templates",
      () -> CreativeModeTab.builder()
            .m_257941_(Component.m_237115_("itemGroup.ancientwarfare.aws_templates"))
            .m_257737_(() -> AwsTemplateBlockItem.create((Block)AWCoreBlocks.UNDEAD_CEMETERY_FILL_SKULL_1.get(), AwsTemplateIndex.defaultTemplate()))
            .m_257501_((parameters, output) -> {
               List<String> templates = AwsTemplateIndex.allTemplates();
               if (templates.isEmpty()) {
                  output.m_246326_((ItemLike)AWCoreBlocks.UNDEAD_CEMETERY_FILL_SKULL_1.get());
               }

               for (String template : templates) {
                  output.m_246342_(AwsTemplateBlockItem.create((Block)AWCoreBlocks.UNDEAD_CEMETERY_FILL_SKULL_1.get(), template));
               }
            })
            .m_257652_()
   );
   public static final RegistryObject<CreativeModeTab> AWT_TOWNS = REGISTER.register(
      "awt_towns",
      () -> CreativeModeTab.builder()
            .m_257941_(Component.m_237115_("itemGroup.ancientwarfare.awt_towns"))
            .m_257737_(() -> AwtTownBlockItem.create((Block)AWCoreBlocks.AWT_TOWN_MARKER.get(), AwtTemplateIndex.defaultTemplate()))
            .m_257501_((parameters, output) -> {
               List<String> templates = AwtTemplateIndex.allTemplates();
               if (templates.isEmpty()) {
                  output.m_246326_((ItemLike)AWCoreBlocks.AWT_TOWN_MARKER.get());
               }

               for (String template : templates) {
                  output.m_246342_(AwtTownBlockItem.create((Block)AWCoreBlocks.AWT_TOWN_MARKER.get(), template));
               }
            })
            .m_257652_()
   );

   private AWCoreCreativeTabs() {
   }
}
