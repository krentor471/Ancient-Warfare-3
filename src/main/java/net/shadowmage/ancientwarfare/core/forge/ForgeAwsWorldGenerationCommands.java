package net.shadowmage.ancientwarfare.core.forge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.compat.ValkyrienShipyardCompat;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenTemplateRegistry;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerationData;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerationQueue;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenTemplateRegistry;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenerationData;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenerator;
import net.shadowmage.ancientwarfare.core.structure.LargeIslandWorldGenerator;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeAwsWorldGenerationCommands {
   private ForgeAwsWorldGenerationCommands() {
   }

   @SubscribeEvent
   public static void registerCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "awworldgen"
                              )
                              .requires(source -> source.hasPermission(2)))
                           .then(Commands.literal("status").executes(context -> generationStatus((CommandSourceStack)context.getSource()))))
                        .then(
                           Commands.literal("resume")
                              .executes(
                                 context -> {
                                    boolean resumed = LargeIslandWorldGenerator.resume(((CommandSourceStack)context.getSource()).getServer().overworld());
                                    ((CommandSourceStack)context.getSource())
                                       .sendSuccess(
                                          () -> Component.literal(
                                                resumed ? "Resumed paused large island construction." : "No paused large island construction."
                                             ),
                                          false
                                       );
                                    return resumed ? 1 : 0;
                                 }
                              )
                        ))
                     .then(Commands.literal("generate").executes(context -> generate(((CommandSourceStack)context.getSource()).getPlayerOrException()))))
                  .then(
                     Commands.literal("town")
                        .then(
                           Commands.argument("name", StringArgumentType.greedyString())
                              .suggests(
                                 (context, builder) -> SharedSuggestionProvider.suggest(
                                       AwtWorldGenTemplateRegistry.templates().stream().map(AwtWorldGenTemplateRegistry.Template::name), builder
                                    )
                              )
                              .executes(
                                 context -> queueTown(((CommandSourceStack)context.getSource()).getPlayerOrException(), StringArgumentType.getString(context, "name"))
                              )
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("locate").executes(context -> locate(((CommandSourceStack)context.getSource()).getPlayerOrException(), "")))
                     .then(
                        Commands.argument("name", StringArgumentType.greedyString())
                           .suggests(
                              (context, builder) -> SharedSuggestionProvider.suggest(
                                    Stream.concat(
                                       AwtWorldGenTemplateRegistry.templates().stream().map(AwtWorldGenTemplateRegistry.Template::name),
                                       AwsWorldGenTemplateRegistry.templates().stream().map(AwsWorldGenTemplateRegistry.Template::name)
                                    ),
                                    builder
                                 )
                           )
                           .executes(context -> locate(((CommandSourceStack)context.getSource()).getPlayerOrException(), StringArgumentType.getString(context, "name")))
                     )
               )
         );
   }

   private static int generationStatus(CommandSourceStack source) {
      ServerLevel level = source.getServer().overworld();
      int pending = AwsWorldGenerationQueue.get(level).pendingCount();
      String state = !ValkyrienShipyardCompat.isReady()
         ? "paused: VS compatibility error"
         : (
            !AWCorePlatform.gameplayConfig().awsEnabled()
               ? "paused: AWS disabled"
               : (
                  LargeIslandWorldGenerator.active(level)
                     ? "building phased AWS"
                     : (AwtWorldGenerationData.get(level).active() != null ? "waiting for AWT construction" : (pending > 0 ? "processing" : "idle"))
               )
         );
      String islandStatus = LargeIslandWorldGenerator.status(level);
      String details = islandStatus != null && !islandStatus.isBlank() ? "; " + islandStatus : "";
      source.sendSuccess(() -> Component.literal("AWS pending candidates: " + pending + "; " + state + details), false);
      return pending;
   }

   private static int generate(ServerPlayer player) {
      ChunkPos center = player.chunkPosition();

      for (int radius = 1; radius <= 8; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if ((Math.abs(dx) == radius || Math.abs(dz) == radius)
                  && AwsWorldGenerator.generateAt(player.serverLevel(), new ChunkPos(center.x + dx, center.z + dz), true)) {
                  player.sendSystemMessage(Component.literal("Generated an AWS structure nearby."));
                  return 1;
               }
            }
         }
      }

      player.sendSystemMessage(Component.literal("No compatible AWS structure could be placed nearby."));
      return 0;
   }

   private static int queueTown(ServerPlayer player, String name) {
      ChunkPos center = player.chunkPosition();

      for (int radius = 8; radius <= 32; radius += 4) {
         for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
               if ((Math.abs(dx) == radius || Math.abs(dz) == radius)
                  && AwtWorldGenerator.queueAt(player.serverLevel(), new ChunkPos(center.x + dx, center.z + dz), true, name)) {
                  player.sendSystemMessage(Component.literal("Queued phased AWT construction for " + name + ".").withStyle(ChatFormatting.GREEN));
                  return 1;
               }
            }
         }
      }

      player.sendSystemMessage(Component.literal("No matching AWT template or suitable construction area was found.").withStyle(ChatFormatting.YELLOW));
      return 0;
   }

   private static int locate(ServerPlayer player, String nameFilter) {
      String filter = normalize(nameFilter);
      AwsWorldGenerationData data = AwsWorldGenerationData.get(player.serverLevel());
      Optional<AwsWorldGenerationData.Entry> nearestAws = nearest(data, player, filter);
      Optional<AwtWorldGenerationData.TownTask> nearestAwt = AwtWorldGenerationData.get(player.serverLevel())
         .towns()
         .stream()
         .filter(town -> filter.isBlank() || normalize(town.name).contains(filter))
         .min(Comparator.comparingDouble(town -> town.distanceSquared(player.getBlockX(), player.getBlockZ())));
      boolean useAwt = nearestAwt.isPresent()
         && (
            nearestAws.isEmpty()
               || nearestAwt.get().distanceSquared(player.getBlockX(), player.getBlockZ())
                  <= nearestAws.get().horizontalDistanceSquared(player.getBlockX(), player.getBlockZ())
         );
      if (!useAwt && nearestAws.isEmpty() && filter.isBlank() && generateNearby(player)) {
         nearestAws = nearest(data, player, filter);
         player.sendSystemMessage(
            Component.literal("No previous AWS record existed, so a compatible nearby structure was generated for this search.")
               .withStyle(ChatFormatting.GRAY)
         );
      }

      if (useAwt) {
         AwtWorldGenerationData.TownTask town = nearestAwt.get();
         return showLocation(
            player,
            town.name + " [AWT " + town.phase.name().toLowerCase(Locale.ROOT) + "]",
            town.x,
            town.y,
            town.z,
            town.distanceSquared(player.getBlockX(), player.getBlockZ())
         );
      } else if (nearestAws.isEmpty()) {
         player.sendSystemMessage(
            Component.literal(
                  filter.isBlank()
                     ? "No Ancient Warfare structure is recorded in this dimension."
                     : "No recorded Ancient Warfare structure matches: " + nameFilter
               )
               .withStyle(ChatFormatting.YELLOW)
         );
         return 0;
      } else {
         AwsWorldGenerationData.Entry entry = nearestAws.get();
         int x = (entry.minX() + entry.maxX()) / 2;
         int y = entry.minY() + 1;
         int z = (entry.minZ() + entry.maxZ()) / 2;
         return showLocation(player, displayName(entry.template()) + " [AWS]", x, y, z, entry.horizontalDistanceSquared(player.getBlockX(), player.getBlockZ()));
      }
   }

   private static int showLocation(ServerPlayer player, String name, int x, int y, int z, double distanceSquared) {
      int distance = (int)Math.round(Math.sqrt(distanceSquared));
      String teleport = "/tp @s " + x + " " + y + " " + z;
      Component coordinates = Component.literal("[" + x + ", " + y + ", " + z + "]")
         .withStyle(
            style -> style.withColor(ChatFormatting.GREEN)
                  .withUnderlined(true)
                  .withClickEvent(new ClickEvent(Action.RUN_COMMAND, teleport))
                  .withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport")))
         );
      player.sendSystemMessage(
         Component.literal("Nearest Ancient Warfare structure: ")
            .append(Component.literal(name).withStyle(ChatFormatting.GOLD))
            .append(Component.literal(" at "))
            .append(coordinates)
            .append(Component.literal(" (" + distance + " blocks away)"))
      );
      return distance;
   }

   private static Optional<AwsWorldGenerationData.Entry> nearest(AwsWorldGenerationData data, ServerPlayer player, String filter) {
      return data.entries()
         .stream()
         .filter(entry -> filter.isBlank() || normalize(displayName(entry.template())).contains(filter))
         .min(Comparator.comparingDouble(entry -> entry.horizontalDistanceSquared(player.getBlockX(), player.getBlockZ())));
   }

   private static boolean generateNearby(ServerPlayer player) {
      ChunkPos center = player.chunkPosition();

      for (int radius = 1; radius <= 8; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if ((Math.abs(dx) == radius || Math.abs(dz) == radius)
                  && AwsWorldGenerator.isCandidateChunk(player.serverLevel().getSeed(), new ChunkPos(center.x + dx, center.z + dz))
                  && AwsWorldGenerator.generateAt(player.serverLevel(), new ChunkPos(center.x + dx, center.z + dz), true)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static String displayName(String templatePath) {
      int slash = Math.max(templatePath.lastIndexOf(47), templatePath.lastIndexOf(92));
      String name = slash >= 0 ? templatePath.substring(slash + 1) : templatePath;
      return name.toLowerCase().endsWith(".aws") ? name.substring(0, name.length() - 4) : name;
   }

   private static String normalize(String value) {
      return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
   }
}
