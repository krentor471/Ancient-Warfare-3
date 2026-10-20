package net.shadowmage.ancientwarfare.core.common.config;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class ConfigFileWatcher implements AutoCloseable {
   private static final Logger LOGGER = System.getLogger(ConfigFileWatcher.class.getName());
   private static final long DEBOUNCE_NANOS = TimeUnit.MILLISECONDS.toNanos(750L);
   private final Path structureFile;
   private final Path lootOverridesRoot;
   private final Consumer<Set<Path>> callback;
   private final WatchService watchService;
   private final Map<WatchKey, Path> directories = new HashMap<>();
   private final Set<Path> knownLootFiles = new HashSet<>();
   private final Set<Path> pending = new HashSet<>();
   private final AtomicBoolean closed = new AtomicBoolean();
   private final Thread thread;
   private long lastChangeNanos;

   public ConfigFileWatcher(Path structureFile, Path lootOverridesRoot, Consumer<Set<Path>> callback) throws IOException {
      this.structureFile = structureFile.toAbsolutePath().normalize();
      this.lootOverridesRoot = lootOverridesRoot.toAbsolutePath().normalize();
      this.callback = Objects.requireNonNull(callback, "callback");
      this.watchService = this.structureFile.getFileSystem().newWatchService();

      try {
         this.registerDirectory(this.structureFile.getParent());
         if (this.structureFile.getParent().getParent() != null) {
            this.registerDirectory(this.structureFile.getParent().getParent());
         }

         this.registerDirectory(this.lootOverridesRoot.getParent());
         this.scanLootDirectories(false);
      } catch (RuntimeException | IOException var5) {
         this.watchService.close();
         throw var5;
      }

      this.thread = new Thread(this::run, "ancientwarfare-config-watcher");
      this.thread.setDaemon(true);
      this.thread.start();
   }

   private void registerDirectory(Path directory) throws IOException {
      WatchKey key = directory.register(
         this.watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE
      );
      this.directories.entrySet().removeIf(entry -> {
         if (entry.getValue().equals(directory) && entry.getKey() != key) {
            entry.getKey().cancel();
            return true;
         } else {
            return false;
         }
      });
      this.directories.put(key, directory);
   }

   private boolean relevantLootFile(Path path) {
      if (path.startsWith(this.lootOverridesRoot) && !path.equals(this.lootOverridesRoot)) {
         String name = path.getFileName().toString();
         return name.endsWith(".json") || path.equals(this.lootOverridesRoot.resolve("pack.mcmeta"));
      } else {
         return false;
      }
   }

   private void scanLootDirectories(final boolean notifyNewFiles) throws IOException {
      if (Files.isDirectory(this.lootOverridesRoot)) {
         Files.walkFileTree(this.lootOverridesRoot, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
               ConfigFileWatcher.this.registerDirectory(directory);
               return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
               if (attributes.isRegularFile() && ConfigFileWatcher.this.relevantLootFile(file)) {
                  boolean added = ConfigFileWatcher.this.knownLootFiles.add(file);
                  if (notifyNewFiles && added) {
                     ConfigFileWatcher.this.changed(file);
                  }
               }

               return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFileFailed(Path file, IOException failure) {
               ConfigFileWatcher.LOGGER.log(Level.WARNING, "Unable to watch configuration path " + file, failure);
               return FileVisitResult.CONTINUE;
            }
         });
      }
   }

   private void changed(Path path) {
      this.pending.add(path);
      this.lastChangeNanos = System.nanoTime();
   }

   private void removeLootDirectory(Path removed) {
      this.knownLootFiles.removeIf(file -> {
         if (file.startsWith(removed)) {
            this.changed(file);
            return true;
         } else {
            return false;
         }
      });
      this.directories.entrySet().removeIf(entry -> {
         if (entry.getValue().startsWith(removed)) {
            entry.getKey().cancel();
            return true;
         } else {
            return false;
         }
      });
   }

   private void overflow() throws IOException {
      this.changed(this.structureFile);
      this.changed(this.lootOverridesRoot);
      this.knownLootFiles.removeIf(file -> {
         if (!Files.isRegularFile(file)) {
            this.changed(file);
            return true;
         } else {
            return false;
         }
      });
      this.scanLootDirectories(true);
   }

   private void processKey(WatchKey key) throws IOException {
      try {
         Path directory = this.directories.get(key);
         if (directory != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
               if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                  this.overflow();
               } else {
                  Path path = (Path)event.context();
                  if (path instanceof Path) {
                     Path relative = path;
                     path = directory.resolve(relative).toAbsolutePath().normalize();
                     if (path.equals(this.structureFile)) {
                        this.changed(path);
                     } else if (path.equals(this.structureFile.getParent())) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                           this.directories.entrySet().removeIf(entry -> {
                              if (entry.getValue().equals(path)) {
                                 entry.getKey().cancel();
                                 return true;
                              } else {
                                 return false;
                              }
                           });
                           this.changed(this.structureFile);
                           if (Files.isDirectory(path)) {
                              this.registerDirectory(path);
                           }
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(path)) {
                           this.registerDirectory(path);
                           this.changed(this.structureFile);
                        }
                     } else if (this.relevantLootFile(path)) {
                        this.changed(path);
                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                           this.knownLootFiles.remove(path);
                        } else if (Files.isRegularFile(path)) {
                           this.knownLootFiles.add(path);
                        }
                     } else if (path.startsWith(this.lootOverridesRoot)) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                           this.removeLootDirectory(path);
                           if (Files.isDirectory(path)) {
                              this.scanLootDirectories(true);
                           }
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(path)) {
                           this.scanLootDirectories(true);
                        }
                     }
                  }
               }
            }
         }
      } finally {
         this.resetKey(key);
      }
   }

   private void resetKey(WatchKey key) {
      if (!key.reset()) {
         Path removed = this.directories.remove(key);
         if (removed != null && removed.startsWith(this.lootOverridesRoot) && !Files.isDirectory(removed)) {
            this.removeLootDirectory(removed);
         }
      }
   }

   private void deliverPending() {
      if (!this.pending.isEmpty() && System.nanoTime() - this.lastChangeNanos >= DEBOUNCE_NANOS) {
         Set<Path> changedPaths = Set.copyOf(this.pending);
         this.pending.clear();
         if (!this.closed.get()) {
            try {
               this.callback.accept(changedPaths);
            } catch (RuntimeException var3) {
               LOGGER.log(Level.WARNING, "Configuration change callback failed", var3);
            }
         }
      }
   }

   private void run() {
      while (!this.closed.get()) {
         try {
            long waitNanos = this.pending.isEmpty() ? TimeUnit.SECONDS.toNanos(1L) : Math.max(1L, DEBOUNCE_NANOS - (System.nanoTime() - this.lastChangeNanos));

            for (WatchKey key = this.watchService.poll(waitNanos, TimeUnit.NANOSECONDS); key != null && !this.closed.get(); key = this.watchService.poll()) {
               this.processKey(key);
            }

            this.deliverPending();
         } catch (ClosedWatchServiceException var4) {
            break;
         } catch (InterruptedException var5) {
            if (!this.closed.get()) {
               continue;
            }
            break;
         } catch (RuntimeException | IOException var6) {
            LOGGER.log(Level.WARNING, "Unable to watch configuration changes", var6);
            this.changed(this.structureFile);
            this.changed(this.lootOverridesRoot);
         }
      }
   }

   @Override
   public void close() {
      if (this.closed.compareAndSet(false, true)) {
         try {
            this.watchService.close();
         } catch (IOException var3) {
            LOGGER.log(Level.WARNING, "Unable to close configuration watcher", var3);
         }

         this.thread.interrupt();
         if (Thread.currentThread() != this.thread) {
            try {
               this.thread.join(2000L);
            } catch (InterruptedException var2) {
               Thread.currentThread().interrupt();
            }
         }
      }
   }
}
