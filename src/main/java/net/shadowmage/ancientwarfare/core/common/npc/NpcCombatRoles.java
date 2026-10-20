package net.shadowmage.ancientwarfare.core.common.npc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Rock-paper-scissors between kinds of troops: spears stop horsemen, horsemen ride down archers, archers wear down
 * infantry without shields, and swordsmen get inside a spear's reach.
 */
public final class NpcCombatRoles {
   private static final float SPEAR_VS_RIDER = 1.75F;
   private static final float RIDER_VS_ARCHER = 1.5F;
   private static final float LANCE_VS_FOOT = 1.25F;
   private static final float ARROW_VS_UNSHIELDED = 1.2F;
   private static final float BLADE_VS_SPEAR = 1.25F;

   private NpcCombatRoles() {
   }

   public static float damageMultiplier(DamageSource source, LivingEntity victim) {
      Entity direct = source.getDirectEntity();
      if (direct instanceof AbstractArrow) {
         return !victim.isPassenger() && !hasShield(victim) ? ARROW_VS_UNSHIELDED : 1.0F;
      } else if (direct instanceof LivingEntity attacker && direct == source.getEntity()) {
         ItemStack weapon = attacker.getMainHandItem();
         float multiplier = 1.0F;
         if (isPolearm(weapon) && victim.isPassenger()) {
            multiplier *= SPEAR_VS_RIDER;
         }

         if (attacker.isPassenger() && !victim.isPassenger()) {
            if (NpcEquipmentResolver.isRangedWeapon(victim.getMainHandItem())) {
               multiplier *= RIDER_VS_ARCHER;
            }

            if (isLance(weapon)) {
               multiplier *= LANCE_VS_FOOT;
            }
         }

         if (!attacker.isPassenger() && !isPolearm(weapon) && !NpcEquipmentResolver.isRangedWeapon(weapon) && isPolearm(victim.getMainHandItem())) {
            multiplier *= BLADE_VS_SPEAR;
         }

         return multiplier;
      } else {
         return 1.0F;
      }
   }

   public static boolean isPolearm(ItemStack stack) {
      String path = itemPath(stack);
      return path.endsWith("spear") || path.endsWith("halberd") || path.endsWith("pike");
   }

   public static boolean isLance(ItemStack stack) {
      return itemPath(stack).endsWith("lance");
   }

   private static boolean hasShield(LivingEntity entity) {
      return entity.getOffhandItem().getItem() instanceof ShieldItem || entity.getMainHandItem().getItem() instanceof ShieldItem;
   }

   private static String itemPath(ItemStack stack) {
      return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
   }
}
