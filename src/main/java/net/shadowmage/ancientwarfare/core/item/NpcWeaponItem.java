package net.shadowmage.ancientwarfare.core.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item.Properties;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWeaponEffect;

public class NpcWeaponItem extends Item {
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;
   private final float reach;
   private final NpcWeaponEffect effect;

   public NpcWeaponItem(Tier tier, double attackOffset, double attackSpeed, float reach, NpcWeaponEffect effect, Properties properties) {
      super(properties.durability(tier.getUses()));
      this.reach = reach;
      this.effect = effect;
      Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double)tier.getAttackDamageBonus() + attackOffset, Operation.ADDITION));
      builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", attackSpeed, Operation.ADDITION));
      this.defaultModifiers = builder.build();
   }

   public float reach() {
      return this.reach;
   }

   public NpcWeaponEffect effect() {
      return this.effect;
   }

   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      switch (this.effect) {
         case ICE:
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 150));
            break;
         case WITHER:
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100));
            break;
         case CLUB_KNOCKBACK:
            target.knockback(0.8, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
         case NONE:
      }

      stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      return true;
   }

   public boolean isEnchantable(ItemStack stack) {
      return true;
   }
}
