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
      this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLAZE_ROD));
      this.applyFactionDefaults();
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 32.0);
   }

   protected void registerGoals() {
      this.addSharedGoals();
      this.goalSelector.addGoal(2, new RangedAttackGoal(this, 0.85, 40, 16.0F));
      this.addFactionCombatTargets();
   }

   public void performRangedAttack(LivingEntity target, float velocity) {
      double x = target.getX() - this.getX();
      double y = target.getY(0.5) - this.getY(0.5);
      double z = target.getZ() - this.getZ();
      double horizontal = Math.sqrt(x * x + z * z);
      SmallFireball fireball = new SmallFireball(this.level(), this, x, y + horizontal * 0.08, z);
      fireball.setPos(this.getX(), this.getY(0.6), this.getZ());
      this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
      this.swing(InteractionHand.MAIN_HAND);
      this.level().addFreshEntity(fireball);
   }

   public boolean isSpellcastingAnimation() {
      return false;
   }
}
