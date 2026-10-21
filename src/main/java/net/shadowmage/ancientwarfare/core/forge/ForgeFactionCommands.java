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
import net.shadowmage.ancientwarfare.core.faction.FactionDiplomacy;
import net.shadowmage.ancientwarfare.core.faction.FactionCaravans;
import net.shadowmage.ancientwarfare.core.faction.FactionExpansion;
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
         );
   }

   private static void registerWorldCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            Commands.literal("awfaction")
               .then(Commands.literal("region").executes(context -> showRegion(context.getSource())))
               .then(Commands.literal("map").executes(context -> showMap(context.getSource())))
               .then(Commands.literal("settlements").executes(context -> showSettlements(context.getSource())))
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
                  Commands.literal("raid")
                     .requires(source -> source.hasPermission(2))
                     .then(
                        factionArgument("faction")
                           .then(
                              Commands.argument("size", IntegerArgumentType.integer(1, 12))
                                 .executes(context -> raid(context.getSource(), faction(context, "faction"), IntegerArgumentType.getInteger(context, "size")))
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
                                    Commands.argument("size", IntegerArgumentType.integer(1, 12))
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
               .then(Commands.literal("relations").then(factionArgument("faction").executes(context -> showRelations(context.getSource(), faction(context, "faction")))))
               .then(stateCommand("war", RelationState.WAR))
               .then(stateCommand("peace", RelationState.NEUTRAL))
               .then(stateCommand("pact", RelationState.PACT))
               .then(stateCommand("alliance", RelationState.ALLIANCE))
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

   private static int showSettlements(CommandSourceStack source) {
      List<FactionWorldData.Settlement> settlements = new ArrayList<>(FactionWorldData.get(source.getLevel()).settlements());
      Map<String, Integer> counts = new TreeMap<>();
      settlements.forEach(settlement -> counts.merge(settlement.faction(), 1, Integer::sum));
      tell(source, Component.literal("Settlements: " + settlements.size() + " " + counts));
      settlements.sort(Comparator.comparingDouble(settlement -> settlement.horizontalDistanceSquared(source.getPosition().x, source.getPosition().z)));

      for (FactionWorldData.Settlement settlement : settlements.subList(0, Math.min(8, settlements.size()))) {
         BlockPos center = settlement.center();
         String template = settlement.template().substring(settlement.template().lastIndexOf('/') + 1);
         tell(source, 
            Component.literal(
               "#" + settlement.id() + " " + settlement.faction() + " " + settlement.kind() + " " + template + " tier " + settlement.tier() + ", people "
                  + settlement.population() + ", garrison " + settlement.garrison() + " at " + center.getX() + " " + center.getY() + " " + center.getZ() + " ("
                  + (int)Math.sqrt(settlement.horizontalDistanceSquared(source.getPosition().x, source.getPosition().z)) + " blocks)"
            )
         );
      }

      return settlements.size();
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
            "Day " + data.lastSimulatedDay() + ", " + data.plannedSettlements().size() + " village(s) planned. Faction: settlements, provinces, people, garrison, treasury"
         )
      );
      totals.forEach(
         (faction, total) -> tell(
               source,
               Component.literal(
                  "  " + faction + ": " + total[0] + ", " + data.provincesOf(faction).size() + ", " + total[1] + ", " + total[2] + ", " + data.treasury(faction) + " gold"
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
   private static int raid(CommandSourceStack source, String faction, int size) {
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
      FactionWorldData.Army army = FactionArmies.raid(source.getLevel(), data, faction, village, owner, size, 100L);
      if (army == null) {
         source.sendFailure(Component.literal(faction + " has no soldiers to send"));
         return 0;
      }

      tell(source, Component.literal("Army #" + army.id() + " of " + faction + " (" + army.size() + ") raids the village at " + village.toShortString()));
      return army.id();
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
      tell(source, Component.literal(routes.size() + " route(s), " + done + " road(s) finished"));

      // Routes to player villages first: those are the ones a player looks for.
      List<FactionWorldData.Route> shown = new ArrayList<>(routes);
      shown.sort(Comparator.comparing(route -> route.village() == null));

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

      return data.armies().size();
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

   private static int addStanding(ServerPlayer player, String faction, int value) {
      int standing = FactionStandingData.get(player.serverLevel()).adjustStanding(player.getUUID(), faction, value);
      player.sendSystemMessage(Component.literal("Faction " + faction + " standing is now " + standing));
      return standing;
   }
}
