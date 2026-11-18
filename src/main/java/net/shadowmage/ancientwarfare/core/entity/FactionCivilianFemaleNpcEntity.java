package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;

public class FactionCivilianFemaleNpcEntity extends FactionNpcEntity {
   public FactionCivilianFemaleNpcEntity(EntityType<? extends FactionCivilianFemaleNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.CIVILIAN_FEMALE);
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 20.0);
   }

   protected void registerGoals() {
      this.addSharedGoals();
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
      this.goalSelector.addGoal(4, new EatMealGoal(this));
      this.goalSelector.addGoal(5, new CivilianRoutineGoal(this));
      this.goalSelector.addGoal(5, new TradeWorkGoal(this));
   }
}
