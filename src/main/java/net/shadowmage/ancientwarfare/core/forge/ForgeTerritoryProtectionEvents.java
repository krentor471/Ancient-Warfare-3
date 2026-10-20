package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.structure.TerritoryProtectionData;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeTerritoryProtectionEvents {
   private ForgeTerritoryProtectionEvents() {
   }

   public static void onBreakSpeed(BreakSpeed event) {
      if (event.getEntity().m_9236_() instanceof ServerLevel serverLevel
         && TerritoryProtectionData.get(serverLevel).isProtected(event.getPosition().orElse(event.getEntity().m_20183_()))) {
         event.setNewSpeed(event.getNewSpeed() * 0.5F);
      }
   }
}
