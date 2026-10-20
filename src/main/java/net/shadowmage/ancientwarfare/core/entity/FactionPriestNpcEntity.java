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
      this.m_21008_(InteractionHand.MAIN_HAND, new ItemStack(Items.f_42517_));
      this.applyFactionDefaults();
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 40.0).m_22268_(Attributes.f_22279_, 0.375).m_22268_(Attributes.f_22277_, 35.0);
   }

   protected void m_8099_() {
      this.addSharedGoals();
      this.f_21345_.m_25352_(1, new PanicGoal(this, 1.2));
      this.f_21345_.m_25352_(2, new FactionPriestNpcEntity.HealFactionAllyGoal(this));
   }

   private static class HealFactionAllyGoal extends Goal {
      private final FactionPriestNpcEntity priest;
      private int cooldown;

      private HealFactionAllyGoal(FactionPriestNpcEntity priest) {
         this.priest = priest;
         this.m_7021_(EnumSet.of(Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.cooldown-- > 0) {
            return false;
         } else {
            this.cooldown = 40;
            return this.findWoundedAlly() != null;
         }
      }

      public void m_8037_() {
         LivingEntity ally = this.findWoundedAlly();
         if (ally != null) {
            this.priest.m_21563_().m_148051_(ally);
            if (this.priest.m_20280_(ally) <= 36.0) {
               ally.m_5634_(1.0F);
               this.priest.swingWeapon();
            }
         }
      }

      private LivingEntity findWoundedAlly() {
         List<FactionNpcEntity> allies = this.priest
            .m_9236_()
            .m_6443_(
               FactionNpcEntity.class,
               this.priest.m_20191_().m_82400_(8.0),
               ally -> ally != this.priest && ally.m_6084_() && ally.getFactionName().equals(this.priest.getFactionName()) && ally.m_21223_() < ally.m_21233_()
            );
         return allies.isEmpty() ? null : (LivingEntity)allies.get(0);
      }
   }
}
