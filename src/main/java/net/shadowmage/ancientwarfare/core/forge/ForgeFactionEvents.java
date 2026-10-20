package net.shadowmage.ancientwarfare.core.forge;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.faction.FactionDiplomacy;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeFactionEvents {
   private ForgeFactionEvents() {
   }

   @SubscribeEvent
   public static void onFactionNpcDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof FactionNpcEntity npc && npc.level() instanceof ServerLevel serverLevel) {
         generateConfiguredLoot(npc, serverLevel, event.getSource());
         if (event.getSource().getEntity() instanceof Player player) {
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
