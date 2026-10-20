package net.shadowmage.ancientwarfare.core.compat;

import java.lang.reflect.Method;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

public final class ValkyrienShipyardCompat {
   private static final String MOD_ID = "valkyrienskies";
   private static volatile boolean searched;
   private static volatile boolean failed;
   private static boolean installed;
   private static boolean failureLogged;
   private static Object allocator;
   private static Method isChunkInShipyard;

   private ValkyrienShipyardCompat() {
   }

   public static boolean isReady() {
      initialize();
      return !installed || !failed;
   }

   public static boolean isShipyardChunk(Level level, int chunkX, int chunkZ) {
      initialize();
      if (!installed) {
         return false;
      } else if (failed) {
         return true;
      } else {
         try {
            if (isChunkInShipyard.invoke(allocator, chunkX, chunkZ) instanceof Boolean inShipyard) {
               return inShipyard;
            } else {
               throw new IllegalStateException("VS shipyard allocator did not return a boolean");
            }
         } catch (LinkageError | RuntimeException | ReflectiveOperationException var5) {
            fail(var5);
            return true;
         }
      }
   }

   private static synchronized void initialize() {
      if (!searched) {
         installed = AWCorePlatform.isModLoaded("valkyrienskies");
         if (installed) {
            try {
               Class<?> type = Class.forName("org.valkyrienskies.mod.common.VS2ChunkAllocator");
               allocator = type.getField("INSTANCE").get(null);
               isChunkInShipyard = type.getMethod("isChunkInShipyardCompanion", int.class, int.class);
            } catch (LinkageError | RuntimeException | ReflectiveOperationException var1) {
               fail(var1);
            }
         }

         searched = true;
      }
   }

   private static synchronized void fail(Throwable error) {
      failed = true;
      if (!failureLogged) {
         failureLogged = true;
         AncientWarfareCore.LOG
            .error(
               "Valkyrien Skies shipyard detection failed. AWS natural generation is paused to avoid generating into reserved hull storage; pending chunks are retained. Resolve the VS compatibility issue and restart the server to resume.",
               error
            );
      }
   }
}
