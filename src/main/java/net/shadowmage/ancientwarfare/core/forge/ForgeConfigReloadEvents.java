package net.shadowmage.ancientwarfare.core.forge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent.Reloading;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.config.ConfigFileWatcher;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.forge.config.AWLootConfig;
import net.shadowmage.ancientwarfare.core.forge.config.AWNpcConfig;
import net.shadowmage.ancientwarfare.core.forge.config.AWStructureConfig;
import net.shadowmage.ancientwarfare.core.forge.config.AWTargetingConfig;
import net.shadowmage.ancientwarfare.core.structure.AWStructureTemplateOverrides;
import net.shadowmage.ancientwarfare.core.structure.AwsWorldGenTemplateRegistry;
import net.shadowmage.ancientwarfare.core.structure.AwtWorldGenTemplateRegistry;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeConfigReloadEvents {
   private static ForgeConfigReloadEvents.ReloadSession session;

   private ForgeConfigReloadEvents() {
   }

   public static void onTomlReload(Reloading event) {
      IConfigSpec<? extends IConfigSpec<?>> spec = event.getConfig().getSpec();
      if (spec == AWLootConfig.SPEC || spec == AWNpcConfig.SPEC || spec == AWStructureConfig.SPEC || spec == AWTargetingConfig.SPEC) {
         AncientWarfareCore.LOG.info("Applied saved Ancient Warfare configuration: {}", event.getConfig().getFileName());
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         if (spec == AWStructureConfig.SPEC && server != null) {
            server.execute(() -> {
               if (ServerLifecycleHooks.getCurrentServer() == server && !server.isStopped()) {
                  ForgeAwsWorldGenerationEvents.refreshConfigCooldown();
               }
            });
         }
      }
   }

   @SubscribeEvent
   public static void onServerStarted(ServerStartedEvent event) {
      stopWatcher();
      if (AWStructureTemplateOverrides.reload()) {
         invalidateTemplates();
      }

      ForgeConfigReloadEvents.ReloadSession created = new ForgeConfigReloadEvents.ReloadSession(event.getServer());
      session = created;

      try {
         created.watcher = new ConfigFileWatcher(created.structureFile, created.lootRoot, changes -> created.server.execute(() -> {
               if (session == created && !created.server.isStopped()) {
                  created.changed(changes);
               }
            }));
         AncientWarfareCore.LOG.info("Watching Ancient Warfare structure and loot overrides; save files to apply changes automatically");
      } catch (IOException var3) {
         AncientWarfareCore.LOG.error("Unable to watch Ancient Warfare JSON settings; TOML auto-reload remains available", var3);
      }
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      stopWatcher();
   }

   private static void stopWatcher() {
      ForgeConfigReloadEvents.ReloadSession previous = session;
      session = null;
      if (previous != null && previous.watcher != null) {
         previous.watcher.close();
      }
   }

   private static void invalidateTemplates() {
      AwsWorldGenTemplateRegistry.invalidateCaches();
      AwtWorldGenTemplateRegistry.invalidateCaches();
      ForgeAwsWorldGenerationEvents.refreshConfigCooldown();
   }

   private static final class ReloadSession {
      private final MinecraftServer server;
      private final Path structureFile = AWStructureTemplateOverrides.overrideFile().toAbsolutePath().normalize();
      private final Path lootRoot = AWCorePlatform.configDirectory().resolve("ancientwarfare-loot-tables/overrides").toAbsolutePath().normalize();
      private final Set<Path> pending = new LinkedHashSet<>();
      private ConfigFileWatcher watcher;
      private boolean reloadingLoot;

      private ReloadSession(MinecraftServer server) {
         this.server = server;
      }

      private void changed(Set<Path> changes) {
         this.pending.addAll(changes);
         this.applyPending();
      }

      private void applyPending() {
         if (this.pending.remove(this.structureFile) && AWStructureTemplateOverrides.reload()) {
            ForgeConfigReloadEvents.invalidateTemplates();
         }

         if (!this.reloadingLoot && !this.pending.stream().noneMatch(path -> path.startsWith(this.lootRoot))) {
            this.pending.removeIf(path -> path.startsWith(this.lootRoot));

            ForgeLootOverrideSnapshot saved;
            try {
               saved = ForgeLootOverrideSnapshot.read(this.lootRoot);
            } catch (RuntimeException | IOException var5) {
               AncientWarfareCore.LOG
                  .error("Invalid Ancient Warfare loot override; keeping the current loaded resources. Fix and save the JSON to retry", var5);
               return;
            }

            ForgeLootOverrideSnapshot previous = ForgeLootTableOverridePack.activeSnapshot();
            ForgeLootTableOverridePack.useSnapshot(saved);
            this.reloadingLoot = true;

            try {
               this.server.reloadResources(this.server.getPackRepository().getSelectedIds()).whenComplete((unused, failure) -> this.server.execute(() -> {
                     if (ForgeConfigReloadEvents.session == this && !this.server.isStopped()) {
                        this.reloadingLoot = false;
                        if (failure == null) {
                           AncientWarfareCore.LOG.info("Applied saved Ancient Warfare loot override files");
                        } else {
                           ForgeLootTableOverridePack.useSnapshot(previous);
                           AncientWarfareCore.LOG.error("Ancient Warfare loot reload failed; fix and save the override files to retry", failure);
                        }

                        this.applyPending();
                     }
                  }));
            } catch (RuntimeException var4) {
               this.reloadingLoot = false;
               ForgeLootTableOverridePack.useSnapshot(previous);
               AncientWarfareCore.LOG.error("Unable to start Ancient Warfare loot override reload", var4);
            }
         }
      }
   }
}
