package net.shadowmage.ancientwarfare.core.forge.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.BallistaBoatEntity;
import net.shadowmage.ancientwarfare.core.entity.BallistaBoltEntity;
import net.shadowmage.ancientwarfare.core.entity.BallistaMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.BallistaStandFixedEntity;
import net.shadowmage.ancientwarfare.core.entity.BallistaTurretStandEntity;
import net.shadowmage.ancientwarfare.core.entity.BardNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.BatteringRamEntity;
import net.shadowmage.ancientwarfare.core.entity.CannonMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.CannonShotEntity;
import net.shadowmage.ancientwarfare.core.entity.CannonTurretStandEntity;
import net.shadowmage.ancientwarfare.core.entity.CatapultBoatEntity;
import net.shadowmage.ancientwarfare.core.entity.CatapultMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.CatapultShotEntity;
import net.shadowmage.ancientwarfare.core.entity.CatapultTurretStandEntity;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.CourierNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionArcherEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionArcherNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionBardNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionCavalryNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionCivilianFemaleNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionCivilianMaleNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionLeaderEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionLeaderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionMountedArcherNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionPriestNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSiegeEngineerNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSoldierEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSoldierNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionTraderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;
import net.shadowmage.ancientwarfare.core.entity.GiantTrebuchetEntity;
import net.shadowmage.ancientwarfare.core.entity.HwachaEntity;
import net.shadowmage.ancientwarfare.core.entity.HwachaRocketEntity;
import net.shadowmage.ancientwarfare.core.entity.PriestNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.SiegeEngineerNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.TraderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.TransportBoatEntity;
import net.shadowmage.ancientwarfare.core.entity.TrebuchetFixedEntity;
import net.shadowmage.ancientwarfare.core.entity.TrebuchetMobileEntity;
import net.shadowmage.ancientwarfare.core.entity.TrebuchetTurretEntity;
import net.shadowmage.ancientwarfare.core.entity.WorkerNpcEntity;

public final class AWCoreEntityTypes {
   public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "ancientwarfare");
   public static final RegistryObject<EntityType<ChestPushcartEntity>> CHEST_PUSHCART = REGISTER.register(
      "chest_pushcart",
      () -> Builder.of(ChestPushcartEntity::new, MobCategory.MISC)
            .sized(2.7F, 1.8F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("chest_pushcart").toString())
   );
   public static final RegistryObject<EntityType<BallistaMobileEntity>> BALLISTA_MOBILE = REGISTER.register(
      "ballista_mobile",
      () -> Builder.of(BallistaMobileEntity::new, MobCategory.MISC)
            .sized(2.7F, 1.4F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("ballista_mobile").toString())
   );
   public static final RegistryObject<EntityType<BallistaBoatEntity>> BALLISTA_BOAT = REGISTER.register(
      "ballista_boat",
      () -> Builder.of(BallistaBoatEntity::new, MobCategory.MISC)
            .sized(2.7F, 1.4F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("ballista_boat").toString())
   );
   public static final RegistryObject<EntityType<BallistaTurretStandEntity>> BALLISTA_TURRET_STAND = REGISTER.register(
      "ballista_turret_stand",
      () -> Builder.of(BallistaTurretStandEntity::new, MobCategory.MISC)
            .sized(1.2F, 1.4F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("ballista_turret_stand").toString())
   );
   public static final RegistryObject<EntityType<BallistaStandFixedEntity>> BALLISTA_STAND_FIXED = REGISTER.register(
      "ballista_stand_fixed",
      () -> Builder.of(BallistaStandFixedEntity::new, MobCategory.MISC)
            .sized(1.2F, 1.4F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("ballista_stand_fixed").toString())
   );
   public static final RegistryObject<EntityType<CatapultMobileEntity>> CATAPULT_MOBILE = REGISTER.register(
      "catapult_mobile",
      () -> Builder.of(CatapultMobileEntity::new, MobCategory.MISC)
            .sized(2.7F, 2.0F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("catapult_mobile").toString())
   );
   public static final RegistryObject<EntityType<TrebuchetMobileEntity>> TREBUCHET_MOBILE = REGISTER.register(
      "trebuchet_mobile",
      () -> Builder.of(TrebuchetMobileEntity::new, MobCategory.MISC)
            .sized(2.7F, 2.7F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("trebuchet_mobile").toString())
   );
   public static final RegistryObject<EntityType<TrebuchetFixedEntity>> TREBUCHET_FIXED = REGISTER.register(
      "trebuchet_fixed",
      () -> Builder.of(TrebuchetFixedEntity::new, MobCategory.MISC)
            .sized(2.0F, 2.0F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("trebuchet_fixed").toString())
   );
   public static final RegistryObject<EntityType<TrebuchetTurretEntity>> TREBUCHET_TURRET = REGISTER.register(
      "trebuchet_turret",
      () -> Builder.of(TrebuchetTurretEntity::new, MobCategory.MISC)
            .sized(2.7F, 2.7F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("trebuchet_turret").toString())
   );
   public static final RegistryObject<EntityType<GiantTrebuchetEntity>> GIANT_TREBUCHET = REGISTER.register(
      "giant_trebuchet",
      () -> Builder.of(GiantTrebuchetEntity::new, MobCategory.MISC)
            .sized(5.0F, 5.0F)
            .clientTrackingRange(12)
            .updateInterval(1)
            .build(AWCoreIds.id("giant_trebuchet").toString())
   );
   public static final RegistryObject<EntityType<CatapultTurretStandEntity>> CATAPULT_TURRET_STAND = REGISTER.register(
      "catapult_turret_stand",
      () -> Builder.of(CatapultTurretStandEntity::new, MobCategory.MISC)
            .sized(2.7F, 2.0F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("catapult_turret_stand").toString())
   );
   public static final RegistryObject<EntityType<CatapultBoatEntity>> CATAPULT_BOAT = REGISTER.register(
      "catapult_boat",
      () -> Builder.of(CatapultBoatEntity::new, MobCategory.MISC)
            .sized(2.7F, 1.4F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("catapult_boat").toString())
   );
   public static final RegistryObject<EntityType<TransportBoatEntity>> TRANSPORT_BOAT = REGISTER.register(
      "transport_boat",
      () -> Builder.of(TransportBoatEntity::new, MobCategory.MISC)
            .sized(2.8F, 1.8F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("transport_boat").toString())
   );
   public static final RegistryObject<EntityType<BallistaBoltEntity>> BALLISTA_BOLT = REGISTER.register(
      "ballista_bolt",
      () -> Builder.<BallistaBoltEntity>of(BallistaBoltEntity::new, MobCategory.MISC)
            .sized(0.35F, 0.35F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build(AWCoreIds.id("ballista_bolt").toString())
   );
   public static final RegistryObject<EntityType<CatapultShotEntity>> CATAPULT_SHOT = REGISTER.register(
      "catapult_shot",
      () -> Builder.<CatapultShotEntity>of(CatapultShotEntity::new, MobCategory.MISC)
            .sized(0.85F, 0.85F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("catapult_shot").toString())
   );
   public static final RegistryObject<EntityType<CannonTurretStandEntity>> CANNON_TURRET_STAND = REGISTER.register(
      "cannon_turret_stand",
      () -> Builder.of(CannonTurretStandEntity::new, MobCategory.MISC)
            .sized(2.7F, 2.0F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("cannon_turret_stand").toString())
   );
   public static final RegistryObject<EntityType<CannonMobileEntity>> CANNON_MOBILE = REGISTER.register(
      "cannon_mobile",
      () -> Builder.of(CannonMobileEntity::new, MobCategory.MISC)
            .sized(1.9F, 1.5F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("cannon_mobile").toString())
   );
   public static final RegistryObject<EntityType<CannonShotEntity>> CANNON_SHOT = REGISTER.register(
      "cannon_shot",
      () -> Builder.<CannonShotEntity>of(CannonShotEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("cannon_shot").toString())
   );
   public static final RegistryObject<EntityType<BatteringRamEntity>> BATTERING_RAM = REGISTER.register(
      "battering_ram",
      () -> Builder.of(BatteringRamEntity::new, MobCategory.MISC)
            .sized(2.9F, 2.1F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("battering_ram").toString())
   );
   public static final RegistryObject<EntityType<HwachaEntity>> HWACHA = REGISTER.register(
      "hwacha",
      () -> Builder.of(HwachaEntity::new, MobCategory.MISC).sized(2.8F, 2.0F).clientTrackingRange(10).updateInterval(1).build(AWCoreIds.id("hwacha").toString())
   );
   public static final RegistryObject<EntityType<HwachaRocketEntity>> HWACHA_ROCKET = REGISTER.register(
      "hwacha_rocket",
      () -> Builder.<HwachaRocketEntity>of(HwachaRocketEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(AWCoreIds.id("hwacha_rocket").toString())
   );
   public static final RegistryObject<EntityType<GateEntity>> DYNAMIC_GATE = REGISTER.register(
      AWCoreIds.DYNAMIC_GATE.getPath(),
      () -> Builder.of(GateEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(16).updateInterval(1).build(AWCoreIds.DYNAMIC_GATE.toString())
   );
   public static final RegistryObject<EntityType<WorkerNpcEntity>> WORKER_NPC = REGISTER.register(
      AWCoreIds.WORKER_NPC.getPath(),
      () -> Builder.of(WorkerNpcEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(8).build(AWCoreIds.WORKER_NPC.toString())
   );
   public static final RegistryObject<EntityType<CombatNpcEntity>> COMBAT_NPC = REGISTER.register(
      AWCoreIds.COMBAT_NPC.getPath(),
      () -> Builder.<CombatNpcEntity>of(CombatNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.COMBAT_NPC.toString())
   );
   public static final RegistryObject<EntityType<CourierNpcEntity>> COURIER_NPC = REGISTER.register(
      AWCoreIds.COURIER_NPC.getPath(),
      () -> Builder.of(CourierNpcEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(8).build(AWCoreIds.COURIER_NPC.toString())
   );
   public static final RegistryObject<EntityType<TraderNpcEntity>> TRADER_NPC = REGISTER.register(
      AWCoreIds.TRADER_NPC.getPath(),
      () -> Builder.of(TraderNpcEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(8).build(AWCoreIds.TRADER_NPC.toString())
   );
   public static final RegistryObject<EntityType<BardNpcEntity>> BARD_NPC = REGISTER.register(
      AWCoreIds.BARD_NPC.getPath(),
      () -> Builder.of(BardNpcEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(8).build(AWCoreIds.BARD_NPC.toString())
   );
   public static final RegistryObject<EntityType<PriestNpcEntity>> PRIEST_NPC = REGISTER.register(
      AWCoreIds.PRIEST_NPC.getPath(),
      () -> Builder.of(PriestNpcEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(8).build(AWCoreIds.PRIEST_NPC.toString())
   );
   public static final RegistryObject<EntityType<SiegeEngineerNpcEntity>> SIEGE_ENGINEER_NPC = REGISTER.register(
      AWCoreIds.SIEGE_ENGINEER_NPC.getPath(),
      () -> Builder.of(SiegeEngineerNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.SIEGE_ENGINEER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionSoldierNpcEntity>> FACTION_SOLDIER_NPC = REGISTER.register(
      AWCoreIds.FACTION_SOLDIER_NPC.getPath(),
      () -> Builder.of(FactionSoldierNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_SOLDIER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionSoldierEliteNpcEntity>> FACTION_SOLDIER_ELITE_NPC = REGISTER.register(
      AWCoreIds.FACTION_SOLDIER_ELITE_NPC.getPath(),
      () -> Builder.of(FactionSoldierEliteNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_SOLDIER_ELITE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionArcherNpcEntity>> FACTION_ARCHER_NPC = REGISTER.register(
      AWCoreIds.FACTION_ARCHER_NPC.getPath(),
      () -> Builder.of(FactionArcherNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_ARCHER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionArcherEliteNpcEntity>> FACTION_ARCHER_ELITE_NPC = REGISTER.register(
      AWCoreIds.FACTION_ARCHER_ELITE_NPC.getPath(),
      () -> Builder.of(FactionArcherEliteNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_ARCHER_ELITE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionCavalryNpcEntity>> FACTION_CAVALRY_NPC = REGISTER.register(
      AWCoreIds.FACTION_CAVALRY_NPC.getPath(),
      () -> Builder.of(FactionCavalryNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_CAVALRY_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionMountedArcherNpcEntity>> FACTION_MOUNTED_ARCHER_NPC = REGISTER.register(
      AWCoreIds.FACTION_MOUNTED_ARCHER_NPC.getPath(),
      () -> Builder.of(FactionMountedArcherNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_MOUNTED_ARCHER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionLeaderNpcEntity>> FACTION_LEADER_NPC = REGISTER.register(
      AWCoreIds.FACTION_LEADER_NPC.getPath(),
      () -> Builder.of(FactionLeaderNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_LEADER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionLeaderEliteNpcEntity>> FACTION_LEADER_ELITE_NPC = REGISTER.register(
      AWCoreIds.FACTION_LEADER_ELITE_NPC.getPath(),
      () -> Builder.of(FactionLeaderEliteNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_LEADER_ELITE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionSiegeEngineerNpcEntity>> FACTION_SIEGE_ENGINEER_NPC = REGISTER.register(
      AWCoreIds.FACTION_SIEGE_ENGINEER_NPC.getPath(),
      () -> Builder.of(FactionSiegeEngineerNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_SIEGE_ENGINEER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionCivilianMaleNpcEntity>> FACTION_CIVILIAN_MALE_NPC = REGISTER.register(
      AWCoreIds.FACTION_CIVILIAN_MALE_NPC.getPath(),
      () -> Builder.of(FactionCivilianMaleNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_CIVILIAN_MALE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionCivilianFemaleNpcEntity>> FACTION_CIVILIAN_FEMALE_NPC = REGISTER.register(
      AWCoreIds.FACTION_CIVILIAN_FEMALE_NPC.getPath(),
      () -> Builder.of(FactionCivilianFemaleNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_CIVILIAN_FEMALE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionPriestNpcEntity>> FACTION_PRIEST_NPC = REGISTER.register(
      AWCoreIds.FACTION_PRIEST_NPC.getPath(),
      () -> Builder.of(FactionPriestNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_PRIEST_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionBardNpcEntity>> FACTION_BARD_NPC = REGISTER.register(
      AWCoreIds.FACTION_BARD_NPC.getPath(),
      () -> Builder.of(FactionBardNpcEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(8).build(AWCoreIds.FACTION_BARD_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionTraderNpcEntity>> FACTION_TRADER_NPC = REGISTER.register(
      AWCoreIds.FACTION_TRADER_NPC.getPath(),
      () -> Builder.of(FactionTraderNpcEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build(AWCoreIds.FACTION_TRADER_NPC.toString())
   );

   private AWCoreEntityTypes() {
   }
}
