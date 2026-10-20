package net.shadowmage.ancientwarfare.core.entity;

import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;

final class VerticalRangeTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
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
      super.findTarget();
   }

   protected AABB getTargetSearchArea(double followDistance) {
      return this.mob.getBoundingBox().inflate(followDistance, Math.max(4.0, this.verticalRange.getAsDouble()), followDistance);
   }
}
