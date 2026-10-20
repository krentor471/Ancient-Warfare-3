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
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                                 "awworldgen"
                              )
                              .requires(source -> source.m_6761_(2)))
                           .then(Commands.m_82127_("status").executes(context -> generationStatus((CommandSourceStack)context.getSource()))))
                        .then(
                           Commands.m_82127_("resume")
                              .executes(
                                 context -> {
                                    boolean resumed = LargeIslandWorldGenerator.resume(((CommandSourceStack)context.getSource()).m_81377_().m_129783_());
                                    ((CommandSourceStack)context.getSource())
                                       .m_288197_(
                                          () -> Component.m_237113_(
                                                resumed ? "Resumed paused large island construction." : "No paused large island construction."
                                             ),
                                          false
                                       );
                                    return resumed ? 1 : 0;
                                 }
                              )
                        ))
                     .then(Commands.m_82127_("generate").executes(context -> generate(((CommandSourceStack)context.getSource()).m_81375_()))))
                  .then(
                     Commands.m_82127_("town")
                        .then(
                           Commands.m_82129_("name", StringArgumentType.greedyString())
                              .suggests(
                                 (context, builder) -> SharedSuggestionProvider.m_82981_(
                                       AwtWorldGenTemplateRegistry.templates().stream().map(AwtWorldGenTemplateRegistry.Template::name), builder
                                    )
                              )
                              .executes(
                                 context -> queueTown(((CommandSourceStack)context.getSource()).m_81375_(), StringArgumentType.getString(context, "name"))
                              )
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.m_82127_("locate").executes(context -> locate(((CommandSourceStack)context.getSource()).m_81375_(), "")))
                     .then(
                        Commands.m_82129_("name", StringArgumentType.greedyString())
                           .suggests(
                              (context, builder) -> SharedSuggestionProvider.m_82981_(
                                    Stream.concat(
                                       AwtWorldGenTemplateRegistry.templates().stream().map(AwtWorldGenTemplateRegistry.Template::name),
                                       AwsWorldGenTemplateRegistry.templates().stream().map(AwsWorldGenTemplateRegistry.Template::name)
                                    ),
                                    builder
                                 )
                           )
                           .executes(context -> locate(((CommandSourceStack)context.getSource()).m_81375_(), StringArgumentType.getString(context, "name")))
                     )
               )
         );
   }

   private static int generationStatus(CommandSourceStack source) {
      ServerLevel level = source.m_81377_().m_129783_();
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
      source.m_288197_(() -> Component.m_237113_("AWS pending candidates: " + pending + "; " + state + details), false);
      return pending;
   }

   private static int generate(ServerPlayer player) {
      ChunkPos center = player.m_146902_();

      for (int radius = 1; radius <= 8; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if ((Math.abs(dx) == radius || Math.abs(dz) == radius)
                  && AwsWorldGenerator.generateAt(player.m_284548_(), new ChunkPos(center.f_45578_ + dx, center.f_45579_ + dz), true)) {
                  player.m_213846_(Component.m_237113_("Generated an AWS structure nearby."));
                  return 1;
               }
            }
         }
      }

      player.m_213846_(Component.m_237113_("No compatible AWS structure could be placed nearby."));
      return 0;
   }

   private static int queueTown(ServerPlayer player, String name) {
      ChunkPos center = player.m_146902_();

      for (int radius = 8; radius <= 32; radius += 4) {
         for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
               if ((Math.abs(dx) == radius || Math.abs(dz) == radius)
                  && AwtWorldGenerator.queueAt(player.m_284548_(), new ChunkPos(center.f_45578_ + dx, center.f_45579_ + dz), true, name)) {
                  player.m_213846_(Component.m_237113_("Queued phased AWT construction for " + name + ".").m_130940_(ChatFormatting.GREEN));
                  return 1;
               }
            }
         }
      }

      player.m_213846_(Component.m_237113_("No matching AWT template or suitable construction area was found.").m_130940_(ChatFormatting.YELLOW));
      return 0;
   }

   private static int locate(ServerPlayer player, String nameFilter) {
      String filter = normalize(nameFilter);
      AwsWorldGenerationData data = AwsWorldGenerationData.get(player.m_284548_());
      Optional<AwsWorldGenerationData.Entry> nearestAws = nearest(data, player, filter);
      Optional<AwtWorldGenerationData.TownTask> nearestAwt = AwtWorldGenerationData.get(player.m_284548_())
         .towns()
         .stream()
         .filter(town -> filter.isBlank() || normalize(town.name).contains(filter))
         .min(Comparator.comparingDouble(town -> town.distanceSquared(player.m_146903_(), player.m_146907_())));
      boolean useAwt = nearestAwt.isPresent()
         && (
            nearestAws.isEmpty()
               || nearestAwt.get().distanceSquared(player.m_146903_(), player.m_146907_())
                  <= nearestAws.get().horizontalDistanceSquared(player.m_146903_(), player.m_146907_())
         );
      if (!useAwt && nearestAws.isEmpty() && filter.isBlank() && generateNearby(player)) {
         nearestAws = nearest(data, player, filter);
         player.m_213846_(
            Component.m_237113_("No previous AWS record existed, so a compatible nearby structure was generated for this search.")
               .m_130940_(ChatFormatting.GRAY)
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
            town.distanceSquared(player.m_146903_(), player.m_146907_())
         );
      } else if (nearestAws.isEmpty()) {
         player.m_213846_(
            Component.m_237113_(
                  filter.isBlank()
                     ? "No Ancient Warfare structure is recorded in this dimension."
                     : "No recorded Ancient Warfare structure matches: " + nameFilter
               )
               .m_130940_(ChatFormatting.YELLOW)
         );
         return 0;
      } else {
         AwsWorldGenerationData.Entry entry = nearestAws.get();
         int x = (entry.minX() + entry.maxX()) / 2;
         int y = entry.minY() + 1;
         int z = (entry.minZ() + entry.maxZ()) / 2;
         return showLocation(player, displayName(entry.template()) + " [AWS]", x, y, z, entry.horizontalDistanceSquared(player.m_146903_(), player.m_146907_()));
      }
   }

   private static int showLocation(ServerPlayer player, String name, int x, int y, int z, double distanceSquared) {
      int distance = (int)Math.round(Math.sqrt(distanceSquared));
      String teleport = "/tp @s " + x + " " + y + " " + z;
      Component coordinates = Component.m_237113_("[" + x + ", " + y + ", " + z + "]")
         .m_130938_(
            style -> style.m_131140_(ChatFormatting.GREEN)
                  .m_131162_(true)
                  .m_131142_(new ClickEvent(Action.RUN_COMMAND, teleport))
                  .m_131144_(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.f_130831_, Component.m_237113_("Click to teleport")))
         );
      player.m_213846_(
         Component.m_237113_("Nearest Ancient Warfare structure: ")
            .m_7220_(Component.m_237113_(name).m_130940_(ChatFormatting.GOLD))
            .m_7220_(Component.m_237113_(" at "))
            .m_7220_(coordinates)
            .m_7220_(Component.m_237113_(" (" + distance + " blocks away)"))
      );
      return distance;
   }

   private static Optional<AwsWorldGenerationData.Entry> nearest(AwsWorldGenerationData data, ServerPlayer player, String filter) {
      return data.entries()
         .stream()
         .filter(entry -> filter.isBlank() || normalize(displayName(entry.template())).contains(filter))
         .min(Comparator.comparingDouble(entry -> entry.horizontalDistanceSquared(player.m_146903_(), player.m_146907_())));
   }

   private static boolean generateNearby(ServerPlayer player) {
      ChunkPos center = player.m_146902_();

      for (int radius = 1; radius <= 8; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if ((Math.abs(dx) == radius || Math.abs(dz) == radius)
                  && AwsWorldGenerator.isCandidateChunk(player.m_284548_().m_7328_(), new ChunkPos(center.f_45578_ + dx, center.f_45579_ + dz))
                  && AwsWorldGenerator.generateAt(player.m_284548_(), new ChunkPos(center.f_45578_ + dx, center.f_45579_ + dz), true)) {
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
