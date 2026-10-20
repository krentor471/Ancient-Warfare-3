package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.entity.BardNpcEntity;
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
import net.shadowmage.ancientwarfare.core.entity.ModernSoldierNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.PriestNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.ShipCaptainNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.SiegeEngineerNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.TraderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.WorkerNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.MOD
)
public final class ForgeCommonEvents {
   private ForgeCommonEvents() {
   }

   @SubscribeEvent
   public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
      event.put((EntityType)AWCoreEntityTypes.WORKER_NPC.get(), WorkerNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.COMBAT_NPC.get(), CombatNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.COURIER_NPC.get(), CourierNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.TRADER_NPC.get(), TraderNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.BARD_NPC.get(), BardNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.PRIEST_NPC.get(), PriestNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.SIEGE_ENGINEER_NPC.get(), SiegeEngineerNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.MODERN_SOLDIER_NPC.get(), ModernSoldierNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.SHIP_CAPTAIN_NPC.get(), ShipCaptainNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_SOLDIER_NPC.get(), FactionSoldierNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_SOLDIER_ELITE_NPC.get(), FactionSoldierEliteNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_ARCHER_NPC.get(), FactionArcherNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_ARCHER_ELITE_NPC.get(), FactionArcherEliteNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_CAVALRY_NPC.get(), FactionCavalryNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_MOUNTED_ARCHER_NPC.get(), FactionMountedArcherNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_LEADER_NPC.get(), FactionLeaderNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_LEADER_ELITE_NPC.get(), FactionLeaderEliteNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_SIEGE_ENGINEER_NPC.get(), FactionSiegeEngineerNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_CIVILIAN_MALE_NPC.get(), FactionCivilianMaleNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_CIVILIAN_FEMALE_NPC.get(), FactionCivilianFemaleNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_PRIEST_NPC.get(), FactionPriestNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_SPELLCASTER_NPC.get(), FactionSpellcasterNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_BARD_NPC.get(), FactionBardNpcEntity.createAttributes().build());
      event.put((EntityType)AWCoreEntityTypes.FACTION_TRADER_NPC.get(), FactionTraderNpcEntity.createAttributes().build());
   }
}
