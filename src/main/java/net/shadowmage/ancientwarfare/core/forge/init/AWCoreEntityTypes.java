package net.shadowmage.ancientwarfare.core.forge.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.compat.IronsSpellbooksCompat;
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
import net.shadowmage.ancientwarfare.core.entity.FactionSpellcasterNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionTraderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;
import net.shadowmage.ancientwarfare.core.entity.GiantTrebuchetEntity;
import net.shadowmage.ancientwarfare.core.entity.HwachaEntity;
import net.shadowmage.ancientwarfare.core.entity.HwachaRocketEntity;
import net.shadowmage.ancientwarfare.core.entity.ModernSoldierNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.PriestNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.ShipCaptainNpcEntity;
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
      () -> Builder.m_20704_(ChestPushcartEntity::new, MobCategory.MISC)
            .m_20699_(2.7F, 1.8F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("chest_pushcart").toString())
   );
   public static final RegistryObject<EntityType<BallistaMobileEntity>> BALLISTA_MOBILE = REGISTER.register(
      "ballista_mobile",
      () -> Builder.m_20704_(BallistaMobileEntity::new, MobCategory.MISC)
            .m_20699_(2.7F, 1.4F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("ballista_mobile").toString())
   );
   public static final RegistryObject<EntityType<BallistaBoatEntity>> BALLISTA_BOAT = REGISTER.register(
      "ballista_boat",
      () -> Builder.m_20704_(BallistaBoatEntity::new, MobCategory.MISC)
            .m_20699_(2.7F, 1.4F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("ballista_boat").toString())
   );
   public static final RegistryObject<EntityType<BallistaTurretStandEntity>> BALLISTA_TURRET_STAND = REGISTER.register(
      "ballista_turret_stand",
      () -> Builder.m_20704_(BallistaTurretStandEntity::new, MobCategory.MISC)
            .m_20699_(1.2F, 1.4F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("ballista_turret_stand").toString())
   );
   public static final RegistryObject<EntityType<BallistaStandFixedEntity>> BALLISTA_STAND_FIXED = REGISTER.register(
      "ballista_stand_fixed",
      () -> Builder.m_20704_(BallistaStandFixedEntity::new, MobCategory.MISC)
            .m_20699_(1.2F, 1.4F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("ballista_stand_fixed").toString())
   );
   public static final RegistryObject<EntityType<CatapultMobileEntity>> CATAPULT_MOBILE = REGISTER.register(
      "catapult_mobile",
      () -> Builder.m_20704_(CatapultMobileEntity::new, MobCategory.MISC)
            .m_20699_(2.7F, 2.0F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("catapult_mobile").toString())
   );
   public static final RegistryObject<EntityType<TrebuchetMobileEntity>> TREBUCHET_MOBILE = REGISTER.register(
      "trebuchet_mobile",
      () -> Builder.m_20704_(TrebuchetMobileEntity::new, MobCategory.MISC)
            .m_20699_(2.7F, 2.7F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("trebuchet_mobile").toString())
   );
   public static final RegistryObject<EntityType<TrebuchetFixedEntity>> TREBUCHET_FIXED = REGISTER.register(
      "trebuchet_fixed",
      () -> Builder.m_20704_(TrebuchetFixedEntity::new, MobCategory.MISC)
            .m_20699_(2.0F, 2.0F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("trebuchet_fixed").toString())
   );
   public static final RegistryObject<EntityType<TrebuchetTurretEntity>> TREBUCHET_TURRET = REGISTER.register(
      "trebuchet_turret",
      () -> Builder.m_20704_(TrebuchetTurretEntity::new, MobCategory.MISC)
            .m_20699_(2.7F, 2.7F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("trebuchet_turret").toString())
   );
   public static final RegistryObject<EntityType<GiantTrebuchetEntity>> GIANT_TREBUCHET = REGISTER.register(
      "giant_trebuchet",
      () -> Builder.m_20704_(GiantTrebuchetEntity::new, MobCategory.MISC)
            .m_20699_(5.0F, 5.0F)
            .m_20702_(12)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("giant_trebuchet").toString())
   );
   public static final RegistryObject<EntityType<CatapultTurretStandEntity>> CATAPULT_TURRET_STAND = REGISTER.register(
      "catapult_turret_stand",
      () -> Builder.m_20704_(CatapultTurretStandEntity::new, MobCategory.MISC)
            .m_20699_(2.7F, 2.0F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("catapult_turret_stand").toString())
   );
   public static final RegistryObject<EntityType<CatapultBoatEntity>> CATAPULT_BOAT = REGISTER.register(
      "catapult_boat",
      () -> Builder.m_20704_(CatapultBoatEntity::new, MobCategory.MISC)
            .m_20699_(2.7F, 1.4F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("catapult_boat").toString())
   );
   public static final RegistryObject<EntityType<TransportBoatEntity>> TRANSPORT_BOAT = REGISTER.register(
      "transport_boat",
      () -> Builder.m_20704_(TransportBoatEntity::new, MobCategory.MISC)
            .m_20699_(2.8F, 1.8F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("transport_boat").toString())
   );
   public static final RegistryObject<EntityType<BallistaBoltEntity>> BALLISTA_BOLT = REGISTER.register(
      "ballista_bolt",
      () -> Builder.m_20704_(BallistaBoltEntity::new, MobCategory.MISC)
            .m_20699_(0.35F, 0.35F)
            .m_20702_(8)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("ballista_bolt").toString())
   );
   public static final RegistryObject<EntityType<CatapultShotEntity>> CATAPULT_SHOT = REGISTER.register(
      "catapult_shot",
      () -> Builder.m_20704_(CatapultShotEntity::new, MobCategory.MISC)
            .m_20699_(0.85F, 0.85F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("catapult_shot").toString())
   );
   public static final RegistryObject<EntityType<CannonTurretStandEntity>> CANNON_TURRET_STAND = REGISTER.register(
      "cannon_turret_stand",
      () -> Builder.m_20704_(CannonTurretStandEntity::new, MobCategory.MISC)
            .m_20699_(2.7F, 2.0F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("cannon_turret_stand").toString())
   );
   public static final RegistryObject<EntityType<CannonMobileEntity>> CANNON_MOBILE = REGISTER.register(
      "cannon_mobile",
      () -> Builder.m_20704_(CannonMobileEntity::new, MobCategory.MISC)
            .m_20699_(1.9F, 1.5F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("cannon_mobile").toString())
   );
   public static final RegistryObject<EntityType<CannonShotEntity>> CANNON_SHOT = REGISTER.register(
      "cannon_shot",
      () -> Builder.m_20704_(CannonShotEntity::new, MobCategory.MISC)
            .m_20699_(0.5F, 0.5F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("cannon_shot").toString())
   );
   public static final RegistryObject<EntityType<BatteringRamEntity>> BATTERING_RAM = REGISTER.register(
      "battering_ram",
      () -> Builder.m_20704_(BatteringRamEntity::new, MobCategory.MISC)
            .m_20699_(2.9F, 2.1F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("battering_ram").toString())
   );
   public static final RegistryObject<EntityType<HwachaEntity>> HWACHA = REGISTER.register(
      "hwacha",
      () -> Builder.m_20704_(HwachaEntity::new, MobCategory.MISC).m_20699_(2.8F, 2.0F).m_20702_(10).m_20717_(1).m_20712_(AWCoreIds.id("hwacha").toString())
   );
   public static final RegistryObject<EntityType<HwachaRocketEntity>> HWACHA_ROCKET = REGISTER.register(
      "hwacha_rocket",
      () -> Builder.m_20704_(HwachaRocketEntity::new, MobCategory.MISC)
            .m_20699_(0.25F, 0.25F)
            .m_20702_(10)
            .m_20717_(1)
            .m_20712_(AWCoreIds.id("hwacha_rocket").toString())
   );
   public static final RegistryObject<EntityType<GateEntity>> DYNAMIC_GATE = REGISTER.register(
      AWCoreIds.DYNAMIC_GATE.m_135815_(),
      () -> Builder.m_20704_(GateEntity::new, MobCategory.MISC).m_20699_(1.0F, 1.0F).m_20702_(16).m_20717_(1).m_20712_(AWCoreIds.DYNAMIC_GATE.toString())
   );
   public static final RegistryObject<EntityType<WorkerNpcEntity>> WORKER_NPC = REGISTER.register(
      AWCoreIds.WORKER_NPC.m_135815_(),
      () -> Builder.m_20704_(WorkerNpcEntity::new, MobCategory.CREATURE).m_20699_(0.6F, 1.95F).m_20702_(8).m_20712_(AWCoreIds.WORKER_NPC.toString())
   );
   public static final RegistryObject<EntityType<CombatNpcEntity>> COMBAT_NPC = REGISTER.register(
      AWCoreIds.COMBAT_NPC.m_135815_(),
      () -> Builder.m_20704_(IronsSpellbooksCompat::createCombatNpc, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.COMBAT_NPC.toString())
   );
   public static final RegistryObject<EntityType<CourierNpcEntity>> COURIER_NPC = REGISTER.register(
      AWCoreIds.COURIER_NPC.m_135815_(),
      () -> Builder.m_20704_(CourierNpcEntity::new, MobCategory.CREATURE).m_20699_(0.6F, 1.95F).m_20702_(8).m_20712_(AWCoreIds.COURIER_NPC.toString())
   );
   public static final RegistryObject<EntityType<TraderNpcEntity>> TRADER_NPC = REGISTER.register(
      AWCoreIds.TRADER_NPC.m_135815_(),
      () -> Builder.m_20704_(TraderNpcEntity::new, MobCategory.CREATURE).m_20699_(0.6F, 1.95F).m_20702_(8).m_20712_(AWCoreIds.TRADER_NPC.toString())
   );
   public static final RegistryObject<EntityType<BardNpcEntity>> BARD_NPC = REGISTER.register(
      AWCoreIds.BARD_NPC.m_135815_(),
      () -> Builder.m_20704_(BardNpcEntity::new, MobCategory.CREATURE).m_20699_(0.6F, 1.95F).m_20702_(8).m_20712_(AWCoreIds.BARD_NPC.toString())
   );
   public static final RegistryObject<EntityType<PriestNpcEntity>> PRIEST_NPC = REGISTER.register(
      AWCoreIds.PRIEST_NPC.m_135815_(),
      () -> Builder.m_20704_(PriestNpcEntity::new, MobCategory.CREATURE).m_20699_(0.6F, 1.95F).m_20702_(8).m_20712_(AWCoreIds.PRIEST_NPC.toString())
   );
   public static final RegistryObject<EntityType<SiegeEngineerNpcEntity>> SIEGE_ENGINEER_NPC = REGISTER.register(
      AWCoreIds.SIEGE_ENGINEER_NPC.m_135815_(),
      () -> Builder.m_20704_(SiegeEngineerNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.SIEGE_ENGINEER_NPC.toString())
   );
   public static final RegistryObject<EntityType<ModernSoldierNpcEntity>> MODERN_SOLDIER_NPC = REGISTER.register(
      AWCoreIds.MODERN_SOLDIER_NPC.m_135815_(),
      () -> Builder.m_20704_(ModernSoldierNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.MODERN_SOLDIER_NPC.toString())
   );
   public static final RegistryObject<EntityType<ShipCaptainNpcEntity>> SHIP_CAPTAIN_NPC = REGISTER.register(
      AWCoreIds.SHIP_CAPTAIN_NPC.m_135815_(),
      () -> Builder.m_20704_(ShipCaptainNpcEntity::new, MobCategory.CREATURE).m_20699_(0.6F, 1.95F).m_20702_(8).m_20712_(AWCoreIds.SHIP_CAPTAIN_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionSoldierNpcEntity>> FACTION_SOLDIER_NPC = REGISTER.register(
      AWCoreIds.FACTION_SOLDIER_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionSoldierNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_SOLDIER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionSoldierEliteNpcEntity>> FACTION_SOLDIER_ELITE_NPC = REGISTER.register(
      AWCoreIds.FACTION_SOLDIER_ELITE_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionSoldierEliteNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_SOLDIER_ELITE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionArcherNpcEntity>> FACTION_ARCHER_NPC = REGISTER.register(
      AWCoreIds.FACTION_ARCHER_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionArcherNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_ARCHER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionArcherEliteNpcEntity>> FACTION_ARCHER_ELITE_NPC = REGISTER.register(
      AWCoreIds.FACTION_ARCHER_ELITE_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionArcherEliteNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_ARCHER_ELITE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionCavalryNpcEntity>> FACTION_CAVALRY_NPC = REGISTER.register(
      AWCoreIds.FACTION_CAVALRY_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionCavalryNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_CAVALRY_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionMountedArcherNpcEntity>> FACTION_MOUNTED_ARCHER_NPC = REGISTER.register(
      AWCoreIds.FACTION_MOUNTED_ARCHER_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionMountedArcherNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_MOUNTED_ARCHER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionLeaderNpcEntity>> FACTION_LEADER_NPC = REGISTER.register(
      AWCoreIds.FACTION_LEADER_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionLeaderNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_LEADER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionLeaderEliteNpcEntity>> FACTION_LEADER_ELITE_NPC = REGISTER.register(
      AWCoreIds.FACTION_LEADER_ELITE_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionLeaderEliteNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_LEADER_ELITE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionSiegeEngineerNpcEntity>> FACTION_SIEGE_ENGINEER_NPC = REGISTER.register(
      AWCoreIds.FACTION_SIEGE_ENGINEER_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionSiegeEngineerNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_SIEGE_ENGINEER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionCivilianMaleNpcEntity>> FACTION_CIVILIAN_MALE_NPC = REGISTER.register(
      AWCoreIds.FACTION_CIVILIAN_MALE_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionCivilianMaleNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_CIVILIAN_MALE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionCivilianFemaleNpcEntity>> FACTION_CIVILIAN_FEMALE_NPC = REGISTER.register(
      AWCoreIds.FACTION_CIVILIAN_FEMALE_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionCivilianFemaleNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_CIVILIAN_FEMALE_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionPriestNpcEntity>> FACTION_PRIEST_NPC = REGISTER.register(
      AWCoreIds.FACTION_PRIEST_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionPriestNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_PRIEST_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionSpellcasterNpcEntity>> FACTION_SPELLCASTER_NPC = REGISTER.register(
      AWCoreIds.FACTION_SPELLCASTER_NPC.m_135815_(),
      () -> Builder.m_20704_(IronsSpellbooksCompat::createFactionSpellcaster, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_SPELLCASTER_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionBardNpcEntity>> FACTION_BARD_NPC = REGISTER.register(
      AWCoreIds.FACTION_BARD_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionBardNpcEntity::new, MobCategory.CREATURE).m_20699_(0.6F, 1.95F).m_20702_(8).m_20712_(AWCoreIds.FACTION_BARD_NPC.toString())
   );
   public static final RegistryObject<EntityType<FactionTraderNpcEntity>> FACTION_TRADER_NPC = REGISTER.register(
      AWCoreIds.FACTION_TRADER_NPC.m_135815_(),
      () -> Builder.m_20704_(FactionTraderNpcEntity::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.95F)
            .m_20702_(8)
            .m_20712_(AWCoreIds.FACTION_TRADER_NPC.toString())
   );

   private AWCoreEntityTypes() {
   }
}
