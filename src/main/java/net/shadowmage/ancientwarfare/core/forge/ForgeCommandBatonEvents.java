package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeCommandBatonEvents {
   private ForgeCommandBatonEvents() {
   }

   @SubscribeEvent
   public static void onAttackEntity(AttackEntityEvent event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide && player.level() instanceof ServerLevel level && event.getTarget() instanceof LivingEntity target) {
         ItemStack stack = player.getMainHandItem();
         if (CommandBatonItem.isAttackMode(stack)) {
            int commanded = CommandBatonItem.commandAttack(level, player, stack, target);
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.attack_command", new Object[]{commanded}), true);
            event.setCanceled(true);
         }
      }
   }
}
