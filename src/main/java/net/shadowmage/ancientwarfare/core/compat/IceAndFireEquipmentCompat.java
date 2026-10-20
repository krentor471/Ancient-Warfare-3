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
      if (weapon != null && !weapon.m_41619_() && target != null && attacker != null && !attacker.m_9236_().f_46443_) {
         ResourceLocation itemId = BuiltInRegistries.f_257033_.m_7981_(weapon.m_41720_());
         if ("iceandfire".equals(itemId.m_135827_()) && SUPPORTED_MELEE_WEAPONS.contains(itemId.m_135815_())) {
            try {
               weapon.m_41720_().m_7579_(weapon, target, attacker);
               if (itemId.m_135815_().endsWith("_sword_venom")) {
                  target.m_7292_(new MobEffectInstance(MobEffects.f_19614_, 200));
               }
            } catch (Throwable var5) {
               AncientWarfareCore.LOG.warn("Failed to apply Ice and Fire NPC weapon effect for {}", itemId, var5);
            }
         }
      }
   }
}
