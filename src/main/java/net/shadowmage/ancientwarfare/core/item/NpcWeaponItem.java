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
      super(properties.m_41503_(tier.m_6609_()));
      this.reach = reach;
      this.effect = effect;
      Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(Attributes.f_22281_, new AttributeModifier(f_41374_, "Weapon modifier", (double)tier.m_6631_() + attackOffset, Operation.ADDITION));
      builder.put(Attributes.f_22283_, new AttributeModifier(f_41375_, "Weapon modifier", attackSpeed, Operation.ADDITION));
      this.defaultModifiers = builder.build();
   }

   public float reach() {
      return this.reach;
   }

   public NpcWeaponEffect effect() {
      return this.effect;
   }

   public Multimap<Attribute, AttributeModifier> m_7167_(EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.m_7167_(slot);
   }

   public boolean m_7579_(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      switch (this.effect) {
         case ICE:
            target.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 50));
            target.m_7292_(new MobEffectInstance(MobEffects.f_19599_, 150));
            break;
         case WITHER:
            target.m_7292_(new MobEffectInstance(MobEffects.f_19615_, 100));
            break;
         case CLUB_KNOCKBACK:
            target.m_147240_(0.8, attacker.m_20185_() - target.m_20185_(), attacker.m_20189_() - target.m_20189_());
         case NONE:
      }

      stack.m_41622_(1, attacker, entity -> entity.m_21166_(EquipmentSlot.MAINHAND));
      return true;
   }

   public boolean m_8120_(ItemStack stack) {
      return true;
   }
}
