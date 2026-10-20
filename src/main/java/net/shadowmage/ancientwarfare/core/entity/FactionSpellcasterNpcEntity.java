package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;

public class FactionSpellcasterNpcEntity extends FactionNpcEntity implements RangedAttackMob {
   private static final double ATTACK_SPEED = 0.85;
   private static final int ATTACK_INTERVAL_TICKS = 40;
   private static final float ATTACK_RADIUS = 16.0F;

   public FactionSpellcasterNpcEntity(EntityType<? extends FactionSpellcasterNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.SPELLCASTER);
      this.m_21008_(InteractionHand.MAIN_HAND, new ItemStack(Items.f_42585_));
      this.applyFactionDefaults();
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 24.0).m_22268_(Attributes.f_22279_, 0.27).m_22268_(Attributes.f_22277_, 32.0);
   }

   protected void m_8099_() {
      this.addSharedGoals();
      this.f_21345_.m_25352_(2, new RangedAttackGoal(this, 0.85, 40, 16.0F));
      this.addFactionCombatTargets();
   }

   public void m_6504_(LivingEntity target, float velocity) {
      double x = target.m_20185_() - this.m_20185_();
      double y = target.m_20227_(0.5) - this.m_20227_(0.5);
      double z = target.m_20189_() - this.m_20189_();
      double horizontal = Math.sqrt(x * x + z * z);
      SmallFireball fireball = new SmallFireball(this.m_9236_(), this, x, y + horizontal * 0.08, z);
      fireball.m_6034_(this.m_20185_(), this.m_20227_(0.6), this.m_20189_());
      this.m_5496_(SoundEvents.f_11705_, 1.0F, 1.0F / (this.m_217043_().m_188501_() * 0.4F + 0.8F));
      this.m_6674_(InteractionHand.MAIN_HAND);
      this.m_9236_().m_7967_(fireball);
   }

   public boolean isSpellcastingAnimation() {
      return false;
   }
}
