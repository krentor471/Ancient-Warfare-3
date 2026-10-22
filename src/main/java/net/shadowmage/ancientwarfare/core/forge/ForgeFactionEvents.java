package net.shadowmage.ancientwarfare.core.forge;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.shadowmage.ancientwarfare.core.faction.FactionArmies;
import net.shadowmage.ancientwarfare.core.faction.FactionRulers;
import net.shadowmage.ancientwarfare.core.faction.PlayerDiplomacy;
import net.shadowmage.ancientwarfare.core.faction.FactionTournaments;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.faction.FactionCaravans;
import net.shadowmage.ancientwarfare.core.faction.FactionDiplomacy;
import net.shadowmage.ancientwarfare.core.faction.FactionEnvoys;
import net.shadowmage.ancientwarfare.core.faction.FactionSimulation;
import net.shadowmage.ancientwarfare.core.faction.FactionWorldData;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeFactionEvents {
   private ForgeFactionEvents() {
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         FactionSimulation.tick(event.getServer().overworld());
      }
   }

   /** Food given to a faction's people in a settlement going short. */
   @SubscribeEvent
   public static void onFeedFactionNpc(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
      if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
         && event.getTarget() instanceof FactionNpcEntity npc
         && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
         && net.shadowmage.ancientwarfare.core.faction.FactionLife.onFeed(player, npc, event.getItemStack())) {
         event.setCanceled(true);
         event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
      }
   }

   /** Blows in a tournament bout. */
   @SubscribeEvent
   public static void onTournamentDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
      if (!event.getEntity().level().isClientSide
         && FactionTournaments.onDamage(event.getEntity(), event.getSource().getEntity(), event.getAmount())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
         FactionArmies.onLogin(player);
      }
   }

   @SubscribeEvent
   public static void onFactionNpcJoin(EntityJoinLevelEvent event) {
      if (event.getEntity() instanceof FactionNpcEntity npc
         && event.getLevel() instanceof ServerLevel serverLevel
         && serverLevel.dimension() == Level.OVERWORLD
         && (FactionEnvoys.isStaleEnvoy(npc) || FactionTournaments.isStaleChampion(npc) || !FactionSimulation.onNpcJoin(serverLevel, npc))) {
         event.setCanceled(true);
      } else if (event.getEntity() instanceof FactionNpcEntity npc && event.getLevel() instanceof ServerLevel serverLevel && PlayerDiplomacy.isLeader(npc)) {
         FactionRulers.onLeaderJoin(serverLevel, npc);
      }
   }

   /** Loads the faction world with the overworld, so its region scale is known before any land is generated. */
   @SubscribeEvent
   public static void onLevelLoad(LevelEvent.Load event) {
      if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
         FactionWorldData.get(level);
      }
   }

   @SubscribeEvent
   public static void onCaravanAnimalJoin(EntityJoinLevelEvent event) {
      if ((event.getEntity() instanceof AbstractHorse || event.getEntity() instanceof ChestPushcartEntity)
         && event.getLevel() instanceof ServerLevel
         && FactionCaravans.isStale(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onFactionNpcDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof FactionNpcEntity npc && npc.level() instanceof ServerLevel serverLevel) {
         generateConfiguredLoot(npc, serverLevel, event.getSource());
         FactionSimulation.onNpcDeath(serverLevel, npc);
         if (FactionRulers.isRuler(npc)) {
            FactionRulers.onLeaderDeath(serverLevel, npc);
         }
         if (event.getSource().getEntity() instanceof Player player) {
            FactionCaravans.onNpcKilled(serverLevel, npc, player);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
               FactionEnvoys.onKill(serverPlayer, npc);
            }

            String faction = npc.getFactionName();
            FactionStandingData.get(serverLevel).adjustStanding(player.getUUID(), faction, FactionRegistry.get(faction).killStandingChange());
         } else if (event.getSource().getEntity() instanceof FactionNpcEntity killer) {
            FactionDiplomacy.adjustOpinion(serverLevel, npc.getFactionName(), killer.getFactionName(), FactionDiplomacy.KILL_OPINION);
         }

         return;
      }
   }

   private static void generateConfiguredLoot(FactionNpcEntity npc, ServerLevel level, DamageSource damageSource) {
      FactionNpcRole role = FactionNpcRole.byId(npc.getNpcSubtype()).orElse(FactionNpcRole.SOLDIER);
      ResourceLocation tableId = FactionNpcDefaultsRegistry.lootTable(npc.getFactionName(), role).orElse(null);
      if (tableId != null && !tableId.equals(ResourceLocation.tryParse("minecraft:empty"))) {
         LootTable table = level.getServer().getLootData().getLootTable(tableId);
         Builder builder = new Builder(level)
            .withParameter(LootContextParams.ORIGIN, npc.position())
            .withParameter(LootContextParams.THIS_ENTITY, npc)
            .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource);
         LootParams params = builder.create(LootContextParamSets.ENTITY);
         ObjectListIterator var8 = table.getRandomItems(params).iterator();

         while (var8.hasNext()) {
            ItemStack stack = (ItemStack)var8.next();
            if (!stack.isEmpty()) {
               ItemEntity item = new ItemEntity(level, npc.getX(), npc.getY() + 0.2, npc.getZ(), stack);
               item.setDefaultPickUpDelay();
               level.addFreshEntity(item);
            }
         }
      }
   }
}
