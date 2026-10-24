package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;

public class FactionPriestNpcEntity extends FactionNpcEntity {
   public FactionPriestNpcEntity(EntityType<? extends FactionPriestNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.PRIEST);
      this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));
      this.applyFactionDefaults();
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.375).add(Attributes.FOLLOW_RANGE, 35.0);
   }

   protected void registerGoals() {
      this.addSharedGoals();
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.2));
      this.goalSelector.addGoal(2, new FactionPriestNpcEntity.HealFactionAllyGoal(this));
   }

   private static class HealFactionAllyGoal extends Goal {
      private final FactionPriestNpcEntity priest;
      private int cooldown;

      private HealFactionAllyGoal(FactionPriestNpcEntity priest) {
         this.priest = priest;
         this.setFlags(EnumSet.of(Flag.LOOK));
      }

      public boolean canUse() {
         if (this.cooldown-- > 0) {
            return false;
         } else {
            this.cooldown = 40;
            return this.findWoundedAlly() != null;
         }
      }

      public void tick() {
         LivingEntity ally = this.findWoundedAlly();
         if (ally != null) {
            this.priest.getLookControl().setLookAt(ally);
            if (this.priest.distanceToSqr(ally) <= 36.0) {
               ally.heal(1.0F);
               this.priest.swingWeapon();
            }
         }
      }

      private LivingEntity findWoundedAlly() {
         List<FactionNpcEntity> allies = this.priest
            .level()
            .getEntitiesOfClass(
               FactionNpcEntity.class,
               this.priest.getBoundingBox().inflate(8.0),
               ally -> ally != this.priest && ally.isAlive() && ally.getFactionName().equals(this.priest.getFactionName()) && ally.getHealth() < ally.getMaxHealth()
            );
         return allies.isEmpty() ? null : (LivingEntity)allies.get(0);
      }
   }
}
