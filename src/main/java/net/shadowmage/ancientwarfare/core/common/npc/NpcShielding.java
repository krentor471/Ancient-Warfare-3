package net.shadowmage.ancientwarfare.core.common.npc;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.NpcShieldItem;

public final class NpcShielding {
   private static final double PROJECTILE_SCAN_RANGE = 10.0;
   private static final double BLADE_PROJECTILE_SCAN_RANGE = 18.0;
   private static final double MAX_INTERCEPT_TICKS = 24.0;

   private NpcShielding() {
   }

   public static boolean hasShield(LivingEntity entity) {
      return entity != null && isShield(entity.getOffhandItem());
   }

   public static boolean isShield(ItemStack stack) {
      return stack != null && !stack.isEmpty() && (stack.getItem() instanceof NpcShieldItem || stack.getItem() instanceof ShieldItem);
   }

   public static boolean shouldRaiseShield(LivingEntity entity, LivingEntity target) {
      return !hasShield(entity) ? false : targetIsUsingRangedWeapon(target) || findIncomingProjectile(entity, 10.0) != null;
   }

   public static boolean isBlockedFromFront(LivingEntity entity, Vec3 sourcePosition) {
      if (entity != null && sourcePosition != null) {
         Vec3 look = entity.getViewVector(1.0F).normalize();
         Vec3 incoming = sourcePosition.vectorTo(entity.position()).normalize();
         return incoming.dot(look) < -0.25;
      } else {
         return false;
      }
   }

   private static boolean targetIsUsingRangedWeapon(LivingEntity target) {
      return target != null && target.isUsingItem() && target.getUseItem().getUseDuration() > 0 && isRangedWeapon(target.getUseItem());
   }

   @Nullable
   public static Projectile findIncomingProjectile(LivingEntity entity) {
      return findIncomingProjectile(entity, 18.0);
   }

   @Nullable
   private static Projectile findIncomingProjectile(LivingEntity entity, double scanRange) {
      AABB box = entity.getBoundingBox().inflate(scanRange);
      Projectile closest = null;
      double closestInterceptTime = Double.MAX_VALUE;
      Vec3 targetCenter = entity.position().add(0.0, (double)entity.getBbHeight() * 0.52, 0.0);
      double hitRadius = Math.max(1.05, (double)entity.getBbWidth() * 0.5 + 0.55);

      for (Projectile projectile : entity.level().getEntitiesOfClass(Projectile.class, box, projectilex -> projectilex.isAlive() && projectilex.getOwner() != entity)) {
         if (!isFriendlyProjectileOwner(entity, projectile.getOwner())) {
            Vec3 velocity = projectile.getDeltaMovement();
            if (!(velocity.lengthSqr() < 0.01)) {
               Vec3 toNpc = targetCenter.subtract(projectile.position());
               double interceptTime = toNpc.dot(velocity) / velocity.lengthSqr();
               if (!(interceptTime < 0.0) && !(interceptTime > 24.0)) {
                  Vec3 closestPoint = projectile.position().add(velocity.scale(interceptTime));
                  if (!(closestPoint.distanceToSqr(targetCenter) > hitRadius * hitRadius)
                     && !(velocity.normalize().dot(toNpc.normalize()) < 0.78)
                     && interceptTime < closestInterceptTime) {
                     closestInterceptTime = interceptTime;
                     closest = projectile;
                  }
               }
            }
         }
      }

      return closest;
   }

   private static boolean isFriendlyProjectileOwner(LivingEntity defender, @Nullable Entity owner) {
      if (owner == null) {
         return false;
      } else if (!defender.isAlliedTo(owner) && !owner.isAlliedTo(defender)) {
         if (defender instanceof NpcOwnerAccess ownedDefender) {
            if (owner instanceof Player player && ownedDefender.getOwner().map(player.getUUID()::equals).orElse(false)) {
               return true;
            }

            if (owner instanceof NpcOwnerAccess ownedShooter && ownedDefender.getOwner().equals(ownedShooter.getOwner())) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean isRangedWeapon(ItemStack stack) {
      return stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT);
   }
}
