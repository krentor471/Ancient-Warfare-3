package net.shadowmage.ancientwarfare.core.entity;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

final class NpcRangedAttackHelper {
   private static final double ARC_ANGLE = Math.toRadians(45.0);
   private NpcRangedAttackHelper() {
   }

   static void configureBowArrow(LivingEntity attacker, AbstractArrow arrow, float drawForce) {
      arrow.setBaseDamage((double)drawForce * 2.0 + attacker.getRandom().nextGaussian() * 0.25);
      int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, attacker.getMainHandItem());
      if (power > 0) {
         arrow.setBaseDamage(arrow.getBaseDamage() + (double)power * 0.5 + 0.5);
      }

      int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, attacker.getMainHandItem());
      if (punch > 0) {
         arrow.setKnockback(punch);
      }

      if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, attacker.getMainHandItem()) > 0) {
         arrow.setSecondsOnFire(100);
      }
   }

   static void configureCrossbowArrow(LivingEntity attacker, AbstractArrow arrow, ItemStack crossbow) {
      arrow.setBaseDamage(2.0 + attacker.getRandom().nextGaussian() * 0.25);
      int piercing = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, crossbow);
      if (piercing > 0) {
         arrow.setPierceLevel((byte)piercing);
      }
   }

   static int crossbowProjectileCount(ItemStack crossbow) {
      return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, crossbow) > 0 ? 3 : 1;
   }

   static int crossbowChargeDuration(ItemStack crossbow) {
      int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, crossbow);
      return Math.max(5, 25 - quickCharge * 5);
   }

   static Vec3 predictTargetPosition(LivingEntity attacker, LivingEntity target, double projectileSpeed) {
      Vec3 predicted = target.position().add(0.0, (double)target.getBbHeight() / 3.0, 0.0);
      Vec3 targetVelocity = target.getDeltaMovement();

      for (int iteration = 0; iteration < 3; iteration++) {
         double distance = attacker.getEyePosition().distanceTo(predicted);
         double flightTicks = distance / Math.max(0.1, projectileSpeed);
         predicted = target.position().add(targetVelocity.scale(flightTicks));
         predicted = predicted.add(0.0, (double)target.getBbHeight() / 3.0, 0.0);
      }

      return predicted;
   }

   static Optional<NpcRangedAttackHelper.BallisticShot> findBallisticShot(
      LivingEntity attacker, LivingEntity target, Vec3 start, double projectileSpeed, double gravity
   ) {
      if (attacker.level() != null && target != null && !(projectileSpeed <= 0.01) && !(gravity <= 0.0)) {
         Vec3 aim = predictTargetPosition(attacker, target, projectileSpeed);
         double dx = aim.x - start.x;
         double dz = aim.z - start.z;
         double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
         if (horizontalDistance < 0.01) {
            return Optional.empty();
         } else {
            double height = aim.y - start.y;
            double speedSquared = projectileSpeed * projectileSpeed;
            double discriminant = speedSquared * speedSquared - gravity * (gravity * horizontalDistance * horizontalDistance + 2.0 * height * speedSquared);
            if (discriminant < 0.0) {
               return Optional.empty();
            } else {
               double root = Math.sqrt(discriminant);
               double lowAngle = Math.atan((speedSquared - root) / (gravity * horizontalDistance));
               double highAngle = Math.atan((speedSquared + root) / (gravity * horizontalDistance));
               Vec3 horizontalDirection = new Vec3(dx / horizontalDistance, 0.0, dz / horizontalDistance);
               NpcRangedAttackHelper.BallisticShot low = shot(horizontalDirection, lowAngle, projectileSpeed);
               if (hasClearPath(attacker, start, horizontalDistance, gravity, low)) {
                  return Optional.of(low);
               } else {
                  NpcRangedAttackHelper.BallisticShot high = shot(horizontalDirection, highAngle, projectileSpeed);
                  return Math.abs(highAngle - lowAngle) > 1.0E-4 && hasClearPath(attacker, start, horizontalDistance, gravity, high)
                     ? Optional.of(high)
                     : Optional.empty();
               }
            }
         }
      } else {
         return Optional.empty();
      }
   }

   /**
    * High arcing shot at a point on the ground, for area fire over the heads of the front ranks. Speed is searched with
    * the arrow's real drag; empty when the point is out of reach at {@code maxSpeed}.
    */
   static Optional<NpcRangedAttackHelper.BallisticShot> findArcShot(Vec3 start, Vec3 aim, double maxSpeed) {
      double dx = aim.x - start.x;
      double dz = aim.z - start.z;
      double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
      if (horizontalDistance < 4.0) {
         return Optional.empty();
      }

      double height = aim.y - start.y;
      if (arcRange(maxSpeed, ARC_ANGLE, height) < horizontalDistance) {
         return Optional.empty();
      }

      double low = 0.3;
      double high = maxSpeed;

      for (int iteration = 0; iteration < 16; iteration++) {
         double middle = (low + high) * 0.5;
         if (arcRange(middle, ARC_ANGLE, height) < horizontalDistance) {
            low = middle;
         } else {
            high = middle;
         }
      }

      return Optional.of(shot(new Vec3(dx / horizontalDistance, 0.0, dz / horizontalDistance), ARC_ANGLE, high));
   }

   /** Horizontal distance an arrow covers before it falls back to {@code height}, stepping like the arrow does in flight. */
   private static double arcRange(double speed, double angle, double height) {
      double vx = speed * Math.cos(angle);
      double vy = speed * Math.sin(angle);
      double x = 0.0;
      double y = 0.0;

      for (int tick = 0; tick < 300; tick++) {
         x += vx;
         y += vy;
         vx *= 0.99;
         vy = vy * 0.99 - 0.05;
         if (vy < 0.0 && y < height) {
            break;
         }
      }

      return x;
   }

   private static NpcRangedAttackHelper.BallisticShot shot(Vec3 horizontalDirection, double angle, double speed) {
      double cos = Math.cos(angle);
      Vec3 direction = horizontalDirection.scale(cos).add(0.0, Math.sin(angle), 0.0).normalize();
      return new NpcRangedAttackHelper.BallisticShot(direction, (float)speed);
   }

   private static boolean hasClearPath(LivingEntity attacker, Vec3 start, double horizontalDistance, double gravity, NpcRangedAttackHelper.BallisticShot shot) {
      double horizontalSpeed = (double)shot.speed() * shot.direction().horizontalDistance();
      if (horizontalSpeed < 0.01) {
         return false;
      } else {
         double flightTime = horizontalDistance / horizontalSpeed;
         int steps = Math.max(4, Math.min(160, (int)Math.ceil(flightTime / 0.5)));
         Vec3 velocity = shot.direction().scale((double)shot.speed());
         Vec3 previous = start;

         for (int step = 1; step <= steps; step++) {
            double time = flightTime * (double)step / (double)steps;
            Vec3 current = start.add(velocity.scale(time)).add(0.0, -0.5 * gravity * time * time, 0.0);
            if (attacker.level().clip(new ClipContext(previous, current, Block.COLLIDER, Fluid.NONE, attacker)).getType() != Type.MISS) {
               return false;
            }

            previous = current;
         }

         return true;
      }
   }

   static record BallisticShot(Vec3 direction, float speed) {
   }
}
