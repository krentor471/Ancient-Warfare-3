package net.shadowmage.ancientwarfare.core.forge;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeFactionCommands {
   private ForgeFactionCommands() {
   }

   @SubscribeEvent
   public static void registerCommands(RegisterCommandsEvent event) {
      RequiredArgumentBuilder<CommandSourceStack, String> factionArgument = Commands.argument("faction", StringArgumentType.word())
         .suggests((context, builder) -> SharedSuggestionProvider.suggest(FactionRegistry.factionNames(), builder));
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("awfaction")
                     .then(
                        Commands.literal("get")
                           .then(
                              factionArgument.executes(
                                 context -> showStanding(((CommandSourceStack)context.getSource()).getPlayerOrException(), StringArgumentType.getString(context, "faction"))
                              )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("set").requires(source -> source.hasPermission(2)))
                        .then(
                           Commands.argument("faction", StringArgumentType.word())
                              .suggests((context, builder) -> SharedSuggestionProvider.suggest(FactionRegistry.factionNames(), builder))
                              .then(
                                 Commands.argument("value", IntegerArgumentType.integer())
                                    .executes(
                                       context -> setStanding(
                                             ((CommandSourceStack)context.getSource()).getPlayerOrException(),
                                             StringArgumentType.getString(context, "faction"),
                                             IntegerArgumentType.getInteger(context, "value")
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("add").requires(source -> source.hasPermission(2)))
                     .then(
                        Commands.argument("faction", StringArgumentType.word())
                           .suggests((context, builder) -> SharedSuggestionProvider.suggest(FactionRegistry.factionNames(), builder))
                           .then(
                              Commands.argument("value", IntegerArgumentType.integer())
                                 .executes(
                                    context -> addStanding(
                                          ((CommandSourceStack)context.getSource()).getPlayerOrException(),
                                          StringArgumentType.getString(context, "faction"),
                                          IntegerArgumentType.getInteger(context, "value")
                                       )
                                 )
                           )
                     )
               )
         );
   }

   private static int showStanding(ServerPlayer player, String faction) {
      int standing = FactionStandingData.get(player.serverLevel()).getStanding(player.getUUID(), faction);
      player.sendSystemMessage(Component.literal("Faction " + faction + " standing: " + standing + (standing < 0 ? " (hostile)" : " (neutral/friendly)")));
      return standing;
   }

   private static int setStanding(ServerPlayer player, String faction, int value) {
      int standing = FactionStandingData.get(player.serverLevel()).setStanding(player.getUUID(), faction, value);
      player.sendSystemMessage(Component.literal("Faction " + faction + " standing set to " + standing));
      return standing;
   }

   private static int addStanding(ServerPlayer player, String faction, int value) {
      int standing = FactionStandingData.get(player.serverLevel()).adjustStanding(player.getUUID(), faction, value);
      player.sendSystemMessage(Component.literal("Faction " + faction + " standing is now " + standing));
      return standing;
   }
}
