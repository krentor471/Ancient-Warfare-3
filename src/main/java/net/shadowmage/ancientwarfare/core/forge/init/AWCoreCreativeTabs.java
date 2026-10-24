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
   public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "ancientwarfare");
   public static final RegistryObject<CreativeModeTab> CORE = REGISTER.register(
      "core",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ancientwarfare.core"))
            .icon(() -> ((Item)AWCoreItems.MANUAL.get()).getDefaultInstance())
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)AWCoreItems.MANUAL.get());
               output.accept((ItemLike)AWCoreItems.INFO_TOOL.get());
               output.accept((ItemLike)AWCoreItems.RESEARCH_BOOK.get());

               for (ResearchGoal goal : ResearchRegistry.all()) {
                  output.accept(ResearchNoteItem.create(goal.id()));
               }

               for (int level = 0; level <= 3; level++) {
                  output.accept(BackpackItem.create(level));
               }

               output.accept((ItemLike)AWCoreItems.STEEL_INGOT.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_QUILL.get());
               output.accept((ItemLike)AWCoreItems.STONE_QUILL.get());
               output.accept((ItemLike)AWCoreItems.IRON_QUILL.get());
               output.accept((ItemLike)AWCoreItems.GOLD_QUILL.get());
               output.accept((ItemLike)AWCoreItems.DIAMOND_QUILL.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_HAMMER.get());
               output.accept((ItemLike)AWCoreItems.STONE_HAMMER.get());
               output.accept((ItemLike)AWCoreItems.IRON_HAMMER.get());
               output.accept((ItemLike)AWCoreItems.GOLD_HAMMER.get());
               output.accept((ItemLike)AWCoreItems.DIAMOND_HAMMER.get());
               output.accept((ItemLike)AWCoreBlocks.ENGINEERING_STATION.get());
               output.accept((ItemLike)AWCoreBlocks.MAILBOX.get());
               output.accept((ItemLike)AWCoreBlocks.RESEARCH_STATION.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_GEAR.get());
               output.accept((ItemLike)AWCoreItems.IRON_GEAR.get());
               output.accept((ItemLike)AWCoreItems.STEEL_GEAR.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_SHAFT.get());
               output.accept((ItemLike)AWCoreItems.IRON_SHAFT.get());
               output.accept((ItemLike)AWCoreItems.STEEL_SHAFT.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_BEARINGS.get());
               output.accept((ItemLike)AWCoreItems.IRON_BEARINGS.get());
               output.accept((ItemLike)AWCoreItems.STEEL_BEARINGS.get());
            })
            .build()
   );
   public static final RegistryObject<CreativeModeTab> VEHICLES = REGISTER.register(
      "vehicles",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ancientwarfare.vehicles"))
            .icon(() -> ((Item)AWCoreItems.CHEST_PUSHCART.get()).getDefaultInstance())
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)AWCoreItems.CHEST_PUSHCART.get());
               output.accept((ItemLike)AWCoreItems.CHEST_PUSHCART_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.CHEST_PUSHCART_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.CHEST_PUSHCART_IRON.get());
               output.accept((ItemLike)AWCoreItems.CHEST_PUSHCART_STEEL.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE_IRON.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE_STEEL.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED_IRON.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_MOBILE_FIXED_STEEL.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_BOAT.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_BOAT_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_BOAT_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_BOAT_IRON.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_BOAT_STEEL.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND_IRON.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_TURRET_STAND_STEEL.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED_IRON.get());
               output.accept((ItemLike)AWCoreItems.BALLISTA_STAND_FIXED_STEEL.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE_IRON.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE_STEEL.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED_IRON.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_MOBILE_FIXED_STEEL.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_MOBILE.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_MOBILE_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_MOBILE_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_MOBILE_IRON.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_MOBILE_STEEL.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_FIXED.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_FIXED_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_FIXED_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_FIXED_IRON.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_FIXED_STEEL.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_TURRET.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_TURRET_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_TURRET_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_TURRET_IRON.get());
               output.accept((ItemLike)AWCoreItems.TREBUCHET_TURRET_STEEL.get());
               output.accept((ItemLike)AWCoreItems.GIANT_TREBUCHET.get());
               output.accept((ItemLike)AWCoreItems.GIANT_TREBUCHET_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.GIANT_TREBUCHET_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.GIANT_TREBUCHET_IRON.get());
               output.accept((ItemLike)AWCoreItems.GIANT_TREBUCHET_STEEL.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND_IRON.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_TURRET_STAND_STEEL.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED_IRON.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_STAND_FIXED_STEEL.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_BOAT.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_BOAT_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_BOAT_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_BOAT_IRON.get());
               output.accept((ItemLike)AWCoreItems.CATAPULT_BOAT_STEEL.get());
               output.accept((ItemLike)AWCoreItems.TRANSPORT_BOAT.get());
               output.accept((ItemLike)AWCoreItems.TRANSPORT_BOAT_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.TRANSPORT_BOAT_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.TRANSPORT_BOAT_IRON.get());
               output.accept((ItemLike)AWCoreItems.TRANSPORT_BOAT_STEEL.get());
               output.accept((ItemLike)AWCoreItems.CANNON_TURRET_STAND.get());
               output.accept((ItemLike)AWCoreItems.CANNON_TURRET_STAND_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.CANNON_TURRET_STAND_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.CANNON_TURRET_STAND_IRON.get());
               output.accept((ItemLike)AWCoreItems.CANNON_TURRET_STAND_STEEL.get());
               output.accept((ItemLike)AWCoreItems.CANNON_STAND_FIXED.get());
               output.accept((ItemLike)AWCoreItems.CANNON_STAND_FIXED_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.CANNON_STAND_FIXED_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.CANNON_STAND_FIXED_IRON.get());
               output.accept((ItemLike)AWCoreItems.CANNON_STAND_FIXED_STEEL.get());
               output.accept((ItemLike)AWCoreItems.CANNON_MOBILE.get());
               output.accept((ItemLike)AWCoreItems.CANNON_MOBILE_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.CANNON_MOBILE_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.CANNON_MOBILE_IRON.get());
               output.accept((ItemLike)AWCoreItems.CANNON_MOBILE_STEEL.get());
               output.accept((ItemLike)AWCoreItems.BATTERING_RAM.get());
               output.accept((ItemLike)AWCoreItems.BATTERING_RAM_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.BATTERING_RAM_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.BATTERING_RAM_IRON.get());
               output.accept((ItemLike)AWCoreItems.BATTERING_RAM_STEEL.get());
               output.accept((ItemLike)AWCoreItems.HWACHA.get());
               output.accept((ItemLike)AWCoreItems.HWACHA_IRON_BAND_1.get());
               output.accept((ItemLike)AWCoreItems.HWACHA_IRON_BAND_2.get());
               output.accept((ItemLike)AWCoreItems.HWACHA_IRON.get());
               output.accept((ItemLike)AWCoreItems.HWACHA_STEEL.get());
               output.accept((ItemLike)AWCoreItems.FLAME_CHARGE.get());
               output.accept((ItemLike)AWCoreItems.EXPLOSIVE_CHARGE.get());
               output.accept((ItemLike)AWCoreItems.ROCKET_CHARGE.get());
               output.accept((ItemLike)AWCoreItems.CLUSTER_CHARGE.get());
               output.accept((ItemLike)AWCoreItems.NAPALM_CHARGE.get());
               output.accept((ItemLike)AWCoreItems.CLAY_CASING.get());
               output.accept((ItemLike)AWCoreItems.IRON_CASING.get());
               output.accept((ItemLike)AWCoreItems.MOBILITY_UNIT.get());
               output.accept((ItemLike)AWCoreItems.TURRET_COMPONENTS.get());
               output.accept((ItemLike)AWCoreItems.TORSION_UNIT.get());
               output.accept((ItemLike)AWCoreItems.COUNTER_WEIGHT_UNIT.get());
               output.accept((ItemLike)AWCoreItems.POWDER_CASE.get());
               output.accept((ItemLike)AWCoreItems.EQUIPMENT_BAY.get());
               output.accept((ItemLike)AWCoreItems.ROUGH_WOOD.get());
               output.accept((ItemLike)AWCoreItems.TREATED_WOOD.get());
               output.accept((ItemLike)AWCoreItems.IRONSHOD_WOOD.get());
               output.accept((ItemLike)AWCoreItems.IRON_CORE_WOOD.get());
               output.accept((ItemLike)AWCoreItems.ROUGH_IRON.get());
               output.accept((ItemLike)AWCoreItems.FINE_IRON.get());
               output.accept((ItemLike)AWCoreItems.TEMPERED_IRON.get());
               output.accept((ItemLike)AWCoreItems.MINOR_ALLOY.get());
               output.accept((ItemLike)AWCoreItems.MAJOR_ALLOY.get());
               output.accept((ItemLike)AWCoreItems.ARMOR_STONE.get());
               output.accept((ItemLike)AWCoreItems.ARMOR_IRON.get());
               output.accept((ItemLike)AWCoreItems.ARMOR_OBSIDIAN.get());
               output.accept((ItemLike)AWCoreItems.VEHICLE_UPGRADE_SPEED.get());
               output.accept((ItemLike)AWCoreItems.VEHICLE_UPGRADE_AIM.get());
               output.accept((ItemLike)AWCoreItems.VEHICLE_UPGRADE_RELOAD.get());
               output.accept((ItemLike)AWCoreItems.VEHICLE_UPGRADE_POWER.get());
               output.accept((ItemLike)AWCoreItems.VEHICLE_UPGRADE_TURRET_PITCH.get());
               output.accept((ItemLike)AWCoreItems.VEHICLE_UPGRADE_PITCH_UP.get());
               output.accept((ItemLike)AWCoreItems.VEHICLE_UPGRADE_PITCH_DOWN.get());
               output.accept((ItemLike)AWCoreItems.AMMO_STONE_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_STONE_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_STONE_SHOT_30.get());
               output.accept((ItemLike)AWCoreItems.AMMO_STONE_SHOT_45.get());
               output.accept((ItemLike)AWCoreItems.AMMO_FLAME_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_FLAME_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_FLAME_SHOT_30.get());
               output.accept((ItemLike)AWCoreItems.AMMO_FLAME_SHOT_45.get());
               output.accept((ItemLike)AWCoreItems.AMMO_PEBBLE_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_PEBBLE_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_PEBBLE_SHOT_30.get());
               output.accept((ItemLike)AWCoreItems.AMMO_PEBBLE_SHOT_45.get());
               output.accept((ItemLike)AWCoreItems.AMMO_CLUSTER_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_CLUSTER_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_CLUSTER_SHOT_30.get());
               output.accept((ItemLike)AWCoreItems.AMMO_CLUSTER_SHOT_45.get());
               output.accept((ItemLike)AWCoreItems.AMMO_EXPLOSIVE_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_EXPLOSIVE_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_EXPLOSIVE_SHOT_30.get());
               output.accept((ItemLike)AWCoreItems.AMMO_EXPLOSIVE_SHOT_45.get());
               output.accept((ItemLike)AWCoreItems.AMMO_HIGH_EXPLOSIVE_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_HIGH_EXPLOSIVE_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_HIGH_EXPLOSIVE_SHOT_30.get());
               output.accept((ItemLike)AWCoreItems.AMMO_HIGH_EXPLOSIVE_SHOT_45.get());
               output.accept((ItemLike)AWCoreItems.AMMO_NAPALM_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_NAPALM_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_NAPALM_SHOT_30.get());
               output.accept((ItemLike)AWCoreItems.AMMO_NAPALM_SHOT_45.get());
               output.accept((ItemLike)AWCoreItems.AMMO_BALLISTA_BOLT.get());
               output.accept((ItemLike)AWCoreItems.AMMO_BALLISTA_BOLT_IRON.get());
               output.accept((ItemLike)AWCoreItems.AMMO_BALLISTA_BOLT_FLAME.get());
               output.accept((ItemLike)AWCoreItems.AMMO_BALLISTA_BOLT_EXPLOSIVE.get());
               output.accept((ItemLike)AWCoreItems.AMMO_HWACHA_ROCKET.get());
               output.accept((ItemLike)AWCoreItems.AMMO_HWACHA_ROCKET_FLAME.get());
               output.accept((ItemLike)AWCoreItems.AMMO_HWACHA_ROCKET_EXPLOSIVE.get());
               output.accept((ItemLike)AWCoreItems.AMMO_HWACHA_ROCKET_AIRBURST.get());
               output.accept((ItemLike)AWCoreItems.AMMO_IRON_SHOT_5.get());
               output.accept((ItemLike)AWCoreItems.AMMO_IRON_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_IRON_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_IRON_SHOT_25.get());
               output.accept((ItemLike)AWCoreItems.AMMO_GRAPE_SHOT_5.get());
               output.accept((ItemLike)AWCoreItems.AMMO_GRAPE_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_GRAPE_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_GRAPE_SHOT_25.get());
               output.accept((ItemLike)AWCoreItems.AMMO_CANISTER_SHOT_5.get());
               output.accept((ItemLike)AWCoreItems.AMMO_CANISTER_SHOT_10.get());
               output.accept((ItemLike)AWCoreItems.AMMO_CANISTER_SHOT_15.get());
               output.accept((ItemLike)AWCoreItems.AMMO_CANISTER_SHOT_25.get());
            })
            .build()
   );
   public static final RegistryObject<CreativeModeTab> NPC = REGISTER.register(
      "npc",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ancientwarfare.npc"))
            .icon(NpcSpawnerItem::createWorker)
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)AWCoreItems.WOODEN_COMMAND_BATON.get());
               output.accept((ItemLike)AWCoreItems.STONE_COMMAND_BATON.get());
               output.accept((ItemLike)AWCoreItems.IRON_COMMAND_BATON.get());
               output.accept((ItemLike)AWCoreItems.GOLD_COMMAND_BATON.get());
               output.accept((ItemLike)AWCoreItems.DIAMOND_COMMAND_BATON.get());
               output.accept((ItemLike)AWCoreBlocks.TOWN_HALL.get());
               output.accept((ItemLike)AWCoreBlocks.QUEST_BOARD.get());
               output.accept((ItemLike)AWCoreItems.CONSTRUCTION_WAND.get());
               output.accept((ItemLike)AWCoreBlocks.FIELD_TENT.get());
               output.accept((ItemLike)AWCoreItems.WORK_ORDER.get());
               output.accept((ItemLike)AWCoreItems.ROUTING_ORDER.get());
               output.accept((ItemLike)AWCoreItems.TRADE_ORDER.get());
               output.accept((ItemLike)AWCoreItems.UPKEEP_ORDER.get());
               output.accept((ItemLike)AWCoreItems.COMBAT_ORDER.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_SPEAR.get());
               output.accept((ItemLike)AWCoreItems.STONE_SPEAR.get());
               output.accept((ItemLike)AWCoreItems.IRON_SPEAR.get());
               output.accept((ItemLike)AWCoreItems.GOLDEN_SPEAR.get());
               output.accept((ItemLike)AWCoreItems.DIAMOND_SPEAR.get());
               output.accept((ItemLike)AWCoreItems.OBSIDIAN_SPEAR.get());
               output.accept((ItemLike)AWCoreItems.ICE_SPEAR.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_HALBERD.get());
               output.accept((ItemLike)AWCoreItems.STONE_HALBERD.get());
               output.accept((ItemLike)AWCoreItems.IRON_HALBERD.get());
               output.accept((ItemLike)AWCoreItems.GOLDEN_HALBERD.get());
               output.accept((ItemLike)AWCoreItems.DIAMOND_HALBERD.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_LANCE.get());
               output.accept((ItemLike)AWCoreItems.STONE_LANCE.get());
               output.accept((ItemLike)AWCoreItems.IRON_LANCE.get());
               output.accept((ItemLike)AWCoreItems.GOLDEN_LANCE.get());
               output.accept((ItemLike)AWCoreItems.DIAMOND_LANCE.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_CLEAVER.get());
               output.accept((ItemLike)AWCoreItems.STONE_CLEAVER.get());
               output.accept((ItemLike)AWCoreItems.IRON_CLEAVER.get());
               output.accept((ItemLike)AWCoreItems.GOLDEN_CLEAVER.get());
               output.accept((ItemLike)AWCoreItems.DIAMOND_CLEAVER.get());
               output.accept((ItemLike)AWCoreItems.SICKLE.get());
               output.accept((ItemLike)AWCoreItems.PITCHFORK.get());
               output.accept((ItemLike)AWCoreItems.SCYTHE.get());
               output.accept((ItemLike)AWCoreItems.DEATH_SCYTHE.get());
               output.accept((ItemLike)AWCoreItems.GIANT_CLUB.get());
               output.accept((ItemLike)AWCoreItems.MACUAHUITL.get());
               output.accept((ItemLike)AWCoreItems.WOODEN_SHIELD.get());
               output.accept((ItemLike)AWCoreItems.STONE_SHIELD.get());
               output.accept((ItemLike)AWCoreItems.IRON_SHIELD.get());
               output.accept((ItemLike)AWCoreItems.GOLD_SHIELD.get());
               output.accept((ItemLike)AWCoreItems.DIAMOND_SHIELD.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_TRIBAL_1.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_TRIBAL_2.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_ROUND_1.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_ROUND_2.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_ROUND_3.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_ROUND_4.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_ROUND_5.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_ROUND_6.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_WITCHBANE_1.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_WITCHBANE_2.get());
               output.accept((ItemLike)AWCoreItems.SHIELD_BUFFLOKA.get());

               for (CoinMetal metal : CoinMetal.values()) {
                  output.accept(CoinItem.create(metal));
               }

               output.accept((ItemLike)AWCoreItems.FOOD_BUNDLE.get());

               for (BardInstrumentType type : BardInstrumentType.values()) {
                  output.accept(BardInstrumentItem.create(type));
               }

               output.accept(NpcSpawnerItem.createWorker());
               output.accept(NpcSpawnerItem.createCombat());
               output.accept(NpcSpawnerItem.createCourier());
               output.accept(NpcSpawnerItem.createTrader());
               output.accept(NpcSpawnerItem.createBard());
               output.accept(NpcSpawnerItem.createPriest());
               output.accept(NpcSpawnerItem.createSiegeEngineer());

               for (ItemStack stack : NpcSpawnerItem.createFactionCreativeStacks()) {
                  output.accept(stack);
               }
            })
            .build()
   );
   public static final RegistryObject<CreativeModeTab> AUTOMATION = REGISTER.register(
      "automation",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ancientwarfare.automation"))
            .icon(() -> ((Block)AWCoreBlocks.CROP_FARM.get()).asItem().getDefaultInstance())
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)AWCoreBlocks.CROP_FARM.get());
               output.accept((ItemLike)AWCoreBlocks.ANIMAL_FARM.get());
               output.accept((ItemLike)AWCoreBlocks.TREE_FARM.get());
               output.accept((ItemLike)AWCoreBlocks.FISH_FARM.get());
               output.accept((ItemLike)AWCoreBlocks.FRUIT_FARM.get());
               output.accept((ItemLike)AWCoreBlocks.QUARRY.get());
               output.accept((ItemLike)AWCoreBlocks.WAREHOUSE_CONTROL.get());
               output.accept((ItemLike)AWCoreBlocks.WAREHOUSE_STORAGE_SMALL.get());
               output.accept((ItemLike)AWCoreBlocks.WAREHOUSE_STORAGE_MEDIUM.get());
               output.accept((ItemLike)AWCoreBlocks.WAREHOUSE_STORAGE_LARGE.get());
               output.accept((ItemLike)AWCoreBlocks.WAREHOUSE_INTERFACE.get());
               output.accept((ItemLike)AWCoreBlocks.WAREHOUSE_CRAFTING.get());
               output.accept((ItemLike)AWCoreBlocks.AUTO_CRAFTING.get());
               output.accept((ItemLike)AWCoreBlocks.WAREHOUSE_STOCK_VIEWER.get());
               output.accept((ItemLike)AWCoreBlocks.WAREHOUSE_STOCK_LINKER.get());
               output.accept((ItemLike)AWCoreBlocks.TORQUE_SHAFT_LIGHT.get());
               output.accept((ItemLike)AWCoreBlocks.TORQUE_SHAFT_MEDIUM.get());
               output.accept((ItemLike)AWCoreBlocks.TORQUE_SHAFT_HEAVY.get());
               output.accept((ItemLike)AWCoreBlocks.TORQUE_JUNCTION_LIGHT.get());
               output.accept((ItemLike)AWCoreBlocks.TORQUE_JUNCTION_MEDIUM.get());
               output.accept((ItemLike)AWCoreBlocks.TORQUE_JUNCTION_HEAVY.get());
               output.accept((ItemLike)AWCoreBlocks.TORQUE_DISTRIBUTOR_LIGHT.get());
               output.accept((ItemLike)AWCoreBlocks.TORQUE_DISTRIBUTOR_MEDIUM.get());
               output.accept((ItemLike)AWCoreBlocks.TORQUE_DISTRIBUTOR_HEAVY.get());
               output.accept((ItemLike)AWCoreBlocks.HAND_CRANKED_GENERATOR.get());
               output.accept((ItemLike)AWCoreBlocks.STIRLING_GENERATOR.get());
               output.accept((ItemLike)AWCoreBlocks.WATERWHEEL_GENERATOR.get());
               output.accept((ItemLike)AWCoreBlocks.WINDMILL_GENERATOR.get());
               output.accept((ItemLike)AWCoreBlocks.WINDMILL_BLADE.get());
               output.accept((ItemLike)AWCoreBlocks.CHUNK_LOADER_SIMPLE.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_SIZE_MEDIUM.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_SIZE_LARGE.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_QUARRY_MEDIUM.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_QUARRY_LARGE.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_ENCHANTED_TOOLS_1.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_ENCHANTED_TOOLS_2.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_TOOL_QUALITY_1.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_TOOL_QUALITY_2.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_TOOL_QUALITY_3.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_BASIC_CHUNK_LOADER.get());
               output.accept((ItemLike)AWCoreItems.WORKSITE_UPGRADE_QUARRY_CHUNK_LOADER.get());
               output.accept((ItemLike)AWCoreBlocks.FLYWHEEL_CONTROLLER_LIGHT.get());
               output.accept((ItemLike)AWCoreBlocks.FLYWHEEL_CONTROLLER_MEDIUM.get());
               output.accept((ItemLike)AWCoreBlocks.FLYWHEEL_CONTROLLER_HEAVY.get());
               output.accept((ItemLike)AWCoreBlocks.FLYWHEEL_STORAGE_LIGHT.get());
               output.accept((ItemLike)AWCoreBlocks.FLYWHEEL_STORAGE_MEDIUM.get());
               output.accept((ItemLike)AWCoreBlocks.FLYWHEEL_STORAGE_HEAVY.get());
            })
            .build()
   );
   public static final RegistryObject<CreativeModeTab> STRUCTURE = REGISTER.register(
      "structure",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ancientwarfare.structure"))
            .icon(() -> ((Block)AWCoreBlocks.SCISSOR_SEAT.get()).asItem().getDefaultInstance())
            .displayItems(
               (parameters, output) -> {
                  output.accept((ItemLike)AWCoreItems.STRUCTURE_SCANNER.get());
                  output.accept((ItemLike)AWCoreBlocks.STRUCTURE_SCANNER_BLOCK.get());
                  output.accept((ItemLike)AWCoreBlocks.DRAFTING_STATION.get());

                  for (AwsTemplatePlacer.TemplateInfo template : AwsTemplatePlacer.survivalTemplates()) {
                     output.accept(SurvivalStructureBuilderItem.forAwsTemplate(template.path(), template.name()));
                  }

                  output.accept((ItemLike)AWCoreBlocks.SCISSOR_SEAT.get());
                  output.accept((ItemLike)AWCoreBlocks.TRIBAL_STOOL.get());
                  output.accept((ItemLike)AWCoreBlocks.WOODEN_THRONE.get());
                  output.accept((ItemLike)AWCoreBlocks.GOLDEN_IDOL.get());
                  output.accept((ItemLike)AWCoreBlocks.GOLDEN_THRONE.get());
                  output.accept((ItemLike)AWCoreBlocks.ORC_TOTEM_1.get());
                  output.accept((ItemLike)AWCoreBlocks.ORC_TOTEM_2.get());
                  output.accept((ItemLike)AWCoreBlocks.ORC_TOTEM_2_LIT.get());
                  output.accept((ItemLike)AWCoreBlocks.GOBLIN_TOTEM_1.get());
                  output.accept((ItemLike)AWCoreBlocks.GOBLIN_TOTEM_2.get());
                  output.accept((ItemLike)AWCoreBlocks.GOBLIN_TOTEM_2_LIT.get());
                  output.accept((ItemLike)AWCoreBlocks.IRON_CAGE.get());
                  output.accept((ItemLike)AWCoreBlocks.GIBBET.get());
                  output.accept((ItemLike)AWCoreBlocks.WOODEN_POST.get());
                  output.accept((ItemLike)AWCoreBlocks.STOOL.get());
                  output.accept((ItemLike)AWCoreBlocks.TABLE.get());
                  output.accept((ItemLike)AWCoreBlocks.CHAIR.get());
                  output.accept((ItemLike)AWCoreBlocks.BENCH.get());
                  output.accept((ItemLike)AWCoreBlocks.TRIBAL_CHAIR.get());
                  output.accept((ItemLike)AWCoreBlocks.ALTAR_LECTERN.get());
                  output.accept((ItemLike)AWCoreBlocks.ALTAR_SUN.get());
                  output.accept((ItemLike)AWCoreBlocks.FIRE_PIT.get());
                  output.accept((ItemLike)AWCoreBlocks.FIRE_PIT_LOG.get());
                  output.accept((ItemLike)AWCoreBlocks.FIRE_PIT_SQUARE.get());
                  output.accept((ItemLike)AWCoreBlocks.BRAZIER_FLAME.get());
                  output.accept((ItemLike)AWCoreBlocks.BRAZIER_EMBER.get());

                  for (int size = 8; size <= 64; size += 8) {
                     output.accept(((CoinStackBlock)AWCoreBlocks.COIN_STACK_COPPER.get()).stackItem(size));
                     output.accept(((CoinStackBlock)AWCoreBlocks.COIN_STACK_SILVER.get()).stackItem(size));
                     output.accept(((CoinStackBlock)AWCoreBlocks.COIN_STACK_GOLD.get()).stackItem(size));
                     output.accept(((CoinStackBlock)AWCoreBlocks.COIN_STACK_ANCIENT.get()).stackItem(size));
                  }

                  output.accept((ItemLike)AWCoreBlocks.STRETCHING_RACK.get());
                  output.accept((ItemLike)AWCoreBlocks.GRAVESTONE_1.get());
                  output.accept((ItemLike)AWCoreBlocks.GRAVESTONE_2.get());
                  output.accept((ItemLike)AWCoreBlocks.GRAVESTONE_3.get());
                  output.accept((ItemLike)AWCoreBlocks.GRAVESTONE_4.get());
                  output.accept((ItemLike)AWCoreBlocks.GRAVESTONE_5.get());
                  output.accept((ItemLike)AWCoreBlocks.GRAVESTONE_6.get());
                  output.accept((ItemLike)AWCoreBlocks.GRAVESTONE_7.get());
                  output.accept((ItemLike)AWCoreBlocks.GRAVESTONE_8.get());
                  output.accept((ItemLike)AWCoreBlocks.ALTAR_SHORT_CLOTH.get());
                  output.accept((ItemLike)AWCoreBlocks.ALTAR_LONG_CLOTH.get());
                  output.accept((ItemLike)AWCoreBlocks.ALTAR_CANDLE.get());
                  output.accept((ItemLike)AWCoreBlocks.STAKE.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_BASE.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_MID.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_MID_ALT.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_TOP.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_WINGS.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_IRMINSUL_BASE.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_IRMINSUL_MID.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_IRMINSUL_TOP.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_IRMINSUL_SIDE.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_ELEPHANT_BODY.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_ELEPHANT_LEFT.get());
                  output.accept((ItemLike)AWCoreBlocks.TOTEM_PART_ELEPHANT_RIGHT.get());
                  output.accept((ItemLike)AWCoreBlocks.XOLTEC_IDOL_BODY_LEFT.get());
                  output.accept((ItemLike)AWCoreBlocks.XOLTEC_IDOL_BODY_RIGHT.get());
                  output.accept((ItemLike)AWCoreBlocks.XOLTEC_IDOL_HEAD_LEFT.get());
                  output.accept((ItemLike)AWCoreBlocks.XOLTEC_IDOL_HEAD_RIGHT.get());
                  output.accept((ItemLike)AWCoreBlocks.LOOT_BASKET.get());
                  output.accept((ItemLike)AWCoreBlocks.URN.get());
                  output.accept((ItemLike)AWCoreBlocks.WOODEN_COFFIN.get());
                  output.accept((ItemLike)AWCoreBlocks.STONE_COFFIN.get());
                  output.accept((ItemLike)AWCoreBlocks.ADVANCED_LOOT_CHEST.get());
                  output.accept((ItemLike)AWCoreBlocks.ADVANCED_SPAWNER.get());
                  FactionRegistry.factionNames()
                     .stream()
                     .sorted()
                     .forEach(faction -> output.accept(ProtectionFlagBlockItem.create((Block)AWCoreBlocks.PROTECTION_FLAG.get(), faction)));
                  output.accept((ItemLike)AWCoreItems.GATE_SPAWNER.get());
                  output.accept((ItemLike)AWCoreItems.GATE_SPAWNER_IRON.get());
                  output.accept((ItemLike)AWCoreItems.GATE_SPAWNER_SINGLE_WOOD.get());
                  output.accept((ItemLike)AWCoreItems.GATE_SPAWNER_SINGLE_IRON.get());
                  output.accept((ItemLike)AWCoreItems.GATE_SPAWNER_DOUBLE_WOOD.get());
                  output.accept((ItemLike)AWCoreItems.GATE_SPAWNER_DOUBLE_IRON.get());
                  output.accept((ItemLike)AWCoreItems.GATE_SPAWNER_DRAWBRIDGE.get());
               }
            )
            .build()
   );
   public static final RegistryObject<CreativeModeTab> AWS_TEMPLATES = REGISTER.register(
      "aws_templates",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ancientwarfare.aws_templates"))
            .icon(() -> AwsTemplateBlockItem.create((Block)AWCoreBlocks.UNDEAD_CEMETERY_FILL_SKULL_1.get(), AwsTemplateIndex.defaultTemplate()))
            .displayItems((parameters, output) -> {
               List<String> templates = AwsTemplateIndex.allTemplates();
               if (templates.isEmpty()) {
                  output.accept((ItemLike)AWCoreBlocks.UNDEAD_CEMETERY_FILL_SKULL_1.get());
               }

               for (String template : templates) {
                  output.accept(AwsTemplateBlockItem.create((Block)AWCoreBlocks.UNDEAD_CEMETERY_FILL_SKULL_1.get(), template));
               }
            })
            .build()
   );
   public static final RegistryObject<CreativeModeTab> AWT_TOWNS = REGISTER.register(
      "awt_towns",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ancientwarfare.awt_towns"))
            .icon(() -> AwtTownBlockItem.create((Block)AWCoreBlocks.AWT_TOWN_MARKER.get(), AwtTemplateIndex.defaultTemplate()))
            .displayItems((parameters, output) -> {
               List<String> templates = AwtTemplateIndex.allTemplates();
               if (templates.isEmpty()) {
                  output.accept((ItemLike)AWCoreBlocks.AWT_TOWN_MARKER.get());
               }

               for (String template : templates) {
                  output.accept(AwtTownBlockItem.create((Block)AWCoreBlocks.AWT_TOWN_MARKER.get(), template));
               }
            })
            .build()
   );

   private AWCoreCreativeTabs() {
   }
}
