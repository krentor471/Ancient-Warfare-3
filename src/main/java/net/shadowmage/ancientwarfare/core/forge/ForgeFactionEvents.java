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

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeFactionEvents {
   private ForgeFactionEvents() {
   }

   @SubscribeEvent
   public static void onFactionNpcDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof FactionNpcEntity npc && npc.m_9236_() instanceof ServerLevel serverLevel) {
         generateConfiguredLoot(npc, serverLevel, event.getSource());
         if (event.getSource().m_7639_() instanceof Player player) {
            String faction = npc.getFactionName();
            FactionStandingData.get(serverLevel).adjustStanding(player.m_20148_(), faction, FactionRegistry.get(faction).killStandingChange());
         }

         return;
      }
   }

   private static void generateConfiguredLoot(FactionNpcEntity npc, ServerLevel level, DamageSource damageSource) {
      FactionNpcRole role = FactionNpcRole.byId(npc.getNpcSubtype()).orElse(FactionNpcRole.SOLDIER);
      ResourceLocation tableId = FactionNpcDefaultsRegistry.lootTable(npc.getFactionName(), role).orElse(null);
      if (tableId != null && !tableId.equals(ResourceLocation.m_135820_("minecraft:empty"))) {
         LootTable table = level.m_7654_().m_278653_().m_278676_(tableId);
         Builder builder = new Builder(level)
            .m_287286_(LootContextParams.f_81460_, npc.m_20182_())
            .m_287286_(LootContextParams.f_81455_, npc)
            .m_287286_(LootContextParams.f_81457_, damageSource);
         LootParams params = builder.m_287235_(LootContextParamSets.f_81415_);
         ObjectListIterator var8 = table.m_287195_(params).iterator();

         while (var8.hasNext()) {
            ItemStack stack = (ItemStack)var8.next();
            if (!stack.m_41619_()) {
               ItemEntity item = new ItemEntity(level, npc.m_20185_(), npc.m_20186_() + 0.2, npc.m_20189_(), stack);
               item.m_32060_();
               level.m_7967_(item);
            }
         }
      }
   }
}
