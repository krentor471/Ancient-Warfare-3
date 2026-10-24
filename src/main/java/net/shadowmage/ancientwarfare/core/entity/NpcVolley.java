package net.shadowmage.ancientwarfare.core.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;

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
   /** Archers this close count as shooting together. */
   private static final double MATE_RANGE = 16.0;
   /** How far from the chosen man an archer looks for one nobody is shooting at yet. */
   private static final double SPREAD_RANGE = 6.0;

   private NpcVolley() {
   }

   static boolean canStartDraw(Entity archer) {
      return archer.level().getGameTime() % PERIOD_TICKS < WINDOW_TICKS;
   }

   /**
    * Spreads a volley over the enemy ranks: when comrades already aim at the archer's man, it switches to an enemy next
    * to him that fewer of them aim at, so the arrows do not all land in one soldier.
    */
   static LivingEntity spreadTarget(Mob archer, LivingEntity target, Predicate<LivingEntity> canShoot) {
      if (target == null) {
         return null;
      }

      List<Mob> mates = archer.level()
         .getEntitiesOfClass(
            Mob.class,
            archer.getBoundingBox().inflate(MATE_RANGE),
            mate -> mate != archer
               && mate.isAlive()
               && mate.getTarget() != null
               && NpcEquipmentResolver.isRangedWeapon(mate.getMainHandItem())
               && (NpcTargeting.isProtectedAlly(archer, mate) || archer.isAlliedTo(mate))
         );
      Map<LivingEntity, Integer> aimedAt = new HashMap<>();
      mates.forEach(mate -> aimedAt.merge(mate.getTarget(), 1, Integer::sum));
      int current = aimedAt.getOrDefault(target, 0);
      if (current == 0) {
         return target;
      }

      LivingEntity best = target;
      int bestCount = current;
      double bestDistance = Double.MAX_VALUE;

      for (LivingEntity candidate : archer.level()
         .getEntitiesOfClass(
            LivingEntity.class,
            target.getBoundingBox().inflate(SPREAD_RANGE, 3.0, SPREAD_RANGE),
            candidate -> candidate != target && candidate.isAlive() && sameSide(candidate, target) && NpcTargeting.canHarm(archer, candidate)
         )) {
         int count = aimedAt.getOrDefault(candidate, 0);
         double distance = candidate.distanceToSqr(target);
         if ((count < bestCount || count == bestCount && best != target && distance < bestDistance) && canShoot.test(candidate)) {
            best = candidate;
            bestCount = count;
            bestDistance = distance;
         }
      }

      return best;
   }

   private static boolean sameSide(LivingEntity a, LivingEntity b) {
      return a.getType() == b.getType() && !(a instanceof NpcBaseEntity) || NpcTargeting.isProtectedAlly(a, b) || a.isAlliedTo(b) || b.isAlliedTo(a);
   }
}
