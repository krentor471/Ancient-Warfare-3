package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;

public class FactionSoldierEliteNpcEntity extends FactionSoldierNpcEntity {
   public FactionSoldierEliteNpcEntity(EntityType<? extends FactionSoldierNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.SOLDIER_ELITE, new ItemStack(Items.f_42383_));
   }

   public static Builder createAttributes() {
      return m_21552_()
         .m_22268_(Attributes.f_22276_, 30.0)
         .m_22268_(Attributes.f_22279_, 0.325)
         .m_22268_(Attributes.f_22281_, 4.0)
         .m_22268_(Attributes.f_22283_, 4.0)
         .m_22268_(Attributes.f_22277_, 35.0);
   }
}
