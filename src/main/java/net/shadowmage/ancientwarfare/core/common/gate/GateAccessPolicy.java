package net.shadowmage.ancientwarfare.core.common.gate;

import java.util.UUID;

public final class GateAccessPolicy {
   private GateAccessPolicy() {
   }

   public static boolean canOperate(UUID owner, UUID player, boolean creative, boolean spectator) {
      return !spectator && player != null && (owner == null || owner.equals(player) || creative);
   }

   public static boolean canPack(UUID owner, UUID player, boolean creative, boolean spectator) {
      return canOperate(owner, player, creative, spectator) && (owner != null || creative);
   }
}
