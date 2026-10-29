package net.shadowmage.ancientwarfare.core.forge;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.faction.FactionArmies;
import net.shadowmage.ancientwarfare.core.faction.FactionBandits;
import net.shadowmage.ancientwarfare.core.faction.FactionDiplomacy;
import net.shadowmage.ancientwarfare.core.faction.FactionCampaigns;
import net.shadowmage.ancientwarfare.core.faction.FactionCaravans;
import net.shadowmage.ancientwarfare.core.faction.FactionEnvoys;
import net.shadowmage.ancientwarfare.core.faction.FactionExpansion;
import net.shadowmage.ancientwarfare.core.faction.FactionMercenaries;
import net.shadowmage.ancientwarfare.core.faction.FactionRegions;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;
import net.shadowmage.ancientwarfare.core.faction.FactionSimulation;
import net.shadowmage.ancientwarfare.core.faction.FactionRoads;
import net.shadowmage.ancientwarfare.core.faction.FactionTrade;
import net.shadowmage.ancientwarfare.core.faction.FactionWorldData;
import net.shadowmage.ancientwarfare.core.faction.RelationState;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeFactionCommands {
   private static final int MAP_STEP = 256;
   private static final int MAP_HALF_WIDTH = 15;
   private static final int MAP_HALF_HEIGHT = 8;

   private ForgeFactionCommands() {
   }

   @SubscribeEvent
   public static void registerCommands(RegisterCommandsEvent event) {
      RequiredArgumentBuilder<CommandSourceStack, String> factionArgument = Commands.argument("faction", StringArgumentType.word())
         .suggests((context, builder) -> SharedSuggestionProvider.suggest(FactionRegistry.factionNames(), builder));
      registerWorldCommands(event);
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
               .then(
                  Commands.literal("treaty").requires(source -> source.hasPermission(2))
                     .then(
                        Commands.argument("faction", StringArgumentType.word())
                           .suggests((context, builder) -> SharedSuggestionProvider.suggest(FactionRegistry.factionNames(), builder))
                           .then(
                              Commands.argument("state", StringArgumentType.word())
                                 .suggests((context, builder) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(RelationState.values()).map(r -> r.name().toLowerCase(java.util.Locale.ROOT)), builder))
                                 .executes(
                                    context -> setTreaty(
                                          ((CommandSourceStack)context.getSource()).getPlayerOrException(),
                                          StringArgumentType.getString(context, "faction"),
                                          StringArgumentType.getString(context, "state")
                                       )
                                 )
                           )
                     )
               )
         );
   }

   private static void registerWorldCommands(RegisterCommandsEvent event) {
      // Run by clicking an envoy's buttons in chat.
      event.getDispatcher()
         .register(
            Commands.literal("awenvoy")
               .then(Commands.literal("accept").then(Commands.argument("offer", IntegerArgumentType.integer()).executes(context -> answerEnvoy(context, true))))
               .then(Commands.literal("decline").then(Commands.argument("offer", IntegerArgumentType.integer()).executes(context -> answerEnvoy(context, false))))
         );
      // Run by the buttons under a courtier's plot.
      event.getDispatcher()
         .register(Commands.literal("awfeast").then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
            net.shadowmage.ancientwarfare.core.faction.FactionFeasts.accept(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"));
            return 1;
         })));
      event.getDispatcher()
         .register(Commands.literal("awhunt").then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
            net.shadowmage.ancientwarfare.core.faction.FactionPastimes.accept(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"));
            return 1;
         })));
      event.getDispatcher()
         .register(Commands.literal("awpilgrim").then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
            net.shadowmage.ancientwarfare.core.faction.FactionPastimes.give(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"));
            return 1;
         })));
      event.getDispatcher()
         .register(Commands.literal("awrising").then(Commands.argument("settlement", IntegerArgumentType.integer()).executes(context -> {
            net.shadowmage.ancientwarfare.core.faction.FactionRebellions.help(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "settlement"));
            return 1;
         })));
      event.getDispatcher()
         .register(Commands.literal("awcivil").then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("side", StringArgumentType.word())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("ruler", "pretender"), builder))
            .executes(context -> {
               net.shadowmage.ancientwarfare.core.faction.FactionRebellions.back(
                  context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"), "pretender".equals(StringArgumentType.getString(context, "side"))
               );
               return 1;
            }))));
      event.getDispatcher()
         .register(Commands.literal("awoffering").then(Commands.argument("faith", StringArgumentType.word()).executes(context -> {
            net.shadowmage.ancientwarfare.core.faction.FactionFaith.offer(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faith"));
            return 1;
         })));
      event.getDispatcher()
         .register(Commands.literal("awquarrel").then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("role", IntegerArgumentType.integer()).executes(context -> {
            net.shadowmage.ancientwarfare.core.faction.FactionPastimes.side(
               context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"), IntegerArgumentType.getInteger(context, "role")
            );
            return 1;
         }))));
      event.getDispatcher()
         .register(Commands.literal("awfamily").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            net.shadowmage.ancientwarfare.core.faction.FactionFamilies.describe(player.serverLevel(), player.getUUID()).forEach(line -> player.sendSystemMessage(line));
            return 1;
         }));
      event.getDispatcher()
         .register(
            Commands.literal("awplot")
               .then(Commands.argument("answer", StringArgumentType.word())
                  .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("back", "warn", "ignore"), builder))
                  .then(Commands.argument("plot", IntegerArgumentType.integer()).executes(context -> {
                     net.shadowmage.ancientwarfare.core.faction.FactionPlots.answer(
                        context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "plot"), StringArgumentType.getString(context, "answer")
                     );
                     return 1;
                  })))
         );
      // Lists the rulers' secrets the player holds; "blackmail" and "publish" are run by its buttons.
      event.getDispatcher()
         .register(
            Commands.literal("awsecret")
               .executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionPlots.show(context.getSource().getPlayerOrException());
                  return 1;
               })
               .then(Commands.argument("use", StringArgumentType.word())
                  .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("blackmail", "publish"), builder))
                  .then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
                     net.shadowmage.ancientwarfare.core.faction.FactionPlots.use(
                        context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "use")
                     );
                     return 1;
                  })))
         );
      // Shows the sitting council of factions; "yes" and "no" are run by its buttons.
      event.getDispatcher()
         .register(
            Commands.literal("awcouncil")
               .executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionCouncil.show(context.getSource().getPlayerOrException());
                  return 1;
               })
               .then(Commands.literal("yes").executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionCouncil.cast(context.getSource().getPlayerOrException(), true);
                  return 1;
               }))
               .then(Commands.literal("no").executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionCouncil.cast(context.getSource().getPlayerOrException(), false);
                  return 1;
               }))
         );
      // Lists the player's vassals and overlord; "release" and "renounce" are run by its buttons.
      event.getDispatcher()
         .register(
            Commands.literal("awvassal")
               .executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionVassals.show(context.getSource().getPlayerOrException());
                  return 1;
               })
               .then(Commands.literal("release").then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionVassals.release(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"));
                  return 1;
               })))
               .then(Commands.literal("renounce").executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionVassals.renounce(context.getSource().getPlayerOrException());
                  return 1;
               }))
         );
      // Shows the coalition against the player; "bribe" is run by its buttons.
      event.getDispatcher()
         .register(
            Commands.literal("awcoalition")
               .executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionCoalitions.show(context.getSource().getPlayerOrException());
                  return 1;
               })
               .then(Commands.literal("bribe").then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionCoalitions.bribe(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"));
                  return 1;
               })))
         );
      // Run by clicking the buttons under a faction's demand after an incident.
      event.getDispatcher()
         .register(
            Commands.literal("awincident")
               .then(Commands.argument("answer", StringArgumentType.word())
                  .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("pay", "surrender", "refuse"), builder))
                  .then(Commands.argument("incident", IntegerArgumentType.integer()).executes(context -> {
                     if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        net.shadowmage.ancientwarfare.core.faction.FactionIncidents.answer(
                           player, IntegerArgumentType.getInteger(context, "incident"), StringArgumentType.getString(context, "answer")
                        );
                        return 1;
                     }

                     return 0;
                  })))
         );
      // Run by clicking the buttons under a caught spy in chat.
      event.getDispatcher()
         .register(
            Commands.literal("awspy")
               .then(Commands.argument("verdict", StringArgumentType.word())
                  .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("execute", "exchange", "turn"), builder))
                  .then(Commands.argument("spy", IntegerArgumentType.integer()).executes(context -> {
                     if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        net.shadowmage.ancientwarfare.core.faction.FactionCounterSpies.judge(
                           player, IntegerArgumentType.getInteger(context, "spy"), StringArgumentType.getString(context, "verdict")
                        );
                        return 1;
                     }

                     return 0;
                  })))
         );
      // Run by clicking the buttons of a besieger's ultimatum in chat.
      event.getDispatcher()
         .register(
            Commands.literal("awultimatum")
               .then(Commands.literal("pay").then(Commands.argument("army", IntegerArgumentType.integer()).executes(context -> answerUltimatum(context, true))))
               .then(Commands.literal("refuse").then(Commands.argument("army", IntegerArgumentType.integer()).executes(context -> answerUltimatum(context, false))))
         );
      // Run by clicking a barman's offer in chat.
      event.getDispatcher()
         .register(
            Commands.literal("awtavern")
               .then(
                  Commands.argument("barman", IntegerArgumentType.integer())
                     .then(
                        Commands.argument("offer", StringArgumentType.word())
                           .executes(
                              context -> serve(context.getSource(), IntegerArgumentType.getInteger(context, "barman"), StringArgumentType.getString(context, "offer"))
                           )
                     )
               )
         );
      // Run by clicking a traveller's offer in chat.
      event.getDispatcher()
         .register(
            Commands.literal("awroad")
               .then(
                  Commands.argument("group", IntegerArgumentType.integer())
                     .then(
                        Commands.argument("action", StringArgumentType.word())
                           .executes(context -> {
                              net.shadowmage.ancientwarfare.core.faction.FactionTravellers.act(
                                 context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "group"), StringArgumentType.getString(context, "action")
                              );
                              return 1;
                           })
                     )
               )
         );
      // Run by clicking a townsperson's offer to work for the player.
      event.getDispatcher()
         .register(
            Commands.literal("awtownsfolk")
               .then(
                  Commands.argument("npc", IntegerArgumentType.integer())
                     .then(Commands.literal("hire").executes(context -> {
                        net.shadowmage.ancientwarfare.core.faction.TownsfolkFavours.hire(
                           context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "npc")
                        );
                        return 1;
                     }))
               )
         );
      // Run by clicking a mercenary captain's offer in chat.
      event.getDispatcher()
         .register(
            Commands.literal("awhire")
               .then(
                  Commands.argument("captain", IntegerArgumentType.integer())
                     .then(
                        Commands.argument("offer", StringArgumentType.word())
                           .executes(
                              context -> hire(
                                 context.getSource(), IntegerArgumentType.getInteger(context, "captain"), StringArgumentType.getString(context, "offer")
                              )
                           )
                     )
               )
         );
      event.getDispatcher()
         .register(
            Commands.literal("awfaction")
               .then(Commands.literal("region").executes(context -> showRegion(context.getSource())))
               .then(Commands.literal("map").executes(context -> showMap(context.getSource())))
               .then(
                  Commands.literal("settlements")
                     .executes(context -> showSettlements(context.getSource(), 8))
                     .then(Commands.literal("all").executes(context -> showSettlements(context.getSource(), Integer.MAX_VALUE)))
               )
               .then(Commands.literal("factions").executes(context -> showFactions(context.getSource())))
               .then(
                  Commands.literal("town")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(Commands.literal("capital").executes(context -> buildTown(context.getSource(), faction(context, "faction"), true)))
                           .then(Commands.literal("village").executes(context -> buildTown(context.getSource(), faction(context, "faction"), false)))
                     )
               )
               .then(
                  Commands.literal("found")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(
                              Commands.argument("garrison", IntegerArgumentType.integer(0, 64))
                                 .executes(context -> found(context.getSource(), faction(context, "faction"), IntegerArgumentType.getInteger(context, "garrison")))
                           )
                     )
               )
               .then(
                  Commands.literal("banner")
                     .requires(source -> source.hasPermission(2))
                     .then(factionArgument("faction").executes(context -> banner(context.getSource(), faction(context, "faction"))))
               )
               .then(
                  Commands.literal("envoy")
                     .requires(source -> source.hasPermission(2))
                     .executes(context -> envoy(context.getSource(), ""))
                     .then(Commands.argument("kind", StringArgumentType.word()).executes(context -> envoy(context.getSource(), StringArgumentType.getString(context, "kind"))))
               )
               .then(
                  Commands.literal("tavern")
                     .requires(source -> source.hasPermission(2))
                     .then(factionArgument("faction").executes(context -> tavern(context.getSource(), faction(context, "faction"))))
               )
               .then(
                  Commands.literal("raid")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(
                              Commands.argument("size", IntegerArgumentType.integer(1, 12))
                                 .executes(context -> raid(context.getSource(), faction(context, "faction"), IntegerArgumentType.getInteger(context, "size"), false))
                           )
                     )
               )
               .then(
                  Commands.literal("siege")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(
                              Commands.argument("size", IntegerArgumentType.integer(1, 100))
                                 .executes(context -> raid(context.getSource(), faction(context, "faction"), IntegerArgumentType.getInteger(context, "size"), true))
                           )
                     )
               )
               .then(
                  Commands.literal("march")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(
                              Commands.argument("settlement", IntegerArgumentType.integer(1))
                                 .then(
                                    Commands.argument("size", IntegerArgumentType.integer(1, 100))
                                       .executes(
                                          context -> march(
                                             context.getSource(),
                                             faction(context, "faction"),
                                             IntegerArgumentType.getInteger(context, "settlement"),
                                             IntegerArgumentType.getInteger(context, "size")
                                          )
                                       )
                                 )
                           )
                     )
               )
               .then(
                  Commands.literal("assault")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(
                              Commands.argument("settlement", IntegerArgumentType.integer(1))
                                 .then(
                                    Commands.argument("size", IntegerArgumentType.integer(1, 100))
                                       .executes(
                                          context -> assault(
                                             context.getSource(),
                                             faction(context, "faction"),
                                             IntegerArgumentType.getInteger(context, "settlement"),
                                             IntegerArgumentType.getInteger(context, "size")
                                          )
                                       )
                                 )
                           )
                     )
               )
               .then(Commands.literal("armies").executes(context -> showArmies(context.getSource())))
               .then(Commands.literal("bandits").executes(context -> showBandits(context.getSource())))
               .then(Commands.literal("rumours").executes(context -> showRumours(context.getSource())))
               .then(Commands.literal("thief").requires(source -> source.hasPermission(2)).executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionTownLife.forceTheft(context.getSource().getLevel(), context.getSource().getPlayerOrException());
                  return 1;
               }))
               .then(Commands.literal("season").executes(context -> {
                  net.minecraft.server.level.ServerLevel level = context.getSource().getLevel();
                  net.shadowmage.ancientwarfare.core.faction.FactionSeasons.Season season = net.shadowmage.ancientwarfare.core.faction.FactionSeasons.season(level);
                  context.getSource()
                     .sendSuccess(
                        () -> Component.translatable(
                           "season.ancientwarfare.now",
                           Component.translatable(season.key()),
                           net.shadowmage.ancientwarfare.core.faction.FactionSeasons.daysLeft(level),
                           Component.translatable(season.key() + ".effect")
                        ),
                        false
                     );
                  return 1;
               }))
               .then(Commands.literal("grow").requires(source -> source.hasPermission(2)).executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionGrowth.force(context.getSource().getLevel(), context.getSource().getPlayerOrException());
                  return 1;
               }))
               .then(Commands.literal("prebuild").requires(source -> source.hasPermission(2)).executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionWorldData.get(context.getSource().getLevel()).setPrebuilt(false);
                  return 1;
               }))
               .then(Commands.literal("rankup").requires(source -> source.hasPermission(2)).executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionPorts.force(context.getSource().getLevel(), context.getSource().getPlayerOrException(), false);
                  return 1;
               }))
               .then(Commands.literal("spyarrive").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
                  ServerPlayer player = context.getSource().getPlayerOrException();
                  boolean arrived = net.shadowmage.ancientwarfare.core.faction.FactionCounterSpies.arrive(
                     context.getSource().getLevel(), net.shadowmage.ancientwarfare.core.faction.FactionWorldData.get(context.getSource().getLevel()), player.getUUID(), StringArgumentType.getString(context, "faction")
                  );
                  context.getSource().sendSuccess(() -> Component.literal(arrived ? "Spy arrived" : "No village"), false);
                  return 1;
               })))
               .then(Commands.literal("taken").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
                  // Marks the player's nearest village as taken from the faction.
                  ServerPlayer player = context.getSource().getPlayerOrException();
                  net.shadowmage.ancientwarfare.core.faction.FactionWorldData data = net.shadowmage.ancientwarfare.core.faction.FactionWorldData.get(context.getSource().getLevel());
                  net.minecraft.core.BlockPos nearest = data.villages().entrySet().stream().filter(entry -> entry.getValue().equals(player.getUUID())).map(java.util.Map.Entry::getKey)
                     .min(java.util.Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition()))).orElse(null);
                  if (nearest != null) {
                     data.setVillageState(nearest, new net.shadowmage.ancientwarfare.core.faction.FactionWorldData.VillageState(StringArgumentType.getString(context, "faction"), 20, 40, 45));
                  }

                  context.getSource().sendSuccess(() -> Component.literal(nearest == null ? "No village" : "Taken: " + nearest.toShortString()), false);
                  return 1;
               })))
               .then(Commands.literal("caravanrobbed").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
                  ServerPlayer player = context.getSource().getPlayerOrException();
                  boolean opened = net.shadowmage.ancientwarfare.core.faction.FactionIncidents.onCaravanRobbed(context.getSource().getLevel(), StringArgumentType.getString(context, "faction"), player.getX(), player.getZ());
                  context.getSource().sendSuccess(() -> Component.literal(opened ? "Incident opened" : "No incident"), false);
                  return 1;
               })))
               .then(Commands.literal("pastime").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("what", StringArgumentType.word()).executes(context -> {
                  String result = net.shadowmage.ancientwarfare.core.faction.FactionPastimes.debug(
                     context.getSource().getLevel(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what")
                  );
                  context.getSource().sendSuccess(() -> Component.literal(result), false);
                  return 1;
               }))))
               .then(Commands.literal("rebel").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("what", StringArgumentType.word()).executes(context -> {
                  String result = net.shadowmage.ancientwarfare.core.faction.FactionRebellions.debug(
                     context.getSource().getLevel(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what")
                  );
                  context.getSource().sendSuccess(() -> Component.literal(result), false);
                  return 1;
               }))))
               .then(Commands.literal("faith").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("what", StringArgumentType.greedyString()).executes(context -> {
                  String result = net.shadowmage.ancientwarfare.core.faction.FactionFaith.debug(
                     context.getSource().getLevel(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what")
                  );
                  context.getSource().sendSuccess(() -> Component.literal(result), false);
                  return 1;
               }))))
               .then(Commands.literal("muster").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("what", StringArgumentType.word()).executes(context -> {
                  String result = net.shadowmage.ancientwarfare.core.faction.FactionMusters.debug(
                     context.getSource().getLevel(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what")
                  );
                  context.getSource().sendSuccess(() -> Component.literal(result), false);
                  return 1;
               }))))
               .then(Commands.literal("plot").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
                  boolean offered = net.shadowmage.ancientwarfare.core.faction.FactionPlots.forcePlot(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"));
                  context.getSource().sendSuccess(() -> Component.literal(offered ? "Plot offered" : "No courtier"), false);
                  return 1;
               })))
               .then(Commands.literal("family").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("what", StringArgumentType.word())
                  .executes(context -> family(context.getSource(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what"), 0))
                  .then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 200))
                     .executes(context -> family(context.getSource(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "value")))))))
               .then(Commands.literal("feast").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("what", StringArgumentType.word())
                  .executes(context -> feast(context.getSource(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what"), ""))
                  .then(Commands.argument("other", StringArgumentType.word())
                     .executes(context -> feast(context.getSource(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what"), StringArgumentType.getString(context, "other")))))))
               .then(Commands.literal("rulerlife").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("what", StringArgumentType.word())
                  .executes(context -> rulerLife(context.getSource(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what"), 0))
                  .then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 200))
                     .executes(context -> rulerLife(context.getSource(), StringArgumentType.getString(context, "faction"), StringArgumentType.getString(context, "what"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "value")))))))
               .then(Commands.literal("secret").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionPlots.forceSecret(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "faction"));
                  return 1;
               })))
               .then(Commands.literal("council").requires(source -> source.hasPermission(2))
                  .executes(context -> callCouncil(context.getSource(), ""))
                  .then(Commands.literal("call").then(Commands.argument("matter", StringArgumentType.word()).executes(context -> callCouncil(context.getSource(), StringArgumentType.getString(context, "matter")))))
                  .then(Commands.literal("resolve").executes(context -> {
                     net.shadowmage.ancientwarfare.core.faction.FactionCouncil.forceResolve(context.getSource().getLevel());
                     return 1;
                  })))
               .then(Commands.literal("discontent").requires(source -> source.hasPermission(2)).then(Commands.argument("faction", StringArgumentType.word()).then(Commands.argument("value", IntegerArgumentType.integer(0, 100)).executes(context -> {
                  boolean set = net.shadowmage.ancientwarfare.core.faction.FactionVassals.setDiscontent(
                     context.getSource().getLevel(), context.getSource().getPlayerOrException().getUUID(), StringArgumentType.getString(context, "faction"), IntegerArgumentType.getInteger(context, "value")
                  );
                  context.getSource().sendSuccess(() -> Component.literal(set ? "Discontent set" : "Not a vassal"), false);
                  return 1;
               }))))
               .then(Commands.literal("conquests").requires(source -> source.hasPermission(2)).then(Commands.argument("count", IntegerArgumentType.integer(1, 20)).executes(context -> {
                  ServerPlayer player = context.getSource().getPlayerOrException();
                  net.shadowmage.ancientwarfare.core.faction.FactionCoalitions.forceConquests(context.getSource().getLevel(), player.getUUID(), IntegerArgumentType.getInteger(context, "count"));
                  int threat = net.shadowmage.ancientwarfare.core.faction.FactionCoalitions.threat(
                     context.getSource().getLevel(), net.shadowmage.ancientwarfare.core.faction.FactionWorldData.get(context.getSource().getLevel()), player.getUUID()
                  );
                  context.getSource().sendSuccess(() -> Component.literal("Threat " + threat), false);
                  return 1;
               })))
               .then(Commands.literal("port").requires(source -> source.hasPermission(2)).executes(context -> {
                  net.shadowmage.ancientwarfare.core.faction.FactionPorts.force(context.getSource().getLevel(), context.getSource().getPlayerOrException(), true);
                  return 1;
               }))
               .then(Commands.literal("festival").requires(source -> source.hasPermission(2)).executes(context -> startFestival(context.getSource())))
               .then(
                  Commands.literal("routes")
                     .executes(context -> showRoutes(context.getSource()))
                     .then(Commands.literal("retry").requires(source -> source.hasPermission(2)).executes(context -> retryRoads(context.getSource())))
                     .then(
                        Commands.literal("caravans")
                           .requires(source -> source.hasPermission(2))
                           .executes(context -> hurryCaravans(context.getSource(), false))
                           .then(Commands.literal("ambush").executes(context -> hurryCaravans(context.getSource(), true)))
                     )
               )
               .then(
                  Commands.literal("fortify")
                     .requires(source -> source.hasPermission(2))
                     .then(factionArgument("faction").executes(context -> fortify(context.getSource(), faction(context, "faction"))))
               )
               .then(
                  Commands.literal("simulate")
                     .requires(source -> source.hasPermission(2))
                     .then(Commands.argument("days", IntegerArgumentType.integer(1, 365)).executes(context -> simulate(context.getSource(), IntegerArgumentType.getInteger(context, "days"))))
               )
               .then(
                  Commands.literal("famine")
                     .requires(source -> source.hasPermission(2))
                     .executes(context -> lifeEvent(context.getSource(), false))
               )
               .then(
                  Commands.literal("fleet")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(
                              Commands.argument("settlement", IntegerArgumentType.integer(1))
                                 .then(
                                    Commands.argument("size", IntegerArgumentType.integer(1, 100))
                                       .executes(
                                          context -> fleet(
                                             context.getSource(),
                                             faction(context, "faction"),
                                             IntegerArgumentType.getInteger(context, "settlement"),
                                             IntegerArgumentType.getInteger(context, "size")
                                          )
                                       )
                                 )
                           )
                     )
               )
               .then(
                  Commands.literal("plague")
                     .requires(source -> source.hasPermission(2))
                     .executes(context -> lifeEvent(context.getSource(), true))
               )
               .then(Commands.literal("relations").then(factionArgument("faction").executes(context -> showRelations(context.getSource(), faction(context, "faction")))))
               .then(stateCommand("war", RelationState.WAR))
               .then(stateCommand("peace", RelationState.NEUTRAL))
               .then(stateCommand("pact", RelationState.PACT))
               .then(stateCommand("alliance", RelationState.ALLIANCE))
               .then(
                  Commands.literal("treaty")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(
                              Commands.argument("state", StringArgumentType.word())
                                 .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("war", "neutral", "pact", "alliance"), builder))
                                 .executes(context -> setTreaty(context.getSource(), faction(context, "faction"), StringArgumentType.getString(context, "state")))
                           )
                     )
               )
               .then(
                  Commands.literal("traveller")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        Commands.argument("kind", StringArgumentType.word())
                           .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("pilgrims", "peddler", "messenger", "refugees", "patrol", "shepherd", "hunter", "knight", "tax_collector", "procession"), builder))
                           .executes(context -> {
                              boolean sent = net.shadowmage.ancientwarfare.core.faction.FactionTravellers.spawnNear(
                                 context.getSource().getLevel(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "kind")
                              );
                              if (!sent) {
                                 context.getSource().sendFailure(Component.literal("No finished road 40-90 blocks from you, or no such kind"));
                              }

                              return sent ? 1 : 0;
                           })
                     )
               )
               .then(
                  Commands.literal("freevillage")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        Commands.literal("annex")
                           .then(
                              factionArgument("faction")
                                 .executes(context -> freeVillage(context.getSource(), faction(context, "faction"), false))
                           )
                     )
                     .then(
                        Commands.literal("raid")
                           .then(
                              factionArgument("faction")
                                 .executes(context -> freeVillage(context.getSource(), faction(context, "faction"), true))
                           )
                     )
               )
               .then(
                  Commands.literal("opinion")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(
                              factionArgument("other")
                                 .then(
                                    Commands.argument("change", IntegerArgumentType.integer(-200, 200))
                                       .executes(
                                          context -> changeOpinion(
                                                context.getSource(), faction(context, "faction"), faction(context, "other"), IntegerArgumentType.getInteger(context, "change")
                                             )
                                       )
                                 )
                           )
                     )
               )
         );
   }

   /** Sets the player's treaty with a faction; "war" is the faction falling on the player, with all that follows. */
   private static int setTreaty(CommandSourceStack source, String faction, String state) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      RelationState treaty = switch (state.toLowerCase(Locale.ROOT)) {
         case "war" -> RelationState.WAR;
         case "pact" -> RelationState.PACT;
         case "alliance" -> RelationState.ALLIANCE;
         default -> RelationState.NEUTRAL;
      };
      if (!FactionRegistry.isKnown(faction)) {
         source.sendFailure(Component.literal("Unknown faction " + faction));
         return 0;
      }

      FactionStandingData standings = FactionStandingData.get(source.getLevel());
      standings.discover(player.getUUID(), faction);
      standings.setTreaty(player.getUUID(), faction, treaty);
      if (treaty == RelationState.WAR) {
         net.shadowmage.ancientwarfare.core.faction.CountryDiplomacy.warDeclaredOn(source.getLevel(), player.getUUID(), faction);
      }

      tell(source, Component.literal("Treaty of " + player.getScoreboardName() + " with " + faction + " is now " + treaty.serializedName()));
      return 1;
   }

   private static RequiredArgumentBuilder<CommandSourceStack, String> factionArgument(String name) {
      return Commands.argument(name, StringArgumentType.word())
         .suggests((context, builder) -> SharedSuggestionProvider.suggest(FactionRegistry.factionNames(), builder));
   }

   private static String faction(CommandContext<CommandSourceStack> context, String name) {
      return StringArgumentType.getString(context, name).toLowerCase(Locale.ROOT);
   }

   private static LiteralArgumentBuilder<CommandSourceStack> stateCommand(String name, RelationState state) {
      return Commands.literal(name)
         .requires(source -> source.hasPermission(2))
         .then(
            factionArgument("faction")
               .then(factionArgument("other").executes(context -> setState(context.getSource(), faction(context, "faction"), faction(context, "other"), state)))
         );
   }

   private static int showRelations(CommandSourceStack source, String faction) {
      if (!FactionRegistry.isKnown(faction)) {
         source.sendFailure(Component.literal("Unknown faction " + faction));
         return 0;
      }

      tell(source, Component.literal("Relations of " + faction + ":"));

      for (String other : new TreeSet<>(FactionRegistry.factionNames())) {
         if (!other.equals(faction)) {
            FactionDiplomacy.Relation relation = FactionDiplomacy.relation(source.getLevel(), faction, other);
            tell(source, Component.literal("  " + other + ": " + relation.state().serializedName() + ", opinion " + relation.opinion()));
         }
      }

      return 1;
   }

   private static int setState(CommandSourceStack source, String faction, String other, RelationState state) {
      if (!FactionDiplomacy.setState(source.getLevel(), faction, other, state)) {
         source.sendFailure(Component.literal("Relation of " + faction + " and " + other + " is already " + state.serializedName() + " or cannot change (outlaws are always at war)"));
         return 0;
      }

      tell(source, Component.literal(faction + " and " + other + " are now at " + state.serializedName()));
      return 1;
   }

   private static int changeOpinion(CommandSourceStack source, String faction, String other, int change) {
      FactionDiplomacy.Relation relation = FactionDiplomacy.adjustOpinion(source.getLevel(), faction, other, change);
      tell(source, Component.literal("Opinion of " + faction + " and " + other + " is now " + relation.opinion()));
      return relation.opinion();
   }

   private static int showRegion(CommandSourceStack source) {
      ServerLevel level = source.getLevel();
      FactionRegions.Province province = FactionRegions.provinceAt(level, (int)Math.floor(source.getPosition().x), (int)Math.floor(source.getPosition().z));
      FactionRegions.Realm realm = FactionRegions.realmAt(level, (int)Math.floor(source.getPosition().x), (int)Math.floor(source.getPosition().z));
      tell(source, 
         Component.literal(
            "Province " + province.cellX() + "," + province.cellZ() + " owner: " + ownerName(province.owner()) + ". Realm of " + ownerName(realm.faction())
               + ", capital area at " + realm.seedX() + " " + realm.seedZ()
         )
      );
      return 1;
   }

   private static int showMap(CommandSourceStack source) {
      ServerLevel level = source.getLevel();
      Map<String, Character> letters = new TreeMap<>();
      List<String> rows = new ArrayList<>();

      for (int row = -MAP_HALF_HEIGHT; row <= MAP_HALF_HEIGHT; row++) {
         StringBuilder line = new StringBuilder();

         for (int column = -MAP_HALF_WIDTH; column <= MAP_HALF_WIDTH; column++) {
            if (row == 0 && column == 0) {
               line.append('@');
            } else {
               String owner = FactionRegions.ownerAt(level, (int)Math.floor(source.getPosition().x) + column * MAP_STEP, (int)Math.floor(source.getPosition().z) + row * MAP_STEP);
               line.append(owner.isEmpty() ? '.' : letters.computeIfAbsent(owner, faction -> mapLetter(faction, letters.values())));
            }
         }

         rows.add(line.toString());
      }

      tell(source, Component.literal("Faction regions, north up, one character per " + MAP_STEP + " blocks:"));
      rows.forEach(line -> tell(source, Component.literal(line).withStyle(ChatFormatting.WHITE)));
      StringBuilder legend = new StringBuilder(". wild");
      letters.forEach((faction, letter) -> legend.append(", ").append(letter).append(' ').append(faction));
      tell(source, Component.literal(legend.toString()));
      return letters.size();
   }

   private static char mapLetter(String faction, Collection<Character> used) {
      for (char letter : faction.toUpperCase(Locale.ROOT).toCharArray()) {
         if (Character.isLetter(letter) && !used.contains(letter)) {
            return letter;
         }
      }

      for (char letter = '0'; letter <= '9'; letter++) {
         if (!used.contains(letter)) {
            return letter;
         }
      }

      return '?';
   }

   /** Brings a bad harvest or plague on the settlement nearest the source, for testing. */
   private static int fleet(CommandSourceStack source, String faction, int settlementId, int size) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      FactionWorldData.Settlement target = data.settlements().stream().filter(settlement -> settlement.id() == settlementId).findFirst().orElse(null);
      if (target == null) {
         source.sendFailure(Component.literal("No settlement #" + settlementId));
         return 0;
      }

      FactionWorldData.Campaign campaign = net.shadowmage.ancientwarfare.core.faction.FactionFleets.sendFrom(
         source.getLevel(), data, faction, target, size, BlockPos.containing(source.getPosition())
      );
      if (campaign == null) {
         source.sendFailure(Component.literal(faction + " has no settlement to sail from"));
         return 0;
      }

      tell(source, Component.literal("Fleet #" + campaign.id() + " lands at " + Math.round(campaign.landX()) + " " + Math.round(campaign.landZ())));
      return campaign.id();
   }

   private static int lifeEvent(CommandSourceStack source, boolean plague) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      FactionWorldData.Settlement nearest = data.settlements()
         .stream()
         .min(Comparator.comparingDouble(settlement -> settlement.horizontalDistanceSquared(source.getPosition().x, source.getPosition().z)))
         .orElse(null);
      if (nearest == null) {
         source.sendFailure(Component.literal("No settlements"));
         return 0;
      }

      if (plague) {
         net.shadowmage.ancientwarfare.core.faction.FactionLife.startPlague(source.getLevel(), nearest, source.getLevel().getRandom(), true);
      } else {
         net.shadowmage.ancientwarfare.core.faction.FactionLife.badHarvest(source.getLevel(), new ArrayList<>(data.settlements()), nearest, source.getLevel().getRandom());
      }

      data.setDirty();
      return nearest.id();
   }

   private static int showSettlements(CommandSourceStack source, int limit) {
      List<FactionWorldData.Settlement> settlements = new ArrayList<>(FactionWorldData.get(source.getLevel()).settlements());
      Map<String, Integer> counts = new TreeMap<>();
      settlements.forEach(settlement -> counts.merge(settlement.faction(), 1, Integer::sum));
      tell(source, Component.literal("Settlements: " + settlements.size() + " " + counts));
      settlements.sort(Comparator.comparingDouble(settlement -> settlement.horizontalDistanceSquared(source.getPosition().x, source.getPosition().z)));

      for (FactionWorldData.Settlement settlement : settlements.subList(0, Math.min(limit, settlements.size()))) {
         BlockPos center = settlement.center();
         String template = settlement.template().substring(settlement.template().lastIndexOf('/') + 1);
         tell(source, 
            Component.literal("#" + settlement.id() + " ").append(settlement.nameComponent()).append(
               " " + settlement.faction() + " " + settlement.kind() + " " + template + " tier " + settlement.tier() + ", people "
                  + settlement.population() + ", garrison " + settlement.garrison() + " at " + center.getX() + " " + center.getY() + " " + center.getZ() + " ("
                  + (int)Math.sqrt(settlement.horizontalDistanceSquared(source.getPosition().x, source.getPosition().z)) + " blocks)"
                  + " food " + settlement.food() + " health " + settlement.health() + " order " + settlement.order() + " loyalty " + settlement.loyalty()
                  + (settlement.famine() > 0 ? " FAMINE" : "") + (settlement.plague() > 0 ? " PLAGUE" : "")
            )
         );
      }

      return settlements.size();
   }

   /** Builds a tavern of the faction near the command's position, for testing mercenaries. */
   private static int tavern(CommandSourceStack source, String faction) {
      BlockPos pos = BlockPos.containing(source.getPosition());
      boolean built = net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator.generateSettlementAt(
         source.getLevel(), new net.minecraft.world.level.ChunkPos(pos), faction, net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator.Site.TAVERN
      );
      tell(source, Component.literal(built ? "Built a tavern of " + faction : "Could not build a tavern here"));
      return built ? 1 : 0;
   }

   /** Gives the player the faction's banner, to see its arms. */
   private static int banner(CommandSourceStack source, String faction) {
      if (source.getEntity() instanceof ServerPlayer player) {
         player.getInventory().add(net.shadowmage.ancientwarfare.core.faction.FactionHeraldry.banner(faction));
         return 1;
      }

      return 0;
   }

   private static int answerUltimatum(CommandContext<CommandSourceStack> context, boolean pay) {
      if (context.getSource().getEntity() instanceof ServerPlayer player) {
         FactionArmies.answerUltimatum(player, IntegerArgumentType.getInteger(context, "army"), pay);
         return 1;
      }

      return 0;
   }

   private static int answerEnvoy(CommandContext<CommandSourceStack> context, boolean accepted) {
      if (context.getSource().getEntity() instanceof ServerPlayer player) {
         FactionEnvoys.answer(player, IntegerArgumentType.getInteger(context, "offer"), accepted);
         return 1;
      }

      return 0;
   }

   /** Sends an envoy to the player running the command, of the kind named or any that fits. */
   private static int envoy(CommandSourceStack source, String kind) {
      if (!(source.getEntity() instanceof ServerPlayer player)) {
         return 0;
      }

      FactionEnvoys.Kind wanted = null;
      for (FactionEnvoys.Kind candidate : FactionEnvoys.Kind.values()) {
         if (candidate.name().equalsIgnoreCase(kind)) {
            wanted = candidate;
         }
      }

      boolean sent = FactionEnvoys.send(source.getLevel(), player, wanted);
      tell(source, Component.literal(sent ? "An envoy is on the way" : "No country has such an offer for you now"));
      return sent ? 1 : 0;
   }

   private static int hire(CommandSourceStack source, int captain, String offer) {
      if (!(source.getEntity() instanceof ServerPlayer player)) {
         return 0;
      }

      for (FactionMercenaries.Offer candidate : FactionMercenaries.Offer.values()) {
         if (candidate.name().equalsIgnoreCase(offer)) {
            FactionMercenaries.hire(player, captain, candidate);
            return 1;
         }
      }

      return 0;
   }

   private static int serve(CommandSourceStack source, int barman, String offer) {
      if (!(source.getEntity() instanceof ServerPlayer player)) {
         return 0;
      }

      for (net.shadowmage.ancientwarfare.core.faction.FactionTaverns.Offer candidate : net.shadowmage.ancientwarfare.core.faction.FactionTaverns.Offer.values()) {
         if (candidate.name().equalsIgnoreCase(offer)) {
            net.shadowmage.ancientwarfare.core.faction.FactionTaverns.serve(player, barman, candidate);
            return 1;
         }
      }

      return 0;
   }

   private static void tell(CommandSourceStack source, Component message) {
      source.sendSuccess(() -> message, false);
   }

   private static int showFactions(CommandSourceStack source) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      Map<String, int[]> totals = new TreeMap<>();

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         int[] total = totals.computeIfAbsent(settlement.faction(), ignored -> new int[3]);
         total[0]++;
         total[1] += settlement.population();
         total[2] += settlement.garrison();
      }

      tell(
         source,
         Component.literal(
            "Day " + data.lastSimulatedDay() + ", " + data.plannedSettlements().size() + " village(s) planned. Faction: settlements, provinces, people, garrison, treasury, wood/stone/iron/grain/leather/wool/coal/copper"
         )
      );
      totals.forEach(
         (faction, total) -> tell(
               source,
               Component.literal(
                  "  " + faction + ": " + total[0] + ", " + data.provincesOf(faction).size() + ", " + total[1] + ", " + total[2] + ", " + data.treasury(faction) + " gold, " + java.util.Arrays.stream(data.stores(faction)).mapToObj(Integer::toString).collect(java.util.stream.Collectors.joining("/"))
               )
            )
      );
      return totals.size();
   }

   /** Registers a 33x33 settlement around the command position and claims its province; for testing. */
   private static int found(CommandSourceStack source, String faction, int garrison) {
      if (!FactionRegistry.isKnown(faction)) {
         source.sendFailure(Component.literal("Unknown faction " + faction));
         return 0;
      }

      BlockPos center = BlockPos.containing(source.getPosition());
      FactionWorldData.Settlement settlement = FactionWorldData.get(source.getLevel())
         .addSettlement(
            faction,
            "structure",
            "command",
            center.offset(-16, -4, -16),
            center.offset(16, 8, 16),
            FactionRegions.provinceAt(source.getLevel(), center.getX(), center.getZ()).key(),
            source.getLevel().getGameTime(),
            garrison,
            FactionSimulation.startingPopulation(garrison, 2)
         );
      FactionWorldData.get(source.getLevel()).setProvinceOwner(settlement.province(), faction);
      tell(source, Component.literal("Founded settlement #" + settlement.id() + " of " + faction + " with garrison " + garrison));
      return settlement.id();
   }

   /** Lays out a faction's capital or village town here, on whatever land is here, for testing. */
   private static int buildTown(CommandSourceStack source, String faction, boolean capital) {
      if (!FactionTowns.canBuild(faction, capital)) {
         source.sendFailure(Component.literal(faction + " has no buildings for a " + (capital ? "capital" : "village")));
         return 0;
      }

      BlockPos center = BlockPos.containing(source.getPosition());
      String path = FactionTowns.path(faction, capital);
      int[] site = FactionExpansion.findTownSite(source.getLevel(), center.getX(), center.getZ(), AwtWorldGenerator.townChunks(path));
      if (site == null) {
         source.sendFailure(Component.literal("No dry, flat land for it nearby"));
         return 0;
      } else if (!AwtWorldGenerator.queueFactionTown(source.getLevel(), path, site[0], site[1])) {
         source.sendFailure(Component.literal("Another town is being built; try again once it is done"));
         return 0;
      }

      tell(source, Component.literal("Building a " + faction + " " + (capital ? "capital" : "village") + " at " + site[0] + ", " + site[1] + "; it appears over the next minutes"));
      return 1;
   }

   /** Sends a raid against the player village nearest to the command's position, setting out almost at once. */
   private static int raid(CommandSourceStack source, String faction, int size, boolean siege) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      BlockPos origin = BlockPos.containing(source.getPosition());
      BlockPos village = null;

      for (BlockPos pos : data.villages().keySet()) {
         if (village == null || pos.distSqr(origin) < village.distSqr(origin)) {
            village = pos;
         }
      }

      if (village == null) {
         source.sendFailure(Component.literal("No player village (town hall) is known"));
         return 0;
      }

      UUID owner = data.villages().get(village);
      FactionStandingData.get(source.getLevel()).setTreaty(owner, faction, RelationState.WAR);
      FactionWorldData.Army army = siege
         ? FactionArmies.siege(source.getLevel(), data, faction, village, owner, size)
         : FactionArmies.raid(source.getLevel(), data, faction, village, owner, size, 100L);
      if (army == null) {
         source.sendFailure(Component.literal(faction + " has no soldiers to send"));
         return 0;
      }

      tell(source, Component.literal("Army #" + army.id() + " of " + faction + " (" + army.size() + ") " + (siege ? "besieges" : "raids") + " the village at " + village.toShortString()));
      return army.id();
   }

   /** Declares war on the settlement's owner if need be and sends an army marching on it from the faction's nearest settlement. */
   private static int march(CommandSourceStack source, String faction, int settlementId, int size) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.id() == settlementId) {
            if (settlement.faction().equals(faction)) {
               source.sendFailure(Component.literal("Settlement #" + settlementId + " already belongs to " + faction));
               return 0;
            }

            FactionDiplomacy.setState(source.getLevel(), faction, settlement.faction(), RelationState.WAR);
            if (!FactionCampaigns.launch(source.getLevel(), data, faction, settlement, size)) {
               source.sendFailure(Component.literal(faction + " has no soldiers to send"));
               return 0;
            }

            tell(source, Component.literal(faction + " marches on " + settlement.faction() + " settlement #" + settlementId + "; see /awfaction armies"));
            return 1;
         }
      }

      source.sendFailure(Component.literal("No settlement #" + settlementId));
      return 0;
   }

   /** The nearest free village to the caller is taken in by a faction, or raided by it. */
   private static int freeVillage(CommandSourceStack source, String faction, boolean raid) {
      net.minecraft.world.phys.Vec3 pos = source.getPosition();
      boolean done = raid
         ? net.shadowmage.ancientwarfare.core.faction.FreeVillages.raidNearest(source.getLevel(), faction, pos.x, pos.z)
         : net.shadowmage.ancientwarfare.core.faction.FreeVillages.annexNearest(source.getLevel(), faction, pos.x, pos.z);
      if (!done) {
         source.sendFailure(Component.literal("No free village known near here"));
         return 0;
      }

      tell(source, Component.literal(raid ? faction + " will raid the nearest free village" : faction + " took in the nearest free village"));
      return 1;
   }

   private static int assault(CommandSourceStack source, String faction, int settlementId, int size) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.id() == settlementId) {
            FactionWorldData.Army army = FactionArmies.assault(source.getLevel(), data, faction, settlement, size);
            if (army == null) {
               source.sendFailure(Component.literal("No army sent: " + faction + " has no soldiers, or the settlement is already under attack"));
               return 0;
            }

            tell(source, Component.literal("Army #" + army.id() + " of " + faction + " (" + army.size() + ") marches on settlement #" + settlementId));
            return army.id();
         }
      }

      source.sendFailure(Component.literal("No settlement #" + settlementId));
      return 0;
   }

   /** Plans a border fort for the faction at once, free; it is built once no player is near the site. */
   private static int fortify(CommandSourceStack source, String faction) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      FactionWorldData.PlannedSettlement plan = FactionExpansion.planFort(source.getLevel(), data, faction, data.lastSimulatedDay(), true);
      if (plan == null) {
         source.sendFailure(Component.literal(faction + " has no border with a rival that lacks a fort"));
         return 0;
      }

      tell(source, Component.literal(faction + " plans a border fort at " + plan.x() + ", " + plan.z()));
      return 1;
   }

   private static int hurryCaravans(CommandSourceStack source, boolean ambush) {
      FactionCaravans.hurry(ambush);
      tell(
         source,
         Component.literal(
            FactionCaravans.active() + " caravan(s) on the road; the next ones set out near players now" + (ambush ? ", and outlaws wait for the first one" : "")
         )
      );
      return 1;
   }

   private static int retryRoads(CommandSourceStack source) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      data.routes().forEach(FactionWorldData.Route::resetRoad);
      data.setDirty();
      tell(source, Component.literal("Failed roads will be laid again"));
      return 1;
   }

   private static int showRoutes(CommandSourceStack source) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      FactionTrade.updateRoutes(source.getLevel(), data);
      List<FactionWorldData.Route> routes = data.routes();
      long done = routes.stream().filter(route -> route.road() == FactionWorldData.RoadState.DONE).count();
      long building = routes.stream().filter(route -> route.road() == FactionWorldData.RoadState.BUILDING).count();
      long failed = routes.stream().filter(route -> route.road() == FactionWorldData.RoadState.FAILED).count();
      tell(source, Component.literal(routes.size() + " route(s), " + done + " road(s) finished, " + building + " being built, " + failed + " failed"));

      // Routes to player villages first: those are the ones a player looks for; then roads under way.
      List<FactionWorldData.Route> shown = new ArrayList<>(routes);
      shown.sort(Comparator.<FactionWorldData.Route, Boolean>comparing(route -> route.village() == null)
         .thenComparing(route -> route.road() != FactionWorldData.RoadState.BUILDING));

      for (FactionWorldData.Route route : shown.subList(0, Math.min(24, shown.size()))) {
         FactionWorldData.Settlement a = data.endpoint(route, true);
         FactionWorldData.Settlement b = data.endpoint(route, false);
         tell(
            source,
            Component.literal(
               "  #" + route.id() + " " + (a == null ? "?" : a.faction() + " #" + a.id()) + " - " + (b == null ? "?" : route.village() != null ? "player village at " + route.village().getX() + " " + route.village().getZ() : b.faction() + " #" + b.id()) + ", "
                  + FactionTrade.length(data, route) + " blocks, road " + route.road().name().toLowerCase(Locale.ROOT)
                  + (route.points().isEmpty() ? "" : " via " + FactionRoads.unpackX(route.points().get(route.points().size() / 2)) + " "
                     + FactionRoads.unpackZ(route.points().get(route.points().size() / 2)))
                  + (route.blocked() ? ", blocked by war" : ", " + route.income() + " gold/day")
            )
         );
      }

      return routes.size();
   }

   /** Bandit lairs with their level: 1 waylays caravans, 2 raids trading posts, 3 guard posts too, 4 villages too. */
   /** The nearest settlement holds a festival from now. */
   private static int startFestival(CommandSourceStack source) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      FactionWorldData.Settlement nearest = null;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (nearest == null
            || settlement.horizontalDistanceSquared(source.getPosition().x, source.getPosition().z)
               < nearest.horizontalDistanceSquared(source.getPosition().x, source.getPosition().z)) {
            nearest = settlement;
         }
      }

      if (nearest == null) {
         source.sendFailure(Component.literal("No settlements"));
         return 0;
      }

      net.shadowmage.ancientwarfare.core.faction.FactionTownLife.startFestival(source.getLevel(), nearest);
      tell(source, Component.literal("Settlement #" + nearest.id() + " holds a festival"));
      return 1;
   }

   /** A handful of what people around here would say, for checking the rumours. */
   private static int showRumours(CommandSourceStack source) {
      for (int i = 0; i < 6; i++) {
         tell(
            source,
            net.shadowmage.ancientwarfare.core.faction.FactionRumours.rumour(
               source.getLevel(), source.getPlayer(), source.getPosition().x, source.getPosition().z, source.getLevel().getRandom()
            ).copy()
         );
      }

      return 6;
   }

   private static int showBandits(CommandSourceStack source) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      int count = 0;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (FactionDiplomacy.isOutlaw(settlement.faction())) {
            count++;
            tell(
               source,
               Component.literal(
                  "  #" + settlement.id() + " " + settlement.faction() + " level " + FactionBandits.level(settlement) + ", tier " + settlement.tier() + ", bandits "
                     + settlement.garrison() + ", people " + settlement.population() + " at " + settlement.center().toShortString()
               )
            );
         }
      }

      tell(source, Component.literal(count + " bandit lair(s); level 1 waylays caravans, 2 raids trading posts, 3 guard posts too, 4 villages too"));
      return count;
   }

   private static int showArmies(CommandSourceStack source) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());
      tell(source, Component.literal(data.armies().size() + " army(ies), " + data.villages().size() + " player village(s)"));

      for (FactionWorldData.Army army : data.armies()) {
         tell(
            source,
            Component.literal(
               "  #" + army.id() + " " + army.faction() + " -> " + (army.isRaid() ? "village" : army.defender() + " #" + army.targetSettlement()) + " at "
                  + army.target().toShortString() + ", " + (army.spawned() ? "fighting, " + army.alive() + " alive" : "gathering " + army.size())
            )
         );
      }

      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         tell(
            source,
            Component.literal(
               (campaign.raid() ? "  raid #" : "  march #") + campaign.id() + " " + campaign.faction() + " -> " + campaign.defender() + " #" + campaign.targetSettlement() + ", " + campaign.size()
                  + " soldiers at " + (int)campaign.x() + ", " + (int)campaign.z()
            )
         );
      }

      return data.armies().size() + data.campaigns().size();
   }

   private static int simulate(CommandSourceStack source, int days) {
      FactionWorldData data = FactionWorldData.get(source.getLevel());

      for (int i = 0; i < days; i++) {
         // Runs ahead of the clock; the daily tick counts these days as already past.
         data.setLastSimulatedDay(data.lastSimulatedDay() + 1);
         FactionSimulation.simulateDay(source.getLevel(), data);
      }

      tell(source, Component.literal("Simulated " + days + " faction day(s)"));
      return showFactions(source);
   }

   private static String ownerName(String owner) {
      return owner.isEmpty() ? "wild" : owner;
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

   private static int setTreaty(ServerPlayer player, String faction, String state) {
      RelationState relation;
      try {
         relation = RelationState.valueOf(state.toUpperCase(java.util.Locale.ROOT));
      } catch (IllegalArgumentException e) {
         player.sendSystemMessage(Component.literal("Unknown treaty " + state));
         return 0;
      }
      FactionStandingData.get(player.serverLevel()).setTreaty(player.getUUID(), faction, relation);
      player.sendSystemMessage(Component.literal("Faction " + faction + " treaty set to " + relation));
      return 1;
   }

   private static int addStanding(ServerPlayer player, String faction, int value) {
      int standing = FactionStandingData.get(player.serverLevel()).adjustStanding(player.getUUID(), faction, value);
      player.sendSystemMessage(Component.literal("Faction " + faction + " standing is now " + standing));
      return standing;
   }

   private static int callCouncil(net.minecraft.commands.CommandSourceStack source, String matter) {
      boolean called = net.shadowmage.ancientwarfare.core.faction.FactionCouncil.forceConvene(source.getLevel(), matter);
      net.minecraft.core.BlockPos hall = net.shadowmage.ancientwarfare.core.faction.FactionCouncil.hall(source.getLevel());
      source.sendSuccess(() -> Component.literal(called ? "Council at " + hall.toShortString() : "No council"), false);
      return 1;
   }


   private static int family(CommandSourceStack source, String faction, String what, int value) {
      String result = net.shadowmage.ancientwarfare.core.faction.FactionFamilies.debug(source.getLevel(), faction, what, value);
      source.sendSuccess(() -> Component.literal(result), false);
      return 1;
   }

   private static int feast(CommandSourceStack source, String faction, String what, String other) {
      String result = net.shadowmage.ancientwarfare.core.faction.FactionFeasts.debug(source.getLevel(), faction, what, other);
      source.sendSuccess(() -> Component.literal(result), false);
      return 1;
   }

   private static int rulerLife(CommandSourceStack source, String faction, String what, int value) {
      String result = net.shadowmage.ancientwarfare.core.faction.FactionRulers.debugLife(source.getLevel(), faction, what, value);
      source.sendSuccess(() -> Component.literal(result), false);
      return 1;
   }
}
