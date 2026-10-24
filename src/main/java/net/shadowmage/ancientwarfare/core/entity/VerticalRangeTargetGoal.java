package net.shadowmage.ancientwarfare.core.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

final class VerticalRangeTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
   private static final int FREE_ATTACKERS = 2;
   private static final double CROWD_PENALTY = 3.0;
   private final DoubleSupplier verticalRange;

   VerticalRangeTargetGoal(
      Mob mob, Class<T> targetType, int randomInterval, boolean mustSee, boolean mustReach, Predicate<LivingEntity> predicate, double verticalRange
   ) {
      this(mob, targetType, randomInterval, mustSee, mustReach, predicate, () -> verticalRange);
   }

   VerticalRangeTargetGoal(
      Mob mob, Class<T> targetType, int randomInterval, boolean mustSee, boolean mustReach, Predicate<LivingEntity> predicate, DoubleSupplier verticalRange
   ) {
      super(mob, targetType, randomInterval, mustSee, mustReach, predicate);
      this.verticalRange = verticalRange;
      if (!mustSee) {
         this.targetConditions.ignoreLineOfSight();
      }
   }

   protected void findTarget() {
      this.targetConditions.range(this.getFollowDistance());
      if (this.targetType == Player.class || this.targetType == ServerPlayer.class) {
         super.findTarget();
      } else {
         this.target = this.pickSpreadTarget();
      }
   }

   /**
    * Nearest enemy, but one that several of our side already fight counts as further away, so a group spreads over
    * the enemy line instead of all hitting the first man.
    */
   private LivingEntity pickSpreadTarget() {
      AABB area = this.getTargetSearchArea(this.getFollowDistance());
      List<T> candidates = this.mob.level().getEntitiesOfClass(this.targetType, area, candidate -> this.targetConditions.test(this.mob, candidate));
      if (candidates.size() <= 1) {
         return candidates.isEmpty() ? null : candidates.get(0);
      } else {
         Map<LivingEntity, Integer> engaged = new HashMap<>();
         for (Mob other : this.mob.level().getEntitiesOfClass(Mob.class, area, other -> other != this.mob && other.getTarget() != null)) {
            engaged.merge(other.getTarget(), 1, Integer::sum);
         }

         LivingEntity best = null;
         double bestScore = Double.MAX_VALUE;
         for (T candidate : candidates) {
            int crowd = Math.max(0, engaged.getOrDefault(candidate, 0) - FREE_ATTACKERS);
            double score = Math.sqrt(this.mob.distanceToSqr(candidate)) + CROWD_PENALTY * crowd;
            if (score < bestScore) {
               bestScore = score;
               best = candidate;
            }
         }

         return best;
      }
   }

   protected AABB getTargetSearchArea(double followDistance) {
      return this.mob.getBoundingBox().inflate(followDistance, Math.max(4.0, this.verticalRange.getAsDouble()), followDistance);
   }
}
