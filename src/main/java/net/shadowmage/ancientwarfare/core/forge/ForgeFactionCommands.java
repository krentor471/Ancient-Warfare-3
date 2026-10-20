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
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.faction.FactionDiplomacy;
import net.shadowmage.ancientwarfare.core.faction.FactionRegions;
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
               "#" + settlement.id() + " " + settlement.faction() + " " + settlement.kind() + " " + template + " at " + center.getX() + " " + center.getY() + " "
                  + center.getZ() + " (" + (int)Math.sqrt(settlement.horizontalDistanceSquared(source.getPosition().x, source.getPosition().z)) + " blocks)"
            )
         );
      }

      return settlements.size();
   }

   private static void tell(CommandSourceStack source, Component message) {
      source.sendSuccess(() -> message, false);
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
