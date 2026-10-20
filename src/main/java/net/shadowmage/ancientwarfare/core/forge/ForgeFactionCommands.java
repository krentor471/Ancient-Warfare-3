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
      RequiredArgumentBuilder<CommandSourceStack, String> factionArgument = Commands.m_82129_("faction", StringArgumentType.word())
         .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(FactionRegistry.factionNames(), builder));
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("awfaction")
                     .then(
                        Commands.m_82127_("get")
                           .then(
                              factionArgument.executes(
                                 context -> showStanding(((CommandSourceStack)context.getSource()).m_81375_(), StringArgumentType.getString(context, "faction"))
                              )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.m_82127_("set").requires(source -> source.m_6761_(2)))
                        .then(
                           Commands.m_82129_("faction", StringArgumentType.word())
                              .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(FactionRegistry.factionNames(), builder))
                              .then(
                                 Commands.m_82129_("value", IntegerArgumentType.integer())
                                    .executes(
                                       context -> setStanding(
                                             ((CommandSourceStack)context.getSource()).m_81375_(),
                                             StringArgumentType.getString(context, "faction"),
                                             IntegerArgumentType.getInteger(context, "value")
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.m_82127_("add").requires(source -> source.m_6761_(2)))
                     .then(
                        Commands.m_82129_("faction", StringArgumentType.word())
                           .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(FactionRegistry.factionNames(), builder))
                           .then(
                              Commands.m_82129_("value", IntegerArgumentType.integer())
                                 .executes(
                                    context -> addStanding(
                                          ((CommandSourceStack)context.getSource()).m_81375_(),
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
      int standing = FactionStandingData.get(player.m_284548_()).getStanding(player.m_20148_(), faction);
      player.m_213846_(Component.m_237113_("Faction " + faction + " standing: " + standing + (standing < 0 ? " (hostile)" : " (neutral/friendly)")));
      return standing;
   }

   private static int setStanding(ServerPlayer player, String faction, int value) {
      int standing = FactionStandingData.get(player.m_284548_()).setStanding(player.m_20148_(), faction, value);
      player.m_213846_(Component.m_237113_("Faction " + faction + " standing set to " + standing));
      return standing;
   }

   private static int addStanding(ServerPlayer player, String faction, int value) {
      int standing = FactionStandingData.get(player.m_284548_()).adjustStanding(player.m_20148_(), faction, value);
      player.m_213846_(Component.m_237113_("Faction " + faction + " standing is now " + standing));
      return standing;
   }
}
