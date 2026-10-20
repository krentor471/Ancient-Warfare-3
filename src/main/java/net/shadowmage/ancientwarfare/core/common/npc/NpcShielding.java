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
      return entity != null && isShield(entity.m_21206_());
   }

   public static boolean isShield(ItemStack stack) {
      return stack != null && !stack.m_41619_() && (stack.m_41720_() instanceof NpcShieldItem || stack.m_41720_() instanceof ShieldItem);
   }

   public static boolean shouldRaiseShield(LivingEntity entity, LivingEntity target) {
      return !hasShield(entity) ? false : targetIsUsingRangedWeapon(target) || findIncomingProjectile(entity, 10.0) != null;
   }

   public static boolean isBlockedFromFront(LivingEntity entity, Vec3 sourcePosition) {
      if (entity != null && sourcePosition != null) {
         Vec3 look = entity.m_20252_(1.0F).m_82541_();
         Vec3 incoming = sourcePosition.m_82505_(entity.m_20182_()).m_82541_();
         return incoming.m_82526_(look) < -0.25;
      } else {
         return false;
      }
   }

   private static boolean targetIsUsingRangedWeapon(LivingEntity target) {
      return target != null && target.m_6117_() && target.m_21211_().m_41779_() > 0 && isRangedWeapon(target.m_21211_());
   }

   @Nullable
   public static Projectile findIncomingProjectile(LivingEntity entity) {
      return findIncomingProjectile(entity, 18.0);
   }

   @Nullable
   private static Projectile findIncomingProjectile(LivingEntity entity, double scanRange) {
      AABB box = entity.m_20191_().m_82400_(scanRange);
      Projectile closest = null;
      double closestInterceptTime = Double.MAX_VALUE;
      Vec3 targetCenter = entity.m_20182_().m_82520_(0.0, (double)entity.m_20206_() * 0.52, 0.0);
      double hitRadius = Math.max(1.05, (double)entity.m_20205_() * 0.5 + 0.55);

      for (Projectile projectile : entity.m_9236_().m_6443_(Projectile.class, box, projectilex -> projectilex.m_6084_() && projectilex.m_19749_() != entity)) {
         if (!isFriendlyProjectileOwner(entity, projectile.m_19749_())) {
            Vec3 velocity = projectile.m_20184_();
            if (!(velocity.m_82556_() < 0.01)) {
               Vec3 toNpc = targetCenter.m_82546_(projectile.m_20182_());
               double interceptTime = toNpc.m_82526_(velocity) / velocity.m_82556_();
               if (!(interceptTime < 0.0) && !(interceptTime > 24.0)) {
                  Vec3 closestPoint = projectile.m_20182_().m_82549_(velocity.m_82490_(interceptTime));
                  if (!(closestPoint.m_82557_(targetCenter) > hitRadius * hitRadius)
                     && !(velocity.m_82541_().m_82526_(toNpc.m_82541_()) < 0.78)
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
      } else if (!defender.m_7307_(owner) && !owner.m_7307_(defender)) {
         if (defender instanceof NpcOwnerAccess ownedDefender) {
            if (owner instanceof Player player && ownedDefender.getOwner().map(player.m_20148_()::equals).orElse(false)) {
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
      return stack.m_150930_(Items.f_42411_) || stack.m_150930_(Items.f_42717_) || stack.m_150930_(Items.f_42713_);
   }
}
