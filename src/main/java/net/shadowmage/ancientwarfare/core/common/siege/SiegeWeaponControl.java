package net.shadowmage.ancientwarfare.core.common.siege;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public interface SiegeWeaponControl {
   boolean canBeOperatedBy(PathfinderMob var1);

   boolean beginOperation(PathfinderMob var1);

   void stopOperation(PathfinderMob var1);

   boolean canAttackTarget(LivingEntity var1);

   default float projectileSpeed() {
      return 3.0F;
   }

   default float projectileGravity() {
      return 0.03F;
   }

   default double muzzleHeight() {
      return 1.5;
   }

   default double minimumAttackRange() {
      return 0.0;
   }

   default double maximumAttackRange() {
      return 128.0;
   }

   default boolean isMobile() {
      return false;
   }

   void aimAt(PathfinderMob var1, LivingEntity var2);

   boolean fireAt(PathfinderMob var1, LivingEntity var2);
}
