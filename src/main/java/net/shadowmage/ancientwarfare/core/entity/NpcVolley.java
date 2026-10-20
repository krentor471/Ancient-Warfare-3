package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.world.entity.Entity;

/**
 * Shared rhythm for archers: they only start drawing at the top of each cycle, so nearby archers loose together as a
 * volley. Goals tick every other game tick, so a 20-step draw and the cooldown fill about 90 of the 100 ticks of a
 * cycle: the rate of fire stays as it was, one arrow every five seconds.
 */
final class NpcVolley {
   static final int PERIOD_TICKS = 100;
   /** Goal steps after a shot: a little short of the rest of the cycle, so an archer is ready before the next volley. */
   static final int COOLDOWN_TICKS = 20;
   private static final int WINDOW_TICKS = 4;

   private NpcVolley() {
   }

   static boolean canStartDraw(Entity archer) {
      return archer.level().getGameTime() % PERIOD_TICKS < WINDOW_TICKS;
   }
}
