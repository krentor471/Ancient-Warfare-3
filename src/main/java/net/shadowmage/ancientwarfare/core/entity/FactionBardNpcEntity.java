package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;

public class FactionBardNpcEntity extends FactionNpcEntity {
   public FactionBardNpcEntity(EntityType<? extends FactionBardNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.BARD);
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 18.0).m_22268_(Attributes.f_22279_, 0.25).m_22268_(Attributes.f_22277_, 20.0);
   }

   protected void m_8099_() {
      this.addSharedGoals();
      this.f_21345_.m_25352_(1, new PanicGoal(this, 1.25));
   }
}
