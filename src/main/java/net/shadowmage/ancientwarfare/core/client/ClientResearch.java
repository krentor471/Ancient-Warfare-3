package net.shadowmage.ancientwarfare.core.client;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.shadowmage.ancientwarfare.core.research.PlayerResearch;

/** The research the server says this player knows, for views the client opens on its own. */
public final class ClientResearch {
   private static Set<String> known = Set.of();

   private ClientResearch() {
   }

   public static void set(Set<String> research) {
      known = Set.copyOf(research);
   }

   public static boolean has(String research) {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.player != null && minecraft.player.isCreative() || known.contains(research);
   }

   /** Whether the player knows the research; if not, says which one they need. */
   public static boolean require(String research) {
      if (has(research)) {
         return true;
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         minecraft.player.displayClientMessage(PlayerResearch.needs(research), true);
      }

      return false;
   }
}
