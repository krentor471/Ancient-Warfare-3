package net.shadowmage.ancientwarfare.core.common.npc;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.item.NpcShieldItem;
import net.shadowmage.ancientwarfare.core.item.NpcWeaponItem;

public final class NpcEquipmentResolver {
   private static final double DEFAULT_ATTACK_SPEED = 4.0;
   private static final int MIN_MELEE_INTERVAL = 4;
   private static final int MAX_MELEE_INTERVAL = 60;
   private static final double DEFAULT_ENTITY_REACH = 3.0;
   private static final double MIN_ENTITY_REACH = 1.5;
   private static final double MAX_ENTITY_REACH = 16.0;

   private NpcEquipmentResolver() {
   }

   public static NpcEquipmentResolver.WeaponType weaponType(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return NpcEquipmentResolver.WeaponType.NONE;
      } else if (stack.getItem() instanceof CrossbowItem) {
         return NpcEquipmentResolver.WeaponType.CROSSBOW;
      } else if (stack.getItem() instanceof BowItem) {
         return NpcEquipmentResolver.WeaponType.BOW;
      } else if (stack.getItem() instanceof TridentItem) {
         return NpcEquipmentResolver.WeaponType.TRIDENT;
      } else if (stack.getItem() instanceof ShieldItem || stack.getItem() instanceof NpcShieldItem) {
         return NpcEquipmentResolver.WeaponType.SHIELD;
      } else if (stack.getItem() instanceof SwordItem) {
         return NpcEquipmentResolver.WeaponType.SWORD;
      } else if (stack.getItem() instanceof AxeItem) {
         return NpcEquipmentResolver.WeaponType.AXE;
      } else {
         return attackDamageModifier(stack) > 0.0 ? NpcEquipmentResolver.WeaponType.MELEE : NpcEquipmentResolver.WeaponType.OTHER;
      }
   }

   public static boolean isRangedWeapon(ItemStack stack) {
      NpcEquipmentResolver.WeaponType type = weaponType(stack);
      return type == NpcEquipmentResolver.WeaponType.BOW || type == NpcEquipmentResolver.WeaponType.CROSSBOW;
   }

   public static boolean isCrossbow(ItemStack stack) {
      return weaponType(stack) == NpcEquipmentResolver.WeaponType.CROSSBOW;
   }

   public static boolean isMeleeWeapon(ItemStack stack) {
      return switch (weaponType(stack)) {
         case SWORD, AXE, TRIDENT, MELEE -> true;
         default -> false;
      };
   }

   public static EquipmentSlot equipmentSlot(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         if (stack.getItem() instanceof ArmorItem armor) {
            return armor.getEquipmentSlot();
         } else {
            return weaponType(stack) == NpcEquipmentResolver.WeaponType.SHIELD ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
         }
      } else {
         return EquipmentSlot.MAINHAND;
      }
   }

   public static double attackDamage(LivingEntity npc, ItemStack stack, LivingEntity target) {
      double damage = npc.getAttributeValue(Attributes.ATTACK_DAMAGE);
      if (stack != null && !stack.isEmpty() && target != null) {
         damage += (double)EnchantmentHelper.getDamageBonus(stack, target.getMobType());
      }

      return Math.max(0.0, damage);
   }

   public static double attackSpeed(LivingEntity npc) {
      return npc.getAttribute(Attributes.ATTACK_SPEED) == null ? 4.0 : Math.max(0.1, npc.getAttributeValue(Attributes.ATTACK_SPEED));
   }

   public static int meleeAttackInterval(LivingEntity npc) {
      int ticks = (int)Math.ceil(20.0 / attackSpeed(npc));
      return Math.max(4, Math.min(60, ticks));
   }

   public static double meleeAttackReach(LivingEntity npc) {
      ItemStack stack = npc == null ? ItemStack.EMPTY : npc.getMainHandItem();
      if (!stack.isEmpty() && stack.getItem() instanceof NpcWeaponItem npcWeapon) {
         return clampReach((double)npcWeapon.reach());
      } else if (stack.isEmpty()) {
         return 3.0;
      } else {
         Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
         Attribute reachAttribute = AWCorePlatform.entityReachAttribute();
         if (reachAttribute == null) {
            return 3.0;
         } else {
            double addition = 0.0;
            double multiplyBase = 0.0;
            double multiplyTotal = 1.0;

            for (AttributeModifier modifier : modifiers.get(reachAttribute)) {
               switch (modifier.getOperation()) {
                  case ADDITION:
                     addition += modifier.getAmount();
                     break;
                  case MULTIPLY_BASE:
                     multiplyBase += modifier.getAmount();
                     break;
                  case MULTIPLY_TOTAL:
                     multiplyTotal *= 1.0 + modifier.getAmount();
               }
            }

            return clampReach((3.0 + addition) * (1.0 + multiplyBase) * multiplyTotal);
         }
      }
   }

   public static double meleeHoldDistance(LivingEntity npc) {
      return Math.max(1.5, meleeAttackReach(npc) - 0.2);
   }

   private static double clampReach(double reach) {
      return Math.max(1.5, Math.min(16.0, reach));
   }

   private static double attackDamageModifier(ItemStack stack) {
      return modifierValue(stack.getAttributeModifiers(EquipmentSlot.MAINHAND), Attributes.ATTACK_DAMAGE);
   }

   private static double modifierValue(Multimap<Attribute, AttributeModifier> modifiers, Attribute attribute) {
      double value = 0.0;

      for (AttributeModifier modifier : modifiers.get(attribute)) {
         if (modifier.getOperation() == Operation.ADDITION) {
            value += modifier.getAmount();
         }
      }

      return value;
   }

   public static enum WeaponType {
      NONE,
      SWORD,
      AXE,
      BOW,
      CROSSBOW,
      TRIDENT,
      SHIELD,
      MELEE,
      OTHER;
   }
}
