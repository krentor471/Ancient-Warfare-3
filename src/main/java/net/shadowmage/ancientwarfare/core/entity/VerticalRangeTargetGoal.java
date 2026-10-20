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
         this.f_26051_.m_148355_();
      }
   }

   protected void m_26073_() {
      this.f_26051_.m_26883_(this.m_7623_());
      super.m_26073_();
   }

   protected AABB m_7255_(double followDistance) {
      return this.f_26135_.m_20191_().m_82377_(followDistance, Math.max(4.0, this.verticalRange.getAsDouble()), followDistance);
   }
}
