package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;

public class FactionArcherEliteNpcEntity extends FactionArcherNpcEntity {
   public FactionArcherEliteNpcEntity(EntityType<? extends FactionArcherNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.ARCHER_ELITE);
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 30.0).m_22268_(Attributes.f_22279_, 0.27).m_22268_(Attributes.f_22277_, 80.0);
   }
}
