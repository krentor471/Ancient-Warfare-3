package net.shadowmage.ancientwarfare.core.compat;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public final class IceAndFireEquipmentCompat {
   private static final int MYRMEX_VENOM_DURATION = 200;
   private static final Set<String> SUPPORTED_MELEE_WEAPONS = Set.of(
      "dragonbone_sword_fire",
      "dragonbone_sword_ice",
      "dragonbone_sword_lightning",
      "dragonsteel_fire_sword",
      "dragonsteel_fire_axe",
      "dragonsteel_ice_sword",
      "dragonsteel_ice_axe",
      "dragonsteel_lightning_sword",
      "dragonsteel_lightning_axe",
      "silver_sword",
      "silver_axe",
      "myrmex_desert_sword",
      "myrmex_desert_sword_venom",
      "myrmex_desert_axe",
      "myrmex_jungle_sword",
      "myrmex_jungle_sword_venom",
      "myrmex_jungle_axe"
   );

   private IceAndFireEquipmentCompat() {
   }

   public static void applySupportedMeleeHit(ItemStack weapon, LivingEntity target, LivingEntity attacker) {
      if (weapon != null && !weapon.isEmpty() && target != null && attacker != null && !attacker.level().isClientSide) {
         ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(weapon.getItem());
         if ("iceandfire".equals(itemId.getNamespace()) && SUPPORTED_MELEE_WEAPONS.contains(itemId.getPath())) {
            try {
               weapon.getItem().hurtEnemy(weapon, target, attacker);
               if (itemId.getPath().endsWith("_sword_venom")) {
                  target.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
               }
            } catch (Throwable var5) {
               AncientWarfareCore.LOG.warn("Failed to apply Ice and Fire NPC weapon effect for {}", itemId, var5);
            }
         }
      }
   }
}
