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
   private NpcRangedAttackHelper() {
   }

   static void configureBowArrow(LivingEntity attacker, AbstractArrow arrow, float drawForce) {
      arrow.m_36781_((double)drawForce * 2.0 + attacker.m_217043_().m_188583_() * 0.25);
      int power = EnchantmentHelper.m_44843_(Enchantments.f_44988_, attacker.m_21205_());
      if (power > 0) {
         arrow.m_36781_(arrow.m_36789_() + (double)power * 0.5 + 0.5);
      }

      int punch = EnchantmentHelper.m_44843_(Enchantments.f_44989_, attacker.m_21205_());
      if (punch > 0) {
         arrow.m_36735_(punch);
      }

      if (EnchantmentHelper.m_44843_(Enchantments.f_44990_, attacker.m_21205_()) > 0) {
         arrow.m_20254_(100);
      }
   }

   static void configureCrossbowArrow(LivingEntity attacker, AbstractArrow arrow, ItemStack crossbow) {
      arrow.m_36781_(2.0 + attacker.m_217043_().m_188583_() * 0.25);
      int piercing = EnchantmentHelper.m_44843_(Enchantments.f_44961_, crossbow);
      if (piercing > 0) {
         arrow.m_36767_((byte)piercing);
      }
   }

   static int crossbowProjectileCount(ItemStack crossbow) {
      return EnchantmentHelper.m_44843_(Enchantments.f_44959_, crossbow) > 0 ? 3 : 1;
   }

   static int crossbowChargeDuration(ItemStack crossbow) {
      int quickCharge = EnchantmentHelper.m_44843_(Enchantments.f_44960_, crossbow);
      return Math.max(5, 25 - quickCharge * 5);
   }

   static Vec3 predictTargetPosition(LivingEntity attacker, LivingEntity target, double projectileSpeed) {
      Vec3 predicted = target.m_20182_().m_82520_(0.0, (double)target.m_20206_() / 3.0, 0.0);
      Vec3 targetVelocity = target.m_20184_();

      for (int iteration = 0; iteration < 3; iteration++) {
         double distance = attacker.m_146892_().m_82554_(predicted);
         double flightTicks = distance / Math.max(0.1, projectileSpeed);
         predicted = target.m_20182_().m_82549_(targetVelocity.m_82490_(flightTicks));
         predicted = predicted.m_82520_(0.0, (double)target.m_20206_() / 3.0, 0.0);
      }

      return predicted;
   }

   static Optional<NpcRangedAttackHelper.BallisticShot> findBallisticShot(
      LivingEntity attacker, LivingEntity target, Vec3 start, double projectileSpeed, double gravity
   ) {
      if (attacker.m_9236_() != null && target != null && !(projectileSpeed <= 0.01) && !(gravity <= 0.0)) {
         Vec3 aim = predictTargetPosition(attacker, target, projectileSpeed);
         double dx = aim.f_82479_ - start.f_82479_;
         double dz = aim.f_82481_ - start.f_82481_;
         double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
         if (horizontalDistance < 0.01) {
            return Optional.empty();
         } else {
            double height = aim.f_82480_ - start.f_82480_;
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

   private static NpcRangedAttackHelper.BallisticShot shot(Vec3 horizontalDirection, double angle, double speed) {
      double cos = Math.cos(angle);
      Vec3 direction = horizontalDirection.m_82490_(cos).m_82520_(0.0, Math.sin(angle), 0.0).m_82541_();
      return new NpcRangedAttackHelper.BallisticShot(direction, (float)speed);
   }

   private static boolean hasClearPath(LivingEntity attacker, Vec3 start, double horizontalDistance, double gravity, NpcRangedAttackHelper.BallisticShot shot) {
      double horizontalSpeed = (double)shot.speed() * shot.direction().m_165924_();
      if (horizontalSpeed < 0.01) {
         return false;
      } else {
         double flightTime = horizontalDistance / horizontalSpeed;
         int steps = Math.max(4, Math.min(160, (int)Math.ceil(flightTime / 0.5)));
         Vec3 velocity = shot.direction().m_82490_((double)shot.speed());
         Vec3 previous = start;

         for (int step = 1; step <= steps; step++) {
            double time = flightTime * (double)step / (double)steps;
            Vec3 current = start.m_82549_(velocity.m_82490_(time)).m_82520_(0.0, -0.5 * gravity * time * time, 0.0);
            if (attacker.m_9236_().m_45547_(new ClipContext(previous, current, Block.COLLIDER, Fluid.NONE, attacker)).m_6662_() != Type.MISS) {
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
