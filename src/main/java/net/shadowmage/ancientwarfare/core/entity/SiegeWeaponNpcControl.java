package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.common.siege.SiegeAmmoSupplier;

final class SiegeWeaponNpcControl {
   private SiegeWeaponNpcControl() {
   }

   static boolean canOperate(ChestPushcartEntity weapon, PathfinderMob engineer) {
      if (weapon.m_6084_() && engineer.m_6084_()) {
         Entity passenger = weapon.m_146895_();
         return passenger == null || passenger == engineer;
      } else {
         return false;
      }
   }

   static boolean beginOperation(ChestPushcartEntity weapon, PathfinderMob engineer) {
      if (!canOperate(weapon, engineer)) {
         return false;
      } else if (engineer.m_20202_() == weapon) {
         return true;
      } else {
         engineer.m_21573_().m_26573_();
         return engineer.m_7998_(weapon, true);
      }
   }

   static void stopOperation(ChestPushcartEntity weapon, PathfinderMob engineer) {
      if (engineer.m_20202_() == weapon) {
         engineer.m_8127_();
      }
   }

   static boolean canAttack(ChestPushcartEntity weapon, LivingEntity target, double minimumRange, double maximumRange) {
      Entity passenger = weapon.m_6688_();
      if (passenger instanceof LivingEntity observer && target != null && target != observer && NpcTargeting.canHarm(observer, target)) {
         double distanceSqr = weapon.m_20280_(target);
         return distanceSqr >= minimumRange * minimumRange && distanceSqr <= maximumRange * maximumRange;
      }

      return false;
   }

   static float worldYawTo(LivingEntity target, Entity weapon) {
      double x = target.m_20185_() - weapon.m_20185_();
      double z = target.m_20189_() - weapon.m_20189_();
      return (float)Math.toDegrees(Math.atan2(-x, z));
   }

   static float pitchTo(LivingEntity target, Entity weapon, double muzzleHeight) {
      double x = target.m_20185_() - weapon.m_20185_();
      double z = target.m_20189_() - weapon.m_20189_();
      double y = target.m_20227_(0.5) - (weapon.m_20186_() + muzzleHeight);
      return (float)Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)));
   }

   static float ballisticPitchTo(LivingEntity target, Entity weapon, double muzzleHeight, double projectileSpeed, double gravity) {
      float[] pitches = ballisticPitchesTo(target, weapon, muzzleHeight, projectileSpeed, gravity);
      return pitches.length == 0 ? pitchTo(target, weapon, muzzleHeight) : pitches[0];
   }

   private static float[] ballisticPitchesTo(LivingEntity target, Entity weapon, double muzzleHeight, double projectileSpeed, double gravity) {
      double x = target.m_20185_() - weapon.m_20185_();
      double z = target.m_20189_() - weapon.m_20189_();
      double distance = Math.sqrt(x * x + z * z);
      if (!(distance < 0.001) && !(projectileSpeed <= 0.001) && !(gravity <= 0.0)) {
         double height = target.m_20227_(0.5) - (weapon.m_20186_() + muzzleHeight);
         double speedSquared = projectileSpeed * projectileSpeed;
         double discriminant = speedSquared * speedSquared - gravity * (gravity * distance * distance + 2.0 * height * speedSquared);
         if (discriminant < 0.0) {
            return new float[0];
         } else {
            double root = Math.sqrt(discriminant);
            float low = (float)Math.toDegrees(Math.atan((speedSquared - root) / (gravity * distance)));
            float high = (float)Math.toDegrees(Math.atan((speedSquared + root) / (gravity * distance)));
            return Math.abs(high - low) < 0.01F ? new float[]{low} : new float[]{low, high};
         }
      } else {
         return new float[]{pitchTo(target, weapon, muzzleHeight)};
      }
   }

   static float clearBallisticPitch(
      Level level,
      Entity weapon,
      LivingEntity target,
      double muzzleHeight,
      double muzzleForwardOffset,
      double speed,
      double gravity,
      float worldYaw,
      float minimumPitch,
      float maximumPitch
   ) {
      return clearBallisticPitch(level, weapon, target, muzzleHeight, muzzleForwardOffset, speed, gravity, worldYaw, minimumPitch, maximumPitch, 0.0);
   }

   static float clearBallisticPitch(
      Level level,
      Entity weapon,
      LivingEntity target,
      double muzzleHeight,
      double muzzleForwardOffset,
      double speed,
      double gravity,
      float worldYaw,
      float minimumPitch,
      float maximumPitch,
      double blockGraceDistance
   ) {
      for (float pitch : ballisticPitchesTo(target, weapon, muzzleHeight, speed, gravity)) {
         if (pitch >= minimumPitch
            && pitch <= maximumPitch
            && hasClearBallisticPath(level, weapon, target, muzzleHeight, muzzleForwardOffset, speed, gravity, worldYaw, pitch, blockGraceDistance)) {
            return pitch;
         }
      }

      return Float.NaN;
   }

   static float localYaw(float worldYaw, Entity weapon) {
      return Mth.m_14177_(worldYaw - weapon.m_146908_());
   }

   static boolean isWithinYaw(float desiredLocalYaw, float maximumYaw) {
      return Math.abs(Mth.m_14177_(desiredLocalYaw)) <= maximumYaw;
   }

   static boolean hasClearBallisticPath(
      Level level,
      Entity weapon,
      LivingEntity target,
      double muzzleHeight,
      double muzzleForwardOffset,
      double speed,
      double gravity,
      float worldYaw,
      float pitchDegrees
   ) {
      return hasClearBallisticPath(level, weapon, target, muzzleHeight, muzzleForwardOffset, speed, gravity, worldYaw, pitchDegrees, 0.0);
   }

   static boolean hasClearBallisticPath(
      Level level,
      Entity weapon,
      LivingEntity target,
      double muzzleHeight,
      double muzzleForwardOffset,
      double speed,
      double gravity,
      float worldYaw,
      float pitchDegrees,
      double blockGraceDistance
   ) {
      if (level != null && target != null && !(speed <= 0.001) && !(gravity < 0.0)) {
         double yaw = Math.toRadians((double)worldYaw);
         double pitch = Math.toRadians((double)pitchDegrees);
         Vec3 horizontalDirection = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
         Vec3 horizontal = horizontalDirection.m_82490_(Math.cos(pitch) * speed);
         Vec3 start = weapon.m_20182_().m_82549_(horizontalDirection.m_82490_(muzzleForwardOffset)).m_82520_(0.0, muzzleHeight, 0.0);
         double targetDistance = Math.sqrt(weapon.m_20275_(target.m_20185_(), weapon.m_20186_(), target.m_20189_()));
         double horizontalSpeed = horizontal.m_165924_();
         if (horizontalSpeed < 0.01) {
            return false;
         } else {
            double flightTime = targetDistance / horizontalSpeed;
            int steps = Math.max(4, Math.min(500, (int)Math.ceil(flightTime / 0.2)));
            Vec3 previous = start;
            double graceDistanceSqr = Math.max(0.0, blockGraceDistance * blockGraceDistance);

            for (int step = 1; step <= steps; step++) {
               double time = flightTime * (double)step / (double)steps;
               Vec3 current = start.m_82549_(horizontal.m_82490_(time)).m_82520_(0.0, Math.sin(pitch) * speed * time - 0.5 * gravity * time * time, 0.0);
               Vec3 clipStart = previous;
               if (graceDistanceSqr > 0.0 && previous.m_82557_(start) < graceDistanceSqr) {
                  if (current.m_82557_(start) <= graceDistanceSqr) {
                     previous = current;
                     continue;
                  }

                  clipStart = segmentSphereExit(previous, current, start, blockGraceDistance);
               }

               if (clipStart.m_82557_(current) > 1.0E-8) {
                  BlockHitResult hit = level.m_45547_(new ClipContext(clipStart, current, Block.COLLIDER, Fluid.NONE, weapon));
                  if (hit.m_6662_() != Type.MISS) {
                     return false;
                  }
               }

               previous = current;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   static Vec3 segmentSphereExit(Vec3 inside, Vec3 outside, Vec3 center, double radius) {
      Vec3 direction = outside.m_82546_(inside);
      Vec3 offset = inside.m_82546_(center);
      double a = direction.m_82556_();
      if (a < 1.0E-10) {
         return outside;
      } else {
         double b = 2.0 * offset.m_82526_(direction);
         double c = offset.m_82556_() - radius * radius;
         double discriminant = b * b - 4.0 * a * c;
         if (discriminant < 0.0) {
            return outside;
         } else {
            double t = (-b + Math.sqrt(discriminant)) / (2.0 * a);
            return inside.m_82549_(direction.m_82490_(Mth.m_14008_(t, 0.0, 1.0)));
         }
      }
   }

   static void faceVehicle(Entity weapon, float worldYaw) {
      weapon.m_146922_(worldYaw);
      weapon.m_5616_(worldYaw);
      weapon.m_5618_(worldYaw);
      weapon.f_19859_ = worldYaw;
   }

   static void supplyFactionAmmo(PathfinderMob engineer, ChestPushcartEntity weapon, ItemStack ammo) {
      if (engineer instanceof SiegeAmmoSupplier && !hasMatchingItem(weapon, ammo)) {
         for (int slot = 0; slot < weapon.m_6643_(); slot++) {
            if (weapon.m_8020_(slot).m_41619_() && weapon.m_7013_(slot, ammo)) {
               weapon.m_6836_(slot, ammo.m_41777_());
               weapon.m_6596_();
               return;
            }
         }
      }
   }

   private static boolean hasMatchingItem(ChestPushcartEntity weapon, ItemStack ammo) {
      for (int slot = 0; slot < weapon.m_6643_(); slot++) {
         if (ItemStack.m_150942_(weapon.m_8020_(slot), ammo)) {
            return true;
         }
      }

      return false;
   }
}
