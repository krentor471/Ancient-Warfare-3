package net.shadowmage.ancientwarfare.core.forge;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.structure.AdvancedSpawnerWorldData;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeSpawnerCommands {
   private ForgeSpawnerCommands() {
   }

   @SubscribeEvent
   public static void registerCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("awspawner").requires(source -> source.m_6761_(2)))
                  .then(
                     Commands.m_82127_("creative")
                        .then(
                           Commands.m_82129_("enabled", BoolArgumentType.bool())
                              .executes(
                                 context -> setCreative(((CommandSourceStack)context.getSource()).m_81375_(), BoolArgumentType.getBool(context, "enabled"))
                              )
                        )
                  ))
               .then(Commands.m_82127_("status").executes(context -> status(((CommandSourceStack)context.getSource()).m_81375_())))
         );
   }

   private static int setCreative(ServerPlayer player, boolean enabled) {
      AdvancedSpawnerWorldData.get(player.m_284548_()).setCreativePlayersActivateSpawners(enabled);
      player.m_213846_(Component.m_237113_("Advanced spawner creative activation: " + (enabled ? "enabled" : "disabled")));
      return 1;
   }

   private static int status(ServerPlayer player) {
      boolean enabled = AdvancedSpawnerWorldData.get(player.m_284548_()).creativePlayersActivateSpawners();
      player.m_213846_(Component.m_237113_("Advanced spawner creative activation is " + (enabled ? "enabled" : "disabled")));
      return enabled ? 1 : 0;
   }
}
