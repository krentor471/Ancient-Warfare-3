package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireDragonCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippocampusCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippogryphCompat;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.DragonRiderController;
import net.shadowmage.ancientwarfare.core.entity.HippocampusRiderController;
import net.shadowmage.ancientwarfare.core.entity.HippogryphRiderController;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeDragonRiderEvents {
   private ForgeDragonRiderEvents() {
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST,
      receiveCanceled = true
   )
   public static void onEntityInteract(EntityInteract event) {
      Player player = event.getEntity();
      ItemStack stack = player.getItemInHand(event.getHand());
      if (event.getTarget() instanceof LivingEntity target) {
         boolean var10 = IceAndFireDragonCompat.isDragon(target);
         boolean hippogryph = IceAndFireHippogryphCompat.isHippogryph(target);
         boolean hippocampus = IceAndFireHippocampusCompat.isHippocampus(target);
         if (var10 || hippogryph || hippocampus) {
            CombatNpcEntity rider = controlledRider(player.level(), target);
            if (!(stack.getItem() instanceof CommandBatonItem)) {
               if (rider != null && rider.hasCommandPermission(player)) {
                  if (!player.level().isClientSide) {
                     rider.openNpcInventory(player);
                  }

                  event.setCancellationResult(InteractionResult.SUCCESS);
                  event.setCanceled(true);
               }
            } else if (!player.level().isClientSide && player.level() instanceof ServerLevel level) {
               if (CommandBatonItem.isMountMode(stack)) {
                  int commanded = CommandBatonItem.commandMount(level, player, stack, target);
                  player.displayClientMessage(Component.translatable("guistrings.npc.baton.mount_command", new Object[]{commanded}), true);
               } else {
                  rider = controlledRider(level, target);
                  if (rider == null || !rider.hasCommandPermission(player)) {
                     return;
                  }

                  CommandBatonItem.toggleNpcSelection(stack, player, rider);
               }

               event.setCancellationResult(InteractionResult.SUCCESS);
               event.setCanceled(true);
            } else {
               if (rider != null) {
                  event.setCancellationResult(InteractionResult.SUCCESS);
                  event.setCanceled(true);
               }
            }
         }
      }
   }

   private static CombatNpcEntity controlledRider(Level level, LivingEntity mount) {
      for (Entity passenger : mount.getPassengers()) {
         if (passenger instanceof CombatNpcEntity) {
            return (CombatNpcEntity)passenger;
         }
      }

      return !IceAndFireDragonCompat.isDragon(mount)
         ? null
         : (CombatNpcEntity)level.getEntitiesOfClass(
               CombatNpcEntity.class,
               mount.getBoundingBox().inflate(24.0),
               rider -> rider.getDragonRiderMountId() == mount.getId() || mount.getUUID().equals(rider.getDragonRiderMountUuid())
            )
            .stream()
            .findFirst()
            .orElse(null);
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         MinecraftServer server = event.getServer();

         for (ServerLevel level : server.getAllLevels()) {
            DragonRiderController.postServerTick(level);
            HippogryphRiderController.postServerTick(level);
            HippocampusRiderController.postServerTick(level);
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            DragonRiderController.postClientTick(level.entitiesForRendering());
         }
      }
   }
}
